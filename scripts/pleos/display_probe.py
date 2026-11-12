"""Read-only display/activity facts and original physical-display screenshots for a fresh AVD."""
from pathlib import Path
import json
import re
import subprocess
from adb_readiness import diagnostic


def collect(adb,serial,reports):
    reports=Path(reports)
    checks={}
    filters={
        'display':r'DisplayDeviceInfo|DisplayInfo\{|mDisplayId|displayId|mDisplayState|state=|uniqueId=',
        'activity':r'Display #|displayId=|mDisplayId=|ResumedActivity|topResumedActivity|dev\.leemh\.dochagjari',
        'window':r'mCurrentFocus|mFocusedApp|mDisplayId|Window\{|isOnScreen|mHasSurface|mViewVisibility',
        'power':r'mWakefulness|mInteractive|mDisplayReady|Display Power|mScreenOn|mUserActivitySummary',
        'input':r'dev\.leemh|Focused|focused|name=|touchable|frame=|transform=|displayId',
        'app_errors':r'dev\.leemh\.dochagjari|ANR in|FATAL EXCEPTION'}
    for name,args in [('size',['wm','size']),('density',['wm','density']),('current_user',['am','get-current-user']),
                      ('display',['dumpsys','display']),('activity',['dumpsys','activity','activities']),
                      ('window',['dumpsys','window','windows']),('power',['dumpsys','power']),
                      ('input',['dumpsys','input']),('app_errors',['logcat','-d','-v','brief','ActivityManager:I','ActivityTaskManager:I','AndroidRuntime:E','*:S']),
                      ('physical_ids',['dumpsys','SurfaceFlinger','--display-id']),('uiautomator_help',['uiautomator','help'])]:
        try:
            r=subprocess.run([adb,'-s',serial,'shell',*args],capture_output=True,text=True,timeout=20)
            output=r.stdout
            if name in filters:output='\n'.join(l for l in output.splitlines() if re.search(filters[name],l))
            checks[name]={'returncode':r.returncode,'stdout':output[:24000],'stderr':diagnostic(r.stderr)}
        except (OSError,subprocess.SubprocessError) as error:
            checks[name]={'returncode':None,'stdout':'','stderr':diagnostic(str(error))}
        (reports/'display-activity.json').write_text(json.dumps({'scope':'Fresh test AVD only; read-only; no launch/display/permission policy changes','checks':checks},indent=2)+'\n')
    physical=checks.get('physical_ids',{})
    ids=list(dict.fromkeys(re.findall(r'^Display (\d+)\b',physical.get('stdout',''),re.M)))[:4] if physical.get('returncode')==0 else []
    captures=[]
    for display in ids:
        name='physical-display-'+display+'.png'
        try:
            r=subprocess.run([adb,'-s',serial,'exec-out','screencap','-p','-d',display],capture_output=True,timeout=20)
            valid=r.returncode==0 and r.stdout.startswith(b'\x89PNG\r\n\x1a\n')
            if valid:(reports/name).write_bytes(r.stdout)
            captures.append({'physical_id':display,'returncode':r.returncode,'valid_png':valid,'file':name if valid else None,
                             'stderr':diagnostic(r.stderr.decode(errors='replace'))})
        except (OSError,subprocess.SubprocessError) as error:
            captures.append({'physical_id':display,'returncode':None,'error':diagnostic(str(error))})
    (reports/'physical-display-captures.json').write_text(json.dumps(captures,indent=2)+'\n')
