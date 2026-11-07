package com.kun.pixelwords.game;

import java.util.ArrayList;
import java.util.List;

/**
 * The cat's accessory shop: the catalogue plus the rules for buying and wearing. The cat wears at most
 * one item per slot. Saves keep each item's {@code id}, so never reuse or renumber one: add new items
 * with the next free id.
 */
public final class Shop {

    private Shop() {}

    public enum Slot {
        HEAD("Hats"), FACE("Glasses"), NECK("Neckwear");

        public final String title;

        Slot(String title) {
            this.title = title;
        }
    }

    public enum Item {
        BELL(1, "bell", "Bell Collar", Slot.NECK, 50),
        BOW(2, "bow", "Pink Bow", Slot.HEAD, 60),
        BOWTIE(3, "bowtie", "Bow Tie", Slot.NECK, 80),
        PARTY_HAT(4, "party", "Party Hat", Slot.HEAD, 90),
        SUNGLASSES(5, "shades", "Cool Shades", Slot.FACE, 120),
        SCARF(6, "scarf", "Cosy Scarf", Slot.NECK, 140),
        BEANIE(7, "beanie", "Beanie", Slot.HEAD, 150),
        HEART_GLASSES(8, "hearts", "Heart Glasses", Slot.FACE, 200),
        MONOCLE(9, "monocle", "Fancy Monocle", Slot.FACE, 250),
        WIZARD_HAT(10, "wizard", "Wizard Hat", Slot.HEAD, 300),
        CAPE(11, "cape", "Hero Cape", Slot.NECK, 350),
        CROWN(12, "crown", "Royal Crown", Slot.HEAD, 500),
        TOP_HAT(13, "tophat", "Top Hat", Slot.HEAD, 400),
        PIRATE_HAT(14, "pirate", "Pirate Hat", Slot.HEAD, 450),
        CHEF_HAT(15, "chef", "Chef Hat", Slot.HEAD, 280),
        FLOWER_CROWN(16, "flowers", "Flower Crown", Slot.HEAD, 320),
        COWBOY_HAT(17, "cowboy", "Cowboy Hat", Slot.HEAD, 380),
        SANTA_HAT(18, "santa", "Santa Hat", Slot.HEAD, 350),
        NERD_GLASSES(19, "nerd", "Nerd Glasses", Slot.FACE, 180),
        STAR_GLASSES(20, "stars", "Star Glasses", Slot.FACE, 260),
        EYE_PATCH(21, "patch", "Eye Patch", Slot.FACE, 220),
        VISOR(22, "visor", "Neon Visor", Slot.FACE, 340),
        HERO_MASK(23, "mask", "Hero Mask", Slot.FACE, 300),
        PEARLS(24, "pearls", "Pearl Necklace", Slot.NECK, 380),
        BANDANA(25, "bandana", "Bandana", Slot.NECK, 170),
        MEDAL(26, "medal", "Gold Medal", Slot.NECK, 420),
        NECKTIE(27, "tie", "Necktie", Slot.NECK, 190),
        LEI(28, "lei", "Flower Lei", Slot.NECK, 240),
        WINGS(29, "wings", "Angel Wings", Slot.NECK, 600),
        HALO(30, "halo", "Halo", Slot.HEAD, 550),
        GALAXY_CROWN(31, "galaxycrown", "Galaxy Crown", Slot.HEAD, Source.SPIN),
        GOLD_SHADES(32, "goldshades", "Gold Shades", Slot.FACE, Source.SPIN),
        PHOENIX_CAPE(33, "phoenix", "Phoenix Cape", Slot.NECK, Source.SPIN),
        ICE_CROWN(34, "icecrown", "Ice Crown", Slot.HEAD, Source.SEASON),
        BLOSSOM_HAT(35, "blossom", "Blossom Hat", Slot.HEAD, Source.SEASON),
        SUN_HAT(36, "sunhat", "Sun Hat", Slot.HEAD, Source.SEASON),
        BOLT_SHADES(37, "bolt", "Bolt Shades", Slot.FACE, Source.SEASON),
        PUMPKIN_HAT(38, "pumpkin", "Pumpkin Hat", Slot.HEAD, Source.SEASON),
        STAR_CLOAK(39, "cloak", "Star Cloak", Slot.NECK, Source.SEASON),
        FISH_HAT(40, "fishhat", "Fish Hat", Slot.HEAD, Source.EVENT),
        EXPLORER_HAT(41, "explorer", "Explorer Hat", Slot.HEAD, Source.ALBUM),
        SCHOLAR_GLASSES(42, "scholar", "Scholar Glasses", Slot.FACE, Source.ALBUM),
        HEART_COLLAR(43, "heartcollar", "Heart Collar", Slot.NECK, Source.BOND);

        public final int id;
        /** Short name used in file names: assets/cat/acc_&lt;key&gt;.png */
        public final String key;
        public final String title;
        public final Slot slot;
        /** Coins to buy it. Zero means it can't be bought: see {@link #source}. */
        public final int price;
        public final Source source;

        Item(int id, String key, String title, Slot slot, int price) {
            this(id, key, title, slot, price, Source.COINS);
        }

        Item(int id, String key, String title, Slot slot, Source source) {
            this(id, key, title, slot, 0, source);
        }

        Item(int id, String key, String title, Slot slot, int price, Source source) {
            this.id = id;
            this.key = key;
            this.title = title;
            this.slot = slot;
            this.price = price;
            this.source = source;
        }

        private long bit() {
            return 1L << id;
        }
    }

    /** A coat for the cat, saved by id. The first breed needs no art suffix and every player has it. */
    public enum Breed {
        ORANGE(0, "", "Orange Tabby", 0, Source.COINS),
        TUXEDO(1, "tuxedo", "Tuxedo", 800, Source.COINS),
        SNOW(2, "snow", "Snow White", 1000, Source.COINS),
        BLACK(3, "black", "Midnight", 1200, Source.COINS),
        GREY(4, "grey", "Grey Tabby", 1400, Source.COINS),
        SIAMESE(5, "siamese", "Siamese", 1800, Source.COINS),
        CALICO(6, "calico", "Calico", 2500, Source.COINS),
        GALAXY(7, "galaxy", "Galaxy Cat", 0, Source.SPIN);

        public final int id;
        /** Suffix of the art layers: assets/cat/body_&lt;key&gt;.png */
        public final String key;
        public final String title;
        public final int price;
        public final Source source;

        Breed(int id, String key, String title, int price, Source source) {
            this.id = id;
            this.key = key;
            this.title = title;
            this.price = price;
            this.source = source;
        }

        private long bit() {
            return 1L << id;
        }
    }

    /** Where something comes from. Only {@link #COINS} things can be bought. */
    public enum Source {
        COINS(""), SPIN("Won on the Daily Spin"), SEASON("A Season Pass reward"),
        EVENT("A Fish Hunt reward"), ALBUM("A Sticker Album reward"), BOND("Reach bond level " + Bond.MAX_LEVEL + " with your cat");

        public final String how;

        Source(String how) {
            this.how = how;
        }
    }

    public static List<Item> itemsIn(Slot slot) {
        List<Item> out = new ArrayList<>();
        for (Item i : Item.values()) if (i.slot == slot) out.add(i);
        return out;
    }

    public static boolean owns(Progress p, Item item) {
        return (p.ownedItems & item.bit()) != 0;
    }

    public static boolean canAfford(Progress p, Item item) {
        return p.coins >= item.price;
    }

    /** Charges the coins and puts the item on straight away. False if owned, too expensive or not for sale. */
    public static boolean buy(Progress p, Item item) {
        if (item.source != Source.COINS || owns(p, item) || !canAfford(p, item)) return false;
        p.coins -= item.price;
        p.ownedItems |= item.bit();
        wear(p, item);
        return true;
    }

    /** Hands over an item without charging. False if the player already has it. */
    public static boolean give(Progress p, Item item) {
        if (owns(p, item)) return false;
        p.ownedItems |= item.bit();
        return true;
    }

    public static boolean owns(Progress p, Breed breed) {
        return breed == Breed.ORANGE || (p.breedsOwned & breed.bit()) != 0;
    }

    public static boolean canAfford(Progress p, Breed breed) {
        return p.coins >= breed.price;
    }

    /** Charges the coins and switches to the breed. False if owned, too expensive or not for sale. */
    public static boolean buy(Progress p, Breed breed) {
        if (breed.source != Source.COINS || owns(p, breed) || !canAfford(p, breed)) return false;
        p.coins -= breed.price;
        p.breedsOwned |= breed.bit();
        p.breed = breed.id;
        return true;
    }

    /** Hands over a breed without charging. False if the player already has it. */
    public static boolean give(Progress p, Breed breed) {
        if (owns(p, breed)) return false;
        p.breedsOwned |= breed.bit();
        return true;
    }

    /** The breed the cat is, falling back to the first if a save names one the player doesn't have. */
    public static Breed breed(Progress p) {
        for (Breed b : Breed.values()) if (b.id == p.breed && owns(p, b)) return b;
        return Breed.ORANGE;
    }

    public static void use(Progress p, Breed breed) {
        if (owns(p, breed)) p.breed = breed.id;
    }

    public static Item wearing(Progress p, Slot slot) {
        int id = slot == Slot.HEAD ? p.wearHead : slot == Slot.FACE ? p.wearFace : p.wearNeck;
        for (Item i : Item.values()) if (i.id == id && i.slot == slot) return i;
        return null;
    }

    public static boolean isWearing(Progress p, Item item) {
        return wearing(p, item.slot) == item;
    }

    /** Puts on an owned item, replacing whatever was in that slot. */
    public static void wear(Progress p, Item item) {
        if (owns(p, item)) setSlot(p, item.slot, item.id);
    }

    public static void takeOff(Progress p, Slot slot) {
        setSlot(p, slot, 0);
    }

    private static void setSlot(Progress p, Slot slot, int id) {
        if (slot == Slot.HEAD) p.wearHead = id;
        else if (slot == Slot.FACE) p.wearFace = id;
        else p.wearNeck = id;
    }
}
