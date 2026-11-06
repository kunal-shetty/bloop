#!/usr/bin/env python3
"""Draws the cat's room and its furniture into app/src/main/assets/home/*.png.

Sizes must match Home.Slot in core: a window is 36x36, wall art 28x24, a rug 56x24, a plant 20x36,
a bed 48x32, a toy 20x20, a lamp 16x40 and a shelf 32x14. Each room is 120x120, with the wall above
row 60 and the floor below it.
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


def rgb(r, g, b, a=255):
    return (r, g, b, a)


def planks(d, top, fill, seam, shine):
    """Staggered floorboards from row top down, each with a lit upper edge."""
    d.rectangle([0, top, 119, 119], fill=fill)
    for y in range(top, 120, 10):
        d.line([0, y, 119, y], fill=seam)
        d.line([0, y + 1, 119, y + 1], fill=shine)
        offset = 16 if (y // 10) % 2 else 0
        for x in range(offset, 120, 32):
            d.line([x, y, x, y + 9], fill=seam)


def trim(d, y, light, dark):
    """A skirting board along the bottom of the wall."""
    d.rectangle([0, y, 119, 59], fill=light)
    d.line([0, y, 119, y], fill=dark)
    d.line([0, 59, 119, 59], fill=dark)


def light_pool(im, x0, y0, x1, y1, color, strength):
    """Soft window light: stacked translucent boxes, kept blocky to suit the pixel art."""
    glow = Image.new("RGBA", im.size, (0, 0, 0, 0))
    g = ImageDraw.Draw(glow)
    steps = 4
    for i in range(steps):
        inset = i * 4
        alpha = strength * (i + 1) // steps
        g.rectangle([x0 + inset, y0 + inset // 2, x1 - inset, y1 - inset // 2], fill=color + (alpha,))
    im.alpha_composite(glow)


def corners(im, strength=46):
    """Darkens the room's edges so the middle feels lit."""
    shade = Image.new("RGBA", im.size, (0, 0, 0, 0))
    s = ImageDraw.Draw(shade)
    for i, band in enumerate((10, 6, 3)):
        alpha = strength * (i + 1) // 3
        s.rectangle([0, 0, band - 1, 119], fill=(11, 8, 32, alpha))
        s.rectangle([120 - band, 0, 119, 119], fill=(11, 8, 32, alpha))
        s.rectangle([0, 0, 119, band - 1], fill=(11, 8, 32, alpha))
        s.rectangle([0, 120 - band, 119, 119], fill=(11, 8, 32, alpha))
    im.alpha_composite(shade)


def room():
    im, d = canvas(120, 120)
    d.rectangle([0, 0, 119, 59], fill=rgb(92, 74, 160))
    for x in range(0, 120, 16):
        for y in range(0, 52, 12):
            cx, cy = x + (8 if (y // 12) % 2 else 0), y + 6
            d.point([cx, cy - 1, cx - 1, cy, cx + 1, cy, cx, cy + 1], fill=rgb(112, 92, 186))
    d.rectangle([0, 46, 119, 53], fill=rgb(78, 62, 140))
    d.line([0, 46, 119, 46], fill=WOOD_LIGHT)
    trim(d, 54, WOOD_LIGHT, WOOD_DARK)
    planks(d, 60, rgb(148, 96, 62), rgb(118, 74, 46), rgb(170, 116, 76))
    light_pool(im, 4, 62, 62, 116, (255, 226, 160), 40)
    corners(im)
    return im


def room_snowy():
    im, d = canvas(120, 120)
    for y in range(0, 54, 9):
        d.rectangle([0, y, 119, y + 8], fill=rgb(126, 80, 52))
        d.line([0, y, 119, y], fill=rgb(158, 106, 70))
        d.line([0, y + 8, 119, y + 8], fill=rgb(82, 50, 34))
        for x in range(6 + y % 24, 120, 34):
            d.ellipse([x, y + 2, x + 4, y + 6], outline=rgb(96, 58, 38))
    d.rectangle([0, 0, 119, 4], fill=rgb(236, 244, 255))
    for x in range(0, 120, 7):
        d.rectangle([x, 5, x + 2, 6 + x % 4], fill=rgb(236, 244, 255))
    trim(d, 54, rgb(196, 214, 236), rgb(120, 140, 176))
    planks(d, 60, rgb(170, 128, 92), rgb(136, 98, 70), rgb(194, 152, 112))
    d.rectangle([84, 66, 119, 119], fill=rgb(232, 238, 248))
    for y in range(68, 120, 6):
        for x in range(86 + y % 4, 120, 5):
            d.point([x, y], fill=rgb(200, 212, 232))
    light_pool(im, 2, 60, 80, 116, (255, 170, 90), 44)
    corners(im)
    return im


def room_forest():
    im, d = canvas(120, 120)
    d.rectangle([0, 0, 119, 59], fill=rgb(92, 66, 46))
    for x in range(0, 120, 6):
        d.line([x, 0, x + (x % 3) - 1, 59], fill=rgb(74, 52, 36))
    for x, y in [(10, 26), (46, 18), (84, 34), (108, 14)]:
        d.ellipse([x, y, x + 8, y + 10], outline=rgb(64, 44, 30))
        d.ellipse([x + 2, y + 3, x + 6, y + 7], outline=rgb(64, 44, 30))
    leaves = [rgb(60, 142, 86), rgb(82, 176, 104), rgb(44, 112, 72)]
    for x in range(-4, 124, 7):
        tall = 8 + (x * 5) % 11
        d.ellipse([x, -6, x + 12, tall], fill=leaves[(x // 7) % 3])
    for x in (18, 62, 98):
        d.line([x, 6, x, 30 + x % 8], fill=rgb(60, 142, 86))
        for y in range(10, 30, 6):
            d.point([x - 1, y, x + 1, y + 2], fill=rgb(107, 227, 168))
    trim(d, 55, rgb(122, 84, 54), rgb(66, 44, 30))
    d.rectangle([0, 60, 119, 119], fill=rgb(86, 148, 84))
    for y in range(62, 120, 4):
        for x in range(2 + y % 7, 120, 7):
            d.line([x, y, x + 1, y - 2], fill=rgb(66, 124, 72))
    for x, y in [(14, 72), (100, 80), (70, 110), (30, 104), (92, 104)]:
        d.point([x, y, x + 1, y, x, y + 1, x + 1, y + 1], fill=rgb(255, 241, 176))
    light_pool(im, 20, 60, 100, 118, (214, 255, 170), 30)
    corners(im, 60)
    return im


def room_beach():
    im, d = canvas(120, 120)
    for x in range(0, 120, 12):
        d.rectangle([x, 0, x + 11, 59], fill=rgb(150, 224, 226) if (x // 12) % 2 else rgb(166, 234, 232))
        d.line([x, 0, x, 59], fill=rgb(104, 176, 186))
    for i, x in enumerate(range(4, 116, 11)):
        color = (ROSE, GOLD, SKY)[i % 3]
        d.line([x, 3 + (i % 2), x + 11, 4 - (i % 2)], fill=WOOD_DARK)
        d.polygon([(x + 2, 4), (x + 8, 4), (x + 5, 11)], fill=color)
    trim(d, 54, rgb(244, 226, 188), rgb(186, 150, 104))
    d.rectangle([0, 60, 119, 119], fill=rgb(244, 222, 164))
    for y in range(62, 120, 3):
        for x in range(1 + (y * 3) % 8, 120, 8):
            d.point([x, y], fill=rgb(220, 192, 130))
    d.line([0, 74, 119, 78], fill=rgb(226, 200, 142))
    d.line([0, 96, 119, 92], fill=rgb(226, 200, 142))
    for x, y in [(16, 110), (104, 72), (92, 112)]:
        d.ellipse([x, y, x + 5, y + 4], fill=ROSE_PALE, outline=ROSE_DARK)
        d.line([x + 2, y, x + 2, y + 4], fill=ROSE_DARK)
    d.polygon([(30, 112), (36, 106), (42, 112)], fill=GOLD, outline=GOLD_DARK)
    light_pool(im, 6, 60, 110, 118, (255, 244, 200), 34)
    corners(im, 26)
    return im


def room_candy():
    im, d = canvas(120, 120)
    for x in range(0, 120, 10):
        d.rectangle([x, 0, x + 9, 59], fill=rgb(255, 174, 196) if (x // 10) % 2 else rgb(255, 214, 226))
    d.rectangle([0, 0, 119, 6], fill=rgb(255, 248, 240))
    for x in range(0, 120, 9):
        d.ellipse([x, 2, x + 7, 10 + (x * 3) % 8], fill=rgb(255, 248, 240))
    for x, y, c in [(30, 30, MINT), (58, 40, GOLD), (92, 26, SKY), (112, 44, VIOLET), (8, 44, MINT)]:
        d.ellipse([x, y, x + 6, y + 6], fill=c)
        d.point([x + 2, y + 2], fill=WHITE)
    trim(d, 54, rgb(255, 248, 240), rgb(232, 150, 176))
    for y in range(60, 120, 10):
        for x in range(0, 120, 10):
            even = ((x // 10) + (y // 10)) % 2
            d.rectangle([x, y, x + 9, y + 9], fill=rgb(150, 232, 202) if even else rgb(255, 240, 226))
    light_pool(im, 8, 60, 110, 118, (255, 255, 255), 36)
    corners(im, 22)
    return im


def room_haunted():
    im, d = canvas(120, 120)
    d.rectangle([0, 0, 119, 59], fill=rgb(58, 44, 84))
    for x in range(0, 120, 20):
        d.line([x, 0, x, 59], fill=rgb(46, 34, 68))
    for x, y, w, h in [(14, 18, 16, 12), (70, 8, 12, 16), (98, 30, 16, 14)]:
        d.rectangle([x, y, x + w, y + h], fill=rgb(82, 64, 94))
        d.line([x, y + h, x + w, y + h - 2], fill=rgb(42, 30, 62))
    for x, y in [(44, 30), (36, 12)]:
        d.line([x, y, x + 6, y + 8, x + 2, y + 14, x + 8, y + 22], fill=rgb(32, 22, 50))
    web = rgb(190, 190, 220)
    for end in [(22, 0), (18, 10), (8, 18), (0, 22)]:
        d.line([0, 0, end[0], end[1]], fill=web)
    for r in (8, 15):
        d.arc([-r, -r, r, r], 0, 90, fill=web)
    for end in [(98, 0), (102, 10), (112, 18), (119, 22)]:
        d.line([119, 0, end[0], end[1]], fill=web)
    trim(d, 55, rgb(70, 52, 72), rgb(32, 22, 44))
    planks(d, 60, rgb(78, 60, 70), rgb(50, 36, 48), rgb(96, 76, 86))
    for x, y in [(30, 84), (88, 106), (60, 72)]:
        d.rectangle([x, y, x + 8, y + 2], fill=rgb(28, 18, 34))
    light_pool(im, 10, 60, 70, 116, (150, 170, 255), 34)
    corners(im, 80)
    return im


def room_space():
    im, d = canvas(120, 120)
    for y in range(0, 54, 18):
        for x in range(0, 120, 30):
            d.rectangle([x, y, x + 29, y + 17], fill=rgb(38, 42, 82), outline=rgb(60, 66, 118))
            for cx, cy in [(x + 2, y + 2), (x + 27, y + 2), (x + 2, y + 15), (x + 27, y + 15)]:
                d.point([cx, cy], fill=rgb(120, 130, 190))
    d.rectangle([56, 8, 110, 38], fill=rgb(8, 8, 28), outline=rgb(98, 198, 255))
    for x, y in [(62, 14), (72, 30), (84, 12), (98, 22), (104, 33), (66, 24), (90, 32)]:
        d.point([x, y], fill=WHITE)
    d.ellipse([92, 12, 104, 24], fill=rgb(200, 110, 86))
    d.line([92, 20, 104, 16], fill=rgb(240, 170, 130))
    d.rectangle([0, 44, 119, 45], fill=rgb(98, 198, 255))
    d.rectangle([0, 46, 119, 46], fill=rgb(46, 127, 191))
    trim(d, 55, rgb(84, 92, 148), rgb(30, 32, 66))
    d.rectangle([0, 60, 119, 119], fill=rgb(58, 62, 106))
    for y in range(60, 120, 12):
        d.line([0, y, 119, y], fill=rgb(36, 38, 76))
        d.line([0, y + 1, 119, y + 1], fill=rgb(82, 88, 140))
    for x in range(0, 120, 20):
        d.line([x, 60, x, 119], fill=rgb(36, 38, 76))
    for x in range(10, 120, 40):
        d.rectangle([x, 88, x + 12, 91], fill=rgb(107, 227, 168))
    light_pool(im, 30, 60, 90, 118, (120, 210, 255), 30)
    corners(im, 60)
    return im


def room_castle():
    im, d = canvas(120, 120)
    d.rectangle([0, 0, 119, 59], fill=rgb(128, 126, 148))
    for row, y in enumerate(range(0, 54, 9)):
        d.line([0, y, 119, y], fill=rgb(94, 92, 116))
        for x in range(12 if row % 2 else 0, 120, 24):
            d.line([x, y, x, y + 8], fill=rgb(94, 92, 116))
            d.line([x + 2, y + 1, x + 20, y + 1], fill=rgb(150, 148, 170))
    for x in (4, 100):
        d.rectangle([x, 0, x + 15, 34], fill=rgb(170, 40, 66))
        d.polygon([(x, 34), (x + 15, 34), (x + 7, 42)], fill=rgb(170, 40, 66))
        d.rectangle([x + 5, 10, x + 10, 18], fill=GOLD)
        d.line([x, 0, x + 15, 0], fill=GOLD_DARK)
    for x in (56,):
        d.rectangle([x + 2, 26, x + 5, 36], fill=WOOD_DARK)
        d.polygon([(x, 26), (x + 7, 26), (x + 3, 15)], fill=rgb(255, 154, 60))
        d.polygon([(x + 2, 26), (x + 5, 26), (x + 3, 20)], fill=GOLD)
    trim(d, 54, rgb(98, 96, 120), rgb(54, 52, 74))
    for y in range(60, 120, 15):
        for x in range(0, 120, 15):
            even = ((x // 15) + (y // 15)) % 2
            d.rectangle([x, y, x + 14, y + 14], fill=rgb(172, 170, 190) if even else rgb(120, 118, 142))
    d.rectangle([40, 60, 80, 119], fill=rgb(170, 40, 66))
    d.line([40, 60, 40, 119], fill=GOLD)
    d.line([80, 60, 80, 119], fill=GOLD)
    light_pool(im, 30, 60, 92, 112, (255, 196, 110), 34)
    corners(im, 66)
    return im


ROOMS = {
    "room": room, "room_snowy": room_snowy, "room_forest": room_forest, "room_beach": room_beach,
    "room_candy": room_candy, "room_haunted": room_haunted, "room_space": room_space, "room_castle": room_castle,
}


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


def sunset(d):
    for y, c in enumerate([(255, 120, 90), (255, 150, 90), (255, 190, 110), (255, 220, 150)]):
        d.rectangle([3, 3 + y * 6, 32, 8 + y * 6], fill=c + (255,))
    d.ellipse([11, 14, 24, 27], fill=GOLD)
    d.rectangle([3, 27, 32, 32], fill=(92, 58, 110, 255))
    d.polygon([(3, 27), (12, 21), (20, 27)], fill=(70, 44, 92, 255))


def snowfall(d):
    d.rectangle([3, 24, 32, 32], fill=WHITE)
    d.polygon([(19, 24), (25, 13), (31, 24)], fill=GREEN_DARK)
    d.polygon([(21, 18), (25, 8), (29, 18)], fill=GREEN)
    for x, y in [(6, 7), (12, 14), (9, 21), (17, 6), (15, 19), (29, 7), (22, 5), (5, 15)]:
        d.point([x, y], fill=WHITE)


def aurora(d):
    for x in range(3, 33):
        wave = 8 + int(4 * math.sin(x / 4.0))
        d.line([x, wave, x, wave + 9], fill=MINT)
        d.line([x, wave + 10, x, wave + 14], fill=VIOLET)
    for x, y in [(6, 5), (15, 4), (27, 6), (22, 26), (9, 27)]:
        d.point([x, y], fill=WHITE)
    d.polygon([(3, 32), (12, 23), (21, 32)], fill=NAVY)
    d.polygon([(16, 32), (26, 25), (32, 32)], fill=NAVY)


def cat_art(d):
    d.rectangle([4, 4, 23, 19], fill=(200, 232, 255, 255))
    d.polygon([(8, 9), (9, 5), (12, 8)], fill=(255, 170, 84, 255))
    d.polygon([(15, 8), (18, 5), (19, 9)], fill=(255, 170, 84, 255))
    d.ellipse([8, 7, 19, 18], fill=(255, 170, 84, 255))
    d.point([11, 12, 16, 12], fill=INK)
    d.point([13, 14, 14, 14], fill=ROSE)


def map_art(d):
    d.rectangle([4, 4, 23, 19], fill=(150, 210, 240, 255))
    d.polygon([(6, 7), (13, 6), (12, 12), (7, 13)], fill=GREEN)
    d.polygon([(15, 11), (21, 9), (22, 16), (16, 17)], fill=WICKER)
    d.line([9, 15, 12, 17, 15, 14], fill=ROSE)
    d.point([15, 14], fill=INK)


def galaxy_art(d):
    d.rectangle([4, 4, 23, 19], fill=NIGHT)
    for r in (7, 5, 3):
        d.ellipse([13 - r, 11 - r // 2, 13 + r, 11 + r // 2], outline=(VIOLET, ROSE, GOLD)[(7 - r) // 2])
    for x, y in [(6, 6), (20, 6), (22, 16), (7, 17), (17, 17)]:
        d.point([x, y], fill=WHITE)


def rug_sun():
    im, d = canvas(56, 24)
    d.ellipse([0, 0, 55, 23], fill=GOLD_DARK, outline=INK)
    d.ellipse([3, 2, 52, 21], fill=GOLD)
    d.ellipse([17, 6, 38, 17], fill=ROSE)
    for x in range(8, 50, 6):
        d.point([x, 12], fill=GOLD_DARK)
    d.ellipse([22, 8, 33, 15], fill=CREAM)
    return im


def rug_paw():
    im, d = canvas(56, 24)
    box(d, 0, 2, 55, 21, VIOLET_DARK)
    d.rectangle([3, 4, 52, 19], fill=VIOLET)
    for cx in (15, 41):
        d.ellipse([cx - 5, 10, cx + 5, 18], fill=CREAM)
        for dx, dy in [(-6, 6), (-2, 4), (2, 4), (6, 6)]:
            d.ellipse([cx + dx - 1, dy, cx + dx + 2, dy + 3], fill=CREAM)
    return im


def rug_wave():
    im, d = canvas(56, 24)
    d.ellipse([0, 0, 55, 23], fill=SKY_DARK, outline=INK)
    d.ellipse([3, 2, 52, 21], fill=SKY)
    for y in (7, 12, 17):
        points = [(x, y + int(2 * math.sin(x / 2.5))) for x in range(8, 49)]
        d.line(points, fill=WHITE)
    return im


def bed_cloud():
    im, d = canvas(48, 32)
    for x0, y0, x1, y1 in [(0, 12, 20, 31), (10, 4, 30, 28), (24, 8, 47, 31), (6, 16, 42, 31)]:
        d.ellipse([x0, y0, x1, y1], fill=WHITE, outline=INK)
    d.rectangle([4, 19, 43, 28], fill=WHITE)
    d.line([6, 29, 42, 29], fill=SKY)
    d.ellipse([12, 8, 24, 15], fill=(232, 244, 255, 255))
    d.ellipse([26, 12, 40, 19], fill=(232, 244, 255, 255))
    return im


def bed_hammock():
    im, d = canvas(48, 32)
    for x in (1, 45):
        d.rectangle([x, 2, x + 2, 31], fill=WOOD_DARK)
        d.line([x, 2, x, 31], fill=INK)
    d.line([3, 6, 12, 14], fill=WOOD_LIGHT)
    d.line([45, 6, 36, 14], fill=WOOD_LIGHT)
    d.chord([9, 2, 39, 28], 0, 180, fill=ROSE, outline=INK)
    for x in range(14, 36, 6):
        d.line([x, 16, x, 25], fill=GOLD)
    return im


def bed_pumpkin():
    im, d = canvas(48, 32)
    d.ellipse([0, 6, 47, 31], fill=(255, 154, 60, 255), outline=INK)
    for x in (12, 24, 36):
        d.arc([x - 10, 6, x + 10, 31], 90, 270, fill=(214, 84, 40, 255))
    d.ellipse([9, 13, 38, 27], fill=INK)
    d.ellipse([11, 15, 36, 26], fill=(255, 241, 176, 255))
    d.rectangle([21, 2, 26, 7], fill=GREEN_DARK)
    return im


def plant_bonsai():
    im, d = pot_canvas((70, 100, 150, 255), (40, 62, 110, 255))
    d.line([10, 28, 10, 20, 6, 15], fill=WOOD_DARK)
    d.line([10, 22, 15, 14], fill=WOOD_DARK)
    d.ellipse([0, 8, 12, 17], fill=GREEN, outline=INK)
    d.ellipse([9, 5, 19, 14], fill=GREEN, outline=INK)
    d.ellipse([3, 4, 12, 11], fill=(107, 200, 120, 255))
    return im


def pot_canvas(color, dark):
    im, d = canvas(20, 36)
    pot(d, color, dark)
    return im, d


def plant_sunflower():
    im, d = pot_canvas(CLAY, CLAY_DARK)
    d.line([10, 28, 10, 12], fill=GREEN_DARK)
    d.ellipse([11, 19, 17, 23], fill=GREEN)
    for dx, dy in [(-6, 0), (6, 0), (0, -6), (0, 6), (-4, -4), (4, -4), (-4, 4), (4, 4)]:
        d.ellipse([10 + dx - 2, 9 + dy - 2, 10 + dx + 2, 9 + dy + 2], fill=GOLD)
    d.ellipse([6, 5, 14, 13], fill=WOOD_DARK)
    return im


def plant_cherry():
    im, d = pot_canvas((255, 241, 214, 255), ROSE_DARK)
    d.line([10, 28, 10, 16], fill=WOOD_DARK)
    d.line([10, 20, 5, 14], fill=WOOD_DARK)
    d.line([10, 18, 15, 12], fill=WOOD_DARK)
    for x, y, r in [(5, 10, 5), (14, 8, 6), (10, 5, 5), (3, 16, 3), (17, 15, 3)]:
        d.ellipse([x - r, y - r, x + r, y + r], fill=ROSE_PALE, outline=ROSE)
    for x, y in [(6, 8), (13, 6), (10, 3), (15, 11)]:
        d.point([x, y], fill=ROSE)
    return im


def tower():
    im, d = canvas(20, 20)
    box(d, 2, 15, 17, 19, WOOD)
    box(d, 8, 5, 11, 15, WICKER)
    for y in range(6, 15, 2):
        d.line([8, y, 11, y], fill=WOOD_DARK)
    box(d, 4, 1, 15, 5, VIOLET)
    d.point([6, 2, 13, 3], fill=WHITE)
    return im


def cardboard():
    im, d = canvas(20, 20)
    box(d, 1, 7, 18, 19, WICKER)
    d.polygon([(1, 7), (5, 2), (9, 7)], fill=WOOD_LIGHT, outline=INK)
    d.polygon([(10, 7), (14, 2), (18, 7)], fill=WOOD_LIGHT, outline=INK)
    d.rectangle([3, 9, 16, 12], fill=WOOD)
    d.line([3, 16, 9, 16], fill=WOOD)
    return im


def lamp_base(d, x0=4, x1=11):
    d.rectangle([7, 12, 8, 37], fill=WOOD_DARK)
    box(d, x0, 36, x1, 39, WOOD)


def lamp_classic():
    im, d = canvas(16, 40)
    lamp_base(d)
    d.polygon([(4, 0), (11, 0), (15, 13), (0, 13)], fill=GOLD, outline=INK)
    d.line([3, 11, 12, 11], fill=WHITE)
    return im


def lamp_lantern():
    im, d = canvas(16, 40)
    lamp_base(d)
    box(d, 3, 4, 12, 15, GOLD)
    d.rectangle([5, 6, 10, 13], fill=(255, 154, 60, 255))
    d.rectangle([6, 8, 9, 12], fill=WHITE)
    box(d, 2, 1, 13, 3, INK)
    return im


def lamp_lava():
    im, d = canvas(16, 40)
    box(d, 3, 32, 12, 39, GREY)
    d.polygon([(5, 5), (10, 5), (13, 31), (2, 31)], fill=ROSE, outline=INK)
    for x, y, r in [(7, 12, 2), (8, 21, 3), (6, 27, 2)]:
        d.ellipse([x - r, y - r, x + r, y + r], fill=GOLD)
    box(d, 5, 1, 10, 4, GREY)
    return im


def lamp_star():
    im, d = canvas(16, 40)
    lamp_base(d)
    points = []
    for i in range(10):
        radius = 8 if i % 2 == 0 else 4
        angle = -math.pi / 2 + i * math.pi / 5
        points.append((7.5 + radius * math.cos(angle), 8 + radius * math.sin(angle)))
    d.polygon(points, fill=GOLD, outline=INK)
    d.point([7, 8], fill=WHITE)
    return im


def lamp_storm():
    im, d = canvas(16, 40)
    lamp_base(d)
    d.ellipse([1, 0, 14, 13], fill=VIOLET_DARK, outline=INK)
    d.ellipse([4, 2, 11, 9], fill=VIOLET)
    d.line([8, 2, 6, 7, 9, 7, 7, 12], fill=GOLD)
    return im


def shelf(stuff):
    im, d = canvas(32, 14)
    box(d, 0, 11, 31, 13, WOOD)
    d.line([1, 11, 30, 11], fill=WOOD_LIGHT)
    stuff(d)
    return im


def books(d):
    for x, h, c in [(3, 9, ROSE), (6, 8, SKY), (9, 10, MINT), (12, 7, GOLD), (15, 9, VIOLET), (18, 8, ROSE_DARK), (21, 10, SKY_DARK)]:
        d.rectangle([x, 10 - h, x + 2, 10], fill=c, outline=INK)
    d.polygon([(25, 10), (28, 3), (30, 10)], fill=WOOD_LIGHT, outline=INK)


def pots(d):
    for x, c in [(3, CLAY), (13, SKY_DARK), (23, ROSE_DARK)]:
        d.rectangle([x, 7, x + 5, 10], fill=c, outline=INK)
        d.ellipse([x - 1, 1, x + 6, 7], fill=GREEN, outline=INK)


def trophies(d):
    for x, h in [(4, 8), (14, 10), (24, 7)]:
        d.rectangle([x + 2, 9, x + 3, 10], fill=GOLD_DARK)
        d.ellipse([x, 10 - h, x + 5, 9 - h // 3], fill=GOLD, outline=INK)
    d.point([16, 3], fill=WHITE)


def crystals(d):
    for x, top, c in [(3, 3, VIOLET), (9, 5, SKY), (14, 1, MINT), (21, 4, ROSE), (26, 2, VIOLET)]:
        d.polygon([(x, 10), (x + 2, top), (x + 5, 10)], fill=c, outline=INK)
        d.point([x + 2, top + 2], fill=WHITE)


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
    "bed_cloud": bed_cloud, "bed_hammock": bed_hammock, "bed_pumpkin": bed_pumpkin,
    "rug_sun": rug_sun, "rug_paw": rug_paw, "rug_wave": rug_wave,
    "win_sunset": lambda: window((255, 120, 90, 255), sunset),
    "win_snow": lambda: window((176, 212, 240, 255), snowfall),
    "win_aurora": lambda: window(NIGHT, aurora),
    "art_cat": lambda: frame(cat_art), "art_map": lambda: frame(map_art), "art_galaxy": lambda: frame(galaxy_art),
    "plant_bonsai": plant_bonsai, "plant_sunflower": plant_sunflower, "plant_cherry": plant_cherry,
    "toy_tower": tower, "toy_box": cardboard,
    "lamp_classic": lamp_classic, "lamp_lantern": lamp_lantern, "lamp_lava": lamp_lava,
    "lamp_star": lamp_star, "lamp_storm": lamp_storm,
    "shelf_books": lambda: shelf(books), "shelf_pots": lambda: shelf(pots),
    "shelf_trophy": lambda: shelf(trophies), "shelf_crystal": lambda: shelf(crystals),
}

# Back to front, in the same spots as Home.Slot.
PREVIEW = [("win_blossom", 12, 10), ("art_moon", 72, 14), ("rug_star", 44, 90),
           ("plant_flowers", 96, 48), ("bed_canopy", 6, 62), ("toy_ball", 62, 66),
           ("lamp_classic", 50, 46), ("shelf_books", 72, 42)]


def main():
    os.makedirs(OUT, exist_ok=True)
    for key, draw in ROOMS.items():
        draw().save(os.path.join(OUT, key + ".png"))
    for key, draw in ITEMS.items():
        draw().save(os.path.join(OUT, key + ".png"))
    if len(sys.argv) > 1:
        sheet(sys.argv[1])
    print(len(ITEMS) + len(ROOMS), "images written")


def sheet(path):
    scale = 3
    columns = 4
    rows = len(ROOMS) // columns
    grid = Image.new("RGBA", (columns * 124 + 12 * 62, rows * 124 + 8), (40, 32, 80, 255))
    for i, draw in enumerate(ROOMS.values()):
        full = draw()
        for key, x, y in PREVIEW:
            full.alpha_composite(ITEMS[key](), (x, y))
        grid.alpha_composite(full, (i % columns * 124, i // columns * 124))
    for i, (key, draw) in enumerate(ITEMS.items()):
        grid.alpha_composite(draw(), (columns * 124 + 2 + i % 12 * 62, 2 + i // 12 * 44))
    grid.resize((grid.width * scale, grid.height * scale), Image.NEAREST).save(path)


if __name__ == "__main__":
    main()
