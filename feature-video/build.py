"""Ascend Google Play feature video (v4) — dynamic.
Real app screens with Ken-Burns zoom + vertical-slide scene changes, and a
notification-driven user-journey arc (application received -> interview
requested -> offer extended). Music muxed separately.
"""
import math, os, random, numpy as np
from PIL import Image, ImageDraw, ImageFilter
import imageio_ffmpeg
from ascend import font, icon, tw, logo_mark, _rgb, INDIGO, INDIGO2, VIOLET
import screens

W,H,FPS = 1920,1080,30
HERE=os.path.dirname(os.path.abspath(__file__))

def clamp(x,a=0.0,b=1.0): return max(a,min(b,x))
def smooth(x): x=clamp(x); return x*x*(3-2*x)
def lerp(a,b,t): return a+(b-a)*t
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

# ---------- real phones (rounded, with alpha) ----------
PH=940
NAMES=["home","search","tracker","optimizer","mock","copilot","games"]
def load_phone(n):
    im=Image.open(os.path.join(HERE,"real",f"{n}.png")).convert("RGB")
    w=round(im.size[0]*PH/im.size[1]); im=im.resize((w,PH),Image.LANCZOS)
    rad=round(54*w/412)
    a=Image.new("L",(w,PH),0); ImageDraw.Draw(a).rounded_rectangle([0,0,w-1,PH-1],radius=rad,fill=255)
    im=im.convert("RGBA"); im.putalpha(a); return im
PHI={n:load_phone(n) for n in NAMES}
PW=PHI["home"].size[0]
M=130
SHADOW=Image.new("RGBA",(PW+2*M,PH+2*M),(0,0,0,0))
ImageDraw.Draw(SHADOW).rounded_rectangle([M,M+26,M+PW,M+PH+26],radius=round(54*PW/412),fill=(8,6,26,165))
SHADOW=SHADOW.filter(ImageFilter.GaussianBlur(54))

def logo_tile(size,r):
    t=Image.new("RGBA",(size,size),(0,0,0,0))
    g=screens.vgrad((0,0,size,size),INDIGO,VIOLET,150).convert("RGBA")
    m=Image.new("L",(size,size),0); ImageDraw.Draw(m).rounded_rectangle([0,0,size-1,size-1],radius=r,fill=255)
    t.paste(g,(0,0),m); logo_mark(ImageDraw.Draw(t),size/2,size/2,size*0.56,"#ffffff")
    return t
LOGO_BIG=logo_tile(150,42); LOGO_SM=logo_tile(64,18); LOGO_NOTIF=logo_tile(56,16)

PCX=W-PW//2-220; PCY=H//2

def put_phone(img,name,z,fx,fy,dy):
    w=max(1,round(PW*z)); h=max(1,round(PH*z))
    ph=PHI[name].resize((w,h),Image.LANCZOS)
    px=int(PCX-fx*w); py=int(PCY+dy-fy*h)
    sw=round(SHADOW.width*z); sh=round(SHADOW.height*z)
    shimg=SHADOW.resize((sw,sh),Image.LANCZOS)
    img.paste(shimg,(px-round(M*z),py-round(M*z)),shimg)
    img.paste(ph,(px,py),ph)

def put_phone_slide(img,prev,cur,e,z,fx,fy,float_y):
    # whole-phone vertical slide: prev exits up, cur enters from below (no ghosting)
    off=(PH+160)
    put_phone(img,prev,1.0,0.5,0.5,float_y-e*off)
    put_phone(img,cur,1.0,0.5,0.5,float_y+(1-e)*off)

# ---------- badge ----------
def google_play_badge():
    bw,bh=486,132
    b=Image.new("RGBA",(bw,bh),(0,0,0,0)); d=ImageDraw.Draw(b)
    d.rounded_rectangle([0,0,bw-1,bh-1],radius=24,fill=(0,0,0,255),outline=(150,150,165,255),width=2)
    s=4; th=58; tw_=50; tx,ty=40,(bh-th)//2
    tri=Image.new("RGBA",(tw_*s,th*s),(0,0,0,0)); td=ImageDraw.Draw(tri)
    Wt,Ht=tw_*s,th*s; A=(0,0); Bp=(0,Ht); P=(Wt,Ht//2); C=(int(Wt*0.30),Ht//2)
    td.polygon([A,P,C],fill=(0,224,255,255)); td.polygon([Bp,P,C],fill=(255,61,84,255))
    td.polygon([A,Bp,C],fill=(0,224,150,255))
    mid=((C[0]+P[0])//2,Ht//2); td.polygon([mid,(P[0],Ht//2-int(Ht*0.16)),(P[0],Ht//2+int(Ht*0.16))],fill=(255,200,60,255))
    b.alpha_composite(tri.resize((tw_,th),Image.LANCZOS),(tx,ty))
    txx=tx+tw_+26; ef=font(20,600); ex=txx
    for ch in "GET IT ON":
        d.text((ex,40),ch,font=ef,fill=(196,196,206),anchor="lm"); ex+=tw(d,ch,ef)+3
    d.text((txx-2,86),"Google Play",font=font(40,700),fill=(255,255,255),anchor="lm")
    return b
BADGE=google_play_badge()

# ---------- timeline ----------
# phone beat: (kind, eyebrow, lines, sub, screen, accent, z1, fx, fy)
# notif beat: (kind='notif', eyebrow, n_count, confetti, screen)
PHONE_BEATS = {
 "search":("FIND JOBS",["Find your","next role"],"AI-matched jobs, ranked by how you fit","search","#34d17f",1.07,0.5,0.46),
 "optimizer":("RESUME",["Beat the ATS"],"AI tailors your resume to each role","optimizer","#ffd66b",1.13,0.5,0.22),
 "games":("BRAIN GAMES",["Sharpen up","between applications"],"A daily puzzle to keep you sharp","games","#34d17f",1.05,0.5,0.4),
 "mock":("PREP",["Practice,","then perform"],"Mock interviews with instant scoring","mock","#ff8fb1",1.08,0.5,0.34),
 "copilot":("LIVE COPILOT",["Your live","interview copilot"],"Real-time answers during the call","copilot","#b3a6ff",1.10,0.5,0.52),
}
NOTIFS=[
 ("mark_email_read","Application received","Northwind · Senior Product Manager","#34d17f"),
 ("event","Interview requested","Tempo · Wednesday, 2:00 PM","#7c5cff"),
 ("military_tech","Offer extended","Hollow · Principal PM, AI","#ffb02e"),
]

# (type, key/params, duration)
TL=[
 ("intro",None,3.4),
 ("phone","search",3.4),
 ("phone","optimizer",3.6),
 ("notif",1,2.6),     # show first 1 notification (Application received)
 ("phone","games",3.0),
 ("phone","mock",3.4),
 ("notif",2,2.6),     # +Interview requested
 ("phone","copilot",3.6),
 ("notif",3,3.2),     # +Offer extended (confetti)
 ("outro",None,4.0),
]
STARTS=[]; t=0.0
for _,_,d in TL: STARTS.append(t); t+=d
TOTAL=t
TR=0.55

# screen shown per beat index (for slide continuity)
def beat_screen(i):
    typ,key,_=TL[i]
    if typ=="phone": return key
    if typ=="notif": return "tracker"
    return None

def _ltext(d,x,y,s,f,col,a,anchor="lm"):
    if a<=0.01:return
    col=col if isinstance(col,tuple) else _rgb(col)
    d.text((x,y),s,font=f,fill=col+(int(255*clamp(a)),),anchor=anchor)
def _pill(d,x,y,label,acc,a):
    f=font(21,800); pad=20; h=40; w=tw(d,label,f)+pad*2
    d.rounded_rectangle([x,y,x+w,y+h],radius=h//2,fill=_rgb(acc)+(int(235*a),))
    dark=acc in ("#ffd66b","#34d17f","#ff8fb1","#b3a6ff","#8b9bff","#ffb02e")
    d.text((x+pad,y+h/2),label,font=f,fill=((22,18,42) if dark else (255,255,255))+(int(255*a),),anchor="lm")

CX=130
def draw_caption(img,d,beat,ca,dx):
    eb,lines,sub,_,acc,_,_,_=PHONE_BEATS[beat]
    hl=lines; block=40+24+len(hl)*86+14+34; top=int(H*0.52-block/2)
    if ca<=0.02: return
    _pill(d,CX-dx,top,eb,acc,ca)
    yy=top+40+24+30
    for ln in hl: _ltext(d,CX-dx,yy,ln,font(74,800),"#ffffff",ca); yy+=86
    yy+=2; _ltext(d,CX-dx,yy,sub,font(30,500),(224,222,246),ca)

def draw_notifications(img,count,enter,confetti_p):
    d=ImageDraw.Draw(img,"RGBA")
    cw=900; ch=150; gap=18; x=CX
    items=NOTIFS[:count]
    total_h=len(items)*ch+(len(items)-1)*gap
    y0=int(H*0.5-total_h/2)-10
    _ltext(d,x,y0-46,"NOTIFICATIONS",font(22,700,mono=True),(214,210,245),1.0)
    for idx,(ic,title,body,acc) in enumerate(items):
        last=idx==len(items)-1
        rise=(1-out_back(enter))*70 if last else 0
        y=y0+idx*(ch+gap)+rise
        # shadow
        sh=Image.new("RGBA",(cw+80,ch+80),(0,0,0,0))
        ImageDraw.Draw(sh).rounded_rectangle([40,52,cw+40,ch+44],radius=26,fill=(6,5,22,150))
        sh=sh.filter(ImageFilter.GaussianBlur(26)); img.paste(sh,(x-40,int(y)-40),sh)
        dd=ImageDraw.Draw(img,"RGBA")
        dd.rounded_rectangle([x,int(y),x+cw,int(y)+ch],radius=26,fill=(255,255,255,255))
        img.paste(LOGO_NOTIF,(x+24,int(y)+24),LOGO_NOTIF)
        tx=x+24+56+18
        dd.text((tx,int(y)+30),"Ascend",font=font(24,800),fill=_rgb("#15151c"),anchor="lm")
        dd.text((tx+tw(dd,"Ascend",font(24,800))+10,int(y)+30),"· now",font=font(20,500),fill=_rgb("#9a9aa6"),anchor="lm")
        dd.text((tx,int(y)+72),title,font=font(31,800),fill=_rgb("#15151c"),anchor="lm")
        dd.text((tx,int(y)+112),body,font=font(23,500),fill=_rgb("#6b6b78"),anchor="lm")
        # right status icon
        cxr=x+cw-56; cyr=int(y)+ch//2
        dd.ellipse([cxr-34,cyr-34,cxr+34,cyr+34],fill=_rgb(acc)+(40,))
        icon(dd,(cxr,cyr),ic,34,_rgb(acc),fill=1,anchor="mm")
    if confetti_p>0: draw_confetti(img,confetti_p)

def draw_confetti(img,p):
    d=ImageDraw.Draw(img,"RGBA")
    rng=random.Random(7); cols=[(124,92,255),(52,209,127),(255,176,46),(255,99,99),(0,200,255)]
    for i in range(90):
        x0=rng.uniform(CX-40,CX+940); col=cols[i%len(cols)]
        fall=p*rng.uniform(700,1100); sway=math.sin(p*6+i)*18
        x=x0+sway; y=H*0.18+fall-200
        if -20<y<H+20:
            s=rng.uniform(6,12); ang=p*360+i*20
            box=Image.new("RGBA",(int(s*2),int(s*2)),(0,0,0,0))
            ImageDraw.Draw(box).rectangle([0,0,s,s*0.6],fill=col+(235,))
            box=box.rotate(ang,expand=True); img.paste(box,(int(x),int(y)),box)

def frame(t):
    img=BG.copy(); d=ImageDraw.Draw(img,"RGBA")
    # which beat
    i=0
    while i+1<len(TL) and t>=STARTS[i+1]: i+=1
    typ,key,dur=TL[i]; local=t-STARTS[i]

    if typ=="intro":
        p=local/dur; sc=out_back(clamp(p/0.5)); ls=int(150*sc)
        if ls>4:
            lg=LOGO_BIG.resize((ls,ls),Image.LANCZOS); img.paste(lg,(W//2-ls//2,H//2-230-ls//2),lg)
        fade=1-smooth((p-0.82)/0.18)
        _ctext(d,W//2,H//2-70,"Ascend",font(96,800),"#ffffff",smooth((p-0.18)/0.25)*fade)
        _ctext(d,W//2,H//2+12,"Land your next role, faster",font(38,500),(236,234,255),smooth((p-0.34)/0.25)*fade)
        _ctext(d,W//2,H//2+122,"YOUR AI JOB-SEARCH COPILOT",font(25,700,mono=True),(176,164,255),smooth((p-0.55)/0.3)*fade)
        return np.asarray(img)

    if typ=="outro":
        p=local/dur; ap=out_cubic(clamp(p/0.4))
        img.paste(LOGO_BIG,(W//2-75,int(H//2-250-(1-ap)*40)),LOGO_BIG)
        _ctext(d,W//2,H//2-58,"Ascend",font(96,800),"#ffffff",ap)
        _ctext(d,W//2,H//2+20,"Land your next role, faster",font(38,500),(236,234,255),ap)
        b=out_back(clamp((p-0.4)/0.5))
        if b>0.02:
            bw=int(BADGE.size[0]*b); bh=int(BADGE.size[1]*b)
            bb=BADGE.resize((max(1,bw),max(1,bh)),Image.LANCZOS); img.paste(bb,(W//2-bw//2,int(H//2+150-bh//2)),bb)
        return np.asarray(img)

    # ---- phone present beats (phone + notif) ----
    img.paste(LOGO_SM,(120,84),LOGO_SM)
    d.text((200,102),"Ascend",font=font(34,800),fill=(255,255,255),anchor="lm")
    d.text((200,138),"Land your next role, faster",font=font(17,500),fill=(212,210,236),anchor="lm")

    float_y=math.sin(t*1.05)*8
    cur=beat_screen(i); prev=beat_screen(i-1) if i>0 else None

    # zoom params for this beat
    if typ=="phone": _,_,_,_,_,z1,fx,fy=PHONE_BEATS[key]
    else: z1,fx,fy=1.05,0.5,0.40
    if local<TR and prev is not None:
        e=smooth(local/TR)
        put_phone_slide(img,prev,cur,e,1.0,0.5,0.5,float_y)
    else:
        zt=clamp((local-TR)/(dur-TR))
        z=lerp(1.0,z1,smooth(zt))
        put_phone(img,cur,z,fx,fy,float_y)

    cin=smooth(clamp((local-(TR if prev else 0.15))/0.4))
    cout=1.0 if i==len(TL)-2 else 1-smooth(clamp((local-(dur-0.3))/0.3))
    ca=cin*cout; dx=(1-cin)*55

    if typ=="phone":
        draw_caption(img,d,key,ca,dx)
    else:
        conf=0.0
        if key==3:  # offer beat
            conf=clamp((local-0.2)/(dur-0.2))
        draw_notifications(img,key,cin,conf)
    return np.asarray(img)

def _ctext(d,cx,cy,s,f,col,a):
    if a<=0.01:return
    col=col if isinstance(col,tuple) else _rgb(col)
    d.text((cx,cy),s,font=f,fill=col+(int(255*clamp(a)),),anchor="mm")

def main():
    out="out/ascend_silent.mp4"; n=int(TOTAL*FPS)
    wr=imageio_ffmpeg.write_frames(out,(W,H),fps=FPS,codec="libx264",quality=7,
        macro_block_size=8,output_params=["-pix_fmt","yuv420p","-movflags","+faststart","-profile:v","high"])
    wr.send(None)
    for k in range(n):
        wr.send(np.ascontiguousarray(frame(k/FPS)))
        if k%90==0:print(f"{k}/{n}",flush=True)
    wr.close(); print("DONE",out,f"{TOTAL:.1f}s {n}f")

if __name__=="__main__": main()
