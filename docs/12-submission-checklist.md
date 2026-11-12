# 11월 15일 제출 준비 체크리스트

2026-11-12 UTC · 기준 [개발계획](07-development-plan.md) · [최신 검증](08-verification.md)

## 제출 후보와 재현 자료

| 항목 | 현재 증거/상태 |
| --- | --- |
| 브랜치/PR | feat/android-submission-mvp, [PR#1](https://github.com/leemh-dev/dochagjari/pull/1) open/미병합 |
| 앱 버전/코드 | 0.2.0 / 2, APK 코드 3a01515. 이후 진단 도구·문서 변경은 앱 APK에 포함되지 않음 |
| APK | [artifact13871881801](https://github.com/leemh-dev/dochagjari/actions/runs/43055801516/artifacts/13871881801), debug/test/unsigned release·SHA256SUMS·metadata. GitHub 로그인 필요. 확인 시 만료 전(2027-02-09 만료 예정) |
| 해시·서명 | [실제 파일 metadata](evidence/v0.2-apk-metadata.json), [다운로드 대조](evidence/v0.2-downloaded-apk-verification.txt). debug/test는 동일 runner 개발 서명, 이전 Cloud 서명과 다르므로 새 테스트 AVD 사용 |
| 맥북 M1 8GiB | 오디오를 끈 Pleos ARM64 단독 실행, 배포 debug 설치·앱 조작, AVD 종료 후 debug/test 빌드, 새 AVD 표준 ADB 기능 검사 6/6 확인. Gradle connected 보류. 실제 내비 핀·Console 좌석은 미검증. [실측](08-verification.md) |
| 제출 release 서명 | **미완료**. unsigned release 제공. 키·비밀번호 없음. [README](../README.md)의 사용자 로컬 키 생성/서명/해시 절차를 실행하고 실제 서명 후 기록 갱신 필요 |
| 코드 검사 | 9430b7d [PR43154858975](https://github.com/leemh-dev/dochagjari/actions/runs/43154858975) 전체 성공. JVM33/서버14/lint/진단35/기능6/프로세스2/UI13장/실제HTTP 복구. 이후 현재-user/최종 문서 CI는 별도 확인 |
| 신규 진단 검사 | native 대상 실제 회귀/동일 원본 독립 확대 identity·ADB 완료 판정/fresh-device guard 포함 35건 로컬 통과. [로그](evidence/pleos-diagnostic-tests.txt). 실제 기기 성공과 구분 |
| 설치 전 확인 | `python3 scripts/pleos/adb_readiness.py --serial <전용AVD> --output /tmp/dochagjari-adb-readiness.json` 종료0일 때만 설치. transport/device + boot1 + package/activity found·각 종료0 필요 |

## 최신 환경 진단

[최신9430b7d 수동43154848171](evidence/pleos-approval-unconfirmed-latest/summary.json)은 정상 Allow 입력에도 승인창/unauthorized가 남아 새 host 터치·SDK/Console·표준 ADB 기기/복원 driver를 실행하지 못했습니다. 추가 부팅은 요청하지 않았습니다. 이전 정상 승인/설치/앱 표시 성공과 구분하며 실제 연동은 미완료입니다.9430b7d PR43154858975 일반 CI는 진단35 포함 전체 성공, 이후 현재-user/문서 CI는 별도로 확인합니다.

[8d12198 실제 앱 표시](evidence/pleos-first-app-display/summary.json)를 확보했습니다. display0/물리1개·user10·focused/visible/multi-window이며 다른 display 추정은 지지되지 않습니다. 차량/내비는 테스트/모의 상태입니다.9430b7d는 정상 창 터치/새 XML·전환 확인/현재user·표준 ADB 검사를 수동43154848171에서 확인하려 했으나 승인 차단으로 실행하지 못했습니다. 로컬 진단35건 통과, 실제 SDK/핀/Console·driver 통과는 아직 미검증입니다.

[실제 a8c91d2 Pleos](evidence/pleos-installed-ui-blocked/summary.json): 정상 승인/device·boot1·package/activity·각rc0·설치 직전 재확인·APK 설치 성공. 새8GiB guest `/data`여유약5.3GiB/NORMAL, 이전 설치 공간 오류는 재발하지 않았습니다. UI dump timeout/계기 영역만 표시로 실제 앱 화면·SDK/핀/Console는 미검증입니다. 8d12198 관찰에서 실제 앱 표시와 display0/현재user10을 확인했습니다.

[connected 최종 결과](evidence/pleos-installed-ui-blocked/connected-final-summary.json)는 test APK 설치 commit 응답 없음으로 tests0/실패입니다. 기능6건 실패가 아니며 console 경고를 원인으로 확정하지 않습니다.5a6ed1b의 정상 ADB instrumentation 대체 driver는 준비/로컬 진단33건 통과, 실제 실행은 아직 미검증입니다. 기존 기기/서버DB/저장 정책/인증/배포 APK를 바꾸지 않습니다.

## 출품 범위별 완료 경계

| 기능 | 확인한 것 | 제출 전 남은 것 |
| --- | --- | --- |
| 픽업·주차·우회 | 일반 Android UI/앱 모델에서 후보 변경·후보 없음 | Pleos 실제 SDK 결과와 37.48544722/127.03636666, 37.48615/127.03715의 순정 핀 대조 |
| 좌석·저장 | 테스트 입력, DataStore 실제 완료값·프로세스 재시작 복원 | Console 좌우 Empty→Occupied→Empty, 앱 초기값/콜백/옵션 제안. Console·앱 수신·실차 각각 기록 |
| 병원 | 가상 병원에서 동행·휠체어·무단차 미확인 제외 | 실제 동선 근거가 없으므로 가상 표시 유지 |
| 영상 | 실제 일반 Android TFLite/ML Kit C12·생성/공개 영상·점유→우회 | Pleos 순정 외부 카메라·실차 미검증. 사전 영상 결과로 대체하지 않음 |
| 자동 알림 | 같은 세션/지문 복원, 실제 Android HTTP 서버모의 응답 유실·앱/서버 재시작 후 GET복구·POST1/공급자0. [원본3장](evidence/android-http-recovery/README.md) | SENS 수락·휴대폰 수신은 별도 검증. 자격증명/등록 발신번호/동의 수신자 없으면 모의 유지 |
| Pleos 환경 | 실제 ADB device/boot1/package/activity·각rc0 확인 | a8c91d2 새8GiB에서 실제 설치 성공. 계기 영역/UI dump timeout의 display/activity를 관찰 중. connected는 test APK 설치 단계 tests0/실패. SDK/Console 미검증 |

새 AVD에서 실제 서명 APK를 설치한 뒤 두 내비 핀·좌석·영상·설정 유지·주행 제한·알림 결과를 시연하고 녹화합니다. 현재 맥북 결과는 위 항목과 검증 기록을 따릅니다. 실차·실제 SMS·제출 서명은 미검증입니다.

11월 14일 18:00 제출 후보 고정·녹화·커밋/해시 대조, 11월 15일 18:00 내부 제출 목표는 [개발계획](07-development-plan.md)의 일정입니다. 현재 실제 제출 완료나 전체 실연동 완료 상태는 아닙니다. 제출 양식·시연 영상·공개 저장소·설치 산출물의 버전/코드/검증 표시를 마지막으로 대조해야 합니다.

## 제출 시연 녹화·증거 수집 순서

아래는 **실행 예정 절차**이며 완료 증거가 아닙니다. 제출 후보 APK를 새 전용 AVD에 설치한 뒤 실행한 항목만 체크하고, 실패/권한 없음/미검증을 편집으로 숨기지 않습니다.

1. APK 버전·코드 커밋·SHA256·서명 종류와 기기 시리얼/이미지 버전을 기록합니다. 설치 직전 준비 helper JSON과 install rc를 보관합니다. 제출 키 서명 전이면 개발 서명/미서명 상태를 명시합니다.
2. 학원 바로 태우기→주차 후 데리러 가기→첫 후보 이용 불가를 시연합니다. 실제 내비 모드에서 요청 좌표·SDK 콜백·순정 목적지 핀을 각각 캡처합니다. 콜백만 성공하고 핀이 확인되지 않으면 핀 미검증으로 남깁니다.
3. Console 좌우 각각 Empty→Occupied→Empty를 바꾸고 같은 시점의 앱 좌석 문구·SDK 이벤트·저장 옵션 제안을 캡처합니다. 저장 완료 후 앱 프로세스를 종료/재실행해 복원을 확인합니다. Console 조작과 앱 수신을 나누고 AVD를 실차로 표기하지 않습니다.
4. 가상 병원 동행/휠체어와 후보 없음, 주행 중 조작 제한을 시연합니다. 가상 병원·차량 테스트 입력 표시가 보이게 촬영합니다.
5. 같은 Android 앱에서 사전 빈/장애물/공개 영상의 점유·C12 인식·우회를 보여 줍니다. 사전 영상이며 순정 외부 카메라가 아님을 표시합니다. 호스트 OCR 화면으로 앱 내 분석을 대체하지 않습니다.
6. 동의/확정 위치/정차 조건의 모의 알림과 같은 세션 복구를 보여 줍니다. 실제 SENS를 사용하지 않았으면 모의로 표기합니다. 실제 공급자 수락·휴대폰 수신은 별도 증거가 있을 때만 추가하며 수신번호/토큰은 노출하지 않습니다.

최종 증거 목록에는 `항목 / 실제 실행 환경 / 입력 방식 / 결과 / 실행 시각 / 코드·APK 해시 / 로그·원본 화면 링크 / 미검증 경계`를 남깁니다. 영상·공식 접수양식·공개 저장소·설치 APK의 버전/링크를 제출 직전에 대조합니다. 실제 제출은 아직 실행하지 않았습니다.

## 11월 12일 화면/오류 우선 점검 추가

- [실제 원본13장/검사 요약](evidence/android-ui-screens/README.md): cc1c19e PR43128321081/push43128313393 전체 성공. 앱 테스트 입력/가상 병원/생성 영상이며 Pleos 실연동과 구분합니다.
- 실제 Compose XML에서 클래스 기반 스위치 선택 오류를 재현·수정했습니다(900ae4c). 로컬 Python17건 통과, 실제 Pleos 선택기 조작은 아직 미검증입니다.
- Pleos055bdea 수동43128107467은 허용 후에도 승인창이 남고 unauthorized였습니다. 정상 누름·떼기 비교43129699644도 unauthorized였습니다. OCR 미인식은 unknown이며 원본/ADB를 대조합니다. 키/인증/항상 허용 설정은 변경하지 않습니다.
