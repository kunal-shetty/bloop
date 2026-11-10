package com.kun.pixelwords;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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
import com.kun.pixelwords.game.CatMood;
import com.kun.pixelwords.game.Comeback;
import com.kun.pixelwords.game.DailySpin;
import com.kun.pixelwords.game.FishHunt;
import com.kun.pixelwords.game.MegaBoard;
import com.kun.pixelwords.game.PiggyJar;
import com.kun.pixelwords.game.Progress;
import com.kun.pixelwords.game.Quest;
import com.kun.pixelwords.game.Scoring;
import com.kun.pixelwords.game.Season;
import com.kun.pixelwords.game.RedeemCode;
import com.kun.pixelwords.game.Reward;
import com.kun.pixelwords.game.StreakLogic;
import com.kun.pixelwords.game.StreakMilestone;
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
    /** The chest takes over four seconds at its own pace, which is too long to watch every time. */
    private static final int MAX_CODE_LENGTH = 24;
    private static final long KEYBOARD_DELAY_MS = 250;
    private static final long CAT_SPEAKS_AFTER_MS = 900;
    private static final long CAT_BUBBLE_MS = 3500;

    private ProgressStore store;
    private Progress progress;
    private AmbientView ambient;
    /** Players on these first levels skip the list of modes. */
    private static final int NEW_PLAYER_LEVELS = 3;

    /** The cat's size on the menu. It shrinks to fit on a short screen. */
    private static final int CAT_DP = 240;

    private PixelButton playBtn, modesBtn;
    private PixelButton homeBtn, spinBtn, passBtn;
    private TextView homeState, spinState, passState;
    private StatChip coinChip, hintChip;
    private CatView cat;
    private TextView streakText, streakSub, claimBadge, catBubble;
    private int catGift;
    private final Random catRandom = new Random();
    private LinearLayout column;
    private boolean introduced;

    @Override
    protected View createBackground() {
        ambient = new AmbientView(this);
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
        fx.preload("pickups_chest-open", "ui_confetti");
    }

    @Override
    protected void onResume() {
        super.onResume();
        progress = store.load();
        ambient.setHour(java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY));
        boolean newWeek = Quest.rollOver(progress, Days.today());
        boolean newSeason = Season.rollOver(progress, Days.today());
        if (newWeek || newSeason) store.save(progress);
        refresh();
        if (!introduced) {
            introduced = true;
            playIntro();
        }
        if (!welcomeBack() && Comeback.see(progress, Days.today())) store.save(progress);
        offerStreakRescue();
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
        // Home, Spin and the Pass sit up top, over the cat's head, where they're always in view.
        column.addView(extrasRow(), matchWrap(10));

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

        column.addView(streakPanel(), matchWrap(8));

        playBtn = new PixelButton(this, "PLAY", PixelTheme.GOLD, PixelTheme.INK);
        playBtn.setLabelSize(PixelTheme.SIZE_HERO);
        playBtn.setIcon("play");
        playBtn.setIconSize(30);
        playBtn.setShine(true);
        playBtn.setOnClickListener(v -> onPlay());
        column.addView(playBtn, matchWrap(10));

        modesBtn = new PixelButton(this, "MODES", 0xFF8E7FD0, PixelTheme.INK);
        modesBtn.setLabelSize(PixelTheme.SIZE_LABEL);
        modesBtn.setIcon("star");
        modesBtn.setIconSize(22);
        modesBtn.setSubtitle("Daily, Tower, Rush, Shop + more");
        modesBtn.setOnClickListener(v -> go(new Intent(this, PlayActivity.class)));
        column.addView(modesBtn, matchWrap(8));

        contentLayer.addView(scroll, fill());
    }

    private View topBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        coinChip = new StatChip(this, "coin");
        coinChip.setSpokenName("Coins");
        hintChip = new StatChip(this, "bulb");
        hintChip.setSpokenName("Hint tokens");
        bar.addView(coinChip);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hp.leftMargin = dp(8);
        bar.addView(hintChip, hp);
        bar.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));

        PixelButton trophy = iconButton("trophy", "Achievements");
        trophy.setOnClickListener(v -> go(new Intent(this, AchievementsActivity.class)));
        FrameLayout trophyBox = new FrameLayout(this);
        trophyBox.addView(trophy, fill());
        claimBadge = PixelTheme.display(this, "", PixelTheme.SIZE_CAPTION, PixelTheme.INK);
        claimBadge.setBackground(new PanelDrawable(PixelTheme.ROSE, Math.max(2f, dp(1))));
        claimBadge.setPadding(dp(4), dp(3), dp(4), dp(3));
        trophyBox.addView(claimBadge, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END));
        bar.addView(trophyBox, new LinearLayout.LayoutParams(dp(46), dp(46)));
        PixelButton gear = iconButton("gear", "Settings");
        gear.setOnClickListener(v -> showSettings());
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(dp(46), dp(46));
        gp.leftMargin = dp(8);
        bar.addView(gear, gp);
        return bar;
    }

    private PixelButton iconButton(String icon, String name) {
        PixelButton b = new PixelButton(this, "", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
        b.setIcon(icon);
        b.setSpokenName(name);
        b.setIconSize(24);
        return b;
    }

    /** The streak, across the width of the screen. Tap it for the details. */
    private View streakPanel() {
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
        row.addView(streak, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
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
        go(new Intent(this, JourneyActivity.class));
    }

    /** The daily extras. A badge on one means something is waiting there. */
    private View extrasRow() {
        LinearLayout row = new LinearLayout(this);
        homeBtn = extraButton("home", PixelTheme.SKY);
        homeBtn.setOnClickListener(v -> go(new Intent(this, HomeActivity.class)));
        spinBtn = extraButton("wheel", 0xFFC9A6FF);
        spinBtn.setOnClickListener(v -> go(new Intent(this, SpinActivity.class)));
        passBtn = extraButton("ticket", PixelTheme.ROSE);
        passBtn.setOnClickListener(v -> go(new Intent(this, SeasonActivity.class)));
        PixelButton[] all = {homeBtn, spinBtn, passBtn};
        String[] names = {"HOME", "SPIN", "PASS"};
        TextView[] states = new TextView[all.length];
        for (int i = 0; i < all.length; i++) {
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            all[i].setSpokenName(names[i]);
            cell.addView(all[i], matchWrap(0));
            android.widget.TextView caption = PixelTheme.display(this, names[i], PixelTheme.SIZE_CAPTION, PixelTheme.TEXT);
            caption.setGravity(Gravity.CENTER);
            cell.addView(caption, matchWrap(4));
            states[i] = PixelTheme.body(this, " ", PixelTheme.SIZE_BODY, PixelTheme.TEXT);
            states[i].setGravity(Gravity.CENTER);
            states[i].setSingleLine(true);
            cell.addView(states[i], matchWrap(1));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.leftMargin = dp(6);
            row.addView(cell, lp);
        }
        homeState = states[0];
        spinState = states[1];
        passState = states[2];
        return row;
    }

    /** One line under an extra says why it's worth a tap. Gold means something is waiting there. */
    private void showState(PixelButton button, TextView state, String text, boolean ready) {
        state.setText(text);
        state.setTextColor(ready ? PixelTheme.GOLD : PixelTheme.TEXT);
        button.setBadge(ready ? "!" : "");
        button.setContentDescription(button.getContentDescription() + ", " + text);
    }

    private void refreshExtras(long today) {
        showState(homeBtn, homeState, "YOUR ROOM", false);
        showState(spinBtn, spinState, DailySpin.canSpin(progress, today) ? "READY" : "DONE", DailySpin.canSpin(progress, today));
        int passReady = Season.claimableCount(progress) + FishHunt.claimableCount(progress);
        showState(passBtn, passState, passReady > 0 ? passReady + " READY" : "TIER " + Season.tiersReached(progress), passReady > 0);
    }

    private PixelButton extraButton(String icon, int fill) {
        PixelButton b = new PixelButton(this, "", fill, PixelTheme.INK);
        b.setIcon(icon);
        b.setIconSize(24);
        return b;
    }

    private LinearLayout.LayoutParams matchWrap(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(top);
        return p;
    }

    /** Everything rises into place, one row after another. */
    private void playIntro() {
        if (PixelTheme.reduceMotion()) return;
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

        playBtn.setSubtitle(isNewPlayer() ? "Start your journey" : "Journey: level " + progress.level);

        long today = Days.today();
        int streak = StreakLogic.displayStreak(progress.streak, today);
        boolean doneToday = progress.streak.lastDay == today;
        boolean megaReady = MegaBoard.isOpen(today) && !MegaBoard.isSolved(progress, today);
        modesBtn.setBadge(!isNewPlayer() && (!doneToday || megaReady) ? "NEW" : "");

        refreshExtras(today);

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

    /** The cat waits by the door for a player who's been away. True if it's showing. */
    private boolean welcomeBack() {
        long today = Days.today();
        int away = Comeback.daysAway(progress, today);
        if (away == 0 || modalShowing()) return false;
        CatView greeter = new CatView(this, 112);
        greeter.dress(progress);
        greeter.setCheering(true);
        greeter.setClickable(false);
        PixelModal m = new PixelModal(this).title("YOU'RE BACK!").icon("heart").accent(PixelTheme.ROSE)
                .quiet().cancelable(false)
                .custom(greeter)
                .body("Your cat waited by the door for " + days(away) + ", and saved you something.");
        for (Reward r : Comeback.gift(away)) m.stat(r.icon(), r.label(), "", PixelTheme.GOLD);
        m.primary("THANKS, CAT!", "heart", this::claimComeback).show();
        Sfx.play(Sfx.Id.MEOW);
        return true;
    }

    private void claimComeback() {
        Comeback.claim(progress, Days.today());
        store.save(progress);
        PlayGamesService.saveProgress(this, progress.serialize());
        Haptics.celebrate();
        earn(cat, coinChip, progress.coins);
        hintChip.animateTo(progress.hintTokens);
    }

    private void onCatPetted() {
        if (catGift == 0) {
            if (catRandom.nextInt(3) == 0) catSays(CatMood.line(progress, Days.today(), catRandom));
            return;
        }
        CatMood.collectGift(progress, Days.today(), catGift);
        store.save(progress);
        earn(cat, coinChip, progress.coins);
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
        go(new Intent(this, StreakActivity.class));
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
        m.button("REDEEM CODE", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT, "ticket", Sfx.Id.UI_TAP, this::showRedeem);
        m.button("CLOUD SAVE", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT, "cloud", Sfx.Id.UI_TAP, this::onCloud);
        m.button("HOW TO PLAY", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT, "book", Sfx.Id.UI_TAP, this::showHowToPlay);
        m.button("GAME GUIDE", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT, "book", Sfx.Id.UI_TAP, this::showGuide);
        if (Links.hasDiscord()) {
            m.button("JOIN OUR DISCORD", PixelTheme.SKY, PixelTheme.INK, "sparkle", Sfx.Id.UI_TAP, () -> LegalActivity.openDiscord(this));
        }
        m.button("CREDITS", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT, "heart", Sfx.Id.UI_TAP, () -> startActivity(LegalActivity.open(this, LegalText.Kind.CREDITS)));
        m.button("PRIVACY POLICY", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT, "book", Sfx.Id.UI_TAP, () -> startActivity(LegalActivity.open(this, LegalText.Kind.PRIVACY)));
        m.button("TERMS OF SERVICE", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT, "book", Sfx.Id.UI_TAP, () -> startActivity(LegalActivity.open(this, LegalText.Kind.TERMS)));
        m.button("REFUND POLICY", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT, "book", Sfx.Id.UI_TAP, () -> startActivity(LegalActivity.open(this, LegalText.Kind.REFUNDS)));
        AdsService ads = AdsService.get(this);
        if (ads.hasPrivacyOptions()) {
            m.button("AD PRIVACY", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT, "gear", Sfx.Id.UI_TAP, () -> ads.showPrivacyOptions(this));
        }
        m.show();
    }

    private void showRedeem() {
        EditText field = new EditText(this);
        field.setTypeface(PixelTheme.DISPLAY);
        field.setTextSize(TypedValue.COMPLEX_UNIT_PX, PixelTheme.displayPx(this, PixelTheme.SIZE_LABEL));
        field.setTextColor(PixelTheme.INK);
        field.setHintTextColor(PixelTheme.shade(PixelTheme.INK, 1.5f));
        field.setHint("CODE");
        field.setGravity(Gravity.CENTER);
        field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_CODE_LENGTH)});
        field.setBackground(new PanelDrawable(PixelTheme.CREAM, Math.max(2f, dp(1))));
        field.setPadding(dp(10), dp(12), dp(10), dp(12));
        field.setContentDescription("Gift code");
        new PixelModal(this).title("REDEEM CODE").icon("ticket").accent(PixelTheme.GOLD)
                .body("Got a gift code? Type it here. Each one works once.")
                .custom(field)
                .primary("REDEEM", "check", () -> {
                    hideKeyboard(field);
                    redeem(field.getText().toString());
                })
                .secondary("CANCEL", () -> hideKeyboard(field))
                .show();
        field.requestFocus();
        field.postDelayed(() -> ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                .showSoftInput(field, InputMethodManager.SHOW_IMPLICIT), KEYBOARD_DELAY_MS);
    }

    private void hideKeyboard(View field) {
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(field.getWindowToken(), 0);
    }

    private void redeem(String typed) {
        RedeemCode.Result result = RedeemCode.redeem(progress, typed, Days.today());
        if (result.status != RedeemCode.Status.OK) {
            new PixelModal(this).title("NO LUCK").icon("close").accent(PixelTheme.ROSE)
                    .body(result.status == RedeemCode.Status.ALREADY_USED ? "You've already used this code."
                            : result.status == RedeemCode.Status.EXPIRED ? "This code has run out."
                            : "That code doesn't look right. Check it and try again.")
                    .primary("TRY AGAIN", this::showRedeem)
                    .secondary("CLOSE", null)
                    .show();
            return;
        }
        store.save(progress);
        PlayGamesService.saveProgress(this, progress.serialize());
        Sfx.play(Sfx.Id.BUY);
        Haptics.celebrate();
        fx.play("ui_confetti", fx.getWidth() / 2f, fx.getHeight() * 0.3f, PixelTheme.dp(this, 2.4f), null);
        earn(coinChip, progress.coins);
        hintChip.animateTo(progress.hintTokens);
        PixelModal m = new PixelModal(this).title("CODE REDEEMED!").icon("ticket").accent(PixelTheme.MINT);
        for (Reward r : result.rewards) m.stat(r.icon(), r.label(), "", PixelTheme.GOLD);
        m.primary("NICE", null).show();
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
                .toggle("cloud", "REDUCE MOTION", "Less shaking, sliding and drifting", Settings.reduceMotion(), on -> {
                    Settings.setReduceMotion(on);
                    rebuild[0] = true;
                })
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

    /** Once a day, when a good streak has just broken for want of a freeze, a video can bring it back. */
    private void offerStreakRescue() {
        long today = Days.today();
        if (!Features.MONETIZATION || modalShowing() || Settings.rescueOfferedDay() == today
                || !StreakLogic.canRescue(progress.streak, today)) return;
        Settings.setRescueOfferedDay(today);
        new PixelModal(this).title("SAVE YOUR STREAK?").icon("flame").accent(PixelTheme.ROSE)
                .body("You missed a day, and your " + progress.streak.streak + "-day streak is about to end. "
                        + "Watch a short video to get a freeze, then solve today's daily to keep it.")
                .primary("WATCH VIDEO", "film", this::rescueStreak)
                .secondary("LET IT GO", null)
                .show();
    }

    private void rescueStreak() {
        AdsService.get(this).showRewarded(this, () -> runOnUiThread(() -> {
            progress.streak = StreakLogic.withFreeze(progress.streak);
            store.save(progress);
            refresh();
            new PixelModal(this).title("STREAK SAVED!").icon("freeze").accent(PixelTheme.SKY)
                    .body("A freeze is ready. Solve today's daily to keep your " + progress.streak.streak + "-day streak.")
                    .primary("GREAT", null)
                    .show();
        }), () -> new PixelModal(this).title("NO VIDEO").icon("film").accent(PixelTheme.ROSE)
                .body("There isn't a video ready right now. Try again in a moment.")
                .primary("OK", null)
                .show());
    }

    /** One time only, after the first daily, when the player already knows what's being offered. */
    private void offerReminders() {
        if (Settings.reminderOffered() || Settings.reminders() || (progress.dailiesCompleted < 1 && progress.level < 4) || modalShowing()) return;
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
                    .body("Reminders can't appear until notifications are allowed for Paws & Puzzles in your phone's settings.")
                    .primary("GOT IT", null)
                    .show();
        }
    }

    /** Every mode and mechanic's how-to-play card, to read again. */
    private void showGuide() {
        PixelModal m = new PixelModal(this).title("GAME GUIDE").icon("book").accent(PixelTheme.GOLD)
                .body("Tap anything to read how it works.");
        for (Explain.Topic topic : Explain.Topic.values()) {
            m.button(topic.title, PixelTheme.PANEL_LIGHT, PixelTheme.TEXT, topic.icon, Sfx.Id.UI_TAP,
                    () -> Explain.card(this, topic, "BACK", this::showGuide).show());
        }
        m.secondary("CLOSE", null).show();
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
                .heading("MEANINGS")
                .body("Tap a word you've found to see what it means.")
                .heading("BOSSES")
                .body("Every chapter ends with a boss. Each word you find hurts it, but it fights back every few words, "
                        + "and whenever you spell something wrong.")
                .heading("SHOP")
                .body("Spend your coins on accessories, breeds and rooms for your cat. You'll find the shop under Modes.")
                .heading("EVERY DAY")
                .body("Spin the wheel, fill the piggy jar, and climb the Season Pass.")
                .primary("LET'S GO", null)
                .show();
    }

    static String versionName(android.content.Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return "?";
        }
    }
}
