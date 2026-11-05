#!/usr/bin/env python3
"""Draws the launcher icon (a chunky gold B letter tile on a night sky) into res/mipmap-*."""
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res")
BG, BG2, STAR = (20, 16, 43), (37, 29, 74), (167, 155, 214)
INK, GOLD, LIGHT, DARK = (11, 8, 32), (255, 211, 77), (255, 241, 176), (196, 138, 18)
B = ["#####.", "##..##", "##..##", "#####.", "##..##", "##..##", "#####."]


def art(foreground_only):
    n = 36
    im = Image.new("RGBA", (n, n), (0, 0, 0, 0) if foreground_only else BG + (255,))
    px = im.load()
    if not foreground_only:
        for y in range(n):
            for x in range(n):
                t = y / n + (0.12 if (x + y) % 2 else 0)  # dithered sky
                px[x, y] = (BG2 if t > 0.55 else BG) + (255,)
        for x, y in [(3, 4), (31, 6), (5, 30), (32, 28), (29, 2), (2, 18)]:
            px[x, y] = STAR + (255,)
    l, t, r, b = 9, 8, 27, 28  # tile
    for y in range(t, b):
        for x in range(l, r):
            if x in (l, r - 1) and y in (t, b - 1):
                continue  # notched corner
            edge = x in (l, r - 1) or y in (t, b - 1)
            c = INK if edge else GOLD
            if not edge and (y == t + 1 or x == l + 1):
                c = LIGHT
            if not edge and (y >= b - 3 or x == r - 2):
                c = DARK
            px[x, y] = c + (255,)
    for y, row in enumerate(B):
        for x, ch in enumerate(row):
            if ch == "#":
                for dy in range(2):
                    px[15 + x, 10 + y * 2 + dy] = INK + (255,)
    return im


SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
for name, size in SIZES.items():
    d = os.path.join(ROOT, "mipmap-" + name)
    os.makedirs(d, exist_ok=True)
    art(False).resize((size, size), Image.NEAREST).save(os.path.join(d, "ic_launcher.png"))
    # adaptive foreground is 108dp with the icon inside the 66dp safe zone
    fg = int(size * 108 / 48)
    canvas = Image.new("RGBA", (fg, fg), (0, 0, 0, 0))
    inner = int(fg * 0.72)
    glyph = art(True).resize((inner, inner), Image.NEAREST)
    canvas.paste(glyph, ((fg - inner) // 2, (fg - inner) // 2), glyph)
    canvas.save(os.path.join(d, "ic_launcher_foreground.png"))
print("icons written")
