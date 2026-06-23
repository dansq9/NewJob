"""Compose the 1920x1080 Google Play feature/promo video for Ascend."""
import math, sys, numpy as np
from PIL import Image, ImageDraw, ImageFilter
import imageio_ffmpeg
from ascend import (font, icon, icon_font, text, rrect, tw, logo_mark, _rgb,
                    INDIGO, INDIGO2, VIOLET, VIOLET2)
import screens

W, H, FPS = 1920, 1080, 30

# ---------- easing ----------
def clamp(x,a=0.0,b=1.0): return max(a,min(b,x))
def smooth(x): x=clamp(x); return x*x*(3-2*x)
def out_cubic(x): x=clamp(x); return 1-(1-x)**3
def out_back(x):
    x=clamp(x); c=1.70158
    return 1+ (c+1)*(x-1)**3 + c*(x-1)**2

# ---------- brand background (static, computed once) ----------
def make_bg():
    base = screens.vgrad((0,0,W,H), "#6d5cf0", "#1b1842", angle=145).convert("RGB")
    ov = Image.new("RGBA",(W,H),(0,0,0,0)); d=ImageDraw.Draw(ov)
    for (cx,cy,r,a,col) in [(250,180,520,46,VIOLET),(1700,920,620,40,INDIGO2),
                            (1500,140,360,30,"#a89bff"),(120,1000,420,26,"#8b5cf6")]:
        d.ellipse([cx-r,cy-r,cx+r,cy+r], fill=_rgb(col)+(a,))
    ov = ov.filter(ImageFilter.GaussianBlur(120))
    base = Image.alpha_composite(base.convert("RGBA"), ov)
    # subtle vignette
    vig = Image.new("L",(W,H),0); ImageDraw.Draw(vig).ellipse([-300,-200,W+300,H+200],fill=255)
    vig = vig.filter(ImageFilter.GaussianBlur(220))
    dark = Image.new("RGBA",(W,H),(8,6,30,150)); base.paste(dark,(0,0),Image.eval(vig,lambda v:255-v))
    return base.convert("RGB")

BG = make_bg()

# ---------- phone shell (static) ----------
SCALE = 1.02
IW, IH = round(screens.DW*SCALE), round(screens.DH*SCALE)      # inner screen
PADP = round(11*SCALE)
OW, OH = IW+PADP*2, IH+PADP*2
ROUT, RIN = round(54*SCALE), round(43*SCALE)
SS = 3  # supersample for crisp shell

def make_shell():
    img = Image.new("RGBA",(OW*SS,OH*SS),(0,0,0,0)); d=ImageDraw.Draw(img)
    d.rounded_rectangle([0,0,OW*SS-1,OH*SS-1],radius=ROUT*SS,fill=(17,19,25,255))
    d.rounded_rectangle([2*SS,2*SS,(OW-2)*SS,(OH-2)*SS],radius=(ROUT-2)*SS,
                        outline=(255,255,255,18),width=2*SS)
    return img.resize((OW,OH),Image.LANCZOS)
SHELL = make_shell()

INNER_MASK = Image.new("L",(IW,IH),0)
ImageDraw.Draw(INNER_MASK).rounded_rectangle([0,0,IW-1,IH-1],radius=RIN,fill=255)

def make_shadow():
    sh = Image.new("RGBA",(W,H),(0,0,0,0)); d=ImageDraw.Draw(sh)
    return sh  # filled per-position later
SHADOW_BASE = Image.new("RGBA",(OW+160,OH+160),(0,0,0,0))
ImageDraw.Draw(SHADOW_BASE).rounded_rectangle([80,96,OW+80,OH+72],radius=ROUT,fill=(10,8,30,150))
SHADOW_BASE = SHADOW_BASE.filter(ImageFilter.GaussianBlur(46))

# pre-render the six screens once, scaled to inner size
SCR = {}
for name in ["home","search","tracker","optimizer","mock","navigator"]:
    SCR[name] = getattr(screens,name)().resize((IW,IH),Image.LANCZOS)

# ---------- timeline ----------
# eyebrow, headline, subline, screen, accent-color
PHONE = [
 ("THE APP","Everything to land the job","One AI workspace for your whole search","home",INDIGO2),
 ("FIND JOBS","Find your next role","AI-matched roles, ranked by how well you fit","search","#34d17f"),
 ("TRACKER","Track every application","Saved → Applied → Interview → Offer, one board","tracker","#7c5cff"),
 ("RESUME","Beat the ATS","AI rewrites your resume for each role you target","optimizer","#ffd66b"),
 ("PREP","Practice, then perform","Mock interviews with instant, scored feedback","mock","#ff8fb1"),
 ("LIVE COPILOT","Your live interview navigator","Real-time answers in your voice, during the call","navigator","#a89bff"),
]
INTRO, OUT, SCENE, TRANS = 3.6, 4.2, 3.7, 0.55
T0 = INTRO
TOTAL = INTRO + len(PHONE)*SCENE + OUT

# phone home position
PX = W - OW//2 - 250    # phone center x
PY = H//2

def draw_logo(d, x, y, s, wordmark=True, color="#ffffff", sub=False):
    rr = Image.new("RGBA",(s,s),(0,0,0,0))
    paste_grad_tile(rr, INDIGO, VIOLET)
    return

def paste_grad_tile(img,c0,c1):
    g=screens.vgrad((0,0,img.size[0],img.size[1]),c0,c1,150)
    img.paste(g.convert("RGBA"),(0,0))

def logo_tile(size, r):
    t=Image.new("RGBA",(size,size),(0,0,0,0))
    g=screens.vgrad((0,0,size,size),INDIGO,VIOLET,150).convert("RGBA")
    m=Image.new("L",(size,size),0); ImageDraw.Draw(m).rounded_rectangle([0,0,size-1,size-1],radius=r,fill=255)
    t.paste(g,(0,0),m)
    logo_mark(ImageDraw.Draw(t), size/2, size/2, size*0.56, "#ffffff")
    return t

LOGO_BIG = logo_tile(150, 42)
LOGO_SM  = logo_tile(64, 18)

def wrap_lines(d,s,f,maxw):
    out=[]; cur=""
    for w in s.split():
        t=(cur+" "+w).strip()
        if tw(d,t,f)<=maxw: cur=t
        else: out.append(cur); cur=w
    if cur: out.append(cur)
    return out

def frame(t):
    img = BG.copy()
    d = ImageDraw.Draw(img,"RGBA")

    # ===== INTRO =====
    if t < INTRO:
        p = t/INTRO
        # logo pop
        sc = out_back(clamp(p/0.5))
        ls = int(150*sc)
        if ls>4:
            lg = LOGO_BIG.resize((ls,ls),Image.LANCZOS)
            img.paste(lg,(W//2-ls//2,H//2-230-ls//2),lg)
        a1 = smooth((p-0.18)/0.25)
        a2 = smooth((p-0.34)/0.25)
        a3 = smooth((p-0.55)/0.3)
        fade = 1-smooth((p-0.82)/0.18)   # fade whole intro out at end
        _ctext(d, W//2, H//2-70, "Ascend", font(96,800), "#ffffff", a1*fade)
        _ctext(d, W//2, H//2+10, "Land your next role, faster", font(38,500), (235,233,255), a2*fade)
        _ctext(d, W//2, H//2+120, "YOUR AI JOB-SEARCH COPILOT", font(26,700,mono=True), (168,155,255), a3*fade)
        return np.asarray(img)

    # ===== OUTRO =====
    if t > TOTAL-OUT:
        p = (t-(TOTAL-OUT))/OUT
        appear = out_cubic(clamp(p/0.4))
        ls=150
        img.paste(LOGO_BIG,(W//2-ls//2, int(H//2-250 - (1-appear)*40)),LOGO_BIG)
        _ctext(d, W//2, H//2-60, "Ascend", font(96,800), "#ffffff", appear)
        _ctext(d, W//2, H//2+18, "Land your next role, faster", font(38,500), (235,233,255), appear)
        b = out_back(clamp((p-0.35)/0.5))
        _play_badge(img, W//2, H//2+150, b)
        return np.asarray(img)

    # ===== PHONE SCENES =====
    lt = t - T0
    idx = min(len(PHONE)-1, int(lt//SCENE))
    local = lt - idx*SCENE
    # persistent top-left logo
    img.paste(LOGO_SM,(120,86),LOGO_SM)
    text(d,(200,104),"Ascend",font(34,800),"#ffffff",anchor="lm")
    text(d,(200,140),"Land your next role, faster",font(17,500),(210,208,235),anchor="lm")

    # phone entrance on first phone scene
    enter = out_cubic(clamp(lt/0.7)) if idx==0 else 1.0
    float_y = math.sin((lt)*1.1)*10
    py = PY + (1-enter)*240 + float_y
    _paste_phone(img, SCR[PHONE[idx][3]], PX, py,
                 cross=_crossfade_pair(idx, local))

    # caption (slide/fade per scene)
    cin = smooth(clamp(local/0.5))
    cout = 1-smooth(clamp((local-(SCENE-0.35))/0.35)) if idx<len(PHONE)-1 else 1.0
    ca = cin*cout
    dx = (1-cin)*60
    eb, head, sub, _, acc = PHONE[idx]
    cx0 = 120 - dx
    # measure block to vertically center it around ~52% height
    hl = wrap_lines(d, head, font(72,800), 780)
    sl = wrap_lines(d, sub, font(31,500), 740)
    block_h = 42 + 30 + len(hl)*84 + 14 + len(sl)*44
    top = int(H*0.52 - block_h/2)
    if ca>0.02:
        ew = tw(d,eb,font(22,800))+44
        _pill(d, cx0, top, ew, 42, acc, eb, ca)
        yy = top + 42 + 34
        for ln in hl:
            _ltext(d, cx0, yy, ln, font(72,800), "#ffffff", ca); yy+=84
        yy += 8
        for ln in sl:
            _ltext(d, cx0, yy, ln, font(31,500), (222,220,245), ca); yy+=44
    # progress dots
    _dots(d, 120, H-110, idx)
    return np.asarray(img)

def _crossfade_pair(idx, local):
    """returns (prev_img, alpha) to blend into current during scene-in."""
    if idx>0 and local<TRANS:
        a = smooth(local/TRANS)
        return (SCR[PHONE[idx-1][3]], 1-a)
    return None

def _paste_phone(img, screen_img, cx, cy, cross=None):
    x = int(cx-OW//2); y=int(cy-OH//2)
    img.paste(SHADOW_BASE,(x-80,y-80),SHADOW_BASE)
    img.paste(SHELL,(x,y),SHELL)
    if cross is not None:
        prev,a = cross
        screen_img = Image.blend(screen_img, prev, a) if False else Image.blend(prev,screen_img,1-a)
    img.paste(screen_img,(x+PADP,y+PADP),INNER_MASK)

def _ctext(d,cx,cy,s,f,col,a):
    if a<=0.01: return
    col = col if isinstance(col,tuple) else _rgb(col)
    d.text((cx,cy),s,font=f,fill=col+(int(255*clamp(a)),),anchor="mm")
def _ltext(d,x,y,s,f,col,a):
    if a<=0.01: return
    col = col if isinstance(col,tuple) else _rgb(col)
    d.text((x,y),s,font=f,fill=col+(int(255*clamp(a)),),anchor="lm")

def _pill(d,x,y,w,h,col,label,a):
    c=_rgb(col)
    d.rounded_rectangle([x,y,x+w,y+h],radius=h//2,fill=c+(int(235*a),))
    fg=(20,18,40) if col in ("#ffd66b","#34d17f","#a89bff","#ff8fb1") else (255,255,255)
    d.text((x+22,y+h/2),label,font=font(22,800),fill=fg+(int(255*a),),anchor="lm")

def _dots(d,x,y,idx):
    for i in range(len(PHONE)):
        on = i==idx
        w = 34 if on else 12
        col=(255,255,255,235) if on else (255,255,255,80)
        d.rounded_rectangle([x,y,x+w,y+10],radius=5,fill=col); x+=w+10

def _play_badge(img,cx,cy,p):
    if p<=0.02: return
    bw,bh=470,116
    x=int(cx-bw//2); y=int(cy-bh//2)
    badge=Image.new("RGBA",(bw,bh),(0,0,0,0)); bd=ImageDraw.Draw(badge)
    bd.rounded_rectangle([0,0,bw-1,bh-1],radius=22,fill=(0,0,0,255),outline=(120,120,140,255),width=2)
    # play triangle (multicolor-ish)
    tx,ty=44,bh//2
    bd.polygon([(tx,ty-30),(tx,ty+30),(tx+52,ty)],fill=(0,0,0,0))
    for col,off in [((0,224,255),0)]:
        bd.polygon([(tx,ty-30),(tx,ty+30),(tx+50,ty)],fill=(38,166,255))
        bd.polygon([(tx,ty-30),(tx+25,ty-15),(tx,ty)],fill=(0,224,182))
        bd.polygon([(tx,ty),(tx+25,ty+15),(tx,ty+30)],fill=(255,99,99))
        bd.polygon([(tx+25,ty-15),(tx+50,ty),(tx+25,ty+15)],fill=(255,196,55))
    bd.text((125,bh//2-22),"GET IT ON",font=font(20,600),fill=(190,190,200),anchor="lm")
    bd.text((123,bh//2+10),"Google Play",font=font(38,700),fill=(255,255,255),anchor="lm")
    if p<1:
        badge=badge.resize((max(1,int(bw*p)),max(1,int(bh*p))),Image.LANCZOS)
        x=int(cx-badge.size[0]//2); y=int(cy-badge.size[1]//2)
    img.paste(badge,(x,y),badge)

def main():
    out = "out/ascend_feature_1080p.mp4"
    nframes = int(TOTAL*FPS)
    writer = imageio_ffmpeg.write_frames(
        out,(W,H),fps=FPS,codec="libx264",quality=7,
        macro_block_size=8,
        output_params=["-pix_fmt","yuv420p","-movflags","+faststart","-profile:v","high"])
    writer.send(None)
    for i in range(nframes):
        f = frame(i/FPS)
        writer.send(np.ascontiguousarray(f))
        if i%60==0: print(f"frame {i}/{nframes}",flush=True)
    writer.close()
    print("DONE", out, f"{TOTAL:.1f}s {nframes} frames")

if __name__=="__main__":
    main()
