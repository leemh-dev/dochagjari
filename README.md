# 도착자리

**목적지 너머, 만날 자리까지.** 학원 픽업·주차장 우회와 병원 동행을 안내하는 Kotlin Android 앱입니다. [11월 15일 개발계획](docs/07-development-plan.md)을 구현 기준으로 사용합니다.

가상 학원·병원 각각 한 곳을 동봉합니다. 사람 출입구를 유지하면서 방문 모드·이용 불가·영상 점유에 따라 차량 도착점이 바뀝니다. 순정 내비 좌표의 실제 승하차 허용 여부나 실제 시설 무단차 경로를 검증한 서비스는 아닙니다.

개발을 이어갈 때는 [다음 개발 실행 프롬프트](docs/09-next-development-prompt.md)를 사용합니다. 주요 진행 절차·실패·후속 작업은 [개발 일지](docs/10-development-log.md), 기능별 완료 증거는 [검증 기록](docs/08-verification.md)에 남깁니다.

## 빌드와 APK

- JDK **17**, Android SDK Platform **35**, Build Tools **35.0.0**. 실행 대상은 **API 34 이상**, ARM64 또는 x86_64입니다.
- Gradle **8.10.2**, AGP **8.8.0**, Kotlin **1.9.22**. Wrapper를 동봉합니다.
- 공식 SDK: `NaviHelper:3.0.5`, `Vehicle:2.3.2`. Maven: `https://nexus-playground.pleos.ai/repository/maven-releases/`.
- 현재 버전은 **0.2.0 / versionCode 2**입니다. [개발 APK·기기 테스트 APK·unsigned release artifact](https://github.com/leemh-dev/dochagjari/actions/runs/43055801516/artifacts/13871881801)의 서명 종류와 SHA-256은 [검증 기록](docs/08-verification.md)에 정리합니다. debug는 개발 서명이고 제출용 서명 APK가 아닙니다.
- 배포 artifact는 `development-release-assets-and-metadata`입니다. 압축을 풀면 세 APK와 `apk-metadata.json`, `SHA256SUMS`가 있습니다. GitHub 로그인 후 다운로드합니다. runner 개발 서명은 이전 Cloud 서명과 다르므로 **새 테스트 AVD**를 사용합니다. 기존 데이터가 있는 앱을 임의 삭제하지 않습니다. Release 게시 API의 인증 제한과 대체 경로는 [개발 일지](docs/10-development-log.md)에 기록했습니다.
- 저장소의 [`apk/dochagjari-debug.apk`](apk/dochagjari-debug.apk)는 **이전 0.1.0 보관본**입니다. 큰 새 바이너리는 CI artifact로 제공해 소스 이력을 불필요하게 늘리지 않습니다.

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
# SDK Manager에서 Android API 35와 Build Tools 35.0.0 설치
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
# 결과: app/build/outputs/apk/debug/app-debug.apk
```

Linux에서는 설치된 SDK와 JDK 경로로 `ANDROID_HOME`, `JAVA_HOME`을 지정합니다. 머신별 SDK 경로를 `local.properties`의 `sdk.dir`에 지정해도 됩니다. 공개 의존성 다운로드에는 Google Maven, Maven Central, Gradle 배포 사이트, 공식 Pleos Maven 접근이 필요합니다.

## 맥북 M1에서 Pleos 설치·실행

1. [공식 설치 가이드](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)에 따라 Android Studio → SDK Manager → SDK Update Sites에 아래 **Mac ARM64** 주소를 추가합니다.
   `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`
2. SDK Platforms의 **Show Package Details**를 켜고 API 34의 Pleos 이미지를 설치합니다. Device Manager → Automotive → **Pleos Connect v2.1.0**, API 34를 선택합니다. 선택한 이미지의 공식 기본 메모리·장치 프로필을 유지하고 Cold boot로 설정합니다. 이번 Linux x86_64 이미지의 source.properties 기본 RAM은 8192MB였으므로 호스트 여유 메모리도 확인합니다. 화면은 제공되는 Pleos 장치 프로필을 사용합니다.
3. VS Code를 맥북에서 로컬로 열고 터미널에서 실행합니다.

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
emulator -list-avds
emulator -avd "PLEOS_AVD_NAME" -no-snapshot-load
```

다른 터미널에서 장치의 실제 시리얼을 지정합니다. 아래 `emulator-5554`는 예시입니다.

```bash
adb devices -l
export DOCHAGJARI_SERIAL=emulator-5554
adb -s "$DOCHAGJARI_SERIAL" shell getprop sys.boot_completed
# 출력이 1인지 확인한 뒤 다음 두 서비스가 모두 found인지 확인합니다.
adb -s "$DOCHAGJARI_SERIAL" shell service check package
adb -s "$DOCHAGJARI_SERIAL" shell service check activity
# device 표시만으로 설치하지 않습니다. 위 조건이 충족되지 않으면 부팅 진단을 수집합니다.
./gradlew :app:assembleDebug
adb -s "$DOCHAGJARI_SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk
adb -s "$DOCHAGJARI_SERIAL" shell am start -n dev.leemh.dochagjari/.MainActivity
adb -s "$DOCHAGJARI_SERIAL" forward tcp:8088 tcp:8088
adb -s "$DOCHAGJARI_SERIAL" shell wm size
adb -s "$DOCHAGJARI_SERIAL" shell wm density
adb -s "$DOCHAGJARI_SERIAL" logcat -v time Dochagjari:I DochagjariVehicle:I DochagjariPleosSeat:I '*:S'
```

### 내비 목적지 두 곳

- 앱 **연동·테스트 → 내비 모의 요청 끄기**. **도착 안내 → 학원 → 바로 태우기 → 내비로 안내**를 실행합니다.
- 요청 좌표는 `37.48544722, 127.03636666`입니다. 앱의 실제 SDK 결과와 **순정 내비의 도착 핀**을 각각 기록합니다.
- **주차하고 데리러 가기 → 내비로 안내**로 전환합니다. 두 번째 좌표는 `37.48615, 127.03715`입니다. 같은 사람 출입구와 서로 다른 차량 핀을 비교합니다.
- **바로 태우기 → 이 지점 이용 불가 · 우회**에서도 주차장으로 바뀌어야 합니다. 새 후보의 **내비로 안내**를 눌러 요청합니다.
- 이 좌표는 가상 학원을 설명하기 위한 테스트 좌표입니다. 좌표 요청이 POI 보정되거나 실패하면 앱 결과와 핀 차이를 그대로 기록합니다. 임의 POI ID를 만들지 않습니다.
- [공식 내비 샘플](https://github.com/Pleos-Playground/pleos-sample-navi-app)도 별도로 빌드·설치해 기준 동작을 비교합니다. 공식 샘플 첫 목적지는 `42dot / 37.48544722, 127.03636666`입니다.

### Virtual Console 뒷좌석

- 앱 **연동·테스트 → 차량 신호 테스트 입력 끄기**. 속도 읽기 권한 요청이 나오면 허용합니다.
- 브라우저에서 `http://127.0.0.1:8088/` → **Emulation Controls → Convenience → Seat Occupancy**를 엽니다.
- **뒷좌석 왼쪽**, **뒷좌석 오른쪽**을 각각 `Empty → Occupied → Empty`로 변경합니다.
- 앱 **좌석·조건**과 `DochagjariPleosSeat` 로그에서 같은 좌석의 상태·옵션 제안이 바뀌는지 확인합니다. 앱 재실행 후 초기값도 확인합니다.
- 좌석은 Pleos `getSeat()`의 capability·초기값·변경 콜백을 사용합니다. 정차 판단은 AAOS 속도와 기어 P를 모두 확인합니다. 둘 중 하나라도 미수신이면 **주행 상태 미확인**으로 표시합니다.
- **Console 조작 성공**, **앱의 SDK 이벤트 수신**, **실차 센서 검증**은 서로 다른 검증입니다. 권한 오류·미지원 메시지는 우회하지 않고 기록합니다.

## 실제 연동 없이 시연

기본값은 **차량 테스트 입력 + 모의 내비 + 모의 발송**입니다. 화면에 입력 방식을 표시합니다.

1. **도착 안내:** 학원 픽업 ↔ 주차, 이용 불가 → 우회, 모든 후보 제외 → 후보 없음을 확인합니다.
2. **좌석·조건:** 카시트 문 방향·유모차·휠체어·동행·차량 높이를 저장합니다. `저장 중 → 저장 완료`를 확인합니다. 실패하면 `설정 다시 저장`으로 재시도합니다. **연동·테스트**에서 좌우 점유를 바꾸면 저장 옵션 적용을 제안합니다. 저장 완료된 옵션은 재실행 후 유지되고, 테스트 신호는 실제 차량 신호로 표시되지 않습니다.
3. **병원:** 하차 후 주차 ↔ 함께 주차를 비교합니다. 보호자 동행 유지 시 먼저 내려두는 후보가 제외됩니다. 무단차 미확인 후보는 유모차·휠체어의 필수 조건을 충족하지 않습니다.
4. **영상 탐색:** 동봉 **생성 빈 구간**, **생성 장애물·C12**, **공개 장애물 영상**을 분석하거나 직접 촬영 영상을 선택합니다. 실제 TFLite 탐지·OCR을 실행하며 상태 입력 버튼과 구분합니다. 생성 영상의 기준 비교는 해당 생성 카메라 구도에서만 사용합니다. 공개 영상은 보행자 장애물 샘플입니다.
5. **만남·알림:** 시연 좌표를 채우거나 실제 정차 좌표·단서를 입력하고, 표시된 대상 ID·발송 방식을 확인해 사전 동의를 켠 뒤 **여기서 만나요 · 위치 확인**을 누릅니다. 별도 발송 버튼 없이 모의 알림이 한 번 기록됩니다. 확정 후 위치·대상·방식은 고정되고, 반복 정차·재실행에도 같은 세션으로 복구합니다. 미해결 알림을 새 세션으로 재발송하지 않습니다.
6. **주행/미확인** 입력에서는 설정·영상 결과·위치 확인이 제한됩니다. **권한 없음**, **미지원**, **저신뢰**, **만료** 입력도 재현합니다. 영상 관측은 30초 후 미확인입니다. 빈 영상으로 도로 정차 허용을 추론하지 않습니다.

공유 QR에는 사용자 확인 위치와 만남 단서만 담습니다. 휴대폰에서 지도 링크·문구를 확인해 카카오톡 대상을 직접 선택합니다. 차량에서 공유 앱 열기 성공은 실제 메시지 수신을 뜻하지 않습니다. 순정 외부 카메라, 자동 실내 측위, 실제 병원 무단차 전체 경로는 현재 확인되지 않았습니다.

## 알림 서버

```bash
python3 -m venv .venv
.venv/bin/pip install -r server/requirements.txt
cp server/.env.example server/.env
# server/.env에서 토큰과 사전 동의한 테스트 수신자 ID를 설정합니다.
set -a
. server/.env
set +a
.venv/bin/uvicorn server.app:app --host 127.0.0.1 --port 8000
```

앱 **연동·테스트 → 알림 서버 사용**을 켜고 URL `http://10.0.2.2:8000`, 서버 토큰, 수신자 ID를 입력합니다. 기본값은 **서버 모의 발송**입니다. 실제 SENS 서버에 연결하는 경우에만 해당 모드도 켭니다. localhost 개발 연결만 HTTP를 허용하며 원격 서버에는 HTTPS를 사용합니다. 토큰은 앱 메모리에만 유지합니다.

- `POST /v1/arrivals`: Bearer 인증, UUID 세션, 사전 등록 수신자 ID, 확인한 좌표·단서, 정차·위치 확인 여부, 입력 출처, `delivery_mode`를 받습니다. 요청 모드가 서버 설정과 다르면 409입니다.
- `GET /v1/arrivals/{session_id}/{recipient_id}`: 같은 인증으로 처리 결과를 조회합니다.
- 세션을 SQLite에서 원자적으로 예약합니다. 같은 내용은 기존 결과를 반환하고 수신자·내용·모드가 달라지면 409입니다. 수신자별 24시간 5건 제한이 있습니다. 이미 사용한 수신자 ID를 다른 전화번호에 재할당하지 않습니다.
- `mock`는 외부 공급자를 호출하지 않습니다. 앱의 기본 모의 발송도 서버를 호출하지 않습니다.
- SENS를 사용할 때만 `SMS_MODE=sens`, API 자격증명, 등록 발신번호, 실제 동의한 수신번호를 서버 환경에 설정합니다. 테스트 차량 입력으로 실제 문자를 검증할 때는 `ALLOW_TEST_INPUT_SMS=true`도 필요합니다. 비밀값·전화번호를 앱이나 저장소에 넣지 않습니다.
- SENS는 위치 링크가 포함된 긴 문구를 LMS로 요청합니다. `accepted`는 공급자 수락이며 **휴대폰 수신 확인과 다릅니다**. 실제 휴대폰 수신은 별도 기록합니다.
- 앱은 요청 전에 좌표·단서·수신자 ID·발송 방식·입력 출처·동의 범위를 원자적으로 저장합니다. 최초 전송과 복구 모두 GET부터 실행하고 지문·모드를 대조합니다. 기록이 없고 기존 요청·동의가 유효할 때만 같은 ID·내용으로 POST합니다.
- 타임아웃·프로세스 종료로 결과가 불명확하면 `unknown` 또는 `pending`을 유지하며 자동 재발송하지 않습니다. 이미 확인한 서버 기록이 사라져도 전송을 중지합니다. 오류 시 새 세션을 반복 생성하거나 DB를 삭제하지 말고 같은 ID의 서버·공급자 기록을 확인합니다.
- 서버 요청 복구에 필요한 토큰이 재시작 후 없으면 **만남·알림 → 설정 필요**로 표시합니다. **연동·테스트**에서 같은 서버의 토큰을 입력한 뒤 **발송 결과 확인·복구**를 실행합니다. 토큰 입력으로 저장된 수신자·위치·발송 방식이 바뀌지 않습니다. 동의를 해제하면 기록에도 반영해 조회만 수행하며, 이미 시작된 전송은 취소할 수 없습니다.
- 이전 v0.1 기록은 빠진 좌표·대상을 추측해 복원하지 않습니다. 구형 앱 저널·DB 처리, 동의 해제와 불명 상태의 한계는 [복구 계약](docs/11-arrival-recovery.md)을 참조하세요.

## 검증 명령

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
.venv/bin/python -m unittest discover -s server/tests -v
# 부팅된 기기에서 추천 UI·영상 모델·설정 및 알림 저널 검증
ANDROID_SERIAL="$DOCHAGJARI_SERIAL" ./gradlew :app:connectedDebugAndroidTest
```

프로세스 강제 종료 검사는 **전용 테스트 AVD**에서만 실행합니다. 아래 스크립트는 선택한 기기의 도착자리 앱 데이터를 초기화하므로 기존 설정·픽업 기록을 보존할 기기에서는 실행하지 않습니다.

```bash
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
adb -s "$DOCHAGJARI_SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk
adb -s "$DOCHAGJARI_SERIAL" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
python3 scripts/process_restart_smoke.py --serial "$DOCHAGJARI_SERIAL" --reset-test-data
# 예상: 서로 다른 PID에서 OK (1 test) 두 번, 마지막 PASS: process restart ...
# 기록: app/build/reports/process-restart.txt
```

이 검사는 토큰 없는 서버 모의 요청을 영속 저장하고 `am force-stop` 전후 좌표·수신자·모드·동의·저장 완료 프로필을 비교합니다. 서로 다른 PID와 전체 확정 요청의 동일 SHA-256 지문도 확인합니다. 실제 공급자를 호출하지 않습니다. 일반 `connectedDebugAndroidTest`에서는 단계 인자가 없는 두 프로세스 전용 케이스를 건너뛰고 위 스크립트가 각각 실행합니다.

CI는 단위 테스트·lint·debug APK·서버 테스트와 별도의 **일반 Android API 34 에뮬레이터** 작업을 정의하고 APK·기기 보고서·프로세스 검사·화면을 artifact로 보관합니다. 실제 성공 여부와 실행 링크는 [검증 기록](docs/08-verification.md)을 확인하세요. 일반 Android의 모의 입력 검증은 Pleos SDK·Virtual Console·실차 연동 검증과 다릅니다. 기기 테스트 APK의 빌드와 기기 테스트의 **실행 완료**도 구분합니다.

Cloud 소프트웨어 AVD에서는 ADB 연결 후에도 `package`·`activity` 서비스 준비와 부팅 완료를 확인하지 못했습니다. APK 설치·내비 핀·Virtual Console 검증은 완료로 표시하지 않았습니다. GitHub KVM의 보완 실행에서는 공식 이미지 전송·체크섬 검증·에뮬레이터 시작과 `Boot completed` 로그를 확인했지만, ADB의 부팅 속성·package/activity 준비를 확인하지 못해 설치하지 않았습니다. 첫 진단이 ADB 오류 출력을 누락해 원인을 구분할 수 없으며 후속 진단 코드를 보완했습니다. Pleos workflow는 수동 실행으로 전환했고 추가 부팅은 실행하지 않았습니다. 자세한 시도와 오류는 [검증 기록](docs/08-verification.md)을 참조하세요. 맥북에서는 위의 공식 ARM64 AVD 생성 절차로 이어서 확인할 수 있습니다.

## 제출용 release 서명

이미 보관 중인 제출 키가 있으면 그 키를 사용합니다. 처음 키를 만드는 경우 사용자 맥북의 개인 경로에서 아래 명령을 실행하고 비밀번호는 대화형으로 입력합니다. 키와 비밀번호는 저장소·artifact에 넣지 않고 이후 업데이트용으로 보관합니다. 이번 Cloud 작업에서는 제출 키 생성·아래 서명을 실행하지 않았습니다.

```bash
mkdir -p "$HOME/.dochagjari-private"
chmod 700 "$HOME/.dochagjari-private"
"$JAVA_HOME/bin/keytool" -genkeypair \
  -keystore "$HOME/.dochagjari-private/submission.jks" \
  -alias dochagjari-submission -keyalg RSA -keysize 3072 -validity 10000
```

```bash
export DOCHAGJARI_KEYSTORE="$HOME/.dochagjari-private/submission.jks"
export DOCHAGJARI_KEY_ALIAS=dochagjari-submission
# DOCHAGJARI_STORE_PASSWORD, DOCHAGJARI_KEY_PASSWORD는 비밀 환경 변수로 제공
./gradlew :app:assembleRelease
"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --verbose app/build/outputs/apk/release/app-release.apk
shasum -a 256 app/build/outputs/apk/release/app-release.apk
```

키가 없으면 `app-release-unsigned.apk`만 생성합니다. 이를 서명된 제출 APK로 부르지 않습니다. 서명 후 새 해시·키 지문·코드 커밋·버전을 기록하고 제출 전 새 Pleos AVD에서 서명 APK를 설치해 목적지 핀·좌석·영상·휴대폰 수신을 다시 검증해야 합니다.

[기획 문서](docs/01-product-plan.md) · [개발계획](docs/07-development-plan.md) · [검증 기록](docs/08-verification.md) · [알림 복구 계약](docs/11-arrival-recovery.md) · [외부 모델·영상 출처](THIRD_PARTY_NOTICES.md)
