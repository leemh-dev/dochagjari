# 다음 개발 실행 프롬프트

2026-11-11 UTC · v0.2.0 복구/저장 보완 이후. 매번 [검증 기록](08-verification.md)과 [개발 일지](10-development-log.md)의 최신 결과를 먼저 확인합니다. 이미 완료한 구현을 반복하지 않습니다.

다음 개발 대화에 아래 전체를 붙여 넣습니다.

```text
leemh-dev/dochagjari의 도착자리 Android 앱 제출 준비를 이어서 진행해줘.
11월 15일 범위는 docs/07-development-plan.md를 기준으로 유지해줘.

먼저 AGENTS.md, README.md, docs/08-verification.md, docs/10-development-log.md,
docs/11-arrival-recovery.md, PR #1과 최신 CI·artifact를 확인해줘.
PR #1이 열려 있으면 feat/android-submission-mvp에서 이어가고, 병합됐다면
최신 main에서 새 브랜치를 만들어줘. 기존 작업을 덮어쓰거나 자동 병합하지 마.

완료한 구현
- v0.2.0: 확정 좌표·수신자·발송 방식·동의와 상태를 영속 저장하고 같은 세션으로 GET 선조회.
  기록 없음·유효한 기존 동의일 때만 같은 ID/내용으로 재개하며 pending/unknown/통신 실패에서
  새 세션 재발송을 하지 않아. 서버 요청 지문·모드·수신자 불일치도 차단해.
- 화면 값과 DataStore 저장 완료를 구분하고 앱 수명의 직렬 저장 큐와 실패 재시도를 구현했어.
- Android 디코더 Bitmap을 ARGB_8888로 정규화해 실제 Android 모델 입력 오류를 수정했어.
- JVM 33건, 서버 14건, lint·debug/test APK·unsigned release 빌드는 통과했어.
- 일반 Android API34에서 앱 내 영상 TFLite·ML Kit C12 및 여섯 기능 검사가 통과했어.
  실제 프로세스 강제 종료 전후 PID 변경·동일 요청 지문·완료 프로필 복원도 통과했어.
  Pleos의 최종 실행 수준은 docs/08의 최신 증거로 판단해.
- 새 APK는 검증 문서의 development-release-assets-and-metadata artifact에서 받아. 저장소 apk/의 v0.1 보관본과 혼동하지 마.
  Release API 게시 제한으로 artifact를 사용해. 코드·해시·개발 서명 지문은 docs/evidence/v0.2-apk-metadata.json에 있어.

남은 우선순위
1. Pleos 증거에서 부팅 완료/package/activity, SDK 요청 결과, 순정 내비 핀,
   Virtual Console 조작, 앱 초기값/좌우 변경 콜백을 각각 확인해줘.
   미확인 부분만 실제 접근 가능한 공식 AVD에서 보완해줘.
   기존 Cloud는 KVM이 없었고 별도 GitHub KVM 증거 수집도 있으니 기존 로그를 먼저 읽고
   같은 실패를 근거 없이 반복하지 마. 맥북에 접근할 수 없으면 실행했다고 주장하지 마.
2. 내비 두 목적지 37.48544722/127.03636666, 37.48615/127.03715의 SDK 결과와
   실제 핀을 비교해줘. 좌석은 Console 좌우 Empty→Occupied→Empty와 실제 SDK 로그·
   앱 옵션 제안을 대조해줘. 일반 Android 테스트 입력을 Pleos 성공으로 간주하지 마.
3. 자격증명·등록 발신번호·동의한 테스트 수신자가 안전하게 설정돼 있을 때만 실제 SMS를
   검증해줘. 모의 발송·공급자 수락·휴대폰 수신을 구분해. 비밀값을 채팅/GitHub에 남기지 마.
   준비되지 않으면 모의 발송으로 계속하고 pending/unknown을 재발송으로 해결하지 마.
4. 실제 병원 무단차 동선은 근거가 없으면 가상 시연을 유지해. 순정 외부 카메라·실차도
   사전 영상이나 AVD 수신으로 완료 처리하지 마. 영역 고도화·전국 시설 확대는 이번 범위에 넣지 마.
5. 제출 키가 준비되면 README의 환경 변수로 release를 서명하고 새 AVD 설치·시연을 확인해.
   키가 없으면 unsigned release와 절차만 제공하고 서명 완료로 표시하지 마.

모든 주요 조사·변경·환경·명령·실제 결과·실패와 해결·남은 작업을 docs/10에 기록해줘.
기능별 검증 수준은 docs/08에, 비밀값을 제거한 증거는 docs/evidence 또는 artifact에 남겨줘.
기능 단위로 커밋·푸시하고 PR에 검사·차단·후속 작업을 갱신해줘.
APK 변경 시 코드 커밋·버전·SHA-256·서명 종류를 함께 기록해줘.
마지막에 PR·APK·검증 링크와 통과한 검사·미검증·다음 작업을 알려줘.
```
