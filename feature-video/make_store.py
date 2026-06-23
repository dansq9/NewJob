"""Generate Google Play Store assets for Ascend from the real app screenshots:
  - 8 phone screenshots, 1080x1920 (portrait, branded marketing style)
  - 1 feature graphic, 1024x500
"""
import os, numpy as np
from PIL import Image, ImageDraw, ImageFilter
from ascend import font, tw, logo_mark, _rgb, INDIGO, INDIGO2, VIOLET
import screens

HERE=os.path.dirname(os.path.abspath(__file__))
OUT=os.path.join(HERE,"store"); os.makedirs(OUT,exist_ok=True)

def brand_bg(W,H,angle=160):
    base=screens.vgrad((0,0,W,H),"#6d5cf0","#191641",angle).convert("RGBA")
    ov=Image.new("RGBA",(W,H),(0,0,0,0)); d=ImageDraw.Draw(ov)
    for cx,cy,r,a,col in [(int(W*0.18),int(H*0.10),int(W*0.55),52,VIOLET),
                          (int(W*0.92),int(H*0.62),int(W*0.6),46,INDIGO2),
                          (int(W*0.85),int(H*0.06),int(W*0.35),30,"#a89bff"),
                          (int(W*0.08),int(H*0.92),int(W*0.45),26,"#8b5cf6")]:
        d.ellipse([cx-r,cy-r,cx+r,cy+r],fill=_rgb(col)+(a,))
    base=Image.alpha_composite(base,ov.filter(ImageFilter.GaussianBlur(int(W*0.12))))
    vig=Image.new("L",(W,H),0); ImageDraw.Draw(vig).ellipse([-int(W*0.2),-int(H*0.12),W+int(W*0.2),H+int(H*0.12)],fill=255)
    vig=vig.filter(ImageFilter.GaussianBlur(int(W*0.16)))
    base.paste(Image.new("RGBA",(W,H),(8,6,28,135)),(0,0),Image.eval(vig,lambda v:255-v))
    return base.convert("RGB")

def logo_tile(size,r):
    t=Image.new("RGBA",(size,size),(0,0,0,0))
    g=screens.vgrad((0,0,size,size),INDIGO,VIOLET,150).convert("RGBA")
    m=Image.new("L",(size,size),0); ImageDraw.Draw(m).rounded_rectangle([0,0,size-1,size-1],radius=r,fill=255)
    t.paste(g,(0,0),m); logo_mark(ImageDraw.Draw(t),size/2,size/2,size*0.56,"#ffffff")
    return t

def phone(name,height):
    im=Image.open(os.path.join(HERE,"real",f"{name}.png")).convert("RGB")
    w=round(im.size[0]*height/im.size[1]); im=im.resize((w,height),Image.LANCZOS)
    rad=round(54*w/412)
    return im,rad,w

def cpill(d,cx,y,label,acc):
    f=font(30,800); pad=30; h=58; w=tw(d,label,f)+pad*2
    x=cx-w/2
    d.rounded_rectangle([x,y,x+w,y+h],radius=h//2,fill=_rgb(acc))
    dark=acc in ("#ffd66b","#34d17f","#ff8fb1","#b3a6ff","#8b9bff")
    d.text((cx,y+h/2),label,font=f,fill=(22,18,42) if dark else (255,255,255),anchor="mm")

# eyebrow, headline lines, subline, screenshot, accent
SHOTS=[
 ("ALL-IN-ONE",["Everything to","land the job"],"Your AI job-search companion","home",INDIGO2),
 ("FIND JOBS",["Find your","next role"],"AI-matched jobs, ranked by how you fit","search","#34d17f"),
 ("TRACKER",["Track every","application"],"Saved → applied → interview → offer","tracker","#8b9bff"),
 ("RESUME",["Beat the ATS"],"AI tailors your resume to every role","optimizer","#ffd66b"),
 ("INTERVIEW PREP",["Practice,","then perform"],"Mock interviews with instant scoring","mock","#ff8fb1"),
 ("LIVE COPILOT",["Your live","interview copilot"],"Real-time answers during the call","copilot","#b3a6ff"),
 ("BRAIN GAMES",["Sharpen up","between applications"],"A daily puzzle to keep your mind fresh","games","#34d17f"),
]

W,H=1080,1920
for i,(eb,lines,sub,name,acc) in enumerate(SHOTS,1):
    img=brand_bg(W,H); d=ImageDraw.Draw(img,"RGBA")
    # top lockup
    lg=logo_tile(74,21); img.paste(lg,(W//2-100,70),lg)
    d.text((W//2-100+90,70+37),"Ascend",font=font(40,800),fill=(255,255,255),anchor="lm")
    # eyebrow + headline + sub (centred)
    cpill(d,W//2,196,eb,acc)
    yy=300
    for ln in lines:
        d.text((W//2,yy),ln,font=font(80,800),fill=(255,255,255),anchor="mm"); yy+=92
    d.text((W//2,yy+22),sub,font=font(34,500),fill=(226,224,248),anchor="mm")
    # phone
    ph,rad,pw=phone(name,1300)
    px=W//2-pw//2; py=560
    sh=Image.new("RGBA",(pw+260,1300+260),(0,0,0,0))
    ImageDraw.Draw(sh).rounded_rectangle([130,150,pw+130,1300+118],radius=rad,fill=(8,6,26,170))
    sh=sh.filter(ImageFilter.GaussianBlur(60)); img.paste(sh,(px-130,py-130),sh)
    corner=Image.new("L",(pw,1300),0); ImageDraw.Draw(corner).rounded_rectangle([0,0,pw-1,1300-1],radius=rad,fill=255)
    img.paste(ph,(px,py),corner)
    img.save(os.path.join(OUT,f"{i:02d}_{name}.png"))
    print("screenshot",f"{i:02d}_{name}.png")

# ---- feature graphic 1024x500 ----
W,H=1024,500
img=brand_bg(W,H,150); d=ImageDraw.Draw(img,"RGBA")
lg=logo_tile(120,34); img.paste(lg,(80,90),lg)
d.text((80,250),"Ascend",font=font(96,800),fill=(255,255,255),anchor="lm")
d.text((84,330),"Land your next role, faster",font=font(34,500),fill=(226,224,248),anchor="lm")
d.text((84,386),"Jobs · Tracker · AI Resume · Interviews",font=font(22,600),fill=(184,172,255),anchor="lm")
# phone peeks on the right
for nm,xoff,rot in [("optimizer",640,-8),("copilot",810,6)]:
    ph,rad,pw=phone(nm,470)
    corner=Image.new("L",(pw,470),0); ImageDraw.Draw(corner).rounded_rectangle([0,0,pw-1,470-1],radius=rad,fill=255)
    tile=Image.new("RGBA",(pw,470),(0,0,0,0)); tile.paste(ph,(0,0),corner)
    tile=tile.rotate(rot,expand=True,resample=Image.BICUBIC)
    img.paste(tile,(xoff,120),tile)
img.save(os.path.join(OUT,"feature_graphic_1024x500.png")); print("feature graphic done")
