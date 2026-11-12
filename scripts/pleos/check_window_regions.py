"""Reproduce a visible but non-interactive OWN synthetic window; never changes an emulator."""
import ctypes as C
import json
import os
from pathlib import Path
import secrets
import subprocess
import tempfile
import time
import argparse
from native_window_state import NativeDisplay,Rectangle


def check():
    with tempfile.TemporaryDirectory(prefix='x11-input-region-') as folder:
        authority=Path(folder)/'authority';authority.touch(mode=0o600)
        number=next(n for n in range(97,110) if not Path(f'/tmp/.X11-unix/X{n}').exists())
        environment=os.environ|{'DISPLAY':f':{number}','XAUTHORITY':str(authority)}
        auth=subprocess.run(['xauth','-f',str(authority),'add',f':{number}','MIT-MAGIC-COOKIE-1',secrets.token_hex(16)],capture_output=True)
        if auth.returncode:raise RuntimeError('Private Xauthority setup failed')
        server=subprocess.Popen(['Xvfb',f':{number}','-screen','0','800x600x24','-nolisten','tcp','-auth',str(authority)],
                                env=environment,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
        previous={name:os.environ.get(name) for name in ['DISPLAY','XAUTHORITY']}
        try:
            for _ in range(30):
                ready=subprocess.run(['xdotool','getdisplaygeometry'],env=environment,capture_output=True)
                if ready.returncode==0:break
                time.sleep(.1)
            else:raise RuntimeError('Synthetic X server unavailable')
            os.environ.update({name:environment[name] for name in previous})
            display=NativeDisplay()
            try:
                lib=display.lib;shape=display.shape;d=display.display
                lib.XCreateSimpleWindow.argtypes=[C.c_void_p,C.c_ulong,C.c_int,C.c_int,C.c_uint,C.c_uint,C.c_uint,C.c_ulong,C.c_ulong]
                lib.XCreateSimpleWindow.restype=C.c_ulong
                lib.XMapWindow.argtypes=[C.c_void_p,C.c_ulong];lib.XUnmapWindow.argtypes=[C.c_void_p,C.c_ulong]
                shape.XShapeCombineRectangles.argtypes=[C.c_void_p,C.c_ulong,C.c_int,C.c_int,C.c_int,C.POINTER(Rectangle),C.c_int,C.c_int,C.c_int]
                window=lib.XCreateSimpleWindow(d,display.root,100,100,240,120,0,0,0x447788)
                lib.XMapWindow(d,window);lib.XSync(d,0)
                subprocess.run(['xdotool','mousemove','200','150'],env=environment,capture_output=True,check=True,timeout=3)
                normal=display.snapshot([window],(200,150))
                # Only this newly-created toy window: reproduce empty input shape without hiding it.
                shape.XShapeCombineRectangles(d,window,2,0,0,None,0,0,0);lib.XSync(d,0)
                empty=display.snapshot([window],(200,150))
                lib.XUnmapWindow(d,window);lib.XSync(d,0)
                unmapped=display.snapshot([window],(200,150))
                assert not normal['x11_errors'] and not empty['x11_errors'] and not unmapped['x11_errors']
                assert normal['direct_pointer']['child']==window
                assert normal['windows'][0]['point_in_input_region'] is True
                assert empty['windows'][0]['map_state']==2 and empty['windows'][0]['input_rectangles_count']==0
                assert empty['direct_pointer']['child']==0
                assert unmapped['windows'][0]['map_state']==0
                return {'success':True,'scope':'Own synthetic host window only; no emulator/ADB or authentication change',
                        'normal':normal,'visible_empty_input':empty,'unmapped':unmapped}
            finally:display.close()
        finally:
            for name,value in previous.items():
                if value is None:os.environ.pop(name,None)
                else:os.environ[name]=value
            server.terminate();server.wait(timeout=5)


if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--output',required=True,type=Path);args=parser.parse_args()
    try:result=check()
    except Exception as error:result={'success':False,'scope':'Host-only input-region fixture','error_type':type(error).__name__}
    args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_text(json.dumps(result,indent=2)+'\n')
    print('Synthetic window input-region check success='+str(result['success']))
    raise SystemExit(0 if result['success'] else 1)
