# Android 지도 중심 UX 적용

2026-11-13 UTC. [설계24](24-navigation-ux-design.md)의 기본 A를 Android Compose에 적용한다. ①DB~⑥주차/귀환의 저장 계약 위에 UX 검증을 먼저 수행하고 ⑦최종 시연은 이후 진행한다. 앱0.9.0/9. 호스트 HTML이 아니라 `MainActivity → ProductApp`가 기본 실행 화면이다.

## 변경

- 밝은 중립색·검정 주요 버튼·파란 경로, 넓은 앱 영역의 공간 시각화/짧은 행동 카드. 좁은 영역은 세로 배치. `safeDrawingPadding`으로 앱 가용 영역의 시스템 Insets를 존중한다. 실제 Pleos DPI/터치 적합성은 아직 별도 확인 대상이다.
- 에뜨와 목적지/방문 준비 → 기존 방문 저장 → 내비 요청 → 명시적 입차 → 큰 기울인 5층 지도 → 주차 저장 확인 → 요약/귀환으로 이어진다. DB/방문/설정/알림 로직을 재작성하거나 기존 데이터를 초기화하지 않는다.
- TEST 차량 모드의 입차/주차 완료 버튼에 TEST를 표시한다. SDK 선택과 TEST 입차를 혼동하지 않는다. 실제 위치나 차량 게이트를 만들어 넣지 않는다.
- 5층의 기존14개 경로 점/개략도 블록을 재사용한다. 6초 파란선 재생·일시정지/명시적 재생, 모션 줄이기에서는 전체 선을 정적으로 표시한다. 선은 매장행 미리보기이며 측위/복귀선이 아니다.
- 주행/unknown에는 상세 지도·설정·단서를 숨긴다. 다시 정차해도 자동으로 지도/요약/관측/내비를 재개하지 않는다. 닫기/백그라운드 시 기존 모델의 중지/숨김 처리를 사용한다.
- 주차 요약/귀환은 실제 저장 ACK 뒤만 열린다. 직접 단서 보완은 빈 USER 입력으로 명시적으로 시작하며 기존 저장값은 완료 전 유지한다. 화면 변경·저장 중·저장 확인 실패를 구분한다. TEST 단서를 자동 USER/위치로 승격하지 않는다.
- 선택적 좌석 역할/건너뛰기와 내비 방식은 제품 설정에서 접근한다. 저장 상태는 기존 DataStore writer의 상태를 표시한다. 긴 UUID/SQLite 진단·센서/영상/HTTP fixture는 별도 개발자 도구에 보존한다. 기존 회귀 테스트는 명시적으로 이 화면에 진입한다.

## 순정 내비 경계

공식 [NaviHelper 목적지 문서](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/NaviHelper/destinations)의 `requestRoute(RouteInfo)`·`getDestinationInfo()`·`onDestinationInfo`를 재확인했다. [조회 시각/원문 해시/범위](evidence/android-product-ux/nav-boundary.json). 이 문서만으로 내비 지도 embedding/임의 overlay/자동 foreground를 지원한다고 주장하지 않는다. 앱의 도로 장면은 상태/연결 안내이며 순정 도로 지도를 복제하지 않는다.

실제 SDK 함수 반환·이벤트 수신·실제 핀·입차를 구분한다. 이 UX 변경은 SDK 계약을 확장하지 않는다. 실제 Pleos 목적지 핀은 준비된 Pleos에서 따로 확인해야 한다. Cloud 확인에서 ADB목록0/transport rc1 device not found/KVM없음이고 사용자는 Mac이 꺼져 있다고 답했다. [실제 기기 조회](evidence/android-product-ux/cloud-device-check.json). 새 무가속 부팅/다운로드/설치/인증 조작은 실행하지 않는다.

## 검증 계획과 현재 기록

- JVM/lint/debug/test/unsigned release 및 기존 기능 회귀.
- 새 제품 화면의 지도/닫기·주행 제한·정차 후 자동재개 없음·Activity 재생성과 실제 SQLite 방문 재조회, 기존 profile/outbox 유지.
- 새 전용 일반 Android CI AVD에서 목적지/모의 내비/TEST입차·주차/기울인 실제 Compose 지도/동적·정적 경로/수동 단서/새 PID 복원/주행 화면을 원본 PNG와 XML로 보관한다. 해당 job이 만든 AVD에서만 초기 테스트 fixture를 만든다. 실제 사용자 기기 데이터나 알림 서버 DB는 지우지 않는다.
- CI artifact는 job 종료 후 확보 가능하므로 실행 중 이미지인 척하지 않는다. 최종 검사/이미지·APK해시·서명은 [증거 디렉터리](evidence/android-product-ux/)에 실제 결과를 반영한다.
- 실제 Pleos 순정 내비/Console·외부 카메라·현장 주차장 연결/측위·실차·실제 SMS는 이 검사로 대체하지 않는다. 앱마켓 출시/출시 서명은 범위 밖이다. 자동 병합 없음.
