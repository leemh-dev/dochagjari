# 구현·검증 기록

2026-11-11 UTC · 기준: [개발계획](07-development-plan.md)

## 코드와 APK

- Android 구현: `0a04f40` (`feat/android-submission-mvp`). 이후 변경은 서버·호스트 검증·문서입니다.
- SMS 서버: `ae33591`. 호스트 미디어 검증: `0872124`.
- 개발 APK: [`apk/dochagjari-debug.apk`](../apk/dochagjari-debug.apk). 개발 서명이며 제출용 release 서명 APK가 아닙니다.
- debug APK SHA-256: `c72bc9d9893fd64d23942fb7b751ef7269d870962ce55f9add6d84f3541c6ae4`.
- unsigned release SHA-256: `6536145a8248777a58d1fe4871f36b9db7c309b830ea767ce07015f4e489703e` (로컬 빌드 산출물, 미서명).
- 실행 대상: API 34+, `arm64-v8a`와 `x86_64`. 앱 ID `dev.leemh.dochagjari`, 버전 `0.1.0` / `1`.
- unsigned release는 `:app:assembleRelease`로 생성했습니다. 제출용 키가 제공되지 않아 서명된 release·새 AVD 설치 검증은 남아 있습니다.

## 검증 수준

| 기능 | 이번에 확인한 내용 | 실제 연동·기기 검증 |
| --- | --- | --- |
| 학원 픽업·우회 | 추천 단위 테스트: 같은 문에서 모드별 도착점 변경, 점유·이용 불가 우회, 후보 없음 | APK 빌드 완료. 앱 화면 기기 테스트는 아래 Cloud 결과 참조 |
| NaviHelper | 공식 샘플 및 도착자리 SDK 포함 APK 빌드. 실제·모의 요청 경로 구현 | 목적지 두 곳의 실제 결과 이벤트·순정 내비 핀은 별도 기기 검증 필요 |
| 좌석 | Pleos Vehicle 2.3.2의 capability, 좌우 초기값·변경 구독 구현. 테스트 입력·권한 없음·미지원 흐름 구현 | Virtual Console 조작과 앱 수신은 아직 완료 증거 없음. 실차 미검증 |
| 설정 저장 | DataStore 프로필·차량 높이 저장 구현, 재시작 기기 테스트 APK 빌드 | 저장·재시작 테스트는 기기 실행 필요 |
| 병원 접근성 | 동행 시 하차 분리 제외, 무단차 미확인 제외, 높이·시간 조건을 단위 테스트로 확인 | 가상 병원 시연. 용인세브란스 전체 무단차·하차 허용은 미검증 |
| 영상 | 호스트 CPU에서 동봉 실제 모델로 공개 보행자 영상 점유 확인. 생성 빈 구간·장애물 입력으로 결과 변경, 별도 Tesseract ROI에서 C12 인식 | Android Task Vision·ML Kit 파이프라인 구현 및 테스트 APK 빌드. 앱 내 실행은 별도 기기 검증 필요. 순정 외부 카메라 미연동 |
| 자동 알림 | 서버 테스트 8건: 인증·수신자 제한·정차와 확인 조건·동시 중복·재시작·타임아웃·공급자 거부·일일 제한 | 모의 공급자 사용. 실제 SENS 자격증명·동의 수신자 미제공으로 공급자 수락·휴대폰 수신 미검증 |
| 공유 | 위치·단서 QR와 Android 수동 공유 구현 | 휴대폰 QR·카카오톡 대상 선택·수신은 미검증 |

**빌드 성공, 테스트 입력 검증, AVD 이벤트 검증, 실차·휴대폰 수신은 서로 다른 결과입니다.**

## 실행한 검사

Cloud: Linux x86_64, JDK 17, Android SDK 35 / Build Tools 35.0.0, Gradle 8.10.2. 원격 의존성 다운로드는 환경 프록시와 CA 신뢰를 사용했습니다.

```text
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest \
  :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
BUILD SUCCESSFUL in 2m 10s
129 actionable tasks: 34 executed, 95 up-to-date

python -m unittest discover -s server/tests -v
Ran 8 tests · OK

공식 Pleos 내비 샘플 :app:assembleDebug
BUILD SUCCESSFUL in 4m 27s
```

- Android 단위 테스트 **9건, 실패 0건**: [JUnit XML](evidence/android-unit-tests.xml).
- lint 오류 **0건**. API 34 대상·고정 의존성 버전에 대한 경고는 유지합니다. min/target은 이번 Pleos API 34 검증 범위에 맞췄습니다.
- `apksigner verify --print-certs`: debug APK 서명 검증 성공, `CN=Android Debug`.
- Android 기기 테스트는 추천 UI, 모델·OCR, DataStore·알림 저널 재시작을 포함합니다. **테스트 APK 생성은 기기 실행 성공을 뜻하지 않습니다.**

호스트 입력 검증:

```bash
pip install -r scripts/media-requirements.txt
python scripts/verify_media.py
```

[실제 실행 JSON](evidence/host-media.json): 빈 구간 `EMPTY`, 생성 장애물 `OCCUPIED`, 공개 장애물 `OCCUPIED` (프레임 점수 0.816 / 0.758 / 0.668 / 0.723). 생성 표지는 호스트 Tesseract의 등록 ROI에서 `C12`로 인식했습니다. Android는 ML Kit를 사용하므로 이 결과를 Android OCR 성공으로 대체하지 않습니다.

## Cloud Pleos 실행

- 공식 Linux 이미지: Pleos Connect **2.1.0**, API **34**, x86_64.
- 공식 프로필: **2560 × 1440**, 160dpi, RAM 4GB.
- `/dev/kvm` 없음. [가속 검사](evidence/cloud-acceleration.txt): `KVM requires a CPU that supports vmx or svm`.
- Emulator 37.2.13에서 `-accel off`, SwiftShader로 소프트웨어 부팅을 시도했습니다. ADB 연결은 관찰했지만 부팅 완료로 판단하지 않았습니다.
- 연장 부팅의 커널 로그에 `/data/misc` 암호화 정책 설정 실패와 recovery 재부팅이 나타났습니다. 원인을 앱 오류로 단정하지 않습니다. 공식 Emulator 35.2.10과 초기화한 개발 AVD로 비교 검증을 진행합니다.
- 부팅 완료, 앱 설치·실행, 순정 내비 핀, Virtual Console 좌석 수신은 각각 성공 증거를 확보한 뒤 갱신합니다.

맥북 M1 재현 명령, ARM64 이미지 설치와 두 목적지·좌우 좌석 확인 순서는 [README](../README.md)에 있습니다.

## 제출 전 남은 확인

- Pleos AVD에서 공식 샘플과 앱을 설치해 목적지 두 곳의 실제 핀을 비교합니다.
- Virtual Console 좌우 좌석 변경과 앱 콜백을 함께 녹화하고, 저장 옵션을 재실행으로 확인합니다.
- Android 기기 테스트에서 실제 영상·ML Kit OCR을 실행합니다.
- 실제 SMS는 SENS 등록 발신번호·동의 수신자를 설정한 뒤 공급자 수락과 휴대폰 수신을 각각 확인합니다.
- 실제 시설의 미확인 동선은 계속 가상 데이터와 구분하고 제출용 키로 release APK를 서명합니다.
