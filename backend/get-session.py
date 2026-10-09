"""Termux: python get-session.py"""
from telethon.sync import TelegramClient
from telethon.sessions import StringSession

API_ID = int(input("API ID: "))
API_HASH = input("API HASH: ")

with TelegramClient(StringSession(), API_ID, API_HASH) as c:
    print("\n" + "=" * 60)
    print("SESSION STRING (copy this):")
    print(c.session.save())
    print("=" * 60 + "\n")
