"""Own private Xvfb pointer preflight. This never runs an emulator or approves ADB."""
import argparse
import json
import os
from pathlib import Path
import secrets
import subprocess
import tempfile
import time
from x11_context import observe_pointer


def check():
    result = {'scope':'Private host Xvfb only; no emulator, app or ADB approval', 'success':False}
    with tempfile.TemporaryDirectory(prefix='x11-pointer-check-') as folder:
        authority = Path(folder) / 'authority'
        authority.touch(mode=0o600)
        display = next(n for n in range(97,110) if not Path(f'/tmp/.X11-unix/X{n}').exists())
        environment = os.environ | {'DISPLAY':f':{display}', 'XAUTHORITY':str(authority)}
        # Never log this command or cookie. Keep normal X11 authentication enabled.
        auth = subprocess.run(['xauth','-f',str(authority),'add',f':{display}',
                               'MIT-MAGIC-COOKIE-1',secrets.token_hex(16)],capture_output=True)
        if auth.returncode: raise RuntimeError('Private Xauthority creation failed')
        server = subprocess.Popen(['Xvfb',f':{display}','-screen','0','800x600x24','-nolisten','tcp','-auth',str(authority)],
                                  env=environment,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
        try:
            for _ in range(30):
                probe = subprocess.run(['xdotool','getdisplaygeometry'],env=environment,capture_output=True)
                if probe.returncode == 0: break
                time.sleep(.1)
            else: raise RuntimeError('Private X server did not start')
            subprocess.run(['xdotool','mousemove','200','200'],env=environment,capture_output=True,check=True,timeout=2)
            result['before'] = observe_pointer(environment,(200,200))
            started = time.monotonic()
            try:
                old = subprocess.run(['xdotool','mousemove','--sync','200','200','mousedown','1','sleep','0.2','mouseup','1'],
                                     env=environment,capture_output=True,timeout=2)
                result['same_point_sync'] = {'returncode':old.returncode,'timed_out':False}
            except subprocess.TimeoutExpired:
                result['same_point_sync'] = {'returncode':None,'timed_out':True}
            result['same_point_sync']['seconds'] = round(time.monotonic()-started,2)
            moved = subprocess.run(['xdotool','mousemove','200','200'],env=environment,capture_output=True,timeout=2)
            gesture = subprocess.run(['xdotool','mousedown','1','sleep','0.2','mouseup','1'],env=environment,capture_output=True,timeout=2)
            result['separate_move_and_gesture'] = {'move_rc':moved.returncode,'gesture_rc':gesture.returncode}
            if moved.returncode or gesture.returncode: raise RuntimeError('Host pointer preflight failed')
            result['success'] = True
        finally:
            server.terminate(); server.wait(timeout=5)
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--output',type=Path,required=True)
    args = parser.parse_args()
    try:
        result = check()
    except Exception as error:
        # A CalledProcessError could include private Xauth arguments: omit all arguments.
        result = {'success':False,'scope':'Host-only pointer preflight','error_type':type(error).__name__}
    args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    print('Host pointer preflight success=' + str(result['success']))
    raise SystemExit(0 if result['success'] else 1)
