"""Package current APKs and derive identity from the manifests/signatures, never a fixed version."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess


def collect(code_commit, output, sdk, device_only=False):
    output.mkdir(parents=True, exist_ok=True)
    build_tools = sdk / 'build-tools' / '35.0.0'
    app_version = None
    items = []
    sources = [('debug', 'debug/app-debug.apk'), ('androidTest', 'androidTest/debug/app-debug-androidTest.apk')]
    if not device_only:
        sources.append(('unsigned-release', 'release/app-release-unsigned.apk'))
    for kind, source in sources:
        apk = Path('app/build/outputs/apk') / source
        badging = subprocess.run([str(build_tools / 'aapt'), 'dump', 'badging', str(apk)], check=True, text=True, capture_output=True).stdout.splitlines()[0]
        identity = dict(re.findall(r"(?:^|\s)(name|versionCode|versionName)='([^']*)'", badging))
        expected_package = 'dev.leemh.dochagjari' + ('.test' if kind == 'androidTest' else '')
        assert identity['name'] == expected_package, f'Unexpected APK package: {identity}'
        if kind == 'debug':
            app_version = (identity['versionName'], int(identity['versionCode']))
        elif kind == 'unsigned-release':
            assert app_version == (identity['versionName'], int(identity['versionCode']))
        target = output / f'dochagjari-{app_version[0]}-{kind}.apk'
        shutil.copyfile(apk, target)
        check = subprocess.run([str(build_tools / 'apksigner'), 'verify', '--print-certs', str(target)], text=True, capture_output=True)
        item = dict(kind=kind, file=target.name, bytes=target.stat().st_size,
                    sha256=hashlib.sha256(target.read_bytes()).hexdigest(), manifest=identity)
        if kind == 'unsigned-release':
            assert check.returncode != 0, 'Expected unsigned release'
            item['signing'] = 'unsigned; not installable until signed'
        else:
            assert check.returncode == 0, check.stderr
            item['signing'] = 'Android development/debug key; compare certificate before installation'
            item['signer_sha256'] = next(line.split(': ', 1)[1] for line in check.stdout.splitlines() if 'certificate SHA-256 digest:' in line)
        items.append(item)
    assert items[0]['signer_sha256'] == items[1]['signer_sha256']
    metadata = dict(code_commit=code_commit, versionName=app_version[0], versionCode=app_version[1],
                    workflow_run=os.environ.get('GITHUB_RUN_ID'), artifacts=items)
    (output / 'apk-metadata.json').write_text(json.dumps(metadata, indent=2) + '\n')
    (output / 'SHA256SUMS').write_text(''.join(f"{item['sha256']}  {item['file']}\n" for item in items))
    print(f'Packaged {len(items)} APKs, version {app_version[0]}/{app_version[1]}, code {code_commit}')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--code-commit', required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--device-only', action='store_true', help='Package the exact debug/test outputs used by the device job')
    args = parser.parse_args()
    commit = subprocess.run(['git', 'rev-parse', '--verify', args.code_commit + '^{commit}'], text=True, capture_output=True, check=True).stdout.strip()
    collect(commit, args.output, Path(os.environ['ANDROID_HOME']), args.device_only)
