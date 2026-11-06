package com.kun.pixelwords;

import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kun.pixelwords.audio.Haptics;
import com.kun.pixelwords.audio.Sfx;
import com.kun.pixelwords.data.Days;
import com.kun.pixelwords.data.GameEngine;
import com.kun.pixelwords.data.ProgressStore;
import com.kun.pixelwords.data.Settings;
import com.kun.pixelwords.game.Achievement;
import com.kun.pixelwords.game.DuelBot;
import com.kun.pixelwords.game.GameSession;
import com.kun.pixelwords.game.Progress;
import com.kun.pixelwords.game.Scoring;
import com.kun.pixelwords.game.ShareCard;
import com.kun.pixelwords.game.StreakLogic;
import com.kun.pixelwords.generator.Level;
import com.kun.pixelwords.generator.LevelGenerator;
import com.kun.pixelwords.services.AdsService;
import com.kun.pixelwords.services.PlayGamesService;
import com.kun.pixelwords.ui.AmbientView;
import com.kun.pixelwords.ui.CatView;
import com.kun.pixelwords.ui.CrosswordView;
import com.kun.pixelwords.ui.PanelDrawable;
import com.kun.pixelwords.ui.PixelBar;
import com.kun.pixelwords.ui.PixelButton;
import com.kun.pixelwords.ui.PixelModal;
import com.kun.pixelwords.ui.PixelTheme;
import com.kun.pixelwords.ui.StarsView;
import com.kun.pixelwords.ui.StatChip;
import com.kun.pixelwords.ui.WheelView;
import com.kun.pixelwords.ui.WordPreview;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/** The puzzle screen for all four modes: campaign, daily, timed and (offline-bot) duel. */
public class GameActivity extends BaseActivity {

    public static final String EXTRA_MODE = "mode";

    public enum Mode { CAMPAIGN, DAILY, TIMED, DUEL }

    private static final int TIMED_SECONDS = 120;
    private static final int DUEL_SECONDS = 90;
    private static final int URGENT_SECONDS = 10;
    private static final int DUEL_WIN_COINS = 20;
    private static final int LAST_COACHED_LEVEL = 2;

    private Mode mode = Mode.CAMPAIGN;
    private ProgressStore store;
    private Progress progress;
    private Level level;
    private GameSession session;
    private boolean finished;

    private AmbientView ambient;
    private CrosswordView grid;
    private WheelView wheel;
    private WordPreview preview;
    private TextView titleText, subText, timerText, duelText, coachText, banner, countdownText;
    private PixelBar timerBar;
    private View timerRow;
    private StatChip coinChip;
    private PixelButton hintBtn;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private CountDownTimer timer;
    private int secondsLeft;
    private int botScore;
    private boolean roundLive;      // the clock may run: false during the countdown and after the round
    private boolean adShowing;
    private boolean resumed;
    private boolean startPending;   // the countdown ended while the app was in the background
    private int combo;
    private int coachLevel;         // the opening level being taught this round, or 0
    private List<DuelBot.Event> botScript;
    private boolean firstDailyToday;
    private final Random rnd = new Random();

    @Override
    protected View createBackground() {
        ambient = new AmbientView(this);
        return ambient;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String m = getIntent().getStringExtra(EXTRA_MODE);
        if (m != null) mode = Mode.valueOf(m);
        store = new ProgressStore(this);
        progress = store.load();
        buildUi();
        fx.preload("pickups_collect-burst", "ui_confetti", "ui_quest-complete", "pickups_coin-spin", "ui_achievement");
        AdsService.get(this); // start loading a rewarded ad early
        GameEngine.get().whenReady(this::startRound);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        if (adShowing) {
            adShowing = false;
            resumeClock();
        }
        if (startPending) {
            startPending = false;
            goLive();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = false;
        // Leaving the app must never burn the clock.
        if (timer != null && !adShowing) {
            stopClock();
            showPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopClock();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onBack() {
        if (finished) finish();
        else showPause();
    }

    // ------------------------------------------------------------------ round setup

    private void startRound() {
        LevelGenerator gen;
        try {
            gen = GameEngine.get().generator();
        } catch (IllegalStateException e) {
            new PixelModal(this).title("LOAD ERROR").icon("close").accent(PixelTheme.ROSE).cancelable(false)
                    .body("The word list couldn't be loaded, so this puzzle can't start. Closing and reopening the game usually fixes it.")
                    .primary("BACK TO MENU", this::finish)
                    .show();
            return;
        }
        long today = Days.today();
        finished = false;
        roundLive = false;
        combo = 0;
        botScore = 0;
        fx.clear();

        switch (mode) {
            case DAILY:
                level = gen.forDaily(today);
                firstDailyToday = progress.streak.lastDay != today;
                ambient.setTheme(AmbientView.Theme.PETALS);
                titleText.setText("DAILY");
                break;
            case TIMED:
                level = gen.forDuel(rnd.nextLong());
                ambient.setTheme(AmbientView.Theme.SNOW);
                titleText.setText("TIMED");
                break;
            case DUEL:
                long seed = rnd.nextLong();
                level = gen.forDuel(seed);
                botScript = DuelBot.script(level, seed, 0.55, DUEL_SECONDS);
                ambient.setTheme(AmbientView.Theme.LEAVES);
                titleText.setText("DUEL");
                break;
            default:
                level = gen.forProgression(progress.level);
                ambient.setTheme(AmbientView.Theme.values()[((progress.level - 1) / 25) % AmbientView.Theme.values().length]);
                titleText.setText("LEVEL " + progress.level);
                break;
        }

        session = new GameSession(level, level.fingerprint());
        coachLevel = mode == Mode.CAMPAIGN && progress.level <= LAST_COACHED_LEVEL ? progress.level : 0;
        coachText.setVisibility(coachLevel > 0 ? View.VISIBLE : View.GONE);
        grid.setSession(session);
        wheel.setLetters(level.letters);
        preview.clear();
        timerBar.setAlarm(false);
        timerBar.setColor(PixelTheme.MINT);
        timerBar.setValue(1f);
        boolean timed = limit() > 0;
        timerRow.setVisibility(timed ? View.VISIBLE : View.GONE);
        duelText.setVisibility(mode == Mode.DUEL ? View.VISIBLE : View.GONE);
        secondsLeft = limit();
        updateHud();

        if (timed) {
            wheel.setLocked(true);
            runCountdown();
        } else {
            wheel.setLocked(false);
            roundLive = true;
        }
    }

    private int limit() {
        return mode == Mode.TIMED ? TIMED_SECONDS : mode == Mode.DUEL ? DUEL_SECONDS : 0;
    }

    /** 3, 2, 1, GO! before any timed round. */
    private void runCountdown() {
        final String[] steps = {"3", "2", "1", "GO!"};
        long at = 900;
        for (int i = 0; i < steps.length; i++) {
            final int n = i;
            handler.postDelayed(() -> {
                boolean go = n == steps.length - 1;
                countdownText.setText(steps[n]);
                countdownText.setTextColor(go ? PixelTheme.MINT : PixelTheme.GOLD);
                countdownText.setVisibility(View.VISIBLE);
                countdownText.setAlpha(1f);
                countdownText.setScaleX(2.2f);
                countdownText.setScaleY(2.2f);
                countdownText.animate().cancel();
                countdownText.animate().scaleX(1f).scaleY(1f).setDuration(240)
                        .setInterpolator(new OvershootInterpolator(2f)).start();
                Sfx.play(go ? Sfx.Id.COUNT_GO : Sfx.Id.COUNT_BEEP);
                if (go) Haptics.thud();
                else Haptics.click();
            }, at);
            at += 750;
        }
        handler.postDelayed(() -> {
            countdownText.animate().alpha(0f).setDuration(160)
                    .withEndAction(() -> countdownText.setVisibility(View.GONE)).start();
            if (resumed) goLive();
            else startPending = true;
        }, at - 250);
    }

    private void goLive() {
        roundLive = true;
        wheel.setLocked(false);
        resumeClock();
    }

    /** Starts or continues the clock from wherever it stopped. */
    private void resumeClock() {
        if (finished || !roundLive || limit() == 0 || timer != null || adShowing) return;
        timer = new CountDownTimer(secondsLeft * 1000L, 250L) {
            @Override public void onTick(long ms) {
                int s = (int) Math.ceil(ms / 1000.0);
                if (s != secondsLeft) {
                    secondsLeft = s;
                    onSecond();
                }
            }

            @Override public void onFinish() {
                secondsLeft = 0;
                timer = null;
                updateHud();
                endRound(false);
            }
        }.start();
    }

    private void stopClock() {
        if (timer != null) timer.cancel();
        timer = null;
    }

    private void onSecond() {
        updateHud();
        if (secondsLeft == URGENT_SECONDS) {
            Sfx.play(Sfx.Id.CLOCK_URGENT);
            timerBar.setAlarm(true);
            timerBar.setColor(PixelTheme.ROSE);
        } else if (secondsLeft < URGENT_SECONDS && secondsLeft > 0) {
            Sfx.play(Sfx.Id.CLOCK_URGENT, 1f + (URGENT_SECONDS - secondsLeft) * 0.03f);
            Haptics.tick();
        } else if (secondsLeft == 30) {
            timerBar.setColor(PixelTheme.GOLD);
            Sfx.play(Sfx.Id.CLOCK_TICK);
        }
        if (mode == Mode.DUEL && botScript != null) {
            int now = DuelBot.scoreAt(botScript, DUEL_SECONDS - secondsLeft);
            if (now > botScore) {
                botScore = now;
                say("BOT SCORED!", PixelTheme.ROSE);
                Sfx.play(Sfx.Id.WORD_ALREADY, 0.8f);
                updateHud();
            }
        }
    }

    // ------------------------------------------------------------------ input

    private void onWordSubmitted(String word) {
        if (finished || session == null) {
            preview.clear();
            return;
        }
        GameSession.Result r = session.submit(word);
        switch (r) {
            case FOUND_GRID:
                combo++;
                preview.resolve(PixelTheme.MINT, false);
                Level.Placement p = placementOf(word);
                if (p != null) burstAt(p.rowAt(p.word.length() / 2), p.colAt(p.word.length() / 2));
                grid.refresh();
                Sfx.play(Sfx.Id.WORD_FOUND, Math.min(1.5f, 1f + (combo - 1) * 0.08f));
                Haptics.success();
                String praise = praiseFor(word.length());
                say((praise == null ? "" : praise + "  ") + "+" + Scoring.gridWordPoints(word.toLowerCase(Locale.ROOT)), PixelTheme.MINT);
                if (session.isComplete()) endRound(true);
                break;
            case FOUND_BONUS:
                combo++;
                preview.resolve(PixelTheme.GOLD, false);
                say("BONUS WORD  +" + Scoring.COINS_PER_BONUS_WORD, PixelTheme.GOLD);
                Sfx.play(Sfx.Id.WORD_BONUS);
                handler.postDelayed(() -> Sfx.play(Sfx.Id.COIN), 220);
                Haptics.success();
                break;
            case ALREADY_FOUND:
                preview.resolve(PixelTheme.TEXT_DIM, false);
                say("ALREADY FOUND", PixelTheme.TEXT_DIM);
                Sfx.play(Sfx.Id.WORD_ALREADY);
                Haptics.click();
                break;
            case NOT_IN_PUZZLE:
                preview.resolve(PixelTheme.TEXT_DIM, false); // a real word, so it isn't punished as a mistake
                say("NOT IN THIS PUZZLE", PixelTheme.TEXT_DIM);
                Sfx.play(Sfx.Id.WORD_ALREADY);
                Haptics.click();
                break;
            case NOT_A_WORD:
                combo = 0;
                preview.resolve(PixelTheme.ROSE, true);
                say("NOT A WORD", PixelTheme.ROSE);
                Sfx.play(Sfx.Id.WORD_WRONG);
                Haptics.error();
                shakeWheel();
                break;
            default:
                preview.clear(); // too short: ignore silently
                break;
        }
        updateHud();
    }

    /** Praise grows with your run of correct words; a long word earns some right away. */
    private String praiseFor(int length) {
        if (combo >= 6) return "UNSTOPPABLE!";
        if (combo == 5 || length >= 7) return "AMAZING!";
        if (combo == 4 || length == 6) return "AWESOME!";
        if (combo == 3) return "GREAT!";
        if (combo == 2 || length == 5) return "NICE!";
        return null;
    }

    private void onHintPressed() {
        if (finished || session == null || !roundLive) return;
        Sfx.play(Sfx.Id.UI_TAP);
        boolean hasToken = progress.hintTokens > 0;
        if (hasToken || progress.coins >= Scoring.HINT_COST_COINS) confirmHint(hasToken);
        else showOutOfHints();
    }

    /** Nothing is spent until the player agrees, and the clock waits while they decide. */
    private void confirmHint(final boolean useToken) {
        stopClock();
        wheel.setLocked(true);
        PixelModal m = new PixelModal(this).title("USE A HINT?").icon("bulb").accent(PixelTheme.GOLD)
                .body("This reveals one hidden letter on the board.");
        if (useToken) {
            m.stat("bulb", "Cost", "1 TOKEN", PixelTheme.GOLD)
                    .stat("bulb", "Tokens left after", String.valueOf(progress.hintTokens - 1), PixelTheme.TEXT);
        } else {
            m.stat("coin", "Cost", Scoring.HINT_COST_COINS + " COINS", PixelTheme.GOLD)
                    .stat("coin", "Coins left after", String.valueOf(progress.coins - Scoring.HINT_COST_COINS), PixelTheme.TEXT);
        }
        m.stat("trophy", "Score penalty", "-" + Scoring.HINT_SCORE_PENALTY, PixelTheme.ROSE);
        if (mode == Mode.CAMPAIGN) m.note("Clearing a level with no hints earns three stars.");
        m.primary("REVEAL LETTER", "bulb", () -> spendHint(useToken))
                .secondary("CANCEL", null)
                .onDismiss(this::afterModal)
                .show();
    }

    private void spendHint(boolean useToken) {
        if (!revealHint()) return;
        if (useToken) progress.hintTokens--;
        else progress.coins -= Scoring.HINT_COST_COINS;
        store.save(progress);
        updateHud();
    }

    /** Hands the game back to the player once a modal closes, unless that action ended the round. */
    private void afterModal() {
        if (finished) return;
        wheel.setLocked(false);
        resumeClock();
    }

    private void showOutOfHints() {
        stopClock();
        wheel.setLocked(true);
        int needed = Scoring.HINT_COST_COINS - progress.coins;
        new PixelModal(this).title("OUT OF HINTS").icon("bulb").accent(PixelTheme.GOLD)
                .body("You've used all your hint tokens, and don't have enough coins to buy one.")
                .stat("bulb", "Hint tokens", "0", PixelTheme.ROSE)
                .stat("coin", "Your coins", String.valueOf(progress.coins), PixelTheme.GOLD)
                .stat("coin", "Hint price", String.valueOf(Scoring.HINT_COST_COINS), PixelTheme.TEXT)
                .note("You're " + needed + " coins short. Bonus words pay +" + Scoring.COINS_PER_BONUS_WORD
                        + " each, or watch a short video to reveal one letter for free.")
                .primary("WATCH VIDEO", "film", this::watchVideo)
                .secondary("KEEP PLAYING", null)
                .onDismiss(this::afterModal)
                .show();
    }

    private void watchVideo() {
        adShowing = true;
        stopClock();
        AdsService.get(this).showRewarded(this, () -> handler.post(this::revealHint), () -> {
            adShowing = false;
            new PixelModal(this).title("NO VIDEO").icon("film").accent(PixelTheme.ROSE)
                    .body("There isn't a video available right now. This usually clears up in a minute, or check your connection.")
                    .note("You can also earn coins: bonus words +" + Scoring.COINS_PER_BONUS_WORD
                            + ", clearing a level +" + Scoring.COINS_PER_LEVEL
                            + ", the daily puzzle +" + Scoring.COINS_PER_DAILY + ".")
                    .primary("KEEP PLAYING", null)
                    .onDismiss(this::afterModal)
                    .show();
        });
    }

    /** Reveals one letter. Returns false if nothing was left to reveal (so the player is not charged). */
    private boolean revealHint() {
        int[] cell = session.revealHint();
        if (cell == null) return false;
        burstAt(cell[0], cell[1]);
        grid.refresh();
        Sfx.play(Sfx.Id.HINT);
        Haptics.click();
        updateHud();
        if (session.isComplete()) endRound(true);
        return true;
    }

    private void showPause() {
        if (finished || session == null || !roundLive || modalShowing()) return;
        stopClock();
        wheel.setLocked(true);
        boolean timed = limit() > 0;
        PixelModal m = new PixelModal(this).title("PAUSED").icon("pause").accent(PixelTheme.SKY)
                .stat("book", "Words found", session.foundGridWords().size() + " / " + level.placements.size(), PixelTheme.MINT)
                .stat("sparkle", "Bonus words", String.valueOf(session.foundBonusWords().size()), PixelTheme.GOLD);
        if (timed) m.stat("clock", "Time left", clock(secondsLeft), PixelTheme.SKY);
        m.note(timed ? "The clock is stopped. If you quit, this run won't count."
                        : "If you quit, this puzzle restarts next time and coins from bonus words are lost.")
                .primary("RESUME", "play", null)
                .button("QUIT TO MENU", PixelTheme.ROSE, PixelTheme.INK, null, Sfx.Id.UI_BACK, this::quit)
                .onDismiss(this::afterModal)
                .show();
    }

    private void quit() {
        finished = true;
        stopClock();
        finish();
    }

    // ------------------------------------------------------------------ finishing

    private void endRound(boolean solved) {
        if (finished) return;
        finished = true;
        stopClock();
        wheel.setLocked(true);
        timerBar.setAlarm(false);

        // Everything is banked immediately, so leaving during the celebration loses nothing.
        final Outcome o = new Outcome();
        o.solved = solved;
        o.found = session.foundGridWords().size();
        o.bonus = session.foundBonusWords().size();
        o.hints = session.hintsUsed();
        o.score = session.score(secondsLeft);
        o.coins = session.coinsEarned();
        o.levelNumber = progress.level;
        o.streakBefore = StreakLogic.displayStreak(progress.streak, Days.today());
        int freezesBefore = progress.streak.freezes;
        Set<Achievement> hadBefore = Achievement.unlocked(progress);

        if (solved && mode == Mode.CAMPAIGN) {
            o.coins += Scoring.COINS_PER_LEVEL;
            progress.level++;
        } else if (solved && mode == Mode.DAILY && firstDailyToday) {
            o.coins += Scoring.COINS_PER_DAILY;
            progress.streak = StreakLogic.onDailyCompleted(progress.streak, Days.today());
            progress.dailiesCompleted++;
            o.streakGrew = true;
            o.freezeEarned = progress.streak.freezes > freezesBefore;
        }
        if (mode == Mode.TIMED) {
            o.record = o.score > progress.bestTimed && o.score > 0;
            o.previousBest = progress.bestTimed;
            progress.bestTimed = Math.max(progress.bestTimed, o.score);
        }
        if (mode == Mode.DUEL) {
            o.mine = session.gridPoints() + session.bonusPoints();
            o.bot = DuelBot.scoreAt(botScript, DUEL_SECONDS - secondsLeft);
            progress.duelsPlayed++;
            if (o.mine > o.bot) {
                o.coins += DUEL_WIN_COINS;
                progress.duelWins++;
            }
        }
        progress.coins += o.coins;
        progress.totalWordsFound += o.found + o.bonus;
        o.achievements = Achievement.newlyUnlocked(hadBefore, progress);
        store.save(progress);
        PlayGamesService.saveProgress(this, progress.serialize());
        submitScore(o.score);
        coinChip.animateTo(progress.coins);

        if (solved) {
            fx.play("ui_confetti", fx.getWidth() / 2f, fx.getHeight() * 0.3f, PixelTheme.dp(this, 3), null);
            Haptics.celebrate();
        } else {
            Haptics.fail();
        }
        playEndingAudio(o);
        handler.postDelayed(() -> showResults(o), solved ? 1150 : 900);
    }

    /** What happened in the round, gathered for the results modal. */
    private static final class Outcome {
        boolean solved, record, streakGrew, freezeEarned;
        int found, bonus, hints, score, coins, levelNumber, streakBefore, mine, bot, previousBest;
        List<Achievement> achievements = new ArrayList<>();
    }

    private void addAchievements(PixelModal m, Outcome o) {
        if (o.achievements.isEmpty()) return;
        m.heading("ACHIEVEMENT UNLOCKED");
        for (Achievement a : o.achievements) m.stat("trophy", a.title, "+" + a.tier.coins, PixelTheme.GOLD);
        m.note("Claim your coins from the trophy on the menu.");
        handler.postDelayed(() -> {
            fx.play("ui_achievement", fx.getWidth() / 2f, fx.getHeight() * 0.25f, PixelTheme.dp(this, 3), null);
            Sfx.play(Sfx.Id.STREAK_UP);
        }, 700);
    }

    private static int stars(int hints) {
        return hints == 0 ? 3 : hints <= 2 ? 2 : 1;
    }

    private void playEndingAudio(Outcome o) {
        if (mode == Mode.DUEL) {
            if (o.mine > o.bot) {
                Sfx.play(Sfx.Id.DUEL_WIN);
            } else if (o.mine < o.bot) {
                Sfx.play(Sfx.Id.DUEL_LOSE);
            } else {
                Sfx.play(Sfx.Id.TIME_UP);
            }
        } else if (!o.solved) {
            Sfx.play(Sfx.Id.TIME_UP);
        } else if (mode == Mode.DAILY) {
            Sfx.play(Sfx.Id.DAILY_COMPLETE);
        } else {
            Sfx.play(Sfx.Id.LEVEL_COMPLETE);
        }
    }

    private void showResults(Outcome o) {
        switch (mode) {
            case DAILY:
                showDailyResults(o);
                break;
            case TIMED:
                showTimedResults(o);
                break;
            case DUEL:
                showDuelResults(o);
                break;
            default:
                showCampaignResults(o);
                break;
        }
    }

    private void showCampaignResults(Outcome o) {
        int s = stars(o.hints);
        PixelModal m = new PixelModal(this).title("LEVEL " + o.levelNumber + " CLEAR!").icon("star").accent(PixelTheme.GOLD)
                .quiet().cancelable(false)
                .custom(cheeringCat())
                .custom(new StarsView(this, s, 350))
                .countStat("trophy", "Score", "", o.score, PixelTheme.GOLD)
                .stat("book", "Words found", o.found + " / " + level.placements.size(), PixelTheme.MINT)
                .stat("sparkle", "Bonus words", String.valueOf(o.bonus), PixelTheme.GOLD)
                .stat("bulb", "Hints used", String.valueOf(o.hints), o.hints == 0 ? PixelTheme.MINT : PixelTheme.TEXT)
                .countStat("coin", "Coins earned", "+", o.coins, PixelTheme.GOLD);
        addAchievements(m, o);
        if (s < 3) m.note("Clear a level with no hints for three stars.");
        m.primary("NEXT LEVEL", "play", this::startRound)
                .secondary("MENU", this::finish)
                .show();
    }

    private View cheeringCat() {
        CatView cat = new CatView(this, 112);
        cat.dress(progress);
        cat.setCheering(true);
        cat.setClickable(false);
        return cat;
    }

    private void showDailyResults(Outcome o) {
        PixelModal m = new PixelModal(this).title("DAILY COMPLETE!").icon("calendar").accent(PixelTheme.MINT)
                .quiet().cancelable(false)
                .custom(cheeringCat())
                .custom(new StarsView(this, stars(o.hints), 350))
                .countStat("trophy", "Score", "", o.score, PixelTheme.GOLD);
        if (o.streakGrew) {
            int after = progress.streak.streak;
            m.stat("flame", "Streak", o.streakBefore + " > " + after, PixelTheme.ROSE)
                    .countStat("coin", "Coins earned", "+", o.coins, PixelTheme.GOLD);
            if (o.freezeEarned) m.stat("freeze", "Freeze earned", "+1", PixelTheme.SKY);
            m.note("Come back tomorrow to keep your streak alive.");
            handler.postDelayed(() -> {
                Sfx.play(Sfx.Id.STREAK_UP);
                Haptics.success();
            }, 1250);
        } else {
            m.note("You'd already solved today's puzzle, so this replay pays no coins and doesn't change your streak.");
        }
        addAchievements(m, o);
        if (o.streakGrew) m.primary("SHARE", "share", () -> shareDaily(o)).secondary("MENU", this::finish);
        else m.primary("MENU", this::finish);
        m.show();
    }

    /** Back at the menu after sharing: the results modal closes with its button, so nothing is left to return to. */
    private void shareDaily(Outcome o) {
        Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,
                ShareCard.daily(Days.today(), stars(o.hints), progress.streak.streak, o.hints, o.score));
        startActivity(Intent.createChooser(send, "Share your result"));
        finish();
    }

    private void showTimedResults(Outcome o) {
        PixelModal m = new PixelModal(this).title(o.solved ? "BOARD CLEARED!" : "TIME'S UP!").icon("clock").accent(PixelTheme.SKY)
                .quiet().cancelable(false)
                .countStat("trophy", "Score", "", o.score, PixelTheme.GOLD);
        if (o.record) {
            m.stat("sparkle", "New record!", o.previousBest > 0 ? "WAS " + o.previousBest : "FIRST RUN", PixelTheme.MINT);
            handler.postDelayed(() -> {
                Sfx.play(Sfx.Id.STREAK_UP);
            }, 1000);
        } else {
            m.stat("star", "Your best", String.valueOf(progress.bestTimed), PixelTheme.TEXT);
        }
        m.stat("book", "Words found", o.found + " / " + level.placements.size(), PixelTheme.MINT)
                .stat("sparkle", "Bonus words", String.valueOf(o.bonus), PixelTheme.GOLD);
        if (o.coins > 0) m.countStat("coin", "Coins earned", "+", o.coins, PixelTheme.GOLD);
        addAchievements(m, o);
        addMissed(m);
        m.primary("AGAIN", "play", this::startRound).secondary("MENU", this::finish).show();
    }

    private void showDuelResults(Outcome o) {
        boolean won = o.mine > o.bot, draw = o.mine == o.bot;
        PixelModal m = new PixelModal(this).title(won ? "YOU WIN!" : draw ? "IT'S A DRAW" : "BOT WINS")
                .icon("swords").accent(won ? PixelTheme.MINT : draw ? PixelTheme.GOLD : PixelTheme.ROSE)
                .quiet().cancelable(false)
                .countStat("heart", "You", "", o.mine, won ? PixelTheme.MINT : PixelTheme.TEXT)
                .countStat("close", "Bot", "", o.bot, won ? PixelTheme.TEXT : PixelTheme.ROSE)
                .stat("trophy", "Your record", MainActivity.wins(progress.duelWins).toUpperCase(Locale.ROOT) + " / " + progress.duelsPlayed, PixelTheme.TEXT);
        if (won) m.countStat("coin", "Coins earned", "+", o.coins, PixelTheme.GOLD);
        addAchievements(m, o);
        addMissed(m);
        m.primary("REMATCH", "swords", this::startRound).secondary("MENU", this::finish).show();
    }

    private void addMissed(PixelModal m) {
        List<String> missed = new ArrayList<>();
        for (Level.Placement p : level.placements) {
            if (!session.foundGridWords().contains(p.word)) missed.add(p.word.toUpperCase(Locale.ROOT));
        }
        if (!missed.isEmpty()) {
            m.heading("YOU MISSED");
            m.note(android.text.TextUtils.join(", ", missed));
        }
    }

    private void submitScore(int score) {
        if (score <= 0) return;
        switch (mode) {
            case DAILY:
                PlayGamesService.submitScore(this, getString(R.string.leaderboard_daily), score);
                break;
            case TIMED:
                PlayGamesService.submitScore(this, getString(R.string.leaderboard_timed), score);
                break;
            case DUEL:
                PlayGamesService.submitScore(this, getString(R.string.leaderboard_duel), score);
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------------ UI helpers

    private static String clock(int seconds) {
        return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60);
    }

    /** Level 1 shows the swipe with a ghost finger, one word at a time. Level 2 points out the two tools. */
    private void updateCoach() {
        if (session == null) return;
        wheel.setGuide(coachLevel == 1 ? nextWord() : null);
        if (coachLevel == 0) return;
        coachText.setText(coachLevel == 2 ? "STUCK? SHUFFLE OR USE A HINT"
                : session.foundGridWords().isEmpty() ? "SWIPE THE LETTERS TO SPELL A WORD"
                : "NICE! NOW SPELL THE LAST WORD");
    }

    /** The first grid word still to find, or null once the board is solved. */
    private String nextWord() {
        for (Level.Placement p : level.placements) {
            if (!session.foundGridWords().contains(p.word)) return p.word;
        }
        return null;
    }

    private void updateHud() {
        updateCoach();
        int earned = session == null || finished ? 0 : session.coinsEarned();
        coinChip.animateTo(progress.coins + earned);
        if (progress.hintTokens > 0) hintBtn.setBadge("x" + progress.hintTokens);
        else if (progress.coins >= Scoring.HINT_COST_COINS) hintBtn.setBadge(String.valueOf(Scoring.HINT_COST_COINS));
        else hintBtn.setBadge("AD");

        if (session != null) {
            subText.setText(session.foundGridWords().size() + " of " + level.placements.size() + " words");
        }
        if (limit() > 0) {
            timerText.setText(clock(secondsLeft));
            timerText.setTextColor(secondsLeft <= URGENT_SECONDS ? PixelTheme.ROSE : PixelTheme.TEXT);
            timerBar.setValue(secondsLeft / (float) limit());
        }
        if (mode == Mode.DUEL && session != null) {
            duelText.setText("YOU " + (session.gridPoints() + session.bonusPoints()) + "   VS   BOT " + botScore);
        }
    }

    /** A short coloured callout that pops over the board and floats away. */
    private void say(String message, int color) {
        banner.setText(message);
        banner.setTextColor(color);
        banner.setVisibility(View.VISIBLE);
        banner.animate().cancel();
        banner.setAlpha(1f);
        banner.setTranslationY(0f);
        banner.setScaleX(0.7f);
        banner.setScaleY(0.7f);
        banner.animate().scaleX(1f).scaleY(1f).setDuration(150).setInterpolator(new OvershootInterpolator(2.5f))
                .withEndAction(() -> banner.animate().alpha(0f).translationY(-PixelTheme.dp(this, 14))
                        .setStartDelay(650).setDuration(300).start()).start();
    }

    private void shakeWheel() {
        final float d = PixelTheme.dp(this, 8);
        wheel.animate().translationX(d).setDuration(45).withEndAction(() ->
                wheel.animate().translationX(-d).setDuration(70).withEndAction(() ->
                        wheel.animate().translationX(d / 2).setDuration(60).withEndAction(() ->
                                wheel.animate().translationX(0).setDuration(45).start()).start()).start()).start();
    }

    private Level.Placement placementOf(String word) {
        String w = word.toLowerCase(Locale.ROOT);
        for (Level.Placement p : level.placements) if (p.word.equals(w)) return p;
        return null;
    }

    /** Plays a pickup burst on a grid cell. */
    private void burstAt(int row, int col) {
        PointF c = grid.cellCenter(row, col);
        int[] g = new int[2], f = new int[2];
        grid.getLocationOnScreen(g);
        fx.getLocationOnScreen(f);
        fx.play("pickups_collect-burst", g[0] - f[0] + c.x, g[1] - f[1] + c.y, PixelTheme.dp(this, 1.4f), null);
    }

    private int dp(float v) {
        return (int) PixelTheme.dp(this, v);
    }

    private void buildUi() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(12), dp(12), dp(12), dp(12));

        // HUD
        LinearLayout hud = new LinearLayout(this);
        hud.setGravity(Gravity.CENTER_VERTICAL);
        PixelButton pause = new PixelButton(this, "", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
        pause.setIcon("pause");
        pause.setIconSize(24);
        pause.setOnClickListener(v -> showPause());
        hud.addView(pause, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setGravity(Gravity.CENTER_HORIZONTAL);
        titleText = PixelTheme.display(this, "", 13, PixelTheme.GOLD);
        titleText.setGravity(Gravity.CENTER);
        subText = PixelTheme.body(this, "", 17, PixelTheme.TEXT);
        subText.setGravity(Gravity.CENTER);
        subText.setPadding(0, dp(4), 0, 0);
        titles.addView(titleText);
        titles.addView(subText);
        hud.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        coinChip = new StatChip(this, "coin");
        coinChip.setValue(progress.coins);
        hud.addView(coinChip);
        col.addView(hud, wrapWidth(0));

        LinearLayout tr = new LinearLayout(this);
        tr.setGravity(Gravity.CENTER_VERTICAL);
        timerBar = new PixelBar(this);
        timerBar.setSegments(30);
        tr.addView(timerBar, new LinearLayout.LayoutParams(0, dp(18), 1f));
        timerText = PixelTheme.display(this, "", 12, PixelTheme.TEXT);
        timerText.setPadding(dp(10), 0, 0, 0);
        tr.addView(timerText);
        timerRow = tr;
        col.addView(tr, wrapWidth(8));

        duelText = PixelTheme.display(this, "", 10, PixelTheme.TEXT);
        duelText.setGravity(Gravity.CENTER);
        duelText.setBackground(new PanelDrawable(0xE03A2E6E, Math.max(2f, dp(1))));
        duelText.setPadding(dp(8), dp(8), dp(8), dp(8));
        col.addView(duelText, wrapWidth(8));

        coachText = PixelTheme.display(this, "", 10, PixelTheme.GOLD);
        coachText.setGravity(Gravity.CENTER);
        coachText.setLineSpacing(dp(4), 1f);
        coachText.setBackground(new PanelDrawable(0xE03A2E6E, Math.max(2f, dp(1))));
        coachText.setPadding(dp(8), dp(8), dp(8), dp(8));
        coachText.setVisibility(View.GONE);
        col.addView(coachText, wrapWidth(8));

        // Board, with the callout banner floating over its lower edge
        FrameLayout boardBox = new FrameLayout(this);
        grid = new CrosswordView(this);
        grid.setLetterListener(order -> {
            Sfx.play(Sfx.Id.TILE_POP, Math.min(2f, 1f + order * 0.09f), 0.8f);
            Haptics.tick();
        });
        boardBox.addView(grid, fill());
        banner = PixelTheme.display(this, "", 11, PixelTheme.MINT);
        banner.setBackground(new PanelDrawable(0xF0251D4A, Math.max(2f, dp(1))));
        banner.setPadding(dp(14), dp(9), dp(14), dp(10));
        banner.setVisibility(View.INVISIBLE);
        boardBox.addView(banner, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));
        LinearLayout.LayoutParams boardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        boardLp.topMargin = dp(8);
        boardLp.bottomMargin = dp(4);
        col.addView(boardBox, boardLp);

        preview = new WordPreview(this);
        col.addView(preview, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

        // Wheel, with the two action buttons tucked into the corners its circle leaves free
        FrameLayout wheelBox = new FrameLayout(this);
        wheel = new WheelView(this);
        wheel.setListener(new WheelView.Listener() {
            @Override public void onPreview(String word) { preview.setWord(word); }

            @Override public void onLetterAdded(int position) {
                Sfx.letter(position);
                Haptics.tick();
            }

            @Override public void onLetterRemoved() {
                Sfx.play(Sfx.Id.BACKTRACK);
                Haptics.tick();
            }

            @Override public void onSubmit(String word) { onWordSubmitted(word); }
        });
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;
        int wheelSize = Math.min((int) (screenW * 0.8f), (int) (screenH * 0.38f));
        wheelBox.addView(wheel, new FrameLayout.LayoutParams(wheelSize, wheelSize, Gravity.CENTER));

        PixelButton shuffle = new PixelButton(this, "", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
        shuffle.setIcon("shuffle");
        shuffle.setIconSize(30);
        shuffle.setClickSound(Sfx.Id.SHUFFLE);
        shuffle.setOnClickListener(v -> wheel.shuffle(rnd));
        // The hint is the one players reach for under pressure, so it sits by their thumb.
        boolean leftHanded = Settings.leftHanded();
        wheelBox.addView(shuffle, new FrameLayout.LayoutParams(dp(62), dp(62),
                Gravity.BOTTOM | (leftHanded ? Gravity.END : Gravity.START)));

        hintBtn = new PixelButton(this, "", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
        hintBtn.setIcon("bulb");
        hintBtn.setIconSize(30);
        hintBtn.setClickSound(null);
        hintBtn.setOnClickListener(v -> onHintPressed());
        wheelBox.addView(hintBtn, new FrameLayout.LayoutParams(dp(62), dp(62),
                Gravity.BOTTOM | (leftHanded ? Gravity.START : Gravity.END)));

        col.addView(wheelBox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, wheelSize));

        contentLayer.addView(col, fill());

        countdownText = PixelTheme.display(this, "", 56, PixelTheme.GOLD);
        countdownText.setGravity(Gravity.CENTER);
        countdownText.setShadowLayer(0.01f, dp(4), dp(4), PixelTheme.INK);
        countdownText.setVisibility(View.GONE);
        contentLayer.addView(countdownText, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
    }

    private LinearLayout.LayoutParams wrapWidth(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(top);
        return p;
    }
}
