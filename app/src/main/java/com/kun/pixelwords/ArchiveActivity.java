package com.kun.pixelwords;

import android.content.Intent;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.kun.pixelwords.audio.Sfx;
import com.kun.pixelwords.data.Days;
import com.kun.pixelwords.game.DailyArchive;
import com.kun.pixelwords.ui.AmbientView;
import com.kun.pixelwords.ui.PixelButton;
import com.kun.pixelwords.ui.PixelModal;
import com.kun.pixelwords.ui.PixelTheme;

import java.util.ArrayList;
import java.util.List;

/** The last four weeks of daily puzzles, for catching up on the ones that were missed. */
public class ArchiveActivity extends PageActivity {

    private static final int DAYS_PER_ROW = 7;
    private static final float TILE_GAP_DP = 4, TILE_HEIGHT_DP = 54;

    private final List<PixelButton> tiles = new ArrayList<>();
    private final List<Long> days = new ArrayList<>();
    private LinearLayout summary;

    @Override
    protected String pageTitle() {
        return "PAST PUZZLES";
    }

    @Override
    protected AmbientView.Theme pageTheme() {
        return AmbientView.Theme.PETALS;
    }

    @Override
    protected void fillPage(LinearLayout list) {
        DailyArchive.backfill(progress);
        summary = new LinearLayout(this);
        summary.setOrientation(LinearLayout.VERTICAL);
        list.addView(summary);
        days.addAll(DailyArchive.days(Days.today()));
        LinearLayout row = null;
        for (int i = 0; i < days.size(); i++) {
            if (i % DAYS_PER_ROW == 0) {
                row = new LinearLayout(this);
                row.setGravity(Gravity.CENTER_HORIZONTAL);
                list.addView(row, matchWrap((int) TILE_GAP_DP));
            }
            long day = days.get(i);
            PixelButton tile = new PixelButton(this, Days.dayOfMonth(day), PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
            tile.setLabelSize(PixelTheme.SIZE_SMALL);
            tile.setOnClickListener(v -> onDay(day));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(TILE_HEIGHT_DP), 1f);
            if (i % DAYS_PER_ROW > 0) lp.leftMargin = dp(TILE_GAP_DP);
            row.addView(tile, lp);
            tiles.add(tile);
        }
        list.addView(PixelTheme.body(this, "Green is solved. Puzzles get harder towards the weekend, and a catch-up never changes your streak.",
                PixelTheme.SIZE_BODY, PixelTheme.TEXT_DIM), matchWrap(10));
    }

    @Override
    protected void refreshPage() {
        long today = Days.today();
        DailyArchive.backfill(progress);
        summary.removeAllViews();
        addHeading(summary, Days.monthAndDay(days.get(0)) + " - " + Days.monthAndDay(days.get(days.size() - 1)),
                DailyArchive.missed(progress, today) + " to solve");
        for (int i = 0; i < tiles.size(); i++) {
            PixelButton tile = tiles.get(i);
            long day = days.get(i);
            boolean solved = DailyArchive.isSolved(progress, day);
            boolean open = DailyArchive.isOpen(progress, day);
            tile.setFill(solved ? PixelTheme.MINT : open ? PixelTheme.GOLD : PixelTheme.PANEL_LIGHT);
            tile.setTextColor(solved || open ? PixelTheme.INK : PixelTheme.TEXT);
            tile.setSpokenName(Days.monthAndDay(day));
            tile.setContentDescription(Days.monthAndDay(day) + (solved ? ", solved" : open ? ", open" : ", missed"));
        }
    }

    private void onDay(long day) {
        long today = Days.today();
        String name = Days.monthAndDay(day);
        if (DailyArchive.isSolved(progress, day)) {
            new PixelModal(this).title(name).icon("check").accent(PixelTheme.MINT)
                    .body("You solved this one. You can play it again for fun, but it won't pay anything.")
                    .primary("REPLAY", "play", () -> play(day))
                    .secondary("CLOSE", null)
                    .show();
        } else if (DailyArchive.isOpen(progress, day)) {
            play(day);
        } else if (DailyArchive.canOpen(progress, day, today)) {
            new PixelModal(this).title(name).icon("calendar").accent(PixelTheme.GOLD)
                    .body("Catch up on this day's puzzle.")
                    .stat("coin", "To open", String.valueOf(DailyArchive.OPEN_COINS), PixelTheme.GOLD)
                    .stat("coin", "For solving it", "+" + DailyArchive.SOLVE_COINS, PixelTheme.MINT)
                    .primary("OPEN", "play", () -> open(day))
                    .secondary("NOT NOW", null)
                    .show();
        } else {
            new PixelModal(this).title(name).icon("coin").accent(PixelTheme.ROSE)
                    .body("You need " + DailyArchive.OPEN_COINS + " coins to open this puzzle.")
                    .primary("GOT IT", null)
                    .show();
        }
    }

    private void open(long day) {
        if (!DailyArchive.open(progress, day, Days.today())) return;
        Sfx.play(Sfx.Id.BUY);
        save();
        coins.animateTo(progress.coins);
        play(day);
    }

    private void play(long day) {
        goToBoard(new Intent(this, GameActivity.class)
                .putExtra(GameActivity.EXTRA_MODE, GameActivity.Mode.ARCHIVE.name())
                .putExtra(GameActivity.EXTRA_DAY, day));
    }
}
