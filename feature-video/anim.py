"""Ascend Google Play promo — fully animated motion graphics (no screenshots).
Every element is drawn and animated per-frame with Pillow.
"""
import math, os, random, numpy as np
from PIL import Image, ImageDraw, ImageFilter
import imageio_ffmpeg
from ascend import font, icon, tw, logo_mark, _rgb, INDIGO, INDIGO2, VIOLET, VIOLET2
import screens

W,H,FPS = 1920,1080,30
HERE=os.path.dirname(os.path.abspath(__file__))

def clamp(x,a=0.0,b=1.0): return max(a,min(b,x))
def smooth(x): x=clamp(x); return x*x*(3-2*x)
def lerp(a,b,t): return a+(b-a)*t
def out_cubic(x): x=clamp(x); return 1-(1-x)**3
def out_back(x):
    x=clamp(x); c=1.70158; return 1+(c+1)*(x-1)**3+c*(x-1)**2
def stagger(p, i, n, overlap=0.55):
    """eased reveal for item i of n within progress p."""
    span=1.0/(n*(1-overlap)+overlap); start=i*span*(1-overlap)
    return out_cubic((p-start)/span)

INK=(21,21,28); MUTE=(122,122,135); MUT2=(150,150,166); LINE=(236,236,242)
GREEN=(15,157,104); GREENBG=(231,247,239)

# ---------- animated background ----------
_BG=screens.vgrad((0,0,W,H),"#6d5cf0","#191641",145).convert("RGB")
def make_bg(t):
    img=_BG.copy(); ov=Image.new("RGBA",(W,H),(0,0,0,0)); d=ImageDraw.Draw(ov)
    blobs=[(250,170,540,46,VIOLET,0.6),(1720,930,640,42,INDIGO2,0.4),
           (1500,120,360,28,"#a89bff",0.8),(120,1010,440,24,"#8b5cf6",0.5)]
    for cx,cy,r,a,col,sp in blobs:
        dx=math.sin(t*sp)*40; dy=math.cos(t*sp*0.8)*30
        d.ellipse([cx+dx-r,cy+dy-r,cx+dx+r,cy+dy+r],fill=_rgb(col)+(a,))
    img=Image.alpha_composite(img.convert("RGBA"),ov.filter(ImageFilter.GaussianBlur(130)))
    return img.convert("RGB")
# precompute a few bg frames and reuse (cheap, subtle drift)
_BGCACHE={}
def bg(t):
    k=round(t*4)
    if k not in _BGCACHE:
        if len(_BGCACHE)>40: _BGCACHE.clear()
        _BGCACHE[k]=make_bg(k/4)
    return _BGCACHE[k].copy()

# ---------- logo ----------
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

# ---------- small draw helpers (alpha-aware) ----------
def A(c,a):
    c=c if isinstance(c,tuple) else _rgb(c); return (c[0],c[1],c[2],int(255*clamp(a)))
def card(d,x,y,w,h,a=1.0,r=22,fill=(255,255,255),outline=None,shadow=None):
    d.rounded_rectangle([x,y,x+w,y+h],radius=r,fill=A(fill,a),
                        outline=A(outline,a) if outline else None,width=2 if outline else 1)
def txt(d,x,y,s,f,col,a=1.0,anchor="lm"):
    if a<=0.01:return
    d.text((x,y),s,font=f,fill=A(col,a),anchor=anchor)
def avatar(d,x,y,s,col,ini,a=1.0):
    d.rounded_rectangle([x,y,x+s,y+s],radius=int(s*0.28),fill=A(col,a))
    txt(d,x+s/2,y+s/2,ini,font(int(s*0.42),800),(255,255,255),a,"mm")

def soft_shadow(img,x,y,w,h,r,blur=26,col=(6,5,22,150)):
    sh=Image.new("RGBA",(int(w+blur*4),int(h+blur*4)),(0,0,0,0))
    ImageDraw.Draw(sh).rounded_rectangle([blur*2,blur*2+10,blur*2+w,blur*2+h+10],radius=r,fill=col)
    sh=sh.filter(ImageFilter.GaussianBlur(blur)); img.paste(sh,(int(x-blur*2),int(y-blur*2)),sh)

# ---------- caption ----------
CX=130
def caption(img,d,eyebrow,lines,sub,accent,ca,dx=0.0):
    if ca<=0.02:return
    block=40+24+len(lines)*86+14+34; top=int(H*0.5-block/2)
    f=font(21,800); pad=20; w=tw(d,eyebrow,f)+pad*2
    dark=accent in ("#ffd66b","#34d17f","#ff8fb1","#b3a6ff","#8b9bff","#ffb02e")
    d.rounded_rectangle([CX-dx,top,CX-dx+w,top+40],radius=20,fill=A(accent,0.95*ca))
    txt(d,CX-dx+pad,top+20,eyebrow,f,(22,18,42) if dark else (255,255,255),ca)
    yy=top+40+24+30
    for ln in lines: txt(d,CX-dx,yy,ln,font(74,800),(255,255,255),ca); yy+=86
    txt(d,CX-dx,yy+2,sub,font(30,500),(224,222,246),ca)

# ============ animated visuals ============
VX=1360   # visual centre x (right half)

def vis_search(img,d,p,t):
    # search bar typing, then job cards fly in with counting match%
    bx,bw=1010,700; by=210
    soft_shadow(img,bx,by,bw,72,18); card(d,bx,by,bw,72,r=18)
    icon(d,(bx+34,by+36),"search",30,INDIGO,anchor="mm")
    q="Product Manager"; tp=clamp(p/0.28); n=int(len(q)*tp)
    txt(d,bx+66,by+36,q[:n],font(26,600),INK)
    if tp<1 and int(t*2)%2==0:
        cw=bx+66+tw(d,q[:n],font(26,600)); d.rectangle([cw+2,by+22,cw+5,by+50],fill=A(INK,1))
    jobs=[("Senior Product Manager","Northwind · Remote","N",INDIGO,96),
          ("Product Manager, Growth","Tempo · Hybrid","T",GREEN,91),
          ("Group Product Manager","Ledgerline · NY","L",(224,145,63),84)]
    cy=by+108
    for i,(title,co,ini,col,m) in enumerate(jobs):
        rp=stagger(clamp((p-0.30)/0.7),i,len(jobs))
        if rp<=0.01: continue
        off=(1-out_back(rp))*70; a=clamp(rp*1.3)
        x=bx; y=cy+i*128+off; w=bw; h=110
        soft_shadow(img,x,y,w,h,18,blur=22,col=(6,5,22,int(120*a)))
        card(d,x,y,w,h,a,18)
        avatar(d,x+18,y+20,46,col,ini,a)
        txt(d,x+80,y+34,title,font(25,700),INK,a)
        txt(d,x+80,y+66,co,font(20,500),MUTE,a)
        mv=int(m*out_cubic(clamp((rp-0.2)/0.8)))
        bw2=tw(d,f"{mv}%",font(20,800,mono=True))+24
        d.rounded_rectangle([x+w-bw2-20,y+34,x+w-20,y+34+34],radius=12,fill=A(GREENBG,a))
        txt(d,x+w-bw2-20+bw2/2,y+34+17,f"{mv}%",font(20,800,mono=True),GREEN,a,"mm")

def vis_tracker(img,d,p,t):
    cols=[("SAVED",(107,107,120)),("APPLIED",(37,99,235)),("INTERVIEW",(124,92,255)),("OFFER",(15,157,104))]
    n=len(cols); gap=24; cw=150; total=n*cw+(n-1)*gap; x0=VX-total/2; y0=300; colh=420
    for i,(lab,col) in enumerate(cols):
        x=x0+i*(cw+gap)
        card(d,x,y0,cw,colh,0.16,18,fill=(255,255,255))
        d.rounded_rectangle([x,y0,x+cw,y0+44],radius=14,fill=A(col,0.9))
        txt(d,x+cw/2,y0+22,lab,font(15,800),(255,255,255),1,"mm")
    # a card travels Saved->Applied->Interview->Offer
    seg=clamp(p/0.92)*3       # 0..3 across columns
    ci=min(3,int(seg)); fr=out_cubic(seg-ci)
    def colx(i): return x0+i*(cw+gap)+cw/2
    cardx=lerp(colx(ci),colx(min(3,ci+1)),fr)
    cy=y0+90; ch=96; cwd=cw-16
    # ghost trail in passed columns
    for i in range(ci+1):
        gx=colx(i)
        if i<ci or fr>0.1:
            a=0.25 if i<ci else 0.25
            d.rounded_rectangle([gx-cwd/2,cy,gx+cwd/2,cy+ch],radius=14,fill=A((255,255,255),a))
    # moving card
    x=cardx-cwd/2
    soft_shadow(img,x,cy,cwd,ch,16,blur=20)
    card(d,x,cy,cwd,ch,1,16)
    avatar(d,x+14,cy+14,40,INDIGO,"N",1)
    txt(d,x+14,cy+72,"Senior PM",font(18,700),INK,1)
    # checkmark blips when landing on offer
    if ci>=3 and fr>0.6:
        a=clamp((fr-0.6)/0.4)
        icon(d,(colx(3),y0+colh-40),"verified",46,GREEN,fill=1,anchor="mm")

def vis_ats(img,d,p,t):
    cx,cy,r=VX,430,150
    sc=4; big=Image.new("RGBA",((r+22)*2*sc,)*2,(0,0,0,0)); bd=ImageDraw.Draw(big)
    o=(r+22)*sc; bb=[o-r*sc,o-r*sc,o+r*sc,o+r*sc]
    bd.arc(bb,0,360,fill=(238,240,244,255),width=22*sc)
    val=int(93*out_cubic(clamp(p/0.7)))
    if val>0: bd.arc(bb,-90,-90+360*val/100,fill=_rgb("#0f9d68")+(255,),width=22*sc)
    big=big.resize(((r+22)*2,)*2,Image.LANCZOS); img.paste(big,(int(cx-(r+22)),int(cy-(r+22))),big)
    d2=ImageDraw.Draw(img,"RGBA")
    txt(d2,cx,cy-14,str(val),font(78,800,mono=True),INK,1,"mm")
    txt(d2,cx,cy+46,"ATS SCORE",font(22,700),MUT2,1,"mm")
    checks=["Keywords matched","Impact quantified","ATS-safe format","Tailored to the role"]
    yy=cy+r+70
    for i,c in enumerate(checks):
        rp=stagger(clamp((p-0.4)/0.6),i,len(checks));
        if rp<=0.02: continue
        a=clamp(rp*1.4); x=VX-300
        icon(d2,(x+18,yy+18),"check_circle",30,GREEN,fill=1,anchor="mm")
        txt(d2,x+50,yy+18,c,font(26,600),INK,a)
        yy+=58

def vis_mock(img,d,p,t):
    x=VX-330; w=660
    # question card types in
    qy=250; qh=150
    soft_shadow(img,x,qy,w,qh,18); card(d,x,qy,w,qh,1,18)
    txt(d,x+24,qy+34,"QUESTION 3 OF 5",font(18,700,mono=True),INDIGO,1)
    q="Design a system for 40k requests per second."
    tp=clamp(p/0.4); n=int(len(q)*tp)
    # wrap
    words=q[:n].split(" "); line1=" ".join(words[:5]); line2=" ".join(words[5:])
    txt(d,x+24,qy+78,line1,font(30,800),INK,1)
    txt(d,x+24,qy+116,line2,font(30,800),INK,1)
    # score bars fill
    areas=[("Clarity",88,INDIGO),("Structure",81,VIOLET2),("Impact",92,GREEN)]
    by=qy+qh+50
    for i,(lab,sc,col) in enumerate(areas):
        rp=clamp((p-0.45-i*0.12)/0.4)
        txt(d,x,by+i*64+18,lab,font(24,600),INK,1)
        bx=x+180; bw=w-300
        d.rounded_rectangle([bx,by+i*64+8,bx+bw,by+i*64+28],radius=10,fill=A(LINE,1))
        fw=bw*(sc/100)*out_cubic(rp)
        if fw>2: d.rounded_rectangle([bx,by+i*64+8,bx+fw,by+i*64+28],radius=10,fill=A(col,1))
        val=int(sc*out_cubic(rp))
        txt(d,x+w,by+i*64+18,f"{val}",font(24,800,mono=True),col,1,"rm")

def waveform(d,cx,cy,t,a=1.0,scale=1.0,col=INDIGO):
    for i in range(-6,7):
        h=(18+abs(math.sin(t*4+i*0.6))*54)*scale
        x=cx+i*16; d.rounded_rectangle([x-5,cy-h/2,x+5,cy+h/2],radius=4,
            fill=A(col if i%2==0 else VIOLET2,a))

def vis_copilot(img,d,p,t):
    x=VX-340; w=680
    # listening waveform
    if p<0.9: waveform(d,VX,240,t,clamp(1-(p-0.7)/0.2),1.0)
    # detected question bubble slides in
    qp=out_back(clamp((p-0.12)/0.25));
    if qp>0.02:
        qy=300+(1-qp)*30;
        card(d,x,qy,w,72,clamp(qp),16,fill=(26,24,56),outline=(58,52,112))
        txt(d,x+22,qy+36,"Walk me through a tradeoff you made.",font(24,600),(230,230,245),clamp(qp))
    # answer card with STAR sections typing/revealing
    ap=clamp((p-0.34)/0.62)
    if ap>0.02:
        ay=400; ah=300
        soft_shadow(img,x,ay,w,ah,20,col=(20,10,60,120))
        card(d,x,ay,w,ah,clamp(ap*1.3),20,fill=(26,22,56),outline=(58,52,112))
        icon(d,(x+26,ay+34),"bolt",26,(168,155,255),anchor="mm")
        txt(d,x+48,ay+34,"COPILOT ANSWER",font(20,800),(168,155,255),clamp(ap*1.3))
        secs=[("CONTEXT","Owned the sharding layer for a 40k-rps service."),
              ("WHAT I BUILT","A consistent-hashing router; cut p99 latency 35%."),
              ("TRADE-OFF","Chose eventual consistency, kept writes idempotent.")]
        yy=ay+78
        for i,(lab,body) in enumerate(secs):
            rp=clamp((ap-0.15-i*0.22)/0.3)
            if rp<=0.02: continue
            txt(d,x+26,yy,lab,font(19,800),(139,120,236),clamp(rp*1.5))
            nn=int(len(body)*clamp(rp*1.4));
            txt(d,x+26,yy+30,body[:nn],font(23,500),(230,230,240),1)
            yy+=72

NOTIFS=[("mark_email_read","Application received","Northwind · Senior Product Manager","#34d17f"),
        ("event","Interview requested","Tempo · Wednesday, 2:00 PM","#7c5cff"),
        ("military_tech","Offer extended","Hollow · Principal PM, AI","#ffb02e")]
def vis_notifs(img,d,p,t,count,confetti):
    cw=720; ch=140; gap=18; x=VX-cw/2
    items=NOTIFS[:count]; total=len(items)*ch+(len(items)-1)*gap; y0=int(H*0.5-total/2)
    for idx,(ic,title,body,acc) in enumerate(items):
        last=idx==len(items)-1; rise=(1-out_back(clamp(p/0.6)))*80 if last else 0
        y=y0+idx*(ch+gap)+rise
        soft_shadow(img,x,y,cw,ch,24)
        card(d,x,int(y),cw,ch,1,24)
        img.paste(LOGO_NOTIF,(int(x+22),int(y)+22),LOGO_NOTIF)
        tx=x+22+56+18
        txt(d,tx,int(y)+30,"Ascend",font(23,800),INK,1)
        txt(d,tx+tw(d,"Ascend",font(23,800))+10,int(y)+30,"· now",font(19,500),MUT2,1)
        txt(d,tx,int(y)+70,title,font(29,800),INK,1)
        txt(d,tx,int(y)+106,body,font(22,500),MUTE,1)
        cxr=x+cw-54; cyr=int(y)+ch//2
        d.ellipse([cxr-32,cyr-32,cxr+32,cyr+32],fill=A(acc,0.16))
        icon(d,(cxr,cyr),ic,32,_rgb(acc),fill=1,anchor="mm")
    if confetti>0: draw_confetti(img,confetti)

def draw_confetti(img,p):
    d=ImageDraw.Draw(img,"RGBA"); rng=random.Random(7)
    cols=[(124,92,255),(52,209,127),(255,176,46),(255,99,99),(0,200,255)]
    for i in range(110):
        x0=rng.uniform(VX-460,VX+460); col=cols[i%len(cols)]
        fall=p*rng.uniform(720,1150); x=x0+math.sin(p*6+i)*20; y=120+fall-200
        if -20<y<H+20:
            s=rng.uniform(7,13); box=Image.new("RGBA",(int(s*2),int(s*2)),(0,0,0,0))
            ImageDraw.Draw(box).rectangle([0,0,s,s*0.6],fill=col+(235,))
            box=box.rotate(p*360+i*20,expand=True); img.paste(box,(int(x),int(y)),box)

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

def brand_corner(img,d):
    img.paste(LOGO_SM,(120,84),LOGO_SM)
    txt(d,200,102,"Ascend",font(34,800),(255,255,255),1)
    txt(d,200,138,"Land your next role, faster",font(17,500),(212,210,236),1)

def frame(t):
    i=0
    while i+1<len(SCENES) and t>=STARTS[i+1]: i+=1
    name,dur,cap=SCENES[i]; local=t-STARTS[i]; p=clamp(local/dur)
    img=bg(t); d=ImageDraw.Draw(img,"RGBA")

    if name=="intro":
        sc=out_back(clamp(p/0.5)); ls=int(150*sc)
        if ls>4:
            lg=LOGO_BIG.resize((ls,ls),Image.LANCZOS); img.paste(lg,(W//2-ls//2,H//2-230-ls//2),lg)
        fade=1-smooth((p-0.82)/0.18)
        txt(d,W//2,H//2-70,"Ascend",font(96,800),(255,255,255),smooth((p-0.18)/0.25)*fade,"mm")
        txt(d,W//2,H//2+12,"Land your next role, faster",font(38,500),(236,234,255),smooth((p-0.34)/0.25)*fade,"mm")
        txt(d,W//2,H//2+122,"YOUR AI JOB-SEARCH COPILOT",font(25,700,mono=True),(176,164,255),smooth((p-0.55)/0.3)*fade,"mm")
        return np.asarray(img)

    if name=="outro":
        ap=out_cubic(clamp(p/0.4))
        img.paste(LOGO_BIG,(W//2-75,int(H//2-250-(1-ap)*40)),LOGO_BIG)
        txt(d,W//2,H//2-58,"Ascend",font(96,800),(255,255,255),ap,"mm")
        txt(d,W//2,H//2+20,"Land your next role, faster",font(38,500),(236,234,255),ap,"mm")
        b=out_back(clamp((p-0.4)/0.5))
        if b>0.02:
            bw=int(BADGE.size[0]*b); bh=int(BADGE.size[1]*b)
            bb=BADGE.resize((max(1,bw),max(1,bh)),Image.LANCZOS); img.paste(bb,(W//2-bw//2,int(H//2+150-bh//2)),bb)
        return np.asarray(img)

    brand_corner(img,d)
    ca=smooth(clamp(local/0.4))*(1-smooth(clamp((local-(dur-0.3))/0.3)))
    if name=="findjobs": vis_search(img,d,p,t)
    elif name=="tracker": vis_tracker(img,d,p,t)
    elif name=="ats": vis_ats(img,d,p,t)
    elif name=="mock": vis_mock(img,d,p,t)
    elif name=="copilot": vis_copilot(img,d,p,t)
    elif name.startswith("notif"):
        cnt={"notif1":1,"notif2":2,"notif3":3}[name]
        conf=clamp((local-0.2)/(dur-0.2)) if name=="notif3" else 0.0
        vis_notifs(img,d,p,t,cnt,conf)
        txt(d,CX,H*0.5- ( {1:1,2:2,3:3}[cnt]*79)-60,"YOUR SEARCH, ON TRACK",font(22,700,mono=True),(214,210,245),smooth(clamp(local/0.4)))
    if cap is not None:
        eb,lines,sub,acc=cap; caption(img,d,eb,lines,sub,acc,ca,(1-smooth(clamp(local/0.4)))*55)
    return np.asarray(img)

def main():
    out="out/ascend_silent.mp4"; n=int(TOTAL*FPS)
    wr=imageio_ffmpeg.write_frames(out,(W,H),fps=FPS,codec="libx264",quality=7,macro_block_size=8,
        output_params=["-pix_fmt","yuv420p","-movflags","+faststart","-profile:v","high"])
    wr.send(None)
    for k in range(n):
        wr.send(np.ascontiguousarray(frame(k/FPS)))
        if k%90==0:print(f"{k}/{n}",flush=True)
    wr.close(); print("DONE",out,f"{TOTAL:.1f}s {n}f")

if __name__=="__main__": main()
