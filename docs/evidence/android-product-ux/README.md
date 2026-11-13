# 지도 중심 Android UX · 원본 증거

2026-11-13 UTC. 앱 **0.9.0/9**, 기능48b2ba5·내비방식 표시 보완 **e2b93baa91ce5bc56fb770fdc65dad08b9992175**. [설계24](../../24-navigation-ux-design.md) → [Android 적용25](../../25-android-navigation-ux.md). 호스트HTML/일반Android/Pleos·TEST/실제를 구분한다.

## 최종 빌드/원본 대조

[CI43420964354](https://github.com/leemh-dev/dochagjari/actions/runs/43420964354) verify131870545211 **success**. 원본 HTML JVM99/failure0/ignored0, 서버14/진단35·lint/debug/test/unsigned release. [검사 요약](ci-verify.json) · [원본 보고서 artifact14034616082](https://github.com/leemh-dev/dochagjari/actions/runs/43420964354/artifacts/14034616082). 이전0.8/CI43404749340 결과를 이번 실행으로 재사용한 것이 아니다.

[새 세 APK artifact14033304488](https://github.com/leemh-dev/dochagjari/actions/runs/43420964354/artifacts/14033304488) · [manifest/해시/인증서](ci-apk-metadata.json) · [SHA256SUMS](ci-SHA256SUMS) · [독립 대조](ci-apk-comparison.json). 원본 ZIP digest와 실제 APK bytes/SHA256/aapt manifest/apksigner를 Cloud에서 다시 대조했다. push CI checkout이 e2b93ba 자체이므로 virtual PR merge SHA를 코드로 혼동하지 않는다. 기기 job은 같은 소스의 별도 runner 재빌드이며 배포 APK byte/개발인증서와 동일하다고 가정하지 않는다.

- debug/test: Android 개발서명. 기존 Cloud/Mac 앱과 인증서가 다를 수 있어 새 전용 AVD에서만 확인한다. 기존 사용자 데이터를 삭제하지 않는다.
- release: unsigned, 서명된 출시/제출 완료본이 아니다. 목표는 기능검증·시연이며 앱마켓 출시/출시 서명은 범위 밖이다.
- 초기48b2ba5 빌드/캡처와 최종e2b93ba 결과를 구분하며 최종 배포 기준은 위 artifact다. 저장소의 옛 apk/ 보관본과 혼동하지 않는다.

## Android 기기/UI

[최종 기기 artifact14034214983](https://github.com/leemh-dev/dochagjari/actions/runs/43420964354/artifacts/14034214983) · job131873465768 **success**. [원본 XML](connected-results.xml) 총15건 중 **13pass/실패0/error0/프로세스전용2skip**, [별도 준비/복원](process-restart.txt) 2단계OK. [기능별 대조](device-verification.json). 새 ProductInstrumentedTest는 기본 제품 설정/지도·닫기·주행 숨김/설정 차단·정차/Activity 재생성 후 자동 지도 없음·SQLite 방문ID/자료/입차 유지·기존 profile/outbox 보존을 확인했다. 기존 픽업/병원/영상/좌석/입차·주차/귀환 검사를 계속 통과했다.

[연결 준비](pre-connected-readiness.json)·[설치 직전 준비](pre-install-readiness.json)의 transport=device/boot1/package·activity found/각rc0·빈stderr를 원본으로 확인했다. 새 CI 전용 AVD에서만 fixture 초기화를 수행했다.

[제품 UI15장/추적](product-ui.json), [기존 주차 UI14장](parking-regression.json), [실제 Android HTTP SERVER_MOCK](http-recovery.json) 모두success. 제품UI 프로세스 **10324→11631**에서 수동 합성메모manual-demo를 복원하고 자동지도/요약 없이 기본 화면으로 돌아왔다. 별도 process 검사와 기존 주차회귀는 실제 저장 ID·자료/프로필/확정 알림까지 대조한다. HTTP는 같은DB 서버재시작/응답보류/빈토큰→동일세션GET복구, POST1/공급자0이며 실제SENS/휴대폰수신이 아니다.

6초 구현의 실제 Compose 재생 시작/완료 PNG와 모션 줄이기 전체 선을 확인했다. [원본 픽셀 비교](route-render-check.json)는 경로가 나타나고 정적 전체선이 일치하는지 보조 확인이며 FPS/정확한6초 실측은 아니다. 시작PNG는 XMLdump 전에 촬영해 시작/완료 시점 차이를 보존했다. 요약 캡처는 현재 실행에서 자동닫기를 끈 상태이고 기본8초 닫기는 별도 기기 시나리오로 검증한다.

| 원본 앱 화면 | 확인 범위 |
| --- | --- |
| [목적지](01-product-destination.png) | 밝은 카드/방문 준비·TEST 차량/다음 모의 요청 |
| [도로 연결](03-mock-road-handoff.png) | 저장된모의요청/다음방식 분리·순정지도 아님 |
| [재생 시작](04-animated-floor-early.png) / [완료](05-animated-floor-complete.png) | 기울인5F 개략도·파란 매장행 선·측위 아님 |
| [모션 줄이기](07-reduced-motion-static.png) | 전체 경로 정적 표시 |
| [주차 저장](08-parking-saved-unknown.png) | ACK 뒤 요약·층/기둥/승강기 미확인 |
| [수동 메모 귀환](12-return-manual-clue.png) | USER 합성메모·출처/시각·복귀선 생성 없음 |
| [주행](15-driving-brief.png) | 상세 지도/설정 숨김·자동재개 없음 |

PNG와 동명의 XML도 함께 보관한다. 작은 대표8장 외 전체15장·기존14장/HTTP3장/원본로그는 위 기기artifact에 있다. 일반Android 시스템 작업표시줄/상태바는 시스템 영역이며 Pleos 화면이 아니다. [초기48b2ba5 별도 결과](initial-code-ci.json)와 최종e2b93ba 원본을 구분하고 채팅에도 각각 확보 직후 직접 표시했다.

## 실제 Pleos/현장 경계

[기기 조회](cloud-device-check.json): Cloud ADB 목록0/transport rc1(device not found), /dev/kvm 없음. 사용자는 Mac이 꺼져 있다고 확인했다. 새 Pleos 부팅/다운로드/설치/인증 수정은 수행하지 않았다. 실제 SDK/순정 목적지 핀·정상 앱 전환/Console·실차는 미검증이다.

[공식 문서 확인 범위](nav-boundary.json): NaviHelper 목적지 요청·결과 조회/콜백. 지도 embedding/내비 내부 카드·분할 지원은 이 근거로 확인되지 않았다. 정상 요청과 별도 앱 안내를 기본으로 한다. 실제 Pleos DPI/앱영역·터치/시스템 적합성도 별도 검증 대상이다.

TEST 입차/주차와 모의 내비·생성/공개 사전 영상·가상 병원·SERVER_MOCK를 실제 차량 신호/카메라·측위·공급자 수락/휴대폰 수신으로 승격하지 않는다. 주차층/기둥·승강기 연결·거리/현재위치·복귀선은 근거 없으면 미확인이다. AI자료 보완 worker도 아직 구현되지 않았다.

[로컬 검사](local-checks.json)와 CI 원본 결과를 각각 보관한다.
