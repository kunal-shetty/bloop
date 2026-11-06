package com.kun.pixelwords;

import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kun.pixelwords.audio.Haptics;
import com.kun.pixelwords.audio.Sfx;
import com.kun.pixelwords.game.Expedition;
import com.kun.pixelwords.game.Reward;
import com.kun.pixelwords.services.Reminders;
import com.kun.pixelwords.ui.AmbientView;
import com.kun.pixelwords.ui.CatView;
import com.kun.pixelwords.ui.PanelDrawable;
import com.kun.pixelwords.ui.PixelButton;
import com.kun.pixelwords.ui.PixelModal;
import com.kun.pixelwords.ui.PixelTheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Send the cat on a trip, then collect what it brings home. */
public class ExpeditionActivity extends PageActivity {

    private static final long TICK_MS = 1000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<PixelButton> placeButtons = new ArrayList<>();
    private final Runnable tick = this::onTick;
    private TextView status;
    private CatView cat;
    private PixelButton action;
    private PixelButton rush;

    @Override
    protected String pageTitle() {
        return "CAT TRIPS";
    }

    @Override
    protected AmbientView.Theme pageTheme() {
        return AmbientView.Theme.LEAVES;
    }

    @Override
    protected void fillPage(LinearLayout list) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setBackground(new PanelDrawable(0xE03A2E6E, Math.max(2f, dp(1))));
        panel.setPadding(dp(12), dp(10), dp(12), dp(12));
        cat = new CatView(this, 112);
        cat.dress(progress);
        panel.addView(cat);
        status = PixelTheme.body(this, "", 19, PixelTheme.TEXT);
        status.setGravity(Gravity.CENTER);
        panel.addView(status);
        action = new PixelButton(this, "COLLECT", PixelTheme.GOLD, PixelTheme.INK);
        action.setIcon("bag");
        action.setOnClickListener(v -> collect());
        panel.addView(action, matchWrap(8));
        rush = new PixelButton(this, "BRING HOME NOW", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
        rush.setLabelSize(10);
        rush.setIcon("coin");
        rush.setOnClickListener(v -> confirmRush());
        panel.addView(rush, matchWrap(8));
        list.addView(panel, matchWrap(10));

        addHeading(list, "WHERE TO?", null);
        for (Expedition.Place place : Expedition.Place.values()) {
            PixelButton b = new PixelButton(this, place.title.toUpperCase(Locale.ROOT), PixelTheme.SKY, PixelTheme.INK);
            b.setLabelSize(11);
            b.setIcon(place.icon);
            b.setSubtitle(place.hours + (place.hours == 1 ? " hour" : " hours") + ", " + place.coinRange() + " coins");
            b.setOnClickListener(v -> send(place));
            placeButtons.add(b);
            list.addView(b, matchWrap(8));
        }
        list.addView(PixelTheme.body(this, "Longer trips can bring back stickers and hints.", 16, PixelTheme.TEXT_DIM), matchWrap(8));
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    private void onTick() {
        refreshPage();
        handler.postDelayed(tick, TICK_MS);
    }

    @Override
    protected void refreshPage() {
        long now = System.currentTimeMillis();
        boolean away = Expedition.isAway(progress);
        boolean back = Expedition.isBack(progress, now);
        cat.setVisibility(away && !back ? View.GONE : View.VISIBLE);
        action.setVisibility(back ? View.VISIBLE : View.GONE);
        rush.setVisibility(away && !back ? View.VISIBLE : View.GONE);
        for (PixelButton b : placeButtons) {
            b.setFill(away ? PixelTheme.PANEL : PixelTheme.SKY);
            b.setTextColor(away ? PixelTheme.TEXT_DIM : PixelTheme.INK);
        }
        if (!away) {
            status.setText("Your cat is ready for an adventure.");
        } else if (back) {
            status.setText("Back from " + Expedition.place(progress).title + "!");
        } else {
            status.setText("Exploring " + Expedition.place(progress).title + ", back in "
                    + clock(Expedition.millisLeft(progress, now)));
            rush.setSubtitle(Expedition.rushCost(progress, now) + " coins");
        }
    }

    private static String clock(long millis) {
        long seconds = (millis + 999) / 1000;
        return String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
    }

    private void send(Expedition.Place place) {
        if (!Expedition.start(progress, place, System.currentTimeMillis())) {
            Sfx.play(Sfx.Id.UI_TAP);
            return;
        }
        save();
        Reminders.scheduleTripHome(this, progress.expEnd);
        Sfx.play(Sfx.Id.MEOW);
        refreshPage();
    }

    private void confirmRush() {
        long now = System.currentTimeMillis();
        int cost = Expedition.rushCost(progress, now);
        if (progress.coins < cost) {
            new PixelModal(this).title("NOT ENOUGH COINS").icon("coin").accent(PixelTheme.ROSE)
                    .stat("coin", "Cost", String.valueOf(cost), PixelTheme.GOLD)
                    .stat("coin", "Your coins", String.valueOf(progress.coins), PixelTheme.ROSE)
                    .primary("OK", null)
                    .show();
            return;
        }
        new PixelModal(this).title("BRING HOME NOW?").icon("paw").accent(PixelTheme.GOLD)
                .stat("coin", "Cost", String.valueOf(cost), PixelTheme.GOLD)
                .button("PAY " + cost, PixelTheme.GOLD, PixelTheme.INK, "coin", null, this::rush)
                .secondary("LET IT EXPLORE", null)
                .show();
    }

    private void rush() {
        if (!Expedition.rush(progress, System.currentTimeMillis())) return;
        save();
        Reminders.cancelTripHome(this);
        coins.animateTo(progress.coins);
        refreshPage();
    }

    private void collect() {
        List<Reward> paid = Expedition.collect(progress, System.currentTimeMillis());
        if (paid.isEmpty()) return;
        save();
        coins.animateTo(progress.coins);
        celebrate();
        Haptics.celebrate();
        cat.hop();
        refreshPage();
        PixelModal m = new PixelModal(this).title("SOUVENIRS!").icon("paw").accent(PixelTheme.GOLD);
        for (Reward r : paid) m.stat(r.icon(), r.label(), "", PixelTheme.GOLD);
        m.primary("NICE", null).show();
    }
}
