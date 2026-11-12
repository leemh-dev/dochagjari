"""Read-only X11 pointer/focus/geometry evidence on the private CI-owned display."""
import re
import subprocess
from adb_readiness import diagnostic


def observe_pointer(environment, expected, runner=subprocess.run):
    result = {'readonly': True, 'expected_point': list(expected), 'checks': {}}

    def read(name, arguments):
        try:
            value = runner(arguments, env=environment, capture_output=True, text=True, timeout=5)
            record = {'returncode': value.returncode, 'stdout': diagnostic(value.stdout.strip()),
                      'stderr': diagnostic(value.stderr.strip())}
        except (OSError, subprocess.SubprocessError) as error:
            record = {'returncode': None, 'stdout': '', 'stderr': diagnostic(str(error))}
        result['checks'][name] = record
        return record['stdout'] if record['returncode'] == 0 else ''

    pointer = read('pointer', ['xdotool', 'getmouselocation', '--shell'])
    values = {key: int(value) for key, value in re.findall(r'^(X|Y|SCREEN|WINDOW)=(-?\d+)$', pointer, re.M)}
    result['pointer'] = values
    result['pointer_coordinates_match'] = values.get('X') == expected[0] and values.get('Y') == expected[1]
    geometry = read('display', ['xdotool', 'getdisplaygeometry'])
    result['display_size'] = [int(value) for value in geometry.split()] if re.fullmatch(r'\d+ \d+', geometry) else None
    focus = read('focus', ['xdotool', 'getwindowfocus'])
    result['focus_window'] = int(focus) if focus.isdigit() else None
    window = values.get('WINDOW')
    if window and window > 0:
        # No window titles or arbitrary properties: only class and numeric geometry.
        result['target_class'] = read('target_class', ['xprop', '-id', str(window), 'WM_CLASS'])
        hierarchy = read('target_tree', ['xwininfo', '-id', str(window), '-tree'])
        result['target_ancestors'] = {name: int(match[1], 16) for name, pattern in
            [('root', r'Root window id:\s*(0x[0-9a-fA-F]+)'), ('parent', r'Parent window id:\s*(0x[0-9a-fA-F]+)')]
            if (match := re.search(pattern, hierarchy))}
        result['checks']['target_tree']['stdout'] = str(result['target_ancestors'])
        info = read('target_geometry', ['xwininfo', '-id', str(window), '-stats'])
        fields = [('x', r'Absolute upper-left X:\s*(-?\d+)'), ('y', r'Absolute upper-left Y:\s*(-?\d+)'),
                  ('width', r'Width:\s*(\d+)'), ('height', r'Height:\s*(\d+)')]
        result['target_geometry'] = {key: int(match[1]) for key, pattern in fields if (match := re.search(pattern, info))}
        # xwininfo includes a title in its first line: never retain that line.
        result['checks']['target_geometry']['stdout'] = '\n'.join(line for line in info.splitlines()
            if any(label in line for label in ['Absolute upper-left X:', 'Absolute upper-left Y:', 'Width:', 'Height:', 'Map State:']))
    return result
