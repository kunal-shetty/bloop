package com.kun.pixelwords.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.view.View;

import com.kun.pixelwords.audio.Haptics;
import com.kun.pixelwords.audio.Sfx;

import java.util.Random;

/**
 * The boot sequence, all on one timeline: a CRT switches on, letter tiles slam down one by one with
 * screen shake and dust, a gold wave and sun rays hit as the logo sting plays, then a pixel iris
 * closes to the menu.
 */
public class SplashView extends View {

    public interface Listener {
        void onFinished();
    }

    private static final String[] WORD = {"BLOOP"};
    private static final long CRT_END = 320;
    private static final long FIRST_TILE = 420;
    private static final long TILE_GAP = 140;
    private static final long TILE_FALL = 190;
    private static final long STING_AT = FIRST_TILE + 5 * TILE_GAP + 160;
    private static final long READY_AT = STING_AT + 1250;
    private static final long IRIS_MS = 380;

    private static final int STARS = 70, DUST = 40;

    private final Paint paint = new Paint();
    private final Paint tilePaint = new Paint();
    private final Paint letter = new Paint();
    private final Paint small = new Paint();
    private final Path ray = new Path();
    private final float px;
    private final float[] starX = new float[STARS], starY = new float[STARS], starPhase = new float[STARS];
    private final float[] dustX = new float[DUST], dustY = new float[DUST], dustVx = new float[DUST], dustVy = new float[DUST], dustLife = new float[DUST];
    private final Random rnd = new Random(42);
    private final boolean[] landed = new boolean[5];

    private long start;
    private long lastFrame;
    private long shakeAt = -1000;
    private long irisAt;
    private float shakePower;
    private boolean stung;
    private boolean ready;
    private boolean skip;
    private boolean finished;
    private Listener listener;

    public SplashView(Context context) {
        super(context);
        px = Math.max(2f, Math.round(PixelTheme.dp(context, 1.5f)));
        letter.setTextAlign(Paint.Align.CENTER);
        PixelTheme.styleDisplay(letter, 32f);
        small.setTextAlign(Paint.Align.CENTER);
        PixelTheme.styleDisplay(small, PixelTheme.displayPx(context, PixelTheme.SIZE_CAPTION));
        for (int i = 0; i < STARS; i++) {
            starX[i] = rnd.nextFloat();
            starY[i] = rnd.nextFloat();
            starPhase[i] = rnd.nextFloat() * 6.28f;
        }
        setOnClickListener(v -> skip = true);
    }

    public void setListener(Listener l) {
        listener = l;
    }

    /** The game data has loaded, so the splash may now finish. */
    public void setReady() {
        ready = true;
    }

    private void dustBurst(float x, float y, int count) {
        int added = 0;
        for (int i = 0; i < DUST && added < count; i++) {
            if (dustLife[i] > 0) continue;
            dustX[i] = x;
            dustY[i] = y;
            dustVx[i] = (rnd.nextFloat() - 0.5f) * px * 110f;
            dustVy[i] = -rnd.nextFloat() * px * 60f;
            dustLife[i] = 0.3f + rnd.nextFloat() * 0.3f;
            added++;
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        long now = SystemClock.uptimeMillis();
        if (start == 0) start = now;
        long t = now - start;
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.05f, (now - lastFrame) / 1000f);
        lastFrame = now;
        float w = getWidth(), h = getHeight();

        c.drawColor(PixelTheme.BG_DEEP);

        if (t < CRT_END) {
            drawCrt(c, w, h, t / (float) CRT_END);
            postInvalidateOnAnimation();
            return;
        }

        c.save();
        long sinceShake = t - shakeAt;
        if (sinceShake < 150) {
            float mag = shakePower * (1f - sinceShake / 150f);
            c.translate(Math.round((rnd.nextFloat() - 0.5f) * mag / px) * px, Math.round((rnd.nextFloat() - 0.5f) * mag / px) * px);
        }

        drawStars(c, w, h, t);

        float size = Math.min((w - px * 20) / 5f, h / 7f);
        float gap = px * 2;
        float topY = h * 0.44f - size / 2f;
        letter.setTextSize(Math.max(16f, (float) Math.floor(size * 0.62f / 8f) * 8f));

        if (stung) drawRays(c, w / 2f, topY + size / 2f, (t - STING_AT) / 1000f, Math.max(w, h));

        float rowW = 5 * size + 4 * gap;
        float x0 = (w - rowW) / 2f;
        for (int i = 0; i < 5; i++) {
            drawTile(c, 0, i, i, x0 + i * (size + gap), topY, size, t, FIRST_TILE + i * TILE_GAP);
        }

        if (t >= STING_AT && !stung) {
            stung = true;
            Sfx.play(Sfx.Id.LOGO_STING);
            Haptics.celebrate();
            shake(px * 10, t);
        }

        drawDust(c, dt);
        drawFooter(c, w, h, t);
        c.restore();

        if (stung) drawFlash(c, w, h, t - STING_AT);

        boolean mayLeave = ready && (t >= READY_AT || (skip && t > CRT_END));
        if (irisAt == 0 && mayLeave) irisAt = now;
        if (irisAt != 0) {
            float k = (now - irisAt) / (float) IRIS_MS;
            drawIris(c, w, h, k);
            if (k >= 1f && !finished) {
                finished = true;
                if (listener != null) listener.onFinished();
                return;
            }
        }
        postInvalidateOnAnimation();
    }

    private void shake(float power, long t) {
        shakePower = power;
        shakeAt = t;
    }

    /** An old TV turning on: a white line snaps open into the full picture. */
    private void drawCrt(Canvas c, float w, float h, float k) {
        paint.setStyle(Paint.Style.FILL);
        if (k < 0.4f) {
            float lineW = w * (k / 0.4f);
            paint.setColor(0xFFFFFFFF);
            c.drawRect((w - lineW) / 2f, h / 2f - px, (w + lineW) / 2f, h / 2f + px, paint);
        } else {
            float open = (k - 0.4f) / 0.6f;
            float bandH = h * open * open;
            paint.setColor(PixelTheme.shade(0xFFFFFFFF, 1f));
            paint.setAlpha((int) (255 * (1f - open)));
            c.drawRect(0, (h - bandH) / 2f, w, (h + bandH) / 2f, paint);
            paint.setAlpha(255);
        }
    }

    private void drawStars(Canvas c, float w, float h, long t) {
        float snap = px * 2;
        for (int i = 0; i < STARS; i++) {
            float tw = 0.5f + 0.5f * (float) Math.sin(t / 400f + starPhase[i]);
            paint.setColor(tw > 0.75f ? 0xFFFFFFFF : tw > 0.35f ? PixelTheme.TEXT_DIM : PixelTheme.PANEL);
            float x = Math.round(starX[i] * w / snap) * snap, y = Math.round(starY[i] * h / snap) * snap;
            c.drawRect(x, y, x + px * 1.5f, y + px * 1.5f, paint);
        }
    }

    private void drawTile(Canvas c, int r, int i, int idx, float x, float y, float size, long t, long tAt) {
        long since = t - tAt;
        if (since < 0) return;

        float dy, squash = 1f;
        if (since < TILE_FALL) {
            float k = since / (float) TILE_FALL;
            dy = -(1f - k * k) * (getHeight() * 0.5f + size);   // accelerates like it has weight
        } else {
            dy = 0;
            if (!landed[idx]) {
                landed[idx] = true;
                Sfx.play(Sfx.Id.TILE_LAND, 0.9f + idx * 0.05f);
                Haptics.click();
                shake(px * 4, t);
                dustBurst(x + size / 2f, y + size, 7);
            }
            long after = since - TILE_FALL;
            if (after < 120) squash = 1f - 0.18f * (1f - after / 120f);
        }

        // Once the logo hits, a wave of light rolls across the tiles.
        int fill = i % 2 == 0 ? PixelTheme.GOLD : PixelTheme.MINT;
        long wave = t - STING_AT - idx * 40;
        if (wave > 0 && wave < 220) fill = PixelTheme.shade(fill, 1.75f);

        float tileH = size * squash;
        float top = y + dy + size - tileH;
        PixelTheme.drawTile(c, tilePaint, x, top, x + size, top + tileH, fill, px, px * 3);
        float cap = -letter.ascent() * 0.86f;
        letter.setColor(PixelTheme.INK);
        c.drawText(WORD[r].substring(i, i + 1), x + size / 2f, top + (tileH - px * 3) / 2f + cap / 2f, letter);
    }

    private void drawRays(Canvas c, float cx, float cy, float t, float reach) {
        int n = 12;
        float spin = t * 0.35f;
        float grow = Math.min(1f, t * 3f);
        paint.setColor(0x24FFD34D);
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < n; i++) {
            double a = spin + i * Math.PI * 2 / n;
            double spread = Math.PI / n * 0.55;
            ray.reset();
            ray.moveTo(cx, cy);
            ray.lineTo((float) (cx + Math.cos(a - spread) * reach * grow), (float) (cy + Math.sin(a - spread) * reach * grow));
            ray.lineTo((float) (cx + Math.cos(a + spread) * reach * grow), (float) (cy + Math.sin(a + spread) * reach * grow));
            ray.close();
            c.drawPath(ray, paint);
        }
    }

    private void drawDust(Canvas c, float dt) {
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < DUST; i++) {
            if (dustLife[i] <= 0) continue;
            dustLife[i] -= dt;
            dustX[i] += dustVx[i] * dt;
            dustY[i] += dustVy[i] * dt;
            dustVy[i] += px * 180f * dt;
            paint.setColor(dustLife[i] > 0.2f ? PixelTheme.CREAM : PixelTheme.TEXT_DIM);
            float s = px * (dustLife[i] > 0.25f ? 2.5f : 1.5f);
            c.drawRect(dustX[i], dustY[i], dustX[i] + s, dustY[i] + s, paint);
        }
    }

    private void drawFooter(Canvas c, float w, float h, long t) {
        if (t < FIRST_TILE) return;
        float barW = w * 0.5f, barH = px * 6, x = (w - barW) / 2f, y = h * 0.8f;
        float k = Math.min(1f, (t - FIRST_TILE) / (float) (READY_AT - FIRST_TILE - 200));
        if (!ready) k = Math.min(k, 0.85f);
        PixelTheme.drawSlot(c, paint, x, y, x + barW, y + barH, PixelTheme.BG_MID, px);
        int segments = 16;
        float seg = (barW - px * 4 - px * (segments - 1)) / segments;
        paint.setColor(PixelTheme.GOLD);
        for (int i = 0; i < (int) (k * segments); i++) {
            float l = x + px * 2 + i * (seg + px);
            c.drawRect(l, y + px * 2, l + seg, y + barH - px * 2, paint);
        }
        small.setColor(PixelTheme.TEXT_DIM);
        c.drawText(k >= 1f ? "READY!" : "LOADING WORDS", w / 2f, y + barH + small.getTextSize() * 2.2f, small);
        if (stung) {
            small.setColor((t / 300) % 2 == 0 ? PixelTheme.TEXT : PixelTheme.TEXT_DIM);
            c.drawText("SWIPE . SPELL . SHINE", w / 2f, y - small.getTextSize() * 2f, small);
        }
    }

    private void drawFlash(Canvas c, float w, float h, long since) {
        if (since > 240) return;
        paint.setColor(0xFFFFFFFF);
        paint.setAlpha((int) (200 * (1f - since / 240f)));
        c.drawRect(0, 0, w, h, paint);
        paint.setAlpha(255);
    }

    /** Black closes in as a blocky circle: every cell is on or off, there is no smooth edge. */
    private void drawIris(Canvas c, float w, float h, float k) {
        float cell = px * 7;
        float maxR = (float) Math.hypot(w, h) / 2f;
        float e = Math.min(1f, k);
        float r = maxR * (1f - e * e);
        float cx = w / 2f, cy = h / 2f;
        paint.setColor(PixelTheme.BG_DEEP);
        for (float y = 0; y < h; y += cell) {
            for (float x = 0; x < w; x += cell) {
                if (Math.hypot(x + cell / 2f - cx, y + cell / 2f - cy) >= r) c.drawRect(x, y, x + cell + 1, y + cell + 1, paint);
            }
        }
    }
}
