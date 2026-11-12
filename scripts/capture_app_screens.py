"""Visible UI smoke and unmodified screenshots on a fresh CI-owned Android AVD."""
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

sys.path.insert(0, str(Path(__file__).parent / 'pleos'))
from adb_readiness import probe

PACKAGE = 'dev.leemh.dochagjari'
ADB = str(Path(os.environ['ANDROID_HOME']) / 'platform-tools/adb')
SERIAL = os.environ.get('ANDROID_SERIAL', 'emulator-5554')
OUT = Path('app/build/reports/ui-screens')
OUT.mkdir(parents=True, exist_ok=True)
report = {'environment': 'Generic Android API34 / CI-owned fresh AVD',
          'inputs': 'App test vehicle, mock navigation, local mock notification, generated video',
          'checks': [], 'screenshots': [], 'pleos_native_verified': False}


def adb(*args, timeout=40):
    return subprocess.run([ADB, '-s', SERIAL, *args], capture_output=True, check=True, timeout=timeout).stdout


def tree():
    adb('shell', 'uiautomator', 'dump', '/sdcard/dochagjari-screen.xml')
    return ET.fromstring(adb('exec-out', 'cat', '/sdcard/dochagjari-screen.xml'))


def box(node):
    return tuple(map(int, re.findall(r'\d+', node.attrib.get('bounds', ''))))


def visible(node):
    b = box(node)
    return len(b) == 4 and b[2] > b[0] and b[3] > b[1]


def scroll(root, down):
    node = next((n for n in root.iter('node') if n.attrib.get('scrollable') == 'true' and visible(n)), None)
    if node is None:
        return False
    x1, y1, x2, y2 = box(node)
    x = (x1 + x2) // 2
    upper, lower = y1 + (y2-y1)//5, y2 - (y2-y1)//5
    adb('shell', 'input', 'swipe', str(x), str(lower if down else upper), str(x), str(upper if down else lower), '350')
    return True


def find(text):
    root = tree()
    for phase in range(2):
        for _ in range(7):
            matches = [n for n in root.iter('node') if n.attrib.get('text') == text and visible(n)]
            if len(matches) == 1:
                return matches[0]
            if len(matches) > 1:
                raise AssertionError('Ambiguous visible control: ' + text)
            if not scroll(root, down=phase == 1):
                break
            root = tree()
    raise AssertionError('Visible control not found: ' + text)


def tap(text):
    node = find(text)
    if node.attrib.get('enabled') == 'false':
        raise AssertionError('Control disabled: ' + text)
    x1, y1, x2, y2 = box(node)
    adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(.5)


def capture(name):
    OUT.joinpath(name + '.png').write_bytes(adb('exec-out', 'screencap', '-p'))
    root = tree()
    OUT.joinpath(name + '.xml').write_bytes(ET.tostring(root, encoding='utf-8'))
    report['screenshots'].append(name + '.png')


def expect(text):
    find(text)
    report['checks'].append({'text': text, 'result': 'visible'})


def video(button, occupancy, name):
    tap('영상 탐색'); tap(button)
    deadline = time.monotonic() + 90
    while time.monotonic() < deadline:
        texts = [n.attrib.get('text', '') for n in tree().iter('node')]
        if any('분석 실패' in s for s in texts):
            raise AssertionError('Android video analysis failed')
        if any('원본 관측 ' + occupancy in s for s in texts):
            capture(name)
            report['checks'].append({'video': button, 'occupancy': occupancy, 'result': 'visible Android analysis'})
            return
        time.sleep(1)
    raise AssertionError('Android analysis result did not appear: ' + button)


try:
    if os.environ.get('GITHUB_ACTIONS') != 'true' or os.environ.get('DOCHAGJARI_FRESH_TEST_AVD') != '1':
        raise RuntimeError('Refusing data reset outside the explicitly owned fresh CI test AVD')
    report['readiness'] = probe(ADB, SERIAL)
    if not report['readiness']['ready']:
        raise RuntimeError('ADB/services not ready')
    adb('shell', 'pm', 'clear', PACKAGE)
    adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity')
    time.sleep(2)
    expect('서쪽 승하차점 (시연 좌표)'); capture('01-pickup')
    tap('주차하고 데리러 가기'); expect('뒤편 주차장 입구 (시연 좌표)'); capture('02-parking')
    tap('바로 태우기'); tap('이 지점 이용 불가 · 우회'); expect('뒤편 주차장 입구 (시연 좌표)'); capture('03-diversion')
    tap('이 지점 이용 불가 · 우회'); expect('후보 없음'); capture('04-no-candidates')
    tap('제외 초기화')
    tap('도착병원 · 가상 시연'); capture('05-hospital')
    tap('도착학원 · 가상 시연')
    tap('좌석·조건'); expect('설정 저장 완료'); capture('06-profile')
    video('생성 빈 구간', 'EMPTY', '07-video-empty')
    video('생성 장애물·C12', 'OCCUPIED', '08-video-blocked')
    tap('도착 안내'); expect('뒤편 주차장 입구 (시연 좌표)'); capture('09-video-diversion')
    tap('만남·알림'); tap('시연용 추천 좌표 채우기 · 실제 위치 아님'); capture('10-notification')
    tap('연동·테스트'); capture('11-test-inputs'); tap('주행')
    tap('좌석·조건'); expect('정차 상태가 확인되면 설정할 수 있습니다.'); capture('12-driving-lock')
    tap('영상 탐색'); expect('주행 중 또는 주행 상태 미확인 · 영상 결과와 카메라 화면을 숨깁니다.'); capture('13-driving-video-hidden')
    report['success'] = True
except Exception as error:
    report['success'] = False
    report['error'] = str(error)
    try:
        capture('failure')
    except Exception:
        pass
    raise
finally:
    OUT.joinpath('summary.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
