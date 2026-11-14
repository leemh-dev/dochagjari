# Cloud 개발과 Mac Pleos 검증 · 단계별 실행

2026-11-14 UTC. 사용자가 Cloud 개발과 맥북 에뮬레이터 확인을 개발 단계마다 구분하도록 요청했다. 현행 브랜치 `feat/arrival-context-ux`/Draft PR#2, 기능 설계는 docs33/31, 실행 순서는 docs34를 따른다. 이번에는 절차/프롬프트를 정리했으며 새 앱 구현·Mac 접속·에뮬레이터 실행·검사를 수행하지 않았다.

## 준비 상태

새 브랜치·최신 설계·시안 보존·코드 정리 기준은 준비돼 있다. **옛 앱 코드를 모두 제거한 상태는 아니다.** [정리 기준35](35-code-retention-and-cleanup.md)에 따라 필요한 저장/복구 기반을 재사용하고 대체 구현/검사 후 불필요한 옛 화면을 제거한다.

현재 앱 기준은0.9.0/9·e2b93ba이며 새 다중프리셋/이번 구성/3D패널은 아직 미구현이다. [과거 Mac 기록](08-verification.md)은 M1/8GiB·Pleos v2.1.0 ARM64의 부팅/앱표시·로컬개발서명 app/test 검사6/6 성공을 담고 있다. 당시 메모리압력은 경고, swap은 약3.6~3.9GiB였다. 이전0.2배포/당시로컬빌드의 결과를 신규브랜치/앱0.9이상의 새 성공으로 세지 않는다. 실제 음성/순정핀/Console좌석수신·실차의 성공도 아니다.

[새 단계의 상태표](evidence/implementation-restart/stage-status.json)는 준비 완료/1단계 구현 전으로 시작한다. 새 기능 단계의 Cloud/Mac 통과 기록은 아직 없다. 실제 실행 후 해당 코드/산출물과 결과를 갱신한다.

## 두 환경의 역할

| 환경 | 맡을 일 | 결과를 넘기는 단위 |
| --- | --- | --- |
| Cloud/CI | Kotlin 구현·데이터이행·단위/통합검사·lint·APK빌드·가능한 일반Android 기기검사 | 코드커밋/버전·같은실행의app/test APK·SHA256/인증서·검사보고서 |
| Mac M1/8GB + 외장SSD | 전달APK 식별·공식ARM64 Pleos 부팅/서비스준비·설치·화면/SDK/Console·원본캡처 | 같은커밋/산출물과기기정보·readiness·기능별실제결과·화면/로그 |
| GitHub PR#2 | 변경·실제검사·차단·남은항목·다음단계 기록 | docs08/10/12/18·정제증거/artifact·새다음프롬프트 |

Cloud에 KVM/기기가 없으면 일반Android 기기검사는 가능한 CI에서 수행한다. Pleos 무가속 부팅을 반복하지 않는다. Mac이 꺼져 있거나 연결되지 않으면 해당 기기 확인만 `차단/미검증`으로 두고 독립 가능한 Cloud 코드는 계속한다. 기기 결과가 필요한 SDK/렌더링 가정은 미확인으로 유지하고 TEST 어댑터로 진행한다.

## 새 개발 단계 · Cloud와 Mac의 서로 다른 완료 기준

| 단계 | Cloud 구현/검사 | Mac Pleos 확인 | 다음 단계로 넘길 결과 |
| --- | --- | --- | --- |
| 0 준비 | 기준커밋/기존데이터/시안22개·최신6개·현재지시 확인 | 연결 여부·SSD/메모리·기존Mac실적 확인 | 준비는완료, 새기기실행은별도 |
| 1 저장/이행 | 프리셋·이번구성·관측분리, 옛설정이행·실제저장재조회/실패/연속쓰기·종료복원 | 동일 APK의저장/재실행 확인. 화면전이면진단진입점또는instrumentation 사용 | 코드/보고서/APK, 기존설정/방문/알림보존 |
| 2 독립탑승UI | 목적지없는프리셋추가/선택/이번만변경/건너뛰기·저장상태·주행제한 | 혼자/아이/가족전환·창크기/글자/터치·재실행저장값 | 최초/전환/저장·복원 실제캡처 |
| 3 입력/상황인지 | 텍스트명령·TEST점유·모호함/충돌/미지원·오래됨 처리, SDK연결은지원확인후 | Virtual Console뒤좌우Empty→Occupied→Empty, 초기값/콜백/제안; 음성은오디오/STT연결가능시별도 | TEST/실제SDK수신/STT를나눈결과 |
| 4 도착패널/지도 | 앱영역의상단영상/하단3D지도·파란선·닫기/주행숨김·일반Android검사 | 순정내비와앱실제분할·패널크기/성능·재생·닫기 확인 | 일반Android/Pleos 원본을분리 |
| 5 같은방문흐름 | 에뜨와요청→내비요청→명시입차→관측→주차저장/닫기→귀환, 병원/픽업조건 | SDK요청/콜백/순정핀각각대조, 같은방문저장/재시작, 확인된조건의차이 | 핀/입차·위치/후보·모의/실제의별도증거 |
| 6 회귀/시연 | 최종변경회귀·APK식별·모션샘플/본편·실행영상 편집 | 가능한환경에서최종같은APK연속녹화, Mac미연결이면일반Android로표시 | 재생가능영상/자막·원본·커밋/해시·미검증목록 |

각 단계는 `Cloud: 대기/진행/통과/실패/차단`, `Mac: 대기/진행/통과/실패/차단/미검증`으로 따로 기록한다. Cloud 통과를 Pleos 완료로 합치지 않는다. Mac이 막혀도 독립 다음코드는 진행할 수 있지만 미확인 연동을 전제로 한 확정안내는 만들지 않는다.

## 기능 단위의 반복 절차

1. **시작:** 현재단계·이번코드범위·Cloud/Mac 각각의상태·마지막검증커밋을 알린다. 기존원격/로컬변경을 확인하고 같은기능을 다시 구현하지 않는다.
2. **Cloud 구현:** 필요한 코드만 바꾸고 의미있는저장/회귀/경계검사를 수행한다. 이전화면제거는 호출전환/이행후 같은단위에 포함한다.
3. **기기 전달:** 녹화/설치할 특정커밋과 app/test APK세트를 고른다. 같은소스라도runner/로컬재빌드의bytes/개발인증서가같다고가정하지않는다. 버전·SHA256·인증서·일반Android검사결과·Mac에서확인할항목을 전달한다.
4. **Mac 사전확인:** 실제연결/외장SSD경로/호스트메모리/AVD/ADB와서비스준비를 확인한다. 설치직전에다시readiness를통과해야한다. 기존사용자AVD/서버DB를초기화하지않는다.
5. **Mac 실행/증거:** 설치/실행·단계기능·가능한기기검사를 수행하고 새캡처를 확보즉시채팅에표시한다. 결과를기준으로재현오류부터수정한다. 환경실패를기능실패로세지않는다.
6. **마감:** 기능커밋/푸시·PR2·docs10/08/12/18·증거를갱신한다. Cloud/Mac의결과·남은항목·다음단계를구분한다. 미검증인SDK/카메라/음성/SMS를완료로쓰지않는다.

## Mac 자원/저장 정책

Cloud는 소스 구현/빌드 기준을, Mac은 전달받은 특정 커밋의 기기 확인을 맡는다. 예를 들어 Cloud가2단계B커밋을 구현하는 동안 Mac이1단계A커밋을 검사하면 그 상태를 그대로 보고한다. A의 기기 성공을 B의 성공으로 붙이지 않는다.

Mac Codex는 기본적으로 앱 소스를 임의 수정하지 않고 단계별 증거/결과를 기록한다. 앱 오류가 발견되면 재현 입력/원본을 Cloud에 전달해 수정한다. 두 환경에서 같은 브랜치의 작업이 갈라지면 사용자 변경을 reset/강제 push로 없애지 말고 서로의 커밋을 확인해 보존하며 정리한다. 재시작 때 원격 최신 상태와 설치 APK의 소스 커밋을 모두 확인한다.

- 저장소·SDK·AVD·Gradle캐시·다운로드·임시파일·검증자료는 연결된 외장SSD의전용개발디렉터리에둔다. 기존SSD체크아웃을사용하고중복clone/이미지다운로드를먼저하지않는다.
- [macos-ssd-env.sh](../scripts/pleos/macos-ssd-env.sh)의실제볼륨/경로검사를재사용한다. SSD가빠졌거나경로가다르면내장스토리지로조용히대체하지않는다. 개인로컬경로를공개기록에넣지않는다.
- 에뮬레이터한대만실행하고 **Cloud빌드APK설치·확인을기본**으로한다. Mac빌드가필요하면에뮬레이터를종료하고기존실측구성처럼worker1/heap1536MiB로작업한후다시기기를연다. OOM/메모리경고를무시하고빌드·렌더러·AVD를동시에반복하지않는다.
- 공식AVD프로필/이미지를기준으로실제환경을기록한다. RAM/userdata를원인으로단정하거나호스트상태없이임의증설하지않는다.
- 과거CoreAudio문제조사용 `-no-audio` 실행에서는음성을검증할수없다. 음성단계는오디오를포함한안정된실행/권한/STT를확인하고,안되면텍스트주입만검증한것으로기록한다.

## Mac 인수인계 명령 예시

다음은 **Mac에서 향후 실행할 예시**이며 이 Cloud 작업에서 실행한 결과가 아니다. 실제SSD/AVD/시리얼/APK경로로바꾸고로컬변경을먼저확인한다. 정상연결이없으면맥북실행을했다고표시하지않는다. 기존체크아웃은 `git status --short` 확인후 fetch/브랜치전환/fast-forward만수행하며reset/stash/삭제로사용자변경을없애지않는다.

저장소를기존SSD개발폴더아래에서연다. 경로구성이기존실행기와같으면실행기가SDK/AVD/Gradle/TMPDIR을설정한다.

```bash
bash scripts/pleos/macos-ssd-env.sh emulator -list-avds
bash scripts/pleos/macos-ssd-env.sh adb devices -l
df -h .
memory_pressure -Q
sysctl vm.swapusage
```

AVD가이미실행중이면새로띄우지않는다. 부팅명령/메모리진단은 README의기존Mac절차를참고한다. `DOCHAGJARI_SERIAL`은위목록에서확인한시리얼, `DOCHAGJARI_APK`는해시/인증서대조를마친특정실행의debug APK절대경로다. 아래명령은대상AVD가데이터보존대상인지먼저확인한뒤사용한다.

```bash
shasum -a 256 "$DOCHAGJARI_APK"
bash scripts/pleos/macos-ssd-env.sh python3 scripts/pleos/adb_readiness.py \
  --serial "$DOCHAGJARI_SERIAL" --output /tmp/dochagjari-mac-preinstall.json && \
bash scripts/pleos/macos-ssd-env.sh adb -s "$DOCHAGJARI_SERIAL" install -r "$DOCHAGJARI_APK" && \
bash scripts/pleos/macos-ssd-env.sh adb -s "$DOCHAGJARI_SERIAL" shell am start \
  -n dev.leemh.dochagjari/.MainActivity
```

readiness는현재transport=device/boot1/package·activity found·각rc0/오류를실제확인한다. 옛JSON이나Boot completed로그만으로통과하지않는다. unauthorized는정상AVD승인절차만사용하고키복사/편집·인증우회·Always allow선택을하지않는다. 앱설치가성공해도동일사용자/실제창/앱표시를따로확인한다.

호스트인증서/기존앱서명이다르면기존앱을지우지말고별도전용테스트AVD를쓴다. 같은인증서의설정이행확인은전용기기에마련한옛버전fixture에서수행한다. Cloud/CI개발키를Mac에복사해서인증서를맞추지않는다.

화면캡처예시:

```bash
bash scripts/pleos/macos-ssd-env.sh adb -s "$DOCHAGJARI_SERIAL" \
  exec-out screencap -p > /tmp/dochagjari-mac-screen.png
```

AVD의차량정보/내비영역인지우리앱영역인지,일반Android/Pleos·TEST/모의/실제SDK인지캡션에표시한다. 개인정보/비밀값이보이는화면은공개전정제한다. 기기전환/오류/수정전후화면을연속보관한다.

## 기기검사 방식과 결과명

- **Cloud/CI 일반Android:** 가능한runner에서 `connectedDebugAndroidTest` 및해당단위기기검사를실행하고XML의pass/fail/skip을확인한다.
- **Mac 표준ADB검사:** Cloud에서받은같은실행의app/test APK쌍을식별한뒤전용테스트AVD에서실행할수있다. 현재선택검사/인자/사용자·대상클래스를확인하고신규단위검사가포함됐는지검토한다. 기존direct_instrumentation.py는기존6건의AppInstrumentedTest를대상으로하므로새프리셋전체검사로간주하지않는다.
- **Mac Gradle connected:** 자원/서명/로컬빌드가준비된경우전용AVD에서SSD실행기를통해 `./gradlew --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx1536m :app:connectedDebugAndroidTest`를실행한다. 실제실행하지않으면통과로쓰지않고,호스트메모리/설치계층실패와앱assert실패를나눈다. Cloud서명APK에로컬키test APK를섞지않는다.
- 표준ADB instrumentation의통과를Gradle connected통과로이름바꾸지않는다. 종료/새PID복원은별도준비/복원과실제저장재조회로확인한다. 데이터초기화가있는검사스크립트는fresh-test guard와새전용AVD에서만수행하며이문서에기존기기초기화명령을추가하지않는다.

## 진행 보고와 저장할 증거

Cloud→Mac 전달에는 최소한 **단계명, 소스 커밋, 앱버전, app/test APK 경로·SHA256·인증서, Cloud 검사 결과, Mac 확인 항목, 알려진 미검증**을 포함한다. Mac→Cloud 회신에는 같은 식별값과 실제 실행환경·readiness·설치/실행/검사 결과·새 화면·실패 재현·남은 작업을 넣는다. 단순히 ‘최신 APK’라고 전달하지 않는다.

보고 형식:

> 현재 단계: 2 독립 탑승 UI. Cloud: 저장/이행 검사 통과, UI 진행. Mac: 연결되지 않아 대기. 다음: 같은커밋 APK 전달 후 Pleos 전환/복원 확인.

중요결과/단계전환·긴실행중약1분마다실제상태/차단/다음을공유한다. 기기화면은최초/주요전환/오류·수정후확보즉시채팅에직접표시한다. CI에서종료후artifact만제공되면그제약과확보시점을알리고받은즉시보여준다. 근거없는진행률%나과거캡처의새실행표시는사용하지않는다.

각단위증거는 `docs/evidence/<기능>/cloud/`, `mac/`, `apk-metadata.json` 또는artifact의대응폴더로분리할계획이다. 날짜/호스트환경·코드커밋/앱버전·실제명령/rc·AVD/사용자·readiness·APK해시/인증서·pass/fail/skip·사진/로그·미검증·다음작업을기록한다. 이폴더구조는계획이며새실행파일이생겼다는뜻이아니다.

첫실제코드단위는 **1 저장/이행**이다. Mac이연결될때까지코드개발을멈추지않고,확보된기기의결과만해당단계검증에추가한다.

## Mac Codex에 넘길 실행 프롬프트

아래의 단계/커밋/산출물은 Cloud가 실제 결과를 채워 전달한다. 비워 둔 값이나 예시를 실행 완료로 간주하지 않는다.

```text
도착자리의 전달받은 단계/커밋 APK를 맥북 Pleos에서 확인해줘.
AGENTS.md·README·docs36/34/33·docs08의 기존Mac기록과 PR #2를 먼저 읽어.
대상 단계: <Cloud가 전달한 단계>
소스 커밋/앱버전: <실제 값>
app/test APK와 metadata/artifact: <실제 경로/링크·SHA256·인증서>
확인 항목: <이번 단계의 Mac 확인 목록>
기존 Mac M1/8GB·외장SSD 체크아웃/SDK/AVD/캐시를 사용하고 중복 clone/다운로드는 피해야 해.
접근 가능한 기기/메모리압력/여유공간을 먼저 확인하고 AVD 한 대만 실행해.
기존 Mac실적은 과거 결과로 보존하고 이번 대상 APK의 새 검증으로 세지 마.
현재 transport/boot/package/activity와 각각rc0을 확인하고 설치 직전에 다시 확인해.
기존 사용자 앱/AVD 데이터를 삭제하거나 키를 복사/편집·인증을 우회하지 마.
Cloud빌드 app/test는 같은 실행/인증서 쌍을 쓰고 다른 인증서면 새 전용테스트AVD를 사용해.
Mac에서 빌드가 필요하면 에뮬레이터를 종료하고 SSD/worker1/heap1536MiB 방식을 사용해.
실제 설치/앱 표시/단계 기능·가능한 기기검사를 나눠 확인하고 초기/전환/오류·수정후 화면을 즉시 채팅에 보여줘.
표준 ADB검사와 Gradle connected, TEST점유와 Console SDK수신, 실제STT와 텍스트주입을 구분해.
-no-audio로 실행했다면 실제 음성 성공이라고 쓰지 마.
앱 소스는 임의 수정하지 말고 오류 재현/화면/로그를 기록해 Cloud 구현에 전달해.
단계/소스커밋/버전/해시·환경/실제명령과rc·통과/실패/차단/미검증을 증거폴더·docs10/08·PR #2에 남겨.
다른 환경의 커밋과 사용자 변경을 덮어쓰거나 자동 병합하지 마.
```
