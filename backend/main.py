import re
import base64
import os
import asyncio
import logging
import tempfile
import time
import aiofiles
from collections import defaultdict
from contextlib import asynccontextmanager

from fastapi import (
    Response,
    FastAPI, UploadFile, File, Form, Header, HTTPException, Request,
)
from fastapi.responses import StreamingResponse, Response
from fastapi.middleware.cors import CORSMiddleware
import firebase_admin
from firebase_admin import credentials, firestore, messaging, auth as fb_auth

from config import FIREBASE_JSON_PATH, MAX_UPLOAD_BYTES, TG_CHANNEL_ID
from telegram_client import (
    get_client, upload_video, stream_video, get_video_info, delete_video,
    upload_photo, stream_photo, get_photo_info,
)
from auth import verify_token

logging.basicConfig(level=logging.INFO)
log = logging.getLogger("api")

if not firebase_admin._apps:
    cred = credentials.Certificate(FIREBASE_JSON_PATH)
    firebase_admin.initialize_app(cred)
db = firestore.client()

# ── Simple in-memory rate limiter (per-key sliding window) ──
_rate_buckets: dict = defaultdict(list)

def rate_limit(key: str, max_calls: int, window_sec: int) -> None:
    """Raise 429 if key exceeds max_calls within window_sec."""
    now = time.time()
    bucket = _rate_buckets[key]
    bucket[:] = [t for t in bucket if now - t < window_sec]
    if len(bucket) >= max_calls:
        raise HTTPException(429, "Too many requests — slow down")
    bucket.append(now)

# ── /stream HEAD info cache (msg_id -> info dict) ──
_info_cache: dict = {}

def _cache_put(msg_id: int, info: dict) -> None:
    if len(_info_cache) > 512:
        _info_cache.clear()
    _info_cache[msg_id] = info


@asynccontextmanager
async def lifespan(app: FastAPI):
    log.info("Startup: connecting Telegram...")
    await get_client()
    log.info("Channel: %s" % TG_CHANNEL_ID)
    yield
    log.info("Shutdown")


app = FastAPI(title="Telegram TikTok Backend", lifespan=lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], allow_methods=["*"], allow_headers=["*"],
)


@app.get("/")
@app.head("/")
async def root():
    return {"ok": True, "service": "telegram-tiktok", "channel": TG_CHANNEL_ID}


@app.get("/health")
@app.head("/health")
async def health():
    # HEAD support added so UptimeRobot (uses HEAD by default) doesn't 405
    return {"status": "healthy"}


@app.post("/upload")
async def upload(
    file: UploadFile = File(...),
    caption: str = Form(""),
    duration: int = Form(0),
    width: int = Form(0),
    height: int = Form(0),
    authorization: str = Header(...),
):
    uid = await verify_token(authorization)
    rate_limit(f"upload:{uid}", max_calls=5, window_sec=60)
    log.info("Upload by uid=%s filename=%s" % (uid, file.filename))

    tmp = tempfile.NamedTemporaryFile(delete=False, suffix=".mp4")
    tmp_path = tmp.name
    tmp.close()
    total = 0

    try:
        async with aiofiles.open(tmp_path, "wb") as f:
            while True:
                chunk = await file.read(1024 * 1024)
                if not chunk:
                    break
                total += len(chunk)
                if total > MAX_UPLOAD_BYTES:
                    raise HTTPException(413, "File > 2GB limit")
                await f.write(chunk)
        if total == 0:
            raise HTTPException(400, "Empty file")
        log.info("Saved %s bytes" % total)

        # ---- Generate thumbnail (first frame at ~1s, 256px wide) ----
        thumb_b64 = ""
        try:
            import subprocess as _sp
            thumb_path = tmp_path + ".thumb.jpg"
            r = _sp.run(
                ["ffmpeg", "-y", "-ss", "1", "-i", tmp_path,
                 "-vframes", "1", "-vf", "scale=256:-2",
                 "-q:v", "5", thumb_path],
                capture_output=True, timeout=30
            )
            if r.returncode == 0 and os.path.exists(thumb_path):
                with open(thumb_path, "rb") as tf:
                    raw = tf.read()
                if len(raw) < 200 * 1024:  # sanity cap
                    thumb_b64 = base64.b64encode(raw).decode("ascii")
                try: os.unlink(thumb_path)
                except: pass
                log.info(f"thumbnail {len(thumb_b64)} b64 chars")
            else:
                log.warning(f"thumb ffmpeg rc={r.returncode}")
        except Exception as e:
            log.warning(f"thumbnail skipped: {e}")

        msg_id = await upload_video(tmp_path, caption, duration, width, height)
    finally:
        try:
            os.unlink(tmp_path)
        except Exception:
            pass

    # Lookup uploader's profile for display name
    uploader_name = "user"
    uploader_handle = ""
    uploader_pic_id = 0
    try:
        u_doc = db.collection("users").document(uid).get()
        if u_doc.exists:
            d = u_doc.to_dict() or {}
            uploader_handle = d.get("username", "") or ""
            uploader_name = d.get("fullName", "") or d.get("firstName", "user")
            uploader_pic_id = int(d.get("profilePicMsgId", 0) or 0)
    except Exception as e:
        log.warning(f"profile lookup failed: {e}")

    doc = db.collection("videos").document()
    video_id = doc.id

    # ---- Extract hashtags ----
    tags = sorted(set(
        t.lower() for t in re.findall(r"#([A-Za-z0-9_]+)", caption or "")
    ))[:10]
    log.info(f"hashtags={tags}")

    doc.set({
        "uploader": uid,
        "uploaderName": uploader_name,
        "uploaderHandle": uploader_handle,
        "uploaderPicMsgId": uploader_pic_id,
        "caption": caption,
        "telegramMsgId": msg_id,
        "sizeBytes": total,
        "duration": duration,
        "width": width,
        "height": height,
        "thumbB64": thumb_b64 if 'thumb_b64' in dir() else "",
        "hashtags": tags,
        "likes": 0,
        "views": 0,
        "comments": 0,
        "createdAt": firestore.SERVER_TIMESTAMP,
    })

    # ---- Update hashtag aggregates ----
    for tag in tags:
        try:
            db.collection("hashtags").document(tag).set(
                {
                    "tag": tag,
                    "videoCount": firestore.Increment(1),
                    "updatedAt": firestore.SERVER_TIMESTAMP,
                },
                merge=True,
            )
            db.collection("hashtags").document(tag) \
                .collection("posts").document(video_id).set({
                    "videoId": video_id,
                    "createdAt": firestore.SERVER_TIMESTAMP,
                })
        except Exception as e:
            log.warning(f"hashtag write failed for {tag}: {e}")

    return {"videoId": video_id, "msgId": msg_id, "size": total}


@app.api_route("/stream/{msg_id}", methods=["GET", "HEAD"])
async def stream(msg_id: int, request: Request):
    """Cache + remux with faststart for streaming."""
    info = _info_cache.get(msg_id)
    if not info:
        info = await get_video_info(msg_id)
        if info:
            _cache_put(msg_id, info)
    if not info:
        raise HTTPException(404, "Video not found")

    size = info["size"]
    mime = info["mime"]

    if request.method == "HEAD":
        return Response(
            content=b"",
            media_type=mime,
            headers={
                "Accept-Ranges": "bytes",
                "Content-Length": str(size),
                "Cache-Control": "public, max-age=86400",
            },
        )

    # ---- Cache + remux ----
    cache_dir = "/tmp/earny_cache"
    os.makedirs(cache_dir, exist_ok=True)
    cache_path = f"{cache_dir}/{msg_id}_fast.mp4"

    if not os.path.exists(cache_path):
        log.info(f"/stream/{msg_id} caching + remuxing")
        tmp_raw = f"{cache_dir}/{msg_id}_raw.mp4"
        try:
            with open(tmp_raw, "wb") as fh:
                async for ch in stream_video(msg_id, offset=0):
                    fh.write(ch)
            log.info(f"/stream/{msg_id} downloaded {os.path.getsize(tmp_raw)} bytes")

            import subprocess
            result = await asyncio.to_thread(
                subprocess.run,
                ["ffmpeg", "-y", "-i", tmp_raw,
                 "-c", "copy", "-movflags", "+faststart",
                 cache_path],
                capture_output=True, timeout=120,
            )
            if result.returncode != 0:
                log.error(f"ffmpeg err: {result.stderr.decode()[:500]}")
                os.rename(tmp_raw, cache_path)
            else:
                try: os.unlink(tmp_raw)
                except: pass
            log.info(f"/stream/{msg_id} cached {os.path.getsize(cache_path)} bytes")
        except Exception as e:
            log.error(f"/stream/{msg_id} failed: {e}")
            try: os.unlink(tmp_raw)
            except: pass
            raise HTTPException(500, "Cache failed")

    # ---- Serve from cache ----
    actual_size = os.path.getsize(cache_path)
    range_header = request.headers.get("range")

    if not range_header:
        async def _full_stream():
            with open(cache_path, "rb") as fh:
                while True:
                    b = fh.read(1024 * 1024)
                    if not b:
                        break
                    yield b
        return StreamingResponse(
            _full_stream(),
            media_type=mime,
            headers={
                "Accept-Ranges": "bytes",
                "Content-Length": str(actual_size),
                "Cache-Control": "public, max-age=86400",
            },
        )

    # Parse range (support suffix: bytes=-N)
    try:
        r = range_header.replace("bytes=", "").strip()
        if r.startswith("-"):
            # Suffix: last N bytes
            n = int(r[1:])
            start = max(0, actual_size - n)
            end = actual_size - 1
        else:
            parts = r.split("-")
            start = int(parts[0]) if parts[0] else 0
            end = int(parts[1]) if len(parts) > 1 and parts[1] else actual_size - 1
    except Exception:
        raise HTTPException(416, "Bad range")

    if start >= actual_size:
        raise HTTPException(416, "Out of bounds")
    if end >= actual_size:
        end = actual_size - 1
    if start > end:
        raise HTTPException(416, "Bad range")

    length = end - start + 1
    log.info(f"/stream/{msg_id} {start}-{end} ({length}b)")

    async def _range_stream():
        with open(cache_path, "rb") as fh:
            fh.seek(start)
            remaining = length
            while remaining > 0:
                b = fh.read(min(1024 * 1024, remaining))
                if not b:
                    break
                remaining -= len(b)
                yield b

    return StreamingResponse(
        _range_stream(),
        status_code=206,
        media_type=mime,
        headers={
            "Content-Range": f"bytes {start}-{end}/{actual_size}",
            "Accept-Ranges": "bytes",
            "Content-Length": str(length),
            "Cache-Control": "public, max-age=86400",
        },
    )

@app.post("/view/{video_id}")
async def increment_view(video_id: str, authorization: str = Header(...)):
    uid = await verify_token(authorization)
    rate_limit(f"view:{uid}", max_calls=120, window_sec=60)
    ref = db.collection("videos").document(video_id)
    if not ref.get().exists:
        raise HTTPException(404, "Video not found")
    ref.update({"views": firestore.Increment(1)})
    return {"ok": True}


@app.delete("/video/{video_id}")
async def delete_own_video(video_id: str, authorization: str = Header(...)):
    uid = await verify_token(authorization)
    doc_ref = db.collection("videos").document(video_id)
    doc = doc_ref.get()
    if not doc.exists:
        raise HTTPException(404, "Not found")
    data = doc.to_dict()
    if data["uploader"] != uid:
        raise HTTPException(403, "Not your video")
    await delete_video(data["telegramMsgId"])
    doc_ref.delete()
    return {"ok": True}

# ═══════════════════════════════════════════════════════
#  PROFILE PICTURE — upload + serve
# ═══════════════════════════════════════════════════════

@app.post("/profile-pic")
async def upload_profile_pic(
    file: UploadFile = File(...),
    authorization: str = Header(...),
):
    uid = await verify_token(authorization)
    log.info(f"profile-pic upload uid={uid}")

    # Validate mime
    ct = (file.content_type or "").lower()
    if not ct.startswith("image/"):
        raise HTTPException(400, "Only images allowed")

    tmp = tempfile.NamedTemporaryFile(delete=False, suffix=".jpg")
    tmp_path = tmp.name
    tmp.close()
    total = 0

    try:
        async with aiofiles.open(tmp_path, "wb") as f:
            while True:
                chunk = await file.read(256 * 1024)
                if not chunk:
                    break
                total += len(chunk)
                if total > 5 * 1024 * 1024:
                    raise HTTPException(413, "Image > 5 MB")
                await f.write(chunk)
        if total == 0:
            raise HTTPException(400, "Empty file")

        msg_id = await upload_photo(tmp_path, f"pic:{uid}")
    finally:
        try: os.unlink(tmp_path)
        except: pass

    # Save reference in Firestore
    db.collection("users").document(uid).set(
        {"profilePicMsgId": msg_id, "profilePicUpdatedAt": firestore.SERVER_TIMESTAMP},
        merge=True,
    )

    return {"ok": True, "msgId": msg_id}


@app.get("/profile-pic/{msg_id}")
async def get_profile_pic(msg_id: int):
    try:
        info = await get_photo_info(msg_id)
        if not info:
            raise HTTPException(404, "Not found")
        mime = info.get("mime", "image/jpeg")

        async def gen():
            async for chunk in stream_photo(msg_id):
                yield chunk

        return StreamingResponse(
            gen(),
            media_type=mime,
            headers={
                "Cache-Control": "public, max-age=604800",
                "X-Content-Type-Options": "nosniff",
            },
        )
    except HTTPException:
        raise
    except Exception as e:
        log.warning(f"profile-pic fetch failed: {e}")
        raise HTTPException(404, "Not found")

# ═══════════════════════════════════════════════════════
#  PUSH NOTIFICATIONS — FCM via Firebase Admin
# ═══════════════════════════════════════════════════════

from pydantic import BaseModel as _PydBase

class _NotifyReq(_PydBase):
    target_uid: str
    kind: str            # "like" | "comment" | "follow"
    title: str = ""
    body: str = ""
    video_id: str = ""
    data_extra: str = ""


@app.post("/notify")
async def notify(
    payload: _NotifyReq,
    authorization: str = Header(...),
):
    sender_uid = await verify_token(authorization)
    rate_limit(f"notify:{sender_uid}", max_calls=30, window_sec=60)
    if sender_uid == payload.target_uid:
        return {"ok": True, "skipped": "self"}

    # Lookup target's FCM token
    try:
        doc = db.collection("users").document(payload.target_uid).get()
        if not doc.exists:
            return {"ok": False, "reason": "target not found"}
        target_data = doc.to_dict() or {}
        token = target_data.get("fcmToken")
        if not token:
            return {"ok": False, "reason": "no token"}
    except Exception as e:
        log.warning(f"notify lookup failed: {e}")
        return {"ok": False, "reason": "lookup failed"}

    # Sender display name
    try:
        s_doc = db.collection("users").document(sender_uid).get()
        s_data = s_doc.to_dict() or {}
        sender_name = (s_data.get("fullName")
                       or s_data.get("username")
                       or "Someone")
    except Exception:
        sender_name = "Someone"

    title = payload.title or "Earny"
    body = payload.body or f"{sender_name} interacted with your content"

    message = messaging.Message(
        notification=messaging.Notification(title=title, body=body),
        data={
            "kind": payload.kind,
            "videoId": payload.video_id,
            "senderUid": sender_uid,
        },
        token=token,
    )

    # ---- 1. Write notification doc (always, even if FCM fails) ----
    notif_ref = db.collection("users").document(payload.target_uid) \
        .collection("notifications").document()
    try:
        notif_ref.set({
            "kind": payload.kind,
            "title": title,
            "body": body,
            "videoId": payload.video_id,
            "senderUid": sender_uid,
            "senderName": sender_name,
            "unread": True,
            "createdAt": firestore.SERVER_TIMESTAMP,
        })
    except Exception as e:
        log.warning(f"notif doc write failed: {e}")

    # ---- 2. Send FCM push ----
    try:
        messaging.send(message)
        log.info(f"notify sent to {payload.target_uid} ({payload.kind})")
        return {"ok": True}
    except Exception as e:
        log.warning(f"notify send failed: {e}")
        try:
            db.collection("users").document(payload.target_uid).update(
                {"fcmToken": firestore.DELETE_FIELD}
            )
        except Exception:
            pass
        # Still return ok=True because the notif doc was saved
        return {"ok": True, "fcm": False, "reason": str(e)}


@app.post("/fcm-token")
async def register_fcm_token(
    request: Request,
    authorization: str = Header(...),
):
    uid = await verify_token(authorization)
    data = await request.json()
    token = (data.get("token") or "").strip()
    if not token:
        raise HTTPException(400, "Missing token")
    db.collection("users").document(uid).set(
        {"fcmToken": token}, merge=True
    )
    return {"ok": True}

# ═══════════════════════════════════════════════════════
#  DELETE ACCOUNT — full cleanup
# ═══════════════════════════════════════════════════════

@app.post("/delete-account")
async def delete_account(authorization: str = Header(...)):
    uid = await verify_token(authorization)
    log.info(f"delete-account uid={uid}")

    # 1. Delete user's videos (+ Telegram messages + subcollections)
    try:
        vids = db.collection("videos").whereEqualTo("uploader", uid).stream()
        for v in vids:
            data = v.to_dict() or {}
            msg_id = data.get("telegramMsgId")
            if msg_id:
                try:
                    await delete_video(int(msg_id))
                except Exception:
                    pass
            # Subcollections
            for sub in ("likes", "comments", "views", "reposts"):
                for s in v.reference.collection(sub).stream():
                    try: s.reference.delete()
                    except Exception: pass
            try: v.reference.delete()
            except Exception: pass
    except Exception as e:
        log.warning(f"video cleanup failed: {e}")

    # 2. Delete profile picture from Telegram
    try:
        u = db.collection("users").document(uid).get()
        if u.exists:
            pic_id = (u.to_dict() or {}).get("profilePicMsgId")
            if pic_id:
                try:
                    await delete_video(int(pic_id))
                except Exception:
                    pass
    except Exception as e:
        log.warning(f"pic cleanup failed: {e}")

    # 3. Delete comments this user made on other videos
    #    + decrement parent video's comments counter (avoids drift)
    try:
        for v in db.collection("videos").stream():
            removed = 0
            for c in v.reference.collection("comments").whereEqualTo("uid", uid).stream():
                try:
                    c.reference.delete()
                    removed += 1
                except Exception:
                    pass
            if removed > 0:
                try:
                    v.reference.update({"comments": firestore.Increment(-removed)})
                except Exception:
                    pass
    except Exception as e:
        log.warning(f"comment cleanup failed: {e}")

    # 4. Delete likes, follows, blocks, reports, bookmarks, notifications
    for coll in ("follows", "blocks"):
        try:
            for d in db.collection(coll).whereEqualTo("blocker" if coll == "blocks" else "follower", uid).stream():
                try: d.reference.delete()
                except Exception: pass
            for d in db.collection(coll).whereEqualTo("blocked" if coll == "blocks" else "followee", uid).stream():
                try: d.reference.delete()
                except Exception: pass
        except Exception as e:
            log.warning(f"{coll} cleanup failed: {e}")

    try:
        for d in db.collection("reports").whereEqualTo("reporter", uid).stream():
            try: d.reference.delete()
            except Exception: pass
    except Exception:
        pass

    # 5. Delete user sub-collections (chats live top-level, not here)
    try:
        for sub in ("bookmarks", "notifications", "drafts", "sessions", "settings"):
            for d in db.collection("users").document(uid).collection(sub).stream():
                try: d.reference.delete()
                except Exception: pass
    except Exception as e:
        log.warning(f"user sub cleanup failed: {e}")

    # 5b. Delete DM chats + their message subcollections
    try:
        for chat in db.collection("chats") \
                .whereArrayContains("participants", uid).stream():
            for m in chat.reference.collection("messages").stream():
                try: m.reference.delete()
                except Exception: pass
            try: chat.reference.delete()
            except Exception: pass
    except Exception as e:
        log.warning(f"chat cleanup failed: {e}")

    # 6. Delete username reservation
    try:
        u = db.collection("users").document(uid).get()
        if u.exists:
            uname = (u.to_dict() or {}).get("usernameLower")
            if uname:
                try:
                    db.collection("usernames").document(uname).delete()
                except Exception: pass
    except Exception:
        pass

    # 7. Delete user doc
    try:
        db.collection("users").document(uid).delete()
    except Exception as e:
        log.warning(f"user doc delete failed: {e}")

    # 8. Delete Firebase Auth user
    try:
        fb_auth.delete_user(uid)
    except Exception as e:
        log.warning(f"auth delete failed: {e}")
        raise HTTPException(500, f"Auth delete failed: {e}")

    log.info(f"delete-account done for {uid}")
    return {"ok": True}

# ═══════════════════════════════════════════════════════════
#  SHARE PAGE — TikTok-style /v/{videoId}
# ═══════════════════════════════════════════════════════════

SHARE_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
<title>{TITLE}</title>
<meta name="description" content="{DESCRIPTION}">
<meta property="og:type" content="video.other">
<meta property="og:title" content="{TITLE}">
<meta property="og:description" content="{DESCRIPTION}">
<meta property="og:image" content="{THUMB_URL}">
<meta property="og:image:width" content="720">
<meta property="og:image:height" content="1280">
<meta property="og:url" content="{CANONICAL}">
<meta property="og:site_name" content="Earny">
<meta name="twitter:card" content="player">
<meta name="twitter:title" content="{TITLE}">
<meta name="twitter:description" content="{DESCRIPTION}">
<meta name="twitter:image" content="{THUMB_URL}">
<meta name="theme-color" content="#000000">
<style>
*{box-sizing:border-box;margin:0;padding:0;-webkit-tap-highlight-color:transparent}
body{background:#000;color:#fff;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif;min-height:100vh;display:flex;align-items:center;justify-content:center;padding:16px}
.card{width:100%;max-width:420px;background:#121212;border-radius:20px;overflow:hidden;box-shadow:0 20px 60px rgba(0,0,0,.6)}
.hero{position:relative;aspect-ratio:9/16;background:#1a1a1a;overflow:hidden}
.hero img{width:100%;height:100%;object-fit:cover;display:block}
.play{position:absolute;inset:0;display:flex;align-items:center;justify-content:center;background:rgba(0,0,0,.15);cursor:pointer;transition:opacity .2s}
.play svg{width:80px;height:80px;opacity:.85;filter:drop-shadow(0 4px 12px rgba(0,0,0,.6))}
.badge{position:absolute;top:14px;left:14px;background:linear-gradient(135deg,#FFD700,#FF8C00);color:#000;padding:6px 12px;border-radius:100px;font-weight:800;font-size:11px;letter-spacing:.5px}
.meta{padding:16px 18px 8px}
.username{font-weight:800;color:#FFD700;font-size:15px;margin-bottom:6px}
.caption{color:#fff;font-size:14px;line-height:1.4;opacity:.95;word-break:break-word}
.stats{display:flex;gap:18px;margin-top:12px;color:rgba(255,255,255,.6);font-size:13px;font-weight:600}
.stats span{display:flex;align-items:center;gap:5px}
.actions{padding:14px 18px 18px;display:flex;flex-direction:column;gap:10px}
.btn{display:block;text-align:center;padding:14px;border-radius:100px;font-weight:800;font-size:15px;text-decoration:none;transition:transform .1s;border:none;cursor:pointer}
.btn:active{transform:scale(.97)}
.btn-primary{background:linear-gradient(135deg,#FFD700,#FF8C00);color:#000}
.btn-secondary{background:#1E1E1E;color:#fff}
.footer{text-align:center;padding:0 18px 20px;color:rgba(255,255,255,.35);font-size:11px}
</style>
</head>
<body>
<div class="card">
  <div class="hero">
    <img src="{THUMB_URL}" alt="">
    <div class="play" id="playBtn" onclick="openApp()">
      <svg viewBox="0 0 24 24" fill="white"><circle cx="12" cy="12" r="11" fill="rgba(0,0,0,.5)"/><path d="M10 8l6 4-6 4V8z" fill="#FFD700"/></svg>
    </div>
    <div class="badge">EARNY</div>
  </div>
  <div class="meta">
    <div class="username">@{USERNAME}</div>
    <div class="caption">{CAPTION}</div>
    <div class="stats">
      <span>❤️ {LIKES}</span>
      <span>💬 {COMMENTS}</span>
      <span>👁 {VIEWS}</span>
    </div>
  </div>
  <div class="actions">
    <a class="btn btn-primary" id="openBtn" href="javascript:openApp()">▶ Watch in Earny</a>
    <a class="btn btn-secondary" href="https://play.google.com/store/apps/details?id=com.aim.earny">Get Earny — it's free</a>
  </div>
  <div class="footer">Made with Earny · earny.new.tg</div>
</div>
<script>
var VIDEO_ID = "{VIDEO_ID}";
var CANONICAL = "{CANONICAL}";
var INTENT_URL = "intent://earny.new.tg/v/" + VIDEO_ID +
  "#Intent;scheme=https;package=com.aim.earny;S.browser_fallback_url=" +
  encodeURIComponent(CANONICAL) + ";end";

function openApp() {
  var t0 = Date.now();
  window.location = INTENT_URL;
  setTimeout(function() {
    if (Date.now() - t0 < 2000) return;
    var b = document.getElementById("openBtn");
    if (b) b.textContent = "📲 Get Earny (free)";
  }, 1500);
}
if (/Android/i.test(navigator.userAgent)) {
  setTimeout(openApp, 250);
}
</script>
</body>
</html>
"""

@app.get("/v/{video_id}")
async def share_page(video_id: str, request: Request):
    doc = db.collection("videos").document(video_id).get()
    if not doc.exists:
        raise HTTPException(404, "Video not found")
    v = doc.to_dict() or {}
    handle = v.get("uploaderHandle", "") or v.get("uploaderName", "user")
    caption = (v.get("caption", "") or "").strip()
    likes = int(v.get("likes", 0) or 0)
    comments = int(v.get("comments", 0) or 0)
    views = int(v.get("views", 0) or 0)
    
    title = caption[:80] if caption else f"@{handle} on Earny"
    desc = caption if caption else f"Watch @{handle}'s video on Earny"
    
    canonical = f"https://earny.new.tg/v/{video_id}"
    thumb_url = f"https://earny.new.tg/thumb/{video_id}"
    
    import html as _html
    html_out = (SHARE_HTML
        .replace("{VIDEO_ID}", video_id)
        .replace("{CANONICAL}", canonical)
        .replace("{THUMB_URL}", thumb_url)
        .replace("{TITLE}", _html.escape(title))
        .replace("{DESCRIPTION}", _html.escape(desc))
        .replace("{USERNAME}", _html.escape(handle))
        .replace("{CAPTION}", _html.escape(caption))
        .replace("{LIKES}", str(likes))
        .replace("{COMMENTS}", str(comments))
        .replace("{VIEWS}", str(views)))
    return Response(content=html_out, media_type="text/html; charset=utf-8")


@app.get("/thumb/{video_id}")
async def share_thumb(video_id: str):
    doc = db.collection("videos").document(video_id).get()
    if not doc.exists:
        raise HTTPException(404, "Not found")
    thumb_b64 = (doc.to_dict() or {}).get("thumbB64") or ""
    if not thumb_b64:
        raise HTTPException(404, "No thumb")
    try:
        raw = base64.b64decode(thumb_b64)
    except Exception:
        raise HTTPException(500, "Bad thumb")
    return Response(content=raw, media_type="image/jpeg", headers={"Cache-Control": "public, max-age=604800"})


@app.get("/.well-known/assetlinks.json")
async def assetlinks():
    return [{
        "relation": ["delegate_permission/common.handle_all_urls"],
        "target": {
            "namespace": "android_app",
            "package_name": "com.aim.earny",
            "sha256_cert_fingerprints": ["4C:76:C2:34:E7:0D:35:C1:63:41:B2:3F:28:72:E4:3C:00:F1:97:91:DC:7E:46:1D:DA:7D:46:72:73:66:16:ED"],
        },
    }]

# ═══════════════════════════════════════════════════════════
#  PROFILE SHARE PAGE — /@username
# ═══════════════════════════════════════════════════════════

PROFILE_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
<title>@{USERNAME} on Earny</title>
<meta name="description" content="{BIO}">
<meta property="og:type" content="profile">
<meta property="og:title" content="@{USERNAME} on Earny">
<meta property="og:description" content="{BIO}">
<meta property="og:image" content="{THUMB_URL}">
<meta property="og:url" content="{CANONICAL}">
<meta property="og:site_name" content="Earny">
<meta name="twitter:card" content="summary">
<meta name="theme-color" content="#000000">
<style>
*{box-sizing:border-box;margin:0;padding:0;-webkit-tap-highlight-color:transparent}
body{background:#000;color:#fff;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif;min-height:100vh;display:flex;align-items:center;justify-content:center;padding:16px}
.card{width:100%;max-width:420px;background:#121212;border-radius:20px;overflow:hidden;box-shadow:0 20px 60px rgba(0,0,0,.6);text-align:center;padding:32px 24px}
.avatar{width:96px;height:96px;border-radius:50%;object-fit:cover;margin:0 auto 16px;background:#1a1a1a;border:3px solid #FFD700;display:flex;align-items:center;justify-content:center;font-size:36px;font-weight:800;color:#FFD700}
.username{font-size:22px;font-weight:800;color:#fff;margin-bottom:6px}
.bio{color:rgba(255,255,255,.7);font-size:14px;line-height:1.4;margin-bottom:24px;word-break:break-word;min-height:20px}
.stats{display:flex;justify-content:center;gap:24px;margin-bottom:28px}
.stat-val{font-size:16px;font-weight:800;color:#fff;display:block}
.stat-lbl{font-size:11px;color:rgba(255,255,255,.5);text-transform:uppercase;letter-spacing:.5px}
.btn{display:block;width:100%;text-align:center;padding:16px;border-radius:100px;font-weight:800;font-size:15px;text-decoration:none;transition:transform .1s;border:none;cursor:pointer;margin-bottom:10px}
.btn:active{transform:scale(.97)}
.btn-primary{background:linear-gradient(135deg,#FFD700,#FF8C00);color:#000}
.btn-secondary{background:#1E1E1E;color:#fff}
.footer{color:rgba(255,255,255,.35);font-size:11px;margin-top:10px}
</style>
</head>
<body>
<div class="card">
  <img src="{THUMB_URL}" class="avatar" onerror="this.style.display='none';this.nextElementSibling.style.display='flex'">
  <div class="avatar" style="display:none">E</div>
  
  <div class="username">@{USERNAME}</div>
  <div class="bio">{BIO}</div>
  
  <div class="stats">
    <div><span class="stat-val">{FOLLOWERS}</span><span class="stat-lbl">Followers</span></div>
    <div><span class="stat-val">{FOLLOWING}</span><span class="stat-lbl">Following</span></div>
    <div><span class="stat-val">{LIKES}</span><span class="stat-lbl">Likes</span></div>
  </div>
  
  <a class="btn btn-primary" id="openBtn" href="javascript:openApp()">Open in Earny</a>
  <a class="btn btn-secondary" href="https://play.google.com/store/apps/details?id=com.aim.earny">Get Earny — it's free</a>
  
  <div class="footer">Earny · earny.new.tg</div>
</div>
<script>
var USERNAME = "{USERNAME}";
var CANONICAL = "{CANONICAL}";
var INTENT_URL = "intent://earny.new.tg/@" + USERNAME +
  "#Intent;scheme=https;package=com.aim.earny;S.browser_fallback_url=" +
  encodeURIComponent(CANONICAL) + ";end";

function openApp() {
  var t0 = Date.now();
  window.location = INTENT_URL;
  setTimeout(function() {
    if (Date.now() - t0 < 2000) return;
    var b = document.getElementById("openBtn");
    if (b) b.textContent = "📲 Get Earny (free)";
  }, 1500);
}
if (/Android/i.test(navigator.userAgent)) {
  setTimeout(openApp, 250);
}
</script>
</body>
</html>
"""

@app.get("/@{username}")
async def share_profile(username: str):
    clean = username.lower().strip()
    # Query user by usernameLower
    users_ref = db.collection("users").where("usernameLower", "==", clean).limit(1).get()
    
    if not users_ref:
        raise HTTPException(404, "User not found")
        
    user_doc = users_ref[0]
    u = user_doc.to_dict() or {}
    
    handle = u.get("username", clean)
    bio = (u.get("bio", "") or "").strip()
    followers = int(u.get("followers", 0) or 0)
    following = int(u.get("following", 0) or 0)
    likes = int(u.get("totalLikes", 0) or 0)
    pic_id = u.get("profilePicMsgId", 0) or 0
    
    canonical = f"https://earny.new.tg/@{handle}"
    thumb_url = f"https://earny.new.tg/profile-pic/{pic_id}" if pic_id > 0 else ""
    
    import html as _html
    html_out = (PROFILE_HTML
        .replace("{USERNAME}", _html.escape(handle))
        .replace("{BIO}", _html.escape(bio) if bio else "No bio yet.")
        .replace("{THUMB_URL}", thumb_url)
        .replace("{CANONICAL}", canonical)
        .replace("{FOLLOWERS}", str(followers))
        .replace("{FOLLOWING}", str(following))
        .replace("{LIKES}", str(likes)))
        
    return Response(content=html_out, media_type="text/html; charset=utf-8")

@app.get("/hashtags/trending")
async def trending_hashtags(limit: int = 20):
    try:
        snap = db.collection("hashtags") \
            .orderBy("videoCount", direction=firestore.Query.DESCENDING) \
            .limit(int(limit)) \
            .stream()
        return {"items": [
            {
                "tag": d.get("tag"),
                "videoCount": d.get("videoCount", 0),
            } for d in snap
        ]}
    except Exception as e:
        log.warning(f"trending failed: {e}")
        return {"items": []}

@app.post("/dm/notify")
async def notify_dm(
    request: Request,
    authorization: str = Header(...),
):
    sender_uid = await verify_token(authorization)
    rate_limit(f"dm:{sender_uid}", max_calls=30, window_sec=60)
    data = await request.json()
    chat_id = (data.get("chatId") or "").strip()
    receiver_uid = (data.get("receiverUid") or "").strip()
    text = (data.get("text") or "").strip()[:100]

    if not chat_id or not receiver_uid:
        raise HTTPException(400, "Missing chatId/receiverUid")
    if sender_uid == receiver_uid:
        return {"ok": True, "skipped": "self"}

    # Get sender name
    try:
        sdoc = db.collection("users").document(sender_uid).get()
        sdata = sdoc.to_dict() or {}
        sender_name = sdata.get("username") or sdata.get("fullName") or "Someone"
    except Exception:
        sender_name = "Someone"

    # Target fcm token
    try:
        doc = db.collection("users").document(receiver_uid).get()
        target_token = (doc.to_dict() or {}).get("fcmToken") if doc.exists else None
    except Exception:
        target_token = None

    # Write notification doc (always)
    try:
        db.collection("users").document(receiver_uid) \
            .collection("notifications").document().set({
                "kind": "message",
                "title": f"@{sender_name}",
                "body": text,
                "videoId": "",
                "senderUid": sender_uid,
                "senderName": sender_name,
                "chatId": chat_id,
                "unread": True,
                "createdAt": firestore.SERVER_TIMESTAMP,
            })
    except Exception as e:
        log.warning(f"dm notif doc failed: {e}")

    # FCM push
    if target_token:
        try:
            messaging.send(messaging.Message(
                notification=messaging.Notification(
                    title=f"@{sender_name}",
                    body=text,
                ),
                data={"kind": "message", "chatId": chat_id, "senderUid": sender_uid},
                token=target_token,
            ))
        except Exception as e:
            log.warning(f"dm fcm failed: {e}")

    return {"ok": True}

