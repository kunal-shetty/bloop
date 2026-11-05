package com.kun.pixelwords;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.kun.pixelwords.audio.Haptics;
import com.kun.pixelwords.audio.Sfx;
import com.kun.pixelwords.data.Days;
import com.kun.pixelwords.data.ProgressStore;
import com.kun.pixelwords.data.Settings;
import com.kun.pixelwords.game.Progress;
import com.kun.pixelwords.game.Scoring;
import com.kun.pixelwords.game.StreakLogic;
import com.kun.pixelwords.services.PlayGamesService;
import com.kun.pixelwords.ui.AmbientView;
import com.kun.pixelwords.ui.CatView;
import com.kun.pixelwords.ui.ModeCard;
import com.kun.pixelwords.ui.PanelDrawable;
import com.kun.pixelwords.ui.PixelButton;
import com.kun.pixelwords.ui.PixelModal;
import com.kun.pixelwords.ui.PixelTheme;
import com.kun.pixelwords.ui.StatChip;

import java.util.Locale;

/** Main menu: your cat, the streak and shop, the big PLAY button, and a card for each game mode. */
public class MainActivity extends BaseActivity {

    private static final String[] CHAPTERS = {"FROSTFALL", "PETAL DRIFT", "FIREFLY WOODS", "EMBER LEAVES"};
    private static final int LEVELS_PER_CHAPTER = 25;

    private ProgressStore store;
    private Progress progress;
    private PixelButton playBtn, cloudBtn;
    private ModeCard dailyCard, timedCard, duelCard, ranksCard;
    private StatChip coinChip, hintChip;
    private CatView cat;
    private TextView streakText, streakSub;
    private LinearLayout column;
    private boolean introduced;

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
        buildUi();
        syncWithCloud();
    }

    @Override
    protected void onResume() {
        super.onResume();
        progress = store.load();
        refresh();
        if (!introduced) {
            introduced = true;
            playIntro();
        }
    }

    private int dp(float v) {
        return (int) PixelTheme.dp(this, v);
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);

        column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(16), dp(14), dp(16), dp(14));
        scroll.addView(column);

        column.addView(topBar(), matchWrap(0));

        // The cat takes all the spare height, sitting just above the panels below.
        cat = new CatView(this, 132);
        column.addView(cat, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(136), 1f));

        column.addView(streakAndShop(), matchWrap(8));

        playBtn = new PixelButton(this, "PLAY", PixelTheme.GOLD, PixelTheme.INK);
        playBtn.setLabelSize(20);
        playBtn.setIcon("play");
        playBtn.setIconSize(30);
        playBtn.setShine(true);
        playBtn.setOnClickListener(v -> launch(GameActivity.Mode.CAMPAIGN));
        column.addView(playBtn, matchWrap(10));

        dailyCard = new ModeCard(this, "calendar", "DAILY", PixelTheme.MINT);
        dailyCard.setOnClickListener(v -> onDaily());
        timedCard = new ModeCard(this, "clock", "TIMED", PixelTheme.SKY);
        timedCard.setOnClickListener(v -> onTimed());
        duelCard = new ModeCard(this, "swords", "DUEL", PixelTheme.ROSE);
        duelCard.setOnClickListener(v -> onDuel());
        ranksCard = new ModeCard(this, "trophy", "RANKS", 0xFFC9A6FF);
        ranksCard.setOnClickListener(v -> onLeaderboard());

        column.addView(cardRow(dailyCard, timedCard), matchWrap(10));
        column.addView(cardRow(duelCard, ranksCard), matchWrap(10));

        cloudBtn = new PixelButton(this, "SAVE TO CLOUD", PixelTheme.PANEL, PixelTheme.TEXT);
        cloudBtn.setLabelSize(10);
        cloudBtn.setIcon("cloud");
        cloudBtn.setIconSize(18);
        cloudBtn.setOnClickListener(v -> onCloud());
        column.addView(cloudBtn, matchWrap(10));

        contentLayer.addView(scroll, fill());
    }

    private View topBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        coinChip = new StatChip(this, "coin");
        hintChip = new StatChip(this, "bulb");
        bar.addView(coinChip);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hp.leftMargin = dp(8);
        bar.addView(hintChip, hp);
        bar.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));

        PixelButton help = iconButton("book");
        help.setOnClickListener(v -> showHowToPlay());
        bar.addView(help, new LinearLayout.LayoutParams(dp(46), dp(46)));
        PixelButton gear = iconButton("gear");
        gear.setOnClickListener(v -> showSettings());
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(dp(46), dp(46));
        gp.leftMargin = dp(8);
        bar.addView(gear, gp);
        return bar;
    }

    private PixelButton iconButton(String icon) {
        PixelButton b = new PixelButton(this, "", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
        b.setIcon(icon);
        b.setIconSize(24);
        return b;
    }

    private View cardRow(View a, View b) {
        LinearLayout row = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(a, lp);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        rp.leftMargin = dp(10);
        row.addView(b, rp);
        return row;
    }

    /** Streak summary on the left, the shop on the right. */
    private View streakAndShop() {
        LinearLayout row = new LinearLayout(this);

        LinearLayout streak = new LinearLayout(this);
        streak.setGravity(Gravity.CENTER_VERTICAL);
        streak.setBackground(new PanelDrawable(0xE03A2E6E, Math.max(2f, Math.round(PixelTheme.dp(this, 1.5f)))));
        streak.setPadding(dp(10), dp(8), dp(10), dp(10));
        streak.setClickable(true);
        streak.setSoundEffectsEnabled(false);
        streak.setOnClickListener(v -> {
            Sfx.play(Sfx.Id.UI_TAP);
            Haptics.click();
            showStreakDetails();
        });
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(30), dp(30));
        ip.rightMargin = dp(8);
        streak.addView(com.kun.pixelwords.ui.PixelIcons.view(this, "flame"), ip);
        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        streakText = PixelTheme.display(this, "", 10, PixelTheme.GOLD);
        streakSub = PixelTheme.body(this, "", 15, PixelTheme.TEXT_DIM);
        streakSub.setPadding(0, dp(4), 0, 0);
        text.addView(streakText);
        text.addView(streakSub);
        streak.addView(text);
        row.addView(streak, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.45f));

        PixelButton shop = new PixelButton(this, "SHOP", 0xFFFF9A3C, PixelTheme.INK);
        shop.setLabelSize(12);
        shop.setIcon("bag");
        shop.setIconSize(24);
        shop.setOnClickListener(v -> go(new Intent(this, ShopActivity.class)));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        sp.leftMargin = dp(10);
        row.addView(shop, sp);
        return row;
    }

    private LinearLayout.LayoutParams matchWrap(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(top);
        return p;
    }

    /** Everything rises into place, one row after another. */
    private void playIntro() {
        float rise = dp(36);
        for (int i = 0; i < column.getChildCount(); i++) {
            View v = column.getChildAt(i);
            v.setAlpha(0f);
            v.setTranslationY(rise);
            v.animate().alpha(1f).translationY(0f).setStartDelay(100 + i * 65L).setDuration(300)
                    .setInterpolator(new DecelerateInterpolator(1.5f)).start();
        }
    }

    private void refresh() {
        coinChip.setValue(progress.coins);
        hintChip.setValue(progress.hintTokens);
        cat.dress(progress);

        int chapter = (progress.level - 1) / LEVELS_PER_CHAPTER;
        int inChapter = (progress.level - 1) % LEVELS_PER_CHAPTER + 1;
        playBtn.setSubtitle(CHAPTERS[chapter % CHAPTERS.length] + " - LEVEL " + progress.level
                + "  (" + inChapter + "/" + LEVELS_PER_CHAPTER + ")");

        long today = Days.today();
        int streak = StreakLogic.displayStreak(progress.streak, today);
        boolean doneToday = progress.streak.lastDay == today;
        dailyCard.setStatus(doneToday ? "Solved!" : "New puzzle");
        dailyCard.setBadge(doneToday ? "" : "NEW");
        dailyCard.setFill(doneToday ? 0xFF4FB488 : PixelTheme.MINT);
        timedCard.setStatus(progress.bestTimed > 0 ? "Best " + progress.bestTimed : "2:00 sprint");
        duelCard.setStatus(progress.duelsPlayed > 0 ? wins(progress.duelWins) : "Beat the bot");
        ranksCard.setStatus("Top players");

        streakText.setText(streak == 0 ? "NO STREAK" : days(streak).toUpperCase(Locale.ROOT));
        streakSub.setText(doneToday ? "Back tomorrow!" : streak > 0 ? "Play today!" : "Solve a daily");

        if (PlayGamesService.isConfigured(this)) {
            PlayGamesService.isSignedIn(this, ok -> cloudBtn.setVisibility(ok ? View.GONE : View.VISIBLE));
        }
    }

    static String wins(int n) {
        return n + (n == 1 ? " win" : " wins");
    }

    private static String days(int n) {
        return n + (n == 1 ? " day" : " days");
    }

    private void syncWithCloud() {
        if (!PlayGamesService.isConfigured(this)) return;
        PlayGamesService.loadProgress(this, remoteText -> {
            if (remoteText == null) return;
            Progress merged = Progress.merge(store.load(), Progress.parse(remoteText));
            store.save(merged);
            progress = merged;
            refresh();
        });
    }

    private void launch(GameActivity.Mode mode) {
        Intent i = new Intent(this, GameActivity.class);
        i.putExtra(GameActivity.EXTRA_MODE, mode.name());
        go(i);
    }

    // ------------------------------------------------------------------ modals

    private void onDaily() {
        long today = Days.today();
        if (progress.streak.lastDay != today) {
            launch(GameActivity.Mode.DAILY);
            return;
        }
        new PixelModal(this).title("DAILY DONE").icon("calendar").accent(PixelTheme.MINT)
                .body("You already solved today's puzzle. A new one arrives at midnight.")
                .stat("flame", "Current streak", days(StreakLogic.displayStreak(progress.streak, today)), PixelTheme.ROSE)
                .stat("freeze", "Streak freezes", progress.streak.freezes + " / " + StreakLogic.MAX_FREEZES, PixelTheme.SKY)
                .note("You can replay it for fun, but it won't pay coins or count towards your streak again.")
                .primary("REPLAY", "play", () -> launch(GameActivity.Mode.DAILY))
                .secondary("CLOSE", null)
                .show();
    }

    private void onTimed() {
        new PixelModal(this).title("TIMED RUN").icon("clock").accent(PixelTheme.SKY)
                .body("Two minutes on the clock. Find as many words as you can on a fresh board.")
                .stat("clock", "Time limit", "2:00", PixelTheme.SKY)
                .stat("trophy", "Your best", progress.bestTimed > 0 ? String.valueOf(progress.bestTimed) : "-", PixelTheme.GOLD)
                .stat("sparkle", "Clear the board", "+2 / SEC LEFT", PixelTheme.MINT)
                .primary("START", "play", () -> launch(GameActivity.Mode.TIMED))
                .secondary("NOT NOW", null)
                .show();
    }

    private void onDuel() {
        new PixelModal(this).title("WORD DUEL").icon("swords").accent(PixelTheme.ROSE)
                .body("You and the bot race on the same board for 90 seconds. Highest score when time runs out wins.")
                .stat("clock", "Time limit", "1:30", PixelTheme.SKY)
                .stat("coin", "Win reward", "+20", PixelTheme.GOLD)
                .stat("trophy", "Your record", wins(progress.duelWins).toUpperCase(Locale.ROOT) + " / " + progress.duelsPlayed, PixelTheme.TEXT)
                .note("Online opponents aren't available yet, so this is practice against the bot.")
                .primary("FIGHT!", "swords", () -> launch(GameActivity.Mode.DUEL))
                .secondary("NOT NOW", null)
                .show();
    }

    private void onLeaderboard() {
        if (PlayGamesService.isConfigured(this) && PlayGamesService.hasLeaderboards(this)) {
            PlayGamesService.showLeaderboard(this, getString(R.string.leaderboard_daily));
            return;
        }
        new PixelModal(this).title("RANKS").icon("trophy").accent(0xFFC9A6FF)
                .body("Global leaderboards need Google Play Games, which isn't connected in this build yet.")
                .heading("YOUR BEST SO FAR")
                .stat("clock", "Timed score", progress.bestTimed > 0 ? String.valueOf(progress.bestTimed) : "-", PixelTheme.GOLD)
                .stat("swords", "Duels won", String.valueOf(progress.duelWins), PixelTheme.ROSE)
                .stat("flame", "Best streak", days(progress.streak.best), PixelTheme.ROSE)
                .stat("book", "Words found", String.valueOf(progress.totalWordsFound), PixelTheme.MINT)
                .primary("GOT IT", null)
                .show();
    }

    private void onCloud() {
        if (!PlayGamesService.isConfigured(this)) {
            new PixelModal(this).title("CLOUD SAVE").icon("cloud").accent(PixelTheme.SKY)
                    .body("Your progress is safe on this phone. Backing it up to Google Play Games isn't connected in this build yet.")
                    .stat("star", "Level reached", String.valueOf(progress.level), PixelTheme.GOLD)
                    .stat("coin", "Coins", String.valueOf(progress.coins), PixelTheme.GOLD)
                    .note("Until then, uninstalling the app also removes your progress.")
                    .primary("GOT IT", null)
                    .show();
            return;
        }
        PlayGamesService.signIn(this, ok -> {
            if (ok) {
                cloudBtn.setVisibility(View.GONE);
                syncWithCloud();
                new PixelModal(this).title("SIGNED IN").icon("check").accent(PixelTheme.MINT)
                        .body("You're connected. Progress now backs up to your Google account automatically.")
                        .primary("GREAT", null)
                        .show();
            } else {
                new PixelModal(this).title("SIGN-IN FAILED").icon("cloud").accent(PixelTheme.ROSE)
                        .body("We couldn't reach Google Play Games. Check your connection and that you're signed in to the Play Games app.")
                        .note("Nothing was lost: your progress is still saved on this phone.")
                        .primary("TRY AGAIN", this::onCloud)
                        .secondary("LATER", null)
                        .show();
            }
        });
    }

    private void showStreakDetails() {
        long today = Days.today();
        PixelModal m = new PixelModal(this).title("YOUR STREAK").icon("flame").accent(PixelTheme.ROSE)
                .body("Solve the daily puzzle every day to keep your streak alive.")
                .stat("flame", "Current streak", days(StreakLogic.displayStreak(progress.streak, today)), PixelTheme.ROSE)
                .stat("trophy", "Best streak", days(progress.streak.best), PixelTheme.GOLD)
                .stat("freeze", "Streak freezes", progress.streak.freezes + " / " + StreakLogic.MAX_FREEZES, PixelTheme.SKY)
                .note("You earn a freeze every " + StreakLogic.FREEZE_EVERY_DAYS + " days. A freeze saves your streak if you miss one day.");
        if (progress.streak.lastDay != today) {
            m.primary("PLAY DAILY", "calendar", () -> launch(GameActivity.Mode.DAILY));
        }
        m.secondary("CLOSE", null).show();
    }

    private void showSettings() {
        PixelModal m = new PixelModal(this).title("SETTINGS").icon("gear").accent(PixelTheme.TEXT_DIM);
        m.toggle("sound", "SOUND FX", "Clicks, chimes and fanfares", Settings.sound(), Settings::setSound);
        m.toggle("buzz", "HAPTICS", "Vibration on every action", Settings.haptics(), on -> {
            Settings.setHaptics(on);
            if (on) Haptics.success();
        });
        m.button("HOW TO PLAY", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT, "book", Sfx.Id.UI_TAP, this::showHowToPlay);
        m.button("CREDITS", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT, "heart", Sfx.Id.UI_TAP, this::showCredits);
        m.show();
    }

    private void showHowToPlay() {
        new PixelModal(this).title("HOW TO PLAY").icon("book").accent(PixelTheme.GOLD)
                .heading("SPELL")
                .body("Swipe across the letters on the wheel, then lift your finger. Slide back over a letter to undo it.")
                .heading("SOLVE")
                .body("Words in the crossword fill it in. Find them all to clear the level.")
                .heading("BONUS")
                .body("A few other words are hidden bonuses that pay +" + Scoring.COINS_PER_BONUS_WORD + " coins each.")
                .heading("HINTS")
                .body("A hint reveals one letter. Spend a token, or " + Scoring.HINT_COST_COINS + " coins once you run out. You'll always be asked first.")
                .heading("SHOP")
                .body("Spend your coins on accessories for your cat.")
                .primary("LET'S GO", null)
                .show();
    }

    private void showCredits() {
        new PixelModal(this).title("CREDITS").icon("heart").accent(PixelTheme.ROSE)
                .heading("MADE FOR AYUSHI")
                .body("Thank you to Ayushi, for inspiring me to create this game.")
                .stat(null, "Version", version(), PixelTheme.TEXT)
                .heading("EFFECTS")
                .note("PixelFX by patch-gg (MIT), with parts adapted from Hugo Duprez's MIT effects.")
                .heading("FONTS")
                .note("Press Start 2P and VT323, SIL Open Font License 1.1.")
                .heading("WORDS")
                .note("ENABLE word list (public domain); google-10000-english; LDNOOBW word filter (CC BY 4.0).")
                .primary("CLOSE", null)
                .show();
    }

    private String version() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return "?";
        }
    }
}
