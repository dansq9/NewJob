"""Ascend Google Play promo — premium animated motion graphics (no screenshots).
Bloom/glow, depth particles, glassy cards, spring + motion blur, kinetic type,
light-sweep transitions, cinematic grade. All drawn per-frame with Pillow.
"""
import math, os, random, numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageChops
import imageio_ffmpeg
from ascend import font, icon, tw, logo_mark, _rgb, INDIGO, INDIGO2, VIOLET, VIOLET2
import screens

W,H,FPS = 1920,1080,30
HERE=os.path.dirname(os.path.abspath(__file__))

def clamp(x,a=0.0,b=1.0): return max(a,min(b,x))
def smooth(x): x=clamp(x); return x*x*(3-2*x)
def lerp(a,b,t): return a+(b-a)*t
def out_cubic(x): x=clamp(x); return 1-(1-x)**3
def out_quint(x): x=clamp(x); return 1-(1-x)**5
def out_back(x,c=1.7): x=clamp(x); return 1+(c+1)*(x-1)**3+c*(x-1)**2
def in_out(x): return smooth(x)
def stagger(p,i,n,ov=0.55):
    span=1.0/(n*(1-ov)+ov); start=i*span*(1-ov); return out_cubic((p-start)/span)

INK=(21,21,28); MUTE=(122,122,135); MUT2=(150,150,166); LINE=(236,236,242)
GREEN=(15,157,104); GREENBG=(231,247,239)

# ---------- assets ----------
def _radial(size=256):
    m=Image.new("L",(size,size),0); d=ImageDraw.Draw(m)
    for i in range(size//2,0,-1):
        a=int(255*(i/(size/2))**0.0)  # placeholder
    # smooth radial falloff
    arr=np.zeros((size,size),np.float32); c=size/2
    yy,xx=np.mgrid[0:size,0:size]; r=np.sqrt((xx-c)**2+(yy-c)**2)/c
    arr=np.clip(1-r,0,1)**2.2
    return Image.fromarray((arr*255).astype(np.uint8),"L")
RADIAL=_radial(256)

def halo(img,cx,cy,r,color,a=1.0):
    s=max(2,int(r*2)); spr=RADIAL.resize((s,s),Image.LANCZOS)
    tile=Image.new("RGBA",(s,s),(0,0,0,0))
    col=_rgb(color) if not isinstance(color,tuple) else color
    solid=Image.new("RGBA",(s,s),col+(0,));
    alpha=spr.point(lambda v:int(v*clamp(a)))
    solid.putalpha(alpha); img.paste(solid,(int(cx-r),int(cy-r)),solid)

def logo_tile(size,r):
    t=Image.new("RGBA",(size,size),(0,0,0,0))
    g=screens.vgrad((0,0,size,size),INDIGO,VIOLET,150).convert("RGBA")
    m=Image.new("L",(size,size),0); ImageDraw.Draw(m).rounded_rectangle([0,0,size-1,size-1],radius=r,fill=255)
    t.paste(g,(0,0),m); logo_mark(ImageDraw.Draw(t),size/2,size/2,size*0.56,"#ffffff")
    return t
LOGO_BIG=logo_tile(150,42); LOGO_SM=logo_tile(64,18); LOGO_NOTIF=logo_tile(56,16)

def google_play_badge():
    bw,bh=486,132; b=Image.new("RGBA",(bw,bh),(0,0,0,0)); d=ImageDraw.Draw(b)
    d.rounded_rectangle([0,0,bw-1,bh-1],radius=24,fill=(0,0,0,255),outline=(150,150,165,255),width=2)
    s=4; th=58; tw_=50; tx,ty=40,(bh-th)//2
    tri=Image.new("RGBA",(tw_*s,th*s),(0,0,0,0)); td=ImageDraw.Draw(tri)
    Wt,Ht=tw_*s,th*s; A=(0,0); Bp=(0,Ht); P=(Wt,Ht//2); C=(int(Wt*0.30),Ht//2)
    td.polygon([A,P,C],fill=(0,224,255,255)); td.polygon([Bp,P,C],fill=(255,61,84,255))
    td.polygon([A,Bp,C],fill=(0,224,150,255)); mid=((C[0]+P[0])//2,Ht//2)
    td.polygon([mid,(P[0],Ht//2-int(Ht*0.16)),(P[0],Ht//2+int(Ht*0.16))],fill=(255,200,60,255))
    b.alpha_composite(tri.resize((tw_,th),Image.LANCZOS),(tx,ty))
    txx=tx+tw_+26; ef=font(20,600); ex=txx
    for ch in "GET IT ON":
        d.text((ex,40),ch,font=ef,fill=(196,196,206),anchor="lm"); ex+=tw(d,ch,ef)+3
    d.text((txx-2,86),"Google Play",font=font(40,700),fill=(255,255,255),anchor="lm")
    return b
BADGE=google_play_badge()

# ---------- background + particles + grade ----------
_BG=screens.vgrad((0,0,W,H),"#7468f5","#161334",148).convert("RGB")
def _bg_blobs(t):
    img=_BG.copy(); ov=Image.new("RGBA",(W,H),(0,0,0,0)); d=ImageDraw.Draw(ov)
    for cx,cy,r,a,col,sp in [(250,170,560,60,VIOLET,0.5),(1720,930,660,52,INDIGO2,0.4),
                             (1500,110,380,40,"#a89bff",0.7),(120,1010,460,32,"#8b5cf6",0.5)]:
        dx=math.sin(t*sp)*46; dy=math.cos(t*sp*0.8)*34
        d.ellipse([cx+dx-r,cy+dy-r,cx+dx+r,cy+dy+r],fill=_rgb(col)+(a,))
    img=Image.alpha_composite(img.convert("RGBA"),ov.filter(ImageFilter.GaussianBlur(150)))
    return img.convert("RGB")
_BGC={}
def bg_base(t):
    k=round(t*4)
    if k not in _BGC:
        if len(_BGC)>50:_BGC.clear()
        _BGC[k]=_bg_blobs(k/4)
    return _BGC[k]

_rng=random.Random(11)
PARTICLES=[]
for _ in range(46):
    PARTICLES.append(dict(x=_rng.uniform(0,W),y=_rng.uniform(0,H),
        r=_rng.uniform(3,16),z=_rng.uniform(0.15,1.0),
        col=_rng.choice([(200,194,255),(170,210,255),(255,255,255),(180,160,255)]),
        ph=_rng.uniform(0,6.28)))
def draw_particles(img,t,front=False):
    d=ImageDraw.Draw(img,"RGBA")
    for p in PARTICLES:
        if (p["z"]>0.6)!=front: continue
        x=(p["x"]+t*18*p["z"])%(W+80)-40
        y=p["y"]+math.sin(t*0.5+p["ph"])*22*p["z"]
        a=int(46*p["z"]+18); r=p["r"]*(1.4 if front else 1.0)
        halo(img,x,y,r*2.4,p["col"],a/255*0.5)
        d.ellipse([x-r/2,y-r/2,x+r/2,y+r/2],fill=p["col"]+(a,))

# vignette
_yy,_xx=np.mgrid[0:H,0:W]
_d=np.sqrt(((_xx-W/2)/(W/2))**2+((_yy-H/2)/(H/2))**2)
VIGN=np.clip(1-(_d-0.55)*0.5,0.62,1.0).astype(np.float32)[...,None]

_GRAIN=[ (np.random.default_rng(s).standard_normal((H,W,1)).astype(np.float32))*3.0 for s in range(12) ]
_gi=[0]
def grade(img):
    a=np.asarray(img).astype(np.float32)
    # bloom: bright-pass -> blur -> add (all at reduced res for speed)
    lum=a@np.array([0.299,0.587,0.114],np.float32)
    m=np.clip((lum-158)/97,0,1)[...,None]
    src=Image.fromarray((a*m).astype(np.uint8))
    b1=np.asarray(src.resize((W//2,H//2)).filter(ImageFilter.GaussianBlur(6)).resize((W,H))).astype(np.float32)
    b2=np.asarray(src.resize((W//4,H//4)).filter(ImageFilter.GaussianBlur(12)).resize((W,H))).astype(np.float32)
    a=a+b1*0.30+b2*0.50
    a=a*VIGN
    _gi[0]=(_gi[0]+1)%len(_GRAIN); a=a+_GRAIN[_gi[0]]   # cheap grain (precomputed)
    return np.clip(a,0,255).astype(np.uint8)

# ---------- right-side stage panel (frosted glass container) ----------
VX=1360
PANEL=(VX-410,148,VX+410,936); PR=40
_PANELC={}
def _frosted(t):
    k=round(t*4)
    if k not in _PANELC:
        if len(_PANELC)>50:_PANELC.clear()
        x0,y0,x1,y1=PANEL
        crop=bg_base(k/4).crop((x0,y0,x1,y1)).filter(ImageFilter.GaussianBlur(30))
        crop=Image.blend(crop,Image.new("RGB",crop.size,(255,255,255)),0.10)
        _PANELC[k]=crop
    return _PANELC[k]
def draw_stage_panel(img,a,t):
    if a<=0.02:return
    x0,y0,x1,y1=PANEL; w=x1-x0; h=y1-y0
    soft_shadow(img,x0,y0,w,h,PR,blur=44,col=(3,2,16,int(150*a)))
    rmask=Image.new("L",(w,h),0); ImageDraw.Draw(rmask).rounded_rectangle([0,0,w-1,h-1],radius=PR,fill=255)
    if a<1: rmask=rmask.point(lambda v:int(v*a))
    img.paste(_frosted(t),(x0,y0),rmask)
    d=ImageDraw.Draw(img,"RGBA")
    d.rounded_rectangle([x0,y0,x1,y1],radius=PR,outline=(255,255,255,int(46*a)),width=2)
    d.line([x0+PR,y0+2,x1-PR,y0+2],fill=(255,255,255,int(95*a)),width=2)  # top sheen

# ---------- alpha-aware draw helpers ----------
def Acol(c,a):
    c=c if isinstance(c,tuple) else _rgb(c); return (c[0],c[1],c[2],int(255*clamp(a)))
def txt(d,x,y,s,f,col,a=1.0,anchor="lm"):
    if a<=0.01:return
    d.text((x,y),s,font=f,fill=Acol(col,a),anchor=anchor)
def soft_shadow(img,x,y,w,h,r,blur=30,col=(4,3,18,170)):
    sh=Image.new("RGBA",(int(w+blur*4),int(h+blur*4)),(0,0,0,0))
    ImageDraw.Draw(sh).rounded_rectangle([blur*2,blur*2+12,blur*2+w,blur*2+h+12],radius=r,fill=col)
    sh=sh.filter(ImageFilter.GaussianBlur(blur)); img.paste(sh,(int(x-blur*2),int(y-blur*2)),sh)
def glass_card(img,x,y,w,h,a=1.0,r=22,fill=(255,255,255),outline=None,shadow=True):
    x,y,w,h=int(x),int(y),int(w),int(h)
    if shadow and a>0.5: soft_shadow(img,x,y,w,h,r,blur=28,col=(4,3,18,int(150*a)))
    d=ImageDraw.Draw(img,"RGBA")
    d.rounded_rectangle([x,y,x+w,y+h],radius=r,fill=Acol(fill,a),
        outline=Acol(outline,a) if outline else None,width=2 if outline else 0)
    # top highlight sheen
    d.line([x+r,y+1,x+w-r,y+1],fill=(255,255,255,int(150*a)),width=2)
def avatar(d,x,y,s,col,ini,a=1.0):
    d.rounded_rectangle([x,y,x+s,y+s],radius=int(s*0.28),fill=Acol(col,a))
    txt(d,x+s/2,y+s/2,ini,font(int(s*0.42),800),(255,255,255),a,"mm")

# ---------- kinetic caption (wipe reveal) ----------
CX=132
def kinetic_line(img,x,y,s,f,col,reveal,drift=0):
    if reveal<=0.01:return
    w=int(tw(ImageDraw.Draw(img),s,f))+8; asc=f.size+12
    lay=Image.new("RGBA",(w,asc),(0,0,0,0)); ImageDraw.Draw(lay).text((0,asc/2),s,font=f,fill=col+(255,),anchor="lm")
    rv=out_quint(reveal); mw=int(w*rv)
    mask=Image.new("L",(w,asc),0); ImageDraw.Draw(mask).rectangle([0,0,mw,asc],fill=255)
    if mw<w: mask=mask.filter(ImageFilter.GaussianBlur(6))
    lay.putalpha(ImageChops.multiply(lay.split()[3],mask))
    img.paste(lay,(int(x),int(y-asc/2+(1-rv)*drift)),lay)
def caption(img,eyebrow,lines,sub,accent,p,local,dur):
    cin=clamp(local/0.5); cout=1-smooth(clamp((local-(dur-0.35))/0.35))
    if cin*cout<=0.02:return
    d=ImageDraw.Draw(img,"RGBA")
    block=44+26+len(lines)*88+16+34; top=int(H*0.5-block/2)
    eb_a=cin*cout; dx=(1-out_cubic(cin))*40
    f=font(21,800); pad=20; w=tw(d,eyebrow,f)+pad*2
    dark=accent in ("#ffd66b","#34d17f","#ff8fb1","#b3a6ff","#8b9bff","#ffb02e")
    halo(img,CX-dx+w/2,top+20,90,accent,0.5*eb_a)
    d.rounded_rectangle([CX-dx,top,CX-dx+w,top+42],radius=21,fill=Acol(accent,0.96*eb_a))
    txt(d,CX-dx+pad,top+21,eyebrow,f,(22,18,42) if dark else (255,255,255),eb_a)
    yy=top+44+26+34
    for i,ln in enumerate(lines):
        kinetic_line(img,CX,yy,ln,font(76,800),(255,255,255),clamp((cin-i*0.12)/0.6)*cout,drift=30); yy+=88
    txt(d,CX,yy+4,sub,font(30,500),(224,222,246),clamp((cin-0.25)/0.5)*cout)

def static_caption(img,eyebrow,lines,sub,accent,a):
    if a<=0.02:return
    d=ImageDraw.Draw(img,"RGBA")
    block=44+26+len(lines)*88+16+34; top=int(H*0.5-block/2)
    f=font(21,800); pad=20; w=tw(d,eyebrow,f)+pad*2
    dark=accent in ("#ffd66b","#34d17f","#ff8fb1","#b3a6ff","#8b9bff","#ffb02e")
    halo(img,CX+w/2,top+20,90,accent,0.5*a)
    d.rounded_rectangle([CX,top,CX+w,top+42],radius=21,fill=Acol(accent,0.96*a))
    txt(d,CX+pad,top+21,eyebrow,f,(22,18,42) if dark else (255,255,255),a)
    yy=top+44+26+34
    for ln in lines: txt(d,CX,yy,ln,font(76,800),(255,255,255),a); yy+=88
    txt(d,CX,yy+4,sub,font(30,500),(224,222,246),a)

# ============ visuals ============
VX=1360
def vis_search(img,p,t):
    d=ImageDraw.Draw(img,"RGBA")
    bx,bw=1010,700; by=205
    glass_card(img,bx,by,bw,72,1,18)
    d=ImageDraw.Draw(img,"RGBA")
    icon(d,(bx+34,by+36),"search",30,INDIGO,anchor="mm")
    q="Product Manager"; tp=clamp(p/0.26); n=int(len(q)*tp)
    txt(d,bx+66,by+36,q[:n],font(26,600),INK)
    if tp<1 and int(t*2.5)%2==0:
        cw=bx+66+tw(d,q[:n],font(26,600)); d.rectangle([cw+2,by+22,cw+5,by+50],fill=Acol(INDIGO,1))
    jobs=[("Senior Product Manager","Northwind · Remote","N",INDIGO,96),
          ("Product Manager, Growth","Tempo · Hybrid","T",GREEN,91),
          ("Group Product Manager","Ledgerline · NY","L",(224,145,63),84)]
    cy=by+110
    for i,(title,co,ini,col,m) in enumerate(jobs):
        rp=stagger(clamp((p-0.28)/0.72),i,len(jobs))
        if rp<=0.01: continue
        off=(1-out_back(rp))*90; a=clamp(rp*1.4)
        x=bx; w=bw; h=112; ybase=cy+i*130
        # motion-blur ghosts while moving
        if off>4 and a>0.4:
            for g in (2.0,1.0):
                glass_card(img,x,ybase+off+g*22,w,h,a*0.10,18,shadow=False)
        glass_card(img,x,ybase+off,w,h,a,18)
        dd=ImageDraw.Draw(img,"RGBA")
        avatar(dd,x+18,ybase+off+22,46,col,ini,a)
        txt(dd,x+82,ybase+off+36,title,font(25,700),INK,a)
        txt(dd,x+82,ybase+off+68,co,font(20,500),MUTE,a)
        mv=int(m*out_cubic(clamp((rp-0.2)/0.8)))
        b2=tw(dd,f"{mv}%",font(20,800,mono=True))+24; bx2=x+w-b2-20; by2=ybase+off+38
        halo(img,bx2+b2/2,by2+17,46,GREEN,0.4*a)
        dd=ImageDraw.Draw(img,"RGBA")
        dd.rounded_rectangle([bx2,by2,bx2+b2,by2+34],radius=12,fill=Acol(GREENBG,a))
        txt(dd,bx2+b2/2,by2+17,f"{mv}%",font(20,800,mono=True),GREEN,a,"mm")

def vis_tracker(img,p,t):
    d=ImageDraw.Draw(img,"RGBA")
    cols=[("SAVED",(107,107,120)),("APPLIED",(37,99,235)),("INTERVIEW",(124,92,255)),("OFFER",(15,157,104))]
    n=len(cols); gap=26; cw=152; total=n*cw+(n-1)*gap; x0=VX-total/2; y0=296; colh=430
    for i,(lab,col) in enumerate(cols):
        x=x0+i*(cw+gap)
        d.rounded_rectangle([x,y0,x+cw,y0+colh],radius=20,fill=(255,255,255,30))
        d.rounded_rectangle([x,y0,x+cw,y0+46],radius=15,fill=Acol(col,0.92))
        txt(d,x+cw/2,y0+23,lab,font(15,800),(255,255,255),1,"mm")
    seg=clamp(p/0.9)*3; ci=min(3,int(seg)); fr=out_quint(seg-ci)
    def colx(i): return x0+i*(cw+gap)+cw/2
    cardx=lerp(colx(ci),colx(min(3,ci+1)),fr); cy=y0+92; ch=98; cwd=cw-18
    for i in range(ci+1):
        gx=colx(i); d.rounded_rectangle([gx-cwd/2,cy,gx+cwd/2,cy+ch],radius=16,fill=(255,255,255,55))
    # motion blur trail
    if fr>0.05 and fr<0.95:
        for g in (1,2,3):
            gx=lerp(cardx,colx(ci),0.06*g)
            glass_card(img,gx-cwd/2,cy,cwd,ch,0.10,16,shadow=False)
    x=cardx-cwd/2; glass_card(img,x,cy,cwd,ch,1,16)
    dd=ImageDraw.Draw(img,"RGBA"); avatar(dd,x+14,cy+15,40,INDIGO,"N",1)
    txt(dd,x+14,cy+74,"Senior PM",font(18,700),INK,1)
    if ci>=3 and fr>0.55:
        a=clamp((fr-0.55)/0.45); halo(img,colx(3),y0+colh-46,70,GREEN,0.6*a)
        icon(ImageDraw.Draw(img,"RGBA"),(colx(3),y0+colh-46),"verified",50,GREEN,fill=1,anchor="mm")

def vis_ats(img,p,t):
    cx,cy,r=VX,420,152
    val=int(93*out_quint(clamp(p/0.7)))
    halo(img,cx,cy,r*1.7,"#0f9d68",0.35*clamp(p/0.6))
    sc=4; big=Image.new("RGBA",((r+24)*2*sc,)*2,(0,0,0,0)); bd=ImageDraw.Draw(big)
    o=(r+24)*sc; bb=[o-r*sc,o-r*sc,o+r*sc,o+r*sc]
    bd.arc(bb,0,360,fill=(255,255,255,46),width=22*sc)
    if val>0: bd.arc(bb,-90,-90+360*val/100,fill=_rgb("#19c97f")+(255,),width=22*sc)
    big=big.resize(((r+24)*2,)*2,Image.LANCZOS); img.paste(big,(int(cx-(r+24)),int(cy-(r+24))),big)
    d=ImageDraw.Draw(img,"RGBA")
    txt(d,cx,cy-12,str(val),font(82,800,mono=True),(255,255,255),1,"mm")
    txt(d,cx,cy+50,"ATS SCORE",font(22,700),(210,210,235),1,"mm")
    checks=["Keywords matched","Impact quantified","ATS-safe format","Tailored to the role"]
    yy=cy+r+64; x=VX-300
    for i,c in enumerate(checks):
        rp=stagger(clamp((p-0.38)/0.62),i,len(checks))
        if rp<=0.02: continue
        a=clamp(rp*1.4); sx=x+(1-out_back(rp))*-30
        icon(d,(sx+18,yy+18),"check_circle",30,(40,220,150),fill=1,anchor="mm")
        txt(d,sx+50,yy+18,c,font(26,600),(255,255,255),a); yy+=56

def vis_mock(img,p,t):
    x=VX-330; w=660; qy=240; qh=152
    glass_card(img,x,qy,w,qh,1,18); d=ImageDraw.Draw(img,"RGBA")
    txt(d,x+24,qy+34,"QUESTION 3 OF 5",font(18,700,mono=True),INDIGO,1)
    q="Design a system for 40k requests per second."; tp=clamp(p/0.4); n=int(len(q)*tp)
    words=q[:n].split(" "); txt(d,x+24,qy+80,(" ".join(words[:5])),font(30,800),INK,1)
    txt(d,x+24,qy+118,(" ".join(words[5:])),font(30,800),INK,1)
    areas=[("Clarity",88,INDIGO),("Structure",81,VIOLET2),("Impact",92,GREEN)]
    by=qy+qh+54
    for i,(lab,scv,col) in enumerate(areas):
        rp=clamp((p-0.45-i*0.12)/0.4); txt(d,x,by+i*66+18,lab,font(24,600),(255,255,255),1)
        bx=x+180; bw=w-300
        d.rounded_rectangle([bx,by+i*66+8,bx+bw,by+i*66+30],radius=11,fill=(255,255,255,45))
        fw=bw*(scv/100)*out_quint(rp)
        if fw>3:
            halo(img,bx+fw,by+i*66+19,30,col,0.5*rp); d=ImageDraw.Draw(img,"RGBA")
            d.rounded_rectangle([bx,by+i*66+8,bx+fw,by+i*66+30],radius=11,fill=Acol(col,1))
        txt(d,x+w,by+i*66+19,f"{int(scv*out_quint(rp))}",font(24,800,mono=True),col,1,"rm")

def waveform(img,cx,cy,t,a=1.0):
    d=ImageDraw.Draw(img,"RGBA")
    for i in range(-7,8):
        h=18+abs(math.sin(t*4+i*0.6))*58; x=cx+i*18; col=INDIGO if i%2==0 else VIOLET2
        halo(img,x,cy,h*0.5,col,0.10*a)
        ImageDraw.Draw(img,"RGBA").rounded_rectangle([x-5,cy-h/2,x+5,cy+h/2],radius=4,fill=Acol(col,a))

def vis_copilot(img,p,t):
    x=VX-340; w=680
    if p<0.92: waveform(img,VX,236,t,clamp(1-(p-0.72)/0.2))
    d=ImageDraw.Draw(img,"RGBA")
    qp=out_back(clamp((p-0.12)/0.25))
    if qp>0.02:
        qy=300+(1-qp)*30; glass_card(img,x,qy,w,72,clamp(qp),16,fill=(26,24,56),outline=(64,58,120),shadow=False)
        txt(ImageDraw.Draw(img,"RGBA"),x+22,qy+36,"Walk me through a tradeoff you made.",font(24,600),(232,232,246),clamp(qp))
    ap=clamp((p-0.34)/0.62)
    if ap>0.02:
        ay=400; ah=300; halo(img,x+w/2,ay+ah/2,260,VIOLET2,0.18*ap)
        glass_card(img,x,ay,w,ah,clamp(ap*1.3),20,fill=(28,24,58),outline=(64,58,120))
        d=ImageDraw.Draw(img,"RGBA")
        icon(d,(x+26,ay+34),"bolt",26,(168,155,255),anchor="mm")
        txt(d,x+48,ay+34,"COPILOT ANSWER",font(20,800),(168,155,255),clamp(ap*1.3))
        secs=[("CONTEXT","Owned the sharding layer for a 40k-rps service."),
              ("WHAT I BUILT","A consistent-hashing router; cut p99 latency 35%."),
              ("TRADE-OFF","Chose eventual consistency, kept writes idempotent.")]
        yy=ay+78
        for i,(lab,body) in enumerate(secs):
            rp=clamp((ap-0.15-i*0.22)/0.3)
            if rp<=0.02: continue
            txt(d,x+26,yy,lab,font(19,800),(150,132,240),clamp(rp*1.5))
            nn=int(len(body)*clamp(rp*1.4)); txt(d,x+26,yy+30,body[:nn],font(23,500),(232,232,242),1); yy+=72

NOTIFS=[("mark_email_read","Application received","Northwind · Senior Product Manager","#34d17f"),
        ("event","Interview requested","Tempo · Wednesday, 2:00 PM","#7c5cff"),
        ("military_tech","Offer extended","Hollow · Principal PM, AI","#ffb02e")]
def vis_notifs(img,p,t,count,confetti):
    cw=720; ch=140; gap=18; x=VX-cw/2
    items=NOTIFS[:count]; total=len(items)*ch+(len(items)-1)*gap; y0=int(H*0.5-total/2)
    d=ImageDraw.Draw(img,"RGBA")
    for idx,(ic,title,body,acc) in enumerate(items):
        last=idx==len(items)-1; rise=(1-out_back(clamp(p/0.6)))*90 if last else 0
        y=y0+idx*(ch+gap)+rise
        if last and rise>4:
            glass_card(img,x,y+24,cw,ch,0.12,24,shadow=False)
        glass_card(img,x,int(y),cw,ch,1,24)
        img.paste(LOGO_NOTIF,(int(x+22),int(y)+22),LOGO_NOTIF)
        dd=ImageDraw.Draw(img,"RGBA"); tx=x+22+56+18
        txt(dd,tx,int(y)+30,"Ascend",font(23,800),INK,1)
        txt(dd,tx+tw(dd,"Ascend",font(23,800))+10,int(y)+30,"· now",font(19,500),MUT2,1)
        txt(dd,tx,int(y)+70,title,font(29,800),INK,1)
        txt(dd,tx,int(y)+106,body,font(22,500),MUTE,1)
        cxr=x+cw-54; cyr=int(y)+ch//2
        if last: halo(img,cxr,cyr,60,acc,0.5)
        dd=ImageDraw.Draw(img,"RGBA"); dd.ellipse([cxr-32,cyr-32,cxr+32,cyr+32],fill=Acol(acc,0.18))
        icon(dd,(cxr,cyr),ic,32,_rgb(acc),fill=1,anchor="mm")
    if confetti>0: draw_confetti(img,confetti)

def draw_confetti(img,p):
    rng=random.Random(7); cols=[(124,92,255),(52,209,127),(255,176,46),(255,99,99),(0,200,255)]
    for i in range(120):
        x0=rng.uniform(VX-470,VX+470); col=cols[i%len(cols)]
        fall=p*rng.uniform(740,1180); x=x0+math.sin(p*6+i)*22; y=110+fall-200
        if -20<y<H+20:
            s=rng.uniform(7,13); box=Image.new("RGBA",(int(s*2),int(s*2)),(0,0,0,0))
            ImageDraw.Draw(box).rectangle([0,0,s,s*0.62],fill=col+(240,))
            box=box.rotate(p*360+i*20,expand=True); img.paste(box,(int(x),int(y)),box)

# ---------- transition light sweep ----------
def light_sweep(img,prog):
    # diagonal bright band crossing the frame
    band=Image.new("RGBA",(W,H),(0,0,0,0)); d=ImageDraw.Draw(band)
    cx=int(lerp(-400,W+400,prog))
    for i in range(-180,181,6):
        a=int(70*math.exp(-(i/90)**2))
        d.line([cx+i,0,cx+i-300,H],fill=(255,255,255,a),width=6)
    img.alpha_composite(band)

# ============ timeline ============
SCENES=[
 ("intro",3.2,None),
 ("findjobs",4.2,("FIND JOBS",["Find your","next role"],"AI-matched jobs, ranked by fit","#34d17f")),
 ("tracker",4.0,("TRACKER",["Track every","application"],"From saved to offer, automatically","#8b9bff")),
 ("ats",4.0,("RESUME",["Beat the ATS"],"AI tailors your resume to each role","#ffd66b")),
 ("mock",4.0,("PREP",["Practice,","then perform"],"Scored mock interviews","#ff8fb1")),
 ("copilot",4.4,("LIVE COPILOT",["Real-time","answers"],"In your voice, during the call","#b3a6ff")),
 ("notif1",1.7,None),("notif2",1.7,None),("notif3",2.8,None),
 ("outro",3.6,None),
]
STARTS=[]; _t=0.0
for _,d,_ in SCENES: STARTS.append(_t); _t+=d
TOTAL=_t
TRN=0.45  # transition window

def brand_corner(img):
    img.paste(LOGO_SM,(120,84),LOGO_SM); d=ImageDraw.Draw(img,"RGBA")
    txt(d,200,102,"Ascend",font(34,800),(255,255,255),1)
    txt(d,200,138,"Land your next role, faster",font(17,500),(212,210,236),1)

def render_scene(img,i,t):
    name,dur,cap=SCENES[i]; local=t-STARTS[i]; p=clamp(local/dur)
    if name=="intro":
        sc=out_back(clamp(p/0.5)); ls=int(150*sc)
        halo(img,W//2,H//2-230,260,VIOLET2,0.5*clamp(p/0.4))
        if ls>4:
            lg=LOGO_BIG.resize((ls,ls),Image.LANCZOS); img.paste(lg,(W//2-ls//2,H//2-230-ls//2),lg)
        fade=1-smooth((p-0.84)/0.16); d=ImageDraw.Draw(img,"RGBA")
        kinetic_line(img,W//2-tw(d,"Ascend",font(96,800))/2,H//2-70,"Ascend",font(96,800),(255,255,255),clamp((p-0.18)/0.4)*fade,30)
        txt(d,W//2,H//2+12,"Land your next role, faster",font(38,500),(236,234,255),smooth((p-0.4)/0.25)*fade,"mm")
        txt(d,W//2,H//2+122,"YOUR AI JOB-SEARCH COPILOT",font(25,700,mono=True),(176,164,255),smooth((p-0.58)/0.3)*fade,"mm")
        return
    if name=="outro":
        ap=out_cubic(clamp(p/0.4)); halo(img,W//2,H//2-250,240,VIOLET2,0.5*ap)
        img.paste(LOGO_BIG,(W//2-75,int(H//2-250-(1-ap)*40)),LOGO_BIG)
        d=ImageDraw.Draw(img,"RGBA")
        txt(d,W//2,H//2-58,"Ascend",font(96,800),(255,255,255),ap,"mm")
        txt(d,W//2,H//2+20,"Land your next role, faster",font(38,500),(236,234,255),ap,"mm")
        b=out_back(clamp((p-0.4)/0.5))
        if b>0.02:
            bw=int(BADGE.size[0]*b); bh=int(BADGE.size[1]*b)
            bb=BADGE.resize((max(1,bw),max(1,bh)),Image.LANCZOS); img.paste(bb,(W//2-bw//2,int(H//2+150-bh//2)),bb)
        return
    brand_corner(img)
    if name=="findjobs": vis_search(img,p,t)
    elif name=="tracker": vis_tracker(img,p,t)
    elif name=="ats": vis_ats(img,p,t)
    elif name=="mock": vis_mock(img,p,t)
    elif name=="copilot": vis_copilot(img,p,t)
    elif name.startswith("notif"):
        cnt={"notif1":1,"notif2":2,"notif3":3}[name]
        conf=clamp((local-0.2)/(dur-0.2)) if name=="notif3" else 0.0
        vis_notifs(img,p,t,cnt,conf)
        # persistent left caption across the whole notification group (no per-scene flicker)
        gstart=STARTS[6]; gdur=STARTS[9]-STARTS[6]; gl=t-gstart
        aL=smooth(clamp(gl/0.5))*(1-smooth(clamp((gl-(gdur-0.5))/0.5)))
        static_caption(img,"ON TRACK",["From applied","to hired"],"Notified at every step","#34d17f",aL)
    if cap is not None: caption(img,cap[0],cap[1],cap[2],cap[3],p,local,dur)

def frame(t):
    i=0
    while i+1<len(SCENES) and t>=STARTS[i+1]: i+=1
    img=bg_base(t).convert("RGBA")
    draw_particles(img,t,front=False)
    name,dur,_=SCENES[i]; local=t-STARTS[i]
    vis = name not in ("intro","outro")
    if vis:
        pa=1.0
        if i==1: pa=smooth(local/0.5)
        if i==len(SCENES)-2: pa*=1-smooth((local-(dur-0.5))/0.5)
        draw_stage_panel(img,pa,t)
    render_scene(img,i,t)
    draw_particles(img,t,front=True)
    # light-sweep transition straddling each scene cut (band crosses the frame)
    local=t-STARTS[i]; dur=SCENES[i][1]
    if i>0 and local<TRN:                       # just entered: band centre -> right
        light_sweep(img, 0.5+(local/TRN)*0.5)
    if i<len(SCENES)-1 and dur-local<TRN:        # about to leave: band left -> centre
        light_sweep(img, ((dur-local)/TRN)*0.5)
    return grade(img.convert("RGB"))

def main():
    out="out/ascend_silent.mp4"; n=int(TOTAL*FPS)
    wr=imageio_ffmpeg.write_frames(out,(W,H),fps=FPS,codec="libx264",quality=6,macro_block_size=8,
        output_params=["-pix_fmt","yuv420p","-movflags","+faststart","-profile:v","high"])
    wr.send(None)
    for k in range(n):
        wr.send(np.ascontiguousarray(frame(k/FPS)))
        if k%60==0:print(f"{k}/{n}",flush=True)
    wr.close(); print("DONE",out,f"{TOTAL:.1f}s {n}f")

if __name__=="__main__": main()
