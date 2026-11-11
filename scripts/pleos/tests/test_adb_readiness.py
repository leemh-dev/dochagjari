import importlib.util
from pathlib import Path
import subprocess
import unittest

spec = importlib.util.spec_from_file_location("adb_readiness", Path(__file__).parents[1] / "adb_readiness.py")
readiness = importlib.util.module_from_spec(spec)
spec.loader.exec_module(readiness)


class AdbReadinessTest(unittest.TestCase):
    def runner(self, overrides=None):
        answers = {"get-state": (0, "device\r\n", ""), "sys.boot_completed": (0, "1\n", ""),
                   "package": (0, "Service package: found\n", ""), "activity": (0, "Service activity: found\n", "")}
        answers.update(overrides or {})
        def run(command, **kwargs):
            answer = answers[command[-1]]
            if isinstance(answer, Exception):
                raise answer
            return subprocess.CompletedProcess(command, *answer)
        return run

    def test_all_checks_must_succeed_for_selected_serial(self):
        captured = []
        runner = self.runner()
        def run(command, **kwargs):
            captured.append(command)
            return runner(command, **kwargs)
        result = readiness.probe("adb", "emulator-5560", runner=run)
        self.assertTrue(result["ready"])
        self.assertEqual(4, len(captured))
        self.assertTrue(all(command[1:3] == ["-s", "emulator-5560"] for command in captured))

    def test_unauthorized_offline_missing_device_do_not_query_services(self):
        for rc, state, error in [(0, "unauthorized", ""), (0, "offline", ""), (1, "", "device not found")]:
            with self.subTest(state=state):
                result = readiness.probe("adb", "emulator-5554", runner=self.runner({"get-state": (rc, state, error)}))
                self.assertFalse(result["ready"])
                self.assertEqual(["transport"], list(result["checks"]))
                self.assertEqual(error, result["checks"]["transport"]["stderr"])

    def test_partial_boot_nonzero_exit_and_false_positive_text_fail(self):
        for name, answer in [("sys.boot_completed", (0, "10", "")),
                             ("package", (0, "Service package: not found", "")),
                             ("activity", (1, "Service activity: found", "service command failed")),
                             ("activity", (0, "Service activity: found but stale", ""))]:
            with self.subTest(name=name, answer=answer):
                self.assertFalse(readiness.probe("adb", "emulator-5554", runner=self.runner({name: answer}))["ready"])

    def test_timeout_and_missing_client_are_reported(self):
        for error in [subprocess.TimeoutExpired("adb", 8), FileNotFoundError("ADB executable unavailable")]:
            result = readiness.probe("adb", "emulator-5554", runner=self.runner({"get-state": error}))
            self.assertFalse(result["ready"])
            self.assertIsNone(result["checks"]["transport"]["returncode"])
            self.assertTrue(result["checks"]["transport"]["stderr"])

    def test_credentials_are_not_written_to_diagnostics(self):
        result = readiness.probe("adb", "emulator-5554", runner=self.runner({"get-state": (1, "", "unauthorized\nAuthorization: Bearer fixture-value")}))
        self.assertEqual("unauthorized\n[credential diagnostic omitted]", result["checks"]["transport"]["stderr"])


if __name__ == "__main__":
    unittest.main()
