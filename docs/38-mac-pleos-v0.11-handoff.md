# Mac Pleos 인계 · 0.11.0/11

> 2026-11-14 영상 범위 정정: 기존90초/20초는 앱 단독 기능기록이다. 요청한 **실제 Pleos3분할 대표시연은 미완료**다. [3분할 UX합성 예상도](evidence/demo-video/three-panel-concept/README.md)를 별도 제공하며 실제 통합 성공으로 세지 않는다. [실제 완료 기준39](39-three-panel-demo-gap.md).

2026-11-14 UTC. Cloud 구현과 일반 Android 검증을 마쳤다. 다음 단계는 **같은 APK의 Mac Pleos 배치·실제 SDK 확인**이다. 이 세션에는 Mac 접속 경로가 없어 실행하지 않았다. 과거 M1 검사 성공을 이번 버전 성공으로 세지 않는다.

## 전달할 파일

- 코드: **f0640b00da92409c265cfd5563238e0d972edcaa**, 앱 **0.11.0/versionCode11**.
- 실제 검사/녹화 APK: [artifact14104088875](https://github.com/leemh-dev/dochagjari/actions/runs/43569033733/artifacts/14104088875), 이름 `tested-device-apks-and-metadata`.
- [APK 메타데이터](evidence/arrival-panel/f064-device/apk-metadata.json) · [SHA256SUMS](evidence/arrival-panel/f064-device/SHA256SUMS). debug/test의 동일 개발 인증서는 `5238432ff3515f6a6011ab89e758d8abe9dacb16150e9ef4d71b0ac7296d7b93`이다. 공개 인증서 지문이며 개인키를 복사하지 않는다.
- debug SHA256: `b704d03bda46491fd6d6a7f86ccb0e85cd4d33cc40ec7439ac24d6a57de7ddf4`.
- test SHA256: `6ba9f8d6924a39efa41a490a3cb19219022b28188538784ac46c9ad0c8f20215`.
- [검사/원본 화면](evidence/arrival-panel/f064-device/README.md) · [90초 시연/20초 모션](evidence/demo-video/README.md).

실제 APK ZIP에서 arm64-v8a/x86_64 네이티브 라이브러리를 확인했다. 이는 M1 실행/성능 검증이 아니다.

별도 verify runner의 3APK artifact14104315679는 개발 인증서/bytes가 다르다. Mac 전달에는 위 실제 검사 쌍을 사용한다. 이전 Cloud/Mac 서명과도 다를 수 있으므로 기존 앱을 삭제하지 않고 **새 전용 테스트 AVD**를 쓴다. release는 unsigned이며 이번 인계에서 설치 대상이 아니다.

## 자원과 저장

[SSD 실행기](../scripts/pleos/macos-ssd-env.sh)를 사용한다. 저장소·SDK·AVD·다운로드·Gradle/임시파일·증거를 기존 외장SSD 개발 폴더에 둔다. SSD가 없으면 내장 디스크로 조용히 대체하지 않는다. 기존 체크아웃/이미지를 재사용하고 중복 clone/다운로드를 먼저 하지 않는다.

M1/8GB에서는 AVD 한 대만 실행하고 Cloud APK를 설치한다. 로컬 Gradle 빌드와 에뮬레이터/영상 렌더를 동시에 돌리지 않는다. 과거 측정에는 메모리 압력 경고와 swap3.6~3.9GiB가 있었으므로 이번 버전의 안정성을 다시 측정한다. 이 과거 수치가 이번 실행 결과는 아니다.

```bash
git status --short
bash scripts/pleos/macos-ssd-env.sh adb devices -l
bash scripts/pleos/macos-ssd-env.sh emulator -list-avds
df -h .
memory_pressure -Q
sysctl vm.swapusage
```

로컬 변경이 없고 대상 브랜치가 맞을 때만 fetch/fast-forward한다. reset/강제 push/기존 AVD 삭제로 맞추지 않는다. APK 다운로드 경로와 증거 디렉터리도 실제 SSD 아래로 정한다. 아래 환경변수는 Mac에서 확인한 실제 값이어야 한다.

```bash
gh run download 43569033733 --repo leemh-dev/dochagjari \
  --name tested-device-apks-and-metadata --dir "$DOCHAGJARI_APK_DIR"
(cd "$DOCHAGJARI_APK_DIR" && shasum -a 256 -c SHA256SUMS)
```

SDK의 apksigner로 app/test 인증서를 metadata와 대조한다. `DOCHAGJARI_SERIAL`은 실제 전용 AVD 시리얼, `DOCHAGJARI_APK`/`DOCHAGJARI_TEST_APK`는 검증한 APK 절대경로, `DOCHAGJARI_MAC_EVIDENCE`는 SSD 증거 경로다. 설치 직전 공통 helper를 다시 통과해야 한다.

```bash
bash scripts/pleos/macos-ssd-env.sh python3 scripts/pleos/adb_readiness.py \
  --serial "$DOCHAGJARI_SERIAL" --output "$DOCHAGJARI_MAC_EVIDENCE/pre-app.json" && \
bash scripts/pleos/macos-ssd-env.sh adb -s "$DOCHAGJARI_SERIAL" install -r "$DOCHAGJARI_APK"
bash scripts/pleos/macos-ssd-env.sh python3 scripts/pleos/adb_readiness.py \
  --serial "$DOCHAGJARI_SERIAL" --output "$DOCHAGJARI_MAC_EVIDENCE/pre-test.json" && \
bash scripts/pleos/macos-ssd-env.sh adb -s "$DOCHAGJARI_SERIAL" install -t -r "$DOCHAGJARI_TEST_APK"
bash scripts/pleos/macos-ssd-env.sh adb -s "$DOCHAGJARI_SERIAL" \
  shell am start -W -n dev.leemh.dochagjari/.MainActivity
```

예상 readiness는 transport=`device`, boot=`1`, package/activity=`found`, 각각 rc0이다. unauthorized이면 정상 AVD 화면의 일회 승인을 확인한다. 키 복사/편집·인증 비활성화·권한 우회·Always allow 선택을 하지 않는다. 준비가 안 되면 설치를 중단하고 로그를 남긴다. 같은 무가속 부팅을 반복하지 않는다. 위 명령은 **Mac에서 향후 실행할 절차**이며 Cloud에서 실행했다고 표시하지 않는다.

## 확인 순서

1. 앱 창과 실제 순정 내비의 분할 배치를 확인한다. 480×900 일반Android 성공이 Pleos 배치 증거는 아니다. 최초 화면과 영상/주차/5층 전환·닫기를 캡처한다.
2. 독립 탑승 설정의 이번만 변경/저장값 재조회·재시작 재확인, 주행/unknown 제한을 확인한다. 센서 점유로 아이/환자/앞좌석을 판정하지 않는다.
3. 내비 모의를 끄고 실제 NaviHelper 요청/콜백과 순정 핀을 각각 대조한다. 판교점 **37.3927854/127.1120699는 시설 대표 POI**이며 확정 차량 진입점이 아니다. 내비 계산 완료를 입차로 처리하지 않는다. 기존 SDK 시험 두 좌표는 README 기준으로 별도 TEST 목적지라고 기록한다.
4. Virtual Console 뒤좌우 Empty→Occupied→Empty 조작, 앱 SDK 초기값/변경 콜백·저장 옵션 제안을 각각 기록한다. 실제 차량 검증은 별도다.
5. 같은 방문의 명시 입차→생성 영상 분석→5층 동선→주차 저장·닫기·귀환을 확인한다. 저장된 후보/직접 확인 단서/현재 위치를 구분한다. 실제 카메라가 아니다.
6. 필요하면 전용 AVD에서 전달된 app/test로 표준 ADB instrumentation을 실행한다. `OccupantInstrumentedTest`, `ScenarioInstrumentedTest`, `ProductInstrumentedTest`는 각1건이며 TEST 입력을 쓴다. 실제 SDK/Console 검증이나 Gradle connected 실행으로 이름을 바꾸지 않는다. Cloud connected15건은 이미 통과했으므로 M1에서 전체 재빌드를 필수로 하지 않는다.
7. 실제 STT는 지원 인텐트/오디오와 복귀 동작을 확인한 경우만 성공으로 표시한다. `-no-audio` 환경에서는 음성 검증을 완료하지 않는다. OEM카메라·실차·실제SMS·AI자료보완은 현재 미검증/미구현 수준을 유지한다.

오류가 나오면 원본 화면/환경·대상 파일 지문/재현 순서를 남기고 Cloud에 전달한다. Mac에서 임의로 앱을 바꾸거나 기존 자료를 초기화해 맞추지 않는다. docs08/10/12/18과 이 문서·PR에 기능별 결과를 기록한다. 비밀값·전화번호·키·개인 이동정보는 공개하지 않는다.
