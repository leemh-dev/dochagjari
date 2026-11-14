# 영상 목록 · 3분할 예상도와 앱 단독 기능기록

**현재 대표 UX 검토본:** [백화점 도착 후72초 시연](store-arrival-v2/README.md). 이동 중 넓은 내비→도착 후3분할→몰 주차장 실사→정차→5층 경로를 같은 시간순으로 연결했다. 실제 앱/실제 Pleos 녹화와는 구분하며, 아래 기존20초·90초는 보존한다.

**영상 구분 정정:** 기존90초/20초는 앱 단독 기능기록이며 요청한 Pleos3분할 대표시연이 아니다. [새3분할 구성 예상도20초](three-panel-concept/README.md)는 기존 생성 정지시안+상단 공개 실사 전방 주행20초+하단 실제 앱 지도 재배치의 **UX 합성**이다. 실제 Pleos 동시 실행은 아직 미검증이다. [완료 기준/남은 작업](../../39-three-panel-demo-gap.md).

2026-11-14 UTC. **0.11.0/11·f0640b0**의 실제 일반Android API34 실행에서 제작했다. TEST차량/입차/주차·MOCK내비·생성영상·TEXT명령이다. 실제Pleos/외부카메라/STT/실차/SMS로표시하지않는다. 음성나레이션은없고한국어자막을사용한다.

- [앱 단독90초 기능기록](dochagjari-android-demo.mp4): 실제 스크린샷 장면편집+실제파란선8초 동작구간. 장면편집/시간생략을 항상표시한다.
- [앱 단독20초 연속 모션](dochagjari-motion-sample.mp4): 원본40.5초부터20초를연속추출. 생성영상/5층전환과파란선동작을확인한다.
- [원본89.174211초 무편집녹화](../arrival-panel/f064-device/product-ux/arrival-context-demo.mp4).
- [한국어자막SRT](dochagjari-android-demo.srt) · [실제파일/입력별SHA256·FFprobe](video-metadata.json).

FFmpeg출력은1920×1080/30fps/H.264/yuv420p다.90초/20초 길이와전체디코딩을확인하고대표프레임에서글자/표시/실제경로재생을열람했다. 실제기기APK쌍은[metadata](../arrival-panel/f064-device/apk-metadata.json)와[artifact14104088875](https://github.com/leemh-dev/dochagjari/actions/runs/43569033733/artifacts/14104088875)다. 원본54장/각검사는[device artifact14103609767](https://github.com/leemh-dev/dochagjari/actions/runs/43569033733/artifacts/14103609767)에있다.

8초구간은원본46.5초부터다. 같은원본의연속프레임에서파란선이실제로늘어나고완료되는것을확인했다. 정지PNG만으로애니메이션성공을주장하지않는다.90초편집본의정지장면과동작구간은metadata에서구별한다. 원본이약89초인이유는screenrecord90초제한의실제인코딩기간이며90초편집본과혼동하지않는다.

재현(저장소루트,FFmpeg/FFprobe/NotoSansCJK필요):

```bash
python3 scripts/render_evidence_demo.py \
  --reports docs/evidence/arrival-panel/f064-device \
  --metadata docs/evidence/arrival-panel/f064-device/apk-metadata.json \
  --output /tmp/dochagjari-video-reproduction --route-start 46.5
```

글꼴은시스템NotoSansCJK(SIL OFL)를래스터출력했다. 카메라클립은앱의생성테스트자산이며참고용외부사진을무단삽입하지않았다.210초 UX컨셉본편은[기존제안](../../32-demo-video-production-plan.md)으로남으며이번90초실행편집본의완료와구분한다.

카메라 표현 수정은3분할 예상도에 적용했다. 위 기존 앱 단독 영상의 생성 C12 입력은 회귀 기록으로 보존하며 새 실사 인식 성공으로 세지 않는다. [실사 출처/허락](../../media/front-driving/README.md).
