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
        CROWN(12, "crown", "Royal Crown", Slot.HEAD, 500);

        public final int id;
        /** Short name used in file names: assets/cat/acc_&lt;key&gt;.png */
        public final String key;
        public final String title;
        public final Slot slot;
        public final int price;

        Item(int id, String key, String title, Slot slot, int price) {
            this.id = id;
            this.key = key;
            this.title = title;
            this.slot = slot;
            this.price = price;
        }

        private long bit() {
            return 1L << id;
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

    /** Charges the coins and puts the item on straight away. False if owned already or too expensive. */
    public static boolean buy(Progress p, Item item) {
        if (owns(p, item) || !canAfford(p, item)) return false;
        p.coins -= item.price;
        p.ownedItems |= item.bit();
        wear(p, item);
        return true;
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
