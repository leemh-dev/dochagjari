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
from dialog_regions import inspect_regions, crop_ocr
from dialog_identity import identity, same_frame_header_confirms
from x11_context import observe_pointer, input_target_known, native_input_target


def approval_plan(tsv, fingerprint):
    rows = [r for r in csv.DictReader(io.StringIO(tsv), delimiter="\t") if r.get("text", "").strip()]
    text = " ".join(r["text"] for r in rows).casefold()
    observed=identity(text,fingerprint)
    dialog=observed['dialog_detected'];matches=observed['host_fingerprint_matches']
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
    result = observed | {"unambiguous_allow_button":len(candidates)==1,"click":None}
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
            "dialog_state": "present" if plan["dialog_detected"] else "unknown",
            "absence_verified": False,
            "note": "OCR misses do not prove dismissal; inspect the original image and ADB transport"}


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
        observation_point = point or regions["permission_click"]
        if observation_point:
            # Read-only window evidence must survive a title OCR miss. This never moves/clicks.
            result["x11_observation"] = observe_pointer(environment, observation_point)
        if (not plan["dialog_detected"] and plan.get("title_detected") and plan["host_fingerprint_matches"]
                and not regions["desktop_panel_blocked"] and regions["permission_click"]):
            observation=result.get("x11_observation",{})
            cx,cy=regions["permission_click"]
            views={ (w['x'],w['y'],w['width'],w['height'])
                    for w in observation.get('window_inventory',{}).get('windows',[])
                    if any(c.casefold()=='emulator' for c in w['classes']) and w['width']>400 and w['height']>300
                    and w['x']<=cx<w['x']+w['width'] and w['y']<=cy<w['y']+w['height'] }
            if len(views)==1:
                x,y,width,height=next(iter(views))
                tops=[b['box'][1] for b in regions['buttons'] if b['label'] in {'allow','cancel'}]
                band=min(450,min(tops)-y) if tops else 0
                if x>=0 and y>=0 and 80<=band<=height:
                    header=identity(crop_ocr(screenshot,work,(x,y,min(width,1200),band),environment,6),fingerprint)
                    result['focused_header_identity']=header
                    result['full_frame_dialog_detected']=plan['dialog_detected']
                    if same_frame_header_confirms(plan,header):
                        plan['dialog_detected']=True
                        result['dialog_detected']=True
                        result['dialog_confirmed_by']='same original full title and two matching fingerprints, header labels'
        if regions["notice_click"]:
            nx, ny = regions["notice_click"]
            subprocess.run(["xdotool", "mousemove", str(nx), str(ny), "click", "1"],
                           env=environment, capture_output=True, check=True, timeout=10)
            result["nested_notice_dismissed"] = True
            return result
        if regions["desktop_panel_blocked"]:
            return result
        if not point and plan["dialog_detected"] and plan["host_fingerprint_matches"]:
            point = regions["permission_click"]
        if point:
            subprocess.run(["xdotool", "mousemove", str(point[0]), str(point[1])],
                           env=environment, capture_output=True, check=True, timeout=10)
            result["x11_before_input"] = observe_pointer(environment, point)
            if not input_target_known(result["x11_before_input"]):
                result["input_blocked"] = "X11 pointer target is unknown; no approval gesture emitted"
                capture_desktop(reports / f"debugging-target-unknown-{attempt}.png", environment)
                return result
            native_target=native_input_target(result["x11_before_input"])
            if native_target:
                # Normal host window activation, after strict scene/host/native target verification.
                # Never alters input shapes, Android permissions, keys or ADB authentication.
                focus=subprocess.run(["xdotool","windowfocus",str(native_target)],env=environment,
                                     capture_output=True,text=True,timeout=5)
                result['window_activation']={'target':native_target,'returncode':focus.returncode,
                                             'stderr':diagnostic(focus.stderr)}
                if focus.returncode:
                    result['input_blocked']='Normal emulator window activation failed'
                    return result
                time.sleep(.2)
                after=observe_pointer(environment,point)
                result['x11_after_activation']=after
                if (native_input_target(after)!=native_target or
                    after.get('native_state',{}).get('direct_focus',{}).get('window')!=native_target):
                    result['input_blocked']='Fresh native target/focus not confirmed after normal activation'
                    return result
            subprocess.run(["xdotool", "mousedown", "1", "sleep", "0.2", "mouseup", "1"],
                           env=environment, capture_output=True, check=True, timeout=10)
            result["clicked"] = True
            result["click_input_sent"] = True
            result["input_method"] = "Normal visible Allow button: mouse down, 200ms hold, mouse up"
            time.sleep(2)
            result["x11_after_input"] = observe_pointer(environment, point)
            result["post_input_observation"] = observe_after_input(
                reports / f"debugging-after-input-{attempt}.png", environment, fingerprint)
    except (OSError, ValueError, KeyError, IndexError, subprocess.SubprocessError) as error:
        result["error"] = diagnostic(str(error))
    return result
