package com.kun.pixelwords.fx;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

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
        Runnable onDone;
    }

    private final Map<String, SpriteSheet> cache = new HashMap<>();
    private final List<Active> actives = new ArrayList<>();
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
        return start(name, cx, cy, scale, false, onDone);
    }

    /** Plays an effect until {@link #stop(Object)} is called. */
    public Object loop(String name, float cx, float cy, float scale) {
        return start(name, cx, cy, scale, true, null);
    }

    public void stop(Object handle) {
        actives.remove(handle);
        invalidate();
    }

    public void clear() {
        actives.clear();
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
            int idx = (int) ((now - a.startMs) * a.sheet.fps / 1000f);
            if (!a.loop && idx >= a.sheet.frames) {
                it.remove();
                if (a.onDone != null) finished.add(a.onDone);
                continue;
            }
            idx %= a.sheet.frames;
            a.sheet.frameRect(idx, src);
            float w = a.sheet.frameW * a.scale;
            float h = a.sheet.frameH * a.scale;
            dst.set(a.cx - w / 2f, a.cy - h / 2f, a.cx + w / 2f, a.cy + h / 2f);
            canvas.drawBitmap(a.sheet.bitmap, src, dst, paint);
        }

        if (!actives.isEmpty()) postInvalidateOnAnimation();
        for (Runnable r : finished) r.run();
    }
}
