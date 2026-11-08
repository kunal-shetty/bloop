package com.kun.pixelwords.game;

import java.util.Map;

/**
 * What real money buys, and how paid coins are kept safe.
 *
 * Coins are a plain balance that follows the newest save, which would lose a purchase whenever an older
 * copy of the save wins: a cloud sync, or a screen that loaded its copy before the payment cleared. So
 * every paid grant is also written to a ledger in the save, keyed by a hash of its Play purchase token.
 * A grant counts once however often it is retried, and any copy of the save that lacks it adds it back.
 */
public final class Purchases {

    private Purchases() {}

    /** One-time product. It stops ad breaks between levels; videos the player chooses to watch stay. */
    public static final String REMOVE_ADS = "remove_ads";

    /** Consumable coin packs. The ids must match Play Console, and the prices live there too. */
    public enum Pack {
        SMALL("coins_small", 250),
        MEDIUM("coins_medium", 800),
        LARGE("coins_large", 2000);

        public final String productId;
        public final int coins;

        Pack(String productId, int coins) {
            this.productId = productId;
            this.coins = coins;
        }

        /** The pack sold under a product id, or null if it isn't a coin pack. */
        public static Pack byProduct(String productId) {
            for (Pack pack : values()) if (pack.productId.equals(productId)) return pack;
            return null;
        }
    }

    /** Adds paid coins once per purchase token. False if this purchase was already paid out. */
    public static boolean credit(Progress p, String purchaseToken, int coins) {
        long id = tokenId(purchaseToken);
        if (p.paidCoins.containsKey(id)) return false;
        p.paidCoins.put(id, coins);
        p.addCoins(coins);
        return true;
    }

    /** Brings {@code into} up to date with every purchase recorded in {@code other}. */
    public static void absorb(Progress into, Progress other) {
        into.adsRemoved |= other.adsRemoved;
        for (Map.Entry<Long, Integer> grant : other.paidCoins.entrySet()) {
            if (into.paidCoins.containsKey(grant.getKey())) continue;
            into.paidCoins.put(grant.getKey(), grant.getValue());
            into.addCoins(grant.getValue());
        }
    }

    /** FNV-1a, so the save never holds a real purchase token and the hash is the same on every device. */
    static long tokenId(String token) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < token.length(); i++) {
            hash ^= token.charAt(i);
            hash *= 0x100000001b3L;
        }
        return hash;
    }
}
