import concurrent.futures
import tempfile
import unittest
import uuid
from pathlib import Path
from urllib.error import HTTPError

from fastapi.testclient import TestClient
from server.app import Settings, create_app


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
        return client.post("/v1/arrivals", json=self.payload | changes, headers=self.headers)

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
        self.assertEqual(first, {"status": "mock", "duplicate": False})
        self.assertEqual(self.send(self.client()).json(), {"status": "mock", "duplicate": True})
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
        self.assertEqual(self.send(self.client()).json()["status"], "unknown")

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


if __name__ == "__main__":
    unittest.main()
