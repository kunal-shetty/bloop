#!/usr/bin/env python3
"""Draws the pixel cat, its breeds and its accessories into app/src/main/assets/cat/*.png.

Every layer is a transparent 32x32 image in the same position, so the game stacks them directly:
tail, body, eyes, then neck, face and head accessories. Each breed is the same cat recoloured, saved
as body_<breed>.png and tail_<n>_<breed>.png. Change the text art below and re-run.
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
    "c": (150, 98, 60),     # brown
    "C": (98, 62, 40),      # dark brown
    "i": (206, 240, 255),   # ice
    "f": (255, 154, 60),    # flame orange
    "F": (214, 84, 40),     # deep flame
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
    "eyes_wince": Image.alpha_composite(sprite(8, 15, ["k...", ".kk.", "..kk", ".kk.", "k..."]),
                                        sprite(18, 15, ["...k", ".kk.", "kk..", ".kk.", "...k"])),
    "eyes_wide": two_eyes(["kkkk", "kWWk", "kWbk", "kkkk"]),
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
    "heartcollar": sprite(7, 22, [
        "...kkkkkkkkkkkk...",
        "..krrrkkkrkkkrrk..",
        "...kkkgygkgggkkk..",
        ".....kgyggggGk....",
        "......kgggGGk.....",
        ".......kgGGk......",
        "........kGk.......",
        ".........k........",
    ]),
    "bowtie": sprite(10, 23, [        "kk.......kk.",
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

ACCESSORIES.update({
    # ---- coin hats
    "tophat": sprite(8, 0, [
        "...kkkkkkkkkk...",
        "...knnnnnnnnk...",
        "...knnnnnnnnk...",
        "...knnnnnnnnk...",
        "...knnnnnnnnk...",
        "...knnnnnnnnk...",
        "...krrrrrrrrk...",
        "...kRRRRRRRRk...",
        "kkkkkkkkkkkkkkkk",
        "knnnnnnnnnnnnnnk",
        "kkkkkkkkkkkkkkkk",
    ]),
    "pirate": sprite(5, 3, [
        "........kkkkkk........",
        "......kknnnnnnkk......",
        "....kknnnnWWnnnnkk....",
        "..kknnnnnWnnWnnnnnkk..",
        ".knnnnnnnnWWnnnnnnnnk.",
        "knnnnnnnWnnnnWnnnnnnnk",
        "knnnnnnnnnnnnnnnnnnnnk",
        "kggggggggggggggggggggk",
        ".kkkkkkkkkkkkkkkkkkkk.",
    ]),
    "chef": sprite(8, 0, [
        "...kkkk..kkkk...",
        "..kWWWWkkWWWWk..",
        ".kWWWWWWWWWWWWk.",
        ".kWWWWWWWWWWWWk.",
        ".kWWeWWWeWWWeWk.",
        "..kWeWWWeWWWeWk.",
        "..kWeWWWeWWWeWk.",
        "..kWWWWWWWWWWk..",
        "..keeeeeeeeeek..",
        "..kkkkkkkkkkkk..",
    ]),
    "flowers": sprite(7, 7, [
        "...rr....gg....rr...",
        "..rgrr..mggm..rgrr..",
        ".kMrrMkkMggMkkMrrMk.",
        "kMMMMMMMMMMMMMMMMMMk",
        ".kkkkkkkkkkkkkkkkkk.",
    ]),
    "cowboy": sprite(3, 2, [
        ".........kkkkkkkk.........",
        ".......kkccccccccKK.......".replace("K", "k"),
        "......kccccCCcccccck......",
        "......kccccccccccccck.....",
        "......kcccccccccccck......",
        "......kCCCCCCCCCCCCk......",
        "kk....kGGGGGGGGGGGGk....kk",
        "kckkkkkkkkkkkkkkkkkkkkkkck",
        ".kccccccccccccccccccccck..",
        "..kkkkkkkkkkkkkkkkkkkkk...",
    ]),
    "santa": sprite(6, 2, [
        "..............kWk.",
        ".........kkkkkkk..",
        "......kkkrrrrrk...",
        "....kkrrrrrrrrrk..",
        "...krrrrrrrrrrrk..",
        "..krrrrrrrrrrrrrk.",
        ".kWWWWWWWWWWWWWWWk",
        ".kWWWWWWWWWWWWWWWk",
        "..kkkkkkkkkkkkkkk.",
    ]),
    "halo": sprite(8, 2, [
        "....kkkkkkkk....",
        "..kkggggggggkk..",
        ".kgg........ggk.",
        ".kgg........ggk.",
        "..kkggggggggkk..",
        "....kkkkkkkk....",
    ]),
    # ---- coin glasses
    "nerd": sprite(6, 14, [
        "kkkkkkkkkkkkkkkkkk",
        "k.....k....k.....k",
        "k.....kkkkkk.....k",
        "k.....k....k.....k",
        "kkkkkkk....kkkkkkk",
    ]),
    "stars": sprite(6, 13, [
        "...kg.......kg....",
        "..kggk.....kggk...",
        "kkggggkkkkkggggkk.",
        ".kggggk...kggggk..",
        "..kggk.....kggk...",
        "..k..k.....k..k...",
    ]),
    "patch": sprite(5, 12, [
        "kk.................",
        "..kk...............",
        "....kkkk...........",
        "...knnnnk..........",
        "...knnnnkkkkkkkkkkk",
        "...knnnnk..........",
        "....kkkk...........",
    ]),
    "visor": sprite(5, 14, [
        ".kkkkkkkkkkkkkkkkkkk",
        "kssssssssssssssssssk",
        "kiiissssssssssiiiisk",
        "kSSSSSSSSSSSSSSSSSSk",
        ".kkkkkkkkkkkkkkkkkk.",
    ]),
    "mask": sprite(5, 13, [
        ".kkkkkkkkkkkkkkkkkk.",
        "krrrrrrrrrrrrrrrrrrk",
        "krrk....krrk....krrk",
        "krrk....krrk....krrk",
        "krrrrrrrrkkrrrrrrrrk",
        ".kkkkkkkkkkkkkkkkkk.",
    ]),
    # ---- coin neckwear
    "pearls": sprite(7, 23, [
        "kWkWkWkWkWkWkWkWkk",
        "kWWkkkkkkkkkkkWWk.",
        ".kWWkWkWkWkWkWWk..",
        "..kkWWWeWeWWWkk...",
        "....kkkkkkkkk.....",
    ]),
    "bandana": sprite(6, 22, [
        "..kkkkkkkkkkkkkkkk..",
        ".krrWrrWrrWrrWrrWrk.",
        "..krrWrrWrrWrrWrrk..",
        "...krrrWrrWrrWrrk...",
        ".....krrWrrWrrk.....",
        ".......krrWrk.......",
        ".........kk.........",
    ]),
    "medal": sprite(11, 22, [
        "kss.....sk",
        ".ksr...rk.",
        "..ksr.rk..",
        "...kggk...",
        "..kgyggk..",
        "..kgggGk..",
        "...kkkk...",
    ]),
    "tie": sprite(12, 23, [
        ".kkkkkk.",
        ".krrrrk.",
        "..krrk..",
        "..krRk..",
        "..krrk..",
        ".krRrrk.",
        ".krrrRk.",
        "..krrk..",
    ]),
    "lei": sprite(6, 23, [
        "kkkkkkkkkkkkkkkkkkkkk",
        "krrrkgggkrrrkmmmkrrrk",
        "krgrkgygkrgrkmgmkrgrk",
        "krrrkgggkrrrkmmmkrrrk",
        ".kkkkrrrkgggkrrrkkkk.",
        "....krgrkgygkrgrk....",
        "....kkkkkkkkkkkkk....",
    ]),
    "wings": sprite(0, 14, [
        "kk..........................kk..",
        "kWkk......................kkWk..",
        "kWWWkk..................kkWWWk..",
        "kWeWWWk................kWWWeWk..",
        "kWWeWWk................kWWeWWk..",
        ".kWWeWk................kWeWWk...",
        ".kWeWWk................kWWeWk...",
        "..kWWk..................kWWk....",
        "...kk....................kk.....",
    ]),
    # ---- spin legendaries
    "galaxycrown": sprite(8, 2, [
        "..g....s....r...",
        "..k....k....k...",
        "k.kv.k.vk.k.vk.k",
        "kvkvkvvvkvkvvkvk",
        "kvvvvvvvvvvvvvvk",
        "kvgvsvvgvvsvgvvk",
        "kVVVVVVVVVVVVVVk",
        "kVsVVgVVsVVgVVsk",
        "kkkkkkkkkkkkkkkk",
    ]),
    "goldshades": sprite(6, 14, [
        "gggggggggggggggggg",
        ".gkkkkkg..gkkkkkg.",
        ".gkykkkggggkykkkg.",
        ".gkkkkkg..gkkkkkg.",
        "..ggggg....ggggg..",
    ]),
    "phoenix": sprite(0, 15, [
        "..g..........................g",
        ".gF..........................Fg",
        ".fFf........................fFf",
        "kffFk......................kFffk",
        "kfFffk....................kffFfk",
        ".kffFk...................kFffk.",
        ".kfgffk..................kffgfk",
        "..kffffk................kffffk.",
        "..kFfgfk...krrrrrrrrk...kfgfFk.",
        "...kffffkkkrRRRRRRRRrkkkffffk..",
        "....kkFffffffffffffffffffFkk...",
        "......kkkkkkkkkkkkkkkkkkkk.....",
    ]),
    # ---- season rewards
    "icecrown": sprite(9, 3, [
        "..i...i..i...i",
        ".kik.kikkik.ki",
        ".kiikiiiiiikik",
        "kiiiiiiiiiiiik",
        "kisiiisiiisiik",
        "kSSSSSSSSSSSSk",
        "kkkkkkkkkkkkkk",
    ]),
    "blossom": sprite(5, 4, [
        ".....rrr..........rr.",
        "....rgrrr..kkkkk.rgrr",
        "..kkrrrkkkkyyyykkkrr.",
        ".kyyyyyyyyyyyyyyyyyk.",
        "kyyyyyyrrrrrrrryyyyyk",
        "kyyyyyyyyyyyyyyyyyyyk",
        ".kkkkkkkkkkkkkkkkkkk.",
    ]),
    "sunhat": sprite(3, 4, [
        ".........kkkkkkkk.........",
        ".......kkyyyyyyyykk.......",
        "......kyyyyyyyyyyyyk......",
        "......krrrrrrrrrrrrk......",
        "...kkkkRRRRRRRRRRRRkkkk...",
        ".kkyyyyyyyyyyyyyyyyyyyykk.",
        "kyyyyyyyyyyyyyyyyyyyyyyyyk",
        ".kkkkkkkkkkkkkkkkkkkkkkkk.",
    ]),
    "bolt": sprite(6, 13, [
        "kkkkkkkk..kkkkkkkk",
        "kvvvgvvk..kvvvgvvk",
        "kvvggvvkkkkvvggvvk",
        "kvggggvk..kvggggvk",
        "kvvvggvk..kvvvggvk",
        ".kvvgvk....kvvgvk.",
        "..kkkk......kkkk..",
    ]),
    "pumpkin": sprite(7, 1, [
        ".........Mk.........",
        "........kMMk........",
        "....kkkkkkkkkkkk....",
        "..kkffFffFffFffFkk..",
        ".kfffFffFffFffFfffk.",
        "kffffFffFffFffFffffk",
        "kffkkFffFffFffFkkffk",
        "kfffFfkkkkkkkkfFfffk",
        ".kFFFFFFFFFFFFFFFFk.",
        "..kkkkkkkkkkkkkkkk..",
    ]),
    "cloak": sprite(2, 22, [
        "....kgkkkkkkkkkkkkkkkkgk....",
        "...knnngnnnnnnnnnnnnnnnnk...",
        "..knnnnk............knnnnk..",
        "..knsnnk............knngnk..",
        ".knnnngk............knnnnnk.",
        ".knnnnnk............ksnnnnk.",
        ".kngnnnk............knnnnnk.",
        ".kkkkkkk............kkkkkkk.",
    ]),
    # ---- event and album rewards
    "fishhat": sprite(6, 2, [
        "kk...............k",
        "ksk...kkkkkkkk..k.",
        "kssk.kssssssssskk.",
        "kSsskssSsSsSsssWsk",
        "kSsskssSsSsSsssnsk",
        "kssk.kssssssssssk.",
        "ksk...kkkkkkkkkk..",
        "kk................",
        ".kkkkkkkkkkkkkkkkk",
    ]),
    "explorer": sprite(5, 5, [
        ".......kkkkkkkk......",
        ".....kkccccccccck....",
        "....kccccccccccccck..",
        "....kCCCCCCCCCCCCCk..",
        "kkkkkkkkkkkkkkkkkkkkk",
        "kcccccccccccccccccccc",
        ".kkkkkkkkkkkkkkkkkkk.",
    ]),
    "scholar": sprite(6, 14, [
        "..ggggg....ggggg..",
        ".g.....g..g.....g.",
        ".g.....gggg.....g.",
        ".g.....g..g.....g.",
        "..ggggg....ggggg..",
    ]),
})


def recolor(im, shade):
    """A copy of a layer with each pixel passed through shade(x, y, rgb)."""
    out = im.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = shade(x, y, (r, g, b)) + (a,)
    return out


FUR, STRIPE, LIT, MUZZLE, NOSE = (PALETTE[c] for c in "odhwp")


def swap(colors):
    def shade(x, y, rgb):
        return colors.get(rgb, rgb)
    return shade


def calico(x, y, rgb):
    if rgb in (STRIPE, FUR, LIT):
        patch = (x // 6 + y // 5) % 3
        return {0: (245, 236, 224), 1: (232, 142, 68), 2: (62, 50, 58)}[patch]
    return rgb


def galaxy(x, y, rgb):
    if rgb in (FUR, STRIPE, LIT):
        star = (x * 7 + y * 13) % 37
        if star == 0:
            return (255, 255, 255)
        if star == 19:
            return PALETTE["g"]
        return (58, 40, 132) if (x + y) % 9 < 5 else (40, 26, 100)
    return {MUZZLE: (150, 130, 235), NOSE: (98, 198, 255)}.get(rgb, rgb)


BREEDS = {
    "tuxedo": swap({FUR: (46, 44, 64), STRIPE: (34, 32, 50), LIT: (74, 72, 98)}),
    "snow": swap({FUR: (248, 244, 240), STRIPE: (224, 218, 216), LIT: (255, 255, 255), MUZZLE: (255, 255, 255)}),
    "black": swap({FUR: (42, 36, 66), STRIPE: (28, 24, 48), LIT: (72, 64, 106), MUZZLE: (84, 76, 120)}),
    "grey": swap({FUR: (152, 158, 178), STRIPE: (104, 110, 132), LIT: (194, 200, 214)}),
    "siamese": swap({FUR: (240, 222, 190), STRIPE: (116, 82, 60), LIT: (250, 238, 214)}),
    "calico": calico,
    "galaxy": galaxy,
}


def main():
    os.makedirs(OUT, exist_ok=True)
    body = over(from_art(BODY), FACE_DETAIL)
    body.save(os.path.join(OUT, "body.png"))
    for name, im in {**EYES, **TAIL}.items():
        im.save(os.path.join(OUT, name + ".png"))
    for key, im in ACCESSORIES.items():
        im.save(os.path.join(OUT, "acc_" + key + ".png"))
    for breed, shade in BREEDS.items():
        recolor(body, shade).save(os.path.join(OUT, "body_" + breed + ".png"))
        for name, im in TAIL.items():
            recolor(im, shade).save(os.path.join(OUT, name + "_" + breed + ".png"))
    print(len(os.listdir(OUT)), "cat layers written")

    if len(sys.argv) > 1:
        keys = [None] + list(ACCESSORIES)
        scale, cols = 6, 8
        rows = (len(keys) + 2 + len(BREEDS)) // cols + 1
        sheet = Image.new("RGB", (cols * 32 * scale, rows * 32 * scale), (58, 46, 110))
        base = over(TAIL["tail_0"], body, EYES["eyes_open"])
        cells = [base if k is None else over(base, ACCESSORIES[k]) for k in keys]
        cells.append(over(TAIL["tail_1"], body, EYES["eyes_happy"]))
        cells.append(over(TAIL["tail_2"], body, EYES["eyes_closed"], ACCESSORIES["crown"], ACCESSORIES["hearts"], ACCESSORIES["scarf"]))
        for shade in BREEDS.values():
            cells.append(over(recolor(TAIL["tail_0"], shade), recolor(body, shade), EYES["eyes_open"]))
        for i, cell in enumerate(cells):
            big = cell.resize((32 * scale, 32 * scale), Image.NEAREST)
            sheet.paste(big, ((i % cols) * 32 * scale, (i // cols) * 32 * scale), big)
        sheet.save(sys.argv[1])


if __name__ == "__main__":
    main()
