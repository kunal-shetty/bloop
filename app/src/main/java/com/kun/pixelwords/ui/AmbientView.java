package com.kun.pixelwords.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import com.kun.pixelwords.game.Region;

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

    /** The light at one time of day, laid over a scene. Night has none: the scene keeps its own colours. */
    private enum Light {
        DAWN(0xFF3C2C78, 0xFFFF9E7A, 0xFF4A3A6E, 0xFF2C2448, 0xFFFFD9A0, 0xFFFFE9C4, false),
        DAY(0xFF2F86DA, 0xFFA8E2FF, 0xFF4E9E74, 0xFF2F6E56, 0xFFFFE27A, 0xFFFFFFFF, true),
        DUSK(0xFF2B1A5A, 0xFFFF7A59, 0xFF4B2A5C, 0xFF2A1A3A, 0xFFFFC56B, 0xFFFFD08A, false);

        final int skyTop, skyBottom, hillFar, hillNear, sun, particle;
        final boolean noStars;

        Light(int skyTop, int skyBottom, int hillFar, int hillNear, int sun, int particle, boolean noStars) {
            this.skyTop = skyTop;
            this.skyBottom = skyBottom;
            this.hillFar = hillFar;
            this.hillNear = hillNear;
            this.sun = sun;
            this.particle = particle;
            this.noStars = noStars;
        }

        static Light at(int hour) {
            if (hour >= 5 && hour < 8) return DAWN;
            if (hour >= 8 && hour < 17) return DAY;
            if (hour >= 17 && hour < 20) return DUSK;
            return null;
        }
    }

    /** Everything the view needs to draw a scene, whether it came from a {@link Theme} or a {@link Region}. */
    private static final class Look {
        int skyTop, skyBottom, hillFar, hillNear, moon, particle;
        float vx, vy, sway, stretch = 1f;
        boolean wanders;
        long seed;
        float moonX = 0.76f, moonY = 0.13f, moonSize = 1f / 11f, hillWave = 1f, hillShift = 0f;

        static Look of(Theme t) {
            Look l = new Look();
            l.skyTop = t.skyTop;
            l.skyBottom = t.skyBottom;
            l.hillFar = t.hillFar;
            l.hillNear = t.hillNear;
            l.moon = t.moon;
            l.particle = t.particle;
            l.vx = t.vx;
            l.vy = t.vy;
            l.sway = t.sway;
            l.wanders = t == Theme.FIREFLIES;
            l.seed = t.ordinal() * 91L + 7;
            return l;
        }

        static Look of(Region r) {
            Look l = new Look();
            l.skyTop = r.skyTop;
            l.skyBottom = r.skyBottom;
            l.hillFar = r.hillFar;
            l.hillNear = r.hillNear;
            l.moon = r.moon;
            l.particle = r.particle;
            l.vx = r.weather.vx;
            l.vy = r.weather.vy;
            l.sway = r.weather.sway;
            l.stretch = r.weather.stretch;
            l.wanders = r.weather.wanders;
            l.seed = r.chapter * 91L + 7;
            l.moonX = r.moonX;
            l.moonY = r.moonY;
            l.moonSize = r.moonSize;
            l.hillWave = r.hillWave;
            l.hillShift = r.hillShift;
            return l;
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
    private Region region;
    private Look look = Look.of(Theme.SNOW);
    private Light light;
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
        if (t == theme && region == null && scene != null) return;
        theme = t;
        region = null;
        look = Look.of(t);
        scene = null;
        seeded = false;
        invalidate();
    }

    /** Shows a Journey region: every one has its own colours, land and weather. */
    public void setRegion(Region r) {
        if (region != null && region.chapter == r.chapter && scene != null) return;
        region = r;
        look = Look.of(r);
        scene = null;
        seeded = false;
        invalidate();
    }

    /** Lights the scene for this hour of the day, from 0 to 23. Screens that don't call this stay at night. */
    public void setHour(int hour) {
        Light next = Light.at(hour);
        if (next == light) return;
        light = next;
        scene = null;
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
        Random r = new Random(look.seed);
        int skyTop = light != null ? light.skyTop : look.skyTop;
        int skyBottom = light != null ? light.skyBottom : look.skyBottom;
        int moon = light != null ? light.sun : look.moon;
        int hillFar = light != null ? light.hillFar : look.hillFar;
        int hillNear = light != null ? light.hillNear : look.hillNear;

        // Sky: five flat bands with ordered dithering between them, for that classic console gradient.
        int bands = 5;
        for (int yy = 0; yy < sh; yy++) {
            float t = Math.min(1f, yy / (sh * 0.72f));
            float f = t * (bands - 1);
            int band = (int) f;
            float frac = f - band;
            for (int xx = 0; xx < sw; xx++) {
                int step = frac * 16 > BAYER[yy & 3][xx & 3] ? band + 1 : band;
                px[yy * sw + xx] = lerp(skyTop, skyBottom, step / (float) (bands - 1));
            }
        }

        int stars = light != null && light.noStars ? 0 : sw * sh / 500;
        for (int i = 0; i < stars; i++) {
            int sx = r.nextInt(sw), sy = r.nextInt((int) (sh * 0.55f));
            px[sy * sw + sx] = r.nextInt(5) == 0 ? 0xFFFFFFFF : lerp(skyTop, 0xFFFFFFFF, 0.55f);
        }

        // Moon with a soft dithered halo.
        int mx = (int) (sw * look.moonX), my = (int) (sh * look.moonY), mr = (int) (sw * look.moonSize);
        for (int yy = Math.max(0, my - mr * 2); yy < Math.min(sh, my + mr * 2); yy++) {
            for (int xx = Math.max(0, mx - mr * 2); xx < Math.min(sw, mx + mr * 2); xx++) {
                float d = (float) Math.hypot(xx - mx, yy - my);
                if (d <= mr) {
                    boolean crater = light == null && (Math.hypot(xx - mx - mr * 0.3, yy - my + mr * 0.2) < mr * 0.25
                            || Math.hypot(xx - mx + mr * 0.4, yy - my - mr * 0.3) < mr * 0.18);
                    px[yy * sw + xx] = crater ? lerp(moon, skyBottom, 0.3f) : moon;
                } else if (d < mr * 1.6f && (xx + yy) % 2 == 0) {
                    px[yy * sw + xx] = lerp(px[yy * sw + xx], moon, 0.22f);
                }
            }
        }

        float wave = look.hillWave, shift = look.hillShift, phase = look.seed * 0.37f;
        hills(px, sw, sh, hillFar, 0.70f + shift, 0.075f, 0.011f * wave, 1.3f + phase);
        hills(px, sw, sh, lerp(hillFar, hillNear, 0.5f), 0.79f + shift, 0.06f, 0.019f * wave, 4.1f + phase);
        hills(px, sw, sh, hillNear, 0.88f + shift / 2f, 0.045f, 0.03f * wave, 2.2f + phase);

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
            if (look.wanders) {
                x[i] += (float) Math.sin(phase[i] * 1.3f) * 20f * dp * dt;
                y[i] += (float) Math.cos(phase[i] * 0.9f) * 14f * dp * dt;
            } else {
                x[i] += (look.vx * dp * d + (float) Math.sin(phase[i]) * look.sway * dp) * dt;
                y[i] += look.vy * dp * d * dt;
            }
            if (y[i] > h + 8 || x[i] < -16 || x[i] > w + 16 || y[i] < -16) respawn(i, w, h, false);

            float size = Math.max(2f, (2f + 3f * d) * dp);
            int color = light != null ? light.particle : look.particle;
            if (look.wanders) {
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
            c.drawRect(px, py, px + size, py + size * look.stretch, paint);
        }
        postInvalidateOnAnimation();
    }

    private void respawn(int i, int w, int h, boolean anywhere) {
        depth[i] = 0.4f + 0.6f * rnd.nextFloat();
        phase[i] = rnd.nextFloat() * 6.28f;
        x[i] = rnd.nextFloat() * w;
        if (look.wanders || anywhere) {
            y[i] = rnd.nextFloat() * h;
        } else {
            y[i] = look.vy < 0 ? h + 8 : -8; // rising weather starts at the bottom
            if (look.vx < 0 && rnd.nextBoolean()) { // drifting left: some enter from the right edge
                x[i] = w + 8;
                y[i] = rnd.nextFloat() * h;
            }
        }
    }
}
