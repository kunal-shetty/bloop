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
import com.kun.pixelwords.data.Days;
import com.kun.pixelwords.data.ProgressStore;
import com.kun.pixelwords.game.Achievement;
import com.kun.pixelwords.game.Progress;
import com.kun.pixelwords.game.Quest;
import com.kun.pixelwords.game.Season;
import com.kun.pixelwords.services.PlayGamesService;
import com.kun.pixelwords.ui.AmbientView;
import com.kun.pixelwords.ui.GoalCard;
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
import java.util.function.BooleanSupplier;

/** This week's quests, then every achievement: rewards to claim first, then the closest goals. */
public class AchievementsActivity extends BaseActivity {

    private static final int BRONZE = 0xFFD08A4E;
    private static final int SILVER = 0xFFCFD8EA;
    private static final int PLATINUM = 0xFF9BE8F5;

    private ProgressStore store;
    private Progress progress;
    private StatChip coins;
    private TextView summary, waiting;
    private PixelBar bar;
    private final List<Runnable> cardUpdates = new ArrayList<>();

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
        if (Quest.rollOver(progress, Days.today())) store.save(progress);
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

        int days = Quest.daysLeft(Days.today());
        addHeading(list, "THIS WEEK", days == 1 ? "Last day!" : days + " days left");
        for (Quest q : Quest.forWeek(progress.questWeek)) addQuest(list, q);
        List<Achievement> ordered = inDisplayOrder();
        for (Achievement.Category category : Achievement.Category.values()) {
            int done = 0, total = 0;
            for (Achievement a : ordered) {
                if (a.category != category) continue;
                total++;
                if (a.isUnlocked(progress)) done++;
            }
            addHeading(list, category.label, done + " / " + total);
            for (Achievement a : ordered) if (a.category == category) addAchievement(list, a);
        }

        scroll.addView(list);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        sp.topMargin = dp(4);
        page.addView(scroll, sp);
        contentLayer.addView(page, fill());
    }

    private void addHeading(LinearLayout list, String text, String detail) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(new PanelDrawable(0xE0251D4A, Math.max(2f, dp(1))));
        row.setPadding(dp(10), dp(8), dp(10), dp(9));
        row.addView(PixelTheme.display(this, text, PixelTheme.SIZE_SMALL, PixelTheme.CREAM),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (detail != null) row.addView(PixelTheme.body(this, detail, PixelTheme.SIZE_BODY, PixelTheme.GOLD));
        list.addView(row, matchWrap(10));
    }

    private void addQuest(LinearLayout list, Quest q) {
        GoalCard card = new GoalCard(this, q.title(), q.icon(), PixelTheme.SKY, q.coins, q.goal);
        addCard(list, card, () -> card.update(q.progress(progress), q.isClaimed(progress)), () -> {
            if (!q.claim(progress)) return false;
            Season.addXp(progress, Days.today(), Season.XP_QUEST);
            return true;
        });
    }

    private void addAchievement(LinearLayout list, Achievement a) {
        GoalCard card = new GoalCard(this, a.title, a.icon, medalColor(a), a.tier.coins, a.goal);
        addCard(list, card, () -> card.update(a.progress(progress), a.isClaimed(progress)), () -> a.claim(progress));
    }

    private void addCard(LinearLayout list, GoalCard card, Runnable update, BooleanSupplier claim) {
        card.setClickSound(null);
        card.setOnClickListener(v -> onCard(card, claim));
        cardUpdates.add(update);
        list.addView(card, matchWrap(8));
    }

    private static int medalColor(Achievement a) {
        switch (a.tier) {
            case BRONZE: return BRONZE;
            case SILVER: return SILVER;
            case PLATINUM: return PLATINUM;
            default: return PixelTheme.GOLD;
        }
    }

    private View topBar() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        PixelButton back = new PixelButton(this, "", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
        back.setIcon("back");
        back.setSpokenName("Back");
        back.setIconSize(24);
        back.setClickSound(Sfx.Id.UI_BACK);
        back.setOnClickListener(v -> finish());
        row.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView title = PixelTheme.display(this, "GOALS", PixelTheme.SIZE_TITLE, PixelTheme.GOLD);
        title.setGravity(Gravity.CENTER);
        row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        coins = new StatChip(this, "coin");
        coins.setSpokenName("Coins");
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
        summary = PixelTheme.display(this, "", PixelTheme.SIZE_SMALL, PixelTheme.CREAM);
        text.addView(summary);
        bar = new PixelBar(this);
        bar.setSegments(Achievement.values().length);
        bar.setColor(PixelTheme.GOLD);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(16));
        bp.topMargin = dp(6);
        text.addView(bar, bp);
        waiting = PixelTheme.body(this, "", PixelTheme.SIZE_BODY, PixelTheme.TEXT_DIM);
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
        for (Quest q : Quest.claimable(progress)) ready += q.coins;
        waiting.setText(ready > 0 ? ready + " coins ready to claim!" : "Keep playing to earn more coins");
        waiting.setTextColor(ready > 0 ? PixelTheme.GOLD : PixelTheme.TEXT_DIM);
        for (Runnable update : cardUpdates) update.run();
    }

    private void onCard(View card, BooleanSupplier claim) {
        if (!claim.getAsBoolean()) {
            Sfx.play(Sfx.Id.UI_TAP);
            return;
        }
        store.save(progress);
        if (PlayGamesService.isConfigured(this)) PlayGamesService.saveProgress(this, progress.serialize());
        refresh();
        earn(card, coins, progress.coins);
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
