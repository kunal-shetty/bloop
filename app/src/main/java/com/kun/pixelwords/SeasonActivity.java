package com.kun.pixelwords;

import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kun.pixelwords.audio.Sfx;
import com.kun.pixelwords.data.Days;
import com.kun.pixelwords.game.FishHunt;
import com.kun.pixelwords.game.Reward;
import com.kun.pixelwords.game.Season;
import com.kun.pixelwords.ui.AmbientView;
import com.kun.pixelwords.ui.PanelDrawable;
import com.kun.pixelwords.ui.PixelBar;
import com.kun.pixelwords.ui.PixelButton;
import com.kun.pixelwords.ui.PixelModal;
import com.kun.pixelwords.ui.PixelTheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The season's tiers on two tracks, with the month's Fish Hunt above them. */
public class SeasonActivity extends PageActivity {

    /** One button and how to bring it up to date. */
    private interface Row {
        void refresh();
    }

    private final List<Row> rows = new ArrayList<>();
    private TextView summary, huntStatus;
    private PixelBar bar;
    private PixelButton premium;

    @Override
    protected String pageTitle() {
        return "SEASON PASS";
    }

    @Override
    protected AmbientView.Theme pageTheme() {
        return AmbientView.Theme.SNOW;
    }

    @Override
    protected void fillPage(LinearLayout list) {
        long today = Days.today();
        Season.Theme theme = Season.theme(progress.seasonId);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(new PanelDrawable(0xE03A2E6E, Math.max(2f, dp(1))));
        panel.setPadding(dp(12), dp(10), dp(12), dp(12));
        panel.addView(PixelTheme.display(this, "SEASON " + progress.seasonId + ": "
                + theme.title.toUpperCase(Locale.ROOT), 11, theme.color));
        summary = PixelTheme.body(this, "", PixelTheme.SIZE_BODY, PixelTheme.TEXT);
        summary.setPadding(0, dp(6), 0, dp(4));
        panel.addView(summary);
        bar = new PixelBar(this);
        bar.setSegments(Season.XP_PER_TIER / 4);
        bar.setColor(theme.color);
        panel.addView(bar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(16)));
        premium = new PixelButton(this, "UNLOCK PREMIUM", PixelTheme.GOLD, PixelTheme.INK);
        premium.setLabelSize(PixelTheme.SIZE_LABEL);
        premium.setIcon("ticket");
        premium.setSubtitle(Season.PREMIUM_COINS + " coins");
        premium.setOnClickListener(v -> confirmPremium());
        panel.addView(premium, matchWrap(10));
        list.addView(panel, matchWrap(10));

        addHunt(list, today);

        addHeading(list, "REWARDS", "FREE  /  PREMIUM");
        for (int tier = 1; tier <= Season.TIERS; tier++) addTier(list, tier);
        list.addView(PixelTheme.body(this, "Earn XP from new levels, dailies, weekly quests and the weekend board. "
                + "Anything you've earned but not claimed is paid when the season ends.", 16, PixelTheme.TEXT_DIM), matchWrap(8));
    }

    private void addHunt(LinearLayout list, long today) {
        boolean live = FishHunt.isLive(today);
        addHeading(list, "FISH HUNT", live ? FishHunt.daysToChange(today) + " days left"
                : "Starts in " + FishHunt.daysToChange(today) + " days");
        huntStatus = PixelTheme.body(this, "", PixelTheme.SIZE_BODY, PixelTheme.TEXT);
        huntStatus.setPadding(dp(4), dp(6), dp(4), 0);
        list.addView(huntStatus);
        for (int i = 0; i < FishHunt.prizes(); i++) addPrize(list, i);
    }

    private void addPrize(LinearLayout list, int prize) {
        Reward reward = FishHunt.prize(prize);
        PixelButton b = new PixelButton(this, FishHunt.goal(prize) + " FISH", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
        b.setLabelSize(PixelTheme.SIZE_SMALL);
        b.setIcon(reward.icon());
        b.setSubtitle(reward.label());
        b.setOnClickListener(v -> {
            Reward paid = FishHunt.claim(progress, prize);
            onClaimed(paid);
        });
        rows.add(() -> styleClaim(b, FishHunt.isClaimable(progress, prize), FishHunt.isClaimed(progress, prize), reward));
        list.addView(b, matchWrap(6));
    }

    private void addTier(LinearLayout list, int tier) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView number = PixelTheme.display(this, String.valueOf(tier), PixelTheme.SIZE_SMALL, PixelTheme.CREAM);
        number.setGravity(Gravity.CENTER);
        row.addView(number, new LinearLayout.LayoutParams(dp(34), LinearLayout.LayoutParams.WRAP_CONTENT));
        row.addView(tierButton(tier, false), trackParams(0));
        row.addView(tierButton(tier, true), trackParams(6));
        list.addView(row, matchWrap(6));
    }

    private LinearLayout.LayoutParams trackParams(int left) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p.leftMargin = dp(left);
        return p;
    }

    private PixelButton tierButton(int tier, boolean isPremium) {
        Reward reward = Season.reward(progress.seasonId, tier, isPremium);
        PixelButton b = new PixelButton(this, "", PixelTheme.PANEL, PixelTheme.TEXT);
        b.setLabelSize(PixelTheme.SIZE_CAPTION);
        b.setIcon(reward.icon());
        b.setIconSize(20);
        b.setOnClickListener(v -> onClaimed(Season.claim(progress, tier, isPremium)));
        rows.add(() -> {
            styleClaim(b, Season.isClaimable(progress, tier, isPremium), Season.isClaimed(progress, tier, isPremium), reward);
            b.setText(Season.isClaimed(progress, tier, isPremium) ? "DONE" : shortLabel(reward));
            b.setSubtitle("");
        });
        return b;
    }

    /** Tier buttons are narrow, so long names lose their count. */
    private static String shortLabel(Reward reward) {
        String label = reward.label().toUpperCase(Locale.ROOT).replace("1 X ", "").replace("PICK A LETTER", "PICK LETTER");
        return label.length() <= 12 ? label : label.substring(0, 11).trim() + ".";
    }

    private void styleClaim(PixelButton b, boolean ready, boolean done, Reward reward) {
        b.setFill(ready ? PixelTheme.GOLD : done ? 0xFF2FA873 : PixelTheme.PANEL);
        b.setTextColor(ready ? PixelTheme.INK : PixelTheme.TEXT);
        b.setShine(ready);
        b.setSubtitle(done ? "Claimed" : reward.label());
    }

    @Override
    protected void refreshPage() {
        int reached = Season.tiersReached(progress);
        summary.setText("Tier " + reached + " of " + Season.TIERS + ", " + Season.daysLeft(Days.today()) + " days left");
        bar.setValue(Season.xpIntoTier(progress) / (float) Season.XP_PER_TIER);
        premium.setVisibility(progress.seasonPremium || !Season.PREMIUM_WITH_COINS ? android.view.View.GONE : android.view.View.VISIBLE);
        huntStatus.setText("You've caught " + progress.fishCount + " fish. They hide under letters on campaign levels and dailies.");
        for (Row r : rows) r.refresh();
    }

    private void onClaimed(Reward paid) {
        if (paid == null) {
            Sfx.play(Sfx.Id.UI_TAP);
            return;
        }
        save();
        earn(coins, progress.coins);
        Sfx.play(Sfx.Id.COIN);
        refreshPage();
        new PixelModal(this).title("REWARD").icon(paid.icon()).accent(PixelTheme.GOLD)
                .body(paid.label())
                .primary("NICE", null)
                .show();
    }

    private void confirmPremium() {
        if (progress.coins < Season.PREMIUM_COINS) {
            new PixelModal(this).title("PREMIUM PASS").icon("ticket").accent(PixelTheme.ROSE)
                    .stat("coin", "Price", String.valueOf(Season.PREMIUM_COINS), PixelTheme.GOLD)
                    .stat("coin", "Your coins", String.valueOf(progress.coins), PixelTheme.ROSE)
                    .note("Premium adds a second reward on every tier, with " + (Season.EXCLUSIVES_LOCKED ? "a big coin prize" : "an exclusive accessory") + " at the top.")
                    .primary("OK", null)
                    .show();
            return;
        }
        new PixelModal(this).title("UNLOCK PREMIUM?").icon("ticket").accent(PixelTheme.GOLD)
                .body("Adds a second reward on every tier this season, with " + (Season.EXCLUSIVES_LOCKED ? "a big coin prize" : "an exclusive accessory") + " at the top.")
                .stat("coin", "Price", String.valueOf(Season.PREMIUM_COINS), PixelTheme.GOLD)
                .stat("coin", "Coins left after", String.valueOf(progress.coins - Season.PREMIUM_COINS), PixelTheme.TEXT)
                .button("UNLOCK", PixelTheme.GOLD, PixelTheme.INK, "ticket", null, () -> {
                    if (!Season.buyPremium(progress)) return;
                    save();
                    coins.animateTo(progress.coins);
                    celebrate();
                    refreshPage();
                })
                .secondary("NOT NOW", null)
                .show();
    }
}
