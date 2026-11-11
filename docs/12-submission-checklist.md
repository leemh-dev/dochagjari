# 11월 15일 제출 준비 체크리스트

2026-11-11 20:33 UTC · 기준 [개발계획](07-development-plan.md) · [최신 검증](08-verification.md)

## 제출 후보와 재현 자료

| 항목 | 현재 증거/상태 |
| --- | --- |
| 브랜치/PR | feat/android-submission-mvp, [PR#1](https://github.com/leemh-dev/dochagjari/pull/1) open/미병합 |
| 앱 버전/코드 | 0.2.0 / 2, APK 코드 3a01515. 이후 진단 도구·문서 변경은 앱 APK에 포함되지 않음 |
| APK | [artifact13871881801](https://github.com/leemh-dev/dochagjari/actions/runs/43055801516/artifacts/13871881801), debug/test/unsigned release·SHA256SUMS·metadata. GitHub 로그인 필요. 확인 시 만료 전(2027-02-09 만료 예정) |
| 해시·서명 | [실제 파일 metadata](evidence/v0.2-apk-metadata.json), [다운로드 대조](evidence/v0.2-downloaded-apk-verification.txt). debug/test는 동일 runner 개발 서명, 이전 Cloud 서명과 다르므로 새 테스트 AVD 사용 |
| 제출 release 서명 | **미완료**. unsigned release 제공. 키·비밀번호 없음. [README](../README.md)의 사용자 로컬 키 생성/서명/해시 절차를 실행하고 실제 서명 후 기록 갱신 필요 |
| 코드 검사 | 기존 a020643 [PR CI](https://github.com/leemh-dev/dochagjari/actions/runs/43062184213) 성공. 새 변경의 CI는 대기 상태를 별도로 확인 |
| 신규 진단 검사 | 공통 ADB 준비/설치 차단 7건 로컬 통과. [로그](evidence/adb-readiness-tests.txt). 실제 기기 성공과 구분 |
| 설치 전 확인 | `python3 scripts/pleos/adb_readiness.py --serial <전용AVD> --output /tmp/dochagjari-adb-readiness.json` 종료0일 때만 설치. transport/device + boot1 + package/activity found·각 종료0 필요 |

## 출품 범위별 완료 경계

| 기능 | 확인한 것 | 제출 전 남은 것 |
| --- | --- | --- |
| 픽업·주차·우회 | 일반 Android UI/앱 모델에서 후보 변경·후보 없음 | Pleos 실제 SDK 결과와 37.48544722/127.03636666, 37.48615/127.03715의 순정 핀 대조 |
| 좌석·저장 | 테스트 입력, DataStore 실제 완료값·프로세스 재시작 복원 | Console 좌우 Empty→Occupied→Empty, 앱 초기값/콜백/옵션 제안. Console·앱 수신·실차 각각 기록 |
| 병원 | 가상 병원에서 동행·휠체어·무단차 미확인 제외 | 실제 동선 근거가 없으므로 가상 표시 유지 |
| 영상 | 실제 일반 Android TFLite/ML Kit C12·생성/공개 영상·점유→우회 | Pleos 순정 외부 카메라·실차 미검증. 사전 영상 결과로 대체하지 않음 |
| 자동 알림 | 같은 세션 요청/상태 복원, 중복 방지·서버 대역, 앱 모의 완료 | Android 실제 서버 통신·SENS 수락·휴대폰 수신은 별도 검증. 자격증명/등록 발신번호/동의 수신자 없으면 모의 유지 |
| Pleos 환경 | 과거 KVM boot 로그 관찰, ADB 서비스 준비 미확인 | 현재 수동 진단43095641582의 최신 결과 확인 → ADB 오류/시리얼/공식 프로필 조사 → 준비 확인 후 설치 |

새 AVD에서 실제 서명 APK를 설치한 뒤 두 내비 핀·좌석·영상·설정 유지·주행 제한·알림 결과를 시연하고 녹화합니다. 이번 작업에서 맥북·실차·실제 SMS·제출 서명을 실행한 것으로 표시하지 않습니다.

11월 14일 18:00 제출 후보 고정·녹화·커밋/해시 대조, 11월 15일 18:00 내부 제출 목표는 [개발계획](07-development-plan.md)의 일정입니다. 현재 실제 제출 완료나 전체 실연동 완료 상태는 아닙니다. 제출 양식·시연 영상·공개 저장소·설치 산출물의 버전/코드/검증 표시를 마지막으로 대조해야 합니다.
