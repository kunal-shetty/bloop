package com.kun.pixelwords;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.kun.pixelwords.audio.Sfx;
import com.kun.pixelwords.data.Days;
import com.kun.pixelwords.data.ProgressStore;
import com.kun.pixelwords.game.Progress;
import com.kun.pixelwords.game.Season;
import com.kun.pixelwords.services.PlayGamesService;
import com.kun.pixelwords.ui.AmbientView;
import com.kun.pixelwords.ui.PanelDrawable;
import com.kun.pixelwords.ui.PixelButton;
import com.kun.pixelwords.ui.PixelTheme;
import com.kun.pixelwords.ui.StatChip;

/** A titled screen with a back button, the player's coins and a scrolling list. */
abstract class PageActivity extends BaseActivity {

    protected ProgressStore store;
    protected Progress progress;
    protected StatChip coins;

    protected abstract String pageTitle();

    protected abstract AmbientView.Theme pageTheme();

    /** Adds the screen's contents. {@code progress} is ready when this runs. */
    protected abstract void fillPage(LinearLayout list);

    /** Brings the screen up to date with {@code progress}. */
    protected abstract void refreshPage();

    @Override
    protected View createBackground() {
        AmbientView ambient = new AmbientView(this);
        ambient.setTheme(pageTheme());
        return ambient;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new ProgressStore(this);
        progress = store.load();
        if (Season.rollOver(progress, Days.today())) store.save(progress);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(14), dp(12), dp(14), 0);
        page.addView(topBar(), matchWrap(0));

        ScrollView scroll = new ScrollView(this);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(2), 0, dp(16));
        scroll.addView(list);
        page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        contentLayer.addView(page, fill());

        fillPage(list);
        fx.preload("ui_confetti", "pickups_collect-burst");
        coins.setValue(progress.coins);
    }

    @Override
    protected void onResume() {
        super.onResume();
        progress = store.load();
        coins.setValue(progress.coins);
        refreshPage();
    }

    private View topBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        PixelButton back = new PixelButton(this, "", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
        back.setIcon("back");
        back.setSpokenName("Back");
        back.setIconSize(24);
        back.setClickSound(Sfx.Id.UI_BACK);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView title = PixelTheme.display(this, pageTitle(), PixelTheme.SIZE_TITLE, PixelTheme.GOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        coins = new StatChip(this, "coin");
        coins.setSpokenName("Coins");
        bar.addView(coins);
        return bar;
    }

    protected int dp(float v) {
        return (int) PixelTheme.dp(this, v);
    }

    protected LinearLayout.LayoutParams matchWrap(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(top);
        return p;
    }

    protected void addHeading(LinearLayout list, String text, String detail) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(new PanelDrawable(0xE0251D4A, Math.max(2f, dp(1))));
        row.setPadding(dp(10), dp(8), dp(10), dp(9));
        row.addView(PixelTheme.display(this, text, PixelTheme.SIZE_SMALL, PixelTheme.CREAM),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (detail != null) row.addView(PixelTheme.body(this, detail, PixelTheme.SIZE_BODY, PixelTheme.GOLD));
        list.addView(row, matchWrap(10));
    }

    /** Saves locally and, if the player has cloud save, there too. */
    protected void save() {
        store.save(progress);
        if (PlayGamesService.isConfigured(this)) PlayGamesService.saveProgress(this, progress.serialize());
    }

    protected void celebrate() {
        Sfx.play(Sfx.Id.BUY);
        fx.play("ui_confetti", fx.getWidth() / 2f, fx.getHeight() * 0.2f, PixelTheme.dp(this, 2.4f), null);
    }
}
