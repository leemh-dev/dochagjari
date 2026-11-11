# 다음 개발 실행 프롬프트

2026-11-11 UTC · 후속 ADB 준비 검사·제출 체크리스트 보완 이후. 아래 전체를 다음 개발 대화에 붙여 넣습니다. 최신 상태는 항상 GitHub 실행 결과와 검증 기록으로 다시 확인합니다.

```text
leemh-dev/dochagjari의 도착자리 Android 앱 개발과 11월 15일 제출 준비를 이어서 진행해줘.
범위는 docs/07-development-plan.md를 유지해줘.

먼저 AGENTS.md, README.md, docs/08-verification.md, docs/09-next-development-prompt.md,
docs/10-development-log.md, docs/11-arrival-recovery.md, docs/12-submission-checklist.md와
PR #1·최신 CI·artifact를 확인해줘.
PR #1이 열려 있으면 feat/android-submission-mvp에서 이어가고,
병합됐다면 최신 main에서 새 브랜치를 만들어줘. 기존 변경을 덮어쓰거나 자동 병합하지 마.

현재 상태
- 앱은 0.2.0/2, APK 코드 3a01515. artifact13871881801의 세 APK·해시·서명은
  docs/evidence/v0.2-apk-metadata.json에 있어. 앱 코드는 이후 바뀌지 않았어.
  debug/test는 runner 개발 서명이고 이전 Cloud 키와 달라 새 전용 AVD를 써야 해.
  unsigned release는 제출용 서명 완료본이 아니야.
- 기존 a020643 CI는 JVM33·서버14·lint·일반 Android 기능6·프로세스 복원2단계까지 통과했어.
  이 결과를 이번에 반복 실행한 것으로 표시하지 마.
- 후속 변경은 공통 adb_readiness.py와 설치 직전 재확인이야. transport/boot/package/activity의
  개별 종료 코드·stdout/stderr가 모두 준비 상태일 때만 설치해.
  진단/설치 차단 테스트7건은 로컬에서 통과했지만 Pleos 기기 성공은 아니야.
- 공식 devices.xml은 RAM4GiB, source.properties는8192MB야. 원본 userdata 약1.1GB와
  이전 빈 테스트2GiB도 달라. 이 차이를 실패 원인으로 단정하지 마.

우선 할 일
1. 수동 Pleos 실행43095641582의 최신 상태·로그·artifact를 먼저 확인해줘.
   20:32 UTC 기록 당시 queued/job0였어. 이는 부팅 실패가 아니라 실행 전 대기야.
   이 요청은 a020643의 포트/ADB 진단 코드이며 새 공통 helper 변경 이전 코드야.
   현재 HEAD의 새 CI도 확인하고 기존 대기 실행을 읽기 전에 같은 부팅을 중복 요청하지 마.
2. 실제 ADB rc/stderr/devices로 연결 문제와 프레임워크 미준비를 구분해줘.
   과거 emulator Boot completed31초 로그만으로 설치 성공을 선언하지 마.
   접근 가능한 기기부터 확인하고, transport=device·sys.boot_completed=1·package/activity
   준비를 실제로 확인한 뒤 APK를 설치해. unauthorized는 정상 AVD 화면 승인으로 처리하고
   인증·권한을 우회하지 마. Cloud에는 KVM/연결 기기가 없었으니 같은 무가속 부팅은 반복하지 마.
3. 준비된 Pleos에서 내비 모의 요청을 끄고 두 목적지의 SDK 결과와 실제 핀을 비교해줘.
   픽업37.48544722/127.03636666, 주차37.48615/127.03715.
   Virtual Console 뒷좌석 좌우 Empty→Occupied→Empty, 앱 초기값·콜백·저장 옵션 제안·
   재실행 설정 유지도 확인해줘. Console 조작·앱 수신·실차를 각각 기록해줘.
   가능한 환경에서 connectedDebugAndroidTest를 실행해. 일반 Android 입력을 Pleos 성공으로 대체하지 마.
4. 실제 SMS는 자격증명·등록 발신번호·동의 수신자가 준비됐을 때만 검증해.
   그전에는 모의 발송을 유지하고 공급자 수락과 휴대폰 수신을 구분해.
   pending/unknown/통신 실패를 새 세션 재발송이나 DB 삭제로 해결하지 마.
   실제 병원 동선은 근거 없으면 가상 표시를 유지하고, 사전 영상을 순정 카메라 검증으로 표시하지 마.
5. 제출 키가 준비됐을 때만 README 절차로 서명·새 AVD 설치·해시 대조를 실행해.
   키가 없으면 unsigned release와 절차만 제공해. 접근하지 못한 맥북에서 실행했다고 주장하지 마.

환경이 막혀도 독립적으로 가능한 구현·검사는 계속하고, 이미 통과한 검사를 근거 없이 반복하지 마.
주요 날짜·조사·변경·환경/명령·실제 결과·실패/해결·증거·커밋·남은 작업은 docs/10에,
기능별 검증 수준은 docs/08에, 정제한 로그/화면은 docs/evidence 또는 artifact에 기록해줘.
기능 단위로 커밋·푸시하고 PR을 갱신해. APK 변경 시 코드·버전·SHA-256·서명을 기록해.
비밀값·전화번호·키는 채팅/GitHub/artifact에 남기지 마.
마지막에 PR·APK·검증 링크, 통과한 검사·미검증·다음 작업을 알려줘.
다음 실행 프롬프트도 갱신하고 파일 저장과 함께 채팅에 직접 보여줘.
```
