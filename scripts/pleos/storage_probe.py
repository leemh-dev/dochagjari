"""Read-only storage facts for this fresh test AVD; no cleanup or policy changes."""
from pathlib import Path
import json
import subprocess
from adb_readiness import diagnostic


def collect(adb,serial,apk,reports):
    def read(args):
        try:
            r=subprocess.run(args,capture_output=True,text=True,timeout=20)
            return dict(returncode=r.returncode,stdout=diagnostic(r.stdout),stderr=diagnostic(r.stderr))
        except (OSError,subprocess.SubprocessError) as error:
            return dict(returncode=None,stdout='',stderr=diagnostic(str(error)))
    result={'apk_bytes':Path(apk).stat().st_size,'scope':'Fresh dedicated test AVD; read-only; no data deletion or storage-policy changes',
            'host_df':read(['df','-Pk',str(reports)]),'guest':{}}
    for name,args in [('df_data',['df','-k','/data','/data/local/tmp']),
                      ('install_location',['pm','get-install-location']),
                      ('low_bytes',['settings','get','global','sys_storage_threshold_max_bytes']),
                      ('low_percent',['settings','get','global','sys_storage_threshold_percentage']),
                      ('storage_monitor',['dumpsys','devicestoragemonitor'])]:
        result['guest'][name]=read([adb,'-s',serial,'shell',*args])
    Path(reports,'storage-before-install.json').write_text(json.dumps(result,indent=2)+'\n')
    return result
