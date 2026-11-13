"""Read-only OCR regions from one original screenshot; never synthesizes UI evidence."""
import re
import subprocess


def rectangles(image, environment, threshold):
    result = subprocess.run(["convert", str(image), "-alpha", "off", "-colorspace", "Gray", "-threshold", threshold,
                             "-depth", "8", "-define", "connected-components:verbose=true", "-connected-components", "4", "null:"],
                            env=environment, capture_output=True, text=True, check=True, timeout=20)
    boxes = []
    for line in result.stdout.splitlines():
        match = re.search(r"(\d+)x(\d+)\+(\d+)\+(\d+)\s+\S+\s+([0-9.]+)\s+(.*)$", line)
        if match and ("gray(255)" in match[6] or "gray(100%)" in match[6]):
            w, h, x, y = map(int, match.group(1, 2, 3, 4))
            if float(match[5]) >= 300:
                boxes.append((x, y, w, h))
    return boxes


def crop_ocr(image, work, box, environment, mode, threshold=None):
    x, y, w, h = box
    crop = work / "ui-ocr-analysis.png"
    arguments = ["convert", str(image), "-crop", f"{w}x{h}+{x}+{y}", "+repage"]
    if threshold:
        arguments += ["-colorspace", "Gray", "-threshold", threshold]
    subprocess.run([*arguments, "-resize", "400%", str(crop)], env=environment, capture_output=True, check=True, timeout=15)
    return subprocess.run(["tesseract", str(crop), "stdout", "-l", "eng+kor", "--psm", str(mode)],
                          env=environment, capture_output=True, text=True, check=True, timeout=15).stdout


def exact_label(text, choices):
    labels = set()
    for line in text.splitlines():
        value = re.sub(r"[^a-zA-Z가-힣]", "", line).casefold()
        if value in choices:
            labels.add(choices[value])
    return labels


def permission_button_pair(buttons):
    allowed = [b["box"] for b in buttons if b["label"] == "allow"]
    cancelled = [b["box"] for b in buttons if b["label"] == "cancel"]
    if len(allowed) != 1 or len(cancelled) != 1:
        return None
    x, y, w, h = allowed[0]
    cx, cy, cw, ch = cancelled[0]
    if cx + cw <= x and x - cx - cw <= max(200, 2 * w) and abs(y + h / 2 - cy - ch / 2) <= min(h, ch):
        return (x + w // 2, y + h // 2)
    return None


def inspect_regions(image, work, environment):
    white = rectangles(image, environment, "90%")
    panels = [b for b in white if 300 <= b[2] <= 1000 and 80 <= b[3] <= 300][:4]
    result = {"notice_click": None, "desktop_panel_blocked": bool(panels), "permission_click": None, "buttons": []}
    for box in panels:
        text = " ".join(crop_ocr(image, work, box, environment, 6).casefold().split())
        if "emulator is running using nested virtualization" not in text or "recommended" not in text or "performance" not in text:
            continue
        x, y, w, h = box
        ok = []
        for small in white:
            sx, sy, sw, sh = small
            if x + w / 2 <= sx and y + h / 2 <= sy and sx + sw <= x + w and sy + sh <= y + h and 40 <= sw <= 200 and 15 <= sh <= 80:
                labels = set()
                for mode in [7, 6, 13]:
                    labels.update(exact_label(crop_ocr(image, work, small, environment, mode, "75%"), {"ok": "ok"}))
                if labels == {"ok"}:
                    ok.append((sx + sw // 2, sy + sh // 2))
        if len(ok) == 1:
            result["notice_click"] = ok[0]
            return result
    if panels:
        return result
    for box in rectangles(image, environment, "40%"):
        x, y, w, h = box
        if not (80 <= w <= 500 and 20 <= h <= 100 and w / h >= 2):
            continue
        labels = set()
        for mode in [7, 6, 13]:
            labels.update(exact_label(crop_ocr(image, work, box, environment, mode, "40%"),
                                      {"allow": "allow", "허용": "allow", "cancel": "cancel", "취소": "cancel"}))
        result["buttons"].append({"box": box, "label": next(iter(labels)) if len(labels) == 1 else "unknown"})
    result["permission_click"] = permission_button_pair(result["buttons"])
    return result
