package com.kun.pixelwords.game;

/**
 * Anything the game can hand out. The spin, the Season Pass, bosses, streaks and Fish Hunt all pay
 * through this, so a reward the player already has always turns into coins in the same way.
 */
public final class Reward {

    public static final int DUPLICATE_COINS = 200;

    public enum Kind { COINS, HINTS, POWER_UP, ITEM, BREED, FURNITURE }

    public final Kind kind;
    public final int amount;
    private final PowerUps.Kind powerUp;
    private final Shop.Item item;
    private final Shop.Breed breed;
    private final Home.Item furniture;
    private final String label;

    private Reward(Kind kind, int amount, PowerUps.Kind powerUp, Shop.Item item, Shop.Breed breed,
                   Home.Item furniture, String label) {
        this.kind = kind;
        this.amount = amount;
        this.powerUp = powerUp;
        this.item = item;
        this.breed = breed;
        this.furniture = furniture;
        this.label = label;
    }

    public static Reward coins(int amount) {
        return new Reward(Kind.COINS, amount, null, null, null, null, amount + " coins");
    }

    public static Reward hints(int amount) {
        return new Reward(Kind.HINTS, amount, null, null, null, null, amount + (amount == 1 ? " hint" : " hints"));
    }

    public static Reward powerUp(PowerUps.Kind kind, int amount) {
        return new Reward(Kind.POWER_UP, amount, kind, null, null, null, amount + " x " + kind.title);
    }

    public static Reward item(Shop.Item item) {
        return new Reward(Kind.ITEM, 1, null, item, null, null, item.title);
    }

    public static Reward breed(Shop.Breed breed) {
        return new Reward(Kind.BREED, 1, null, null, breed, null, breed.title);
    }

    public static Reward furniture(Home.Item item) {
        return new Reward(Kind.FURNITURE, 1, null, null, null, item, item.title);
    }

    /** What to show the player. */
    public String label() {
        return label;
    }

    /** The pixel icon for this reward. */
    public String icon() {
        switch (kind) {
            case COINS: return "coin";
            case HINTS: return "bulb";
            case POWER_UP: return powerUp.icon;
            case ITEM: return "bag";
            case BREED: return "heart";
            default: return "home";
        }
    }

    /**
     * Pays the reward and returns what the player actually got, which is coins if they already had it.
     * {@code seed} is kept so callers stay the same if a random reward comes back.
     */
    public Reward grant(Progress p, long seed) {
        switch (kind) {
            case COINS:
                p.addCoins(amount);
                return this;
            case HINTS:
                p.hintTokens += amount;
                return this;
            case POWER_UP:
                PowerUps.addTokens(p, powerUp, amount);
                return this;
            case ITEM:
                return Shop.give(p, item) ? this : duplicate(p);
            case BREED:
                return Shop.give(p, breed) ? this : duplicate(p);
            default:
                return Home.give(p, furniture) ? this : duplicate(p);
        }
    }

    private static Reward duplicate(Progress p) {
        p.addCoins(DUPLICATE_COINS);
        return coins(DUPLICATE_COINS);
    }
}
