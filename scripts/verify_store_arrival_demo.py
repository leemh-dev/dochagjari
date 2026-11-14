"""Validate the encoded shopping UX film; does not run Android or Pleos checks."""
from pathlib import Path
import argparse,hashlib,json,subprocess,importlib.util
import numpy as np
from PIL import Image
parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--output',type=Path,required=True);args=parser.parse_args()
out=args.output;video=out/'dochagjari-store-arrival-demo.mp4'
decode=subprocess.run(['ffmpeg','-v','error','-i',str(video),'-f','null','-'],capture_output=True,text=True)
assert decode.returncode==0,decode.stderr
probe=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_format','-show_streams','-of','json',str(video)],text=True))
assert abs(float(probe['format']['duration'])-72)<.1
s=probe['streams'][0];assert (s['width'],s['height'],s['nb_frames'])==(1920,1080,'2160')
frames={}
for sec in [4,10,18,31,39,41,47,55,61,65,69]:
 target=out/f'video-frame-{sec:02}.png'
 subprocess.run(['ffmpeg','-v','error','-y','-ss',str(sec),'-i',str(video),'-frames:v','1',str(target)],check=True)
 frames[sec]=Image.open(target).convert('RGB')
def diff(a,b,area):
 x=np.array(frames[a].crop(area),dtype=np.float32);y=np.array(frames[b].crop(area),dtype=np.float32)
 return float(np.abs(x-y).mean())
results={'moving_camera_18_vs_31_mae':diff(18,31,(1264,251,1756,521)), 'parked_camera_39_vs_41_mae':diff(39,41,(1264,251,1756,521)), 'parked_thumbnail_47_vs_61_mae':diff(47,61,(1264,251,1756,371)), 'floor_route_55_vs_61_mae':diff(55,61,(1264,440,1756,801))}
assert results['moving_camera_18_vs_31_mae']>1
assert results['parked_camera_39_vs_41_mae']<1
# H.264 is lossy: independently prove that pre-encode camera crops are identical.
spec=importlib.util.spec_from_file_location('store_film',Path(__file__).with_name('render_store_arrival_demo.py'))
renderer=importlib.util.module_from_spec(spec);spec.loader.exec_module(renderer)
film=renderer.Film()
raw_source_checks={}
for a,b,area in [(39,41,(1264,251,1756,521)),(47,61,(1264,251,1756,371))]:
    left=film.draw(a).crop(area).tobytes();right=film.draw(b).crop(area).tobytes()
    assert left==right
    raw_source_checks[f'{a}_vs_{b}']={'identical':True,'sha256':hashlib.sha256(left).hexdigest()}
assert results['parked_thumbnail_47_vs_61_mae']<3
assert results['floor_route_55_vs_61_mae']>.1
story=json.loads((out/'storyboard.json').read_text())
assert story['stages'][0]['layout']=='wide_navigation' and story['stages'][0]['camera']=='hidden'
assert all(x['parked'] for x in story['stages'] if x['map'] in ['lift','floor_5f'])
assert all(x['camera'].startswith('parked') or x['camera']=='off' for x in story['stages'] if x['parked'])
assert story['stages'][-1]['camera']=='off'
report={'date_utc':'2026-11-14','full_decode':{'returncode':decode.returncode,'stderr':decode.stderr},'duration_seconds':float(probe['format']['duration']),'frames':int(s['nb_frames']),'video_dimensions':[s['width'],s['height']],'fps':s['r_frame_rate'],'scenario_contract':'road navigation spans two panels before arrival; floor guidance requires parked; no moving camera after parked; closed camera off','encoded_pixel_checks':results,'pre_encode_camera_crops':raw_source_checks,'thumbnail_lossy_decode_mae_tolerance_8bit':3,'checks_passed':['entire72s_decode','1080p30_2160_frames','road_service_hidden_storyboard','floor_after_parked_storyboard','parked_camera_stable_encoded','parked_thumbnail_stable_encoded','map_route_changes_while_parked_encoded','camera_off_on_close_storyboard'],'scope':'media validation only; not app/native Pleos/vehicle/OCR verification','file_sha256':hashlib.sha256(video.read_bytes()).hexdigest()}
(out/'validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(report,ensure_ascii=False))

contact=Image.new('RGB',(2880,1620),'#0D1420')
for idx,sec in enumerate([4,10,18,31,39,47,55,61,69]):
    contact.paste(frames[sec].resize((960,540),Image.Resampling.LANCZOS),((idx%3)*960,(idx//3)*540))
contact.save(out/'encoded-storyboard.jpg',quality=88)
