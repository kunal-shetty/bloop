package com.kun.pixelwords.game;

import java.util.ArrayList;
import java.util.List;

/**
 * The cat's accessory shop: the catalogue plus the rules for buying and wearing. The cat wears at most
 * one item per slot. Saves keep each item's {@code id}, so never reuse or renumber one: add new items
 * with the next free id, up to {@link #MAX_ITEM_ID}. Ids 0-63 live in {@code ownedItems} and 64-127 in
 * {@code ownedItems2}.
 */
public final class Shop {

    /** Two 64-bit numbers of owned flags. */
    public static final int MAX_ITEM_ID = 127;

    private Shop() {}

    public enum Slot {
        HEAD("Hats"), FACE("Glasses"), NECK("Neckwear"), BACK("Backpacks");

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
        CAPE(11, "cape", "Hero Cape", Slot.BACK, 350),
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
        WINGS(29, "wings", "Angel Wings", Slot.BACK, 600),
        HALO(30, "halo", "Halo", Slot.HEAD, 550),
        GALAXY_CROWN(31, "galaxycrown", "Galaxy Crown", Slot.HEAD, Source.SPIN),
        GOLD_SHADES(32, "goldshades", "Gold Shades", Slot.FACE, Source.SPIN),
        PHOENIX_CAPE(33, "phoenix", "Phoenix Cape", Slot.BACK, Source.SPIN),
        ICE_CROWN(34, "icecrown", "Ice Crown", Slot.HEAD, Source.SEASON),
        BLOSSOM_HAT(35, "blossom", "Blossom Hat", Slot.HEAD, Source.SEASON),
        SUN_HAT(36, "sunhat", "Sun Hat", Slot.HEAD, Source.SEASON),
        BOLT_SHADES(37, "bolt", "Bolt Shades", Slot.FACE, Source.SEASON),
        PUMPKIN_HAT(38, "pumpkin", "Pumpkin Hat", Slot.HEAD, Source.SEASON),
        STAR_CLOAK(39, "cloak", "Star Cloak", Slot.BACK, Source.SEASON),
        FISH_HAT(40, "fishhat", "Fish Hat", Slot.HEAD, Source.EVENT),
        EXPLORER_HAT(41, "explorer", "Explorer Hat", Slot.HEAD, 340),
        SCHOLAR_GLASSES(42, "scholar", "Scholar Glasses", Slot.FACE, 290),
        HEART_COLLAR(43, "heartcollar", "Heart Collar", Slot.NECK, Source.BOND),
        SPROUT(44, "sprout", "Little Sprout", Slot.HEAD, 110),
        CAP(45, "cap", "Sports Cap", Slot.HEAD, 160),
        BERET(46, "beret", "Artist Beret", Slot.HEAD, 210),
        HEADPHONES(47, "headphones", "Headphones", Slot.HEAD, 330),
        VIKING_HELMET(48, "viking", "Viking Helmet", Slot.HEAD, 460),
        UNICORN_HORN(49, "unicorn", "Unicorn Horn", Slot.HEAD, 520),
        ROSY_CHEEKS(50, "blush", "Rosy Cheeks", Slot.FACE, 130),
        RETRO_3D(51, "retro3d", "3D Glasses", Slot.FACE, 230),
        MOUSTACHE(52, "mustache", "Fine Moustache", Slot.FACE, 270),
        GOGGLES(53, "goggles", "Pilot Goggles", Slot.FACE, 360),
        SAILOR_COLLAR(54, "sailor", "Sailor Collar", Slot.NECK, 260),
        SPIKED_COLLAR(55, "spiked", "Spiked Collar", Slot.NECK, 310),
        GEM_PENDANT(56, "pendant", "Gem Pendant", Slot.NECK, 480),
        BACKPACK(57, "backpack", "Red Backpack", Slot.BACK, 220),
        BALLOON(58, "balloon", "Heart Balloon", Slot.BACK, 300),
        TEDDY(59, "teddy", "Teddy Pal", Slot.BACK, 380),
        TURTLE_SHELL(60, "shell", "Turtle Shell", Slot.BACK, 420),
        JETPACK(61, "jetpack", "Jetpack", Slot.BACK, 650);

        public final int id;
        /**
         * Short name used in file names: assets/cat/acc_&lt;key&gt;.png, drawn over the body. Optional extras:
         * acc_&lt;key&gt;_back.png behind the tail and body, and an _f1 version of either for a second frame.
         */
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
        COINS(""), SPIN("Won on the Daily Spin"),
        SEASON(Season.EXCLUSIVES_LOCKED ? "Locked for now. A new way to earn it is coming" : "A Season Pass reward"),
        EVENT("A Fish Hunt reward"),
        BOND(Bond.ENABLED ? "Reach bond level " + Bond.MAX_LEVEL + " with your cat" : "Locked for now. A new way to earn it is coming");

        public final String how;

        Source(String how) {
            this.how = how;
        }

        /** No way to earn it at the moment. */
        public boolean locked() {
            return this == SEASON && Season.EXCLUSIVES_LOCKED || this == BOND && !Bond.ENABLED;
        }
    }

    public static List<Item> itemsIn(Slot slot) {
        List<Item> out = new ArrayList<>();
        for (Item i : Item.values()) if (i.slot == slot) out.add(i);
        return out;
    }

    public static boolean owns(Progress p, Item item) {
        return item.id < 64 ? (p.ownedItems >>> item.id & 1) != 0 : (p.ownedItems2 >>> (item.id - 64) & 1) != 0;
    }

    private static void own(Progress p, Item item) {
        if (item.id < 64) p.ownedItems |= 1L << item.id;
        else p.ownedItems2 |= 1L << (item.id - 64);
    }

    public static int ownedCount(Progress p) {
        return Long.bitCount(p.ownedItems) + Long.bitCount(p.ownedItems2);
    }

    /**
     * Capes and wings used to be neckwear. A save from then may wear one in the neck slot, so this moves
     * it to the back, where it now belongs.
     */
    static void settleSlots(Progress p) {
        for (Item i : Item.values()) {
            if (i.id == p.wearNeck && i.slot == Slot.BACK) {
                if (p.wearBack == 0) p.wearBack = i.id;
                p.wearNeck = 0;
            }
        }
    }

    public static boolean canAfford(Progress p, Item item) {
        return p.coins >= item.price;
    }

    /** Charges the coins and puts the item on straight away. False if owned, too expensive or not for sale. */
    public static boolean buy(Progress p, Item item) {
        if (item.source != Source.COINS || owns(p, item) || !canAfford(p, item)) return false;
        p.coins -= item.price;
        own(p, item);
        wear(p, item);
        return true;
    }

    /** Hands over an item without charging. False if the player already has it. */
    public static boolean give(Progress p, Item item) {
        if (owns(p, item)) return false;
        own(p, item);
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
        int id = slot == Slot.HEAD ? p.wearHead : slot == Slot.FACE ? p.wearFace : slot == Slot.NECK ? p.wearNeck : p.wearBack;
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
        else if (slot == Slot.NECK) p.wearNeck = id;
        else p.wearBack = id;
    }
}
