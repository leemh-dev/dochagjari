# 증상노트 SDK 정적 조사 증거

2026-11-14 UTC · Cloud Linux. 새 앱/기기/마이크/Console/SDK 서비스 호출은 실행하지 않았다. 연구 결과는 [기획48](../../48-voice-symptom-recorder-plan.md)에 있다. 앱0.11.0/11·f0640b0와 기존 APK/해시/서명은 변경 없음.

## 실제 실행한 확인

프로젝트 의존성 `ai.pleos.playground:Vehicle:2.3.2`의 기존 Gradle 캐시 AAR을 Python `zipfile`로 열어 `classes.jar`를 조사용 `/workspace/research/symptom-sdk-review/`에 추출했다. 다음 형태로 JDK17 명령을 실행했다.

```sh
/workspace/tools/jdk17/bin/javap -classpath /workspace/research/symptom-sdk-review/vehicle-2.3.2.jar ai.pleos.playground.vehicle.api.Safety
/workspace/tools/jdk17/bin/javap -v -classpath /workspace/research/symptom-sdk-review/vehicle-2.3.2.jar ai.pleos.playground.vehicle.api.Tire
```

`javap` 서명 조회는 Vehicle·Brake·DrivingMode·EvBattery·Fuel·Gear·HVAC·Odometer·Safety·SteeringWheel·Tire·TurnSignal에서 rc0이었다. 최초 `Steeringwheel` 오기는 rc1이었고 정확한 `SteeringWheel`로 수정했다. bare `jar/javap`는 PATH에 없어 Python zipfile/명시적 JDK 경로로 해결했다. 이는 앱 실패/성공 검사 수가 아니다.

[binary-inspection.json](binary-inspection.json)에 AAR/classes SHA-256와 조회 결과, [vehicle-selected-methods.json](vehicle-selected-methods.json)에 28개 선별 공개 시그니처와 `RequiresPermission`을 기록했다. `javap -v`의 각 public abstract method 블록에서 권한 value를 추출했다. proprietary AAR/JAR/전체 바이트코드는 저장소에 추가하지 않았다.

중요 결과: `getTirePressure`/타이어 수치 callback과 `checkTelltalesCapabilities`/`registerTelltales`는 PRIVATE 권한이다. `getTelltalesInfo` getter는 일반 CAR_SAFETY이며 조향각/기어 조회는 CAR_DRIVE다. 이름이 비슷한 `getLowTirePressure`는 Float 반환으로 의미/단위를 확인하기 전 제품 데이터로 쓰지 않는다.

## 확인하지 않은 것

컴파일/새 APK/설치/음성 인식 정확도/권한 실제 부여/AVD 값/실차/정비소 효용은 미검증이다. STT/TTS3.3.1은 공식 웹 문서 근거이며 바이너리는 이번에 받지 않았다. 현재 매니페스트에 STT_SERVICE와 이번 계획의 신규 도메인 권한이 없음을 읽기만 했다.

공식 SDK 웹 페이지의 일부 메서드 상세가 서명을 렌더하지 않아 AAR을 대조했다. STT 시작/종료 페이지는 추측 URL 실패 후 공식 소개 페이지의 링크로 `start-and-stop`을 확인했다. PRIVATE 권한은 접근 가능/불가능을 실행한 결과가 아니라 정적 제한 근거다. 일반 앱에 부여 가능하다고 추정하지 않으며 우회하지 않는다.
