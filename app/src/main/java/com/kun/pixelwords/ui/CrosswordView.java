package com.kun.pixelwords.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import com.kun.pixelwords.game.GameSession;
import com.kun.pixelwords.generator.Level;

/**
 * Draws the crossword for a session. Slots deal in when a round starts; found letters flip in one
 * after another with a bright flash.
 */
public class CrosswordView extends View {

    /** Told which hidden cell the player tapped while picking a letter, or null for {row, col} if they missed. */
    public interface PickListener {
        void onPicked(int[] cell);
    }

    /** Told about each letter as it lands, so sound and haptics stay in time with the animation. */
    public interface LetterListener {
        void onLetterShown(int order);
    }

    private static final long STAGGER_MS = 55;
    private static final long FLIP_MS = 170;
    private static final long INTRO_MS = 12;
    /** A word that lands, or is found again, hops as one; each letter a moment after the one before. */
    private static final long PULSE_MS = 260;
    private static final long PULSE_STEP_MS = 35;
    private static final long RIPPLE_STEP_MS = 40;

    private final Paint paint = new Paint();
    private final Paint textPaint = new Paint();
    private GameSession session;
    private float cell, originX, originY;
    private long[][] showAt;     // when each letter was scheduled to appear; 0 = still hidden
    private long[][] dealAt;     // when each slot deals in
    private long[][] pulseAt;    // when each letter last hopped as part of a word; 0 = never
    private boolean[][] reported;
    private int reportedCount;
    private LetterListener letterListener;
    private PickListener pickListener;
    private PickListener tapListener;
    private float downX, downY;
    private boolean[][] frozen, spark, fish, dark;

    public CrosswordView(Context context) {
        this(context, null);
    }

    public CrosswordView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setAntiAlias(false);
        textPaint.setTextAlign(Paint.Align.CENTER);
        PixelTheme.styleDisplay(textPaint, 32f);
    }

    public void setLetterListener(LetterListener l) {
        letterListener = l;
    }

    /** Hears every ordinary tap on the board, with the cell tapped, or null if it missed the grid. */
    public void setTapListener(PickListener l) {
        tapListener = l;
    }

    /** Waits for the player to tap a hidden letter. Tapping anywhere else cancels. */
    public void pickLetter(PickListener l) {
        pickListener = l;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent e) {
        if (pickListener == null) return onOrdinaryTouch(e);
        if (e.getActionMasked() == android.view.MotionEvent.ACTION_UP) {
            PickListener l = pickListener;
            pickListener = null;
            l.onPicked(cellAt(e.getX(), e.getY()));
            invalidate();
        }
        return true;
    }

    private static final int DARK_TILE = 0xFF1A1436;

    private boolean onOrdinaryTouch(android.view.MotionEvent e) {
        if (tapListener == null) return super.onTouchEvent(e);
        if (e.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) {
            downX = e.getX();
            downY = e.getY();
        } else if (e.getActionMasked() == android.view.MotionEvent.ACTION_UP
                && Math.hypot(e.getX() - downX, e.getY() - downY) < cell) {
            tapListener.onPicked(cellAt(e.getX(), e.getY()));
        }
        return true;
    }

    private int[] cellAt(float x, float y) {
        if (session == null || cell == 0) return null;
        int col = (int) Math.floor((x - originX) / cell), row = (int) Math.floor((y - originY) / cell);
        Level l = session.level();
        return row >= 0 && col >= 0 && row < l.rows && col < l.cols ? new int[]{row, col} : null;
    }

    /** Marks the cells hiding fish. A fish shows once its letter is revealed. */
    public void setFish(java.util.List<int[]> cells) {
        Level l = session.level();
        fish = new boolean[l.rows][l.cols];
        for (int[] cell : cells) fish[cell[0]][cell[1]] = true;
        invalidate();
    }

    /** Hides the letters in these cells behind darkness, until the next call. */
    public void setDark(java.util.List<int[]> cells) {
        Level l = session.level();
        dark = new boolean[l.rows][l.cols];
        for (int[] cell : cells) dark[cell[0]][cell[1]] = true;
        invalidate();
    }

    /** Works out which cells are frozen or part of a lit spark word right now. */
    private void markTwists(Level l) {
        if (frozen == null || frozen.length != l.rows) {
            frozen = new boolean[l.rows][l.cols];
            spark = new boolean[l.rows][l.cols];
        }
        for (int r = 0; r < l.rows; r++) {
            java.util.Arrays.fill(frozen[r], false);
            java.util.Arrays.fill(spark[r], false);
        }
        for (int i = 0; i < l.placements.size(); i++) {
            Level.Placement p = l.placements.get(i);
            boolean ice = session.isFrozen(i);
            boolean lit = i == session.mods().sparkWord() && session.sparkLive();
            if (!ice && !lit) continue;
            for (int j = 0; j < p.word.length(); j++) {
                if (ice) frozen[p.rowAt(j)][p.colAt(j)] = true;
                if (lit) spark[p.rowAt(j)][p.colAt(j)] = true;
            }
        }
    }

    public void setSession(GameSession s) {
        session = s;
        fish = null;
        dark = null;
        frozen = null;
        pickListener = null;
        Level l = s.level();
        showAt = new long[l.rows][l.cols];
        dealAt = new long[l.rows][l.cols];
        reported = new boolean[l.rows][l.cols];
        pulseAt = new long[l.rows][l.cols];
        long now = SystemClock.uptimeMillis();
        for (int r = 0; r < l.rows; r++) {
            for (int c = 0; c < l.cols; c++) dealAt[r][c] = now + (r + c) * INTRO_MS * 3;
        }
        computeLayout();
        invalidate();
    }

    /** Call after the session changed. Newly revealed letters are queued left to right, top to bottom. */
    public void refresh() {
        if (session == null) return;
        Level l = session.level();
        long at = SystemClock.uptimeMillis();
        reportedCount = 0;
        for (int r = 0; r < l.rows; r++) {
            for (int c = 0; c < l.cols; c++) {
                if (session.isRevealed(r, c) && showAt[r][c] == 0) {
                    showAt[r][c] = at;
                    at += STAGGER_MS;
                }
            }
        }
        invalidate();
    }

    /** Makes a whole word hop once its letters have landed, crossings included. */
    public void pulse(Level.Placement p) {
        if (session == null || PixelTheme.reduceMotion()) return;
        long landed = SystemClock.uptimeMillis();
        for (int i = 0; i < p.word.length(); i++) landed = Math.max(landed, showAt[p.rowAt(i)][p.colAt(i)] + FLIP_MS);
        for (int i = 0; i < p.word.length(); i++) pulseAt[p.rowAt(i)][p.colAt(i)] = landed + i * PULSE_STEP_MS;
        invalidate();
    }

    /** A wave across the whole board, for clearing it. */
    public void ripple() {
        if (session == null || PixelTheme.reduceMotion()) return;
        Level l = session.level();
        long now = SystemClock.uptimeMillis();
        for (int r = 0; r < l.rows; r++) {
            for (int c = 0; c < l.cols; c++) pulseAt[r][c] = Math.max(pulseAt[r][c], now + 300 + (r + c) * RIPPLE_STEP_MS);
        }
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        computeLayout();
    }

    private void computeLayout() {
        if (session == null || getWidth() == 0) return;
        Level l = session.level();
        float maxCell = PixelTheme.dp(getContext(), 58);
        cell = Math.min(maxCell, Math.min(getWidth() / (float) l.cols, getHeight() / (float) l.rows));
        originX = (getWidth() - cell * l.cols) / 2f;
        originY = (getHeight() - cell * l.rows) / 2f;
        textPaint.setTextSize(Math.max(8f, (float) Math.floor(cell * 0.5f / 8f) * 8f));
    }

    /** Centre of a grid cell in this view's pixel coordinates (for spawning effects). */
    public PointF cellCenter(int row, int col) {
        return new PointF(originX + (col + 0.5f) * cell, originY + (row + 0.5f) * cell);
    }

    public float cellSize() {
        return cell;
    }

    @Override
    protected void onDraw(Canvas c) {
        if (session == null) return;
        Level l = session.level();
        char[][] grid = l.grid();
        float gap = Math.max(2f, Math.round(cell / 24f));
        float px = Math.max(2f, Math.round(cell / 22f));
        float cap = -textPaint.ascent() * 0.86f;
        long now = SystemClock.uptimeMillis();
        boolean animating = false;
        markTwists(l);

        for (int r = 0; r < l.rows; r++) {
            for (int col = 0; col < l.cols; col++) {
                if (grid[r][col] == '\0') continue;
                float cx = originX + (col + 0.5f) * cell;
                float cy = originY + (r + 0.5f) * cell;
                float half = cell / 2f - gap;

                long deal = now - dealAt[r][col];
                if (deal < 0) {
                    animating = true;
                    continue;
                }
                if (deal < 180) {
                    half *= Math.round(deal / 180f * 5f) / 5f;
                    animating = true;
                }

                long at = showAt[r][col];
                long since = at == 0 ? -1 : now - at;
                if (at == 0 || since < 0) {
                    if (since < 0) animating = true;
                    int slot = frozen[r][col] ? 0xF06FB6E6 : spark[r][col] ? 0xF0B85A1E
                            : session.mods().isGold(l, r, col) ? 0xF07A5F14 : pickListener != null ? 0xF0705CBE : 0xF0251D4A;
                    PixelTheme.drawSlot(c, paint, cx - half, cy - half, cx + half, cy + half, slot, px);
                    continue;
                }

                if (!reported[r][col]) {
                    reported[r][col] = true;
                    if (letterListener != null) letterListener.onLetterShown(reportedCount++);
                }

                if (dark != null && dark[r][col]) {
                    PixelTheme.drawTile(c, paint, cx - half, cy - half, cx + half, cy + half, DARK_TILE, px, px * 2);
                    continue;
                }

                float k = Math.min(1f, since / (float) FLIP_MS);
                if (k < 1f) animating = true;
                float bump = k < 1f ? 1f + 0.28f * (1f - k) : 1f;
                long pulse = pulseAt[r][col] == 0 ? -1 : now - pulseAt[r][col];
                if (pulseAt[r][col] != 0 && pulse < PULSE_MS) {
                    animating = true;
                    if (pulse >= 0) bump *= 1f + 0.16f * (float) Math.sin(Math.PI * pulse / PULSE_MS);
                }
                float s = half * bump;
                int fill = k < 0.5f ? 0xFFFFFFFF : session.mods().isGold(l, r, col) ? PixelTheme.GOLD : PixelTheme.CREAM;
                PixelTheme.drawTile(c, paint, cx - s, cy - s, cx + s, cy + s, fill, px, px * 2);
                if (k >= 0.34f) {
                    textPaint.setColor(PixelTheme.INK);
                    c.drawText(String.valueOf(Character.toUpperCase(grid[r][col])), cx, cy - px + cap / 2f, textPaint);
                }
                if (fish != null && fish[r][col]) PixelIcons.draw(c, paint, "fish", cx, cy, half);
            }
        }
        if (animating) postInvalidateOnAnimation();
    }
}
