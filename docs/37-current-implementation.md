# 현행 구현 · 0.11.0/11

> 2026-11-14 카메라 표현 수정: [3분할 예상도](evidence/demo-video/three-panel-concept/README.md) 상단을 CC BY3.0 **실제 전방 주행20초**로 교체했다. 하단만 실제 앱 지도다. 영상 편집 변경이며 앱0.11.0/11·f0640b0/APK와 기존 검사 결과는 유지한다. 새 실사 OCR·OEM카메라·실제 Pleos 동시 실행 성공은 아니다. [출처](media/front-driving/README.md).

> 2026-11-14 영상 범위 정정: 기존90초/20초는 앱 단독 기능기록이다. 요청한 **실제 Pleos3분할 대표시연은 미완료**다. [3분할 UX합성 예상도](evidence/demo-video/three-panel-concept/README.md)를 별도 제공하며 실제 통합 성공으로 세지 않는다. [실제 완료 기준39](39-three-panel-demo-gap.md).

2026-11-14 UTC. `feat/arrival-context-ux`, [PR#2](https://github.com/leemh-dev/dochagjari/pull/2). **Cloud 구현·일반Android 검사·실제 앱 시연 영상까지 완료했다.** Mac/Pleos 새버전 실연동은 이 세션에서 접근하지 못해 미검증이다. 기존 PR#1·앱 데이터·시안은 보존하고 자동병합하지 않았다.

## 실제 산출물과 검사

앱/검사 기준 **f0640b00da92409c265cfd5563238e0d972edcaa**, 앱 **0.11.0/versionCode11**. [push CI43569033733](https://github.com/leemh-dev/dochagjari/actions/runs/43569033733)와 [PR CI43569041435](https://github.com/leemh-dev/dochagjari/actions/runs/43569041435)의 verify/android-device가 모두success다. 이후 문서/영상 편집 커밋은 앱 코드 기준과 구분한다.

- JVM **115**, 서버 **14**, 진단 **35**, lint·debug/test/unsigned release 빌드 통과.
- connectedDebugAndroidTest XML **15pass/0fail/4skip**. skip4는 복원 준비/검증 전용이며 각각 별도 실행해 기존 복원2단계+탑승 복원2단계 모두통과했다.
- 원본54장: 제품28·탑승8·기존주차14·HTTP3·최종1. 이 중제품/탑승/HTTP39장을 [증거폴더](evidence/arrival-panel/f064-device/)에도 보존했다. 사진의 일반Android/TEST/MOCK/생성 입력을 Pleos 성공으로 바꾸지 않는다.
- 실제 기기 설치/검사/녹화에 쓴 [APK쌍 artifact14104088875](https://github.com/leemh-dev/dochagjari/actions/runs/43569033733/artifacts/14104088875)의 두파일 SHA256·동일개발인증서를 독립다운로드 대조했다. [메타데이터](evidence/arrival-panel/f064-device/apk-metadata.json).
- 별도 verify runner의 [3APK artifact14104315679](https://github.com/leemh-dev/dochagjari/actions/runs/43569033733/artifacts/14104315679)도 실제파일과 대조했다. 개발인증서가 다르므로 Mac에는 실제검사APK쌍을 전달한다. release는unsigned다.
- [90초 편집 시연·20초 연속 모션·자막/원본](evidence/demo-video/README.md). 같은 APK의 실제기록이고 장면편집/시간생략을 표시한다. 실제 Pleos 배경을 합성하지 않았다.

## 단계별 실제 수준

| 단계 | Cloud/일반Android 완료 | Mac Pleos |
| --- | --- | --- |
| 1 저장/이행 | 다중프리셋/이번구성/기존설정이행·순차쓰기/ACK/실패/재조회, 실제파일/새PID 복원 | 이번APK 미실행 |
| 2 독립탑승UI | 생성/수정/삭제/이번만변경/건너뛰기·재시작 재확인·주행제한, 실제저장값 확인 | 분할폭/터치/성능 미검증 |
| 3 입력 | 텍스트→후보→확인·부정/모호함, TEST점유와 사용자역할 분리 | 실제STT/Gleo/SDK수신 미검증 |
| 4 패널 | 상단 생성영상 원본비율/하단 기울인 지도·5층 파란선, 1280×800와480×900 실제캡처 | 순정내비와 실제배치 미검증 |
| 5 대표흐름 | 같은방문 준비/모의요청/명시입차/Android C12후보/주차저장·닫기·귀환·새PID복원, 병원/학원 가상추천/우회/후보없음 | 실제SDK콜백/핀·Console·실차 미검증 |
| 6 시연 | 실제89.174211초 원본,90초편집본/20초모션,1080p30fps·전체디코딩/프레임/해시대조 | 같은APK의 현장배치 확인 필요 |

## 복구와 저장의 증거

- 기존복원 PID7349→7400에서 같은확정요청·방문/자료·내비요청·관측·주차/메모를 재조회했다. 관측/지도/내비는 자동재개하지 않는다.
- 탑승복원 PID14667→14724에서 실제DataStore의book/profile/방문ID와 선택적outbox지문을 대조하고, 지난구성을 이번구성으로 자동확정하지 않음을 확인했다. 확정알림보존은 기존복원과JVM UNKNOWN레코드 통합검사도 함께 근거로 쓴다.
- 제품UI PID10239→13557에서 수동메모와주차기록 복원/안내자동열림없음을 확인했다.
- [실제 앱 HTTP SERVER_MOCK](evidence/arrival-panel/f064-device/android-server-recovery/summary.json): PID14994→15657, GET404→POST저장/응답보류→동일DB서버재시작→빈토큰/같은세션GET200. POST1/공급자0. 실제SENS수락/휴대폰수신이아니다.

## 구현 선택과 남은 경계

사용자 확인 역할/이번구성과 센서점유를 분리했다. 뒤좌우EMPTY만으로 혼자/아이/환자를 판정하지 않는다. 이번구성이 legacyProfile·방문·확정알림을 덮어쓰지 않으며 재시작하면 다시 확인한다. Android음성 인텐트와 미지원 텍스트 대체는 구현했지만 실제STT 복귀는 기기검증 전이다.

앱영역만 구현했다. 주차/도로 그림은 배치예시이며 주차층·기둥·승강연결·현재위치·정차허용을 생성하지 않는다.5층은 기존 공식기반 개략도/매장행 경로 재생이고 측위/복귀선이 아니다. 생성C12는 문자후보다. 닫기/ON_STOP/주행/주차완료에서미디어·관측을중지하고자동재개하지않는다.

병원/학원은 기존 가상시설의 명시이동조건·차량높이·동행/승하차·후보제외를 재사용한다. 독립패널의모의요청은 매장방문/알림세션을 바꾸지 않는다. 역/공항은 실제차량승차점 근거가 없어 개념안내이며 확정내비를요청하지않는다. 실제병원/픽업보행동선도미검증이다.

오늘 범위는 기능검증과 시연이다. 실제OEM카메라·실차/현장연결·STT/Gleo·SDK핀/Console·실제SMS는미검증이며, 이동중AI시설자료보완worker는미구현이다. 기존공식기반사전DB를사용하며 실시간AI조회처럼 합성하지않는다. 앱마켓출시/출시서명은범위밖이고debug/test개발서명·release unsigned다.

첫 실제실행의 텍스트입력 자동화실패, 중복후보문구, 주행후상세조작을시도한드라이버, 늘어진영상/작은지도와 겹침, 부정문후보오류를 수정했다. [과정/실패/해결](10-development-log.md)과 이전실패원본을 남겼다. 다음은 [Mac M1/8GB·외장SSD 인계](38-mac-pleos-v0.11-handoff.md)다.
