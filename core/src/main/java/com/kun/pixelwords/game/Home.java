package com.kun.pixelwords.game;

import java.util.ArrayList;
import java.util.List;

/**
 * The cat's room: furniture bought with coins. Each slot holds one item, which starts in the slot's
 * spot and can be dragged anywhere that keeps it inside the room. Items are saved by id, so add new
 * ones with a new id and never renumber. Art comes from tools/gen_home.py.
 */
public final class Home {

    /** The room's size in art pixels. */
    public static final int ROOM_SIZE = 120;

    private static final int NOT_PLACED = 0;
    private static final int OFFSET_ZERO = 128;

    /** Listed back to front, which is also the order they are drawn in. */
    public enum Slot {
        WINDOW("Window", 12, 10, 36, 36),
        WALL("Wall art", 72, 14, 28, 24),
        RUG("Rug", 44, 90, 56, 24),
        PLANT("Plant", 96, 48, 20, 36),
        BED("Bed", 6, 62, 48, 32),
        TOY("Toy", 62, 66, 20, 20);

        public final String title;
        public final int x, y, width, height;

        Slot(String title, int x, int y, int width, int height) {
            this.title = title;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    public enum Item {
        BASKET(1, "bed_basket", "Basket", Slot.BED, 120),
        CUSHION(2, "bed_cushion", "Big Cushion", Slot.BED, 220),
        CANOPY(3, "bed_canopy", "Royal Bed", Slot.BED, 450),
        RUG_MINT(4, "rug_mint", "Mint Rug", Slot.RUG, 100),
        RUG_ROSE(5, "rug_rose", "Rose Rug", Slot.RUG, 180),
        RUG_STAR(6, "rug_star", "Star Rug", Slot.RUG, 320),
        WINDOW_DAY(7, "win_day", "Sunny Window", Slot.WINDOW, 150),
        WINDOW_NIGHT(8, "win_night", "Night Window", Slot.WINDOW, 260),
        WINDOW_BLOSSOM(9, "win_blossom", "Blossom Window", Slot.WINDOW, 380),
        ART_FISH(10, "art_fish", "Fish Painting", Slot.WALL, 90),
        ART_HEART(11, "art_heart", "Heart Frame", Slot.WALL, 160),
        ART_MOON(12, "art_moon", "Moon Poster", Slot.WALL, 280),
        FERN(13, "plant_fern", "Fern", Slot.PLANT, 110),
        CACTUS(14, "plant_cactus", "Cactus", Slot.PLANT, 170),
        FLOWERS(15, "plant_flowers", "Flower Pot", Slot.PLANT, 300),
        BALL(16, "toy_ball", "Yarn Ball", Slot.TOY, 60),
        MOUSE(17, "toy_mouse", "Toy Mouse", Slot.TOY, 130),
        FISH(18, "toy_fish", "Toy Fish", Slot.TOY, 240);

        public final String key;
        public final String title;
        public final Slot slot;
        public final int price;
        private final int id;

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

    private Home() {}

    public static List<Item> itemsIn(Slot slot) {
        List<Item> items = new ArrayList<>();
        for (Item i : Item.values()) if (i.slot == slot) items.add(i);
        return items;
    }

    public static boolean owns(Progress p, Item item) {
        return (p.homeOwned & item.bit()) != 0;
    }

    public static boolean canAfford(Progress p, Item item) {
        return p.coins >= item.price;
    }

    /** Buys an item and puts it in the room. Returns false if it's owned or too expensive. */
    public static boolean buy(Progress p, Item item) {
        if (owns(p, item) || !canAfford(p, item)) return false;
        p.coins -= item.price;
        p.homeOwned |= item.bit();
        place(p, item);
        return true;
    }

    /** Puts an owned item in its slot, in the slot's spot. */
    public static void place(Progress p, Item item) {
        if (!owns(p, item)) return;
        p.homePlaced[item.slot.ordinal()] = pack(item.id, 0, 0);
    }

    public static void remove(Progress p, Slot slot) {
        p.homePlaced[slot.ordinal()] = NOT_PLACED;
    }

    /** The item in a slot, or null if it's empty. */
    public static Item placed(Progress p, Slot slot) {
        int id = p.homePlaced[slot.ordinal()] & 0xFF;
        if (id == NOT_PLACED) return null;
        for (Item i : Item.values()) if (i.id == id && i.slot == slot) return i;
        return null;
    }

    public static int x(Progress p, Slot slot) {
        return slot.x + ((p.homePlaced[slot.ordinal()] >> 8) & 0xFF) - OFFSET_ZERO;
    }

    public static int y(Progress p, Slot slot) {
        return slot.y + ((p.homePlaced[slot.ordinal()] >> 16) & 0xFF) - OFFSET_ZERO;
    }

    /** Moves a placed item, stopping at the walls so it can never be lost off the edge of the room. */
    public static void move(Progress p, Slot slot, int x, int y) {
        Item item = placed(p, slot);
        if (item == null) return;
        int inX = Math.max(0, Math.min(ROOM_SIZE - slot.width, x));
        int inY = Math.max(0, Math.min(ROOM_SIZE - slot.height, y));
        p.homePlaced[slot.ordinal()] = pack(item.id, inX - slot.x, inY - slot.y);
    }

    private static int pack(int id, int dx, int dy) {
        return id | (dx + OFFSET_ZERO) << 8 | (dy + OFFSET_ZERO) << 16;
    }
}
