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
