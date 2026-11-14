# 다음 실개발 — 다중 탑승 프리셋과 이번 구성 저장

2026-11-14 UTC. 현재 브랜치는 `feat/arrival-context-ux`, Draft PR은 [#2](https://github.com/leemh-dev/dochagjari/pull/2)다. 기존 `feat/android-submission-mvp`/PR#1과 사용자 데이터를 보존한다. [정리 전 다음 작업 문서](https://github.com/leemh-dev/dochagjari/blob/fa155b4a5ac1feab2e4a5130d1cd3f86c95f57d9/docs/09-next-development-prompt.md)는 이력으로 남긴다.

읽기 순서: [개발 기준34](34-implementation-restart.md) → [탑승 설계33](33-occupant-modes-and-trip-context.md) → [최신 UX31](31-road-scenario-ux-concepts.md) → [코드 보존/교체35](35-code-retention-and-cleanup.md). 제출 범위는 [docs07](07-development-plan.md), 실제 검증은 [docs08](08-verification.md), 영상은 [docs32](32-demo-video-production-plan.md)다. 이전 단계 문서는 필요한 저장 계약/이행/검사 근거를 읽을 때 참고한다.

[Cloud개발↔Mac Pleos단계표36](36-cloud-mac-development-workflow.md)을 함께 적용한다. 매 단계 Cloud 검사와 Mac 확인의 소스커밋/상태를 따로 기록한다. Mac M1/8GB는 외장SSD·AVD한대·Cloud빌드 전달을 기본으로 하며 연결이 없어도 독립 Cloud개발을 계속한다. 36의Mac용프롬프트에는 실제 전달APK/해시/인증서와 확인항목을 채운다.

현재 첫 작업은 **모델·영속저장·옛 설정 이행**이다. 화면 전면 교체/모션영상/Pleos 재부팅을 첫 저장 작업과 한 번에 묶지 않는다. 다음 프롬프트를 사용한다.

```text
도착자리의 다중 탑승 프리셋/이번 탑승 구성 저장 개발을 진행해줘.
AGENTS.md·README·docs34/33/31/35/36·docs07/08과 PR #2·최신 원격/CI를 먼저 확인해.
feat/arrival-context-ux에서 이어가고 기존 PR #1/브랜치·다른 변경·사용자 데이터를 보존해. 자동 병합하지 마.
현재 앱 기준은0.9.0/9·e2b93ba, 기존 APK와 실제검증은 docs/evidence/android-product-ux/README.md를 대조해.
첫 구현은 다음 범위야.
- 내비와 독립된 여러 탑승 프리셋, 이번만 적용할 구성, 센서 점유 관측을 분리해.
- ProfileRepository/DataStore의 순차쓰기·revision·SAVED/persisted·실제재조회를 유지해.
- 기존 단일 좌우 역할/카시트와 이동조건을 이행하고 기존 방문UUID/snapshot·주차/확정알림/outbox를 보존해.
- 화면 변경/저장대기/완료/실패를 구분하고 저장실패·연속변경·즉시종료/새인스턴스·예전파일 이행을 검증해.
- 이번만 변경이 기본프리셋을 덮어쓰지 않게 하고 탑승세션을 방문/알림세션과 합치지 마.
- 센서 점유만으로 아이/환자/신원을 판정하거나 뒤좌우EMPTY만으로 혼자라고 확정하지 마.
- 실제 음성/센서가 없으면 출처를 표시한 텍스트/TEST 어댑터로 같은 로직을 개발해.
기존 화면은 ProductApp→개발자도구→ArrivalApp 경로에서 사용 중이므로 통째로 지우지 마.
대체 구현/호출자/저장키/자산/회귀검사를 확인한 단위에서만 옛 코드를 제거해.
이 저장 단위가 검증되면 독립 설정 UI로 연결하고 최신 디자인6개는 기준manifest와 함께 보존해.
각 단계는 Cloud 구현/검사→소스커밋/버전/app·testAPK해시/인증서/검사결과 전달→Mac Pleos확인→증거기록으로 나눠.
Cloud와 Mac의 진행/통과/실패/차단/미검증을 따로 표시하고 서로 다른 커밋의 검증을 합치지 마.
Mac이 꺼져 있으면 독립 Cloud개발을 계속하고 실제 연결 후 해당커밋의 기기증거를 추가해.
Mac은 M1/8GB·외장SSD의기존체크아웃/SDK/AVD/캐시를 쓰고 에뮬레이터한대만 실행해. Cloud빌드APK확인을 우선해.
Mac빌드가 필요하면 AVD를 종료하고 worker1/heap1536MiB로 빌드한 뒤 기기를 다시 실행해.
36의Mac전달프롬프트를 실제산출물값으로 채워 제공하고 같은실행 app/test 인증서쌍을 대조해.
실제 기기 단계에서는 접근 가능한 기기부터 확인하고 transport=device/boot1/package/activity/각rc0·설치직전재확인 뒤 설치해.
새 근거 없는 Pleos 재부팅·키 복사/편집·인증우회·Always allow 선택은 하지 마. 기존 기기 데이터를 지우지 마.
SDK요청/콜백·실제핀·입차, 일반Android/TEST·MOCK·사전영상·실차를 구분하고 확인되지 않은 위치/거리/경로를 만들지 마.
중요 결과/단계 전환과 긴 실행 중 약1분마다 현재 단계·차단·다음을 공유해.
앱을 실행하면 최초/주요전환/오류·수정 후 화면을 확보 즉시 채팅 이미지로 보여줘. 예전 캡처를 새 실행처럼 쓰지 마.
기능 단위 커밋/푸시·PR #2·docs10/08/12/18·정제한 증거를 갱신해.
APK가 바뀌면 실제파일의 버전/코드/SHA256/서명을 대조해. debug/test개발서명·release unsigned와 비밀값 비공개를 유지해.
기존 통과를 새 검사로 세지 말고 변경에 맞는 검사를 실행해.
마지막에 실제 구현·검사/미검증·PR/APK/증거 링크와 다음 실행 프롬프트를 파일 및 채팅에 알려줘.
```
