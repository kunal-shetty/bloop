package com.kun.pixelwords;

import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kun.pixelwords.audio.Sfx;
import com.kun.pixelwords.game.Journey;
import com.kun.pixelwords.game.Region;
import com.kun.pixelwords.game.Trophies;
import com.kun.pixelwords.ui.AmbientView;
import com.kun.pixelwords.ui.PanelDrawable;
import com.kun.pixelwords.ui.PixelIcons;
import com.kun.pixelwords.ui.PixelModal;
import com.kun.pixelwords.ui.PixelTheme;

import java.util.ArrayList;
import java.util.List;

/** A trophy for every boss beaten. Tap one to stand it in the cat's room. */
public class TrophiesActivity extends PageActivity {

    private static final int COLUMNS = 2;
    private static final float GAP_DP = 8;

    private final List<LinearLayout> cards = new ArrayList<>();
    private LinearLayout summary;

    @Override
    protected String pageTitle() {
        return "TROPHIES";
    }

    @Override
    protected AmbientView.Theme pageTheme() {
        return AmbientView.Theme.FIREFLIES;
    }

    @Override
    protected void fillPage(LinearLayout list) {
        summary = new LinearLayout(this);
        summary.setOrientation(LinearLayout.VERTICAL);
        list.addView(summary);
        int count = Trophies.count(progress);
        LinearLayout row = null;
        for (int chapter = count - 1; chapter >= 0; chapter--) {
            int i = count - 1 - chapter;
            if (i % COLUMNS == 0) {
                row = new LinearLayout(this);
                list.addView(row, matchWrap((int) GAP_DP));
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i % COLUMNS > 0) lp.leftMargin = dp(GAP_DP);
            row.addView(card(chapter), lp);
        }
        if (count % COLUMNS != 0) row.addView(new LinearLayout(this), new LinearLayout.LayoutParams(0, 1, 1f));
        String hint = count == 0
                ? "Every chapter of the Journey ends with a boss. Beat one to win its trophy."
                : "Beat the boss of chapter " + (count + 1) + " for your next trophy. Tap a trophy to put it in your cat's room.";
        list.addView(PixelTheme.body(this, hint, PixelTheme.SIZE_BODY, PixelTheme.TEXT_DIM), matchWrap(12));
    }

    private LinearLayout card(int chapter) {
        Region region = Region.forChapter(chapter);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(8), dp(10), dp(8), dp(10));
        card.addView(PixelIcons.view(this, PixelIcons.boss(region)), new LinearLayout.LayoutParams(dp(64), dp(64)));
        TextView name = PixelTheme.body(this, region.bossName(), PixelTheme.SIZE_BODY, PixelTheme.CREAM);
        name.setGravity(Gravity.CENTER);
        card.addView(name, matchWrap(6));
        TextView where = PixelTheme.body(this, Journey.chapterName(chapter), PixelTheme.SIZE_BODY, PixelTheme.TEXT_DIM);
        where.setGravity(Gravity.CENTER);
        card.addView(where, matchWrap(2));
        card.setTag(chapter);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> onTrophy(chapter));
        cards.add(card);
        return card;
    }

    @Override
    protected void refreshPage() {
        summary.removeAllViews();
        int count = Trophies.count(progress);
        addHeading(summary, count == 1 ? "1 BOSS BEATEN" : count + " BOSSES BEATEN", null);
        int shown = Trophies.shown(progress);
        for (LinearLayout card : cards) {
            int chapter = (Integer) card.getTag();
            boolean onShow = chapter == shown;
            card.setBackground(new PanelDrawable(onShow ? 0xFF6B5418 : PixelTheme.PANEL, Math.max(2f, dp(1))));
            card.setContentDescription(Region.forChapter(chapter).bossName() + ", " + Journey.chapterName(chapter)
                    + (onShow ? ", in your room" : ""));
        }
    }

    private void onTrophy(int chapter) {
        Region region = Region.forChapter(chapter);
        boolean onShow = chapter == Trophies.shown(progress);
        PixelModal m = new PixelModal(this).title(region.bossName().toUpperCase(java.util.Locale.ROOT)).icon("trophy").accent(PixelTheme.GOLD)
                .body("Beaten in " + Journey.chapterName(chapter) + ".");
        if (onShow) {
            m.note("This trophy stands in your cat's room.").primary("NICE", null);
        } else {
            m.primary("PUT IN ROOM", "home", () -> {
                Trophies.show(progress, chapter);
                save();
                Sfx.play(Sfx.Id.BUY);
                refreshPage();
            }).secondary("CLOSE", null);
        }
        m.show();
    }
}
