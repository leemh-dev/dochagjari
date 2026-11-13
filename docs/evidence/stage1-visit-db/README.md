# 1단계 · 사전 매장 DB와 방문 저장/복원

2026-11-13 UTC · 앱 0.3.0 / versionCode 3. 앱 기능 코드 `4d31a15`, APK 패키지명 메타데이터 수정 `3b9a3c5`.

## 로컬 검사

[검사 요약](local-checks.json) · [Gradle 로그](local-gradle.txt)

JVM 41건(기존 33 + 신규 8), 실패·오류 0. lint, debug·기기 테스트·unsigned release 빌드가 2분 3초에 통과했다. 기존 JDK 17 / Android 35 / Gradle 캐시를 사용해 offline, 작업자 1개, heap 1536MiB로 실행했다.

신규 검사는 다음을 확인한다.

- 저장된 별칭 조회, 다른 브랜드·지점 오인 방지와 SQL 문구 입력 처리
- 모호한 매장명은 전체 후보를 반환하고 첫 후보로 임의 확정하지 않음
- DB 종료·재개 후 동일 방문 ID, 숨김 상태, 출처가 표시된 테스트 주차 단서 복원
- 새 자료 버전 추가 후에도 기존 방문과 이전 자료 보존
- 같은 버전의 내용 변경과 방문 식별 변경 거부
- DB 연결 두 개의 동시 준비 요청 10건을 방문 하나로 처리
- 없는 매장으로 방문 저장 거부
- 저장 실패·재시도 및 실제 저장 성공 후에만 완료 상태 알림

Cloud에서 조회한 연결 기기는 0대이고 KVM이 없다. 같은 무가속 부팅을 반복하지 않았다. 기기 테스트 APK 생성은 기기 검사 실행 성공과 다르다.

## APK 식별

[로컬 실제 manifest·서명·SHA-256](local-apk-metadata.json) · [SHA256SUMS](local-SHA256SUMS)

세 APK의 앱 코드는 `4d31a15`다. debug/test는 동일한 Cloud 개발 키를 사용한다. 이전 배포 runner 키와 다를 수 있으므로 새 전용 AVD에 설치하며 기존 앱 데이터를 삭제하지 않는다. release는 미서명으로 설치·제출 서명 완료본이 아니다.

최초 aapt 속성 이름 정규식이 `compileSdkVersionCodename`의 접미 `name`까지 읽는 문제를 실제 APK 대조에서 재현했다. `3b9a3c5`에서 공백 경계와 정확한 app/test 패키지 검사를 추가했다. 이 수정으로 앱 바이너리는 바뀌지 않았다. 로컬 metadata는 수정된 수집기로 다시 생성했다.

## GitHub CI와 artifact

[수정 후 CI](https://github.com/leemh-dev/dochagjari/actions/runs/43344658563) · [새 APK artifact14001506355](https://github.com/leemh-dev/dochagjari/actions/runs/43344658563/artifacts/14001506355)

verify와 android-device job 모두 성공했다. [GitHub API 실제 결과](ci-result.json)에 job/step 결론과 artifact 식별자를 기록했다. `development-apks-and-metadata`에는 세 APK와 실제 manifest·코드·해시·서명이 포함된다. CI 생성 단계에서 서명/패키지 검사가 통과했다. 이 Cloud에서의 독립 다운로드·대조는 아직 못 했다.

GitHub API는 artifact를 반환했지만 Azure 저장소로의 프록시 터널이 403을 반환했다. 새 아카이브 응답과 다른 정상 HTTP 클라이언트로 확인해 리다이렉트 대상의 터널 차단으로 구분했다. 반복 재시도·다른 프록시 우회는 하지 않는다. 임시 서명 URL이나 토큰을 증거 파일에 저장하지 않는다. 실제 기기 화면도 이 경로가 막히면 링크로 제공한다.

이전 `43344530173`의 artifact는 패키지명 파싱 수정 전 기록이므로 신규 배포 링크로 권장하지 않는다. 현재 추천은 위 수정 후 artifact다. 로컬 APK와 CI runner APK를 같은 해시·서명으로 표시하지 않는다. 이전 0.2.0/2 artifact13871881801은 별도 보관본이고 고정 0.2 패키징 workflow도 유지했다.

## 기기 검사 범위와 한계

기기 검사에 매장 검색·방문 준비와 실제 SQLite 재조회·Activity 재시작 1건을 추가했다. 일반 Android CI 화면 14~16에는 검색·저장·앱 강제 종료·새 PID에서 동일 방문 ID 복원을 추가했다. 같은 코드의 CI 전체 실행이 성공해 기기 검사와 새 프로세스 복원 캡처 절차가 통과했다. 개별 보고서/원본은 다운로드 차단 때문에 이 Cloud에서 독립 열람하지 못했다.

입력은 정차 테스트와 사용자 방문 준비다. 실제 차량 주행·주차·카메라·Pleos SDK 완료 증거가 아니다. 이번 단계는 판교점 에뜨와 5F, 출처, 별칭, 보행 14좌표의 사전 조회와 방문 기록까지다. 차량 게이트·주차 기둥·승강기 호기·거리·무단차·도면 자산 재사용 범위는 미확인이다. 앱 지도 애니메이션·내비·입차·영상 연결은 2단계 이후다. 기존 알림과 설정 저장소는 별도로 보존한다.


[실제 기기 보고서/화면 artifact14001348823](https://github.com/leemh-dev/dochagjari/actions/runs/43344658563/artifacts/14001348823)에 `ui-screens/14-store-lookup.png`, `15-visit-saved.png`, `16-visit-restored.png`를 포함하는 캡처 절차의 산출물이 있다. 기기 artifact를 처음 조회한 후 다운로드를 시도했지만 같은 저장소 호스트의 터널403이었다. 이전 화면을 새 앱 화면으로 표시하지 않는다.

후속 문서/증거 커밋은 앱 코드를 바꾸지 않으며 `[skip ci]`로 이미 통과한 동일 코드 검사의 중복 실행을 생략한다. 검증한 앱 기능은4d31a15, 수정된 전체 CI 기준은3b9a3c5다.
