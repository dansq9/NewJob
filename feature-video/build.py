"""Ascend Google Play feature video (v3) — uses REAL app screenshots.
Whole-phone vertical slide transitions (no ghosting), balanced captions,
proper Google Play badge, music muxed separately.
"""
import math, os, numpy as np
from PIL import Image, ImageDraw, ImageFilter
import imageio_ffmpeg
from ascend import font, text, tw, logo_mark, _rgb, INDIGO, INDIGO2, VIOLET
import screens

W,H,FPS = 1920,1080,30
HERE=os.path.dirname(os.path.abspath(__file__))

def clamp(x,a=0.0,b=1.0): return max(a,min(b,x))
def smooth(x): x=clamp(x); return x*x*(3-2*x)
def out_cubic(x): x=clamp(x); return 1-(1-x)**3
def out_back(x):
    x=clamp(x); c=1.70158
    return 1+(c+1)*(x-1)**3+c*(x-1)**2

# ---------- background ----------
def make_bg():
    base=screens.vgrad((0,0,W,H),"#6d5cf0","#191641",145).convert("RGBA")
    ov=Image.new("RGBA",(W,H),(0,0,0,0)); d=ImageDraw.Draw(ov)
    for cx,cy,r,a,col in [(250,170,540,46,VIOLET),(1720,930,640,42,INDIGO2),
                          (1500,120,360,28,"#a89bff"),(120,1010,440,24,"#8b5cf6")]:
        d.ellipse([cx-r,cy-r,cx+r,cy+r],fill=_rgb(col)+(a,))
    base=Image.alpha_composite(base,ov.filter(ImageFilter.GaussianBlur(130)))
    vig=Image.new("L",(W,H),0); ImageDraw.Draw(vig).ellipse([-260,-180,W+260,H+180],fill=255)
    vig=vig.filter(ImageFilter.GaussianBlur(230))
    base.paste(Image.new("RGBA",(W,H),(8,6,28,140)),(0,0),Image.eval(vig,lambda v:255-v))
    return base.convert("RGB")
BG=make_bg()

# ---------- real phone images ----------
PH=980                                   # phone display height
NAMES=["home","search","tracker","optimizer","mock","copilot"]
def load_phone(n):
    im=Image.open(os.path.join(HERE,"real",f"{n}.png")).convert("RGB")
    w=round(im.size[0]*PH/im.size[1])
    return im.resize((w,PH),Image.LANCZOS)
PHI={n:load_phone(n) for n in NAMES}
PW=PHI["home"].size[0]
RAD=round(54*PW/412)                      # device corner radius at this scale
CORNER=Image.new("L",(PW,PH),0)
ImageDraw.Draw(CORNER).rounded_rectangle([0,0,PW-1,PH-1],radius=RAD,fill=255)
SHADOW=Image.new("RGBA",(PW+200,PH+200),(0,0,0,0))
ImageDraw.Draw(SHADOW).rounded_rectangle([100,118,PW+100,PH+92],radius=RAD,fill=(8,6,26,165))
SHADOW=SHADOW.filter(ImageFilter.GaussianBlur(54))

# ---------- logo tiles ----------
def logo_tile(size,r):
    t=Image.new("RGBA",(size,size),(0,0,0,0))
    g=screens.vgrad((0,0,size,size),INDIGO,VIOLET,150).convert("RGBA")
    m=Image.new("L",(size,size),0); ImageDraw.Draw(m).rounded_rectangle([0,0,size-1,size-1],radius=r,fill=255)
    t.paste(g,(0,0),m); logo_mark(ImageDraw.Draw(t),size/2,size/2,size*0.56,"#ffffff")
    return t
LOGO_BIG=logo_tile(150,42); LOGO_SM=logo_tile(64,18)

# ---------- timeline ----------
PHONE=[
 ("THE APP",["Everything you need","to land the job"],"Your whole job search in one app","home",INDIGO2),
 ("FIND JOBS",["Find your","next role"],"AI-matched jobs, ranked by how you fit","search","#34d17f"),
 ("TRACKER",["Track every","application"],"From saved to offer, on one board","tracker","#8b9bff"),
 ("RESUME",["Beat the ATS"],"AI tailors your resume to each role","optimizer","#ffd66b"),
 ("PREP",["Practice,","then perform"],"Mock interviews with instant scoring","mock","#ff8fb1"),
 ("LIVE COPILOT",["Your live","interview copilot"],"Real-time answers during the call","copilot","#b3a6ff"),
]
INTRO,OUT,SCENE,TR=3.6,4.2,3.7,0.55
T0=INTRO; TOTAL=INTRO+len(PHONE)*SCENE+OUT
CXT=132
PCX=W-PW//2-235          # phone centre x
PCY=H//2

def _ltext(d,x,y,s,f,col,a):
    if a<=0.01:return
    col=col if isinstance(col,tuple) else _rgb(col)
    d.text((x,y),s,font=f,fill=col+(int(255*clamp(a)),),anchor="lm")
def _ctext(d,cx,cy,s,f,col,a):
    if a<=0.01:return
    col=col if isinstance(col,tuple) else _rgb(col)
    d.text((cx,cy),s,font=f,fill=col+(int(255*clamp(a)),),anchor="mm")
def _pill(d,x,y,label,acc,a):
    f=font(21,800); pad=20; h=40; w=tw(d,label,f)+pad*2
    d.rounded_rectangle([x,y,x+w,y+h],radius=h//2,fill=_rgb(acc)+(int(235*a),))
    dark=acc in ("#ffd66b","#34d17f","#ff8fb1","#b3a6ff","#8b9bff")
    d.text((x+pad,y+h/2),label,font=f,fill=((22,18,42) if dark else (255,255,255))+(int(255*a),),anchor="lm")

def google_play_badge(scale=1.0):
    bw,bh=int(486*scale),int(132*scale)
    b=Image.new("RGBA",(bw,bh),(0,0,0,0)); d=ImageDraw.Draw(b)
    d.rounded_rectangle([0,0,bw-1,bh-1],radius=int(24*scale),fill=(0,0,0,255),outline=(150,150,165,255),width=max(1,int(1.5*scale)))
    s=4; th=int(58*scale); tw_=int(50*scale); tx,ty=int(40*scale),(bh-th)//2
    tri=Image.new("RGBA",(tw_*s,th*s),(0,0,0,0)); td=ImageDraw.Draw(tri)
    Wt,Ht=tw_*s,th*s; A=(0,0); Bp=(0,Ht); P=(Wt,Ht//2); C=(int(Wt*0.30),Ht//2)
    td.polygon([A,P,C],fill=(0,224,255,255)); td.polygon([Bp,P,C],fill=(255,61,84,255))
    td.polygon([A,Bp,C],fill=(0,224,150,255))
    mid=((C[0]+P[0])//2,Ht//2)
    td.polygon([mid,(P[0],Ht//2-int(Ht*0.16)),(P[0],Ht//2+int(Ht*0.16))],fill=(255,200,60,255))
    b.alpha_composite(tri.resize((tw_,th),Image.LANCZOS),(tx,ty))
    txx=tx+tw_+int(26*scale); ef=font(int(20*scale),600); ex=txx
    for ch in "GET IT ON":
        d.text((ex,int(40*scale)),ch,font=ef,fill=(196,196,206),anchor="lm"); ex+=tw(d,ch,ef)+int(3*scale)
    d.text((txx-2,int(86*scale)),"Google Play",font=font(int(40*scale),700),fill=(255,255,255),anchor="lm")
    return b
BADGE=google_play_badge(1.0)

def put_phone(img,name,dy):
    """paste real phone (rounded) centred at (PCX, PCY+dy)."""
    x=int(PCX-PW//2); y=int(PCY-PH//2+dy)
    img.paste(SHADOW,(x-100,y-100),SHADOW)
    img.paste(PHI[name],(x,y),CORNER)

def _dots(d,x,y,idx):
    for i in range(len(PHONE)):
        on=i==idx; w=34 if on else 12
        d.rounded_rectangle([x,y,x+w,y+10],radius=5,fill=(255,255,255,235) if on else (255,255,255,70)); x+=w+10

SLIDE=PH+120
def frame(t):
    img=BG.copy(); d=ImageDraw.Draw(img,"RGBA")
    if t<INTRO:
        p=t/INTRO; sc=out_back(clamp(p/0.5)); ls=int(150*sc)
        if ls>4:
            lg=LOGO_BIG.resize((ls,ls),Image.LANCZOS); img.paste(lg,(W//2-ls//2,H//2-230-ls//2),lg)
        fade=1-smooth((p-0.82)/0.18)
        _ctext(d,W//2,H//2-70,"Ascend",font(96,800),"#ffffff",smooth((p-0.18)/0.25)*fade)
        _ctext(d,W//2,H//2+12,"Land your next role, faster",font(38,500),(236,234,255),smooth((p-0.34)/0.25)*fade)
        _ctext(d,W//2,H//2+122,"YOUR AI JOB-SEARCH COPILOT",font(25,700,mono=True),(176,164,255),smooth((p-0.55)/0.3)*fade)
        return np.asarray(img)
    if t>TOTAL-OUT:
        p=(t-(TOTAL-OUT))/OUT; ap=out_cubic(clamp(p/0.4))
        img.paste(LOGO_BIG,(W//2-75,int(H//2-250-(1-ap)*40)),LOGO_BIG)
        _ctext(d,W//2,H//2-58,"Ascend",font(96,800),"#ffffff",ap)
        _ctext(d,W//2,H//2+20,"Land your next role, faster",font(38,500),(236,234,255),ap)
        b=out_back(clamp((p-0.4)/0.5))
        if b>0.02:
            bw=int(BADGE.size[0]*b); bh=int(BADGE.size[1]*b)
            img.paste(BADGE.resize((max(1,bw),max(1,bh)),Image.LANCZOS),(W//2-bw//2,int(H//2+150-bh//2)),BADGE.resize((max(1,bw),max(1,bh)),Image.LANCZOS))
        return np.asarray(img)

    lt=t-T0; idx=min(len(PHONE)-1,int(lt//SCENE)); local=lt-idx*SCENE
    img.paste(LOGO_SM,(120,84),LOGO_SM)
    text(d,(200,102),"Ascend",font(34,800),"#ffffff",anchor="lm")
    text(d,(200,138),"Land your next role, faster",font(17,500),(212,210,236),anchor="lm")

    float_y=math.sin(lt*1.05)*9
    if idx==0:
        enter=out_cubic(clamp(lt/0.7))
        put_phone(img,PHONE[0][3],(1-enter)*SLIDE+float_y)
    elif local<TR:
        e=smooth(local/TR)
        put_phone(img,PHONE[idx-1][3],-e*SLIDE+float_y)     # exits upward
        put_phone(img,PHONE[idx][3],(1-e)*SLIDE+float_y)    # enters from below
    else:
        put_phone(img,PHONE[idx][3],float_y)

    cin=smooth(clamp((local-(TR if idx>0 else 0.15))/0.4))
    cout=1.0 if idx==len(PHONE)-1 else 1-smooth(clamp((local-(SCENE-0.3))/0.3))
    ca=cin*cout; dx=(1-cin)*55
    eb,lines,sub,_,acc=PHONE[idx]
    block_h=40+24+len(lines)*86+14+34; top=int(H*0.52-block_h/2)
    if ca>0.02:
        _pill(d,CXT-dx,top,eb,acc,ca)
        yy=top+40+24+30
        for ln in lines: _ltext(d,CXT-dx,yy,ln,font(74,800),"#ffffff",ca); yy+=86
        yy+=2; _ltext(d,CXT-dx,yy,sub,font(30,500),(224,222,246),ca)
    _dots(d,132,H-112,idx)
    return np.asarray(img)

def main():
    out="out/ascend_silent.mp4"; n=int(TOTAL*FPS)
    wr=imageio_ffmpeg.write_frames(out,(W,H),fps=FPS,codec="libx264",quality=7,
        macro_block_size=8,output_params=["-pix_fmt","yuv420p","-movflags","+faststart","-profile:v","high"])
    wr.send(None)
    for i in range(n):
        wr.send(np.ascontiguousarray(frame(i/FPS)))
        if i%90==0:print(f"{i}/{n}",flush=True)
    wr.close(); print("DONE",out,f"{TOTAL:.1f}s {n}f")

if __name__=="__main__": main()
