"""Host-only media smoke test; does NOT establish Android/ML Kit runtime success.

Requires ffmpeg, tesseract, and scripts/media-requirements.txt in a local venv.
"""
from pathlib import Path
import json
import subprocess
import tempfile

import numpy as np
from PIL import Image
from ai_edge_litert.interpreter import Interpreter

assets = Path(__file__).resolve().parents[1] / "app/src/main/assets"
interpreter = Interpreter(model_path=str(assets / "efficientdet-lite0.tflite"), num_threads=2)
interpreter.allocate_tensors()
input_info = interpreter.get_input_details()[0]
outputs = interpreter.get_output_details()
reference = np.asarray(Image.open(assets / "generated-empty.png").convert("RGB").resize((80, 60), Image.Resampling.NEAREST), dtype=np.int32)
results = []
with tempfile.TemporaryDirectory() as tmp:
    for name in ["generated-empty", "generated-blocked", "public-obstacles"]:
        occupied = empty = 0
        labels = []
        text = ""
        for second in range(4):
            frame = Path(tmp) / f"{name}-{second}.png"
            subprocess.run(["ffmpeg", "-v", "error", "-y", "-ss", str(second), "-i", str(assets / f"{name}.mp4"), "-frames:v", "1", str(frame)], check=True)
            image = Image.open(frame).convert("RGB")
            data = np.asarray(image.resize((320, 320)), dtype=np.uint8)[None]
            interpreter.set_tensor(input_info["index"], data)
            interpreter.invoke()
            boxes, classes, scores, count = [interpreter.get_tensor(d["index"]) for d in outputs]
            score = 0.0
            # COCO 0-based person/bicycle/car/motorcycle/bus/truck/cat/dog/bench/chair/suitcase.
            obstacles = {0, 1, 2, 3, 5, 7, 14, 15, 16, 27, 56}
            for index in range(int(count[0])):
                top, left, bottom, right = boxes[0, index]
                if min(bottom, .9) <= max(top, .1) or min(right, .9) <= max(left, .1):
                    continue
                if int(classes[0, index]) in obstacles:
                    score = max(score, float(scores[0, index]))
            if name.startswith("generated"):
                pixels = np.asarray(image.resize((80, 60), Image.Resampling.NEAREST), dtype=np.int32)
                delta = np.abs(pixels - reference).sum(axis=2)[12:48, 8:72]
                difference = float((delta > 100).mean())
                if difference > .08:
                    score = max(score, .9)
                elif difference < .025 and score < .65:
                    empty += 1
            if score >= .65:
                occupied += 1
            labels.append(round(score, 3))
            if second == 0:
                text = subprocess.run(["tesseract", str(frame), "stdout", "--psm", "6"], check=True, capture_output=True, text=True).stdout.strip()
        status = "OCCUPIED" if occupied >= 2 else "EMPTY" if empty == 4 else "UNKNOWN"
        results.append(dict(input=name, status=status, obstacle_scores=labels, host_tesseract_text=text))
assert results[0]["status"] == "EMPTY", results
assert results[1]["status"] == "OCCUPIED", results
assert "C12" in results[1]["host_tesseract_text"].replace(" ", ""), results
assert results[2]["status"] == "OCCUPIED", results
print(json.dumps({"verification": "HOST CPU ONLY; Android Task Vision and ML Kit instrumentation not executed by this script", "results": results}, ensure_ascii=False, indent=2))
