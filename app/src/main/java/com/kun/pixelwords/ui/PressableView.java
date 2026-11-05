package com.kun.pixelwords.ui;

import android.content.Context;
import android.view.View;

import com.kun.pixelwords.audio.Haptics;
import com.kun.pixelwords.audio.Sfx;

/** Base for every tappable thing: physically sinks when held, and always clicks and buzzes. */
public abstract class PressableView extends View {

    protected final float px;
    protected final float depth;
    private Sfx.Id clickSound = Sfx.Id.UI_TAP;

    protected PressableView(Context context) {
        super(context);
        px = Math.max(2f, Math.round(PixelTheme.dp(context, 1.5f)));
        depth = px * 3;
        setClickable(true);
        setFocusable(true);
        setSoundEffectsEnabled(false);
        setHapticFeedbackEnabled(false);
    }

    public void setClickSound(Sfx.Id id) {
        clickSound = id;
    }

    /** How far the face has sunk into its lip right now. */
    protected float sink() {
        return isPressed() ? depth * 0.75f : 0f;
    }

    @Override
    public void setPressed(boolean pressed) {
        boolean was = isPressed();
        super.setPressed(pressed);
        if (pressed != was) {
            if (pressed) Haptics.tick();
            invalidate();
        }
    }

    @Override
    public boolean performClick() {
        if (clickSound != null) Sfx.play(clickSound);
        Haptics.click();
        return super.performClick();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setAlpha(enabled ? 1f : 0.5f);
    }
}
