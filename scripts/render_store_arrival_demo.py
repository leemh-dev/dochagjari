"""Render a phase-consistent shopping UX film; not a native Pleos screen recording.

Real, licensed mall garage footage is edited for entry/search/parking and frozen
at the explicitly staged parked moment. Only the maps use a tilted projection.
"""
import argparse
from functools import lru_cache
import hashlib
import json
import math
from pathlib import Path
import subprocess

import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageOps

ROOT = Path(__file__).resolve().parents[1]
W, H, FPS, DURATION = 1920, 1080, 30, 72
INK = '#0D1420'; PANEL = '#141F2E'; MUTED = '#A8B8CC'; WHITE = '#F6F8FC'
BLUE = '#4B9DFF'; TEAL = '#61DED0'
FONT = '/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc'
REFERENCE = ROOT / 'docs/ux/photo-based-3d/05-camera-and-3d-map.png'
MEDIA = ROOT / 'docs/media/mall-parking'
STAGES = [
    dict(id='road', start=0, end=8, title='백화점까지는 내비게이션으로', caption='양재 사옥 → 현대백화점 판교점 · 이동 중에는 도착자리 화면을 열지 않습니다.', layout='wide_navigation', camera='hidden', map='road', parked=False),
    dict(id='handoff', start=8, end=10, title='도로 안내가 끝나면, 도착자리가 이어받습니다', caption='목적지 도착 후 내비가 줄어들며 도착자리 패널이 열립니다. 분할 전환은 UX 연출입니다.', layout='transition', camera='hidden_to_mall', map='parking', parked=False),
    dict(id='entry', start=10, end=16, title='도착 이후 · 주차장 안으로', caption='상단은 실제 몰 주차장 영상, 하단은 주차장 지도입니다. 판교점 현장 영상은 아닙니다.', layout='three_panel', camera='mall_a_0_6', map='parking', parked=False),
    dict(id='search', start=16, end=28, title='주차장에서는, 주변 단서를 확인합니다', caption='주차장 이동 영상과 주차 안내를 함께 봅니다. 읽히지 않은 층·기둥은 미확인으로 남깁니다.', layout='three_panel', camera='mall_a_6_18', map='parking', parked=False),
    dict(id='parking', start=28, end=36, title='주차 구획에 들어가 멈춥니다', caption='같은 제작자의 주차장 실사를 역재생·감속해 주차 동작을 연출했습니다.', layout='three_panel', camera='mall_b_7.5_to_5.5_reverse', map='parking', parked=False),
    dict(id='parked', start=36, end=43, title='주차 완료를 확인한 뒤, 다음 안내로', caption='주차 완료를 명시적으로 확인합니다. 카메라는 같은 정차 장면을 유지하고 관측은 멈춥니다.', layout='three_panel', camera='parked_still_b_5.5', map='parking_saved', parked=True),
    dict(id='lift', start=43, end=50, title='이제, 5층으로 이동할 차례입니다', caption='이용할 승강시설을 먼저 확인합니다. 주차층과 승강기 연결은 아직 미확인입니다.', layout='three_panel', camera='parked_thumbnail_b_5.5', map='lift', parked=True),
    dict(id='floor', start=50, end=66, title='5층 엘리베이터에서 에뜨와까지', caption='사용자가 매장 안내를 열면 파란 경로선을 재생합니다. 현재 위치를 추적하는 선은 아닙니다.', layout='three_panel', camera='parked_thumbnail_b_5.5', map='floor_5f', parked=True),
    dict(id='close', start=66, end=72, title='필요할 때 다시 볼 수 있도록', caption='안내를 닫으면 카메라·지도 재생은 끝납니다. 실제 앱 적용 여부는 비교표에서 구분합니다.', layout='three_panel', camera='off', map='summary', parked=True),
]


def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
@lru_cache(maxsize=64)
def font(size): return ImageFont.truetype(FONT, size)
@lru_cache(maxsize=1024)
def text_tile(text, size, color, anchor):
    bounds=ImageDraw.Draw(Image.new('L',(1,1))).textbbox((0,0),text,font=font(size),anchor=anchor)
    a,b,c,d=bounds
    tile=Image.new('RGBA',(max(1,c-a),max(1,d-b)),(0,0,0,0))
    ImageDraw.Draw(tile).text((-a,-b),text,font=font(size),fill=color,anchor=anchor)
    return tile,a,b

def txt(im, xy, text, size=22, color=WHITE, anchor=None):
    tile,a,b=text_tile(text,size,color,anchor)
    im.paste(tile,(round(xy[0]+a),round(xy[1]+b)),tile)
def box(im, xy, fill=PANEL, radius=20, outline=None, width=1):
    ImageDraw.Draw(im).rounded_rectangle(xy, radius, fill=fill, outline=outline, width=width)
def smooth(p): return (max(0., min(1., p)))**2 * (3-2*max(0., min(1., p)))
def stage(t): return next(s for s in STAGES if s['start'] <= t < s['end'])
def cover(img, size): return ImageOps.fit(img, size, Image.Resampling.LANCZOS)

def partial_line(im, points, fraction, color=BLUE, width=6):
    distances=[math.dist(a,b) for a,b in zip(points,points[1:])]
    remaining=sum(distances)*max(0,min(1,fraction)); path=[points[0]]
    for a,b,length in zip(points,points[1:],distances):
        if remaining >= length: path.append(b); remaining-=length
        else:
            f=remaining/length if length else 0; path.append((a[0]+(b[0]-a[0])*f,a[1]+(b[1]-a[1])*f));break
    d=ImageDraw.Draw(im)
    if len(path)>1:
        d.line(path,fill=color,width=width,joint='curve')
        for x,y in path:d.ellipse((x-width/2,y-width/2,x+width/2,y+width/2),fill=color)
    return path[-1]

def tilt(layer, size, angle=-6):
    # Image perspective only for the self-drawn map, never for the camera.
    canvas=Image.new('RGBA',size,(0,0,0,0))
    rw=int(size[0]*.91);rh=int(size[1]*.85)
    z=layer.resize((rw,rh),Image.Resampling.LANCZOS).rotate(angle,Image.Resampling.BICUBIC,expand=True)
    canvas.alpha_composite(z,((size[0]-z.width)//2,(size[1]-z.height)//2))
    return canvas

@lru_cache(maxsize=1)
def reference(): return Image.open(REFERENCE).convert('RGB')
@lru_cache(maxsize=1)
def road_map(): return reference().crop((565,62,1105,733))

@lru_cache(maxsize=1)
def parking_map():
    im=Image.new('RGBA',(920,600),'#1B2A3B');d=ImageDraw.Draw(im)
    d.rounded_rectangle((25,55,895,555),16,fill='#263A50',outline='#526782',width=2)
    for row in [135,320,460]:
        for col in range(11):
            x=60+col*74;d.rectangle((x,row,x+64,row+70),outline='#8BA2BB',width=2)
            if (col+row)%4==0:
                box(im,(x+11,row+10,x+51,row+62),'#788DA3',7)
                d.rectangle((x+17,row+17,x+45,row+30),fill='#B0BDCD')
    for x in [110,350,590,830]:
        for y in [255,420]:d.ellipse((x-7,y-7,x+7,y+7),fill='#E5C879')
    box(im,(630,10,795,84),'#2F5C87',10); txt(im,(712,45),'엘리베이터',23,anchor='mm')
    box(im,(370,10,565,84),'#345D5C',10);txt(im,(467,45),'에스컬레이터',21,anchor='mm')
    # No dot/route/number is drawn: no measured position or matching garage map.
    txt(im,(460,590),'배치 예시 · 층 / 기둥 / 현재 위치 미확인',22,MUTED,'mm')
    return im

@lru_cache(maxsize=1)
def floor_data():
    cat=json.loads((ROOT/'app/src/main/assets/visit-catalog.json').read_text())
    return cat['destinations'][0]['route']['points']

def floor_map(progress):
    im=Image.new('RGBA',(700,615),'#EEF3F7');d=ImageDraw.Draw(im)
    def p(x,y):return ((x+64)/.96-400,(y+88)/.96-420)
    blocks=[('엘리베이터',668,328,36,108,'lift'),('유아휴게실',354,519,119,152,'service'),('유아차 대여',509,519,102,33,'service'),('아디다스\n키즈',509,552,52,70,''),('플레이\n키즈프로',561,552,50,70,''),('뉴발란스\n키즈',611,519,101,103,''),('파타고니아\n키즈',712,519,101,103,''),('노스페이스\n키즈',813,519,101,103,''),('아람북스',500,653,62,65,''),('메론\n스위치',562,653,62,65,''),('더바디샵',624,653,62,65,''),('리틀\n아카이브',686,653,62,65,''),('블루독\n베이비',500,718,62,66,''),('밍크뮤',562,718,62,66,''),('에뜨와',624,718,62,66,'destination'),('압소바',686,718,62,66,''),('에스컬레이터',690,818,33,80,'escalator')]
    for label,x,y,w,h,kind in blocks:
        a,b=p(x,y);c,e=p(x+w,y+h)
        fill={'lift':'#A6DACE','service':'#DAD6E7','destination':'#C5E0FF','escalator':'#A6DACE'}.get(kind,'#DCE2E9')
        d.rectangle((a,b,c,e),fill=fill,outline='#FFFFFF',width=2)
        if kind in ['lift','escalator']:
            txt(im,((a+c)/2,(b+e)/2),'E' if kind=='lift' else '↗',25,'#2B5377','mm')
        else:
            for i,line in enumerate(label.split('\n')):
                txt(im,((a+c)/2,(b+e)/2+(i-(len(label.split('\n'))-1)/2)*18),line,15 if w<70 else 18,'#344A64','mm')
    points=[(p['x']-400,p['y']-420) for p in floor_data()]
    ImageDraw.Draw(im).line(points,fill='#C5D8EC',width=7,joint='curve')
    head=partial_line(im,points,progress,BLUE,8)
    for point in [points[0],points[-1]]:
        x,y=point;d.ellipse((x-7,y-7,x+7,y+7),fill=BLUE,outline='white',width=2)
    x,y=head;d.ellipse((x-8,y-8,x+8,y+8),fill=BLUE,outline='white',width=3)
    return im

class Film:
    def __init__(self):
        self.frames={}
        sources=json.loads((MEDIA/'sources.json').read_text())['selected']
        for source in sources:
            path=MEDIA/source['file']; assert sha(path)==source['sha256']
            raw=subprocess.check_output(['ffmpeg','-v','error','-i',str(path),'-vf',f'scale=480:270,fps={FPS}','-f','rawvideo','-pix_fmt','rgb24','-'])
            self.frames[source['id']]=np.frombuffer(raw,dtype=np.uint8).reshape(-1,270,480,3)
        self.parked=self.camera(5972197,5.5)

    def camera(self,ident,second):
        frames=self.frames[ident]; idx=min(len(frames)-1,max(0,int(second*FPS)))
        return Image.fromarray(frames[idx])

    def draw(self,t):
        s=stage(t);im=Image.new('RGB',(W,H),INK)
        txt(im,(78,20),'도착자리',32);txt(im,(230,31),'목적지 너머, 만날 자리까지',18,MUTED)
        txt(im,(1840,40),'매장 방문  /  UX 시연',18,MUTED,'rm')
        # Preserve approved vehicle layout and bottom controls; values are staged, not SDK signals.
        im.paste(reference().crop((0,0,1672,896)),(124,90))
        d=ImageDraw.Draw(im)
        d.rectangle((142,92,360,174),fill='#10131A');txt(im,(157,100),'차량 정보',18,MUTED)
        d.rectangle((143,136,350,274),fill='#10131A')
        if s['parked']:
            txt(im,(165,142),'0',76);txt(im,(176,226),'km/h',20,MUTED);gear='P'
        else:
            txt(im,(162,153),'주행 중',36);txt(im,(176,224),'시연 상태',17,MUTED);gear='D'
        d.rectangle((396,95,464,151),fill='#10131A');txt(im,(426,124),gear,39,anchor='mm')
        # Road navigation owns both right-hand areas until arrival, then shrinks smoothly.
        opening=smooth((t-8)/2)
        nav_width=round(1085-545*opening)
        nx,ny,nh=690,152,744
        nav=cover(road_map(),(nav_width,nh)); im.paste(nav,(nx,ny))
        box(im,(nx+16,ny+16,nx+nav_width-16,ny+108),'#18283C',18)
        txt(im,(nx+36,ny+31),'현대백화점 판교점',27)
        txt(im,(nx+36,ny+74),'에뜨와  ·  5층 유아동',18,MUTED)
        if t<8:
            pts=[(nx+nav_width*.48,ny+600),(nx+nav_width*.48,ny+455),(nx+nav_width*.58,ny+405),(nx+nav_width*.58,ny+240),(nx+nav_width*.66,ny+200)]
            d.line(pts,fill='#173B69',width=18,joint='curve'); partial_line(im,pts,.4+.6*t/8,BLUE,8)
            txt(im,(nx+30,ny+nh-50),'도로 내비게이션 · UX 연출',18)
        else:
            box(im,(nx+35,ny+270,nx+nav_width-35,ny+444),'#142439',22,outline='#2A435F')
            txt(im,(nx+nav_width/2,ny+311),'목적지에 도착했습니다',26,anchor='mm')
            txt(im,(nx+nav_width/2,ny+355),'도로 안내 종료',20,MUTED,'mm')
            txt(im,(nx+nav_width/2,ny+408),'도착 후 안내는 오른쪽에서',18,TEAL,'mm')
        if t>=8:
            panel=self.panel(t,s)
            # Slide panel into view as navigation relinquishes the third area.
            x=round(1790-550*opening)
            mask=Image.new('L',(W,H),0);ImageDraw.Draw(mask).rectangle((1240,152,1779,896),fill=255)
            layer=Image.new('RGB',(W,H),INK);layer.paste(panel,(x,152));im.paste(layer,(0,0),mask)
        # Narrative captions outside the in-car UI, preserving panel readability.
        txt(im,(124,998),s['title'],28)
        txt(im,(124,1040),s['caption'],17,MUTED)
        # Continuous provenance; UX animation never presented as device or OCR success.
        box(im,(1238,96,1778,137),'#182334',12)
        txt(im,(1508,116),'UX 합성 · 내비/차량 연출 · 실사 대체영상',15,MUTED,'mm')
        # Thin phase timeline beneath the frame.
        labels=[('도로',0,8),('도착',8,10),('주차장',10,28),('주차',28,36),('완료',36,43),('승강',43,50),('5층',50,66),('닫기',66,72)]
        for label,start,end in labels:
            a=124+1672*start/DURATION;b=124+1672*end/DURATION
            d.rectangle((a,980,b-3,984),fill=BLUE if t>=start else '#29364B')
        return im

    def panel(self,t,s):
        im=Image.new('RGB',(540,744),INK)
        txt(im,(24,17),'도착자리',27);txt(im,(500,31),'×',27,MUTED,'mm');txt(im,(24,57),'현대백화점 판교점  ·  에뜨와',19,MUTED)
        moving=t<36; thumb=t>=43
        cam_h=270 if not thumb else 120; cam_y=99
        if t<28:
            camera=self.camera(5972195,max(0,t-10))
        elif t<36:
            camera=self.camera(5972197,7.5-(t-28)*.25)
        else:camera=self.parked
        if s['camera']!='off':
            camera_view = camera.crop((0,0,480,135)) if thumb else camera
            im.paste(cover(camera_view,(492,cam_h)),(24,cam_y))
            box(im,(34,cam_y+10,219,cam_y+41),'#132333',9)
            txt(im,(45,cam_y+15),'실사 주차장 · 편집 연출',14)
            box(im,(198,cam_y+cam_h-36,505,cam_y+cam_h-9),'#132333',8)
            badge='층·기둥 미확인' if moving else '주차 완료 · 정차 화면 연출'
            txt(im,(492,cam_y+cam_h-23),badge,14,TEAL,'rm')
        else:
            box(im,(24,99,516,171),'#172739',16)
            txt(im,(270,135),'카메라 꺼짐  ·  관측 중지',20,MUTED,'mm')
        if s['map'] in ('parking','parking_saved'):
            txt(im,(24,392),'주차장 안내' if moving else '주차 완료 확인',23)
            im.paste(tilt(parking_map(),(516,245)),(12,425),tilt(parking_map(),(516,245)))
            if moving:
                txt(im,(24,682),'층·기둥 표기를 확인해 주세요',20)
                txt(im,(24,715),'주차장 지도는 배치 예시 · 위치 연결 미확인',13,MUTED)
            else:
                box(im,(24,678,516,733),'#1E4F5B',14)
                txt(im,(270,704),'✓  주차 완료 확인  →  매장 안내',20,anchor='mm')
        elif s['map']=='lift':
            txt(im,(24,245),'이용할 승강시설을 확인하세요',23)
            d=ImageDraw.Draw(im); d.line((270,330,270,580),fill='#344963',width=4)
            for y in [350,540]:
                d.ellipse((249,y-21,291,y+21),fill='#233D58',outline=BLUE,width=3)
            txt(im,(70,342),'주차층',23);txt(im,(70,378),'아직 미확인',16,MUTED)
            txt(im,(70,530),'5F',33,BLUE);txt(im,(70,575),'유아동 · 에뜨와',19)
            txt(im,(310,430),'승강기 연결',18,MUTED);txt(im,(310,462),'현장 확인 필요',17,MUTED)
            dot_y=350+190*((t-43)/7)
            d.ellipse((264,dot_y-6,276,dot_y+6),fill=TEAL)
            txt(im,(24,610),'5층 안내는 직접 열어 확인합니다',18)
            box(im,(24,650,516,699),'#1D385B',13)
            txt(im,(270,674),'5층 동선 보기',20,anchor='mm')
            txt(im,(24,716),'호기·주차장 연결은 현장 확인이 필요합니다',14,MUTED)
        elif s['map']=='floor_5f':
            txt(im,(24,245),'5F  엘리베이터 → 에뜨와',23)
            progress=smooth((t-50)/8)
            floor=tilt(floor_map(progress),(516,365))
            im.paste(floor,(12,285),floor)
            box(im,(24,654,516,705),'#1D385B',13)
            txt(im,(270,678),'엘리베이터에서 매장까지, 한눈에',20,anchor='mm')
            txt(im,(24,720),'공식 기반 개략도 · 경로 재생 / 현재 위치 아님',13,MUTED)
        elif s['map']=='summary':
            txt(im,(24,218),'안내를 마쳤습니다',29)
            for y,title,value in [(303,'방문 목적지','판교점 5층 에뜨와'),(412,'주차 단서','층 · 기둥 미확인'),(521,'다시 볼 내용','주차 기록 · 5층 동선')]:
                box(im,(24,y-15,516,y+73),'#19283A',15)
                txt(im,(44,y),title,16,MUTED);txt(im,(44,y+29),value,22)
            txt(im,(24,670),'다시 열기 전에는 자동 재생하지 않습니다',18,TEAL)
            txt(im,(24,713),'저장 동작은 기존 앱 검증 기록과 별도 대조',14,MUTED)
        # Staged touch markers make user-confirmed transitions visible in the UX film.
        for begin,end,point in [(42.2,43,(270,704)),(49.2,50,(270,674)),(65.2,66,(500,31))]:
            if begin <= t < end:
                fraction=(t-begin)/(end-begin);r=12+20*fraction
                ImageDraw.Draw(im).ellipse((point[0]-r,point[1]-r,point[0]+r,point[1]+r),outline=TEAL,width=3)
        return im


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,required=True);p.add_argument('--previews-only',action='store_true');args=p.parse_args()
    out=args.output;out.mkdir(parents=True,exist_ok=True);film=Film()
    moments=[4,9,18,31,39,46,54,61,69]
    for second in moments:film.draw(second).save(out/f'frame-{second:02}.png')
    thumb=Image.new('RGB',(960*3,540*3),INK)
    for idx,second in enumerate(moments):thumb.paste(film.draw(second).resize((960,540),Image.Resampling.LANCZOS),((idx%3)*960,(idx//3)*540))
    thumb.save(out/'storyboard.jpg',quality=88)
    (out/'storyboard.json').write_text(json.dumps({'duration_seconds':DURATION,'fps':FPS,'stages':STAGES,'scope':'UX composited experience film; real stock mall footage, generated native-looking frame and drawn maps; not native Pleos/app execution'},ensure_ascii=False,indent=2)+'\n')
    if args.previews_only:print('Previews ready',flush=True);return
    target=out/'dochagjari-store-arrival-demo.mp4'
    command=['ffmpeg','-v','error','-y','-f','rawvideo','-pix_fmt','rgb24','-s',f'{W}x{H}','-r',str(FPS),'-i','-','-an','-c:v','libx264','-preset','veryfast','-crf','22','-pix_fmt','yuv420p','-movflags','+faststart',str(target)]
    process=subprocess.Popen(command,stdin=subprocess.PIPE)
    try:
        for i in range(DURATION*FPS):
            process.stdin.write(film.draw(i/FPS).tobytes())
            if i%(FPS*8)==0:print(f'rendered {i/FPS:.0f}/{DURATION}s',flush=True)
        process.stdin.close();rc=process.wait();assert rc==0
    except Exception:
        process.kill();raise
    probe=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_format','-show_streams','-of','json',str(target)],text=True));probe['format']['filename']=target.name
    assert abs(float(probe['format']['duration'])-DURATION)<.1
    result={'file':target.name,'bytes':target.stat().st_size,'sha256':sha(target),'duration_seconds':DURATION,'app_code_unchanged':'f0640b00da92409c265cfd5563238e0d972edcaa','apk_changed':False,'native_pleos_recording':False,'real_oem_camera':False,'ocr_on_new_footage':False,'garage_location_matches_pangyo':False,'garage_map_matches_footage':False,'parked_frame_source':{'file':'docs/media/mall-parking/5972197.mp4','time_seconds':5.5,'held_from_demo_second':36,'camera_off_at_demo_second':66},'edits':['real stock garage moving footage','parking approach staged with 7.5→5.5s reverse playback','parked moment uses held frame; collapsed thumbnail crops the upper half, not actual recorded vehicle parking proof','navigation/vehicle state and user touch markers staged','5F route redrawn from same existing catalog, not new app capture'],'inputs':[{'file':str(p.relative_to(ROOT)),'sha256':sha(p)} for p in [REFERENCE,MEDIA/'5972195.mp4',MEDIA/'5972197.mp4',ROOT/'app/src/main/assets/visit-catalog.json']],'ffprobe':probe}
    (out/'metadata.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    def stamp(s):return f'{s//3600:02}:{s//60%60:02}:{s%60:02},000'
    (out/'subtitles.srt').write_text('\n'.join(f"{i+1}\n{stamp(s['start'])} --> {stamp(s['end'])}\n{s['title']}\n{s['caption']}\n" for i,s in enumerate(STAGES)))
    print(json.dumps({k:result[k] for k in ['file','bytes','sha256']},ensure_ascii=False))

if __name__=='__main__':main()
