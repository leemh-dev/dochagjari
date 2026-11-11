"""Operate only the test app via visible accessibility nodes; collect genuine SDK evidence."""
from pathlib import Path
import json
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

from adb_readiness import diagnostic, probe

adb = str(Path(os.environ["ANDROID_HOME"]) / "platform-tools/adb")
reports = Path("app/build/reports/pleos")
reports.mkdir(parents=True, exist_ok=True)
package = "dev.leemh.dochagjari"
path = reports / "app-probe.json"
report = json.loads(path.read_text()) if path.exists() else {"navigation": [], "pin_verified": False, "note": "SDK results and screenshots require separate pin review; not real-car validation"}


def command(*args, timeout=45):
    result = subprocess.run([adb, "-s", "emulator-5554", *args], capture_output=True, timeout=timeout)
    if result.returncode:
        raise RuntimeError(result.stderr.decode(errors="replace")[:300])
    return result.stdout


def nodes():
    command("shell", "uiautomator", "dump", "/sdcard/dochagjari-probe.xml")
    return list(ET.fromstring(command("exec-out", "cat", "/sdcard/dochagjari-probe.xml")).iter("node"))


def bounds(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.attrib["bounds"]))
    return (x1 + x2) // 2, (y1 + y2) // 2


def tap_text(text):
    candidate = next((n for n in nodes() if text in [n.attrib.get("text"), n.attrib.get("content-desc")]), None)
    if candidate is None:
        raise RuntimeError("Visible app control missing: " + text)
    x, y = bounds(candidate)
    command("shell", "input", "tap", str(x), str(y)); time.sleep(1)


def switch_node(label):
    tree = nodes()
    name = next((n for n in tree if n.attrib.get("text") == label), None)
    if name is None:
        raise RuntimeError("Test-source control missing: " + label)
    _, y = bounds(name)
    switches = [n for n in tree if "Switch" in n.attrib.get("class", "") and abs(bounds(n)[1] - y) < 60]
    if len(switches) != 1:
        raise RuntimeError("Cannot safely identify source switch: " + label)
    return switches[0]


def set_switch(label, enabled):
    switch = switch_node(label)
    if switch.attrib.get("checked") != str(enabled).lower():
        if switch.attrib.get("enabled") != "true":
            raise RuntimeError("App control is disabled: " + label)
        x, y = bounds(switch)
        command("shell", "input", "tap", str(x), str(y)); time.sleep(2)
    if switch_node(label).attrib.get("checked") != str(enabled).lower():
        raise RuntimeError("Switch state was not acknowledged: " + label)


def disable_switch(label):
    set_switch(label, False)


def start_app():
    command("shell", "am", "start", "-W", "-n", package + "/.MainActivity", timeout=90)
    time.sleep(3)


def screenshot(name):
    (reports / name).write_bytes(command("exec-out", "screencap", "-p"))


try:
    readiness = json.loads((reports / "readiness.json").read_text())
    if not readiness.get("boot_completed"):
        report["skipped"] = "Pleos boot/services not ready; no installation, navigation, Console or app reception claimed"
        path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
        sys.exit(0 if len(sys.argv) > 1 and sys.argv[1] == "collect" else 1)
    report["live_adb_probe"] = probe(adb, "emulator-5554")
    if not report["live_adb_probe"]["ready"]:
        report["skipped"] = "Live ADB readiness no longer matches prior boot record"
        sys.exit(0 if len(sys.argv) > 1 and sys.argv[1] == "collect" else 1)
    if len(sys.argv) > 1 and sys.argv[1] == "collect":
        logs = command("logcat", "-d", "-v", "brief", "DochagjariPleosSeat:I", "DochagjariVehicle:I", "*:S").decode(errors="replace")
        (reports / "seat-sdk-events.txt").write_text(logs)
        real = [line for line in logs.splitlines() if "DochagjariPleosSeat" in line and "source=VEHICLE" in line]
        report["left_sdk_changed"] = any("left=EMPTY" in line for line in real) and any("left=OCCUPIED" in line for line in real)
        report["right_sdk_changed"] = any("right=EMPTY" in line for line in real) and any("right=OCCUPIED" in line for line in real)
        screenshot("seat-app.png")
    else:
        command("install", "-r", "app/build/outputs/apk/debug/app-debug.apk", timeout=180)
        report["apk_installed"] = True
        grant = subprocess.run([adb, "-s", "emulator-5554", "shell", "pm", "grant", package, "android.car.permission.CAR_SPEED"], capture_output=True)
        report["car_speed_permission_granted"] = grant.returncode == 0
        report["car_speed_permission_result"] = {"returncode": grant.returncode,
                                                 "stderr": diagnostic(grant.stderr.decode(errors="replace"))}
        command("emu", "geo", "fix", "127.034", "37.484")
        for mode, position in [("바로 태우기", [37.48544722, 127.03636666]), ("주차하고 데리러 가기", [37.48615, 127.03715])]:
            attempt = {"mode": mode, "requested_coordinate": position, "pin_verified": False}
            try:
                start_app(); tap_text("연동·테스트"); disable_switch("내비 모의 요청")
                tap_text("도착 안내"); tap_text(mode)
                command("logcat", "-c"); tap_text("내비로 안내"); time.sleep(20)
                logs = command("logcat", "-d", "-v", "brief", "Dochagjari:I", "*:S").decode(errors="replace")
                attempt["sdk_events"] = logs
                attempt["native_request_logged"] = "실제 SDK 요청:" in logs
                attempt["result_callback_logged"] = "실제 SDK 목적지:" in logs or "실제 SDK 경로 계산 완료" in logs
                screenshot("navigation-" + str(len(report["navigation"])) + ".png")
            except Exception as exc:
                attempt["error"] = str(exc)
            report["navigation"].append(attempt)
        start_app(); tap_text("좌석·조건")
        saved_options = ["왼쪽 카시트 문 여유", "오른쪽 카시트 문 여유", "유모차 · 무단차 필요"]
        for option in saved_options:
            set_switch(option, True)
        deadline = time.monotonic() + 15
        while time.monotonic() < deadline:
            if any(n.attrib.get("text") == "설정 저장 완료" for n in nodes()):
                break
            time.sleep(1)
        else:
            raise RuntimeError("DataStore save acknowledgement missing")
        screenshot("profile-saved.png")
        # This fresh dedicated AVD contains only this probe's test settings.
        before_pid = command("shell", "pidof", package).decode().strip()
        command("shell", "am", "force-stop", package)
        start_app(); tap_text("좌석·조건")
        after_pid = command("shell", "pidof", package).decode().strip()
        restored = {option: switch_node(option).attrib.get("checked") == "true" for option in saved_options}
        acknowledged = any(n.attrib.get("text") == "설정 저장 완료" for n in nodes())
        report["profile_restart"] = {"options": restored, "save_acknowledged": acknowledged,
                                     "before_pid": before_pid, "after_pid": after_pid}
        if not all(restored.values()) or not acknowledged or not before_pid or not after_pid or before_pid == after_pid:
            raise RuntimeError("Saved options were not restored after process restart")
        screenshot("profile-restored.png")
        tap_text("연동·테스트"); disable_switch("차량 신호 테스트 입력")
        tap_text("좌석·조건"); command("forward", "tcp:8088", "tcp:8088")
        initial = command("logcat", "-d", "-v", "brief", "DochagjariPleosSeat:I", "DochagjariVehicle:I", "*:S").decode(errors="replace")
        (reports / "seat-initial-sdk-events.txt").write_text(initial)
        report["initial_vehicle_source_logged"] = any("source=VEHICLE" in line for line in initial.splitlines())
        command("logcat", "-c")
        report["ready_for_console_probe"] = True
        screenshot("seat-before-console.png")
except Exception as exc:
    report["probe_error"] = str(exc)
    raise
finally:
    path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
