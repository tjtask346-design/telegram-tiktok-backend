# Telegram TikTok Backend

FastAPI + Telethon + Firebase. Telegram private channel = video storage.

## Endpoints
- `GET  /`                → health
- `GET  /health`          → Render health check
- `POST /upload`          → upload video (Bearer Firebase token)
- `GET  /stream/{msgId}`  → video stream (Range supported)
- `POST /view/{videoId}`  → increment view
- `DELETE /video/{videoId}` → delete own video

## Env vars (Render)
TG_API_ID, TG_API_HASH, TG_SESSION, TG_CHANNEL_ID, FIREBASE_JSON_B64
