"""Ordinary touchscreen gesture through this job's authenticated X11 emulator window."""
from pathlib import Path
import os
import json
import re
import subprocess
import time
from x11_context import window_inventory,observe_pointer,native_input_target


def project_point(window,size,point):
    x,y=point;pw,ph=size;w,h=window['width'],window['height']
    if min(pw,ph,w,h)<=0 or min(window['x'],window['y'])<0 or not (0<=x<pw and 0<=y<ph):
        raise ValueError('Point or viewport outside supported geometry')
    if abs(w/h-pw/ph)>.005:raise ValueError('Viewport/device aspect ratio does not agree')
    return [window['x']+round(x*w/pw),window['y']+round(y*h/ph)]


def touch(adb,serial,point,reports,index):
    result={'clicked':False,'method':'Normal visible emulator touchscreen; no Android permission/display policy changes'}
    try:
        authority=Path(os.environ['ANDROID_AVD_HOME']).parent/'xauthority'
        if authority.is_symlink() or not authority.is_file() or authority.stat().st_uid!=os.getuid() or authority.stat().st_mode&0o077:
            raise RuntimeError('Own private Xauthority is not ready; no host input')
        env=os.environ|{'DISPLAY':':91','XAUTHORITY':str(authority)}
        def read(args):return subprocess.run(args,env=env,capture_output=True,text=True,check=True,timeout=10).stdout
        focus=read([adb,'-s',serial,'shell','dumpsys','window','windows'])
        if not re.search(r'mCurrentFocus=.*dev\.leemh\.dochagjari/.*MainActivity',focus):
            raise RuntimeError('Fresh Android app focus is not confirmed; no host input')
        physical=read([adb,'-s',serial,'shell','dumpsys','SurfaceFlinger','--display-id'])
        if len(re.findall(r'^Display \d+\b',physical,re.M))!=1:
            raise RuntimeError('Single physical display not confirmed; no coordinate guessing')
        dimensions=read([adb,'-s',serial,'shell','wm','size'])
        match=re.search(r'Physical size: (\d+)x(\d+)',dimensions)
        if not match or 'Override size:' in dimensions:raise RuntimeError('Unambiguous physical dimensions unavailable')
        inventory=window_inventory(read(['xwininfo','-root','-tree']))
        views=[w for w in inventory['windows'] if any(c.casefold()=='emulator' for c in w['classes'])]
        if len(views)!=1:raise RuntimeError('Unique emulator viewport not confirmed')
        view=views[0];host=project_point(view,tuple(map(int,match.groups())),point)
        read(['xdotool','mousemove',*map(str,host)])
        before=observe_pointer(env,host);result['before']=before
        if native_input_target(before)!=view['id']:raise RuntimeError('Native host input target is unknown')
        read(['xdotool','windowfocus',str(view['id'])]);time.sleep(.2)
        after=observe_pointer(env,host);result['after_activation']=after
        fresh=[w for w in after.get('window_inventory',{}).get('windows',[]) if w['id']==view['id']]
        if (native_input_target(after)!=view['id'] or after.get('native_state',{}).get('direct_focus',{}).get('window')!=view['id'] or fresh!=[view]):
            raise RuntimeError('Fresh normal host focus/geometry not confirmed')
        result.update(device_point=list(point),host_point=host,viewport=view)
        subprocess.run(['import','-window','root',str(Path(reports,f'host-touch-{index}-before.png'))],env=env,check=True,capture_output=True,timeout=15)
        try:read(['xdotool','mousedown','1','sleep','0.2','mouseup','1'])
        finally:subprocess.run(['xdotool','mouseup','1'],env=env,capture_output=True,timeout=5)
        result['clicked']=True
        subprocess.run(['import','-window','root',str(Path(reports,f'host-touch-{index}-after.png'))],env=env,check=True,capture_output=True,timeout=15)
        return result
    except Exception as error:
        result['error']=str(error)
        raise
    finally:
        Path(reports,f'host-touch-{index}.json').write_text(json.dumps(result,indent=2)+'\n')
