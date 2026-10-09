import os
import base64
from pathlib import Path

TG_API_ID = int(os.environ["TG_API_ID"])
TG_API_HASH = os.environ["TG_API_HASH"]
TG_SESSION = os.environ["TG_SESSION"]
TG_CHANNEL_ID = int(os.environ["TG_CHANNEL_ID"])

FIREBASE_JSON_B64 = os.environ.get("FIREBASE_JSON_B64")


def write_firebase_json() -> str:
    if FIREBASE_JSON_B64:
        p = Path("/tmp/firebase.json")
        p.write_bytes(base64.b64decode(FIREBASE_JSON_B64))
        return str(p)
    return os.environ.get("FIREBASE_JSON", "./firebase.json")


FIREBASE_JSON_PATH = write_firebase_json()
MAX_UPLOAD_BYTES = 2 * 1024 * 1024 * 1024
PORT = int(os.environ.get("PORT", 8080))
