#!/usr/bin/env python3
"""Draws the cat's room and its furniture into app/src/main/assets/home/*.png.

Sizes must match Home.Slot in core: a window is 36x36, wall art 28x24, a rug 56x24, a plant 20x36,
a bed 48x32 and a toy 20x20. The room itself is 120x120.
Run:  python tools/gen_home.py      (needs Pillow)
      python tools/gen_home.py sheet.png   also writes a preview of a furnished room and every item
"""
import math
import os
import sys

from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets", "home")

INK = (11, 8, 32, 255)
CREAM = (255, 241, 214, 255)
WHITE = (255, 255, 255, 255)
GOLD = (255, 211, 77, 255)
GOLD_DARK = (196, 138, 18, 255)
ROSE = (255, 107, 139, 255)
ROSE_DARK = (184, 58, 94, 255)
ROSE_PALE = (255, 190, 205, 255)
MINT = (107, 227, 168, 255)
MINT_DARK = (47, 168, 115, 255)
SKY = (98, 198, 255, 255)
SKY_DARK = (46, 127, 191, 255)
NAVY = (37, 29, 74, 255)
NIGHT = (20, 20, 60, 255)
VIOLET = (140, 104, 220, 255)
VIOLET_DARK = (88, 70, 163, 255)
WOOD = (176, 112, 66, 255)
WOOD_DARK = (122, 72, 40, 255)
WOOD_LIGHT = (214, 156, 100, 255)
WICKER = (222, 178, 112, 255)
GREEN = (78, 170, 96, 255)
GREEN_DARK = (40, 118, 76, 255)
CLAY = (206, 110, 74, 255)
CLAY_DARK = (150, 72, 52, 255)
GREY = (167, 155, 214, 255)


def canvas(w, h):
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    return im, ImageDraw.Draw(im)


def box(d, x0, y0, x1, y1, fill, line=INK):
    """A filled box with a pixel outline. Both corners are inclusive."""
    d.rectangle([x0, y0, x1, y1], fill=fill, outline=line)


def stripes(d, x0, y0, x1, y1, step, color):
    for x in range(x0, x1 + 1):
        if (x - x0) % step == 0:
            d.line([x, y0, x, y1], fill=color)


def room():
    im, d = canvas(120, 120)
    d.rectangle([0, 0, 119, 59], fill=(92, 74, 160, 255))
    for x in range(0, 120, 8):
        d.line([x, 0, x, 59], fill=(84, 66, 150, 255))
    d.rectangle([0, 56, 119, 59], fill=WOOD_LIGHT)
    d.line([0, 59, 119, 59], fill=WOOD_DARK)
    d.rectangle([0, 60, 119, 119], fill=(148, 96, 62, 255))
    for y in range(60, 120, 10):
        d.line([0, y, 119, y], fill=(122, 76, 48, 255))
        offset = 16 if (y // 10) % 2 else 0
        for x in range(offset, 120, 32):
            d.line([x, y, x, y + 9], fill=(122, 76, 48, 255))
    return im


def window(sky, scene):
    im, d = canvas(36, 36)
    box(d, 0, 0, 35, 35, WOOD)
    d.rectangle([3, 3, 32, 32], fill=sky)
    scene(d)
    d.line([17, 3, 17, 32], fill=WOOD_DARK)
    d.line([18, 3, 18, 32], fill=WOOD)
    d.line([3, 17, 32, 17], fill=WOOD_DARK)
    d.line([3, 18, 32, 18], fill=WOOD)
    d.rectangle([3, 3, 32, 3], fill=(0, 0, 0, 60))
    return im


def sunny(d):
    d.ellipse([20, 6, 29, 15], fill=GOLD)
    d.rectangle([5, 22, 13, 25], fill=WHITE)
    d.rectangle([8, 20, 15, 23], fill=WHITE)
    d.rectangle([3, 27, 32, 32], fill=GREEN)


def starry(d):
    d.ellipse([6, 6, 15, 15], fill=CREAM)
    d.ellipse([9, 5, 17, 13], fill=NIGHT)
    for x, y in [(23, 7), (29, 13), (21, 23), (8, 26), (27, 28), (13, 21)]:
        d.point([x, y], fill=WHITE)
        d.point([x + 1, y], fill=GOLD)


def blossom(d):
    d.line([3, 31, 15, 19], fill=WOOD_DARK)
    d.line([15, 19, 30, 11], fill=WOOD_DARK)
    for x, y in [(8, 22), (14, 15), (21, 11), (27, 7), (25, 16), (12, 25), (19, 21), (28, 22)]:
        d.ellipse([x - 3, y - 3, x + 3, y + 3], fill=ROSE_PALE)
        d.point([x, y], fill=ROSE)


def frame(paint):
    im, d = canvas(28, 24)
    box(d, 0, 0, 27, 23, GOLD_DARK)
    d.rectangle([1, 1, 26, 22], fill=GOLD)
    d.rectangle([3, 3, 24, 20], fill=INK)
    paint(d)
    return im


def fish_art(d):
    d.rectangle([4, 4, 23, 19], fill=SKY)
    d.ellipse([7, 8, 18, 15], fill=ROSE)
    d.polygon([(18, 11), (22, 8), (22, 15)], fill=ROSE_DARK)
    d.point([10, 11], fill=INK)
    d.line([4, 18, 23, 18], fill=SKY_DARK)


def heart_art(d):
    d.rectangle([4, 4, 23, 19], fill=CREAM)
    d.ellipse([8, 7, 14, 13], fill=ROSE)
    d.ellipse([13, 7, 19, 13], fill=ROSE)
    d.polygon([(8, 11), (19, 11), (13, 18)], fill=ROSE)


def moon_art(d):
    d.rectangle([4, 4, 23, 19], fill=NIGHT)
    d.ellipse([9, 6, 19, 16], fill=GOLD)
    d.ellipse([12, 5, 21, 14], fill=NIGHT)
    for x, y in [(6, 7), (21, 16), (7, 16)]:
        d.point([x, y], fill=WHITE)


def rug(ring, inner, center):
    im, d = canvas(56, 24)
    d.ellipse([0, 0, 55, 23], fill=INK)
    d.ellipse([1, 1, 54, 22], fill=ring)
    d.ellipse([6, 4, 49, 19], fill=inner)
    d.ellipse([13, 7, 42, 16], fill=center)
    return im


def rug_star():
    im = rug(VIOLET_DARK, VIOLET, NAVY)
    d = ImageDraw.Draw(im)
    points = []
    for i in range(10):
        a = -math.pi / 2 + i * math.pi / 5
        r = 6 if i % 2 == 0 else 3
        points.append((28 + r * 1.5 * math.cos(a), 12 + r * 0.8 * math.sin(a)))
    d.polygon(points, fill=GOLD)
    return im


def pot(d, color=CLAY, dark=CLAY_DARK):
    box(d, 3, 24, 16, 27, color)
    d.polygon([(5, 28), (14, 28), (13, 35), (6, 35)], fill=color, outline=INK)
    d.line([12, 29, 12, 34], fill=dark)


def fern():
    im, d = canvas(20, 36)
    for tip, base in [((2, 8), (9, 24)), ((17, 8), (10, 24)), ((9, 1), (9, 24)), ((1, 18), (8, 24)), ((18, 18), (11, 24))]:
        d.line([base, tip], fill=GREEN_DARK, width=3)
        d.line([base, tip], fill=GREEN, width=1)
    pot(d)
    return im


def cactus():
    im, d = canvas(20, 36)
    box(d, 7, 5, 12, 24, GREEN)
    box(d, 2, 11, 5, 18, GREEN)
    d.rectangle([5, 15, 7, 17], fill=GREEN, outline=INK)
    box(d, 14, 8, 17, 15, GREEN)
    d.rectangle([12, 13, 14, 15], fill=GREEN, outline=INK)
    d.line([9, 7, 9, 23], fill=GREEN_DARK)
    d.point([8, 4], fill=ROSE)
    d.point([11, 4], fill=ROSE)
    pot(d, SKY, SKY_DARK)
    return im


def flowers():
    im, d = canvas(20, 36)
    for x, y, color in [(4, 9, ROSE), (10, 4, GOLD), (15, 10, VIOLET)]:
        d.line([10, 24, x, y], fill=GREEN_DARK)
        d.ellipse([x - 3, y - 3, x + 3, y + 3], fill=color, outline=INK)
        d.point([x, y], fill=CREAM)
    pot(d, CREAM, GREY)
    return im


def basket():
    im, d = canvas(48, 32)
    d.ellipse([3, 6, 44, 22], fill=ROSE_PALE, outline=INK)
    box(d, 1, 13, 46, 30, WICKER)
    for y in range(14, 30, 3):
        d.line([2, y, 45, y], fill=(190, 140, 80, 255))
    stripes(d, 2, 14, 45, 29, 6, (190, 140, 80, 255))
    return im


def cushion():
    im, d = canvas(48, 32)
    d.rounded_rectangle([0, 8, 47, 31], radius=7, fill=SKY, outline=INK)
    d.rounded_rectangle([6, 12, 41, 25], radius=6, fill=SKY_DARK)
    d.rounded_rectangle([8, 14, 39, 24], radius=5, fill=(150, 220, 255, 255))
    return im


def canopy():
    im, d = canvas(48, 32)
    box(d, 2, 0, 5, 31, WOOD)
    box(d, 42, 0, 45, 31, WOOD)
    box(d, 0, 0, 47, 5, ROSE)
    for x in range(1, 47, 6):
        d.pieslice([x, 3, x + 5, 9], 0, 180, fill=ROSE_DARK)
    box(d, 3, 15, 44, 29, GOLD_DARK)
    d.rectangle([4, 16, 43, 23], fill=CREAM)
    d.rectangle([4, 21, 43, 26], fill=ROSE)
    box(d, 8, 13, 17, 19, WHITE)
    return im


def ball():
    im, d = canvas(20, 20)
    d.ellipse([2, 3, 17, 18], fill=ROSE, outline=INK)
    d.arc([4, 5, 19, 20], 180, 300, fill=ROSE_DARK)
    d.arc([0, 3, 12, 15], 300, 80, fill=ROSE_DARK)
    d.line([16, 15, 19, 18], fill=ROSE_DARK)
    return im


def mouse():
    im, d = canvas(20, 20)
    d.ellipse([3, 8, 15, 17], fill=GREY, outline=INK)
    d.ellipse([3, 5, 8, 10], fill=ROSE_PALE, outline=INK)
    d.point([3, 12], fill=ROSE)
    d.point([6, 11], fill=INK)
    d.line([15, 14, 19, 11], fill=ROSE_DARK)
    return im


def fish():
    im, d = canvas(20, 20)
    d.ellipse([1, 5, 14, 14], fill=SKY, outline=INK)
    d.polygon([(13, 9), (19, 4), (19, 15)], fill=SKY_DARK, outline=INK)
    d.point([4, 8], fill=INK)
    d.line([7, 6, 7, 13], fill=SKY_DARK)
    return im


ITEMS = {
    "bed_basket": basket, "bed_cushion": cushion, "bed_canopy": canopy,
    "rug_mint": lambda: rug(MINT_DARK, MINT, CREAM),
    "rug_rose": lambda: rug(ROSE_DARK, ROSE, ROSE_PALE),
    "rug_star": rug_star,
    "win_day": lambda: window((150, 220, 255, 255), sunny),
    "win_night": lambda: window(NIGHT, starry),
    "win_blossom": lambda: window((200, 232, 255, 255), blossom),
    "art_fish": lambda: frame(fish_art),
    "art_heart": lambda: frame(heart_art),
    "art_moon": lambda: frame(moon_art),
    "plant_fern": fern, "plant_cactus": cactus, "plant_flowers": flowers,
    "toy_ball": ball, "toy_mouse": mouse, "toy_fish": fish,
}

# Back to front, in the same spots as Home.Slot.
PREVIEW = [("win_blossom", 12, 10), ("art_moon", 72, 14), ("rug_star", 44, 90),
           ("plant_flowers", 96, 48), ("bed_canopy", 6, 62), ("toy_ball", 62, 66)]


def main():
    os.makedirs(OUT, exist_ok=True)
    room().save(os.path.join(OUT, "room.png"))
    for key, draw in ITEMS.items():
        draw().save(os.path.join(OUT, key + ".png"))
    if len(sys.argv) > 1:
        sheet(sys.argv[1])
    print(len(ITEMS) + 1, "images written")


def sheet(path):
    scale = 4
    full = room()
    for key, x, y in PREVIEW:
        im = ITEMS[key]()
        full.alpha_composite(im, (x, y))
    grid = Image.new("RGBA", (120 + 6 * 62, 120 + 8), (40, 32, 80, 255))
    grid.alpha_composite(full, (0, 0))
    for i, (key, draw) in enumerate(ITEMS.items()):
        im = draw()
        grid.alpha_composite(im, (124 + i % 6 * 62, 2 + i // 6 * 42))
    grid.resize((grid.width * scale, grid.height * scale), Image.NEAREST).save(path)


if __name__ == "__main__":
    main()
