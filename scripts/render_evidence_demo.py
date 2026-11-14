"""Compose an explicitly edited demo from one run's actual app captures (FFmpeg only)."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--reports', type=Path, required=True)
    parser.add_argument('--metadata', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--route-start', type=float, required=True,
                        help='Visually verified first second of the actual route animation in screenrecord')
    parser.add_argument('--font', default='/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc')
    args = parser.parse_args()
    product = args.reports / 'product-ux'
    occupant = args.reports / 'occupant-context'
    result = json.loads((product / 'summary.json').read_text())
    settings = json.loads((occupant / 'occupant-ui.json').read_text())
    metadata = json.loads(args.metadata.read_text())
    assert result.get('success') and settings.get('success'), 'Do not present an incomplete capture flow as completed'
    assert result['source_commit'] == settings['source_commit'] == metadata['code_commit'], 'Different source runs'
    assert args.route_start >= 0
    out = args.output.resolve(); out.mkdir(parents=True, exist_ok=True)
    source = metadata['code_commit']
    # All stills are actual screenshots, with visible environment/cut disclosure throughout.
    shots = [
        (product / '01-product-destination.png', 6, '도착자리 · 낯선 곳에 도착한 다음의 안내'),
        (occupant / '02-child-confirmed.png', 8, '오늘의 탑승을 선택해요 · 목적지와 독립된 설정'),
        (product / '02-visit-ready.png', 8, '저장한 자료로 에뜨와 판교점 방문을 준비해요'),
        (product / '03-mock-road-handoff.png', 8, '도로 내비로 요청을 전달해요 · 이번 실행은 MOCK'),
        (product / '04a-parking-map.png', 8, '정차 후 입차를 명시 확인 · 주차 지도는 배치 예시'),
        (product / '04c-android-generated-c12-candidate.png', 6, '생성 영상의 C12 문자 후보 · 실제 위치 확정 아님'),
        (product / 'arrival-context-demo.mp4', 8, '5층 엘리베이터에서 에뜨와까지 · 실제 앱 경로 재생'),
        (product / '08-parking-saved-unknown.png', 8, '주차 완료 · 관측을 중지하고 저장 결과를 확인해요'),
        (product / '12-return-manual-clue.png', 8, '돌아갈 때는 직접 보완한 저장 단서를 확인해요'),
        (product / '06a-hospital.png', 6, '병원 동행 · 사용자 조건에 맞춘 가상 후보'),
        (product / '06e-academy-diversion.png', 6, '학원 픽업 · 이용 불가 후보를 제외하고 대안을 확인해요'),
        (product / '15-driving-brief.png', 6, '주행 중에는 상세 조작을 제한하고 자동 재개하지 않아요'),
        (product / '13-restored-home-no-auto-guide.png', 4, '구현 범위와 실제 연동을 구분한 도착 안내 프로토타입'),
    ]
    assert sum(length for _, length, _ in shots) == 90
    font = Path(args.font); assert font.is_file()
    header = out / 'header.txt'
    header.write_text('도착자리  ' + metadata['versionName'] + '  |  실제 Android 앱 시연  |  코드 ' + source[:7])
    badge = out / 'scope.txt'
    badge.write_text('일반 Android · TEST / MOCK / 생성 영상  |  같은 APK의 장면 편집 · 시간 생략')
    def frame_filters(text):
        return (
            'scale=1856:820:force_original_aspect_ratio=decrease,'
            'pad=1920:1080:(ow-iw)/2:112:color=0x101722,setsar=1,'
            f'drawtext=fontfile={font}:textfile={header}:fontcolor=white:fontsize=34:x=40:y=18,'
            f'drawtext=fontfile={font}:textfile={badge}:fontcolor=0xBAC8DB:fontsize=24:x=40:y=66,'
            f'drawtext=fontfile={font}:textfile={text}:fontcolor=white:fontsize=31:x=40:y=974'
        )

    sources = []
    subtitles = []
    at = 0
    for i, (path, length, caption) in enumerate(shots):
        assert path.is_file(), str(path)
        text = out / f'caption-{i:02}.txt'; text.write_text(caption)
        target = out / f'segment-{i:02}.mp4'
        video = path.suffix == '.mp4'
        filters = frame_filters(text)
        command = ['ffmpeg', '-hide_banner', '-loglevel', 'error', '-y']
        command += ['-ss', str(args.route_start)] if video else ['-loop', '1']
        command += ['-i', str(path), '-t', str(length), '-an', '-vf', filters, '-r', '30',
                    '-c:v', 'libx264', '-preset', 'veryfast', '-crf', '23', '-pix_fmt', 'yuv420p', str(target)]
        subprocess.run(command, check=True)
        actual_duration = float(subprocess.check_output(['ffprobe', '-v', 'error', '-show_entries',
            'format=duration', '-of', 'default=nw=1:nk=1', str(target)], text=True))
        assert abs(actual_duration - length) < .15, 'Animation clip ended before the requested cut'
        def stamp(seconds):
            return f'{seconds//3600:02}:{seconds//60%60:02}:{seconds%60:02},000'
        subtitles.append(f'{i+1}\n{stamp(at)} --> {stamp(at+length)}\n{caption}\n')
        sources.append({'input': str(path.relative_to(args.reports)), 'seconds': length,
                        'sha256': hashlib.sha256(path.read_bytes()).hexdigest(), 'caption': caption,
                        'kind': 'actual Android screenrecord cut' if video else 'actual Android still capture',
                        **({'start': args.route_start} if video else {})})
        at += length
    listing = out / 'segments.txt'
    listing.write_text(''.join(f"file 'segment-{i:02}.mp4'\n" for i in range(len(shots))))
    final = out / 'dochagjari-android-demo.mp4'
    subprocess.run(['ffmpeg', '-hide_banner', '-loglevel', 'error', '-y', '-f', 'concat', '-safe', '0',
        '-i', str(listing), '-c', 'copy', '-movflags', '+faststart', str(final)], check=True)
    (out / 'dochagjari-android-demo.srt').write_text('\n'.join(subtitles))
    probe = json.loads(subprocess.check_output(['ffprobe', '-v', 'error', '-show_format', '-show_streams',
        '-of', 'json', str(final)], text=True))
    assert abs(float(probe['format']['duration']) - 90) < .2
    probe['format']['filename'] = final.name
    motion = out / 'dochagjari-motion-sample.mp4'
    motion_caption = out / 'motion-caption.txt'
    motion_caption.write_text('실제 앱 모션 · 생성 영상 후보에서 5층 매장행 경로 재생까지')
    motion_start = max(0, args.route_start - 6)
    subprocess.run(['ffmpeg', '-hide_banner', '-loglevel', 'error', '-y', '-ss', str(motion_start),
        '-i', str(product / 'arrival-context-demo.mp4'), '-t', '20', '-an', '-vf', frame_filters(motion_caption),
        '-r', '30', '-c:v', 'libx264', '-preset', 'veryfast', '-crf', '23', '-pix_fmt', 'yuv420p',
        '-movflags', '+faststart', str(motion)], check=True)
    motion_probe = json.loads(subprocess.check_output(['ffprobe', '-v', 'error', '-show_format', '-show_streams',
        '-of', 'json', str(motion)], text=True))
    assert abs(float(motion_probe['format']['duration']) - 20) < .2
    motion_probe['format']['filename'] = motion.name
    report = {'code_commit': source, 'version': metadata['versionName'], 'versionCode': metadata['versionCode'],
        'kind': 'Edited actual generic Android captures; still montage plus real eight-second app motion cut',
        'native_pleos': False, 'actual_camera': False, 'actual_speech': False,
        'file': final.name, 'bytes': final.stat().st_size, 'sha256': hashlib.sha256(final.read_bytes()).hexdigest(),
        'sources': sources, 'ffprobe': probe,
        'motion_sample': {'file': motion.name, 'start_in_original': motion_start,
            'kind': 'Continuous twenty-second cut of the actual Android recording',
            'bytes': motion.stat().st_size, 'sha256': hashlib.sha256(motion.read_bytes()).hexdigest(), 'ffprobe': motion_probe}}

    (out / 'video-metadata.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({k: report[k] for k in ['file', 'bytes', 'sha256']}, ensure_ascii=False))


if __name__ == '__main__':
    main()
