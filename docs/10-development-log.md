# 도착자리 개발 일지

이 문서는 주요 진행 절차의 이력입니다. 현재 검증 수준은 [검증 기록](08-verification.md), 다음 실행 지시는 [개발 프롬프트](09-next-development-prompt.md)에서 확인합니다.

## 기록 방식

- 각 주요 조사·변경·검사·실패·해결·차단 단계가 끝날 때 날짜(시간을 적으면 시간대 포함), 목적, 수행 내용, 환경·명령, 실제 결과, 증거·커밋, 남은 일을 추가합니다.
- 과거 기록을 새 실행 결과로 덮어쓰지 않습니다. 가설·코드 검토에서 찾은 위험·재현한 오류를 구분합니다. 내부 추론이나 원본 대화 전체 대신 작업 판단의 근거와 확인 가능한 결과를 적습니다.
- 기능 단위로 커밋·푸시하고 PR 설명 또는 진행 댓글에서 문서와 검사 결과를 연결합니다. 완료하지 않은 작업은 진행 중·차단·미검증으로 표시합니다.
- API 키·토큰·서명 키·전화번호·개인 이동 기록은 남기지 않습니다. 공개 가능한 작은 로그는 `docs/evidence/`, 큰 영상·APK·보고서는 GitHub artifact 또는 Release 링크로 보관합니다.

## 2026-11-11 — 첫 APK 구현 및 검증 (기존 커밋·증거에서 정리)

| 단계 | 수행 내용·실제 결과 | 근거 | 남은 일 |
| --- | --- | --- | --- |
| 기준 확인 | `docs/07-development-plan.md`와 관련 기획 문서, 공식 Pleos 샘플·SDK 문서를 확인. 문서만 있던 저장소에 Kotlin 프로젝트 구성 | 기준 문서, `0a04f40` | 실제 기기 검증 |
| Android 구현 | 학원 픽업·우회, 병원 추천, 내비·좌석 어댑터, 테스트 입력, 설정·영상·알림 UI 구현 | `0a04f40` | 화면·SDK·저장·영상의 기기 실행 확인 |
| SMS 서버 | FastAPI·SQLite·등록 수신자·인증·중복 제거·모의/SENS 어댑터 구현. 서버 테스트 8건 통과 | `ae33591`, 검증 기록 | 실제 공급자·휴대폰 수신, 앱 복구 흐름 |
| 호스트 영상 | 실제 TFLite 모델로 빈/장애물 입력 확인. 전체 프레임의 호스트 Tesseract는 C12를 읽지 못해 등록 표지 ROI로 좁혀 인식 확인 | `0872124`, [결과 JSON](evidence/host-media.json) | Android ML Kit 전체 프레임 검증과 구분 |
| 빌드·검사 | JDK 17·SDK 35 환경에서 debug·기기 테스트 APK·unsigned release 및 공식 내비 샘플 빌드 성공. Android 단위 테스트 9건 통과. 네트워크 설정 lint 오류 3건 수정 후 오류 0건 | `fd5b333`, [JUnit 결과](evidence/android-unit-tests.xml), 검증 기록 | 기기 테스트 실행·제출 서명 |
| Cloud 부팅 | KVM 없음. Emulator 37·35에서 암호화 초기화 오류. 별도 빈 데이터로 ADB·system_server 시작은 확인했으나 package·activity 준비 및 부팅 완료 미확인 | `d829a77`, [가속 검사](evidence/cloud-acceleration.txt), [부팅 진단](evidence/cloud-boot.txt) | 맥북 공식 ARM64 AVD에서 설치·내비·좌석 검증 |
| GitHub·CI | 기능 브랜치와 PR #1 생성. SDK setup action이 폐기된 `tools` 설치로 실패해 `platform-tools`만 설치하도록 수정. 이후 CI 성공 | `2413d22`, [PR #1](https://github.com/leemh-dev/dochagjari/pull/1) | 다음 구현에서도 CI 유지 |
| 최신 검사 확인 | `d829a77`의 PR workflow 결과가 `completed / success`임을 GitHub에서 재확인 | [CI 실행](https://github.com/leemh-dev/dochagjari/actions/runs/43034094191) | 이 성공은 기기 테스트 실행을 포함하지 않음 |

빌드·검사 명령과 APK 해시, 실제 연동 미검증 범위는 [검증 기록](08-verification.md)에 있습니다. 코드·설정 변경 후 해당 검사를 통과했으며, 이번 인계 문서 작성 때문에 앱 검사를 실행한 것으로 기록하지 않습니다.

## 2026-11-11 — 코드 검토 및 다음 개발 인계

- **요청:** 다음 실행 프롬프트를 준비하고 모든 주요 진행 절차를 GitHub에 기록한다.
- **확인:** PR #1은 열려 있으며 기준 head는 `d829a77`. 작업 트리는 문서 작성 전 깨끗했고, 최신 head의 CI 성공을 확인했다.
- **코드 검토에서 확인한 공백:** 앱 저널에는 확인 좌표·수신자·발송 방식이 없고 서버 결과 조회 경로가 없다. 프로필은 화면 상태가 먼저 갱신되고 DataStore 저장은 ViewModel 코루틴에서 실행된다. 저장 완료 전 종료 위험은 아직 기기에서 재현한 오류가 아니다.
- **다음 순서:** 알림 복구 → 저장 완료 보완 → Android 기기 테스트 → Pleos 내비·좌석 → 준비된 경우 실제 SMS·제출 서명.
- **변경:** 다음 대화용 실행 프롬프트, 개발 일지, 저장소 작업 지침과 README 연결 추가. 앱 코드·APK는 변경하지 않는다.
- **검증:** 문서 링크와 경로를 검사하고 `git diff --check`를 수행한다. 커밋·푸시 후 PR #1에 인계 댓글로 문서와 커밋을 연결한다.
- **미검증 유지:** APK 설치·기기 테스트 실행·내비 핀·좌석 이벤트·앱 내 영상·SMS 수신·실제 병원 동선·release 서명. 기존 Cloud 부팅 시도는 이번 문서 작업에서 재실행하지 않는다.

## 2026-11-11 14:47 UTC — 복구 개발 시작 및 재현 준비

- **기준 확인:** PR #1이 열린 상태이며 head `0cd9616`과 로컬 브랜치가 일치한다. README·07/08/09/10 문서와 AGENTS.md, 기존 Cloud 부팅 증거를 읽었다. 해당 head의 CI는 조회 시 진행 중이었다.
- **환경 확인:** 기존 SDK·JDK·Gradle 캐시는 존재하지만 `adb devices -l`에 연결 기기가 없다. KVM과 맥북 접근 경로, 실제 SMS 자격증명도 제공되지 않았다. 기존 Pleos 실패 부팅은 반복하지 않는다.
- **재현 준비:** 실제 AppModel을 대상으로 Robolectric/API 34 테스트 두 건을 추가한다. 확정 위치를 기록한 직후 worker 실행 전에 ViewModel을 종료하는 경우와, 저장 코루틴 실행 전 ViewModel 종료로 프로필 쓰기가 취소되는 경우를 확인한다. 결과는 실행 후 추가한다.
- **다음:** 재현 결과 기록 후 영속 알림 요청·결과 조회·동의 및 대상 고정, 애플리케이션 수명의 설정 저장 큐를 구현한다. 일반 Android 기기 테스트는 CI에서 별도로 실행하고 Pleos 실연동으로 표시하지 않는다.

## 2026-11-11 14:49 UTC — 기존 AppModel 재시작 오류 재현

- **명령:** `./gradlew :app:testDebugUnitTest --tests '*RestartRegressionTest'` (JDK 17, SDK 35, Robolectric/API 34, 제어된 Main dispatcher).
- **결과:** 두 회귀 테스트 모두 예상한 실패. 알림 worker 실행 전에 ViewModel을 종료하고 재생성하면 확인 좌표가 `Position(...)`에서 `null`로 사라졌다. 프로필 변경 후 쓰기 코루틴 실행 전에 ViewModel을 종료하면 저장 파일이 없었다.
- **증거:** [수정 전 회귀 테스트 XML](evidence/restart-regression-before.xml). 이는 JVM에서 실제 AppModel과 Android 환경 시뮬레이션을 사용한 재현이며 실제 기기/프로세스 강제 종료 검증은 아니다.
- **다음:** 원인별 구현 후 같은 시나리오를 새 저장소에서 재검증하고 추가 복구·동의·동시성 테스트를 실행한다. 의도적으로 실패하는 재현 커밋은 로컬 이력에 남기고 수정 커밋과 함께 푸시한다.

## 2026-11-11 15:07 UTC — 서버 복구 계약·대상 고정 검증

- **변경:** `delivery_mode`와 언어 공통 요청 지문을 추가하고 GET/POST 결과에 요청 증거를 반환한다. 같은 UUID를 다른 등록 수신자에게 사용하는 요청도 쓰기 트랜잭션에서 거부한다. 기존 DB 행을 보존하면서 nullable 모드 열을 추가했다.
- **명령·결과:** `python -m unittest discover -s server/tests -v` — 14건 통과. 공급자는 테스트 대역이며 실제 SENS 호출·휴대폰 수신은 검증하지 않았다.
- **시나리오:** 응답 유실 후 재시작 조회, 동시 중복, 예약 후 공급자 호출 전 종료, 모드 변경 방지, 수신자 재지정 방지, 구형 DB 보존, Kotlin/Python 지문 벡터.
- **수정한 검사 문제:** 기존 타임아웃 테스트가 재시작 시 서버를 SENS에서 mock으로 바꾸던 것을 동일 모드 재시작으로 고쳤다. 모드 변경 거부는 별도 검사한다. Android의 JDK 전용 HTTP 서버 클래스는 컴파일할 수 없어 MockWebServer로 교체했다.
- **근거:** [복구 계약](11-arrival-recovery.md), `server/tests/test_app.py`. 최종 검사 로그는 완료 시 evidence에 추가한다.
- **다음:** 앱 회귀 검사와 기기 테스트 APK 빌드를 마치고 일반 API 34 기기 CI를 실행한다. Pleos 실연동은 별도 미검증으로 유지한다.

## 2026-11-11 15:14 UTC — 앱 복구·저장 보완 및 로컬 검사 통과

- **변경:** enum 상태와 원자적 영속 저널, 고정된 위치/수신자/모드/동의 복원, GET 선조회·동일 요청 재개, 불명·불일치·기록 소실 시 재발송 중단을 구현했다. 토큰은 저장하지 않으며 동의 해제를 영속 반영한다. 미해결 요청의 새 세션 생성·대상 변경을 차단했다.
- **저장:** 프로필을 애플리케이션 수명의 직렬 큐에서 쓰고 저장 중/완료/실패·revision·실제 저장값을 구분했다. ViewModel 종료 후 쓰기 완료, DataStore를 닫고 새 인스턴스로 값 재조회, 실패·재시도·연속 변경을 검증했다.
- **실행:** `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug` 및 마지막 소규모 변경 후 단위 검사·기기 테스트 APK·lint 재검사 성공. Android/JVM 테스트 31건·서버 14건 통과. HTTP 테스트는 로컬 MockWebServer를 사용한다.
- **증거:** [수정 후 단위 검사 요약](evidence/recovery-unit-tests/summary.json), 같은 폴더의 JUnit XML, [서버 검사 로그](evidence/recovery-server-tests.txt). XML의 호스트명을 정규화하고 콘솔 출력을 제외했다. 실제 기기 검사 결과가 아니다.
- **기기 검사 준비:** 일반 API 34 Google APIs 에뮬레이터 CI 작업, 앱 내 영상→우회·병원·주행 제한 검사, 별도 프로세스 강제 종료 스크립트를 추가했다. 이 단계에서 실행 성공은 아직 확인하지 않았다. 스크립트는 전용 테스트 기기에만 사용하며 명시적 앱 데이터 초기화 인자가 필요하다.
- **버전:** 앱 0.2.0 / versionCode 2. 큰 APK는 이후 artifact/Release로 보관하고 이전 0.1 APK는 이력으로 유지한다. 서명·해시는 최종 빌드 후 기록한다.
- **다음:** 커밋·푸시 후 CI 기기 작업 실행 결과와 실패를 기록하고 필요한 수정 후 다시 검사한다. Pleos 핀·좌석과 실제 SMS는 여전히 별도 미검증이다.

## 2026-11-11 15:28 UTC — 일반 Android 기기에서 Bitmap 호환 오류 발견

- **환경:** GitHub Actions의 KVM 지원 일반 Android API 34 Google APIs 에뮬레이터. 기존 Cloud Pleos 소프트웨어 부팅과 다른 환경이며 Pleos 실연동은 아니다.
- **실행:** [CI 43046428938](https://github.com/leemh-dev/dochagjari/actions/runs/43046428938)의 기본 검사는 성공했지만 `connectedDebugAndroidTest`의 영상 분석·영상→우회 두 사례가 실패했다. 프로세스 강제 종료 스크립트는 앞 단계 실패로 실행되지 않았다.
- **실제 오류:** Android 디코더 Bitmap에 대해 TensorImage가 `Only supports loading ARGB_8888 bitmaps` 예외를 냈다. 후속 우회 검사에서는 관측값이 생성되지 않았다. 호스트 모델 검사만으로는 발견하지 못한 앱 런타임 문제다.
- **수정:** 모델/OCR 입력 전에 RGB_565·하드웨어 프레임을 소프트웨어 ARGB_8888로 변환하고 원본을 해제한다. 픽셀·크기 유지와 호환 프레임의 불필요한 복사를 막는 회귀 검사를 추가했다.
- **다음:** 로컬 회귀·lint·APK 빌드와 CI 기기 검사를 다시 실행한다. 성공으로 갱신하기 전까지 앱 내 영상·기기 프로세스 검증은 실패/미검증으로 유지한다.

## 2026-11-11 15:39 UTC — 가속 환경을 활용한 Pleos 증거 수집 준비

- **근거:** 일반 Android 기기 CI에서 `/dev/kvm` 사용과 부팅·앱 설치가 확인됐다. 이는 KVM이 없던 기존 Cloud와 다른 실행 조건이므로 별도 Pleos 증거 수집 작업을 추가한다. 동일한 소프트웨어 부팅을 반복하는 시도가 아니다.
- **구성:** 가속 검사 성공 후에만 공식 Linux 이미지 내려받기·체크섬 확인·희소 추출을 수행한다. SDK 이미지는 바꾸지 않고 별도 테스트 데이터 파티션을 사용한다. 부팅 완료와 package/activity 준비를 확인한 뒤 앱을 설치한다.
- **수집 범위:** 앱 접근성 노드로 실제/모의 스위치를 확인하고 내비 두 지점 요청·SDK 로그·화면을 수집한다. 공식 Virtual Console의 Seat Occupancy 카드가 안전하게 식별될 때만 뒷좌석 좌우를 조작하고 실제 SDK 변화와 대조한다. 식별 실패·SDK 오류·미확인 핀은 그대로 남긴다.
- **주의:** 증거 수집 workflow 성공만으로 내비 핀·좌석 수신 성공을 선언하지 않는다. 이미지·키를 artifact에 넣지 않으며 공개 가능한 준비 상태·선별 로그·테스트 화면만 보관한다. 실제 SMS나 실차는 사용하지 않는다.
- **검사:** Python 문법·Node 구문·`git diff --check` 통과. 실제 Pleos 실행 결과는 아직 확인하지 않았다.
- **기존 기기 실패 증거:** GitHub artifact를 연결 도구로 다운로드해 [수정 전 Android 검사 XML](evidence/android-device-before-bitmap-fix.xml)을 보존했다. CLI의 임시 다운로드 주소 접근 실패는 연결 도구의 재사용 파일 참조로 해결했으며 임시 서명 주소는 문서에 남기지 않는다.

## 2026-11-11 15:47 UTC — 앱 내 영상 검사 통과·프로세스 검사 기대값 수정

- **실제 기기 결과:** [Bitmap 수정 CI](https://github.com/leemh-dev/dochagjari/actions/runs/43049645888)의 `connectedDebugAndroidTest`가 성공했다. UI·픽업/우회·병원·주행 제한·설정·세 영상 모델/OCR·영상 URI→점유→우회 여섯 사례가 통과했고, 단계 인자가 없는 프로세스 전용 두 사례는 별도 스크립트 대상으로 건너뛰었다.
- **남은 실패:** 이어진 프로세스 스크립트의 복원 기대값이 잘못됐다. 준비 프로필은 보호자 동행을 켜서 주차장 좌표를 확정했는데 검증 단계가 승하차점 좌표를 기대했다. 앱이 주차장 좌표를 복원한 것을 알림 좌표 변경 오류로 단정하지 않는다.
- **수정:** 등록된 주차장 지점을 기대값으로 사용하고, 강제 종료 전후 서로 다른 PID와 전체 확정 요청의 SHA-256 지문 일치를 함께 비교한다. 로컬 스크립트 구문·기기 테스트 APK 컴파일을 확인한 뒤 다시 CI를 실행한다.
- **미검증:** 전체 프로세스 검사의 최종 PASS와 별도 KVM Pleos 결과는 아직 확인하지 않았다. 실제 휴대폰 수신은 계속 미검증이다.

## 2026-11-11 15:52 UTC — Pleos 대용량 전송·진행 증거 보완

- **변경:** 약 4GB 공식 이미지의 Range 지원을 먼저 확인하고 8개 구간으로 내려받은 뒤 전체 크기·공식 체크섬을 검증한다. 전송·추출·포맷·부팅 단계와 30초 주기의 준비 상태를 즉시 JSON/로그로 남겨 종료 시에도 진행 근거를 보존한다. SDK 내용은 변경하지 않는다.
- **근거:** 이전 Cloud 전송에서 단일 연결 속도가 병목이었고 같은 공식 호스트의 범위 요청으로 다운로드를 완료한 이력이 있다. 이전 CI의 이미지 준비 단계를 이 전송 방식으로 교체하며, 취소한 실행을 Pleos 부팅 실패로 기록하지 않는다.
- **검사:** Python 구문 검사와 KVM 없는 현재 Cloud에서의 사전조건 검사를 통과했다. 이 경우 이미지 다운로드·에뮬레이터 실행을 모두 생략했음을 확인했다. KVM에서의 실제 SDK·Console 결과는 여전히 별도 확인 대상이다.

## 2026-11-11 16:03 UTC — 실제 Android 프로세스 종료·재시작 검증 통과

- **환경/명령:** GitHub KVM의 일반 Google APIs API 34 x86_64, `:app:connectedDebugAndroidTest`와 `python scripts/process_restart_smoke.py --serial emulator-5554 --reset-test-data`. 전용 테스트 AVD의 앱 데이터만 초기화했다.
- **실제 결과:** [CI 43052357723](https://github.com/leemh-dev/dochagjari/actions/runs/43052357723) 전체 성공. 연결 검사 여섯 건 통과, 별도 단계 케이스 두 건은 연결 검사에서 skip 후 각각 실행하여 통과했다. 프로세스 PID 7047→7098, 확정 요청 지문 일치, 양쪽 좌석 옵션·유모차·휠체어·동행·높이 2.2와 좌표/수신자/모드/동의 복원 확인. 토큰·공급자 호출은 없었다.
- **증거:** [Android XML](evidence/android-api34-passed.xml), [프로세스 로그](evidence/android-process-restart-passed.txt), [요약](evidence/android-api34-summary.json), [재실행 화면](evidence/android-api34-app.png). artifact 13870457689의 실제 결과를 다운로드해 공개 가능한 증거만 보존했다.
- **추가:** 다음 일반 Android CI에서도 설치 전에 `sys.boot_completed=1`·package·activity 서비스 준비를 명시적으로 검사하도록 했다. 기존 검사 실행/설치 성공과 구분해 이 검사 결과는 해당 후속 실행에서 확인한다.
- **범위:** 실제 Android의 생성/공개 영상·ML Kit·설정/알림 복구가 확인됐으며 Pleos 실제 SDK·Virtual Console·실차·실제 SMS·제출 서명 성공을 뜻하지 않는다. Pleos KVM 실행은 진행 중이다.

## 2026-11-11 16:07 UTC — APK 패키징·Release 전송 인증 제한

- **로컬 결과:** 코드 `3a01515` 기준 `:app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease` 성공. debug/test 개발 서명 검증 성공, release는 미서명임을 확인했다. 이 로컬 파일을 배포 성공으로 기록하지 않는다.
- **실패:** `gh release create ... --prerelease --target 3a01515`의 uploads.github.com 파일 전송이 HTTP 401 Bad credentials로 거부됐다. Release 조회도 not found였다. 자격증명은 출력하거나 문서에 남기지 않았다.
- **해결 방향:** GitHub runner의 제한된 `contents: write` 토큰으로 **동일 코드 커밋을 새로 빌드**하여 개발 prerelease를 게시하는 별도 workflow를 추가했다. 앱 버전은 0.2.0/2이며 APK·테스트 APK·미서명 release·실제 해시·공개 개발 서명 지문을 함께 제공한다. 아직 게시 성공으로 기록하지 않는다.
- **서명 구분:** runner의 개발 서명은 기존 Cloud 개발 키와 다르므로 새 전용 AVD에서 설치해야 한다. 제출용 키를 생성하거나 기존 키를 저장소/로그로 옮기지 않는다. 배포 파일의 metadata는 실제 업로드 결과로 갱신한다.
- **다음:** 배포 결과·해시를 확인하고 문서/PR을 갱신한다. 실제 Pleos 증거 수집과 일반 Android 설치 전 서비스 검사도 진행 중이다.

## 2026-11-11 16:11 UTC — GitHub artifact로 APK 제공 경로 확정

- **실제 결과:** [패키징 CI 43054889150](https://github.com/leemh-dev/dochagjari/actions/runs/43054889150)에서 동일 앱 코드의 세 APK 빌드·서명/미서명 확인·해시 계산과 artifact 업로드까지 성공했다. 하지만 Release 생성은 GitHub integration 권한으로 HTTP 403 `Resource not accessible by integration`이 발생했다. Release 게시 완료로 표시하지 않는다.
- **해결:** 사용자 요청에 포함된 GitHub artifact 제공 경로를 사용한다. 불필요한 Release 쓰기 권한/게시 단계를 제거하고 읽기 권한으로 APK·metadata·SHA256SUMS를 보관하는 패키징 작업으로 정리한다. 기존 실패는 감추지 않고 [인증 제한 증거](evidence/apk-publishing-limits.txt)에 남긴다.
- **산출물 구분:** 서버/앱 기능 검사 실패가 아니라 배포 API 권한 제한이다. artifact에는 실제 파일과 서명 지문이 있으며 최종 성공 실행의 metadata를 docs/evidence에 반영한다. 소스에 바이너리·서명 키를 추가하지 않는다.

## 2026-11-11 16:16 UTC — Pleos 다운로드 연결 종료 확인·구간 재시도 보완

- **실제 결과:** [KVM 실행 43052661853](https://github.com/leemh-dev/dochagjari/actions/runs/43052661853)에서 KVM version 12 usable 확인. 4,087,751,964-byte 공식 이미지의 512MB 구간 8개 중 7개를 받았으나 나머지 연결이 `Connection reset by peer`로 끝났다. **추출·에뮬레이터 실행 전 실패**이며, KVM Pleos 부팅 실패나 APK 설치 실패로 분류하지 않는다.
- **증거:** artifact 13871218207의 readiness/app-probe JSON을 보존한다. 부팅하지 않은 기기에서 always 수집 단계의 ADB가 45초 대기한 것도 확인했으며 준비 실패 시 명시적으로 건너뛰도록 수정했다. 순정 내비/Console/앱 수신은 실행하지 않았다.
- **보완 근거:** 실제 실패가 큰 요청 하나의 연결 종료였으므로 64MB 구간·동시 4개·구간당 최대 3회·연결 10초/전송 60초·전체 전송 10분 제한으로 바꾼다. 모든 범위/크기·마지막 공식 체크섬은 그대로 확인한다. 기존 무가속 Cloud 부팅을 반복하는 것이 아니다.
- **다음:** 문법·KVM 없는 환경의 사전조건 검사 후 변경한 전송 방식으로 한 번 더 KVM 실행한다. 새 결과도 다운로드·부팅·설치·SDK·핀·Console을 따로 기록한다.

## 2026-11-11 16:18 UTC — 배포 artifact 다운로드·해시·서명 대조 완료

- **결과:** [패키징 CI 43055801516](https://github.com/leemh-dev/dochagjari/actions/runs/43055801516) 성공. [APK 다운로드 artifact](https://github.com/leemh-dev/dochagjari/actions/runs/43055801516/artifacts/13871881801)에 debug/test/unsigned release, apk-metadata.json, SHA256SUMS를 게시했다. Release API 제한은 artifact 경로로 해결했다.
- **검증:** 연결 도구로 실제 artifact를 다운로드하고 `sha256sum -c SHA256SUMS` 세 파일 OK, debug/test `apksigner verify --print-certs` 성공, `aapt dump badging`에서 0.2.0/2·API34·ARM64/x86_64 확인.
- **기준 코드/서명:** APK 코드 `3a01515`, 이후 변경은 검증/배포 도구·문서다. runner 개발 서명이며 제출 서명이 아니다. release는 미서명이다. 기존 Cloud 키와 다른 개발 키이므로 새 AVD에 설치한다. 키 자체는 artifact에 없다.
- **증거:** [배포 metadata](evidence/v0.2-apk-metadata.json), [다운로드 재검증](evidence/v0.2-downloaded-apk-verification.txt). debug SHA-256 `d1a2761d0dac84142b4ff8917f82d6737d3d4e1e30520dd8895e8454e4c85e4d`.
- **CI:** 설치 전 부팅·package/activity 검사 추가 후 [43054503112](https://github.com/leemh-dev/dochagjari/actions/runs/43054503112)도 전체 성공했다. Pleos의 보완 전송 재시도는 별도 실행 중이며 완료 수준을 섞지 않는다.

## 2026-11-11 16:22 UTC — 전송 재시도 프로토콜 검사·중복 CI 정리

- **검사:** 실제 `fetch_part`를 로컬 HTTP 서버·실제 curl로 실행했다. HTTP500 후 동일 범위 재시도 성공/정확한 bytes, 잘못된 Content-Range 세 번 거부/파일 미반영, 전체 기한 초과 시 네트워크 요청 중단을 확인했다. [실행 로그](evidence/pleos-range-transfer-check.txt). 이는 공식 이미지/부팅/SDK 성공 증거가 아니다.
- **사전조건:** 현재 Cloud에서 `prepare_avd.py`를 실행해 KVM 없음이면 이미지 다운로드·에뮬레이터 시작을 모두 생략함을 재확인했다.
- **CI 정리:** 앱 코드와 설정 검사 수준이 같은 이전/중복 generic 실행 43057135514, 43056615550, 43056602467, 43055814944, 43055801721은 자원을 절약하기 위해 취소했다. 실패를 성공 처리한 것이 아니다. 이미 전체 성공한 43052357723·43054503112·43054889390은 그대로 보존하고 최신 PR 실행 43057154160 및 보완 Pleos 실행 43056602693을 유지한다.
- **현황:** Pleos 보완 실행은 runner를 배정받아 빌드 중이다. 최종 부팅/설치/SDK/Console 결과는 별도 확인한다.

## 2026-11-11 16:49 UTC — KVM Pleos 최종 결과·검증 경계 확정

- **전송 해결:** [43056602693](https://github.com/leemh-dev/dochagjari/actions/runs/43056602693)에서 61/61 범위 완료·공식 체크섬 검증·추출·빈 개발 데이터 포맷 후 KVM emulator를 시작했다. [단계 로그](evidence/pleos-kvm-readiness/transfer-and-boot-stages.txt). 전송 연결 종료 문제는 보완 실행에서 해결됐다.
- **관찰/미확인 구분:** emulator log에는 `Boot completed in 31156 ms`와 `ai.pleos.hardware.profile` native crash가 있다. 하지만 900초 동안 ADB 부팅 속성/서비스 확인은 성공하지 않았고 stdout은 비어 있었다. **부팅 로그 관찰**과 **ADB 서비스 준비 미확인**을 구분한다. vendor crash가 원인인지 단정하지 않는다.
- **진단 한계/보완:** 기존 스크립트가 ADB returncode/stderr·devices 목록을 기록하지 않아 연결 문제인지 프레임워크 문제인지 구분할 수 없다. 이를 추가하고 emulator 포트 5554를 명시했다. 보완 후 부팅은 실행하지 않았다. 같은 실패의 자동 반복을 막기 위해 Pleos workflow는 workflow_dispatch로 전환했다.
- **구성 차이:** 테스트 AVD 4GB/2core/SwiftShader/빈 별도 데이터, Linux 이미지 기본 source.properties RAM8192MB. 공식 프로필/데이터 구성 차이를 후속 확인 항목으로 남기며 실패 원인이라고 주장하지 않는다.
- **증거:** [readiness](evidence/pleos-kvm-readiness/readiness.json), [선별 boot 로그](evidence/pleos-kvm-readiness/boot-diagnostics.txt), [미실행 기록](evidence/pleos-kvm-readiness/app-probe.json), artifact 13873542737. 이미지·키는 공개하지 않았다.
- **미실행:** APK 설치/Pleos connected test/내비 SDK 요청·실제 두 핀/Console 좌우 조작/앱 좌석 초기값·콜백·제안. 맥북·실차·실제 SMS에도 접근하지 않았다. README 설치·검증·제출 키 생성/서명 명령을 정리했지만 키 생성·서명을 실행하지 않았다.
- **완료:** 복구/저장 구현과 JVM33·서버14·lint·세 APK 빌드, 일반 Android6+프로세스2단계, 게시 APK 재다운로드 해시·개발 서명 대조. [최신 검증 PR CI 43057154160](https://github.com/leemh-dev/dochagjari/actions/runs/43057154160) 전체 성공. 이후 변경은 진단/문서이며 APK 코드 `3a01515`는 그대로다.
- **다음:** 실제 접근 가능한 공식 AVD에서 ADB 상태·원래 프로필·두 서비스부터 확인하고 내비 두 핀과 Console/앱 수신을 대조한다. 실제 SMS는 자격증명·등록 번호·동의 수신자 준비 시, 제출 서명은 키 준비 시에만 검증한다.

## 2026-11-11 20:27 UTC — 후속 개발 시작·ADB 준비 검사 분리

- **최신 확인:** PR#1 open/미병합, 로컬/원격 a020643 동일. 해당 PR/push CI 43062184213·43062171171 모두 성공. APK artifact13871881801 유효, 기존 v0.2 코드/해시/개발 서명은 유지한다.
- **환경:** 현재 Cloud /dev/kvm 없음·연결 기기 없음. 기본 sandbox의 ADB listener/네트워크가 거부돼 같은 읽기 검사를 승인된 외부 실행에서 재시도했고 기기 없음 확인. 무가속 부팅은 반복하지 않았다. SMS/제출 키 자격증명은 준비되지 않았다.
- **수동 진단:** 이전에 실행하지 않은 포트/ADB 진단 보완 a020643으로 [43095641582](https://github.com/leemh-dev/dochagjari/actions/runs/43095641582)를 수동 요청했다. 현재 runner 배정 대기이며 실행 완료/설치 성공으로 표시하지 않는다.
- **비교:** [공식 메타데이터](evidence/pleos-profile-comparison.json)에서 devices.xml RAM4GiB와 source.properties8192MB가 공존함을 확인했다. 4GB를 원인으로 단정하지 않는다. 원본 userdata 약1.1GB와 기존 빈 별도 2GiB도 구분한다. 비교 부팅은 실행하지 않았다.
- **변경:** ADB transport/boot/package/activity를 개별 명령·종료 코드·stdout/stderr로 검사하는 공통 helper/CLI를 추가했다. 이전 JSON이 true여도 설치 직전에 다시 검사한다. 준비 실패하면 설치/SDK/Console 단계가 진행되지 않게 했다. README의 설치도 같은 gate를 사용한다.
- **검사:** 새 준비 검사 5건(정상, unauthorized/offline/없음, 부분 부팅/비정상 종료/유사 문자열, timeout/실행파일 없음, 자격증명 제거) 통과. Python 구문·diff 검사 통과. 이는 명령 대역 검사이며 Pleos 성공이 아니다. 앱 코드는 바꾸지 않아 APK를 재배포하지 않는다.
- **다음:** 수동 진단의 실제 ADB 상태를 먼저 받아 원인에 맞춰 해결하고, 부팅/두 서비스 준비 후에만 실제 앱 설치·내비/좌석 확인을 진행한다.

## 2026-11-11 20:33 UTC — 설치 차단 회귀·실행 대기와 제출 체크리스트

- **추가 검사:** 실제 ui_probe Python 프로세스를 대역 ADB로 실행해 과거 성공 JSON/현재 미접속과 과거 실패 JSON에서 install/am start를 호출하지 않음을 확인했다. 진단 검사 총7건 통과. [로그](evidence/adb-readiness-tests.txt). 실제 Cloud CLI도 device not found·exit1로 종료했다([JSON](evidence/cloud-adb-readiness.json)).
- **진행 상태:** [20:32 API 스냅샷](evidence/followup-actions-queue.json)에서 수동 Pleos43095641582와 fcfbfc0 CI43096203840·43096197226이 모두 queued/job0. 저장소 pending deployments0, 공개 GitHub Status Actions operational/미해결 incident 없음. 대기 원인을 단정하지 않으며 부팅/검사 실패로 분류하지 않는다. 새 실행 결과는 아직 없다.
- **제출 준비:** [체크리스트](12-submission-checklist.md)에 APK코드/버전/서명/다운로드·완료 증거·실연동/서명/영상/폼 대조의 남은 확인을 정리했다. 앱을 변경하지 않아 기존 성공 검사를 수동 반복하거나 APK를 재배포하지 않았다.
- **외부 조건:** SENS 서비스/접근키/비밀키/발신번호/수신자 매핑과 제출 키 환경 변수 모두 없음, server/.env 없음. 실제 메시지 발송·실제 수신·제출 키 생성/서명을 실행하지 않았다. 실제 병원 동선은 가상 시연으로 유지한다.
- **다음:** runner 진단 결과를 먼저 확인해 ADB 원인을 해결하고 실제 Pleos 내비·좌석·기기 검사를 진행한다. 반복 부팅 전에 기존 실행 결과를 읽고 새 근거를 기록한다.

## 2026-11-11 20:41 UTC — 수동 workflow 시작 실패·정적 검사·회귀 재현

- **실제 종료:** 수동43095641582는 20:35:56 UTC startup_failure로 종료됐다. job0/check-run0/artifact0, `gh run view --log-failed` log not found. [API 증거](evidence/followup-actions-startup-failure.json). 이번 에뮬레이터/이미지 전송/설치/SDK가 시작된 것은 아니다.
- **원인 경계:** CLI의 “workflow file issue일 가능성” 일반 힌트를 확인했다. 실행 권한·dispatch 조건·스케줄링 등 실제 원인은 확인하지 못했다. 인프라 문제나 YAML 문제라고 단정하지 않는다. 새 근거 없는 수동 부팅 재요청은 하지 않았다. 다른 새 CI는 queued 상태로 결과가 없다.
- **추가 검사:** /usr/bin/go가 Go 컴파일러가 아니어서 install/version이 거부됐다. 공식 actionlint1.7.7 바이너리와 체크섬을 내려받아 SHA256 확인 후 이전 a020643 workflow 및 현재 전체 workflow를 검사했고 exit0/진단0. [정적 검사 기록](evidence/workflow-static-check.txt). GitHub 실행 성공을 뜻하지 않는다.
- **독립 버그 재현:** 이전 a020643 ui_probe와 현재 ui_probe를 실제 Python 프로세스/대역 ADB로 실행했다. stale true JSON에서 이전 코드는 install 명령을 호출했고, 현재는 get-state 실패에서 멈췄다. [전후 명령 기록](evidence/adb-stale-record-regression.txt). 실제 설치나 기존 Pleos 연결 실패의 원인 입증은 아니다.
- **완료/차단:** 새 준비·설치 차단7건 로컬 통과, 실제 Cloud CLI 미접속 검증, 워크플로 정적 검사·구문·diff 확인. 앱/서버 기존 완료 증거·APK코드3a01515·버전0.2.0·해시는 유지한다. 새 실제 Pleos 부팅/설치·내비 핀·좌석 수신·SMS/실차/서명은 미검증이다.
- **다음:** workflow 시작 실패의 구체 근거를 먼저 확보하고 실행 조건을 해결한 뒤 실제 ADB/서비스→설치→핀/Console/앱 수신을 검증한다. README/제출 체크리스트/다음 프롬프트를 갱신했다. 프롬프트는 최종 채팅에도 직접 제공한다.

## 2026-11-11 21:49 UTC — 실행 화면 annotation 확인·근거 있는 단일 재시도

- **최신 상태:** PR#1 open/미병합, 로컬/원격71f34c3 동일. Cloud에 KVM/연결 기기/설정 자격증명 없음. APK artifact13871881801 유효. 기존 API의 queued 상태와 이전 기록을 이번 실행 결과로 계산하지 않는다.
- **새 증거:** 공개 실행 HTML에서 수동43095641582 annotation “An unexpected error has occurred and we've been automatically notified”와 request ID `7e7e1f38-de0f-11f1-8cbe-50cc47e59582`를 확인했다. [정제 annotation](evidence/workflow-startup-annotation.json). 일반 gh CLI의 workflow file issue 힌트와 구분한다. 내부 원인·전역 장애를 단정하지 않는다.
- **단일 재시도:** GitHub가 일시적 unexpected error를 보고한 새 근거로 최신 진단71f34c3의 [43105761433](https://github.com/leemh-dev/dochagjari/actions/runs/43105761433)을 수동 요청했다. 이번에는 runner job 생성·진단7건 단계 통과·APK 빌드 시작을 확인했다. 아직 부팅/설치/SDK 성공으로 표시하지 않는다. 앱 코드/배포 APK를 바꾸거나 실제 SMS를 보내지 않았다.
- **자동화 보완:** 이후 workflow에 boot step 성공을 조건으로 connectedDebugAndroidTest와 전용 AVD 프로세스 복원 검사를 연결했다. SDK/Console 조사 실패 시에도 준비된 기기의 독립 검사는 실행할 수 있게 한다. 진단용 임시 APK의 코드/버전/해시/개발 서명도 기록한다. 이 변경은 현재 실행71f34c3에는 포함되지 않는다.
- **좌석 초기값 증거:** 변경 이벤트 수집을 위해 logcat을 비우기 전에 실제 SDK 초기 로그를 별도 보존하도록 보완했다. 초기값·변경·화면/Console을 각각 대조하기 위한 변경이며 실제 수신 성공을 선언하지 않는다.
- **검사:** actionlint1.7.7과 Python 구문 검사 통과. 이미 통과한 앱/JVM/서버 검사를 수동 반복하지 않았다. 새 자동화의 실제 실행은 후속 조건 충족 시 확인한다.
- **다음:** 이번 runner의 실제 transport·boot·package/activity 결과를 확인하고, 준비 성공 시만 설치/두 목적지·Console 좌우/앱 수신을 검증한다.

## 2026-11-11 21:58 UTC — 좌석 설정 복원·변경 시점 증거 보완

- **변경 목적:** 마지막 Empty 화면만으로 Occupied 순간의 저장 옵션 제안을 확인할 수 없어 Console 각 변경 직후 앱 좌석 문구/옵션 안내/화면을 수집하도록 보완했다. Console UI 조작 성공과 앱 수신을 같은 것으로 계산하지 않는다.
- **저장 검증 준비:** 테스트 입력의 정차 상태에서 좌우 카시트 문 여유·유모차를 설정하고 저장 완료를 기다린다. 전용 AVD의 앱을 force-stop/재실행한 뒤 서로 다른 PID·복원 옵션·저장 완료를 대조하고 실제 차량 소스로 전환하도록 했다. 차량 권한 요청 결과의 rc/정제 stderr도 남긴다. 아직 실행 결과가 아니라 다음 probe 코드다.
- **기기 검사 준비:** boot 성공 조건 외에 connected 검사 직전에도 live helper로 준비를 재확인한다. SDK 증거를 먼저 저장한 뒤 이 job이 만든 새 전용 AVD의 테스트 앱 데이터만 초기화해 일반 기능 검사 fixture와 분리한다. 기존 사용자 기기를 초기화하는 절차가 아니다.
- **검사:** Python/Node 구문·actionlint·diff 확인. 현재 실행43105761433은71f34c3이므로 이 보완을 검증한 것으로 표시하지 않는다. 실제 결과가 나오면 별도 기록한다.

## 2026-11-11 22:15 UTC — ADB unauthorized를 확인·정상 화면 승인 준비

- **실제 결과:** [43105761433](https://github.com/leemh-dev/dochagjari/actions/runs/43105761433)에서 진단7건·APK빌드·KVM/공식 이미지61범위·체크섬·에뮬레이터 실행까지 진행했다. transport get-state rc1/stderr `device unauthorized`, devices 목록 emulator-5554 unauthorized를 수집했다. [JSON](evidence/pleos-adb-unauthorized/readiness.json), [정제 요약](evidence/pleos-adb-unauthorized/boot-summary.txt), artifact13894429055.
- **원인 경계:** 이번 실행에서 속성·서비스 조회가 막힌 직접 원인은 ADB 인증 미승인이다. transport 단계에서 중단되어 boot/package/activity는 조회하지 않았으며 프레임워크 준비 여부는 아직 알 수 없다. vendor profile crash/메모리를 이 접근 실패의 원인으로 단정하지 않는다. 앱 설치/SDK/Console은 생략했다([기록](evidence/pleos-adb-unauthorized/app-probe.json)).
- **보완:** Xvfb의 정상 AVD 창을 띄우고 USB 디버깅 승인 화면을 관찰하는 경로를 추가했다. 이 job의 새 AVD/미사용 serial 확인 + GitHub Actions 전용 flag에서만 자동 승인하며, 공개 host adbkey.pub의 지문과 화면 RSA 지문이 정확히 일치하고 정상 문구·취소 옆의 유일한 허용 버튼이 확인될 때만 클릭한다. 항상 허용 checkbox·다른 권한·다른 호스트·모호한 화면은 클릭하지 않는다.
- **비밀/권한:** ADB 키 파일 편집/복사·인증 비활성화·root/privileged 우회는 하지 않는다. 개인키·공개키 본문·X11 cookie는 artifact에 포함하지 않는다. 화면의 공개 지문은 키 본문이나 비밀값이 아니며, 새 테스트 AVD의 화면/판정 booleans만 보존한다. 호스트 OCR은 이 승인 UI 관찰용이고 앱 ML Kit/C12 성공으로 계산하지 않는다.
- **검사:** 새 화면 판단4건 로컬 통과(정상/호스트 지문 불일치/다른 권한/모호·checkbox/한국어). [로그](evidence/debugging-dialog-tests.txt). Python 구문·actionlint·diff 통과. 기존7건과 합쳐 workflow는11건을 실행한다. 실제 화면 클릭은 아직 미실행이다.
- **다음 실행 근거:** GitHub 시작 실패의 무근거 반복이 아니라 확인된 unauthorized에 대해 정상 GUI 승인 조건을 추가한 별도 실행이다. 정상 승인/transport=device/boot1/두 서비스 준비가 확인될 때만 설치·SDK·Console·기기 테스트를 진행한다. 승인 화면을 안전하게 식별할 수 없으면 300초 뒤 차단으로 남긴다.
- **기존 검사와 구분:** [0f21ead 일반 PR CI43106443044](https://github.com/leemh-dev/dochagjari/actions/runs/43106443044)는 전체 성공했다. 실제 Pleos 핀/좌석/실차 성공과 다르다. 앱 코드/배포 APK/서명 키는 변경하지 않았다.

## 2026-11-11 22:27 UTC — GUI launcher127·선행 라이브러리 검사 보완

- **실제 결과:** [GUI43110281026](https://github.com/leemh-dev/dochagjari/actions/runs/43110281026)에서 진단11건·빌드·GUI도구 설치·공식 이미지 검증 후 Xvfb launcher가 약5초/rc127로 종료됐다. authorization_ui는 빈 배열이며 정상 승인 화면 판단·클릭은 실행되지 않았다. [증거](evidence/pleos-gui-launch-failure/readiness.json), artifact13895588943. unauthorized 이후 정상 승인 성공으로 표시하지 않는다.
- **진단 한계 수정:** 기존 필터가 loader의 소문자 error/shared library 메시지를 놓칠 수 있어 대소문자 무관 오류·공유 라이브러리·파일 없음도 수집하도록 했다. 초기에 종료된 launcher를 900초 timeout처럼 표시하지 않도록 rc/실제 대기 시간에 맞춰 오류 문구를 분리했다.
- **선행 확인:** SDK GUI qemu ELF의 실제 의존성에서 libpulse·번들 Qt 라이브러리를 확인했다. libpulse0 명시 설치와 SDK 번들 LD_LIBRARY_PATH를 GUI 프로세스에만 추가하고, 다운로드 전에 ldd로 누락을 검사하도록 했다. GitHub의 정확한 누락 라이브러리는 이전 로그에 없어 단정하지 않는다. [Cloud 읽기 전용 사전 검사](evidence/cloud-gui-library-preflight.json)는 준비 성공이며 VM/부팅을 실행한 검사가 아니다.
- **임시 APK 구분:** 해당 CI가 만든 debug APK의 소스/버전/해시/공개 개발 서명은 [identity](evidence/pleos-gui-launch-failure/verification-apk-identity.txt)에 있다. 설치하지 못한 검증용 임시 빌드이며 기존 배포0.2 artifact를 바꾸지 않았다.
- **검사:** actionlint·Python 구문·diff 확인. 이 수정 후 실행은 loader preflight를 먼저 통과해야 SDK 이미지/GUI/정상 승인 단계로 진행한다. 정상 승인을 못 하면 실제 연동 미검증으로 남기며 인증 설정이나 키 파일로 우회하지 않는다.

## 2026-11-11 22:54 UTC — 정상 GUI·일치 지문 확인, 모달/버튼 인식 보완

- **실제 실행:** [43111928160](https://github.com/leemh-dev/dochagjari/actions/runs/43111928160), 코드022160c, artifact13897640819. GUI ELF 사전 검사 성공·실제 창 표시·USB 디버깅 문구/현재 호스트 공개 지문 일치를 확인했다. 그러나 전체 화면 OCR이 허용 버튼을 식별하지 못해 클릭0/transport unauthorized로 종료됐다. 설치·내비·Console·기기 검사는 실행하지 않았다. [원본 화면](evidence/pleos-normal-ui-blocked/debugging-screen-12.png), [준비 기록](evidence/pleos-normal-ui-blocked/readiness.json).
- **구체 관찰:** 화면에는 nested virtualization 안내 모달과 그 뒤의 정상 한국어 USB 승인 버튼이 있었다. 전체 화면 OCR 대신 실제 화면의 연결 영역을 분석해 안내문/유일한 OK와 취소·허용 쌍을 읽는다. 알려진 안내만 정상 OK로 닫고 반드시 새 화면을 수집한다. 모호한 모달은 차단하며 USB 승인에는 기존 문구/현재 호스트 지문 조건을 그대로 적용한다. Always allow/인증 설정/키 파일을 건드리지 않는다.
- **로컬 증거:** 같은 원본 화면을 읽기만 해 안내 OK와 배경 버튼 쌍을 찾는 것을 확인했다([분석 JSON](evidence/observed-dialog-region-check.json)). Cloud에서 클릭·승인·설치를 한 것이 아니다. 임시 OCR crop은 증거 원본을 변경하지 않고 배포하지 않는다. 호스트 UI OCR을 Android ML Kit 성공으로 계산하지 않는다.
- **검사:** Python 진단12건(기존7+화면5)·compileall·actionlint1.7.7·diff 검사 통과. 일반 Android 최신022160c PR43112125115/push43111927279 CI 모두 성공. 별도 이전 PR43110288580의 SDK ZIP 실패도 보존했다. 앱/서버/Gradle 코드·기존 배포 APK는 동일하다.
- **다음 실행 근거:** 실제 모달/버튼 인식 실패라는 새로운 증거와 해당 수정으로 수동 실행을 한 번 요청한다. 정상 승인 이후에도 transport/boot/package/activity 모두 준비되기 전에는 설치하지 않는다. 성공/실패를 artifact로 다시 확인한다.

## 2026-11-11 23:10 UTC — 정상 입력 전송 후에도 unauthorized·후속 관찰 보완

- **실제 종료:** [43115895691](https://github.com/leemh-dev/dochagjari/actions/runs/43115895691), 코드472b68d, artifact13899317062. 진단12건·APK빌드·GUI 라이브러리·KVM·공식61범위/체크섬·실제 GUI까지 성공했다. 1차 정상 안내 OK 입력 후 2차 새 화면에서 USB 문구/현재 호스트 지문/취소·허용 쌍을 확인해 허용 위치에 X11 입력을 전송했다. [원본 화면](evidence/pleos-input-unconfirmed/debugging-screen-2.png).
- **완료 경계:** clicked=true는 X11 입력 명령 성공이다. 게스트 승인 완료가 아니다. get-state는300초 후에도 rc1/unauthorized, devices도 unauthorized였다. transport에서 멈춰 boot/package/activity는 미조회이며 설치/내비/Console/connected 검사는 생략했다. [준비 기록](evidence/pleos-input-unconfirmed/readiness.json), [요약](evidence/pleos-input-unconfirmed/summary.json). 로그의 profile 서비스 문제를 인증 실패 원인으로 단정하지 않는다.
- **관찰 누락 보완:** 클릭 이후 화면이 없어 실제 버튼 반영/대화상자 종료/재등장 여부를 알 수 없다. 후속 코드에 정상 입력2초 뒤 읽기 전용 화면/문구·지문 관찰과 종료 직전 원본 화면을 추가했다. 새로운 자동 클릭이나 키/인증 설정 변경은 없다. 이 보완으로 부팅을 재요청하지 않았고 실제 Pleos 실행 증거는 아직 없다.
- **검사:** 신규 읽기 전용 관찰 회귀를 포함해 Python13건 통과, 구문·actionlint·diff 검사 통과. [최신472b68d 일반 CI43115896326](https://github.com/leemh-dev/dochagjari/actions/runs/43115896326)는 verify·일반 Android 기기/프로세스 검사 모두 성공했다. 이 결과는 Pleos 성공이 아니다.
- **제출 자료:** 시연 녹화/증거 수집 순서를 docs/12에 추가했다. docs/08·09·README/PR에 완료/미검증을 정리한다. 배포 APK·앱0.2.0/2·코드3a01515·기존 해시는 유지한다. 임시 검증 APK는 [별도 identity](evidence/pleos-input-unconfirmed/verification-apk-identity.txt)로 보관한다.
- **환경/명령:** GitHub KVM 수동workflow, gh run view/API/artifact 조회. Cloud는 KVM/연결기기/외부 설정이 없고 Mac에 접근하지 않았다. gh pr edit는 구형 Projects GraphQL 조회 오류로 실패해 동일 본문을 REST PATCH로 갱신했다.
- **커밋/최신 CI:** 입력 후 관찰 보완1dfa196 푸시. 23:10 확인 시 PR43117773337/push43117765439 진행 중. 직전472b68d PR43116162416도 전체 성공. 최신 대기/진행을 성공으로 계산하지 않는다.
- **남은 일:** 승인 입력 이후 실제 화면을 확인하고 연결을 복구한 뒤 준비 helper→설치→두 목적지 SDK/핀→Console 좌우/앱 초기값·콜백/옵션 제안→Pleos 기기 검사를 진행한다. 실제 SMS·제출 서명·병원 동선·순정 카메라·실차는 별도 조건/증거가 필요하다. 무근거 재부팅은 하지 않는다.

## 2026-11-12 00:35 UTC — 사용자 요청: 에뮬레이터 화면·오류 우선 확인

- **요청/환경:** 사용자가 직접 에뮬레이터를 실행해 스크린샷을 보여 주고 기존 오류부터 수정하도록 요청했다. PR#1 open/HEAD055bdea, 깨끗한 작업 트리. Cloud KVM 없음·VPN 미설정·연결 ADB기기0. 기본 sandbox ADB socket이 거부되어 정상 권한 경로로 재확인한 결과 devices 목록이 비었다. Mac 접근을 주장하지 않는다.
- **기존 CI 확인:**055bdea PR43118021413/push43118012795와1dfa196 CI 모두 success. 이전 진행 중 표기를 새 조회 결과로 갱신한다.
- **근거 있는 Pleos 진단:** 기존 입력 후 관찰 누락을 해결하는1dfa196 코드는 아직 실행하지 않았다. 새 증거 수집 목적을 명시하고 현재055bdea의 수동43128107467을 한 번 요청했다. 앱 설치/승인 성공으로 미리 계산하지 않는다.
- **앱 화면 검사 준비:** 새 전용 일반 Android AVD에서 픽업/주차/우회/후보 없음/가상 병원/설정/생성 영상 실제 분석/우회/알림/주행 제한을 화면 조작하고 원본 PNG·UI XML·결과를 보존하는 capture_app_screens.py를 추가했다. CI 전용 플래그와 설치 전 live 준비 확인을 요구하고 이 job이 만든 테스트 AVD에서만 앱 데이터를 초기화한다. 실제 Pleos SDK 성공을 대체하지 않는다.
- **검사/다음:** Python 구문·actionlint·diff 통과. 새 화면 검사는 아직 미실행이며 GitHub KVM 실행 결과/실패 화면을 확인해 실제 앱 오류와 자동화 오류를 구분하고 수정한다. 앱 런타임/배포 APK는 아직 변경하지 않았다.

## 2026-11-12 00:45 UTC — 실제 승인 입력 후에도 동일 대화상자가 남음

- **새 증거:**055bdea 수동43128107467, artifact13904930954. 정상 안내 OK 후 호스트 지문/USB 문구/허용 버튼을 확인해 입력했으나 직후와 종료 직전 모두 승인창이 남았다. transport rc1/unauthorized 지속, 속성/서비스 미조회, 설치/앱 SDK/Console/기기 검사는 생략했다. [입력 후 원본](evidence/pleos-post-input-dialog/debugging-after-input-2.png), [준비 JSON](evidence/pleos-post-input-dialog/readiness.json). 클릭 여부만으로 승인 성공을 판단하던 관찰 공백을 실제 화면으로 해소했다.
- **변경/가설:** 짧은 X11 click 대신 같은 화면의 같은 검증된 허용 버튼에 정상 mouse down→200ms→mouse up을 보낸다. 짧은 입력이 게스트에 반영되지 않았을 가능성을 비교하는 변경이며 정확한 내부 원인은 아직 입증되지 않았다. 호스트 지문/문구/유일한 버튼 조건·후속 화면/ADB 확인을 유지하고 인증 설정/키/항상 허용을 변경하지 않는다.
- **검사/후속:** 기존13건·구문·actionlint·diff 검사 후 이 구체 입력 변경을 별도 수동 실행으로 비교한다. 앱/배포 APK는 동일하다. 일반 Android 화면 검사는 별도로 진행 중이다.

## 2026-11-12 00:50 UTC — 앱 원본 화면13장 확보·실제 Compose 스위치 선택 오류 수정

- **실제 실행 성공:**cc1c19e PR43128321081/push43128313393 전체 성공. JVM33·서버14·lint·당시 Python13·일반 Android 기능6·프로세스 복원2단계·새 UI 조작 검사 모두 통과했다. artifact13903799876의 원본13 PNG/XML·요약을 [화면 증거](evidence/android-ui-screens/README.md)에 보존했다. 픽업/주차/우회/후보 없음/가상 병원/설정/Android 영상 EMPTY·OCCUPIED/C12/우회/알림 좌표 입력/주행 잠금을 실제로 확인했다. 알림 화면 캡처를 발송 완료로 계산하지 않는다.
- **재현된 오류:**실제11-test-inputs.xml에 Switch 클래스 노드가0개여서 기존 Pleos ui_probe의 클래스 선택기로는 모의 내비/차량 소스 스위치를 찾을 수 없다. Compose는 checkable/clickable android.view.View로 노출한다. [전후 회귀 JSON](evidence/compose-toggle-regression.json). Pleos 실제 기기 실패를 재현했다고 주장하지 않고, 실제 동일 앱 UI XML에서 선택기 오류를 재현한 것이다.
- **수정:**클래스명 대신 정확한 라벨/같은 행/라벨 오른쪽의 유일한 checkable·clickable 노드를 찾도록 ui_nodes.py로 분리했다. 기존 set_switch의 enabled 검사와 변경 후 checked 확인을 유지한다. 탭·다른 스위치·중복/미존재는 선택하지 않는다. 주행 화면의 실제 disabled 상태도 검증했다.
- **검사:**새 실제 XML 회귀3건 추가, Python16건 통과. compileall·actionlint·diff 통과. Kotlin 앱/배포 APK 버전·해시는 변경하지 않았다. 새 스위치 선택기의 실제 Pleos 조작 검증은 아직 남아 있다. 정상 누름·떼기 Pleos43129699644는 별도로 진행 중이다.

## 2026-11-12 00:58 UTC — 정상 누름·떼기도 unauthorized·OCR 음성 판정 경계 수정

- **실제 비교 결과:**d661520 수동43129699644/artifact13904479034. 검증된 허용 버튼에 down→200ms→up을 보냈으나304초 후에도 get-state rc1/unauthorized였다. 설치/SDK/Console/Pleos 기기 검사는 생략했다. [요약](evidence/pleos-sustained-input/summary.json) · [원본 입력 후 화면](evidence/pleos-sustained-input/debugging-after-input-2.png). 정상 입력 길이 변경으로 인증 문제가 해결됐다고 주장하지 않는다. 이후 추가 부팅을 요청하지 않았다.
- **추가 재현된 진단 오류:**입력 후 OCR dialog_detected=false였지만 원본 PNG를 직접 보면 승인창이 남아 있다. false는 문구 인식 실패이며 창 종료 증거가 아니다. 종료 직전 화면은 검정이었고 이 역시 승인/연결 성공이 아니다. GUI/게스트 입력의 정확한 내부 원인은 미확인이다.
- **수정/검사:**후속 관찰에 dialog_state=present/unknown과 absence_verified=false를 명시했다. 미인식은 unknown으로 기록하고 원본/ADB 상태를 보도록 했다. OCR 누락 회귀를 추가해 Python17건 로컬 통과, 구문·actionlint·diff 통과. 수정 후 실제 Pleos 재실행은 하지 않았다.
- **완료/미완료:**실제 일반 Android 화면13장·기기 검사 성공과 Compose 선택기 수정은 완료 증거를 보존했다. 인증 성공·실제 내비 핀/좌석·Mac/실차/SMS/제출 서명은 미검증이다. Kotlin 런타임/배포 APK는 동일하다. 다음에는 기기 접근부터 확인하고 같은 실패를 근거 없이 재요청하지 않는다.

## 2026-11-12 00:59 UTC — 화면·오류 우선 점검 최종 정리

- **커밋:**cc1c19e 화면 조작/캡처, d661520 정상 누름·떼기 비교,900ae4c 실제 Compose 선택기 수정,ffa7fcf OCR 미인식 unknown 판정. 각 기능별 푸시. README/docs/08·09·12를 최신 결과로 정리했다. 원본13장과 UI XML·실제 Pleos 입력 후/종료 화면·준비 JSON을 GitHub에 보존했다.
- **완료 증거:**cc1c19e PR43128321081/push43128313393 전체 성공. 이후 d661520 push43129700273도 전체 성공. 최신900ae4c PR43130278797/push43130268157·ffa7fcf PR43131230889/push43131223889는 확인 시 진행 중이다. 최신 성공으로 계산하지 않는다. 로컬17건·구문·actionlint·diff 통과.
- **미검증/남은 일:**Pleos 정상 입력200ms 비교도 unauthorized. 원인은 미확인이고 추가 부팅을 중단했다. 승인창 원본/호스트 입력 전달·연결을 확인한 뒤 준비/설치/SDK 두 목적지·핀/Console·앱 수신을 이어간다. 수정된 선택기 실제 Pleos 조작·실차·Mac·실제 SMS·제출 서명은 미검증이다.
- **산출물/인계:**앱 런타임/배포0.2.0/code2와 artifact13871881801은 유지한다. 새로운 Android 앱 기능 오류를 재현한 것처럼 쓰지 않는다. 이번에 수정한 오류는 조작기와 진단 판정이다. 다음 실행 프롬프트는 docs/09와 최종 채팅에 제공한다. PR을 자동 병합하지 않는다.

## 2026-11-12 01:12 UTC — 다음 연동 작업 시작·입력 전달 대상 진단 준비

- **기준/환경:**AGENTS·개발계획/docs08~12·README·기존 승인 원본/JSON을 확인했다. PR#1 open/feat 브랜치, 기존6109222와900ae4c CI 확인.900ae4c PR43130278797/push43130268157 성공,6109222/ffa7fcf CI는 시작 확인 시 진행 중이었다. Cloud에 KVM/ADB연결기기/VPN 설정이 없고 Mac 접근 경로를 확인하지 못했다. 무가속 부팅을 실행하지 않았다.
- **새 진단 목적:**기존 실제 원본에서 승인창은 남았지만 X11 클릭의 실제 좌표/대상 창/포커스/창 위치가 누락돼 호스트 입력 전달과 게스트 미수신을 구분할 수 없다. 같은200ms 입력을 유지하고 전후 read-only pointer 좌표/대상 창ID·class·부모/포커스ID·디스플레이/창 geometry·개별rc/stderr를 기록하도록 추가했다. 창 제목은 보존하지 않는다. 키/인증/권한/Always allow는 변경하지 않는다.
- **검사:**새 읽기 전용 진단3건 포함 Python20건 로컬 통과. Python 구문·actionlint·diff 통과. source/coords/fingerprint/모달/버튼 확인 조건과 설치 직전 live ADB 준비 확인은 유지한다. 새 진단은 실제 Pleos 입력 성공 증거가 아니며 다음 단일 수동 실행의 목적은 누락된 입력 대상 근거 확보다.
- **변경 범위:**X11 관찰·CI 의존 도구와 문서만 변경했다. 앱0.2.0/code2·배포 코드3a01515·기존 APK/서명/해시는 유지하며 디자인 개편은 미룬다. 기존 통과한 앱/JVM/서버 검사를 수동 반복하지 않는다.

## 2026-11-12 01:18 UTC — Android 실제 HTTP 모의 복구 검증 준비

- **독립 작업:**앱 코드/배포 APK를 유지하며 새 전용 일반 Android AVD에서 실제 앱 UI→로컬 Python 모의 서버 HTTP를 검증하는 스크립트를 추가했다. 서버가 POST를 DB에 기록한 뒤 응답을 보류하고 앱 프로세스를 종료한다. 같은 DB로 서버를 재시작하고 앱의 같은 세션/요청·토큰 미복원·재입력 후 GET만 수행하는지, POST1개/DB행1개/요청지문 동일/공급자 호출0을 검사한다.
- **격리:**GITHUB_ACTIONS/새 전용 AVD 플래그와 live 준비 확인을 요구한다. fixture는 loopback·명시적 mock이며 공급자 함수는 실행 금지 대역이다. 수신자에는 실제 전화번호 대신 단순 비전화 placeholder를 쓰며 외부 SMS를 보내지 않는다. 임시 토큰/DB/hold파일은 artifact 밖 작업 디렉터리에서만 만들고 토큰·명령 인수·원본 요청 본문을 로그에 기록하지 않는다. 소유 서버 PID 헤더로 해당 child 프로세스 응답인지 확인한다.
- **호스트 사전 확인:**실제 local HTTP에서 응답 보류 중 DB mock 저장·서버 재시작 후 동일 행/지문 GET·공급자 호출0을 확인했다([JSON](evidence/mock-http-fixture-self-check.json)). 호스트 fixture 검사이며 Android 성공을 뜻하지 않는다. Python 구문·actionlint·diff 검사 통과. 다음 CI에서 실제 앱 결과/실패를 확인한다.
- **자료 보존:**기존 화면 캡처 함수를 import 시 자동 실행하지 않도록 main guard로 옮겨 재사용한다. 새 검사는 이전 화면13장/기기 검사를 대체하지 않는다. 실제 SENS 수락·휴대폰 수신·Pleos 실연동과 구분한다.

## 2026-11-12 01:22 UTC — 기기 실행 전 fixture 수신자 기본값 대조 수정

- **코드 검토 사실:**AppModel 기본 수신자 ID가 demo인데 신규 mock fixture는 demo-family여서 실제 앱 요청이403이 될 구성 불일치를 발견했다. 아직 Android에서 이 실패를 재현한 것은 아니며 앱 기능 오류가 아니다. fixture를 앱 기본 demo에 맞췄다.
- **검사:**수정된 ID와 소유 서버 PID 헤더 조건으로 호스트 HTTP 응답 보류/동일DB 재시작 GET을 재확인해 통과했다. 구문·actionlint·diff 통과. 불일치가 있는 이전24a7d2c의 내 CI43133592127/43133585377은 취소 요청해 불필요한 기기 실행을 줄이고 수정 코드의 CI로 확인한다. 취소 요청/완료를 성공 결과로 계산하지 않는다. Pleos X11 근거 수집 실행은 유지한다.

## 2026-11-12 01:32 UTC — X11 실제 대상 미확인·별도 호스트 비교 후 입력 차단 보완

- **새 실제 증거:**93b4c16 수동43132924010/artifact13906886041은 unauthorized 종료. 입력 전 좌표는 요청/실제 모두2403,351이고 display3000x2000이지만 pointer WINDOW=0, focus543이었다. 입력 명령은10초 timeout으로 완료되지 않았다. 설치/속성·서비스/앱 SDK/Console/기기 검사는 생략했다. [요약](evidence/pleos-x11-input/summary.json) · [원본/JSON](evidence/pleos-x11-input/readiness.json).
- **가설 비교:**시스템 설치 helper/sudo가 없어 설치할 수 없었다. 공식 Debian trixie 패키지 메타데이터의 SHA256을 검증해 Xvfb/xdotool/libxdo3를 /workspace/tools/x11-isolated에만 추출했다. 시스템/키/인증 설정은 변경하지 않았다. 개인 Xauthority·TCP 비활성 자체 Xvfb에서 동일좌표 --sync 명령과 분리 입력 모두 약0.23초에 성공했다([기록](evidence/x11-pointer-repeat-check.json)). 따라서 반복 --sync만으로 기존 실패를 설명할 수 없다. KVM/Android는 이 호스트 비교에서 실행하지 않았다.
- **보완:**중복 이동을 제거해 이동→대상 관찰→정상 누름/떼기를 분리했다. WINDOW=0 등 대상 미확인이면 입력을 보내지 않고 즉시 새 원본을 수집한다. 제목을 제외한 root 창 목록/좌표/class를 추가해 실제 창이 존재하는지 확인한다. 같은 인증 실패를 반복하는 목적이 아니라 새 근거인 실제 대상0의 의미를 좁히는 진단이다.
- **선행 검사:**새 check_pointer_runtime.py로 에뮬레이터 없이 호스트 포인터 동작을 먼저 확인하고 실패 시 이미지 다운로드 단계로 가지 않게 했다. Cloud 사용자 경로 런타임에서 실제 사전 검사 성공([JSON](evidence/host-pointer-preflight.json)). 정상 X11 인증은 유지하고 cookie를 출력하지 않는다.
- **검사/후속:**새 inventory/대상 차단 회귀 포함 Python21건·구문·actionlint·diff 통과. 새 창 목록/차단 직전 원본 근거 수집 목적의 단일 후속 실행으로 비교한다. 앱0.2.0/2·기존 APK는 유지한다. Android 실제 HTTP 복구2c2feb0은 verify 성공·기기 단계 진행 중이다.

## 2026-11-12 01:39 UTC — 실제 Android HTTP 복구 통과·원본3장 보존

- **실제 성공:**2c2feb0 PR43134026080/push43134018393 전체 success, artifact13907511615. 새 Android API34 KVM에서 기존 JVM33·서버14·lint·당시 Python20·기기6·프로세스2단계·화면13장과 새 실제HTTP 검사 모두 통과했다.
- **새 완료 범위:**실제 앱 UI로 서버 mock/동의/확정 위치를 입력했다. GET404→POST의 DB mock 저장 후 응답을 보류→앱 force-stop→서버 동일DB 재시작→새 앱 PID/빈 토큰 확인→같은 세션 GET200 복구. POST총1회/DB행1개/요청지문 동일/공급자0회, PID8659→9445. [정제 요약/원본3장](evidence/android-http-recovery/README.md). 모의 결과이며 SENS 수락/휴대폰 수신/실차 성공이 아니다.
- **민감정보/격리:**토큰 재입력은 메모리/임시 private 파일만 사용했고 artifact에는 토큰/DB/전화번호/실제 요청 본문을 남기지 않았다. 실제 수신자가 없는 loopback fixture이다. 서버 재시작 중 DB를 삭제하거나 새 세션으로 재발송하지 않았다. Kotlin앱/배포 APK는 바꾸지 않았다.
- **X11 추가 비교:**자체 Xvfb에 실제 Xmessage 창을 띄웠을 때 포인터는 실제 child ID를 반환했다([JSON](evidence/x11-mapped-window-check.json)). 따라서 실제 Pleos WINDOW0을 정상 전달로 간주하지 않는다. 이것만으로 창 종료/이동/게스트 장애 원인을 확정할 수는 없다. 창 목록/입력 차단 원본을 수집하는 dacfc37 수동43135246535 결과를 기다린다.

## 2026-11-12 01:51 UTC — 창 목록과 실제 대상 불일치 확인·추가 부팅 중단

- **실제 결과:**dacfc37 수동43135246535/artifact13907941147. host 포인터 사전 검사 성공, 공식 이미지/GUI 실행 후 원본에는 USB 승인창이 있고 root 창 목록에는 Emulator2400x1350/100,100이 있었다. 그러나 기대/실제 좌표2403,351에서 pointer WINDOW0, focus=root543이었다. 대상 미확인 차단이 작동해 허용 동작을 보내지 않았고 unauthorized로 종료했다. [새 원본/정제 근거](evidence/pleos-window-target-blocked/summary.json). 창 목록/기하정보/화면 표시는 실제 전달 증거가 아니다. GUI 입력·포커스/창 상태의 정확한 내부 원인은 아직 미확인이다.
- **경계:**transport에서 막혀 부팅 속성/package/activity는 미조회, 앱 설치/실제 내비·핀/Console·SDK수신/Pleos 기기 검사는 생략했다. 이후 추가 부팅을 요청하지 않았다. 기존 RAM/userdata/profile crash나 auth키 부족을 원인으로 단정하지 않는다. 인증/키/권한/항상 허용은 바꾸지 않았다.
- **호스트 비교:**자체 Xmessage 창에서 screenshot 전/후 포인터 ID가 동일하게 양수였다([JSON](evidence/x11-screenshot-effect-check.json)). 이 조건에서는 screenshot/sync만으로 같은 문제를 재현하지 못했다. Android/Pleos 성공 증거가 아니며 Qt/AVD 조건과 동일하다고 가정하지 않는다.
- **최종 검사:**2c2feb0 및 최신 dacfc37 PR43135254549/push43135246257 일반 CI 전체 성공. JVM33/서버14/lint/진단21/기기6/프로세스2단계/기존UI13장/새실제HTTP 복구가 통과했다. 문서4530bb9 CI는 확인 시 진행 중. 앱/서버/Gradle 런타임을3a01515와 diff해 동일함을 확인했고 배포 artifact13871881801도 미만료다. SENS/제출키 설정은 미완비, server/.env 없음(값은 출력하지 않음).
- **문서/산출물:**docs/08·11·12에 실제 Android SERVER_MOCK HTTP 복구 완료 수준을 반영하고 README 재현 명령·docs/09 다음 프롬프트를 정리했다. 앱0.2.0/code2/배포 SHA·개발 서명·unsigned release는 유지한다. 원본 화면/PR/검증/다음 프롬프트를 최종 채팅에도 제공한다. PR은 병합하지 않는다.
- **다음:**접근 가능한 정상 Pleos/맥북 원격 연결 또는 근거 있는 X11/Qt 입력/포커스·창 상태 진단을 먼저 진행한다. 준비 helper가 실제 성공한 뒤에만 설치/두 목적지 SDK·핀/Console 좌우·앱 초기값·콜백/설정 복원/기기 검사를 한다. 실제 SMS·실차·병원 동선·순정 카메라·제출 서명은 여전히 별도 미검증이다.

## 2026-11-12 — 중간 화면·전체 작업 위치 안내 요구 반영

- 사용자 요청으로 docs/09 프롬프트에 전체5개 작업 영역과 완료/진행 중/차단/미검증, 현재/다음 작업 안내를 추가했다. 단계 변경·중요 결과/실패·장시간 실행 중 약1분 간격의 실제 상태 공유를 요구한다.
- 앱 최초 실행/기능 전환/오류·수정 전후의 원본 화면을 중간중간 채팅 이미지로 표시하고 환경/실제·모의 구분/확인 결과를 붙이도록 했다. CI 종료 후에만 캡처를 받을 수 있으면 제약·수신 시점을 알리고 확보 즉시 보여 주며, 이전 화면/승인창/검정 화면을 새 앱 성공 증거로 대체하지 않는다. 민감정보 공개 금지는 유지한다.
- 문서만 변경했다. git diff --check로 확인하며 기존 앱 검사·부팅을 재실행하지 않는다. 앱0.2.0/2·APK/서명/해시는 유지하고 기존 PR#1에 커밋·푸시/기록한다. 실제 연동 검증 수준은 변경하지 않는다.

## 2026-11-12 02:06 UTC — 표시/입력 영역을 분리하는 native X11 진단 준비

- **전체 위치:**①최신 상태/기기 확인 완료(PR#1 open/feat 브랜치·시작 HEAD9cf6d7d·작업 트리 깨끗함),②Pleos 승인/설치 차단 조사 진행,③실제 내비/좌석 미검증,④기존 Android HTTP 모의 복구 완료 유지,⑤디자인/서명/제출 준비 후속. 현재 Cloud는 KVM/ADB기기/VPN 없음. Mac 원격 연결 여부를 질문했고 답변과 무관한 진단은 계속한다. Cloud 환경 skill/status와 기존 README/docs07~12·원본/JSON·최신CI를 확인했다.
- **새 가설/근거:**기존43135246535에는 승인창/Emulator 창 목록이 있으나 pointer WINDOW0/focus=root였다. 창 목록은 MapState/실제 입력 영역을 포함하지 않아 표시와 입력 가능 상태를 구분할 수 없다. RAM/userdata/sync/capture/키 부족을 원인으로 단정하지 않는다.
- **호스트 통제 실험:**private Xauthority가 있는 자체 Xvfb에서 새로 만든 작은 합성 창만 사용했다. 정상 창은 직접 포인터 child ID가 양수, 같은 창을 계속 표시한 채 그 합성 창의 입력 영역만 비우면 IsViewable인 채 child0, Unmap하면 IsUnmapped였다. native Xlib/XShape snapshot이 이를 구분했고 X11오류0이다. [실제 JSON](evidence/host-window-input-regions.json). 이 조작은 합성 테스트 창에만 했으며 실제 에뮬레이터/ADB/인증 설정을 변경한 것이 아니다.
- **새 구현:**별도 subprocess에서 XGetWindowAttributes/직접 XQueryPointer/XShape 입력·bounding 영역/요청점 포함 여부/숫자 X11오류를 read-only 수집한다. 창 제목/키/clipboard는 읽지 않는다. X11 대상 미확인 입력 차단·정상 지문/문구/버튼·live 설치 gate를 유지한다. DLL/ABI/창 변경 오류는 available=false로 처리하고 주 진단 프로세스를 보호한다.
- **검사/다음:**실제 합성3상태 및 host pointer 사전 검사 통과, 기존 Python21건/구문/actionlint/diff 통과. 새 가설을 실제 Pleos의 read-only 속성과 대조하는 단일 수동 실행을 요청한다. 같은 실패의 부팅 반복이 아니라 이전에 빠진 MapState/입력 영역 근거를 확보하는 목적이다. 테스트/앱 화면은 얻는 즉시 환경·범위를 붙여 채팅에 공유한다. 앱0.2.0/code2·APK/해시/서명/디자인은 유지한다.

## 2026-11-12 02:26 UTC — OCR 실패 시 native 관찰까지 생략되는 진단 오류 수정

- **실제 결과:**c610c10 수동43139256982/artifact13908664722은 unauthorized로 종료했다. host 합성 native 검사/포인터 검사는 성공했으나 실제 기기 native 속성은 수집하지 못했다. 첫 전체화면 OCR20초 timeout 후 모달을 정상 닫았고, 이후 지문/버튼은 인식해도 문구 판정이false여서 승인 입력과 함께 native 관찰 코드까지 건너뛰었다. 설치/SDK/Console/기기 검사는 생략했다. [원본/요약](evidence/pleos-native-ocr-gate/summary.json). 실제 Pleos MapState/입력 영역이 확인됐다고 쓰지 않는다.
- **재현/수정:**제목 OCR miss + 지문/버튼 인식 조건에서 observe_pointer가 호출0인 문제를 회귀 검사로 재현했다([수정 전 실패](evidence/native-observation-gate-before.txt)). read-only 관찰은 승인 판정 앞에서 독립적으로 수행하도록 분리했다. 이 경로는 포인터도 움직이지 않는다. 실제 허용 입력에는 기존 정확한 호스트 지문/문구/버튼과 대상 확인 조건을 그대로 요구한다. OCR 판정의 title/RSA/지문 라벨/지문 개수 boolean도 추가해 원문/키를 기록하지 않고 실패 부분을 구분한다.
- **검사:**Python22건 통과·구문/actionlint/diff 통과. 실제 원본의 호스트 OCR 재분석에서는 title은 인식되지만 RSA/지문 라벨이 읽히지 않는 경우가 있었다. 이것은 Android ML Kit 검사가 아니고 인증 성공도 아니다. 키/영역/포커스는 바꾸지 않았다.
- **다음 실행 근거:**실제 missing snapshot의 구현 오류를 수정했으므로 같은 가설의 속성을 이제 수집할 수 있는 단일 실행으로 재확인한다. 앱0.2.0/2와 배포 APK는 유지한다. 새 일반CI c610c10은 시작 확인 시 verify 성공/기기 실행 중이다. artifact가 제공되면 새 화면을 채팅에 표시한다.

## 2026-11-12 — native 대상 판정의 false negative 재현·정상 창 활성화 준비

- **실제 근거:**0accb78 수동43141423229/artifact13910466262에서 read-only native 관찰을 확보했다. legacy xdotool은 WINDOW0이지만 직접 XQueryPointer는 child4194311, 같은 Emulator 창은 IsViewable/InputOutput·완전한 input/bounding 영역 안에 요청점이 있었고 X11오류0이었다. 기존 guard가 이 실제 창을 잘못 차단하는 회귀 실패를 재현했다([원본/요약](evidence/pleos-native-target/summary.json), [수정 전 실패](evidence/native-target-guard-before.txt)). unauthorized이며 설치/SDK/Console 검사는 실행되지 않았다.
- **수정:**root/좌표/직접 child/Emulator class/MapState/InputOutput/완전한 두 영역/오류0을 모두 요구하는 native 판정으로 교체했다. 확인된 창에만 표준 windowfocus를 보내고 직접 XGetInputFocus와 대상을 다시 확인한 뒤 정상 Allow 버튼 입력을 허용한다. 에뮬레이터 입력 영역·키·Android 권한·인증 설정은 변경하지 않는다. 자체 합성 창에서만 정상 포커스/빈 입력 영역/Unmap을 비교했다.
- **OCR 보완:**기존 원본에서 전체 OCR의 RSA/지문 라벨 오독을 확인했다. 같은 원본의 확대 header에서 라벨과 동일 지문을 재확인하는 방법을 추가했다. 전체 USB 제목+정확한 현재 호스트 공개 지문1개, header의 RSA/라벨+동일 지문1개를 모두 요구한다. 이전 이미지 재분석은 새 호스트 인증 성공이 아니다([정제 결과](evidence/focused-header-ocr-check.json)).
- **검사/실행 근거:**Python28건·실제 host 합성3상태/정상 포커스·구문/actionlint/diff 통과. 원래 없던 native 근거로 판정 오류를 재현·수정했으므로 최신 수정의 효과를 확인하는 단일 수동 실행을 진행한다. transport=device/boot1/package/activity/각rc0 및 설치 직전 재확인이 없으면 설치하지 않는다. 배포0.2.0/2·3a01515·artifact13871881801·해시/서명은 유지한다.
- **독립 작업:**c610c10 PR43139267182 일반 CI 전체 성공/artifact13910000313에서 새 픽업 화면과 실제 HTTP 모의 복구 완료 화면을 확보하자마자 채팅 이미지로 표시했다. 일반 Android/테스트 입력/SERVER_MOCK이며 실제 SENS·휴대폰·Pleos 연동 증거로 대체하지 않는다.

## 2026-11-12 03:00 UTC — 실제 전체화면 지문 OCR 누락 재현·동일 원본 독립 확대 판정

- **실제 실행:**ebfa349 수동43143551458/artifact13910264896은 unauthorized였다. 전체 화면에서 지문16쌍을 인식하지 못해 정확한 현재호스트 조건이false, 정상 허용 입력0회였다. 따라서 native 정상 활성화 수정도 실행되지 않았고 설치/속성·서비스/SDK·핀/Console/기기 검사는 생략했다. 새 승인창 원본을 확보 즉시 채팅에 표시하고 앱 화면과 구분했다([근거](evidence/pleos-header-ocr-blocked/summary.json)).
- **원본 재현:**같은 PNG header를 확대해 psm11 원본색과 psm6/40% 두 방식으로 읽으면 각각 USB 제목·RSA/지문 라벨·동일 지문16쌍을 모두 읽었다. 전체화면은 지문0개였다([정제 재현](evidence/pleos-header-ocr-blocked/offline-ocr-regression.json)). 이 offline 비교는 이미지에서 얻은 기준이며 당시 실제 호스트 키 일치/승인 성공을 새로 증명하지 않는다.
- **수정/보안 경계:**전체 제목이 있으나 지문0개일 때만 같은 원본의 두 확대 방식 각각이 모든 문구와 정확한 현재 호스트 지문1개를 독립 확인해야 허용 판정을 보완한다. 전체화면의 다른 지문/중복·확대 문구 누락/다른 호스트는 차단한다. 그 다음 native 대상/정상 포커스 재확인·live ADB 준비 gate는 유지한다. 키/인증/권한/Always allow/에뮬레이터 입력 영역을 바꾸지 않는다.
- **검사/다음:**Python30건·실제 이번 원본 OCR 재현·구문/actionlint/diff 통과. 전체화면 인식 누락이라는 새 실패를 실제 원본에서 재현·수정했으므로 이 변경의 단일 수동 진단을 실행한다. 앱/배포APK/디자인은 유지한다. ebfa349 일반CI verify는 성공, 기기 검사는 진행 중이다.

## 2026-11-12 03:14 UTC — Pleos 준비 단계 통과 관찰·중간 artifact 제공 시점 보완

- **진행 관찰:**3c0ad6e 수동43145130375의 준비 step이 success로 바뀌었고 다음 설치/내비 준비 step은 failure였다. connected 검사는 진행 중이다. 최종 readiness/app-probe 원본을 아직 받지 못했으므로 설치 여부·내비 실패 원인·기기 검사 결과는 미확인으로 유지한다. 실행 중 job 로그 API는 제공되지 않아 상세 결과를 얻지 못했다. 결과를 받는 즉시 문서를 갱신한다.
- **화면 지연 보완:**사용자의 중간 화면 공유 요구에 맞춰 `Collect SDK seat events and app screen` 직후 `pleos-before-connected-tests` artifact 업로드를 추가했다. 이후 긴 connected 검사 종료 전에도 승인/준비/설치/앱 화면 근거를 받을 수 있도록 한다. 최종 전체 artifact는 유지한다. 실제 실행/인증/기기 준비 조건은 바꾸지 않는다.
- **검사/범위:**actionlint1.7.7·diff 통과. 이미 실행 중인43145130375에는 이 단계가 없으므로 종료 후 화면을 받는다. 새 workflow의 조기 업로드 자체는 아직 실행 미검증이다. 변경 확인만을 목적으로 부팅을 추가 실행하지 않는다. 배포 APK/앱 런타임/버전/해시/서명/디자인은 유지한다.

## 2026-11-12 03:32 UTC — 정상 승인/서비스 준비 실제 확인·설치 저장 공간 오류·장기 검사 취소

- **실제 결과:**3c0ad6e 수동43145130375/artifact13912507873. 정확한 현재 호스트 공개 지문·USB/RSA/라벨·native 대상/정상 포커스를 확인하고 정상 Allow1회 후 transport=device, boot1, package/activity found·각rc0이었다. 설치 직전과 connected 전 실제 재확인도 ready였다. 키/권한/인증/Always allow/입력 영역을 변경하지 않았다. [실제 readiness·원본·요약](evidence/pleos-storage-blocked/summary.json).
- **새 차단:**설치가 `INSTALL_FAILED_INSUFFICIENT_STORAGE: Failed to override installation location`로 실패했다. 앱 설치·실행/SDK·핀/Console는 미검증이다. 검정 screencap을 확보 즉시 채팅에 보여 주고 앱 화면이 아님을 명시했다. 당시 guest df/임계값은 수집하지 않아 정확한 여유 공간·오류 내부 원인은 미확인이다. 기존2GiB 테스트 데이터와 다른 profile/RAM/userdata를 이전 승인 실패의 원인으로 소급 단정하지 않는다.
- **검사 취소:**연결 검사 task가 약20분간 종료되지 않아 취소했고 always upload로 증거를 회수했다. AndroidTest APK 빌드/task 진입 로그는 있으나 완료 XML은 없어 통과로 계산하지 않는다. `Failed to start Emulator console for5554` 경고도 있지만 정체 원인으로 확정하지 않는다([정제 로그](evidence/pleos-storage-step-log.txt)).
- **수정/새 목적:**새 job의 자체 빈 userdata만8GiB로 만들고 설정/파일 크기를 일치시켰다. 앱 설치 전 host df·guest df/data·설치 위치·저장 임계값/monitor를 read-only 수집하고 그 뒤 live ADB 준비를 다시 확인한다. 실제 설치 성공 output이 없으면 connected step을 생략하며,20분 command timeout으로 최종 artifact 회수 여유를 둔다. 초기 화면 artifact f670f13도 포함한다. 기존 기기/서버DB/Android 저장 정책은 바꾸지 않는다.
- **독립 검사/다음:**자체 임시8GiB sparse ext4 생성/tune2fs0으로 크기 일치를 확인했다([JSON](evidence/host-eight-gib-ext4-check.json)). 구문/actionlint/diff 통과. Python30건과3c0ad6e PR43145276876 전체 일반 Android CI 성공은 별도 완료 증거다. 새 원본의 실제 설치 실패를 근거로 공간 관찰/크기 변경을 검증하는 단일 실행을 진행한다. 배포 APK/앱 런타임/버전/서명/디자인은 유지한다.
- **환경 제약 추가 확인:**Cloud Desktop 스킬로 현재 환경을 확인했으나 enabled/unknown이며 cua_repl.js/open_in_codex 도구가 없어 실행 화면 제어를 할 수 없었다([환경ID/결과](evidence/cloud-desktop-status.json)). GitHub 실행 중 API blob은 미생성/공개 개별 로그404였고 종료 후 정상 로그를 회수했다. Mac에는 접근하지 않았다. 우회하지 않고 기기 증거만 사용했다.

## 2026-11-12 03:50 UTC — 실제 APK 설치 성공·계기 영역/앱 UI 조작 차단 진단 준비

- **실제 결과:**a8c91d2 수동43148832528의 조기 artifact13914340256을 기기 검사 도중 회수했다. 정상 승인/실제 readiness·설치 직전 재확인 모두 성공했고 APK 설치 true, CAR_SPEED 정상 권한 부여rc0이다. `/data`8107484KiB/여유5548684KiB, storage monitor NORMAL·저장 위치auto였다. 새8GiB 구성에서 설치 공간 오류가 해결된 것을 실제 확인했다. 이전 guest 수치가 없으므로 용량 차이가 과거 모든 실패의 원인이라고 소급 단정하지 않는다([원본/요약](evidence/pleos-installed-ui-blocked/summary.json)).
- **새 UI 차단:**두 내비 시도는 uiautomator dump45초 timeout으로 SDK 호출을 확인하지 못했고 이후 왼쪽 카시트 옵션 라벨이 없었다. screenshot에는0km/h·P/기어 계기 영역만 보이고 도착자리 UI는 없다. 확보 즉시 채팅에 표시하고 설치/앱 표시/실제 SDK를 구분했다. 좌표 배열은 계획한 입력일 뿐 SDK 요청 증거가 아니다. Console는 생략됐고 connected는 조기 artifact 시점 진행 중이다.
- **새 진단 목적/변경:**am start 응답·최초/각 launch 원본·display/활성 activity/window·전원 상태·현재 user·SurfaceFlinger 물리display ID·물리별 원본·uiautomator help/timeout부분출력·마지막XML을 read-only 기록한다. 실제 display 정책/포커스/앱 crash를 아직 구분하지 못했으며 계기 화면만으로 단정하지 않는다. 앱 코드/공식 이미지/디스플레이 설정/인증/키를 바꾸지 않는다. 소스 검토상 무제한이던 permission grant를20초로 제한했지만 이번에는rc0이므로 현재 실패 원인이라고 주장하지 않는다. UI probe 전체12분 INT 제한도 추가한다.
- **검사/다음 실행 근거:**구문/actionlint/diff 통과, 새 진단 모듈의 실제 결과는 아직 미검증이다. 현재 같은 앱의 기기 검사가 이미 실행 중이므로 다음 read-only 관찰 실행은 run_connected_tests=false로 중복 검사하지 않는다. 새 실제 화면/UI dump 실패와 누락된 display 근거를 확보하는 단일 진단 목적이다. 배포 APK/앱 런타임/디자인/버전/서명은 유지한다.
- **공식 데이터 비교:**원본 userdata ext41153433600bytes/메타데이터만 읽었고 파일 내용/키를 읽거나 복사하지 않았다([JSON](evidence/official-userdata-filesystem.json)). source.properties에는 multi_display_window/hotplug_multi_display=no가 있으나 수동 config에는 명시되지 않은 차이가 있다. 이 차이 또는 vendor profile crash를 원인으로 확정하지 않고 먼저 실제 display/activity를 관찰한다.

## 2026-11-12 — 정상 ADB instrumentation 대체 검사 경로 준비

- **근거/목적:**3c0ad6e connected task는 약20분 후 취소/결과 XML 없음, a8c91d2도 기기 검사 진행 중이다. AGP console5554 경고를 원인으로 확정하지 않는다. 정상 승인된 ADB와 테스트 APK로 기존6개 기능 검사를 실행하는 별도 driver를 준비해 Gradle 연결 계층 정체와 앱 검사 결과를 구분한다. 인증/권한/키를 바꾸지 않는다.
- **구현:**device_test_driver=adb 선택 시 assembleDebugAndroidTest·검증용 test APK 해시/개발 서명 기록·live readiness/대상 APK 존재 확인 후 표준 adb install -t/-r와 am instrument -w/-r를 사용한다. 기존6개 클래스만 실행하고 실제 완료 status0 6개·OK(6 tests)·최종 instrumentation code-1·오류 없음이 모두 있어야 통과다. 잘린/실패/중복 status를 성공으로 해석하지 않는다. 모델/프로필 두 프로세스 복원 검사는 별도 step으로 독립 실행한다.
- **격리:**DOCHAGJARI_FRESH_TEST_AVD=1과 --reset-test-data가 모두 있어야 고정된 이 앱만 초기화한다. 사용자 기기/서버DB에 사용하지 않는다. 앱 테스트 입력/모의 알림/사전 영상 검증이며 실제 NaviHelper/좌석/실차/SENS 성공과 다르다. driver 결과를 connectedDebugAndroidTest 통과로 표시하지 않는다.
- **검사/경계:**실제 완료 판정2건과 fresh-device guard1건을 추가해 진단Python33건 통과. 구문/actionlint/diff 통과. 정상 ADB driver의 실제 Pleos 실행은 아직 미검증이며 현재 display 관찰 실행에는 포함되지 않았다. 새 부팅은 실제 display/기기 검사 원본을 읽고 필요 근거를 기록한 뒤 결정한다. 앱 런타임/배포APK/버전/서명은 유지한다.

## 2026-11-12 04:04 UTC — connected 최종 tests0·test APK 설치 commit 응답 없음 확인

- **최종 결과:**a8c91d2 수동43148832528은 failure로 종료/최종artifact13913448564. `connectedDebugAndroidTest`14m35s 후 AndroidTestApkInstallerPlugin의 test APK split install commit에서 ShellCommandUnresponsiveException이 발생했다. [실제 XML](evidence/pleos-installed-ui-blocked/connected-result.xml)은 tests0이며 기능6개가 실행돼 실패한 결과가 아니다. [정제 단계 로그/요약](evidence/pleos-installed-ui-blocked/connected-final-summary.json). Console5554 경고는 별도로 존재하지만 설치 commit 실패의 원인으로 확정하지 않는다.
- **정상 대체 경로 선택:**기존5a6ed1b의 ADB driver에서 단일 test APK를 `adb install --no-streaming -t -r`로 설치하도록 해 AGP split 설치 경로와 구분한다. Android 정상 설치 검사·서명·권한·인증은 유지한다. 승인된 ADB에서 build fingerprint/API level도 읽어 환경을 기록한다. 설치가 실패하면 instrumentation 통과로 계산하지 않는다. 실제 실행은 아직 미검증이고 현재 display 관찰 결과를 먼저 기다린다.
- **검사/후속:**구문/actionlint/diff 통과. 진단33건 통과는5a6ed1b의 실제 로컬 완료 증거이며 이번 단일 정상 install 옵션/환경 메타데이터 변경을 새 기기 성공으로 표시하지 않는다. 앱/배포 APK/버전/서명/디자인은 그대로다. display/activity 관찰43151137564는 준비 통과 후 UI probe 진행 중이며 기기 검사 중복은 껐다.

## 2026-11-12 04:12 UTC — 실제 Pleos 앱 표시 확인·현재 user/정상 터치·전환 확인 보완

- **실제 결과:**8d12198 수동43151137564/조기13913959543/최종13914083961. 실제 앱 설치/launch/물리화면을 확보 즉시 채팅에 표시했다. 첫 am start는 rc0이지만 Status timeout, 첫 캡처는 순정 지도 로딩이었다. 이후 원본에는 도착자리 앱의 픽업·모의 내비/테스트 입력 화면이 보인다. display0/물리1개·Awake·foreground user10이며 MainActivity는 focused/visible/multi-window였다. 다른 display 가설은 지지되지 않으므로 display 정책을 바꾸지 않는다([원본/JSON/XML](evidence/pleos-first-app-display/summary.json)).
- **아직 미검증:**SDK 요청/결과/핀·Console·좌석 수신은 없다. 기존 ADB 탭 후 XML은 도착 안내에 남고 내비 모의/좌석 옵션 라벨이 없었다. 계기 화면만으로 앱 crash/디스플레이 오류를 주장하지 않는다. 이 관찰 실행은 중복 기기 검사를 껐으며 전체 success가 아니다.
- **새 구현:**실제 foreground user를 읽어 install/launch/정상 권한/고정 앱 데이터 초기화/instrumentation/force-stop에 명시한다. user를 전환하거나 다른 계정에 접근하지 않는다. UI dump는 매번 새 진단 XML 경로를 사용하고 rc0이어도 ERROR면 거절해 이전 XML을 소비하지 않는다. 앱 root/탭 전환의 실제 라벨 확인 후에만 다음 조작을 한다.
- **정상 터치 비교:**현재 ADB 좌표 입력으로 전환 확인이 되지 않았으므로 이번 비교는 실제 앱 포커스/물리1개/동일 종횡비 viewport/기하정보·native 입력 영역·정상 창 포커스를 모두 확인한 private X11 에뮬레이터 창에 정상200ms 터치1회를 보낸다. 개인키/인증/Android 입력·display·주행 제한 정책을 바꾸지 않으며 대상 모르면 입력을 차단한다. 제스처/전후 원본/전환 acknowledgement를 따로 기록한다. 전체 화면 비율/포인터 불일치는 거절한다.
- **검사/다음 근거:**실제 viewport/앱 XML 좌표 투영 및 불일치 거절2건을 추가해 진단35건 통과. 구문/actionlint/diff 통과. 현재 실패의 실제 XML/원본과 user10을 근거로 정상 터치/명시적 현재-user 및 표준 단일 ADB test APK 경로의 단일 실행을 진행한다. 새 driver 결과를 Gradle connected 통과로 표시하지 않는다. App/main·서버·배포0.2.0/code2·3a01515/기존 artifact/서명/디자인은 그대로다.
- **맥북:**사용자가 원격 맥북이 아직 꺼져 있다고 답했다. Mac 접근/자원 검증은 하지 않고 Cloud 진단을 계속한다. README 자원/설치/화면/정상 ADB 검사 명령은 재현용이다.

## 2026-11-12 — 진행 문서·새 일반 Android 원본·재현 명령 정리

- docs/08·12에 Pleos 실제 설치/앱 표시와 UI/SDK 미검증, Gradle test APK 설치 단계 tests0/실패, 정상 ADB driver 준비 및9430b7d 진행 상태를 나눠 반영했다. docs/09 다음 프롬프트도 실제 원본/현재user/정상 터치/전환 guard·기기 driver 경계를 유지해 갱신했다. 최종 결과는 후속 기록에 반영하고 마지막 채팅에 프롬프트 본문을 직접 제공한다.
- 새 일반 Android 원본(이미 확보 즉시 채팅에 표시)을 source별 c610c10 픽업/HTTP복구, ebfa349 영상C12/점유우회,3c0ad6e 가상병원/주행제한으로 따로 보관했다. 이전 원본을 덮어쓰지 않았고 실제 SDK/병원/차량/SMS 성공으로 대체하지 않는다.
- README에 맥북 읽기 전용 메모리 압력/공간·guest df·화면 캡처와 정상 ADB instrumentation 재현 명령을 추가했다. Mac 미실행/새 AVD 한정·키/서명/정책 경계를 명시했다. 배포 APK artifact13871881801 미만료, 앱/서버/Gradle 런타임 diff0·버전/해시/서명은 유지한다. 문서는 diff 검사로 확인하고 이미 완료한 앱 검사를 수동 반복하지 않았다. PR#1은 open/미병합이다.

## 2026-11-12 — 남은 profile stop/Gradle 준비 초기화의 현재-user 범위 일치

- 정상 ADB driver/두 프로세스 복원에 이어 UI profile force-stop과 Gradle 전 고정 앱 데이터 초기화에도 읽은 foreground user를 명시했다. user 전환은 하지 않으며 app이 해당 현재user에 없으면 connected task를 시작하지 않는다. 새 job의 전용 AVD에서만 이 고정 패키지를 다룬다.
- 구문/actionlint/diff 통과. 이 명시성 보완은 이미 실행 중인9430b7d 수동43154848171에 포함되지 않는다. 해당 실행의 실제 APK/스크린샷/검사 결과는9430b7d 기준으로만 기록한다. 앱/서버/Gradle 런타임과 배포 APK는 동일하다. 같은 새 AVD의 profile PID/저장값 검증과 표준 ADB driver 결과를 기다리며 보완만을 이유로 부팅을 반복하지 않는다.

## 2026-11-12 — 최신 정상 허용 입력 후 unauthorized 재발·추가 부팅 중단

- **실제 결과:**9430b7d 수동43154848171/조기13915193424/최종13914939268. 현재 호스트 지문/USB·RSA·라벨/버튼·native 입력 영역/정상 창 포커스를 확인해 Allow 입력1회를 보냈으나2초 뒤 원본에도 승인창이 있었고300초 뒤 transport rc1/unauthorized였다. devices rc0·마지막 transport_id1을 기록했지만 전후 history가 없어 reconnect를 원인으로 단정하지 않는다. [원본/JSON/요약](evidence/pleos-approval-unconfirmed-latest/summary.json). 새 원본을 확보 즉시 채팅에 표시하고 앱 화면과 구분했다.
- **실행 경계:**transport에서 막혀 boot/package/activity는 미조회, 설치·정상 host 앱 터치·SDK/핀·Console·표준 ADB 기기/복원 검사는 생략됐다. 새35건/driver가 실제 Pleos에서 통과했다고 쓰지 않는다. 앞선 정상 승인·설치·앱 표시 성공 원본은 별도 증거로 유지한다. 원인은 미확인이며 키/인증/Always allow/user/display/주행 제한 정책을 바꾸지 않았다. 이후 추가 부팅을 요청하지 않았다.
- **완료 검사:**9430b7d PR43154858975 일반 Android CI 전체 성공(JVM33·서버14·lint·진단35·기능6·프로세스2단계·UI13장·HTTP 모의 복구). 로컬35건/구문/actionlint/diff 통과도 기록했다. 후속d6fa66f는 남은 profile stop/Gradle 초기화의 현재-user 명시 보완이며 해당 CI는 별도 확인한다. 앱/서버/Gradle 런타임 diff0·배포0.2.0/2·3a01515·artifact13871881801·기존 해시/서명은 유지한다.
- **전체 상태/다음:**①최신/기기 확인 완료,②실제 승인/설치/앱 표시 성공 증거는 있으나 최신 승인 차단 재발,③실제 SDK/핀·Console/좌석 수신 차단·미검증,④일반 Android 회귀/HTTP 복구 성공·Pleos Gradle tests0설치실패/새 ADB 경로 미실행,⑤문서/증거/다음 프롬프트 정리·디자인 유보·서명/SMS/실차 미검증이다. 맥북은 사용자 확인으로 아직 꺼져 있다. 다음에는 안정적으로 승인된 접근 가능 기기 또는 새 승인/입력/transport 근거를 먼저 확보하고, 같은 실패의 근거 없는 부팅·키 복사/권한 우회는 하지 않는다.

## 2026-11-12 04:48 UTC — 최종 경계/소스 대조·기록 마무리

- [CI 상태 snapshot](evidence/ci-final-status.json):9430b7d PR43154858975 전체 성공, d6fa66f PR43156134619 verify 성공/기기 작업 진행 중. 최종 문서 push의 CI는 별도 실행이며 진행 상태를 성공으로 합산하지 않는다. actual native latest9430b7d는 승인 차단/driver 생략이며 추가 부팅 없음이다.
- [실제 runtime 소스 대조](evidence/runtime-source-comparison.json)에서 app/server/Gradle 관련 경로가3a01515와 diff0·버전0.2.0/code2임을 확인했다. CI 임시 검증 APK의 바이트/서명 동일성 주장이 아니고 배포 artifact13871881801/기존 metadata·해시는 그대로다. unsigned release를 제출 서명 완료로 표시하지 않는다.
- docs/08·09·10·12/README·최신 승인 실패 원본과 PR의 성공/실패/미검증 경계를 정리했다. 구문/actionlint/diff 검사 통과. 키/전화번호/개인 경로·서버DB를 추가하지 않았다. 최종 채팅에는 PR·APK·실제 화면·검증/일지 링크와 다음 프롬프트 본문을 직접 제공한다. PR은 open/미병합이고 Mac은 미접근이다.

## 이후 기록 양식

```text
날짜·시간대:
작업/단계:
목적과 변경:
환경·실행 명령:
실제 결과: 성공 / 실패 / 진행 중 / 차단 / 미검증
증거·커밋·CI/PR 링크:
원인: 확인한 사실과 가설을 구분
다음 작업·완료 기준:
```

## 2026-11-13 KST — M1 8GiB Pleos 로컬 검증

- **범위·원격 기준** — 개발계획을 유지하고 열린 PR #1의 `feat/android-submission-mvp`를 SSD에 복제했다. 시작 HEAD는 `4ec72f6`이며 [해당 커밋의 일반 Android CI 43157275594](https://github.com/leemh-dev/dochagjari/actions/runs/43157275594)는 성공했다. 해당 CI 임시 APK와 기존 배포 APK의 출처가 다르다. 최신 기존 Cloud 수동 Pleos 실행 43154848171은 승인 후에도 `unauthorized`였고, 별도 이전 실행 43151137564는 앱 표시를 확인했다. Mac은 이 두 결과와 독립적으로 측정했다.
- **호스트·SSD** — MacBookPro17,1, Apple M1 8코어, ARM64, macOS 27.0.1, RAM 8GiB. 시작 시 `memory_pressure -Q` 46%, swap 1200.88MiB, 내부 APFS 데이터 볼륨 약 228GiB 중 57GiB 여유, 외장 USB APFS 약 1.86TiB 중 1.54TiB 여유였다. macOS의 이동식 볼륨 접근 창에서 사용자가 허용한 뒤 쓰기와 clone이 완료됐다. Git 2.45.2와 설치된 ARM64 Zulu JDK 17.0.6을 재사용했다. 기존 SDK 디렉터리·Studio·ADB/AVD 설치는 조사한 경로에서 확인되지 않았고, 새 ADB의 초기 `devices -l`은 비어 있었다. 기존 ADB 키·서명 키·자격증명 경로는 이동하지 않았다.
- **공식 자료·설치** — [Pleos 가이드](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)는 v2.1.0, Mac ARM64, API 34, 콜드 부팅, 하드웨어 그래픽, 게스트 RAM 4GiB를 제시한다. Mac ZIP의 `devices.xml`은 4GiB RAM과 2GiB 저장소를, `source.properties`는 8192MB RAM을 지정한다. [Android Studio 호스트 요건](https://developer.android.com/studio/install)은 M1을 최소 CPU로, Studio와 Emulator 조합은 최소 16GB RAM을 제시한다. 이미지 ZIP 4,669,762,816B와 공식 SHA-1을 검증했다. 압축 해제 논리 크기 19,982,411,330B, APFS sparse 추출 실사용 6.4GiB였다. Google 명령줄 도구 23의 `sdkmanager`는 Intel 전용 `android` 바이너리를 호출하며 rc126으로 끝났다. 공식 Google 목록에서 네이티브 Emulator 37.3.9, Platform-Tools 37.0.3, Build Tools 35.0.0을 직접 받아 해시를 확인하고, Java `avdmanager`로 공식 장치 프로필을 사용했다. 수동 설치한 Emulator의 SDK 메타데이터를 추가하고 이미지의 `source.properties`와 일치하는 `android-automotive` 디렉터리를 사용했다. `emulator -accel-check` rc0 및 부팅 로그의 Hypervisor.Framework 확인. Studio 설치는 생략했다.
- **AVD·저장 위치** — SDK·AVD·Gradle·다운로드·임시 데이터·프로젝트를 하나의 외장 볼륨 아래에 배치했다. [실행 스크립트](../scripts/pleos/macos-ssd-env.sh)는 볼륨과 쓰기 권한 및 데이터 경로의 장치 번호를 확인하며, SDK와 Gradle 환경을 해당 명령에만 적용한다. 이 Mac에서는 새 `DochagjariPleosArm64`에 공식 4GiB RAM·호스트 그래픽·콜드 부팅을 적용했다. 여유 공간을 확인하고 `avdmanager`가 생성한 10GiB 데이터 설정을 유지했다. 이는 게스트 디스크 상한이며 RAM과 별개다. 내부에는 macOS·Codex·JDK, 시스템 캐시·swap과 기존 홈의 ADB 인증 설정이 남는다.
- **배포 APK 검증** — [기존 0.2.0/2 배포 artifact](https://github.com/leemh-dev/dochagjari/actions/runs/43055801516/artifacts/13871881801)의 ZIP SHA-256, 세 APK의 SHA-256, JSON 메타데이터를 대조했다. `aapt dump badging`으로 앱 ID·버전·API 34·ARM64 ABI를 확인했다. `apksigner verify --print-certs`로 debug와 androidTest의 동일한 GitHub runner 개발 서명을 확인했다. unsigned release의 제출 서명은 별도 작업이다.
- **첫 부팅·실패** — 사용자 요청으로 Chrome을 직접 종료한 뒤 AVD만 실행했다. 부팅 전 압력 경고/47%, swap 1270.12MiB. 약 1분 뒤 기기는 `emulator-5560/device`로 나타났고 swap 1507.12MiB였다. 준비 helper의 transport/package/activity는 rc0이었으나 그 표본의 `sys.boot_completed`는 비어 있었다. 에뮬레이터 로그에 74,107ms의 boot 완료가 남은 뒤 QEMU 메인/vCPU 응답이 15초 이상 없었고 rc139로 끝났다. 종료 직후 swap 2542.19MiB, 압력 정상/64%, 기기 없음. macOS 크래시 스택은 한 vCPU가 CoreAudio 입력 스트림 초기화에 있고 다른 vCPU와 메인 루프가 I/O 스레드 잠금을 기다리는 동안, QEMU watchdog이 `GenerateDumpAndDie`를 호출했음을 보였다. 최초 APK 설치 전이었다.
- **근거가 있는 변경** — RAM·vCPU·하드웨어 그래픽·가속·AVD 데이터를 유지하고 재실행에 [공식 `-no-audio`](https://developer.android.com/tools/variables)만 추가했다. 시작 전 swap 2526.19MiB, 압력 정상/67%. 앞선 crash report의 전송 확인 창에서 새 실행이 대기한다는 스택을 확보했고, 사용자가 전송을 거절한 뒤 동일 프로세스를 이어갔다. 음성은 이 실행의 검증 범위에서 제외한다.

- **오디오를 끈 단독 실행** — 준비 helper의 transport/device, boot/1, package와 activity 모두 rc0. 첫 캡처의 Android 시작 안내가 이후 기본 차량/내비 화면으로 바뀌었다. ADB 탭으로 앱 목록을 열었다. 초기 탭 명령 약 4.35초, 첫 후속 캡처 15초 시간 초과, 추가 입력 없이 후속 캡처에서 앱 목록 확인. 빠른 설정 탭은 약 143ms, 화면 캡처는 약 468ms였다. 11개 표본에서 서비스가 응답했으며 watchdog 종료가 재발하지 않았다. 메모리 압력은 경고, swap은 시작 2406.19MiB, 최대 3923.81MiB, 10분 3615.69MiB였다. QEMU RSS는 대략 0.9–1.8GiB였으며 VM 전체 메모리 사용량은 아니다.
- **설치·표시** — 실제 `emulator-5560`의 foreground user를 조회해 10임을 확인했다. 설치 직전 `/data` 여유 7,596,288KiB 및 transport/boot/package/activity 각각 rc0을 확인했다. 기존 배포 debug만 `adb install --no-streaming --user <현재 사용자>`로 설치했다. 설치 rc0/Success 약 2.25초, `am start -W` rc0/Status ok, Android TotalTime 3005ms. `MainActivity` focus·논리 display0·물리 디스플레이 한 개·멀티윈도와 실제 화면을 확보했다. 사용자나 디스플레이 전환 없이 진행했고 ADB 상태는 `device`로 확인됐다.
- **앱 조작·자원** — 10분 앱 표본 동안 transport와 서비스가 응답했다. 경고 압력에서 swap 시작 3875.19MiB, 최대 3904.88MiB, 10분 3807.00MiB였다. 표본 수집과 기능 조작이 완전히 겹치지는 않았으며 후속 조작 전후에도 측정했다. 학원 픽업↔주차 도착점 변경, 카시트 문 여유 저장 및 왼쪽 모의 OCCUPIED 안내, 가상 병원 하차점, 모의 주행 중 영상 탐색 제한을 정상 ADB 화면 탭과 실제 화면으로 확인했다. 실제 SDK 내비 핀, Console 좌석, 영상 분석 실행, 실제 SMS와 실차는 별도 검증이다.
- **자원·증거** — [정제한 호스트 표본](evidence/macos-m1-pleos/host-metrics.txt)과 [화면·준비·부팅 로그](evidence/macos-m1-pleos/)를 남겼다. 내부 여유 공간과 swap은 macOS 및 다른 앱의 영향을 함께 받으며, 에뮬레이터나 빌드의 단독 사용량으로 해석하지 않는다.
- **빌드·APK 식별** — AVD를 정상 종료한 뒤 SDK Platform 35를 설치하고 JDK 17, Gradle 8.10.2, `--no-daemon --max-workers=1`, heap 1536MiB, Kotlin in-process로 `:app:assembleDebug :app:assembleDebugAndroidTest` 실행. rc0, 전체 253초, Gradle 67개 작업. SDK XML 버전 경고가 있었고 빌드는 완료됐다. 빌드 전후 압력은 정상, swap 2663.00→2335.88MiB, SSD 여유 1,636,016,832→1,633,865,468KiB. [APK 식별](evidence/macos-m1-pleos/macos-local-apk-identity.txt)에 0.2.0/2, 소스 HEAD, app/test SHA-256, 동일 Mac 개발 인증서 지문을 남겼다.
- **표준 ADB 기기 검사** — 배포 서명과 다른 로컬 APK 쌍을 새 `DochagjariPleosDeviceCheck`에 설치했다. `emulator-5562`에서 현재 user10과 준비 조건 rc0·guest 여유 9,800,440KiB를 확인했다. 이 AVD의 고정 앱/현재 사용자에서만 테스트 데이터를 초기화했다. `direct_instrumentation.py` rc0·108초, JUnit 34.559초, 완료 상태 0 여섯 건, `OK (6 tests)`, 최종 `INSTRUMENTATION_CODE: -1`. 표준 ADB로 사전 영상 분석·앱 모의 상태가 포함된 기존 기능 검사를 실행했다. Gradle connected와 별도 프로세스 재시작 검사는 보류했다. 검사 전후 경고, swap 2518.00→3621.31MiB. 두 AVD를 동시에 실행하지 않았다.
- **최종 상태** — 검사 AVD 정상 종료 후 압력 정상, swap 3265.19MiB. 내부 여유 61,563,872KiB(58.7GiB), SSD 여유 1,629,778,108KiB(약 1.52TiB). SSD에서 SDK 8.2GiB, 두 AVD 7.8GiB, 다운로드 5.2GiB, Gradle 1.5GiB, 앱 빌드 산출물 322MiB를 확인했다. 앱과 빌드·검사는 순차로 수행하는 환경으로 기록한다.

## 2026-11-13 KST — 사용자의 원래 도착 계획 요구와 구현 적합성 검토

- **새 설명:**몰/백화점의 실내 목적지에 맞는 주차 게이트·구역·엘리베이터를 블로그/리뷰 등에서 찾아 내비에 연결하고, 귀환 시 차를 찾는 수고를 AI가 줄이는 것이 핵심이다. 병원/아이 픽업/카시트/휠체어는 확장이다. 목표는 검증/시연영상이며 실제 마켓 출시는 요구하지 않는다.
- **조사/근거:**최신 feat 원격 d15907a를 fetch하고 clean 작업 트리에서 fast-forward했다. AGENTS·기획/데이터/기능/개발계획과 Domain/시설 JSON/AppModel/Gateways를 읽었다. 실내 POI, 자료 검색/추출, 연결 그래프/목적지별 비용 비교, 실제 주차 위치를 연결한 귀환 경로는 미구현이다. 현재 추천은 정적 후보 필터/정렬이고 walking은 미리 작성한 문장이다. 영상 AI/기본 기능 검사 성공을 새 핵심 요구 충족으로 간주하지 않는다.
- **결과:**[요구/구현 차이·냉정한 평가·검증 제안](13-product-fit-review.md)을 별도 문서로 남겼다. 사용 가치는 기대되지만 데이터 정확성/갱신·연결 검증이 핵심이며 경쟁 우위/절감 수치는 미검증이다. 실제 복합시설 한 곳·실내 목적지 두 곳의 검색→연결 계획→내비→귀환 검증을 우선하는 제안이다.
- **범위/검사:**기존 제출 계획/앱 코드/배포 APK를 변경하지 않은 검토 기록이다. 실제/가상, 준비/구현/검증을 구분했다. Markdown 링크와 diff를 확인하며 이미 통과한 빌드/기기 검사는 수동 재실행하지 않는다. PR #1에 기록 링크를 남기고 병합하지 않는다.

## 2026-11-13 KST — 데이터 확보 대응과 입차 후 카메라 위치 기억 요구 추가

- **사용자 요청:**최신 리뷰 우선은 기본이며 공공데이터/공식 몰·시설 사이트/제휴 등 여러 방법으로 정보 부족에 대응한다. 주차장 진입 후 카메라가 주변 기둥·벽 표기를 관측해 실제 주차 위치를 기억하고, 표기가 없으면 확인 가능한 범위와 대체 방법을 사용한다.
- **변경:**docs/14에 채널별 확보/최신성·커버리지, 내비→입차 확인→관측→정차 단서→귀환 상태, 표기 없는 경우/다른 층 동일 번호/이동 재개/카메라 미지원 대체 입력을 설계했다. docs/01·03·04·07·13과 다음 프롬프트/체크리스트에 연결했다. 실제 출시/출시용 서명은 목표에서 제외하고 개발 서명 APK 검증·시연영상 목표를 반영했다.
- **중요 구분:**기존 onRoutingCompleted는 경로 계산 완료여서 입차 트리거가 아니다. 실제 도착/진입과 P/정차 신호 지원을 확인하고 미연동 시 명시한 테스트 입력으로 개발한다. 차량 카메라로 사람이 내려 이용한 엘리베이터를 자동 관측했다고 가정하지 않는다. 순정 카메라 권한과 시설 정보 제휴는 별도다. 기존 SMS 동일 세션 복구 규칙은 유지한다.
- **실제 결과/검사:**요구사항·대응 설계 반영이며 앱 코드/새 데이터 수집/제휴/순정 카메라 연동/새 APK 완료가 아니다. docs/08에도 신규 흐름 미검증을 명시했다. Markdown 링크/diff를 확인하고 기존 빌드·기기 검사는 수동 반복하지 않았다. 기존 앱/서버/배포 APK는 유지하며 PR #1에 기록하고 병합하지 않는다.

## 2026-11-13 KST — 현대 양재 사옥 출발/더현대 서울 매장 방문으로 범위 고정

- **요청/조사:**몰 전체 대신 현대백화점 계열부터 시작하고 현대 양재 사옥을 출발지로 고정하라는 요청을 반영했다. AGENTS·docs/07·08·12와 현재 계획을 읽고 원격 fetch/PR#1 open·feat 브랜치/f601d0b를 확인했다. 이번 작업에서 외부 시설 안내/입점 정보는 조회하지 않았으며 매장 층/게이트 번호·좌표를 새로 확정하지 않았다.
- **변경:**docs/15에 더현대 서울 한 곳·나이키 대표 후보(공식 확인 후 확정)의 사용자 흐름, 조사/모델·추천/자료 검색/앱·내비/관측/귀환/녹화의 개발 단계·완료 조건·일정을 작성했다. 첫 매장 전체 흐름을 우선하고 두 번째는 후속 비교로 축소했다. docs/07·09·08·12에 우선순위·실행 프롬프트·미검증 상태를 반영했다.
- **환경/검사/결과:**Cloud 저장소에서 문서만 변경했다. Markdown 상대 링크와 git diff --check를 확인하며 기존 빌드/기기 검사·부팅은 반복하지 않는다. 고정 출발지는 여행 계획이며 실제 차량 GPS를 바꾼 것으로 표시하지 않는다. 배포 APK·버전·해시·서명과 앱/서버 코드는 유지한다.
- **남은 작업:**공식 자료/최신 관련 리뷰로 사옥 출차 지점·매장 입점/층·더현대 서울 주차 연결을 확보한 뒤 앱 데이터/계획 엔진을 구현한다. 제휴/시설 정보 확보·순정 내비/카메라 연동 완료 주장은 없다. 문서 커밋·푸시 후 PR#1에 링크하고 병합하지 않는다.

## 2026-11-13 KST — 사전 시설 DB·아가방 방문·층별 경로선 요구

- **요청/조사:**매번 실시간 자료 검색 대신 미리 DB에 시설 정보를 보관하고 목적 매장을 아가방으로 변경하라는 요청, 게이트/엘리베이터/층 안내와 가능하면 유아동층 엘리베이터→매장 경로선 요구를 반영했다. AGENTS/기존 고정 시나리오를 읽고 원격 fetch·PR#1 open을 확인했다. 이번 문서 작업에서 아가방 입점·층·도면/실제 거리 외부 조사는 수행하지 않았다.
- **설계 변경:**docs/16에 수집/AI 추출/검토/버전 발행과 앱의 DB 조회를 분리했다. Room/SQLite 사전 데이터, 별칭 조회·오프라인·갱신/차등 동기화 방향, 지도 버전/좌표/복도 그래프/축척, 텍스트 안내 필수·도면 확보 시 경로선 우선순위를 정의했다. docs/03·07·08·09·12·15에 연결했다. 이전 나이키 후보를 아가방으로 바꾸고, 입점이 확인 안 될 때 다른 브랜드로 자동 대체하지 않도록 했다.
- **검사/실제 결과:**문서 변경의 diff 형식·로컬 링크를 검사한다. 앱/서버/DB/경로선 구현·아가방 입점 확인 완료나 새 APK 증거는 아니며 기존 검사/부팅은 반복하지 않는다. 배포 코드/버전·해시·서명을 유지한다. PR#1에 기록하고 자동 병합하지 않는다.
- **다음 작업:**공식 아가방 입점·층/실제 시설 연결·안내도 확보→사전 DB/조회→안내 카드/내비→실제 통로 기반 경로선→주차 관측/저장·귀환 순서. 도면 미확보/축척 미확인은 명시하고 실제 m 거리/현재 위치를 추정해 표시하지 않는다.

## 2026-11-13 UTC — 판교점 변경 허용/공식 아가방 조회

- **요청/환경:**더현대 서울에 아가방이 없으면 판교점으로 바꿔도 된다는 사용자 요청. Cloud에서 curl/표준 urllib로 공개 공식 페이지를 조회했다. 초기 지점 코드 추측은 다른 지점/메인으로 리다이렉트되고 구형 층별 URL은404여서 공식 메뉴의 판교점B00148000·FG 경로와 최종URL/제목을 확인해 바로잡았다. bs4 미설치 보조 파싱 실패는 표준 라이브러리로 대체했다.
- **실제 결과:**판교점 공식 층별 HTML에서 에뜨와·직전 5F 아동/패밀리 제목·브랜드ID를 확인했다. 아가방/AGABANG은0건이다. 더현대 서울 두 페이지 수신HTML도0건이나 동적 전체 목록 미확인이라 미입점을 단정하지 않는다. 에뜨와는 아가방과 별도 대안이며 다른 브랜드로 자동 치환하지 않는다.
- **변경/증거:**정제한 URL/확인시각/필드/응답SHA/한계를 docs/evidence/hyundai-store-check/에 저장하고 docs/07·08·09·12·15·16에 판교점 변경/매장 미확인과 확인된 대안을 우선 반영했다. 최초 양재 사옥 출발·사전DB·엘리베이터 안내/도면 경로·주차/귀환 요구는 유지한다. 다음은 목적 매장 확정과 게이트/엘리베이터/실내 보행 연결 조사다.
- **검사/한계:**문서 링크·git diff --check로 확인한다. 앱/서버/배포APK는 변경하지 않고 기존 빌드/기기 검사를 반복하지 않는다. 실제 영업/현장/내비/카메라 검증이 아니며 PR#1에 기록하고 병합하지 않는다.

## 2026-11-13 UTC — 사용자 선택으로 판교점 5층 에뜨와 확정

- **요청/근거:**사용자가 “에뜨와로 하자”로 목적 매장을 확정했다. 기존 공식 조회 증거의 판교점 5F 유·아동/에뜨와 브랜드 식별자를 재사용했다. 추가 실시간 수집은 반복하지 않았다.
- **변경:**docs/07·15·16의 현재 시나리오를 현대자동차 양재 사옥→현대백화점 판교점 5층 에뜨와→주차/귀환으로 통일했다. docs/08·12에 입점/층 확인과 게이트/엘리베이터/도면·거리 미확인을 분리하고 docs/09 실행 프롬프트를 갱신했다. 과거 조회/개발 기록은 보존한다.
- **검사/결과:**문서 링크/diff 형식 확인. 앱/배포 APK는 변경하지 않으며 기존 빌드/기기 검사를 반복하지 않는다. 기능 구현 완료나 실내 경로 검증 증거는 아니다. 기능별 기록 커밋을 푸시하고 PR#1에 남기며 병합하지 않는다.
- **다음:**판교점 차량 게이트·주차 구역·엘리베이터와 5층 에뜨와까지 보행 연결 확보→사전 DB/안내 카드→내비/실내 경로선→주차 관측·저장/귀환.

## 2026-11-13 UTC — 앱 외부에서 실제 판교점 에뜨와 경로선 예시 실행

- **요청:**앱을 고치지 않고 채팅에서 실제 주차 기둥/게이트·엘리베이터/에스컬레이터·5층 지도 경로를 확인하고 보여달라는 요청. 공식 주차/층별/문화센터 안내와 관련 검색을 조사했다. Cloud 실행 환경 스킬과 상태/네트워크 지침을 읽고 Playwright/설치 Chromium으로 공개 지도를 열었다.
- **실행/실패·해결:**첫 Chromium은 샌드박스 socket차단으로 실패해 승인된 실행으로 전환했다. 기본UA는 빈 모바일 페이지로 이동해 PC UA/환경프록시로 올바른 제목·5F 팝업·map ready·page errors0을 확인했다. 세션유지 시도1회 TargetClosed 후 기존 PC 절차로 재조회해 경로를 완료했다. 출발점8개 비교의 마커 누적을 확인해 최종 캡처에서는 routeOff/clearMarker 후 각 출발점1개로 재실행했다. 브라우저 캡처3장을 채팅에 직접 표시했다.
- **실제 결과:**공식 지도 getRouteOn으로 유아휴게실/유아차대여 쪽 엘리베이터→복도→에뜨와 입구와 에스컬레이터→입구를 각각 계산/표시했다. 공식 지도 좌표/선이며 수작업으로 꾸민 실내 지도는 아니다. 비교원시 거리·시간은 단위/실측 미확인이라 m/분 안내로 쓰지 않았다. 에스컬레이터는5F 도착 후 매장 앞쪽에 있으나 주차장에서의 총 이동/대기 비교는 아니다.
- **주차 조사 경계:**공식 2026-11-27 문화센터 안내의 B3~B6 C1~C3/5·6호기는9F 방문 안내이고 보행7게이트는 차량 진입번호가 아니다. 에뜨와 최적 기둥/차량게이트·위5F 엘리베이터 호기 연결은 확보하지 못했다. 과거CGV/J기둥 팁은 목적/시점이 달라 확정 안내에 재사용하지 않는다. 유모차 동반 엘리베이터 우선은 제안이며 무단차 전 구간/정밀위치 측위는 미검증이다.
- **증거/검사/후속:**docs/evidence/pangyo-route-example/의 공식 원본3장·정제2경로JSON·출처/한계, docs/08·09·12를 갱신했다. 원본DOM/브라우저상태/인증구성/연락처를 공개하지 않는다. 링크/diff 확인, 앱/서버/APK 변경·기기검사 반복 없음. PR#1에 기록하고 병합하지 않는다. 다음은 주차/승강기번호/5F출구 연결과 지도 재사용 범위 확인 후 앱DB/안내 구현이다.

## 2026-11-13 UTC — 채팅용 전체 시나리오/2.5D 지도 파란선 애니메이션

- **요청:**앱 외부에서 하나의 방문 시나리오로 보여주고,5F 지도를 기울이고 회전시켜 엘리베이터→매장 파란 선 애니메이션을 추가한다. 차량용 안내에 보행게이트를 쓰지 않고 기둥/승강기ID가 없으면 근거 있는 설명으로 보완하라는 요청.
- **변경/실행:**공식 지도 같은 카메라의 배경을1회 캡처하고 기존 공식 경로14좌표를SVG로 투영했다. HTML/CSS perspective·32°기울기/-10°회전, 파란선/이동점, 재생/일시정지·단계 선택·슬라이더·평면 전환을 구현했다. 양재→본관주차→고객용엘리베이터/5F→에뜨와→주차단서귀환으로 이어진다. 기둥/승강기 번호를 꾸미지 않고 유아휴게실/유아차대여 등 주변표지로 설명하며 주차·귀환의 추정/시나리오 상태를 표시했다.
- **환경/실패·해결:**Cloud Playwright/Chromium에서 관리정책상file URL 차단을 확인해 표준 로컬HTTP서버로 열었다. 정책/인증을 해제하지 않았다.1280×850 캡처79장/6fps를ffmpeg로GIF/MP4로 변환했다. 실제 기울인 중간 화면을 채팅에 직접 표시했다. 앱/서버/배포APK는 변경하지 않았다.
- **검증/증거:**docs/evidence/pangyo-scenario-preview/의 자기완결HTML·GIF/MP4·원본PNG/모바일화면·브라우저JSON·정합정보/README. 브라우저 오류0, 실제재생/일시정지·단계선택/평면전환 확인,390px 가로넘침없음. 원래분홍선과 투영선39표본 중37개3px 이내/중앙0.39px.5F 경로표현 검증이며 현장최적기둥·엘리베이터호기매칭·실차/측위·앱완료 증거가 아니다.13초 타임라인/실제녹화약13.17초.
- **후속:**주차구역/기둥→승강기→해당5F출구 연결과 재사용 범위 확인 후DB/앱에 옮긴다. 기존 빌드/기기검사를 반복하지 않고 문서/JSON·링크/diff확인 후 기능단위커밋·푸시/PR기록, 자동병합없음.

## 2026-11-13 UTC — 합의한 방문/주차 시나리오의 앱 구현 계획

- **요청/조사:**선택적 좌석설정→에뜨와목적지요청→도로내비→입차→관측/지도갱신→주차요약/닫기 흐름이 앱에서 동작하도록 계획 요청. AGENTS·docs15와 Domain/ProfileRepository/VideoSceneAnalyzer/Gateways/AppModel/MainActivity/Gradle 구성을 읽었다. 현재 프로필은 문/동행조건이고 좌석별탑승역할은없다. 영상은 선택영상 분석이며 연속FrameSource/실내위치/방문상태는신규다. 원격fetch·PR#1 open,42035f6 CI43339266811/43339258708 success 조회.
- **변경:**docs/17에 재사용/부족분·8개커밋단계·상태/종료규칙·네이티브Compose2.5D/PathMeasure·Room/SQLite사전DB방향·관측안정화/처리량초기값·음성/자동화면전환권한경계·기기검사/일정·APK식별 계획을 작성했다. docs/07·08·09·12·15에최신우선순위/프롬프트/미완료를연결했다.
- **중요 결정:**첫APK는테스트입차/관측까지 앱에서이어지고 이후같은어댑터에영상/실제SDK를연결한다. 계산완료≠입차,잠깐정차≠주차완료,경로이동점≠실제측위. 주행중큰방향표시/정차후상세애니메이션,닫기즉시관측중지·동일이벤트재열림방지/기록유지,저장복원후임의카메라재개없음. 주차층→5F연결미확인은테스트/추정으로표시한다.
- **검사/결과:**계획문서의상대링크/diff확인만수행한다. 앱코드/DB/새APK는이번에변경하지않았고기존검사를반복실행하지않는다. 기존0.2.0/2·3a01515·해시/서명유지. 다음버전후보0.3.0/3은구현시원격과재대조한다. PR기록/기능별커밋푸시·자동병합없음.
- **다음:**시설DB/방문상태→Android안에서움직이는지도화면부터구현. 실제SDK/음성/순정카메라·주차호기연결은미검증으로남기고독립작업은계속한다.

## 2026-11-13 UTC — 실행단계 확정/1단계 시설DB·방문복원 구현

- **요청/조사:**단계를정리하고1단계부터개발·GitHub기록요청. AGENTS·AppServices/AppModel/ProfileRepository/MainActivity/빌드·기존검사/CI를읽고원격fetch/PR#1 open을확인했다. docs/18을실제진행표로추가했다.
- **구현:**SQLite 사전시설카탈로그/별칭검색·버전불변/이전스냅샷보존·독립방문저널·같은ID준비/재시작복원·저장실패표시/재시도와정차제한된매장방문탭. 공식판교에뜨와5F·14좌표를동봉하고게이트/기둥/호기/거리미확인은유지한다. 신규unit8건·기기UI1건·기존화면캡처에검색/저장/새프로세스복원3장을추가했다. 현재버전을manifest에서읽는APK메타데이터/새artifact를CI에추가하고기존0.2패키징workflow는보존한다.
- **환경/명령/실제결과:**기존Java17/Android35/Gradle캐시,1worker·1536MiB·offline로 testDebugUnitTest/lintDebug/assembleDebug/assembleDebugAndroidTest/assembleRelease 실행,2m3s성공. JVM41/실패0·오류0(신규8),Python구문/actionlint/diff통과. ADB는샌드박스소켓차단후승인된조회에서연결기기0, /dev/kvm없음. 무가속부팅반복없음. 기기검사/원본화면은CI실제결과대기이며성공으로계산하지않는다.
- **산출물/후속:**앱0.3.0/3으로새APK생성. 기존0.2.0/2/3a01515·해시/서명/보관본은유지한다. 코드커밋후현재APK서명/해시metadata를기록하고전용CI AVD검색/저장/새PID같은방문ID를확인한다.1단계이후2단계지도애니메이션구현은이번범위에서아직시작하지않았다. 증거docs/evidence/stage1-visit-db/·PR#1기록,자동병합없음.

### 1단계 후속 — 코드푸시/APK식별 대조

- 기능코드4d31a15를푸시하고새CI43344530173/43344518024가시작됐다. 실제APK세개의서명/manifest/SHA를추출하면서패키지name정규식이compileSdkVersionCodename접미까지읽는오류를재현했다. 공백경계/정확한app·test패키지assert로수정한3b9a3c5를추가푸시했다. APK바이너리는같은4d31a15코드이며로컬metadata를수정된수집기로재생성했다.
- 로컬debug/test는같은Cloud개발키,release는미서명이다. SHA/지문/버전/코드와세파일크기는docs/evidence/stage1-visit-db/local-apk-metadata.json에기록했다. 기존0.2runner키와혼동/기존앱삭제없이새전용AVD를사용한다. 새CI43344658563/43344649386실행중이며성공으로계산하지않는다.

### CI 생성 성공과 다운로드 차단

- 수정 후 CI43344658563의 verify job이 성공했고 새 artifact14001506355(`development-apks-and-metadata`)가 생성됐다. CI 생성 단계의 실제 manifest·서명 검사와 이 Cloud의 독립 파일 대조를 구분한다.
- GitHub API는 정상 리다이렉트하지만 Azure blob 저장소로의 프록시 터널이403으로 차단됐다. 신선한 아카이브 응답/정상 HTTP 클라이언트에서도 같은 계층임을 확인했다. 환경 변경 요청 도구는 없어 다른 프록시로 우회하지 않는다. 임시 서명 URL·토큰은 공개 증거에 저장하지 않는다. 화면 artifact도 직접 반입 못 할 수 있어 실제 상태/링크를 제공한다.
- PR#1 제목/설명을1단계 실제 동작·새 버전/기존 보관본·현재 검사·기기 대기/다운로드 차단·7단계 후속으로 갱신했다. android-device는 아직 실행 중이다.

### 1단계 완료 — 실제 일반 Android CI 성공

- 수정 후 코드3b9a3c5의CI43344658563이완료됐고verify/android-device모두success다. 수정전기능코드4d31a15의43344530173도전체성공했다. 신규매장기기검사와기존기능/프로세스2단계·새검색/저장/강제종료후새PID같은방문ID캡처절차가포함된다. GitHub API결론/step/artifact를ci-result.json에정제저장했다.
- 신규APKartifact14001506355와기기화면/보고서artifact14001348823을확인했다. 새기기artifact다운로드도같은저장소터널403이라원본을독립열람/채팅이미지로표시하지못했다. 성공한job절차와독립파일대조를구분하고원본링크를제공한다. 이전화면으로대체하지않는다.
- docs/08·09·12·18/README를1단계완료·2단계다음으로갱신한다. 앱기능코드4d31a15/metadata수정3b9a3c5와로컬/CIAPK서명·해시범위를구분한다. 후속문서/증거만추가하므로[skip ci]문서커밋으로동일코드검사중복실행을생략한다.2단계구현은이번에시작하지않았다.

## 2026-11-13 UTC — 2단계 네이티브 지도/파란선 구현과 이동 중 조사 방향

- **요청/조사:**AGENTS/README/docs07~18과 기존 화면/DB/방문 저장·CI를 확인했다. 원격 c4202d3 clean, PR#1 open·3b9a3c5 CI43344658563/43344649386 success다. 사용자는 주행 시간을 자료가 충분해질 때까지 백그라운드 조사하는 데 활용하자는 방향도 추가했다. DB 우선/누락 보완·캐시·충분성/작업한도·같은 방문의 새 계획 revision 보존 방향을 docs/19에 반영했다. 실제 AI 조회 worker는 이번 지도 단계에 추가하지 않았다.
- **구현:**앱0.4.0/4. IndoorFloorPlan/IndoorGuideScreen의 자체 5층 일부 개략도, 기존 공식 보행14좌표·Canvas/PathMeasure·graphicsLayer 기울기/회전·파란선/이동점, 재생/일시정지/처음부터·평면전환·닫기. 매장 방문의 저장된 방문에서 수동으로 연다. 같은ID/원본자료·기존설정/SMS를 보존하고 표시상태만저장한다. 주행/미확인/차량소스변경은상세지도를닫고앱background는재생중지한다. 자동입차/실제측위/음성/카메라/주차연결 성공과구분한다.
- **실패/해결:**호스트Compose검사를추가하며offline캐시에없는JUnit1.1.5/tracing1.1/runner1.5 의존성요구를발견했다. 기존앱/기기검사와동일한캐시버전(JUnit1.2.1/tracing1.2/runner1.6.2)으로test의존성만정렬했다. 호스트PixelCopy는forceRedraw콜백2초timeout이었다. Robolectric native View.draw 직접캡처로전환해같은Kotlin화면을확인했다. 호스트실제렌더에서검은제목/작은지도를보고명시적contentColor·넓은화면이동순서/지도배치·범위/글자분할을수정했다.
- **로컬 결과:**기존JDK17/Android35/Gradle캐시,1worker/1536MiB/offline. JVM45/실패0·오류0,lint/debug/test/unsigned release빌드2m9s성공. 이후같은호스트렌더검사에중간25프레임·10개초과진행값/최종100%확인을추가해해당1건을단독재실행통과했다. 기존검사를근거없이반복하지않았다. Python구문/actionlint/diff확인. 실제기기검사2건과화면17~21(기울기/재생/평면/닫기/새프로세스숨김복원)을추가했으며CI실행전에는성공으로계산하지않는다.
- **증거/후속:**docs/evidence/stage2-native-guide/의로컬로그/검사JSON·호스트flat/tilted PNG·Kotlin실제프레임GIF. 호스트이미지는에뮬레이터/Pleos화면이아니라고채팅에서표시했다. 새코드커밋후APK코드/manifest/서명/SHA와CI결과를갱신하고원본기기화면반입은기존저장소403진단을따른다. 데이터버전/시설/층/출발·도착불일치는지도대신미준비안내다. 공식지도이미지/인증구성을앱에복사하지않았다. PR갱신/기능별커밋푸시,자동병합없음.

### 2단계 실제 기기 회귀 — 일시정지 프레임 경합 수정

- 코드 cad08a9의 CI43355911129에서 verify는 성공했으나 Android API34 기기 검사 신규 `nativeRouteMovesPausesChangesPerspectiveAndClosesWithoutLosingVisit`가 실패했다. 실제 비교값은 일시정지 직후 `동선 재생 24%`→`25%`였다. 전체13건 중 실패1, 별도 절차 전용 process 검사2건은 skip이며 이후 화면17~21·HTTP 복구 단계는 실행되지 않았다. 빌드 성공/호스트 렌더를 기기 통과로 계산하지 않는다.
- `gh run view 43355911129 --log-failed`로 실제 assertion을 확인했다. 진행 중 frame await가 재개된 뒤 Compose effect 취소 전에 한 번 갱신할 수 있어, 프레임 수신 직후 재생/전경/자료 지원 상태를 다시 검사하도록 수정했다. 호스트에도 동일한 정지 후1000ms 진행률 불변 검사를 추가했고 기존 실제 기기 assertion은 유지한다.
- 최초 로컬 Gradle 호출은 샌드박스의 로컬 소켓 제한으로 daemon 시작 전 실패했다. 승인된 동일 offline 빌드 환경으로 재실행하며 기기 결과는 새 코드 CI에서 별도로 확인한다. 이전 APK artifact14006426759는 이 수정 전 산출물이다.
- **수정 후 로컬 실제 결과:**동일 JDK17/Android35·offline·1worker/1536MiB로 JVM45/실패0·오류0, lint/debug/test/unsigned release 전체빌드1m59s성공. 정지1000ms불변/25중간프레임 검사를 포함한다. GitHub CLI는401/일반Git push dry-run은인증없음이어서연결된GitHub앱의정상권한으로fast-forward갱신한다. 강제푸시/인증우회없음. 새기기CI완료는아직아니다.

### 수정 코드의 정상 연결 경로·CI APK 독립 대조

- GitHub앱으로원격cad08a9를기대값으로확인한fast-forward e573e8e를반영하고공개Git fetch/파일보존mixed index갱신으로로컬HEAD를맞췄다. CLI401/인증없는push dry-run은진짜차단이지만연결된GitHub앱은정상권한이다. 수동키주입/인증/프록시우회는하지않았다.
- 새PR CI43358680681의verify성공:Gradle5m3s·서버14건/진단35건통과. APKartifact14006823539를연결된GitHub앱의정상파일참조로다운로드했다. 원래CLI Azure터널403과다른지원기능이며더이상원본반입전체가불가능하다고표시하지않는다.
- 최초APK비교는metadata코드가head와다르다는assert에서중지했다. 조사결과PR workflow의GITHUB_SHA인임시병합718920494f4761e31aa4ee82947c0ea021ec338b였다. 공개refs/pull/1/merge를fetch해부모head e573e8e와동일tree d56e260b794b038fb4f9e9cf086c43b94ba05568을실제대조했다. 자동병합/브랜치변경이아니다. 세APK SHA256·aapt manifest·apksigner개발인증서/미서명여부를각각독립확인해모두metadata와일치했다. 로컬/CI키·해시를구분한JSON/SHA파일을추가한다. 기기job은별도대기중이다.

### 2단계 완료 — 실제 기기 화면/정지 수정/복원 확인

- CI43358680681의verify/android-device모두success를확인했다. 연결된GitHub앱으로기기artifact14008371120(약2.4MB)를실제다운로드·해제하고새PNG/XML/JSON/JUnit을읽었다.17기울기/0%,18실제재생25%,19평면/100%,20닫기,21새프로세스/같은방문HIDDEN을열어채팅중간에이미지로표시했다. 호스트GIF/기기화면을구분한다.
- 실제JUnit XML은9pass(기존6·매장1·신규지도2)/0failure·2프로세스전용skip이다. 콘솔Finished13을13pass로표시하지않는다. 별도process_restart_smoke가새PID/불변요청/저장프로필복원2단계PASS를기록했다. 신규정지검사는assert유지한채통과했고주행/unknown상세지도닫기·재열기차단도성공했다.
- UI21장전체절차·Android영상EMPTY/OCCUPIED·C12·우회/설정·동일방문복원을이번실행에서확인했다. 별도HTTP SERVER_MOCK도응답보류→동일DB서버재시작/빈토큰→같은세션GET200·POST1·공급자0으로성공했다. 실제SMS/Pleos/실차아니다. 테스트용무작위방문/세션ID만증거에남기고실제전화번호/키는없다.
- docs08/09/10/12/18·README·증거·PR을최신결과로갱신한다. 이후문서/증거만추가하므로[skip ci]로같은앱코드검사를중복실행하지않는다. 코드e573e8e/CI임시병합7189204·0.4.0/4·실제APKhash/서명을기록했다. 자동병합없음. 다음3단계선택설정이며AI조회worker/목적지내비·입차/카메라관측·주차귀환/전체시연은남아있다.

### 화면 표시 오독 확인·실험 변경 철회

- 채팅에서20번이미지를열었을때표시를검정으로오독해일시적으로Dialog제거/실제픽셀검사·0.4.1/5를로컬에서시도했다. 하지만수정반영전에원본PNG를Pillow로직접읽어검사하니앱영역비검정비율100%,배경RGB(9,18,27)였고20/21본문의픽셀차이bbox=None으로완전히같았다. 축소본을다시열어정상본문을확인했다. 실제앱검정문제는재현되지않았으며잘못된판단을사용자에게즉시정정했다.
- 이실험은원격브랜치에커밋/푸시하지않았고5개코드파일을정확히기존HEAD로복원했다. 실험빌드2m9s는제출검증성과로계산하지않는다. 앱기능/버전은CI통과e573e8e/0.4.0·4를유지한다. 생성된APK도원래검증버전으로복원해혼동을막는다. 실제원본20/21과픽셀비교결과는보존하며새로운기기부팅/CI중복요청없다.
- 철회후JVM검사중복없이debug/test/unsigned release만1m18s재생성해로컬산출물도0.4.0/4로복구했다. 원래검증로컬APK와세hash/서명을대조했으며최종기기CI/배포판코드는변경없다.
- 재생성한로컬APK의hash는이전동일소스빌드와달랐고개발인증서는동일했다. 동일소스의byte재현성을가정하지않고local metadata/SHA를실제최종파일로갱신했다. CI배포APK의hash/manifest/서명대조결과와CI파일은그대로다.

## 2026-11-13 UTC — 3단계 선택적 좌석 역할 구현 시작

- **조사/원격:**AGENTS·README·docs07~19·기존 Profile/DataStore/AppModel/UI/순차 writer·기기/프로세스 검사를 확인했다. PR#1 open/미병합/mergeable,원격 f097143 clean; 기존 CI43358680681 verify/android-device success. 이과거결과를이번새검사로계산하지않는다. 연결된GitHub앱을사용하고기존CLI401/터널403실패를반복하지않는다. Cloud-runtime스킬/현재환경의정책·자격값없는정상연결을확인했다.
- **환경:**기존JDK17/Android35/Gradle offline캐시·1worker/1536MiB. 승인된adb devices목록0, /dev/kvm없음. 새무가속부팅/맥북실행주장없음. 실제기기검사/새화면은전용일반Android CI에서수행후반입한다.
- **구현:**앱후보0.5.0/5. 지원확인뒷좌석좌우의역할(미지정/성인/어린이/영유아)·카시트사용과NOT_SET/CONFIGURED/SKIPPED를추가했다. 건너뛰기는역할사용만중지하고저장선택/기존이동조건/방문을유지한다. 점유OCCUPIED일때저장선택확인제안만하며역할자동추정/프로필변경없음. 기존profile파일/여섯키와순차저장/status를재사용하고새키만atomic edit에추가했다. 기존방문/알림저널/확정요청스키마는바꾸지않았다.
- **검사 준비:**실제DataStore이행/즉시종료후새store복원·다른키보존·알수없는enum·주입쓰기실패/실제파일유지/재시도,순차쓰기역할포함·순수점유제안검사. 실제기기2건/기존프로세스2단계역할포함과UI22~25를추가했다. readStoredProfile로실제저장값재조회후판정하며UI선택값만저장성공으로보지않는다. 현재로컬빌드실행중이며새CI/기기성공으로표시하지않는다.
- **후속:**로컬검사→기능커밋/푸시·새APKmetadata/SHA/서명→실제CI/앱원본화면확인→docs08/09/12/18/PR갱신. 자동병합없음. AI조회worker/내비·입차/연속카메라·주차귀환은후속이며지어낸기둥/번호/거리/현재위치를추가하지않는다.
- **로컬실패/수정:**첫실행의JVM/lint/debug는통과했지만AndroidTest Kotlin컴파일에서다른모듈의nullable guidance속성smart cast가불가능해실패했다. 테스트에서requireNotNull한로컬val로참조를고정했다. 이미통과한JVM/lint를되풀이하지않고실패/미완료AndroidTest·release빌드만이어간다.
- **로컬완료:**JUnit51/실패0·오류0(새6건),lint/debug통과. nullable참조수정후AndroidTest/unsigned release잔여빌드1m28s성공. Python구문/actionlint/diff통과. 로그두개/실제JUnit집계JSON을stage3-seat-roles에보존한다. 새CI/앱원본은아직대기다.

### 3단계 코드 반영·CI APK 독립 대조

- 기능 c98630d를정상fast-forward로반영했고공개Git fetch/파일보존mixed index동기화로로컬과일치시켰다. PR#1제목/본문을새역할설정·로컬결과/기기대기로갱신했다. 자동병합/강제푸시없음.
- 새CI43364708532 verify success:Gradle3m22s·서버14/진단35성공. 로컬APKs0.5.0/5의실제metadata/SHA와CI APKartifact14008949223을구분해기록한다. 정상연결된GitHub앱의파일참조로CI세APK를내려받고실제byte SHA256/aapt manifest/apksigner인증서·미서명을각각독립대조해모두일치했다. 임시서명URL/키는증거에남기지않는다.
- CI빌드는PR임시병합af004328168ea876e91b531cea4c6525455f314c이며그부모에기능c98630d가있다. refs/pull/1/merge를실제fetch해tree3cad39b3ee964b52892be06bdd4eb07ca78bd3e1이head와동일함을대조했다. PR을실제로병합한것은아니다. 기기job은진행중이며새성공/화면으로계산하지않는다.

### 코드 검토에서 발견한 저장 완료 제안 경계 보강

- c98630d 기기CI가진행중일때검토에서SeatRoleSettings의제안이편집profile.seats를직접쓰는것을확인했다. 저장중/실패에도제안문구가'저장선택'이라고할수있는코드경로다. 실제기기실패/실측으로발견한문제와구분한다.
- 제안은ProfileState의최신revision==savedRevision·SAVED·persisted에있는역할에서만만들도록보강한다. 편집값은별도선택UI에남기고SAVING/ERROR/stale-ack에서는저장제안을숨긴다. 이세경계와정상저장완료제안검사를추가했다. 기능코드변경이므로새검사/빌드후별도수정커밋과새CI를진행한다. 버전은미완성개발0.5.0/5범위에서유지하되코드/hash/서명을분리기록한다.
- **보강로컬결과:**JVM52/실패0·오류0,lint/debug/test/unsigned release전체2m33s성공. stale-ack검사를실제저장역할이있는경우로강화한후SeatPreferencesTest3건만21s재실행통과했다. 이전CI51건과구분하고새코드의기기/화면은후속CI에서확인한다.
- 보강수정a8e440b를fast-forward반영해새CI43366222134가시작됐다. 초기c98630d/CI43364708532와별도이며같은버전0.5.0/5안에서도실제코드/hash/인증서metadata를갱신했다. 초기CI APKmetadata는initial-ci-apk-metadata.json으로보존한다. 최신기기결과는아직대기다.
- **초기코드실제기기완료:**c98630d/CI43364708532 verify/android-device둘다success. artifact14011310449를정상GitHub앱으로실제다운로드해JUnit11pass/2skip·UI25장/역할요약을읽었다. 건너뛰기22·테스트OCCUPIED/왼쪽저장영유아·카시트제안24·재시작25원본을채팅에열었다. 초기이미지3장/요약을initial-c986에보존하고최종a8e440b실행과구분한다.
- **최신코드기본CI완료:**a8e440b/CI43366222134 verify success. 원본JUnit HTML보고서를실제다운로드해52건/실패0/ignored0을독립읽었다. Gradle4m2s·서버14/진단35도이번실행성공이다. 최신APKartifact14010825928을내려받아세파일hash/manifest/인증서·미서명모두일치확인했다. CI임시병합1550a11/동일tree7583555febf511e0443f0199ee9c7847e9edfa91과head a8e440b관계도실제대조했다. 새기기job은아직대기/진행중이다.

### 3단계 완료 — 최신 실제 기기 원본/저장 복원 확인

- a8e440b/CI43366222134 verify/android-device모두success를실제조회했다. 기기artifact14010628050(약2.9MB)를정상GitHub앱으로다운로드/해제해JUnit XML13메서드중11pass/0failure·프로세스전용2skip을확인했다.별도프로세스준비/종료/새PID(7085→7134)2단계PASS와동일확정요청fingerprint·저장프로필복원을원본로그에서읽었다. 콘솔Finished집계를모두pass로표시하지않는다.
- 최신22건너뛰기/23역할·카시트선택/24테스트OCCUPIED저장선택제안/25재실행후역할유지의실제PNG4장을확보즉시채팅에서열었다. 원본PNG/XML·출처/SHAJSON·UI전체25장요약을보존했다. 초기c9863장과최신a8e440b4장을명확히분리했다.
- 실제기기신규2건은저장값재조회/기존6조건·방문/알림요청보존/건너뛰기후지도이용·Activity복원, 점유의역할불변/주행·unknown쓰기차단을확인한다. 프로세스검사는좌우역할/카시트와기존조건을함께확인했다. DataStore옛파일이행/저장직후새store/주입실패·재시도·연속변경·미완료제안차단은단위검사로구분한다.
- UI25장·방문새PID복원·기존Android영상/점유·C12·우회/기타기능성공. HTTP SERVER_MOCK도응답보류→앱종료/동일DB서버재시작/빈토큰→같은세션GET200·POST1/공급자0으로성공했다. 실제SENS/휴대폰수신/Pleos/실차검증이아니다. 실제Mac이전설치본업그레이드는실행하지않았고기존사용자앱데이터는삭제하지않았다.
- README/docs08/09/12/18/20·증거·PR을최종갱신한다. 기존0.4/0.3/0.2APK/metadata를유지하고최신0.5.0/5·a8e440b/CI임시병합1550a11·실제hash/서명을별도기록했다. 이후문서/증거만[skip ci]커밋해같은앱코드검사를중복실행하지않는다. 자동병합없음. 다음4단계목적지/내비/입차이며AI조회worker·연속카메라/측위·주차귀환/전체시연은남아있다.

## 2026-11-13 UTC — 4단계 시설 목적지/내비/명시적 입차 시작

- **조사:**AGENTS·README·docs07~20·stage3원본metadata/검증과기존VisitGuidance/SQLite/게이트웨이/AppModel/CI를확인했다. PR#1 open/미병합,원격89b5d81 clean·기존CI43366222134 verify/android-device success. 이기존결과를이번실행으로계산하지않는다. Cloud-runtime스킬/현재무자격환경의정책을읽고기존CLI인증실패를반복하지않고정상GitHub앱을사용한다.
- **공식조사/실패해결:**공식판교점WC위치/주차URL을표준HTTP로조회하다샌드박스소켓Operation not permitted를확인해승인된같은요청으로HTTP200/138930bytes를받았다. 시설주소판교역로146번길20,지도마커37.3927854/127.1120699를확인했다. 이는차량게이트가아니라시설대표좌표다. 정제발췌/원문hash만보관하고지도SDK키/무관스크립트는GitHub증거에넣지않는다.
- **구현:**0.6.0/6. 별도FACILITY_POI road-target자료·요청별snapshot,기존VisitRecord에선택적journeyJSON·옛JSON빈값복원. SQLite버전/자료/방문ID·설정·알림저널은유지한다. FIFO방문IO/준비→전달claim영속기록→외부어댑터순서,동시claim한번/재시작자동요청없음·취소/늦은콜백/새요청이력·중복입차닫힘유지. SDK요청함수/요청ID없는이벤트/실제핀/입차를분리한다. 명시USER/TEST입차저장후정차일때기존지도·같은방문으로연결한다. 실제자동입차/카메라/주차확정은구현했다고표시하지않는다.
- **검사:**첫로컬JVM62/실패0·lint/debug/test/unsigned release빌드2m25s성공. 이후기기일괄검사에서서로의입차상태에의존하지않도록취소→주행/unknown→명시입차/복원을하나의시나리오검사로묶고,기존프로세스2단계에동일방문/요청의전달구간종료복원을추가했다. UI26~29를준비했다. 로컬계속검증중이며실제기기성공으로아직표시하지않는다.
- **검토보강/최종로컬:**SDK함수반환과실제콜백을서로다른상태로구분하고요청ID없는오류이벤트도'이요청일치미확인'으로남긴다. 같은방문재요청은기존목적지snapshot을고정해새bundle과화면/전달불일치를막았다. SDKhelper중복/저장중차량상태변경을보강했다. 이생산코드변경에따라로컬검사를갱신했으며마지막JVM62/실패0·오류0/lint/세APK1m43s성공. 앞선중간검사1m49s/1m45s와최종결과를분리해로그보존했다. Python구문/actionlint/diff통과. 승인된adb목록0·KVM없음을재확인했고무가속부팅/접근없는Mac실행주장없음. 실제기기CI는아직이다.
- 기능f959cb9를fast-forward반영하고정상Gitfetch/파일보존mixed index동기화를확인했다. 새CI43371857767진행중. 로컬0.6.0/6 APK세개의실제manifest/SHA/서명을코드f959cb9로기록했다. 새기기/SDK/핀성공으로아직표시하지않는다.

### 새 CI 기본 검사·APK 독립 대조

- CI43371857767 verify success. Gradle4m35s·서버14/진단35성공을원본로그에서읽었다. report artifact14013126386도실제로내려받아JUnit HTML의62건/실패0/ignored0을확인했다. 실제기기는별도진행중이다.
- APKartifact14012324593의세파일을정상GitHub앱파일참조로내려받아bytes SHA256/aapt manifest/apksigner인증서·미서명을각각대조해모두일치했다. CI는임시PR병합58b5fe1ace1c8f256400018e25b97429a9bce8ad를빌드했고Gitfetch로동일tree2ec1f141efeb24a09b02f372a45b37ea01db7ef9·부모f959cb9를대조했다. PR실제병합/인증우회없음. 로컬/CI/Mac키와hash를구분하고기존APK/사용자데이터를유지한다.

### 4단계 완료 — 최신 실제 앱 흐름/복원 확인

- f959cb9/CI43371857767 verify/android-device모두success. 기기artifact14013477464(약3.6MB)를정상GitHub앱으로실제다운로드/해제하고JUnit14메서드중12pass/실패0·프로세스전용2skip을확인했다. 콘솔Finished숫자를모두pass로세지않는다.
- 새26시설대표좌표/모의내비/입차없음→27명시TEST입차/기존네이티브지도→28닫기HIDDEN→29새프로세스/같은방문PARKING_GUIDANCE·HIDDEN의원본PNG4장을확보즉시채팅에서열었다. 같은실행18의수동미리보기재생26%도별도사례라고표시해열었다. CI동작줄이기로27은재생대기0%이며입차후자동재생을실측한것으로표시하지않는다. PNG/XML/출처SHAJSON을보존한다.
- 신규기기1시나리오는모의계산의입차비전환·취소/재요청/늦은결과·주행/unknown차단·명시TEST입차·동일방문/원본자료/설정/알림보존·닫기/반복입차재열림없음/Activity재시작을확인했다. 별도프로세스준비/종료/새PID7370→7420에서동일방문/요청ID·DISPATCHED/이벤트없음/입차없음과기존역할/프로필/확정알림fingerprint복원도PASS했다. 실제SDK응답유실실측이아니라claim후외부전달전종료를명시적으로검사한것이다.
- UI29장/기존영상·점유/C12·우회/역할/방문복원과HTTP SERVER_MOCK도성공. 응답보류→동일DB서버재시작/빈토큰→같은세션GET200·POST1/공급자0. 서버DB삭제/새세션재발송으로복구하지않았다. 실제SENS/휴대폰수신·Pleos/실차가아니다.
- 배포artifact는verify job에서hash/manifest/서명을독립대조했고기기job은같은소스를별도runner에서재빌드한것이다. 배포APK와기기APK의byte/서명이같다고단정하지않는다. 기존Mac설치본업그레이드/실제SDK/핀·자동입차·카메라/측위는실행하지않았다.
- README/docs08/09/12/18/21·원본/PR을갱신하며이후문서/증거만[skip ci]커밋해같은앱검사를중복실행하지않는다. 앱0.6.0/6·f959cb9/임시병합58b5fe1·실제hash/서명을기록했고기존0.5/0.4/0.3/0.2보관본·사용자데이터유지. 자동병합없음. 다음5단계관측/갱신이며차량게이트/승강연결·실제카메라/AI자료worker·주차귀환/전체시연은남아있다.

## 2026-11-13 UTC · 5단계 같은 방문 관측/추천 갱신 착수

- AGENTS/README/docs07~21·4단계 원본/metadata·PR#1/최신CI를 확인했다. PR OPEN/head9fb5085, 기능f959cb9/0.6.0·CI43371857767 성공이며 기존 결과를 새 실행으로 세지 않는다. 열린 feat/android-submission-mvp에서 기존 DB/자료/설정/방문/알림/보관APK를 보존한다.
- Cloud runtime skill/환경 확인 후 ADB devices -l 목록0·/dev/kvm없음을 확인했다. 과거 unauthorized/창대상 미확인 진단을 새로운 성공으로 대체하거나 무가속 부팅을 반복하지 않았다. Mac/실차/외부카메라 접속을 실행하지 않았다.
- 입차한 같은 방문에 source별 관측 저널/문자·시각·점유·종료사유를 추가했다. 옛 JSON 기본값·FIFO 저장/제한·늦은 프레임/중복 차단,2프레임 안정화·낮음/오래됨/표기없음·동일번호 층충돌을 구현했다. 연결 근거 없으면 주차층/승강기/현재위치는 확정하지 않고 기존5층 동선을 유지한다. MLKit 점수는 null이며 생성 C12를 실제 판교점으로 취급하지 않는다.
- 수동/생명주기/주행/권한 철회 중지·generation 차단·native finally 정리·앱 공통 분석 Mutex를 추가했다. URI 권한을 프레임 간 확인한다. 완료/종료기록 없는 저널도 자동 재개하지 않는다. native 진행 호출의 즉시 중단을 보장하지 않는다.
- 첫 로컬 Kotlin compile1m1s·JVM/린트/세APK2m10s 성공. 이후 추가한 생명주기3건(늦은 비협조 소스/권한 오류/저널 자동재개 금지)43s 성공. 기기 시나리오/UI30~37와 프로세스 동일관측 복원을 보강한 최종 전체 검사는 진행 중이다. 실제 Android/Pleos 성공으로 아직 표시하지 않는다. 도중 화면/진행을 채팅에 공유했고 CI artifact는 기기job 종료 후만 확보 가능함을 밝혔다.

- 기기 스크립트 사전 검토에서 ffprobe로 생성 영상 길이가4.0초임을 확인해 OCR 처리간격2초의 기대 저장프레임을2로 수정했다. CI 실패를 재현한 것이 아니라 실행 전 fixture/기대값 불일치 보정이다. 실제 기기 결과는 후속 CI로 확인한다.

- 최종 로컬 JVM77/실패0·lint/세APK1m56s 통과, 이후 주행 상세 숨김/문구 수정의 lint/세APK 패키징도 통과했다. Python구문/actionlint/diff 성공. 새 CI 기기 검사는 다음 커밋에서 실행한다. 원본 로컬 로그는 stage5-visit-observation에 보존한다.

### 5단계 기능 푸시와 새 CI

- ef3f306cb752ae5d1817216faa008b3b756d53d9를 expected_sha9fb5085/force=false로 fast-forward하고 정상 Git fetch/mixed 인덱스 동기화로 파일 보존/clean을 확인했다. PR#1은 OPEN/unmerged로 유지하고 작업/차단 현황을 갱신했다. 새 CI43388009453 시작, verify 실행 중이다.
- 로컬 최종 UI 패키징1m29s 성공. 기능커밋ef3f306 기준 실제 manifest0.7.0/7·세APK SHA256·개발 debug/test 동일 인증서와 release 미서명을 수집했다. local-apk-metadata.json/local-SHA256SUMS에 기록하며 CI runner 결과와 혼동하지 않는다. 실제기기CI/새화면은 아직 대기다.

### 새 CI 기본검사 완료 · 실제 기기 준비 중

- CI43388009453 verify success. 원본job로그에서 Gradle4m36s·서버14/진단35 성공을 확인했다. 실제 Android job131762989031 준비 중이다. 배포APKartifact14019243185와보고서14019910987을 정상 GitHub앱 파일 참조로 다운로드/독립 대조 중이다.
- 실제 Git refs/pull/1/merge를읽어임시f6c552e8ae4718e30d15002754e35abca373e394/기능ef3f306의tree5376de1e0bfe7fe04f8b5c6d22d156c8d4fabff8일치를확인했다. PR을병합한것이아니다. 앱원본화면은기기job종료후만artifact로제공돼대기중이라고채팅에알렸다.

- 정상앱으로다운로드한보고서원본HTML에서JVM77/실패0/ignored0을확인했다. APK세파일 bytes/SHA256·aapt manifest·apksigner인증서/미서명도모두독립일치했다. ci-apk-comparison/metadata/SHA와ci-verify요약에기록한다. 실제기기검사는별도진행중이다.

### 실제 기기 회귀 발견 · 앱 전용 파일과 문서 URI 구분 수정

- CI43388009453 android-device 실패. 원본artifact14019563596 XML14메서드중11pass/1fail/2skip을다운로드해읽었다. 새 VisitRoad의관측/생성C12/닫기/주행시나리오는PASS였지만기존videoInputChanges...에서expected VIDEO / actual GENERATED가실패했다. connected실패로프로세스/화면/HTTP후속은실행안됨이며새화면성공으로표시하지않았다.
- 실제실패원인은추가URI검사가앱이소유한cache file://입력에도문서grant를요구한회귀다. 소스의SecurityException반환경로와실패결과를대조했다. 앱cache/files canonical하위의읽기가능한파일만직접허용하고외부content문서는계속정상read grant를요구하도록수정했다. 유사prefix외부경로/권한없는content는거부한다. 인증/권한검사를해제하지않는다.
- 새단위4건(cache/files허용·유사경로거부·미승인content거부)을추가해전체검사/빌드중이다. 실패요약은first-device-failure.json이며수정후새소스CI로회귀와후속화면/복원을확인한다. 기존실패를숨기거나새기기성공으로대체하지않는다.

- URI 단위검사 첫 실행에서 Robolectric 기본 checkUriPermission이 허용을 반환해 미승인 content 기대가 실패했다. 실제 OS 거부를 재현한 것이 아니므로 ContextWrapper로 명시 GRANTED/DENIED를 주입하고 허용/거부 두 경로를 검증하도록 보정했다. 총 신규 경계5건이며 실제 OS 철회와 구분한다.

- 실제 수정후 로컬 JVM82/실패0·lint/debug/test/unsigned release 전체1m50s 성공. 새 소스 커밋/CI로 기기 영상 회귀와 프로세스/화면 후속을 확인한다. 원본 uri-fixed-local.log에 기록한다.

- 회귀수정8ea046a65e69b0c15749683cf979e0df26eae536을expected_sha ef3f306/force=false로fast-forward했고파일보존인덱스동기화를확인했다. 새CI43390462215진행중이며PR에첫실패·수정·새검사상태를명시했다. 앱버전0.7.0/7을유지하고서로다른코드/바이트/서명을metadata로구분한다. 첫APK는first-*로보존하고수정로컬APKmetadata를갱신했다.

- 새CI43390462215 verify success/Gradle5m19s·서버14/진단35를원본로그에서확인했다. report artifact14021120849의HTML은JVM82/실패0/ignored0이다. 새배포artifact14021185433도세APKbytes/SHA256·manifest·개발인증서/미서명모두대조일치했다. 독립대조정규식의필드경계/이스케이프오류를수정해일치를확인했으며APK자체실패가아니다. 임시5894f438/기능8ea046a의실제tree af875b388905631af7c67bf20ce74046e4d7fb25도일치한다. 실제기기는별도진행중이다.

### 5단계 완료 — 수정후 실제 Android 원본/복원·화면 확인

- 8ea046a/CI43390462215 verify/android-device모두success. 기기artifact14022042145(약4.1MB)를정상GitHub앱으로실제다운로드하고원본XML14메서드중12pass/실패0/2skip을확인했다. 기존선택영상회귀도PASS. 새관측시나리오의TEST층충돌/낮음/오래됨/표기없음·Android생성C12/문자점수null·닫기/주행중지/저장재조회·재시작이통과했다.
- 프로세스전용2메서드는일괄검사에서skip후별도준비/강제종료/새PID7575→7626에서같은방문/내비요청/관측ID·2TEST프레임/불완전저널·기존프로필/좌석역할/동일확정알림을복원했다. source/내비자동재개없음. 명시미완료저널fixture검사이며실제카메라활성중강제종료실측이아니다.
- UI37장/관측신규8장성공.30TEST반복문자후보/35실제Android생성영상2처리프레임·C12/36출처·미확인지도와31B3/B4상충/37새PIDidle원본을확보즉시채팅에이미지로표시했다.36은CI동작줄이기의재생대기0%라자동재생/현재위치기반선갱신으로표시하지않았다. PNG/XML·파일SHA·run/source를보존했다.
- UI관측강제종료도11670→13133에서같은방문/저장2프레임과자동재개없음·오래됨후보보류를확인했다. readiness는transport=device/boot1/package/activity found·각rc0이다. HTTP SERVER_MOCK도같은DB서버재시작/응답보류/빈토큰→같은세션GET200·POST1/공급자0으로성공했다. 실제SENS/휴대폰수신이아니다.
- CI기기job실행중세부단계를볼수없어완료기준을앞당기지않고대기했다. 실행중로그조회1회는BlobNotFound404였고앱실패로표시하지않았다. 종료후완료로그/원본을확인했다. UIartifact는job종료후에만제공돼그시점과차단을채팅에공유했다.
- README/docs08/09/12/18/22·증거·PR을최신수준으로갱신한다. APK는0.7.0/7·코드8ea046a,CI임시5894f438/tree af875b3·세파일실제hash/개발서명/미서명을구분한다. 첫ef3/첫CI실패와metadata는first-*로보존한다. 기기APK는같은소스별도runner재빌드이며배포byte/서명과동일을가정하지않는다.
- 연결근거없어parking=null·주차층/현재위치·승강연결미확인·기존5층선유지다. 실제Pleos SDK/핀·외부카메라/측위·실차·새관측OpenDocument/공개영상실행/실제OS철회·AI조회worker는미검증/미구현. native강제중단/메모리성능을실측했다고주장하지않는다. 가상병원/모의SMS·기존DB/방문/설정/알림/이전APK보존,자동병합없음.
- 후속은6단계주차완료·요약/닫기·귀환과7단계같은APK연속시연이다. 다음실행프롬프트를파일과최종채팅에직접제공한다. 이후문서/증거만[skip ci]커밋해같은앱검사를근거없이반복하지않는다.

## 2026-11-13 UTC · 6단계 주차 완료·요약/귀환 착수

- AGENTS/README/docs07~22·5단계원본/metadata와PR#1/최신원격·CI를확인했다. OPEN/head ec21ce8/코드8ea046a·CI43390462215 completed success이다. 로컬은같은feat/android-submission-mvp/clean이며이전검사를새실행으로세지않는다. Cloud runtime skill/현재환경·네트워크정책을확인했고ADB devices -l목록0·KVM없음. 새근거없는부팅·Mac접속/인증우회없음.
- 앱0.8.0/8. 같은방문의별도parked JSON에명시완료/차량상태출처·수동단서revision/확인시각·이동재개이력을추가했다. SQLite schema1/자료 snapshot·방문ID·road/observation/legacyParkingClue·기존설정/역할·알림을보존한다. OCR후보나과거단서를새확인위치로복사하지않는다.
- application FIFO저장후currentVisit재조회/전체값일치ACK,중복완료/수정ID멱등성·연속저장/미저장폼구분·동일요청재시도·기록한도시보류를구현했다. 주차완료전관측generation무효화/원자적저널중지. ACK후한번만요약/기본8초닫기·명시귀환·알수없는위치/승강연결/복귀선을유지한다.
- 닫기/탭이탈/ON_STOP/해제/종료/주행·unknown에서화면/관측중지,재시작자동열기없음을보강했다. stopped=false수신또는명시다시찾기는이전주차에재개출처/시각을남기고같은방문PARKING_GUIDANCE로돌아가되관측/지도/내비를자동시작하지않는다. 저장중이동도ACK후예전기록으로전환하고요약을띄우지않는다.
- 첫Kotlin compile53s,로컬JVM94/실패0·lint/세APK2m42s성공. 이후저장실패·ACK유실·저장중이동AppModel3건과기기확장·프로세스동일주차/단서복원·집중UI14장을추가한최종검사는진행중이다. 새실제기기성공으로아직표시하지않는다.
- CI는기존기기회귀/HTTP모의복구를유지하며UI는새주차흐름14장에집중하도록바꿨다. 기존37장을근거없이중복캡처하지않고7단계전체시연에서연속흐름을검증한다. 공통readiness로연결/부팅/package/activity·각rc0을connected/설치직전에재확인하도록추가했다. 새Python구문/actionlint/diff통과. 기존ComposeXML의EditText/label부모관계를읽어필드선택기를구성했으며추정좌표를누르지않는다.

- 생명주기3건을포함한중간최종JVM97/실패0·lint/세APK2m18s성공. 검토에서쓰기후ACK/재조회유실은이전값롤백으로단정할수없어UI를저장결과미확인으로바꿨다. ACK유실뒤이동한경계도같은과거ID복구/이전기록전환·현재주차/팝업없음으로확인하는4번째생명주기검사를추가했다. 최종98건과빌드는실행중이다. 기존설정/알림/방문·실제동선은보존한다.

- 최종 검토후JVM98/lint/세APK2m21s성공. 저장중새입력을완료로잘못표시하지않는추가검사1건을넣어앱코드변경없이JVM만갱신했고최종원본XML99/실패0/errors0/skip0·48s성공을확인했다. 신규17건(규칙/SQLite12·AppModel5)이다. 새기기는아직대기다. 캡처기는실제IME표시를확인한경우만Back을보내도록보강했다. Python구문/actionlint/diffPASS.

### 6단계 기능 푸시·CI 시작

- 84acdd374521c4df63d82352cc86f03a219213f0을expected_sha ec21ce8/force=false로fast-forward하고파일보존Gitfetch/mixed인덱스동기화/clean을확인했다. PR#1은OPEN/unmerged로유지하며구현/로컬검사·미검증을갱신했다. 새CI43404749340진행중이다.
- 로컬APK0.8.0/8세파일의실제manifest·bytes/SHA256·개발debug/test동일인증서/unsigned release를기능커밋84acdd3로수집했다. CI/Mac서명과같다고가정하지않는다. 정상Gitfetch로CI임시9a1ede3d9848b2c686a73fb4720dad430226a0aa와기능tree f6ddd1844a84dbe84eaa2bd3d6e8873ce7d6d1bd일치도확인했다. PR실제병합이아니다.
- 다음7단계프롬프트초안을저장했으며새실제기기/화면/CI결과후상태를최종갱신한다. 기기artifact는job종료후만제공되므로제약/시점을채팅에알렸다.

- CI43404749340 verify success/Gradle5m35s·서버14/진단35를완료원본로그에서확인했다. APKartifact14027490674와report14027027388을정상GitHub앱파일참조로다운로드했다. 실제원본HTML의JVM99/실패0/ignored0과세APKbytes/SHA256·aapt manifest·apksigner개발인증서/미서명을독립대조해모두일치했다. ci-verify/ci-apk-metadata/ci-apk-comparison에기록한다. 기기job131818679615는별도진행중이다.

### 6단계 완료 — 실제 주차/귀환 원본과 새PID 복원

- 84acdd3/CI43404749340 verify/android-device모두success. 기기artifact14028535017(약1.96MB)을정상GitHub앱으로다운로드해원본XML14메서드중12pass/실패0/2skip을확인했다. 확장VisitRoad한시나리오에서8초자동요약닫기/반복완료재열림없음·관측자동승격없음·TEST/USER단서/연속저장·SQLite재조회·귀환/Activity재시작/주행/이동재개·새완료미확인을확인했다. 기존설정/좌석·알림/방문/자료를보존했다.
- 프로세스전용2메서드는일괄skip후별도준비/종료/새PID7411→7464에서동일방문/road/관측/주차/단서ID·TEST출처/시각·기존프로필/역할·동일확정알림을복원하고모든화면/관측/내비자동재개없음을확인했다. 명시fixture이며실차/카메라활성종료실측이아니다.
- 집중UI14장성공(이전37장재실행아님). 04주차미확인요약/05TEST입력미저장/06TEST단서저장과08USER검증용메모귀환/10새PID복원/12TEST이동후이전기록원본6장을확보즉시채팅에이미지로표시했다. 04는자동닫기끄기이며8초동작은별도실제기기검사다. 02/09지도는동작줄이기의재생대기0%/매장행개략도이며자동재생/복귀선/측위성공으로표시하지않았다. PNG/XML28개원본/hash/run/코드를보존한다.
- UI새PID7528→8964에서도같은방문/PARKED·HIDDEN/USER메모manual-demo와화면/관측자동재개없음을확인했다. 메모는정상EditText부모를확인해입력하고실제IME표시를확인한경우만Back을보냈다. 일반Android수동입력시험이며현장확인이아니다.
- connected/설치직전readiness2개원본은transport=device/boot1/package·activity found·각rc0/빈stderr로통과했다. HTTP SERVER_MOCK도같은DB서버재시작/응답보류/빈토큰→동일세션GET200·POST1/공급자0으로성공했다. 실제SENS/휴대폰수신이아니다.
- README/docs08/09/12/18/23·증거·PR을갱신한다. 앱0.8.0/8·코드84acdd3/CI임시9a1ede3d/tree f6ddd184와세APK실제hash/개발서명/미서명을기록했고기기APK는동일소스별도runner재빌드라는수준을유지한다. 첫CI에서기본/기기모두통과했으며앱소스를추가변경하지않았다.
- 실제Pleos/외부카메라/측위·주차승강연결/복귀선·실차·실제OS철회·새관측문서/공개영상경로·SMS수신·AIworker는미검증/미구현. 기존DB/자료/방문/설정/알림·이전APK/사용자데이터를유지하고서버DB삭제/새세션재발송/자동병합없음. 다음7단계는같은APK의전체시연영상/제출증거대조다.
- 문서/증거만[skip ci]커밋해통과한앱검사를근거없이반복하지않는다. 다음실행프롬프트를파일에저장하고최종채팅에직접제공한다.

## 2026-11-13 UTC · 사용자 요청에 따른 UX 설계 선행

- 기능검증완료와제품UX완성을구분하고7단계최종녹화앞에내비연결UX설계/Android적용을배치했다. PR#1 OPEN/head9248a92·로컬clean을확인했다. AGENTS/기존계획·실제Mac Pleos참조/현재앱화면·NaviHelper연동범위를읽었다. Android코드/DB/버전/APK/CI를변경하지않았다.
- 지도중심/행동카드6장면을독립HTML로설계했다. 공식스타일인증이아닌Pleos참고형제안이며내비내부카드/분할은조건부미확인,기본정상앱전환과구분했다. 도로는구성모형이고실제경로가아니다. 5층은기존14점/블록을재사용했고미확인주차층/호기·위치/복귀선을만들지않았다.
- file URL이관리정책으로차단돼설정을변경하지않고정상루프백HTTP서버/Chromium·Playwright로렌더링했다.6장면·경로재생/완료·주행상세숨김/계획조작제한·닫기경로제거를확인했다. 호스트시안8PNG를확보했고실제Android/Pleos성공으로세지않는다. 원본출처/hash는ux/arrival-v1/review.json에보관한다.
- 긴텍스트/테스트조작·UUID/SQLite정보를사용자화면에서분리하는규칙,안전상태/모션/데이터부족동작과공식내비연결확인게이트를docs24에기록했다. README/docs08/09/12/18·PR을설계수준으로갱신하고문서/시안만[skip ci]로기록한다. 앱84acdd3와기존설정/방문/알림/사용자데이터·APK보존/자동병합없음. 다음은시안검토반영·정상플랫폼지원범위확인후Android UI적용이다.

- UX최종검토에서주행→정차토글이상세지도자동재생으로돌아갈수있는시안동작을발견했다. 간단안내상태로유지하고명시열기후에만상세재생하도록수정했다. 별도호스트브라우저검사로주행숨김→정차시닫힘유지→명시열기→닫기중지를PASS했고07캡처/해시를갱신했다. Android코드/검사는변경/재실행하지않았다.


## 2026-11-13 UTC · 내비 연계 Android UX 적용 / 실제 Pleos 확인 경계

- 조사: AGENTS·README·기능/검증/단계/UX문서와 PR1 원격 HEAD cf7a3b6(open)을 확인했다. 사용자는 UX 시안을 Android에 적용하고 가능하면 실제 에뮬레이터 내비를 확인하도록 요청했다. Cloud runtime 설정은 기존과 같고 등록된 원격 접근 정보/기기/KVM이 없었다. 사용자도 Mac이 아직 꺼져 있다고 답했다.
- 실제 명령: `adb devices -l` rc0/목록0/빈stderr, 공통 `probe(adb, emulator-5554)` transport rc1/빈stdout/`device not found`. boot/package/activity는 transport 미준비로 조회하지 않았다. [원본 정제 JSON](evidence/android-product-ux/cloud-device-check.json). 새 Pleos 부팅/이미지 다운로드/설치/인증 조작은 하지 않았다. 기존 Cloud unauthorized를 이번 기기 성공으로 바꾸지 않는다.
- 공식 근거: 정상 proxy/TLS로 NaviHelper destinations 문서를 조회해 목적지 요청/결과 조회/콜백을 확인했다. [URL/원문SHA/지원경계](evidence/android-product-ux/nav-boundary.json). 지도 embedding/강제 foreground/임의 overlay 지원은 이 문서로 확인되지 않아 기본 A(정상 SDK 요청+별도 앱 안내)를 적용한다.
- 변경: 기능 **48b2ba5**, 앱0.9.0/9. 기본 ProductApp의 밝은 목적지/짧은 카드·선택 설정·명시 입차·큰 기울인5층 개략도/파란선·주차 요약/귀환·직접 단서 보완을 구현했다. 기존 AppModel/SQLite/DataStore/outbox를 재사용한다. TEST/모의 표시·위치/연결 미확인·주행 숨김·닫기/자동재개 없음 계약을 유지한다. 기술 진단 화면은 개발자 도구로 분리하고 기존 기기 회귀는 명시적으로 여기에 진입한다. [Android 규격25](25-android-navigation-ux.md).
- 환경/검사: Cloud JDK17/SDK35·캐시 Gradle offline. 기본 sandbox의 Gradle daemon socket이 Operation not permitted여서 동일 작업을 정상 승인 실행 환경에서 수행했다. `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease` BUILD SUCCESSFUL 2m35s, JVM99/failure0/error0/skip0. Python구문/actionlint/diff검사 통과. [로컬 기록](evidence/android-product-ux/local-checks.json). 기기 검증은 로컬 빌드와 구분한다.
- CI [43419890120](https://github.com/leemh-dev/dochagjari/actions/runs/43419890120)를 시작하고 PR을 진행 상태로 갱신했다. 새 제품 UI 기기 검사·15장 캡처/동적·정적 경로·새PID 복원과 기존 회귀/HTTP SERVER_MOCK를 정의했다. CI artifact는 job 종료 후 확보되므로 캡처 전에 앱 화면이라고 표시하지 않았다. 아래 후속 실제 결과로 완료 여부를 판단한다.
- 차단/남은 작업: 실제 Pleos SDK/순정 핀·앱 전환/Console·현장 승강연결/측위·외부 카메라·실차·실제 SMS·AI자료 worker는 별도다. Mac 접근 없이 실행했다고 주장하지 않는다. 목표는 기능검증/시연이고 앱마켓 출시/출시 서명은 범위 밖. 기존 사용자 데이터·서버DB/세션 보존, 자동 병합 없음.

- 코드 검토 보완 **e2b93ba**: 제품 카드가 저장된 요청 방식 대신 현재 선택값만 설명하던 혼동 가능성을 확인했다(실제 Pleos 재현으로 주장하지 않음). 다음 요청 방식과 저장된 `RoadAttempt.channel`을 각각 표시하고 모의/실제 SDK를 바꾸어도 이전 요청의 출처를 유지한다. 이 변경으로 최종 코드의 CI를 새로 실행하며 앞선 결과와 구분한다.

- 최종 코드의 PR 전용 workflow 조회가 빈 배열이어서 공개 Actions API의 `head_sha=e2b93ba` 읽기 조회로 push 실행 **43420964354**를 확인했다. 수동 재실행으로 해결하지 않았다. verify131870545211 성공, 원본보고서14034616082의JVM99/0fail/0ignored·완료log의서버14/진단35·lint/세APK를확인했다(Gradle5m52s). 개발APK artifact14033304488의ZIP digest와실제세파일크기/SHA256·aapt manifest·apksigner를독립대조했다. debug/test개발인증서같음·release unsigned. [대조결과](evidence/android-product-ux/ci-apk-comparison.json). 기기job별도재빌드byte/인증서와배포artifact동일을가정하지않는다.

- 초기 코드48b2ba5의 CI43419890120은 verify/android-device 모두 success. artifact14035146367 원본을 받아 XML15건 중13pass/0fail/프로세스전용2skip·별도process2단계OK, parking14장/product15장/HTTP SERVER_MOCK 성공을 확인했다. PID10212→11493·manual-demo 복원, POST1/공급자0. 확보 직후 목적지/5F완료/귀환/재생시작의 실제 앱PNG를 채팅에 직접 표시하고 일반Android/TEST/MOCK와 초기 코드를 명시했다. 최종e2b93ba의 결과와 구분한다. [초기 결과](evidence/android-product-ux/initial-code-ci.json).

- 최종 **e2b93ba / CI43420964354** android-device131873465768도success. artifact14034214983 ZIP digest와 원본 XML/PNG/readiness/복원/HTTP 보고서를 받아 대조했다. 기기13pass/0fail/2skip 후 별도process2단계OK. 새제품설정/지도/닫기·주행/재생성/실제SQLite 방문보존 검사와 기존회귀가 통과했다. 제품15장(대표8장PNG/XML보관), 기존주차14장, HTTP3장. 제품새PID10324→11631에서manual-demo 복원, 기존주차회귀도동일ID유지. HTTP POST1/공급자0. 설치전4개check각rc0/빈stderr 확인. 원본재생시작/완료와모션줄이기전체선을비교했고FPS/정확한재생시간실측으로과장하지않았다. [최종대조](evidence/android-product-ux/device-verification.json)·[원본화면](evidence/android-product-ux/README.md). 확보즉시최종코드목적지/지도/주행화면도채팅에직접표시했다.
- 마무리: README/docs08·09·10·12·18·25와증거/PR을최종검사로갱신한다. 추가앱코드/DB/키변경없고문서/원본증거커밋은[skip ci]로구분한다. 다음은7단계같은APK연속시연과접근가능한Mac/Pleos의SDK/순정핀·앱전환/Console확인이다. 이번실행에서실제Pleos/실차·SENS/휴대폰수신·AI자료worker를성공/완료로표시하지않는다.

## 2026-11-13 UTC · 도착자리 유지와 Pleos UX 고도화 재개 결정

- 사용자 결정: 대안 아이디어 탐색을 중단하고 기존 도착자리를 계속 고도화한다. 기존 서비스를 없다고 가정하거나 일부 중복만으로 방향을 폐기하지 않는다. 매장에 맞는 도착 계획→도로 내비→주차 단서→승강/매장 동선을 핵심으로 유지한다.
- 조사/환경: AGENTS·README·docs07/09/24/25·최근 개발일지를 읽고 `git status --short --branch`, `git log -12 --oneline`으로 로컬 clean/head c3359e5를 확인했다. origin 추적 ref는 오래되어 ahead10으로 표시되었지만 GitHub 연결 도구의 PR#1 GET은 OPEN·같은 원격 head c3359e5였다. 기존 변경을 덮어쓰지 않았다.
- 원격 확인: GitHub Actions GET에서 기존 43420964354와 후속 43421204705는 같은 앱 코드 e2b93ba의 completed/success였다. 후속 실행 artifact 독립 재검사는 하지 않았으며 새 검사 통과로 세지 않는다. 기존 앱0.9.0/9·APK/개발서명/unsigned release 식별은 유지한다.
- 조사 구분: 현대오토에버의 국내 실내 주차 서비스 운영과 현재 Pleos의 동일한 전체 서비스 제공은 별개다. 후자는 공개 근거 미확인이고 부재를 확정하지 않는다. 공식 Pleos 소개에서 두 앱 분할 자체는 확인되므로 이전의 포괄적 미확인 표현을 정정한다. 개발 이미지/우리 앱 실호환·지도 내부 삽입·자동 표시·Gleo 호출·카메라 접근까지 지원한다고 확대하지 않는다.
- 변경: [docs26](26-service-continuation.md)에 유지 결정·두 시나리오·작은 패널/확대 UX·확인 자료와 미확인의 연결·실기 및 연속 시연 순서를 기록했다. README/docs07/09에 다음 우선순위를 연결한다. 외부 조사 시안/화면은 실제 앱 증거로 올리지 않는다.
- 검사/실제 결과: 문서 변경의 diff/참조 파일을 확인한다. 앱 소스/DB/버전/APK를 바꾸지 않아 JVM·기기·서버·lint 검사를 반복하지 않는다. 새 스크린샷·Mac/Pleos 접근/부팅/설치/카메라·SDK 핀·Console 실행은 이번 작업에 없다. 검증 수준이 바뀌지 않아 docs08/12의 완료 증거는 유지한다.
- 남은 작업: 공식 화면을 바탕으로 작은 패널/확대 UX 규격을 확정하고 실제 판교점 주차/승강 연결 근거를 보완한다. 이후 기존 상태/저장 계약을 보존해 Android를 조정하고 접근 가능한 Pleos에서 별도 대조한다. 기능 검증/시연이 목표이며 자동 병합/데이터 삭제/인증 우회는 하지 않는다.


## 2026-11-13 UTC · 전체 시나리오 범위 정정

- 사용자 지적: 매장 방문은 시나리오1이며 기존 병원 동행·픽업 등도 서비스에 포함된다. 앞선 유지 결정에서 매장/주차 관측의 두 시연 화면을 전체 시나리오처럼 좁혀 기록한 부분을 바로잡았다. 주차 관측은 공통 기능이며 병원 시나리오를 대체하지 않는다.
- 조사/환경: 기존 docs01/02와 docs26을 다시 읽고 로컬 clean·원격 PR1 OPEN/head81dfdac 일치를 GitHub GET으로 확인했다. 매장 방문·병원 동행·학교/학원 등하원/픽업과 역/공항 마중 확장을 확인했다.
- 변경: docs02에 대표3시나리오와 공통 기능/기존 확장을 명시하고 기존 세부 표를 보존했다. docs26의 서비스 정의·전체 범위·UX 우선순위와 README/다음 프롬프트를 정정했다. 대표 매장 시연의 우선순위와 전체 서비스 범위는 별개이며 11월15일 범위 확대/추가 구현 완료로 표시하지 않는다. PR 설명도 같은 수준으로 갱신한다.
- 검증/실제 결과: 문서 diff/로컬 참조 검사를 수행한다. 앱0.9.0/9·e2b93ba, DB/설정/알림·APK/해시/서명은 그대로다. 이번 작업에 새 빌드·기기 테스트·화면·Pleos/카메라/실제 SMS 실행은 없다. 병원 미확인 연결은 가상 시연, 픽업 알림의 모의/공급자 수락/휴대폰 수신은 별도 수준으로 유지한다.
- 다음: 전체3시나리오의 다른 목적과 공통 상태를 보존한 채 매장 시연을 먼저 UX 고도화한다. 기능별 실제 검증은 docs08을 따르고 자동 병합/기존 데이터 삭제는 하지 않는다.


## 2026-11-13 UTC · 초행 이용자 중심 정의와 역/공항 마중 대표 편입

- 사용자 결정: 역·공항 마중도 포함해 초행 장소·건물·주차장에 익숙하지 않은 사람에게 유용한 서비스로 개발한다. 핵심 사용자를 특정 매장 고객으로 한정하지 않고, 차량 진입·승하차·대기·주차·최종 목적지/만남·귀환의 불확실성을 줄이는 방향으로 정의했다.
- 조사/환경: AGENTS·docs01/02/26을 재확인하고 로컬 clean·원격 PR1 OPEN/head30f575d 일치를 확인했다. 기존 시나리오/검증 상태를 보존했다. 이번에는 외부 시설 운영 규정이나 특정 역/공항 도착 정보를 새로 조사/검증하지 않았다.
- 변경: docs01의 대상·문제·흐름을 초행 이용자로 갱신했다. docs02/26에서 역·공항 마중을 대표 시나리오4로 승격하고, 사람 출구와 차량 진입/승차 허용점·대기 장소를 구분했다. README/docs07/09에도 네 시나리오와 대표 매장 시연의 우선순위를 반영했다. 실시간 항공편/열차·출구 변경 연동을 현재 완료로 표시하지 않는다.
- 검사/결과: 문서 diff/로컬 참조 검사를 수행한다. 문서 변경만 있어 JVM·서버·기기 검사를 반복하지 않는다. 앱0.9.0/9·e2b93ba, 기존 APK/해시/서명·DB/설정/알림과 실제 검증 수준은 유지한다. 새 화면/빌드·Pleos/차량 실행은 없다.
- 다음: 네 상황을 다룰 수 있는 공통 UX를 정하되 에뜨와 매장 시연부터 고도화한다. 역/공항의 특정 지점과 허용 승차/대기 규정은 향후 확인 자료에 따라 채운다. 대표 시나리오 편입을 11월15일 제출 범위 확대나 외부 연동 완료로 세지 않는다. PR/브랜치에 기록하고 자동 병합하지 않는다.


## 2026-11-13 UTC · 병원·픽업·역/공항 UX 예상도 제작

- 요청/단계: 매장 시안에 이어 다른 대표 시나리오도 디자인해 달라는 요청으로, 현재 단계는 ⑦전체 시연 앞의 화면/연결 경험 설계다. AGENTS와 기존 research/dochagjari-ui-concepts의 매장 시안/원본 metadata를 확인했고 PR1 OPEN·원격/로컬5274fc1·clean을 확인했다.
- 생성/공유: 매장 승인 시안을 스타일 참조로 image_gen 도구에서 가상 병원·학원·역·공항4장면을 생성했다. 왼쪽 시스템/중앙 내비모형/오른쪽 서비스 패널, 큰 한국어 글씨·어두운 중립색·파란 계획 경로·청록 보행 연결을 유지했다. 병원은 함께 주차/먼저 하차, 픽업은 막힌 승차점→대체주차, 역은 사람출구/차량승차점, 공항은 도착층/대기주차를 구분했다. 생성 완료 직후 채팅에 이미지를 직접 표시했다. 실제 기기 캡처가 아니다.
- 수정 전후: 공항 초안의 파란선은 대기주차장 안내 버튼과 달리 승차구역으로 향했다. 시각 검토에서 발견해 같은 이미지 생성 도구로 수정했고, 최종선은 주차장으로 들어가 끝난다. '일행 준비 후 승차 구역으로' 다음 단계를 구분했다. 수정 전/후 이미지를 채팅에 직접 표시했고 초안/생성 원본은 workspace에 보존했다. 실제 공항 규정/도착정보/일행위치 연동은 없다.
- 저장: [시안 원본 PNG와 설명](ux/scenario-concepts/README.md), [장면별 전환 규칙](27-scenario-ux-concepts.md), image-metadata.json을 추가했다. 기존 매장 기준1장(이번 생성 아님)+새 최종4장을 복사했고 전부1672x941 원본이다. PNG헤더·실제크기·SHA256을 재조회한다. 앱 source/APK/키가 아니라 그림 파일 식별값이다. README/docs02/09/26과PR에 디자인 수준을 기록한다.
- 검사/실제 결과: 문서 diff/로컬 참조·PNG/metadata 대조를 수행한다. 앱0.9.0/9·e2b93ba·DB/설정/알림·APK/기존해시/서명 유지. 새 Android/Pleos 설치/실행·JVM/서버/기기 검사·애니메이션 실행은 하지 않았다. 생성 글꼴 인상을 실제 폰트 파일/라이선스/실기 가독성 검증으로 세지 않는다. docs08/12의 완료 수준도 변경하지 않는다.
- 다음: 시안의 주행/정차·자료부족·닫기·저장/공유 조건을 Android 규격으로 구체화하고, 매장부터 단계적으로 적용하되 나머지 시나리오 구조도 보존한다. 실제 Pleos 분할 호환/SDK핀·시설 경로/카메라·역공항 외부정보·SMS는 별도 검증이다. 자동 병합/데이터 삭제/실제 메시지 발송은 없다.


## 2026-11-13 UTC · 생성 시안 대신 실제 장소 사진 조사

- 요청/현재 위치: 생성 시안이 AI 이미지처럼 보인다는 지적에 따라 디자인 적용을 진행하지 않고 실제 시설 사진을 조사했다. 대상은 분당서울대병원 입구/주차, 분당 학원가 건물/픽업 대기, 인천공항 장기주차장, 정자역 주변이다. PR1 OPEN·로컬/원격9776e8d·clean을 확인했다. 앱은 그대로다.
- 도구/환경: 웹/이미지 검색으로 원문과 직접 이미지 주소를 확인했다. 공개 자료 다운로드는 Cloud 기본 네트워크 제한을 고려해 정상 승인 네트워크 환경의 Python urllib/기존 proxy·TLS로 수행했다. 직접 기기/현장 촬영이나 로드뷰 실시간 관찰은 하지 않았다. 이미지 생성/수정 도구는 사용하지 않았다. 원본은 /workspace/research/real-place-references-20261113/에 보관했다.
- 결과: 사진13장(병원4/학원가3/공항4/정자역2)과 공식 주차 안내도2장을 내려받아 원문/실제 표지를 대조하고 채팅에 직접 표시했다. 학원가는 수내동 건물2와 2022년 셔틀버스 보도사진1이며, 분당 학부모 승용차 픽업 대기로 확인된 사진은 미확보다. 다른 지역 사진으로 바꾸어 채우지 않았다.
- 근거/실패: 병원 공식 지하/지상 주차 사진과 2023년 방향표지 방문기, 공항2022년 방문기/2024년 타워 내부, 정자역2/5번 표지를 확인했다. 게시/촬영일 미확인 사진은 별도로 표기했다. 성남시 보도 원문은 웹 도구403으로 본문 열람이 막혀 공개 보도/사진을 참고했고 우회하지 않았다. 네이버 일부 원문은 웹 도구에서 접근되지 않았지만 검색에 제공된 공개 사진을 확보했고 게시일을 추정하지 않았다.
- 저장/검사: [docs28](28-real-place-photo-research.md)·[출처 목록](research/real-place-references/README.md)·source-catalog.json과 README/다음 프롬프트를 갱신한다. 실제 사진의 권리 조건을 앱 재배포 허락으로 간주하지 않아 사진/HTML 원본은 로컬 참고 캐시만 두고 GitHub에는 링크/식별/한계를 기록한다. 공개CDN 만료 쿼리를 metadata에서 제거했다. 로컬 HTML 갤러리와 원본15개/출처JSON의ZIP 약10MB를 만들었다. 파일 크기/SHA256/ZIP무결성·문서 diff/참조를 확인한다.
- 수준/다음: 앱0.9.0/9·e2b93ba·DB/설정/알림·APK/키·기존검증유지. 새빌드/실제기기/SDK핀·카메라OCR·현재위치·현장무단차/승하차 허용/항공편 연동은 검증하지 않았다. 공항 장기주차 사진 확보를 짧은 마중 대기에 최적인 장소 추천으로 쓰지 않는다. 실제사진/공식도면·이용조건을 먼저 대조한 뒤 다음 UI를 검토한다. 자동 병합/기존시안삭제/실제SMS발송없음.

## 2026-11-13 UTC · 실제 사진을 단순화한 3분할 3D UX

- 요청/현재 위치: 실제 사진을 그대로 붙이는 대신 차량 주변 인지 화면처럼 건물/차량/기둥을 단순한 3D로 표현해 달라는 요청이다. 현재는 전체 시연 전 UX 검토이며 Android 구현으로 전환하지 않았다. AGENTS/README/docs09·10·28와 출처 목록을 읽고 로컬 clean·PR1 OPEN/원격 head ced8f02 일치를 확인했다.
- 환경/도구: Cloud 이미지 생성 도구에 기존 매장 화면(분할/테마 참고)과 확보한 실제 병원/학원/정자역/공항 사진 및 공항 공식 안내도를 입력했다. 독립 시안4개를 생성하고 확보 즉시 채팅 이미지로 표시했다. 생성물을 실제 센서 처리/현재 현장/SDK 실행 증거로 세지 않았다. 사진 수정/현장 촬영/추가 부팅/인증 우회는 없다.
- 변경: 차량/주변 상황·내비 모형·도착자리의 세 영역, 회색 입체와 얇은 파란 강조, 한 장면/한 문장/두 행동으로 정리했다. 병원은 낮은 갈색 캐노피/유리 입면, 학원은 크리스탈빌딩, 역은 실제5번 유리 출입구, 공항은 실제 사진의2F/227을 참고했다. 차량 배치/지도/입구 강조는 설계 연출이고 승하차 허용점/위치/거리/연결은 미확인이다. 학원 셔틀 사진을 학부모 승용차 대기 현장으로 바꾸지 않았다.
- 저장: docs/ux/photo-based-3d/에 생성 PNG4장(각1672×941)과 지시/참고사진식별/출처/SHA256 metadata를 저장하고 원래 생성 파일도 보존했다. docs29에 화면 책임·자료/관측 경계·후속 적용 순서를 기록하고 README/docs09/28·PR을 연결한다. 외부 원본 사진은 GitHub/앱 자산에 추가하지 않았다. 기존 시안도 삭제하지 않는다.
- 검사/결과: PNG header/실제 크기/SHA256와 metadata·출처 연결, 문서 diff/로컬 링크 검사를 수행한다. 소스/APK/DB/설정/알림 변경이 없어 기존 JVM/기기/서버 검사를 반복하지 않는다. 앱0.9.0/9·e2b93ba·기존 APK해시/개발서명/unsigned release와 docs08/12의 검증 수준을 유지한다. 병원 캐노피 강조는 특정 승하차문이 아니며 공항/지도 자차 표시는 위치 확인이 아니다.
- 남은 작업: 사용자의 시안 검토 후 실제 오른쪽 앱 패널의 레이아웃/상태를 구체화한다. 생성물의 창문/바닥 세부는 실제 렌더러에서 더 줄일 후보이며 3D 모델/렌더러·실시간 센서 처리를 구현한 것은 아니다. 실제 Pleos 분할/SDK핀/카메라 지원·시설 연결은 별도 검증이다. 이번 디자인 기록은 별도 커밋/푸시로 남기고 자동 병합하지 않는다.

## 2026-11-13 UTC · 3D 대상/카메라 영역 해석 정정

- 사용자 정정/원인: 3D 대상은 지도이며 카메라/건물 외관을3D로 바꾸라는 뜻이 아니었다. 첫 번째 차량 정보도 그대로 유지해야 한다. 앞선 네 시안을 의도에 맞는 최종안처럼 기록한 해석을 정정한다.
- 조사/환경: AGENTS/기존 기준PNG/docs29·git 상태와 원격PR1 OPEN/head26db896 일치를 확인했다. 이미지 생성 편집 도구에 기존 매장 기준 화면과 실제 공항 주차 내부 참고 사진을 입력해 차량/내비 구성 유지·세 번째 상단 영상/하단3D지도 구성으로 새PNG1장을 생성했다. 확보 직후 채팅에 표시했다. 카메라 프리뷰는 생성 예시로 실제 빌트인캠 수신이 아니다.
- 변경/증거: docs/ux/photo-based-3d/05-camera-and-3d-map.png와 corrected-map-metadata.json에 원본 경로/지시/공개 출처/크기/SHA256을 기록했다. 첫 번째는 차량 정보, 두 번째는 내비, 세 번째만 위는 사진형 영상/우측하단2F·227관측표시·아래는 기울인 주차지도/승강아이콘이다. 기존 사진의 문자만 참고했고 지도/승강 위치는 예시·위치 미확인으로 표시했다. 가운데 판교 지도는 기준 화면 보존용이며 공항과 연결된 실제 경로라고 주장하지 않는다.
- 정정 기록: README/docs09/29/시안 모음에 최신 구조를 우선 표기했다. 이전 네 장/지시는 삭제하지 않고 잘못 해석한 검토 이력임을 명시한다. 이미지 생성물이므로 차량/내비 구성이 픽셀까지 동일한지는 보장하지 않는다. 실제 앱은 변경하지 않았다.
- 검사/결과/다음: PNG header/1672×941/실제SHA256/metadata와 문서 링크·diff검사를 수행한다. 앱0.9.0/9·e2b93ba/APK·DB/설정/알림·기존 검사 수준 유지. 빌드/기기/카메라/SDK 검사를 새로 실행하지 않았다. 다음은 정정한 패널 구조를 검토하고 시설 지도/관측 연결 근거가 있는 부분만 앱 규격으로 구체화하는 것이다. 새 코드/자동병합/실제 위치·경로 성공으로 표시하지 않는다.

## 2026-11-13 UTC · 도로3D/도로변 승하차 실제 이미지 수집과 검토 준비

- 요청/현재 위치: 주차장 외에도 학원·병원 등의 도로변 정차 상황에 같은 구조를 적용하기 전에 실제3D도로표시/현장 사진부터 모아 검토하자는 요청이다. 첫 번째 차량정보와 두 번째 내비는 유지하고 세 번째 상단카메라/하단3D지도 기준도 유지한다. 앱/새시안 제작은 이번 범위가 아니다. Cloud runtime 스킬의 네트워크 지침/AGENTS를 확인하고 clean·PR1 OPEN/원격7b5c192 일치를 확인했다.
- 조사/도구: 웹/이미지 검색으로 제조사 공식BMW/Pleos/현대자료와 Tesla/MBUX 보도 화면, 실제 도로·정차 환경 원문을 확인했다. 기본sandbox curl은proxy8080 연결실패였다. 지침에 따라 승인된 네트워크 환경의 Python urllib/기존proxy/TLS로 공개원본을 내려받았고 새로운 부팅/기기접속/인증우회는 없었다. 웹도구의 병원이미지 만료URL410/공식사진내부오류는 원문을 정상 읽어 현재 공개src를 확인해 해결했고 로그인/보호를 우회하지 않았다.
- 실제 결과: 차량UI10장(BMW4/Tesla1/MBUX1/현대AR1/Pleos3)·현장9장(병원3/학원3/정자역1/공항2), 총19장. 새확보17장·기존원본2장을 구분했다. 확보 즉시 모두 채팅이미지로 표시했다. 병원정문PNG 표시 중 도구transport 종료1회는 실제파일 존재를 재확인해 다시 표시했다. 새화면생성/원본사진편집은 없고 실제내비/카메라/센서 실행으로 보고하지 않았다.
- 구분/발견: 도로지도3D/감지객체시각화/영상AR는 역할이 다르다. 학부모승용차픽업은 대치동2025보도이며 분당으로 표시하지 않는다. 기존분당사진은 학원셔틀이다. 병원접근도로/지하만차진입 사진과 서울아산병원 약국차량전용승하차장을 구분한다. 공항2026방문사진은 출국층배웅으로 입국마중장소와 같다고 하지 않는다. 차량줄/여유공간이 정차허용증거는 아니다. 최신촬영일/일반승용차허용점/측위·거리·복귀선은 미확인이다.
- 저장/검사: /workspace/research/road-ui-references-20261113/에 원본19장·출처JSON·HTML갤러리, ZIP22항목/7,320,731바이트를 저장했다. 실제크기/치수/SHA256·갤러리이미지연결/ZIP무결성을 대조한다. 원문HTML/만료CDN서명쿼리는 ZIP/GitHub목록에 포함하지 않았다. docs30/사진별출처README·JSON/README/docs09·PR에 조사결과·경계·다음검토를 기록한다. 외부원본은 참고용로컬캐시로 보관하고 저장소/앱배포자산에는 추가하지 않았다.
- 수준/다음: 자료수집과검토준비만 완료했다. 앱0.9.0/9·e2b93ba·APK/해시/개발서명/unsigned release·DB/좌석/설정/알림/복구·docs08/12의 기존검증 유지. 새빌드/기기/SDK핀/카메라OCR/실차/SMS검사를 하지 않았다. 다음은원본을함께검토해세번째하단도로지도표시와상단카메라노출조건을정하는일이다. 기능확정/새이미지/앱개발전에검토하며기존시안삭제/자동병합없음.

## 2026-11-13 UTC · 사진 참고 도로변 시나리오 UX 제작

- 요청/현재 위치: 사용자가 수집 사진을 활용해 우리 시나리오의 디자인 제작을 요청했다. 자료조사 후 정정된 구조로 디자인하는 단계이며 앱개발은 하지 않았다. AGENTS/docs29·30/기준PNG/실제BMW·학원사진을 읽고 git clean·PR1 OPEN/원격1961242 일치를 확인했다. 기존 자료수집만 하자는 요청 이후 새 디자인 요청이 온 것으로 구분한다.
- 환경/변경: Cloud 이미지생성/편집 도구에 기존정정시안+공식BMW도로지도(표현참고)+사진을 입력했다. 학원/병원/정자역/공항배웅4장을 생성해 확보즉시 채팅이미지로 표시했다. 첫 번째차량정보·두 번째내비 구성을 유지하고 세 번째 상단은 사진형카메라시연, 하단은 기울인3D도로지도다. 실제빌트인캠 입력이나 실측지도/내비실행으로 표시하지 않았다.
- 장면/경계: 학원은 실제대치동사진을 참고한 정차금지/대안찾기, 병원은 분당서울대병원진입사진을 참고한 사람출입구/주차/승하차후보·함께주차/먼저하차 선택, 역은5번출구/보행동선과 승차점확인전, 공항은T1출국층배웅/승하차와긴대기구분이다. 미확인정차점/무단차·거리·위치를 확정하지 않으며 공항입국마중으로 치환하지 않는다. 가운데판교지도는기준화면보존용 배경이다.
- 검토/수정: 역초기시안이파란탐색영역을차로위에그려정차점으로오해할수있었다. 해당영역/라벨만제거하고차선을복원하는후속이미지편집을수행해최종본을즉시표시했다. 초기원본은로컬에보존하고metadata에수정이유/지시를남겼다. 생성편집이므로첫번째/두번째화면의픽셀동일성을검증했다고쓰지않는다.
- 저장/검사: docs/ux/road-scenario-concepts/에최종PNG4장과참고출처/지시/실제치수·크기·SHA256을저장했다. 병원1671×941,나머지1672×941로실제파일값을기록했다. docs31/README/docs09·30/PR에화면역할·다음행동·제약을연결한다. PNG/metadata·로컬참조/diff를대조하고앱소스변경이없어JVM/기기/서버검사를반복하지않는다.
- 보존/다음: 앱0.9.0/9·e2b93ba·APK/서명/DB/좌석/설정/알림복구/docs08·12의기존검증수준유지. 매장/주차장/이전생성시안도보존한다. 새빌드/기기/SDK핀/카메라OCR/실차/SMS없음. 다음은사용자의시안검토후선택된패널의전환/상태규격화와실제자료/플랫폼지원확인이다. 별도디자인커밋/푸시/PR갱신으로남기고자동병합하지않는다.

## 2026-11-13 UTC · 시안 기반 시연영상 제작 계획

- 요청/현재 위치: 사용자가 디자인 시안에 이어 시연영상 제작 계획을 요청했다. AGENTS/README/docs07·12·18·31과 기존 영상/앱 증거 경로를 확인하고 git clean·PR1 OPEN/원격5b76a43 일치를 확인했다. 전체7단계의 영상 기획/제작 준비이며 이번에 앱/모션영상/기기녹화를 만들지 않았다.
- 공식 확인: 웹 도구로 공식 submission/FAQ를 다시 읽어 목업·프로토타입·화면녹화 인정, 선택 산출물/2차PT 동작앱 요건을 확인했다. 페이지에서 영상 길이/용량 상한을 확인하지 못해 3분30초/약90초·1080p30은 내부 제안으로 표기했다. APK를 제출할 때의 실행 가능한 release 요건과 현재 개발서명/unsigned release 수준도 분리했다. 새 서명/실제발송/자동제출을 수행하지 않았다.
- 계획: 서비스 본편210초는 문제15→에뜨와요청/선택조건20→DB/이동중자료보완20→내비/입차15→주차영상/지도30→5층파란선30→주차완료/저장/닫기20→귀환15→병원/학원/역/공항30→구현수준/종료15로 연결한다. 실제 앱 약90초 원본은 같은APK/방문/TEST·MOCK로 별도 확보하는 계획이다. 사전자료/AIworker컨셉, 요청/콜백/핀/입차, 사진형영상/후보/측위/실측경로를 구분한다.
- 연출/제작안: 첫째차량정보·둘째내비는 역할 유지, 셋째상단영상/하단3D지도만 전환한다. 평면PNG 전체줌 대신 Remotion/React+SVG/CSS 지도레이어·FFmpeg 출력안을 제안했고 패키지 설치/렌더코드 구현은 하지 않았다. 첫 실제제작은15~20초 핵심모션샘플로 잡았다. 카메라상세/상세조작은 정차, 닫기후 자동재개없음·미확인복귀선없음·기존저장/복구를 유지한다.
- 기록: docs32에210초콘티/나레이션초안·원본녹화/자산/배지/검수·실행순서를 작성하고 docs/demo/video-plan/shot-list.json에계획상태/시간/기존참고자산/공식출처를 저장했다. README/docs07·09·12·18과PR을 연결한다. 문서연결패치에서 기존docs18제목이 달라 검증실패1회였고 실제제목을읽어 정상패치했다. 앱코드나기존변경은 덮어쓰지 않았다.
- 검사/실제 결과: 콘티10장면의 연속시간/합계210초·확장세그먼트30초·기존참고자산/Markdown링크·JSON구문/diff를 확인한다. 실제 영상 인코딩/FFprobe/재생/새테스트는 아직 계획으로 남긴다. 앱0.9.0/9·e2b93ba/APK/DB/설정/좌석/알림복구/docs08의실제검증수준유지. docs12의영상항목은미완료체크박스로 추가했다.
- 다음: 콘티를 기준으로 핵심모션샘플→편집레이어/사용가능자료→같은APK원본녹화→본편/자막/음성→파일/표시/제출정보대조 순서다. Pleos가 막히면 컨셉영상과 일반Android실행을 구분해 계속하고 실제연동성공을 합성하지 않는다. 기능단위 기록커밋/푸시/PR갱신, 자동병합없음.

## 2026-11-13 UTC · 내비와 독립된 다중 탑승 모드/이번 구성 기획

- 사용자 결정: 차를 쓸 때마다 탑승자가 다르므로 좌석별 주 이용 구성은 여러 모드로 저장하고 음성/수동선택/좌석센서를 활용해 오늘 혼자/아이 동반 등 이번 구성을 인지한다. 이 설정은 내비 안내/목적지 입력과 독립한다. 현재는 서비스/UX 기획을 반영하는 작업으로 앱 코드는 변경하지 않았다.
- 조사/환경: AGENTS/docs01·04·20/영상계획32, SeatPreferences.kt/ProfileRepository.kt와 역할 테스트 위치를 읽었다. 현재는 단일 좌우 역할/카시트·DataStore 저장상태/실제저장값을 보존하며 역할·점유를 분리한 구조다. git clean·PR1 OPEN/원격8ff4197 일치를 확인했다. 실제센서/음성 입력이나 새기기검사는 하지 않았다.
- 설계: docs33에 저장프리셋(혼자/아이등하원/가족/부모님동행/사용자구성)·이번탑승구성·센서관측의세정보를분리했다. 설정은목적지없이사용하며 이번만변경과프리셋갱신을구분한다. 음성/선택은역할을확인하고점유는후보제안을보조한다. 현재뒤좌우EMPTY만으로앞동승석까지비어있다고추론하지않으며OCCUPIED를아이/환자로식별하지않는다. 확인한경우만짧은안내를하고반복모달을피한다.
- 보존/후속기준: 기존단일구성을기본프리셋으로이행하는방향과UI/저장ACK/실패/복원·이번만변경·관측오래됨/권한/충돌검사를정했다. 탑승세션은방문/알림과분리하고모드변경이내비/관측/발송을자동시작하거나확정수신자/요청을바꾸지않는다. 주행상세편집제한/건너뛰기/미지원직접입력을유지한다. 앞좌석SDK/실제STT/Gleo호출은새지원완료로표시하지않는다.
- 기록/영상연결: docs01/04/07/09/20/32·README와PR에독립설정/새설계상태를연결했다. 본편210초중초반장면은3~5초독립탑승확인뒤에목적지를요청하도록문구/shot-list를정정했다. 이번에시안이미지/영상/앱/APK를만들지않았다.
- 검사/실제결과: 문서로컬링크/JSON구문·장면시간합계210초/참고파일/diff를확인한다. 앱0.9.0/9·e2b93ba·APK/서명/DB/설정/좌석/알림복구와docs08/12의완료수준은그대로다. 새다중프리셋/음성/센서기능을검증완료로올리거나기존JVM/기기검사를반복하지않는다. 다음은독립탑승설정UX/데이터이행규격을앱구현과연결하는일이며영상에서는구현전까지컨셉표시다. 자동병합없음.

## 2026-11-13 UTC · 새 실개발 브랜치 분리와 시안 보존

- 요청/현재 위치: 사용자가 개선된 방향의 실개발을 별도 브랜치에서 시작하도록 요청하고 시안 보관 상태를 확인했다. AGENTS/README/docs09·33/기존 앱 역할·버전을 확인했다. 로컬clean/HEADf164ec7·GitHub PR1 OPEN/head일치를 확인했고 원격브랜치목록(main/기존feature)에 새 이름이 없음을 확인했다. 기존변경/사용자데이터는없애지않았다.
- 실행/결과: `git switch -c feat/arrival-context-ux f164ec77fbdd651334f946867ab8bd062be5b30e`로 새 로컬 브랜치를 만들었다. 기존PR1/main/feat/android-submission-mvp를그대로보존하고새PR은기존브랜치를base로한Draft로열어새변경만검토한다. 현재앱0.9.0/9·e2b93ba와APK/서명은그대로다.
- 시안검사: `git ls-files docs/ux`의PNG22개를읽어실제치수/크기/SHA256을기록하고`git cat-file blob f164ec7:<path>`와모두일치함을확인했다. 최신기준6개(매장1/정정주차장1/도로변4)는기존metadata의SHA256/크기도독립대조했다. `git diff --quiet e2b93ba -- app/` rc0으로앱디렉터리보존을확인했다. [보존목록](evidence/implementation-restart/design-baseline.json). 이전잘못해석한시안은삭제하지않고기준에서구분한다.
- 새진행기준: docs34에 출발커밋·기존APK·시안/데이터보존, 모델/저장·이행→독립설정UI→텍스트/TEST와실제음성·센서→카메라/3D패널→같은방문흐름→실행녹화의순서를기록했다. 첫코드단위는docs33의다중프리셋/이번구성저장과옛단일선택이행이다. 탑승세션/방문/확정알림분리와실제저장ACK·실패/복구·주행제한을유지한다.
- 기록/검사: README/docs07·09·18/34/보존JSON과새PR에연결한다. 문서링크/JSON/22개원본해시·기존app동일/diff를검사하고새브랜치를푸시한다. 앱/새영상/새UI/프리셋구현이나JVM/기기/서버/SDK검사를이번분리작업에서수행하지않았고docs08/12의기존검증수준을유지한다.
- 다음/남은작업: 새브랜치의첫실제앱코드는영속저장/이행부터구현하고완료후독립설정UI와연결한다. 새앱산출물의버전·코드·해시·개발서명을그때기록한다. 기존PR1자동병합/리셋/인증우회/기존DB삭제없음. 시안은Git에보존돼로컬파일유실시에도기준커밋에서복원할수있다.

## 2026-11-14 UTC · 기존 구현 정리 검토와 개발 지시 혼선 제거

- 요청/범위: 사용자가 새 기획과 맞지 않는 기존 구현을 삭제하고 필요한 것만 남길지 문의했다. 일괄삭제를 실행하지 않고 현재 호출/저장/검사 연결을 조사했다. AGENTS/개발기준34와 새 설계를 읽고 로컬clean·PR2 OPEN/Draft/headfa155b4·base기존feature 일치를 확인했다. 정적조사를 실행오류재현/완전한미사용코드분석으로 표시하지 않는다.
- 실제조사: MainActivity.onCreate는ProductApp을열고 ProductApp의developer분기는ArrivalApp을호출한다. 예전UI는개발자도구에서사용중이다. AppModel은facilities.json을초기화하고AppServices의profile/outbox/guidance를함께관찰한다. workflow는서버/진단/캡처·복원스크립트를사용한다. UI에안보이는파일도즉시삭제할수있는무참조코드라고할수없었다. AppModel425/MainActivity267/ProductScreen271행에서여러책임이연결돼있음을읽었다.
- 판단/정리안: ProfileRepository/DataStore·방문/주차/관측저장·ArrivalOutbox/서버복구·SDK/TEST어댑터·회귀검사는재사용기반이다. 기존카드UI/단일좌석편집은새구현/데이터이행후호출자를전환하고중복만제거한다. 예전가상시설/진단/모의기능은제품기본흐름과구분한다. 시안/출처/검증증거는삭제하지않는다. 이번조사만으로안전삭제를확정한앱소스목록은없다.
- 실제변경: docs35에보존/교체/개발자기능분리표·실제호출관계·삭제전대체/검사기준을기록했다. AGENTS/README에34→33→31및35의현행읽기순서를명시하고07은제출범위/08은실제검증으로유지했다. docs09의누적된과거작업지시를현재첫저장/이행작업한개의프롬프트로정리했다. 과거원문은fa155b4의Git링크로보존한다. 첫작업을매번기기/모션/자료조사로바꾸는혼선을줄이고다중프리셋저장을우선한다.
- 검사/수준: 변경문서로컬참조/diff·기준manifest의PNG22개/최신6개SHA256·app/server/scripts/workflow기준동일을확인한다. 앱소스/데이터/테스트/CI파일을삭제·이동하지않았고앱0.9.0/9·e2b93ba/APK/서명은유지한다. 새빌드/기기/JVM/서버검사를실행하지않으며기존검사를새결과로세지않는다. docs08/12의완료증거는변경하지않는다.
- 다음: 새브랜치에서프리셋/이번구성저장·기존설정이행을구현한뒤독립설정UI로연결한다. 해당단위의새검사와호출전환후사용하지않는옛화면/선택기를제거한다. 이번기록은PR2에갱신하고기존PR1자동병합/데이터초기화/실제발송은없다.

## 2026-11-14 UTC · Cloud 개발/Mac Pleos 단계별 실행과 인수인계 정리

- 요청/현재위치: 사용자가 개발준비상태와Cloud개발/맥북에뮬레이터확인을단계별로명확히넣어달라고요청했다. 준비/문서정리는완료됐으나옛코드를모두삭제한것이아니며대체구현후제거하는기준을재확인했다. 새코드구현전실행절차정리작업이다. 로컬clean·PR2 OPEN/head9fa46cd·base기존feature를확인했다.
- 기존근거조사: README의Mac외장SSD절차·macos-ssd-env.sh/adb_readiness.py/collect_apk_metadata.py/기기검사driver와docs08의Mac M1기록을읽었다. 과거ARM64 Pleos부팅/앱표시·표준ADB6/6성공이있으나메모리압력경고/swap약3.6~3.9GiB였고옛버전결과다. 신규0.9앱/새브랜치의STT/SDK핀/Console성공으로확대하지않는다. 현재Mac접속/명령/새부팅은수행하지않았다.
- 변경: docs36에0준비·1저장/이행·2독립탑승UI·3입력/인지·4카메라/3D지도·5같은방문흐름·6회귀/시연을Cloud/Mac열로나눴다. 기능단위의구현→검사→특정커밋/버전/app·testAPK해시/인증서전달→Mac기기확인→원본/PR기록절차를정했다. Cloud/Mac의서로다른커밋을같은성공으로합치지않고연결차단중에도독립코드는계속한다.
- Mac운영: M1/8GB·외장SSD기존체크아웃/SDK/AVD/캐시·AVD한대·Cloud빌드전달을기본으로하고로컬빌드가필요하면AVD종료/worker1/heap1536MiB와교대로실행한다. -no-audio실행은음성검증이아니다. 실제transport/boot/package/activity/각rc0·설치직전재확인과인증서대조·데이터보존을유지한다. 표준ADB/Gradleconnected/새PID검사와기존6건driver의범위를구분했다.
- 인수인계/증거: Mac Codex전달프롬프트·readiness/설치/실행/캡처의재현명령예시·단계별Cloud/Mac상태/오류회신·화면즉시공유형식을작성했다. 예시명령을Mac실행결과로보고하지않는다. docs/evidence/implementation-restart/stage-status.json은준비완료/1단계구현전·새Mac미확인으로시작한다. AGENTS/README/docs09/12/18/34에36을연결했다.
- 검사/실제결과: 상태JSON/0~6단계순서/미실행상태·참조파일/문서링크/diff·앱/server/scripts/workflow기준동일·시안22개해시를확인한다. 새앱/테스트/시안/영상/APK변경·실제Mac연결·기기부팅/검사는없다. 앱0.9.0/9·e2b93ba/개발서명·unsigned release·docs08의기존실적은유지한다. 체크리스트에새통과를추가하지않는다.
- 다음:1단계저장/이행의실제코드구현과Cloud검사를시작하고실제산출물을Mac프롬프트에채워전달한다. 같은커밋의Mac결과가생기면별도로추가하고미연결동안은차단/미검증으로유지한다. 기능커밋/푸시/PR2갱신·기존PR1보존/자동병합없음.

## 2026-11-14 UTC · 1단계 다중 탑승 저장/이행 실제 구현

- 사용자 위임: 오늘 구현을 목표로 합리적 선택은 자율 판단하고 기록한다. 기존 데이터 보존/실행 가능한 시연을 우선했다. 원격PR2/head4a106a6·로컬clean을확인했다. Cloud 환경상태/기존JDK17·SDK35·Gradle캐시를확인했다. 기본ADB는socket권한오류,정상승인환경의목록조회는빈목록이며/dev/kvm없음이다. 새에뮬레이터부팅/키변경은없다.
- 구현: OccupantContext/Repository를추가해여러프리셋·이번탑승snapshot/출처·센서점유를분리했다. 기존profile DataStore인스턴스를공유하고JSON한키만추가해옛좌우역할/카시트/이동조건을기본프리셋으로원자이행한다. 기존Profile값/다른키는보존한다. schema/JSON이알수없으면덮어쓰지않는다. 저장후실제재조회·순차쓰기/최신ACK·실패재시도와지난세션자동확정없음을구현했다. 이번만적용은프리셋을바꾸지않는다.
- 판단/근거: 독립저장소가같은파일의DataStore를중복생성하지않도록AppServices에서한인스턴스를공유한다. 탑승구성은방문/알림세션에접근하지않는다. 센서로신원/아이/혼자를판별하지않고명시텍스트/선택은확인할후보만만든다. 실제STT/Gleo/앞좌석지원은미검증이다.
- 검사: JDK17/SDK35/기존Gradle캐시·정상승인실행환경에서offline `:app:testDebugUnitTest --tests '*Occupant*Test'` BUILD SUCCESSFUL 1m1s. 신규14건(모델/명령8·실제DataStore/저장복원/실패/연속쓰기6)실패0. [원본XML/요약](evidence/occupant-context/cloud/data-checks.json). 앱은0.10.0/10으로올렸고이로컬단위에서는APK패키징/기기검사를수행하지않았다. 후속실제APK의코드/해시/서명은패키징후대조한다.
- 상태: Cloud1단계저장기반검사통과,Mac새버전미실행. 다음은독립프리셋UI/이번만편집·저장/명령확인과내비에확인된조건읽기를연결한다. 기존UI/DB/알림복구를삭제하지않고기능커밋/푸시후CI를확인한다.

## 2026-11-14 UTC · 2단계 독립 탑승UI/3단계 명령 어댑터 구현

- 구현: ProductApp의탑승설정을독립화면으로교체했다. 여러프리셋선택/추가·이름/좌우역할·카시트/이동조건편집·삭제,이번만변경·탑승종료/건너뛰기·지난구성재확인·저장완료/실패재시도를연결했다. 기존내비방식설정은개발자도구에남기고탑승설정과목적지를분리했다. 저장한프리셋과이번구성snapshot은독립이며역할만으로이동조건을추론하지않는다.
- 입력: 텍스트/명시좌석/모드키워드를후보로파싱하고확인후적용한다. 모호/부정/복수구성은확인필요이며source는TEXT다. Android음성인식Activity호출/결과연결도추가했고인식기가없으면텍스트/선택으로계속한다. 실제STT/Gleo/마이크인식성공은미검증이다.
- 모델연결/보존: AppModel은확인완료/저장재조회된이번이동조건만기존추천에읽기적용하고기존Profile/방문/확정알림을덮어쓰지않는다. 실제SQLite방문과UNKNOWN확정알림fingerprint를준비한통합검사에서모드변경·주행/unknown조작차단을검사했다.
- 로컬검사: JDK17/SDK35/기존Gradle캐시에서전체JVM114/failure0/error0/skip0·lint·debug/test/unsigned release빌드 BUILD SUCCESSFUL 1m45s. 신규14저장/명령검사와AppModel연결1건포함. [요약](evidence/occupant-context/cloud/ui-local-checks.json). 새Compose기기검사/2단계프로세스검사와6장실제캡처script를추가했다. 로컬기기가없어실행완료로세지않고CI에서확인한다. 새앱버전0.10.0/10이며기능커밋후실제세APK metadata/코드/해시/개발서명을대조한다.
- 상태: Cloud저장기반통과·설정/입력연결컴파일/로컬회귀통과. 데이터단위16c7615의CI43563142120/43563146137은진행중으로확인했다. Mac연결기기없음/실제좌석SDK·STT미검증. 다음은시안대로앱영역상단사전영상/하단3D지도패널과대표방문흐름을연결한다.

## 2026-11-14 UTC · 도착 패널/지도·영상과 시나리오 연결

- 전체 위치: 새 실행절차의1저장/2설정UI/3명시입력 구현에 이어4패널·5대표흐름을 구현했다. Mac 원격은 이 세션에 노출되지 않았고 Cloud ADB 목록은 비어 있으며 KVM도 없다. Pleos 재부팅/인증 입력을 반복하지 않았다.
- 변경: 앱0.11.0/11. 시스템 차량/순정내비 영역을 가짜로 그리지 않고 앱영역만 어두운 도착 패널로 전환했다. 명시입차 후 같은 방문의 주차지도부터 열고, 상단은 수동 선택한 실제 재생 생성클립/분석, 하단은 기울인 주차배치 예시 또는 기존 공식기반5층 파란선이다. 관측 후보를 영상 우하단에 출처와 표시하고, 층·현재위치·승강연결은 미확인 유지. 미디어는 닫기/주행/ON_STOP/주차 시 해제하며 자동재개하지 않는다.
- 시나리오: 병원·학원은 기존 가상시설/동행·무단차·이번구성으로 순수 추천/대안/후보없음과 모의 요청을 연결했다. 모의요청은 TEST 모드에서 등록된 가상 접근점만 허용하고 기존 방문/알림 세션을 바꾸지 않는다. 역·공항은 개념 패널이며 미확인 차량승차점·출구번호·위치를 생성하지 않는다. 실제 도로카메라 영상은 없다.
- 검사: JDK17/SDK35/offline Gradle JVM114/실패0·lint·debug/test/unsigned release 빌드 성공(1분13초). 추가 시나리오 기기검사 컴파일/lint 성공(4초), Python 캡처 드라이버 문법검사 성공. [Cloud 검사](evidence/arrival-panel/cloud/checks.json). 실제 기기 실행은 CI 대기이며 컴파일을 실행성공으로 세지 않았다.
- 증거 제작: 새 전용 CI AVD의 실제 screenrecord90초를 artifact로 남기도록 연결했다. 주차지도/생성미리보기/5층동선·네 시나리오·우회/후보없음·닫기/저장/새PID복원 화면을 캡처한다. artifact 업로드 전 화면은 아직 확보하지 못했으며 확보 즉시 공유한다.
- 과거/새 결과: 저장 단위16c7615 CI43563142120/43563146137 전체성공. 탑승UI d3b66ed CI43563982466/43563986963은 조사 당시 실행 중이다. 이들은0.11 패널 검증으로 대체하지 않는다.0.10 로컬 실제APK metadata도 별도보존했다.
- 후속: 새 CI의 실제화면/실패를 확인하고 오류부터 수정, 최종APK/해시/개발인증서·영상과 문서를 연결한다. Mac 같은APK 크기/순정핀/Console·실제STT·외부카메라·실차·실제SMS는 별도 미검증.

### 같은 날 · 좁은 앱 폭과 검사 격리 보완

- 분할 패널 폭을 고려해600dp 미만에서는 홈 헤더2줄/탑승모드1열로 전환했다. CI는480×900 일반Android 화면도 별도캡처하고 원래1280×800으로 복원한다. 이는 Pleos분할 지원 실증이 아니다.
- 실제 기기검사에서 가상후보가 다른 검사의 설정에 영향을 받지 않도록 원래Profile을 보관→기본조건 검사→finally복원한다. 첫 컴파일은 nullable persisted를 그대로 복원해 Type mismatch 오류가 났다. 저장완료 대기와 requireNotNull로 경계를 명시해 수정하고 관련기기APK/lint/release를 다시 확인한다. 앱런타임 오류로 오인하지 않는다.

### 같은 날 2026-11-14 · 첫 실제 탑승UI 기기 결과와 자동화 실패

- d3b66ed PR CI43563986963/artifact14101918590 원본을 다운로드했다. 실제 connected XML은18개 중14pass/4phase-only skip/0fail이다. 기존주차14장/제품15장도완료했으나 새탑승스크립트는3장후 TEXT `혼자 확인 적용` 버튼을 찾지 못해 전체 android-device job은 failure다. HTTP 단계는 이 실행에서 도달하지 않았다. 성공으로 합산하지 않는다.
- [원본3장/상태/기기XML](evidence/occupant-context/first-device-run/) 보존. 아이모드 저장완료 실제화면을 확보즉시채팅이미지로표시했다. PR checkout은합성merge3cda46b이며 head d3b66ed와 구분한다. 일반Android TEST차량/실제DataStore이고Pleos/STT성공이아니다.
- 원인확인범위: 텍스트label좌표/IME가실제편집기·확인버튼을가렸을가능성은추정이다. 아직실패XML이없어원인단정하지않았다. 실제EditText조상선택→입력된solo재조회→IME닫기→후보확인으로드라이버를보완하고 입력/후보/실패때원본PNG/XML을보존한다. 기존Compose기기검사의한국어텍스트입력은통과했다. 이변경후실제성공은다음CI에서확인한다.

### 같은 날 · 시나리오 중복 안내 실제 기기 실패 수정

- b84b467 CI43565179033/43565183301 verify는통과(JVM114·lint·APK3종·서버14·진단35). connected 실행에서 새시나리오검사가 `조건에 맞는 후보가 없습니다` 텍스트2개를발견해실패했다. 제목과reason에같은문구를출력한실제UI중복이므로 selector로숨기지않고후보없음설명을한번만표시하도록수정했다. [원본XML/실패기록](evidence/arrival-panel/first-device-failure/).
- 해당실행은 캡처드라이버 전 실패해 새 패널 PNG/영상이없다. 콘솔 Finished수는중복/skip포함이므로XML기준으로기록한다. 다음부터Compose내부TestWatcher가실패직전PNG를보관하고CI가에뮬레이터종료전수집한다.
- 동일중복을포함한후속27e5a2d CI43565877673/43565881873은수정버전으로대체하기위해취소요청했다. 이두건은성공으로세지않는다. 실패원인에관계없는Pleos부팅/키변경은없다.
- 수정후 로컬 lint·debug/test/release빌드성공29초. 추천로직/저장스키마변경이아니므로이미통과한JVM을추가로반복하지않았고후속CI가전체회귀를수행한다.

- 최종패널추가검사: 생성영상분석의 진행/실패 문구를앱에표시하고, 새패널에서직접 `영상 분석`→저장된 생성C12후보를기다려PNG를남기도록검사를연결했다. 기존 분석기 성공을 새 패널버튼 성공으로 대신하지 않는다. 로컬 lint/세빌드22초성공. 입력값 검증은 키보드추천문구가아닌앱EditText의solo를재조회한다. 실제성공은수정후CI에서확인한다.
