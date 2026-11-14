# 음성 증상노트 · 기획과 공개 SDK 현실성 검토

> 후속 사용자 요구: [49·직전1분~종료까지 구간 기록/정비소 인계](49-symptom-session-and-service-handoff.md)가 구간/종료/스캐너 목표의 최신 기준이다. 아래 고정 후15초는 이전 제안이며 사용자 종료 방식으로 대체한다.

2026-11-14 UTC. 사용자가 음성 아이디어 **2번: 정비 증상 음성 기록 + 당시 차량 상태**의 고도화와 SDK 제공 여부 분석을 요청했다. 이번 결과는 기획/정적 조사이며 구현 승인이나 새 앱 성공을 뜻하지 않는다. 기존 도착자리·운전자 교대 후보와 앱 데이터는 보존한다.

## 1. 서비스 정의와 핵심 고객

**“차에서 느낀 이상을 잊기 전에 말로 남기면, 확인 가능한 당시 차량 상태와 함께 정비사가 읽을 수 있는 증상 기록으로 정리합니다.”** 작업명은 ‘증상노트’이며 이름은 미확정이다.

주 고객은 차량 지식이 부족한 공동 이용자와, 소음·진동·공조 이상처럼 간헐적 증상을 정비소에서 설명하기 어려운 운전자다. 기록은 언제, 어떤 조작/상황에서, 어디에서 어떻게 느꼈고, 반복됐는지를 남긴다. 경고가 관측되지 않아도 체감 증상은 보관한다. 경고 미관측을 정상 판정으로 사용하지 않는다.

가치는 녹음이나 계기판 숫자 나열보다 **발생 순간을 보존하고, 반복 기록을 묶고, 정비 상담에 전달하는 일의 완결**에 있다. 자동 고장 진단·고장 원인 확정·사고 예방 성능·수리비 절감은 현재 약속하지 않는다.

## 2. 대표 시나리오와 화면

다음은 설계 예시이며 실제 차량에서 수집한 기록이 아니다.

1. 앱을 열고 사용자가 기록 준비를 선택한다. 지원 필드와 수신 여부를 확인하고, 허용된 값만 짧은 메모리 버퍼에 유지한다. 앱이 꺼져 있던 과거 데이터를 복원한다고 약속하지 않는다.
2. 안전하게 정차한 운전자가 “방금 주차하면서 핸들을 돌릴 때 앞쪽에서 덜컥 소리가 두 번 났어요”라고 말한다. STT 최종 문장을 원문으로 보존하고 ‘소음/앞쪽/회전 중/두 번’을 **사용자 진술**로 분리한다.
3. 음성 시작 시점을 먼저 북마크하고, 해당 구간의 기어·조향각·누적 주행거리 등 **실제 수신한 값만** 연결한다. “방금”의 정확한 발생 시각을 모르면 시각 불확실성을 표시한다. 음성 종료 후 읽은 현재 값을 증상 발생 순간의 값으로 둔갑시키지 않는다.
4. 정차 화면에서 원문과 요약을 확인·수정한다. “차량 원인은 판단하지 않았습니다 / 당시 관측은 일부만 있습니다”를 구체적으로 표시한다. 저장 중/완료/실패를 구분한다.
5. 나중에 유사 증상이 기록되면 ‘비슷한 사용자 기록 3건’으로 모아 보여준다. 같은 기계적 고장이라는 뜻이 아니며, 자동 병합은 되돌릴 수 있다.
6. 정비소 방문 때 원문·발생 조건·각 관측 시각·누락 항목을 포함한 한 장 요약을 사용자 확인 후 내보낸다. 정비사가 확인한 내용은 별도 사후 메모로 저장한다.

Pleos 화면은 차량 정보/순정 내비를 가리지 않는 앱 패널을 전제로 한다. 기록 준비 화면은 큰 음성 버튼과 짧은 상태, 완료 화면은 ‘말한 증상 / 확인된 상태 / 미확인’을 시각적으로 구분한다. 정차 시에만 상세 타임라인·수정·내보내기를 연다. 3분할 자동 제어·순정 내비 화면 위 임의 오버레이는 이번 서비스의 전제 조건이 아니다.

Gleo로 “증상노트에 기록해 줘”를 전달하는 UX는 후속 검증 대상이다. STT만 붙이면 순정 음성 호출/스티어링 휠 버튼으로 앱이 자동 실행된다고 설명하면 안 된다. 주행 중에는 검증된 호출 경로와 운전 제한 정책을 확보한 경우에만 짧은 북마크/음성 흐름을 제공한다. 초기 시연은 정차 상태로 완결한다.

## 3. SDK 제공 여부: 문서와 실제 AAR을 분리한 판정

조사 기준은 공식 Vehicle 2.3.2 문서와 **현재 프로젝트가 사용하는 같은 버전 AAR**, 공식 STT/TTS 3.3.1 문서다. `javap -v`로 Vehicle의 메서드·권한을 확인했다. STT/TTS는 이번에 바이너리 다운로드/컴파일하지 않았다. **아래 모든 항목의 새 Pleos 실행 결과는 미검증**이다.

| 필요한 기능 | 확인한 API / 근거 | 권한·조건과 MVP 판정 |
| --- | --- | --- |
| 차량 마이크 음성→텍스트 | `SpeechToText`의 `initialize/addListener/request/stop`, `ResultListener` | `pleos.car.permission.STT_SERVICE`. ON_DEVICE는 네트워크/서버용 앱 등록 불필요. 핵심, 한국어/마이크/최종 결과 실기기 검증 필요 |
| 녹음된 테스트 음성→공식 STT | `sendAudio(ByteArray)`, `completeAudioSend()` | ON_DEVICE 전용. 마이크 대신 PCM 입력 시험 가능. 공식 엔진 실행과 실제 마이크 입력 성공을 따로 기록 |
| 주행거리 | `Odometer.getCurrentDistanceDriven`, `registerDistanceDriven`, capability 조회 | `CAR_DRIVE`. 핵심 후보. 값의 단위/정의/수신 시각을 확인해 저장 |
| 기어 | `Gear.getCurrentGearSelectionState`, `registerGearSelection` | AAR에서 `CAR_DRIVE` 확인. 주차/후진 등 맥락 후보. 기어만으로 안전한 조작 상태를 확정하지 않음 |
| 조향각 | `SteeringWheel.getCurrentSteeringWheelAngle`, `registerSteeringWheelAngle`, capability 조회 | AAR에서 `CAR_DRIVE` 확인. ‘회전할 때 소음’의 유용한 맥락. 단위/0점/방향/수신 주기 미검증 |
| 브레이크 조작 | `Brake.getCurrentBrakePedalStatus`, `registerBrakePedalStatus` | `CAR_DRIVE`. 상태만 기록. 제동압/제동 성능 수치로 확대하지 않음 |
| 일부 경고 조회 | `Safety.getTelltalesInfo`, 오일/브레이크액 상태 getter | getter는 `CAR_SAFETY`. 지원된 시점 조회를 후보로 사용. DTC/정밀 진단으로 해석하지 않음 |
| 모든 경고의 연속 이벤트 | `checkTelltalesCapabilities`, `registerTelltales` | **AAR에 `PRIVATE_CAR_SAFETY`**. 일반 권한만으로 실시간 경고 전체 타임라인 가능하다고 주장하지 않음. MVP 제외 |
| 공조 상태 | `HVAC.getHvacAcState`, `getHvacFanSpeed` 및 관련 callback | `CAR_CLIMATE`. ‘에어컨 켜면 소리’ 같은 예시의 핵심 후보. 설정값과 실제 성능/고장 구분 |
| 연료/EV 충전량 | `Fuel.getFuelPercent`, `EvBattery.getEvBatteryStateOfCharge` | `CAR_ENERGY`. 차종/지원에 따라 선택, 두 파워트레인 값을 동시에 요구하지 않음 |
| 타이어 공기압 실측 수치 | `Tire.getTirePressure`, capability/이벤트 등록 | **`PRIVATE_CAR_SAFETY`**. 공개 함수 존재만으로 사용 가능하다고 판단하지 않음. MVP 제외 |
| 타이어 보조 조회 | `getLowTirePressure` | `CAR_SAFETY`이나 반환형은 Float. 이름만 보고 ‘저압 경고 boolean’으로 해석하지 않음. 의미/단위 확인 전 제품 필드 제외 |
| 음성 확인 읽어주기 | Pleos TTS `speak/stop` | 공식 TTS ON_DEVICE 지원. 선택 기능, 마이크/오디오 경합·권한·한국어 확인 필요 |
| 순정 Gleo에서 앱 기능 호출 | App Schema의 function / Gleo Action / Callback | 공식 가이드 있음. 증상 원문 전달/앱 실행/콜백을 실제 시험해야 함. 일반 LLM 추론 API가 아님 |
| 정비 보고서 저장/내보내기 | Android SQLite, `PdfDocument`, `FileProvider`, 공유 Intent | 앱 구현 영역. Pleos 문서 공유 대상 앱/권한/화면 가용성은 별도 검증. 안 되면 로컬 보고서까지로 표시 |
| 고장 코드·엔진 RPM·진동·소음 원인 분석 | 이번 공개 SDK 조사에서 필요한 경로 미확인 | DTC/OBD/raw CAN/정비사 전산/빌트인캠을 기본 기능으로 약속하지 않음. 별도 승인 없는 접근 시도 없음 |

특히 공식 권한 문서의 도메인 대표 권한만으로 개별 API를 판단하면 틀릴 수 있다. Tire의 실측 공기압과 Safety의 통합 경고 이벤트가 실제 사례다. 같은 Safety 안에서도 일부 개별 getter/callback은 공개 권한이므로 필요한 필드별로 판정한다. PRIVATE 권한을 매니페스트에 추가하면 해결된다고 가정하거나 우회하지 않는다.

차속은 현재 앱에 Android Automotive `CAR_SPEED` 선언이 있지만, 이 사실이 이번 새 SDK/에뮬레이터 차속 수신 증거는 아니다. 속도는 별도 API·권한·출처·실제 값 검증 후 선택 필드로 추가한다. 사용자가 말한 ‘저속’과 센서 실측 속도를 혼동하지 않는다.

증거: [AAR 해시/실행 종료 코드](evidence/symptom-sdk-feasibility/binary-inspection.json), [28개 선정 메서드/권한/시그니처](evidence/symptom-sdk-feasibility/vehicle-selected-methods.json), [재현과 조사 한계](evidence/symptom-sdk-feasibility/README.md).

## 4. AI가 맡을 일과 맡기지 않을 일

- 1차: 공식 STT로 인식하고, 앱 내부 규칙/템플릿으로 증상 종류·사용자 표현·발생 조건을 정리한다. 원문을 버리지 않고 해석 불확실성을 남긴다. 이것만으로도 수집→확인→반복 묶기→보고서까지 시연할 수 있다.
- 고도화: Gleo App Schema로 앱 기능 호출을 연결한다. 문서상 function calling은 확인했지만 임의의 긴 증상 기록을 모두 구조화해 준다는 보장은 없다. 먼저 작은 명령/응답 계약으로 검증한다.
- 외부 LLM은 핵심 의존성에서 제외한다. Pleos LLM SDK는 제공되지 않는다는 공지와 외부 STT 전송 불가 규칙을 따른다. 텍스트 요약 외부 API도 대회 허용 범위/동의/비식별·전송 정책을 별도 확인한 이후에만 고려한다.
- STT는 사람이 말한 소리 설명을 텍스트로 바꾸는 도구다. “덜컥”이라는 발화를 인식하는 것과 실제 기계음을 분석하는 것은 다른 기능이다. 자동 청음 진단/고장 확률/계속 주행해도 된다는 판정을 넣지 않는다.

## 5. 기록 품질과 저장 설계

제안 모델은 `SymptomEvent(id, driverAlias?, createdAt, userEstimatedOccurredAt?, uncertainty, originalText, confirmedSummary, source, status, schemaVersion)`와 `Observation(eventId, field, value?, unit?, receivedAt, monotonicTime, source, availability)`다. source는 `PLEOS_SDK / CONSOLE_DRIVEN_SDK / APP_TEST / USER_REPORTED / PCM_STT_TEST` 등을 구분한다. 실제 구현 시 SDK 수신과 입력 출처가 함께 표현되게 한다.

가용 상태는 `AVAILABLE / UNSUPPORTED / PERMISSION_DENIED / STALE / ERROR / NOT_OBSERVED`로 나눈다. 수신 안 됨을 0/정상으로 저장하지 않는다. callback 값마다 수신 시각을 남기고 비동기 조회 묶음을 원자적 ECU snapshot이라고 부르지 않는다. SDK에 센서 원시 시각이 없으면 앱 수신 시각이라고 표시한다.

짧은 상태 버퍼는 **앱의 명시적 기록 준비 동안만** 유지하는 설계다. 초기 제안은 발화 전60초/후15초, 실제 빈도·메모리·배터리 측정 뒤 조정한다. 연속 경고 PRIVATE 이벤트는 사용하지 않는다. 프로세스 종료/권한 철회/종료 시 수집 중지, 다음 시작에 몰래 음성/수집을 재개하지 않는다. 당시 버퍼가 없으면 기록에 공백을 표시한다.

편집 화면 값, STT 중간 결과, 사용자 확인, SQLite commit ACK는 서로 다르다. 같은 event ID를 유지하며 재시도하고 강제 종료 후 저장된 원문/확정 상태/관측을 실제 DB 재조회한다. 기존 도착자리 DB·설정·방문·알림은 변경하지 않고 신규 테이블/마이그레이션을 별도 검토한다. 연구만 한 현재 단계에서는 마이그레이션하지 않는다.

원시 PCM/차내 주변 음성은 기본 영속 저장하지 않는다. 차량 식별번호·정밀 위치는 핵심 기능에 불필요하므로 수집하지 않는다. 운전자 별칭은 선택이며 공동 이용자의 원문 공유는 별도 선택이다. 보고서는 원문과 관측/추론을 구분하고, 사용자가 확인한 후 공유한다. OEM 경고/설명서를 대체하거나 미관측 경고를 ‘이상 없음’으로 표시하지 않는다.

## 6. 현대 기존 서비스와 차별성·냉정한 가치 평가

현대 리모트케어는 지원 차종/가입 조건에서 이상 데이터 감지→알림→상담/정비 연계를 이미 제공한다. ‘AI 차량 진단’으로 홍보하면 중복과 진단 신뢰도 문제가 크다. 이번 서비스는 **운전자의 체감 증상·발생 조건을 차량에서 즉시 기록하고 정비 상담용으로 정리하는 사용자 측 증거 기록**에 집중한다. 같은 전체 흐름의 현대 제공 여부는 [47](47-hyundai-only-voice-comparison.md)에서 미확인으로 기록했으며, 전세계 최초/현대에 절대 없음은 주장하지 않는다.

유효성은 아직 가설이다. 정비사에게 시각/기어/공조 상태가 실제 문진에 도움이 되는지 확인해야 한다. ‘음성 메모보다 얼마나 나은가’에 답하지 못하면 독립 앱으로서 약하다. 차속·RPM·DTC 없이도 정비에 쓸모 있다는 근거를 실제 인터뷰로 얻어야 한다.

추천 검증은 운전자5명/정비사3명에게 동일 증상을 자유 설명과 한 장 요약으로 비교하게 하는 것이다. 이는 **향후 표본 계획**이지 실시 결과가 아니다. 성공 판단은 발생 조건 누락 감소, 정비사의 추가 질문 수, 요약 오류/과장 여부, 운전자의 다시 사용할 의향이다. 안전 진단 정확도·수리비 절감율 같은 검증 안 된 성과 숫자는 넣지 않는다.

## 7. 실개발 순서와 통과 문턱

| 단계 | Cloud 작업 | Mac Pleos 확인 | 완료 기준/중단 기준 |
| --- | --- | --- | --- |
| 0 SDK 검증용 작은 앱/모듈 | STT ON_DEVICE + Vehicle 최소 getter/callback, 출처/오류 상태 | 전용 AVD에서 STT PCM/실제 마이크를 각각 시험. Odometer + HVAC 또는 Safety 공개 필드 확인 | 문서→컴파일→초기화→권한→실제 값 단계를 따로 기록. 전부 APP_TEST이면 SDK 검증 완료가 아님 |
| 1 증상 한 건 완결 | 원문/상태 snapshot/확인/저장ACK/DB복원 | 정차 음성→확인→종료/새 PID 복원 | 필드별 시각/누락, 동일ID, 저장실패 복구 |
| 2 반복 기록·상담 카드 | 수정 가능한 유사 기록 묶기, 한 장 요약 | 실제 입력 문장 오인식/정정/두 증상 구분 | 다른 증상 과도 병합 방지, 원문 보존, 미지원 값 날조0 |
| 3 Gleo 호출·내보내기 | App Schema/Callback, PDF/명시 공유 | 실제 Gleo 앱 호출과 보고서 공유 대상 유무 | 안 되면 앱 내 음성/로컬 보고서로 한계 표시. 자동 정비소 전송 성공 주장 금지 |
| 4 시연 | 실제 앱 흐름 촬영/편집/증거 | Console 조작→SDK수신→증상 연결을 한 흐름으로 확인 | TEST/PCM/마이크/SDK/실차를 각각 표기. 실차 불필요 조건은 공식 공지45 따름 |

초기 SDK 통과 최소 조합은 공식 STT 최종 텍스트 + 누적 주행거리 + 한 가지 관련 공개 상태다. 조향각/기어/브레이크는 추가 실익이 큰 후보지만 실제 AVD 값/입력 지원을 확인한 것만 쓴다. 차별화에 필요한 맥락 필드를 확보하지 못하면 음성메모로 축소됐다고 평가하고, 구현을 크게 늘리기 전에 가치/범위를 재검토한다.

마이크 실패 시 PCM 공식 엔진 시험은 독립 진행 가능하지만 ‘차량 마이크 성공’으로 세지 않는다. 외장SSD/M1·8GB/AVD 한 대/Cloud 빌드를 기본으로 한다. 실제 기기를 찾고 ADB transport/boot/package/activity 및 설치 직전 준비를 확인한다. 데이터 삭제·인증 우회·키 복사·근거 없는 Pleos 재부팅을 하지 않는다.

필수 회귀 시나리오는 무음/오인식/부분 결과/사용자 취소, 뒤늦은 증상 설명, 이전 관측 없음·오래된 값·단위 미확인, 권한 거부/PRIVATE 차단/SDK 끊김, 연속 두 건/저장 실패/새 PID, 주행 또는 상태 미확인에서 상세 조작 제한, 요약 원문 차이/내보내기 취소다. 기존 저장·좌석·방문·확정 알림 복구는 보존한다.

## 8. 공식 자료와 확인 수준

- [STT 소개/모드/버전](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/intro), [권한](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/declaring-permissions), [마이크 시작·종료](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/start-and-stop), [PCM 입력](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/sending-audio-data).
- [Vehicle 소개](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/intro), [개별 권한 우선 규칙](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission), [API 도메인 목록](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/entry).
- [Odometer](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/odometer/entry), [Safety](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/safety/entry), [Tire](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/tire/entry), [SteeringWheel](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/steering-wheel/entry). 일부 웹 메서드 본문이 서명 없이 탐색 메뉴만 렌더되어 버전 명확한 AAR로 서명/어노테이션을 대조했다.
- [TTS](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro), [Gleo App Schema와 호출 구조](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/core-concepts).
- [현대 리모트케어](https://www.hyundai.com/kr/ko/service-membership/car-management-service/remote-care/preemptive-care), [공모전 SDK 범위 공지](https://appdevchallenge.auto-hmg.io/notice/c0a1a000-2026-4923-8000-000000000008), [전체 규정 검토45](45-contest-qna-complete-review.md).

결론: **공개 문서/API 구성으로 실현 가능성이 있는 제한된 MVP**다. 카메라·정밀 진단·비공개 데이터가 없어도 서비스 흐름을 설계할 수 있다. 그러나 현재 앱에는 새 STT/권한/증상 저장/보고서가 구현되어 있지 않고 새 Pleos 실행도 하지 않았다. 다음은 기능 대량 구현이 아니라 단계0의 작은 SDK 실행 검증이다.
