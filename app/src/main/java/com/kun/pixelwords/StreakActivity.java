package com.kun.pixelwords;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kun.pixelwords.audio.Haptics;
import com.kun.pixelwords.audio.Sfx;
import com.kun.pixelwords.data.Days;
import com.kun.pixelwords.game.DailyArchive;
import com.kun.pixelwords.game.StreakLogic;
import com.kun.pixelwords.game.StreakMilestone;
import com.kun.pixelwords.ui.AmbientView;
import com.kun.pixelwords.ui.PanelDrawable;
import com.kun.pixelwords.ui.PixelBar;
import com.kun.pixelwords.ui.PixelButton;
import com.kun.pixelwords.ui.PixelIcons;
import com.kun.pixelwords.ui.PixelModal;
import com.kun.pixelwords.ui.PixelTheme;

import java.util.Calendar;
import java.util.Locale;

/**
 * The streak as something to look after: a flame that grows, the week at a glance, freeze shields, a
 * countdown to midnight, and a trail of milestones to walk. Everything on it can be tapped.
 */
public class StreakActivity extends PageActivity {

    private static final String[] WEEKDAYS = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};

    private View flame;
    private ValueAnimator pulse;
    private TextView count, title, status;
    private PixelButton playButton;
    private LinearLayout week, shields, trail;
    private PixelBar nextBar;
    private TextView nextText;

    @Override
    protected Explain.Topic explainTopic() {
        return Explain.Topic.STREAK;
    }

    @Override
    protected String pageTitle() {
        return "STREAK";
    }

    @Override
    protected AmbientView.Theme pageTheme() {
        return AmbientView.Theme.FIREFLIES;
    }

    @Override
    protected void fillPage(LinearLayout list) {
        DailyArchive.backfill(progress);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setBackground(new PanelDrawable(0xE03A2E6E, Math.max(2f, dp(1))));
        hero.setPadding(dp(12), dp(14), dp(12), dp(14));
        flame = PixelIcons.view(this, "flame");
        flame.setContentDescription("Streak flame. Tap it.");
        flame.setOnClickListener(v -> poke());
        hero.addView(flame, new LinearLayout.LayoutParams(dp(96), dp(96)));
        count = PixelTheme.display(this, "0", PixelTheme.SIZE_TITLE * 2, PixelTheme.ROSE);
        hero.addView(count);
        title = PixelTheme.display(this, "", PixelTheme.SIZE_SMALL, PixelTheme.CREAM);
        title.setPadding(0, dp(4), 0, 0);
        hero.addView(title);
        status = PixelTheme.body(this, "", PixelTheme.SIZE_BODY, PixelTheme.TEXT);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(8), 0, 0);
        hero.addView(status);
        playButton = new PixelButton(this, "PLAY TODAY'S DAILY", PixelTheme.GOLD, PixelTheme.INK);
        playButton.setLabelSize(PixelTheme.SIZE_LABEL);
        playButton.setIcon("calendar");
        playButton.setShine(true);
        playButton.setOnClickListener(v -> startActivity(new Intent(this, GameActivity.class)
                .putExtra(GameActivity.EXTRA_MODE, GameActivity.Mode.DAILY.name())));
        hero.addView(playButton, matchWrap(10));
        list.addView(hero, matchWrap(10));

        addHeading(list, "THIS WEEK", "Tap a day");
        week = new LinearLayout(this);
        list.addView(week, matchWrap(8));

        addHeading(list, "FREEZE SHIELDS", null);
        shields = new LinearLayout(this);
        shields.setGravity(Gravity.CENTER);
        shields.setBackground(new PanelDrawable(0xE0251D4A, Math.max(2f, dp(1))));
        shields.setPadding(dp(8), dp(10), dp(8), dp(10));
        shields.setOnClickListener(v -> explainFreezes());
        list.addView(shields, matchWrap(8));

        addHeading(list, "MILESTONE TRAIL", null);
        nextText = PixelTheme.body(this, "", PixelTheme.SIZE_BODY, PixelTheme.GOLD);
        nextText.setPadding(dp(4), dp(8), dp(4), dp(4));
        list.addView(nextText);
        nextBar = new PixelBar(this);
        nextBar.setSegments(20);
        nextBar.setColor(PixelTheme.ROSE);
        list.addView(nextBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(16)));
        trail = new LinearLayout(this);
        trail.setOrientation(LinearLayout.VERTICAL);
        list.addView(trail, matchWrap(8));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pulse == null) {
            pulse = ValueAnimator.ofFloat(1f, 1.1f);
            pulse.setDuration(700);
            pulse.setRepeatMode(ValueAnimator.REVERSE);
            pulse.setRepeatCount(ValueAnimator.INFINITE);
            pulse.addUpdateListener(a -> {
                float s = (float) a.getAnimatedValue();
                flame.setScaleX(1f + (s - 1f) * 0.5f);
                flame.setScaleY(s);
            });
        }
        if (!com.kun.pixelwords.data.Settings.reduceMotion()) pulse.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (pulse != null) pulse.cancel();
    }

    /** The flame's name grows with the streak. */
    private static String flameName(int streak) {
        if (streak >= 100) return "LEGEND";
        if (streak >= 30) return "INFERNO";
        if (streak >= 7) return "BLAZE";
        if (streak >= 3) return "EMBER";
        if (streak >= 1) return "SPARK";
        return "NO FLAME YET";
    }

    private void poke() {
        Sfx.play(Sfx.Id.UI_TOGGLE);
        Haptics.click();
        fx.play("pickups_collect-burst", flame.getX() + flame.getWidth() / 2f + dp(14),
                flame.getY() + dp(110), PixelTheme.dp(this, 2.2f), null);
        flame.animate().cancel();
        flame.animate().rotation(flame.getRotation() == 0 ? 12f : 0f).setDuration(160).start();
    }

    @Override
    protected void refreshPage() {
        long today = Days.today();
        int streak = StreakLogic.displayStreak(progress.streak, today);
        boolean done = progress.streak.lastDay == today;
        count.setText(String.valueOf(streak));
        title.setText(streak == 0 ? flameName(0) : flameName(streak) + "  -  " + (streak == 1 ? "DAY" : "DAYS") + " IN A ROW");
        count.setTextColor(streak >= 30 ? PixelTheme.GOLD : streak >= 7 ? PixelTheme.ROSE : PixelTheme.TEXT);
        if (done) {
            status.setText("Today's daily is done. Come back after midnight to keep it going.");
            status.setTextColor(PixelTheme.MINT);
        } else if (streak > 0) {
            status.setText(hoursLeft() + " left to keep your " + streak + "-day streak alive!");
            status.setTextColor(PixelTheme.GOLD);
        } else {
            status.setText("Solve a daily to light your first flame.");
            status.setTextColor(PixelTheme.TEXT);
        }
        playButton.setVisibility(done ? View.GONE : View.VISIBLE);
        fillWeek(today);
        fillShields(streak);
        fillTrail();
    }

    private static String hoursLeft() {
        Calendar c = Calendar.getInstance();
        long left = (24 * 60) - (c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE));
        return (left / 60) + "h " + (left % 60) + "m";
    }

    private void fillWeek(long today) {
        week.removeAllViews();
        for (long day = today - 6; day <= today; day++) {
            final long d = day;
            boolean solved = DailyArchive.isSolved(progress, d) || (d == progress.streak.lastDay);
            boolean isToday = d == today;
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setPadding(dp(2), dp(6), dp(2), dp(6));
            cell.setBackground(new PanelDrawable(isToday ? 0xE0584A9A : 0xE0251D4A, Math.max(2f, dp(1))));
            TextView name = PixelTheme.body(this, WEEKDAYS[(int) Math.floorMod(d + 3, 7L)], PixelTheme.SIZE_CAPTION,
                    isToday ? PixelTheme.GOLD : PixelTheme.TEXT_DIM);
            cell.addView(name);
            View icon = PixelIcons.view(this, solved ? "flame" : isToday ? "calendar" : "close");
            icon.setAlpha(solved || isToday ? 1f : 0.35f);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(26), dp(26));
            ip.topMargin = dp(4);
            cell.addView(icon, ip);
            TextView date = PixelTheme.body(this, Days.dayOfMonth(d), PixelTheme.SIZE_CAPTION, PixelTheme.TEXT);
            date.setPadding(0, dp(2), 0, 0);
            cell.addView(date);
            cell.setOnClickListener(v -> onDay(d, solved, isToday));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (day > today - 6) lp.leftMargin = dp(4);
            week.addView(cell, lp);
        }
    }

    private void onDay(long day, boolean solved, boolean isToday) {
        Sfx.play(Sfx.Id.UI_TAP);
        PixelModal m = new PixelModal(this).title(Days.monthAndDay(day).toUpperCase(Locale.ROOT)).icon(solved ? "flame" : "calendar")
                .accent(solved ? PixelTheme.ROSE : PixelTheme.SKY);
        if (solved) {
            m.body("You solved this day's puzzle. Nice!").primary("OK", null);
        } else if (isToday) {
            m.body("Today's puzzle is waiting.").primary("PLAY", "play", () -> playButton.performClick()).secondary("LATER", null);
        } else {
            m.body("You missed this one. You can still play it from the past puzzles, though it won't change your streak.")
                    .primary("PAST PUZZLES", "book", () -> startActivity(new Intent(this, ArchiveActivity.class)))
                    .secondary("CLOSE", null);
        }
        m.show();
    }

    private void fillShields(int streak) {
        shields.removeAllViews();
        for (int i = 0; i < StreakLogic.MAX_FREEZES; i++) {
            View icon = PixelIcons.view(this, "freeze");
            icon.setAlpha(i < progress.streak.freezes ? 1f : 0.25f);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(40), dp(40));
            lp.rightMargin = dp(10);
            shields.addView(icon, lp);
        }
        int toNext = StreakLogic.FREEZE_EVERY_DAYS - (Math.max(0, streak) % StreakLogic.FREEZE_EVERY_DAYS);
        TextView note = PixelTheme.body(this, progress.streak.freezes >= StreakLogic.MAX_FREEZES ? "Shields full!"
                : "Next shield in " + toNext + (toNext == 1 ? " day" : " days"), PixelTheme.SIZE_BODY, PixelTheme.SKY);
        shields.addView(note);
    }

    private void explainFreezes() {
        Sfx.play(Sfx.Id.UI_TAP);
        new PixelModal(this).title("FREEZE SHIELDS").icon("freeze").accent(PixelTheme.SKY)
                .body("A shield covers one missed day, so your streak survives. You earn one every "
                        + StreakLogic.FREEZE_EVERY_DAYS + " days of streak and can hold " + StreakLogic.MAX_FREEZES + ".")
                .stat("freeze", "Shields held", progress.streak.freezes + " / " + StreakLogic.MAX_FREEZES, PixelTheme.SKY)
                .primary("GOT IT", null)
                .show();
    }

    private void fillTrail() {
        trail.removeAllViews();
        int best = progress.streak.best;
        StreakMilestone next = StreakMilestone.next(progress);
        if (next == null) {
            nextText.setText("Every milestone won. You're a legend!");
            nextBar.setValue(1f);
        } else {
            int current = Math.min(best, next.days);
            nextText.setText("Next: " + next.days + " days  (" + (next.days - best) + " to go)");
            nextBar.animateTo(current / (float) next.days);
        }
        for (StreakMilestone goal : StreakMilestone.values()) {
            boolean won = goal.isPaid(progress) || best >= goal.days;
            boolean isNext = goal == next;
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(new PanelDrawable(won ? 0xE02E6B54 : isNext ? 0xE0584A9A : 0xE0251D4A, Math.max(2f, dp(1))));
            row.setPadding(dp(10), dp(8), dp(10), dp(8));
            View node = PixelIcons.view(this, won ? "check" : isNext ? "flame" : "lock");
            node.setAlpha(won || isNext ? 1f : 0.5f);
            row.addView(node, new LinearLayout.LayoutParams(dp(30), dp(30)));
            LinearLayout text = new LinearLayout(this);
            text.setOrientation(LinearLayout.VERTICAL);
            text.setPadding(dp(10), 0, 0, 0);
            text.addView(PixelTheme.display(this, goal.days + " DAYS", PixelTheme.SIZE_SMALL, won ? PixelTheme.MINT : PixelTheme.CREAM));
            text.addView(PixelTheme.body(this, goal.summary(), PixelTheme.SIZE_BODY, PixelTheme.TEXT_DIM));
            row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.setOnClickListener(v -> {
                Sfx.play(Sfx.Id.UI_TAP);
                new PixelModal(this).title(goal.days + "-DAY STREAK").icon(won ? "check" : "flame")
                        .accent(won ? PixelTheme.MINT : PixelTheme.ROSE)
                        .body(won ? "Won! The reward is in your purse." : "Solve the daily " + goal.days + " days in a row to win:")
                        .note(goal.summary())
                        .primary("OK", null)
                        .show();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(6);
            trail.addView(row, lp);
        }
    }
}
