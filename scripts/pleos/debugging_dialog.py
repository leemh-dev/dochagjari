"""Normal on-screen approval for this job's fresh test AVD, never an ADB auth bypass."""
import base64
import csv
import hashlib
import io
import os
from pathlib import Path
import re
import subprocess
import time

from adb_readiness import diagnostic
from dialog_regions import inspect_regions


def approval_plan(tsv, fingerprint):
    rows = [r for r in csv.DictReader(io.StringIO(tsv), delimiter="\t") if r.get("text", "").strip()]
    text = " ".join(r["text"] for r in rows).casefold()
    compact = "".join(text.split())
    title = ("allow usb debugging" in text or ("usb" in compact and "디버깅" in compact and "허용" in compact))
    dialog = title and "rsa" in text and ("fingerprint" in text or "지문" in text)
    fingerprints = re.findall(r"(?<![0-9A-F:])(?:[0-9A-F]{2}\s*[:;]\s*){15}[0-9A-F]{2}(?![0-9A-F:])", text.upper())
    matches = len(fingerprints) == 1 and re.sub(r"\s", "", fingerprints[0]).replace(";", ":") == fingerprint.upper()
    lines = {}
    for row in rows:
        lines.setdefault(tuple(row.get(k) for k in ("block_num", "par_num", "line_num")), []).append(row)
    candidates = []
    for line in lines.values():
        words = [r["text"].casefold() for r in line]
        if not set(words) <= {"allow", "cancel", "허용", "취소"}:
            continue
        for row in line:
            if row["text"].casefold() in {"allow", "허용"} and float(row.get("conf", "-1")) >= 60:
                x = int(row["left"]) + int(row["width"]) // 2
                y = int(row["top"]) + int(row["height"]) // 2
                cancel_on_same_bar = any(r["text"].casefold() in {"cancel", "취소"} and
                                         float(r.get("conf", "-1")) >= 60 and int(r["left"]) < x and
                                         abs(int(r["top"]) + int(r["height"]) // 2 - y) <= int(row["height"])
                                         for r in rows)
                if cancel_on_same_bar:
                    candidates.append((x, y))
    result = {"dialog_detected": dialog, "host_fingerprint_matches": matches,
              "unambiguous_allow_button": len(candidates) == 1, "click": None}
    if dialog and matches and len(candidates) == 1 and all(v > 0 for v in candidates[0]):
        result["click"] = candidates[0]
    return result


def capture_desktop(path, environment):
    subprocess.run(["import", "-window", "root", str(path)], env=environment,
                   capture_output=True, check=True, timeout=15)


def observe_after_input(path, environment, fingerprint):
    """Read-only observation: input delivery never proves guest approval."""
    capture_desktop(path, environment)
    ocr = subprocess.run(["tesseract", str(path), "stdout", "-l", "eng+kor", "--psm", "11", "tsv"],
                         env=environment, capture_output=True, text=True, check=True, timeout=20)
    plan = approval_plan(ocr.stdout, fingerprint)
    return {"screenshot": path.name, "dialog_detected": plan["dialog_detected"],
            "host_fingerprint_matches": plan["host_fingerprint_matches"],
            "note": "Read-only post-input observation; ADB transport is authoritative"}


def approve_visible_dialog(work, reports, environment, attempt):
    """Read only the public host key; click Allow only after exact fingerprint comparison."""
    result = {"clicked": False, "attempt": attempt, "host_fingerprint_available": False,
              "method": "X11 normal AVD screen; exact host fingerprint required; no key editing"}
    try:
        android_user = Path(os.environ.get("ANDROID_USER_HOME", str(Path.home() / ".android")))
        public = (android_user / "adbkey.pub").read_text().split()[0]
        digest = hashlib.md5(base64.b64decode(public), usedforsecurity=False).hexdigest().upper()
        result["host_fingerprint_available"] = True
        fingerprint = ":".join(digest[i:i + 2] for i in range(0, len(digest), 2))
        screenshot = reports / f"debugging-screen-{attempt}.png"
        subprocess.run(["import", "-window", "root", str(screenshot)], env=environment, capture_output=True, check=True, timeout=15)
        ocr = subprocess.run(["tesseract", str(screenshot), "stdout", "-l", "eng+kor", "--psm", "11", "tsv"],
                             env=environment, capture_output=True, text=True, check=True, timeout=20)
        plan = approval_plan(ocr.stdout, fingerprint)
        point = plan.pop("click")
        result.update(plan)
        regions = inspect_regions(screenshot, work, environment)
        result["region_observation"] = regions
        if regions["notice_click"]:
            nx, ny = regions["notice_click"]
            subprocess.run(["xdotool", "mousemove", "--sync", str(nx), str(ny), "click", "1"],
                           env=environment, capture_output=True, check=True, timeout=10)
            result["nested_notice_dismissed"] = True
            return result
        if regions["desktop_panel_blocked"]:
            return result
        if not point and plan["dialog_detected"] and plan["host_fingerprint_matches"]:
            point = regions["permission_click"]
        if point:
            subprocess.run(["xdotool", "mousemove", "--sync", str(point[0]), str(point[1]),
                            "mousedown", "1", "sleep", "0.2", "mouseup", "1"],
                           env=environment, capture_output=True, check=True, timeout=10)
            result["clicked"] = True
            result["click_input_sent"] = True
            result["input_method"] = "Normal visible Allow button: mouse down, 200ms hold, mouse up"
            time.sleep(2)
            result["post_input_observation"] = observe_after_input(
                reports / f"debugging-after-input-{attempt}.png", environment, fingerprint)
    except (OSError, ValueError, KeyError, IndexError, subprocess.SubprocessError) as error:
        result["error"] = diagnostic(str(error))
    return result
