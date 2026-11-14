"""Video-layout concept: static reference + licensed real driving video + real app map crop.

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
    parser.add_argument('--camera-source', type=Path, default=Path('docs/media/front-driving/front-driving-20s.mp4'))
    parser.add_argument('--camera-metadata', type=Path, default=Path('docs/media/front-driving/source.json'))
    args = parser.parse_args()
    out = args.output.resolve(); out.mkdir(parents=True, exist_ok=True)
    reference = Path('docs/ux/photo-based-3d/05-camera-and-3d-map.png').resolve()
    recording = Path('docs/evidence/arrival-panel/f064-device/product-ux/arrival-context-demo.mp4').resolve()
    camera = args.camera_source.resolve()
    camera_metadata = json.loads(args.camera_metadata.read_text())
    assert digest(camera) == camera_metadata['sha256'], 'Camera footage must match its source/license metadata'
    font = '/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc'
    labels = {
        'title': '도착자리 · 3분할 UX 구성 예상도',
        'scope': '차량/내비: 생성 정지 시안  |  상단: 공개 주행 실사  |  하단: 실제 Android 지도  |  Pleos 동시 실행 미검증',
        'vehicle': '① 차량 정보 · 정지 시안',
        'navi': '② 순정 내비 영역 · 정지 시안',
        'app': '③ 도착자리 · 실사/앱 지도 합성',
        'app_header': '도착자리',
        'destination': '5층 에뜨와 · 동선 미리보기',
        'source': '공개 전방 주행 실사 · OEM카메라 미연동',
        'candidate': '층·기둥 인식 미실행 · 현재 위치 미확인',
        'route': '엘리베이터 → 에뜨와 · 경로 재생',
        'unknown': '승강기 호기·주차장 연결·현재 위치 미확인',
        'credit': 'Footage: Relaxing Roads 4K / CC BY 3.0 (creativecommons.org/licenses/by/3.0) / Wikimedia Commons / edited, muted',
        'footer': 'UX 합성 · 차량/내비 정지 시안 · 상단 실사20초 연속 / 하단 앱 지도8초+정지12초',
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
        text('footer', 124, 1022, 18, '0xBAC8DB'), text('credit', 124, 1054, 14, '0xBAC8DB'),
    ])
    graph = (
        f'[0:v]{reference_filters}[base];'
        '[2:v]fps=30,setpts=PTS-STARTPTS,scale=500:216:force_original_aspect_ratio=decrease,'
        'pad=500:216:(ow-iw)/2:(oh-ih)/2:color=0x0B111C[cam];'
        '[1:v]crop=400:350:445:350,scale=500:294:force_original_aspect_ratio=decrease,'
        'pad=500:294:(ow-iw)/2:(oh-ih)/2:color=0x101722[route];'
        '[base][cam]overlay=1259:254:shortest=1[withcam];'
        '[withcam][route]overlay=1259:510:shortest=1,'
        # Provenance stays readable after laying out the crops; it is not an OCR result overlay.
        f'{text("source", 1268, 274, 17)},'
        f'{text("candidate", 1260, 480, 17, "0xBAC8DB")},'
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
        '-i', str(frozen), '-i', str(camera), '-filter_complex', graph, '-map', '[v]', '-t', '20', '-r', '30',
        '-an', '-c:v', 'libx264', '-preset', 'veryfast', '-crf', '22', '-pix_fmt', 'yuv420p',
        '-movflags', '+faststart', str(target)]
    subprocess.run(command, check=True)
    probe = json.loads(subprocess.check_output(['ffprobe', '-v', 'error', '-show_format', '-show_streams', '-of', 'json', str(target)], text=True))
    assert abs(float(probe['format']['duration']) - 20) < .2
    probe['format']['filename'] = target.name
    report = {
        'kind': 'UX composition only; generated static first/second panels, public real front-driving camera video, actual Android map crop',
        'app_code_commit': 'f0640b00da92409c265cfd5563238e0d972edcaa', 'version': '0.11.0/11',
        'native_pleos_multi_window_verified': False, 'live_native_navigation': False,
        'actual_vehicle_signals': False, 'oem_camera_connected': False, 'public_real_footage': True,
        'footage_ocr_executed': False, 'map_synchronized_to_real_footage': False, 'apk_changed': False,
        'app_crops_rearranged_in_editor': True,
        'reference': {'file': str(reference.relative_to(Path.cwd())), 'sha256': digest(reference),
                      'kind': 'Previously approved generated design; not a real native screenshot'},
        'recording': {'file': str(recording.relative_to(Path.cwd())), 'sha256': digest(recording),
                      'start': 46.5, 'motion_seconds': 8, 'freeze_seconds': 12,
                      'map_crop': [445, 350, 400, 350]},
        'camera_recording': {'file': str(camera.relative_to(Path.cwd())), 'sha256': digest(camera),
            'duration_seconds': 20, 'continuous': True, 'source': camera_metadata},
        'labels': labels, 'file': target.name, 'bytes': target.stat().st_size, 'sha256': digest(target), 'ffprobe': probe,
    }
    (out / 'metadata.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({'file': target.name, 'bytes': report['bytes'], 'sha256': report['sha256']}))


if __name__ == '__main__':
    main()
