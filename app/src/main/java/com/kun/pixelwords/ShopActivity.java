package com.kun.pixelwords;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.kun.pixelwords.audio.Haptics;
import com.kun.pixelwords.audio.Sfx;
import com.kun.pixelwords.data.Days;
import com.kun.pixelwords.data.ProgressStore;
import com.kun.pixelwords.game.AdPolicy;
import com.kun.pixelwords.game.Progress;
import com.kun.pixelwords.game.Purchases;
import com.kun.pixelwords.game.Scoring;
import com.kun.pixelwords.game.Shop;
import com.kun.pixelwords.services.AdsService;
import com.kun.pixelwords.services.BillingService;
import com.kun.pixelwords.services.PlayGamesService;
import com.kun.pixelwords.ui.AmbientView;
import com.kun.pixelwords.ui.CatView;
import com.kun.pixelwords.ui.ItemCard;
import com.kun.pixelwords.ui.PanelDrawable;
import com.kun.pixelwords.ui.PixelButton;
import com.kun.pixelwords.ui.PixelModal;
import com.kun.pixelwords.ui.PixelTheme;
import com.kun.pixelwords.ui.StatChip;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Spend coins on accessories for the cat, or get more coins. Tap an owned item to put it on or take it off.
 */
public class ShopActivity extends BaseActivity implements BillingService.Listener {

    private static final int COLUMNS = 3;

    private ProgressStore store;
    private Progress progress;
    private CatView cat;
    private StatChip coins;
    private final List<ItemCard> cards = new ArrayList<>();
    private final Map<Purchases.Pack, PixelButton> packButtons = new EnumMap<>(Purchases.Pack.class);
    private final Map<Shop.Breed, PixelButton> breedButtons = new EnumMap<>(Shop.Breed.class);
    private PixelButton removeAdsButton;
    private PixelButton videoButton;
    private BillingService billing;

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
        billing = BillingService.get(this);
        billing.setListener(this); // stays set behind Play's payment sheet, so the result isn't missed
        AdsService.get(this).load();
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        progress = store.load();
        refresh();
        billing.refresh();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        billing.setListener(null);
    }

    private int dp(float v) {
        return (int) PixelTheme.dp(this, v);
    }

    private void buildUi() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(14), dp(12), dp(14), 0);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        PixelButton back = new PixelButton(this, "", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
        back.setIcon("back");
        back.setIconSize(24);
        back.setClickSound(Sfx.Id.UI_BACK);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        android.widget.TextView title = PixelTheme.display(this, "CAT SHOP", 14, PixelTheme.GOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        coins = new StatChip(this, "coin");
        bar.addView(coins);
        page.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        cat = new CatView(this, 176);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(176));
        cp.topMargin = dp(8);
        page.addView(cat, cp);

        ScrollView scroll = new ScrollView(this);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(4), 0, dp(16));
        if (Features.MONETIZATION) {
            addHeading(list, "GET COINS");
            addStore(list);
        }
        addHeading(list, "BREEDS");
        addBreeds(list);
        for (Shop.Slot slot : Shop.Slot.values()) {
            addHeading(list, slot.title.toUpperCase(java.util.Locale.ROOT));
            addRows(list, Shop.itemsIn(slot));
        }
        scroll.addView(list);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        page.addView(scroll, sp);
        contentLayer.addView(page, fill());
    }

    private void addHeading(LinearLayout list, String text) {
        android.widget.TextView heading = PixelTheme.display(this, text, 10, PixelTheme.CREAM);
        heading.setBackground(new PanelDrawable(0xE0251D4A, Math.max(2f, dp(1))));
        heading.setPadding(dp(10), dp(8), dp(10), dp(9));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hp.topMargin = dp(10);
        hp.bottomMargin = dp(6);
        list.addView(heading, hp);
    }

    private void addBreeds(LinearLayout list) {
        for (Shop.Breed breed : Shop.Breed.values()) {
            PixelButton b = new PixelButton(this, breed.title.toUpperCase(java.util.Locale.ROOT), PixelTheme.PANEL, PixelTheme.TEXT);
            b.setLabelSize(11);
            b.setIcon("heart");
            b.setOnClickListener(v -> onBreed(breed));
            breedButtons.put(breed, b);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(8);
            list.addView(b, lp);
        }
    }

    private void refreshBreeds() {
        Shop.Breed current = Shop.breed(progress);
        for (Map.Entry<Shop.Breed, PixelButton> e : breedButtons.entrySet()) {
            Shop.Breed breed = e.getKey();
            boolean owned = Shop.owns(progress, breed);
            e.getValue().setFill(breed == current ? 0xFF2FA873 : owned ? PixelTheme.PANEL_LIGHT : PixelTheme.PANEL);
            e.getValue().setSubtitle(breed == current ? "Your cat" : owned ? "Owned"
                    : breed.source != Shop.Source.COINS ? breed.source.how : breed.price + " coins");
        }
    }

    private void onBreed(Shop.Breed breed) {
        Sfx.play(Sfx.Id.UI_TAP);
        if (Shop.owns(progress, breed)) {
            Shop.use(progress, breed);
            save();
            refresh();
            cat.hop();
            return;
        }
        CatView preview = new CatView(this, 128);
        preview.dress(progress);
        preview.tryOn(breed);
        preview.setClickable(false);
        PixelModal m = new PixelModal(this).title(breed.title.toUpperCase(java.util.Locale.ROOT)).icon("heart")
                .accent(PixelTheme.GOLD).custom(preview);
        if (breed.source != Shop.Source.COINS) {
            m.note(breed.source.how + ".").primary("OK", null).show();
        } else if (Shop.canAfford(progress, breed)) {
            m.stat("coin", "Price", String.valueOf(breed.price), PixelTheme.GOLD)
                    .stat("coin", "Coins left after", String.valueOf(progress.coins - breed.price), PixelTheme.TEXT)
                    .button("BUY FOR " + breed.price, PixelTheme.GOLD, PixelTheme.INK, "coin", null, () -> {
                        if (!Shop.buy(progress, breed)) return;
                        save();
                        refresh();
                        celebrate();
                    })
                    .secondary("NOT NOW", null)
                    .show();
        } else {
            m.stat("coin", "Price", String.valueOf(breed.price), PixelTheme.GOLD)
                    .stat("coin", "Your coins", String.valueOf(progress.coins), PixelTheme.ROSE)
                    .primary("OK", null)
                    .show();
        }
    }

    private void addStore(LinearLayout list) {
        for (Purchases.Pack pack : Purchases.Pack.values()) {
            PixelButton b = storeButton(list, pack.coins + " COINS", "coin", PixelTheme.GOLD, PixelTheme.INK);
            b.setOnClickListener(v -> buyProduct(pack.productId));
            packButtons.put(pack, b);
        }
        videoButton = storeButton(list, "FREE COINS", "film", PixelTheme.MINT, PixelTheme.INK);
        videoButton.setOnClickListener(v -> watchCoinVideo());
        removeAdsButton = storeButton(list, "NO AD BREAKS", "close", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
        removeAdsButton.setOnClickListener(v -> confirmRemoveAds());
    }

    private PixelButton storeButton(LinearLayout list, String label, String icon, int fill, int text) {
        PixelButton b = new PixelButton(this, label, fill, text);
        b.setLabelSize(12);
        b.setIcon(icon);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        list.addView(b, lp);
        return b;
    }

    private void addRows(LinearLayout list, List<Shop.Item> items) {
        for (int i = 0; i < items.size(); i += COLUMNS) {
            LinearLayout row = new LinearLayout(this);
            for (int c = 0; c < COLUMNS; c++) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                if (c > 0) lp.leftMargin = dp(8);
                if (i + c < items.size()) {
                    final ItemCard card = new ItemCard(this, items.get(i + c));
                    card.setClickSound(null);
                    card.setOnClickListener(v -> onItem(card.item));
                    cards.add(card);
                    row.addView(card, lp);
                } else {
                    row.addView(new View(this), lp); // keeps the last row's cards the same width
                }
            }
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rp.bottomMargin = dp(8);
            list.addView(row, rp);
        }
    }

    private void refresh() {
        coins.animateTo(progress.coins);
        cat.dress(progress);
        for (ItemCard c : cards) c.update(progress);
        refreshBreeds();
        refreshStore();
    }

    private void refreshStore() {
        if (!Features.MONETIZATION) return;
        for (Map.Entry<Purchases.Pack, PixelButton> e : packButtons.entrySet()) {
            String price = billing.price(e.getKey().productId);
            e.getValue().setSubtitle(price != null ? price : "Store closed");
        }
        int left = AdPolicy.coinVideosLeft(progress, Days.today());
        videoButton.setSubtitle("+" + Scoring.REWARDED_AD_COINS + " per video, " + left + " left today");
        removeAdsButton.setVisibility(progress.adsRemoved ? View.GONE : View.VISIBLE);
        String price = billing.price(Purchases.REMOVE_ADS);
        removeAdsButton.setSubtitle(price != null ? price : "Store closed");
    }

    private void buyProduct(String productId) {
        if (billing.buy(this, productId)) return;
        billing.refresh();
        new PixelModal(this).title("STORE CLOSED").icon("bag").accent(PixelTheme.ROSE)
                .body("The store isn't open right now. Check your connection and try again in a minute.")
                .primary("OK", null)
                .show();
    }

    private void confirmRemoveAds() {
        new PixelModal(this).title("NO AD BREAKS").icon("close").accent(PixelTheme.GOLD)
                .body("Stops the ads between levels for good. Videos you choose to watch for hints or coins stay.")
                .primary("BUY", "bag", () -> buyProduct(Purchases.REMOVE_ADS))
                .secondary("NOT NOW", null)
                .show();
    }

    private void watchCoinVideo() {
        long today = Days.today();
        if (AdPolicy.coinVideosLeft(progress, today) == 0) {
            new PixelModal(this).title("ALL DONE TODAY").icon("film").accent(PixelTheme.SKY)
                    .body("You've watched today's coin videos. More tomorrow!")
                    .primary("OK", null)
                    .show();
            return;
        }
        AdsService.get(this).showRewarded(this, () -> runOnUiThread(() -> {
            if (!AdPolicy.payCoinVideo(progress, today)) return;
            save();
            refresh();
        }), () -> new PixelModal(this).title("NO VIDEO").icon("film").accent(PixelTheme.ROSE)
                .body("There isn't a video available right now. This usually clears up in a minute, or check your connection.")
                .primary("OK", null)
                .show());
    }

    // ------------------------------------------------------------------ store results

    @Override
    public void onStoreChanged() {
        refreshStore();
    }

    @Override
    public void onGranted(String productId, int coinsAdded) {
        progress = store.load();
        refresh();
        celebrate();
        new PixelModal(this).title("THANK YOU!").icon("heart").accent(PixelTheme.GOLD)
                .body(coinsAdded > 0 ? coinsAdded + " coins are in your purse." : "No more ad breaks between levels.")
                .primary("OK", null)
                .show();
    }

    @Override
    public void onPending() {
        new PixelModal(this).title("PAYMENT PENDING").icon("clock").accent(PixelTheme.SKY)
                .body("Your payment is still going through. It'll be added as soon as Google confirms it, so there's no need to pay again.")
                .primary("OK", null)
                .show();
    }

    @Override
    public void onFailed() {
        new PixelModal(this).title("NOT COMPLETED").icon("close").accent(PixelTheme.ROSE)
                .body("That purchase didn't go through. Please try again.")
                .primary("OK", null)
                .show();
    }

    private void save() {
        store.save(progress);
        if (PlayGamesService.isConfigured(this)) PlayGamesService.saveProgress(this, progress.serialize());
    }

    private void onItem(Shop.Item item) {
        if (!Shop.owns(progress, item)) {
            Sfx.play(Sfx.Id.UI_TAP);
            confirmBuy(item);
            return;
        }
        Sfx.play(Sfx.Id.UI_TOGGLE);
        if (Shop.isWearing(progress, item)) Shop.takeOff(progress, item.slot);
        else Shop.wear(progress, item);
        save();
        refresh();
        cat.hop();
    }

    private CatView tryOnCat(Shop.Item item) {
        CatView preview = new CatView(this, 128);
        preview.dress(progress);
        preview.tryOn(item);
        preview.setClickable(false);
        return preview;
    }

    private void confirmBuy(Shop.Item item) {
        if (item.source != Shop.Source.COINS) {
            new PixelModal(this).title(item.title.toUpperCase(java.util.Locale.ROOT)).icon("sparkle").accent(PixelTheme.SKY)
                    .custom(tryOnCat(item))
                    .note(item.source.how + ".")
                    .primary("OK", null)
                    .show();
            return;
        }
        if (!Shop.canAfford(progress, item)) {
            int shortBy = item.price - progress.coins;
            new PixelModal(this).title(item.title.toUpperCase(java.util.Locale.ROOT)).icon("coin").accent(PixelTheme.ROSE)
                    .custom(tryOnCat(item))
                    .stat("coin", "Price", String.valueOf(item.price), PixelTheme.GOLD)
                    .stat("coin", "Your coins", String.valueOf(progress.coins), PixelTheme.ROSE)
                    .note("You need " + shortBy + " more coins. Clearing a level pays +" + Scoring.COINS_PER_LEVEL
                            + " and the daily puzzle pays +" + Scoring.COINS_PER_DAILY + ".")
                    .primary("KEEP PLAYING", this::finish)
                    .secondary("CLOSE", null)
                    .show();
            return;
        }
        new PixelModal(this).title("BUY THIS?").icon("bag").accent(PixelTheme.GOLD)
                .custom(tryOnCat(item))
                .body(item.title + " will look great on your cat. It goes on as soon as you buy it.")
                .stat("coin", "Price", String.valueOf(item.price), PixelTheme.GOLD)
                .stat("coin", "Coins left after", String.valueOf(progress.coins - item.price), PixelTheme.TEXT)
                .button("BUY FOR " + item.price, PixelTheme.GOLD, PixelTheme.INK, "coin", null, () -> buy(item))
                .secondary("NOT NOW", null)
                .show();
    }

    private void buy(Shop.Item item) {
        if (!Shop.buy(progress, item)) return;
        save();
        refresh();
        celebrate();
    }

    private void celebrate() {
        Sfx.play(Sfx.Id.BUY);
        Haptics.celebrate();
        cat.hop();
        handler().postDelayed(() -> Sfx.play(Sfx.Id.MEOW, 1.1f), 420);
        fx.play("ui_confetti", fx.getWidth() / 2f, fx.getHeight() * 0.2f, PixelTheme.dp(this, 2.4f), null);
    }

    private android.os.Handler handler() {
        return contentLayer.getHandler();
    }
}
