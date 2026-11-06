package com.kun.pixelwords.services;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.kun.pixelwords.Features;
import com.kun.pixelwords.data.ProgressStore;
import com.kun.pixelwords.game.Progress;
import com.kun.pixelwords.game.Purchases;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Google Play in-app purchases. Create these products in Play Console > Monetize > One-time products:
 * coins_small, coins_medium, coins_large (consumed on delivery, so they can be bought again) and
 * remove_ads (kept for good, and restored from Play after a reinstall).
 *
 * The reward is saved before Play is told the purchase was delivered. If the app dies in between, the
 * purchase comes back on the next launch and Purchases.credit refuses to pay it twice. There is no
 * server, so purchases are not verified: coins only buy cosmetics and hints.
 */
public final class BillingService implements PurchasesUpdatedListener {

    /** Called on the main thread. */
    public interface Listener {
        /** Prices arrived, or the store went away. */
        void onStoreChanged();
        /** A purchase was paid out. {@code coins} is 0 for remove_ads. */
        void onGranted(String productId, int coins);
        /** Paid by a slow method such as UPI: nothing is granted until Play confirms it. */
        void onPending();
        /** The purchase failed. Not called when the player backs out. */
        void onFailed();
    }

    private static BillingService instance;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ProgressStore store;
    private final BillingClient client;
    private final Map<String, ProductDetails> products = new HashMap<>();
    private Listener listener;
    private boolean connecting;

    public static synchronized BillingService get(Context context) {
        if (instance == null) instance = new BillingService(context.getApplicationContext());
        return instance;
    }

    private BillingService(Context app) {
        store = new ProgressStore(app);
        client = BillingClient.newBuilder(app)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection()
                .build();
    }

    public void setListener(Listener l) {
        listener = l;
    }

    /** Connects if needed, then collects anything paid while the app wasn't looking. */
    public void refresh() {
        if (!Features.MONETIZATION) return;
        if (client.isReady()) {
            queryProducts();
            queryPurchases();
            return;
        }
        if (connecting) return;
        connecting = true;
        client.startConnection(new BillingClientStateListener() {
            @Override public void onBillingSetupFinished(BillingResult result) {
                main.post(() -> {
                    connecting = false;
                    if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) refresh();
                });
            }

            @Override public void onBillingServiceDisconnected() {
                main.post(() -> connecting = false);
            }
        });
    }

    /** The local price, such as "Rs 49.00", or null if Play hasn't given us this product. */
    public String price(String productId) {
        ProductDetails details = products.get(productId);
        if (details == null) return null;
        ProductDetails.OneTimePurchaseOfferDetails offer = details.getOneTimePurchaseOfferDetails();
        return offer == null ? null : offer.getFormattedPrice();
    }

    /** Opens Play's payment sheet. False if the product isn't available. */
    public boolean buy(Activity activity, String productId) {
        ProductDetails details = products.get(productId);
        if (details == null || !client.isReady()) return false;
        ProductDetails.OneTimePurchaseOfferDetails offer = details.getOneTimePurchaseOfferDetails();
        if (offer == null) return false;
        BillingFlowParams.ProductDetailsParams.Builder item = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details);
        if (offer.getOfferToken() != null) item.setOfferToken(offer.getOfferToken());
        BillingFlowParams params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(item.build()))
                .build();
        return client.launchBillingFlow(activity, params).getResponseCode() == BillingClient.BillingResponseCode.OK;
    }

    @Override
    public void onPurchasesUpdated(BillingResult result, List<Purchase> purchases) {
        main.post(() -> {
            int code = result.getResponseCode();
            if (code == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (Purchase purchase : purchases) handle(purchase, true);
            } else if (code == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                queryPurchases(); // paid earlier but never delivered
            } else if (code != BillingClient.BillingResponseCode.USER_CANCELED && listener != null) {
                listener.onFailed();
            }
        });
    }

    private void queryProducts() {
        List<QueryProductDetailsParams.Product> list = new ArrayList<>();
        for (Purchases.Pack pack : Purchases.Pack.values()) list.add(product(pack.productId));
        list.add(product(Purchases.REMOVE_ADS));
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder().setProductList(list).build();
        client.queryProductDetailsAsync(params, (result, details) -> main.post(() -> {
            if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) return;
            products.clear();
            for (ProductDetails d : details.getProductDetailsList()) products.put(d.getProductId(), d);
            if (listener != null) listener.onStoreChanged();
        }));
    }

    private static QueryProductDetailsParams.Product product(String id) {
        return QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.INAPP)
                .build();
    }

    private void queryPurchases() {
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build();
        client.queryPurchasesAsync(params, (result, purchases) -> main.post(() -> {
            if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) return;
            for (Purchase purchase : purchases) handle(purchase, false);
        }));
    }

    /** @param fresh true if the player is buying this right now, so a pending payment needs explaining */
    private void handle(Purchase purchase, boolean fresh) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            if (fresh && listener != null) listener.onPending();
            return;
        }
        if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) return;
        for (String productId : purchase.getProducts()) {
            Purchases.Pack pack = Purchases.Pack.byProduct(productId);
            if (pack != null) deliverCoins(purchase, pack);
            else if (Purchases.REMOVE_ADS.equals(productId)) deliverRemoveAds(purchase);
        }
    }

    private void deliverCoins(Purchase purchase, Purchases.Pack pack) {
        int coins = pack.coins * purchase.getQuantity();
        Progress progress = store.load();
        if (Purchases.credit(progress, purchase.getPurchaseToken(), coins)) {
            store.save(progress);
            if (listener != null) listener.onGranted(pack.productId, coins);
        }
        // An unconsumed purchase is retried on the next launch, and the ledger stops a second payout.
        client.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build(),
                (result, token) -> { });
    }

    private void deliverRemoveAds(Purchase purchase) {
        Progress progress = store.load();
        if (!progress.adsRemoved) {
            progress.adsRemoved = true;
            store.save(progress);
            if (listener != null) listener.onGranted(Purchases.REMOVE_ADS, 0);
        }
        if (purchase.isAcknowledged()) return;
        client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken()).build(), result -> { });
    }
}
