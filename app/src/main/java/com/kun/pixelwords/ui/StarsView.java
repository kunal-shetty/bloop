package com.kun.pixelwords.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.View;

import com.kun.pixelwords.audio.Haptics;
import com.kun.pixelwords.audio.Sfx;

/** One to three stars that stamp in one after another on the results modal. */
public class StarsView extends View {

    private static final long GAP = 260, POP = 240;

    private final Paint paint = new Paint();
    private final int earned;
    private final long startAt;
    private int played;

    public StarsView(Context context, int earned, long delayMs) {
        super(context);
        this.earned = earned;
        this.startAt = SystemClock.uptimeMillis() + delayMs;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(resolveSize(0, widthSpec), (int) PixelTheme.dp(getContext(), 64));
    }

    @Override
    protected void onDraw(Canvas c) {
        float base = PixelTheme.dp(getContext(), 48), h = getHeight();
        float step = base + PixelTheme.dp(getContext(), 10);
        float left = getWidth() / 2f - step;
        long now = SystemClock.uptimeMillis();
        boolean running = false;

        for (int i = 0; i < 3; i++) {
            float cx = left + i * step, cy = h / 2f;
            PixelIcons.draw(c, paint, "star_off", cx - base / 2f, cy - base / 2f, base);
            if (i >= earned) continue;
            long t = now - (startAt + i * GAP);
            if (t < 0) {
                running = true;
                continue;
            }
            if (played <= i) {
                played = i + 1;
                Sfx.play(Sfx.Id.STAR, 1f + i * 0.14f);
                Haptics.click();
            }
            float k = Math.min(1f, t / (float) POP);
            if (k < 1f) running = true;
            // Stamps down from double size in chunky whole-pixel steps.
            float size = Math.round((2f - k) * 2f) / 2f * base;
            PixelIcons.draw(c, paint, "star", cx - size / 2f, cy - size / 2f, size);
        }
        if (running) postInvalidateOnAnimation();
    }
}
