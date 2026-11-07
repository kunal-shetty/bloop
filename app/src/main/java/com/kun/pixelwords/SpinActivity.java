package com.kun.pixelwords;

import android.widget.LinearLayout;

import com.kun.pixelwords.audio.Haptics;
import com.kun.pixelwords.audio.Sfx;
import com.kun.pixelwords.data.Days;
import com.kun.pixelwords.data.Settings;
import com.kun.pixelwords.game.DailySpin;
import com.kun.pixelwords.ui.AmbientView;
import com.kun.pixelwords.ui.PixelButton;
import com.kun.pixelwords.ui.PixelModal;
import com.kun.pixelwords.ui.PixelTheme;
import com.kun.pixelwords.ui.SpinWheelView;

/** One free spin a day, with every slice's odds on show. */
public class SpinActivity extends PageActivity {

    private SpinWheelView wheel;
    private PixelButton spinButton;
    private boolean spinning;

    @Override
    protected String pageTitle() {
        return "DAILY SPIN";
    }

    @Override
    protected AmbientView.Theme pageTheme() {
        return AmbientView.Theme.FIREFLIES;
    }

    @Override
    protected void fillPage(LinearLayout list) {
        wheel = new SpinWheelView(this);
        list.addView(wheel, matchWrap(8));

        spinButton = new PixelButton(this, "SPIN", PixelTheme.GOLD, PixelTheme.INK);
        spinButton.setLabelSize(PixelTheme.SIZE_HEADING);
        spinButton.setIcon("wheel");
        spinButton.setShine(true);
        spinButton.setOnClickListener(v -> spin());
        list.addView(spinButton, matchWrap(10));

        addHeading(list, "THE ODDS", "One spin a day");
        for (DailySpin.Segment s : DailySpin.segments()) {
            list.addView(oddsRow(s.legendary ? "star" : s.reward.icon(),
                    s.legendary ? "Legendary" : s.reward.label(), s.oddsLabel(), s.legendary));
        }
        list.addView(PixelTheme.body(this, "A legendary is guaranteed within " + DailySpin.LEGENDARY_AFTER
                + " spins. Legendaries are the Galaxy Cat and three exclusive accessories.", 16, PixelTheme.TEXT_DIM), matchWrap(8));
    }

    private LinearLayout oddsRow(String icon, String label, String odds, boolean legendary) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(4), dp(10), dp(2));
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(22), dp(22));
        ip.rightMargin = dp(10);
        row.addView(com.kun.pixelwords.ui.PixelIcons.view(this, icon), ip);
        row.addView(PixelTheme.body(this, label, PixelTheme.SIZE_BODY, legendary ? PixelTheme.GOLD : PixelTheme.TEXT),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(PixelTheme.body(this, odds, PixelTheme.SIZE_BODY, legendary ? PixelTheme.GOLD : PixelTheme.TEXT_DIM));
        return row;
    }

    @Override
    protected void refreshPage() {
        boolean ready = DailySpin.canSpin(progress, Days.today());
        spinButton.setText(ready ? "SPIN" : "COME BACK TOMORROW");
        spinButton.setLabelSize(ready ? PixelTheme.SIZE_HEADING : PixelTheme.SIZE_SMALL);
        spinButton.setFill(ready ? PixelTheme.GOLD : PixelTheme.PANEL_LIGHT);
    }

    private void spin() {
        if (spinning) return;
        // Paid and saved before the wheel moves, so leaving during the spin loses nothing.
        DailySpin.Result result = DailySpin.spin(progress, Days.today(), Settings.playerId().hashCode());
        if (result == null) {
            Sfx.play(Sfx.Id.UI_TAP);
            return;
        }
        spinning = true;
        save();
        wheel.spinTo(result.segment, () -> onStopped(result));
    }

    private void onStopped(DailySpin.Result result) {
        spinning = false;
        if (isFinishing()) return;
        coins.animateTo(progress.coins);
        refreshPage();
        celebrate();
        Haptics.celebrate();
        new PixelModal(this).title(result.isLegendary() ? "LEGENDARY!" : "YOU WON").icon(result.paid.icon())
                .accent(PixelTheme.GOLD)
                .body(result.paid.label())
                .note(result.isLegendary() ? "Find it in the shop." : "Come back tomorrow for another spin.")
                .primary("NICE", null)
                .show();
    }
}
