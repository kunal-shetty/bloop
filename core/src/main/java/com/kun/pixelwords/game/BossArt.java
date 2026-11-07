package com.kun.pixelwords.game;

import com.kun.pixelwords.generator.SeedUtil;

import java.util.Random;

/**
 * Draws a boss for a generated region. A creature, something on its head and a pair of eyes are picked
 * from the chapter number and painted in colours of their own, so no two bosses ever look the same.
 * Each piece is the left half of the sprite, and is mirrored to make the right.
 */
final class BossArt {

    private BossArt() {}

    static final int SIZE = 16;

    private static final long SALT = 0x424F_5353_4152_5431L;
    private static final int HALF = SIZE / 2;
    private static final int INK = 0xFF0B0820;

    /** Twelve rows each. b is the body, d its shade, l its light, a the accent, e the eye, m the mouth. */
    private static final String[][] CREATURES = {
            { // mouse
                    "..kkk...",
                    ".kaaak..",
                    ".kaaakkk",
                    "..kkkbbb",
                    "..kbbbbb",
                    ".kblbbbb",
                    ".kbeebbb",
                    ".kbeebbb",
                    ".kbbbbbb",
                    "..kbbbbm",
                    "...kbbbd",
                    "....kkkk"},
            { // bat
                    ".....k..",
                    "k....kk.",
                    "kk...kbk",
                    "kak..kbb",
                    "kaakkbbb",
                    "kaaakbbb",
                    "kaaakeeb",
                    "kaakbeeb",
                    "kak.kbbb",
                    "kk...kbm",
                    "k.....kd",
                    "......kk"},
            { // owl
                    ".k....k.",
                    ".kk..kbk",
                    ".kbkkbbb",
                    ".kbbbbbb",
                    "kbleeebb",
                    "kbleeebb",
                    "kbleeebb",
                    "kbbbbbmm",
                    "kdbabbam",
                    ".kdbabba",
                    "..kdbbab",
                    "...kkkkk"},
            { // fox
                    "k.......",
                    "kk......",
                    "kak...kk",
                    "kaak.kbb",
                    "kaakkbbb",
                    ".kbbbbbb",
                    ".kbeebbb",
                    ".kleebbb",
                    "..kllbbb",
                    "...kllbm",
                    "....kllm",
                    ".....kkk"},
            { // toad
                    "..kkk...",
                    ".keeek..",
                    ".keeekkk",
                    ".kbbbbbb",
                    "kbbabbbb",
                    "kbbbbbbb",
                    "kbbbbbab",
                    "kmmmmmmm",
                    "kdbbbbbb",
                    ".kdlllll",
                    "..kkllll",
                    "....kkkk"},
            { // ghost
                    "....kkkk",
                    "...kllll",
                    "..kbbbbb",
                    ".kbbbbbb",
                    ".kbeebbb",
                    ".kbeebbb",
                    ".kbbbbbm",
                    ".kbbbbbb",
                    ".kbbbbbb",
                    ".kdbbbbb",
                    ".kdkbbkb",
                    ".kk.kk.k"},
            { // beetle
                    ".k....k.",
                    "..k..k..",
                    "..kkkkkk",
                    ".kbbbbee",
                    "kaabbbee",
                    "kaaabbkk",
                    "kaaaabka",
                    "kdaaaaka",
                    "kbdaaaka",
                    ".kbdaaka",
                    "..kbbdka",
                    "...kkkkk"},
            { // crab
                    "kk......",
                    "kak.....",
                    "kaak.kee",
                    "kaak.kee",
                    ".kk.kkbk",
                    "..kkbbbb",
                    ".kbbbbbb",
                    "kdbblbbb",
                    "kbbbbbbm",
                    ".kdbbbbb",
                    ".k.kkkkk",
                    "k...k..k"},
    };

    /** Four rows each. g is the headgear's colour, h its highlight. */
    private static final String[][] HEADGEAR = {
            {"........", "........", "........", "........"},
            {"..g..g..", "..gg.gg.", "..gggggg", "..hhhhhh"}, // crown
            {".k......", ".gk.....", ".ggk....", "..ggk..."}, // horns
            {"....k...", ".....k..", ".....gk.", "......gg"}, // antennae
            {"......gk", ".....ggg", "....gggg", "..hhhhhh"}, // tall hat
            {"....g..g", "...gg.gg", "..gggggg", "..gggggg"}, // spikes
            {"........", "...gggg.", "..hhhhhh", "..gggggg"}, // cap
    };

    private static final int EYES_ROUND = 0, EYES_ANGRY = 1, EYE_KINDS = 3; // anything else is sleepy

    /** The boss's face as rows of pixels, SIZE by SIZE. */
    static int[] paint(int chapter, double hue) {
        Random r = new Random(SeedUtil.mix(SALT, chapter));
        String[] creature = CREATURES[r.nextInt(CREATURES.length)];
        String[] headgear = HEADGEAR[r.nextInt(HEADGEAR.length)];
        int eyes = r.nextInt(EYE_KINDS);

        float saturation = 0.45f + r.nextFloat() * 0.4f;
        float value = 0.7f + r.nextFloat() * 0.25f;
        double bodyHue = hue + 40 + r.nextFloat() * 280;
        double accentHue = bodyHue + 90 + r.nextFloat() * 180;
        int body = Region.hsv(bodyHue, saturation, value);
        int dark = Region.hsv(bodyHue, saturation, value * 0.65f);
        int light = Region.hsv(bodyHue, saturation * 0.5f, 1f);
        int accent = Region.hsv(accentHue, 0.7f, 0.95f);
        int gear = Region.hsv(accentHue + 60 + r.nextFloat() * 240, 0.8f, 0.95f);
        int shine = Region.hsv(accentHue + 60, 0.3f, 1f);
        int eye = Region.hsv(hue + r.nextFloat() * 360, 0.25f + r.nextFloat() * 0.5f, 1f);

        int[] pixels = new int[SIZE * SIZE];
        int offset = SIZE - creature.length;
        for (int y = 0; y < creature.length; y++) {
            String row = creature[y];
            for (int x = 0; x < HALF; x++) {
                int colour = colourOf(row.charAt(x), body, dark, light, accent, eye);
                pixels[(y + offset) * SIZE + x] = colour;
                pixels[(y + offset) * SIZE + SIZE - 1 - x] = colour;
            }
        }
        stampEyes(pixels, eye, eyes);
        for (int y = 0; y < headgear.length; y++) {
            for (int x = 0; x < HALF; x++) {
                char c = headgear[y].charAt(x);
                if (c == '.') continue;
                int colour = c == 'k' ? INK : c == 'h' ? shine : gear;
                pixels[y * SIZE + x] = colour;
                pixels[y * SIZE + SIZE - 1 - x] = colour;
            }
        }
        return pixels;
    }

    private static int colourOf(char c, int body, int dark, int light, int accent, int eye) {
        switch (c) {
            case 'k':
            case 'm':
                return INK;
            case 'b': return body;
            case 'd': return dark;
            case 'l': return light;
            case 'a': return accent;
            case 'e': return eye;
            default: return 0;
        }
    }

    /** Finishes each eye: a pupil, plus a brow or a lowered lid. */
    private static void stampEyes(int[] pixels, int eye, int kind) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < HALF; x++) {
                if (pixels[y * SIZE + x] != eye) continue;
                boolean bottom = y + 1 >= SIZE || pixels[(y + 1) * SIZE + x] != eye;
                boolean inner = pixels[y * SIZE + x + 1] != eye;
                boolean top = pixels[(y - 1) * SIZE + x] != eye;
                boolean ink = kind == EYES_ROUND ? bottom && inner : kind == EYES_ANGRY ? (top && inner) || (bottom && inner) : top;
                if (!ink) continue;
                pixels[y * SIZE + x] = INK;
                pixels[y * SIZE + SIZE - 1 - x] = INK;
            }
        }
    }
}
