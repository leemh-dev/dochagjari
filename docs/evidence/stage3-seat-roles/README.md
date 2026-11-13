# 3단계 · 선택적 좌석별 탑승 역할

2026-11-13 UTC · 앱0.5.0/5 · 초기기능c98630d / 저장완료제안수정a8e440b. [설계·보존 기준](../../20-optional-seat-roles.md).

## 로컬 실제 검사

[검사 JSON](local-checks.json) · [첫 빌드](local-first-build.txt) · [컴파일 수정 후 잔여 빌드](local-build-fix.txt) · [저장완료 제안 보강 전체 빌드](local-suggestion-ack.txt) · [강화한 경계3건](local-ack-boundary.txt).

초기JVM51/실패0·lint/debug통과후기기테스트nullable smart-cast컴파일오류를non-null로컬참조로고쳐테스트APK/release빌드1m28s통과. 이후제안에draft가쓰일수있는코드경로를검토에서확인해최신SAVED/revision/persisted로제한했다. 보강후JVM52/실패0·lint/debug/test/unsigned release2m33s통과,강화한경계3건만추가21s통과. 실제기기실패와코드검토발견을구분한다.

실제DataStore옛파일/키이행·관계없는키보존·저장직후store종료/새인스턴스복원·unknown enum·주입write예외/이전실제파일유지·재시도,순차/연속저장·점유/역할경계를검사했다. 주입된예외는물리디스크고장/용량부족의실측주장이아니다.

## APK 독립 대조

[로컬 metadata](local-apk-metadata.json) · [로컬 SHA256SUMS](local-SHA256SUMS) · [CI metadata](ci-apk-metadata.json) · [CI SHA256SUMS](ci-SHA256SUMS) · [세 파일 독립비교](ci-apk-comparison.json).

최신CI43366222134의APKartifact14010825928을정상연결된GitHub앱파일참조로내려받아세APK의bytes hash/aapt manifest/apksigner인증서·미서명을각각대조했다. CI는PR임시병합1550a11을빌드했으며실제tree7583555febf511e0443f0199ee9c7847e9edfa91이head a8e440b와같다. 실제PR병합은하지않았다. 로컬/CI/Mac키·hash를혼동하지않고새전용AVD를사용한다. release는미서명이다.

초기c98630d CI43364708532/APKartifact14008949223의metadata는[별도 보관](initial-ci-apk-metadata.json)한다. 기존0.4/0.3/0.2보관본을덮어쓰지않는다.

## 실제 기기 결과 구분

초기CI43364708532 verify/android-device모두success. [초기 실제 화면/요약](initial-c986/README.md),전체25장/원본JUnit/HTTP복구는artifact14011310449에있다. 기기JUnit11통과·프로세스전용2skip후별도준비/종료/검증2단계PASS다. 새역할과기존모든조건/확정요청fingerprint를새PID에서확인했다. 초기22/24/25앱화면을채팅에서직접열었다. 일반Android/테스트입력이며Pleos/Console/실차가아니다.

[원본 CI JVM52 보고서](ci-jvm-index.html)와[verify 로그 발췌](ci-verify-summary.txt)를 실제로 내려받아 확인했다. 최신a8e440b/CI43366222134의verify/android-device모두성공했다. 아래는이최신실행의원본을실제로열어확인한결과다.

## 최종 최신 일반 Android 검증 — 통과

[CI43366222134](https://github.com/leemh-dev/dochagjari/actions/runs/43366222134) · [job/step/artifact 결론](ci-result.json) · [기기 JUnit XML](device-instrumented-results.xml) · [프로세스 복원 PASS](device-process-restart-summary.txt) · [UI 원본 요약](device-ui-summary.json) · [실제 HTTP 모의 복구](device-http-summary.json).

- JUnit **11통과/실패0**, 별도프로세스전용2메서드는일괄실행에서skip후준비/앱종료/새PID복원2단계PASS다. 기존기능6+매장1+지도2+신규좌석2를포함한다. 콘솔Finished숫자를모두실행통과로세지않는다.
- 역할/카시트선택→저장완료/저장값재조회→건너뛰기·기존6조건/방문/알림요청보존→Activity재시작/지도안내가능을검증했다. 테스트점유로역할이바뀌지않고주행/unknown에서쓰기차단도통과했다.
- 별도프로세스검사는모든이동조건/좌우역할/카시트와동일확정요청fingerprint를새PID에서확인했다. 저장직후종료/새store복원과쓰기실패·재시도·연속변경은실제DataStore를사용한단위/주입검사와구분한다.
- 기존UI25장전체·방문새PID복원·앱내영상EMPTY/OCCUPIED/C12/우회와HTTP SERVER_MOCK복구도이번최신실행에서성공했다. HTTP는응답보류→동일DB서버재시작/빈토큰→같은세션GET200, POST총1/공급자0이다. 실제SENS수락/휴대폰수신아니다.

### 최신 실제 앱 화면

[22 건너뛰기](22-seat-roles-skipped.png) · [23 사용자 지정 역할/카시트](23-seat-roles-configured.png) · [24 테스트 점유의 저장값 확인 제안](24-seat-occupancy-suggestion.png) · [25 앱 종료/재시작 후 유지](25-seat-roles-restarted.png). PNG/XML원본과[이미지 SHA-256/출처](device-image-hashes.json)를보관했다. 이4장을채팅에서확보즉시직접열었다.

위4장은 **최신a8e440b의실제일반Android API34 앱 캡처/테스트입력**이다. 초기c986이미지/호스트/웹/Pleos/실차와구분한다. 전체25장·JUnit/로그는 [기기artifact14010628050](https://github.com/leemh-dev/dochagjari/actions/runs/43366222134/artifacts/14010628050),설치가능한개발APK/metadata는 [APKartifact14010825928](https://github.com/leemh-dev/dochagjari/actions/runs/43366222134/artifacts/14010825928)다.

실제Pleos SDK/Console·실차,기존Mac앱에대한실제업그레이드설치,순정외부카메라/실내측위,실제SMS수신은이번검사로확인하지않았다. 기존DataStore형식의이행은옛키를가진실제파일단위검사이며실제사용자앱데이터를삭제/교체하지않았다. AI자료보완worker와내비/입차·주차귀환은다음단계다.
