package com.kun.pixelwords.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.kun.pixelwords.audio.Sfx;

/** A chunky on/off switch. */
public class ToggleView extends PressableView {

    public interface Listener {
        void onToggled(boolean on);
    }

    private final Paint paint = new Paint();
    private boolean on;
    private float knob;
    private Listener listener;

    public ToggleView(Context context, boolean on, Listener listener) {
        super(context);
        this.on = on;
        this.knob = on ? 1f : 0f;
        this.listener = listener;
        setClickSound(Sfx.Id.UI_TOGGLE);
        setOnClickListener(v -> toggle());
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return android.widget.Switch.class.getName();
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setCheckable(true);
        info.setChecked(on);
    }

    private void toggle() {
        on = !on;
        ValueAnimator a = ValueAnimator.ofFloat(knob, on ? 1f : 0f);
        a.setDuration(110);
        a.addUpdateListener(u -> {
            knob = (float) u.getAnimatedValue();
            invalidate();
        });
        a.start();
        listener.onToggled(on);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension((int) PixelTheme.dp(getContext(), 60), (int) PixelTheme.dp(getContext(), 32));
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        PixelTheme.drawSlot(c, paint, 0, 0, w, h, on ? 0xFF2FA873 : PixelTheme.BG_DEEP, px);
        float size = h - px * 4;
        float x = px * 2 + (w - size - px * 4) * knob;
        PixelTheme.drawTile(c, paint, x, px * 2, x + size, px * 2 + size, on ? PixelTheme.MINT : PixelTheme.TEXT_DIM, px, px);
    }
}
