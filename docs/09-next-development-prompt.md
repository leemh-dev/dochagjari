# 다음 개발 실행 프롬프트

2026-11-12 UTC · 최신 실행 결과와 원본 근거를 먼저 확인합니다.

```text
도착자리 개발을 이어서 진행해줘. AGENTS.md·README·docs/07~12와 PR #1·최신 CI부터 확인해.
PR이 열려 있으면 feat/android-submission-mvp에서 이어가고 자동 병합/기존 변경 덮어쓰기는 하지 마.
앱0.2.0/2·배포 코드3a01515·artifact13871881801·기존 해시/개발 서명을 유지해. 디자인 전면 개편은 아직 미뤄.

새 완료 증거
-2c2feb0 PR43134026080/push43134018393, dacfc37 PR43135254549/push43135246257 전체 성공.
-JVM33·서버14·lint·진단21·일반 Android 기능6·프로세스2단계·기존UI13장과 새 실제 HTTP 복구가 통과했어.
-원본3장/추적은 docs/evidence/android-http-recovery/에 있어. 실제 앱 SERVER_MOCK에서
 GET404→POST 저장/응답 보류→앱 종료·동일DB 서버 재시작→새 PID/빈 토큰→같은 세션 GET200 복구야.
 POST총1/DB행·지문 동일/공급자0이야. 실제 SENS 수락/휴대폰 수신이나 Pleos 성공이 아니야.

Pleos 우선 작업
1. 수동43135246535/artifact13907941147과 docs/evidence/pleos-window-target-blocked/부터 읽어.
 원본에는 승인창이 있고 창 목록에는 Emulator2400x1350/100,100이 있지만 pointer WINDOW0/focus=root543이야.
 대상 미확인으로 입력을 차단했고 unauthorized였어. transport 차단으로 boot/package/activity 미조회,
 설치·SDK/핀·Console·Pleos 기기 검사는 생략됐어. 이후 추가 부팅은 요청하지 않았어.
2. 이전43132924010 입력 명령은10초 timeout이었지만 자체 Xvfb에서 같은좌표 sync/분리 입력과
 screenshot 전후 실제 창 포인터는 정상이었어. sync/capture만으로 원인을 단정하지 마.
 접근 가능한 기기·맥북 원격 연결부터 확인하고, 새 X11/Qt 입력·포커스/창 상태 진단 목적과 변경을
 기록한 경우에만 실행해. 같은 실패를 근거 없이 재부팅하거나 RAM/userdata/profile crash로 단정하지 마.
3. 키 복사/편집·인증 비활성화·권한 우회·Always allow 선택은 하지 마. WINDOW0/OCR unknown은 승인이 아니야.
 transport=device·boot1·package/activity·각 rc0을 실제 확인하고 설치 직전에 다시 확인한 뒤에만 설치해.
 준비되면 내비 모의를 끄고 픽업37.48544722/127.03636666·주차37.48615/127.03715의 SDK 결과/순정 핀을 대조해.
 Console 좌우 Empty→Occupied→Empty와 앱 초기값/콜백/옵션 제안·설정 복원·기기 검사를 각각 기록해.
 일반 Android/호스트 X11 성공으로 Pleos 실연동을 대체하지 마. 실제 스위치 선택기 Pleos 조작도 미검증이야.

실제 화면/로그에서 재현한 오류부터 수정하고 완료 기능/통과 검사는 근거 없이 반복하지 마.
기기 테스트 데이터 초기화는 이 job이 만든 새 전용 AVD에서만 해. 서버 DB 삭제/새 세션 재발송으로 복구하지 마.
자격증명·등록 발신번호·동의 수신자가 없으면 모의 SMS를 유지하고 공급자 수락/휴대폰 수신을 구분해.
제출 키가 있을 때만 서명·새 AVD 설치·해시를 대조해. 가상 병원·사전 영상·실차를 구분해.
비밀값·전화번호·키를 채팅/GitHub/artifact에 남기지 마. docs/08·10·12와 증거·PR을 갱신하고 기능별 커밋·푸시해.
APK 변경 시 버전·코드·해시·서명을 기록해. 마지막에 실제 스크린샷·PR·APK·검증 링크와 통과/미검증을 알려줘.
다음 실행 프롬프트도 파일에 저장하고 채팅에 직접 보여줘.
```
