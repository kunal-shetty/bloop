package com.kun.pixelwords;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.kun.pixelwords.audio.Sfx;
import com.kun.pixelwords.data.Days;
import com.kun.pixelwords.game.Journey;
import com.kun.pixelwords.game.MegaBoard;
import com.kun.pixelwords.game.StreakLogic;
import com.kun.pixelwords.services.PlayGamesService;
import com.kun.pixelwords.ui.AmbientView;
import com.kun.pixelwords.ui.ModeCard;
import com.kun.pixelwords.ui.PixelButton;
import com.kun.pixelwords.ui.PixelModal;
import com.kun.pixelwords.ui.PixelTheme;

import java.util.Locale;

/** Everything there is to play, in one place: the Journey first, then every other mode. */
public class PlayActivity extends PageActivity {

    private PixelButton journey;
    private ModeCard dailyCard, timedCard, duelCard, ranksCard, livesCard, megaCard;

    @Override
    protected String pageTitle() {
        return "PLAY";
    }

    @Override
    protected AmbientView.Theme pageTheme() {
        return AmbientView.Theme.FIREFLIES;
    }

    @Override
    protected void fillPage(LinearLayout list) {
        journey = new PixelButton(this, "JOURNEY", PixelTheme.GOLD, PixelTheme.INK);
        journey.setLabelSize(20);
        journey.setIcon("play");
        journey.setIconSize(30);
        journey.setShine(true);
        journey.setOnClickListener(v -> go(new Intent(this, JourneyActivity.class)));
        list.addView(journey, matchWrap(10));

        dailyCard = new ModeCard(this, "calendar", "DAILY", PixelTheme.MINT);
        dailyCard.setOnClickListener(v -> onDaily());
        timedCard = new ModeCard(this, "clock", "TIMED", PixelTheme.SKY);
        timedCard.setOnClickListener(v -> onTimed());
        livesCard = new ModeCard(this, "heart", "9 LIVES", 0xFFFF9A3C);
        livesCard.setOnClickListener(v -> launch(GameActivity.Mode.LIVES));
        megaCard = new ModeCard(this, "star", "MEGA", PixelTheme.GOLD);
        megaCard.setOnClickListener(v -> onMega());
        duelCard = new ModeCard(this, "swords", "DUEL", PixelTheme.ROSE);
        duelCard.setOnClickListener(v -> onDuel());
        ranksCard = new ModeCard(this, "trophy", "RANKS", 0xFFC9A6FF);
        ranksCard.setOnClickListener(v -> onLeaderboard());

        list.addView(cardRow(dailyCard, timedCard), matchWrap(10));
        list.addView(cardRow(livesCard, megaCard), matchWrap(10));
        list.addView(cardRow(duelCard, ranksCard), matchWrap(10));
    }

    private View cardRow(View a, View b) {
        LinearLayout row = new LinearLayout(this);
        row.addView(a, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        rp.leftMargin = dp(10);
        row.addView(b, rp);
        return row;
    }

    @Override
    protected void refreshPage() {
        long today = Days.today();
        journey.setSubtitle(Journey.chapterName(Journey.chapterOf(progress.level)) + " - LEVEL " + progress.level
                + "  (" + Journey.slotOf(progress.level) + "/" + Journey.LEVELS_PER_CHAPTER + ")");
        boolean doneToday = progress.streak.lastDay == today;
        dailyCard.setStatus(doneToday ? "Solved!" : "New puzzle");
        dailyCard.setBadge(doneToday ? "" : "NEW");
        dailyCard.setFill(doneToday ? 0xFF4FB488 : PixelTheme.MINT);
        timedCard.setStatus(progress.bestTimed > 0 ? "Best " + progress.bestTimed : "2:00 sprint");
        livesCard.setStatus(progress.bestLives > 0 ? "Best " + progress.bestLives : "Survive!");
        boolean megaReady = MegaBoard.isOpen(today) && !MegaBoard.isSolved(progress, today);
        int wait = MegaBoard.daysUntilOpen(today);
        megaCard.setStatus(MegaBoard.isSolved(progress, today) ? "Solved!" : megaReady ? "Open now"
                : "In " + days(wait));
        megaCard.setBadge(megaReady ? "NEW" : "");
        duelCard.setStatus(progress.duelsPlayed > 0 ? MainActivity.wins(progress.duelWins) : "Beat the bot");
        ranksCard.setStatus("Top players");
    }

    private static String days(int n) {
        return n + (n == 1 ? " day" : " days");
    }

    private void launch(GameActivity.Mode mode) {
        Intent i = new Intent(this, GameActivity.class);
        i.putExtra(GameActivity.EXTRA_MODE, mode.name());
        go(i);
    }

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

    private void onMega() {
        long today = Days.today();
        if (MegaBoard.isSolved(progress, today)) {
            new PixelModal(this).title("MEGA BOARD").icon("star").accent(PixelTheme.MINT)
                    .body("You've solved this weekend's board. A new one opens next Saturday.")
                    .primary("OK", null)
                    .show();
        } else if (!MegaBoard.isOpen(today)) {
            new PixelModal(this).title("MEGA BOARD").icon("star").accent(PixelTheme.GOLD)
                    .body("A giant board opens every weekend. Your progress is saved, so you can solve it over both days.")
                    .stat("calendar", "Opens in", days(MegaBoard.daysUntilOpen(today)), PixelTheme.GOLD)
                    .stat("coin", "Reward", String.valueOf(MegaBoard.COINS), PixelTheme.GOLD)
                    .primary("OK", null)
                    .show();
        } else {
            launch(GameActivity.Mode.MEGA);
        }
    }

    private void onDuel() {
        new PixelModal(this).title("WORD DUEL").icon("swords").accent(PixelTheme.ROSE)
                .body("You and the bot race on the same board for 90 seconds. Highest score when time runs out wins.")
                .stat("clock", "Time limit", "1:30", PixelTheme.SKY)
                .stat("coin", "Win reward", "+20", PixelTheme.GOLD)
                .stat("trophy", "Your record", MainActivity.wins(progress.duelWins).toUpperCase(Locale.ROOT) + " / " + progress.duelsPlayed, PixelTheme.TEXT)
                .note("Or race up to three friends on the same Wi-Fi.")
                .primary("FIGHT THE BOT", "swords", () -> launch(GameActivity.Mode.DUEL))
                .button("RACE ON WI-FI", PixelTheme.SKY, PixelTheme.INK, "cloud", Sfx.Id.UI_TAP,
                        () -> go(new Intent(this, LanActivity.class)))
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
                .stat("heart", "Nine Lives", progress.bestLives > 0 ? String.valueOf(progress.bestLives) : "-", PixelTheme.ROSE)
                .stat("swords", "Duels won", String.valueOf(progress.duelWins), PixelTheme.ROSE)
                .stat("flame", "Best streak", days(progress.streak.best), PixelTheme.ROSE)
                .stat("book", "Words found", String.valueOf(progress.totalWordsFound), PixelTheme.MINT)
                .primary("GOT IT", null)
                .show();
    }
}
