# 구현·검증 기록

2026-11-11 UTC · 기준: [개발계획](07-development-plan.md)

## 최신 진행 — 2026-11-11 23:01 UTC

- 시작 실패43095641582의 공개 실행 화면에서 GitHub unexpected error/request ID를 확인했습니다([annotation](evidence/workflow-startup-annotation.json)). 이 새 근거의 재시도43105761433은 실제 runner에서 실행됐습니다. 내부 상세 원인이나 전역 장애를 단정하지 않습니다.
- **확인된 ADB 접근 차단:** [43105761433](https://github.com/leemh-dev/dochagjari/actions/runs/43105761433)의 get-state rc1/stderr device unauthorized, devices 목록 emulator-5554 unauthorized. [준비 JSON](evidence/pleos-adb-unauthorized/readiness.json). 속성·서비스는 transport 단계에서 중단돼 미조회입니다. vendor crash/RAM을 이 접근 실패의 원인으로 주장하지 않습니다. 설치/SDK/Console은 생략했습니다.
- 정상 승인 화면을 관찰하는 GUI 경로를 추가했습니다. 새 전용 AVD·현재 호스트 공개 지문·USB 디버깅 문구·명확한 허용 버튼을 확인할 때만 클릭하며 키/인증/권한을 우회하지 않습니다. 새 화면 판단5건은 [로컬 통과](evidence/debugging-dialog-tests.txt). 기존 진단7건과 합계12건이며 Android 기기 테스트와 다릅니다.
- 첫 GUI43110281026은 launcher rc127/실제 대기5초/승인 시도0으로 끝났습니다([JSON](evidence/pleos-gui-launch-failure/readiness.json)). 타임아웃이나 지문 불일치로 분류하지 않습니다. 이후 loader 메시지 수집·SDK 번들 경로·필수 GUI 라이브러리·이미지 다운로드 전 ldd 검사를 보완했습니다. [Cloud 읽기 검사](evidence/cloud-gui-library-preflight.json)는 성공했지만 VM을 시작한 검사는 아닙니다.
- [GUI 실행43111928160](https://github.com/leemh-dev/dochagjari/actions/runs/43111928160)은 실제 창/정상 USB 승인 문구/현재 호스트 지문까지 일치했지만 허용 버튼 식별 실패로 클릭0·unauthorized에서 종료됐습니다. [원본 화면](evidence/pleos-normal-ui-blocked/debugging-screen-12.png)에서 안내 모달과 한국어 버튼을 확인해 정상 모달 닫기/부분 OCR을 보완했습니다. [읽기 전용 분석](evidence/observed-dialog-region-check.json)은 기기 승인 성공이 아닙니다. 설치·SDK·Console·Pleos 기기 검사는 아직 미검증입니다. 최신022160c 일반 Android PR43112125115·push43111927279 CI는 모두 성공했습니다.
- 일반 Android [0f21ead CI43106443044](https://github.com/leemh-dev/dochagjari/actions/runs/43106443044) 및 동일 앱 코드431ffb6의 [push CI43110280035](https://github.com/leemh-dev/dochagjari/actions/runs/43110280035)는 전체 성공했습니다. 같은431ffb6의 PR CI43110288580은 SDK Emulator ZIP 설치 오류로 기기 시작 전에 실패했고 [실패 증거](evidence/generic-ci-sdk-download-failure.txt)를 보존합니다. 앱 테스트 실패나 Pleos 성공으로 계산하지 않습니다.
- [앱/서버/Gradle 소스 비교](evidence/application-source-unchanged.txt)에 변경이 없어 배포 APK0.2.0/code2·기존 artifact/해시를 유지합니다. CI 검증용 임시 APK는 [별도 identity](evidence/pleos-gui-launch-failure/verification-apk-identity.txt)로 구분합니다. 제출 서명·실제 SMS·실차는 여전히 미검증입니다.

## 후속 ADB 진단 — 2026-11-11 20:33 UTC

- PR#1은 open/미병합이며 기준 `a020643`의 [PR CI](https://github.com/leemh-dev/dochagjari/actions/runs/43062184213)와 push CI가 모두 성공한 것을 이번에 확인했습니다. 아래 Android JVM33·서버14·기기6·프로세스2단계는 이 기존 완료 증거이며 이번에 반복 실행하지 않았습니다.
- `fcfbfc0`에서 transport/boot/package/activity를 개별 명령·종료 코드·stdout/stderr로 검사하는 `scripts/pleos/adb_readiness.py`를 추가했습니다. 준비 기록이 true여도 설치 직전에 실제 상태를 다시 검사합니다. 미준비면 설치·실행을 차단합니다.
- 새 진단 검사 **7건 로컬 통과**: [로그](evidence/adb-readiness-tests.txt). 이 중 두 건은 독립 Python 프로세스의 실제 ui_probe를 실행해 과거 성공 JSON + 현재 기기 없음, 과거 실패 JSON에서 설치 명령이 호출되지 않음을 확인합니다. ADB는 대역이며 Pleos 기기 실행 성공이 아닙니다.
- 현재 Cloud에 KVM/연결 기기가 없고 실제 CLI에서 [device not found](evidence/cloud-adb-readiness.json)를 받았습니다. 설치·소프트웨어 부팅을 실행하지 않았습니다.
- 누락됐던 ADB 오류를 수집하려고 a020643을 [수동 진단 43095641582](https://github.com/leemh-dev/dochagjari/actions/runs/43095641582)으로 요청했습니다. 20:32에는 queued/job0였으나 **20:35:56 UTC에 startup_failure**로 종료됐습니다. job0·artifact0이며 `--log-failed`도 log not found입니다. [대기 스냅샷](evidence/followup-actions-queue.json)과 [종료 증거](evidence/followup-actions-startup-failure.json)를 구분합니다. 이번 새 에뮬레이터 실행·다운로드·설치·SDK 조사는 없었습니다.
- GitHub CLI의 “workflow file issue일 가능성” 안내는 일반 힌트입니다. 공식 actionlint1.7.7로 이전 a020643 workflow와 현재 workflow 전체를 검사해 둘 다 exit0/진단0을 확인했습니다([검사 기록](evidence/workflow-static-check.txt)). 정적 검사가 실행 권한/스케줄링/런타임 성공을 보장하지 않으며 startup 원인은 아직 미확인입니다. 같은 부팅을 재요청하지 않았습니다.
- 별도 [준비 기록 회귀 재현](evidence/adb-stale-record-regression.txt)에서 실제 이전/현재 ui_probe Python 프로세스와 대역 ADB를 실행했습니다. 이전은 과거 true JSON만 보고 install을 시도했고, 수정 후는 get-state 실패에서 설치를 차단했습니다. 이 버그를 기존 Pleos 연결 실패의 원인으로 주장하지 않습니다. 새 CI는 아직 queued이므로 새 HEAD의 CI 성공으로 표시하지 않습니다.
- [공식 설정 비교](evidence/pleos-profile-comparison.json): devices.xml RAM4GiB/source.properties8192MB가 공존합니다. 원본 userdata 약1.1GB와 별도 빈 테스트 userdata2GiB도 다릅니다. 비교 부팅을 하지 않았고 메모리/데이터 차이를 원인으로 단정하지 않습니다.
- 이번에는 앱 코드를 바꾸지 않아 APK를 재빌드·재배포하지 않았습니다. v0.2.0 코드 `3a01515`·기존 artifact·아래 해시는 유지됩니다. SMS/수신자 설정·제출 키 환경 변수가 없고 server/.env도 없어 실제 SMS와 제출 서명은 실행하지 않았습니다.
- [제출 체크리스트](12-submission-checklist.md)와 README의 설치 전 자동 검사·ADB 상태별 재현 절차를 정리했습니다.

## v0.2.0 복구·설정 저장 보완

- 코드 커밋: `3a01515` (배포 빌드 기준), `5ef9aab` (프로세스 검증), `44f0cb4` (Android 프레임 호환), `702432c` (알림·저장·기기 CI), `6616f01` (서버 계약), `4a40a24` (수정 전 실패 재현).
- 현재 버전: **0.2.0 / 2**, API 34+, ARM64·x86_64. [개발용 패키징 artifact 실행](https://github.com/leemh-dev/dochagjari/actions/runs/43055801516/artifacts/13871881801)인 `development-release-assets-and-metadata`에 debug·기기 테스트·unsigned release와 metadata·SHA256SUMS를 보관합니다. 제출용 서명 release가 아닙니다. Release API는 Cloud 401·runner 403으로 게시가 제한되어 artifact를 제공합니다. [실패와 해결 증거](evidence/apk-publishing-limits.txt).
- [APK 메타데이터](evidence/v0.2-apk-metadata.json): 코드 커밋·버전·바이트 크기·각 해시·debug 서명 지문을 기록합니다. debug APK SHA-256은 `d1a2761d0dac84142b4ff8917f82d6737d3d4e1e30520dd8895e8454e4c85e4d`입니다.
- `:app:testDebugUnitTest :app:assembleDebugAndroidTest :app:lintDebug` — **33건, 실패 0건**. [요약](evidence/recovery-unit-tests/summary.json)과 같은 폴더의 JUnit XML을 제공합니다. Robolectric/API 34 사례는 Android 환경을 시뮬레이션한 JVM 검사이며 실제 기기 검사가 아닙니다.
- `python -m unittest discover -s server/tests -v` — **14건 통과**, [서버 로그](evidence/recovery-server-tests.txt). 공급자는 테스트 대역이며 실제 SENS 수락·문자 수신을 뜻하지 않습니다.
- `:app:assembleDebug :app:assembleRelease` — 코드 커밋 후 빌드 성공. debug·기기 테스트 APK 서명 검증 성공, unsigned release 생성. 게시 artifact를 다시 다운로드해 세 해시·APK 버전/ABI·개발 서명도 확인했습니다([재검증 기록](evidence/v0.2-downloaded-apk-verification.txt)). runner 서명은 기존 Cloud 키와 달라 새 테스트 AVD에서 설치합니다. 제출용 키는 제공되지 않았습니다.
- 이전 AppModel에서 확인 좌표 소실·ViewModel 종료 시 쓰기 취소 두 건을 먼저 재현했습니다. [수정 전 실패](evidence/restart-regression-before.xml)와 수정 후 회귀 검사를 구분해 보존합니다.
- 구현·검증한 복구 동작: 확정 요청/동의 복원, GET 선조회, 404에서 동일 내용 재개, 응답 유실 후 조회, 동시 복구, 모드·내용 불일치 차단, 관측한 기록 소실 시 중지, 동의 해제, 토큰 미저장, 구형 기록 보존, 저장 실패 시 미전송.
- 구현·검증한 프로필 동작: 저장 중/완료/실패, 순서 보장, 실패 후 재시도, ViewModel 종료 후 완료, 실제 DataStore를 닫고 새 인스턴스에서 완료 값 재조회. OS 프로세스 강제 종료 검증과는 별도로 기록합니다.
- 일반 Android API 34 Google APIs/KVM에서 `connectedDebugAndroidTest` **6건 통과**, 별도 프로세스 종료/복구 **2단계 통과**: [최신 검증 PR CI](https://github.com/leemh-dev/dochagjari/actions/runs/43057154160). 연결 명령에서 건너뛰는 단계 전용 2건은 별도 스크립트가 각각 실행했습니다. 이전 오류와 테스트 기대값 수정은 개발 일지에 보존합니다.
- 최초 Android 실행에서 영상 2건이 ARGB_8888 형식 오류로 실패했습니다. [실패 XML](evidence/android-device-before-bitmap-fix.xml)을 보존했고 프레임 변환 후 같은 앱의 TFLite·ML Kit **C12** 인식·영상 점유 기반 우회가 실제 Android에서 통과했습니다. 호스트 Tesseract 결과를 대체 근거로 쓰지 않았습니다.

### v0.2.0 일반 Android 실제 실행

[전체 통과 CI 43052357723](https://github.com/leemh-dev/dochagjari/actions/runs/43052357723), 코드 `5ef9aab`. 이후 설치 전 boot_completed/package/activity 준비 검사를 추가한 [CI 43054503112](https://github.com/leemh-dev/dochagjari/actions/runs/43054503112), 최신 문서 커밋 `8827d3c`의 [PR CI 43057154160](https://github.com/leemh-dev/dochagjari/actions/runs/43057154160)도 전체 성공했습니다. Google APIs API 34 x86_64/KVM이며 Pleos AVD가 아닙니다. [JUnit XML](evidence/android-api34-passed.xml), [프로세스 종료 로그](evidence/android-process-restart-passed.txt), [요약](evidence/android-api34-summary.json), [재실행 화면](evidence/android-api34-app.png)을 보존합니다.

| 기능 | 실제 실행한 범위 | 별도 미검증 |
| --- | --- | --- |
| 학원·우회 | Compose UI에서 픽업↔주차·이용 불가 우회, 앱 모델에서 점유→주차→후보 없음 | 순정 내비 목적지 핀 |
| 병원 | 같은 Android 앱 모델에서 휠체어·동행 후보 변경, 무단차 미확인 후보 제외 | 실제 병원 동선 |
| 영상 | 앱의 TFLite/ML Kit로 생성 빈/장애물·공개 영상 분석, **C12** 인식. 영상 URI 입력→점유→우회 | 실제 외부 카메라·실차. URI 검사는 공개 샘플을 캐시에 복사해 입력했으며 시스템 파일 선택 UI까지 자동화한 검사는 아님 |
| 저장 | 실제 DataStore 완료값 대기, 모델 재생성 및 프로세스 종료 후 좌우 옵션·유모차·휠체어·동행·높이 2.2 복원 | 저장 중 종료는 마지막 완료값까지만 보장. 저장 실패/연속 변경은 JVM 대역·실제 DataStore 회귀 검사로 별도 검증 |
| 알림 | 앱 모의 완료·저널 복원. 토큰 없는 SERVER_MOCK 확정 요청을 PID **7047→7098**에서 복원, 전체 요청 지문 동일 | Android에서 실제 서버·SENS 호출/휴대폰 수신. HTTP 복구/응답 유실/동시성은 JVM HTTP 대역·서버 테스트로 검증 |
| 주행 제한 | 테스트 신호의 주행/미확인에서 설정·위치·분석 변경 차단 | 실제 차량 속도·기어 신호 |
| 좌석 | 테스트 입력·프로필 저장 구현 및 일반 Android 실행 | Pleos 초기값·Console 좌우 변화·앱 수신·실차는 각각 별도 확인 |

연결 검사는 **6건 성공**, 단계 없는 프로세스 전용 2건은 skip한 뒤 별도 스크립트에서 **2단계 모두 성공**했습니다. `am force-stop` 전후 서로 다른 PID와 동일한 요청 SHA-256을 확인했으며, 이 시나리오에는 토큰이 없어 서버/공급자 호출은 없습니다. 알림 발송 성공으로 계산하지 않습니다.

복구 계약과 이전 앱/DB 호환 처리는 [자동 알림 복구 계약](11-arrival-recovery.md)을 참조하세요. **Pleos 내비 핀·Virtual Console 좌석 수신·실차·실제 휴대폰 수신은 현재도 미검증**입니다.

### v0.2.0 Pleos 별도 실행

기존 Cloud의 무가속 부팅 실패를 먼저 읽었고, 일반 Android CI에서 KVM 접근이 확인되어 **다른 실행 조건**으로 조사했습니다. [첫 KVM 증거 실행](https://github.com/leemh-dev/dochagjari/actions/runs/43052661853)은 KVM version 12 usable까지 확인한 뒤 공식 이미지 다운로드 8개 구간 중 7개를 완료했으나 남은 연결이 reset되어 종료됐습니다. **에뮬레이터는 시작하지 않았습니다.** [readiness JSON](evidence/pleos-kvm-download-failure/readiness.json), [앱 조사 미실행 JSON](evidence/pleos-kvm-download-failure/app-probe.json).

실패 원인이 구간 전송이므로 `77abd8e`에서 64MB 구간·동시 4개·제한된 재시도를 추가했습니다. [보완 실행 43056602693](https://github.com/leemh-dev/dochagjari/actions/runs/43056602693)의 결과:

- **전송·실행 확인:** 61/61 구간 완료, 공식 크기/체크섬 검증 후 추출·별도 테스트 데이터 포맷·KVM 에뮬레이터 시작. [단계 로그](evidence/pleos-kvm-readiness/transfer-and-boot-stages.txt).
- **부팅 로그 관찰:** Emulator 37.2.13이 `Boot completed in 31156 ms`를 출력했습니다. `ai.pleos.hardware.profile` native process crash도 관찰했습니다. 이것이 ADB 확인 실패의 원인인지는 확인하지 못했습니다. [선별 부팅 로그](evidence/pleos-kvm-readiness/boot-diagnostics.txt).
- **ADB 준비 미확인:** 900초 동안 `sys.boot_completed=1`·package/activity 서비스 준비를 확인하지 못했고 마지막 stdout은 비어 있었습니다. 이 버전의 진단은 ADB returncode/stderr/devices 목록을 누락했으므로 **프레임워크 미준비와 ADB 연결 실패를 구분할 수 없습니다.** [readiness JSON](evidence/pleos-kvm-readiness/readiness.json).
- **설치·연동 미실행:** APK 설치·Pleos 기기 검사·SDK 목적지 요청·실제 내비 핀·Console 조작·앱 좌석 초기값/변경/옵션 제안은 실행하지 않았습니다. [조사 생략 기록](evidence/pleos-kvm-readiness/app-probe.json). 일반 Android 결과로 대체하지 않습니다.
- **후속 진단 보완:** 준비 검사에 ADB returncode/stderr·devices 목록을 추가하고 포트 5554를 명시했습니다. 이 보완 후 추가 부팅은 실행하지 않았습니다. 반복 자동 실행을 막기 위해 Pleos workflow는 수동 실행으로 전환했습니다.
- **환경 차이:** 이번 AVD는 4GB/2core/SwiftShader·별도 빈 테스트 데이터입니다. 내려받은 Linux 이미지 source.properties의 RAM 기본값은 8192MB였습니다. 원인으로 단정하지 않고 공식 기본 프로필·원본 데이터 구성을 확인할 후속 항목으로 남깁니다.

맥북에 접근할 수 없어 실행하지 않았습니다. [README](../README.md)의 공식 ARM64 AVD에서 실제 부팅 속성·ADB 오류/devices·두 서비스를 확인한 뒤 설치/내비/좌석 검증을 이어가야 합니다. 실차·실제 문자 수신·제출 서명은 여전히 미검증입니다.

## v0.1.0 최초 코드와 APK (이전 이력)

아래 표와 실행 기록은 첫 APK 개발 당시의 이력입니다. v0.2.0의 최신 확인 범위는 위 절과 이후 기기 결과를 우선합니다.

- Android 구현: `0a04f40` (`feat/android-submission-mvp`). 이후 변경은 서버·호스트 검증·문서입니다.
- SMS 서버: `ae33591`. 호스트 미디어 검증: `0872124`.
- 개발 APK: [`apk/dochagjari-debug.apk`](../apk/dochagjari-debug.apk). 개발 서명이며 제출용 release 서명 APK가 아닙니다.
- debug APK SHA-256: `c72bc9d9893fd64d23942fb7b751ef7269d870962ce55f9add6d84f3541c6ae4`.
- unsigned release SHA-256: `6536145a8248777a58d1fe4871f36b9db7c309b830ea767ce07015f4e489703e` (로컬 빌드 산출물, 미서명).
- 실행 대상: API 34+, `arm64-v8a`와 `x86_64`. 앱 ID `dev.leemh.dochagjari`, 버전 `0.1.0` / `1`.
- unsigned release는 `:app:assembleRelease`로 생성했습니다. 제출용 키가 제공되지 않아 서명된 release·새 AVD 설치 검증은 남아 있습니다.

## v0.1 당시 검증 수준 (이전 이력)

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
- GitHub Actions도 [push 검사](https://github.com/leemh-dev/dochagjari/actions/runs/43032847880)와 [PR 검사](https://github.com/leemh-dev/dochagjari/actions/runs/43032862081)가 모두 성공했습니다. 검증 커밋: `2413d22`.
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
- 수동 구성한 Cloud AVD의 연장 부팅에서 `/data/misc` 암호화 정책 설정 실패와 recovery 재부팅이 나타났습니다. 공식 Emulator **35.2.10**과 초기화한 개발 AVD에서도 같은 오류를 확인했습니다. 원인을 앱 오류로 단정하지 않습니다.
- SDK 이미지를 변경하지 않고 별도의 빈 개발 데이터 파티션으로 다시 시도했을 때 앞선 암호화 오류는 나타나지 않았습니다. SurfaceFlinger·zygote·system_server 시작과 ADB 연결은 확인했으나, `package`와 `activity` 서비스가 `not found`였고 `sys.boot_completed=1`은 확인하지 못했습니다. 일부 Pleos vendor 서비스의 반복 종료도 관찰했습니다.
- 따라서 **앱 설치·실행, Android 기기 테스트, 순정 내비 핀, Virtual Console 좌석 토글 및 앱 수신은 미검증**입니다. ADB의 `device` 표시를 부팅 완료나 앱 설치 성공으로 처리하지 않았습니다. [부팅 진단 요약](evidence/cloud-boot.txt)을 남깁니다.

맥북 M1 재현 명령, ARM64 이미지 설치와 두 목적지·좌우 좌석 확인 순서는 [README](../README.md)에 있습니다.

## 제출 전 남은 확인

- Pleos AVD에서 공식 샘플과 앱을 설치해 목적지 두 곳의 실제 핀을 비교합니다.
- Virtual Console 좌우 좌석 변경과 앱 콜백을 함께 녹화하고, 저장 옵션을 재실행으로 확인합니다.
- 일반 Android의 실제 영상·ML Kit OCR은 v0.2에서 통과했습니다. Pleos 순정 카메라·실차 검증은 남아 있습니다.
- 실제 SMS는 SENS 등록 발신번호·동의 수신자를 설정한 뒤 공급자 수락과 휴대폰 수신을 각각 확인합니다.
- 실제 시설의 미확인 동선은 계속 가상 데이터와 구분하고 제출용 키로 release APK를 서명합니다.
