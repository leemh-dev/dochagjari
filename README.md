# 도착자리

**현행 구현 완료(Cloud/일반Android): 0.11.0/11·f0640b0**, `feat/arrival-context-ux`/[PR#2](https://github.com/leemh-dev/dochagjari/pull/2). 독립 탑승 모드·이번 구성, 상단 생성영상/하단 기울인 주차·5층 지도, 같은 방문의 주차 저장/귀환을 구현했습니다. [최종 CI](https://github.com/leemh-dev/dochagjari/actions/runs/43569033733) JVM115·서버14·진단35·기기15건과 별도 복원4단계까지 통과했습니다. [실제 화면/검사](docs/evidence/arrival-panel/f064-device/README.md) · [90초 시연/20초 모션](docs/evidence/demo-video/README.md) · [실제 검사 APK](https://github.com/leemh-dev/dochagjari/actions/runs/43569033733/artifacts/14104088875).

Mac/Pleos의 새버전 분할·SDK핀/Console·실제 음성/외부카메라·실차/SMS는 미검증입니다. AI 자료 보완 worker는 미구현이며 현재는 사전DB를 씁니다. [현재 수준](docs/37-current-implementation.md) · [M1/8GB·외장SSD 설치/검사 인계](docs/38-mac-pleos-v0.11-handoff.md). debug/test 개발서명·release unsigned, 앱마켓 출시는 범위 밖입니다. 아래 구버전 실적은 이력으로 구분합니다.


**목적지 너머, 만날 자리까지.** 처음 가거나 익숙하지 않은 장소에서 진입·승하차·대기·주차와 실제 방문/만남까지 연결하는 Kotlin Android 앱을 개발합니다. 매장·병원·학교/학원 픽업·역/공항 마중이 대표 시나리오입니다. 현재 판교점 에뜨와 시설 조회·방문 저장·네이티브 동선 재생·선택적 좌석 역할·같은 방문의 관측 단서 저장과 기존 픽업/병원 시연 기능이 있습니다. 역/공항 실제 시설 연동은 아직 검증하지 않았습니다. [11월 15일 개발계획](docs/07-development-plan.md)을 구현 기준으로 사용합니다.

가상 학원·병원 각각 한 곳을 동봉합니다. 사람 출입구를 유지하면서 방문 모드·이용 불가·영상 점유에 따라 차량 도착점이 바뀝니다. 순정 내비 좌표의 실제 승하차 허용 여부나 실제 시설 무단차 경로를 검증한 서비스는 아닙니다.

개발을 이어갈 때는 [다음 개발 실행 프롬프트](docs/09-next-development-prompt.md)를 사용합니다. [제출 체크리스트](docs/12-submission-checklist.md)에서 남은 실제 확인 항목을 확인합니다. 주요 진행 절차·실패·후속 작업은 [개발 일지](docs/10-development-log.md), 기능별 완료 증거는 [검증 기록](docs/08-verification.md)에 남깁니다.

## 현재 우선순위 · 지도 중심 Android UX

[Cloud 개발 ↔ 맥북 Pleos 단계별 절차](docs/36-cloud-mac-development-workflow.md)를 추가했습니다. 각 단계의 Cloud 검사와 Mac 기기 확인을 따로 기록하며, M1/8GB는 외장SSD·AVD한대·Cloud빌드APK 확인을 기본으로 합니다. Mac 미연결 시에도 독립 구현은 계속합니다. 현재는 준비/절차 정리 상태이고 첫 코드는 다중 탑승 저장/이행부터입니다.

**현재 개발 읽기 순서:** [재개 기준34](docs/34-implementation-restart.md) → [독립 탑승 설정33](docs/33-occupant-modes-and-trip-context.md) → [최신UX31](docs/31-road-scenario-ux-concepts.md). [코드 보존/교체 기준35](docs/35-code-retention-and-cleanup.md)에 따라 공통 저장·복구 기반은 유지하고 옛 화면은 대체 구현 후 정리합니다. 아래의 이전 계획/시안 링크는 진행 이력이며 현재 실행 지시는 [단일 다음 프롬프트](docs/09-next-development-prompt.md)를 따릅니다.

**새 개발 브랜치: `feat/arrival-context-ux`**. 기존 최신f164ec7에서 분리해 시안/앱/데이터를 보존했습니다. [새 개발 기준과 순서](docs/34-implementation-restart.md) · [시안PNG22개/최신6개 보존 대조](docs/evidence/implementation-restart/design-baseline.json). 첫 코드 단위는 내비와 독립된 다중 탑승 프리셋/이번 구성의 저장·기존 설정 이행입니다. 기존 PR#1은 유지합니다.

[탑승 설정 후속 방향](docs/33-occupant-modes-and-trip-context.md): 혼자/아이 동반/가족 등 좌석별 구성을 여러 프리셋으로 저장하고, 이번 구성은 음성·선택·지원 센서로 확인하는 **내비와 독립된 설정**으로 설계합니다. 도착 안내는 확인한 조건을 활용합니다. 현재 앱은 단일 좌우 역할 저장이며 새 다중 프리셋/음성 인지는 아직 구현하지 않았습니다.

[시연영상 제작 계획](docs/32-demo-video-production-plan.md): 3분30초 서비스 본편과 약90초 실제 앱 실행기록을 제안합니다. 양재→판교점 에뜨와 방문/주차단서/귀환을 한 이야기로 연결하고 병원·학원·역/공항은 확장 장면으로 보여줍니다. [210초 콘티](docs/demo/video-plan/shot-list.json). 현재는 계획이며 영상/새 앱을 제작한 것은 아닙니다.

[도로변 시나리오 UX 시안4장](docs/ux/road-scenario-concepts/README.md): 학원 픽업·병원 동행·정자역 마중·공항 배웅을 카메라/3D도로지도로 표현했습니다. 첫 번째 차량정보·두 번째 내비 구성을 유지하고 **세 번째 상단만 카메라, 하단만3D지도**입니다. [설계/정차후보 경계/수정 기록](docs/31-road-scenario-ux-concepts.md). 디자인 검토안이며 앱/APK는 변경하지 않았습니다.

[도로 3D/승하차 참고 이미지19장 조사](docs/30-road-ui-reference-review.md): 실제 차량UI10장과 병원·학원·역·공항 현장9장을 모아 자료부터 검토합니다. [사진별 출처](docs/research/road-ui-references/README.md). 첫 번째 차량정보·두 번째 내비 유지, 세 번째 상단 카메라/하단3D지도 구조를 보존하며 이번에는 새 시안/앱을 만들지 않았습니다.

[정정된 3분할 UX 시안](docs/ux/photo-based-3d/README.md): 첫 번째 차량 정보·두 번째 내비는 기존 구성을 유지하고 **세 번째 상단에 빌트인캠 영상, 하단에 기울인 3D 주차 지도**를 배치합니다. 건물/카메라를 3D로 바꾼 이전 해석을 정정했습니다. 생성 디자인이며 실제 센서/앱 실행은 아닙니다. [화면 규격·자료 경계](docs/29-photo-based-3d-ux.md). 기존 앱/APK는 유지합니다.

[실제 장소 사진 조사](docs/28-real-place-photo-research.md): 분당서울대병원·분당 수내동 학원가·인천공항 T1 장기주차장·정자역의 사진13장과 공식 안내도2장을 확보했습니다. [출처 목록](docs/research/real-place-references/README.md). 다음 디자인은 실제 시설 자료를 먼저 대조합니다. 분당 학부모 승용차 픽업 대기 사진은 미확보이며 셔틀버스 보도사진과 구분합니다.

[병원·학교/학원 픽업·역/공항 마중의 새 UX 예상도](docs/ux/scenario-concepts/README.md)를 저장했습니다. 기존 매장 시안과 같은 테마의 가상 정차 화면이며 실제 앱 실행/시설 지도/연동 성공이 아닙니다. [장면별 행동·전환 규칙](docs/27-scenario-ux-concepts.md).

2026-11-13 사용자 결정: **낯선 장소의 진입·주차·만남에 어려움을 겪는 사람을 위해 도착자리를 고도화합니다.** 매장 방문은 시나리오1이며 병원 동행·학교/학원 픽업과 역/공항 마중을 주요 상황으로 다룹니다. 주차 인식/기억은 여러 시나리오의 공통 기능입니다. [전체 시나리오](docs/02-service-scenarios.md) · [재개 방향과 순서](docs/26-service-continuation.md). 기존 주차 기능의 운영 사례와 현재 Pleos에서 동일한 전체 서비스가 제공되는지는 구분합니다. 현재 앱/APK 검증은 아래 기존 결과이며, 새 시안이나 이번 방향 결정이 실제 연동 성공을 뜻하지 않습니다.

[설계24](docs/24-navigation-ux-design.md)에 이어 **Android0.9.0/9·e2b93ba**에 [제품 UI](docs/25-android-navigation-ux.md)를 적용했습니다. 목적지 카드→명시 입차→기울인5층 파란선→주차 저장 확인/귀환으로 이어집니다. 선택적 탑승 설정은 상단 설정에서, 기존 기능/UUID·영상/HTTP 진단은 `개발자 도구`에서 확인합니다. 기존 DB/설정/알림과 복구는 유지합니다. [현재 검사·원본 화면·APK](docs/evidence/android-product-ux/README.md).

기본은 모의 내비/TEST 차량입니다. `개발자 도구 → 연동·테스트 → Pleos SDK`는 정상 SDK 목적지 요청을 선택할 뿐 지도 삽입이나 실제 핀 성공을 뜻하지 않습니다. 순정 내비 안내 중 앱을 임의로 덮지 않습니다. 주차장에 들어와 정차한 뒤 명시 입차로 앱의5층 미리보기를 엽니다. 주차층/승강연결·현재 위치·복귀선은 확인되지 않으면 만들지 않습니다.

실제 Pleos 확인은 Mac 원격/에뮬레이터가 켜진 뒤 아래 기존 readiness/서명대조·새 전용 AVD 절차로 진행합니다. 이번 Cloud는 기기0/KVM없음이고 사용자는 Mac이 꺼져 있다고 확인했으므로 실제 내비를 실행하지 않았습니다. 준비 후 SDK 콜백/실제 핀/입차를 각각 확인하고 같은 앱의5층 화면과 비교합니다. 실제 Pleos 적합성/시스템 내비 통합과 일반 Android 실행 결과를 구분합니다.

## 6단계 주차 완료·요약·귀환

입차한 같은 방문에서 정차 중 `직접 주차 완료 · 센서 감지 아님` 또는 `주차 완료 테스트`를 누릅니다. 관측을 중지하고 SQLite 저장값을 다시 읽은 뒤 요약을 엽니다. 기본8초 뒤 요약을 닫고 기록은 유지합니다. 현재 실행에서 자동닫기를 끌 수 있습니다. 완료만으로 층·기둥을 채우지 않습니다.

주차 후 직접 확인한 층/구역/기둥/승강기/메모를 입력하고 `입력한 단서 확인 · 저장`을 누릅니다. 화면 입력과 저장한 기록·출처·확인 시각을 구분합니다. 빈 값은 미확인으로 저장합니다. TEST 채우기는 실제 판교점이 아니며 `비우고 직접 입력` 전까지 TEST 출처를 유지합니다.

`차로 돌아가기 · 저장 단서`는 저장 기록을 엽니다. 주차장 승강 연결·현재 위치가 미확인이므로 복귀 경로/거리·역방향 선을 만들지 않습니다. 5층 지도는 확인된 매장행 동선으로만 수동 열 수 있습니다. 주행 또는 명시적 다시 찾기는 이전 주차 기록을 남기고 같은 방문에서 새 확인을 준비하며, 관측/지도/내비를 자동 재개하지 않습니다. 재시작도 저장 기록만 복원합니다.

앱 **0.8.0/8·84acdd3**, [CI43404749340](https://github.com/leemh-dev/dochagjari/actions/runs/43404749340) 전체성공. JVM99·기기12pass/별도종료복원2단계·주차흐름14장/HTTP모의복구를확인했습니다. [주차미확인요약](docs/evidence/stage6-parking-return/04-unknown-parking-summary.png) · [귀환기록](docs/evidence/stage6-parking-return/08-return-record.png) · [재시작복원](docs/evidence/stage6-parking-return/10-restored-hidden.png). 다음은7단계전체시연/제출준비입니다. [계약](docs/23-parking-summary-and-return.md) · [새 검사/화면/APK](docs/evidence/stage6-parking-return/README.md). 실제 차량 카메라·측위·귀환 연결 검증과 구분합니다.

## 5단계 입차 후 관측·추천

입차가 기록된 같은 방문에서 `테스트 관측 시작` 또는 `생성 C12 방문 관측`/`생성 빈 방문 관측`/`공개 영상 방문 관측`/`방문 영상 선택`으로 시작합니다. 출처와 원문·분석 시각·클립 시각을 저장하며 반복된 문자 후보를 표시합니다. TEST B3/C12·B4/C12, 낮은 신뢰도·오래됨·표기 없음을 비교할 수 있습니다.

관측 문자와 실제 차량 위치는 다릅니다. 생성 C12는 실제 판교점 기둥이 아니며 연결 근거가 없는 주차층·승강기·현재 위치를 확정하지 않습니다. 기존5층 동선과 지도에 미확인 안내를 유지합니다. 닫기·관측 중지·탭 이탈·백그라운드·주행/unknown·권한 오류 때 중지하고 재시작은 저장 기록만 복원합니다. 실제 외부 카메라는 미연동입니다. [관측 계약](docs/22-visit-observation.md) · [검사·화면·APK](docs/evidence/stage5-visit-observation/README.md).

앱 **0.7.0/7·8ea046a**, [CI43390462215](https://github.com/leemh-dev/dochagjari/actions/runs/43390462215) 전체성공. JVM82·기기12pass/별도복원2단계·실제UI37장/관측8장을확인했습니다. [Android 생성C12관측](docs/evidence/stage5-visit-observation/35-observation-android-video.png) · [층충돌보류](docs/evidence/stage5-visit-observation/31-observation-floor-conflict.png) · [자동재개없는복원](docs/evidence/stage5-visit-observation/37-observation-restored-idle.png) · [개발APK](https://github.com/leemh-dev/dochagjari/actions/runs/43390462215/artifacts/14021185433). 실제차량카메라/위치성공이아니며다음은6단계주차완료·요약/귀환입니다.

## 4단계 시설 목적지→명시적 입차

정차 상태에서 `매장 방문`→별칭 검색→`방문 준비 · 저장` 후 `시설 목적지로 내비 요청`을 누릅니다. 기본은 모의 요청입니다. 판교점 공식 시설 대표 좌표와 주소를 표시하며, 이 좌표를 차량 주차장 입구로 표시하지 않습니다. 출발지 양재 사옥은 계획 라벨이고 실제 차량/AVD 위치를 옮기지 않습니다.

모의 계산/SDK 함수 반환·콜백은 입차가 아닙니다. 실제 순정 내비 핀은 별도로 확인합니다. `직접 입차 확인 · 센서 감지 아님` 또는 테스트 차량의 `입차 테스트 · 실제 감지 아님`으로 명시적으로 확인하면 같은 방문의 네이티브 5층 동선을 엽니다. 주행/unknown에는 입력과 상세 지도를 제한합니다.

닫으면 같은 입차/재시작으로 다시 열리지 않습니다. 앱을 재실행하고 같은 방문의 HIDDEN/입차 기록을 확인합니다. 내비 전달 구간에서 종료돼도 자동 재요청하지 않습니다. `앱 내비 연결 취소`는 앱의 추적을 취소하며 순정 내비 경로 중지를 보장하지 않습니다. [상태·복구 계약](docs/21-road-navigation-and-entry.md) · [공식 근거/검증/APK](docs/evidence/stage4-road-entry/README.md). 차량 게이트·주차장/승강기 연결·실제 자동입차/카메라 측위는 미확인입니다.

최신 일반 Android CI에서 모의 계산/취소·주행 제한·명시TEST입차/동일 방문 지도·닫기/종료복원이 통과했습니다. [실제 입차 테스트 지도](docs/evidence/stage4-road-entry/27-explicit-entry-native-guide.png) · [APK/metadata](https://github.com/leemh-dev/dochagjari/actions/runs/43371857767/artifacts/14012324593). 실제SDK/핀·자동입차/실차 검증과 구분합니다.

## 3단계 선택적 탑승 역할

정차 상태에서 `좌석·조건`을 엽니다. 지원이 확인된 뒷좌석 좌·우에 역할(미지정/성인/어린이/영유아)과 카시트 사용을 선택할 수 있습니다. `건너뛰기 · 설정 없이 안내`는 역할 사용만 중지하고 기존 선택/이동 조건/방문을 지우지 않습니다. 설정하지 않아도 매장 안내를 사용할 수 있습니다.

화면 선택값과 `설정 저장 완료`를 구분합니다. 저장 실패는 `설정 다시 저장`으로 재시도합니다. 점유 신호는 역할을 판별하지 않으며, 저장한 역할 사용+OCCUPIED일 때만 이번 탑승에도 맞는지 확인하라는 제안을 표시합니다. 주행/상태 미확인에는 변경을 막습니다. [설계·보존 규칙](docs/20-optional-seat-roles.md) · [새 검사/APK/실제 화면](docs/evidence/stage3-seat-roles/README.md).

최신 일반 Android CI에서 저장값 재조회·건너뛰기·주행 차단·새 프로세스 역할/기존 조건/알림 복원이 통과했습니다. [실제 앱의 점유 확인 제안 화면](docs/evidence/stage3-seat-roles/24-seat-occupancy-suggestion.png) · [개발 APK/metadata](https://github.com/leemh-dev/dochagjari/actions/runs/43366222134/artifacts/14010825928).

## 2단계 앱 지도 확인

[2단계 검사와 화면 구분](docs/evidence/stage2-native-guide/README.md)을 따릅니다. 정차 상태에서 아래1단계 절차로 방문을 준비한 뒤 `5층 동선 보기`를 누릅니다. 앱 안의 일부 개략도에서 파란 선이 엘리베이터→에뜨와 순서로 재생됩니다. `재생/일시정지`, `처음부터`, `평면 보기/기울여 보기`, `닫기`를 확인합니다. 시스템 동작 줄이기 설정이면 자동 재생하지 않고 버튼으로 재생합니다.

닫으면 같은 방문의 표시 상태가 저장되며 재실행 후 지도를 자동으로 열지 않습니다. `연동·테스트`에서 주행/미확인으로 바꾸면 열기가 제한됩니다. 이동점은 경로 재생이며 실제 현재 위치가 아닙니다. 기둥/호기·거리·무단차와 실제 주차장 연결은 미확인입니다. [실제 일반 Android 앱 캡처](docs/evidence/stage2-native-guide/18-native-route-animation.png)에서 재생25%와 평면/100%·닫힘 복원을 확인했습니다. 호스트 렌더와 실제 AVD·Pleos를 구분합니다.

## 1단계 매장 방문 확인

[실행 단계](docs/18-execution-stages.md)와 [현재 검사/APK식별](docs/evidence/stage1-visit-db/README.md)을 따릅니다. 새 전용 AVD에 현재 개발 서명 APK를 설치하고 정차 테스트 상태에서 다음을 확인합니다.

1. `매장 방문` 탭에서 기본 문구 `에뜨와 판교 현백`으로 `저장된 매장 찾기`를 누릅니다.
2. 공식 자료의 `현대백화점 판교점 · 5F 에뜨와`, 확인일/출처·14개 보행 좌표와 미확인 필드를 확인합니다. 일반 조회는 웹/LLM을 호출하지 않습니다.
3. `방문 준비 · 저장` 후 방문ID/자료버전/상태가 나오는지 확인합니다. 반복 준비는 같은 방문을 유지합니다.
4. 앱을 종료/재실행하고 같은 탭에서 동일 방문ID가 복원되는지 확인합니다. 일반 Android CI에서 기기 검사와 새 프로세스 복원 캡처 절차가 통과했습니다. 원본 artifact는 Cloud 다운로드 터널403으로 독립 열람하지 못했으므로 검증 기록의 경계를 따릅니다.

앱 지도 재생은2단계에 추가했고 도로내비·입차·연속 관측 연결은 다음 단계입니다. 실제 게이트/주차기둥/승강기호기·거리/무단차는 미확인으로 유지합니다. 기존 설정/알림 기록은 별도 저장소에 보존합니다.

## 빌드와 APK

- JDK **17**, Android SDK Platform **35**, Build Tools **35.0.0**. 실행 대상은 **API 34 이상**, ARM64 또는 x86_64입니다.
- Gradle **8.10.2**, AGP **8.8.0**, Kotlin **1.9.22**. Wrapper를 동봉합니다.
- 공식 SDK: `NaviHelper:3.0.5`, `Vehicle:2.3.2`. Maven: `https://nexus-playground.pleos.ai/repository/maven-releases/`.
- 이전 기능 기반 개발 버전은 **0.8.0 / versionCode 8**, 코드 **84acdd3**입니다. 현행0.9.0/9 APK는 [UX 증거](docs/evidence/android-product-ux/README.md)를 따릅니다. [새 APK/metadata](https://github.com/leemh-dev/dochagjari/actions/runs/43404749340/artifacts/14027490674) 세 파일의 실제 해시·manifest·서명을 독립 대조했습니다. [CI 식별](docs/evidence/stage6-parking-return/ci-apk-metadata.json)·[로컬 식별](docs/evidence/stage6-parking-return/local-apk-metadata.json)을 구분하고 이전 APK/사용자 데이터를 유지합니다.
- 1단계0.3.0/3은 [이전 artifact](https://github.com/leemh-dev/dochagjari/actions/runs/43344658563/artifacts/14001506355)로 보관합니다.
- 이전 **0.2.0 / versionCode 2**의 [개발 APK·기기 테스트 APK·unsigned release 보관 artifact](https://github.com/leemh-dev/dochagjari/actions/runs/43055801516/artifacts/13871881801)의 서명 종류와 SHA-256은 [검증 기록](docs/08-verification.md)에 정리합니다. debug는 개발 서명이고 제출용 서명 APK가 아닙니다.
- 이전0.2 보관 artifact는 `development-release-assets-and-metadata`, 현재 신규버전은 `development-apks-and-metadata`입니다. 압축을 풀면 세 APK와 `apk-metadata.json`, `SHA256SUMS`가 있습니다. GitHub 로그인 후 다운로드합니다. runner 개발 서명은 이전 Cloud 서명과 다르므로 **새 테스트 AVD**를 사용합니다. 기존 데이터가 있는 앱을 임의 삭제하지 않습니다. Release 게시 API의 인증 제한과 대체 경로는 [개발 일지](docs/10-development-log.md)에 기록했습니다.
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

### 외장 SSD의 전용 개발 환경

[M1 8GiB 실측 기록](docs/08-verification.md)과 [다음 실행 프롬프트](docs/09-next-development-prompt.md)를 참고합니다. 외장 APFS 볼륨의 전용 디렉터리 아래에 이 저장소와 `sdk/`, `avd/`, `gradle/`, `downloads/`, `tmp/`, `verification/`을 둡니다. 예를 들어 `/Volumes/<볼륨 이름>/DochagjariDev/dochagjari`가 저장소입니다. SDK Manager를 사용하는 경우 SDK 위치를 이 환경의 `sdk/`로 지정합니다. 로컬 절대 경로는 저장소에 저장하지 않습니다.

아래 명령은 저장소에서 실행합니다. 실행기는 외장 볼륨과 저장소·캐시 경로를 확인하고 기존 ARM64 JDK 17을 사용합니다. 에뮬레이터는 한 대만 실행하고 APK 빌드와 번갈아 사용합니다.

```bash
bash scripts/pleos/macos-ssd-env.sh emulator -accel-check
bash scripts/pleos/macos-ssd-env.sh emulator -list-avds
bash scripts/pleos/macos-ssd-env.sh adb devices -l
```

이 장비에서 만든 `DochagjariPleosArm64`의 비교 실행 명령입니다. `-no-audio`는 초기 CoreAudio 대기와 QEMU watchdog 종료를 재현 조사할 때 적용한 옵션입니다. 이 옵션을 사용한 실행에서는 음성 기능을 검증할 수 없습니다.

```bash
bash scripts/pleos/macos-ssd-env.sh caffeinate -i emulator \
  -avd DochagjariPleosArm64 -accel on -gpu host -no-snapshot -no-audio
```

### Android Studio로 설치하는 경우


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
./gradlew :app:assembleDebug
# transport=device, boot=1, package/activity found와 각각의 종료 코드를 확인합니다.
# 실패하면 아래 && 연결이 설치·실행을 중단합니다. stderr는 JSON에 기록합니다.
python3 scripts/pleos/adb_readiness.py --serial "$DOCHAGJARI_SERIAL" \
  --output /tmp/dochagjari-adb-readiness.json && \
adb -s "$DOCHAGJARI_SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk && \
adb -s "$DOCHAGJARI_SERIAL" shell am start -n dev.leemh.dochagjari/.MainActivity
adb -s "$DOCHAGJARI_SERIAL" forward tcp:8088 tcp:8088
adb -s "$DOCHAGJARI_SERIAL" shell wm size
adb -s "$DOCHAGJARI_SERIAL" shell wm density
adb -s "$DOCHAGJARI_SERIAL" logcat -v time Dochagjari:I DochagjariVehicle:I DochagjariPleosSeat:I '*:S'
```

최신9430b7d/수동43154848171에서는 정상 Allow 입력 후에도 승인창/unauthorized가 남았습니다([최신 원본](docs/evidence/pleos-approval-unconfirmed-latest/summary.json)). 이전 실제 설치/앱 표시 성공과 별도로 기록하며 새 정상 host 터치·ADB driver는 실행되지 못했습니다. 이후 부팅을 추가 요청하지 않았습니다. 새 진단 근거 또는 접근 가능한 정상 승인 기기부터 확인합니다.

### 맥북 자원·공간·원본 화면 재현 확인

아래는 맥북에서 실행할 읽기 전용 명령입니다. 맥북에서 관찰한 결과는 위 M1 실측 기록을 참고합니다. 공식 ARM64 이미지/AVD 프로필을 선택한 뒤 실제 메모리 압력과 이미지·AVD·빌드가 사용할 디스크 여유를 확인합니다.

```bash
uname -m
sysctl -n hw.memsize
memory_pressure
df -h "$HOME"
# ADB가 정상 승인/준비된 후 guest 공간과 현재 설치 위치를 확인합니다.
adb -s "$DOCHAGJARI_SERIAL" shell df -k /data /data/local/tmp
adb -s "$DOCHAGJARI_SERIAL" shell pm get-install-location
```

기대 결과는 host RAM/메모리 압력/여유 디스크와 guest `/data` 크기·사용량의 실제 수치입니다. 파일 크기나 boot 로그만으로 공간 충분/앱 설치 성공을 판단하지 않습니다. 설치 오류가 나면 원본 오류와 수치를 보관하고 기존 기기 데이터를 삭제하거나 Android 저장 임계값을 낮춰 해결하지 않습니다. 이번 Linux CI의8GiB는 **새 테스트 데이터 파일 크기**이며 RAM 요구량이 아닙니다.

앱이 실제 표시된 후 다른 터미널에서 최초 화면과 주요 기능 전환마다 원본 PNG를 저장합니다. 승인창/검정 화면이면 앱 실행 완료로 기록하지 않습니다.

```bash
adb -s "$DOCHAGJARI_SERIAL" exec-out screencap -p > /tmp/dochagjari-first-launch.png
# 이후 기능명·실행 시각을 구분한 파일명으로 같은 명령을 반복합니다.
```

### ADB 준비 실패를 구분하는 방법

준비 helper는 지정한 시리얼의 transport, boot, package, activity 결과를 JSON으로 남기며 성공할 때만 종료 코드 0입니다. 기존 `readiness.json`이나 emulator의 Boot completed 로그만으로 설치하지 않습니다.

| 관찰한 상태 | 다음 확인 |
| --- | --- |
| device not found | `adb devices -l`과 emulator 실행 포트·시리얼을 대조합니다. |
| offline | 해당 AVD의 ADB 연결·부팅 로그를 확인합니다. 다른 기기의 서버나 데이터를 초기화하지 않습니다. |
| unauthorized | AVD 화면의 정상 디버깅 승인 절차를 확인합니다. 키 파일 편집이나 인증 우회로 해결하지 않습니다. |
| transport=device, boot가 1이 아님 | 부팅 진행·오류를 수집하고 설치를 보류합니다. |
| boot=1, package/activity 미준비 | 서비스별 stdout/stderr·종료 코드와 프레임워크 로그를 대조합니다. vendor crash 하나로 원인을 단정하지 않습니다. |

공식 Linux 이미지의 `devices.xml`은 RAM4GiB, `source.properties`는 8192MB를 지정합니다. [읽기 전용 비교](docs/evidence/pleos-profile-comparison.json)를 참고하고 사용한 실제 AVD 값을 기록합니다.

이전 KVM 진단들에서는 `emulator-5554 unauthorized`가 실제로 확인됐습니다. 이 상태에서는 부팅 속성·서비스를 조회하지 못하므로 프레임워크 미준비로 단정하지 않습니다. 맥북에서는 AVD 창을 열고 USB 디버깅 승인 화면의 호스트 지문을 확인한 뒤 정상 **허용**을 선택합니다. 아래는 이 Mac에서 비교할 공개 지문만 출력하는 명령이며 개인키를 읽거나 바꾸지 않습니다. 이번 Cloud 작업에서 맥북 명령을 실행한 것은 아닙니다.

```bash
adb start-server
python3 - <<'PY'
import base64, hashlib, os
from pathlib import Path
root = Path(os.environ.get("ANDROID_USER_HOME", str(Path.home() / ".android")))
public = (root / "adbkey.pub").read_text().split()[0]
value = hashlib.md5(base64.b64decode(public), usedforsecurity=False).hexdigest().upper()
print(":".join(value[i:i+2] for i in range(0, len(value), 2)))
PY
python3 scripts/pleos/adb_readiness.py --serial "$DOCHAGJARI_SERIAL" \
  --output /tmp/dochagjari-adb-readiness.json
```

GUI CI는 이 job이 만든 새 AVD에서만 정상 USB 디버깅 문구·일치하는 공개 호스트 지문·취소 옆의 명확한 허용 버튼을 확인해 클릭합니다. 지문이 다르거나 UI가 모호하면 클릭하지 않으며, 실제 transport/device와 부팅/서비스가 확인돼야 다음 단계로 갑니다. 키 파일 편집·복사, 인증 비활성화, 권한 우회는 하지 않습니다. 호스트 OCR은 이 승인 화면 관찰에만 사용하며 앱 ML Kit 분석 성공으로 계산하지 않습니다.

최신a8c91d2/수동43148832528에서는 정상 승인 후 실제 ADB device·boot1·package/activity·각rc0·설치 직전 재확인과 **APK 설치가 성공**했습니다([실제 근거](docs/evidence/pleos-installed-ui-blocked/summary.json)). 새8GiB userdata의 guest 여유약5.3GiB/NORMAL을 읽었고 저장 정책/기존 기기/DB는 변경하지 않았습니다. 8d12198의 실제 display/activity 관찰과 원본에서 앱 표시까지 확인했습니다. display0/물리1개·user10·multi-window이며 다른 display 추정은 지지되지 않습니다. 기존 ADB 탭 전환과 SDK·핀/Console는 아직 미검증이며, 현재user·정상 창 터치·새 XML/전환 확인을 보완했습니다. Gradle connected 검사는 test APK split install commit 응답 없음으로 tests0/실패이며 기능6건 실패가 아닙니다. display/activity/원본 관찰과 정상 ADB instrumentation 경로를 따로 준비했습니다. 과거 WINDOW0/OCR false/검정 화면은 승인 성공을 뜻하지 않습니다. 새 근거 없이 부팅을 반복하지 말고 [최신 검증](docs/08-verification.md)을 먼저 확인합니다. Mac 명령은 실제 접근했을 때만 결과로 기록합니다.

[일반 Android에서 직접 실행한 원본13장](docs/evidence/android-ui-screens/README.md)은 앱 테스트 입력/생성 영상의 실제 실행 증거입니다. Pleos 실제 연동 검증과 구분합니다. 실제 XML에서 확인한 Compose 스위치는 클래스명이 아닌 라벨/행/checkable·clickable로 선택하도록 수정했습니다.

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

Cloud 자체의 무가속 AVD에서는 서비스 준비를 확인하지 못했습니다. 이후 GitHub KVM에서는 정상 승인/서비스 준비와 새8GiB 구성의 APK 설치를 실제 확인했습니다. UI dump·계기 영역/앱 표시와 test APK 설치 단계가 후속 차단이며 실제 SDK/핀/Virtual Console 완료는 아닙니다. Pleos workflow는 새 목적/근거가 있을 때만 수동 실행합니다. 상세 원본·취소/실패·후속 결과는 [최신 검증](docs/08-verification.md)과 [개발 일지](docs/10-development-log.md)에 기록합니다. 맥북은 실제 접근하지 않았으며 위 공식 ARM64 AVD 절차는 재현용입니다.

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

### 실제 Android 모의 서버 복구 검사

[원본 화면3장/실행 근거](docs/evidence/android-http-recovery/README.md)에서 실제 Android 앱의 서버 모의 HTTP 복구를 확인할 수 있습니다. 서버 저장 뒤 응답 유실·앱 종료·동일DB 서버 재시작에도 같은 세션 GET으로 복구했고 POST1회/공급자0회였습니다. 실제 SMS 수락/휴대폰 수신은 아닙니다.

CI가 만든 새 전용 AVD와 모의 fixture에서만 `DOCHAGJARI_FRESH_TEST_AVD=1 python scripts/android_server_smoke.py`를 실행합니다. 실제 사용자 기기에서 이 명령을 실행하지 않습니다. 임시 토큰/DB는 artifact 밖에 보관하며 외부 공급자는 호출하지 않습니다.

에뮬레이터 없이 호스트 X11 입력만 확인하려면 Xvfb/xdotool/xauth/x11-utils가 준비된 Linux에서 `python scripts/pleos/check_pointer_runtime.py --output /tmp/dochagjari-pointer-check.json`을 실행합니다. 별도 private Xauthority를 쓰고 Android/ADB를 실행하지 않습니다. 호스트 성공이 Pleos 승인 성공을 뜻하지 않습니다.

호스트 native 관찰을 독립 확인하려면 같은 Linux 도구 환경에서 `python scripts/pleos/check_window_regions.py --output /tmp/dochagjari-native-window-check.json`을 실행합니다. 별도 private Xvfb의 자체 합성 창만 사용해 정상 포커스·표시/입력 영역·Unmap을 구분합니다. 실제 에뮬레이터의 입력 영역/인증/키를 변경하지 않으며 Pleos 성공 증거로 대신하지 않습니다.

### Gradle 설치 계층과 구분하는 표준 ADB 기기 검사

GitHub 수동 Pleos workflow에서 `device_test_driver=adb`를 선택하면 정상 ADB로 단일 test APK를 설치하고 기존6개 검사를 실행합니다. `gradle`은 기존 connected task, `none` 또는 `run_connected_tests=false`(gradle 모드)는 관찰용입니다. driver 결과는 서로 구분합니다. 정상 ADB driver는 준비 단계이며 실제 최신 결과는 docs/08을 확인합니다.

다음은 **새 전용 AVD에서만** 실행할 재현 명령이며 기존 사용자 기기에는 사용하지 않습니다. 앞의 명령으로 동일 로컬 개발 키의 debug APK를 설치한 뒤 test APK를 빌드합니다. 두 명시적 guard가 없으면 데이터 초기화/검사를 시작하지 않습니다.

```bash
./gradlew :app:assembleDebugAndroidTest
DOCHAGJARI_FRESH_TEST_AVD=1 python3 scripts/pleos/direct_instrumentation.py \
  --serial "$DOCHAGJARI_SERIAL" --reset-test-data \
  --output /tmp/dochagjari-direct-instrumentation
# 기능 검사 결과와 별도로 프로필/알림 저널의 두 프로세스 복원을 확인합니다.
python3 scripts/pleos/adb_readiness.py --serial "$DOCHAGJARI_SERIAL" \
  --output /tmp/dochagjari-restart-readiness.json && \
python3 scripts/process_restart_smoke.py --serial "$DOCHAGJARI_SERIAL" \
  --reset-test-data --output /tmp/dochagjari-direct-restart.txt
```

완료된 test status0 6개·OK(6 tests)·최종 instrumentation code-1·오류 없음이 모두 있어야 기능 검사 통과입니다. 별도 복원 검사는 PID 변경·같은 확정 요청 지문·저장 완료 프로필을 확인합니다. 앱 차량/내비/알림은 테스트 입력/모의이며 사전 영상 분석을 사용합니다. 실제 Pleos SDK/실차/SENS 수락이나 `connectedDebugAndroidTest` 통과로 대체하지 않습니다.
