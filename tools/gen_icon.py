#!/usr/bin/env python3
"""Builds the launcher icons and the Play Store icon from store/logo.png into res/mipmap-*."""
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..")
RES = os.path.join(ROOT, "app", "src", "main", "res")
BG = (38, 14, 44)  # matches the logo's own backdrop, see colors.xml icon_background
SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
SAFE = 0.74  # the adaptive icon mask keeps the centre two thirds, so the logo sits a little inside it

logo = Image.open(os.path.join(ROOT, "store", "logo.png")).convert("RGB")


def foreground(size):
    """The logo centred on a transparent 108dp-style canvas (size is the launcher size * 108/48)."""
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    inner = int(size * SAFE)
    art = logo.resize((inner, inner), Image.LANCZOS).convert("RGBA")
    canvas.paste(art, ((size - inner) // 2, (size - inner) // 2))
    return canvas


for name, px in SIZES.items():
    d = os.path.join(RES, "mipmap-" + name)
    os.makedirs(d, exist_ok=True)
    logo.resize((px, px), Image.LANCZOS).save(os.path.join(d, "ic_launcher.png"))
    foreground(px * 108 // 48).save(os.path.join(d, "ic_launcher_foreground.png"))

logo.resize((512, 512), Image.LANCZOS).save(os.path.join(ROOT, "store", "play-store-icon-512.png"))
print("icons written")
