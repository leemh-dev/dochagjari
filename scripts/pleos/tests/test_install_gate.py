import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


class InstallGateTest(unittest.TestCase):
    def exercise(self, boot_record):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            reports = root / "app/build/reports/pleos"
            reports.mkdir(parents=True)
            (reports / "readiness.json").write_text(json.dumps({"boot_completed": boot_record}))
            sdk = root / "sdk"
            adb = sdk / "platform-tools/adb"
            adb.parent.mkdir(parents=True)
            adb.write_text("""#!/usr/bin/env python3
import os, pathlib, sys
with pathlib.Path(os.environ['ADB_GATE_TRACE']).open('a') as output:
    output.write(' '.join(sys.argv[1:]) + '\\n')
print("error: device 'emulator-5554' not found", file=sys.stderr)
raise SystemExit(1)
""")
            adb.chmod(0o700)
            trace = root / "commands.txt"
            script = Path(__file__).parents[1] / "ui_probe.py"
            result = subprocess.run([sys.executable, str(script.resolve())], cwd=root, capture_output=True,
                                    text=True, env=os.environ | {"ANDROID_HOME": str(sdk), "ADB_GATE_TRACE": str(trace)})
            self.assertEqual(1, result.returncode, result.stdout + result.stderr)
            commands = trace.read_text() if trace.exists() else ""
            self.assertNotIn("install", commands)
            self.assertNotIn("am start", commands)
            report = json.loads((reports / "app-probe.json").read_text())
            self.assertFalse(report.get("apk_installed", False))
            return commands, report

    def test_prior_success_record_does_not_allow_install_on_missing_device(self):
        commands, report = self.exercise(True)
        self.assertEqual("-s emulator-5554 get-state\n", commands)
        self.assertFalse(report["live_adb_probe"]["ready"])
        self.assertIn("no longer matches", report["skipped"])

    def test_failed_boot_record_does_not_attempt_adb_or_install(self):
        commands, report = self.exercise(False)
        self.assertEqual("", commands)
        self.assertIn("not ready", report["skipped"])


if __name__ == "__main__":
    unittest.main()
