# 0.11.0 실제 Android 검증

기준 f0640b00da92409c265cfd5563238e0d972edcaa / [CI43569033733](https://github.com/leemh-dev/dochagjari/actions/runs/43569033733) 전체성공. 일반Android API34 x86_64·새전용KVM AVD. TEST차량/명시입차/주차·MOCK내비·생성영상·TEXT설정이며Pleos실연동이아니다.

[전체지문/검사수준/환경](evidence-index.json) · [connected 원본15pass/4전용skip](connected-results.xml) · [기존복원2단계](process-restart.txt) · [탑승복원2단계](occupant-context/occupant-ui.json) · [HTTP동일DB복구](android-server-recovery/summary.json) · [실제검사APK메타데이터](apk-metadata.json).

| 실제 화면 | 원본 |
| --- | --- |
| 상단 생성영상/하단 주차지도·좁은앱폭 | [480×900](product-ux/18-narrow-camera-map.png) |
| 기울인5층 매장행파란선 | [480×900](product-ux/19-narrow-floor-route.png) / [1280×800](product-ux/05-animated-floor-complete.png) |
| 새패널의Android C12후보 | [원본](product-ux/04c-android-generated-c12-candidate.png) |
| 병원/학원 가상후보·우회 | [병원](product-ux/06a-hospital.png) / [학원우회](product-ux/06e-academy-diversion.png) |
| 독립탑승/입력확인 | [아이모드](occupant-context/02-child-confirmed.png) / [TEXT확인](occupant-context/04-text-solo-confirmed.png) |
| 새PID뒤 재확인 | [지난구성](occupant-context/05-restored-needs-confirmation.png) |
| 저장/귀환 | [저장ACK](product-ux/08-parking-saved-unknown.png) / [직접보완단서](product-ux/12-return-manual-clue.png) |
| 주행제한 | [상세조작숨김](product-ux/15-driving-brief.png) |

39장과대응XML·로그를그대로보존했다. 원본전체54장은[artifact14103609767](https://github.com/leemh-dev/dochagjari/actions/runs/43569033733/artifacts/14103609767)에있다. [원본녹화/편집영상/해시](../../demo-video/README.md). 확인한장면/문자와실제현장위치/SDK핀·Console수신/실차를구분한다.
