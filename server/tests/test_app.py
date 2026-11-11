import concurrent.futures
import tempfile
import unittest
import uuid
import sqlite3
from pathlib import Path
from urllib.error import HTTPError

from fastapi.testclient import TestClient
from server.app import Arrival, Settings, arrival_digest, create_app


class ArrivalTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.calls = []
        self.settings = dict(token="unit-test-token", database=str(Path(self.directory.name) / "outbox.sqlite3"),
                             recipients={"demo": {"phone": "01000000000", "consented": True}},
                             service_id="test", access_key="test", secret_key="test", sender_phone="01000000000")
        self.payload = dict(session_id=str(uuid.uuid4()), recipient_id="demo", latitude=37.48, longitude=127.03,
                            clue="B1 C12", stopped=True, confirmed=True, input_source="TEST")
        self.headers = {"Authorization": "Bearer unit-test-token"}

    def tearDown(self):
        self.directory.cleanup()

    def client(self, **overrides):
        def sender(*args):
            self.calls.append(args)
            return "provider-123"
        return TestClient(create_app(Settings(**(self.settings | overrides)), sender))

    def send(self, client, **changes):
        mode = client.get("/health").json()["mode"]
        return client.post("/v1/arrivals", json=self.payload | {"delivery_mode": mode} | changes, headers=self.headers)

    def test_auth_and_consent(self):
        client = self.client()
        self.assertEqual(client.post("/v1/arrivals", json=self.payload).status_code, 401)
        self.assertEqual(self.send(client, recipient_id="unregistered").status_code, 403)

    def test_stop_and_location_are_required(self):
        client = self.client()
        self.assertEqual(self.send(client, stopped=False).status_code, 409)
        self.assertEqual(self.send(client, confirmed=False).status_code, 409)
        self.assertEqual(self.send(client, clue=" ").status_code, 409)
        self.assertEqual(self.send(client, latitude=999).status_code, 422)

    def test_mock_no_provider_and_restart_dedup(self):
        first = self.send(self.client()).json()
        self.assertEqual((first["status"], first["duplicate"]), ("mock", False))
        repeated = self.send(self.client()).json()
        self.assertEqual((repeated["status"], repeated["duplicate"]), ("mock", True))
        self.assertEqual(first["request_hash"], repeated["request_hash"])
        self.assertEqual(self.calls, [])
        self.assertEqual(self.send(self.client(), clue="new location").status_code, 409)

    def test_real_test_inputs_require_explicit_enable(self):
        client = self.client(mode="sens")
        self.assertEqual(self.send(client).status_code, 403)
        self.assertEqual(self.calls, [])

    def test_concurrent_duplicates_call_provider_once(self):
        client = self.client(mode="sens", allow_test_sms=True)
        with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
            responses = list(pool.map(lambda _: self.send(client), range(6)))
        self.assertTrue(all(r.status_code == 200 for r in responses))
        self.assertEqual(len(self.calls), 1)
        self.assertEqual(self.send(client).json()["status"], "accepted")

    def test_timeout_is_durable_unknown_without_resend(self):
        def timeout(*args):
            self.calls.append(1)
            raise TimeoutError()
        client = TestClient(create_app(Settings(**self.settings, mode="sens", allow_test_sms=True), timeout))
        self.assertEqual(self.send(client).json()["status"], "unknown")
        self.assertEqual(self.send(client).json()["status"], "unknown")
        self.assertEqual(len(self.calls), 1)
        self.assertEqual(self.send(self.client(mode="sens", allow_test_sms=True)).json()["status"], "unknown")

    def test_provider_reject_is_not_delivery(self):
        def reject(*args):
            raise HTTPError("https://example.invalid", 403, "rejected", {}, None)
        client = TestClient(create_app(Settings(**self.settings, mode="sens", allow_test_sms=True), reject))
        self.assertEqual(self.send(client).json()["status"], "failed")

    def test_daily_limit_and_status(self):
        client = self.client()
        for _ in range(5):
            self.assertEqual(self.send(client, session_id=str(uuid.uuid4())).status_code, 200)
        self.assertEqual(self.send(client).status_code, 429)
        self.assertEqual(client.get(f"/v1/arrivals/{self.payload['session_id']}/demo", headers=self.headers).status_code, 404)

    def test_lookup_after_response_loss_proves_same_payload_and_mode(self):
        client = self.client(mode="sens", allow_test_sms=True)
        self.send(client)  # Simulate a client that lost the accepted POST response.
        restarted = self.client(mode="sens", allow_test_sms=True)
        result = restarted.get(f"/v1/arrivals/{self.payload['session_id']}/demo", headers=self.headers).json()
        self.assertEqual(result["status"], "accepted")
        self.assertEqual(result["delivery_mode"], "sens")
        self.assertEqual(result["request_hash"], arrival_digest(Arrival(**self.payload, delivery_mode="sens")))
        self.assertEqual(self.send(restarted).json()["status"], "accepted")
        self.assertEqual(len(self.calls), 1)

    def test_session_cannot_be_rebound_to_another_registered_recipient(self):
        recipients = self.settings["recipients"] | {"other": {"phone": "01000000001", "consented": True}}
        client = self.client(mode="sens", allow_test_sms=True, recipients=recipients)
        self.assertEqual(self.send(client).status_code, 200)
        self.assertEqual(self.send(client, recipient_id="other").status_code, 409)
        self.assertEqual(len(self.calls), 1)

    def test_mode_change_never_promotes_mock_request_to_real_sms(self):
        self.assertEqual(self.send(self.client()).json()["status"], "mock")
        sens = self.client(mode="sens", allow_test_sms=True)
        self.assertEqual(self.send(sens, delivery_mode="mock").status_code, 409)
        self.assertEqual(self.send(sens).status_code, 409)
        self.assertEqual(self.calls, [])

    def test_crash_after_reservation_before_provider_is_not_retried(self):
        value = Arrival(**self.payload, delivery_mode="sens")
        client = self.client(mode="sens", allow_test_sms=True)
        with sqlite3.connect(self.settings["database"]) as connection:
            connection.execute("INSERT INTO arrivals (session_id,recipient_id,digest,status,created_at,input_source,delivery_mode) VALUES (?,?,?,'pending',0,?,'sens')", (str(value.session_id), value.recipient_id, arrival_digest(value), value.input_source))
        self.assertEqual(self.send(client).json()["status"], "pending")
        self.assertEqual(self.send(self.client(mode="sens", allow_test_sms=True)).json()["status"], "pending")
        self.assertEqual(self.calls, [])

    def test_legacy_database_migration_keeps_row_without_mode_proof(self):
        path = Path(self.directory.name) / "legacy.sqlite3"
        with sqlite3.connect(path) as connection:
            connection.execute("CREATE TABLE arrivals (session_id TEXT, recipient_id TEXT, digest TEXT, status TEXT, created_at REAL, provider_id TEXT, input_source TEXT, PRIMARY KEY(session_id,recipient_id))")
            connection.execute("INSERT INTO arrivals VALUES (?, 'demo', 'old-hash', 'pending', 0, NULL, 'TEST')", (self.payload["session_id"],))
        client = self.client(database=str(path))
        result = client.get(f"/v1/arrivals/{self.payload['session_id']}/demo", headers=self.headers).json()
        self.assertEqual(result["status"], "pending")
        self.assertIsNone(result["delivery_mode"])
        self.assertEqual(result["request_hash"], "old-hash")
        self.assertEqual(self.send(client).status_code, 409)

    def test_shared_kotlin_fingerprint_vector(self):
        value = Arrival(session_id="11111111-1111-4111-8111-111111111111", recipient_id="demo", latitude=37.48,
                        longitude=127.03, clue="B1 C12 · 서쪽 문", stopped=True, confirmed=True, input_source="TEST", delivery_mode="sens")
        self.assertEqual(arrival_digest(value), "519567516c037555e7ec9535c3b6c0ddb7f93e66e8dfd664acb49d2157fd786d")


if __name__ == "__main__":
    unittest.main()
