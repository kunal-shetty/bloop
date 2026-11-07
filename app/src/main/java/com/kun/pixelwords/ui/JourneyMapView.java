package com.kun.pixelwords.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.kun.pixelwords.game.Journey;
import com.kun.pixelwords.game.Progress;

/**
 * One chapter of the journey: a winding path that climbs from level one at the bottom to the chapter's
 * trophy at the top. Sits inside a ScrollView, which keeps any drag and leaves this view the taps.
 */
public class JourneyMapView extends View {

    public interface Listener {
        void onLevel(int level);

        void onTrophy();
    }

    private static final int STOPS = Journey.LEVELS_PER_CHAPTER + 1; // every level, then the trophy
    private static final float NODE_DP = 60, STEP_DP = 90, END_DP = 70, STAR_DP = 15, HARD_ICON_DP = 22;
    private static final float WAVE_PER_STOP = 0.95f;
    private static final float PULSE = 0.07f;
    private static final long PULSE_MS = 900;

    private final Paint paint = new Paint();
    private final Paint number = new Paint();
    private final float px;
    private final float node;
    private final int slop;
    private Listener listener;
    private Progress progress = new Progress();
    private int chapter;
    private float downX, downY;

    public JourneyMapView(Context context) {
        super(context);
        paint.setFilterBitmap(false);
        px = Math.max(2f, Math.round(PixelTheme.dp(context, 1.5f)));
        node = PixelTheme.dp(context, NODE_DP);
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
        PixelTheme.styleDisplay(number, PixelTheme.displayPx(context, PixelTheme.SIZE_LABEL));
        number.setTextAlign(Paint.Align.CENTER);
    }

    public void setListener(Listener l) {
        listener = l;
    }

    public void show(int chapter, Progress progress) {
        this.chapter = chapter;
        this.progress = progress;
        invalidate();
    }

    /** Where a level sits on this map, for placing the cat. */
    public PointF centerOf(int level) {
        int stop = level - Journey.firstLevel(chapter);
        return new PointF(stopX(stop), stopY(stop));
    }

    public float nodeSize() {
        return node;
    }

    private float dp(float v) {
        return PixelTheme.dp(getContext(), v);
    }

    private float stopX(int stop) {
        float swing = getWidth() / 2f - node * 0.8f;
        return getWidth() / 2f + (float) Math.sin(stop * WAVE_PER_STOP) * swing;
    }

    private float stopY(int stop) {
        return getHeight() - dp(END_DP) - stop * dp(STEP_DP);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int h = (int) (dp(END_DP) * 2 + (STOPS - 1) * dp(STEP_DP));
        setMeasuredDimension(resolveSize(0, widthSpec), h);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                return true;
            case MotionEvent.ACTION_UP:
                if (Math.hypot(e.getX() - downX, e.getY() - downY) < slop) tap(e.getX(), e.getY());
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    private void tap(float x, float y) {
        if (listener == null) return;
        float reach = node * 0.7f;
        for (int stop = 0; stop < STOPS; stop++) {
            if (Math.hypot(x - stopX(stop), y - stopY(stop)) > reach) continue;
            if (stop == STOPS - 1) listener.onTrophy();
            else listener.onLevel(Journey.firstLevel(chapter) + stop);
            return;
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        int first = Journey.firstLevel(chapter);
        for (int stop = 0; stop < STOPS - 1; stop++) drawTrail(c, stop, first + stop < progress.level);
        for (int stop = 0; stop < STOPS - 1; stop++) drawLevel(c, stop, first + stop);
        drawTrophy(c);
        if (Journey.chapterOf(progress.level) == chapter) postInvalidateOnAnimation();
    }

    /** Dashes between two stops, lit once the lower level is cleared. */
    private void drawTrail(Canvas c, int stop, boolean lit) {
        float x0 = stopX(stop), y0 = stopY(stop), x1 = stopX(stop + 1), y1 = stopY(stop + 1);
        float gap = dp(13), half = px * 2, snap = px * 2;
        int dashes = (int) (Math.hypot(x1 - x0, y1 - y0) / gap);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(lit ? PixelTheme.GOLD : 0x80FFFFFF);
        for (int i = 1; i < dashes; i++) {
            float t = i / (float) dashes;
            float x = Math.round((x0 + (x1 - x0) * t) / snap) * snap;
            float y = Math.round((y0 + (y1 - y0) * t) / snap) * snap;
            c.drawRect(x - half, y - half, x + half, y + half, paint);
        }
    }

    private void drawLevel(Canvas c, int stop, int level) {
        boolean cleared = level < progress.level, current = level == progress.level, hard = Journey.isHard(level);
        boolean boss = Journey.isBoss(level);
        float x = stopX(stop), y = stopY(stop), half = node / 2f;
        if (current) {
            double wave = Math.sin(2 * Math.PI * (SystemClock.uptimeMillis() % PULSE_MS) / PULSE_MS);
            half *= 1f + PULSE * (float) (wave + 1) / 2f;
        }
        int fill = current ? PixelTheme.GOLD
                : hard || boss ? PixelTheme.shade(PixelTheme.ROSE, cleared ? 0.8f : 0.5f)
                : cleared ? PixelTheme.PANEL_LIGHT : PixelTheme.BG_MID;
        PixelTheme.drawTile(c, paint, x - half, y - half, x + half, y + half, fill, px, px * 3);

        number.setColor(current ? PixelTheme.INK : cleared || hard ? PixelTheme.TEXT : PixelTheme.TEXT_DIM);
        if (boss) {
            float face = node * 0.72f;
            PixelIcons.draw(c, paint, PixelIcons.boss(Journey.regionOf(level)), x - face / 2f, y - face / 2f, face);
        } else {
            c.drawText(String.valueOf(level), x, y + number.getTextSize() * 0.35f, number);
        }
        if (hard) {
            float icon = dp(HARD_ICON_DP);
            PixelIcons.draw(c, paint, "flame", x + half - icon * 0.7f, y - half - icon * 0.3f, icon);
        }
        if (cleared) drawStars(c, x, y + half, Journey.starsShown(progress, level));
    }

    private void drawStars(Canvas c, float x, float y, int earned) {
        float size = dp(STAR_DP), step = size + dp(2);
        float left = x - step * Journey.MAX_STARS / 2f;
        for (int i = 0; i < Journey.MAX_STARS; i++) {
            PixelIcons.draw(c, paint, i < earned ? "star" : "star_off", left + i * step, y - size * 0.5f, size);
        }
    }

    private void drawTrophy(Canvas c) {
        boolean won = Journey.chaptersFinished(progress) > chapter;
        float x = stopX(STOPS - 1), y = stopY(STOPS - 1), half = node * 0.6f, icon = node * 0.72f;
        PixelTheme.drawTile(c, paint, x - half, y - half, x + half, y + half,
                won ? PixelTheme.GOLD : PixelTheme.BG_MID, px, px * 3);
        PixelIcons.draw(c, paint, "trophy", x - icon / 2f, y - icon / 2f, icon);
    }
}
