package com.kun.pixelwords.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.kun.pixelwords.audio.Sfx;
import com.kun.pixelwords.game.DailySpin;

import java.util.List;

/** The prize wheel. The pointer sits at the top, and the first slice starts there. */
public class SpinWheelView extends View {

    private static final int[] SLICE_COLORS = {0xFF5846A3, 0xFF2FA873, 0xFF2E7FBF, 0xFFB83A5E};
    private static final int LEGENDARY = 0xFFFFD34D;
    private static final long SPIN_MS = 3600;
    private static final int FULL_TURNS = 5;

    private final Paint paint = new Paint();
    private final Path pointer = new Path();
    private final RectF oval = new RectF();
    private final List<DailySpin.Segment> segments = DailySpin.segments();
    private float angle;
    private int lastTick;

    public SpinWheelView(Context context) {
        super(context);
        paint.setAntiAlias(true);
    }

    private float sweep() {
        return 360f / segments.size();
    }

    /** Turns the wheel so the pointer lands on a slice, then runs {@code done}. */
    public void spinTo(int segment, Runnable done) {
        float target = -(segment + 0.5f) * sweep();
        float start = angle % 360f;
        ValueAnimator spin = ValueAnimator.ofFloat(start, target - 360f * FULL_TURNS);
        spin.setDuration(SPIN_MS);
        spin.setInterpolator(new DecelerateInterpolator(2.4f));
        spin.addUpdateListener(a -> {
            angle = (float) a.getAnimatedValue();
            int tick = (int) (Math.abs(angle) / sweep());
            if (tick != lastTick) {
                lastTick = tick;
                Sfx.play(Sfx.Id.COUNT_TICK);
            }
            invalidate();
        });
        spin.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) {
                done.run();
            }
        });
        spin.start();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = resolveSize((int) PixelTheme.dp(getContext(), 280), widthSpec);
        int size = Math.min(width, (int) PixelTheme.dp(getContext(), 300));
        setMeasuredDimension(width, size);
    }

    @Override
    protected void onDraw(Canvas c) {
        float size = getHeight();
        float cx = getWidth() / 2f, cy = size / 2f + size * 0.03f, r = size * 0.45f;
        float icon = r * 0.3f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(PixelTheme.INK);
        c.drawCircle(cx, cy, r + PixelTheme.dp(getContext(), 5), paint);

        oval.set(cx - r, cy - r, cx + r, cy + r);
        c.save();
        c.rotate(angle, cx, cy);
        float sweep = sweep();
        for (int i = 0; i < segments.size(); i++) {
            DailySpin.Segment s = segments.get(i);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(s.legendary ? LEGENDARY : SLICE_COLORS[i % SLICE_COLORS.length]);
            c.drawArc(oval, i * sweep - 90f, sweep, true, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(PixelTheme.dp(getContext(), 2));
            paint.setColor(PixelTheme.INK);
            c.drawArc(oval, i * sweep - 90f, sweep, true, paint);

            c.save();
            c.rotate((i + 0.5f) * sweep, cx, cy);
            c.translate(cx, cy - r * 0.68f);
            c.rotate(-(i + 0.5f) * sweep - angle);
            PixelIcons.draw(c, paint, s.legendary ? "star" : s.reward.icon(), -icon / 2f, -icon / 2f, icon);
            c.restore();
        }
        c.restore();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(PixelTheme.GOLD);
        c.drawCircle(cx, cy, r * 0.13f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(PixelTheme.INK);
        c.drawCircle(cx, cy, r * 0.13f, paint);

        float tip = r * 0.14f;
        pointer.reset();
        pointer.moveTo(cx, cy - r + tip);
        pointer.lineTo(cx - tip * 0.7f, cy - r - tip * 0.5f);
        pointer.lineTo(cx + tip * 0.7f, cy - r - tip * 0.5f);
        pointer.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(PixelTheme.ROSE);
        c.drawPath(pointer, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(PixelTheme.INK);
        c.drawPath(pointer, paint);
    }
}
