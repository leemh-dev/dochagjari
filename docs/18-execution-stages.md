# 실제 개발 단계와 진행 기록

> 2026-11-14 영상 범위 정정: 기존90초/20초는 앱 단독 기능기록이다. 요청한 **실제 Pleos3분할 대표시연은 미완료**다. [3분할 UX합성 예상도](evidence/demo-video/three-panel-concept/README.md)를 별도 제공하며 실제 통합 성공으로 세지 않는다. [실제 완료 기준39](39-three-panel-demo-gap.md).

## 2026-11-14 · 0.11.0 최종 Cloud 검증 완료

**앱 0.11.0/11·f0640b0**, [push CI43569033733](https://github.com/leemh-dev/dochagjari/actions/runs/43569033733)와 [PR CI43569041435](https://github.com/leemh-dev/dochagjari/actions/runs/43569041435) 전체 성공. JVM115·서버14·진단35·lint/3빌드, 실제 connected15pass/0fail/4전용skip 후 별도 복원4단계, 원본54장·동일DB HTTP 모의 복구를 확인했다. **90초 편집 시연과20초 연속 모션**도 출력/전체 디코딩/해시 대조를 마쳤다.

[실제 화면·검사 원본](evidence/arrival-panel/f064-device/README.md) · [실제로 검사한 APK 쌍](https://github.com/leemh-dev/dochagjari/actions/runs/43569033733/artifacts/14104088875) · [영상](evidence/demo-video/README.md) · [현재 구현](37-current-implementation.md) · [Mac 인계](38-mac-pleos-v0.11-handoff.md).

일반 Android의 TEST/MOCK/생성 입력 결과다. Mac/Pleos 새버전 배치·SDK 핀/Console·실제 STT/Gleo·외부카메라·실차/현장 동선·실제SMS는 미검증이고, 이동 중 AI 자료 보완 worker는 미구현이다. 병원/학원은 가상 시연, 역/공항은 개념 안내다. debug/test 개발서명·release unsigned이며 앱마켓 출시/출시서명은 범위 밖이다. 기존 PR#1·데이터는 보존하고 자동 병합하지 않았다.


[현행 Cloud/Mac 단계표36](36-cloud-mac-development-workflow.md): 새개발1저장/이행→2독립탑승UI→3입력/인지→4지도패널→5방문흐름→6회귀/시연을각각Cloud/Mac상태로나눈다. 아래기존단계실적은과거검증이며새단계완료로승격하지않는다.

[새 개발 브랜치/실행 순서34](34-implementation-restart.md): `feat/arrival-context-ux`에서 다중 탑승 프리셋/이번 구성 저장·이행부터 별도 단위로 개발한다. 기존1~6단계 결과/시안/PR#1은 보존하며 새 방향 구현 완료로 세지 않는다.

[7단계 시연영상 제작 계획](32-demo-video-production-plan.md)을 추가했다. 서비스 본편210초/실제 앱 실행기록 약90초를 제안하며, 첫 제작 작업은15~20초 모션샘플이다. 콘티 수립만 수행했고 새 영상/앱/기기 검증 완료로 세지 않는다.

2026-11-13 UTC · [상세 앱 계획](17-in-app-visit-guidance-plan.md). 첫 시나리오는 양재 사옥→판교점5F 에뜨와→주차 위치/귀환. 단계별 실제 완료 증거를 구분한다.

**사용자 UX 검토 반영:** 1~6의 기능 검증과 제품 UX 완성을 구분한다. 7단계 최종 영상 전에 [내비 연결 UX 설계/Android 적용](24-navigation-ux-design.md)을 선행한다. 호스트 시안에 이어 [Android0.9.0/9의 제품 UI](25-android-navigation-ux.md)를 적용했다. 일반 Android 기기 검증과 실제 Pleos 연동은 [현행 증거](evidence/android-product-ux/README.md)로 구분한다.

| 단계 | 작업 | 현재 상태/완료 기준 |
| --- | --- | --- |
| 1 | 사전 시설DB·매장 검색·방문 상태 저장/복원 | **완료 · 로컬/일반 Android CI 통과.** 실제 SQLite 재조회·Activity 재시작과 강제종료 후 새PID/같은방문 복원 절차 성공. SQLite 버전별 자료·별칭 검색·같은 방문ID/닫힘/주차단서 복원, 실패·동시요청·새자료 보호 |
| 2 | 앱 지도/파란선·2.5D 애니메이션 | **완료 · 일반 Android CI 통과.** 네이티브 개략도/재생·정지·평면/기울기·닫기/동일방문 복원·주행/미확인 차단. [실제 화면/증거](evidence/stage2-native-guide/README.md) |
| 3 | 선택적 좌석별 탑승 설정 | **완료 · 일반 Android CI 통과.** [역할/카시트·건너뛰기](20-optional-seat-roles.md), 기존 파일 이행/저장 실패·재시도·연속변경, 실제 저장값·새PID 복원/기존 방문·알림 보존/주행 차단 확인 |
| 4 | 목적지·내비·명시적 입차 | **완료 · 일반 Android/모의·명시TEST 검증.** [시설목적지/입차 계약](21-road-navigation-and-entry.md), 실제기기12pass·같은방문/요청복원·자동전달/입차없음. 실제SDK/핀·자동입차는미검증 |
| 5 | 영상/카메라 관측·위치 후보·경로 갱신 | **완료 · 일반 Android/TEST·생성영상 검증.** JVM82·기기12pass/별도복원2단계·UI37장. 출처/단서/충돌·중지/재개없음. 연결자료없어주차층/현재위치·승강연결미확인·기존5층선유지,실제외부카메라미연동 |
| 6 | 주차 완료·요약·닫기/재개·귀환/복구 | **완료 · 일반 Android/MOCK·TEST 검증.** 명시완료·수동단서/출처·재조회ACK/8초요약·귀환기록/이동재개·새PID. JVM99/기기12pass·별도복원2단계·집중UI14장. 실제측위/승강연결·복귀선미확인 |
| UX | 지도 중심 기본 화면/설정·기울인5F/주차·귀환 | **일반Android 완료 · 0.9.0/9·e2b93ba/CI43420964354 전체성공.** 기기13pass/별도복원2단계·제품15장/기존14장. 실제Pleos 내비/핀/적합성 미검증 |
| 7 | 전체 회귀·기기 검사·APK 식별·시연 녹화 | 단계별 수행 후 전체 검증 대기. 같은APK 연속 영상/실제·모의 입력 표시 |

## 2단계와 이동 중 자료 보완

네이티브 지도/파란선·재생/닫기와 사용자 추가 방향인 “이동 중 필요한 근거를 충분히 보완”하는 설계를 [docs/19](19-native-guide-and-enrichment.md)에 반영했다. 저장 DB를 먼저 사용하고 누락/오래된 자료는 백그라운드에서 조사·캐시하는 방향이며, 이번 단계에서 실제 AI 조회 worker를 구현한 것은 아니다. 앱 지도 구현/검사 결과와 기존 웹·호스트·기기 성공을 각각 구분한다.

## 1단계 구현

- `visit-catalog.json`: 판교점/에뜨와/5F·공식 출처/확인일·14개 보행 좌표·별칭. 차량게이트/주차기둥/엘리베이터호기·거리/무단차·도면재사용범위는 미확인으로 유지한다. 지도 이미지는 앱에 새로 동봉하지 않았다.
- `SqliteGuidanceStorage`: 앱 내 SQLite로 버전별 자료/별칭과 방문기록 저장. 같은 자료 버전의 내용 변경은 거부하고 새 버전에도 이전 자료/방문을 보존한다. 기존 설정/알림DB와 분리한다.
- `VisitGuidanceRepository`: 앱 생명주기에 속한 순차 IO, 저장 트랜잭션 성공 후만 저장 방문을 알린다. 실패/재시도·같은 방문 재사용·숨김 상태 유지.
- 앱의 `매장 방문` 탭에서 기본문구 “에뜨와 판교 현백” 조회→실제 에뜨와5F/근거/미확인 확인→방문 준비 저장·동일ID 복원. 정차 미확인/주행중에는 조회/준비를 제한한다. 지도/내비/영상 연결은 아직 수행하지 않는다.
- 로컬 JVM41(신규8)/lint/debug·test·unsigned release 성공,2m3s. Python구문/actionlint/diff검사 통과. [로컬 근거](evidence/stage1-visit-db/local-checks.json).
- 기기검사1건과 실제 화면14~16(검색/저장/프로세스 복원)을 CI에 추가했다. 수정 후 [CI43344658563](https://github.com/leemh-dev/dochagjari/actions/runs/43344658563)의 verify/android-device 모두 성공했다. 현재 Cloud의 artifact 저장소 다운로드는 터널403으로 차단돼 원본화면 독립 열람은 못 했고, [기기 artifact](https://github.com/leemh-dev/dochagjari/actions/runs/43344658563/artifacts/14001348823)로 제공한다.
- 새 APK0.3.0/3은 기존 배포0.2.0/2·코드3a01515·artifact13871881801을 덮어쓰지 않는다. 신규 artifact 이름은 `development-apks-and-metadata`이며 manifest/서명/해시는 현재 빌드에서 추출한다. 예전 고정0.2 패키징 workflow는 보관 목적으로 유지한다. 실제 코드/해시는 후속 metadata·PR 기록을 따른다.

다음은 완료한1단계에 이어2단계 앱 지도 렌더러/애니메이션이다. 좌표의 원시 단위를m로 표시하거나 경로 재생점을 실제 위치로 표현하지 않는다.

## 2단계 완료 · 다음3단계

앱0.4.0/4,기능cad08a9·기기일시정지수정e573e8e,CI43358680681전체성공. 실제기기9통과/프로세스전용2skip후별도2단계성공,21장UI/HTTP모의복구·실제APK대조. 다음은선택적좌석별탑승설정. 실제AI조회worker/내비입차/연속카메라/주차귀환은미구현·별도검증이며[docs/19](19-native-guide-and-enrichment.md)방향을유지한다.

## 3단계 완료 · 다음4단계

앱0.5.0/5,기능c98630d·저장완료제안수정a8e440b,최신CI43366222134 verify/android-device모두success. JVM52/기기11pass·프로세스전용2skip후별도2단계PASS,25장UI/역할새4장·HTTP모의복구/실제APK대조성공. [최신근거](evidence/stage3-seat-roles/README.md). 다음은에뜨와목적지→내비→명시적입차→동일방문안내연결이며차량진입좌표/순정SDK·실제핀·음성은조사/검증을구분한다.

## 4단계 완료 · 다음5단계

0.6.0/6·f959cb9/CI43371857767 verify/android-device성공. JVM62·기기12pass/프로세스전용2skip후별도2단계PASS,UI29장/새4장·HTTP모의복구·CI APK독립대조. [최신원본/수준](evidence/stage4-road-entry/README.md). 시설대표좌표/모의내비/명시TEST입차이며실제Pleos SDK/핀·차량자동입차/실차성공이아니다. 다음은같은방문의관측/추천갱신이다.

## 5단계 완료 · 다음6단계

0.7.0/7·기능ef3f306·실제기기URI회귀수정8ea046a/CI43390462215전체성공. 원본JVM82·서버14/진단35·기기12pass/2skip후별도프로세스2단계PASS·UI37장/신규8장·HTTP모의복구/새APK독립대조. [증거](evidence/stage5-visit-observation/README.md). 첫CI실패1건은보존했다. 닫기/주행/권한오류/종료후자동재개없음,문자후보와현재위치를구분하며연결미확인으로경로선을임의변경하지않는다. 실제Pleos카메라/측위·OS철회·실차성공이아니다. 다음은명시주차완료·요약/닫기·귀환이다.

## 6단계 완료 · 다음7단계

0.8.0/8·84acdd3/CI43404749340전체성공. 원본JVM99/서버14/진단35·기기12pass/2skip후별도종료복원2단계·집중UI14장·HTTP모의복구/배포APK독립대조. [증거](evidence/stage6-parking-return/README.md). 기존37장을재실행했다고세지않는다. 완료/수동확인·관측후보/현재위치를구분하고쓰거나확인하지못한결과를완료로표시하지않는다. 닫기/종료/이동후기록유지·자동재개없음,실제카메라/측위·복귀선미확인. 다음은같은APK의전체시연영상/제출증거다.


## UX 적용 · 7단계 직전

기능48b2ba5·내비방식표시보완e2b93ba·앱0.9.0/9. 실제 Compose의 목적지/짧은 카드·기울인5층 파란선·주차 요약/귀환·선택 설정을 기본으로 한다. 검증용 긴 화면은 개발자 도구다. 기존 방문ID/자료·DataStore·알림 계약을 유지한다. [새 검사/화면/APK](evidence/android-product-ux/README.md). 다음은 UX 실기 결과와 같은 APK의 최종 연속시연이며, 실제 Pleos 순정내비/Console·현장 연결은 별도 준비 필요다. Cloud 기기0/KVM없음·Mac꺼짐이므로 실제 Pleos를 실행했다고 세지 않는다.
