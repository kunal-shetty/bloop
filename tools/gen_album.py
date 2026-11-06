#!/usr/bin/env python3
"""Draws one banner for each Sticker Album page into app/src/main/assets/album/*.png.

Each banner is 96x28, and its name matches the lower-case name of an Album.Page in core.
Run:  python tools/gen_album.py      (needs Pillow)
      python tools/gen_album.py sheet.png   also writes a preview of every banner
"""
import os
import sys

from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets", "album")

WIDTH, HEIGHT = 96, 28
INK = (11, 8, 32, 255)
WHITE = (255, 255, 255, 255)
GOLD = (255, 211, 77, 255)
ROSE = (255, 107, 139, 255)
MINT = (107, 227, 168, 255)
SKY = (98, 198, 255, 255)
VIOLET = (140, 104, 220, 255)
CREAM = (255, 241, 214, 255)
WOOD = (176, 112, 66, 255)
GREEN = (78, 170, 96, 255)
ORANGE = (255, 170, 84, 255)


def banner(top, bottom):
    im = Image.new("RGBA", (WIDTH, HEIGHT), bottom)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, WIDTH - 1, HEIGHT // 2], fill=top)
    return im, d


def finish(d):
    d.rectangle([0, 0, WIDTH - 1, HEIGHT - 1], outline=INK)


def animals():
    im, d = banner((150, 220, 255, 255), (120, 200, 110, 255))
    for x in (14, 46, 78):
        d.ellipse([x, 10, x + 12, 22], fill=ORANGE, outline=INK)
        d.polygon([(x + 1, 12), (x + 2, 6), (x + 6, 10)], fill=ORANGE, outline=INK)
        d.polygon([(x + 6, 10), (x + 10, 6), (x + 11, 12)], fill=ORANGE, outline=INK)
        d.point([x + 4, 15, x + 8, 15], fill=INK)
    finish(d)
    return im


def food():
    im, d = banner((255, 214, 226, 255), (255, 241, 214, 255))
    for x, color in [(12, ROSE), (40, GOLD), (68, MINT)]:
        d.rectangle([x, 12, x + 16, 22], fill=CREAM, outline=INK)
        d.rectangle([x, 8, x + 16, 13], fill=color, outline=INK)
        d.point([x + 5, 6, x + 11, 6], fill=WHITE)
    finish(d)
    return im


def nature():
    im, d = banner((150, 220, 255, 255), (78, 170, 96, 255))
    d.ellipse([72, 3, 84, 15], fill=GOLD)
    for x in (10, 34, 54):
        d.rectangle([x + 4, 16, x + 6, 23], fill=WOOD)
        d.polygon([(x, 16), (x + 5, 4), (x + 10, 16)], fill=(40, 118, 76, 255), outline=INK)
    finish(d)
    return im


def home():
    im, d = banner((92, 74, 160, 255), (148, 96, 62, 255))
    d.rectangle([12, 5, 28, 15], fill=SKY, outline=INK)
    d.line([20, 5, 20, 15], fill=INK)
    d.rectangle([44, 14, 80, 23], fill=ROSE, outline=INK)
    d.rectangle([44, 10, 52, 16], fill=CREAM, outline=INK)
    finish(d)
    return im


def body():
    im, d = banner((255, 214, 170, 255), (255, 190, 140, 255))
    for x in (14, 54):
        d.ellipse([x, 12, x + 20, 24], fill=CREAM, outline=INK)
        for i in range(4):
            d.ellipse([x + 1 + i * 5, 5 + (i % 2), x + 6 + i * 5, 11 + (i % 2)], fill=CREAM, outline=INK)
    finish(d)
    return im


def colours():
    im, d = banner(CREAM, CREAM)
    for i, color in enumerate([ROSE, ORANGE, GOLD, MINT, SKY, VIOLET]):
        d.rectangle([i * 16, 0, i * 16 + 15, HEIGHT], fill=color)
    finish(d)
    return im


def time():
    im, d = banner((37, 29, 74, 255), (88, 70, 163, 255))
    d.ellipse([34, 2, 58, 26], fill=CREAM, outline=INK)
    d.line([46, 14, 46, 6], fill=INK)
    d.line([46, 14, 52, 17], fill=INK)
    for x, y in [(10, 6), (20, 18), (72, 8), (84, 20), (64, 21)]:
        d.point([x, y], fill=WHITE)
    finish(d)
    return im


def travel():
    im, d = banner((150, 220, 255, 255), (62, 150, 214, 255))
    d.polygon([(34, 15), (62, 15), (56, 22), (40, 22)], fill=WOOD, outline=INK)
    d.line([48, 4, 48, 15], fill=INK)
    d.polygon([(49, 5), (60, 13), (49, 13)], fill=WHITE, outline=INK)
    d.ellipse([8, 4, 22, 9], fill=WHITE)
    d.ellipse([72, 7, 88, 12], fill=WHITE)
    finish(d)
    return im


PAGES = {
    "animals": animals, "food": food, "nature": nature, "home": home,
    "body": body, "colours": colours, "time": time, "travel": travel,
}


def main():
    os.makedirs(OUT, exist_ok=True)
    for key, draw in PAGES.items():
        draw().save(os.path.join(OUT, key + ".png"))
    print(len(PAGES), "banners written")
    if len(sys.argv) > 1:
        sheet = Image.new("RGBA", (2 * WIDTH + 6, 4 * HEIGHT + 10), (40, 32, 80, 255))
        for i, draw in enumerate(PAGES.values()):
            sheet.alpha_composite(draw(), (2 + i % 2 * (WIDTH + 2), 2 + i // 2 * (HEIGHT + 2)))
        sheet.resize((sheet.width * 4, sheet.height * 4), Image.NEAREST).save(sys.argv[1])


if __name__ == "__main__":
    main()
