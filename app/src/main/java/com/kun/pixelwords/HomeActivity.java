package com.kun.pixelwords;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
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
import com.kun.pixelwords.game.Season;
import com.kun.pixelwords.game.Progress;
import com.kun.pixelwords.services.PhotoProvider;
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
import com.kun.pixelwords.ui.TabBar;

import java.io.IOException;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private final Map<Home.Theme, PixelButton> themeButtons = new EnumMap<>(Home.Theme.class);

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
        TabBar tabs = new TabBar(this);
        page.addView(tabs, matchWrap(0));

        ScrollView scroll = new ScrollView(this);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(2), 0, dp(16));
        TextView tip = PixelTheme.body(this, "Drag furniture around the room. Tap an item you own to put it away.", PixelTheme.SIZE_BODY, PixelTheme.TEXT_DIM);
        tip.setPadding(0, dp(6), 0, 0);
        list.addView(tip);
        addThemes(tabs.add("ROOMS", list));
        addSlots(tabs.add("FLOOR", list), Home.Slot.BED, Home.Slot.RUG);
        addSlots(tabs.add("WALLS", list), Home.Slot.WINDOW, Home.Slot.WALL, Home.Slot.SHELF);
        addSlots(tabs.add("DECOR", list), Home.Slot.PLANT, Home.Slot.LAMP, Home.Slot.TOY);
        scroll.addView(list);
        tabs.attach(scroll);
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
        back.setSpokenName("Back");
        back.setIconSize(24);
        back.setClickSound(Sfx.Id.UI_BACK);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView title = PixelTheme.display(this, "HOME", PixelTheme.SIZE_TITLE, PixelTheme.GOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        coins = new StatChip(this, "coin");
        coins.setSpokenName("Coins");
        bar.addView(coins);
        bar.addView(barButton("trophy", "Trophies", () -> startActivity(new Intent(this, TrophiesActivity.class))), barSpot());
        bar.addView(barButton("share", "Share a photo of the room", this::sharePhoto), barSpot());
        return bar;
    }

    private PixelButton barButton(String icon, String name, Runnable action) {
        PixelButton b = new PixelButton(this, "", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
        b.setIcon(icon);
        b.setSpokenName(name);
        b.setIconSize(24);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private LinearLayout.LayoutParams barSpot() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(46), dp(46));
        lp.leftMargin = dp(6);
        return lp;
    }

    /** How much bigger than one art pixel per screen pixel the shared photo is drawn. */
    private static final int PHOTO_ART_SCALE = 6;

    /** A snapshot of the room with the cat in it, framed with the game's name, sent to any app. */
    private void sharePhoto() {
        FrameLayout box = (FrameLayout) room.getParent();
        if (box.getWidth() == 0) return;
        Bitmap screen = Bitmap.createBitmap(box.getWidth(), box.getHeight(), Bitmap.Config.ARGB_8888);
        box.draw(new Canvas(screen));
        int s = room.scale(), size = Home.ROOM_SIZE * s;
        Bitmap roomOnly = Bitmap.createBitmap(screen, room.getLeft() + room.screenX(0), room.getTop() + room.screenY(0), size, size);
        int art = Home.ROOM_SIZE * PHOTO_ART_SCALE;
        Bitmap big = Bitmap.createScaledBitmap(roomOnly, art, art, false);

        int pad = PHOTO_ART_SCALE * 4, band = PHOTO_ART_SCALE * 14;
        Bitmap photo = Bitmap.createBitmap(art + pad * 2, art + pad * 2 + band, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(photo);
        c.drawColor(PixelTheme.INK);
        Paint paint = new Paint();
        paint.setColor(PixelTheme.GOLD);
        c.drawRect(pad - PHOTO_ART_SCALE, pad - PHOTO_ART_SCALE, pad + art + PHOTO_ART_SCALE, pad + art + PHOTO_ART_SCALE, paint);
        c.drawBitmap(big, pad, pad, null);
        PixelTheme.styleDisplay(paint, band * 0.36f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(PixelTheme.GOLD);
        c.drawText("MY CAT'S HOME", photo.getWidth() / 2f, art + pad * 2 + band * 0.52f, paint);
        paint.setColor(PixelTheme.TEXT_DIM);
        PixelTheme.styleDisplay(paint, band * 0.2f);
        c.drawText("PAWS & PUZZLES", photo.getWidth() / 2f, art + pad * 2 + band * 0.86f, paint);

        Uri uri;
        try {
            uri = PhotoProvider.save(this, photo);
        } catch (IOException e) {
            new PixelModal(this).title("NO PHOTO").icon("close").accent(PixelTheme.ROSE)
                    .body("The photo couldn't be saved. Free up a little space and try again.")
                    .primary("OK", null).show();
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND).setType("image/png")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_TEXT, "Come and see my cat's home in Paws & Puzzles!")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send, "Share your cat's home"));
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

    private void addSlots(LinearLayout section, Home.Slot... slots) {
        for (Home.Slot slot : slots) addSlot(section, slot);
    }

    private void addSlot(LinearLayout list, Home.Slot slot) {
        TextView heading = PixelTheme.display(this, slot.title.toUpperCase(Locale.ROOT), PixelTheme.SIZE_SMALL, PixelTheme.CREAM);
        heading.setBackground(new PanelDrawable(0xE0251D4A, Math.max(2f, dp(1))));
        heading.setPadding(dp(10), dp(8), dp(10), dp(9));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hp.topMargin = dp(10);
        hp.bottomMargin = dp(6);
        list.addView(heading, hp);

        List<Home.Item> items = Home.itemsIn(slot);
        for (int i = 0; i < items.size(); i += COLUMNS) {
            LinearLayout row = new LinearLayout(this);
            for (int c = 0; c < COLUMNS; c++) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                if (c > 0) lp.leftMargin = dp(8);
                if (i + c < items.size()) {
                    HomeCard card = new HomeCard(this, items.get(i + c));
                    card.setClickSound(null);
                    card.setOnClickListener(v -> onItem(card.item));
                    cards.add(card);
                    row.addView(card, lp);
                } else {
                    row.addView(new View(this), lp); // keeps the last row's cards the same width
                }
            }
            LinearLayout.LayoutParams rp = matchWrap(0);
            rp.bottomMargin = dp(8);
            list.addView(row, rp);
        }
    }

    private void addThemes(LinearLayout list) {
        for (Home.Theme theme : Home.Theme.values()) {
            PixelButton b = new PixelButton(this, theme.title.toUpperCase(Locale.ROOT), PixelTheme.PANEL, PixelTheme.TEXT);
            b.setLabelSize(PixelTheme.SIZE_LABEL);
            b.setIcon("home");
            b.setOnClickListener(v -> onTheme(theme));
            themeButtons.put(theme, b);
            LinearLayout.LayoutParams lp = matchWrap(0);
            lp.bottomMargin = dp(8);
            list.addView(b, lp);
        }
    }

    private void onTheme(Home.Theme theme) {
        Sfx.play(Sfx.Id.UI_TAP);
        if (Home.owns(progress, theme)) {
            Home.use(progress, theme);
            save();
            refresh();
            cat.hop();
            return;
        }
        if (!Home.canAfford(progress, theme)) {
            new PixelModal(this).title(theme.title.toUpperCase(Locale.ROOT)).icon("coin").accent(PixelTheme.ROSE)
                    .stat("coin", "Price", String.valueOf(theme.price), PixelTheme.GOLD)
                    .stat("coin", "Your coins", String.valueOf(progress.coins), PixelTheme.ROSE)
                    .primary("OK", null)
                    .show();
            return;
        }
        new PixelModal(this).title("MOVE IN?").icon("home").accent(PixelTheme.GOLD)
                .body("The " + theme.title + " becomes your cat's room. Your furniture comes too.")
                .stat("coin", "Price", String.valueOf(theme.price), PixelTheme.GOLD)
                .stat("coin", "Coins left after", String.valueOf(progress.coins - theme.price), PixelTheme.TEXT)
                .button("BUY FOR " + theme.price, PixelTheme.GOLD, PixelTheme.INK, "coin", null, () -> {
                    if (!Home.buy(progress, theme)) return;
                    celebrateBuy();
                })
                .secondary("NOT NOW", null)
                .show();
    }

    private LinearLayout.LayoutParams matchWrap(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(top);
        return p;
    }

    /** Back from the trophy screen, which may have changed what stands in the room. */
    @Override
    protected void onRestart() {
        super.onRestart();
        progress = store.load();
        cat.dress(progress);
        refresh();
    }

    private void refresh() {
        coins.animateTo(progress.coins);
        room.show(progress);
        for (HomeCard c : cards) c.update(progress);
        Home.Theme current = Home.theme(progress);
        for (Map.Entry<Home.Theme, PixelButton> e : themeButtons.entrySet()) {
            Home.Theme theme = e.getKey();
            boolean owned = Home.owns(progress, theme);
            e.getValue().setFill(theme == current ? 0xFF2FA873 : owned ? PixelTheme.PANEL_LIGHT : PixelTheme.PANEL);
            e.getValue().setSubtitle(theme == current ? "Your room" : owned ? "Owned" : theme.price + " coins");
        }
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
        if (!Home.forSale(item)) {
            new PixelModal(this).title(item.title.toUpperCase(Locale.ROOT)).icon("sparkle").accent(PixelTheme.SKY)
                    .note(Season.EXCLUSIVES_LOCKED ? "Locked for now. A new way to earn it is coming." : "A Season Pass reward.")
                    .primary("OK", null)
                    .show();
            return;
        }
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
        celebrateBuy();
    }

    private void celebrateBuy() {
        save();
        refresh();
        Sfx.play(Sfx.Id.BUY);
        Haptics.celebrate();
        cat.hop();
        fx.play("ui_confetti", fx.getWidth() / 2f, fx.getHeight() * 0.2f, PixelTheme.dp(this, 2.4f), null);
    }
}
