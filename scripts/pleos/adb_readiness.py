"""Read-only ADB readiness checks; a boot log or an old JSON file is not readiness."""
import argparse
import json
import os
from pathlib import Path
import subprocess


def diagnostic(value):
    sensitive = ("key", "token", "jwt", "secret", "password", "authorization", "bearer", "cookie")
    return "\n".join("[credential diagnostic omitted]" if any(term in line.lower() for term in sensitive)
                     else line for line in value.splitlines())[:1500]


def probe(adb, serial, runner=subprocess.run, timeout=8):
    result = {"serial": serial, "ready": False, "checks": {}}

    def check(name, arguments):
        try:
            completed = runner([str(adb), "-s", serial, *arguments], capture_output=True, text=True,
                               encoding="utf-8", errors="replace", timeout=timeout)
            record = {"returncode": completed.returncode, "stdout": diagnostic(completed.stdout.strip()),
                      "stderr": diagnostic(completed.stderr.strip())}
        except subprocess.TimeoutExpired:
            record = {"returncode": None, "stdout": "", "stderr": "ADB command timed out"}
        except OSError as error:
            record = {"returncode": None, "stdout": "", "stderr": diagnostic(str(error))}
        result["checks"][name] = record
        return record

    state = check("transport", ["get-state"])
    if state["returncode"] != 0 or state["stdout"] != "device":
        result["reason"] = "ADB transport is not an authorized online device"
        return result
    expected = {"boot": "1", "package": "Service package: found", "activity": "Service activity: found"}
    for name, arguments in [("boot", ["shell", "getprop", "sys.boot_completed"]),
                            ("package", ["shell", "service", "check", "package"]),
                            ("activity", ["shell", "service", "check", "activity"])]:
        check(name, arguments)
    result["ready"] = all(result["checks"][name]["returncode"] == 0 and
                          result["checks"][name]["stdout"] == value for name, value in expected.items())
    result["reason"] = "Boot and package/activity services verified" if result["ready"] else "Boot or package/activity service is not ready"
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--adb")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    adb = args.adb or (str(Path(sdk) / "platform-tools/adb") if sdk else "adb")
    result = probe(adb, args.serial)
    encoded = json.dumps(result, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(encoded)
    print(encoded, end="")
    raise SystemExit(0 if result["ready"] else 1)
