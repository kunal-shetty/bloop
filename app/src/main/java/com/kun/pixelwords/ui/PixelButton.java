package com.kun.pixelwords.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;

/** Raised pixel button: optional icon, label, subtitle, corner badge and a passing shine. */
public class PixelButton extends PressableView {

    private final Paint paint = new Paint();
    private final Paint labelPaint = new Paint();
    private final Paint subPaint = new Paint();
    private String label;
    private String sub = "";
    private String icon;
    private String badge = "";
    private int fill;
    private int textColor;
    private int subColor;
    private float iconSize;
    private boolean shine;
    private final long born = SystemClock.uptimeMillis();

    public PixelButton(Context context, String label, int fillColor, int textColor) {
        super(context);
        this.label = label;
        this.fill = fillColor;
        this.textColor = textColor;
        this.subColor = PixelTheme.shade(textColor, 0.7f);
        labelPaint.setTextAlign(Paint.Align.LEFT);
        PixelTheme.styleDisplay(labelPaint, PixelTheme.displayPx(context, PixelTheme.SIZE_TITLE));
        PixelTheme.styleBody(subPaint, PixelTheme.bodyPx(context, PixelTheme.SIZE_BODY));
        iconSize = PixelTheme.dp(context, 22);
    }

    public void setText(String s) {
        label = s;
        requestLayout();
        invalidate();
    }

    public void setSubtitle(String s) {
        sub = s == null ? "" : s;
        requestLayout();
        invalidate();
    }

    public void setIcon(String name) {
        icon = name;
        requestLayout();
        invalidate();
    }

    public void setIconSize(float dp) {
        iconSize = PixelTheme.dp(getContext(), dp);
        requestLayout();
    }

    public void setBadge(String s) {
        badge = s == null ? "" : s;
        invalidate();
    }

    public void setFill(int color) {
        fill = color;
        invalidate();
    }

    public void setTextColor(int color) {
        textColor = color;
        subColor = PixelTheme.shade(color, 0.7f);
        invalidate();
    }

    public void setLabelSize(float dp) {
        labelPaint.setTextSize(PixelTheme.displayPx(getContext(), dp));
        requestLayout();
    }

    /** A bright band sweeps across every few seconds; used to draw the eye to the main button. */
    public void setShine(boolean on) {
        shine = on;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        float pad = PixelTheme.dp(getContext(), 14);
        float textH = -labelPaint.ascent();
        if (!sub.isEmpty()) textH += subPaint.getTextSize() + PixelTheme.dp(getContext(), 4);
        float h = Math.max(textH, icon != null ? iconSize : 0) + pad * 2 + depth + px * 2;
        float w = labelPaint.measureText(label) + (icon != null ? iconSize + pad : 0) + pad * 2;
        setMeasuredDimension(resolveSize((int) w, widthSpec), resolveSize((int) h, heightSpec));
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), s = sink();
        PixelTheme.drawTile(c, paint, 0, s, w, h, fill, px, depth - s);

        float faceH = h - depth - px * 2;
        float labelW = labelPaint.measureText(label);
        float subW = subPaint.measureText(sub);
        float textW = Math.max(labelW, subW);
        float gap = icon != null && textW > 0 ? PixelTheme.dp(getContext(), 10) : 0;
        float groupW = (icon != null ? iconSize : 0) + gap + textW;
        float x = (w - groupW) / 2f;
        float midY = px + s + faceH / 2f;

        if (icon != null) {
            boolean makeRoom = !badge.isEmpty() && textW == 0; // keep an icon-only button's badge off the icon
            float nudge = makeRoom ? PixelTheme.dp(getContext(), 5) : 0;
            PixelIcons.draw(c, paint, icon, x - nudge, midY - iconSize / 2f + nudge, iconSize);
            x += iconSize + gap;
        }
        float capH = -labelPaint.ascent() * 0.86f;
        labelPaint.setColor(textColor);
        if (sub.isEmpty()) {
            c.drawText(label, x + (textW - labelW) / 2f, midY + capH / 2f, labelPaint);
        } else {
            float subH = subPaint.getTextSize();
            float gapY = PixelTheme.dp(getContext(), 5);
            float top = midY - (capH + gapY + subH * 0.8f) / 2f;
            c.drawText(label, x + (textW - labelW) / 2f, top + capH, labelPaint);
            subPaint.setColor(subColor);
            c.drawText(sub, x + (textW - subW) / 2f, top + capH + gapY + subH * 0.8f, subPaint);
        }

        if (!badge.isEmpty()) drawBadge(c, w);
        if (shine) drawShine(c, w, h, s);
    }

    private void drawBadge(Canvas c, float w) {
        Paint p = subPaint;
        float pad = PixelTheme.dp(getContext(), 4);
        float tw = p.measureText(badge);
        float size = p.getTextSize();
        float l = w - tw - pad * 2 - px * 2;
        PixelTheme.drawTile(c, paint, l, 0, w, size + pad * 1.2f, PixelTheme.ROSE, px, 0);
        p.setColor(PixelTheme.INK);
        c.drawText(badge, l + pad + px, size * 0.8f + pad * 0.3f, p);
    }

    private void drawShine(Canvas c, float w, float h, float s) {
        long cycle = 3200;
        float t = ((SystemClock.uptimeMillis() - born) % cycle) / (float) cycle * 2.4f;
        if (t < 1f) {
            float band = w * 0.1f;
            float x = -band + (w + band * 2f) * t;
            paint.setColor(0x55FFFFFF);
            float step = px * 3;
            int rows = (int) ((h - depth - px * 4 - s) / step);
            for (int i = 0; i < rows; i++) {
                float y = px * 2 + s + i * step;
                float off = x - i * step * 0.8f;
                float l = Math.max(px * 2, off), r = Math.min(w - px * 2, off + band);
                if (r > l) c.drawRect(l, y, r, y + step, paint);
            }
        }
        postInvalidateOnAnimation();
    }
}
