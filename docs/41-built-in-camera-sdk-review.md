# 빌트인캠 영상 접근 · 공개 SDK 재확인

2026-11-14 UTC. 사용자 질문에 따라 공식 문서를 다시 조회했다. **확인한 공개 Pleos Connect SDK 문서에서는 빌트인캠 실시간 프레임/녹화 파일을 일반 앱이 가져오는 API를 찾지 못했다.** 모든 차량/비공개 제휴 인터페이스에 기능이 없다고 단정하는 결론은 아니다.

## 근거

- [공식 SDK 개요](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/): Vehicle/NaviHelper/음성/FusedLocation 모듈 설명. 빌트인캠 영상 스트림/녹화 파일 제공 API를 확인하지 못함.
- [공식 하드웨어 접근 정책](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/platform-api-architecture): 차량 하드웨어 직접 접근 금지, SDK 표준 플랫폼 API로 접근. 모바일 API의 차량 지원도 확인 필요. 따라서 Android CAMERA 권한만으로 빌트인캠에 접근할 수 있다고 가정하지 않음.
- [SDK 릴리즈 노트](https://document.pleos.ai/docs/pleos-only/release-note/sdk-release-note): Vehicle2.3.2/NaviHelper3.0.5 등 공개 변경에서 빌트인캠 프레임/녹화 파일 접근을 확인하지 못함. NaviHelper CameraType은 단속 카메라 안내이며 차량 카메라 영상 API가 아님.
- [SDK 사용 신청](https://document.pleos.ai/docs/pleos-only/create-my-project): 제휴 전용 항목의 일반적인 절차는 설명하지만 빌트인캠 API의 존재/제휴 제공을 보장하지 않음.

## 서비스에 적용할 판단

72초 영상 상단은 실사 대체 자료를 편집한 UX다. 빌트인캠 연동이 구현되거나 공식적으로 가능하다는 증거가 아니다. 기존 앱은 사전/선택 영상 분석이며 실제 카메라 연결은 없다. 영상·OCR 분석 능력과 차량 영상 접근 권한을 별도 조건으로 둔다.

자동 층/기둥 인식은 **차량 영상 제공 API·권한·지원 차종과 주행/주차 중 허용 범위를 공식 확인해야 하는 조건부 기능**이다. 카메라 접근이 준비되지 않으면 기본 도착 안내는 사전DB/지도와 사용자가 확인한 주차 단서로 동작하고, 시연/분석은 출처 표시 사전 영상 또는 사용자가 적법하게 제공한 파일을 입력한다. 녹화 파일의 자동 탐색/임의 접근 가능성을 가정하지 않는다.

제조사/공모전 담당자에게 확인할 내용은 일반 앱용 라이브 프레임 API, 저장 영상 전달 API, 별도 제휴/서명/권한, 지원 차종, 운행 상태별 제한이다. 이번에 문의를 발송하거나 지원 답변을 받은 것은 아니다. 공개되지 않은 API를 우회하지 않는다. 앱/APK/기존 데이터는 변경하지 않았다.
