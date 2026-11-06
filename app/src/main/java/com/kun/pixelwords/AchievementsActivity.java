package com.kun.pixelwords;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.kun.pixelwords.audio.Haptics;
import com.kun.pixelwords.audio.Sfx;
import com.kun.pixelwords.data.ProgressStore;
import com.kun.pixelwords.game.Achievement;
import com.kun.pixelwords.game.Progress;
import com.kun.pixelwords.services.PlayGamesService;
import com.kun.pixelwords.ui.AchievementCard;
import com.kun.pixelwords.ui.AmbientView;
import com.kun.pixelwords.ui.PanelDrawable;
import com.kun.pixelwords.ui.PixelBar;
import com.kun.pixelwords.ui.PixelButton;
import com.kun.pixelwords.ui.PixelIcons;
import com.kun.pixelwords.ui.PixelTheme;
import com.kun.pixelwords.ui.StatChip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Every achievement, with rewards to claim at the top, then the closest goals, then finished ones. */
public class AchievementsActivity extends BaseActivity {

    private ProgressStore store;
    private Progress progress;
    private StatChip coins;
    private TextView summary, waiting;
    private PixelBar bar;
    private final List<AchievementCard> cards = new ArrayList<>();

    @Override
    protected View createBackground() {
        AmbientView ambient = new AmbientView(this);
        ambient.setTheme(AmbientView.Theme.FIREFLIES);
        return ambient;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new ProgressStore(this);
        progress = store.load();
        buildUi();
        fx.preload("pickups_collect-burst");
        coins.setValue(progress.coins);
        refresh();
    }

    private int dp(float v) {
        return (int) PixelTheme.dp(this, v);
    }

    private void buildUi() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(14), dp(12), dp(14), 0);
        page.addView(topBar(), matchWrap(0));
        page.addView(summaryPanel(), matchWrap(10));

        ScrollView scroll = new ScrollView(this);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(2), 0, dp(16));
        for (Achievement a : inDisplayOrder()) {
            AchievementCard card = new AchievementCard(this, a);
            card.setClickSound(null);
            card.setOnClickListener(v -> onCard(card));
            cards.add(card);
            list.addView(card, matchWrap(8));
        }
        scroll.addView(list);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        sp.topMargin = dp(4);
        page.addView(scroll, sp);
        contentLayer.addView(page, fill());
    }

    private View topBar() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        PixelButton back = new PixelButton(this, "", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
        back.setIcon("back");
        back.setIconSize(24);
        back.setClickSound(Sfx.Id.UI_BACK);
        back.setOnClickListener(v -> finish());
        row.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView title = PixelTheme.display(this, "ACHIEVEMENTS", 12, PixelTheme.GOLD);
        title.setGravity(Gravity.CENTER);
        row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        coins = new StatChip(this, "coin");
        row.addView(coins);
        return row;
    }

    private View summaryPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setBackground(new PanelDrawable(0xE03A2E6E, Math.max(2f, dp(1))));
        panel.setPadding(dp(12), dp(10), dp(12), dp(12));
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(44), dp(44));
        ip.rightMargin = dp(12);
        panel.addView(PixelIcons.view(this, "trophy"), ip);

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        summary = PixelTheme.display(this, "", 10, PixelTheme.CREAM);
        text.addView(summary);
        bar = new PixelBar(this);
        bar.setSegments(Achievement.values().length);
        bar.setColor(PixelTheme.GOLD);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(16));
        bp.topMargin = dp(6);
        text.addView(bar, bp);
        waiting = PixelTheme.body(this, "", 16, PixelTheme.TEXT_DIM);
        waiting.setPadding(0, dp(4), 0, 0);
        text.addView(waiting);
        panel.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return panel;
    }

    private LinearLayout.LayoutParams matchWrap(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(top);
        return p;
    }

    /** Set when the screen opens, so a card stays where it is when you claim it. */
    private List<Achievement> inDisplayOrder() {
        List<Achievement> order = new ArrayList<>(Arrays.asList(Achievement.values()));
        order.sort(Comparator.<Achievement>comparingInt(this::group).thenComparingDouble(this::remaining));
        return order;
    }

    private int group(Achievement a) {
        return a.isClaimable(progress) ? 0 : a.isClaimed(progress) ? 2 : 1;
    }

    private double remaining(Achievement a) {
        return 1.0 - a.progress(progress) / (double) a.goal;
    }

    private void refresh() {
        int unlocked = Achievement.unlocked(progress).size(), total = Achievement.values().length;
        summary.setText(unlocked + " OF " + total + " UNLOCKED");
        bar.animateTo(unlocked / (float) total);
        int ready = 0;
        for (Achievement a : Achievement.claimable(progress)) ready += a.tier.coins;
        waiting.setText(ready > 0 ? ready + " coins ready to claim!" : "Keep playing to earn more coins");
        waiting.setTextColor(ready > 0 ? PixelTheme.GOLD : PixelTheme.TEXT_DIM);
        for (AchievementCard c : cards) c.update(progress);
    }

    private void onCard(AchievementCard card) {
        if (!card.achievement.claim(progress)) {
            Sfx.play(Sfx.Id.UI_TAP);
            return;
        }
        store.save(progress);
        if (PlayGamesService.isConfigured(this)) PlayGamesService.saveProgress(this, progress.serialize());
        refresh();
        coins.animateTo(progress.coins);
        Sfx.play(Sfx.Id.COIN);
        Haptics.success();
        burstOver(card);
    }

    private void burstOver(View card) {
        int[] from = new int[2], to = new int[2];
        card.getLocationInWindow(from);
        fx.getLocationInWindow(to);
        float x = from[0] - to[0] + card.getWidth() / 2f, y = from[1] - to[1] + card.getHeight() / 2f;
        fx.play("pickups_collect-burst", x, y, PixelTheme.dp(this, 3), null);
    }
}
