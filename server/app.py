"""Authenticated, allow-listed arrival SMS. Defaults to a durable mock outbox."""
from __future__ import annotations

import base64
import hashlib
import hmac
import json
import os
import sqlite3
import time
import uuid
from dataclasses import dataclass, field
from contextlib import contextmanager
from pathlib import Path
from typing import Callable, Literal
from urllib import error, request

from fastapi import Depends, FastAPI, Header, HTTPException
from pydantic import BaseModel, ConfigDict, Field


@dataclass(frozen=True)
class Settings:
    token: str = ""
    database: str = "outbox.sqlite3"
    mode: str = "mock"
    # {"demo": {"phone": "...", "consented": true}}. Never log this mapping.
    recipients: dict = field(default_factory=dict)
    service_id: str = ""
    access_key: str = ""
    secret_key: str = ""
    sender_phone: str = ""
    allow_test_sms: bool = False

    @classmethod
    def from_env(cls):
        return cls(
            token=os.getenv("ARRIVAL_API_TOKEN", ""), database=os.getenv("ARRIVAL_DB", "outbox.sqlite3"),
            mode=os.getenv("SMS_MODE", "mock"), recipients=json.loads(os.getenv("SMS_RECIPIENTS_JSON", "{}")),
            service_id=os.getenv("SENS_SERVICE_ID", ""), access_key=os.getenv("SENS_ACCESS_KEY", ""),
            secret_key=os.getenv("SENS_SECRET_KEY", ""), sender_phone=os.getenv("SENS_SENDER_PHONE", ""),
            allow_test_sms=os.getenv("ALLOW_TEST_INPUT_SMS", "false").lower() == "true",
        )


class Arrival(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)
    session_id: uuid.UUID
    recipient_id: str = Field(min_length=1, max_length=64, pattern=r"^[a-zA-Z0-9_-]+$")
    latitude: float = Field(ge=-90, le=90)
    longitude: float = Field(ge=-180, le=180)
    clue: str = Field(min_length=1, max_length=180)
    stopped: bool
    confirmed: bool
    input_source: Literal["VEHICLE", "TEST"]


def sens_send(settings: Settings, phone: str, content: str) -> str:
    """One outbound attempt. A timeout is ambiguous and must not trigger a retry."""
    from urllib.parse import quote

    path = f"/sms/v2/services/{quote(settings.service_id, safe=':')}/messages"
    timestamp = str(int(time.time() * 1000))
    signature = base64.b64encode(hmac.new(
        settings.secret_key.encode(), f"POST {path}\n{timestamp}\n{settings.access_key}".encode(), hashlib.sha256
    ).digest()).decode()
    payload = {"type": "LMS", "contentType": "COMM", "countryCode": "82",
               "from": settings.sender_phone, "subject": "도착자리 도착 알림", "content": content,
               "messages": [{"to": phone}]}
    req = request.Request("https://sens.apigw.ntruss.com" + path,
                          data=json.dumps(payload, ensure_ascii=False).encode(), method="POST",
                          headers={"Content-Type": "application/json; charset=utf-8",
                                   "x-ncp-apigw-timestamp": timestamp, "x-ncp-iam-access-key": settings.access_key,
                                   "x-ncp-apigw-signature-v2": signature})
    # urllib follows redirects by default; never forward credentials to another host.
    class NoRedirect(request.HTTPRedirectHandler):
        def redirect_request(self, req, fp, code, msg, headers, newurl):
            return None
    with request.build_opener(NoRedirect()).open(req, timeout=15) as response:
        result = json.load(response)
    if str(result.get("statusCode")) != "202" or not result.get("requestId"):
        raise ValueError("Provider did not confirm acceptance")
    return result["requestId"]


def create_app(settings: Settings, sender: Callable = sens_send) -> FastAPI:
    if settings.mode not in {"mock", "sens"}:
        raise ValueError("SMS_MODE must be mock or sens")
    if settings.mode == "sens" and not all((settings.token, settings.service_id, settings.access_key, settings.secret_key, settings.sender_phone)):
        raise ValueError("SENS mode requires server credentials, registered sender and API token")
    Path(settings.database).parent.mkdir(parents=True, exist_ok=True)

    @contextmanager
    def db():
        connection = sqlite3.connect(settings.database, timeout=10)
        connection.row_factory = sqlite3.Row
        try:
            with connection:
                yield connection
        finally:
            connection.close()

    with db() as connection:
        connection.execute("""CREATE TABLE IF NOT EXISTS arrivals (
            session_id TEXT NOT NULL, recipient_id TEXT NOT NULL, digest TEXT NOT NULL,
            status TEXT NOT NULL, created_at REAL NOT NULL, provider_id TEXT,
            input_source TEXT NOT NULL, PRIMARY KEY(session_id, recipient_id))""")
    application = FastAPI(title="도착자리 arrival outbox", docs_url=None, redoc_url=None)

    def authorized(authorization: str = Header(default="")):
        expected = "Bearer " + settings.token
        if not settings.token or not hmac.compare_digest(authorization.encode(), expected.encode()):
            raise HTTPException(401, "Authentication required")

    @application.get("/health")
    def health():
        return {"status": "ok", "mode": settings.mode}

    @application.get("/v1/arrivals/{session_id}/{recipient_id}", dependencies=[Depends(authorized)])
    def status(session_id: uuid.UUID, recipient_id: str):
        with db() as connection:
            row = connection.execute("SELECT status, provider_id, input_source FROM arrivals WHERE session_id=? AND recipient_id=?", (str(session_id), recipient_id)).fetchone()
        if row is None:
            raise HTTPException(404, "Session not found")
        return dict(row)

    @application.post("/v1/arrivals", dependencies=[Depends(authorized)])
    def arrival(value: Arrival):
        recipient = settings.recipients.get(value.recipient_id, {})
        if recipient.get("consented") is not True or not str(recipient.get("phone", "")).isdigit():
            raise HTTPException(403, "Recipient is not pre-registered with consent")
        if not value.stopped or not value.confirmed or not value.clue.strip():
            raise HTTPException(409, "Stopped state and confirmed meeting location required")
        if settings.mode == "sens" and value.input_source == "TEST" and not settings.allow_test_sms:
            raise HTTPException(403, "Real SMS for test inputs is disabled")
        digest = hashlib.sha256(value.model_dump_json().encode()).hexdigest()
        key = (str(value.session_id), value.recipient_id)
        with db() as connection:
            connection.execute("BEGIN IMMEDIATE")
            existing = connection.execute("SELECT status, digest FROM arrivals WHERE session_id=? AND recipient_id=?", key).fetchone()
            if existing:
                if existing["digest"] != digest:
                    raise HTTPException(409, "Session payload changed; inspect original record")
                return {"status": existing["status"], "duplicate": True}
            recent = connection.execute("SELECT count(*) FROM arrivals WHERE recipient_id=? AND created_at>?", (value.recipient_id, time.time() - 86400)).fetchone()[0]
            if recent >= 5:
                raise HTTPException(429, "Daily test recipient limit reached")
            connection.execute("INSERT INTO arrivals VALUES (?, ?, ?, 'pending', ?, NULL, ?)", (*key, digest, time.time(), value.input_source))
            connection.commit()  # Reserve before any network I/O; pending survives process death.
        provider_id = None
        if settings.mode == "mock":
            result = "mock"
        else:
            content = f"{value.clue}\nhttps://maps.google.com/?q={value.latitude},{value.longitude}"
            try:
                provider_id = sender(settings, str(recipient["phone"]), content)
                result = "accepted"  # Never equate provider acceptance with handset delivery.
            except error.HTTPError as exc:
                result = "failed" if 400 <= exc.code < 500 else "unknown"
            except Exception:
                result = "unknown"
        with db() as connection:
            connection.execute("UPDATE arrivals SET status=?, provider_id=? WHERE session_id=? AND recipient_id=?", (result, provider_id, *key))
        return {"status": result, "duplicate": False}

    return application


app = create_app(Settings.from_env())
