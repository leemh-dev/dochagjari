"""Standard ADB instrumentation on this job's own fresh AVD; independent of AGP console lookup."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
from adb_readiness import diagnostic,probe


def suite_passed(returncode,output,expected=6):
    return (returncode==0 and bool(re.search(r'^OK \('+str(expected)+r' tests\)\s*$',output,re.M)) and
            bool(re.search(r'^INSTRUMENTATION_CODE: -1\s*$',output,re.M)) and
            len(re.findall(r'^INSTRUMENTATION_STATUS_CODE: 0\s*$',output,re.M))==expected and
            not re.search(r'^INSTRUMENTATION_(?:FAILED:|STATUS_CODE: -[12]\s*$)',output,re.M))


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--serial',required=True)
    parser.add_argument('--reset-test-data',action='store_true')
    parser.add_argument('--output',default='app/build/reports/pleos/direct-instrumentation')
    args=parser.parse_args()
    if not args.reset_test_data or os.getenv('DOCHAGJARI_FRESH_TEST_AVD')!='1':
        parser.error('Explicit reset and DOCHAGJARI_FRESH_TEST_AVD=1 required; never use an existing user device')
    sdk=os.getenv('ANDROID_HOME') or os.getenv('ANDROID_SDK_ROOT')
    adb=str(Path(sdk)/'platform-tools/adb') if sdk else 'adb'
    output=Path(args.output);output.parent.mkdir(parents=True,exist_ok=True)
    report={'driver':'Standard adb shell am instrument; not connectedDebugAndroidTest',
            'input_scope':'Existing tests use app test vehicle/navigation/mock notifications and prerecorded video; device identity is recorded separately, not native SDK/car/SMS proof',
            'success':False,'commands':[]}
    transcript=[];package='dev.leemh.dochagjari'
    def run(*command,timeout=120):
        result=subprocess.run([adb,'-s',args.serial,*command],capture_output=True,text=True,timeout=timeout)
        transcript.append('$ adb '+ ' '.join(command)+'\n'+result.stdout+result.stderr)
        report['commands'].append({'args':list(command),'returncode':result.returncode})
        if result.returncode:raise RuntimeError('ADB command failed: '+' '.join(command))
        return result
    try:
        report['readiness']=probe(adb,args.serial)
        if not report['readiness']['ready']:raise RuntimeError('Live boot/services not ready')
        if not run('shell','pm','path',package).stdout.strip().startswith('package:'):
            raise RuntimeError('Target APK is absent; no instrumentation attempted')
        report['device_build_fingerprint']=run('shell','getprop','ro.build.fingerprint').stdout.strip()
        report['device_api_level']=run('shell','getprop','ro.build.version.sdk').stdout.strip()
        run('install','--no-streaming','-t','-r','app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk',timeout=180)
        if target := os.getenv('GITHUB_OUTPUT'):
            with Path(target).open('a') as stream:stream.write('test_apk_installed=true\n')
        run('shell','pm','clear',package)
        result=run('shell','am','instrument','-w','-r','-e','class',package+'.AppInstrumentedTest',
                   package+'.test/androidx.test.runner.AndroidJUnitRunner',timeout=600)
        report['completed_test_statuses']=len(re.findall(r'^INSTRUMENTATION_STATUS_CODE: 0\s*$',result.stdout,re.M))
        report['success']=suite_passed(result.returncode,result.stdout)
        if not report['success']:raise RuntimeError('Six completed passing statuses and final OK were not all present')
    except subprocess.TimeoutExpired as error:
        report['error']='Instrumentation/ADB execution timed out; not passed'
        partial=error.stdout or b''
        transcript.append(partial.decode(errors='replace') if isinstance(partial,bytes) else partial)
        raise
    except Exception as error:
        report['error']=diagnostic(str(error));raise
    finally:
        output.with_suffix('.json').write_text(json.dumps(report,indent=2)+'\n')
        output.with_suffix('.txt').write_text('\n'.join(diagnostic(line) for text in transcript for line in text.splitlines())+'\n')


if __name__=='__main__':main()
