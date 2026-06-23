#!/usr/bin/env bash
# Brand fonts used to render the video (not committed; ~10MB Material Symbols).
set -e
mkdir -p fonts
curl -fsSL "https://raw.githubusercontent.com/google/fonts/main/ofl/plusjakartasans/PlusJakartaSans%5Bwght%5D.ttf" -o fonts/PlusJakartaSans.ttf
curl -fsSL "https://raw.githubusercontent.com/google/fonts/main/ofl/jetbrainsmono/JetBrainsMono%5Bwght%5D.ttf" -o fonts/JetBrainsMono.ttf
curl -fsSL "https://raw.githubusercontent.com/google/material-design-icons/master/variablefont/MaterialSymbolsOutlined%5BFILL%2CGRAD%2Copsz%2Cwght%5D.ttf" -o fonts/MaterialSymbols.ttf
echo "Fonts downloaded to ./fonts"

# Music (brag skill's curated track, used in the video):
mkdir -p music
curl -fsSL "https://raw.githubusercontent.com/latent-spaces/brag/main/skills/brag/assets/music/happy-beats-business-moves-vol-1-by-ende-dot-app.mp3" -o music/business-moves-vol1.mp3
echo "Music downloaded to ./music"
