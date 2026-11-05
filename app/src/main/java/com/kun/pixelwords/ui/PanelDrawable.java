package com.kun.pixelwords.ui;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** A pixel-framed panel: notched ink outline, a lit inner edge, and a drop shadow along the bottom. */
public class PanelDrawable extends Drawable {

    private final Paint paint = new Paint();
    private final int fill;
    private final float px;

    public PanelDrawable(int fill, float px) {
        this.fill = fill;
        this.px = px;
    }

    @Override
    public void draw(Canvas c) {
        Rect b = getBounds();
        float l = b.left, t = b.top, r = b.right, bm = b.bottom;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(PixelTheme.INK);
        c.drawRect(l + px * 2, t, r - px * 2, bm, paint);
        c.drawRect(l, t + px * 2, r, bm - px * 2, paint);
        c.drawRect(l + px, t + px, r - px, bm - px, paint);
        paint.setColor(fill);
        c.drawRect(l + px * 2, t + px, r - px * 2, bm - px, paint);
        c.drawRect(l + px, t + px * 2, r - px, bm - px * 2, paint);
        paint.setColor(PixelTheme.shade(fill, 1.22f));
        c.drawRect(l + px * 2, t + px, r - px * 2, t + px * 2, paint);
        c.drawRect(l + px, t + px * 2, l + px * 2, bm - px * 2, paint);
        paint.setColor(PixelTheme.shade(fill, 0.68f));
        c.drawRect(l + px * 2, bm - px * 3, r - px * 2, bm - px, paint);
        c.drawRect(r - px * 2, t + px * 2, r - px, bm - px * 2, paint);
    }

    @Override public void setAlpha(int alpha) {}
    @Override public void setColorFilter(ColorFilter cf) {}
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
