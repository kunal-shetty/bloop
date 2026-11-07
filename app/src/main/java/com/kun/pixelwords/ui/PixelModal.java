package com.kun.pixelwords.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.kun.pixelwords.BaseActivity;
import com.kun.pixelwords.audio.Haptics;
import com.kun.pixelwords.audio.Sfx;

import java.util.ArrayList;
import java.util.List;

/**
 * The game's one dialog: a pixel panel with a coloured header, explanatory copy, labelled stats and
 * actions. Replaces every system alert. Build it, call {@link #show()}; it lives inside the screen so
 * confetti and other effects can play over it.
 */
public final class PixelModal {

    private static final long ROW_STAGGER = 80;

    private final BaseActivity activity;
    private final Context ctx;
    private final LinearLayout content;
    private final List<View> rows = new ArrayList<>();
    private final List<Runnable> rowFinishers = new ArrayList<>();
    private final LinearLayout buttons;

    private FrameLayout overlay;
    private LinearLayout panel;
    private String title = "";
    private String icon;
    private int accent = PixelTheme.GOLD;
    private boolean cancelable = true;
    private boolean quietOpen;
    private Runnable onDismiss;
    private boolean closed;

    public PixelModal(BaseActivity activity) {
        this.activity = activity;
        this.ctx = activity;
        content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        buttons = new LinearLayout(ctx);
        buttons.setOrientation(LinearLayout.VERTICAL);
    }

    private int dp(float v) {
        return (int) PixelTheme.dp(ctx, v);
    }

    public PixelModal title(String s) {
        title = s;
        return this;
    }

    public PixelModal icon(String name) {
        icon = name;
        return this;
    }

    public PixelModal accent(int color) {
        accent = color;
        return this;
    }

    /** Non-cancelable modals have no close button and ignore Back and outside taps. */
    public PixelModal cancelable(boolean c) {
        cancelable = c;
        return this;
    }

    /** Skips the opening sound, for moments that already have their own fanfare. */
    public PixelModal quiet() {
        quietOpen = true;
        return this;
    }

    public PixelModal onDismiss(Runnable r) {
        onDismiss = r;
        return this;
    }

    public PixelModal body(CharSequence text) {
        TextView t = PixelTheme.body(ctx, text, PixelTheme.SIZE_BODY, PixelTheme.TEXT);
        t.setLineSpacing(dp(2), 1f);
        return row(t, 0, 10);
    }

    public PixelModal note(CharSequence text) {
        TextView t = PixelTheme.body(ctx, text, PixelTheme.SIZE_BODY, PixelTheme.TEXT_DIM);
        t.setLineSpacing(dp(2), 1f);
        return row(t, 0, 10);
    }

    public PixelModal heading(String text) {
        return row(PixelTheme.display(ctx, text, PixelTheme.SIZE_CAPTION, PixelTheme.TEXT_DIM), 8, 6);
    }

    public PixelModal custom(View v) {
        return row(v, 0, 8);
    }

    public PixelModal stat(String iconName, String label, String value) {
        return stat(iconName, label, value, PixelTheme.TEXT);
    }

    public PixelModal stat(String iconName, String label, String value, int valueColor) {
        return row(statRow(iconName, label, PixelTheme.display(ctx, value, PixelTheme.SIZE_LABEL, valueColor)), 0, 3);
    }

    /** A stat whose number rolls up from zero when the row appears. */
    public PixelModal countStat(String iconName, String label, final String prefix, final int value, int valueColor) {
        final TextView v = PixelTheme.display(ctx, prefix + "0", PixelTheme.SIZE_LABEL, valueColor);
        row(statRow(iconName, label, v), 0, 3);
        final int index = rows.size() - 1;
        rowFinishers.add(() -> {
            ValueAnimator a = ValueAnimator.ofInt(0, value);
            a.setDuration(Math.min(800, 180 + value * 6L));
            a.setStartDelay(rowDelay(index) + 120);
            a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                private int last;

                @Override public void onAnimationUpdate(ValueAnimator u) {
                    int shown = (int) u.getAnimatedValue();
                    v.setText(prefix + shown);
                    if (shown / 8 != last / 8) Sfx.play(Sfx.Id.COUNT_TICK);
                    last = shown;
                }
            });
            a.start();
        });
        return this;
    }

    /** An on/off line in the settings list. */
    public PixelModal toggle(String iconName, String label, String detail, boolean on, ToggleView.Listener l) {
        LinearLayout r = new LinearLayout(ctx);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.addView(iconView(iconName), new LinearLayout.LayoutParams(dp(26), dp(26)));

        LinearLayout text = new LinearLayout(ctx);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(PixelTheme.display(ctx, label, PixelTheme.SIZE_SMALL, PixelTheme.TEXT));
        TextView d = PixelTheme.body(ctx, detail, PixelTheme.SIZE_BODY, PixelTheme.TEXT_DIM);
        d.setPadding(0, dp(3), 0, 0);
        text.addView(d);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(10), 0, dp(8), 0);
        r.addView(text, lp);
        r.addView(new ToggleView(ctx, on, l));
        return row(r, 2, 10);
    }

    public PixelModal primary(String label, Runnable action) {
        return button(label, PixelTheme.GOLD, PixelTheme.INK, null, Sfx.Id.UI_TAP, action);
    }

    public PixelModal primary(String label, String iconName, Runnable action) {
        return button(label, PixelTheme.GOLD, PixelTheme.INK, iconName, Sfx.Id.UI_TAP, action);
    }

    public PixelModal secondary(String label, Runnable action) {
        return button(label, PixelTheme.PANEL_LIGHT, PixelTheme.TEXT, null, Sfx.Id.UI_BACK, action);
    }

    public PixelModal button(String label, int fill, int textColor, String iconName, Sfx.Id sound, final Runnable action) {
        PixelButton b = new PixelButton(ctx, label, fill, textColor);
        b.setLabelSize(PixelTheme.SIZE_LABEL);
        b.setClickSound(sound);
        if (iconName != null) b.setIcon(iconName);
        b.setOnClickListener(v -> {
            if (closed) return;
            if (action != null) action.run();
            dismiss();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        buttons.addView(b, lp);
        return this;
    }

    private View statRow(String iconName, String label, TextView value) {
        LinearLayout r = new LinearLayout(ctx);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setBackground(new PanelDrawable(PixelTheme.BG_MID, Math.max(2f, dp(1))));
        r.setPadding(dp(10), dp(7), dp(10), dp(7));
        if (iconName != null) {
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(22), dp(22));
            ip.rightMargin = dp(10);
            r.addView(iconView(iconName), ip);
        }
        r.addView(PixelTheme.body(ctx, label, PixelTheme.SIZE_BODY, PixelTheme.TEXT_DIM),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(value);
        return r;
    }

    private View iconView(String name) {
        return PixelIcons.view(ctx, name);
    }

    private PixelModal row(View v, int topMargin, int bottomMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(topMargin);
        lp.bottomMargin = dp(bottomMargin);
        content.addView(v, lp);
        rows.add(v);
        return this;
    }

    private long rowDelay(int index) {
        return 170 + index * ROW_STAGGER;
    }

    public PixelModal show() {
        build();
        activity.attachModal(this, overlay);
        animateIn();
        if (!quietOpen) Sfx.play(Sfx.Id.MODAL_OPEN);
        Haptics.thud();
        return this;
    }

    private void build() {
        overlay = new FrameLayout(ctx);
        overlay.setBackgroundColor(0xD80B0820);
        overlay.setClickable(true);
        overlay.setOnClickListener(v -> {
            if (cancelable) cancel();
        });

        int screenW = ctx.getResources().getDisplayMetrics().widthPixels;
        int screenH = ctx.getResources().getDisplayMetrics().heightPixels;
        float framePx = Math.max(2f, Math.round(PixelTheme.dp(ctx, 1.5f)));

        panel = new LinearLayout(ctx);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(new PanelDrawable(PixelTheme.PANEL, framePx));
        panel.setClickable(true); // tapping the panel must not count as an outside tap
        int inset = (int) (framePx * 3);
        panel.setPadding(inset, inset, inset, inset + (int) framePx);

        panel.addView(header(framePx), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final int maxBody = (int) (screenH * 0.56f);
        ScrollView scroll = new ScrollView(ctx) {
            @Override protected void onMeasure(int w, int h) {
                super.onMeasure(w, MeasureSpec.makeMeasureSpec(maxBody, MeasureSpec.AT_MOST));
            }
        };
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);
        content.setPadding(dp(12), dp(14), dp(12), dp(4));
        scroll.addView(content);
        panel.addView(scroll);

        if (buttons.getChildCount() > 0) {
            buttons.setPadding(dp(12), 0, dp(12), dp(10));
            panel.addView(buttons);
        }

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                Math.min(screenW - dp(32), dp(380)), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        overlay.addView(panel, lp);
    }

    private View header(float framePx) {
        LinearLayout h = new LinearLayout(ctx);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setBackground(new PanelDrawable(accent, framePx));
        h.setPadding(dp(12), dp(11), dp(8), dp(13));
        if (icon != null) {
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(30), dp(30));
            ip.rightMargin = dp(10);
            h.addView(iconView(icon), ip);
        }
        TextView t = PixelTheme.display(ctx, title, PixelTheme.SIZE_LABEL, PixelTheme.INK);
        h.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (cancelable) {
            PixelButton x = new PixelButton(ctx, "", PixelTheme.shade(accent, 0.8f), PixelTheme.INK);
            x.setIcon("close");
            x.setSpokenName("Close");
            x.setIconSize(18);
            x.setClickSound(Sfx.Id.UI_BACK);
            x.setOnClickListener(v -> cancel());
            h.addView(x, new LinearLayout.LayoutParams(dp(38), dp(38)));
        }
        return h;
    }

    private void animateIn() {
        overlay.setAlpha(0f);
        overlay.animate().alpha(1f).setDuration(140).start();
        if (PixelTheme.reduceMotion()) { // it fades in, and everything is already in place
            for (Runnable r : rowFinishers) r.run();
            return;
        }
        panel.setScaleX(0.72f);
        panel.setScaleY(0.72f);
        panel.animate().scaleX(1f).scaleY(1f).setDuration(280).setInterpolator(new OvershootInterpolator(1.8f)).start();

        float slide = dp(26);
        for (int i = 0; i < rows.size(); i++) {
            View r = rows.get(i);
            r.setAlpha(0f);
            r.setTranslationX(slide);
            r.animate().alpha(1f).translationX(0f).setStartDelay(rowDelay(i)).setDuration(180)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }
        long buttonsAt = rowDelay(rows.size());
        buttons.setAlpha(0f);
        buttons.animate().alpha(1f).setStartDelay(buttonsAt).setDuration(160).start();
        for (Runnable r : rowFinishers) r.run();
    }

    public boolean isCancelable() {
        return cancelable;
    }

    /** Back button, outside tap or the X. */
    public void cancel() {
        if (closed || !cancelable) return;
        Sfx.play(Sfx.Id.MODAL_CLOSE);
        dismiss();
    }

    public void dismiss() {
        if (closed) return;
        closed = true;
        panel.animate().cancel();
        panel.animate().scaleX(0.85f).scaleY(0.85f).setStartDelay(0).setDuration(110).setInterpolator(new DecelerateInterpolator()).start();
        overlay.setClickable(false);
        overlay.animate().alpha(0f).setDuration(120).withEndAction(() -> activity.detachModal(this, overlay)).start();
        if (onDismiss != null) onDismiss.run();
    }

    /** Removed instantly because another modal is replacing it, so its dismiss action must not fire. */
    public void replaceSilently() {
        closed = true;
        activity.detachModal(this, overlay);
    }
}
