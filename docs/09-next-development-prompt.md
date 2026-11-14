# 다음 실행 — Mac Pleos 확인과 시연 마감

2026-11-14 UTC. Cloud 구현/일반Android 검사와 실제 앱 시연 영상은 완료했다. [현재 구현](37-current-implementation.md) · [정확한 APK/SSD 인계](38-mac-pleos-v0.11-handoff.md). 아래 프롬프트를 Mac Codex에서 실행한다.

```text
도착자리 0.11.0의 Mac Pleos 확인과 시연 점검을 진행해줘.
AGENTS.md·README·docs/07·08·09·10·12·18·33~38, PR #2와 최신 원격 상태부터 확인해.
feat/arrival-context-ux에서 이어가고 기존 변경·PR #1·DB·방문 ID·설정·알림 복구를 보존해. 자동 병합하지 마.
앱은 0.11.0/11·f0640b0이며 CI43569033733/43569041435가 전체 성공했어.
JVM115·서버14·진단35·기기15건과 별도 복원4단계는 이미 통과했으니 근거 없이 반복하지 마.
전달 APK는 CI43569033733의 tested-device-apks-and-metadata/artifact14104088875야.
메타데이터의 버전·SHA256·개발 인증서를 실제 app/test 파일과 대조하고 다른 runner의 APK를 섞지 마.
실제 화면·90초 시연·20초 모션은 docs/evidence/demo-video/README.md와 docs38을 참고해.

M1/8GB의 기존 외장SSD 체크아웃·SDK·AVD·캐시를 사용하고 AVD 한 대만 실행해.
공간·메모리·연결 기기를 먼저 확인하고 Cloud APK 설치를 우선해. 기존 기기 데이터는 지우지 마.
설치 직전 transport=device·boot1·package/activity 각 rc0을 확인해.
키 복사/편집·인증 우회·Always allow 선택은 하지 마.

탑승 모드/이번만 변경·재시작 재확인과 입차→영상/주차·5층 지도→주차 저장/닫기/귀환을 확인해.
Pleos 실제 분할 배치·가독성·터치·메모리·미디어 종료를 확인하고 주요 실제 화면을 즉시 채팅에 보여줘.
실제 NaviHelper 요청/콜백·순정 핀, Console 뒤좌우 Empty→Occupied→Empty·앱 SDK 수신을 각각 기록해.
TEST 입력을 실제 SDK/Console/실차 성공으로 바꾸지 말고 미확인 위치·거리·승강기·정차 허용을 만들지 마.
실제 STT/Gleo·외부카메라·SMS·현장 동선은 미검증, AI 자료 보완 worker는 미구현이야. 모의·가상 표시를 유지해.
목표는 검증과 시연이야. 앱마켓 출시/출시서명은 제외하고 debug/test 개발서명·release unsigned를 유지해.
현재 단계·Cloud/Mac 상태·차단/다음을 공유하고 재현 오류부터 수정해.
비밀값·전화번호·키를 공개하지 말고 docs/08·10·12·18·38·증거·PR에 결과를 기록해.
기능별 커밋·푸시하고 결과 링크와 다음 작업을 알려줘. 다음 프롬프트도 파일과 채팅에 직접 보여줘.
```
