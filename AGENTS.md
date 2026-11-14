# 도착자리 저장소 작업 지침

- 제출 범위와 완료 기준은 `docs/07-development-plan.md`를 우선한다. 현재 상태는 `docs/08-verification.md`, 다음 작업은 `docs/09-next-development-prompt.md`를 읽는다.
- 이번 인계의 앱 기준은0.11.0/11·f0640b0이다. 최신 실제 결과는 `docs/37-current-implementation.md`, 다음 Mac Pleos 확인은 `docs/38-mac-pleos-v0.11-handoff.md`를 읽는다. 이후 문서/영상 커밋과 실제 APK 코드 기준을 구분하고 완료한 저장/UI를 다시 구현하지 않는다.
- 현행 실개발은 `feat/arrival-context-ux`/PR #2다. `docs/34-implementation-restart.md`→`docs/33-occupant-modes-and-trip-context.md`→`docs/31-road-scenario-ux-concepts.md`를 현재 기능/UX 기준으로 읽고, 영상은 docs32·코드 정리는 docs35를 따른다. 과거 단계별 기획/다음 작업은 이행·검증 이력이며 최신 설계를 덮어쓰지 않는다. 기존 코드 삭제는 호출/저장/검사 연결과 대체 구현을 먼저 확인한다.
- 각 개발 단위는 `docs/36-cloud-mac-development-workflow.md`에 따라 Cloud 구현/검사와 Mac Pleos 확인 상태를 따로 보고한다. Mac M1/8GB는 외장SSD·AVD한대·Cloud빌드 전달을 기본으로 하고 미연결이면 독립 Cloud개발을 계속한다. 과거Mac성공/일반Android검사를 새Pleos성공으로 세지 않는다. 현재단계/차단/다음과 확보즉시 실제화면을 공유한다.
- 주요 조사·구현·검증·실패·해결·차단 절차를 `docs/10-development-log.md`에 지속적으로 기록한다. 날짜, 환경·명령, 실제 결과, 증거·커밋, 후속 작업을 남긴다. 코드 검토에서 추정한 문제와 실제로 재현한 문제를 구분한다.
- 기능 단위로 커밋·푸시하고 사용자가 요청한 PR에 변경·검증·제약과 문서 링크를 갱신한다. 관련 없는 변경을 덮어쓰거나 요청 없이 PR을 병합하지 않는다.
- 빌드 성공, 테스트 APK 생성, 기기 테스트 실행, SDK 요청, 순정 내비 핀, Virtual Console 조작, 앱 이벤트 수신, 실차 검증을 구분한다. 호스트 Tesseract 결과를 Android ML Kit 성공으로 기록하지 않는다.
- 실제 차량 API, AVD 입력, 앱 테스트 입력, 공개·생성 영상, 모의 SMS, 공급자 수락, 실제 휴대폰 수신을 각각 표시한다. 실행하지 못한 검사는 미검증과 이유를 남긴다.
- 작은 공개 가능 증거는 `docs/evidence/`, 큰 파일은 GitHub artifact 또는 Release에 보관하고 링크한다. APK를 갱신하면 코드 커밋·버전·SHA-256·서명 종류를 함께 기록한다.
- 비밀값·실제 전화번호·서명 키·개인 이동 기록을 코드·로그·문서·PR에 넣지 않는다. SMS는 기존 동의와 등록 수신자 범위를 유지하고 불명확한 결과를 새 세션 재발송으로 해결하지 않는다.
- 환경이 막혀도 독립적으로 가능한 구현·빌드·테스트는 계속한다. 기존 실패 진단을 읽고 같은 부팅·다운로드를 근거 없이 반복하지 않는다. 접근할 수 없는 맥북·기기에서 검증했다고 기록하지 않는다.
