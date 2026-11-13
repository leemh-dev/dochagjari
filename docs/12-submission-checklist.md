# 11월 15일 제출 준비 체크리스트

2026-11-13 UTC · 기준 [개발계획](07-development-plan.md) · [최신 검증](08-verification.md)

## 4단계 시설 목적지/명시적 입차 — 일반 Android·모의 입력 검증 통과

앱 **0.6.0 / 6**, 기능 `f959cb9`. 판교점 공식 시설 대표 마커를 차량 진입점과 구분하고, 같은 방문에 요청 ID/목적지 snapshot·전달 claim·이벤트·명시USER/TEST입차와 지도/닫기 복원을 연결했다. SDK 함수 반환·요청ID없는콜백·실제핀·입차는 별도다. 옛JSON/SQLite schema·방문/자료/설정/알림을 보존한다.

최신 CI43371857767 verify/android-device 모두 success. 원본 JVM62/실패0·서버14/진단35·lint/세APK, 실제기기12pass·프로세스전용2skip후별도종료/복원2단계PASS,UI29장/새4장과HTTP모의복구가통과했다. APK세파일hash/manifest/서명을독립대조했고기기검사는동일소스별도runner재빌드라는수준을구분한다. [계약](21-road-navigation-and-entry.md) · [공식근거/원본화면·검사/APK](evidence/stage4-road-entry/README.md).

실제SDK요청/콜백·순정핀/자동입차/실차·기존Mac설치본업그레이드는 미검증이다. 시설 좌표를 차량입구로, 모의 결과를 실제 성공으로 표시하지 않는다. 다음은5단계관측/추천갱신이다.

## 3단계 선택적 좌석 역할 — 로컬/일반 Android 기기 통과

앱 **0.5.0 / 5**, 기능 `c98630d`·최신 저장 완료 제안 수정 `a8e440b`. 역할/카시트·건너뛰기, 기존 파일/6개 이동 조건·방문ID·확정 알림 보존을 구현했다. 점유와 사용자 지정 역할을 분리하며 제안은 최신 SAVED/revision/persisted에서만 만든다.

최신 CI43366222134의 verify/android-device 모두 성공. 원본 보고서 JVM52/실패0, 서버14/진단35·lint/세APK 빌드 통과. 실제 기기11통과·프로세스전용2skip후별도2단계PASS, UI25장/새역할4장·저장값재조회/건너뛰기/종료복원·HTTP 모의복구 성공. 실제 CI APK 세 파일의 hash/manifest/개발인증서·미서명을 독립 대조했다. [설계](20-optional-seat-roles.md) · [원본 화면·보고서·APK 식별](evidence/stage3-seat-roles/README.md).

쓰기 실패는 주입 단위검사이며 실제 기기 디스크 고장 실측이 아니다. 일반 Android/테스트 입력 성공을 Pleos SDK·Console·실차나 실제 SMS 수신으로 표시하지 않는다. 기존 Mac 설치본의 실제 업그레이드는 이번에 실행하지 않았다. 다음은 4단계 목적지/내비/입차다.

## 신규 2단계 APK

앱0.4.0/4·코드e573e8e. [실제로컬APK식별/네이티브렌더](evidence/stage2-native-guide/README.md). JVM45/lint/세빌드·호스트중간프레임은통과했고실제일반Android CI43358680681 verify/android-device가모두통과했다. 실제기기JUnit9통과·프로세스전용2skip후별도2단계PASS,21장원본/동일방문닫힘복원과HTTP모의복구가성공했다. 새기기화면5장과CI APK해시/서명을독립열람·대조했다. 앱지도2단계와웹예시/호스트/기기/실차를구분한다. 기존0.3/0.2보관본과서명/해시를혼동하지않는다.

## 신규 1단계 APK — 기존0.2 보관본과 구분

앱0.3.0/3·기능코드4d31a15,metadata패키지파싱수정3b9a3c5. [실제 로컬 APK식별/검사](evidence/stage1-visit-db/README.md). JVM41/lint/세빌드와CI43344658563 verify/android-device가통과했다. 기기검사/새PID복원캡처절차가포함되며원본artifact다운로드는Cloud터널403으로차단됐다. 새artifact development-apks-and-metadata의코드/manifest/해시/서명과설치전대조한다. 이전artifact13871881801·0.2메타데이터는과거보관증거다. 도로/입차/지도애니메이션·관측/귀환전체완료는아니다.

## 다음 APK의 전체 흐름 완료 기준 — 현재 미완료

[앱 구현계획](17-in-app-visit-guidance-plan.md) 기준: 선택설정/건너뛰기·사전DB조회·에뜨와목적지·도로내비/테스트입차·앱2.5D파란선·영상/테스트관측갱신·주차저장/요약·닫기/재개/복원을 같은 APK에서 이어서 검증한다. 음성/실제SDK와 테스트를 구분하고 주행중 상세조작제한·새입차 중복·권한철회·종료/복원·설정이행·기존SMS복구를 확인한다. 기존 웹GIF/테스트 성공을 이 체크 완료로 계산하지 않는다. 새APK 생성 시 코드/버전/SHA/서명을 별도 기록한다. 이 절은 전체 흐름의 완료 기준이며 현재4단계의일반Android/모의·명시TEST흐름까지완료했다.

## 전체 시나리오 표현 예시

[2.5D 지도·파란선 애니메이션 HTML/GIF](evidence/pangyo-scenario-preview/README.md)을 브라우저에서 확인했다. 앱 외부 시각화이며 실제 차량 주행·주차/귀환·지도 SDK 재사용·Android 구현 완료가 아니다. 번호 미확인은 주변 시설 근거의 추정 안내로 표시한다. 실제 앱 적용/기기 녹화는 남아 있다.

## 앱 외부 실제 경로 예시 확보

[공식 지도 5F 에뜨와 길찾기·원본3장](evidence/pangyo-route-example/README.md)을 확보했다. 웹 지도 실행이며 앱 DB/도면 길찾기/실내 측위·Pleos 연동 성공과 구분한다. 차량 게이트 번호·기둥→엘리베이터 연결·해당 엘리베이터 호기 번호·m 거리 확인은 남아 있다. 문화센터 안내의 C1~C3/5·6호기/보행7게이트를 에뜨와 전체 경로로 확정하지 않는다.

## 확정 시나리오 — 2026-11-13 UTC

사용자가 목적 매장을 **현대백화점 판교점 5층 에뜨와**로 확정했다. 출발지는 현대자동차 양재 사옥이다. [공식 층별 안내 조회 증거](evidence/hyundai-store-check/README.md)로 판교점/에뜨와/5F 유·아동 구역을 확인했다. 게이트 번호·주차 구역·엘리베이터·도면 보행 경로와 거리, 앱 DB/추천/주차·귀환 구현은 아직 별도 조사/개발 대상이다. 과거 아가방 조회 결과는 이력으로 보존하며 두 브랜드를 별칭으로 합치지 않는다.

## 제출 후보와 재현 자료

| 항목 | 현재 증거/상태 |
| --- | --- |
| 브랜치/PR | feat/android-submission-mvp, [PR#1](https://github.com/leemh-dev/dochagjari/pull/1) open/미병합 |
| 앱 버전/코드 | 0.2.0 / 2, APK 코드 3a01515. 이후 진단 도구·문서 변경은 앱 APK에 포함되지 않음 |
| APK | [artifact13871881801](https://github.com/leemh-dev/dochagjari/actions/runs/43055801516/artifacts/13871881801), debug/test/unsigned release·SHA256SUMS·metadata. GitHub 로그인 필요. 확인 시 만료 전(2027-02-09 만료 예정) |
| 해시·서명 | [실제 파일 metadata](evidence/v0.2-apk-metadata.json), [다운로드 대조](evidence/v0.2-downloaded-apk-verification.txt). debug/test는 동일 runner 개발 서명, 이전 Cloud 서명과 다르므로 새 테스트 AVD 사용 |
| 맥북 M1 8GiB | 오디오를 끈 Pleos ARM64 단독 실행, 배포 debug 설치·앱 조작, AVD 종료 후 debug/test 빌드, 새 AVD 표준 ADB 기능 검사 6/6 확인. Gradle connected 보류. 실제 내비 핀·Console 좌석은 미검증. [실측](08-verification.md) |
| APK 서명/제출 목표 | 설치 가능한 개발 서명 APK로 검증. 실제 앱마켓 출시/출시용 키 준비는 목표에서 제외. unsigned release는 별도 보관본이며 서명 완료본으로 표시하지 않음. 제출 형식 요구는 별도 확인 |
| 코드 검사 | 9430b7d [PR43154858975](https://github.com/leemh-dev/dochagjari/actions/runs/43154858975) 전체 성공. JVM33/서버14/lint/진단35/기능6/프로세스2/UI13장/실제HTTP 복구. 이후 현재-user/최종 문서 CI는 별도 확인 |
| 신규 진단 검사 | native 대상 실제 회귀/동일 원본 독립 확대 identity·ADB 완료 판정/fresh-device guard 포함 35건 로컬 통과. [로그](evidence/pleos-diagnostic-tests.txt). 실제 기기 성공과 구분 |
| 설치 전 확인 | `python3 scripts/pleos/adb_readiness.py --serial <전용AVD> --output /tmp/dochagjari-adb-readiness.json` 종료0일 때만 설치. transport/device + boot1 + package/activity found·각 종료0 필요 |

## 추가한 핵심 흐름 검증 — 아직 미완료

- 첫 범위는 현대자동차 양재 사옥→더현대 서울 대표 매장 한 곳이며 두 번째 매장은 첫 흐름 이후 비교용이다([단계별 계획](15-hyundai-department-demo-plan.md)). 아가방 후보 입점/층·실제 출차/게이트 좌표·주차 연결은 조사 전이다.
- 실제 시설/실내 목적지: 최신 관련 자료 검색·출처/확인일·게이트/구역/엘리베이터 연결·내비 전달.
- 입차 확인→지원 카메라/명시한 사전 영상 관측→정차 문맥의 층/구역/기둥/벽 단서 저장→귀환.
- 표기 없음·동일 번호 다른 층·관측 후 이동 재개·카메라 미지원 대체 입력 시험.
- 공공/공식/리뷰/제휴의 실제 확보 상태와 테스트/실제 SDK를 구분한다. 제휴/카메라 접근 완료를 가정하지 않는다.
- [상세 설계/시험 항목](14-data-and-parking-observation.md). 기존 기능 검사 성공으로 이 흐름까지 완료 처리하지 않는다.

- 추가 필수: 사전 시설 DB/별칭 조회·오프라인/반복 조회 시 외부 검색 0·정보 시점/갱신 상태, 게이트→엘리베이터→아가방 층 안내.
- 도면 확보 시 추가: 실제 보행 경로선·확대/축소 정합·벽 통과 없음·버전 불일치/연결 없음 처리. 축척 확인 시에만 m 거리 표시. 실제 입점·시설 연결·도면은 조사 전이다([설계/검증 기준](16-preloaded-db-and-indoor-route.md)).

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

새 AVD에서 설치 가능한 개발 서명 APK를 설치한 뒤 두 내비 핀·좌석·영상·설정 유지·주행 제한·알림 결과를 시연하고 녹화합니다. 현재 맥북 결과는 위 항목과 검증 기록을 따릅니다. 실차·실제 SMS는 미검증입니다. 출시용 release 서명은 이번 검증/시연 목표에서 제외합니다.

11월 14일 18:00 제출 후보 고정·녹화·커밋/해시 대조, 11월 15일 18:00 내부 제출 목표는 [개발계획](07-development-plan.md)의 일정입니다. 현재 실제 제출 완료나 전체 실연동 완료 상태는 아닙니다. 제출 양식·시연 영상·공개 저장소·설치 산출물의 버전/코드/검증 표시를 마지막으로 대조해야 합니다.

## 제출 시연 녹화·증거 수집 순서

아래는 기존 확장 기능의 **실행 예정 절차**이며 완료 증거가 아닙니다. 제출 후보 APK를 새 전용 AVD에 설치한 뒤 실행한 항목만 체크하고, 실패/권한 없음/미검증을 편집으로 숨기지 않습니다.

1. APK 버전·코드 커밋·SHA256·서명 종류와 기기 시리얼/이미지 버전을 기록합니다. 설치 직전 준비 helper JSON과 install rc를 보관합니다. 검증 APK의 개발 서명을 명시하고, 별도 unsigned release를 설치/서명 완료본과 혼동하지 않습니다.
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
