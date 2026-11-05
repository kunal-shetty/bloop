package com.kun.pixelwords.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.widget.TextView;

/** Shared palette, pixel fonts and drawing helpers for the blocky look. */
public final class PixelTheme {

    private PixelTheme() {}

    public static final int BG_DEEP = 0xFF14102B;
    public static final int BG_MID = 0xFF251D4A;
    public static final int PANEL = 0xFF3A2E6E;
    public static final int PANEL_LIGHT = 0xFF5846A3;
    public static final int INK = 0xFF0B0820;
    public static final int GOLD = 0xFFFFD34D;
    public static final int GOLD_DARK = 0xFFC48A12;
    public static final int MINT = 0xFF6BE3A8;
    public static final int ROSE = 0xFFFF6B8B;
    public static final int SKY = 0xFF62C6FF;
    public static final int CREAM = 0xFFFFF1D6;
    public static final int TEXT = 0xFFF4EFFF;
    public static final int TEXT_DIM = 0xFFA79BD6;

    /** Press Start 2P: titles, buttons and letters. Drawn on an 8-pixel grid. */
    public static Typeface DISPLAY = Typeface.MONOSPACE;
    /** VT323: readable body copy. Drawn on a 16-pixel grid. */
    public static Typeface BODY = Typeface.MONOSPACE;

    public static void init(Context c) {
        try {
            DISPLAY = Typeface.createFromAsset(c.getAssets(), "fonts/PressStart2P.ttf");
            BODY = Typeface.createFromAsset(c.getAssets(), "fonts/VT323.ttf");
        } catch (RuntimeException e) {
            DISPLAY = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
            BODY = Typeface.MONOSPACE;
        }
    }

    public static float dp(Context c, float v) {
        return v * c.getResources().getDisplayMetrics().density;
    }

    /** Nearest crisp size for the display font: pixel fonts only look sharp at whole multiples of their grid. */
    public static float displayPx(Context c, float targetDp) {
        return Math.max(8f, Math.round(dp(c, targetDp) / 8f) * 8f);
    }

    public static float bodyPx(Context c, float targetDp) {
        return Math.max(16f, Math.round(dp(c, targetDp) / 16f) * 16f);
    }

    public static void styleDisplay(Paint p, float px) {
        p.setTypeface(DISPLAY);
        p.setTextSize(px);
        p.setAntiAlias(false);
        p.setHinting(Paint.HINTING_OFF); // hinting nudges pixel-font outlines off their grid
    }

    public static void styleBody(Paint p, float px) {
        p.setTypeface(BODY);
        p.setTextSize(px);
        p.setAntiAlias(false);
        p.setHinting(Paint.HINTING_OFF);
    }

    public static TextView display(Context c, CharSequence s, float targetDp, int color) {
        TextView t = baseText(c, s, color);
        t.setTypeface(DISPLAY);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, displayPx(c, targetDp));
        t.setLineSpacing(dp(c, 4), 1f);
        return t;
    }

    public static TextView body(Context c, CharSequence s, float targetDp, int color) {
        TextView t = baseText(c, s, color);
        t.setTypeface(BODY);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, bodyPx(c, targetDp));
        return t;
    }

    private static TextView baseText(Context c, CharSequence s, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        t.setPaintFlags(t.getPaintFlags() & ~Paint.ANTI_ALIAS_FLAG);
        t.getPaint().setHinting(Paint.HINTING_OFF);
        return t;
    }

    /** {@code f} below 1 darkens, above 1 moves towards white. */
    public static int shade(int color, float f) {
        int r = Color.red(color), g = Color.green(color), b = Color.blue(color);
        if (f <= 1f) {
            return Color.argb(Color.alpha(color), (int) (r * f), (int) (g * f), (int) (b * f));
        }
        float t = Math.min(1f, f - 1f);
        return Color.argb(Color.alpha(color), (int) (r + (255 - r) * t), (int) (g + (255 - g) * t), (int) (b + (255 - b) * t));
    }

    /**
     * A chunky bevelled tile with notched corners. {@code depth} is the height of the darker lip along
     * the bottom that makes it look raised; the top of the tile is at {@code t}, the lip ends at {@code b}.
     */
    public static void drawTile(Canvas c, Paint p, float l, float t, float r, float b, int fill, float px, float depth) {
        p.setAntiAlias(false);
        p.setStyle(Paint.Style.FILL);
        p.setColor(INK);
        c.drawRect(l + px, t, r - px, b, p);
        c.drawRect(l, t + px, r, b - px, p);

        float faceBottom = b - px - depth;
        if (depth > 0) {
            p.setColor(shade(fill, 0.55f));
            c.drawRect(l + px, faceBottom, r - px, b - px, p);
        }
        p.setColor(fill);
        c.drawRect(l + px, t + px, r - px, faceBottom, p);

        p.setColor(shade(fill, 1.4f));
        c.drawRect(l + px * 2, t + px, r - px * 2, t + px * 2, p);
        c.drawRect(l + px, t + px * 2, l + px * 2, faceBottom - px, p);
        p.setColor(shade(fill, 0.82f));
        c.drawRect(l + px * 2, faceBottom - px, r - px, faceBottom, p);
        c.drawRect(r - px * 2, t + px * 2, r - px, faceBottom, p);
    }

    /** An empty recessed slot: the shadow sits on the top edge instead of the bottom. */
    public static void drawSlot(Canvas c, Paint p, float l, float t, float r, float b, int fill, float px) {
        p.setAntiAlias(false);
        p.setStyle(Paint.Style.FILL);
        p.setColor(INK);
        c.drawRect(l + px, t, r - px, b, p);
        c.drawRect(l, t + px, r, b - px, p);
        p.setColor(fill);
        c.drawRect(l + px, t + px, r - px, b - px, p);
        p.setColor(shade(fill, 0.6f));
        c.drawRect(l + px, t + px, r - px, t + px * 2.5f, p);
    }
}
