package com.kun.pixelwords;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.kun.pixelwords.audio.Haptics;
import com.kun.pixelwords.audio.Music;
import com.kun.pixelwords.audio.Sfx;
import com.kun.pixelwords.data.Days;
import com.kun.pixelwords.data.ProgressStore;
import com.kun.pixelwords.data.Settings;
import com.kun.pixelwords.game.Achievement;
import com.kun.pixelwords.game.Album;
import com.kun.pixelwords.game.CatMood;
import com.kun.pixelwords.game.DailySpin;
import com.kun.pixelwords.game.Expedition;
import com.kun.pixelwords.game.FishHunt;
import com.kun.pixelwords.game.MegaBoard;
import com.kun.pixelwords.game.PiggyJar;
import com.kun.pixelwords.game.Progress;
import com.kun.pixelwords.game.Quest;
import com.kun.pixelwords.game.Scoring;
import com.kun.pixelwords.game.Season;
import com.kun.pixelwords.game.StreakLogic;
import com.kun.pixelwords.game.TileMods;
import com.kun.pixelwords.services.AdsService;
import com.kun.pixelwords.services.PlayGamesService;
import com.kun.pixelwords.services.Reminders;
import com.kun.pixelwords.ui.AmbientView;
import com.kun.pixelwords.ui.CatView;
import com.kun.pixelwords.ui.PanelDrawable;
import com.kun.pixelwords.ui.PixelBar;
import com.kun.pixelwords.ui.PixelButton;
import com.kun.pixelwords.ui.PixelModal;
import com.kun.pixelwords.ui.PixelTheme;
import com.kun.pixelwords.ui.StatChip;

import java.util.Locale;
import java.util.Random;

/** Main menu: your cat, the streak and shop, the big PLAY button, and a card for each game mode. */
public class MainActivity extends BaseActivity {

    private static final int[] REMINDER_HOURS = {9, 13, 19, 21};
    private static final int REQUEST_NOTIFICATIONS = 1;
    private static final long CAT_SPEAKS_AFTER_MS = 900;
    private static final long CAT_BUBBLE_MS = 3500;

    private ProgressStore store;
    private Progress progress;
    /** Players on these first levels skip the list of modes. */
    private static final int NEW_PLAYER_LEVELS = 3;

    /** The cat's size on the menu. It shrinks to fit on a short screen. */
    private static final int CAT_DP = 240;

    private PixelButton playBtn;
    private PixelButton spinBtn, tripsBtn, passBtn, albumBtn, jarBtn;
    private StatChip coinChip, hintChip;
    private CatView cat;
    private TextView streakText, streakSub, claimBadge, catBubble;
    private int catGift;
    private final Random catRandom = new Random();
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
        AdsService.get(this).gatherConsent(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        progress = store.load();
        boolean newWeek = Quest.rollOver(progress, Days.today());
        boolean newSeason = Season.rollOver(progress, Days.today());
        if (newWeek || newSeason) store.save(progress);
        refresh();
        if (!introduced) {
            introduced = true;
            playIntro();
        }
        offerReminders();
        updateCat();
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
        cat = new CatView(this, CAT_DP);
        cat.enableNaps();
        cat.setPetListener(this::onCatPetted);
        catBubble = PixelTheme.body(this, "", PixelTheme.SIZE_BODY, PixelTheme.INK);
        catBubble.setBackground(new PanelDrawable(PixelTheme.CREAM, Math.max(2f, dp(1))));
        catBubble.setPadding(dp(10), dp(6), dp(10), dp(8));
        catBubble.setGravity(Gravity.CENTER);
        catBubble.setVisibility(View.INVISIBLE);
        FrameLayout catBox = new FrameLayout(this);
        catBox.addView(cat, fill());
        catBox.addView(catBubble, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        catBox.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> sitBubbleOverCat(v.getHeight()));
        column.addView(catBox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(136), 1f));

        column.addView(streakAndShop(), matchWrap(8));

        playBtn = new PixelButton(this, "PLAY", PixelTheme.GOLD, PixelTheme.INK);
        playBtn.setLabelSize(PixelTheme.SIZE_HERO);
        playBtn.setIcon("play");
        playBtn.setIconSize(30);
        playBtn.setShine(true);
        playBtn.setOnClickListener(v -> onPlay());
        column.addView(playBtn, matchWrap(10));

        column.addView(extrasRow(), matchWrap(10));

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

        PixelButton trophy = iconButton("trophy");
        trophy.setOnClickListener(v -> go(new Intent(this, AchievementsActivity.class)));
        FrameLayout trophyBox = new FrameLayout(this);
        trophyBox.addView(trophy, fill());
        claimBadge = PixelTheme.display(this, "", PixelTheme.SIZE_CAPTION, PixelTheme.INK);
        claimBadge.setBackground(new PanelDrawable(PixelTheme.ROSE, Math.max(2f, dp(1))));
        claimBadge.setPadding(dp(4), dp(3), dp(4), dp(3));
        trophyBox.addView(claimBadge, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END));
        bar.addView(trophyBox, new LinearLayout.LayoutParams(dp(46), dp(46)));
        PixelButton help = iconButton("book");
        help.setOnClickListener(v -> showHowToPlay());
        LinearLayout.LayoutParams helpLp = new LinearLayout.LayoutParams(dp(46), dp(46));
        helpLp.leftMargin = dp(8);
        bar.addView(help, helpLp);
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
        streakText = PixelTheme.display(this, "", PixelTheme.SIZE_SMALL, PixelTheme.GOLD);
        streakSub = PixelTheme.body(this, "", PixelTheme.SIZE_BODY, PixelTheme.TEXT_DIM);
        streakSub.setPadding(0, dp(4), 0, 0);
        text.addView(streakText);
        text.addView(streakSub);
        streak.addView(text);
        row.addView(streak, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.6f));

        row.addView(sideButton("SHOP", "bag", 0xFFFF9A3C, ShopActivity.class), sideParams());
        row.addView(sideButton("HOME", "home", PixelTheme.SKY, HomeActivity.class), sideParams());
        return row;
    }

    /** The cat sits at the bottom of its box, so the bubble is moved down to just over its head. */
    private void sitBubbleOverCat(int boxHeight) {
        int catHeight = Math.min(boxHeight, dp(CAT_DP));
        catBubble.setTranslationY(Math.max(0, boxHeight - catHeight - catBubble.getHeight()));
    }

    /** A new player goes straight to the Journey, so the first thing they see is the first level. */
    private boolean isNewPlayer() {
        return progress.level <= NEW_PLAYER_LEVELS;
    }

    private void onPlay() {
        go(new Intent(this, isNewPlayer() ? JourneyActivity.class : PlayActivity.class));
    }

    /** The daily extras. A badge on one means something is waiting there. */
    private View extrasRow() {
        LinearLayout row = new LinearLayout(this);
        spinBtn = extraButton("wheel", 0xFFC9A6FF);
        spinBtn.setOnClickListener(v -> go(new Intent(this, SpinActivity.class)));
        tripsBtn = extraButton("paw", PixelTheme.MINT);
        tripsBtn.setOnClickListener(v -> go(new Intent(this, ExpeditionActivity.class)));
        passBtn = extraButton("ticket", PixelTheme.ROSE);
        passBtn.setOnClickListener(v -> go(new Intent(this, SeasonActivity.class)));
        albumBtn = extraButton("sticker", PixelTheme.SKY);
        albumBtn.setOnClickListener(v -> go(new Intent(this, AlbumActivity.class)));
        jarBtn = extraButton("jar", PixelTheme.GOLD);
        jarBtn.setOnClickListener(v -> showJar());
        PixelButton[] all = {spinBtn, tripsBtn, passBtn, albumBtn, jarBtn};
        String[] names = {"SPIN", "TRIPS", "PASS", "ALBUM", "JAR"};
        for (int i = 0; i < all.length; i++) {
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.addView(all[i], matchWrap(0));
            android.widget.TextView caption = PixelTheme.display(this, names[i], PixelTheme.SIZE_CAPTION, PixelTheme.TEXT);
            caption.setGravity(Gravity.CENTER);
            cell.addView(caption, matchWrap(4));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.leftMargin = dp(6);
            row.addView(cell, lp);
        }
        return row;
    }

    private PixelButton extraButton(String icon, int fill) {
        PixelButton b = new PixelButton(this, "", fill, PixelTheme.INK);
        b.setIcon(icon);
        b.setIconSize(24);
        return b;
    }

    private void showJar() {
        long today = Days.today();
        PixelBar bar = new PixelBar(this);
        bar.setSegments(20);
        bar.setColor(PixelTheme.GOLD);
        bar.setValue(progress.jarWords / (float) PiggyJar.CAPACITY);
        FrameLayout barBox = new FrameLayout(this); // the modal stretches a row's width but not its height
        barBox.addView(bar, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(18)));
        PixelModal m = new PixelModal(this).title("PIGGY JAR").icon("jar").accent(PixelTheme.GOLD)
                .body("Every bonus word you find drops into the jar. Fill it to open it for coins.")
                .custom(barBox)
                .stat("sparkle", "Words", progress.jarWords + " / " + PiggyJar.CAPACITY, PixelTheme.GOLD)
                .stat("coin", "A full jar pays", String.valueOf(PiggyJar.COINS), PixelTheme.GOLD);
        if (PiggyJar.canOpen(progress, today)) {
            m.primary("OPEN THE JAR", "coin", this::openJar).secondary("LATER", null);
        } else {
            if (PiggyJar.isFull(progress)) m.note("That's today's jars opened. It stays full until tomorrow.");
            m.primary("OK", null);
        }
        m.show();
    }

    private void openJar() {
        if (!PiggyJar.open(progress, Days.today())) return;
        store.save(progress);
        PlayGamesService.saveProgress(this, progress.serialize());
        Sfx.play(Sfx.Id.BUY);
        Haptics.celebrate();
        fx.play("ui_confetti", fx.getWidth() / 2f, fx.getHeight() * 0.3f, PixelTheme.dp(this, 2.4f), null);
        refresh();
    }

    private PixelButton sideButton(String label, String icon, int fill, Class<?> screen) {
        PixelButton b = new PixelButton(this, label, fill, PixelTheme.INK);
        b.setLabelSize(PixelTheme.SIZE_CAPTION);
        b.setIcon(icon);
        b.setIconSize(20);
        b.setOnClickListener(v -> go(new Intent(this, screen)));
        return b;
    }

    private LinearLayout.LayoutParams sideParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p.leftMargin = dp(8);
        return p;
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
        int ready = Achievement.claimable(progress).size() + Quest.claimable(progress).size();
        claimBadge.setText(String.valueOf(ready));
        claimBadge.setVisibility(ready > 0 ? View.VISIBLE : View.GONE);

        playBtn.setSubtitle(isNewPlayer() ? "Start your journey" : "Level " + progress.level + " and every game mode");

        long today = Days.today();
        int streak = StreakLogic.displayStreak(progress.streak, today);
        boolean doneToday = progress.streak.lastDay == today;
        boolean megaReady = MegaBoard.isOpen(today) && !MegaBoard.isSolved(progress, today);
        playBtn.setBadge(!isNewPlayer() && (!doneToday || megaReady) ? "NEW" : "");

        long now = System.currentTimeMillis();
        spinBtn.setBadge(DailySpin.canSpin(progress, today) ? "!" : "");
        tripsBtn.setBadge(Expedition.isBack(progress, now) ? "!" : "");
        int passReady = Season.claimableCount(progress) + FishHunt.claimableCount(progress);
        passBtn.setBadge(passReady > 0 ? String.valueOf(passReady) : "");
        int albumReady = Album.claimableCount(progress);
        albumBtn.setBadge(albumReady > 0 ? String.valueOf(albumReady) : "");
        jarBtn.setBadge(PiggyJar.canOpen(progress, today) ? "!" : "");

        streakText.setText(streak == 0 ? "NO STREAK" : days(streak).toUpperCase(Locale.ROOT));
        streakSub.setText(doneToday ? "All done!" : streak > 0 ? "Play today!" : "Play daily");
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

    // ------------------------------------------------------------------ modals

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

    /** One gift at a time: an uncollected coin stays until it's tapped, however often the menu reopens. */
    private void updateCat() {
        if (catGift == 0) {
            catGift = CatMood.rollGift(progress, Days.today(), catRandom);
            cat.setGift(catGift > 0);
        }
        contentLayer.postDelayed(() -> catSays(CatMood.line(progress, Days.today(), catRandom)), CAT_SPEAKS_AFTER_MS);
    }

    private void onCatPetted() {
        if (catGift == 0) {
            if (catRandom.nextInt(3) == 0) catSays(CatMood.line(progress, Days.today(), catRandom));
            return;
        }
        CatMood.collectGift(progress, Days.today(), catGift);
        store.save(progress);
        coinChip.animateTo(progress.coins);
        Sfx.play(Sfx.Id.COIN);
        Haptics.success();
        catSays("+" + catGift + " coins! I found them for you.");
        catGift = 0;
        cat.setGift(false);
    }

    private void catSays(String line) {
        catBubble.setText(line);
        catBubble.setVisibility(View.VISIBLE);
        catBubble.animate().cancel();
        catBubble.setAlpha(1f);
        catBubble.animate().alpha(0f).setStartDelay(CAT_BUBBLE_MS).setDuration(400).start();
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
            m.primary("PLAY DAILY", "calendar", () -> go(new Intent(this, GameActivity.class)
                    .putExtra(GameActivity.EXTRA_MODE, GameActivity.Mode.DAILY.name())));
        }
        m.secondary("CLOSE", null).show();
    }

    private void showSettings() {
        PixelModal m = new PixelModal(this).title("SETTINGS").icon("gear").accent(PixelTheme.TEXT_DIM);
        m.toggle("music", "MUSIC", "Gentle tunes while you play", Settings.music(), on -> {
            Settings.setMusic(on);
            Music.refresh();
        });
        m.toggle("sound", "SOUND FX", "Clicks, chimes and fanfares", Settings.sound(), Settings::setSound);
        m.toggle("buzz", "HAPTICS", "Vibration on every action", Settings.haptics(), on -> {
            Settings.setHaptics(on);
            if (on) Haptics.success();
        });
        m.toggle("calendar", "DAILY REMINDER", "A nudge at " + Reminders.hourLabel(Settings.reminderHour()),
                Settings.reminders(), on -> {
                    if (on) enableReminders();
                    else setReminders(false);
                });
        m.button("REMINDER TIME", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT, "clock", Sfx.Id.UI_TAP, this::showReminderTimes);
        m.button("ACCESSIBILITY", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT, "heart", Sfx.Id.UI_TAP, this::showAccessibility);
        m.button("CLOUD SAVE", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT, "cloud", Sfx.Id.UI_TAP, this::onCloud);
        m.button("HOW TO PLAY", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT, "book", Sfx.Id.UI_TAP, this::showHowToPlay);
        m.button("CREDITS", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT, "heart", Sfx.Id.UI_TAP, this::showCredits);
        AdsService ads = AdsService.get(this);
        if (ads.hasPrivacyOptions()) {
            m.button("AD PRIVACY", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT, "gear", Sfx.Id.UI_TAP, () -> ads.showPrivacyOptions(this));
        }
        m.show();
    }

    private void showAccessibility() {
        boolean[] rebuild = {false}; // the menu is already drawn in the old colours and text size
        new PixelModal(this).title("ACCESSIBILITY").icon("heart").accent(PixelTheme.SKY)
                .toggle("sparkle", "COLOURBLIND", "Blue and orange for right and wrong", Settings.colourblind(), on -> {
                    Settings.setColourblind(on);
                    rebuild[0] = true;
                })
                .toggle("book", "LARGER TEXT", "Bigger words and labels", Settings.largeText(), on -> {
                    Settings.setLargeText(on);
                    rebuild[0] = true;
                })
                .toggle("bulb", "LEFT-HANDED", "Moves the hint button to the left", Settings.leftHanded(), Settings::setLeftHanded)
                .primary("DONE", null)
                .onDismiss(() -> {
                    if (!rebuild[0]) return;
                    PixelTheme.applySettings();
                    contentLayer.post(this::recreate);
                })
                .show();
    }

    private void showReminderTimes() {
        PixelModal m = new PixelModal(this).title("REMINDER TIME").icon("clock").accent(PixelTheme.SKY)
                .body("When should your cat nudge you? There's no nudge if you've already solved the daily.");
        for (int hour : REMINDER_HOURS) {
            boolean current = hour == Settings.reminderHour();
            m.button(Reminders.hourLabel(hour), current ? PixelTheme.GOLD : PixelTheme.PANEL_LIGHT,
                    current ? PixelTheme.INK : PixelTheme.TEXT, null, Sfx.Id.UI_TAP, () -> {
                        Settings.setReminderHour(hour);
                        Reminders.schedule(this);
                        showSettings();
                    });
        }
        m.show();
    }

    /** One time only, after the first daily, when the player already knows what's being offered. */
    private void offerReminders() {
        if (Settings.reminderOffered() || Settings.reminders() || progress.dailiesCompleted < 1 || modalShowing()) return;
        Settings.setReminderOffered();
        new PixelModal(this).title("DAILY REMINDER").icon("calendar").accent(PixelTheme.MINT)
                .body("Want a nudge at " + Reminders.hourLabel(Settings.reminderHour())
                        + " so your streak stays alive? You can change this in Settings.")
                .primary("YES PLEASE", "check", this::enableReminders)
                .secondary("NOT NOW", null)
                .show();
    }

    /** Android 13+ needs the player's permission, which arrives in onRequestPermissionsResult. */
    private void enableReminders() {
        if (Reminders.mayNotify(this)) {
            setReminders(true);
        } else {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    private void setReminders(boolean on) {
        Settings.setReminders(on);
        Reminders.schedule(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode != REQUEST_NOTIFICATIONS) return;
        boolean granted = results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED;
        setReminders(granted);
        if (!granted) {
            new PixelModal(this).title("NOTIFICATIONS OFF").icon("calendar").accent(PixelTheme.ROSE)
                    .body("Reminders can't appear until notifications are allowed for Bloop in your phone's settings.")
                    .primary("GOT IT", null)
                    .show();
        }
    }

    private void showHowToPlay() {
        new PixelModal(this).title("HOW TO PLAY").icon("book").accent(PixelTheme.GOLD)
                .heading("SPELL")
                .body("Swipe across the letters on the wheel, then lift your finger. Slide back over a letter to undo it.")
                .heading("SOLVE")
                .body("Words in the crossword fill it in. Find them all to clear the level.")
                .heading("BONUS")
                .body("Any other real word is a bonus word. Each one drops into your jar, and a full jar opens for "
                        + PiggyJar.COINS + " coins.")
                .heading("HINTS")
                .body("A hint reveals one letter. Spend a token, or " + Scoring.HINT_COST_COINS + " coins once you run out. You'll always be asked first.")
                .heading("TWISTS")
                .body("Hard boards have gold letters that pay coins, frozen words that a crossing word must thaw, "
                        + "and orange spark words worth extra if you find them in your first " + TileMods.SPARK_WINDOW + " words.")
                .heading("SHOP")
                .body("Spend your coins on accessories, breeds and rooms for your cat.")
                .heading("EVERY DAY")
                .body("Spin the wheel, send your cat on a trip, collect stickers, and climb the Season Pass.")
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
