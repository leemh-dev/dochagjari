# 다음 개발 실행 프롬프트

2026-11-12 UTC · 최신 실제 결과를 먼저 확인합니다.

```text
도착자리 개발과 11월15일 제출 준비를 이어서 진행해줘. 범위는 docs/07-development-plan.md를 유지해.
AGENTS.md·README·docs/07~12·PR#1·최신 CI/artifact부터 확인해.
PR이 열려 있으면 feat/android-submission-mvp에서 이어가고, 병합됐으면 최신 main에서 새 브랜치를 만들어.
자동 병합/기존 변경 덮어쓰기는 하지 마. 디자인 전면 개편은 미뤄.
앱0.2.0/2·배포 코드3a01515·artifact13871881801·기존 해시/개발 서명을 유지해.
CI 임시 검증 APK는 배포본과 구분하고 서명이 다르면 새 전용 AVD만 사용해.

진행 상황과 중간 화면
- 시작할 때 ①최신 상태·기기 확인 ②Pleos 승인·준비·설치 ③실제 내비·좌석 연동
  ④회귀·복구·외부 연동 ⑤디자인·서명·시연·제출 준비의 완료/진행 중/차단/미검증,
  현재 위치와 다음 작업을 알려줘. 단계 전환·중요 결과/실패·긴 실행 중 약1분마다 갱신해.
  차단 중 독립 작업은 상태를 따로 표시하고 근거 없는 진행률%는 만들지 마.
- 에뮬레이터에서 앱이 실행되면 최초 화면·주요 기능 전환·검사 결과·오류와 수정 전후에
  실제 앱 화면을 캡처해서 중간중간 채팅 이미지로 직접 보여줘. 환경(Pleos/일반 Android),
  실제 연동/테스트 입력/모의와 확인한 결과를 붙이고 최종 답변이나 링크로만 미루지 마.
- CI 종료 후 artifact를 받아야 하면 제약과 받을 시점을 알려주고 확보 즉시 표시해.
  이전 화면을 새 캡처로 표시하지 마. 승인창/검정 화면은 앱 실행과 구분하고 성공으로 표시하지 마.
  비밀값·전화번호·키를 노출하지 마.

현재 증거
-a8c91d2 PR43148994223 일반 Android CI 전체 성공: JVM33·서버14·lint·당시 진단30·기능6·프로세스2단계·
 UI13장·실제 HTTP SERVER_MOCK 복구야. 새 원본은 docs/evidence/android-current-c610/·android-current-ebfa/·
 android-current-3c/에 있고 확보 즉시 채팅에도 표시했어. 실제 병원·차량·SENS·Pleos SDK 성공은 아니야.
-HTTP 복구는 GET404→POST 저장/응답 보류→앱 종료·동일DB 서버 재시작→새 PID/빈 토큰→같은세션 GET200,
 POST1/DB행·지문 동일/공급자0이야. 새 세션 재발송이나 DB 삭제로 복구하지 않았어.
-Pleos 정상 승인·device/boot1/package/activity·각rc0과 설치 직전 재확인을 실제 확인했어.
 3c0ad6e 수동43145130375는 공간 부족으로 설치 실패/기기검사 취소였고, a8c91d2 수동43148832528은
 새 자체8GiB 데이터에서 APK 설치·CAR_SPEED 정상권한rc0이 성공했어. guest /data여유약5.3GiB/NORMAL이야.
 docs/evidence/pleos-storage-blocked/·pleos-installed-ui-blocked/를 먼저 읽어. 기존 기기/DB/저장 정책은 안 바꿨어.
-a8c91d2 조기artifact13914340256/최종13913448564: UI dump45초 timeout2회·좌석 옵션 라벨 누락이야.
 원본에는 계기 영역만 보이고 앱 표시/SDK 요청/핀/Console/좌석 수신은 미검증이야.
 requested_coordinate 배열은 계획한 입력이며 실제 NaviHelper 호출 증거가 아니야.
-connectedDebugAndroidTest는14m35s 뒤 test APK split install commit ShellCommandUnresponsiveException,
 XML tests0으로 실패했어. 기능6개가 실패한 것이 아니며 Console5554 경고의 인과관계는 미확인이야.
-8d12198 수동43151137564/artifact13914083961에서 실제 Pleos 앱 표시를 확보해 채팅에 보여줬어.
 docs/evidence/pleos-first-app-display/를 읽어. display0/물리1개·Awake·foreground user10·앱focused/visible/multi-window야.
 첫launch Status timeout/순정 지도 로딩과 나중 앱 표시를 구분해. 다른 display 추정은 지지되지 않았어.
 앱은 테스트/모의 상태이며 SDK 호출/핀/Console는 미검증이야. 기존 탭 뒤에도 XML은 도착 안내에 남았어.
-9430b7d는 foreground user를 명시하고 새 XML/앱 root/전환 확인, 정상 에뮬레이터 창 터치를 비교해.
 앱 포커스·물리1개·viewport비율/기하정보·native 입력 영역/정상 창 포커스가 모두 확인돼야 입력해.
 대상 모르면 차단하고 guard를 약화하지 마. Android user/display/입력·주행 제한 정책은 바꾸지 않아.
-최신 수동43154848171(device_test_driver=adb)과 PR43154858975/push43154848167의 결과·artifact부터 확인해.
 현재 진행 중인 결과를 성공으로 표시하지 말고 docs/08·10의 최종 갱신을 우선해.
-표준 ADB 단일 test APK 설치(--no-streaming -t -r)/현재user am instrument와 별도 프로세스 복원 driver를 준비했어.
 진단35건·구문/actionlint/diff는 로컬 통과야.7b3cd2d PR43152685967/push43152553047 일반 CI는 진단33 포함 전체 성공.
 실제 ADB driver 성공은 아직 미검증이고 Gradle connected 통과와 구분해.
 완료 status0 6개·OK(6 tests)·최종 code-1·오류 없음이 모두 있어야 통과야.
-공식 source.properties의 multi_display_window/hotplug_multi_display=no와 수동 config 미명시 차이는
 원인으로 확정하지 않았고 설정을 바꾸지 않았어. userdata는 메타데이터만 읽었고 내용/키 복사는 안 했어.
 맥북은 사용자가 아직 꺼져 있다고 확인했어. 실제 접근/자원 검증을 했다고 하지 마.

우선 작업
1. 접근 가능한 기기·맥북 원격 연결부터 확인해. Cloud에는 KVM/연결기기/VPN이 없어.
   기존 원본/JSON부터 읽고 새 진단 목적·근거·변경을 기록한 경우에만 수동 실행해.
   같은 실패를 근거 없이 재부팅/다운로드하지 말고 sync/capture/RAM/userdata/profile crash로 단정하지 마.
2. 키 복사/편집·인증 비활성화·권한 우회·Always allow 선택은 하지 마.
   정확한 현재 호스트 지문·문구·버튼·native 대상·포커스 조건과 실제 ADB rc/stderr/devices를 대조해.
   OCR unknown/정상 입력 전송/검정 화면도 승인이 아니야. 맥북에 접근하지 못하면 실행했다고 하지 마.
3. display/activity 보고서·am start 응답·최초/물리display 원본·마지막XML·UI dump 부분출력을 먼저 대조해.
   실제 앱이 있는 display와 입력/캡처 대상을 증거로 확인하고, 계기 화면만으로 원인을 단정하지 마.
   정상 SDK/ADB 방식으로만 조작하고 display/포커스/권한 정책을 우회하지 마.
   transport=device·boot1·package/activity·각rc0을 실제 확인하고 설치 직전에 재확인한 뒤에만 설치해.
   준비되면 내비 모의를 끄고 픽업37.48544722/127.03636666·주차37.48615/127.03715의 SDK결과/순정핀을 대조해.
   Console 좌우 각각 Empty→Occupied→Empty, 앱 초기값/콜백/옵션 제안·설정복원·기기검사를 각각 기록해.
   Console 조작·앱 수신·실차는 구분해. 일반 Android/호스트 X11을 Pleos 실연동으로 대체하지 마.
4. Gradle 테스트 설치 차단과 앱 기능 검사를 분리해. 표준 ADB driver를 실행할 때도 실제 readiness,
   같은 개발 서명의 app/test APK와 새 전용 AVD를 확인해. device_test_driver=adb 결과를 Gradle 통과로 표시하지 마.
   DOCHAGJARI_FRESH_TEST_AVD=1과 --reset-test-data가 모두 있어야 하는 guard를 유지해.
   실제 화면/로그에서 재현한 오류부터 수정하고 완료 기능/통과 검사는 근거 없이 반복하지 마.
   기기 데이터 초기화는 이 job이 만든 새 전용 AVD에서만 해. 서버 DB 삭제/새 세션 재발송으로 복구하지 마.
5. 자격증명·등록 발신번호·동의 수신자가 없으면 모의 SMS를 유지해. 공급자 수락/휴대폰 수신은 구분해.
   제출 키가 있을 때만 서명·새 AVD 설치·해시 대조를 해. unsigned release를 제출 서명 완료로 표시하지 마.
   가상 병원·사전 영상·순정 카메라·실차 검증을 구분하고 환경이 막혀도 독립 작업은 계속해.

docs/08·10·12와 정제한 증거·PR을 갱신하고 기능별 커밋·푸시해.
docs/10에 날짜·조사/변경·환경/명령·실제 결과·실패/해결·증거/커밋·남은 작업을 남겨.
APK 변경 시 버전·코드·SHA256·서명을 기록하고 비밀값·전화번호·키는 채팅/GitHub/artifact에 남기지 마.
마지막에 실제 스크린샷·PR·APK·검증 링크, 통과/미검증·다음 작업을 알려줘.
다음 실행 프롬프트도 파일에 저장하고 채팅에 직접 보여줘.
```
