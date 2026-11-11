# 다음 개발 실행 프롬프트

2026-11-11 UTC · 아래 전체를 다음 개발 대화에 사용합니다. 최신 CI는 다시 조회합니다.

```text
leemh-dev/dochagjari의 도착자리 Android 앱 개발과 11월 15일 제출 준비를 이어서 진행해줘.
범위는 docs/07-development-plan.md를 유지해줘.

AGENTS.md, README.md, docs/08-verification.md, docs/09-next-development-prompt.md,
docs/10-development-log.md, docs/11-arrival-recovery.md, docs/12-submission-checklist.md와
PR #1·최신 CI·artifact부터 확인해줘. PR이 열려 있으면 feat/android-submission-mvp에서,
병합됐다면 최신 main의 새 브랜치에서 작업해. 기존 변경을 덮어쓰거나 자동 병합하지 마.

현재 기준
- 앱0.2.0/code2, 배포 코드3a01515. 이후 앱/서버/Gradle 런타임은 동일해.
  artifact13871881801의 debug/test/unsigned release를 docs/evidence/v0.2-apk-metadata.json과 대조해.
  apk/의0.1 보관본·CI 임시 APK와 구분해. 개발 서명이 이전 Cloud 키와 달라 새 전용 AVD를 써.
  unsigned release는 제출 서명 완료본이 아니야.
- 472b68d 일반 Android PR43116162416/push43115896326은 JVM33·서버14·lint·기능6·프로세스 복원2단계 성공이야.
  후속1dfa196 PR43117773337/push43117765439는 마지막 확인 시 진행 중이었어. 최신 결과를 다시 확인해.
  Python 진단/설치 차단/승인 화면/읽기 전용 후속 관찰13건과 actionlint는 로컬 통과했어.
  과거 성공을 이번 직접 재실행으로 표시하거나 이미 통과한 검사를 근거 없이 반복하지 마.

Pleos 우선 작업
1. 수동43115895691·artifact13899317062와 docs/evidence/pleos-input-unconfirmed/부터 읽어.
   KVM·이미지61범위/체크섬·GUI 성공 후 정상 안내 OK를 닫고, 새 USB 승인창의 현재 호스트 지문과
   취소/허용 쌍을 확인해 X11 허용 입력을 보냈어. 그러나300초 후에도 unauthorized rc1이었어.
   입력 전송은 게스트 승인 완료가 아니야. transport 차단으로 boot/package/activity는 미조회,
   설치·내비·Console·Pleos 기기 검사는 실행하지 않았어.
2. 승인 입력 후 화면을 수집하지 않아 대화상자 종료/재등장·입력 반영 여부는 미확인이야.
   1dfa196은 입력2초 뒤 읽기 전용 문구/화면과 종료 직전 화면을 추가했지만 Pleos 미실행이야.
   접근 가능한 기기부터 확인하고 후속 관찰을 확보해 원인을 좁혀. 새 수동 실행이 필요하면
   기존 증거와 진단 목적/변경을 먼저 기록해. 같은 실패를 근거 없이 재요청하지 마.
   Cloud에는 KVM/연결 기기가 없으므로 무가속 부팅하지 마. Mac 접근 없이 실행했다고 주장하지 마.
3. ADB는 정상 AVD 화면 승인으로만 처리해. 호스트 지문 확인을 유지하고 키 편집/복사·인증 비활성화·
   권한 우회·Always allow 선택은 하지 마. 모호한 화면은 중단해. 호스트 UI OCR은 앱 ML Kit 증거가 아니야.
   transport=device·boot1·package/activity found·각 rc0을 실제로 확인하고 설치 직전에 다시 확인해.
   RAM4GiB/8192MB·userdata 차이·profile crash를 인증 실패 원인으로 단정하지 마.
4. 준비된 Pleos에서만 APK 설치, 내비 모의 끄기, 두 목적지 SDK 콜백과 순정 핀 대조를 실행해.
   픽업37.48544722/127.03636666, 주차37.48615/127.03715.
   Console 좌우 각각 Empty→Occupied→Empty, 앱 초기값/콜백/저장 옵션 제안과 재시작 설정 유지를 확인해.
   Console 조작·앱 수신·실차를 각각 기록해. 준비 성공 시 connectedDebugAndroidTest와 프로세스 복원을 실행해.
   SDK 증거 수집 후 이 job이 만든 새 전용 AVD의 테스트 데이터만 초기화할 수 있어.
   일반 Android 테스트 입력 성공으로 Pleos 실제 연동을 대체하지 마.

외부 연동·제출
- 실제 SMS는 자격증명·등록 발신번호·동의 수신자가 안전하게 준비됐을 때만 검증해.
  모의 발송·공급자 수락·휴대폰 수신을 구분하고 pending/unknown/통신 실패를 새 세션 재발송이나 DB 삭제로 해결하지 마.
- 실제 병원 동선 근거가 없으면 가상 표시를 유지하고 사전 영상을 순정 카메라/실차 검증으로 표시하지 마.
- 제출 키가 준비됐을 때만 README 절차로 서명·새 AVD 설치·해시를 대조해. 없으면 unsigned release와 절차만 제공해.
- docs/12의 녹화 순서로 시연 영상·공식 양식·공개 저장소·APK·코드/해시를 대조해. 실제 제출 완료로 허위 표시하지 마.

막힌 환경과 무관하게 가능한 구현/검사를 계속해. 주요 날짜·조사·변경·환경/명령·실제 결과·실패/해결·
증거·커밋·남은 작업은 docs/10에, 기능별 검증 수준은 docs/08에, 정제 로그/원본 화면은 docs/evidence 또는 artifact에 기록해.
기능별 커밋·푸시와 PR 갱신을 해. APK 변경 시 코드·버전·SHA256·서명 종류를 기록해.
비밀값·전화번호·키를 채팅/GitHub/artifact에 남기지 마.
마지막에 PR·APK·검증 링크와 통과 검사·미검증·다음 작업을 알려줘.
다음 실행 프롬프트도 갱신하고 파일 저장과 함께 채팅에 직접 보여줘.
```
