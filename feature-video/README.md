# Ascend — Google Play feature video

A 30-second promotional/feature video for the **Ascend** job-search app
(`Ascend.dc.html`), rendered programmatically from the app's own design system.

## Output
- **`ascend_feature_1080p.mp4`** — 1920×1080 (16:9), 30.0s, 30 fps, H.264 / yuv420p.

16:9 is the correct aspect ratio for the Google Play promo video slot — Play
hosts it as a YouTube link, which must be 16:9, and recommends 30s–2min.

## What it shows
Brand intro → the five key flows → store CTA, following a
Hook → Reveal → Highlights → Outro structure:

1. **The app** — home / reveal
2. **Find jobs** — AI-matched roles ranked by fit
3. **Tracker** — Saved → Applied → Interview → Offer board
4. **Resume** — ATS optimizer (score 93)
5. **Prep** — mock interview with scored feedback
6. **Live Copilot** — real-time interview navigator

Screens are faithful recreations of the actual app UI (indigo `#4f46e5`,
Plus Jakarta Sans, Material Symbols, real seed data: Alex Morgan / Senior PM /
Northwind, Tempo, Ledgerline …).

## Regenerate
```bash
pip install Pillow imageio-ffmpeg numpy
./fetch_fonts.sh        # brand fonts (not committed)
python build.py         # writes out/ascend_feature_1080p.mp4
```

- `ascend.py` — design tokens, fonts, primitives, phone chrome
- `screens.py` — the six app screens
- `build.py` — 16:9 compositor, timeline, animation, encode

## Music
The video uses **"Happy Beats / Business Moves Vol. 1" by [ende.app](https://ende.app/en)**
(120 BPM), the curated track bundled with the `/brag` skill — normalized to
-15 LUFS with a 0.5s fade-in and 1.7s fade-out. The raw mp3 is fetched by
`fetch_fonts.sh` and is not committed.

> **License note:** verify and document the exact ende.app "Business Moves"
> license terms before publishing the video to the Play Store. Swap in your own
> licensed track by replacing `music/business-moves-vol1.mp3` and re-running the
> mux step (see commit/build notes).
