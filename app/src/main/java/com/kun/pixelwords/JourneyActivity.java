package com.kun.pixelwords;

import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.kun.pixelwords.audio.Haptics;
import com.kun.pixelwords.audio.Sfx;
import com.kun.pixelwords.data.ProgressStore;
import com.kun.pixelwords.game.Journey;
import com.kun.pixelwords.game.Progress;
import com.kun.pixelwords.game.Scoring;
import com.kun.pixelwords.services.PlayGamesService;
import com.kun.pixelwords.ui.AmbientView;
import com.kun.pixelwords.ui.CatView;
import com.kun.pixelwords.ui.JourneyMapView;
import com.kun.pixelwords.ui.PanelDrawable;
import com.kun.pixelwords.ui.PixelButton;
import com.kun.pixelwords.ui.PixelModal;
import com.kun.pixelwords.ui.PixelTheme;
import com.kun.pixelwords.ui.StatChip;

/** The journey: a chapter at a time, with the cat on the next level. Cleared levels can be replayed. */
public class JourneyActivity extends BaseActivity {

    private static final float CAT_DP = 68;

    private ProgressStore store;
    private Progress progress;
    private AmbientView ambient;
    private ScrollView scroll;
    private JourneyMapView map;
    private CatView cat;
    private StatChip coins;
    private TextView chapterText, chapterName;
    private PixelButton prevBtn, nextBtn;
    private int chapter;
    private int lastLevel;

    @Override
    protected View createBackground() {
        ambient = new AmbientView(this);
        return ambient;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new ProgressStore(this);
        fx.preload("ui_confetti");
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        progress = store.load();
        coins.setValue(progress.coins);
        if (progress.level != lastLevel) { // first opening, or the player just moved on
            lastLevel = progress.level;
            showChapter(Journey.chapterOf(progress.level));
        } else {
            map.show(chapter, progress); // a replay may have earned stars
        }
        payChapterRewards();
    }

    private int dp(float v) {
        return (int) PixelTheme.dp(this, v);
    }

    private void buildUi() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(14), dp(12), dp(14), 0);
        page.addView(topBar(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        map = new JourneyMapView(this);
        map.setListener(new JourneyMapView.Listener() {
            @Override public void onLevel(int level) { onLevelTapped(level); }

            @Override public void onTrophy() { showTrophy(); }
        });
        cat = new CatView(this, CAT_DP);
        cat.setClickable(false);

        FrameLayout board = new FrameLayout(this);
        board.addView(map, fill());
        board.addView(cat, new FrameLayout.LayoutParams(dp(CAT_DP), dp(CAT_DP)));
        scroll = new ScrollView(this);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);
        // Without this, closing a modal hands focus to the map and the scroll view jumps to it.
        scroll.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        scroll.addView(board);
        page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        contentLayer.addView(page, fill());
    }

    private View topBar() {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        PixelButton back = new PixelButton(this, "", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
        back.setIcon("close");
        back.setIconSize(22);
        back.setClickSound(Sfx.Id.UI_BACK);
        back.setOnClickListener(v -> finish());
        row.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView title = PixelTheme.display(this, "JOURNEY", 12, PixelTheme.GOLD);
        title.setGravity(Gravity.CENTER);
        row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        coins = new StatChip(this, "coin");
        row.addView(coins);
        column.addView(row);

        LinearLayout pager = new LinearLayout(this);
        pager.setGravity(Gravity.CENTER_VERTICAL);
        pager.setBackground(new PanelDrawable(0xE03A2E6E, Math.max(2f, dp(1))));
        pager.setPadding(dp(6), dp(6), dp(6), dp(8));
        prevBtn = arrow(-1);
        pager.addView(prevBtn, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setGravity(Gravity.CENTER);
        chapterText = PixelTheme.body(this, "", 16, PixelTheme.TEXT_DIM);
        chapterName = PixelTheme.display(this, "", 11, PixelTheme.CREAM);
        chapterName.setPadding(0, dp(4), 0, 0);
        chapterText.setGravity(Gravity.CENTER);
        chapterName.setGravity(Gravity.CENTER);
        names.addView(chapterText);
        names.addView(chapterName);
        pager.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        nextBtn = arrow(1);
        nextBtn.setScaleX(-1f); // the back arrow, mirrored
        pager.addView(nextBtn, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pp.topMargin = dp(10);
        column.addView(pager, pp);
        return column;
    }

    private PixelButton arrow(int direction) {
        PixelButton b = new PixelButton(this, "", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
        b.setIcon("back");
        b.setIconSize(22);
        b.setOnClickListener(v -> showChapter(chapter + direction));
        return b;
    }

    /** You can look one chapter ahead of where you are, to see what's coming. */
    private void showChapter(int target) {
        chapter = target;
        int furthest = Journey.chapterOf(progress.level) + 1;
        prevBtn.setVisibility(chapter > 0 ? View.VISIBLE : View.INVISIBLE);
        nextBtn.setVisibility(chapter < furthest ? View.VISIBLE : View.INVISIBLE);
        chapterText.setText("Chapter " + (chapter + 1));
        chapterName.setText(Journey.chapterName(chapter));
        ambient.setTheme(AmbientView.Theme.values()[Journey.themeIndex(chapter)]);
        cat.dress(progress);
        map.show(chapter, progress);
        map.post(this::placeCatAndScroll);
    }

    private void placeCatAndScroll() {
        boolean here = Journey.chapterOf(progress.level) == chapter;
        cat.setVisibility(here ? View.VISIBLE : View.GONE);
        if (!here) {
            boolean finished = chapter < Journey.chapterOf(progress.level);
            scroll.scrollTo(0, finished ? 0 : map.getHeight()); // the trophy, or level one of what's ahead
            return;
        }
        PointF at = map.centerOf(progress.level);
        float size = cat.getLayoutParams().height;
        cat.setTranslationX(at.x - size / 2f);
        cat.setTranslationY(at.y - map.nodeSize() / 2f - size * 0.86f);
        scroll.scrollTo(0, (int) (at.y - scroll.getHeight() / 2f));
    }

    private void onLevelTapped(int level) {
        Sfx.play(Sfx.Id.UI_TAP);
        Haptics.click();
        if (level > progress.level) {
            new PixelModal(this).title("LOCKED").icon("close").accent(PixelTheme.TEXT_DIM)
                    .body("Clear level " + progress.level + " first, and keep following the path.")
                    .primary("GOT IT", null)
                    .show();
        } else if (level < progress.level) {
            showReplay(level);
        } else if (Journey.isHard(level)) {
            showHardLevel(level);
        } else {
            play(0);
        }
    }

    private void showReplay(int level) {
        PixelModal m = new PixelModal(this).title("LEVEL " + level).icon("star").accent(PixelTheme.GOLD)
                .stat("star", "Your best", Journey.starsShown(progress, level) + " / " + Journey.MAX_STARS, PixelTheme.GOLD);
        if (Journey.isHard(level)) m.stat("flame", "Difficulty", "HARD", PixelTheme.ROSE);
        m.note("Replays can win you more stars, but they don't pay coins. Clear a level with no hints for three stars.")
                .primary("REPLAY", "play", () -> play(level))
                .secondary("CLOSE", null)
                .show();
    }

    private void showHardLevel(int level) {
        new PixelModal(this).title("HARD LEVEL").icon("flame").accent(PixelTheme.ROSE)
                .body("Longer words and a bigger wheel. Are you ready?")
                .stat("coin", "Reward", "+" + Journey.coinsForLevel(level), PixelTheme.GOLD)
                .primary("BRING IT ON", "play", () -> play(0))
                .secondary("NOT YET", null)
                .show();
    }

    private void showTrophy() {
        Sfx.play(Sfx.Id.UI_TAP);
        Haptics.click();
        boolean won = Journey.chaptersFinished(progress) > chapter;
        new PixelModal(this).title(Journey.chapterName(chapter)).icon("trophy").accent(PixelTheme.GOLD)
                .body(won ? "You finished this chapter!" : "Clear level " + Journey.lastLevel(chapter) + " to win this reward.")
                .stat("coin", "Coins", "+" + Scoring.COINS_PER_CHAPTER, PixelTheme.GOLD)
                .stat("bulb", "Hint tokens", "+" + Scoring.HINTS_PER_CHAPTER, PixelTheme.GOLD)
                .primary("GOT IT", null)
                .show();
    }

    private void payChapterRewards() {
        int chapters = Journey.payChapterRewards(progress);
        if (chapters == 0 || modalShowing()) {
            if (chapters > 0) store.save(progress);
            return;
        }
        store.save(progress);
        if (PlayGamesService.isConfigured(this)) PlayGamesService.saveProgress(this, progress.serialize());
        coins.animateTo(progress.coins);
        Sfx.play(Sfx.Id.LEVEL_COMPLETE);
        Haptics.celebrate();
        fx.play("ui_confetti", fx.getWidth() / 2f, fx.getHeight() * 0.3f, PixelTheme.dp(this, 3), null);
        new PixelModal(this).title(chapters == 1 ? "CHAPTER COMPLETE!" : chapters + " CHAPTERS DONE!").icon("trophy").accent(PixelTheme.GOLD)
                .body("Your cat is very proud of you.")
                .stat("coin", "Coins", "+" + chapters * Scoring.COINS_PER_CHAPTER, PixelTheme.GOLD)
                .stat("bulb", "Hint tokens", "+" + chapters * Scoring.HINTS_PER_CHAPTER, PixelTheme.GOLD)
                .primary("KEEP GOING", null)
                .show();
    }

    /** 0 plays the next level; anything else replays that cleared level. */
    private void play(int replayLevel) {
        Intent i = new Intent(this, GameActivity.class);
        i.putExtra(GameActivity.EXTRA_MODE, GameActivity.Mode.CAMPAIGN.name());
        i.putExtra(GameActivity.EXTRA_REPLAY_LEVEL, replayLevel);
        go(i);
    }
}
