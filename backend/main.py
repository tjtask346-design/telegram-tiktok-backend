import os
import logging
import tempfile
import aiofiles
from contextlib import asynccontextmanager

from fastapi import (
    FastAPI, UploadFile, File, Form, Header, HTTPException, Request,
)
from fastapi.responses import StreamingResponse
from fastapi.middleware.cors import CORSMiddleware
import firebase_admin
from firebase_admin import credentials, firestore

from config import FIREBASE_JSON_PATH, MAX_UPLOAD_BYTES, TG_CHANNEL_ID
from telegram_client import (
    get_client, upload_video, stream_video, get_video_info, delete_video,
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
        "likes": 0,
        "views": 0,
        "comments": 0,
        "createdAt": firestore.SERVER_TIMESTAMP,
    })

    return {"videoId": doc.id, "msgId": msg_id, "size": total}


@app.get("/stream/{msg_id}")
async def stream(msg_id: int, request: Request):
    info = await get_video_info(msg_id)
    if not info:
        raise HTTPException(404, "Video not found")

    size = info["size"]
    mime = info["mime"]
    range_header = request.headers.get("range")

    if range_header:
        try:
            rng = range_header.replace("bytes=", "").split("-")
            start = int(rng[0]) if rng[0] else 0
            end = int(rng[1]) if len(rng) > 1 and rng[1] else size - 1
        except Exception:
            raise HTTPException(416, "Bad range")
        if start >= size or end >= size or start > end:
            raise HTTPException(416, "Range out of bounds")
        length = end - start + 1

        async def gen():
            async for chunk in stream_video(msg_id, offset=start, limit=length):
                yield chunk

        return StreamingResponse(
            gen(), status_code=206, media_type=mime,
            headers={
                "Content-Range": "bytes %s-%s/%s" % (start, end, size),
                "Accept-Ranges": "bytes",
                "Content-Length": str(length),
                "Cache-Control": "public, max-age=86400",
            },
        )

    async def gen_full():
        async for chunk in stream_video(msg_id, offset=0):
            yield chunk

    return StreamingResponse(
        gen_full(), media_type=mime,
        headers={
            "Accept-Ranges": "bytes",
            "Content-Length": str(size),
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
