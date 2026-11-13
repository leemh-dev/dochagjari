# 2단계 · 앱 내 개략도와 파란 경로 재생

2026-11-13 UTC · 앱 0.4.0 / versionCode 4, 기능 코드 `e573e8e`.

## 현재 실제 결과

- [로컬 검사](local-checks.json) · [전체 Gradle 로그](local-gradle.txt): JVM 45건, 실패/오류0, lint와 debug/test/unsigned release 빌드 성공(2분9초).
- [중간 프레임 검사 로그](host-motion-gradle.txt): 같은 Kotlin/Compose 화면에서 제어된 clock으로 진행값10개 초과·최종100%를 확인하고25프레임을 기록했다. 전체45건 통과 후 이1건을 보강해 단독 재실행했다.
- [기울인 화면](host-tilted.png) · [평면 화면](host-flat.png) · [파란 선 재생 GIF](host-native-route.gif).

이 이미지/GIF는 **Robolectric native graphics의 Android framework/Compose 렌더**다. 웹을 캡처하거나 이미지 생성기로 만든 것이 아니지만, 실제 AVD·Pleos·차량 화면도 아니다. 실제 일반 Android 기기 결과와 원본은 아래 최종 검증 절을 따른다.

## 구현과 보존 범위

저장된 방문에서 `5층 동선 보기`를 누르면 네이티브 Canvas/PathMeasure와 graphicsLayer가 자체5층 일부 개략도·파란선/이동점·기울기/회전을 표시한다. 재생·일시정지·처음부터·평면전환·닫기를 제공한다. 닫기는 같은 방문의 표시 상태만 저장하며 ID/자료 버전·기존 프로필·알림 요청을 바꾸지 않는다. 주행/미확인/차량 소스 변경 시 상세 지도를 닫고 background에서는 재생을 중지한다.

공식 보행14좌표는 기존 사전DB snapshot을 그대로 쓴다. 주변 블록은 확인된 배치를 참고해 직접 작성한 개략도다. 공식 raster지도·지도 SDK 인증 구성을 앱에 복사하지 않았다. 자료 버전·층·출발/도착 식별자가 맞지 않으면 미준비 안내를 표시한다. 거리/실내 현재위치·기둥/호기·무단차 검증은 새로 주장하지 않는다. 자동입차/내비/카메라·전체주차여정은 이후 단계다.

## 조사·오류·해결

- 기존 검사에 없던 호스트 Compose 검사 의존성이 offline 캐시의 버전과 달랐다. 기존 기기 검사와 동일한 AndroidX JUnit1.2.1/runner1.6.2, 앱의 tracing1.2.0으로 test 의존성을 맞췄다. 앱 runtime에 새 외부조회 라이브러리를 추가하지 않았다.
- 호스트 PixelCopy는 forceRedraw callback timeout이었다. Android 기기 오류로 단정하지 않고 native View.draw로 캡처했다.
- 실제 호스트 렌더에서 제목이 어둡고 지도 글씨가 작음을 확인해 contentColor와 넓은 화면의 안내/지도 배치·표시 범위를 수정했다. 수정 후 화면을 채팅에서 즉시 표시했다.
- 도형 정합/종횡비·유효 뷰포트, 자료 버전 불일치, 표시상태 저장·동일 방문 복원과 호스트 렌더/컨트롤을 검사했다. 실제 기기에는 중간 재생·일시정지·기울기·닫기/복원 및 주행/미확인 차단2건을 추가했다.

## APK/CI

[로컬 manifest·SHA-256·서명](local-apk-metadata.json) · [SHA256SUMS](local-SHA256SUMS)

로컬 debug/test는 같은 Cloud 개발 키이며 release는 미서명이다. 다른 runner/Mac 키와 동일하다고 가정하지 않고 새 전용 AVD를 사용한다. 기존0.3/0.2 보관본을 덮어쓰지 않는다. 앱 코드 커밋 후 실제 세 파일에서 manifest/서명/해시를 추출했다.

[CI43358680681](https://github.com/leemh-dev/dochagjari/actions/runs/43358680681)은 verify/android-device 모두 성공했다. APK `development-apks-and-metadata`를 확인했으며 CI별 hash/서명을 로컬 결과와 구분한다. 기존 Cloud의 Azure artifact 저장소 터널403 진단은1단계 기록을 따른다. 연결기기/KVM이 없어 새 무가속 부팅은 반복하지 않는다.

주행 중 충분한 근거를 확보하는 백그라운드 자료 보완 방향은 [docs/19](../../19-native-guide-and-enrichment.md)에 반영했다. 이번 단계에서 실제 AI 조회 worker가 동작한다고 표시하지 않는다.

### 기기에서 발견한 일시정지 오류

초기 cad08a9의 CI43355911129는 verify 성공/기기 실패였다. 정지 뒤 대기 프레임으로24%→25%가 된 실제 비교값은 [원본 발췌](device-pause-failure.txt)에 있다. 수정 e573e8e는 프레임 직후 상태를 재확인한다. [수정 후 로컬 전체 로그](pause-fix-gradle.txt)는 JVM45/실패0·lint/APK성공1m59s이며 정지 후1000ms불변을 포함한다. 기기 assertion은 완화하지 않았다. 최초 PNG/GIF는 cad08a9 호스트 렌더이고 새 기기 캡처로 표시하지 않는다. 로컬 APK metadata는 수정 후 e573e8e 산출물로 갱신했다.

### CI APK 실제 다운로드/대조

[CI metadata](ci-apk-metadata.json) · [CI SHA256SUMS](ci-SHA256SUMS) · [독립 비교](ci-apk-comparison.json) · [verify 요약](ci-verify-summary.txt). 수정 후 CI43358680681의 APKartifact14006823539를 연결된 GitHub 앱의 정상 파일참조로 내려받았다. CLI의401/Azure터널403을 우회한 것이 아니며 정상 연결 기능이다. 세 APK의 실제 bytes SHA-256·aapt manifest·apksigner 인증서/미서명을 모두 대조했다. CI는 PR 임시병합7189204를 빌드했고 기능head e573e8e와 실제 tree가 같음을 확인했다. 로컬/CI hash·키는 각각의 metadata를 따른다.

## 최종 실제 일반 Android 검증 — 통과

[CI43358680681](https://github.com/leemh-dev/dochagjari/actions/runs/43358680681) verify/android-device 모두 성공. [결론/step](ci-result.json) · [기기 JUnit 원본](device-instrumented-results.xml) · [UI 원본 요약](device-ui-summary.json) · [HTTP 복구](device-http-summary.json). XML 기준9건 통과(기존6+매장1+신규지도2), 프로세스 전용2메서드는 일괄검사에서skip 후 별도 준비/재시작/검증2단계로PASS했다. Gradle콘솔의Finished13을13개실행통과로계산하지않는다.

- [17 기울인 최초 화면](17-native-guide-tilted.png)
- [18 재생25% 중간 프레임](18-native-route-animation.png)
- [19 평면/100%](19-native-guide-flat.png)
- [20 닫기](20-native-guide-closed.png)
- [21 새프로세스/같은방문/PLANNED·HIDDEN복원](21-native-guide-hidden-restored.png)

위5장은 **이번 실행의 실제 일반 Android API34 AVD 앱 화면**이며 입력은 테스트 차량/사전DB다. PNG/XML원본을채팅에서실제로열어중간공유했다. Pleos/실차/실내측위가아니다. 전체21장·UI XML/보고서는 [기기 artifact14008371120](https://github.com/leemh-dev/dochagjari/actions/runs/43358680681/artifacts/14008371120)에있다. 기존픽업/주차/우회/후보없음/가상병원·설정/영상점유·C12/주행차단도이번실행에서다시확인했다.

별도 실제 HTTP SERVER_MOCK 복구는 응답보류→앱종료/동일DB서버재시작→빈토큰/같은세션GET200, POST총1/공급자0이다. 실제SENS수락/휴대폰수신이아니다. Pleos순정내비/Console/실차·주차장-승강연결·실제AI조회는이번범위에서미검증/미구현이다.

닫기20/재시작21본문픽셀은완전히같고정상이다. [원본 픽셀 비교](closed-screen-pixel-comparison.json). 채팅표시를검정으로오독한뒤원본비교/축소재열람으로정정했다. 미커밋0.4.1실험은철회했고최종앱은CI통과0.4.0/4·e573e8e다. 오판/복구는개발일지에기록했다.
