# 다음 작업 — Android UX 검증 후 실제 Pleos/최종 시연

[Android UX 규격](25-android-navigation-ux.md) · [현행 결과/원본 화면·APK식별](evidence/android-product-ux/README.md). 아래 결과는 최신 GitHub/증거와 먼저 대조한다.

```text
도착자리의 지도 중심 Android UX 이후 시연 준비를 이어가줘.
AGENTS.md·README·docs/07~25·PR #1·최신 원격/CI/artifact를 먼저 확인해.
열린 feat/android-submission-mvp에서 이어가고 기존 변경/사용자 데이터를 덮어쓰거나 자동 병합하지 마.
앱0.9.0/9·e2b93ba·CI43420964354 전체성공의 원본 화면·검증 수준·APK해시/개발서명은
 docs/evidence/android-product-ux/README.md와 metadata를 대조해. 통과 검사를 이유 없이 반복하지 마.
Mac M1/8GB·외장SSD 구성을 유지하고 메모리 압력/여유공간·접근 가능한 Pleos부터 확인해.
이전에는 Mac이 꺼져 있었고 Cloud ADB0/KVM없음이었다. 새 근거 없는 부팅/다운로드나 인증 우회는 하지 마.
키 복사/편집·Always allow 선택 없이 transport=device·boot1·package/activity·각rc0을 확인하고
설치 직전 재확인·개발인증서 대조 뒤 새 전용 AVD에 설치해. 기존 데이터를 지우지 마.
가능하면 제품 설정에서 Pleos SDK를 선택해 에뜨와 판교점 시설 목적지의 요청/콜백·순정 핀·정상 앱 전환을 각각 확인해.
NaviHelper 요청을 지도 embedding/내비 내부 카드 지원으로 간주하거나 경로 계산을 입차로 처리하지 마.
입차→5층 매장행 파란선→주차 저장/닫기→귀환을 같은 APK/방문으로 연결한 연속 시연영상을 준비해.
실제 연결이 막히면 MOCK/TEST를 표시해 진행하고 일반 Android 성공을 Pleos/실차 성공으로 대체하지 마.
미확인 게이트/기둥·승강연결/거리·현재 위치/복귀선을 만들지 마.
방문/자료 snapshot·설정/좌석·확정 알림·저장 ACK/복구·주행 제한·닫기/자동재개 없음은 보존해.
현재 전체 단계·차단/다음을 약1분마다 공유하고 새 앱 화면을 확보 즉시 채팅 이미지로 보여줘.
기능별 커밋·푸시·PR·docs/08·10·12·18/증거를 갱신하고 APK 변경 시 버전·코드·해시·서명을 실제 파일로 대조해.
목표는 기능검증/시연이며 앱마켓 출시/출시 서명은 범위 밖이야. 실제 SMS와 모의 발송을 구분하고 비밀값은 공개하지 마.
마지막에 PR·APK·화면/검증 링크·통과/미검증·다음을 알려주고 다음 프롬프트도 파일과 채팅에 직접 보여줘.
```
