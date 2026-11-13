# 6단계 증거 · 주차 완료·저장/요약·귀환

2026-11-13 UTC · 앱 **0.8.0/8·84acdd3**. [계약](../../23-parking-summary-and-return.md) · [CI43404749340](https://github.com/leemh-dev/dochagjari/actions/runs/43404749340) **verify/android-device 모두 success**.

## 원본 검사와 실제 범위

- 원본 JVM **99/실패0/ignored0**, 신규17건(규칙/실제SQLite12·AppModel5). 서버14/진단35·lint/debug/test/unsigned release PASS. [CI 요약](ci-verify.json)/[정제로그](ci-verify-summary.txt) · [로컬 검사](local-checks.json). 최종앱소스JVM98/lint/세APK2m21s 후 테스트만1건 추가해 JVM99를48s로확인했고앱코드가같아로컬lint/APK는반복하지않았다. CI에서는새전체소스99건/빌드5m35s를확인했다.
- 실제 기기 XML **12pass/실패0**, 프로세스전용2메서드는일괄에서skip 후 별도종료/복원2단계PASS. [기기요약](ci-device-checks.json)/[정제로그](ci-device-summary.txt). 콘솔 Finished 수를전부pass로세지않는다.
- 확장VisitRoad한시나리오에서기존내비/입차/영상관측을유지하며8초자동요약닫기·중복완료재열림없음·관측자동승격없음·TEST/USER단서/연속변경/실제SQLite재조회·귀환·Activity재시작/주행·새완료/명시재개를확인했다. 설정/좌석·알림/방문/자료ID는동일했다. 쓰기실패/ACK·재조회유실/저장중이동/유실후이동/쓰기대기중새입력은주입소스JVM검사이며실제기기디스크장애를실측한것이아니다.
- 별도프로세스 **7411→7464**에서동일방문/내비요청/관측/주차/단서ID·TEST출처/시각과기존프로필/역할·동일확정알림을복원했다. 요약/지도/내비/관측자동재개없음. [프로세스원본](ci-process-restart.txt). 명시TEST완료/수동단서fixture이며실차주차/카메라활성중강제종료성공이아니다.
- UI는새주차흐름 **14장**이다. 이전37장전체를이번에재실행한것으로세지않는다. **7528→8964**의새PID에서도같은방문/PARKED·HIDDEN·USER수동메모를복원하고화면/관측을자동열지않았다. [UI trace](ci-ui-summary.json). 입력은검증용manual-demo이며현장확인이아니다.
- connected와명시설치직전공통helper로transport=device/boot1/package/activity found·각rc0/빈stderr를확인했다. [connected직전](pre-connected-readiness.json) · [설치직전](pre-install-readiness.json).
- [HTTP SERVER_MOCK](ci-http-recovery-summary.json) PASS: 응답보류→앱종료/같은DB서버재시작/빈토큰→같은세션GET200·POST1/공급자0. DB삭제/새세션재발송없음. 실제SENS수락/휴대폰수신이아니다.

## 실제 앱 화면

이번CI의일반Android API34/새전용AVD이며Pleos/실차가아니다. TEST차량/입차/관측/주차,MOCK내비,검증용수동메모다. PNG/XML은원본artifact에서복사했다. [파일SHA256·출처](screenshot-provenance.json) · [전체기기 artifact14028535017](https://github.com/leemh-dev/dochagjari/actions/runs/43404749340/artifacts/14028535017).

| 화면 | 확인/제약 |
| --- | --- |
| [01 최초앱](01-start.png) | v0.8.0/테스트차량·가상학원 |
| [02 명시입차/기존지도](02-entry-map.png) | MOCK내비→TEST입차. 실제게이트진입감지아님 |
| [03 관측후보](03-observation-only.png) | TEST B3/C12. 실제판교기둥/현재위치아님 |
| [04 주차요약/미확인](04-unknown-parking-summary.png) | 관측문자를주차층/기둥으로자동승격하지않음. 캡처안정을위해자동닫기끄기이며8초닫기는별도기기검사 |
| [05 입력미저장](05-test-draft-unsaved.png) | TEST폼에는B3/C12가있지만저장기록은미확인 |
| [06 TEST단서저장](06-test-note-stored.png) | 저장ACK·출처/확인시각/입력구분. 실제판교점아님 |
| [07 수동메모저장](07-manual-note-stored.png) | 비우고USER입력/정상EditText선택·IME확인후키입력. 검증용manual-demo |
| [08 귀환기록](08-return-record.png) | USER입력/확인시각과미확인위치·승강연결/복귀선표시. 현장확인아님 |
| [09 5층미리보기](09-floor-preview.png) | 매장행개략도이며차량복귀선아님. CI동작줄이기로재생대기0%,자동재생실측아님 |
| [10 종료복원](10-restored-hidden.png) | 새PID/같은방문·USER메모/PARKED·HIDDEN·자동재개없음 |
| [11 주행중숨김](11-driving-hidden.png) | TEST주행. 상세단서/조작숨김 |
| [12 이동후이전기록](12-previous-record-after-movement.png) | TEST이동신호후현재주차가아닌이전기록,같은방문유지 |
| [13 새완료미확인](13-next-parking-unknown.png) | 이전단서를새주차에자동복사하지않음 |
| [14 명시다시찾기](14-explicit-search-idle.png) | PARKING_GUIDANCE/HIDDEN,관측/지도/내비자동시작없음 |

04/05/06과08/10/12원본6장을확보즉시채팅에표시했다. 이전화면을새것처럼표시하지않았다. 기존네이티브지도재생/정지·평면/기울기와Android생성영상MLKit/C12분석은같은실행의기기회귀에포함된다. UI14장은표시한TEST관측이며실제영상OCR스크린샷으로대체하지않는다.

## APK와소스 식별

[새APK artifact14027490674](https://github.com/leemh-dev/dochagjari/actions/runs/43404749340/artifacts/14027490674) 세파일을정상GitHub앱파일참조로실제다운로드해bytes/SHA256·aapt manifest·apksigner개발인증서/미서명을독립대조했다. [metadata](ci-apk-metadata.json) · [해시](ci-SHA256SUMS) · [독립비교](ci-apk-comparison.json).

CI임시9a1ede3d9848b2c686a73fb4720dad430226a0aa와기능84acdd3의tree f6ddd1844a84dbe84eaa2bd3d6e8873ce7d6d1bd가같다. [소스비교](source-comparison.json). PR실제병합아님. 기기job은같은소스별도runner재빌드라배포byte/서명과일치를가정하지않는다. debug/test개발서명·release **unsigned/서명전설치불가**를구분한다. [로컬metadata](local-apk-metadata.json)/[해시](local-SHA256SUMS)는Cloud키로CI/Mac와다를수있다. 새전용AVD를사용하고이전APK/사용자데이터를유지한다. 목표는검증·시연이며앱마켓출시는범위밖이다.

## 차단/미검증·다음

Cloud ADB0/KVM없음. 새근거없는무가속Pleos부팅·Mac접속/인증우회·키복사/편집을실행하지않았다. 실제Pleos SDK/핀·외부카메라/측위·주차장승강연결/복귀경로·실차·실제OS권한철회·새관측OpenDocument/공개영상경로·실제SENS수락/휴대폰수신·AI자료worker는미검증/미구현이다. 가상병원/모의SMS를유지한다. 수동입력확인은공식시설그래프/현재위치검증이아니다.

다음7단계는같은APK의연속시연영상/전체흐름과제출증거대조다. 이단계에서전체시연영상이나실차귀환내비를완료한것으로표시하지않는다.
