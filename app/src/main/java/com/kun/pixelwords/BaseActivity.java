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

import com.kun.pixelwords.fx.FxLayer;
import com.kun.pixelwords.ui.PixelModal;

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

    @SuppressWarnings("deprecation")
    protected void go(Intent intent) {
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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
