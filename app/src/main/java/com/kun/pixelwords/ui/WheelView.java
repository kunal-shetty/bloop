package com.kun.pixelwords.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** The letter wheel: swipe across letters to spell a word. Backtrack over the previous letter to undo. */
public class WheelView extends View {

    public interface Listener {
        /** Called as the swipe grows/shrinks; word may be empty. */
        void onPreview(String word);

        /** A letter was added; {@code position} is 0 for the first letter of the swipe. */
        void onLetterAdded(int position);

        void onLetterRemoved();

        /** Called when the finger lifts. */
        void onSubmit(String word);
    }

    private static final long POP_MS = 140;

    private final Paint paint = new Paint();
    private final Paint textPaint = new Paint();
    private final List<Character> letters = new ArrayList<>();
    private final List<Integer> path = new ArrayList<>();
    private final Rect plateDst = new Rect();
    private float[] lx = new float[0];
    private float[] ly = new float[0];
    private float[] fromX = new float[0];
    private float[] fromY = new float[0];
    private long[] popAt = new long[0];
    private float cx, cy, ringR, tile, px;
    private float fingerX, fingerY;
    private float shuffleT = 1f;
    private boolean touching;
    private boolean locked;
    private Bitmap plate;
    private Listener listener;

    public WheelView(Context context) {
        this(context, null);
    }

    public WheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setAntiAlias(false);
        paint.setFilterBitmap(false);
        textPaint.setTextAlign(Paint.Align.CENTER);
        PixelTheme.styleDisplay(textPaint, 32f);
    }

    public void setListener(Listener l) {
        listener = l;
    }

    /** While locked (countdown, modals) swipes are ignored. */
    public void setLocked(boolean l) {
        locked = l;
        if (l && touching) {
            touching = false;
            path.clear();
            invalidate();
        }
    }

    public void setLetters(String word) {
        letters.clear();
        for (int i = 0; i < word.length(); i++) letters.add(Character.toUpperCase(word.charAt(i)));
        path.clear();
        popAt = new long[letters.size()];
        layoutLetters();
        invalidate();
    }

    public void shuffle(Random rnd) {
        if (letters.size() < 2) return;
        // Tiles glide from where they are now to wherever their letter lands.
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < letters.size(); i++) order.add(i);
        Collections.shuffle(order, rnd);
        float[] oldX = lx.clone(), oldY = ly.clone();
        List<Character> shuffled = new ArrayList<>();
        fromX = new float[letters.size()];
        fromY = new float[letters.size()];
        for (int i = 0; i < order.size(); i++) {
            shuffled.add(letters.get(order.get(i)));
            fromX[i] = oldX[order.get(i)];
            fromY[i] = oldY[order.get(i)];
        }
        letters.clear();
        letters.addAll(shuffled);
        path.clear();
        layoutLetters();

        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(320);
        a.setInterpolator(new DecelerateInterpolator(1.6f));
        a.addUpdateListener(u -> {
            shuffleT = (float) u.getAnimatedValue();
            invalidate();
        });
        shuffleT = 0f;
        a.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        plate = null;
        layoutLetters();
    }

    private void layoutLetters() {
        int n = letters.size();
        lx = new float[n];
        ly = new float[n];
        float size = Math.min(getWidth(), getHeight());
        if (size <= 0) return;
        px = Math.max(2f, Math.round(PixelTheme.dp(getContext(), 1.5f)));
        cx = getWidth() / 2f;
        cy = getHeight() / 2f;
        tile = size * (n <= 4 ? 0.26f : n == 5 ? 0.235f : n == 6 ? 0.21f : n == 7 ? 0.19f : 0.175f);
        ringR = size / 2f - tile * 0.72f - px * 3;
        textPaint.setTextSize(Math.max(16f, (float) Math.floor(tile * 0.5f / 8f) * 8f));
        for (int i = 0; i < n; i++) {
            double a = -Math.PI / 2 + 2 * Math.PI * i / Math.max(1, n);
            lx[i] = (float) (cx + ringR * Math.cos(a));
            ly[i] = (float) (cy + ringR * Math.sin(a));
        }
    }

    /** The round plate, rasterised at low resolution so its edge is a proper stair-stepped pixel circle. */
    private void buildPlate() {
        int size = Math.min(getWidth(), getHeight());
        int cell = Math.max(2, Math.round(px * 1.5f));
        int n = size / cell;
        Bitmap b = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
        float c = n / 2f, r = n / 2f - 1;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                float dx = x + 0.5f - c, dy = y + 0.5f - c;
                float d = (float) Math.hypot(dx, dy);
                int color = 0;
                if (d <= r) {
                    if (d > r - 1.5f) color = PixelTheme.INK;
                    else if (d > r - 3.5f) color = dy < 0 ? PixelTheme.PANEL_LIGHT : PixelTheme.PANEL;
                    else if (d > r - 5f) color = PixelTheme.INK;
                    else color = (x + y) % 2 == 0 ? 0xF0251D4A : 0xF02B2256;
                }
                b.setPixel(x, y, color);
            }
        }
        plate = b;
        int s = n * cell, l = (getWidth() - s) / 2, t = (getHeight() - s) / 2;
        plateDst.set(l, t, l + s, t + s);
    }

    private int indexAt(float x, float y) {
        float reach = tile * 0.72f;
        for (int i = 0; i < lx.length; i++) {
            float dx = x - lx[i], dy = y - ly[i];
            if (dx * dx + dy * dy <= reach * reach) return i;
        }
        return -1;
    }

    private String currentWord() {
        StringBuilder sb = new StringBuilder();
        for (int i : path) sb.append(letters.get(i));
        return sb.toString();
    }

    private void add(int i) {
        path.add(i);
        popAt[i] = SystemClock.uptimeMillis();
        if (listener != null) listener.onLetterAdded(path.size() - 1);
        notifyPreview();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (locked) return e.getActionMasked() == MotionEvent.ACTION_DOWN;
        float x = e.getX(), y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                touching = true;
                path.clear();
                int hit = indexAt(x, y);
                if (hit >= 0) add(hit);
                fingerX = x;
                fingerY = y;
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (!touching) return true;
                int hit = indexAt(x, y);
                if (hit >= 0) {
                    int n = path.size();
                    if (n >= 2 && path.get(n - 2) == hit) {
                        path.remove(n - 1); // backtrack
                        if (listener != null) listener.onLetterRemoved();
                        notifyPreview();
                    } else if (!path.contains(hit)) {
                        add(hit);
                    }
                }
                fingerX = x;
                fingerY = y;
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (!touching) return true;
                touching = false;
                String word = currentWord();
                path.clear();
                invalidate();
                if (listener != null) {
                    if (e.getActionMasked() == MotionEvent.ACTION_UP && !word.isEmpty()) listener.onSubmit(word);
                    else listener.onPreview("");
                }
                return true;
            }
            default:
                return super.onTouchEvent(e);
        }
    }

    private void notifyPreview() {
        if (listener != null) listener.onPreview(currentWord());
    }

    private float tileX(int i) {
        return shuffleT >= 1f || i >= fromX.length ? lx[i] : fromX[i] + (lx[i] - fromX[i]) * shuffleT;
    }

    private float tileY(int i) {
        return shuffleT >= 1f || i >= fromY.length ? ly[i] : fromY[i] + (ly[i] - fromY[i]) * shuffleT;
    }

    /** A thick stepped trail of squares, so the swipe line looks drawn in pixels instead of a smooth stroke. */
    private void chunkyLine(Canvas c, float x0, float y0, float x1, float y1, float half, int color) {
        float len = (float) Math.hypot(x1 - x0, y1 - y0);
        int steps = Math.max(1, (int) (len / (half * 0.8f)));
        float snap = px * 2;
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            float x = Math.round((x0 + (x1 - x0) * t) / snap) * snap;
            float y = Math.round((y0 + (y1 - y0) * t) / snap) * snap;
            c.drawRect(x - half, y - half, x + half, y + half, paint);
        }
    }

    private void drawTrail(Canvas c, float half, int color) {
        for (int i = 1; i < path.size(); i++) {
            int a = path.get(i - 1), b = path.get(i);
            chunkyLine(c, lx[a], ly[a], lx[b], ly[b], half, color);
        }
        if (touching && !path.isEmpty()) {
            int last = path.get(path.size() - 1);
            chunkyLine(c, lx[last], ly[last], fingerX, fingerY, half, color);
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        int n = letters.size();
        if (n == 0 || lx.length != n) return;
        if (plate == null) buildPlate();
        c.drawBitmap(plate, null, plateDst, paint);

        drawTrail(c, px * 3.5f, PixelTheme.INK);
        drawTrail(c, px * 2f, PixelTheme.GOLD);

        long now = SystemClock.uptimeMillis();
        boolean animating = false;
        float cap = -textPaint.ascent() * 0.86f;
        for (int i = 0; i < n; i++) {
            boolean on = path.contains(i);
            float size = tile;
            long age = now - popAt[i];
            if (on && age < POP_MS) { // pops up as the finger reaches it
                size *= 1f + 0.22f * (1f - age / (float) POP_MS);
                animating = true;
            }
            float x = tileX(i), y = tileY(i), half = size / 2f;
            float lift = on ? px : 0;
            PixelTheme.drawTile(c, paint, x - half, y - half - lift, x + half, y + half - lift + px * 3,
                    on ? PixelTheme.GOLD : PixelTheme.PANEL_LIGHT, px, px * 3);
            textPaint.setColor(on ? PixelTheme.INK : PixelTheme.TEXT);
            c.drawText(String.valueOf(letters.get(i)), x, y - lift + cap / 2f, textPaint);
        }
        if (animating) postInvalidateOnAnimation();
    }
}
