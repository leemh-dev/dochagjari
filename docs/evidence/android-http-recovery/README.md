# Android 실제 HTTP 응답 유실·종료/재시작 복구

2026-11-12 UTC. 코드2c2feb0 [PR43134026080](https://github.com/leemh-dev/dochagjari/actions/runs/43134026080)·push43134018393 전체 성공, artifact13907511615. 앱 런타임3a01515 이후 동일,0.2.0/code2. Generic Android API34 Google APIs/x86_64 KVM의 새 전용 AVD다.

실제 앱 UI로 서버 모의 발송을 설정하고 동의/위치 확인했다. Python loopback mock 서버가 POST를 DB에 저장한 뒤 응답을 보류했다. 앱을 force-stop하고 서버도 종료한 뒤 동일 DB로 다시 시작했다. 다른 앱 PID에서 같은 세션/확정 위치/단서/수신자 ID/모드가 복원됐으며, 비어 있는 토큰을 다시 입력하고 결과 확인을 눌러 GET200으로 모의 결과를 복구했다.

[요약](summary.json): GET404→POST200(응답 보류)→재시작 후 GET200, POST총1회, DB행/요청지문 동일, 공급자 호출0. PID8659→9445. 무작위 세션/공개 시연 좌표와 가상 단서뿐이며 실제 수신번호/토큰/키는 기록하지 않았다. 요청 본문/DB/임시 토큰 파일은 artifact에 포함하지 않는다.

PNG는 adb screencap 원본, XML은 같은 단계의 실제 UI 상태다. 모의 서버 결과이며 SENS 수락·실제 휴대폰 수신·Pleos 차량 신호·실차 검증이 아니다. 실제 공급자와 pending/unknown의 모든 조합을 기기에서 검증했다는 뜻도 아니다.

실행: 새 전용 CI AVD에서 `DOCHAGJARI_FRESH_TEST_AVD=1 python scripts/android_server_smoke.py`. 실제 사용자 기기/기존 데이터를 초기화하는 명령이 아니다. fixture는 실제 전화번호 대신 비전화 placeholder와 공급자 실행 금지 대역만 사용한다. 서버 재시작 때 DB를 삭제하거나 새 세션을 만들지 않았다.
