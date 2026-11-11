"""One focused Pleos boot on a KVM runner; never downloads images without acceleration.

SDK images remain unchanged. A separately formatted test userdata avoids the earlier
/data/misc initialization conflict. Only sanitized readiness evidence is uploaded.
"""
from pathlib import Path
import hashlib
import concurrent.futures
import json
import os
import subprocess
import time
import threading
import signal
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

from adb_readiness import diagnostic, probe
from debugging_dialog import approve_visible_dialog

sdk = Path(os.environ["ANDROID_HOME"])
work = Path("app/build/pleos-work").resolve()
reports = Path("app/build/reports/pleos").resolve()
reports.mkdir(parents=True, exist_ok=True)
work.mkdir(parents=True, exist_ok=True)
adb = str(sdk / "platform-tools/adb")
report = {"serial": "emulator-5554", "environment": "GitHub KVM / official Pleos 2.1.0 API34 x86_64 / separate test userdata", "boot_completed": False}


def checkpoint(stage):
    report["stage"] = stage
    (reports / "readiness.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print("Pleos evidence stage: " + stage, flush=True)


def command(*args, timeout=20):
    return subprocess.run(args, capture_output=True, text=True, timeout=timeout)


try:
    if not os.access("/dev/kvm", os.R_OK | os.W_OK):
        raise RuntimeError("KVM unavailable; image download deliberately skipped")
    acceleration = command(str(sdk / "emulator/emulator"), "-accel-check")
    report["acceleration"] = acceleration.stdout.strip()
    if acceleration.returncode != 0:
        raise RuntimeError("Acceleration probe failed; image download skipped")
    checkpoint("verified-kvm")
    manifest_url = "https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml"
    with urllib.request.urlopen(manifest_url, timeout=60) as response:
        root = ET.fromstring(response.read())
    package = next(p for p in root if p.tag.endswith("remotePackage") and p.attrib.get("path", "").endswith("x86_64"))
    archive = next(p for p in package.iter() if p.tag.split("}")[-1] == "complete")
    values = {p.tag.split("}")[-1]: p.text.strip() for p in archive if p.text and p.text.strip()}
    checksum_element = next((p for p in archive if p.tag.split("}")[-1] == "checksum"), None)
    url = values["url"]
    if not url.startswith("https://nexus-playground.pleos.ai/"):
        raise RuntimeError("Unexpected official archive host")
    target = work / "pleos.zip"
    size = int(values["size"])
    report["archive_bytes"] = size
    checkpoint("downloading-image")
    # Previous 512MB requests completed 7/8 ranges, then one connection reset before boot.
    # Smaller independently retried ranges have explicit connection/transfer/overall limits.
    with target.open("wb") as output: output.truncate(size)
    step = 64 * 1024 * 1024
    ranges = [(index, start, min(size - 1, start + step - 1)) for index, start in enumerate(range(0, size, step))]
    transfer_deadline = time.monotonic() + 600
    aborted = threading.Event()
    def fetch_part(spec):
        index, start, end = spec
        part = work / f"range-{index}.part"
        headers = work / f"range-{index}.headers"
        try:
            for attempt in range(3):
                if aborted.is_set() or time.monotonic() >= transfer_deadline:
                    raise RuntimeError("Image transfer deadline or another range failed")
                seconds = max(1, min(60, int(transfer_deadline - time.monotonic())))
                result = subprocess.run(["curl", "-fsS", "--connect-timeout", "10", "--max-time", str(seconds),
                                         "--range", f"{start}-{end}", "--dump-header", str(headers), "--output", str(part),
                                         "--write-out", "%{http_code}", url], capture_output=True, text=True, timeout=seconds + 5)
                content_range = f"content-range: bytes {start}-{end}/{size}"
                valid = result.returncode == 0 and result.stdout.strip() == "206" and part.exists() and part.stat().st_size == end - start + 1
                valid = valid and content_range in headers.read_text(errors="replace").lower().splitlines()
                if valid:
                    with target.open("r+b") as output, part.open("rb") as source:
                        output.seek(start)
                        while block := source.read(4 * 1024 * 1024): output.write(block)
                    return index
                print(f"Pleos range {index + 1}/{len(ranges)} attempt {attempt + 1} failed: curl={result.returncode} HTTP={result.stdout.strip()}", flush=True)
            aborted.set()
            raise RuntimeError(f"Official image range {index + 1} failed after three bounded attempts")
        finally:
            part.unlink(missing_ok=True); headers.unlink(missing_ok=True)
    report["range_count"] = len(ranges)
    report["ranges_completed"] = 0
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        futures = [pool.submit(fetch_part, spec) for spec in ranges]
        try:
            for future in concurrent.futures.as_completed(futures):
                future.result()
                report["ranges_completed"] += 1
                checkpoint("downloading-image")
                print(f"Pleos archive {report['ranges_completed']}/{len(ranges)} ranges complete", flush=True)
        except Exception:
            aborted.set()
            for future in futures: future.cancel()
            raise
    digest = hashlib.new(checksum_element.attrib.get("type", "sha1") if checksum_element is not None else "sha1")
    with target.open("rb") as downloaded:
        while block := downloaded.read(8 * 1024 * 1024): digest.update(block)
    if target.stat().st_size != size:
        raise RuntimeError("Incomplete SDK image download")
    if checksum_element is not None and digest.hexdigest().lower() != checksum_element.text.strip().lower():
        raise RuntimeError("SDK image checksum mismatch")
    checkpoint("extracting-verified-image")
    image = work / "system-image"
    image.mkdir(exist_ok=True)
    with zipfile.ZipFile(target) as bundle:
        for entry in bundle.infolist():
            parts = Path(entry.filename).parts[1:]
            if not parts:
                continue
            destination = image.joinpath(*parts)
            if image not in destination.resolve().parents:
                raise RuntimeError("Unexpected image archive path")
            if entry.is_dir():
                destination.mkdir(parents=True, exist_ok=True); continue
            destination.parent.mkdir(parents=True, exist_ok=True)
            # Preserve zero runs as sparse holes; the official system image is ~18GB logical.
            with bundle.open(entry) as source, destination.open("wb") as output:
                while block := source.read(4 * 1024 * 1024):
                    if block.count(0) == len(block): output.seek(len(block), 1)
                    else: output.write(block)
                output.truncate(entry.file_size)
    target.unlink()
    report["image_properties"] = (image / "source.properties").read_text()
    avd = work / "avd"
    (avd / "PleosEvidence.avd").mkdir(parents=True, exist_ok=True)
    (avd / "PleosEvidence.ini").write_text(f"avd.ini.encoding=UTF-8\npath={avd}/PleosEvidence.avd\ntarget=android-34\n")
    (avd / "PleosEvidence.avd/config.ini").write_text(f"""AvdId=PleosEvidence
abi.type=x86_64
hw.cpu.arch=x86_64
hw.cpu.ncore=2
hw.ramSize=4096
hw.lcd.width=2560
hw.lcd.height=1440
hw.lcd.density=160
hw.gpu.enabled=yes
hw.gpu.mode=swiftshader
hw.keyboard=yes
hw.mainKeys=no
image.sysdir.1={image}/
tag.id=android-automotive
target=android-34
disk.dataPartition.size=2G
""")
    userdata = work / "test-userdata.img"
    checkpoint("formatting-isolated-test-data")
    with userdata.open("wb") as output: output.truncate(2 * 1024 ** 3)
    subprocess.run([str(sdk / "emulator/bin64/mkfs.ext4"), "-F", str(userdata)], check=True, capture_output=True)
    environment = os.environ | {"ANDROID_AVD_HOME": str(avd)}
    server = command(adb, "start-server")
    existing = command(adb, "devices", "-l")
    report["adb_server_start"] = {"returncode": server.returncode, "stderr": diagnostic(server.stderr)}
    if server.returncode != 0 or existing.returncode != 0 or "emulator-5554" in existing.stdout:
        raise RuntimeError("Cannot establish an unused dedicated ADB serial; no existing device is modified")
    log = (work / "boot.log").open("w")
    gui = os.getenv("DOCHAGJARI_AVD_GUI") == "1"
    emulator_arguments = [str(sdk / "emulator/emulator"), "-avd", "PleosEvidence", "-data", str(userdata),
                          "-port", "5554", "-no-audio", "-no-boot-anim", "-no-snapshot", "-accel", "on",
                          "-gpu", "swiftshader", "-feature", "-Vulkan", "-cores", "2", "-memory", "4096", "-show-kernel"]
    gui_environment = environment.copy()
    if gui:
        environment["QT_QPA_PLATFORM"] = "xcb"
        authority = work / "xauthority"
        authority.touch(mode=0o600)
        gui_environment.update({"DISPLAY": ":91", "XAUTHORITY": str(authority), "QT_QPA_PLATFORM": "xcb"})
        emulator_arguments = ["xvfb-run", "--server-num=91", "--auth-file=" + str(authority),
                              "--server-args=-screen 0 3000x2000x24 -nolisten tcp", *emulator_arguments, "-no-skin"]
    else:
        emulator_arguments.append("-no-window")
    report["gui_enabled"] = gui
    report["authorization_ui"] = []
    process = subprocess.Popen(emulator_arguments, env=environment, stdout=log, stderr=subprocess.STDOUT,
                               start_new_session=True)
    report["launcher_pid"] = process.pid
    report["launcher_kind"] = "xvfb-run with emulator child" if gui else "emulator"
    checkpoint("booting")
    if os.getenv("GITHUB_ENV"):
        with open(os.environ["GITHUB_ENV"], "a") as output:
            output.write(f"ANDROID_AVD_HOME={avd}\n")
    boot_started = time.monotonic()
    deadline = boot_started + 900
    last_approval = boot_started - 20
    approval_clicked = False
    last_checkpoint = time.monotonic()
    while time.monotonic() < deadline and process.poll() is None:
        snapshot = probe(adb, "emulator-5554")
        report["adb_probe"] = snapshot
        unauthorized = "unauthorized" in snapshot["checks"]["transport"]["stderr"].lower()
        if gui and unauthorized and not approval_clicked and time.monotonic() - last_approval >= 20:
            if os.getenv("GITHUB_ACTIONS") == "true" and os.getenv("DOCHAGJARI_APPROVE_TEST_ADB") == "1":
                approval = approve_visible_dialog(work, reports, gui_environment, len(report["authorization_ui"]) + 1)
                report["authorization_ui"].append(approval)
                approval_clicked = approval["clicked"]
            last_approval = time.monotonic()
        if gui and unauthorized and time.monotonic() - boot_started >= 300:
            report["authorization_blocked"] = "Normal on-screen debugging approval did not authorize the transport within 300 seconds"
            break
        if snapshot["ready"]:
            report["boot_completed"] = True
            break
        if time.monotonic() - last_checkpoint >= 30:
            try:
                devices = command(adb, "devices", "-l", timeout=10)
                report["adb_devices"] = devices.stdout.strip()
                report["adb_devices_returncode"] = devices.returncode
                report["adb_devices_stderr"] = diagnostic(devices.stderr.strip())
            except subprocess.TimeoutExpired:
                report["adb_devices_error"] = "ADB device discovery timed out"
            checkpoint("booting"); last_checkpoint = time.monotonic()
        time.sleep(5)
    if not report["boot_completed"]:
        report["emulator_returncode_before_cleanup"] = process.poll()
        report["boot_wait_seconds"] = round(time.monotonic() - boot_started, 1)
        if process.poll() is None:
            os.killpg(process.pid, signal.SIGTERM)
        raise RuntimeError(report.get("authorization_blocked", "Pleos boot/services not ready within 900 seconds"))
    checkpoint("boot-completed-with-services")
    print("Pleos boot completed with package/activity services", flush=True)
except Exception as exc:
    report["error"] = str(exc)
    checkpoint("failed")
    raise
finally:
    raw = work / "boot.log"
    if raw.exists():
        selected = [line for line in raw.read_text(errors="replace").splitlines()
                    if any(term in line for term in ["emulator version", "FATAL", "Failed to set encryption", "Rebooting into recovery", "Fatal signal", "is crashing", "KVM", "Boot completed", "adb", "ADB", "qt.qpa", "xcb", "xvfb", "PANIC", "ERROR"])
                    and not any(term in line.lower() for term in ["key", "token", "jwt", "secret", "password"])]
        (reports / "boot-diagnostics.txt").write_text("\n".join(selected[-100:]) + "\n")
    (reports / "readiness.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
