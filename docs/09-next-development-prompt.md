# 다음 개발 실행 프롬프트

## 서비스 핵심과 추가 요구 — 다음 작업에 함께 적용

```text
서비스 핵심은 몰/백화점의 실제 방문 매장/층에 맞는 게이트·주차 구역·엘리베이터 계획과 귀환 기억이야.
docs/13·14 및 갱신한 docs/01·03·04·07을 먼저 읽어.
데이터는 공공데이터·공식 시설 안내·최신 관련 리뷰·운영사 제휴/현장 확인을 조합해 확보해.
입차 확인 후 지원 카메라의 기둥·벽·층/구역·방향 표지를 관측하고 최종 정차 부근의 위치를 확인·저장해.
안내 취소/경로 계산 완료/잠깐 본 기둥을 입차·최종 주차로 오해하지 마. 단서가 없으면 미확인과 수동 보완을 제공해.
순정 카메라가 미연동이어도 사전 영상/테스트 입력을 같은 파이프라인에 연결해 개발을 계속하고 출처를 표시해.
제휴·실시간 검색·자동 주차 위치 기록이 이미 구현됐다고 표시하지 마. 실제 시설 한 곳의 전체 흐름부터 검증해.
목표는 검증과 시연영상이고 앱마켓 출시/출시용 서명 키 준비는 제외해.
```

## 맥북 M1 외장 SSD에서 이어가기

```text
도착자리 Android/Pleos 개발을 외장 SSD의 기존 clone에서 이어서 진행해줘.
AGENTS.md·README·docs/07~14·PR #1·최신 CI를 확인하고 제출 범위와 현재 브랜치를 유지해.
SSD가 마운트됐는지 확인하고 scripts/pleos/macos-ssd-env.sh로 기존 ARM64 SDK·AVD·Gradle 캐시를 재사용해.
시작할 때 메모리 압력·swap·내부/SSD 여유를 읽고 에뮬레이터는 한 대만 실행해.
M1에서는 Pleos v2.1.0/API 34/4GiB/HVF/호스트 그래픽/콜드 부팅과 -no-audio로
기존 배포 APK 0.2.0/2의 설치·기본 조작을 확인했어. 오디오를 켠 첫 실행은 QEMU watchdog 종료였어.
압력이 높으면 AVD를 닫은 뒤 작업자 1개로 빌드하고 Gradle과 AVD를 동시에 띄우지 마.
배포 APK와 Mac 개발 서명이 다르므로 배포 조작 AVD와 기기 검사 AVD를 구분해.
에뮬레이터 종료 후 debug/test 빌드와 표준 ADB 기능 6개는 성공했어. Gradle connected와 별도 프로세스 재시작 검사는 미검증이야.
ADB 시리얼·현재 사용자·display·앱 focus·boot1·package/activity·/data 여유를 실제로 재확인해.
안정적이면 실제 내비 두 목적지의 SDK 결과와 순정 핀, Virtual Console 좌우 좌석 조작과 앱 콜백을 각각 확인해.
앱의 모의 입력, 실제 Pleos SDK/Console, 실차를 구분하고 승인/주행/디스플레이 제한을 우회하지 마.
변경과 검증을 docs/08·10·12와 PR에 기록하고 기능 단위로 커밋·푸시해. 실제 SMS는 필요한 조건이 갖춰졌을 때만 검증하고, 앱마켓 출시/출시용 키 준비는 이번 목표에서 제외해.
```

## 이전 Cloud 검증에서 이어갈 때

2026-11-12 UTC · 최신 실제 실행 경계를 유지합니다.

```text
도착자리 개발과 11월15일 제출 준비를 이어서 진행해줘. 범위는 docs/07-development-plan.md를 유지해.
AGENTS.md·README·docs/07~12·PR#1·최신 CI/artifact부터 확인해.
PR이 열려 있으면 feat/android-submission-mvp에서 이어가고, 병합됐으면 최신 main에서 새 브랜치를 만들어.
자동 병합/기존 변경 덮어쓰기는 하지 마. 디자인 전면 개편은 미뤄.
배포0.2.0/2·앱 코드3a01515·artifact13871881801·기존 해시/개발 서명을 유지해.
CI 임시 검증 APK/서명은 배포본과 구분하고, 서명이 다르면 새 전용 AVD만 사용해.

진행 상황·중간 화면
- 시작할 때 ①최신 상태·기기 확인 ②Pleos 승인·준비·설치 ③실제 내비·좌석 연동
  ④회귀·복구·외부 연동 ⑤디자인·서명·시연·제출 준비의 완료/진행 중/차단/미검증,
  현재 위치·다음 작업을 알려줘. 단계 변경·중요 결과/실패·긴 실행 중 약1분마다 갱신해.
  차단 중 독립 작업은 따로 표시하고 근거 없는 진행률%는 만들지 마.
- 앱 최초 표시·기능 전환·검사 결과·오류와 수정 전후에 실제 화면을 캡처해
  중간중간 채팅 이미지로 직접 보여줘. 환경(Pleos/일반 Android), 실제/테스트 입력/모의와 결과를 붙여.
  CI artifact를 기다려야 하면 제약·수신 시점을 알리고 확보 즉시 표시해.
  이전 화면을 새 캡처로 표시하지 마. 승인창/검정 화면/순정 지도 로딩은 앱 실행 성공과 구분해.
  비밀값·전화번호·키를 노출하지 마.

현재 증거
-9430b7d PR43154858975 일반 Android CI 전체 성공: JVM33·서버14·lint·진단35·기능6·프로세스2단계·
 UI13장·실제 HTTP SERVER_MOCK 복구야. 이후 d6fa66f/문서 HEAD CI는 GitHub에서 다시 확인해.
 새 일반 Android 원본은 docs/evidence/android-current-c610/·android-current-ebfa/·android-current-3c/에 있어.
 과거 완료 검사를 이번에 새로 실행한 것으로 표시하지 마.
-HTTP 복구는 GET404→POST 저장/응답 보류→앱 종료·동일DB 서버 재시작→새 PID/빈 토큰→같은세션 GET200,
 POST1/DB행·지문 동일/공급자0이야. 실제 SENS 수락/휴대폰 수신이 아니야.
-a8c91d2 수동43148832528은 정상 승인/device·boot1·package/activity·각rc0·설치 직전 재확인 후
 새 자체8GiB 데이터에서 APK 설치가 성공했어. guest /data여유약5.3GiB/NORMAL이었고 저장 정책은 안 바꿨어.
-8d12198 수동43151137564/artifact13914083961에서 실제 Pleos 앱 표시를 확보했어.
 docs/evidence/pleos-first-app-display/를 읽어. display0/물리1개·Awake·foreground user10·앱focused/visible/multi-window야.
 첫launch Status timeout/순정 지도 로딩과 나중 앱 표시를 구분해. 다른 display 추정은 지지되지 않았어.
 앱은 차량/내비 테스트·모의 상태야. 탭 전환·SDK 요청/핀·Console·좌석 수신은 미검증이야.
 requested_coordinate 배열은 계획한 입력이며 실제 NaviHelper 호출 증거가 아니야.
-a8c91d2 connectedDebugAndroidTest는14m35s 뒤 test APK split install commit의
 ShellCommandUnresponsiveException으로 XML tests0/실패야. 기능6개 실패가 아니야.
 docs/evidence/pleos-installed-ui-blocked/의 실제 XML을 읽고 Console5554 경고를 원인으로 단정하지 마.
-최신9430b7d 수동43154848171/조기13915193424/최종13914939268은 정상 호스트 지문/문구·native 창/포커스 후
 Allow1회에도 승인창과 unauthorized가 남았어. docs/evidence/pleos-approval-unconfirmed-latest/를 먼저 읽어.
 transport rc1, devices rc0/마지막transport_id1이지만 전후 이력은 없어. boot/package/activity 미조회,
 설치·새 host 터치·SDK/Console·표준 ADB 기기/복원 검사는 생략됐어. 이후 추가 부팅은 요청하지 않았어.
-현재user 명시·새 XML/앱 root/전환 acknowledgement·정상 창 터치와 표준 ADB 단일 test APK
 --no-streaming 설치/am instrument·별도 두 프로세스 복원 driver를 준비했어. 실제 Pleos 실행은 미검증이야.
 로컬 진단35·구문/actionlint/diff 통과, 앱/서버/Gradle 런타임은3a01515와 동일해.
-맥북은 사용자가 아직 꺼져 있다고 확인했어. Cloud에는 KVM/연결기기/VPN이 없어 같은 무가속 부팅은 반복하지 마.

다음 작업
1. 접근 가능한 정상 승인 기기와 기존 원본부터 확인해. 최신 승인 재발의 새 진단 목적·근거·변경을
   먼저 기록한 경우에만 수동 실행해. 같은 부팅/다운로드/입력을 근거 없이 반복하지 마.
   키 복사/편집·인증 비활성화·권한 우회·Always allow·user/display/주행 제한 정책 변경은 하지 마.
   입력 전송·OCR unknown·검정 화면을 승인으로 계산하지 마. RAM/userdata/profile crash/reconnect를 원인으로 단정하지 마.
2. 실제 transport=device·boot1·package/activity·각rc0과 설치 직전 재확인 후에만 설치해.
   기존 기기 데이터는 삭제하지 말고, 데이터 초기화는 이 job이 만든 새 전용 AVD의 고정 앱/현재user만 다뤄.
   user10을 고정하지 말고 실제 foreground user를 읽어. user 전환이나 다른 계정 접근으로 해결하지 마.
3. 정상 host 터치는 실제 앱 포커스·물리1개·viewport 비율/기하정보·native 입력 영역/포커스 guard를 유지해.
   대상 모르면 차단하고 약화하지 마. 매번 새 UI XML과 탭 전환·스위치 상태의 실제 acknowledgement를 확인해.
4. 준비되면 내비 모의를 끄고 픽업37.48544722/127.03636666·주차37.48615/127.03715의 SDK 요청/결과와 순정 핀을 대조해.
   Console 좌우 각각 Empty→Occupied→Empty, 앱 초기값/콜백/옵션 제안·저장 완료/재실행 복원을 확인해.
   Console 조작·SDK 수신·실차를 구분하고 일반 Android/호스트 OCR/테스트 입력으로 실제 SDK 성공을 대체하지 마.
5. 표준 ADB driver는 device_test_driver=adb이며 새 AVD·동일 개발 서명의 app/test APK·실제 readiness부터 확인해.
   DOCHAGJARI_FRESH_TEST_AVD=1과 --reset-test-data guard를 유지해.
   완료 status0 6개·OK(6 tests)·최종 code-1·오류 없음과 별도 프로세스 복원 결과를 기록해.
   실제 실행 전 성공으로 표시하거나 Gradle connected 통과로 대체하지 마.
6. 재현한 오류부터 수정하고 통과 검사/완료 기능은 근거 없이 반복하지 마. 환경이 막혀도 독립 작업은 계속해.
   pending/unknown/통신 실패를 새 세션 재발송이나 서버 DB 삭제로 복구하지 마.
   자격증명·등록 발신번호·동의 수신자가 없으면 모의 SMS를 유지하고 공급자 수락/휴대폰 수신을 구분해.
   제출 키가 있을 때만 서명·새 AVD 설치·해시를 대조해. unsigned release를 제출 서명 완료로 표시하지 마.
   가상 병원·사전 영상·순정 카메라·실차를 구분하고 접근 못한 맥북에서 실행했다고 하지 마.

docs/08·10·12·정제한 증거·PR을 갱신하고 기능별 커밋·푸시해.
docs/10에 날짜·조사/변경·환경/명령·실제 결과·실패/해결·증거/커밋·남은 작업을 남겨.
APK 변경 시 버전·코드·SHA256·서명을 기록하고 비밀값·전화번호·키는 채팅/GitHub/artifact에 남기지 마.
마지막에 실제 스크린샷·PR·APK·검증 링크, 통과/미검증·다음 작업을 알려줘.
다음 실행 프롬프트도 파일에 저장하고 채팅에 직접 보여줘.
```
