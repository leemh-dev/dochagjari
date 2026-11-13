# 5단계 증거 · 관측 단서와 같은 방문

2026-11-13 UTC · 앱 **0.7.0/7**, 기능 **ef3f306**. [계약](../../22-visit-observation.md) · [CI43388009453](https://github.com/leemh-dev/dochagjari/actions/runs/43388009453).

## 기본 검사/APK — 완료

- 원본 보고서 JVM **77/실패0/ignored0**, 새15건(저장/단서12·생명주기3). 서버14/진단35·lint/debug/test/unsigned release 성공. [CI 요약](ci-verify.json) · [정제 로그](ci-verify-summary.txt) · [로컬 검사](local-checks.json).
- [배포 APK artifact14019243185](https://github.com/leemh-dev/dochagjari/actions/runs/43388009453/artifacts/14019243185) 세 파일을 정상 GitHub앱 파일참조로 다운로드하고 bytes/SHA256·aapt manifest·apksigner 인증서/미서명을 독립 대조했다. [메타데이터](ci-apk-metadata.json) · [해시](ci-SHA256SUMS) · [비교](ci-apk-comparison.json).
- CI는 임시 PR병합 f6c552e8ae4718e30d15002754e35abca373e394를 빌드했다. 기능ef3f306과 실제tree5376de1e0bfe7fe04f8b5c6d22d156c8d4fabff8가 동일하다. [소스 비교](source-comparison.json). PR 자동병합이 아니다.
- debug/test 개발서명, release **unsigned/서명 전 설치불가**. [로컬 메타데이터](local-apk-metadata.json)/[해시](local-SHA256SUMS)는 Cloud 키이며 CI/Mac와 다를 수 있다. 기기job은 동일소스 별도runner 재빌드로 배포artifact와 byte/서명 동일을 가정하지 않는다. 새 전용 AVD를 사용하고 기존 사용자 데이터/이전0.6·0.5·0.4·0.3·0.2 APK를 유지한다. 목표는 검증·시연이며 앱마켓 출시는 범위 밖이다.

## 첫 실제 일반 Android — 회귀1건, 수정 검증 중

CI43388009453 기기는 원본XML11pass/1fail/2skip. 새 VisitRoad 관측 시나리오는 PASS였지만 앱 캐시 file://를 문서grant로 잘못 검사해 기존 선택영상 검사1건이 실패했다. 프로세스/화면/HTTP 후속은 실행되지 않아 새 화면이 없다. [정제 실패 요약](first-device-failure.json). 앱 전용 canonical cache/files만 구분해 수정하며 외부 문서 grant를 우회하지 않는다. 새 소스/후속CI로 확정한다. 첫APK/해시는 first-ci-* 보관본과 구분한다. 이전단계 성공을 이번실행으로세지않는다.

## 기능별 수준

| 항목 | 확인 수준/제약 |
| --- | --- |
| 옛 JSON·실제 SQLite 재열기·같은 방문 저장/자료 보존 | JVM 실제 저장/재조회 PASS |
| 낮음/오래됨/미래시각/표기없음·2프레임·층별 같은번호 충돌·중복/늦은프레임·bounded journal | JVM PASS. TEST B3/C12·B4/C12는 fixture이며 실제 판교점이 아님 |
| 닫기 후 비협조 소스의 늦은 결과·권한 오류·불완전 저널 복원 | AppModel 주입 소스/실제 저장 JVM3건 PASS. 실제 OS 철회/실차 카메라 시험은 아님 |
| 생성 영상/ML Kit를 새 방문 저널에 연결 | 새 실제 Android 검사 대기. 기존 별도 영상 기능의 성공으로 대체하지 않음 |
| 공개/선택 영상 새 저널 경로 | 구현됨. 이번 새 경로의 실제 파일 선택/OS 철회·공개 영상 분석은 미검증 |
| 실제 외부 카메라·Pleos/핀·자동입차·현재위치/실차 | 미검증/미연동. Cloud ADB0/KVM없음, 새 근거없는 무가속 부팅/Mac 접속 실행없음 |
| 추천/지도 | 문자/장애물 문구 갱신. parking=null; 주차층/승강기/현재위치 연결 미확인으로 기존5층 경로 유지. 위치 기반 선 갱신 성공이 아님 |
| SMS/병원/AI 자료보완 | 모의와 공급자/휴대폰수신 구분, 가상병원 유지. AI 조회 worker 미구현 |

다음은6단계 명시 주차완료·요약/닫기·귀환과7단계 같은APK 연속시연이다.
