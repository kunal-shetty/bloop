package com.kun.pixelwords.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

/** A small HUD readout (icon + number). Changes count up and give a little bump. */
public class StatChip extends android.view.View {

    private final Paint paint = new Paint();
    private final Paint text = new Paint();
    private final String icon;
    private final float px;
    private int shown;
    private int target;
    private ValueAnimator anim;

    public StatChip(Context context, String icon) {
        super(context);
        this.icon = icon;
        px = Math.max(2f, Math.round(PixelTheme.dp(context, 1.5f)));
        PixelTheme.styleDisplay(text, PixelTheme.displayPx(context, PixelTheme.SIZE_LABEL));
        text.setColor(PixelTheme.TEXT);
    }

    public void setValue(int v) {
        if (anim != null) anim.cancel();
        shown = target = v;
        requestLayout();
        invalidate();
    }

    public void animateTo(int v) {
        if (v == target) return;
        if (anim != null) anim.cancel();
        target = v;
        anim = ValueAnimator.ofInt(shown, v);
        anim.setDuration(Math.min(500, 120 + Math.abs(v - shown) * 12L));
        anim.addUpdateListener(a -> {
            shown = (int) a.getAnimatedValue();
            requestLayout();
            invalidate();
        });
        anim.start();
        setScaleX(1.18f);
        setScaleY(1.18f);
        animate().scaleX(1f).scaleY(1f).setDuration(180).start();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        float pad = PixelTheme.dp(getContext(), 8);
        float size = PixelTheme.dp(getContext(), 22);
        int w = (int) (pad * 3 + size + text.measureText(String.valueOf(shown)));
        int h = (int) (size + pad + px * 2);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas c) {
        float h = getHeight(), pad = PixelTheme.dp(getContext(), 8), size = PixelTheme.dp(getContext(), 22);
        PixelTheme.drawSlot(c, paint, 0, 0, getWidth(), h, 0xE0251D4A, px);
        PixelIcons.draw(c, paint, icon, pad * 0.7f, (h - size) / 2f, size);
        c.drawText(String.valueOf(shown), pad * 1.7f + size, h / 2f - text.ascent() * 0.43f, text);
    }
}
