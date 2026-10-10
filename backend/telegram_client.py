import os
import asyncio
import logging
from telethon import TelegramClient
from telethon.sessions import StringSession
from telethon.tl.types import DocumentAttributeVideo
from telethon.errors import FloodWaitError

from config import TG_API_ID, TG_API_HASH, TG_SESSION, TG_CHANNEL_ID

log = logging.getLogger("tg")
log.setLevel(logging.INFO)

_client = None
_lock = asyncio.Lock()


async def get_client():
    global _client
    async with _lock:
        if _client is None or not _client.is_connected():
            log.info("Connecting Telegram client...")
            _client = TelegramClient(
                StringSession(TG_SESSION),
                TG_API_ID,
                TG_API_HASH,
                connection_retries=5,
                retry_delay=2,
                timeout=60,
            )
            await _client.connect()
            if not await _client.is_user_authorized():
                raise RuntimeError("Telegram session invalid")
            log.info("Telegram client connected OK")
        return _client


async def upload_video(file_path, caption, duration, width, height, progress_cb=None):
    client = await get_client()
    log.info("Uploading %s (%s bytes)" % (file_path, os.path.getsize(file_path)))
    try:
        msg = await client.send_file(
            TG_CHANNEL_ID,
            file_path,
            caption=caption[:1024],
            attributes=[DocumentAttributeVideo(
                duration=duration, w=width, h=height,
                supports_streaming=True,
            )],
            supports_streaming=True,
            part_size_kb=512,
            progress_callback=progress_cb,
        )
        log.info("Uploaded msg_id=%s" % msg.id)
        return msg.id
    except FloodWaitError as e:
        log.warning("FloodWait %ss" % e.seconds)
        raise


async def stream_video(msg_id, offset=0, limit=None):
    client = await get_client()
    msg = await client.get_messages(TG_CHANNEL_ID, ids=msg_id)
    if not msg or not msg.media:
        raise FileNotFoundError("msg %s not found" % msg_id)
    async for chunk in client.iter_download(
        msg.media, offset=offset, limit=limit, request_size=1024 * 512,
    ):
        yield chunk


async def get_video_info(msg_id):
    client = await get_client()
    msg = await client.get_messages(TG_CHANNEL_ID, ids=msg_id)
    if not msg or not msg.media or not msg.document:
        return None
    dur = 0
    for a in (msg.document.attributes or []):
        if isinstance(a, DocumentAttributeVideo):
            dur = a.duration
            break
    return {
        "size": msg.document.size,
        "mime": msg.document.mime_type or "video/mp4",
        "duration": dur,
    }


async def delete_video(msg_id):
    client = await get_client()
    await client.delete_messages(TG_CHANNEL_ID, [msg_id])

async def upload_photo(file_path: str, caption: str = "") -> int:
    """Upload an image to the channel. Returns msg_id."""
    client = await get_client()
    msg = await client.send_file(
        TG_CHANNEL_ID,
        file_path,
        caption=caption[:512],
        force_document=False,
    )
    return msg.id


async def stream_photo(msg_id: int):
    """Yield photo bytes."""
    client = await get_client()
    msg = await client.get_messages(TG_CHANNEL_ID, ids=msg_id)
    if not msg or not msg.photo:
        raise FileNotFoundError(f"photo {msg_id} not found")
    data = await client.download_media(msg, file=bytes)
    yield data


async def get_photo_info(msg_id: int):
    client = await get_client()
    msg = await client.get_messages(TG_CHANNEL_ID, ids=msg_id)
    if not msg or not msg.photo:
        return None
    return {"size": msg.photo.sizes[-1].size if msg.photo.sizes else 0}


async def delete_photo(msg_id: int):
    client = await get_client()
    await client.delete_messages(TG_CHANNEL_ID, [msg_id])

