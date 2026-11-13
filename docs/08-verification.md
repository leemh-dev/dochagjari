# 구현·검증 기록

2026-11-12 UTC · 기준: [개발계획](07-development-plan.md)

## 추가한 서비스 요구 — 설계 반영/구현·검증 전

몰/백화점의 실내 방문 목적별 자료 검색/접근 계획, 공공·공식·최신 리뷰·제휴를 통한 데이터 확보, 입차 후 카메라 관측/최종 주차 위치·귀환 기록을 설계에 추가했다([상세](14-data-and-parking-observation.md)). 실제 제휴·실시간 자료 검색·순정 카메라·자동 입차/주차 기록이 구현/검증됐다는 뜻이 아니다. 기존 C12 사전 영상 인식과 새 전체 흐름 검증을 구분한다. 목표는 검증·시연영상이며 앱마켓 출시/출시용 서명은 제외한다.

## 고정 시나리오 상태

현대자동차 양재 사옥 출발→더현대 서울 특정 매장 방문→주차 위치 기억→귀환으로 첫 범위를 좁혔다([개발계획](15-hyundai-department-demo-plan.md)). 대표 후보 아가방의 현재 입점/층, 실제 출차/게이트 좌표, 주차 구역·엘리베이터 연결은 이번 계획 작성에서 조사/검증하지 않았다. 신규 추천·관측·귀환은 계획 단계이며 기존 APK/검사 성공과 구분한다.

사전 시설 DB 조회/갱신 분리와 아가방 층별 안내도 경로선을 추가 요구로 반영했다([설계](16-preloaded-db-and-indoor-route.md)). 실제 아가방 입점·층/게이트/엘리베이터 번호, 도면·거리, DB/지도 뷰는 이 문서 변경에서 조사/구현하지 않았다. 경로선은 실내 측위/현재 위치 추적과 구분한다.

## 맥북 M1 8GiB · Pleos v2.1.0 ARM64 실측

2026-11-13 KST. Apple Hypervisor 하드웨어 가속을 사용했다. 첫 공식 구성의 부팅은 QEMU watchdog으로 종료됐다. CoreAudio 입력 스트림 대기 스택을 확인해 오디오만 끈 뒤, Pleos 홈·앱 목록·빠른 설정을 실제로 조작하고 10분 동안 서비스를 관찰했다. 메모리 압력은 경고였으며 swap 최대 3923.81MiB, 10분 시점 3615.69MiB였다.

기존 배포 debug 0.2.0/2의 해시·서명·버전 대조 후 설치 및 실제 앱 표시를 확인했다. 논리 display0·물리 한 개·현재 user10·멀티윈도. 가상 학원 픽업↔주차, 좌석 조건 저장과 모의 점유 안내, 가상 병원, 테스트 주행 제한을 조작했다. 앱 실행 후 별도 10분 서비스 측정의 swap은 3875.19→3807.00MiB였다. 초반 단독 화면 지연이 있었고 압력은 경고였다. 음성·순정 내비 핀·Virtual Console 좌석 이벤트·실차·실제 SMS·제출 서명은 미검증이다.

- [실제 도착자리 픽업 화면](evidence/macos-m1-pleos/macos-dochagjari-pickup.png) · [주차 전환](evidence/macos-m1-pleos/macos-dochagjari-parking.png) · [좌석 테스트 입력](evidence/macos-m1-pleos/macos-dochagjari-seat-occupied.png) · [가상 병원](evidence/macos-m1-pleos/macos-dochagjari-hospital.png) · [테스트 주행 제한](evidence/macos-m1-pleos/macos-dochagjari-driving.png)
- [자원 및 부팅 증거](evidence/macos-m1-pleos/) · [개발 일지](10-development-log.md) · [재개 프롬프트](09-next-development-prompt.md)
- 에뮬레이터 종료 후 Gradle 작업자 1개, JVM heap 1536MiB로 debug/test APK 빌드 성공, 종료 코드 0, 253초. [빌드 결과](evidence/macos-m1-pleos/macos-debug-build-status.txt) · [로컬 APK 해시와 개발 서명](evidence/macos-m1-pleos/macos-local-apk-identity.txt)
- 새 Pleos 검사 AVD에서 로컬 개발 서명의 app/test APK 설치와 표준 ADB 기능 검사 6/6 통과, 종료 코드 0, 108초. Gradle connected는 호스트 메모리 압력으로 보류했고 별도 프로세스 재시작 검사는 미실행이다. [기기 검사 결과](evidence/macos-m1-pleos/macos-device-check-summary.txt)

## 최신 실행 종료 — 승인 불안정 재발·새 driver 미실행

9430b7d [수동43154848171](https://github.com/leemh-dev/dochagjari/actions/runs/43154848171)/artifact13914939268은 정확한 현재호스트 지문/문구·native 대상/정상 포커스 후 Allow 입력1회에도 승인창이 남고 transport rc1/unauthorized였습니다([최신 원본/JSON](evidence/pleos-approval-unconfirmed-latest/summary.json)). devices rc0/transport_id1이지만 전후 이력이 없어 reconnect 원인도 미확인입니다. boot/package/activity 미조회, 설치·새 host 터치·SDK/Console·ADB 기기/복원 driver는 생략됐습니다. 이후 추가 부팅은 요청하지 않았습니다. 앞선 설치/앱 표시 성공과 구분합니다.

9430b7d [PR CI43154858975](https://github.com/leemh-dev/dochagjari/actions/runs/43154858975)는 전체 성공(JVM33·서버14·lint·진단35·일반 Android 기능6·프로세스2·UI13장·HTTP 모의 복구). 로컬35·구문/actionlint/diff 통과. d6fa66f 현재-user 명시 보완/최종 문서 CI의 상태는 PR/Actions에서 별도로 확인하며 진행 중 검사를 성공으로 계산하지 않습니다. 이 Cloud 실행 당시에는 맥북 접근과 자원 검증을 하지 않았습니다. 실제 SMS/제출 서명·실차는 여전히 미검증입니다.

## 이전 성공 증거 — 앱 표시 확인·정상 터치/현재-user 검사 준비

8d12198 [수동43151137564](https://github.com/leemh-dev/dochagjari/actions/runs/43151137564)/artifact13914083961에서 실제 앱 표시를 확보했습니다([원본/요약](evidence/pleos-first-app-display/summary.json)). 첫 launch rc0/Status timeout·순정 지도 로딩과 이후 도착자리 표시를 구분해 채팅에 직접 보여 주었습니다. 실제 display0/물리1개·Awake·foreground user10·앱 focused/visible/multi-window입니다. 다른 display 추정은 지지되지 않아 정책을 바꾸지 않습니다. 앱은 차량/내비 테스트·모의 상태여서 SDK 실제 연동 성공이 아닙니다.

9430b7d는 현재 foreground user를 명시하고 새 XML/앱 root/탭 전환 확인·정상 창 터치의 엄격한 대상/기하정보/포커스 guard를 추가했습니다. 진단35건·구문/actionlint/diff 통과. 표준 단일 ADB test APK/현재user instrumentation과 별도 두 프로세스 복원을 수동43154848171에서 확인하려 했으나 승인 차단으로 실행하지 못했습니다. 실제 driver 성공/SDK·핀·Console·좌석 수신은 아직 미검증입니다.7b3cd2d PR43152685967/push43152553047 일반 CI는 진단33 포함 전체 성공,9430b7d PR43154858975는 진단35 포함 전체 성공입니다.

## 최신 Pleos 실제 결과 — APK 설치 성공·UI 차단/테스트 APK 설치 실패

a8c91d2 [수동43148832528](https://github.com/leemh-dev/dochagjari/actions/runs/43148832528) 조기artifact13914340256/최종13913448564: 실제 readiness/설치 직전 재확인 성공, APK 설치true·CAR_SPEED 정상권한rc0. guest `/data`약7.7GiB/여유5.3GiB·NORMAL·auto 설치 위치였습니다. **새8GiB 구성에서 앱 설치 성공을 확인했습니다.** [실제 원본/요약](evidence/pleos-installed-ui-blocked/summary.json).

두 UI dump45초 timeout과 좌석 옵션 라벨 누락이 있었고 화면에는 계기 영역만 보였습니다. 확보 즉시 채팅에 직접 표시했으며 앱 표시 성공으로 계산하지 않습니다. SDK 요청·핀·Console·실제 좌석 수신은 미검증입니다. display/activity/power/physical ID·launch 응답/원본/XML 관찰을8d12198 수동43151137564에서 진행 중입니다. 설정/앱 코드/공식 이미지/키/인증은 바꾸지 않습니다.

connected task는14m35s 뒤 **test APK split install commit ShellCommandUnresponsiveException**으로 실패했습니다. [실제 XML](evidence/pleos-installed-ui-blocked/connected-result.xml)은 tests=0이며 기능6건 실패가 아닙니다([최종 요약/로그](evidence/pleos-installed-ui-blocked/connected-final-summary.json)).3c0ad6e 실행 취소와 구분합니다.5a6ed1b는 정상 ADB 단일 test APK 설치/am instrument 및 별도 프로세스 복원 driver를 준비했고 완료 판정/fresh-device guard를 포함한 진단33건이 로컬 통과했습니다. 실제 driver 실행은 아직 미검증입니다.

## 최신 Pleos 실제 결과 — 정상 승인/서비스 준비 성공·설치 공간 부족

3c0ad6e [수동43145130375](https://github.com/leemh-dev/dochagjari/actions/runs/43145130375)/artifact13912507873에서 정확한 현재 호스트 지문·정상 창 포커스 후 Allow1회로 transport=device·boot1·package/activity found·각rc0을 실제 확인했습니다. 설치 직전 live 재확인도 통과했습니다. [실제 readiness/요약](evidence/pleos-storage-blocked/summary.json). 키·권한·인증·Always allow·에뮬레이터 입력 영역을 변경하지 않았습니다.

앱 설치는 `INSTALL_FAILED_INSUFFICIENT_STORAGE`로 실패했습니다. 당시 guest df를 수집하지 않아 정확한 여유 공간/내부 원인은 미확인입니다. 검정 화면은 앱 실행 증거가 아닙니다. SDK·핀·Console·앱 좌석 수신은 실행되지 않았습니다. connected task가 약20분 종료되지 않아 취소했고 always artifact를 회수했습니다. 완료 XML이 없어 통과로 계산하지 않습니다([정제 로그](evidence/pleos-storage-step-log.txt)). Console5554 경고를 정체 원인으로 단정하지 않습니다.

a8c91d2는 새 job의 자체 빈 userdata만8GiB로 생성하고 설치 전 host/guest 공간·저장 임계값을 읽습니다. 그 뒤 live 준비 재확인, 설치 성공 output이 있을 때만 connected 검사, 검사20분 제한/검사 전 artifact를 추가했습니다. [host 임시 ext4 크기 확인](evidence/host-eight-gib-ext4-check.json)·구문/actionlint/diff 통과. 실제 공간 오류를 근거로 수동43148832528에서 효과를 확인 중입니다. 기존 기기/DB/저장 정책/배포 APK는 바꾸지 않습니다.

## 최신 일반 Android CI — 3c0ad6e

[PR43145276876](https://github.com/leemh-dev/dochagjari/actions/runs/43145276876) 전체 성공: JVM33·서버14·lint·진단30·일반 Android 기능6·프로세스2단계·UI13장·실제 HTTP SERVER_MOCK 복구. [새 가상 병원/주행 제한 원본](evidence/android-current-3c/README.md)을 확보 즉시 채팅에 표시했습니다. 앱/배포 APK/디자인은 유지합니다. 별도 Pleos 결과는 위 실제 readiness/설치 공간 오류/검사 취소 기록을 따릅니다.

## 최신 진행 — 2026-11-12 확대 지문 OCR 보완

- **실제 최신 Pleos:**ebfa349 수동43143551458/artifact13910264896은 전체화면 지문 OCR0개로 정확한 현재 호스트 조건이false여서 승인 입력0회·unauthorized였습니다. native 활성화·boot/package/activity·설치·SDK/Console·기기 검사는 실행되지 않았습니다. [새 승인창/JSON](evidence/pleos-header-ocr-blocked/summary.json)을 확보 즉시 채팅에 앱 화면과 구분해 표시했습니다.
- **재현·수정:**같은 원본의 두 확대 방식이 제목/RSA/라벨/동일 지문을 모두 인식함을 재현했습니다.3c0ad6e는 전체 제목+지문0개 조건에서 두 확대 결과 각각이 모든 문구와 정확한 현재 호스트 지문1개를 확인할 때만 보완합니다. 다른/중복 지문·문구 누락은 거절하고 native 대상/정상 포커스/live 준비 gate는 유지합니다. Python30건·실제 원본 offline OCR·구문/actionlint/diff 통과. offline 이미지 기준 일치는 당시 호스트 인증 성공이 아닙니다.
- **독립 Android:**ebfa349 [push43143551075](https://github.com/leemh-dev/dochagjari/actions/runs/43143551075) 전체 성공. 새 영상 C12/점유 우회 화면2장을 확보 즉시 채팅에 표시했습니다([원본/범위](evidence/android-current-ebfa/README.md)). 일반 Android 앱 내 사전 영상/ML Kit 및 차량·내비 테스트 입력이며 Pleos 성공이 아닙니다.
- **현재:**3c0ad6e 수동43145130375로 이번 재현 오류 수정의 실제 효과를 확인 중입니다. 결과 미확인 상태를 성공으로 기록하지 않습니다.

## 최신 진행 — 2026-11-12 native X11 판정 수정

- **실제 재현:**0accb78 [수동43141423229](https://github.com/leemh-dev/dochagjari/actions/runs/43141423229)/artifact13910466262에서 legacy WINDOW0과 달리 직접 XQueryPointer child4194311·Emulator class·IsViewable/InputOutput·완전한 input/bounding 영역 내 요청점·X11오류0을 수집했습니다. 기존 guard의 false negative를 재현했고 엄격한 native 대조로 수정했습니다. [원본/JSON](evidence/pleos-native-target/summary.json). 이는 정상 표시/입력 대상 확인이며 게스트 승인 완료나 이전 실패의 단일 원인 증명이 아닙니다.
- **새 검증:**Python28건 로컬·자체 Xvfb 합성3상태와 정상 포커스·구문/actionlint/diff 통과. 전체 USB 제목/정확한 호스트 지문 및 같은 원본 header의 RSA/지문 라벨/동일 지문을 함께 요구하는 OCR 보완과 정상 windowfocus 뒤 직접 포커스 재확인을 추가했습니다. 실제 에뮬레이터 입력 영역/권한/키/인증 설정은 바꾸지 않습니다.
- **기존 새 CI 확인:**c610c10 PR43139267182 및0accb78 PR43141431200/push43141422428 일반 CI 전체 성공. [이번에 확보한 새 일반 Android 화면/HTTP 추적](evidence/android-current-c610/README.md)을 채팅에도 표시했습니다. JVM33·서버14·lint·기능6·프로세스2단계·UI13장·HTTP 모의 복구 성공이며 실제 공급자/휴대폰/Pleos 성공이 아닙니다.
- **진행 중:**수정 ebfa349의 수동43143551458으로 실제 native 대상/포커스/승인 효과를 확인 중입니다. 이전 c610c10 수동43139256982는 OCR gate 때문에 native 관찰까지 생략해 unauthorized([근거](evidence/pleos-native-ocr-gate/summary.json)), 이를0accb78에서 분리했습니다. 현재 설치/SDK·핀/Console/좌석/Pleos connected는 미검증입니다. 아래 과거 기록의 '추가 부팅 없음'은 당시 상태입니다.

## 최신 진행 — 2026-11-12 실제 HTTP 복구·입력 전달 진단

- **새 Android 완료 증거:**2c2feb0 [PR43134026080](https://github.com/leemh-dev/dochagjari/actions/runs/43134026080)·push43134018393 전체 성공. 실제 앱 UI→Python loopback 모의 서버에서 GET404→POST 저장 후 응답 보류→앱 force-stop→동일 DB 서버 재시작→새 앱 PID/빈 토큰 확인→같은 세션 GET200 복구를 확인했습니다. POST총1회·DB행1개·요청지문 동일·공급자 호출0회입니다. [원본3장/출처](evidence/android-http-recovery/README.md) · [추적 JSON](evidence/android-http-recovery/summary.json). **SERVER_MOCK 실제 HTTP 검증이며 SENS 수락/휴대폰 수신은 아닙니다.**
- **같은 CI:**JVM33·서버14·lint·당시 진단20·일반 Android 기능6·프로세스 복원2단계·기존 화면 조작13장도 성공했습니다. 이후 dacfc37 PR43135254549/push43135246257도 전체 성공했고 진단21건·새 HTTP 복구를 포함합니다. 문서4530bb9 CI는 확인 시 진행 중입니다. 새 실제 HTTP 검사는 별도 host fixture 사전 검사와 구분합니다. fixture의 수신자 ID 불일치는 코드 대조로 미리 수정했고 이전24a7d2c CI는 취소 완료/성공 아님으로 기록합니다.
- **새 로컬 진단:**read-only X11 pointer/포커스/geometry3건·창 목록/대상 차단1건을 더해 Python21건 로컬 통과. 구문·actionlint·diff 통과. 자체 private Xvfb에서 같은좌표 --sync/분리 입력 모두 성공했고 실제 호스트 창이 있으면 포인터 child ID가 나왔습니다. [포인터 사전 검사](evidence/host-pointer-preflight.json) · [창 비교](evidence/x11-mapped-window-check.json). Android/ADB 승인 검사가 아닙니다.
- **실제 Pleos 추가 근거:**93b4c16 수동43132924010/artifact13906886041은 unauthorized였습니다. 입력 전 좌표는2403,351로 일치하지만 WINDOW0/focus543/display3000x2000이며 입력 명령이10초 timeout이었습니다. [JSON/범위](evidence/pleos-x11-input/summary.json). 반복 --sync만으로 원인을 설명할 수 없고, 창 종료/이동/게스트 장애를 아직 구분하지 못합니다. 설치/속성·서비스/SDK/Console/기기 검사는 생략했습니다.
- **후속 보완:**dacfc37은 대상 창 미확인 시 입력 차단/새 원본 수집, 제목을 제외한 root 창 목록, 이미지 다운로드 전 host 포인터 사전 검사를 추가했습니다. 같은200ms 정상 동작/정확한 지문·문구·버튼 조건·live ADB 설치 준비 검사를 유지합니다. 수동43135246535/artifact13907941147은 host 사전 검사 성공 후에도 WINDOW0/focus=root543이어서 입력 차단·unauthorized로 끝났습니다. 새 원본에는 승인창이 있고 창 목록에는 Emulator2400x1350/100,100이 있습니다. 이것만으로 입력 전달/승인 성공을 뜻하지 않습니다. [실제 결과/원본](evidence/pleos-window-target-blocked/summary.json). 설치/SDK/Console/기기 검사는 생략했고 이후 추가 부팅을 요청하지 않았습니다. 자체 X11 창은 screenshot 전후 모두 정상 대상 ID였으므로 capture/sync만으로 단정할 수 없습니다.
- **유지/외부 조건:**Kotlin 앱/서버 런타임/배포0.2.0/code2·코드3a01515·artifact13871881801·기존 해시/개발 서명은 유지합니다. Cloud KVM/기기/VPN 없음·Mac 미접근. 디자인 전면 개편은 미룹니다. 실제 내비 핀/좌석·순정 카메라·실차·SENS 수락/수신·제출 서명은 미검증입니다.

## 최신 진행 — 2026-11-12: 실제 앱 화면·오류 수정

- **새 실제 실행:**cc1c19e [PR43128321081](https://github.com/leemh-dev/dochagjari/actions/runs/43128321081)·push43128313393 모두 성공. JVM33·서버14·lint·당시 Python13·일반 Android 기능6·프로세스 복원2단계에 더해 **새 UI 조작 검사10개 확인/원본 화면13장**을 확보했습니다. [화면·XML·출처](evidence/android-ui-screens/README.md) · [실제 요약](evidence/android-ui-screens/summary.json). 픽업/주차/우회/후보 없음/가상 병원/저장 완료 표시/실제 Android 생성 영상 EMPTY·OCCUPIED/C12/점유 우회/알림 좌표 입력/주행 잠금·영상 숨김입니다. 알림 캡처 자체는 발송 완료 증거가 아닙니다.
- **구분:**새 전용 Generic Android API34 Google APIs/x86_64 KVM입니다. 앱 차량/내비/알림은 테스트 입력/모의이며 앱 내 영상 모델·ML Kit는 실제 Android 실행입니다. Pleos 순정 내비 핀·실제 좌석·순정 카메라·실차 성공으로 대체하지 않습니다.
- **재현/수정:**기존 Pleos UI 조작기가 Switch 클래스만 찾았으나 실제 앱 XML에는0개이며 Compose 스위치는 checkable/clickable android.view.View였습니다.900ae4c에서 정확한 라벨·행·오른쪽 위치·유일한 체크 노드로 수정했습니다. [전후 증거](evidence/compose-toggle-regression.json). 실제 XML 회귀3건을 추가해 Python **17건 로컬 통과**(실제 XML3 + 기존13 + OCR 누락1), 구문·actionlint·diff 통과. 주행 중 disabled 검사와 변경 후 checked 확인을 유지합니다. 이 선택기 수정의 실제 Pleos 기기 실행은 아직 미검증입니다.900ae4c PR43130278797/push43130268157·후속ffa7fcf PR43131230889/push43131223889 CI는 확인 시 진행 중입니다. 직전 d661520 push43129700273도 일반 Android 전체 성공했습니다.
- **Pleos 관찰 보완 실제 실행:**055bdea 수동43128107467/artifact13904930954에서 허용 직후와 종료 직전에도 같은 승인창이 남고 unauthorized임을 확인했습니다. [입력 후 원본](evidence/pleos-post-input-dialog/debugging-after-input-2.png) · [JSON](evidence/pleos-post-input-dialog/readiness.json). 입력 명령 성공을 승인 성공으로 계산하지 않습니다. transport 차단으로 boot/package/activity 미조회, 설치/SDK/Console/기기 검사는 생략했습니다.
- **비교 결과:**d661520 수동43129699644/artifact13904479034는 정상 down→200ms→up에도 unauthorized로 종료됐습니다. [실제 결과](evidence/pleos-sustained-input/summary.json). 원본 입력 후 화면에는 승인창이 남아 있는데 OCR은 false였습니다. 후속 ffa7fcf에서 미인식을 unknown/absence_verified=false로 명시했습니다. 종료 직전 검정 화면도 승인 증거가 아닙니다. 정확한 원인은 미확인이고 추가 부팅은 요청하지 않았습니다. 이 실행에는 이후900ae4c 선택기 수정은 포함되지 않습니다. 인증/키/항상 허용 설정은 변경하지 않았습니다.
- **산출물:**Kotlin 앱 런타임·배포0.2.0/code2·코드3a01515·기존 APK 해시/서명은 동일합니다. 앱 기능 오류가 새로 재현됐다고 주장하지 않습니다. 이번에 재현·수정한 것은 UI 조작기 오류입니다. Cloud KVM/ADB기기/VPN 없음, Mac 미접근. SMS/제출 키가 없어 실제 발송·휴대폰 수신·제출 서명은 미검증입니다.

## 최신 진행 — 2026-11-11 23:10 UTC

- **일반 Android:** 앱 코드 변경 없이472b68d [PR CI43116162416](https://github.com/leemh-dev/dochagjari/actions/runs/43116162416)·[push CI43115896326](https://github.com/leemh-dev/dochagjari/actions/runs/43115896326) 전체 성공(JVM33·서버14·lint·일반 Android 기능6·프로세스 복원2단계). a020643의 과거 결과와 이번 자동 CI를 구분합니다. 중간43110288580은 SDK ZIP 설치 오류로 기기 실행 전에 실패했고 [증거](evidence/generic-ci-sdk-download-failure.txt)를 보존합니다.
- **신규 로컬:** 공통 준비/설치 차단7 + 정상 승인 화면·후속 관찰6 = Python **13건 통과**, 구문·actionlint1.7.7·diff 검사 통과. [로그](evidence/pleos-diagnostic-tests.txt). 기기 검사가 아닙니다. 후속1dfa196의 [PR43117773337](https://github.com/leemh-dev/dochagjari/actions/runs/43117773337)·push43117765439 CI는 확인 시 진행 중이며 성공으로 계산하지 않습니다.
- **GitHub 시작:** 이전43095641582의 공개 화면에서 unexpected error annotation/request ID를 확보했습니다([증거](evidence/workflow-startup-annotation.json)). 새 근거의43105761433은 실제 KVM runner에서 실행되어 unauthorized rc/stderr/devices를 수집했습니다. 내부 원인·전역 장애·RAM/profile crash와의 인과관계는 단정하지 않습니다.
- **GUI 진전과 차단:** launcher127 이후 loader 사전 검사/번들 경로를 보완했습니다.43111928160은 실제 창·현재 호스트 지문까지 확인했지만 버튼 인식 실패로 클릭0이었습니다. 원본 화면의 모달/한국어 버튼을 근거로472b68d를 실행했습니다.
- **최신 Pleos 실제 결과:** [43115895691](https://github.com/leemh-dev/dochagjari/actions/runs/43115895691), artifact13899317062. KVM·공식 이미지61/61·체크섬·GUI 성공. 정상 안내 OK 후 새 화면에서 USB 문구/현재 호스트 지문/취소·허용 쌍을 확인해 X11 허용 입력을 보냈지만,300초 후에도 transport rc1/unauthorized였습니다. **입력 전송 성공은 게스트 승인 완료가 아닙니다.** [요약](evidence/pleos-input-unconfirmed/summary.json) · [원본 화면](evidence/pleos-input-unconfirmed/debugging-screen-2.png) · [전체 준비 JSON](evidence/pleos-input-unconfirmed/readiness.json).
- **실행하지 못한 것:** transport 단계에서 차단되어 boot 속성·package/activity는 미조회입니다. APK 설치·실제 내비/핀·Console 좌석·앱 SDK 수신·Pleos connected 검사는 생략했습니다. 일반 Android/호스트 OCR로 대체하지 않습니다.
- **후속 구현:** 1dfa196은 입력2초 뒤 읽기 전용 화면/문구 관찰과 종료 직전 원본 화면 수집을 추가했습니다. 이 보완 이후 Pleos 부팅을 재요청하지 않았으므로 실제 후속 화면은 아직 없습니다. 원인은 미확인이고 키/인증/권한을 우회하지 않습니다.
- **APK/외부 조건:** 앱/서버/Gradle 소스는3a01515 이후 동일하며 배포0.2.0/code2 artifact13871881801·기존 해시는 유지합니다. artifact 미만료(2027-02-09). 각 runner의 임시 검증 APK는 [별도 identity](evidence/pleos-input-unconfirmed/verification-apk-identity.txt)로 구분합니다. 제출 키·SMS 설정/동의 수신자 미제공, Mac 접근 없음. 실제 SENS 수락/휴대폰 수신·제출 서명·병원 동선·순정 카메라·실차는 미검증입니다.

## 후속 ADB 진단 — 2026-11-11 20:33 UTC

- PR#1은 open/미병합이며 기준 `a020643`의 [PR CI](https://github.com/leemh-dev/dochagjari/actions/runs/43062184213)와 push CI가 모두 성공한 것을 이번에 확인했습니다. 아래 Android JVM33·서버14·기기6·프로세스2단계는 이 기존 완료 증거이며 이번에 반복 실행하지 않았습니다.
- `fcfbfc0`에서 transport/boot/package/activity를 개별 명령·종료 코드·stdout/stderr로 검사하는 `scripts/pleos/adb_readiness.py`를 추가했습니다. 준비 기록이 true여도 설치 직전에 실제 상태를 다시 검사합니다. 미준비면 설치·실행을 차단합니다.
- 새 진단 검사 **7건 로컬 통과**: [로그](evidence/adb-readiness-tests.txt). 이 중 두 건은 독립 Python 프로세스의 실제 ui_probe를 실행해 과거 성공 JSON + 현재 기기 없음, 과거 실패 JSON에서 설치 명령이 호출되지 않음을 확인합니다. ADB는 대역이며 Pleos 기기 실행 성공이 아닙니다.
- 현재 Cloud에 KVM/연결 기기가 없고 실제 CLI에서 [device not found](evidence/cloud-adb-readiness.json)를 받았습니다. 설치·소프트웨어 부팅을 실행하지 않았습니다.
- 누락됐던 ADB 오류를 수집하려고 a020643을 [수동 진단 43095641582](https://github.com/leemh-dev/dochagjari/actions/runs/43095641582)으로 요청했습니다. 20:32에는 queued/job0였으나 **20:35:56 UTC에 startup_failure**로 종료됐습니다. job0·artifact0이며 `--log-failed`도 log not found입니다. [대기 스냅샷](evidence/followup-actions-queue.json)과 [종료 증거](evidence/followup-actions-startup-failure.json)를 구분합니다. 이번 새 에뮬레이터 실행·다운로드·설치·SDK 조사는 없었습니다.
- GitHub CLI의 “workflow file issue일 가능성” 안내는 일반 힌트입니다. 공식 actionlint1.7.7로 이전 a020643 workflow와 현재 workflow 전체를 검사해 둘 다 exit0/진단0을 확인했습니다([검사 기록](evidence/workflow-static-check.txt)). 정적 검사가 실행 권한/스케줄링/런타임 성공을 보장하지 않으며 startup 원인은 아직 미확인입니다. 같은 부팅을 재요청하지 않았습니다.
- 별도 [준비 기록 회귀 재현](evidence/adb-stale-record-regression.txt)에서 실제 이전/현재 ui_probe Python 프로세스와 대역 ADB를 실행했습니다. 이전은 과거 true JSON만 보고 install을 시도했고, 수정 후는 get-state 실패에서 설치를 차단했습니다. 이 버그를 기존 Pleos 연결 실패의 원인으로 주장하지 않습니다. 새 CI는 아직 queued이므로 새 HEAD의 CI 성공으로 표시하지 않습니다.
- [공식 설정 비교](evidence/pleos-profile-comparison.json): devices.xml RAM4GiB/source.properties8192MB가 공존합니다. 원본 userdata 약1.1GB와 별도 빈 테스트 userdata2GiB도 다릅니다. 비교 부팅을 하지 않았고 메모리/데이터 차이를 원인으로 단정하지 않습니다.
- 이번에는 앱 코드를 바꾸지 않아 APK를 재빌드·재배포하지 않았습니다. v0.2.0 코드 `3a01515`·기존 artifact·아래 해시는 유지됩니다. SMS/수신자 설정·제출 키 환경 변수가 없고 server/.env도 없어 실제 SMS와 제출 서명은 실행하지 않았습니다.
- [제출 체크리스트](12-submission-checklist.md)와 README의 설치 전 자동 검사·ADB 상태별 재현 절차를 정리했습니다.

## v0.2.0 복구·설정 저장 보완

- 코드 커밋: `3a01515` (배포 빌드 기준), `5ef9aab` (프로세스 검증), `44f0cb4` (Android 프레임 호환), `702432c` (알림·저장·기기 CI), `6616f01` (서버 계약), `4a40a24` (수정 전 실패 재현).
- 현재 버전: **0.2.0 / 2**, API 34+, ARM64·x86_64. [개발용 패키징 artifact 실행](https://github.com/leemh-dev/dochagjari/actions/runs/43055801516/artifacts/13871881801)인 `development-release-assets-and-metadata`에 debug·기기 테스트·unsigned release와 metadata·SHA256SUMS를 보관합니다. 제출용 서명 release가 아닙니다. Release API는 Cloud 401·runner 403으로 게시가 제한되어 artifact를 제공합니다. [실패와 해결 증거](evidence/apk-publishing-limits.txt).
- [APK 메타데이터](evidence/v0.2-apk-metadata.json): 코드 커밋·버전·바이트 크기·각 해시·debug 서명 지문을 기록합니다. debug APK SHA-256은 `d1a2761d0dac84142b4ff8917f82d6737d3d4e1e30520dd8895e8454e4c85e4d`입니다.
- `:app:testDebugUnitTest :app:assembleDebugAndroidTest :app:lintDebug` — **33건, 실패 0건**. [요약](evidence/recovery-unit-tests/summary.json)과 같은 폴더의 JUnit XML을 제공합니다. Robolectric/API 34 사례는 Android 환경을 시뮬레이션한 JVM 검사이며 실제 기기 검사가 아닙니다.
- `python -m unittest discover -s server/tests -v` — **14건 통과**, [서버 로그](evidence/recovery-server-tests.txt). 공급자는 테스트 대역이며 실제 SENS 수락·문자 수신을 뜻하지 않습니다.
- `:app:assembleDebug :app:assembleRelease` — 코드 커밋 후 빌드 성공. debug·기기 테스트 APK 서명 검증 성공, unsigned release 생성. 게시 artifact를 다시 다운로드해 세 해시·APK 버전/ABI·개발 서명도 확인했습니다([재검증 기록](evidence/v0.2-downloaded-apk-verification.txt)). runner 서명은 기존 Cloud 키와 달라 새 테스트 AVD에서 설치합니다. 제출용 키는 제공되지 않았습니다.
- 이전 AppModel에서 확인 좌표 소실·ViewModel 종료 시 쓰기 취소 두 건을 먼저 재현했습니다. [수정 전 실패](evidence/restart-regression-before.xml)와 수정 후 회귀 검사를 구분해 보존합니다.
- 구현·검증한 복구 동작: 확정 요청/동의 복원, GET 선조회, 404에서 동일 내용 재개, 응답 유실 후 조회, 동시 복구, 모드·내용 불일치 차단, 관측한 기록 소실 시 중지, 동의 해제, 토큰 미저장, 구형 기록 보존, 저장 실패 시 미전송.
- 구현·검증한 프로필 동작: 저장 중/완료/실패, 순서 보장, 실패 후 재시도, ViewModel 종료 후 완료, 실제 DataStore를 닫고 새 인스턴스에서 완료 값 재조회. OS 프로세스 강제 종료 검증과는 별도로 기록합니다.
- 일반 Android API 34 Google APIs/KVM에서 `connectedDebugAndroidTest` **6건 통과**, 별도 프로세스 종료/복구 **2단계 통과**: [최신 검증 PR CI](https://github.com/leemh-dev/dochagjari/actions/runs/43057154160). 연결 명령에서 건너뛰는 단계 전용 2건은 별도 스크립트가 각각 실행했습니다. 이전 오류와 테스트 기대값 수정은 개발 일지에 보존합니다.
- 최초 Android 실행에서 영상 2건이 ARGB_8888 형식 오류로 실패했습니다. [실패 XML](evidence/android-device-before-bitmap-fix.xml)을 보존했고 프레임 변환 후 같은 앱의 TFLite·ML Kit **C12** 인식·영상 점유 기반 우회가 실제 Android에서 통과했습니다. 호스트 Tesseract 결과를 대체 근거로 쓰지 않았습니다.

### v0.2.0 일반 Android 실제 실행

[전체 통과 CI 43052357723](https://github.com/leemh-dev/dochagjari/actions/runs/43052357723), 코드 `5ef9aab`. 이후 설치 전 boot_completed/package/activity 준비 검사를 추가한 [CI 43054503112](https://github.com/leemh-dev/dochagjari/actions/runs/43054503112), 최신 문서 커밋 `8827d3c`의 [PR CI 43057154160](https://github.com/leemh-dev/dochagjari/actions/runs/43057154160)도 전체 성공했습니다. Google APIs API 34 x86_64/KVM이며 Pleos AVD가 아닙니다. [JUnit XML](evidence/android-api34-passed.xml), [프로세스 종료 로그](evidence/android-process-restart-passed.txt), [요약](evidence/android-api34-summary.json), [재실행 화면](evidence/android-api34-app.png)을 보존합니다.

| 기능 | 실제 실행한 범위 | 별도 미검증 |
| --- | --- | --- |
| 학원·우회 | Compose UI에서 픽업↔주차·이용 불가 우회, 앱 모델에서 점유→주차→후보 없음 | 순정 내비 목적지 핀 |
| 병원 | 같은 Android 앱 모델에서 휠체어·동행 후보 변경, 무단차 미확인 후보 제외 | 실제 병원 동선 |
| 영상 | 앱의 TFLite/ML Kit로 생성 빈/장애물·공개 영상 분석, **C12** 인식. 영상 URI 입력→점유→우회 | 실제 외부 카메라·실차. URI 검사는 공개 샘플을 캐시에 복사해 입력했으며 시스템 파일 선택 UI까지 자동화한 검사는 아님 |
| 저장 | 실제 DataStore 완료값 대기, 모델 재생성 및 프로세스 종료 후 좌우 옵션·유모차·휠체어·동행·높이 2.2 복원 | 저장 중 종료는 마지막 완료값까지만 보장. 저장 실패/연속 변경은 JVM 대역·실제 DataStore 회귀 검사로 별도 검증 |
| 알림 | 앱 모의 완료·저널 복원. 토큰 없는 SERVER_MOCK 확정 요청을 PID **7047→7098**에서 복원, 전체 요청 지문 동일 | Android에서 실제 서버·SENS 호출/휴대폰 수신. HTTP 복구/응답 유실/동시성은 JVM HTTP 대역·서버 테스트로 검증 |
| 주행 제한 | 테스트 신호의 주행/미확인에서 설정·위치·분석 변경 차단 | 실제 차량 속도·기어 신호 |
| 좌석 | 테스트 입력·프로필 저장 구현 및 일반 Android 실행 | Pleos 초기값·Console 좌우 변화·앱 수신·실차는 각각 별도 확인 |

연결 검사는 **6건 성공**, 단계 없는 프로세스 전용 2건은 skip한 뒤 별도 스크립트에서 **2단계 모두 성공**했습니다. `am force-stop` 전후 서로 다른 PID와 동일한 요청 SHA-256을 확인했으며, 이 시나리오에는 토큰이 없어 서버/공급자 호출은 없습니다. 알림 발송 성공으로 계산하지 않습니다.

복구 계약과 이전 앱/DB 호환 처리는 [자동 알림 복구 계약](11-arrival-recovery.md)을 참조하세요. **Pleos 내비 핀·Virtual Console 좌석 수신·실차·실제 휴대폰 수신은 현재도 미검증**입니다.

### v0.2.0 Pleos 별도 실행

기존 Cloud의 무가속 부팅 실패를 먼저 읽었고, 일반 Android CI에서 KVM 접근이 확인되어 **다른 실행 조건**으로 조사했습니다. [첫 KVM 증거 실행](https://github.com/leemh-dev/dochagjari/actions/runs/43052661853)은 KVM version 12 usable까지 확인한 뒤 공식 이미지 다운로드 8개 구간 중 7개를 완료했으나 남은 연결이 reset되어 종료됐습니다. **에뮬레이터는 시작하지 않았습니다.** [readiness JSON](evidence/pleos-kvm-download-failure/readiness.json), [앱 조사 미실행 JSON](evidence/pleos-kvm-download-failure/app-probe.json).

실패 원인이 구간 전송이므로 `77abd8e`에서 64MB 구간·동시 4개·제한된 재시도를 추가했습니다. [보완 실행 43056602693](https://github.com/leemh-dev/dochagjari/actions/runs/43056602693)의 결과:

- **전송·실행 확인:** 61/61 구간 완료, 공식 크기/체크섬 검증 후 추출·별도 테스트 데이터 포맷·KVM 에뮬레이터 시작. [단계 로그](evidence/pleos-kvm-readiness/transfer-and-boot-stages.txt).
- **부팅 로그 관찰:** Emulator 37.2.13이 `Boot completed in 31156 ms`를 출력했습니다. `ai.pleos.hardware.profile` native process crash도 관찰했습니다. 이것이 ADB 확인 실패의 원인인지는 확인하지 못했습니다. [선별 부팅 로그](evidence/pleos-kvm-readiness/boot-diagnostics.txt).
- **ADB 준비 미확인:** 900초 동안 `sys.boot_completed=1`·package/activity 서비스 준비를 확인하지 못했고 마지막 stdout은 비어 있었습니다. 이 버전의 진단은 ADB returncode/stderr/devices 목록을 누락했으므로 **프레임워크 미준비와 ADB 연결 실패를 구분할 수 없습니다.** [readiness JSON](evidence/pleos-kvm-readiness/readiness.json).
- **설치·연동 미실행:** APK 설치·Pleos 기기 검사·SDK 목적지 요청·실제 내비 핀·Console 조작·앱 좌석 초기값/변경/옵션 제안은 실행하지 않았습니다. [조사 생략 기록](evidence/pleos-kvm-readiness/app-probe.json). 일반 Android 결과로 대체하지 않습니다.
- **후속 진단 보완:** 준비 검사에 ADB returncode/stderr·devices 목록을 추가하고 포트 5554를 명시했습니다. 이 보완 후 추가 부팅은 실행하지 않았습니다. 반복 자동 실행을 막기 위해 Pleos workflow는 수동 실행으로 전환했습니다.
- **환경 차이:** 이번 AVD는 4GB/2core/SwiftShader·별도 빈 테스트 데이터입니다. 내려받은 Linux 이미지 source.properties의 RAM 기본값은 8192MB였습니다. 원인으로 단정하지 않고 공식 기본 프로필·원본 데이터 구성을 확인할 후속 항목으로 남깁니다.

맥북에 접근할 수 없어 실행하지 않았습니다. [README](../README.md)의 공식 ARM64 AVD에서 실제 부팅 속성·ADB 오류/devices·두 서비스를 확인한 뒤 설치/내비/좌석 검증을 이어가야 합니다. 실차·실제 문자 수신·제출 서명은 여전히 미검증입니다.

## v0.1.0 최초 코드와 APK (이전 이력)

아래 표와 실행 기록은 첫 APK 개발 당시의 이력입니다. v0.2.0의 최신 확인 범위는 위 절과 이후 기기 결과를 우선합니다.

- Android 구현: `0a04f40` (`feat/android-submission-mvp`). 이후 변경은 서버·호스트 검증·문서입니다.
- SMS 서버: `ae33591`. 호스트 미디어 검증: `0872124`.
- 개발 APK: [`apk/dochagjari-debug.apk`](../apk/dochagjari-debug.apk). 개발 서명이며 제출용 release 서명 APK가 아닙니다.
- debug APK SHA-256: `c72bc9d9893fd64d23942fb7b751ef7269d870962ce55f9add6d84f3541c6ae4`.
- unsigned release SHA-256: `6536145a8248777a58d1fe4871f36b9db7c309b830ea767ce07015f4e489703e` (로컬 빌드 산출물, 미서명).
- 실행 대상: API 34+, `arm64-v8a`와 `x86_64`. 앱 ID `dev.leemh.dochagjari`, 버전 `0.1.0` / `1`.
- unsigned release는 `:app:assembleRelease`로 생성했습니다. 제출용 키가 제공되지 않아 서명된 release·새 AVD 설치 검증은 남아 있습니다.

## v0.1 당시 검증 수준 (이전 이력)

| 기능 | 이번에 확인한 내용 | 실제 연동·기기 검증 |
| --- | --- | --- |
| 학원 픽업·우회 | 추천 단위 테스트: 같은 문에서 모드별 도착점 변경, 점유·이용 불가 우회, 후보 없음 | APK 빌드 완료. 앱 화면 기기 테스트는 아래 Cloud 결과 참조 |
| NaviHelper | 공식 샘플 및 도착자리 SDK 포함 APK 빌드. 실제·모의 요청 경로 구현 | 목적지 두 곳의 실제 결과 이벤트·순정 내비 핀은 별도 기기 검증 필요 |
| 좌석 | Pleos Vehicle 2.3.2의 capability, 좌우 초기값·변경 구독 구현. 테스트 입력·권한 없음·미지원 흐름 구현 | Virtual Console 조작과 앱 수신은 아직 완료 증거 없음. 실차 미검증 |
| 설정 저장 | DataStore 프로필·차량 높이 저장 구현, 재시작 기기 테스트 APK 빌드 | 저장·재시작 테스트는 기기 실행 필요 |
| 병원 접근성 | 동행 시 하차 분리 제외, 무단차 미확인 제외, 높이·시간 조건을 단위 테스트로 확인 | 가상 병원 시연. 용인세브란스 전체 무단차·하차 허용은 미검증 |
| 영상 | 호스트 CPU에서 동봉 실제 모델로 공개 보행자 영상 점유 확인. 생성 빈 구간·장애물 입력으로 결과 변경, 별도 Tesseract ROI에서 C12 인식 | Android Task Vision·ML Kit 파이프라인 구현 및 테스트 APK 빌드. 앱 내 실행은 별도 기기 검증 필요. 순정 외부 카메라 미연동 |
| 자동 알림 | 서버 테스트 8건: 인증·수신자 제한·정차와 확인 조건·동시 중복·재시작·타임아웃·공급자 거부·일일 제한 | 모의 공급자 사용. 실제 SENS 자격증명·동의 수신자 미제공으로 공급자 수락·휴대폰 수신 미검증 |
| 공유 | 위치·단서 QR와 Android 수동 공유 구현 | 휴대폰 QR·카카오톡 대상 선택·수신은 미검증 |

**빌드 성공, 테스트 입력 검증, AVD 이벤트 검증, 실차·휴대폰 수신은 서로 다른 결과입니다.**

## 실행한 검사

Cloud: Linux x86_64, JDK 17, Android SDK 35 / Build Tools 35.0.0, Gradle 8.10.2. 원격 의존성 다운로드는 환경 프록시와 CA 신뢰를 사용했습니다.

```text
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest \
  :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
BUILD SUCCESSFUL in 2m 10s
129 actionable tasks: 34 executed, 95 up-to-date

python -m unittest discover -s server/tests -v
Ran 8 tests · OK

공식 Pleos 내비 샘플 :app:assembleDebug
BUILD SUCCESSFUL in 4m 27s
```

- Android 단위 테스트 **9건, 실패 0건**: [JUnit XML](evidence/android-unit-tests.xml).
- lint 오류 **0건**. API 34 대상·고정 의존성 버전에 대한 경고는 유지합니다. min/target은 이번 Pleos API 34 검증 범위에 맞췄습니다.
- `apksigner verify --print-certs`: debug APK 서명 검증 성공, `CN=Android Debug`.
- GitHub Actions도 [push 검사](https://github.com/leemh-dev/dochagjari/actions/runs/43032847880)와 [PR 검사](https://github.com/leemh-dev/dochagjari/actions/runs/43032862081)가 모두 성공했습니다. 검증 커밋: `2413d22`.
- Android 기기 테스트는 추천 UI, 모델·OCR, DataStore·알림 저널 재시작을 포함합니다. **테스트 APK 생성은 기기 실행 성공을 뜻하지 않습니다.**

호스트 입력 검증:

```bash
pip install -r scripts/media-requirements.txt
python scripts/verify_media.py
```

[실제 실행 JSON](evidence/host-media.json): 빈 구간 `EMPTY`, 생성 장애물 `OCCUPIED`, 공개 장애물 `OCCUPIED` (프레임 점수 0.816 / 0.758 / 0.668 / 0.723). 생성 표지는 호스트 Tesseract의 등록 ROI에서 `C12`로 인식했습니다. Android는 ML Kit를 사용하므로 이 결과를 Android OCR 성공으로 대체하지 않습니다.

## Cloud Pleos 실행

- 공식 Linux 이미지: Pleos Connect **2.1.0**, API **34**, x86_64.
- 공식 프로필: **2560 × 1440**, 160dpi, RAM 4GB.
- `/dev/kvm` 없음. [가속 검사](evidence/cloud-acceleration.txt): `KVM requires a CPU that supports vmx or svm`.
- Emulator 37.2.13에서 `-accel off`, SwiftShader로 소프트웨어 부팅을 시도했습니다. ADB 연결은 관찰했지만 부팅 완료로 판단하지 않았습니다.
- 수동 구성한 Cloud AVD의 연장 부팅에서 `/data/misc` 암호화 정책 설정 실패와 recovery 재부팅이 나타났습니다. 공식 Emulator **35.2.10**과 초기화한 개발 AVD에서도 같은 오류를 확인했습니다. 원인을 앱 오류로 단정하지 않습니다.
- SDK 이미지를 변경하지 않고 별도의 빈 개발 데이터 파티션으로 다시 시도했을 때 앞선 암호화 오류는 나타나지 않았습니다. SurfaceFlinger·zygote·system_server 시작과 ADB 연결은 확인했으나, `package`와 `activity` 서비스가 `not found`였고 `sys.boot_completed=1`은 확인하지 못했습니다. 일부 Pleos vendor 서비스의 반복 종료도 관찰했습니다.
- 따라서 **앱 설치·실행, Android 기기 테스트, 순정 내비 핀, Virtual Console 좌석 토글 및 앱 수신은 미검증**입니다. ADB의 `device` 표시를 부팅 완료나 앱 설치 성공으로 처리하지 않았습니다. [부팅 진단 요약](evidence/cloud-boot.txt)을 남깁니다.

맥북 M1 재현 명령, ARM64 이미지 설치와 두 목적지·좌우 좌석 확인 순서는 [README](../README.md)에 있습니다.

## 제출 전 남은 확인

- Pleos AVD에서 공식 샘플과 앱을 설치해 목적지 두 곳의 실제 핀을 비교합니다.
- Virtual Console 좌우 좌석 변경과 앱 콜백을 함께 녹화하고, 저장 옵션을 재실행으로 확인합니다.
- 일반 Android의 실제 영상·ML Kit OCR은 v0.2에서 통과했습니다. Pleos 순정 카메라·실차 검증은 남아 있습니다.
- 실제 SMS는 SENS 등록 발신번호·동의 수신자를 설정한 뒤 공급자 수락과 휴대폰 수신을 각각 확인합니다.
- 실제 시설의 미확인 동선은 계속 가상 데이터와 구분하고 제출용 키로 release APK를 서명합니다.
