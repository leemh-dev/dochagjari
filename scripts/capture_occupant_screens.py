"""Independent occupant UI and real process restart on a CI-owned fresh generic AVD."""
import json
import os
import re
import subprocess
import time
from pathlib import Path
import capture_app_screens as ui

ui.OUT = Path('app/build/reports/occupant-context')
ui.OUT.mkdir(parents=True, exist_ok=True)
ui.report = {'environment': 'Generic Android API34 / CI-owned fresh AVD',
             'inputs': 'TEST vehicle; real DataStore; TEXT command, not STT',
             'source_commit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip(),
             'checks': [], 'screenshots': [], 'pleos_sdk_verified': False, 'actual_speech_verified': False}


def phase(name, user):
    output = ui.adb('shell', 'am', 'instrument', '--user', user, '-w', '-r',
                    '-e', 'class', ui.PACKAGE + '.OccupantRestartInstrumentedTest#' + name,
                    '-e', 'occupantPhase', name,
                    ui.PACKAGE + '.test/androidx.test.runner.AndroidJUnitRunner', timeout=120).decode()
    ui.OUT.joinpath('restart-' + name + '.txt').write_text(output)
    assert re.search(r'OK \(1 test\)', output) and 'FAILURES' not in output and 'INSTRUMENTATION_CODE: -1' in output, output[-1000:]
    found = re.search(r'OCCUPANT_(?:PREPARED|VERIFIED)_PID=(\d+)', output)
    assert found, 'Missing actual process PID'
    return int(found.group(1))


def main():
    try:
        if os.environ.get('GITHUB_ACTIONS') != 'true' or os.environ.get('DOCHAGJARI_FRESH_TEST_AVD') != '1':
            raise RuntimeError('Only the fresh AVD owned by this CI job may be used')
        ui.report['readiness'] = ui.probe(ui.ADB, ui.SERIAL)
        assert ui.report['readiness']['ready']
        user = ui.adb('shell', 'am', 'get-current-user').decode().strip(); assert user.isdigit()
        ui.adb('shell', 'am', 'force-stop', '--user', user, ui.PACKAGE)
        ui.adb('shell', 'am', 'start', '--user', user, '-W', '-n', ui.PACKAGE + '/.MainActivity'); time.sleep(2)
        ui.tap('탑승 설정'); ui.expect('오늘의 탑승'); ui.capture('01-occupant-presets')
        ui.tap('아이 등하원 적용'); ui.expect('설정 저장 완료'); ui.expect('이번 탑승 · 아이 등하원')
        ui.capture('02-child-confirmed')
        ui.tap('이번만 변경'); ui.expect('이번 탑승만 변경'); ui.capture('03-once-editor'); ui.tap('취소')
        ui.tap('예: 오늘 혼자야 / 아이 오른쪽 뒤에 탔어')
        ui.adb('shell', 'input', 'text', 'solo'); ui.tap('구성 확인'); ui.tap('혼자 확인 적용')
        ui.expect('설정 저장 완료'); ui.expect('이번 탑승 · 혼자'); ui.capture('04-text-solo-confirmed')
        first_pid = phase('prepare', user)
        ui.adb('shell', 'am', 'force-stop', '--user', user, ui.PACKAGE)
        second_pid = phase('verify', user); assert first_pid != second_pid
        ui.report['process_restart'] = {'prepare_pid': first_pid, 'verify_pid': second_pid,
                                        'saved_book_profile_visit_outbox_equal': True,
                                        'previous_trip_not_automatically_confirmed': True}
        ui.adb('shell', 'am', 'start', '--user', user, '-W', '-n', ui.PACKAGE + '/.MainActivity'); time.sleep(2)
        ui.tap('탑승 설정'); ui.expect('지난 구성 · 아이 등하원'); ui.capture('05-restored-needs-confirmation')
        ui.tap('이번에도 같은 구성'); ui.expect('설정 저장 완료'); ui.expect('이번 탑승 · 아이 등하원')
        ui.capture('06-restored-confirmed')
        ui.report['success'] = True
    finally:
        ui.OUT.joinpath('occupant-ui.json').write_text(json.dumps(ui.report, ensure_ascii=False, indent=2) + '\n')


if __name__ == '__main__':
    main()
