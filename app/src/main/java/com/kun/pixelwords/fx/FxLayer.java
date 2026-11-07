package com.kun.pixelwords.fx;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import com.kun.pixelwords.ui.PixelIcons;
import com.kun.pixelwords.ui.PixelTheme;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Transparent overlay that plays PixelFX sprite-sheet effects anywhere on screen. Put it on top of
 * everything else; it is not clickable, so touches fall through to the views underneath.
 */
public class FxLayer extends View {

    private static final class Active {
        SpriteSheet sheet;
        long startMs;
        float cx, cy, scale;
        boolean loop;
        boolean cover; // scales itself to cover the whole screen, whatever size that is
        float speed = 1f;
        int firstFrame;
        Runnable onDone;
    }

    /** One coin on its way to a readout. */
    private static final class Coin {
        float fromX, fromY, toX, toY, bend, size;
        long startMs;
        Runnable onLand;
    }

    private static final long COIN_FLIGHT_MS = 480;
    private static final long COIN_GAP_MS = 60;

    private final Map<String, SpriteSheet> cache = new HashMap<>();
    private final List<Active> actives = new ArrayList<>();
    private final List<Coin> coins = new ArrayList<>();
    private final Paint paint = new Paint();
    private final Rect src = new Rect();
    private final RectF dst = new RectF();

    public FxLayer(Context context) {
        this(context, null);
    }

    public FxLayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setFilterBitmap(false); // nearest-neighbour keeps pixel art crisp
        paint.setAntiAlias(false);
        setClickable(false);
    }

    /** Decodes sheets on a background thread so the first play() does not hitch. */
    public void preload(final String... names) {
        final Context app = getContext().getApplicationContext();
        new Thread(new Runnable() {
            @Override public void run() {
                for (String n : names) {
                    synchronized (cache) {
                        if (cache.containsKey(n)) continue;
                    }
                    try {
                        SpriteSheet s = SpriteSheet.load(app, n);
                        synchronized (cache) { cache.put(n, s); }
                    } catch (IOException ignored) {
                        // missing effect: play() will simply do nothing
                    }
                }
            }
        }, "fx-preload").start();
    }

    /** Plays an effect once, centred on (cx, cy) in view pixels. {@code scale} = screen pixels per sprite pixel. */
    public Object play(String name, float cx, float cy, float scale, Runnable onDone) {
        if (PixelTheme.reduceMotion() && fillsTheScreen(name)) {
            if (onDone != null) post(onDone);
            return null;
        }
        return start(name, cx, cy, scale, false, onDone);
    }

    /** Plays an effect at this many times its normal speed, to fit a long effect into a short moment. */
    public Object play(String name, float cx, float cy, float scale, float speed, Runnable onDone) {
        Active a = (Active) play(name, cx, cy, scale, onDone);
        if (a != null) a.speed = speed;
        return a;
    }

    /** Plays a screen transition from {@code firstFrame}, stretched to cover the screen. */
    public void playCover(String name, int firstFrame, Runnable onDone) {
        Active a = (Active) play(name, 0, 0, 1, onDone);
        if (a == null) return;
        a.cover = true;
        a.firstFrame = firstFrame;
        a.startMs = 0; // the clock starts on the first frame drawn, so a slow screen opening can't skip it
    }

    /**
     * Sends {@code count} coins curving from one point to another, a moment apart. Runs {@code onLand}
     * as each one arrives, so the readout can tick up as they hit it.
     */
    public void flyCoins(float fromX, float fromY, float toX, float toY, int count, float size, Runnable onLand) {
        long now = SystemClock.uptimeMillis();
        for (int i = 0; i < count; i++) {
            Coin c = new Coin();
            c.fromX = fromX;
            c.fromY = fromY;
            c.toX = toX;
            c.toY = toY;
            c.size = size;
            c.bend = (i % 2 == 0 ? 1 : -1) * (40 + i * 18) * getResources().getDisplayMetrics().density;
            c.startMs = now + i * COIN_GAP_MS;
            c.onLand = onLand;
            coins.add(c);
        }
        invalidate();
    }

    /** Effects big enough to bother someone who asked for less movement. Small pickups stay as feedback. */
    private static boolean fillsTheScreen(String name) {
        return name.equals("ui_confetti") || name.startsWith("transitions_") || name.equals("holy_sunburst");
    }

    /** Plays an effect until {@link #stop(Object)} is called. */
    public Object loop(String name, float cx, float cy, float scale) {
        if (PixelTheme.reduceMotion() && fillsTheScreen(name)) return null;
        return start(name, cx, cy, scale, true, null);
    }

    public void stop(Object handle) {
        actives.remove(handle);
        invalidate();
    }

    public void clear() {
        actives.clear();
        coins.clear();
        invalidate();
    }

    private Object start(String name, float cx, float cy, float scale, boolean loop, Runnable onDone) {
        SpriteSheet sheet = get(name);
        if (sheet == null) {
            if (onDone != null) post(onDone);
            return null;
        }
        Active a = new Active();
        a.sheet = sheet;
        a.startMs = SystemClock.uptimeMillis();
        a.cx = cx;
        a.cy = cy;
        a.scale = scale;
        a.loop = loop;
        a.onDone = onDone;
        actives.add(a);
        invalidate();
        return a;
    }

    private SpriteSheet get(String name) {
        synchronized (cache) {
            SpriteSheet s = cache.get(name);
            if (s != null) return s;
        }
        try {
            SpriteSheet s = SpriteSheet.load(getContext().getApplicationContext(), name);
            synchronized (cache) { cache.put(name, s); }
            return s;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long now = SystemClock.uptimeMillis();
        List<Runnable> finished = new ArrayList<>();

        Iterator<Active> it = actives.iterator();
        while (it.hasNext()) {
            Active a = it.next();
            if (a.startMs == 0) a.startMs = now;
            int idx = a.firstFrame + (int) ((now - a.startMs) * a.sheet.fps * a.speed / 1000f);
            if (!a.loop && idx >= a.sheet.frames) {
                it.remove();
                if (a.onDone != null) finished.add(a.onDone);
                continue;
            }
            idx %= a.sheet.frames;
            a.sheet.frameRect(idx, src);
            float scale = a.cover ? Math.max(getWidth() / (float) a.sheet.frameW, getHeight() / (float) a.sheet.frameH) : a.scale;
            float cx = a.cover ? getWidth() / 2f : a.cx, cy = a.cover ? getHeight() / 2f : a.cy;
            float w = a.sheet.frameW * scale;
            float h = a.sheet.frameH * scale;
            dst.set(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
            canvas.drawBitmap(a.sheet.bitmap, src, dst, paint);
        }

        drawCoins(canvas, now, finished);
        if (!actives.isEmpty() || !coins.isEmpty()) postInvalidateOnAnimation();
        for (Runnable r : finished) r.run();
    }

    private void drawCoins(Canvas canvas, long now, List<Runnable> landed) {
        Iterator<Coin> it = coins.iterator();
        while (it.hasNext()) {
            Coin c = it.next();
            float t = (now - c.startMs) / (float) COIN_FLIGHT_MS;
            if (t < 0f) continue;
            if (t >= 1f) {
                it.remove();
                if (c.onLand != null) landed.add(c.onLand);
                continue;
            }
            float e = t * t; // gathers speed as it nears the readout
            float midX = (c.fromX + c.toX) / 2f + c.bend, midY = (c.fromY + c.toY) / 2f;
            float x = (1 - e) * (1 - e) * c.fromX + 2 * (1 - e) * e * midX + e * e * c.toX;
            float y = (1 - e) * (1 - e) * c.fromY + 2 * (1 - e) * e * midY + e * e * c.toY;
            PixelIcons.draw(canvas, paint, "coin", x - c.size / 2f, y - c.size / 2f, c.size);
        }
    }
}
