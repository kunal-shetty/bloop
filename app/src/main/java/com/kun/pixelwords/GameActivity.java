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
import com.kun.pixelwords.game.Boss;
import com.kun.pixelwords.game.BossFight;
import com.kun.pixelwords.game.DailyArchive;
import com.kun.pixelwords.game.Definitions;
import com.kun.pixelwords.game.DuelBot;
import com.kun.pixelwords.game.FishHunt;
import com.kun.pixelwords.game.GameSession;
import com.kun.pixelwords.game.Journey;
import com.kun.pixelwords.game.MegaBoard;
import com.kun.pixelwords.game.BossRush;
import com.kun.pixelwords.game.NineLives;
import com.kun.pixelwords.game.Run;
import com.kun.pixelwords.game.WordTower;
import com.kun.pixelwords.game.PiggyJar;
import com.kun.pixelwords.game.PowerUps;
import com.kun.pixelwords.game.Progress;
import com.kun.pixelwords.game.Region;
import com.kun.pixelwords.game.Reward;
import com.kun.pixelwords.game.Scoring;
import com.kun.pixelwords.game.Season;
import com.kun.pixelwords.game.ShareCard;
import com.kun.pixelwords.game.StreakLogic;
import com.kun.pixelwords.game.StreakMilestone;
import com.kun.pixelwords.game.TileMods;
import com.kun.pixelwords.generator.Level;
import com.kun.pixelwords.generator.LevelGenerator;
import com.kun.pixelwords.lan.LanMatch;
import com.kun.pixelwords.lan.LanPlayer;
import com.kun.pixelwords.services.AdsService;
import com.kun.pixelwords.services.LanSession;
import com.kun.pixelwords.services.PlayGamesService;
import com.kun.pixelwords.ui.AmbientView;
import com.kun.pixelwords.ui.CatView;
import com.kun.pixelwords.ui.CrosswordView;
import com.kun.pixelwords.ui.PanelDrawable;
import com.kun.pixelwords.ui.PixelBar;
import com.kun.pixelwords.ui.PixelButton;
import com.kun.pixelwords.ui.PixelIcons;
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

/** The puzzle screen for every mode. */
public class GameActivity extends BaseActivity {

    public static final String EXTRA_MODE = "mode";
    /** A cleared campaign level to replay. Left out, the campaign plays the next new level. */
    public static final String EXTRA_REPLAY_LEVEL = "replayLevel";

    /** The past daily to play, in ARCHIVE mode. */
    public static final String EXTRA_DAY = "day";

    public enum Mode { CAMPAIGN, DAILY, TIMED, DUEL, LAN, MEGA, LIVES, ARCHIVE, TOWER, RUSH }

    private static final int TIMED_SECONDS = 120;
    private static final int DUEL_SECONDS = 90;
    private static final int URGENT_SECONDS = 10;
    private static final int DUEL_WIN_COINS = 20;
    private static final int LAST_COACHED_LEVEL = 2;
    /** The most of the wheel's width the cat can take without touching the tiles on the smallest wheel. */
    private static final float WHEEL_CAT_SHARE = 0.24f;
    /** The star iris sheet is mostly a closing star this game doesn't need, so it starts on black. */
    private static final int IRIS_OPEN_FRAME = 38;
    /** The combo sheet has "x5" drawn into it, so it can only play on exactly that combo. */
    private static final int COMBO_SHEET_AT = 5;
    private static final int COMBO_SHOWN_FROM = 2;
    private static final long COUNTDOWN_STEP_MS = 780;
    private static final int CAT_HEARTS_FROM = 3;
    private static final long MEANING_SHOWN_MS = 5000;
    private static final long BOSS_ENTRANCE_MS = 700;
    private static final long BOSS_SHOUT_DELAY_MS = 800;
    private static final float BOSS_FACE_DP = 40;
    private static final long MEANING_TIP_DELAY_MS = 1200;

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
    private TextView titleText, subText, timerText, duelText, coachText, banner, comboText, comboBig;
    private PixelBar timerBar;
    private View timerRow;
    private StatChip coinChip;
    private PixelButton hintBtn, powerBtn;
    private CatView wheelCat;
    private LinearLayout meaningCard, bossRow;
    private BossFight fight;        // the boss on the board, on a chapter's last level
    private PixelBar bossBar;
    private TextView bossName;
    private FrameLayout bossFace;
    private TextView meaningWord, meaningText;
    private final Runnable hideMeaning = this::dismissMeaning;
    private String lastMeaningWord = "";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private CountDownTimer timer;
    private int secondsLeft;
    private int botScore;
    private boolean roundLive;      // the clock may run: false during the countdown and after the round
    private boolean sparkAnnounced;
    private boolean adShowing;
    private boolean resumed;
    private boolean startPending;   // the countdown ended while the app was in the background
    private int combo;
    private int coachLevel;         // the opening level being taught this round, or 0
    private int replayLevel;        // the cleared level being replayed, or 0 for the next new level
    private int playLevel;          // the campaign level on the board
    private long archiveDay;        // the past daily on the board, in ARCHIVE mode
    private LanMatch race;          // the Wi-Fi race being played, in LAN mode
    private int lastRaceScore;
    private List<DuelBot.Event> botScript;
    private boolean firstDailyToday;
    private boolean usedExtraTime;  // this Timed run bought seconds, so it can't set a record
    private Run run;                // the run under way, in LIVES, TOWER and RUSH modes
    private long runSeed;
    private boolean runRecorded;
    private List<int[]> fishCells = new ArrayList<>();
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
        replayLevel = getIntent().getIntExtra(EXTRA_REPLAY_LEVEL, 0);
        archiveDay = getIntent().getLongExtra(EXTRA_DAY, 0);
        if (mode == Mode.LAN && !joinRace()) return;
        store = new ProgressStore(this);
        progress = store.load();
        buildUi();
        fx.playCover("transitions_star-iris", IRIS_OPEN_FRAME, null);
        fx.preload("pickups_collect-burst", "pickups_chest-open", "ui_confetti", "ui_quest-complete", "pickups_coin-spin", "ui_achievement",
                "ui_combo-counter", "ui_countdown");
        AdsService.get(this).load(); // start loading a rewarded ad early
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
        // A race can't wait for one player, so its clock keeps running.
        if (timer != null && !adShowing && mode != Mode.LAN) {
            stopClock();
            showPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopClock();
        handler.removeCallbacksAndMessages(null);
        if (mode == Mode.LAN) LanSession.close();
    }

    @Override
    protected void onBack() {
        if (finished) {
            if (isRun()) recordRun(0);
            finish();
        } else if (mode == Mode.LAN) confirmLeaveRace();
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
        comboText.setVisibility(View.INVISIBLE);
        comboBig.setVisibility(View.INVISIBLE);
        meaningCard.setVisibility(View.GONE);
        botScore = 0;
        usedExtraTime = false;
        sparkAnnounced = false;
        fx.clear();
        TileMods mods = TileMods.NONE;
        Season.rollOver(progress, today);

        switch (mode) {
            case DAILY:
                level = gen.forDaily(today);
                firstDailyToday = progress.streak.lastDay != today;
                ambient.setTheme(AmbientView.Theme.PETALS);
                titleText.setText("DAILY");
                break;
            case ARCHIVE:
                level = gen.forDaily(archiveDay);
                ambient.setTheme(AmbientView.Theme.PETALS);
                titleText.setText(Days.monthAndDay(archiveDay));
                break;
            case TIMED:
                level = gen.forDuel(rnd.nextLong());
                ambient.setTheme(AmbientView.Theme.SNOW);
                titleText.setText("TIMED");
                break;
            case LAN:
                level = gen.forDuel(race.seed());
                ambient.setTheme(AmbientView.Theme.LEAVES);
                titleText.setText("WI-FI RACE");
                break;
            case DUEL:
                long seed = rnd.nextLong();
                level = gen.forDuel(seed);
                botScript = DuelBot.script(level, seed, 0.55, DUEL_SECONDS);
                ambient.setTheme(AmbientView.Theme.LEAVES);
                titleText.setText("DUEL");
                break;
            case MEGA:
                MegaBoard.begin(progress, today);
                level = gen.forMega(progress.megaWeek);
                mods = TileMods.forLevel(level);
                ambient.setTheme(AmbientView.Theme.FIREFLIES);
                titleText.setText("MEGA BOARD");
                break;
            case LIVES:
                if (run == null) {
                    run = new NineLives();
                    runSeed = rnd.nextLong();
                    runRecorded = false;
                }
                level = gen.forLives(runSeed, run.boards());
                if (run.boards() >= NineLives.FIRST_TWIST_DEPTH) mods = TileMods.forLevel(level);
                ambient.setTheme(AmbientView.Theme.values()[run.boards() % AmbientView.Theme.values().length]);
                titleText.setText("BOARD " + (run.boards() + 1));
                break;
            case TOWER:
                if (run == null) {
                    run = new WordTower();
                    runSeed = rnd.nextLong();
                    runRecorded = false;
                }
                level = gen.forTower(runSeed, run.boards());
                ambient.setTheme(AmbientView.Theme.values()[run.boards() / 2 % AmbientView.Theme.values().length]);
                titleText.setText("FLOOR " + ((WordTower) run).floor());
                break;
            case RUSH:
                if (run == null) {
                    runSeed = rnd.nextLong();
                    run = new BossRush(progress, runSeed);
                    runRecorded = false;
                }
                BossRush rush = (BossRush) run;
                level = gen.forRush(runSeed, rush.boards(), Journey.lastLevel(rush.chapter()));
                ambient.setRegion(Region.forChapter(rush.chapter()));
                titleText.setText("RUSH " + (rush.boards() + 1));
                break;
            default:
                playLevel = replayLevel > 0 ? replayLevel : progress.level;
                boolean hard = Journey.isHard(playLevel), boss = Journey.isBoss(playLevel);
                level = boss ? gen.forBoss(playLevel) : hard ? gen.forHard(playLevel) : gen.forProgression(playLevel);
                if (hard) mods = TileMods.forLevel(level);
                ambient.setRegion(Region.forChapter(Journey.chapterOf(playLevel)));
                titleText.setText((boss ? "BOSS " : hard ? "HARD " : "LEVEL ") + playLevel);
                break;
        }

        session = new GameSession(level, level.fingerprint(), mods);
        if (mode == Mode.MEGA) session.restore(progress.megaFound);
        coachLevel = mode == Mode.CAMPAIGN && replayLevel == 0 && progress.level <= LAST_COACHED_LEVEL ? progress.level : 0;
        coachText.setVisibility(coachLevel > 0 ? View.VISIBLE : View.GONE);
        grid.setSession(session);
        startFight();
        fishCells = huntOn() ? FishHunt.fishOn(level, Season.indexOf(today)) : new ArrayList<>();
        grid.setFish(fishCells);
        grid.refresh();
        wheel.setLetters(level.letters);
        preview.clear();
        timerBar.setAlarm(false);
        timerBar.setColor(PixelTheme.MINT);
        wheelCat.setWorried(false);
        timerBar.setValue(1f);
        boolean timed = limit() > 0;
        timerRow.setVisibility(timed ? View.VISIBLE : View.GONE);
        duelText.setVisibility(mode == Mode.DUEL || mode == Mode.LAN || isRun() ? View.VISIBLE : View.GONE);
        powerBtn.setVisibility(mode == Mode.DUEL || mode == Mode.LAN ? View.GONE : View.VISIBLE);
        secondsLeft = limit();
        updateHud();
        if (mode == Mode.MEGA && session.isComplete()) { // every word was saved, but the board was never paid
            handler.post(() -> endRound(true));
            return;
        }

        Runnable begin = () -> {
            if (timed) {
                wheel.setLocked(true);
                runCountdown();
            } else {
                wheel.setLocked(false);
                roundLive = true;
            }
        };
        Explain.Topic intro = openingTopic();
        if (intro != null && !Explain.seen(intro)) {
            wheel.setLocked(true);
            Explain.first(this, intro, begin);
        } else {
            begin.run();
        }
    }

    /** A mechanic on this board that the player has not been shown yet. Bosses first, then twists, then fish. */
    private Explain.Topic openingTopic() {
        if (fight != null) return Explain.Topic.BOSS;
        if (session.mods() != TileMods.NONE) return Explain.Topic.TWISTS;
        if (!fishCells.isEmpty()) return Explain.Topic.FISH;
        return null;
    }

    /** Nine Lives, the Word Tower and Boss Rush: boards one after another on one set of lives. */
    private boolean isRun() {
        return mode == Mode.LIVES || mode == Mode.TOWER || mode == Mode.RUSH;
    }

    /** The region whose boss is on this board, or null if there's no boss. */
    private Region bossRegion() {
        if (mode == Mode.RUSH && run != null) return Region.forChapter(((BossRush) run).chapter());
        return mode == Mode.CAMPAIGN && Journey.isBoss(playLevel) ? Journey.regionOf(playLevel) : null;
    }

    private void startFight() {
        Region region = bossRegion();
        boolean boss = region != null;
        fight = boss ? new BossFight(region, session) : null;
        bossRow.setVisibility(boss ? View.VISIBLE : View.GONE);
        if (!boss) return;
        bossName.setText(region.bossName().toUpperCase(Locale.ROOT));
        bossFace.removeAllViews();
        bossFace.addView(PixelIcons.view(this, PixelIcons.boss(region)), fill());
        bossBar.setSegments(fight.maxHealth());
        bossBar.setValue(1f);
        handler.postDelayed(() -> {
            if (fight != null) say("THE BOSS BLOCKS THE WAY!", PixelTheme.ROSE);
        }, BOSS_ENTRANCE_MS);
    }

    /** Brings the screen up to date with the fight, and plays any attack that just landed. */
    private void afterBossTurn(Boss.Attack attack) {
        if (fight == null) return;
        bossBar.animateTo(fight.health() / (float) fight.maxHealth());
        grid.setDark(fight.darkCells());
        wheel.setStolen(fight.stolenLetter());
        grid.refresh();
        if (attack == null || session.isComplete()) return;
        if (attack == Boss.Attack.SCRAMBLE) wheel.shuffle(rnd);
        Sfx.play(Sfx.Id.WORD_ALREADY, 0.7f);
        Haptics.thud();
        wheelCat.startle();
        handler.postDelayed(() -> {
            if (!finished) say(attack.shout, PixelTheme.ROSE);
        }, BOSS_SHOUT_DELAY_MS);
        if (PixelTheme.reduceMotion()) return;
        bossFace.setScaleX(1.5f);
        bossFace.setScaleY(1.5f);
        bossFace.animate().scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(new OvershootInterpolator(2.5f)).start();
    }

    /** The boss flinches as a word lands. */
    private void hitBoss() {
        if (fight == null || PixelTheme.reduceMotion()) return;
        final float d = PixelTheme.dp(this, 5);
        bossFace.animate().cancel();
        bossFace.setTranslationX(d);
        bossFace.animate().translationX(0f).setDuration(180).start();
    }

    /** Fish hide on new campaign levels and on the first daily of the day, while a hunt is on. */
    private boolean huntOn() {
        boolean board = mode == Mode.CAMPAIGN && replayLevel == 0 || mode == Mode.DAILY && firstDailyToday;
        return board && FishHunt.isLive(Days.today());
    }

    /** How many fish the revealed letters have uncovered so far. */
    private int fishFound() {
        int found = 0;
        for (int[] cell : fishCells) if (session.isRevealed(cell[0], cell[1])) found++;
        return found;
    }

    private int limit() {
        if (mode == Mode.LAN) return race.seconds();
        return mode == Mode.TIMED ? TIMED_SECONDS : mode == Mode.DUEL ? DUEL_SECONDS : 0;
    }

    /** 3, 2, 1, GO! before any timed round. */
    private void runCountdown() {
        final int steps = 4;
        long at = 900;
        handler.postDelayed(() -> fx.play("ui_countdown", fx.getWidth() / 2f, fx.getHeight() / 2f, PixelTheme.dp(this, 3), null), at);
        for (int i = 0; i < steps; i++) {
            final boolean go = i == steps - 1;
            handler.postDelayed(() -> {
                Sfx.play(go ? Sfx.Id.COUNT_GO : Sfx.Id.COUNT_BEEP);
                if (go) Haptics.thud();
                else Haptics.click();
            }, at);
            at += COUNTDOWN_STEP_MS;
        }
        handler.postDelayed(() -> {
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
            wheelCat.setWorried(true);
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
                addCombo();
                preview.resolve(PixelTheme.MINT, false);
                Level.Placement p = placementOf(word);
                if (p != null) burstAt(p.rowAt(p.word.length() / 2), p.colAt(p.word.length() / 2));
                grid.refresh();
                if (p != null) grid.pulse(p);
                Sfx.play(Sfx.Id.WORD_FOUND, Math.min(1.5f, 1f + (combo - 1) * 0.08f));
                Haptics.word(word.length(), combo);
                wheelCat.hop();
                if (combo >= CAT_HEARTS_FROM) wheelCat.heart();
                String praise = praiseFor(word.length());
                say((praise == null ? "" : praise + "  ") + "+" + Scoring.gridWordPoints(word.toLowerCase(Locale.ROOT)), PixelTheme.MINT);
                if (session.sparkWon() && !sparkAnnounced) {
                    sparkAnnounced = true;
                    say("SPARK!  +" + TileMods.SPARK_COINS, PixelTheme.GOLD);
                }
                keepMegaProgress();
                if (session.isComplete()) endRound(true);
                else offerMeaningTip();
                if (fight != null) {
                    hitBoss();
                    afterBossTurn(fight.onBoardChanged());
                }
                break;
            case FOUND_BONUS:
                addCombo();
                preview.resolve(PixelTheme.GOLD, false);
                wheelCat.hop();
                say("BONUS WORD  JAR +1", PixelTheme.GOLD);
                Sfx.play(Sfx.Id.WORD_BONUS);
                Haptics.success();
                if (limit() == 0 && !Explain.seen(Explain.Topic.JAR)) handler.postDelayed(() -> {
                    if (!finished) Explain.first(this, Explain.Topic.JAR, null);
                }, 600);
                break;
            case FROZEN:
                preview.resolve(PixelTheme.SKY, true);
                wheelCat.startle();
                say("FROZEN! FIND A CROSSING WORD", PixelTheme.SKY);
                Sfx.play(Sfx.Id.WORD_ALREADY);
                Haptics.click();
                break;
            case ALREADY_FOUND:
                preview.resolve(PixelTheme.TEXT_DIM, false);
                say("ALREADY FOUND", PixelTheme.TEXT_DIM);
                Level.Placement again = placementOf(word);
                if (again != null) grid.pulse(again); // shows where it is
                Sfx.play(Sfx.Id.WORD_ALREADY);
                Haptics.click();
                break;
            case NOT_A_WORD:
                breakCombo();
                preview.resolve(PixelTheme.ROSE, true);
                say("NOT A WORD", PixelTheme.ROSE);
                Sfx.play(Sfx.Id.WORD_WRONG);
                Haptics.error();
                shakeWheel();
                wheelCat.wince();
                if (fight != null) afterBossTurn(fight.onWrongWord());
                if (isRun()) {
                    run.wrongWord();
                    if (run.isOver()) {
                        updateHud();
                        endRound(false);
                        return;
                    }
                }
                break;
            default:
                preview.clear(); // too short: ignore silently
                break;
        }
        updateHud();
    }

    /** The run of right answers, shown beside the board and growing as it climbs. */
    private void addCombo() {
        combo++;
        if (combo < COMBO_SHOWN_FROM) return;
        comboText.setText("x" + combo);
        comboText.setTextColor(combo >= COMBO_SHEET_AT ? PixelTheme.ROSE : combo >= 3 ? PixelTheme.GOLD : PixelTheme.TEXT);
        comboText.setVisibility(View.VISIBLE);
        float size = Math.min(1.6f, 1f + (combo - COMBO_SHOWN_FROM) * 0.12f);
        comboText.animate().cancel();
        comboText.setAlpha(1f);
        comboText.setScaleX(size * 1.5f);
        comboText.setScaleY(size * 1.5f);
        comboText.animate().scaleX(size).scaleY(size).setDuration(220).setInterpolator(new OvershootInterpolator(2.5f)).start();
        if (combo == COMBO_SHEET_AT) {
            fx.play("ui_combo-counter", fx.getWidth() / 2f, fx.getHeight() * 0.16f, PixelTheme.dp(this, 1.5f), null);
        } else if (combo > COMBO_SHEET_AT && combo % COMBO_SHEET_AT == 0) {
            bigCombo();
        }
    }

    /** Every fifth answer in a row past the first gets a big centred "x10", "x15" and so on, with a burst. */
    private void bigCombo() {
        int[] colours = {PixelTheme.ROSE, PixelTheme.GOLD, PixelTheme.MINT, PixelTheme.SKY};
        comboBig.setText("COMBO\nx" + combo);
        comboBig.setTextColor(colours[(combo / COMBO_SHEET_AT - 2) % colours.length]);
        comboBig.setVisibility(View.VISIBLE);
        comboBig.animate().cancel();
        comboBig.setAlpha(1f);
        comboBig.setScaleX(0.3f);
        comboBig.setScaleY(0.3f);
        comboBig.animate().scaleX(1.25f).scaleY(1.25f).setDuration(260).setInterpolator(new OvershootInterpolator(3f))
                .withEndAction(() -> comboBig.animate().alpha(0f).scaleX(1.6f).scaleY(1.6f).setStartDelay(650).setDuration(280)
                        .withEndAction(() -> comboBig.setVisibility(View.INVISIBLE)).start())
                .start();
        fx.play("pickups_collect-burst", fx.getWidth() / 2f, fx.getHeight() * 0.3f, PixelTheme.dp(this, 3f), null);
        Sfx.play(Sfx.Id.WORD_BONUS, Math.min(1.6f, 1f + combo * 0.02f));
        Haptics.celebrate();
    }

    private void breakCombo() {
        combo = 0;
        comboBig.animate().cancel();
        comboBig.setVisibility(View.INVISIBLE);
        comboText.animate().cancel();
        comboText.animate().alpha(0f).scaleX(0.5f).scaleY(0.5f).setDuration(180)
                .withEndAction(() -> comboText.setVisibility(View.INVISIBLE)).start();
    }

    /** Shows what a found word means, over the board. It never stops the clock, so it's safe in a race. */
    private void onBoardTapped(int[] cell) {
        if (cell == null || session == null) {
            dismissMeaning();
            return;
        }
        List<String> here = new ArrayList<>();
        for (Level.Placement p : level.placements) {
            if (!session.foundGridWords().contains(p.word)) continue;
            for (int i = 0; i < p.word.length(); i++) {
                if (p.rowAt(i) == cell[0] && p.colAt(i) == cell[1]) here.add(p.word);
            }
        }
        if (here.isEmpty()) {
            dismissMeaning();
            return;
        }
        // A letter where two words cross shows each of them in turn.
        int next = (here.indexOf(lastMeaningWord) + 1) % here.size();
        showMeaning(here.get(next));
    }

    private void showMeaning(String word) {
        lastMeaningWord = word;
        Definitions.Entry entry = GameEngine.get().definitions() == null ? null : GameEngine.get().definitions().find(word);
        meaningWord.setText(word.toUpperCase(Locale.ROOT) + (entry == null ? "" : "  " + entry.partOfSpeech));
        meaningText.setText(entry == null ? "This one is a name, or too rare for the dictionary."
                : entry.isInflected() ? "From \u201C" + entry.baseWord + "\u201D: " + entry.meaning : entry.meaning);
        Sfx.play(Sfx.Id.UI_TAP);
        meaningCard.animate().cancel();
        meaningCard.setVisibility(View.VISIBLE);
        meaningCard.setAlpha(1f);
        handler.removeCallbacks(hideMeaning);
        handler.postDelayed(hideMeaning, MEANING_SHOWN_MS);
    }

    private void dismissMeaning() {
        handler.removeCallbacks(hideMeaning);
        lastMeaningWord = "";
        if (meaningCard.getVisibility() != View.VISIBLE) return;
        meaningCard.animate().alpha(0f).setDuration(160).withEndAction(() -> meaningCard.setVisibility(View.GONE)).start();
    }

    /** Once ever, after a player's first found word on a level past the tutorial. */
    private void offerMeaningTip() {
        if (Settings.meaningTipShown() || coachLevel > 0) return;
        Settings.setMeaningTipShown();
        handler.postDelayed(() -> say("TAP A WORD TO SEE ITS MEANING", PixelTheme.SKY), MEANING_TIP_DELAY_MS);
    }

    /** A weekend board can take two days, so each word is saved as it's found. */
    private void keepMegaProgress() {
        if (mode != Mode.MEGA) return;
        progress.megaFound = session.foundMask();
        store.save(progress);
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
        if (mode == Mode.LAN) {
            say("NO HINTS IN A RACE", PixelTheme.ROSE); // a hint stops the clock, which a race can't do
            return;
        }
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
        PixelModal m = new PixelModal(this).title("OUT OF HINTS").icon("bulb").accent(PixelTheme.GOLD)
                .body("You've used all your hint tokens, and don't have enough coins to buy one.")
                .stat("bulb", "Hint tokens", "0", PixelTheme.ROSE)
                .stat("coin", "Your coins", String.valueOf(progress.coins), PixelTheme.GOLD)
                .stat("coin", "Hint price", String.valueOf(Scoring.HINT_COST_COINS), PixelTheme.TEXT);
        if (Features.MONETIZATION) {
            m.note("You're " + needed + " coins short. Watch a short video to reveal one letter for free.")
                    .primary("WATCH VIDEO", "film", this::watchVideo)
                    .secondary("KEEP PLAYING", null);
        } else {
            m.note("You're " + needed + " coins short. Fill your piggy jar with bonus words for more.")
                    .primary("KEEP PLAYING", null);
        }
        m.onDismiss(this::afterModal).show();
    }

    private void watchVideo() {
        adShowing = true;
        stopClock();
        AdsService.get(this).showRewarded(this, () -> handler.post(this::revealHint), () -> {
            adShowing = false;
            new PixelModal(this).title("NO VIDEO").icon("film").accent(PixelTheme.ROSE)
                    .body("There isn't a video available right now. This usually clears up in a minute, or check your connection.")
                    .note("You can also earn coins: clearing a level +" + Scoring.COINS_PER_LEVEL
                            + ", the daily puzzle +" + Scoring.COINS_PER_DAILY + ".")
                    .primary("KEEP PLAYING", null)
                    .onDismiss(this::afterModal)
                    .show();
        });
    }

    // ------------------------------------------------------------------ power-ups

    private void onPowerPressed() {
        if (finished || session == null || !roundLive) return;
        if (!Explain.seen(Explain.Topic.POWERUPS)) {
            stopClock();
            wheel.setLocked(true);
            Explain.first(this, Explain.Topic.POWERUPS, () -> {
                afterModal();
                onPowerPressed();
            });
            return;
        }
        Sfx.play(Sfx.Id.UI_TAP);
        stopClock();
        wheel.setLocked(true);
        PixelModal m = new PixelModal(this).title("POWER-UPS").icon("bolt").accent(PixelTheme.GOLD);
        if (mode == Mode.TIMED) addPower(m, PowerUps.Kind.EXTRA_TIME);
        else {
            addPower(m, PowerUps.Kind.PICK_LETTER);
            addPower(m, PowerUps.Kind.REVEAL_WORD);
        }
        if (mode == Mode.CAMPAIGN) m.note("Each one counts against your stars, like a hint.");
        m.secondary("CANCEL", null).onDismiss(this::afterModal).show();
    }

    private void addPower(PixelModal m, PowerUps.Kind kind) {
        int held = PowerUps.tokens(progress, kind);
        m.note(kind.title + ": " + kind.detail);
        String name = kind.title.toUpperCase(Locale.ROOT);
        String label = held > 0 ? name + " x" + held : name + " " + kind.price;
        boolean can = PowerUps.canUse(progress, kind);
        m.button(label, can ? PixelTheme.GOLD : PixelTheme.PANEL, can ? PixelTheme.INK : PixelTheme.TEXT_DIM,
                held > 0 ? kind.icon : "coin", Sfx.Id.UI_TAP, () -> usePower(kind));
    }

    private void usePower(PowerUps.Kind kind) {
        if (!PowerUps.canUse(progress, kind)) {
            say("NOT ENOUGH COINS", PixelTheme.ROSE);
            return;
        }
        switch (kind) {
            case REVEAL_WORD:
                revealWord();
                break;
            case PICK_LETTER:
                holdBanner("TAP A HIDDEN LETTER", PixelTheme.GOLD);
                grid.pickLetter(this::onLetterPicked);
                break;
            default:
                addTime();
                break;
        }
    }

    private void revealWord() {
        if (session.isComplete() || !PowerUps.spend(progress, PowerUps.Kind.REVEAL_WORD)) return;
        Level.Placement p = session.revealWord();
        store.save(progress);
        burstAt(p.rowAt(p.word.length() / 2), p.colAt(p.word.length() / 2));
        afterReveal();
    }

    /** Nothing is charged until the tap lands on a hidden letter. */
    private void onLetterPicked(int[] cell) {
        if (finished || cell == null || !session.revealCell(cell[0], cell[1])) {
            say("NO LETTER PICKED", PixelTheme.TEXT_DIM);
            return;
        }
        PowerUps.spend(progress, PowerUps.Kind.PICK_LETTER);
        store.save(progress);
        banner.setVisibility(View.INVISIBLE);
        burstAt(cell[0], cell[1]);
        afterReveal();
    }

    private void addTime() {
        if (!PowerUps.spend(progress, PowerUps.Kind.EXTRA_TIME)) return;
        store.save(progress);
        usedExtraTime = true;
        stopClock(); // the running timer would otherwise overwrite the new time on its next tick
        secondsLeft += PowerUps.EXTRA_SECONDS;
        resumeClock();
        Sfx.play(Sfx.Id.HINT);
        say("+" + PowerUps.EXTRA_SECONDS + " SECONDS", PixelTheme.MINT);
        updateHud();
    }

    private void afterReveal() {
        grid.refresh();
        if (fight != null) afterBossTurn(fight.onBoardChanged());
        Sfx.play(Sfx.Id.HINT);
        Haptics.click();
        updateHud();
        keepMegaProgress();
        if (session.isComplete()) endRound(true);
    }

    /** Reveals one letter. Returns false if nothing was left to reveal (so the player is not charged). */
    private boolean revealHint() {
        int[] cell = session.revealHint();
        if (cell == null) return false;
        burstAt(cell[0], cell[1]);
        grid.refresh();
        if (fight != null) afterBossTurn(fight.onBoardChanged());
        Sfx.play(Sfx.Id.HINT);
        Haptics.click();
        updateHud();
        keepMegaProgress();
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
                        : mode == Mode.MEGA ? "Your found words are saved, so you can come back to this board."
                        : isRun() ? "If you quit, the run ends here."
                        : "If you quit, this puzzle restarts next time and its bonus words are lost.")
                .primary("RESUME", "play", null)
                .button("QUIT TO MENU", PixelTheme.ROSE, PixelTheme.INK, null, Sfx.Id.UI_BACK, this::quit)
                .onDismiss(this::afterModal)
                .show();
    }

    private void quit() {
        finished = true;
        stopClock();
        if (isRun()) recordRun(session.score(0));
        finish();
    }

    /** Ends a run. Safe to call twice, so the run can never be paid again. */
    private boolean recordRun(int lastBoard) {
        if (run == null || runRecorded) return false;
        runRecorded = true;
        run.addScore(lastBoard);
        boolean record = run.finish(progress);
        store.save(progress);
        PlayGamesService.saveProgress(this, progress.serialize());
        if (run.result() > 0) {
            int board = mode == Mode.TOWER ? R.string.leaderboard_tower : mode == Mode.RUSH ? R.string.leaderboard_rush : R.string.leaderboard_lives;
            PlayGamesService.submitScore(this, getString(board), run.result());
        }
        return record;
    }

    // ------------------------------------------------------------------ finishing

    private void endRound(boolean solved) {
        if (finished) return;
        finished = true;
        stopClock();
        wheel.setLocked(true);
        timerBar.setAlarm(false);
        wheelCat.setWorried(false);
        if (solved) {
            wheelCat.hop();
            grid.ripple();
        } else {
            wheelCat.wince();
        }

        // Everything is banked immediately, so leaving during the celebration loses nothing.
        final Outcome o = new Outcome();
        o.solved = solved;
        o.found = session.foundGridWords().size();
        o.bonus = session.foundBonusWords().size();
        o.hints = session.hintsUsed();
        o.score = session.score(secondsLeft);
        o.coins = session.coinsEarned();
        o.levelNumber = playLevel;
        o.streakBefore = StreakLogic.displayStreak(progress.streak, Days.today());
        int freezesBefore = progress.streak.freezes;
        Set<Achievement> hadBefore = Achievement.unlocked(progress);
        long today = Days.today();
        boolean fresh = replayLevel == 0 && !(mode == Mode.DAILY && !firstDailyToday)
                && !(mode == Mode.ARCHIVE && DailyArchive.isSolved(progress, archiveDay));
        if (fresh) bankCollections(o, today);

        if (solved && mode == Mode.CAMPAIGN) {
            progress.recordStars(playLevel, stars(o.hints));
            if (replayLevel > 0) {
                o.coins = 0; // replays are for stars only, so old levels can't be farmed
            } else {
                o.coins += Journey.coinsForLevel(playLevel);
                o.chapterFinished = Journey.slotOf(playLevel) == Journey.LEVELS_PER_CHAPTER;
                if (fight != null) {
                    o.prize = Journey.bossPrize(playLevel).grant(progress, today);
                    o.trophy = true;
                }
                progress.level++;
                Season.addXp(progress, today, Season.XP_LEVEL);
            }
        } else if (solved && mode == Mode.DAILY && firstDailyToday) {
            o.coins += Scoring.COINS_PER_DAILY;
            progress.streak = StreakLogic.onDailyCompleted(progress.streak, Days.today());
            progress.dailiesCompleted++;
            o.streakGrew = true;
            DailyArchive.backfill(progress); // before the log gets its first entry, or the old streak days are lost
            DailyArchive.markSolved(progress, today);
            o.freezeEarned = progress.streak.freezes > freezesBefore;
            o.milestonePrizes = StreakMilestone.payDue(progress, today);
            Season.addXp(progress, today, Season.XP_DAILY);
        }
        if (solved && mode == Mode.ARCHIVE) {
            o.coins += DailyArchive.solve(progress, archiveDay);
        }
        if (solved && mode == Mode.MEGA && MegaBoard.claim(progress)) {
            Season.addXp(progress, today, MegaBoard.SEASON_XP);
            o.prize = Reward.powerUp(PowerUps.Kind.REVEAL_WORD, 1).grant(progress, today);
            o.megaCoins = MegaBoard.COINS;
        }
        if (isRun() && solved) {
            o.lifeBack = run.boardCleared(o.score);
        }
        if (mode == Mode.TIMED && !usedExtraTime) {
            o.record = o.score > progress.bestTimed && o.score > 0;
            o.previousBest = progress.bestTimed;
            progress.bestTimed = Math.max(progress.bestTimed, o.score);
        }
        if (mode == Mode.LAN) {
            o.mine = session.gridPoints() + session.bonusPoints();
            race.finish(o.mine, solved);
            progress.duelsPlayed++;
            if (wonRace(o.mine)) {
                o.coins += DUEL_WIN_COINS;
                progress.duelWins++;
            }
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
        progress.addCoins(o.coins);
        if (fresh) progress.totalWordsFound += o.found + o.bonus; // or replays could farm achievements
        o.achievements = Achievement.newlyUnlocked(hadBefore, progress);
        store.save(progress);
        PlayGamesService.saveProgress(this, progress.serialize());
        if (mode != Mode.TIMED || !usedExtraTime) submitScore(o.score);
        earn(grid, coinChip, progress.coins);

        if (solved) {
            fx.play("ui_confetti", fx.getWidth() / 2f, fx.getHeight() * 0.3f, PixelTheme.dp(this, 3), null);
            Haptics.celebrate();
        } else {
            Haptics.fail();
        }
        playEndingAudio(o);
        handler.postDelayed(() -> showResults(o), solved ? 1150 : 900);
    }

    /** Drops bonus words in the jar and banks fish. */
    private void bankCollections(Outcome o, long today) {
        PiggyJar.add(progress, session.foundBonusWords().size());
        // A full jar opens right here, so nobody has to leave the level to collect it.
        o.jarOpened = PiggyJar.canOpen(progress, today) && PiggyJar.open(progress, today);
        o.fish = fishFound();
        if (o.fish > 0) FishHunt.add(progress, today, o.fish);
    }

    /** What happened in the round, gathered for the results modal. */
    private static final class Outcome {
        boolean solved, record, streakGrew, freezeEarned, chapterFinished, lifeBack, trophy, jarOpened;
        int found, bonus, hints, score, coins, levelNumber, streakBefore, mine, bot, previousBest;
        int fish, megaCoins;
        Reward prize;
        List<Reward> milestonePrizes = new ArrayList<>();
        List<Achievement> achievements = new ArrayList<>();
    }

    /**
     * Every results screen is laid out the same way, so a player always knows where to look: the cat if
     * it went well, the stars, one big number, the details, what was won, then what to do next.
     */
    private PixelModal results(String title, String icon, int accent, boolean won, int stars, String heroLabel, int heroValue) {
        PixelModal m = new PixelModal(this).title(title).icon(icon).accent(accent).quiet().cancelable(false);
        if (won) m.custom(cheeringCat());
        if (stars > 0) m.custom(new StarsView(this, stars, 350));
        return m.hero(heroLabel, heroValue, PixelTheme.GOLD);
    }

    /** What the round won, in one place, whichever mode it was. */
    private void addWinnings(PixelModal m, Outcome o, int coins) {
        if (coins > 0) m.countStat("coin", "Coins earned", "+", coins, PixelTheme.GOLD);
        if (o.prize != null) m.stat(o.prize.icon(), o.prize.label(), "", PixelTheme.SKY);
        if (o.trophy) m.stat("trophy", "Boss trophy for your room", "", PixelTheme.GOLD);
        if (o.freezeEarned) m.stat("freeze", "Freeze earned", "+1", PixelTheme.SKY);
        if (!o.milestonePrizes.isEmpty()) {
            m.heading("STREAK MILESTONE!");
            for (Reward r : o.milestonePrizes) m.stat(r.icon(), r.label(), "", PixelTheme.GOLD);
        }
        if (o.jarOpened) {
            m.countStat("jar", "Piggy jar opened!", "+", PiggyJar.COINS, PixelTheme.GOLD);
            handler.postDelayed(() -> {
                fx.play("pickups_chest-open", fx.getWidth() / 2f, fx.getHeight() * 0.3f, PixelTheme.dp(this, 3), null);
                Sfx.play(Sfx.Id.BUY);
                Haptics.celebrate();
            }, 500);
        } else if (o.bonus > 0 && replayLevel == 0) {
            m.stat("jar", "Piggy jar", progress.jarWords + " / " + PiggyJar.CAPACITY, PixelTheme.GOLD);
        }
        if (o.fish > 0) m.stat("fish", "Fish caught", "+" + o.fish, PixelTheme.SKY);
        if (o.achievements.isEmpty()) return;
        m.heading("ACHIEVEMENT UNLOCKED");
        for (Achievement a : o.achievements) m.stat("trophy", a.title, "+" + a.tier.coins, PixelTheme.GOLD);
        m.note("Claim your coins from the trophy on the menu.");
        handler.postDelayed(() -> {
            fx.play("ui_achievement", fx.getWidth() / 2f, fx.getHeight() * 0.25f, PixelTheme.dp(this, 3), null);
            Sfx.play(Sfx.Id.STREAK_UP);
        }, 700);
    }

    private void addWords(PixelModal m, Outcome o) {
        m.stat("book", "Words found", o.found + " / " + level.placements.size(), PixelTheme.MINT)
                .stat("sparkle", "Bonus words", String.valueOf(o.bonus), PixelTheme.GOLD);
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
            case ARCHIVE:
                showArchiveResults(o);
                break;
            case TIMED:
                showTimedResults(o);
                break;
            case LAN:
                showRaceResults(o);
                break;
            case DUEL:
                showDuelResults(o);
                break;
            case MEGA:
                showMegaResults(o);
                break;
            case LIVES:
            case TOWER:
            case RUSH:
                showRunResults(o);
                break;
            default:
                showCampaignResults(o);
                break;
        }
    }

    private void showCampaignResults(Outcome o) {
        int s = stars(o.hints);
        PixelModal m = results(fight != null ? "BOSS BEATEN!" : "LEVEL " + o.levelNumber + " CLEAR!",
                fight != null ? PixelIcons.boss(Journey.regionOf(playLevel)) : "star", PixelTheme.GOLD, true, s, "SCORE", o.score);
        addWords(m, o);
        m.stat("bulb", "Hints used", String.valueOf(o.hints), o.hints == 0 ? PixelTheme.MINT : PixelTheme.TEXT);
        addWinnings(m, o, o.coins);
        if (fight != null) m.note(Journey.regionOf(playLevel).bossName() + " runs off, and the path is clear.");
        if (s < 3) m.note("Clear a level with no hints for three stars.");
        String goal = replayLevel == 0 ? Journey.nextGoal(playLevel) : "";
        if (!goal.isEmpty()) m.note(goal);
        if (Features.MONETIZATION && replayLevel == 0 && o.coins > 0) {
            m.button("DOUBLE COINS", PixelTheme.MINT, PixelTheme.INK, "film", Sfx.Id.UI_TAP, () -> doubleCoins(o));
        }
        campaignNext(m, o).show();
    }

    /** Where to go after a level: the map, a replay, or straight on to the next level. */
    private PixelModal campaignNext(PixelModal m, Outcome o) {
        if (replayLevel > 0) {
            return m.note("Replays can win more stars, but they don't pay coins.")
                    .primary("TO THE MAP", this::finish)
                    .secondary("PLAY AGAIN", this::startRound);
        }
        if (o.chapterFinished) return m.primary("TO THE MAP", this::finish); // the chapter's reward is waiting there
        return m.primary("NEXT LEVEL", "play", this::nextLevel).secondary("MAP", this::finish);
    }

    /** A rewarded video that pays the level's coins a second time. */
    private void doubleCoins(Outcome o) {
        AdsService.get(this).showRewarded(this, () -> handler.post(() -> {
            progress.addCoins(o.coins);
            store.save(progress);
            PlayGamesService.saveProgress(this, progress.serialize());
            coinChip.animateTo(progress.coins);
            Sfx.play(Sfx.Id.COIN);
            campaignNext(new PixelModal(this).title("COINS DOUBLED!").icon("coin").accent(PixelTheme.GOLD).cancelable(false)
                    .countStat("coin", "Bonus coins", "+", o.coins, PixelTheme.GOLD), o).show();
        }), () -> campaignNext(new PixelModal(this).title("NO VIDEO").icon("film").accent(PixelTheme.ROSE).cancelable(false)
                .body("There isn't a video ready right now. Your coins from this level are safe."), o).show());
    }

    private void showMegaResults(Outcome o) {
        PixelModal m = results("MEGA BOARD CLEAR!", "star", PixelTheme.GOLD, true, 0, "SCORE", o.score);
        m.stat("ticket", "Season XP", "+" + MegaBoard.SEASON_XP, PixelTheme.ROSE);
        addWinnings(m, o, o.coins + o.megaCoins);
        m.primary("MENU", this::finish).show();
    }

    private String runIcon() {
        return mode == Mode.TOWER ? "tower" : mode == Mode.RUSH ? "boss_king" : "heart";
    }

    /** What the run is ranked by, for the big number on its results. */
    private String runHeadline() {
        return mode == Mode.TOWER ? "FLOORS" : mode == Mode.RUSH ? "BOSSES" : "RUN SCORE";
    }

    private String runBoardsLabel() {
        return mode == Mode.TOWER ? "Floors climbed" : mode == Mode.RUSH ? "Bosses beaten" : "Boards cleared";
    }

    private void showRunResults(Outcome o) {
        if (o.solved) {
            String title = mode == Mode.TOWER ? "FLOOR " + run.boards() + " CLEARED!" : mode == Mode.RUSH ? "BOSS BEATEN!" : "BOARD CLEAR!";
            String next = mode == Mode.TOWER ? "NEXT FLOOR" : mode == Mode.RUSH ? "NEXT BOSS" : "NEXT BOARD";
            PixelModal m = results(title, runIcon(), PixelTheme.MINT, true, 0, runHeadline(), run.result());
            m.stat("heart", "Lives", String.valueOf(run.lives()), PixelTheme.ROSE)
                    .stat("coin", "Coins so far", "+" + run.coinsEarned(), PixelTheme.GOLD);
            if (o.lifeBack) m.note("You won a life back!");
            if (mode == Mode.TOWER) {
                int longer = LevelGenerator.specForTower(run.boards()).minWordLength;
                if (longer > LevelGenerator.specForTower(run.boards() - 1).minWordLength) {
                    m.note("The words get longer from here: " + longer + " letters or more.");
                }
            }
            if (mode == Mode.RUSH) m.note("Next up: " + Region.forChapter(((BossRush) run).chapter()).bossName() + ".");
            addWinnings(m, o, 0);
            m.primary(next, "play", this::startRound)
                    .secondary("END RUN", () -> showRunOver(o))
                    .show();
        } else if (run.canContinue(progress)) {
            results("OUT OF LIVES", runIcon(), PixelTheme.ROSE, false, 0, runHeadline(), run.result())
                    .note("Try this board again with " + run.continueLives + (run.continueLives == 1 ? " life" : " lives") + ". Once per run.")
                    .button("CONTINUE FOR " + run.continueCoins, PixelTheme.GOLD, PixelTheme.INK, "coin", null, this::continueRun)
                    .secondary("END RUN", () -> showRunOver(o))
                    .show();
        } else {
            showRunOver(o);
        }
    }

    private void continueRun() {
        if (!run.continueRun(progress)) return;
        store.save(progress);
        coinChip.animateTo(progress.coins);
        startRound(); // a fresh round, so the words banked from the last one can't be banked twice
    }

    private void showRunOver(Outcome o) {
        boolean record = recordRun(o.solved ? 0 : o.score);
        coinChip.animateTo(progress.coins);
        PixelModal m = results("RUN OVER", runIcon(), PixelTheme.ROSE, false, 0, runHeadline(), run.result());
        // The big number already counts floors or bosses; only Nine Lives ranks by something else.
        if (mode == Mode.LIVES) m.stat("star", runBoardsLabel(), String.valueOf(run.boards()), PixelTheme.MINT);
        m.stat("sparkle", record ? "New best!" : "Your best", String.valueOf(run.best(progress)), record ? PixelTheme.GOLD : PixelTheme.TEXT)
                .countStat("coin", "Coins earned", "+", run.coinsEarned(), PixelTheme.GOLD);
        run = null;
        m.primary("AGAIN", "play", this::startRound).secondary("MENU", this::finish).show();
    }

    private void nextLevel() {
        AdsService.get(this).betweenLevels(this, progress, this::startRound);
    }

    private View cheeringCat() {
        CatView cat = new CatView(this, 112);
        cat.dress(progress);
        cat.setCheering(true);
        cat.setClickable(false);
        return cat;
    }

    private void showDailyResults(Outcome o) {
        PixelModal m = results("DAILY COMPLETE!", "calendar", PixelTheme.MINT, true, stars(o.hints), "SCORE", o.score);
        if (o.streakGrew) {
            m.stat("flame", "Streak", o.streakBefore + " > " + progress.streak.streak, PixelTheme.ROSE);
            handler.postDelayed(() -> {
                Sfx.play(Sfx.Id.STREAK_UP);
                Haptics.success();
            }, 1250);
        }
        addWinnings(m, o, o.streakGrew ? o.coins : 0);
        m.note(o.streakGrew ? "Come back tomorrow to keep your streak alive."
                : "You'd already solved today's puzzle, so this replay pays no coins and doesn't change your streak.");
        if (o.streakGrew) m.primary("SHARE", "share", () -> shareDaily(o)).secondary("MENU", this::finish);
        else m.primary("MENU", this::finish);
        m.show();
    }

    private void showArchiveResults(Outcome o) {
        PixelModal m = results(Days.monthAndDay(archiveDay) + " SOLVED!", "calendar", PixelTheme.MINT, true, stars(o.hints), "SCORE", o.score);
        addWinnings(m, o, o.coins);
        if (o.coins == 0) m.note("You'd already solved this one, so this replay pays nothing.");
        m.primary("MORE PUZZLES", "calendar", this::finish).show();
    }

    /** Back at the menu after sharing: the results modal closes with its button, so nothing is left to return to. */
    private void shareDaily(Outcome o) {
        Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,
                ShareCard.daily(Days.today(), stars(o.hints), progress.streak.streak, o.hints, o.score));
        startActivity(Intent.createChooser(send, "Share your result"));
        finish();
    }

    private void showTimedResults(Outcome o) {
        PixelModal m = results(o.solved ? "BOARD CLEARED!" : "TIME'S UP!", "clock", PixelTheme.SKY, o.solved || o.record, 0, "SCORE", o.score);
        if (o.record) {
            m.stat("sparkle", "New record!", o.previousBest > 0 ? "WAS " + o.previousBest : "FIRST RUN", PixelTheme.MINT);
            handler.postDelayed(() -> Sfx.play(Sfx.Id.STREAK_UP), 1000);
        } else {
            m.stat("star", "Your best", String.valueOf(progress.bestTimed), PixelTheme.TEXT);
        }
        addWords(m, o);
        addWinnings(m, o, o.coins);
        addMissed(m);
        m.primary("AGAIN", "play", this::startRound).secondary("MENU", this::finish).show();
    }

    private void showDuelResults(Outcome o) {
        boolean won = o.mine > o.bot, draw = o.mine == o.bot;
        PixelModal m = results(won ? "YOU WIN!" : draw ? "IT'S A DRAW" : "BOT WINS", "swords",
                won ? PixelTheme.MINT : draw ? PixelTheme.GOLD : PixelTheme.ROSE, won, 0, "YOUR SCORE", o.mine);
        m.countStat("close", "Bot", "", o.bot, won ? PixelTheme.TEXT : PixelTheme.ROSE)
                .stat("trophy", "Your record", MainActivity.wins(progress.duelWins).toUpperCase(Locale.ROOT) + " / " + progress.duelsPlayed, PixelTheme.TEXT);
        addWinnings(m, o, won ? o.coins : 0);
        addMissed(m);
        m.primary("REMATCH", "swords", this::startRound).secondary("MENU", this::finish).show();
    }

    /** Connects this screen to the race the lobby started. False if there's no race to join. */
    private boolean joinRace() {
        race = LanSession.get();
        if (race == null) {
            finish();
            return false;
        }
        race.setListener(new LanMatch.Listener() {
            @Override public void onPlayersChanged() { runOnUiThread(GameActivity.this::updateHud); }
            @Override public void onRaceStarted() {}
            @Override public void onScoresChanged() { runOnUiThread(GameActivity.this::updateHud); }
            @Override public void onRaceEnded() { runOnUiThread(GameActivity.this::endRaceNow); }
            @Override public void onDisconnected(String reason) {
                runOnUiThread(() -> {
                    say(reason, PixelTheme.ROSE);
                    endRaceNow();
                });
            }
        });
        return true;
    }

    /** Someone cleared the board, or time ran out for everyone. */
    private void endRaceNow() {
        if (session != null && !finished) endRound(false);
    }

    private void confirmLeaveRace() {
        new PixelModal(this).title("LEAVE THE RACE?").icon("close").accent(PixelTheme.ROSE)
                .body("The race carries on without you, and the clock is still running.")
                .primary("KEEP RACING", "play", null)
                .button("LEAVE", PixelTheme.ROSE, PixelTheme.INK, null, Sfx.Id.UI_BACK, this::finish)
                .show();
    }

    private boolean wonRace(int mine) {
        List<LanPlayer> players = race.players();
        if (players.size() < 2) return false;
        for (LanPlayer p : players) if (p.score >= mine && !isMe(p)) return false;
        return true;
    }

    private boolean isMe(LanPlayer p) {
        return p.id.equals(Settings.playerId());
    }

    private void showRaceResults(Outcome o) {
        boolean won = wonRace(o.mine);
        PixelModal m = results(won ? "YOU WIN!" : "RACE OVER", "trophy", won ? PixelTheme.MINT : PixelTheme.SKY, won, 0, "YOUR SCORE", o.mine);
        int place = 1;
        for (LanPlayer p : LanPlayer.ranked(race.players())) {
            m.stat(place == 1 ? "trophy" : "star", place + ". " + p.label(), String.valueOf(p.score),
                    isMe(p) ? PixelTheme.GOLD : PixelTheme.TEXT);
            place++;
        }
        addWinnings(m, o, o.coins);
        addMissed(m);
        m.primary("MENU", this::finish).show();
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

    /** Shares this phone's score as it changes, and shows the race so far, leader first. */
    private void showRaceHud() {
        int mine = session.gridPoints() + session.bonusPoints();
        if (mine != lastRaceScore && !finished) {
            lastRaceScore = mine;
            race.sendScore(mine);
        }
        StringBuilder line = new StringBuilder();
        for (LanPlayer p : LanPlayer.ranked(race.players())) {
            if (line.length() > 0) line.append("   ");
            line.append(isMe(p) ? "YOU" : p.name.toUpperCase(Locale.ROOT)).append(' ').append(p.score);
        }
        duelText.setText(line);
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
        coinChip.animateTo(Math.min(Progress.MAX_COINS, progress.coins + earned));
        if (progress.hintTokens > 0) hintBtn.setBadge("x" + progress.hintTokens);
        else if (progress.coins >= Scoring.HINT_COST_COINS) hintBtn.setBadge(String.valueOf(Scoring.HINT_COST_COINS));
        else hintBtn.setBadge(Features.MONETIZATION ? "AD" : "");

        if (session != null) {
            String words = session.foundGridWords().size() + " of " + level.placements.size() + " words";
            subText.setText(fishCells.isEmpty() ? words : words + "   FISH " + fishFound() + "/" + fishCells.size());
            grid.setContentDescription("Crossword, " + words + " found");
        }
        if (limit() > 0) {
            timerText.setText(clock(secondsLeft));
            timerText.setTextColor(secondsLeft <= URGENT_SECONDS ? PixelTheme.ROSE : PixelTheme.TEXT);
            timerBar.setValue(Math.min(1f, secondsLeft / (float) limit()));
        }
        if (mode == Mode.LAN && session != null) showRaceHud();
        if (mode == Mode.LIVES && run != null) {
            int board = session == null || finished ? 0 : session.score(0); // a finished board is already in the run's score
            duelText.setText("LIVES " + run.lives() + "   SCORE " + (run.score() + board));
        }
        if (mode == Mode.TOWER && run != null) {
            duelText.setText("LIVES " + run.lives() + "   FLOOR " + ((WordTower) run).floor() + "   BEST " + run.best(progress));
        }
        if (mode == Mode.RUSH && run != null) {
            duelText.setText("LIVES " + run.lives() + "   BEATEN " + run.boards());
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

    /** Keeps a callout up until the next one replaces it, for something the player has to do. */
    private void holdBanner(String message, int color) {
        banner.animate().cancel();
        banner.setText(message);
        banner.setTextColor(color);
        banner.setVisibility(View.VISIBLE);
        banner.setAlpha(1f);
        banner.setTranslationY(0f);
        banner.setScaleX(1f);
        banner.setScaleY(1f);
    }

    private void shakeWheel() {
        if (PixelTheme.reduceMotion()) return;
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
        pause.setSpokenName("Pause");
        pause.setIconSize(24);
        pause.setOnClickListener(v -> showPause());
        hud.addView(pause, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setGravity(Gravity.CENTER_HORIZONTAL);
        titleText = PixelTheme.display(this, "", PixelTheme.SIZE_LABEL, PixelTheme.GOLD);
        titleText.setGravity(Gravity.CENTER);
        subText = PixelTheme.body(this, "", PixelTheme.SIZE_BODY, PixelTheme.TEXT);
        subText.setGravity(Gravity.CENTER);
        subText.setPadding(0, dp(4), 0, 0);
        titles.addView(titleText);
        titles.addView(subText);
        hud.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        coinChip = new StatChip(this, "coin");
        coinChip.setSpokenName("Coins");
        coinChip.setValue(progress.coins);
        hud.addView(coinChip);
        col.addView(hud, wrapWidth(0));

        LinearLayout tr = new LinearLayout(this);
        tr.setGravity(Gravity.CENTER_VERTICAL);
        timerBar = new PixelBar(this);
        timerBar.setSegments(30);
        tr.addView(timerBar, new LinearLayout.LayoutParams(0, dp(18), 1f));
        timerText = PixelTheme.display(this, "", PixelTheme.SIZE_LABEL, PixelTheme.TEXT);
        timerText.setPadding(dp(10), 0, 0, 0);
        tr.addView(timerText);
        timerRow = tr;
        col.addView(tr, wrapWidth(8));

        bossRow = bossRow();
        col.addView(bossRow, wrapWidth(8));

        duelText = PixelTheme.display(this, "", PixelTheme.SIZE_SMALL, PixelTheme.TEXT);
        duelText.setGravity(Gravity.CENTER);
        duelText.setBackground(new PanelDrawable(0xE03A2E6E, Math.max(2f, dp(1))));
        duelText.setPadding(dp(8), dp(8), dp(8), dp(8));
        col.addView(duelText, wrapWidth(8));

        coachText = PixelTheme.display(this, "", PixelTheme.SIZE_SMALL, PixelTheme.GOLD);
        coachText.setGravity(Gravity.CENTER);
        coachText.setLineSpacing(dp(4), 1f);
        coachText.setBackground(new PanelDrawable(0xE03A2E6E, Math.max(2f, dp(1))));
        coachText.setPadding(dp(8), dp(8), dp(8), dp(8));
        coachText.setVisibility(View.GONE);
        col.addView(coachText, wrapWidth(8));

        // Board, with the callout banner floating over its lower edge
        FrameLayout boardBox = new FrameLayout(this);
        grid = new CrosswordView(this);
        grid.setTapListener(this::onBoardTapped);
        grid.setLetterListener(order -> {
            Sfx.play(Sfx.Id.TILE_POP, Math.min(2f, 1f + order * 0.09f), 0.8f);
            Haptics.tick();
        });
        boardBox.addView(grid, fill());
        banner = PixelTheme.display(this, "", PixelTheme.SIZE_LABEL, PixelTheme.MINT);
        banner.setBackground(new PanelDrawable(0xF0251D4A, Math.max(2f, dp(1))));
        banner.setPadding(dp(14), dp(9), dp(14), dp(10));
        banner.setVisibility(View.INVISIBLE);
        boardBox.addView(banner, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));
        meaningCard = new LinearLayout(this);
        meaningCard.setOrientation(LinearLayout.VERTICAL);
        meaningCard.setBackground(new PanelDrawable(PixelTheme.CREAM, Math.max(2f, dp(1))));
        meaningCard.setPadding(dp(10), dp(8), dp(10), dp(9));
        meaningCard.setVisibility(View.GONE);
        meaningCard.setOnClickListener(v -> dismissMeaning());
        meaningWord = PixelTheme.display(this, "", PixelTheme.SIZE_SMALL, PixelTheme.INK);
        meaningText = PixelTheme.body(this, "", PixelTheme.SIZE_BODY, PixelTheme.INK);
        meaningText.setPadding(0, dp(4), 0, 0);
        meaningCard.addView(meaningWord);
        meaningCard.addView(meaningText);
        FrameLayout.LayoutParams meaningLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP);
        boardBox.addView(meaningCard, meaningLp);
        comboText = PixelTheme.display(this, "", PixelTheme.SIZE_TITLE, PixelTheme.GOLD);
        comboText.setShadowLayer(0.01f, dp(2), dp(2), PixelTheme.INK);
        comboText.setVisibility(View.INVISIBLE);
        comboText.setPadding(dp(14), dp(10), dp(14), dp(10));
        boardBox.addView(comboText, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END));
        comboBig = PixelTheme.display(this, "", PixelTheme.SIZE_TITLE, PixelTheme.GOLD);
        comboBig.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, PixelTheme.displayPx(this, PixelTheme.SIZE_TITLE) * 1.7f);
        comboBig.setGravity(Gravity.CENTER);
        comboBig.setShadowLayer(0.01f, dp(3), dp(3), PixelTheme.INK);
        comboBig.setVisibility(View.INVISIBLE);
        boardBox.addView(comboBig, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
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
                wheelCat.nudge();
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

        // The empty middle of the wheel. Not clickable, so swipes pass straight through to the wheel.
        int catSize = (int) (wheelSize * WHEEL_CAT_SHARE);
        wheelCat = new CatView(this, catSize / getResources().getDisplayMetrics().density);
        wheelCat.dress(progress);
        wheelCat.setClickable(false);
        wheelCat.enableNaps();
        wheelBox.addView(wheelCat, new FrameLayout.LayoutParams(catSize, catSize, Gravity.CENTER));

        PixelButton shuffle = new PixelButton(this, "", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
        shuffle.setIcon("shuffle");
        shuffle.setSpokenName("Shuffle letters");
        shuffle.setIconSize(30);
        shuffle.setClickSound(Sfx.Id.SHUFFLE);
        shuffle.setOnClickListener(v -> wheel.shuffle(rnd));
        // The hint is the one players reach for under pressure, so it sits by their thumb.
        boolean leftHanded = Settings.leftHanded();
        wheelBox.addView(shuffle, new FrameLayout.LayoutParams(dp(62), dp(62),
                Gravity.BOTTOM | (leftHanded ? Gravity.END : Gravity.START)));

        hintBtn = new PixelButton(this, "", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
        hintBtn.setIcon("bulb");
        hintBtn.setSpokenName("Hint");
        hintBtn.setIconSize(30);
        hintBtn.setClickSound(null);
        hintBtn.setOnClickListener(v -> onHintPressed());
        wheelBox.addView(hintBtn, new FrameLayout.LayoutParams(dp(62), dp(62),
                Gravity.BOTTOM | (leftHanded ? Gravity.START : Gravity.END)));

        powerBtn = new PixelButton(this, "", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
        powerBtn.setIcon("bolt");
        powerBtn.setSpokenName("Power-ups");
        powerBtn.setIconSize(30);
        powerBtn.setClickSound(null);
        powerBtn.setOnClickListener(v -> onPowerPressed());
        wheelBox.addView(powerBtn, new FrameLayout.LayoutParams(dp(62), dp(62),
                Gravity.TOP | (leftHanded ? Gravity.START : Gravity.END)));

        col.addView(wheelBox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, wheelSize));

        contentLayer.addView(col, fill());

    }

    /** The boss's face, its name and its health, above the board. */
    private LinearLayout bossRow() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(new PanelDrawable(0xE03A2E6E, Math.max(2f, dp(1))));
        row.setPadding(dp(8), dp(6), dp(10), dp(8));
        row.setVisibility(View.GONE);
        bossFace = new FrameLayout(this);
        row.addView(bossFace, new LinearLayout.LayoutParams(dp(BOSS_FACE_DP), dp(BOSS_FACE_DP)));
        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        bossName = PixelTheme.display(this, "", PixelTheme.SIZE_CAPTION, PixelTheme.ROSE);
        text.addView(bossName);
        bossBar = new PixelBar(this);
        bossBar.setColor(PixelTheme.ROSE);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(14));
        barLp.topMargin = dp(6);
        text.addView(bossBar, barLp);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textLp.leftMargin = dp(10);
        row.addView(text, textLp);
        return row;
    }

    private LinearLayout.LayoutParams wrapWidth(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(top);
        return p;
    }
}
