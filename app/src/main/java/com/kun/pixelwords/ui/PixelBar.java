package com.kun.pixelwords.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.View;

/** Segmented meter: used for the round timer, word progress and streak-to-next-pet. */
public class PixelBar extends View {

    private final Paint paint = new Paint();
    private final float px;
    private float value;
    private int segments = 20;
    private int color = PixelTheme.MINT;
    private boolean alarm;
    private ValueAnimator anim;

    public PixelBar(Context context) {
        super(context);
        px = Math.max(2f, Math.round(PixelTheme.dp(context, 1.5f)));
    }

    public void setSegments(int n) {
        segments = Math.max(1, n);
        invalidate();
    }

    public void setColor(int c) {
        color = c;
        invalidate();
    }

    /** Blinks the bar to signal urgency. */
    public void setAlarm(boolean on) {
        alarm = on;
        invalidate();
    }

    public void setValue(float v) {
        if (anim != null) anim.cancel();
        value = v;
        invalidate();
    }

    public void animateTo(float v) {
        if (anim != null) anim.cancel();
        anim = ValueAnimator.ofFloat(value, v);
        anim.setDuration(350);
        anim.addUpdateListener(a -> {
            value = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        PixelTheme.drawSlot(c, paint, 0, 0, w, h, PixelTheme.BG_DEEP, px);

        float inner = w - px * 4;
        float gap = segments > 1 ? px : 0;
        float seg = (inner - gap * (segments - 1)) / segments;
        int lit = (int) Math.ceil(value * segments - 0.001f);
        boolean blinkOff = alarm && (SystemClock.uptimeMillis() / 180) % 2 == 0;
        for (int i = 0; i < segments; i++) {
            float l = px * 2 + i * (seg + gap);
            float t = px * 2, b = h - px * 2;
            if (i < lit) {
                paint.setColor(blinkOff ? PixelTheme.shade(color, 0.6f) : color);
                paint.setStyle(Paint.Style.FILL);
                c.drawRect(l, t, l + seg, b, paint);
                paint.setColor(PixelTheme.shade(color, 1.4f));
                c.drawRect(l, t, l + seg, t + px, paint);
            } else {
                paint.setColor(0xFF1D1740);
                c.drawRect(l, t, l + seg, b, paint);
            }
        }
        if (alarm) postInvalidateOnAnimation();
    }
}
