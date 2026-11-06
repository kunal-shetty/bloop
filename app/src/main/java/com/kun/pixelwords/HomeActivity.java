package com.kun.pixelwords;

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
import com.kun.pixelwords.game.Home;
import com.kun.pixelwords.game.Progress;
import com.kun.pixelwords.services.PlayGamesService;
import com.kun.pixelwords.ui.AmbientView;
import com.kun.pixelwords.ui.CatView;
import com.kun.pixelwords.ui.HomeCard;
import com.kun.pixelwords.ui.PanelDrawable;
import com.kun.pixelwords.ui.PixelButton;
import com.kun.pixelwords.ui.PixelModal;
import com.kun.pixelwords.ui.PixelTheme;
import com.kun.pixelwords.ui.RoomView;
import com.kun.pixelwords.ui.StatChip;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Furnish the cat's room. Tap an item to buy it, or to put it in or take it out; drag it in the room. */
public class HomeActivity extends BaseActivity {

    private static final int COLUMNS = 3;
    /** The room mustn't push the furniture list off a short screen. */
    private static final float MAX_ROOM_SHARE = 0.42f;
    private static final int CAT_ROOM_X = 44, CAT_ROOM_Y = 62, CAT_ROOM_SIZE = 32;

    private ProgressStore store;
    private Progress progress;
    private RoomView room;
    private CatView cat;
    private StatChip coins;
    private final List<HomeCard> cards = new ArrayList<>();

    @Override
    protected View createBackground() {
        AmbientView ambient = new AmbientView(this);
        ambient.setTheme(AmbientView.Theme.PETALS);
        return ambient;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new ProgressStore(this);
        progress = store.load();
        buildUi();
        fx.preload("ui_confetti");
        coins.setValue(progress.coins);
        refresh();
    }

    private int dp(float v) {
        return (int) PixelTheme.dp(this, v);
    }

    private void buildUi() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(14), dp(12), dp(14), 0);
        page.addView(topBar(), matchWrap(0));
        page.addView(roomBox(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, roomHeight()));

        ScrollView scroll = new ScrollView(this);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(2), 0, dp(16));
        TextView tip = PixelTheme.body(this, "Drag furniture around the room. Tap an item you own to put it away.", 16, PixelTheme.TEXT_DIM);
        tip.setPadding(0, dp(6), 0, 0);
        list.addView(tip);
        for (Home.Slot slot : Home.Slot.values()) addSlot(list, slot);
        scroll.addView(list);
        page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        contentLayer.addView(page, fill());
    }

    private int roomHeight() {
        int width = getResources().getDisplayMetrics().widthPixels - dp(28);
        int tallest = (int) (getResources().getDisplayMetrics().heightPixels * MAX_ROOM_SHARE);
        return Math.min(width, tallest);
    }

    private View topBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        PixelButton back = new PixelButton(this, "", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
        back.setIcon("back");
        back.setIconSize(24);
        back.setClickSound(Sfx.Id.UI_BACK);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView title = PixelTheme.display(this, "CAT HOME", 14, PixelTheme.GOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        coins = new StatChip(this, "coin");
        bar.addView(coins);
        return bar;
    }

    private View roomBox() {
        FrameLayout box = new FrameLayout(this);
        box.setPadding(0, dp(8), 0, 0);
        room = new RoomView(this);
        room.show(progress);
        room.setOnMoved(this::save);
        box.addView(room, fill());
        cat = new CatView(this, 1); // sized to the room once it's laid out
        cat.dress(progress);
        box.addView(cat, new FrameLayout.LayoutParams(0, 0));
        room.post(this::placeCat);
        return box;
    }

    /** One art pixel of the cat matches one of the room, so the two look like the same world. */
    private void placeCat() {
        int size = CAT_ROOM_SIZE * room.scale();
        FrameLayout box = (FrameLayout) cat.getParent();
        box.removeView(cat);
        cat = new CatView(this, size / getResources().getDisplayMetrics().density);
        cat.dress(progress);
        cat.setTranslationX(room.screenX(CAT_ROOM_X));
        cat.setTranslationY(room.screenY(CAT_ROOM_Y));
        box.addView(cat, new FrameLayout.LayoutParams(size, size));
    }

    private void addSlot(LinearLayout list, Home.Slot slot) {
        TextView heading = PixelTheme.display(this, slot.title.toUpperCase(Locale.ROOT), 10, PixelTheme.CREAM);
        heading.setBackground(new PanelDrawable(0xE0251D4A, Math.max(2f, dp(1))));
        heading.setPadding(dp(10), dp(8), dp(10), dp(9));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hp.topMargin = dp(10);
        hp.bottomMargin = dp(6);
        list.addView(heading, hp);

        LinearLayout row = new LinearLayout(this);
        boolean first = true;
        for (Home.Item item : Home.itemsIn(slot)) {
            HomeCard card = new HomeCard(this, item);
            card.setClickSound(null);
            card.setOnClickListener(v -> onItem(card.item));
            cards.add(card);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (!first) lp.leftMargin = dp(8);
            first = false;
            row.addView(card, lp);
        }
        list.addView(row);
    }

    private LinearLayout.LayoutParams matchWrap(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(top);
        return p;
    }

    private void refresh() {
        coins.animateTo(progress.coins);
        room.show(progress);
        for (HomeCard c : cards) c.update(progress);
    }

    private void save() {
        store.save(progress);
        if (PlayGamesService.isConfigured(this)) PlayGamesService.saveProgress(this, progress.serialize());
    }

    private void onItem(Home.Item item) {
        if (!Home.owns(progress, item)) {
            Sfx.play(Sfx.Id.UI_TAP);
            confirmBuy(item);
            return;
        }
        Sfx.play(Sfx.Id.UI_TOGGLE);
        if (Home.placed(progress, item.slot) == item) Home.remove(progress, item.slot);
        else Home.place(progress, item);
        save();
        refresh();
        cat.hop();
    }

    private void confirmBuy(Home.Item item) {
        if (!Home.canAfford(progress, item)) {
            new PixelModal(this).title(item.title.toUpperCase(Locale.ROOT)).icon("coin").accent(PixelTheme.ROSE)
                    .stat("coin", "Price", String.valueOf(item.price), PixelTheme.GOLD)
                    .stat("coin", "Your coins", String.valueOf(progress.coins), PixelTheme.ROSE)
                    .note("You need " + (item.price - progress.coins) + " more coins. Levels, quests and achievements all pay coins.")
                    .primary("KEEP PLAYING", this::finish)
                    .secondary("CLOSE", null)
                    .show();
            return;
        }
        new PixelModal(this).title("BUY THIS?").icon("home").accent(PixelTheme.GOLD)
                .body(item.title + " goes straight into the room, and you can move it wherever you like.")
                .stat("coin", "Price", String.valueOf(item.price), PixelTheme.GOLD)
                .stat("coin", "Coins left after", String.valueOf(progress.coins - item.price), PixelTheme.TEXT)
                .button("BUY FOR " + item.price, PixelTheme.GOLD, PixelTheme.INK, "coin", null, () -> buy(item))
                .secondary("NOT NOW", null)
                .show();
    }

    private void buy(Home.Item item) {
        if (!Home.buy(progress, item)) return;
        save();
        refresh();
        Sfx.play(Sfx.Id.BUY);
        Haptics.celebrate();
        cat.hop();
        fx.play("ui_confetti", fx.getWidth() / 2f, fx.getHeight() * 0.2f, PixelTheme.dp(this, 2.4f), null);
    }
}
