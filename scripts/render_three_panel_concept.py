"""Video-layout concept: approved static reference + explicitly repacked real app crops.

Never records a Pleos session or proves native multi-window/navigation integration.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    out = args.output.resolve(); out.mkdir(parents=True, exist_ok=True)
    reference = Path('docs/ux/photo-based-3d/05-camera-and-3d-map.png').resolve()
    recording = Path('docs/evidence/arrival-panel/f064-device/product-ux/arrival-context-demo.mp4').resolve()
    font = '/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc'
    labels = {
        'title': '도착자리 · 3분할 UX 구성 예상도',
        'scope': '차량/내비: 기존 생성 시안(정지)  |  오른쪽: 실제 Android 앱 영상 재배치  |  Pleos 동시 실행 미검증',
        'vehicle': '① 차량 정보 · 정지 시안',
        'navi': '② 순정 내비 영역 · 정지 시안',
        'app': '③ 도착자리 · 앱 영상 재배치',
        'app_header': '도착자리',
        'destination': '5층 에뜨와 · 동선 미리보기',
        'source': '생성 영상 · 실제 차량 카메라 아님',
        'candidate': '저장 문자 후보 C12 · 주차층/위치 미확인',
        'route': '엘리베이터 → 에뜨와 · 경로 재생',
        'unknown': '승강기 호기·주차장 연결·현재 위치 미확인',
        'footer': 'UX 합성 미리보기 · 첫 두 화면은 실행 내비/차량 신호 아님 · 앱 동작 8초 후 정지 화면 12초',
    }
    for name, value in labels.items(): (out / f'{name}.txt').write_text(value)

    def text(name, x, y, size, color='white'):
        return f'drawtext=fontfile={font}:textfile={out / (name + ".txt")}:x={x}:y={y}:fontsize={size}:fontcolor={color}'

    # Reference native-frame coordinates: original 1672x896 at (124,112).
    # The entire original right card (including its fictional 2F/227 content) is covered.
    reference_filters = ','.join([
        'crop=1672:896:0:0', 'pad=1920:1080:124:112:color=0x101722',
        'drawbox=x=1239:y=172:w=541:h=746:color=0x101722:t=fill',
        text('title', 44, 12, 34), text('scope', 44, 62, 21, '0xBAC8DB'),
        text('vehicle', 160, 90, 16), text('navi', 724, 90, 16), text('app', 1260, 90, 16),
        text('app_header', 1260, 188, 26), text('destination', 1260, 222, 23),
        text('route', 1260, 826, 21), text('unknown', 1260, 866, 17, '0xBAC8DB'),
        text('footer', 124, 1036, 20, '0xBAC8DB'),
    ])
    graph = (
        f'[0:v]{reference_filters}[base];'
        '[1:v]split=2[camera][map];'
        '[camera]crop=160:106:560:160,scale=500:182:force_original_aspect_ratio=decrease,'
        'pad=500:182:(ow-iw)/2:(oh-ih)/2:color=0x0B111C[cam];'
        '[map]crop=400:350:445:350,scale=500:330:force_original_aspect_ratio=decrease,'
        'pad=500:330:(ow-iw)/2:(oh-ih)/2:color=0x101722[route];'
        '[base][cam]overlay=1259:254:shortest=1[withcam];'
        '[withcam][route]overlay=1259:478:shortest=1,'
        # Provenance stays readable after laying out the crops; it is not an OCR result overlay.
        f'{text("source", 1268, 274, 17)},'
        f'{text("candidate", 1260, 448, 17, "0xBAC8DB")},'
        'setsar=1[v]'
    )
    target = out / 'dochagjari-three-panel-concept.mp4'
    # Normalize the variable-rate Android recording before composing multiple overlays.
    motion = out / 'input-motion-8s.mp4'
    frozen = out / 'input-motion-and-freeze.mp4'
    subprocess.run(['ffmpeg', '-hide_banner', '-loglevel', 'error', '-y', '-ss', '46.5', '-i', str(recording),
        '-t', '8', '-r', '30', '-an', '-c:v', 'libx264', '-preset', 'veryfast', '-crf', '22',
        '-pix_fmt', 'yuv420p', str(motion)], check=True)
    subprocess.run(['ffmpeg', '-hide_banner', '-loglevel', 'error', '-y', '-i', str(motion),
        '-vf', 'tpad=stop_mode=clone:stop_duration=12', '-t', '20', '-r', '30', '-an',
        '-c:v', 'libx264', '-preset', 'veryfast', '-crf', '22', '-pix_fmt', 'yuv420p', str(frozen)], check=True)
    command = ['ffmpeg', '-hide_banner', '-loglevel', 'error', '-y', '-loop', '1', '-i', str(reference),
        '-i', str(frozen), '-filter_complex', graph, '-map', '[v]', '-t', '20', '-r', '30',
        '-an', '-c:v', 'libx264', '-preset', 'veryfast', '-crf', '22', '-pix_fmt', 'yuv420p',
        '-movflags', '+faststart', str(target)]
    subprocess.run(command, check=True)
    probe = json.loads(subprocess.check_output(['ffprobe', '-v', 'error', '-show_format', '-show_streams', '-of', 'json', str(target)], text=True))
    assert abs(float(probe['format']['duration']) - 20) < .2
    probe['format']['filename'] = target.name
    report = {
        'kind': 'UX composition only; generated static first/second panels, repacked real Android video crops in third',
        'app_code_commit': 'f0640b00da92409c265cfd5563238e0d972edcaa', 'version': '0.11.0/11',
        'native_pleos_multi_window_verified': False, 'live_native_navigation': False,
        'actual_vehicle_signals': False, 'real_camera': False, 'app_crops_rearranged_in_editor': True,
        'reference': {'file': str(reference.relative_to(Path.cwd())), 'sha256': digest(reference),
                      'kind': 'Previously approved generated design; not a real native screenshot'},
        'recording': {'file': str(recording.relative_to(Path.cwd())), 'sha256': digest(recording),
                      'start': 46.5, 'motion_seconds': 8, 'freeze_seconds': 12,
                      'camera_crop': [560, 160, 160, 106], 'map_crop': [445, 350, 400, 350]},
        'labels': labels, 'file': target.name, 'bytes': target.stat().st_size, 'sha256': digest(target), 'ffprobe': probe,
    }
    (out / 'metadata.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({'file': target.name, 'bytes': report['bytes'], 'sha256': report['sha256']}))


if __name__ == '__main__':
    main()
