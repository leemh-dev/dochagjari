# 실제 Android 앱 시연 영상

2026-11-14 UTC. **0.11.0/11·f0640b0**의 실제 일반Android API34 실행에서 제작했다. TEST차량/입차/주차·MOCK내비·생성영상·TEXT명령이다. 실제Pleos/외부카메라/STT/실차/SMS로표시하지않는다. 음성나레이션은없고한국어자막을사용한다.

- [90초 편집 시연](dochagjari-android-demo.mp4): 실제 스크린샷 장면편집+실제파란선8초 동작구간. 장면편집/시간생략을 항상표시한다.
- [20초 연속 모션 샘플](dochagjari-motion-sample.mp4): 원본40.5초부터20초를연속추출. 생성영상/5층전환과파란선동작을확인한다.
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
