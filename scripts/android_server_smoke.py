"""Real Android HTTP recovery against a local mock server; no actual SMS/recipient."""
import hashlib
import json
import os
from pathlib import Path
import secrets
import sqlite3
import subprocess
import tempfile
import time
import urllib.request

import capture_app_screens as ui
from pleos.ui_nodes import labeled_toggle

OUT = Path('app/build/reports/android-server-recovery')
OUT.mkdir(parents=True, exist_ok=True)
ui.OUT = OUT
report = {'environment': 'Generic Android API34 KVM / own fresh CI AVD',
          'channel': 'SERVER_MOCK', 'real_sms': False, 'screenshots': ui.report['screenshots']}
server = None


def start_server(work):
    process = subprocess.Popen([os.sys.executable, str(Path(__file__).with_name('mock_arrival_fixture.py')), str(work)],
                               stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    deadline = time.monotonic() + 20
    client = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    while time.monotonic() < deadline and process.poll() is None:
        try:
            with client.open('http://127.0.0.1:8000/health', timeout=1) as response:
                if json.load(response) == {'status':'ok','mode':'mock'} and response.headers.get('X-Arrival-Fixture-Pid') == str(process.pid):
                    return process
        except OSError:
            time.sleep(.2)
    process.terminate(); process.wait(timeout=5)
    raise RuntimeError('Owned local mock server did not become ready')


def stop_server(process):
    if process is not None and process.poll() is None:
        process.terminate()
        try: process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            process.kill(); process.wait(timeout=5)


def toggle(label, enabled):
    ui.find(label)
    node = labeled_toggle(list(ui.tree().iter('node')), label)
    if node.attrib.get('checked') != str(enabled).lower():
        if node.attrib.get('enabled') != 'true': raise RuntimeError('Toggle disabled: ' + label)
        x1,y1,x2,y2 = ui.box(node)
        ui.adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
    if labeled_toggle(list(ui.tree().iter('node')),label).attrib.get('checked') != str(enabled).lower():
        raise RuntimeError('Toggle acknowledgement missing: ' + label)


def enter_token(token, must_be_empty=False):
    label = '서버 접근 토큰 · 메모리에만 유지'
    ui.find(label)
    fields = [n for n in ui.tree().iter('node') if 'EditText' in n.attrib.get('class','') and
              any(c.attrib.get('text') == label for c in n.iter('node')) and ui.visible(n)]
    if len(fields) != 1: raise RuntimeError('Cannot safely identify server token input')
    if must_be_empty and fields[0].attrib.get('text',''):
        raise RuntimeError('Token field was not empty after process restart')
    x1,y1,x2,y2 = ui.box(fields[0])
    ui.adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
    # Hex-only ephemeral fixture token. Never include command arguments in a report.
    ui.adb('shell','input','text',token)
    state = ui.adb('shell','dumpsys','input_method').decode(errors='replace')
    if 'mInputShown=true' in state or 'isInputViewShown=true' in state:
        ui.adb('shell','input','keyevent','BACK')


def db_rows(work):
    try:
        with sqlite3.connect(work / 'outbox.sqlite3', timeout=2) as db:
            db.row_factory = sqlite3.Row
            return [dict(r) for r in db.execute('SELECT session_id, digest, status, delivery_mode, provider_id FROM arrivals')]
    except sqlite3.Error:
        return []


def wait_for(check, message, seconds=30):
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        value = check()
        if value: return value
        time.sleep(.2)
    raise RuntimeError(message)


try:
    if os.environ.get('GITHUB_ACTIONS') != 'true' or os.environ.get('DOCHAGJARI_FRESH_TEST_AVD') != '1':
        raise RuntimeError('Refusing reset outside explicitly owned fresh CI AVD')
    report['readiness'] = ui.probe(ui.ADB, ui.SERIAL)
    if not report['readiness']['ready']: raise RuntimeError('Device services are not ready')
    with tempfile.TemporaryDirectory(prefix='arrival-server-smoke-') as directory:
        work = Path(directory)
        token = secrets.token_hex(24)
        (work / 'token').write_text(token); (work / 'token').chmod(0o600)
        (work / 'hold-response').touch()
        server = start_server(work)
        if ui.adb('shell','pm','clear',ui.PACKAGE).strip() != b'Success': raise RuntimeError('Owned test-data reset failed')
        ui.adb('shell','am','start','-W','-n',ui.PACKAGE+'/.MainActivity'); time.sleep(2)
        ui.tap('개발자 도구')
        ui.tap('연동·테스트'); toggle('알림 서버 사용',True)
        toggle('실제 SENS 서버 모드 · 기본은 서버 모의 발송',False)
        enter_token(token)
        ui.tap('만남·알림'); ui.tap('시연용 추천 좌표 채우기 · 실제 위치 아님')
        toggle('테스트 수신자 알림 사전 동의',True)
        ui.tap('여기서 만나요 · 위치 확인')
        rows = wait_for(lambda: db_rows(work), 'Android request never reached local mock server')
        if len(rows) != 1 or rows[0]['status'] != 'mock' or rows[0]['delivery_mode'] != 'mock':
            raise RuntimeError('Expected one durable mock result before response loss')
        frozen = rows[0]
        ui.capture('01-server-stored-response-withheld')
        first_pid = ui.adb('shell','pidof',ui.PACKAGE).decode().strip()
        ui.adb('shell','am','force-stop',ui.PACKAGE)
        stop_server(server); server = None
        (work / 'hold-response').unlink()
        server = start_server(work)  # Same DB and request: never delete or replace rows.
        ui.adb('shell','am','start','-W','-n',ui.PACKAGE+'/.MainActivity'); time.sleep(2)
        ui.tap('개발자 도구')
        second_pid = ui.adb('shell','pidof',ui.PACKAGE).decode().strip()
        if not first_pid or not second_pid or first_pid == second_pid: raise RuntimeError('No new Android process')
        ui.tap('만남·알림'); ui.expect('세션 ' + frozen['session_id'])
        ui.capture('02-request-restored-without-token')
        before = [json.loads(line) for line in (work/'events.jsonl').read_text().splitlines()]
        ui.tap('연동·테스트'); enter_token(token,must_be_empty=True)
        report['token_empty_after_restart'] = True
        ui.tap('만남·알림'); ui.tap('발송 결과 확인·복구')
        ui.expect('모의 발송 완료 · 실제 SMS 미발송')
        ui.capture('03-same-session-server-result-recovered')
        after = [json.loads(line) for line in (work/'events.jsonl').read_text().splitlines()]
        if sum(e['method']=='POST' for e in after) != 1: raise RuntimeError('Unexpected repeat POST')
        recovery = after[len(before):]
        if not recovery or any(e['method']!='GET' or e['status']!=200 for e in recovery):
            raise RuntimeError('Restart recovery did not exclusively query existing result')
        if db_rows(work) != [frozen]: raise RuntimeError('Frozen session/server result changed')
        if (work/'provider-called').exists(): raise RuntimeError('Provider was called')
        report.update(success=True, response_deliberately_withheld=True, server_restarted_with_same_db=True,
                      first_pid=first_pid, second_pid=second_pid, session_id=frozen['session_id'],
                      request_digest=frozen['digest'], post_count=1, recovery_get_count=len(recovery),
                      provider_attempts=0, event_trace=after)
except Exception as error:
    report['success'] = False
    report['error'] = ('ADB command failed; arguments omitted' if isinstance(error,subprocess.CalledProcessError)
                       else str(error))
    raise RuntimeError(report['error']) from None
finally:
    stop_server(server)
    (OUT/'summary.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
