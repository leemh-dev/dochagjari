"""One focused Pleos boot on a KVM runner; never downloads images without acceleration.

SDK images remain unchanged. A separately formatted test userdata avoids the earlier
/data/misc initialization conflict. Only sanitized readiness evidence is uploaded.
"""
from pathlib import Path
import hashlib
import concurrent.futures
import math
import json
import os
import subprocess
import time
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

sdk = Path(os.environ["ANDROID_HOME"])
work = Path("app/build/pleos-work").resolve()
reports = Path("app/build/reports/pleos").resolve()
reports.mkdir(parents=True, exist_ok=True)
work.mkdir(parents=True, exist_ok=True)
adb = str(sdk / "platform-tools/adb")
report = {"environment": "GitHub KVM / official Pleos 2.1.0 API34 x86_64 / separate test userdata", "boot_completed": False}


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
    # The official host supports byte ranges; parallel streams avoid its per-connection cap.
    with urllib.request.urlopen(urllib.request.Request(url, headers={"Range": "bytes=0-0"}), timeout=90) as response:
        range_supported = response.status == 206 and response.headers.get("Content-Range") == f"bytes 0-0/{size}"
    if range_supported:
        with target.open("wb") as output: output.truncate(size)
        step = math.ceil(size / 8)
        def fetch_part(index):
            start = index * step; end = min(size - 1, start + step - 1)
            request = urllib.request.Request(url, headers={"Range": f"bytes={start}-{end}"})
            with urllib.request.urlopen(request, timeout=120) as response, target.open("r+b") as output:
                if response.status != 206 or response.headers.get("Content-Range") != f"bytes {start}-{end}/{size}":
                    raise RuntimeError("Unexpected range response")
                output.seek(start)
                remaining = end - start + 1
                while remaining:
                    block = response.read(min(8 * 1024 * 1024, remaining))
                    if not block: raise RuntimeError("Incomplete SDK range")
                    output.write(block); remaining -= len(block)
            print(f"Pleos archive range {index + 1}/8 complete", flush=True)
        with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
            list(pool.map(fetch_part, range(8)))
    else:
        with urllib.request.urlopen(url, timeout=90) as response, target.open("wb") as output:
            while block := response.read(8 * 1024 * 1024): output.write(block)
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
    command(adb, "start-server")
    log = (work / "boot.log").open("w")
    process = subprocess.Popen([str(sdk / "emulator/emulator"), "-avd", "PleosEvidence", "-data", str(userdata),
                                "-no-window", "-no-audio", "-no-boot-anim", "-no-snapshot", "-accel", "on",
                                "-gpu", "swiftshader", "-feature", "-Vulkan", "-cores", "2", "-memory", "4096", "-show-kernel"],
                               env=environment, stdout=log, stderr=subprocess.STDOUT)
    report["emulator_pid"] = process.pid
    checkpoint("booting")
    if os.getenv("GITHUB_ENV"):
        with open(os.environ["GITHUB_ENV"], "a") as output:
            output.write(f"ANDROID_AVD_HOME={avd}\n")
    deadline = time.monotonic() + 900
    last_checkpoint = time.monotonic()
    while time.monotonic() < deadline and process.poll() is None:
        try:
            result = command(adb, "-s", "emulator-5554", "shell", "getprop sys.boot_completed; service check package; service check activity", timeout=12)
            report["last_readiness"] = result.stdout.strip()
            if result.stdout.startswith("1") and "Service package: found" in result.stdout and "Service activity: found" in result.stdout:
                report["boot_completed"] = True
                break
        except subprocess.TimeoutExpired:
            report["last_readiness"] = "ADB shell timeout"
        if time.monotonic() - last_checkpoint >= 30:
            checkpoint("booting"); last_checkpoint = time.monotonic()
        time.sleep(5)
    if not report["boot_completed"]:
        process.terminate()
        raise RuntimeError("Pleos boot/services not ready within 900 seconds")
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
                    if any(term in line for term in ["emulator version", "FATAL", "Failed to set encryption", "Rebooting into recovery", "Fatal signal", "is crashing", "KVM", "Boot completed"])
                    and not any(term in line.lower() for term in ["pubkey", "token", "jwks"])]
        (reports / "boot-diagnostics.txt").write_text("\n".join(selected[-100:]) + "\n")
    (reports / "readiness.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
