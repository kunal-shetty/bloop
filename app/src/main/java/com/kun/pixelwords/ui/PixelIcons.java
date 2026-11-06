package com.kun.pixelwords.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

/** Hand-drawn square pixel icons (mostly 12x12), kept as text so they need no image files and stay easy to edit. */
public final class PixelIcons {

    private PixelIcons() {}

    private static final Map<String, String[]> ART = new HashMap<>();
    private static final Map<String, Bitmap> CACHE = new HashMap<>();

    private static int color(char c) {
        switch (c) {
            case 'k': return PixelTheme.INK;
            case 'g': return PixelTheme.GOLD;
            case 'y': return 0xFFFFF1B0;
            case 'o': return PixelTheme.GOLD_DARK;
            case 'w': return 0xFFFFFFFF;
            case 'd': return PixelTheme.TEXT_DIM;
            case 'm': return PixelTheme.MINT;
            case 'n': return 0xFF2FA873;
            case 'r': return PixelTheme.ROSE;
            case 'x': return 0xFFB83A5E;
            case 's': return PixelTheme.SKY;
            case 'b': return 0xFF2E7FBF;
            case 'f': return 0xFFFF9A3C;
            case 'p': return PixelTheme.PANEL_LIGHT;
            case 'q': return 0xFF705CBE;
            case 'u': return 0xFF40327C;
            default: return 0;
        }
    }

    static {
        ART.put("coin", new String[]{
                "...kkkkkk...",
                "..kyyyyggk..",
                ".kyggggggok.",
                "kygggyoggook",
                "kygggyoggook",
                "kygggyoggook",
                "kygggyoggook",
                "kygggyoggook",
                "kygggyoggook",
                ".kggggggook.",
                "..kooooook..",
                "...kkkkkk..."});
        ART.put("bulb", new String[]{
                "...kkkkkk...",
                "..kyyyyggk..",
                ".kyyggggggk.",
                ".kyggggggok.",
                ".kggggggook.",
                ".kggggggook.",
                "..kgggggok..",
                "...kggook...",
                "...kdddwk...",
                "...kdddwk...",
                "....kddk....",
                ".....kk....."});
        ART.put("shuffle", new String[]{
                "............",
                "........kk..",
                "kkkk....kwk.",
                "kwwwk..kkwwk",
                "kkkkwkkwwwk.",
                "....kwwkkk..",
                "....kwwkkk..",
                "kkkkwkkwwwk.",
                "kwwwk..kkwwk",
                "kkkk....kwk.",
                "........kk..",
                "............"});
        ART.put("pause", new String[]{
                "............",
                "..kkkk.kkkk.",
                "..kwwk.kwwk.",
                "..kwwk.kwwk.",
                "..kwwk.kwwk.",
                "..kwwk.kwwk.",
                "..kwwk.kwwk.",
                "..kwwk.kwwk.",
                "..kwwk.kwwk.",
                "..kwwk.kwwk.",
                "..kkkk.kkkk.",
                "............"});
        ART.put("play", new String[]{
                "...kk.......",
                "...kwkk.....",
                "...kwwwkk...",
                "...kwwwwwkk.",
                "...kwwwwwwwk",
                "...kwwwwwwwk",
                "...kwwwwwwwk",
                "...kwwwwwwwk",
                "...kwwwwwkk.",
                "...kwwwkk...",
                "...kwkk.....",
                "...kk......."});
        ART.put("gear", new String[]{
                "....kkkk....",
                ".kk.kddk.kk.",
                ".kdkkddkkdk.",
                "..kddddddk..",
                "kkkddkkddkkk",
                "kdddk..kdddk",
                "kdddk..kdddk",
                "kkkddkkddkkk",
                "..kddddddk..",
                ".kdkkddkkdk.",
                ".kk.kddk.kk.",
                "....kkkk...."});
        ART.put("clock", new String[]{
                "...kkkkkk...",
                "..kssssssk..",
                ".kssswwsssk.",
                "kssssbbssssk",
                "kssssbbssssk",
                "kssssbbsssk.",
                "kssssbbbbssk",
                "ksssssssssbk",
                ".kssssssssk.",
                ".kbssssssbk.",
                "..kbbbbbbk..",
                "...kkkkkk..."});
        ART.put("swords", new String[]{
                "kk........kk",
                "kwk......kwk",
                ".kwk....kwk.",
                "..kwk..kwk..",
                "...kwkkwk...",
                "....kwwk....",
                "....kwwk....",
                "...kwkkwk...",
                ".k.kkk.kk.k.",
                ".kgkk...kgk.",
                "..kgk..kgk..",
                "kkk.k..k.kkk"});
        ART.put("trophy", new String[]{
                ".kkkkkkkkkk.",
                "kkygggggookk",
                "kokygggggkok",
                "kokyggggokok",
                ".kkyggggokk.",
                "...kgggok...",
                "....kgok....",
                ".....ko.....",
                ".....ko.....",
                "...kkggkk...",
                "..kggggook..",
                "..kkkkkkkk.."});
        ART.put("calendar", new String[]{
                ".kk.kkkk.kk.",
                "kkrkkkkkkrkk",
                "krrrrrrrrrrk",
                "krrrrrrrrrrk",
                "kkkkkkkkkkkk",
                "kwwwwwwwwwwk",
                "kwkkwkkwkkwk",
                "kwwwwwwwwwwk",
                "kwkkwkkwkkwk",
                "kwwwwwwwwwwk",
                "kddddddddddk",
                ".kkkkkkkkkk."});
        ART.put("flame", new String[]{
                ".....kk.....",
                ".....krk....",
                "....krrk....",
                "...krrrk.k..",
                "..krrfrkkrk.",
                "..krffrrrrk.",
                ".krfffrffrrk",
                ".krffgffffrk",
                ".krfggggffrk",
                "..krfgyyfrk.",
                "...krfgyrk..",
                "....kkkkk..."});
        ART.put("share", new String[]{
                ".....kk.....",
                "....kkkk....",
                "...kkkkkk...",
                "..kk.kk.kk..",
                ".....kk.....",
                ".....kk.....",
                ".....kk.....",
                ".k...kk...k.",
                ".k........k.",
                ".k........k.",
                ".kkkkkkkkkk.",
                "............"});
        ART.put("check", new String[]{
                "............",
                "..........kk",
                ".........kmk",
                "........kmmk",
                ".......kmmk.",
                "kk....kmmk..",
                "kmk..kmmk...",
                "kmmkkmmk....",
                ".kmmmmk.....",
                "..kmmk......",
                "...kk.......",
                "............"});
        ART.put("close", new String[]{
                "............",
                ".kk......kk.",
                ".kkk....kkk.",
                "..kkk..kkk..",
                "...kkkkkk...",
                "....kkkk....",
                "....kkkk....",
                "...kkkkkk...",
                "..kkk..kkk..",
                ".kkk....kkk.",
                ".kk......kk.",
                "............"});
        ART.put("star", new String[]{
                ".................",
                ".......kkk.......",
                ".......kyk.......",
                "......kyygk......",
                "......kyggk......",
                ".....kyggggk.....",
                "kkkkkyggggggkkkkk",
                ".kyyygggggggggok.",
                "..kygggggggggok..",
                "...kggggggggok...",
                "....kgggggook....",
                "....kggggoook....",
                "...kgggkkkoook...",
                "...kggk...kook...",
                "...kkk.....kkk...",
                ".................",
                "................."});
        ART.put("star_off", new String[]{
                ".................",
                ".......kkk.......",
                ".......kqk.......",
                "......kqqpk......",
                "......kqppk......",
                ".....kqppppk.....",
                "kkkkkqppppppkkkkk",
                ".kqqqpppppppppuk.",
                "..kqpppppppppuk..",
                "...kppppppppuk...",
                "....kpppppuuk....",
                "....kppppuuuk....",
                "...kpppkkkuuuk...",
                "...kppk...kuuk...",
                "...kkk.....kkk...",
                ".................",
                "................."});
        ART.put("sound", new String[]{
                "............",
                ".....kk...k.",
                "....kdk.k.dk",
                "...kddk..kdk",
                "kkkdddk.k.dk",
                "kdddddk.k.dk",
                "kdddddk.k.dk",
                "kkkdddk.k.dk",
                "...kddk..kdk",
                "....kdk.k.dk",
                ".....kk...k.",
                "............"});
        ART.put("buzz", new String[]{
                "....kkkk....",
                "...kdddwk...",
                ".k.kkkkkk.k.",
                "k..kssssk..k",
                "k.kksssskk.k",
                "k..kssssk..k",
                "k.kksssskk.k",
                "k..kssssk..k",
                ".k.kkkkkk.k.",
                "...kdkkdk...",
                "...kddddk...",
                "....kkkk...."});
        ART.put("cloud", new String[]{
                "............",
                "....kkkk....",
                "...kwwwwkk..",
                ".kkwwwwwwwk.",
                "kwwwwwwwwwwk",
                "kwwwwwwwwwwk",
                "kwwwwwwwwwdk",
                "kddddddddddk",
                ".kkkkkkkkkk.",
                "............",
                "............",
                "............"});
        ART.put("film", new String[]{
                "kkkkkkkkkkkk",
                "kwkdddddkwdk",
                "kkkdddddkkkk",
                "kwkddkddkwk.",
                "kkkddkkdkkkk",
                "kwkddkkkkwk.",
                "kkkddkkdkkkk",
                "kwkddkddkwk.",
                "kkkdddddkkkk",
                "kwkdddddkwk.",
                "kkkkkkkkkkkk",
                "............"});
        ART.put("book", new String[]{
                "............",
                ".kkkkk.kkkk.",
                "kwwwwwkwwwwk",
                "kwkkkwkwkkwk",
                "kwwwwwkwwwwk",
                "kwkkkwkwkkwk",
                "kwwwwwkwwwwk",
                "kwkkkwkwkkwk",
                "kwwwwwkwwwwk",
                ".kkkkkkkkkk.",
                "............",
                "............"});
        ART.put("heart", new String[]{
                "............",
                "..kkk..kkk..",
                ".krrrkkrrrk.",
                "krwrrrrrrrxk",
                "krwrrrrrrrxk",
                "krrrrrrrrrxk",
                ".krrrrrrrxk.",
                "..krrrrrxk..",
                "...krrrxk...",
                "....krxk....",
                ".....kk.....",
                "............"});
        ART.put("freeze", new String[]{
                ".....kk.....",
                ".k..kwwk..k.",
                "ksk.kwwk.ksk",
                ".kskkwwkksk.",
                "..ksswwssk..",
                "kkkswwwwskkk",
                "kwwwwwwwwwwk",
                "kkkswwwwskkk",
                "..ksswwssk..",
                ".kskkwwkksk.",
                "ksk.kwwk.ksk",
                ".k...kk..k.."});
        ART.put("bag", new String[]{
                "....kkkk....",
                "...k....k...",
                "...k....k...",
                ".kkkkkkkkkk.",
                ".kwwwwwwwdk.",
                ".kwkwwwwkdk.",
                ".kwwwwwwwdk.",
                ".krrrrrrrxk.",
                ".krrrrrrrxk.",
                ".kwwwwwwwdk.",
                ".kwwwwwwwdk.",
                ".kkkkkkkkkk."});
        ART.put("back", new String[]{
                "............",
                ".....kk.....",
                "....kwk.....",
                "...kwwkkkkk.",
                "..kwwwwwwwwk",
                ".kwwwwwwwwwk",
                ".kwwwwwwwwwk",
                "..kwwwwwwwwk",
                "...kwwkkkkk.",
                "....kwk.....",
                ".....kk.....",
                "............"});
        ART.put("sparkle", new String[]{
                ".....kk.....",
                ".....kyk....",
                ".....kyk....",
                "....kyggk...",
                "..kkygggkk..",
                "kkyyyggggokk",
                "..kkgggookk.",
                "....kgok....",
                ".....kok....",
                ".....kok....",
                ".....kk.....",
                "............"});
    }

    public static Bitmap bitmap(String name) {
        Bitmap b = CACHE.get(name);
        if (b != null) return b;
        String[] art = ART.get(name);
        if (art == null) throw new IllegalArgumentException("no icon " + name);
        int n = art.length;
        b = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
        for (int y = 0; y < n; y++) {
            String row = art[y];
            for (int x = 0; x < n && x < row.length(); x++) b.setPixel(x, y, color(row.charAt(x)));
        }
        CACHE.put(name, b);
        return b;
    }

    private static final Rect SRC = new Rect();
    private static final RectF DST = new RectF();

    /** Draws an icon at a whole multiple of its pixels, so each one stays perfectly square. */
    public static void draw(Canvas c, Paint p, String name, float x, float y, float size) {
        Bitmap b = bitmap(name);
        int n = b.getWidth();
        float s = Math.max(n, (float) Math.floor(size / n) * n);
        p.setFilterBitmap(false);
        p.setAlpha(255);
        SRC.set(0, 0, n, n);
        DST.set(x + (size - s) / 2f, y + (size - s) / 2f, x + (size + s) / 2f, y + (size + s) / 2f);
        c.drawBitmap(b, SRC, DST, p);
    }

    /** A simple view showing one icon. */
    public static View view(Context context, final String name) {
        return new View(context) {
            private final Paint paint = new Paint();

            @Override
            protected void onDraw(Canvas c) {
                PixelIcons.draw(c, paint, name, 0, 0, Math.min(getWidth(), getHeight()));
            }
        };
    }
}
