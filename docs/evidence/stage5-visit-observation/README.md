# 5단계 증거 · 관측 단서와 같은 방문

2026-11-13 UTC · 앱 **0.7.0/7**, 기능 **ef3f306**·실제 기기 URI 회귀 수정 **8ea046a**. [계약](../../22-visit-observation.md) · [최신 CI43390462215](https://github.com/leemh-dev/dochagjari/actions/runs/43390462215) **verify/android-device 모두 success**.

## 실제 결과

- 원본 보고서 JVM **82/실패0/ignored0**, 새20건(저장/단서12·생명주기3·URI경계5). 서버14/진단35·lint/debug/test/unsigned release 성공. [CI 요약](ci-verify.json)/[정제 로그](ci-verify-summary.txt) · [로컬 검사](local-checks.json).
- 실제 기기 XML **12pass/실패0**, 프로세스 전용2메서드는 일괄검사에서skip 후 별도 준비/종료/새PID 복원2단계PASS. [기기별 요약](ci-device-checks.json) · [프로세스 원본](ci-process-restart.txt). 콘솔 Finished/15-14 수치를 모두 pass로 세지 않는다.
- 기존 영상선택회귀도PASS. 새로 확장한 VisitRoad 한 시나리오에서 같은방문/원본자료/설정/알림·명시TEST입차와 TEST단서2프레임/층충돌/낮음/오래됨/표기없음·실제Android 생성C12 분석/OCR문자점수null·닫기/늦은입력/주행차단/재시작을확인했다. 위치/주차층·승강연결로승격하지않았다.
- 프로세스7575→7626에서같은방문/내비요청/관측ID·2TEST프레임/불완전저널과기존프로필/역할·동일확정알림을복원했다. source/내비자동재개없음. 명시적으로준비한미완료저널검사이며실제카메라강제종료실측이아니다.
- UI37장/신규30~37관측8장 성공. UI실제TEST관측도11670→13133의새PID에서같은방문/저장2프레임/idle·오래됨후보보류를확인했다. [전체 UI trace/readiness](ci-ui-summary.json). transport=device/boot1/package·activity found/각rc0을기록했다.
- [HTTP SERVER_MOCK](ci-http-recovery-summary.json)도PASS. 응답보류→앱종료/같은DB서버재시작/빈토큰→같은세션GET200,POST1/공급자0. DB삭제/새세션재발송으로복구하지않았다. 실제SENS수락/휴대폰수신이아니다.

## 실제 앱 원본 화면

이번 CI의 일반 Android API34/새 전용 AVD이며 실제 Pleos/실차가 아니다. PNG/XML은 artifact 원본에서 복사했다. [출처·파일 SHA256](screenshot-provenance.json) · [전체 기기 artifact14022042145](https://github.com/leemh-dev/dochagjari/actions/runs/43390462215/artifacts/14022042145).

| 화면 | 입력과 확인 |
| --- | --- |
| [30 반복 문자 후보](30-observation-test-candidate.png) | TEST B3/C12 2프레임. 실제 위치 확정 아님 |
| [31 층 충돌](31-observation-floor-conflict.png) | TEST B3/B4의 같은C12, 후보 보류 |
| [32 낮은 신뢰도](32-observation-low-confidence.png) | TEST 점수0.2, 후보 보류 |
| [33 오래됨](33-observation-stale.png) | TEST 분석시각60초 전, 후보 보류 |
| [34 표기없음](34-observation-no-sign.png) | TEST 빈문자, 후보 보류 |
| [35 실제 Android 영상 분석](35-observation-android-video.png) | 생성4초영상/Android MLKit의2처리프레임·C12·문자점수미제공/층미확인. 실제판교점기둥아님 |
| [36 기존 지도/미확인 안내](36-observation-guide-unverified.png) | 생성영상출처+실제기둥/현재위치·승강연결미확인,기존5층개략도. CI동작줄이기로재생대기0%,자동재생/측위기반선갱신실측아님 |
| [37 종료복원/idle](37-observation-restored-idle.png) | 새PID/동일방문·저장TEST2프레임·자동관측재개없음/오래됨 |

30·35·36과31·37 원본을 확보 즉시 채팅에 표시했다. 이전 캡처를 새 실행처럼 사용하지 않았다. 기존 지도 재생/평면/기울기 검사는 동일 실행의 기기/전체UI trace에 있으며 이동점은 경로 재생이고 현재 위치가 아니다.

## APK/소스 독립 식별

[배포 APK artifact14021185433](https://github.com/leemh-dev/dochagjari/actions/runs/43390462215/artifacts/14021185433) 세 파일을 정상 GitHub앱 파일참조로 다운로드하고 bytes/SHA256·aapt manifest·apksigner 인증서/미서명을 독립 대조했다. [메타데이터](ci-apk-metadata.json) · [해시](ci-SHA256SUMS) · [비교](ci-apk-comparison.json).

CI 임시 PR병합5894f4388389e9f486362dd46477af56f268e149와 기능수정8ea046a의 실제tree af875b388905631af7c67bf20ce74046e4d7fb25가같다. [소스 비교](source-comparison.json). 실제PR병합이아니다. 기기job은같은소스별도runner재빌드이며배포artifact와byte/서명일치를가정하지않는다.

debug/test 개발서명·release **unsigned/서명 전 설치불가**. [로컬 메타데이터](local-apk-metadata.json)/[해시](local-SHA256SUMS)는Cloud키로CI/Mac와다를수있다. 새전용AVD를사용하고기존사용자데이터/이전APK를유지한다. 목표는검증·시연이며앱마켓출시는범위밖이다.

## 첫 실패와 수정 이력

첫CI43388009453/ef3f306은verify성공(JVM77/서버14/진단35)이었지만기기XML11pass/1fail/2skip. 앱소유cache file://도문서grant를요구한회귀로기존선택영상결과가갱신되지않았다. 새관측시나리오는PASS였으나프로세스/UI/HTTP후속은실행되지않았다. [실패요약](first-device-failure.json),first-ci-*metadata/hash/비교·원본artifact14019563596을보존했다.

8ea046a는canonical앱cache/files하위읽기가능파일과외부content문서grant를구분한다. 유사prefix외부경로·권한없는문서는계속거부한다. URI단위첫실패는Robolectric기본grant허용과시험기대불일치여서GRANTED/DENIED를명시주입했다. [첫시험로그](uri-test-fixture-failure.log) · [수정후전체로컬로그](uri-fixed-local.log). 인증/권한검사를해제하지않았으며최신CI에서실제회귀해결을확인했다.

## 기능별 한계·다음

- TEST B3/C12·B4/C12는fixture다. 생성C12도실제판교점기둥아니다. parking=null, 주차층·승강기/현재위치연결미확인으로기존5층선유지/추천문구갱신만한다. 위치기반경로갱신성공이아니다.
- 닫기/주행·unknown/권한오류/종료후재개없음은일반Android·주입소스/실제저장범위로확인했다. 비협조소스의늦은프레임과SecurityException은JVM주입검사다. 실제OS권한철회·새관측OpenDocument/공개영상경로의기기실행은미검증이다. 기존선택cache공개영상경로PASS와구분한다.
- 실제외부카메라/Pleos SDK·핀/측위·실차는미연동/미검증. Cloud ADB0/KVM없음이며새근거없는무가속부팅/Mac접속을실행하지않았다. native중지중호출의즉시강제종료/메모리성능을실측했다고표시하지않는다.
- 가상병원·모의SMS유지. 공급자수락/휴대폰수신·AI자료보완worker는미검증/미구현이다.

다음은6단계 명시주차완료·요약/닫기·귀환,7단계 같은APK 연속시연이다.
