# 4단계 · 시설 목적지 내비/명시적 입차

2026-11-13 UTC · 앱0.6.0/6·기능f959cb9. [상태/복구 계약](../../21-road-navigation-and-entry.md).

## 실제 확인한 공식 자료

[공식 시설 대표 마커 근거](official-facility-marker.json): 판교점 공식 위치/주차 페이지를새로HTTP200조회하고판교역로146번길20·37.3927854/127.1120699를확인했다. 시설지도마커/대표좌표이며차량게이트/진입점이아니다. 기존5층보행좌표도GPS/거리단위가아니다. 원문전체/제3자스크립트키를복제하지않고정제발췌/원문hash만보존했다.

## 로컬 검사

[최종 집계](local-checks.json) · [최종 빌드](local-final.txt) · [초기 구현](local-initial.txt) · [기기 시나리오/경계 보강](local-device-scenario.txt) · [SDK 반환/이벤트 구분](local-sdk-evidence.txt).

새10건포함JVM62/실패0·오류0,lint/debug/test/unsigned release최종1m43s통과. 중간생산코드보강후각검사를갱신했으며같은통과코드를이유없이반복한것이아니다. 옛JSON빈journey복원·계산/SDK와입차분리·닫힘/중복·취소/늦은결과·동시claim단1·주입쓰기실패전달차단·claim후종료복원/원래목적지snapshot유지를검사했다. 주입예외는실제디스크고장실측이아니다.

## 최신 CI/APK 독립 대조

[CI43371857767](https://github.com/leemh-dev/dochagjari/actions/runs/43371857767) verify성공. [원본 JVM62 보고서](ci-jvm-index.html) · [verify 로그 발췌](ci-verify-summary.txt). Gradle4m35s·서버14/진단35도이번실행성공이다. 기기작업도성공했으며아래최종원본을확인했다.

[로컬 metadata](local-apk-metadata.json) · [로컬 SHA](local-SHA256SUMS) · [CI metadata](ci-apk-metadata.json) · [CI SHA](ci-SHA256SUMS) · [세파일 독립 대조](ci-apk-comparison.json).

APKartifact14012324593을정상연결GitHub앱파일참조로다운로드해실제세파일bytes SHA256/aapt manifest/apksigner개발인증서·미서명을모두대조했다. CI임시PR병합58b5fe1의실제tree2ec1f141efeb24a09b02f372a45b37ea01db7ef9가head f959cb9와같다. PR실제병합은하지않았다. debug/test는개발서명,release는미서명이다. 기존0.5/0.4/0.3/0.2보관본/사용자데이터를유지한다.

기기job은같은소스에서별도재빌드해검사한다. 배포artifact는verify job산출물이며서로다른runner/Cloud/Mac의개발키·APK hash가같다고가정하지않는다. 배포세파일의독립hash/서명대조와동일소스기기동작검증을구분한다. 새전용AVD를사용하고기존사용자앱을삭제하지않는다.

## 기기/외부 연동 수준

새실제일반Android원본26~29와아래보고서를실제로열었다. 기존3단계/웹/호스트화면을이번성공으로대체하지않는다.

현재Cloud ADB기기0/KVM없음. 새무가속부팅/연결없는Mac실행주장없다. 실제Pleos SDK/순정핀·차량자동입차·외부카메라/실내측위·실차와실제SMS수신은미검증이다. 모의내비/명시TEST입차와구분한다. AI자료보완worker/관측·주차/귀환은후속이다.

## 최종 실제 일반 Android 검증 — 통과

[CI43371857767](https://github.com/leemh-dev/dochagjari/actions/runs/43371857767)의verify/android-device모두success. [결론/step](ci-result.json) · [기기JUnit XML](device-instrumented-results.xml) · [새프로세스복원 PASS](device-process-restart-summary.txt) · [UI 원본 요약](device-ui-summary.json) · [실제HTTP모의복구](device-http-summary.json).

- JUnit **12pass/0failure**, 프로세스전용2메서드는일괄실행에서skip후별도준비/종료/복원2단계PASS다. 기존6+매장1+지도2+좌석2+새방문시나리오1을포함한다. 콘솔Finished숫자를모두pass로세지않는다.
- 실제앱에서모의계산이입차/지도자동열기로바뀌지않음→취소/재요청·늦은이벤트→주행/unknown차단→명시TEST입차→같은방문의지도→닫기/중복입차/Activity재시작을확인했다. 기존profile·알림요청/방문ID/자료와parking=null을실제저장값으로검사했다.
- 별도새PID7370→7420에서기존설정/역할/동일확정알림fingerprint와동일방문/내비요청ID·DISPATCHED/이벤트없음/입차없음을확인했다. 실제외부전달전종료를재현하는명시적claim검사이며SDK의실제전달/응답유실을실측한것은아니다.
- UI29장전체/역할·방문복원·기존Android영상EMPTY/OCCUPIED·C12·우회/기타기능도이번실행성공. HTTP SERVER_MOCK는응답보류→앱종료/동일DB서버재시작/빈토큰→같은세션GET200·POST1/공급자0으로성공했다. 실제SENS수락/휴대폰수신이아니다.

### 최신 원본 앱 화면

[26 시설목적지·모의내비](26-visit-road-mock.png) · [27 명시TEST입차→네이티브지도](27-explicit-entry-native-guide.png) · [28 닫기/같은방문HIDDEN](28-entry-guide-closed.png) · [29 새프로세스/같은방문PARKING_GUIDANCE·HIDDEN](29-entry-restored-hidden.png).

[18 같은APK의수동미리보기재생26%](18-native-route-animation.png)는이번CI의입차전별도미리보기검사다. 27의입차지도는CI동작줄이기설정때문에재생대기0%이며입차후자동재생을실측했다고표시하지않는다. 모든이동점/선은경로재생이고현재측위가아니다.

위새4장과수동재생1장을확보즉시채팅에서직접열었다. **실제일반Android API34·모의내비·명시TEST입차**이며Pleos/실차/자동감지가아니다. PNG/XML과[이미지출처/SHA](device-image-hashes.json)를보관한다. 전체원본/로그는 [기기artifact14013477464](https://github.com/leemh-dev/dochagjari/actions/runs/43371857767/artifacts/14013477464),배포APK/metadata는 [APKartifact14012324593](https://github.com/leemh-dev/dochagjari/actions/runs/43371857767/artifacts/14012324593)다.

기존Mac설치본업그레이드/실제Pleos SDK요청·콜백/순정핀·차량자동입차·카메라/실내측위·실차는이번에실행하지않았다. 자료의차량게이트/주차층/승강기매핑도미확인이다. 다음5단계관측/갱신과6단계주차/귀환·7단계전체시연은남아있다.
