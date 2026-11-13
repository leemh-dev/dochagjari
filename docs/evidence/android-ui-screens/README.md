# 실제 Android 화면 조작 증거

2026-11-12 UTC. [PR CI43128321081](https://github.com/leemh-dev/dochagjari/actions/runs/43128321081), artifact13903799876. 검사 코드cc1c19e, 앱 런타임3a01515 이후 동일·버전0.2.0/code2. Generic Android API34 Google APIs/x86_64 KVM, 새 전용 AVD.

13 PNG는 adb screencap 원본이며 각 XML은 같은 단계의 실제 accessibility dump다. 크기/색/내용을 편집하지 않았다. 차량 테스트 입력·모의 내비·생성 영상·로컬 모의 알림 환경이다. 알림 화면은 좌표 입력 단계이며 이 캡처만으로 발송 완료를 뜻하지 않는다. 실제 SMS·Pleos 순정 내비/좌석·실차 검증이 아니다. 무작위 세션 ID는 새 테스트 데이터이며 개인 이동 기록/실제 수신자/비밀값은 없다.

[요약 JSON](summary.json)의 성공은 화면 조작10개 확인·13장 캡처다. 기존 connected 기능6건 및 별도 프로세스 복원2단계도 같은 CI에서 통과했다. 단위/JVM33·서버14·lint·당시 Python13건도 성공했다.

- 01/02/03/04: 픽업·주차·우회·후보 없음
- 05/06: 가상 병원·설정 저장 완료 표시
- 07/08/09: Android 실제 빈/장애물 영상 분석·C12 화면·점유에 따른 우회
- 10/11: 알림 좌표 입력·연동 테스트 화면
- 12/13: 주행 테스트 입력에서 설정 비활성·영상 결과 숨김

실제 XML에서 Switch 클래스가 아니라 checkable android.view.View가 확인됐다. 기존 Pleos 조작기의 클래스 기반 선택 실패를 재현했고, 라벨/행/오른쪽 위치/유일한 checkable·clickable 노드로 수정했다. 이 선택기 회귀는 실제 XML의 로컬 검사이며 Pleos 기기에서 수정 동작을 확인한 것은 아니다.
