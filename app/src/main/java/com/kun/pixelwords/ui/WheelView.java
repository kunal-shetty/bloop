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
import java.util.Locale;
import java.util.Objects;
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
    private static final long GUIDE_STEP_MS = 520;
    private static final long GUIDE_REST_MS = 900;

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
    private String guide;
    private int[] guidePath;
    private long guideStart;

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

    private char stolenLetter;

    public void setListener(Listener l) {
        listener = l;
    }

    /** While locked (countdown, modals) swipes are ignored. */
    public void setLocked(boolean l) {
        locked = l;
        if (l && touching) {
            touching = false;
            path.clear();
        }
        invalidate();
    }

    /** Takes one copy of a letter off the wheel, until the next call. 0 gives it back. */
    public void setStolen(char letter) {
        stolenLetter = Character.toUpperCase(letter);
        path.clear();
        touching = false;
        invalidate();
    }

    /** The tile the stolen letter was on, or -1. */
    private int stolenIndex() {
        return stolenLetter == 0 ? -1 : letters.indexOf(stolenLetter);
    }

    public void setLetters(String word) {
        stolenLetter = 0;
        letters.clear();
        for (int i = 0; i < word.length(); i++) letters.add(Character.toUpperCase(word.charAt(i)));
        path.clear();
        popAt = new long[letters.size()];
        layoutLetters();
        traceGuide();
        setContentDescription("Letter wheel: " + String.join(" ", word.toUpperCase(Locale.ROOT).split("")));
        invalidate();
    }

    /** Loops a ghost finger spelling this word until the next swipe, for teaching. Null turns it off. */
    public void setGuide(String word) {
        String next = word == null ? null : word.toUpperCase(Locale.ROOT);
        if (Objects.equals(next, guide)) return;
        guide = next;
        guideStart = SystemClock.uptimeMillis();
        traceGuide();
        invalidate();
    }

    /** Finds which tile each guide letter sits on. A repeated letter uses a different tile each time. */
    private void traceGuide() {
        guidePath = null;
        if (guide == null) return;
        int[] found = new int[guide.length()];
        boolean[] used = new boolean[letters.size()];
        for (int g = 0; g < found.length; g++) {
            found[g] = -1;
            for (int i = 0; i < used.length && found[g] < 0; i++) {
                if (!used[i] && letters.get(i) == guide.charAt(g)) {
                    used[i] = true;
                    found[g] = i;
                }
            }
            if (found[g] < 0) return;
        }
        guidePath = found;
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
        traceGuide();

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
        int stolen = stolenIndex();
        for (int i = 0; i < lx.length; i++) {
            if (i == stolen) continue;
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

    private boolean guideShowing() {
        return guidePath != null && guidePath.length > 1 && !touching && !locked && shuffleT >= 1f;
    }

    /** How far along the word the ghost finger is: 1.5 is halfway from the second letter to the third. */
    private float guideProgress(long now) {
        long travel = (guidePath.length - 1) * GUIDE_STEP_MS;
        long t = (now - guideStart) % (travel + GUIDE_REST_MS);
        return Math.min(t, travel) / (float) GUIDE_STEP_MS;
    }

    private void drawGuideTrail(Canvas c, float progress, float half, int color) {
        int whole = (int) progress;
        for (int i = 0; i < guidePath.length - 1 && i <= whole; i++) {
            int a = guidePath[i], b = guidePath[i + 1];
            float t = Math.min(1f, progress - i);
            chunkyLine(c, lx[a], ly[a], lx[a] + (lx[b] - lx[a]) * t, ly[a] + (ly[b] - ly[a]) * t, half, color);
        }
    }

    /** A chunky white fingertip, set just off the path so the letter underneath stays readable. */
    private void drawGuideFinger(Canvas c, float progress) {
        int seg = Math.min((int) progress, guidePath.length - 2);
        int a = guidePath[seg], b = guidePath[seg + 1];
        float t = progress - seg;
        float x = lx[a] + (lx[b] - lx[a]) * t + tile * 0.24f;
        float y = ly[a] + (ly[b] - ly[a]) * t + tile * 0.24f;
        float half = tile * 0.17f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(PixelTheme.INK);
        c.drawRect(x - half - px, y - half - px, x + half + px, y + half + px, paint);
        paint.setColor(0xFFFFFFFF);
        c.drawRect(x - half, y - half, x + half, y + half, paint);
    }

    private boolean guideReached(int tileIndex, float progress) {
        for (int i = 0; i < guidePath.length && i <= (int) progress; i++) {
            if (guidePath[i] == tileIndex) return true;
        }
        return false;
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
        boolean guiding = guideShowing();
        float guideAt = guiding ? guideProgress(now) : 0f;
        if (guiding) {
            drawGuideTrail(c, guideAt, px * 3.5f, PixelTheme.INK);
            drawGuideTrail(c, guideAt, px * 2f, PixelTheme.MINT);
        }

        boolean animating = guiding;
        float cap = -textPaint.ascent() * 0.86f;
        int stolen = stolenIndex();
        for (int i = 0; i < n; i++) {
            if (i == stolen) {
                float half = tile / 2f, icon = tile * 0.6f;
                PixelTheme.drawSlot(c, paint, tileX(i) - half, tileY(i) - half, tileX(i) + half, tileY(i) + half, PixelTheme.BG_DEEP, px);
                PixelIcons.draw(c, paint, "lock", tileX(i) - icon / 2f, tileY(i) - icon / 2f, icon);
                continue;
            }
            boolean on = path.contains(i);
            boolean guided = guiding && guideReached(i, guideAt);
            float size = tile;
            long age = now - popAt[i];
            if (on && age < POP_MS) { // pops up as the finger reaches it
                size *= 1f + 0.22f * (1f - age / (float) POP_MS);
                animating = true;
            }
            float x = tileX(i), y = tileY(i), half = size / 2f;
            float lift = on ? px : 0;
            PixelTheme.drawTile(c, paint, x - half, y - half - lift, x + half, y + half - lift + px * 3,
                    on ? PixelTheme.GOLD : guided ? PixelTheme.MINT : PixelTheme.PANEL_LIGHT, px, px * 3);
            textPaint.setColor(on || guided ? PixelTheme.INK : PixelTheme.TEXT);
            c.drawText(String.valueOf(letters.get(i)), x, y - lift + cap / 2f, textPaint);
        }
        if (guiding) drawGuideFinger(c, guideAt);
        if (animating) postInvalidateOnAnimation();
    }
}
