"""Product UX evidence on a new CI-owned generic AVD, never a Pleos success claim."""
import hashlib
import json
import os
import re
import subprocess
from pathlib import Path
import time
import capture_app_screens as ui

ui.OUT = Path('app/build/reports/product-ux')
ui.OUT.mkdir(parents=True, exist_ok=True)
ui.report = {'environment': 'Generic Android API34 / CI-owned fresh AVD',
             'inputs': 'TEST vehicle and entry/parking; MOCK road; manual-demo synthetic memo',
             'checks': [], 'screenshots': [], 'pleos_native_verified': False,
             'source_commit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip()}


def main():
    recording = None
    recording_log = None
    try:
        if os.environ.get('GITHUB_ACTIONS') != 'true' or os.environ.get('DOCHAGJARI_FRESH_TEST_AVD') != '1':
            raise RuntimeError('Refusing reset outside owned fresh CI AVD')
        ui.report['readiness'] = ui.probe(ui.ADB, ui.SERIAL)
        assert ui.report['readiness']['ready']
        user = ui.adb('shell', 'am', 'get-current-user').decode().strip(); assert user.isdigit()
        assert ui.adb('shell', 'pm', 'clear', '--user', user, ui.PACKAGE).decode().strip() == 'Success'
        ui.adb('shell', 'am', 'start', '--user', user, '-W', '-n', ui.PACKAGE + '/.MainActivity'); time.sleep(2)
        recording_log = ui.OUT.joinpath('screenrecord.log').open('wb')
        recording = subprocess.Popen([ui.ADB, '-s', ui.SERIAL, 'shell', 'screenrecord', '--bit-rate', '3000000', '--time-limit', '90', '/sdcard/arrival-context-demo.mp4'], stdout=recording_log, stderr=subprocess.STDOUT)
        ui.expect('에뜨와로 갈까요?'); ui.capture('01-product-destination')
        ui.tap('방문 준비'); ui.expect('내비 안내 시작'); ui.capture('02-visit-ready')
        ui.tap('내비 안내 시작'); ui.expect('도로 안내는 순정 내비에서'); ui.capture('03-mock-road-handoff')
        # Enable only animator duration on this owned AVD to exercise the actual Compose line.
        old_scale = ui.adb('shell', 'settings', 'get', 'global', 'animator_duration_scale').decode().strip()
        ui.adb('shell', 'settings', 'put', 'global', 'animator_duration_scale', '1')
        ui.tap('입차 확인 · TEST')
        ui.expect('주차장 안내'); ui.capture('04a-parking-map')
        ui.tap('생성 영상 보기'); time.sleep(1); ui.capture('04b-generated-camera-preview')
        ui.tap('영상 분석')
        deadline = time.monotonic() + 90
        while time.monotonic() < deadline:
            texts = [n.attrib.get('text', '') for n in ui.tree().iter('node')]
            if any('관측 입력/저장 실패' in text or '영상 권한 없음' in text for text in texts):
                raise AssertionError('Panel video analysis failed')
            if any('C12 후보' in text and '생성 영상 분석' in text for text in texts):
                ui.capture('04c-android-generated-c12-candidate'); break
            time.sleep(1)
        else: raise AssertionError('Generated C12 candidate did not reach the arrival panel')
        ui.tap('5층 동선')
        ui.capture('04-animated-floor-early')
        time.sleep(6.5); ui.capture('05-animated-floor-complete')
        ui.expect('엘리베이터에서\n에뜨와까지')
        ui.tap('닫기'); ui.expect('매장까지 이어서'); ui.capture('06-entry-unknown-connection')
        for label, name in [('병원 동행', '06a-hospital'), ('학원 픽업', '06b-academy'), ('정자역 마중', '06c-station'), ('공항 배웅', '06d-airport')]:
            ui.tap(label); ui.capture(name)
            if label == '학원 픽업':
                ui.expect('서쪽 승하차점 (시연 좌표)'); ui.tap('이용 불가 · TEST')
                ui.expect('뒤편 주차장 입구 (시연 좌표)'); ui.capture('06e-academy-diversion')
                ui.tap('이용 불가 · TEST'); ui.expect('조건에 맞는 후보가 없습니다'); ui.capture('06f-academy-no-candidate')
            ui.tap('닫기')
        ui.adb('shell', 'settings', 'put', 'global', 'animator_duration_scale', '0')
        ui.tap('5층 동선 보기'); ui.expect('모션 줄이기 · 전체 경로 표시'); ui.capture('07-reduced-motion-static')
        ui.tap('닫기')
        ui.tap('개발자 도구'); ui.tap('매장 방문'); ui.tap('요약 자동 닫기 끄기'); ui.tap('안내 화면으로 돌아가기')
        ui.tap('주차 완료 · TEST'); ui.expect('주차 기록 저장 완료'); ui.capture('08-parking-saved-unknown')
        ui.tap('닫기'); ui.tap('차로 돌아가기'); ui.expect('주차층 미확인  ·  기둥 미확인'); ui.capture('09-return-clues-no-fake-line')
        ui.tap('주차 단서 직접 보완'); ui.capture('10-manual-clue-form')
        # No invented facility identifiers: a synthetic memo is sufficient for read-back checks.
        root = ui.tree(); parents = {child: parent for parent in root.iter('node') for child in parent}
        editor = next(n for n in root.iter('node') if n.attrib.get('text') == '메모' and ui.visible(n))
        while editor.attrib.get('class') != 'android.widget.EditText':
            editor = parents.get(editor)
            if editor is None: raise AssertionError('Memo editor ancestor absent')
        a,b,c,d = ui.box(editor); ui.adb('shell', 'input', 'tap', str((a+c)//2), str((b+d)//2))
        ui.adb('shell', 'input', 'text', 'manual-demo')
        ime = ui.adb('shell', 'dumpsys', 'input_method').decode(errors='replace')
        if re.search(r'(?:mInputShown|mIsInputViewShown)=true', ime): ui.adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')
        ui.tap('확인한 단서 저장'); ui.capture('11-manual-clue-save')
        ui.tap('닫기'); ui.tap('차로 돌아가기'); ui.expect('manual-demo'); ui.capture('12-return-manual-clue')
        ui.tap('닫기')
        old_pid = ui.adb('shell', 'pidof', ui.PACKAGE).decode().strip()
        ui.adb('shell', 'am', 'force-stop', '--user', user, ui.PACKAGE)
        ui.adb('shell', 'am', 'start', '--user', user, '-W', '-n', ui.PACKAGE + '/.MainActivity'); time.sleep(2)
        new_pid = ui.adb('shell', 'pidof', ui.PACKAGE).decode().strip(); assert old_pid and new_pid and old_pid != new_pid
        ui.expect('주차 기록이 있어요'); ui.capture('13-restored-home-no-auto-guide')
        ui.tap('차로 돌아가기'); ui.expect('manual-demo'); ui.capture('14-restored-return')
        ui.tap('닫기'); ui.tap('개발자 도구'); ui.tap('연동·테스트'); ui.tap('주행')
        ui.tap('안내 화면으로 돌아가기'); ui.expect('안전하게 이동하세요'); ui.capture('15-driving-brief')
        ui.tap('개발자 도구'); ui.tap('연동·테스트'); ui.tap('정차'); ui.tap('안내 화면으로 돌아가기')
        ui.adb('shell', 'wm', 'size', '480x900'); time.sleep(1)
        ui.expect('탑승 설정'); ui.capture('16-narrow-app-panel')
        ui.tap('탑승 설정'); ui.expect('오늘의 탑승'); ui.capture('17-narrow-occupant-settings'); ui.tap('닫기')
        ui.adb('shell', 'wm', 'size', '1280x800')
        ui.adb('shell', 'settings', 'put', 'global', 'animator_duration_scale', old_scale if old_scale != 'null' else '0')
        ui.report.update(success=True, old_pid=old_pid, new_pid=new_pid, actual_sdk_pin=False,
                         current_position_and_return_path_verified=False,
                         animation='Actual Compose six-second route; early PNG taken before XML dump, later PNG after delay',
                         file_sha256={p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in ui.OUT.glob('*.png')})
    except Exception as error:
        ui.report.update(success=False, error=str(error))
        try: ui.capture('failure')
        except Exception: pass
        raise
    finally:
        if recording is not None:
            try:
                recording.wait(timeout=100)
                if recording.returncode != 0:
                    raise RuntimeError('Android screenrecord failed')
                ui.adb('pull', '/sdcard/arrival-context-demo.mp4', str(ui.OUT / 'arrival-context-demo.mp4'))
                video = ui.OUT / 'arrival-context-demo.mp4'
                assert video.stat().st_size > 10000
                ui.report['recording'] = {'file': video.name, 'bytes': video.stat().st_size,
                    'sha256': hashlib.sha256(video.read_bytes()).hexdigest(),
                    'kind': 'Actual generic Android app screenrecord; TEST/MOCK/generated clip; not native Pleos'}
            except Exception as error:
                ui.report['recording_error'] = str(error)
            finally:
                recording_log.close()
        ui.OUT.joinpath('summary.json').write_text(json.dumps(ui.report, ensure_ascii=False, indent=2) + '\n')


if __name__ == '__main__': main()
