"""The five key Ascend flows, rendered faithfully as 412x888 phone screens."""
from ascend import *
from ascend import _rgb
from PIL import Image, ImageDraw

PAD = 18

def _avatar(d, x, y, s, color, ini, fs):
    rrect(d, [x, y, x+s, y+s], int(s*0.28), fill=color)
    text(d, (x+s/2, y+s/2), ini, font(fs, 800), "#ffffff", anchor="mm")

def _chip(d, x, y, label, fg=MUTE, bg=BG, fs=11.5, dot=None, pad=9, h=24):
    w = tw(d, label, font(fs,600)) + pad*2 + (12 if dot else 0)
    rrect(d, [x, y, x+w, y+h], 8, fill=bg)
    tx = x+pad
    if dot:
        d.ellipse([tx, y+h/2-3, tx+6, y+h/2+3], fill=dot); tx+=11
    text(d, (tx, y+h/2), label, font(fs,600), fg, anchor="lm")
    return w

def _heading(d, y, title, sub=None):
    text(d, (PAD, y), title, font(22,800), INK, anchor="lm")
    if sub:
        text(d, (PAD, y+22), sub, font(13,600), MUTE2, anchor="lm")

# ───────────────────────── HOME ─────────────────────────
def home():
    img,d = new_screen(BG); status_bar(d); y=58
    text(d,(PAD,y+4),"Good morning",font(13,600),MUTE2,anchor="lm")
    text(d,(PAD,y+24),"Alex Morgan",font(22,800),INK,anchor="lm")
    icon(d,(DW-66,y+16),"notifications",25,"#5c5c6b")
    rrect(d,[DW-58,y,DW-18,y+40],20,fill=INDIGO); text(d,(DW-38,y+20),"A",font(14,700),"#fff",anchor="mm")
    # search bar
    y+=58; rrect(d,[PAD,y,DW-PAD,y+52],16,fill=CARD,outline=LINE,width=2)
    icon(d,(PAD+16,y+26),"search",23,MUTE2); text(d,(PAD+44,y+26),"Search jobs, titles, companies",font(15,500),"#b3b3bd",anchor="lm")
    # quick actions
    y+=72; text(d,(PAD,y),"Quick actions",font(15,800),INK,anchor="lm")
    qa=[("auto_fix_high","Optimize resume","Beat the ATS","#15151c","#a89bff","#1d1d28","#fff"),
        ("description","Build resume","AI writes it","#fff","#4f46e5",CHIP,INK),
        ("record_voice_over","Mock interview","Practice + score","#fff","#4f46e5","#eafaf2",INK),
        ("bolt","Live Copilot","Real-time answers","#fff","#7c5cff","#f0ecff",INK)]
    y+=14; cw=(DW-PAD*2-12)/2; ch=120
    for i,(ic,lb,sb,bg,icc,icbg,fg) in enumerate(qa):
        cx=PAD+(i%2)*(cw+12); cy=y+(i//2)*(ch+12)
        rrect(d,[cx,cy,cx+cw,cy+ch],20,fill=bg,outline=LINE if bg=="#fff" else None,width=1)
        rrect(d,[cx+15,cy+15,cx+15+42,cy+15+42],13,fill=icbg)
        icon(d,(cx+15+21,cy+15+21),ic,23,icc,fill=1,anchor="mm")
        text(d,(cx+15,cy+ch-42),lb,font(15.5,800),fg,anchor="lm")
        text(d,(cx+15,cy+ch-20),sb,font(11.5,500),(MUTE2 if bg=="#fff" else "#b9b9c8"),anchor="lm")
    # top matches
    y+=ch*2+12+22; text(d,(PAD,y),"Top matches",font(17,800),INK,anchor="lm")
    text(d,(DW-PAD,y),"See all",font(13,700),INDIGO,anchor="rm")
    y+=20; text(d,(PAD,y),"for Senior Product Manager · San Francisco",font(13,500),MUTE2,anchor="lm")
    jobs=[("Senior Product Manager","Northwind · Remote","N",INDIGO,96,"Remote",GREEN),
          ("Principal PM, AI","Hollow · Seattle, WA","H",VIOLET2,88,"Hybrid","#e0913f")]
    y+=16
    for t,co,ini,c1,m,work,wc in jobs:
        h=92; rrect(d,[PAD,y,DW-PAD,y+h],18,fill=CARD,outline=LINE,width=2)
        _avatar(d,PAD+15,y+15,46,c1,ini,18)
        text(d,(PAD+73,y+22),t,font(15.5,700),INK,anchor="lm")
        text(d,(PAD+73,y+42),co,font(13,500),MUTE,anchor="lm")
        bw=tw(d,f"{m}%",font(12,800,mono=True))+16
        rrect(d,[DW-PAD-15-bw,y+16,DW-PAD-15,y+16+22],11,fill=GREEN_BG)
        text(d,(DW-PAD-15-bw/2,y+27),f"{m}%",font(12,800,mono=True),GREEN,anchor="mm")
        text(d,(DW-PAD-15,y+48),"match",font(11,500),MUTE2,anchor="rm")
        _chip(d,PAD+15,y+58,work,wc,BG,dot=wc)
        _chip(d,PAD+15+78,y+58,"Full-time",MUTE)
        y+=h+12
    nav_bar(img,0); return img

# ───────────────────────── SEARCH / FIND JOBS ─────────────────────────
def search():
    img,d = new_screen(BG); status_bar(d); y=58
    text(d,(PAD,y),"Find your next role",font(22,800),INK,anchor="lm")
    y+=36; rrect(d,[PAD,y,DW-PAD,y+50],14,fill=CARD,outline=LINE,width=2)
    icon(d,(PAD+14,y+25),"search",22,MUTE2); text(d,(PAD+42,y+25),"Product Manager",font(15,600),INK,anchor="lm")
    # loc + filter
    y+=60; fw=DW-PAD*2
    locw=fw*0.62
    rrect(d,[PAD,y,PAD+locw,y+44],12,fill=CARD,outline=LINE,width=2)
    icon(d,(PAD+13,y+22),"location_on",19,INDIGO); text(d,(PAD+34,y+22),"San Francisco · Remote",font(13.5,600),INK,anchor="lm")
    rrect(d,[PAD+locw+8,y,DW-PAD,y+44],12,fill=CHIP,outline="#d8d0fb",width=2)
    icon(d,(PAD+locw+24,y+22),"tune",19,INDIGO); text(d,(PAD+locw+44,y+22),"Filters",font(13.5,700),INDIGO,anchor="lm")
    rrect(d,[DW-PAD-26,y+13,DW-PAD-9,y+30],9,fill=INDIGO); text(d,(DW-PAD-17.5,y+21),"2",font(10,800),"#fff",anchor="mm")
    y+=58; text(d,(PAD,y),"248 JOBS · SORTED BY MATCH",font(12,600,mono=True),MUTE2,anchor="lm")
    jobs=[("Senior Product Manager","Northwind · Remote · US","N",INDIGO,96,"Remote",GREEN,"$155k – $185k","2d ago"),
          ("Product Manager, Growth","Tempo · San Francisco","T",GREEN,91,"Hybrid","#e0913f","$140k – $165k","1d ago"),
          ("Group Product Manager","Ledgerline · New York, NY","L","#e0913f",84,"On-site","#2563eb","$185k – $215k","4d ago")]
    y+=20
    for t,co,ini,c1,m,work,wc,pay,posted in jobs:
        h=128; rrect(d,[PAD,y,DW-PAD,y+h],18,fill=CARD,outline=LINE,width=2)
        _avatar(d,PAD+15,y+15,46,c1,ini,18)
        text(d,(PAD+73,y+22),t,font(15.5,700),INK,anchor="lm")
        text(d,(PAD+73,y+42),co,font(13,500),MUTE,anchor="lm")
        icon(d,(DW-PAD-30,y+27),"bookmark",23,MUTE2)
        x=PAD+15; x+=_chip(d,x,y+62,work,wc,BG,dot=wc)+7
        x+=_chip(d,x,y+62,"Full-time",MUTE)+7
        _chip(d,x,y+62,pay,MUTE)
        d.line([PAD+15,y+98,DW-PAD-15,y+98],fill=LINE2,width=1)
        icon(d,(PAD+18,y+112),"schedule",15,MUTE2); text(d,(PAD+34,y+112),posted,font(12,500),MUTE2,anchor="lm")
        bw=tw(d,f"{m}% match",font(11.5,800,mono=True))+16
        rrect(d,[DW-PAD-15-bw,y+101,DW-PAD-15,y+123],11,fill=GREEN_BG)
        text(d,(DW-PAD-15-bw/2,y+112),f"{m}% match",font(11.5,800,mono=True),GREEN,anchor="mm")
        y+=h+12
    nav_bar(img,1); return img

# ───────────────────────── JOB TRACKER ─────────────────────────
def tracker():
    img,d = new_screen(BG); status_bar(d); y=58
    _heading(d,y,"Job Tracker","12 jobs · 7 active"); y+=44
    stats=[("5","Saved","#6b6b78"),("4","Applied","#2563eb"),("2","Interview","#7c5cff"),("1","Offer","#0f9d68"),("0","Closed","#9a9aa6")]
    x=PAD; sw=(DW-PAD*2-7*4)/5
    for cnt,lb,c in stats:
        rrect(d,[x,y,x+sw,y+54],14,fill=CARD,outline=LINE,width=2)
        text(d,(x+sw/2,y+20),cnt,font(20,800,mono=True),c,anchor="mm")
        text(d,(x+sw/2,y+40),lb,font(10,700),MUTE2,anchor="mm")
        x+=sw+7
    y+=74
    cols=[("APPLIED","#2563eb",4,[("Product Manager, Growth","Tempo · Hybrid","T",GREEN),
                                   ("Technical Product Manager","Cargo · On-site","C","#5b6470")]),
          ("INTERVIEW","#7c5cff",2,[("Senior Product Manager","Northwind · Remote","N",INDIGO),
                                     ("Principal PM, AI","Hollow · Hybrid","H",VIOLET2)])]
    for name,c,cnt,jobs in cols:
        d.rounded_rectangle([PAD,y+1,PAD+9,y+10],radius=3,fill=c)
        text(d,(PAD+18,y+6),name,font(13,800),INK,anchor="lm")
        cl=tw(d,str(cnt),font(12,700))+16
        rrect(d,[PAD+18+tw(d,name,font(13,800))+8,y-2,PAD+18+tw(d,name,font(13,800))+8+cl,y+16],9,fill=LINE)
        text(d,(PAD+18+tw(d,name,font(13,800))+8+cl/2,y+7),str(cnt),font(12,700),MUTE2,anchor="mm")
        text(d,(DW-PAD,y+6),"View all",font(12,700),INDIGO,anchor="rm")
        y+=24
        for t,co,ini,c1 in jobs:
            h=92; rrect(d,[PAD,y,DW-PAD,y+h],16,fill=CARD,outline=LINE,width=2)
            d.rounded_rectangle([PAD,y+1,PAD+3,y+h-1],radius=2,fill=c)
            _avatar(d,PAD+12,y+12,42,c1,ini,16)
            text(d,(PAD+64,y+22),t,font(14.5,700),INK,anchor="lm")
            text(d,(PAD+64,y+42),co,font(12,500),MUTE2,anchor="lm")
            d.line([PAD+12,y+62,DW-PAD-12,y+62],fill=LINE2,width=1)
            rrect(d,[PAD+12,y+70,PAD+46,y+70+22],10,fill="#f1f0f5"); icon(d,(PAD+29,y+81),"arrow_downward",18,"#5c5c6b",anchor="mm")
            sbw=DW-PAD-12-46-7-(PAD+12+34+7)
            sx=PAD+12+34+7
            rrect(d,[sx,y+70,DW-PAD-12-46-7,y+70+22],10,fill="#efeaff")
            text(d,((sx+DW-PAD-12-46-7)/2-8,y+81),name.title(),font(12.5,800),c,anchor="mm")
            icon(d,((sx+DW-PAD-12-46-7)/2+22,y+81),"unfold_more",16,c,anchor="mm")
            rrect(d,[DW-PAD-12-34,y+70,DW-PAD-12,y+70+22],10,fill="#efeaff"); icon(d,(DW-PAD-12-17,y+81),"arrow_upward",18,c,anchor="mm")
            y+=h+9
        y+=14
    nav_bar(img,2); return img

# ───────────────────────── RESUME OPTIMIZER ─────────────────────────
def optimizer():
    img,d = new_screen(BG); status_bar(d)
    rrect(d,[0,44,DW,90],0,fill=CARD); d.line([0,90,DW,90],fill=LINE,width=1)
    icon(d,(PAD,67),"arrow_back",25,INK); text(d,(PAD+34,67),"Resume Optimizer",font(17,800),INK,anchor="lm")
    y=110
    # score ring card
    rrect(d,[PAD,y,DW-PAD,y+126],20,fill=CARD,outline=LINE,width=2)
    cx,cy,r=PAD+58,y+63,44
    _ring(img,cx,cy,r,12,"#eef0f4",None)
    _ring(img,cx,cy,r,12,GREEN,0.93)
    d=ImageDraw.Draw(img)
    text(d,(cx,cy-6),"93",font(27,800,mono=True),INK,anchor="mm")
    text(d,(cx,cy+16),"ATS",font(9,700),MUTE2,anchor="mm")
    text(d,(PAD+118,y+44),"Strong — ready to apply",font(15,800),INK,anchor="lm")
    _wrap(d,PAD+118,y+66,DW-PAD-16,"Tailored to Northwind's Senior PM role. Keywords matched.",font(13,500),MUTE,18)
    y+=146
    text(d,(PAD,y),"WHAT WE FIXED",font(13,700,mono=True),MUTE2,anchor="lm"); y+=22
    issues=[("verified","Keyword match raised to 93%","Resolved",GREEN,GREEN),
            ("check_circle","Added 6 missing role keywords","Resolved",GREEN,GREEN),
            ("bolt","Quantified 4 impact bullets","Resolved",GREEN,GREEN),
            ("description","ATS-safe formatting applied","Resolved",GREEN,GREEN)]
    for ic,t,sev,icc,dot in issues:
        h=66; rrect(d,[PAD,y,DW-PAD,y+h],16,fill=CARD,outline=LINE,width=2)
        icon(d,(PAD+16,y+h/2),ic,22,icc,fill=1)
        text(d,(PAD+44,y+22),t,font(14.5,700),INK,anchor="lm")
        d.ellipse([PAD+44,y+40,PAD+50,y+46],fill=dot)
        text(d,(PAD+56,y+43),sev,font(10.5,700),MUTE2,anchor="lm")
        y+=h+10
    # optimized banner
    rrect(d,[PAD,y,DW-PAD,y+58],16,fill=GREEN_BG,outline="#b6e8cf",width=2)
    icon(d,(PAD+20,y+29),"verified",26,GREEN,fill=1)
    text(d,(PAD+44,y+20),"Optimized to 93",font(14.5,800),"#0a7a50",anchor="lm")
    text(d,(PAD+44,y+40),"Ready to download or apply.",font(12.5,500),"#0a7a50",anchor="lm")
    y+=70
    rrect(d,[PAD,y,DW-PAD,y+54],16,fill=INK)
    icon(d,(DW/2-58,y+27),"download",20,"#fff"); text(d,(DW/2-40,y+27),"Download resume",font(16,700),"#fff",anchor="lm")
    return img

# ───────────────────────── MOCK INTERVIEW ─────────────────────────
def mock():
    img,d = new_screen(BG); status_bar(d)
    rrect(d,[0,44,DW,108],0,fill=CARD); d.line([0,108,DW,108],fill=LINE,width=1)
    text(d,(PAD,66),"Question 3 of 5",font(13,700,mono=True),MUTE2,anchor="lm")
    text(d,(DW-PAD-92,66),"04:12",font(13,700,mono=True),INK,anchor="rm")
    rrect(d,[DW-PAD-78,56,DW-PAD,80],12,fill="#fdeaea"); text(d,(DW-PAD-39,68),"End",font(12.5,700),RED,anchor="mm")
    rrect(d,[PAD,94,DW-PAD,99],3,fill=LINE); rrect(d,[PAD,94,PAD+(DW-PAD*2)*0.6,99],3,fill=INDIGO)
    y=128
    rrect(d,[PAD,y,PAD+30,y+30],9,fill=INK); text(d,(PAD+15,y+15),"3",font(14,800,mono=True),"#fff",anchor="mm")
    text(d,(PAD+40,y+15),"Senior PM Interview",font(13,600),MUTE2,anchor="lm")
    y+=44
    _chip(d,PAD,y,"System Design",INDIGO,CHIP,11,pad=10,h=24)
    _chip(d,PAD+tw(d,"System Design",font(11,700))+27,y,"Hard","#a86a12","#fff7e8",11,pad=10,h=24)
    y+=34
    _wrap(d,PAD,y,DW-PAD,"Design a system to handle 40k requests per second with low latency.",font(20,800),INK,28)
    y+=78
    rrect(d,[PAD,y,(DW-PAD-8)/2+PAD/2,y+42],11,fill=CARD,outline=INDIGO,width=2)
    icon(d,((DW-8)/4+PAD/2-30,y+21),"keyboard",18,INDIGO); text(d,((DW-8)/4+PAD/2-10,y+21),"Type",font(14,700),INDIGO,anchor="lm")
    rrect(d,[(DW-PAD-8)/2+PAD/2+8,y,DW-PAD,y+42],11,fill=CARD,outline=LINE,width=2)
    icon(d,(DW*0.74,y+21),"mic",18,MUTE); text(d,(DW*0.74+22,y+21),"Speak",font(14,700),MUTE,anchor="lm")
    y+=54
    rrect(d,[PAD,y,DW-PAD,y+150],14,fill=CARD,outline=LINE,width=2)
    ans=("I'd clarify scale and consistency needs first, then sketch a "
         "consistent-hashing router with read replicas and automatic failover "
         "to cut p99 latency by 35%.")
    _wrap(d,PAD+14,y+18,DW-PAD-14,ans,font(14,500),INK2,22)
    text(d,(DW-PAD-14,y+132),"182 characters",font(11,600),"#b3b3bd",anchor="rm")
    y+=164
    rrect(d,[PAD,y,PAD+96,y+52],14,fill="#f1f0f5"); text(d,(PAD+48,y+26),"Skip",font(15,700),"#5c5c6b",anchor="mm")
    rrect(d,[PAD+106,y,DW-PAD,y+52],14,fill=INDIGO); text(d,((PAD+106+DW-PAD)/2,y+26),"Submit answer",font(15.5,800),"#fff",anchor="mm")
    return img

# ───────────────────────── LIVE NAVIGATOR (COPILOT) ─────────────────────────
def navigator():
    img,d = new_screen(DARK);
    status_bar(d,"#fff")
    d.line([0,58,DW,58],fill="#20202c",width=1)
    text(d,(PAD,76),"Senior Product Manager",font(14,800),"#fff",anchor="lm")
    text(d,(PAD+tw(d,"Senior Product Manager",font(14,800))+4,76)," @ Northwind",font(14,600),"#8a8a99",anchor="lm")
    d.ellipse([PAD,92,PAD+7,99],fill="#34d17f"); text(d,(PAD+13,96),"Listening · live transcription",font(11,500),"#8a8a99",anchor="lm")
    text(d,(DW-PAD-78,80),"08:24",font(13,700,mono=True),"#a89bff",anchor="rm")
    rrect(d,[DW-PAD-66,70,DW-PAD,92],11,fill=RED); icon(d,(DW-PAD-52,81),"call_end",16,"#fff"); text(d,(DW-PAD-36,81),"End",font(13,700),"#fff",anchor="lm")
    y=120
    text(d,(PAD,y),"DETECTED QUESTIONS",font(11,700),"#6a6a7a",anchor="lm")
    rrect(d,[DW-PAD-44,y-9,DW-PAD,y+9],9,fill="#1a1a24"); text(d,(DW-PAD-22,y),"3",font(11,700,mono=True),"#a89bff",anchor="mm")
    y+=22
    qs=[("Q1","How would you scale a service to 40k rps?","#16161f","#23232f","#8a8a99",500),
        ("Q2","Walk me through a tradeoff you made.","#1a1838","#3a3470","#fff",700)]
    for t,q,bg,bd,fg,fw in qs:
        lines=_measure_lines(d,q,font(13,fw),DW-PAD*2-44)
        h=18+len(lines)*19
        rrect(d,[PAD,y,DW-PAD,y+h],13,fill=bg,outline=bd,width=1)
        text(d,(PAD+13,y+13),t,font(11,700,mono=True),"#a89bff",anchor="lm")
        _wrap(d,PAD+38,y+13,DW-PAD-13,q,font(13,fw),fg,19)
        y+=h+8
    # copilot answer card
    y+=4
    paste_grad(img,(PAD,y,DW-PAD,y+232),"#1a1838","#221d44",160,radius=18)
    d=ImageDraw.Draw(img); d.rounded_rectangle([PAD,y,DW-PAD,y+232],radius=18,outline="#3a3470",width=1)
    icon(d,(PAD+16,y+20),"bolt",16,"#a89bff"); text(d,(PAD+34,y+20),"COPILOT ANSWER",font(11,800),"#a89bff",anchor="lm")
    rrect(d,[DW-PAD-118,y+10,DW-PAD-58,y+30],10,fill="#2a2750"); icon(d,(DW-PAD-108,y+20),"content_copy",13,"#cfcfda"); text(d,(DW-PAD-92,y+20),"Copy",font(11,700),"#cfcfda",anchor="lm")
    rrect(d,[DW-PAD-50,y+10,DW-PAD,y+30],10,fill="#2a2750"); icon(d,(DW-PAD-25,y+20),"refresh",13,"#cfcfda",anchor="mm")
    secs=[("CONTEXT","At Tech Corp I owned the sharding and replication layer for a service at 40k rps."),
          ("WHAT I BUILT","A consistent-hashing router with automatic failover and read replicas — cut p99 latency 35%."),
          ("TRADE-OFF","Chose eventual consistency on reads to hit latency, with idempotency keys to keep writes safe.")]
    yy=y+42
    for lb,tx in secs:
        text(d,(PAD+16,yy),lb,font(11,800),"#8b78ec",anchor="lm"); yy+=16
        yy=_wrap(d,PAD+16,yy,DW-PAD-16,tx,font(12.5,500),"#e6e6f0",17)+6
    # listening footer
    fy=DH-44
    d.line([0,fy-12,DW,fy-12],fill="#20202c",width=1)
    bars=[8,16,12]; bx=PAD
    for i,bh in enumerate(bars):
        col=[INDIGO,INDIGO2,VIOLET][i]
        d.rounded_rectangle([bx,fy-bh/2,bx+3,fy+bh/2],radius=2,fill=col); bx+=7
    text(d,(bx+6,fy),"Listening for the interviewer…",font(12,600),"#8a8a99",anchor="lm")
    return img

# ───── helpers that need the image (rings, wrapping) ─────
def _ring(img,cx,cy,r,w,color,frac):
    import math
    sc=4
    big=Image.new("RGBA",(int(2*(r+w)*sc),)*2,(0,0,0,0)); dd=ImageDraw.Draw(big)
    o=(r+w)*sc; bb=[o-r*sc,o-r*sc,o+r*sc,o+r*sc]
    if frac is None:
        dd.arc(bb,0,360,fill=_rgb(color),width=int(w*sc))
    else:
        dd.arc(bb,-90,-90+360*frac,fill=_rgb(color),width=int(w*sc))
    big=big.resize((int(2*(r+w)),int(2*(r+w))),Image.LANCZOS)
    img.paste(big,(int(cx-(r+w)),int(cy-(r+w))),big)

def _measure_lines(d,s,f,maxw):
    words=s.split(); lines=[]; cur=""
    for w in words:
        t=(cur+" "+w).strip()
        if tw(d,t,f)<=maxw: cur=t
        else: lines.append(cur); cur=w
    if cur: lines.append(cur)
    return lines

def _wrap(d,x,y,xmax,s,f,fill,lh):
    for ln in _measure_lines(d,s,f,xmax-x):
        text(d,(x,y),ln,f,fill,anchor="lm"); y+=lh
    return y
