"""Stage 6 focused UI evidence. Data reset is limited to this job's new dedicated AVD."""
import json
import os
import re
from pathlib import Path
import time
import capture_app_screens as ui

ui.OUT = Path('app/build/reports/parking-screens')
ui.OUT.mkdir(parents=True, exist_ok=True)
ui.report = {'environment': 'Generic Android API34 / CI-owned fresh AVD',
             'inputs': 'TEST vehicle/entry/observations/parking; MOCK road; synthetic manual memo',
             'checks': [], 'screenshots': [], 'pleos_native_verified': False}


def signature(root):
    return [(n.attrib.get('text'), n.attrib.get('bounds')) for n in root.iter('node') if ui.visible(n) and n.attrib.get('package') == ui.PACKAGE]


def find(text):
    root = ui.tree()
    for down in (True, False):
        for _ in range(18):
            matches = [n for n in root.iter('node') if n.attrib.get('text') == text and ui.visible(n)]
            if len(matches) == 1: return matches[0]
            if len(matches) > 1: raise AssertionError('Ambiguous control: ' + text)
            if not ui.scroll(root, down): break
            previous = signature(root); root = ui.tree()
            if signature(root) == previous: break
    raise AssertionError('Visible control not found: ' + text)


ui.find = find


def type_memo(value):
    label = find('직접 확인한 메모')
    root = ui.tree(); parents = {child: parent for parent in root.iter() for child in parent}
    # Re-resolve in this XML; never tap a guessed field position or assume label classes.
    matches = [n for n in root.iter('node') if n.attrib.get('text') == '직접 확인한 메모' and ui.visible(n)]
    assert len(matches) == 1
    editor = matches[0]
    while editor.attrib.get('class') != 'android.widget.EditText':
        editor = parents.get(editor)
        if editor is None: raise AssertionError('Verified memo EditText ancestor unavailable')
    a,b,c,d = ui.box(editor)
    ui.adb('shell', 'input', 'tap', str((a+c)//2), str((b+d)//2))
    ui.adb('shell', 'input', 'text', value)
    ime = ui.adb('shell', 'dumpsys', 'input_method').decode(errors='replace')
    keyboard_shown = bool(re.search(r'(?:mInputShown|mIsInputViewShown)=true', ime))
    if keyboard_shown: ui.adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')
    ui.report['memo_input'] = {'verified_editor': True, 'keyboard_back_sent': keyboard_shown}
    time.sleep(.5)


def main():
    try:
        if os.environ.get('GITHUB_ACTIONS') != 'true' or os.environ.get('DOCHAGJARI_FRESH_TEST_AVD') != '1':
            raise RuntimeError('Refusing reset outside owned fresh CI AVD')
        ui.report['readiness'] = ui.probe(ui.ADB, ui.SERIAL)
        assert ui.report['readiness']['ready']
        user = ui.adb('shell', 'am', 'get-current-user').decode().strip(); assert user.isdigit()
        assert ui.adb('shell', 'pm', 'clear', '--user', user, ui.PACKAGE).decode().strip() == 'Success'
        ui.adb('shell', 'am', 'start', '--user', user, '-W', '-n', ui.PACKAGE + '/.MainActivity'); time.sleep(2)
        ui.tap("개발자 도구")
        ui.expect('서쪽 승하차점 (시연 좌표)'); ui.capture('01-start')
        ui.tap('매장 방문'); ui.tap('저장된 매장 찾기'); ui.tap('방문 준비 · 저장')
        visit = None
        for _ in range(12):
            root = ui.tree(); visit = next((n.attrib['text'] for n in root.iter('node') if n.attrib.get('text', '').startswith('방문 ID · ')), None)
            if visit: break
            ui.scroll(root, True)
        assert visit, 'Persisted visit ID required'
        ui.tap('시설 목적지로 내비 요청'); ui.expect('모의 내비 계산 완료 · 실제 SDK/핀 미검증')
        ui.tap('입차 테스트 · 실제 감지 아님'); ui.expect('5층 에뜨와 동선'); ui.capture('02-entry-map')
        ui.tap('닫기'); ui.tap('테스트 관측 시작'); ui.tap('테스트 B3/C12 반복')
        ui.expect('관측 판단: 반복 표지 후보 · 위치 확정 아님'); ui.capture('03-observation-only')
        ui.tap('요약 자동 닫기 끄기'); ui.tap('주차 완료 테스트')
        ui.expect('주차 저장 완료 · 요약'); ui.expect('주차층·구역·기둥·승강기 미확인 · 관측 문자를 자동 확정하지 않았습니다.'); ui.capture('04-unknown-parking-summary')
        ui.tap('요약 닫기'); ui.tap('테스트 주차 단서 채우기')
        ui.expect('입력 출처 · TEST · 입력 변경 · 미저장'); ui.capture('05-test-draft-unsaved')
        ui.tap('입력한 단서 확인 · 저장'); ui.expect('저장 단서 출처 · TEST · 실제 시설 확인 아님'); ui.capture('06-test-note-stored')
        ui.tap('비우고 직접 입력'); type_memo('manual-demo')
        ui.tap('입력한 단서 확인 · 저장'); ui.expect('메모 · manual-demo'); ui.capture('07-manual-note-stored')
        ui.tap('차로 돌아가기 · 저장 단서'); ui.expect('차로 돌아가기 · 저장 기록'); ui.capture('08-return-record')
        ui.tap('저장된 5층 동선 보기 · 매장행'); ui.expect('5층 에뜨와 동선'); ui.capture('09-floor-preview')
        ui.tap('닫기')
        old_pid = ui.adb('shell', 'pidof', ui.PACKAGE).decode().strip()
        ui.adb('shell', 'am', 'force-stop', '--user', user, ui.PACKAGE)
        ui.adb('shell', 'am', 'start', '--user', user, '-W', '-n', ui.PACKAGE + '/.MainActivity'); time.sleep(2)
        ui.tap("개발자 도구")
        ui.tap('매장 방문'); ui.expect(visit); ui.expect('저장 상태 · PARKED / HIDDEN')
        ui.expect('저장 주차 기록 확인 · 자동 재개 없음'); ui.expect('메모 · manual-demo'); ui.capture('10-restored-hidden')
        new_pid = ui.adb('shell', 'pidof', ui.PACKAGE).decode().strip(); assert old_pid and new_pid and old_pid != new_pid
        ui.tap('연동·테스트'); ui.tap('주행'); ui.tap('매장 방문')
        ui.expect('주행 중/상태 미확인 · 주차 상세 조작과 단서 숨김'); ui.capture('11-driving-hidden')
        ui.tap('연동·테스트'); ui.tap('정차'); ui.tap('매장 방문')
        ui.expect('이동 재개 후 이전 기록 · 현재 주차 위치 아님'); ui.capture('12-previous-record-after-movement')
        ui.tap('요약 자동 닫기 끄기'); ui.tap('주차 완료 테스트')
        ui.expect('주차층·구역·기둥·승강기 미확인 · 관측 문자를 자동 확정하지 않았습니다.'); ui.capture('13-next-parking-unknown')
        ui.tap('요약 닫기'); ui.tap('주차 위치 변경 · 다시 찾기')
        ui.expect(visit); ui.expect('저장 상태 · PARKING_GUIDANCE / HIDDEN'); ui.capture('14-explicit-search-idle')
        ui.report.update(success=True, same_visit_id=True, old_pid=old_pid, new_pid=new_pid,
                         observation_promoted=False, automatic_summary_or_source_resume=False,
                         manual_memo='synthetic manual-demo', current_position_and_return_path_verified=False)
    except Exception as error:
        ui.report.update(success=False, error=str(error))
        try: ui.capture('failure')
        except Exception: pass
        raise
    finally:
        ui.OUT.joinpath('summary.json').write_text(json.dumps(ui.report, ensure_ascii=False, indent=2) + '\n')


if __name__ == '__main__': main()
