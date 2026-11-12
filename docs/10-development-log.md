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
