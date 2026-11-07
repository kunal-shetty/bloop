package com.kun.pixelwords.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

/**
 * Animated pixel-art backdrop: a dithered sky with stars, a moon and layered hills, rendered once to a
 * tiny bitmap and scaled up, with weather particles drifting over it.
 */
public class AmbientView extends View {

    public enum Theme {
        SNOW(0xFF0F1A3D, 0xFF2C4A7E, 0xFF22375F, 0xFF16223F, 0xFFE8F4FF, 0xFFFFFFFF, 0f, 40f, 0f),
        PETALS(0xFF2A1240, 0xFF8A3B6E, 0xFF4F2358, 0xFF2E1538, 0xFFFFD9E6, 0xFFFFB3C7, -25f, 30f, 18f),
        FIREFLIES(0xFF0C1030, 0xFF1E5A66, 0xFF143E46, 0xFF0B2228, 0xFFF4F7C4, 0xFFE9FF7A, 0f, 0f, 14f),
        LEAVES(0xFF2B1433, 0xFFB5552E, 0xFF5C2A24, 0xFF321618, 0xFFFFC56B, 0xFFFF9A3C, -35f, 45f, 22f);

        final int skyTop, skyBottom, hillFar, hillNear, moon, particle;
        final float vx;   // base horizontal speed, px/sec per dp
        final float vy;   // base fall speed, px/sec per dp
        final float sway; // sinusoidal wobble amplitude

        Theme(int skyTop, int skyBottom, int hillFar, int hillNear, int moon, int particle, float vx, float vy, float sway) {
            this.skyTop = skyTop;
            this.skyBottom = skyBottom;
            this.hillFar = hillFar;
            this.hillNear = hillNear;
            this.moon = moon;
            this.particle = particle;
            this.vx = vx;
            this.vy = vy;
            this.sway = sway;
        }
    }

    private static final int COUNT = 44;
    private static final int[][] BAYER = {{0, 8, 2, 10}, {12, 4, 14, 6}, {3, 11, 1, 9}, {15, 7, 13, 5}};

    private final Paint paint = new Paint();
    private final float[] x = new float[COUNT];
    private final float[] y = new float[COUNT];
    private final float[] depth = new float[COUNT];
    private final float[] phase = new float[COUNT];
    private final Random rnd = new Random(1234);
    private final Rect dst = new Rect();
    private Theme theme = Theme.SNOW;
    private Bitmap scene;
    private long last;
    private boolean seeded;

    public AmbientView(Context context) {
        this(context, null);
    }

    public AmbientView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setAntiAlias(false);
        paint.setFilterBitmap(false);
        setClickable(false);
    }

    public void setTheme(Theme t) {
        if (t == theme && scene != null) return;
        theme = t;
        scene = null;
        seeded = false;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        scene = null;
    }

    private static int lerp(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private void buildScene(int w, int h) {
        int cell = Math.max(2, Math.round(PixelTheme.dp(getContext(), 2.5f)));
        int sw = w / cell + 1, sh = h / cell + 1;
        int[] px = new int[sw * sh];
        Random r = new Random(theme.ordinal() * 91L + 7);

        // Sky: five flat bands with ordered dithering between them, for that classic console gradient.
        int bands = 5;
        for (int yy = 0; yy < sh; yy++) {
            float t = Math.min(1f, yy / (sh * 0.72f));
            float f = t * (bands - 1);
            int band = (int) f;
            float frac = f - band;
            for (int xx = 0; xx < sw; xx++) {
                int step = frac * 16 > BAYER[yy & 3][xx & 3] ? band + 1 : band;
                px[yy * sw + xx] = lerp(theme.skyTop, theme.skyBottom, step / (float) (bands - 1));
            }
        }

        for (int i = 0; i < sw * sh / 500; i++) {
            int sx = r.nextInt(sw), sy = r.nextInt((int) (sh * 0.55f));
            px[sy * sw + sx] = r.nextInt(5) == 0 ? 0xFFFFFFFF : lerp(theme.skyTop, 0xFFFFFFFF, 0.55f);
        }

        // Moon with a soft dithered halo.
        int mx = (int) (sw * 0.76f), my = (int) (sh * 0.13f), mr = sw / 11;
        for (int yy = Math.max(0, my - mr * 2); yy < Math.min(sh, my + mr * 2); yy++) {
            for (int xx = Math.max(0, mx - mr * 2); xx < Math.min(sw, mx + mr * 2); xx++) {
                float d = (float) Math.hypot(xx - mx, yy - my);
                if (d <= mr) {
                    boolean crater = Math.hypot(xx - mx - mr * 0.3, yy - my + mr * 0.2) < mr * 0.25
                            || Math.hypot(xx - mx + mr * 0.4, yy - my - mr * 0.3) < mr * 0.18;
                    px[yy * sw + xx] = crater ? lerp(theme.moon, theme.skyBottom, 0.3f) : theme.moon;
                } else if (d < mr * 1.6f && (xx + yy) % 2 == 0) {
                    px[yy * sw + xx] = lerp(px[yy * sw + xx], theme.moon, 0.22f);
                }
            }
        }

        hills(px, sw, sh, theme.hillFar, 0.70f, 0.075f, 0.011f, 1.3f);
        hills(px, sw, sh, lerp(theme.hillFar, theme.hillNear, 0.5f), 0.79f, 0.06f, 0.019f, 4.1f);
        hills(px, sw, sh, theme.hillNear, 0.88f, 0.045f, 0.03f, 2.2f);

        scene = Bitmap.createBitmap(px, sw, sh, Bitmap.Config.ARGB_8888);
    }

    private static void hills(int[] px, int sw, int sh, int color, float base, float amp, float freq, float seed) {
        for (int xx = 0; xx < sw; xx++) {
            double wave = Math.sin(xx * freq * 6 + seed) + 0.5 * Math.sin(xx * freq * 13 + seed * 2);
            int top = (int) (sh * (base + amp * wave * 0.66));
            for (int yy = Math.max(0, top); yy < sh; yy++) px[yy * sw + xx] = color;
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;
        if (scene == null) buildScene(w, h);
        if (!seeded) {
            for (int i = 0; i < COUNT; i++) respawn(i, w, h, true);
            seeded = true;
        }
        int cell = w / Math.max(1, scene.getWidth() - 1);
        dst.set(0, 0, scene.getWidth() * cell, scene.getHeight() * cell);
        paint.setAlpha(255); // the last particle left its transparency here, which made the whole sky pulse
        c.drawBitmap(scene, null, dst, paint);
        if (PixelTheme.reduceMotion()) return; // the scenery stays, and the weather stops

        long now = SystemClock.uptimeMillis();
        float dt = last == 0 ? 0.016f : Math.min(0.05f, (now - last) / 1000f);
        last = now;
        float dp = PixelTheme.dp(getContext(), 1);

        for (int i = 0; i < COUNT; i++) {
            float d = depth[i]; // 0.4 (far) .. 1.0 (near)
            phase[i] += dt * (1f + d);
            if (theme == Theme.FIREFLIES) {
                x[i] += (float) Math.sin(phase[i] * 1.3f) * 20f * dp * dt;
                y[i] += (float) Math.cos(phase[i] * 0.9f) * 14f * dp * dt;
            } else {
                x[i] += (theme.vx * dp * d + (float) Math.sin(phase[i]) * theme.sway * dp) * dt;
                y[i] += theme.vy * dp * d * dt;
            }
            if (y[i] > h + 8 || x[i] < -16 || x[i] > w + 16 || y[i] < -16) respawn(i, w, h, false);

            float size = Math.max(2f, (2f + 3f * d) * dp);
            int color = theme.particle;
            if (theme == Theme.FIREFLIES) {
                float pulse = 0.5f + 0.5f * (float) Math.sin(phase[i] * 2f);
                color = (color & 0x00FFFFFF) | ((int) (60 + 195 * pulse) << 24);
            } else {
                color = (color & 0x00FFFFFF) | ((int) (90 + 140 * d) << 24);
            }
            paint.setColor(color);
            // Snap to a coarse grid so motion stays chunky.
            float snap = Math.max(2f, dp * 2f);
            float px = Math.round(x[i] / snap) * snap;
            float py = Math.round(y[i] / snap) * snap;
            c.drawRect(px, py, px + size, py + size, paint);
        }
        postInvalidateOnAnimation();
    }

    private void respawn(int i, int w, int h, boolean anywhere) {
        depth[i] = 0.4f + 0.6f * rnd.nextFloat();
        phase[i] = rnd.nextFloat() * 6.28f;
        x[i] = rnd.nextFloat() * w;
        if (theme == Theme.FIREFLIES || anywhere) {
            y[i] = rnd.nextFloat() * h;
        } else {
            y[i] = -8;
            if (theme.vx < 0 && rnd.nextBoolean()) { // drifting left: some enter from the right edge
                x[i] = w + 8;
                y[i] = rnd.nextFloat() * h;
            }
        }
    }
}
