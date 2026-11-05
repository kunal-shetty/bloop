#!/usr/bin/env python3
"""Draws the pixel cat and its shop accessories into app/src/main/assets/cat/*.png.

Every layer is a transparent 32x32 image in the same position, so the game stacks them directly:
tail, body, eyes, then neck, face and head accessories. Change the text art below and re-run.
Run:  python tools/gen_cat.py      (needs Pillow)
      python tools/gen_cat.py sheet.png   also writes a preview sheet of every item on the cat
"""
import os
import sys

from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets", "cat")

PALETTE = {
    "k": (11, 8, 32),       # ink outline
    "o": (255, 170, 84),    # orange fur
    "d": (226, 124, 52),    # darker stripes and shading
    "h": (255, 205, 140),   # lit fur
    "w": (255, 246, 232),   # white muzzle and chest
    "p": (255, 139, 167),   # pink nose and ears
    "b": (72, 196, 158),    # green eyes
    "W": (255, 255, 255),
    "g": (255, 211, 77),    # gold
    "G": (196, 138, 18),
    "y": (255, 241, 176),
    "r": (255, 107, 139),   # rose
    "R": (184, 58, 94),
    "s": (98, 198, 255),    # sky
    "S": (46, 127, 191),
    "v": (140, 104, 220),   # violet
    "V": (88, 70, 163),
    "n": (37, 29, 74),      # navy
    "m": (107, 227, 168),   # mint
    "M": (47, 168, 115),
    "e": (167, 155, 214),   # grey lilac
}

BODY = [
    "................................",
    "................................",
    "................................",
    "................................",
    "................................",
    "................................",
    "................................",
    "......kk..............kk........",
    ".....kook............kook.......",
    ".....koppk..........kppok.......",
    ".....kopppkkkkkkkkkkpppok.......",
    ".....kohhoooddooddooohhok.......",
    "....kohhooooddooddoooohhok......",
    "....kohoooooooooooooooohok......",
    "....koooooooooooooooooooook......"[:32],
    "...kdoooooooooooooooooooodk.....",
    "...kdoooooooooooooooooooodk.....",
    "...koooooooooooooooooooooook....",
    "...koooooooowwwwwwoooooooook....",
    "...koooooowwwwppwwwwoooooook....",
    "...kdooooowwwwwwwwwwooooodk.....",
    "....kooooooowwwwwwooooooook.....",
    "....kkoooooooooooooooooookk.....",
    ".....kkkooooooooooooooookk......",
    "......kooookkkkkkkkkoooook......",
    "......koodoowwwwwwoodoook.......",
    "......koodoowwwwwwoodoook.......",
    ".....kooooowwwwwwwwooooook......",
    ".....kooooowwwwwwwwooooook......",
    ".....kkwwwkkkkkkkkkkwwwkk.......",
    "......kkkkk........kkkkk........",
    "................................",
]

def blank():
    return Image.new("RGBA", (32, 32), (0, 0, 0, 0))


def from_art(rows):
    im = blank()
    px = im.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row[:32]):
            if ch != ".":
                px[x, y] = PALETTE[ch] + (255,)
    return im


def sprite(x0, y0, rows):
    """A small piece of art placed at (x0, y0) on the 32x32 canvas."""
    im = blank()
    px = im.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                px[x0 + x, y0 + y] = PALETTE[ch] + (255,)
    return im


def over(*layers):
    out = blank()
    for layer in layers:
        out = Image.alpha_composite(out, layer)
    return out


def two_eyes(rows):
    left = sprite(8, 15, rows)
    right = sprite(18, 15, rows)
    return Image.alpha_composite(left, right)


EYES = {
    "eyes_open": two_eyes(["kkkk", "kWbk", "kbbk", "kkkk"]),
    "eyes_closed": two_eyes(["....", "....", "kkkk", "...."]),
    "eyes_happy": two_eyes(["....", ".kk.", "k..k", "...."]),
}

# The mouth and whiskers are part of the face, so they stay in one fixed layer.
FACE_DETAIL = sprite(4, 19, [
    "kk....................kk",
    "...........kk...........",
    "kk.......kk..kk.......kk",
])

TAIL = {
    "tail_0": sprite(24, 14, [
        ".....kkk",
        "....kook",
        "....kodk",
        "....kook",
        "....kodk",
        "...kook.",
        "...kook.",
        "..koodk.",
        "kkoodok.",
        "koooook.",
        "kooook..",
        "kkkkk...",
    ]),
    "tail_1": sprite(24, 15, [
        "......kk",
        ".....kok",
        "....kook",
        "....kodk",
        "....kook",
        "...kodk.",
        "...kook.",
        "..koodk.",
        "kkoodok.",
        "koooook.",
        "kooook..",
        "kkkkk...",
    ]),
    "tail_2": sprite(24, 13, [
        "...kkk..",
        "...kook.",
        "...kodk.",
        "...kook.",
        "...kodk.",
        "...kook.",
        "...kook.",
        "...kodk.",
        "..koook.",
        "kkoodok.",
        "koooook.",
        "kkkkkk..",
    ]),
}

ACCESSORIES = {
    # ---- head
    "bow": sprite(9, 7, [
        "kkk...kkk",
        "krrk.krrk",
        "krrrkrrrk",
        "krRrgrRrk",
        "krrrkrrrk",
        "krrk.krrk",
        "kkk...kkk",
    ]),
    "party": sprite(10, 0, [
        "....gg....",
        "....ky....",
        "...krsk...",
        "...krsk...",
        "..ksrrsk..",
        "..ksrrsk..",
        ".krrssrrk.",
        ".krrssrrk.",
        "ksrrrrrrsk",
        "ksssrrsssk",
        "kkkkkkkkkk",
    ]),
    "beanie": sprite(6, 3, [
        "......kkkkkk......",
        ".....kMMmmmMk.....",
        "....kmmmmmmmmk....",
        "...kmmMmmMmmMmk...",
        "..kmmmMmmMmmMmmk..",
        "..kmmmMmmMmmMmmk..",
        ".kkkkkkkkkkkkkkkk.",
        ".kyyeyyeyyeyyeyyk.",
        ".kyyeyyeyyeyyeyyk.",
        ".kkkkkkkkkkkkkkkk.",
    ]),
    "wizard": sprite(5, 0, [
        "........k...........",
        ".......kvk..........",
        "......kvVk..........",
        ".....kvvvk..........",
        ".....kvgvvk.........",
        "....kvvvvvk.........",
        "....kvvvgvvk........",
        "...kvvvvvvvk........",
        "...kggggggggk.......",
        "..kvvvvvvvvvvk......",
        "kkkkkkkkkkkkkkkkkkk.",
        "kVVVVVVVVVVVVVVVVVk.",
        "kkkkkkkkkkkkkkkkkkk.",
    ]),
    "crown": sprite(9, 4, [
        "k...k....k...k",
        "kgk.kgk.kgk.gk",
        "kgkkgggkgggkgk",
        "kggggggggggggk",
        "kgggrggsggrggk",
        "kGGGGGGGGGGGGk",
        "kkkkkkkkkkkkkk",
    ]),
    # ---- face
    "shades": sprite(6, 14, [
        "kkkkkkkkkkkkkkkkkk",
        ".kkkkkkk..kkkkkkk.",
        ".keknnnk..keknnnk.",
        ".kknnnnkkkkknnnnk.",
        "..kkkkk....kkkkk..",
    ]),
    "hearts": sprite(6, 14, [
        ".kkk.kkk..kkk.kkk.",
        "krrrkrrrkkrrrkrrrk",
        "krRrrrrrkkrRrrrrrk",
        ".krrrrrk..krrrrrk.",
        "..krrrk....krrrk..",
        "...krk......krk...",
        "....k........k....",
    ]),
    "monocle": sprite(17, 13, [
        ".kgggk.",
        "kg...gk",
        "g.....g",
        "g.....g",
        "g.....g",
        "kg...gk",
        ".kgggk.",
        "......g",
        "......g",
        ".....g.",
        ".....g.",
    ]),
    # ---- neck
    "bell": sprite(7, 22, [
        "...kkkkkkkkkkkk...",
        "..krrrrrrrrrrrrk..",
        "..kRRRRkggkRRRRk..",
        "...kkkkgggkkkkk...",
        "......kgyggk......",
        "......kgGGgk......",
        ".......kkkk.......",
    ]),
    "bowtie": sprite(10, 23, [
        "kk.......kk.",
        "ksskk.kkssk.",
        "kssssksssskk"[:12],
        "kSsssgssssk.",
        "kssssksssskk"[:12],
        "ksskk.kkssk.",
        "kk.......kk.",
    ]),
    "scarf": sprite(6, 22, [
        "..kkkkkkkkkkkkkkkk",
        ".krrmmrrmmrrmmrrk.",
        ".krrmmrrmmrrmmrrk.",
        "..kkkkkkkkkkkkkkk.",
        "..krrk............",
        "..kmmk............",
        "..krrk............",
        "..kkkk............",
    ]),
    "cape": sprite(2, 23, [
        "....krrrrrrrrrrrrrrrrk....",
        "...krrrkkkkkkkkkkkkrrrk...",
        "..krRrk............krRrk..",
        "..krRrk............krRrk..",
        ".krRrrk............krrRrk.",
        ".krRrrk............krrRrk.",
        "krRrrrk............krrrRrk",
        "kkkkkk..............kkkkkk",
    ]),
}

def main():
    os.makedirs(OUT, exist_ok=True)
    body = over(from_art(BODY), FACE_DETAIL)
    body.save(os.path.join(OUT, "body.png"))
    for name, im in {**EYES, **TAIL}.items():
        im.save(os.path.join(OUT, name + ".png"))
    for key, im in ACCESSORIES.items():
        im.save(os.path.join(OUT, "acc_" + key + ".png"))
    print(len(os.listdir(OUT)), "cat layers written")

    if len(sys.argv) > 1:
        keys = [None] + list(ACCESSORIES)
        scale, cols = 8, 5
        rows = (len(keys) + 2) // cols + 1
        sheet = Image.new("RGB", (cols * 32 * scale, rows * 32 * scale), (58, 46, 110))
        base = over(TAIL["tail_0"], body, EYES["eyes_open"])
        cells = [base if k is None else over(base, ACCESSORIES[k]) for k in keys]
        cells.append(over(TAIL["tail_1"], body, EYES["eyes_happy"]))
        cells.append(over(TAIL["tail_2"], body, EYES["eyes_closed"], ACCESSORIES["crown"], ACCESSORIES["hearts"], ACCESSORIES["scarf"]))
        for i, cell in enumerate(cells):
            big = cell.resize((32 * scale, 32 * scale), Image.NEAREST)
            sheet.paste(big, ((i % cols) * 32 * scale, (i // cols) * 32 * scale), big)
        sheet.save(sys.argv[1])


if __name__ == "__main__":
    main()
