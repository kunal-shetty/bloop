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
        TOY("Toy", 62, 66, 20, 20),
        LAMP("Lamp", 50, 46, 16, 40),
        SHELF("Shelf", 72, 42, 32, 14);

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
        FISH(18, "toy_fish", "Toy Fish", Slot.TOY, 240),
        BED_CLOUD(19, "bed_cloud", "Cloud Bed", Slot.BED, 600),
        BED_HAMMOCK(20, "bed_hammock", "Hammock", Slot.BED, 520),
        RUG_SUN(21, "rug_sun", "Sun Rug", Slot.RUG, 420),
        RUG_PAW(22, "rug_paw", "Paw Rug", Slot.RUG, 360),
        WINDOW_SUNSET(23, "win_sunset", "Sunset Window", Slot.WINDOW, 480),
        WINDOW_SNOW(24, "win_snow", "Snowy Window", Slot.WINDOW, 520),
        ART_CAT(25, "art_cat", "Cat Portrait", Slot.WALL, 360),
        ART_MAP(26, "art_map", "World Map", Slot.WALL, 400),
        BONSAI(27, "plant_bonsai", "Bonsai", Slot.PLANT, 420),
        SUNFLOWER(28, "plant_sunflower", "Sunflower", Slot.PLANT, 340),
        TOWER(29, "toy_tower", "Mini Tower", Slot.TOY, 380),
        BOX(30, "toy_box", "Cardboard Box", Slot.TOY, 200),
        LAMP_CLASSIC(31, "lamp_classic", "Floor Lamp", Slot.LAMP, 180),
        LAMP_LANTERN(32, "lamp_lantern", "Lantern", Slot.LAMP, 300),
        LAMP_LAVA(33, "lamp_lava", "Lava Lamp", Slot.LAMP, 450),
        LAMP_STAR(34, "lamp_star", "Star Lamp", Slot.LAMP, 380),
        SHELF_BOOKS(35, "shelf_books", "Bookshelf", Slot.SHELF, 220),
        SHELF_POTS(36, "shelf_pots", "Plant Shelf", Slot.SHELF, 300),
        SHELF_TROPHY(37, "shelf_trophy", "Trophy Shelf", Slot.SHELF, 420),
        SHELF_CRYSTAL(38, "shelf_crystal", "Crystal Shelf", Slot.SHELF, 480),
        WINDOW_AURORA(39, "win_aurora", "Aurora Window", Slot.WINDOW),
        CHERRY_TREE(40, "plant_cherry", "Cherry Tree", Slot.PLANT),
        RUG_WAVE(41, "rug_wave", "Wave Rug", Slot.RUG),
        LAMP_STORM(42, "lamp_storm", "Storm Lamp", Slot.LAMP),
        BED_PUMPKIN(43, "bed_pumpkin", "Pumpkin Bed", Slot.BED),
        ART_GALAXY(44, "art_galaxy", "Galaxy Print", Slot.WALL);

        public final String key;
        public final String title;
        public final Slot slot;
        /** Coins to buy it. Zero means it only comes from the Season Pass. */
        public final int price;
        private final int id;

        Item(int id, String key, String title, Slot slot) {
            this(id, key, title, slot, 0);
        }

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

    /** A whole room look: the walls and floor behind the furniture. Saved by id. */
    public enum Theme {
        COZY(0, "room", "Cozy Den", 0),
        SNOWY(1, "room_snowy", "Snow Lodge", 1200),
        FOREST(2, "room_forest", "Treehouse", 1300),
        BEACH(3, "room_beach", "Beach Hut", 1400),
        CANDY(4, "room_candy", "Candy Land", 1500),
        HAUNTED(5, "room_haunted", "Haunted Attic", 1600),
        SPACE(6, "room_space", "Space Station", 1800),
        CASTLE(7, "room_castle", "Royal Castle", 2200);

        public final int id;
        public final String key;
        public final String title;
        public final int price;

        Theme(int id, String key, String title, int price) {
            this.id = id;
            this.key = key;
            this.title = title;
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

    /** True if coins can buy this item. The rest come from the Season Pass. */
    public static boolean forSale(Item item) {
        return item.price > 0;
    }

    /** Hands over an item without charging. False if the player already has it. */
    public static boolean give(Progress p, Item item) {
        if (owns(p, item)) return false;
        p.homeOwned |= item.bit();
        return true;
    }

    public static boolean owns(Progress p, Theme theme) {
        return theme == Theme.COZY || (p.themesOwned & theme.bit()) != 0;
    }

    public static boolean canAfford(Progress p, Theme theme) {
        return p.coins >= theme.price;
    }

    /** Buys a room theme and moves the cat in. False if it's owned or too expensive. */
    public static boolean buy(Progress p, Theme theme) {
        if (owns(p, theme) || !canAfford(p, theme)) return false;
        p.coins -= theme.price;
        p.themesOwned |= theme.bit();
        p.homeTheme = theme.id;
        return true;
    }

    /** The room on show, falling back to the first if a save names one the player doesn't have. */
    public static Theme theme(Progress p) {
        for (Theme t : Theme.values()) if (t.id == p.homeTheme && owns(p, t)) return t;
        return Theme.COZY;
    }

    public static void use(Progress p, Theme theme) {
        if (owns(p, theme)) p.homeTheme = theme.id;
    }

    /** Buys an item and puts it in the room. Returns false if it's owned, too expensive or not for sale. */
    public static boolean buy(Progress p, Item item) {
        if (!forSale(item) || owns(p, item) || !canAfford(p, item)) return false;
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
