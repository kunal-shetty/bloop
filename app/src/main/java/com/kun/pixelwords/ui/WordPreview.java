package com.kun.pixelwords.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.View;

/**
 * The word being spelled, as a row of small tiles above the wheel. When the finger lifts the tiles
 * flash the verdict (mint, gold, rose) and clear.
 */
public class WordPreview extends View {

    private static final long LINGER_MS = 420;

    private final Paint paint = new Paint();
    private final Paint text = new Paint();
    private final float px;
    private String word = "";
    private int verdict;
    private long verdictAt;
    private boolean shake;

    public WordPreview(Context context) {
        super(context);
        px = Math.max(2f, Math.round(PixelTheme.dp(context, 1f)));
        text.setTextAlign(Paint.Align.CENTER);
        PixelTheme.styleDisplay(text, PixelTheme.displayPx(context, 14));
    }

    public void setWord(String w) {
        word = w;
        verdict = 0;
        invalidate();
    }

    /** Colours the current tiles to show the result, then clears them. */
    public void resolve(int color, boolean shake) {
        verdict = color;
        verdictAt = SystemClock.uptimeMillis();
        this.shake = shake;
        invalidate();
    }

    public void clear() {
        word = "";
        verdict = 0;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        if (word.isEmpty()) return;
        long age = SystemClock.uptimeMillis() - verdictAt;
        if (verdict != 0 && age > LINGER_MS) {
            clear();
            return;
        }

        float h = getHeight();
        float size = Math.min(h - px * 3, (getWidth() - PixelTheme.dp(getContext(), 8)) / word.length());
        float step = size + px;
        float x = (getWidth() - step * word.length() + px) / 2f;
        if (shake && verdict != 0 && age < 240) x += (age / 40 % 2 == 0 ? 1 : -1) * px * 3;
        float top = (h - size - px * 2) / 2f;
        float cap = -text.ascent() * 0.86f;
        int fill = verdict != 0 ? verdict : PixelTheme.GOLD;

        for (int i = 0; i < word.length(); i++) {
            float l = x + i * step;
            PixelTheme.drawTile(c, paint, l, top, l + size, top + size + px * 2, fill, px, px * 2);
            text.setColor(PixelTheme.INK);
            c.drawText(word.substring(i, i + 1), l + size / 2f, top + size / 2f + cap / 2f, text);
        }
        if (verdict != 0) postInvalidateOnAnimation();
    }
}
