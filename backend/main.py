import re
import base64
import os
import os
import logging
import tempfile
import aiofiles
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
async def health():
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
    try:
        u_doc = db.collection("users").document(uid).get()
        if u_doc.exists:
            d = u_doc.to_dict() or {}
            uploader_handle = d.get("username", "") or ""
            uploader_name = d.get("fullName", "") or d.get("firstName", "user")
    except Exception as e:
        log.warning(f"profile lookup failed: {e}")

    doc = db.collection("videos").document()
    video_id = doc.id

    # ---- Extract hashtags ----
    tags = list(set(
        t.lower() for t in re.findall(r"#([A-Za-z0-9_]+)", caption or "")
    ))[:10]
    log.info(f"hashtags={tags}")

    doc.set({
        "uploader": uid,
        "uploaderName": uploader_name,
        "uploaderHandle": uploader_handle,
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
    info = await get_video_info(msg_id)
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
            result = subprocess.run(
                ["ffmpeg", "-y", "-i", tmp_raw,
                 "-c", "copy", "-movflags", "+faststart",
                 cache_path],
                capture_output=True, timeout=120
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
        with open(cache_path, "rb") as fh:
            data = fh.read()
        return Response(
            content=data,
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

    with open(cache_path, "rb") as fh:
        fh.seek(start)
        data = fh.read(length)

    return Response(
        content=data,
        status_code=206,
        media_type=mime,
        headers={
            "Content-Range": f"bytes {start}-{end}/{actual_size}",
            "Accept-Ranges": "bytes",
            "Content-Length": str(len(data)),
            "Cache-Control": "public, max-age=86400",
        },
    )

@app.post("/view/{video_id}")
async def increment_view(video_id: str, authorization: str = Header(...)):
    await verify_token(authorization)
    db.collection("videos").document(video_id).update({
        "views": firestore.Increment(1)
    })
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
        chunks = []
        async for chunk in stream_photo(msg_id):
            chunks.append(chunk)
        data = b"".join(chunks)
        return Response(
            content=data,
            media_type="image/jpeg",
            headers={"Cache-Control": "public, max-age=604800"},
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
    try:
        for v in db.collection("videos").stream():
            for c in v.reference.collection("comments").whereEqualTo("uid", uid).stream():
                try: c.reference.delete()
                except Exception: pass
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

    # 5. Delete user sub-collections
    try:
        for sub in ("bookmarks", "notifications", "drafts", "sessions", "settings", "chats"):
            for d in db.collection("users").document(uid).collection(sub).stream():
                try: d.reference.delete()
                except Exception: pass
    except Exception as e:
        log.warning(f"user sub cleanup failed: {e}")

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

