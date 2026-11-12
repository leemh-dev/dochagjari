"""Read-only X11 pointer/focus/geometry evidence on the private CI-owned display."""
import re
import subprocess
import json
from pathlib import Path
import sys
from adb_readiness import diagnostic


def window_inventory(raw):
    root = re.search(r'xwininfo: Window id:\s*(0x[0-9a-fA-F]+)', raw)
    windows = []
    for line in raw.splitlines():
        match = re.search(r'^\s*(0x[0-9a-fA-F]+).*?\s(\d+)x(\d+)[+-]\d+[+-]\d+\s+([+-]\d+)([+-]\d+)\s*$', line)
        if not match:
            continue
        classes = re.search(r'\("([^"\n]*)"\s+"([^"\n]*)"\)', line)
        windows.append({'id':int(match[1],16), 'width':int(match[2]), 'height':int(match[3]),
                        'x':int(match[4]), 'y':int(match[5]),
                        'classes':list(classes.groups()) if classes else []})
    return {'root_id':int(root[1],16) if root else None, 'windows':windows[:50]}


def native_input_target(context):
    native=context.get('native_state',{})
    pointer=native.get('direct_pointer',{})
    point=context.get('expected_point')
    if (not context.get('pointer_coordinates_match') or not isinstance(point,(list,tuple)) or len(point)!=2
            or pointer.get('same_screen') is not True or [pointer.get('x'),pointer.get('y')]!=list(point)
            or native.get('x11_errors') or native.get('root')!=context.get('window_inventory',{}).get('root_id')):
        return None
    target=pointer.get('child',0)
    if not target or target==native.get('root'):return None
    candidates=[w for w in context.get('window_inventory',{}).get('windows',[]) if w['id']==target and
                any(name.casefold()=='emulator' for name in w['classes']) and
                w['x']<=point[0]<w['x']+w['width'] and w['y']<=point[1]<w['y']+w['height']]
    states=[w for w in native.get('windows',[]) if w['id']==target and w.get('attributes_available') is True and
            w.get('map_state')==2 and w.get('window_class')==1 and w.get('point_in_input_region') is True and
            w.get('point_in_bounding_region') is True and w.get('input_regions_complete') is True and
            w.get('bounding_regions_complete') is True]
    return target if len(candidates)==1 and len(states)==1 else None


def input_target_known(context):
    if 'native_state' in context:
        return native_input_target(context) is not None
    return context.get('pointer_coordinates_match') is True and context.get('pointer', {}).get('WINDOW',0)>0


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
    inventory = window_inventory(read('root_tree', ['xwininfo', '-root', '-tree']))
    result['window_inventory'] = inventory
    result['checks']['root_tree']['stdout'] = str(inventory)
    candidates=[w['id'] for w in inventory['windows'] if
                w['x']<=expected[0]<w['x']+w['width'] and w['y']<=expected[1]<w['y']+w['height']][:10]
    if inventory['root_id']:
        try:
            completed=runner([sys.executable,str(Path(__file__).with_name('native_window_state.py')),
                     ','.join(str(value) for value in candidates),str(expected[0]),str(expected[1])],
                     env=environment,capture_output=True,text=True,timeout=5)
            result['checks']['native_state']={'returncode':completed.returncode,
                'stdout':'Structured numeric snapshot stored separately', 'stderr':diagnostic(completed.stderr)}
            result['native_state']=json.loads(completed.stdout) if completed.returncode==0 else {'available':False}
        except (OSError,subprocess.SubprocessError,ValueError,TypeError) as error:
            result['native_state']={'available':False,'error_type':type(error).__name__}
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
