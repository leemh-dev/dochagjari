# Pleos SDK 연동 및 기술 검증

## 구현 구조 제안

Kotlin 기반 Android 차량용 앱으로 시작한다. 추천 계산과 시설 데이터 조회는 Pleos 서비스 호출에서 분리한다. 추천 로직은 가상 데이터로 검증할 수 있게 하고, 실제 내비·차량 상태는 Pleos 연동부를 통해 처리한다. 설정은 단말에 저장하고 소량의 시설 데이터는 앱에 포함한다.

| 연동 | 문서로 확인한 범위 | 실행 시 검증할 내용과 대안 |
| --- | --- | --- |
| 순정 내비 | NaviHelper는 목적지·경유지·경로 요청과 결과 이벤트를 제공한다. | 동일 시설의 차량 도착점 2곳을 각각 요청한다. 응답, 실제 내비 핀, 차량 접근 방향을 비교한다. 좌표만으로 충분한지, POI ID가 필요한지 확인한다. 정밀 지점 전달이 실패하면 내비가 지원하는 접근점과 보행 안내를 제공한다. |
| 좌석 점유 | Virtual Console에서 좌석 점유 상태를 모의 변경할 수 있다. AAOS에는 SEAT_OCCUPANCY 속성과 접근 권한이 문서화돼 있다. | 대상 AVD에서 앱이 좌우 점유 변경을 읽는지 확인한다. 차종·권한 미지원과 값 미수신은 저장 옵션 선택으로 처리한다. |
| 차량 제원 | Vehicle SDK에 차량 정보가 있으며 일부 속성은 지원 여부 확인이 필요하다. | AVD와 대상 차량에서 사용 가능 여부를 확인한다. 높이 조회가 되더라도 루프박스는 사용자가 별도로 설정한다. |
| GPS와 지하 위치 | Pleos 순정 내비로 시설 입구까지의 경로를 요청할 수 있다. Playground의 SDK 신청 안내에는 FusedLocation이 준비 중으로 표시된다. | 차량 앱에서 쓸 수 있는 위치 제공 경로와 정확도를 별도로 검증한다. 지하에서는 오프라인 층·구역 안내와 사용자가 확인한 기둥 번호를 활용한다. |
| 차량 외부 카메라 | 외부 앱에서 순정 외부 카메라 영상에 접근할 수 있는 공개 SDK 경로를 확인하지 못했다. | API와 권한을 확인한 뒤 점유·표지 탐지를 검증한다. 확보 전에는 사전 영상·가상 상태로 시연한다. |
| 메시지 공유 | 휴대폰 카카오톡 공유는 사용자가 수신자를 선택한다. | 헤드유닛에서 휴대폰으로 넘기는 QR, 지도 링크, 문자·카카오톡 수신 흐름을 각각 테스트한다. |

SDK 신청 안내상 Vehicle State와 NaviHelper는 별도 신청 없이 사용할 수 있다. 개별 속성의 접근 권한과 실제 차량 지원은 대상 환경에서 확인한다.

## 검증 환경

- **Codex Cloud**에서 GitHub 저장소를 열고 Android SDK와 Gradle 의존성을 설정한다. 공식 샘플 빌드 성공 여부와 필요한 설정을 기록한 뒤 앱 빌드·단위 테스트를 진행한다. 가속 에뮬레이터 실행 가능 여부는 해당 환경의 `/dev/kvm` 접근과 `emulator -accel-check` 결과로 확인한다.
- **로컬 Android Studio**에서 공식 Pleos Connect Emulator v2.1.0, Automotive API 34 이미지를 사용한다. Apple Silicon에서는 공식 ARM64 이미지를 설치한다. Cold boot와 RAM 4GB는 공식 설치 가이드 기준이다.
- **HTML 미리보기**는 차량 화면과 조작 흐름을 빠르게 검토할 때 선택할 수 있다. 데이터와 SDK 결과를 가상 입력으로 표현한다.

## 실행 체크리스트

- [ ] 공식 내비 샘플의 APK 빌드와 Pleos AVD 설치·실행
- [ ] 목적지 2곳의 경로 결과·내비 도착 핀·접근 차로 비교
- [ ] Virtual Console 좌석 변경, 권한 없음, 미지원 상태 재현
- [ ] 주행 제한 중 설정·공유 조작과 정차 복귀 후 화면 확인
- [ ] GPS 신뢰도가 낮거나 네트워크가 끊긴 상태에서 층·구역 안내 확인
- [ ] 정차 위치 수정 후 휴대폰에서 공유 문구와 링크 확인

2026-11-11에 도착자리와 공식 샘플 APK 빌드, 추천 단위 테스트와 모의 SMS 서버 검증을 진행했다. 현재 실행 결과와 AVD·실차 검증의 구분은 [구현·검증 기록](08-verification.md)에 정리한다. 빌드 성공만으로 위 기기 체크리스트를 완료 처리하지 않는다.

- [Pleos Emulator 설치](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator) · [공식 내비 샘플](https://github.com/Pleos-Playground/pleos-sample-navi-app) · [NaviHelper 목적지](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/NaviHelper/destinations)
- [SDK 사용 조건](https://document.pleos.ai/docs/pleos-only/create-my-project) · [Vehicle SDK](https://document.pleos.ai/api-reference/connect-sdk-pleos/Vehicle/intro) · [Virtual Console](https://document.pleos.ai/docs/pleos-only/app-test-guide/virtual-console-guide) · [AAOS 좌석 점유](https://developer.android.com/reference/android/car/VehiclePropertyIds#SEAT_OCCUPANCY)
- [Pleos 하드웨어 접근 정책](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/platform-api-architecture) · [Codex Cloud 실행 환경](https://learn.chatgpt.com/docs/environments/cloud-environment) · [Android Emulator 가속](https://developer.android.com/studio/run/emulator-acceleration?hl=ko)
