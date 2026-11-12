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
from ui_nodes import labeled_toggle
from storage_probe import collect as collect_storage
from display_probe import collect as collect_displays
from host_touch import touch as host_touch

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
    # Every dump has a new diagnostic filename: an ERROR with rc0 cannot reuse old XML.
    remote="/sdcard/dochagjari-ui-"+format(time.monotonic_ns(),"x")+".xml"
    try:
        result=subprocess.run([adb,"-s","emulator-5554","shell","uiautomator","dump",remote],
                              capture_output=True,text=True,timeout=45)
    except subprocess.TimeoutExpired as error:
        def partial(value):
            return diagnostic(value.decode(errors="replace") if isinstance(value,bytes) else value or "")
        (reports/"ui-dump-timeout.json").write_text(json.dumps({"remote_file":remote,"timeout_seconds":error.timeout,
            "stdout":partial(error.stdout),"stderr":partial(error.stderr)},indent=2)+"\n")
        raise
    (reports/"ui-dump-result.json").write_text(json.dumps({"remote_file":remote,"returncode":result.returncode,
        "stdout":diagnostic(result.stdout),"stderr":diagnostic(result.stderr)},indent=2)+"\n")
    if result.returncode or re.search(r'ERROR:',result.stdout+result.stderr,re.I):
        raise RuntimeError("Fresh UI dump failed; no previous XML consumed")
    xml=command("exec-out","cat",remote)
    (reports/"last-ui.xml").write_bytes(xml)
    return [n for n in ET.fromstring(xml).iter("node") if n.attrib.get("package")==package]


def current_user():
    user=command("shell","am","get-current-user").decode().strip()
    if not user.isdigit():raise RuntimeError("Current foreground Android user is unknown")
    if 'android_user' in report and report['android_user']!=int(user):
        raise RuntimeError("Foreground user changed; no user switching or control attempted")
    report['android_user']=int(user)
    return user


def tap(x,y):
    if os.getenv('DOCHAGJARI_PLEOS_HOST_TOUCH')=='1':
        actions=report.setdefault('host_touch_actions',[])
        result=host_touch(adb,'emulator-5554',(x,y),reports,len(actions));actions.append(result)
    else:
        command("shell","input","touchscreen","swipe",str(x),str(y),str(x),str(y),"200")


def bounds(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.attrib["bounds"]))
    return (x1 + x2) // 2, (y1 + y2) // 2


def tap_text(text):
    observed=nodes()
    matches=[n for n in observed if text in [n.attrib.get("text"),n.attrib.get("content-desc")]]
    if len(matches)!=1:raise RuntimeError("Visible app control missing or ambiguous: "+text)
    x,y=bounds(matches[0]);tap(x,y);time.sleep(1)
    expected={"연동·테스트":"내비 모의 요청","좌석·조건":"왼쪽 카시트 문 여유","도착 안내":"내비로 안내"}.get(text)
    if expected:
        deadline=time.monotonic()+12
        while True:
            if any(expected in [n.attrib.get('text'),n.attrib.get('content-desc')] for n in nodes()):
                report.setdefault('acknowledged_tabs',[]).append(text);screenshot('tab-'+str(len(report['acknowledged_tabs']))+'.png');return
            if time.monotonic()>=deadline:
                screenshot('tab-not-acknowledged.png')
                raise RuntimeError("Input sent but requested app content was not acknowledged: "+text)
            time.sleep(1)


def switch_node(label):
    return labeled_toggle(nodes(), label)


def set_switch(label, enabled):
    switch = switch_node(label)
    if switch.attrib.get("checked") != str(enabled).lower():
        if switch.attrib.get("enabled") != "true":
            raise RuntimeError("App control is disabled: " + label)
        x, y = bounds(switch)
        tap(x,y); time.sleep(2)
    if switch_node(label).attrib.get("checked") != str(enabled).lower():
        raise RuntimeError("Switch state was not acknowledged: " + label)


def disable_switch(label):
    set_switch(label, False)


def start_app():
    launches=report.setdefault("launches",[])
    output=command("shell", "am", "start", "--user",current_user(),"-W", "-n", package + "/.MainActivity", timeout=90)
    time.sleep(3)
    filename="app-launch-"+str(len(launches))+".png"
    launch={"am_start_stdout":diagnostic(output.decode(errors="replace")),"screenshot":None,
            "note":"Launch command/capture alone does not prove correct display or app UI"}
    launches.append(launch)
    path.write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n")
    screenshot(filename);launch["screenshot"]=filename
    path.write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n")
    if len(launches)==1:collect_displays(adb,"emulator-5554",reports)
    if not any(n.attrib.get('text')=='도착자리' for n in nodes()):
        raise RuntimeError('Visible own app hierarchy not confirmed after launch; no controls issued')
    screenshot('app-ready-'+str(len(launches)-1)+'.png')


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
        user=current_user()
        collect_storage(adb,"emulator-5554","app/build/outputs/apk/debug/app-debug.apk",reports)
        report["pre_install_adb_probe"]=probe(adb,"emulator-5554")
        if not report["pre_install_adb_probe"]["ready"]:
            raise RuntimeError("Live readiness changed after read-only storage observations; no installation")
        command("install", "--user",user,"-r", "app/build/outputs/apk/debug/app-debug.apk", timeout=180)
        report["apk_installed"] = True
        if output := os.environ.get("GITHUB_OUTPUT"):
            with Path(output).open("a") as stream:
                stream.write("apk_installed=true\n")
        grant = subprocess.run([adb, "-s", "emulator-5554", "shell", "pm", "grant", "--user",user,package, "android.car.permission.CAR_SPEED"], capture_output=True, timeout=20)
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
    if report.get("apk_installed") and not (reports/"display-activity.json").exists():
        collect_displays(adb,"emulator-5554",reports)
    raise
finally:
    path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
