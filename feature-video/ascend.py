"""Ascend — faithful UI toolkit for the launch video.
Recreates the Ascend app design system (indigo #4f46e5, Plus Jakarta Sans,
Material Symbols) as Pillow render primitives, then builds the key screens.
"""
import os
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
FDIR = os.path.join(HERE, "fonts")

# ---- design tokens ----
INDIGO   = "#4f46e5"
INDIGO2  = "#6d5cf0"
VIOLET   = "#8b5cf6"
VIOLET2  = "#7c5cff"
INK      = "#15151c"
INK2     = "#26262f"
BODY     = "#4a4a57"
MUTE     = "#6b6b78"
MUTE2    = "#9a9aa6"
FAINT    = "#c3c3cd"
BG       = "#f5f5f8"
CARD     = "#ffffff"
LINE     = "#ececf2"
LINE2    = "#f0f0f5"
CHIP     = "#eceaff"
GREEN    = "#0f9d68"
GREEN_BG = "#eafaf2"
RED      = "#e84848"
DARK     = "#0e0e14"

# device
DW, DH = 412, 888   # logical app viewport (inside the phone)

_pj = ImageFont.truetype(os.path.join(FDIR, "PlusJakartaSans.ttf"), 40)
_jb = ImageFont.truetype(os.path.join(FDIR, "JetBrainsMono.ttf"), 40)
_ms = ImageFont.truetype(os.path.join(FDIR, "MaterialSymbols.ttf"), 40, layout_engine=ImageFont.Layout.RAQM)

_cache = {}

def font(size, weight=600, mono=False):
    key = ("m" if mono else "p", round(size), weight)
    f = _cache.get(key)
    if f is None:
        path = os.path.join(FDIR, "JetBrainsMono.ttf" if mono else "PlusJakartaSans.ttf")
        f = ImageFont.truetype(path, round(size))
        try: f.set_variation_by_axes([weight])
        except Exception: pass
        _cache[key] = f
    return f

def icon_font(size, fill=0, weight=400):
    key = ("ms", round(size), fill, weight)
    f = _cache.get(key)
    if f is None:
        f = ImageFont.truetype(os.path.join(FDIR, "MaterialSymbols.ttf"),
                               round(size), layout_engine=ImageFont.Layout.RAQM)
        # axis order in this font: FILL, GRAD, opsz, wght
        try: f.set_variation_by_axes([float(fill), 0.0, float(round(size)), float(weight)])
        except Exception:
            try: f.set_variation_by_axes([float(fill), 0.0, 48.0, float(weight)])
            except Exception: pass
        _cache[key] = f
    return f

# ---- primitives ----
def rrect(d, box, r, fill=None, outline=None, width=1):
    d.rounded_rectangle(box, radius=r, fill=fill, outline=outline, width=width)

def text(d, xy, s, f, fill, anchor="la"):
    d.text(xy, s, font=f, fill=fill, anchor=anchor)

def icon(d, xy, name, size, color, fill=0, weight=400, anchor="lm"):
    d.text(xy, name, font=icon_font(size, fill, weight), fill=color, anchor=anchor)

def tw(d, s, f):
    return d.textlength(s, font=f)

def vgrad(box, c0, c1, angle=160):
    """vertical-ish linear gradient as an RGBA tile."""
    import math
    x0,y0,x1,y1 = box
    w,h = int(x1-x0), int(y1-y0)
    img = Image.new("RGB",(w,h))
    px = img.load()
    a = math.radians(angle-90)
    dx,dy = math.cos(a), math.sin(a)
    c0 = _rgb(c0); c1=_rgb(c1)
    # projection range
    import itertools
    proj=[(x*dx+y*dy) for x,y in ((0,0),(w,0),(0,h),(w,h))]
    lo,hi=min(proj),max(proj)
    for y in range(h):
        for x in range(w):
            t=((x*dx+y*dy)-lo)/(hi-lo+1e-6)
            px[x,y]=tuple(int(c0[i]+(c1[i]-c0[i])*t) for i in range(3))
    return img

def _rgb(c):
    c=c.lstrip("#"); return tuple(int(c[i:i+2],16) for i in (0,2,4))

def paste_grad(base, box, c0, c1, angle=160, radius=0):
    g = vgrad(box, c0, c1, angle).convert("RGBA")
    if radius:
        m = Image.new("L", g.size, 0)
        ImageDraw.Draw(m).rounded_rectangle([0,0,g.size[0]-1,g.size[1]-1], radius=radius, fill=255)
        base.paste(g, (int(box[0]),int(box[1])), m)
    else:
        base.paste(g, (int(box[0]),int(box[1])))

def new_screen(bg=BG):
    img = Image.new("RGB", (DW, DH), bg)
    return img, ImageDraw.Draw(img)

def status_bar(d, color=INK):
    text(d, (26, 22), "9:41", font(14, 600, mono=True), color, anchor="lm")
    icon(d, (DW-92, 22), "signal_cellular_alt", 17, color)
    icon(d, (DW-64, 22), "wifi", 17, color)
    icon(d, (DW-30, 22), "battery_full", 17, color, anchor="rm")

def logo_mark(d, cx, cy, s, color="#ffffff", op2=0.55):
    """ascending double-chevron logo."""
    import math
    def chev(oy, col):
        w = s*0.58; h = s*0.30
        pts1 = [(cx-w/2, cy+oy+h/2),(cx, cy+oy-h/2),(cx+w/2, cy+oy+h/2)]
        d.line(pts1, fill=col, width=max(2,int(s*0.105)), joint="curve")
    chev(-s*0.12, color)
    # faint second chevron
    fade = tuple(int(c) for c in _rgb(color)) + (int(255*op2),)
    chev(s*0.10, fade)

NAV = [("home","Home"),("work","Jobs"),("dashboard","Tracker"),("graphic_eq","Interviews")]

def nav_bar(img, active=0):
    d = ImageDraw.Draw(img, "RGBA")
    y0 = DH-78
    d.rectangle([0,y0,DW,DH], fill=(255,255,255,245))
    d.line([0,y0,DW,y0], fill=_rgb(LINE)+(255,), width=1)
    n=len(NAV); seg=DW/n
    for i,(ic,lb) in enumerate(NAV):
        cx=seg*i+seg/2
        col = INDIGO if i==active else MUTE2
        icon(d,(cx, y0+26),ic,25,col,fill=1 if i==active else 0,
             weight=500 if i==active else 400, anchor="mm")
        text(d,(cx, y0+52),lb,font(11,600),col,anchor="mm")
    # gesture bar
    d.rounded_rectangle([DW/2-60, DH-14, DW/2+60, DH-10], radius=2, fill=(20,20,30,210))
