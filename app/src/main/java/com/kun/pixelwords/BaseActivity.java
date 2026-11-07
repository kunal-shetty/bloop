package com.kun.pixelwords;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.DisplayCutout;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import com.kun.pixelwords.audio.Sfx;
import com.kun.pixelwords.fx.FxLayer;
import com.kun.pixelwords.ui.PixelModal;
import com.kun.pixelwords.ui.PixelTheme;
import com.kun.pixelwords.ui.StatChip;

/**
 * Shared screen scaffold, full-bleed under the notch. Layers, bottom to top:
 * background, content (kept clear of the notch), modals, effects.
 */
public abstract class BaseActivity extends Activity {

    protected FrameLayout root;
    protected FrameLayout contentLayer;
    protected FrameLayout modalLayer;
    protected FxLayer fx;
    private PixelModal modal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        root = new FrameLayout(this);
        contentLayer = new FrameLayout(this);
        modalLayer = new FrameLayout(this);
        fx = new FxLayer(this);

        View bg = createBackground();
        if (bg != null) root.addView(bg, fill());
        root.addView(contentLayer, fill());
        root.addView(modalLayer, fill());
        root.addView(fx, fill());

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            applySafeArea(insets);
            return insets;
        });
        setContentView(root);
        immersive();
    }

    /** The full-screen backdrop for this screen, or null. */
    protected View createBackground() {
        return null;
    }

    private void applySafeArea(WindowInsets insets) {
        int top = 0, bottom = 0;
        if (Build.VERSION.SDK_INT >= 28) {
            DisplayCutout cut = insets.getDisplayCutout();
            if (cut != null) {
                top = cut.getSafeInsetTop();
                bottom = cut.getSafeInsetBottom();
            }
        }
        contentLayer.setPadding(0, top, 0, bottom);
    }

    protected static FrameLayout.LayoutParams fill() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @SuppressWarnings("deprecation")
    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) immersive();
    }

    protected void go(Intent intent) {
        startActivity(intent);
        slide(R.anim.page_in, R.anim.page_stay_out);
    }

    /** Pages slide across, or simply fade for a player who asked for less movement. */
    @SuppressWarnings("deprecation")
    private void slide(int enter, int exit) {
        if (PixelTheme.reduceMotion()) overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        else overridePendingTransition(enter, exit);
    }

    @Override
    public void finish() {
        super.finish();
        slide(R.anim.page_stay_in, R.anim.page_out);
    }

    /** Goes into a puzzle. The screen stays dark until the game's star iris opens onto the board. */
    @SuppressWarnings("deprecation")
    protected void goToBoard(Intent intent) {
        startActivity(intent);
        if (PixelTheme.reduceMotion()) overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        else overridePendingTransition(0, 0);
    }

    // ------------------------------------------------------------------ rewards

    private static final int MIN_FLYING_COINS = 3, MAX_FLYING_COINS = 10, COINS_PER_FLYER = 15;
    private static final float FLYING_COIN_DP = 22;

    /** Coins fly from the middle of the screen into the readout, which counts up to {@code balance}. */
    protected void earn(StatChip chip, int balance) {
        earn(null, chip, balance);
    }

    /** Coins fly from a view into the readout, which counts up to {@code balance}. */
    protected void earn(View from, StatChip chip, int balance) {
        int gained = balance - chip.value();
        if (gained <= 0 || PixelTheme.reduceMotion() || chip.getWidth() == 0) {
            chip.animateTo(balance);
            return;
        }
        int[] layer = new int[2], target = new int[2];
        fx.getLocationInWindow(layer);
        chip.getLocationInWindow(target);
        float fromX = fx.getWidth() / 2f, fromY = fx.getHeight() * 0.45f;
        if (from != null) {
            int[] start = new int[2];
            from.getLocationInWindow(start);
            fromX = start[0] - layer[0] + from.getWidth() / 2f;
            fromY = start[1] - layer[1] + from.getHeight() / 2f;
        }
        int flyers = Math.max(MIN_FLYING_COINS, Math.min(MAX_FLYING_COINS, gained / COINS_PER_FLYER));
        float toX = target[0] - layer[0] + chip.getHeight() / 2f, toY = target[1] - layer[1] + chip.getHeight() / 2f;
        fx.flyCoins(fromX, fromY, toX, toY, flyers, PixelTheme.dp(this, FLYING_COIN_DP), new Runnable() {
            private int landed;

            @Override public void run() {
                landed++;
                Sfx.play(Sfx.Id.COUNT_TICK, 1f + landed * 0.06f);
                chip.animateTo(chip.value() + (landed == flyers ? balance - chip.value() : gained / flyers));
            }
        });
    }

    // ------------------------------------------------------------------ modals

    public void attachModal(PixelModal m, View overlay) {
        if (modal != null) modal.replaceSilently();
        modal = m;
        modalLayer.addView(overlay, fill());
    }

    public void detachModal(PixelModal m, View overlay) {
        if (modal == m) modal = null;
        modalLayer.removeView(overlay);
    }

    protected boolean modalShowing() {
        return modal != null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        if (modal != null) {
            modal.cancel(); // does nothing if this modal insists on an answer
            return;
        }
        onBack();
    }

    protected void onBack() {
        finish();
    }
}
