# Third-party inputs

- `app/src/main/assets/efficientdet-lite0.tflite`: TensorFlow EfficientDet Lite0 object detection model, downloaded from the official TensorFlow task-library distribution: https://storage.googleapis.com/download.tensorflow.org/models/tflite/task_library/object_detection/android/lite-model_efficientdet_lite0_detection_metadata_1.tflite . TensorFlow model/example code is Apache-2.0: https://github.com/tensorflow/examples/blob/master/LICENSE . COCO-trained categories are used as obstacle clues, not a clearance guarantee.
- `public-obstacles.mp4`: first five seconds of OpenCV `samples/data/vtest.avi`, resized to 640 pixels wide and transcoded to H.264. Source: https://github.com/opencv/opencv/blob/4.x/samples/data/vtest.avi . OpenCV repository license: https://github.com/opencv/opencv/blob/4.x/LICENSE (Apache-2.0). This is a public pedestrian surveillance test scene; it is **not** vehicle-camera footage or a verified pickup location.
- `generated-empty.png`, `generated-empty.mp4`, `generated-blocked.mp4`: synthetic test scenes created for this project using FFmpeg color, box and text filters; the latter uses DejaVu Sans rendered as pixels. DejaVu license: https://dejavu-fonts.github.io/License.html . These clips exercise reference-image obstruction detection and OCR; they are not real road evidence.
- Gradle Wrapper scripts and JAR: Gradle 8.10.2 wrapper from the official Pleos sample, Apache-2.0: https://github.com/gradle/gradle/blob/v8.10.2/LICENSE . Pleos sample reference: https://github.com/Pleos-Playground/pleos-sample-navi-app (commit `76ed592487a1eab7ba102b9d4b321705426886fe`).
- Pleos NaviHelper 3.0.5 and Vehicle 2.3.2 are resolved from the official Maven repository under Pleos SDK terms. SDK archives and emulator images are not committed to this repository. https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator
- AndroidX, TensorFlow Lite Task Vision, ML Kit and ZXing are declared as dependencies in Gradle. Their upstream notices and SDK terms apply; see their published artifacts and https://developers.google.com/ml-kit/terms .

Dataset/model suitability and media redistribution terms must be reviewed before a commercial release.

## 시연 편집 전용 공개 실사 전방 주행 영상

**City Driving 4K: Kraków Poland 2024 — Relaxing Roads 4K**, [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:City_Driving_4K-_Krak%C3%B3w_Poland_2024.webm), [CC BY3.0](https://creativecommons.org/licenses/by/3.0/). 원본60–80초 추출·음소거·축소/재인코딩 후 합성했다. [저작자/원본/변경·지문](docs/media/front-driving/README.md). 앱 번들에 새로 포함하지 않았으며3분할 시연 상단의 사전 실사 예시로 사용한다.

## 백화점 UX 시연 · 실제 몰 주차장 두 컷

**Empty Mall Parking (5972195)** / **An Empty Parking Area (5972197)**, gusat silviu, [Pexels License](https://www.pexels.com/license/). 음소거·HD 재인코딩·구간 추출/역재생·감속·정차 프레임 유지·크롭 후 시연 영상에 합성했다. [원본 링크/지문/변경](docs/media/mall-parking/README.md). 앱 번들 미포함이며 판교점 실사/실차/OEM카메라 연결 증거가 아니다.
