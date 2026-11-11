"""Force-stop verification for a dedicated test AVD. Explicitly resets this app's test data."""
import argparse
import os
from pathlib import Path
import re
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument("--serial", required=True)
parser.add_argument("--reset-test-data", action="store_true", help="Required: clears only dev.leemh.dochagjari on the selected dedicated test device")
parser.add_argument("--output", default="app/build/reports/process-restart.txt")
args = parser.parse_args()
if not args.reset_test_data:
    parser.error("Use a dedicated test AVD and explicitly pass --reset-test-data")
sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
adb = str(Path(sdk) / "platform-tools/adb") if sdk else "adb"
package = "dev.leemh.dochagjari"
transcript = []


def run(*command, timeout=120):
    result = subprocess.run([adb, "-s", args.serial, *command], text=True, capture_output=True, timeout=timeout)
    transcript.append("adb " + " ".join(command) + "\n" + result.stdout + result.stderr)
    if result.returncode:
        raise RuntimeError("ADB command failed: " + " ".join(command))
    return result.stdout


try:
    assert run("shell", "getprop", "sys.boot_completed").strip() == "1", "Device must finish booting"
    run("shell", "pm", "clear", package)
    pids = []
    for phase, method in [("prepare", "prepareQueuedArrivalAndSavedProfile"), ("verify", "verifyRestoredArrivalAndProfile")]:
        output = run("shell", "am", "instrument", "-w", "-e", "restartPhase", phase, "-e", "class",
                     package + ".ProcessRestartInstrumentedTest#" + method,
                     package + ".test/androidx.test.runner.AndroidJUnitRunner", timeout=180)
        assert "OK (1 test)" in output, output
        pids.append(int(re.search(r"arrival_process_pid=(\d+)", output).group(1)))
        run("shell", "am", "force-stop", package)
    assert pids[0] != pids[1], "Verification must run in a new Android process"
    transcript.append(f"PASS: process restart {pids[0]} -> {pids[1]}; frozen payload and acknowledged profile restored; no token/provider call")
finally:
    target = Path(args.output)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text("\n".join(transcript) + "\n")
print(transcript[-1])
