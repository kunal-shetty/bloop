package com.kun.pixelwords;

import com.kun.pixelwords.data.Settings;
import com.kun.pixelwords.ui.PixelModal;
import com.kun.pixelwords.ui.PixelTheme;

/**
 * "How to play" cards for every game mode and mechanic. Each is shown by itself the first time the
 * player meets it, before anything is at stake, and can be read again from Settings.
 */
final class Explain {

    enum Topic {
        DAILY("daily", "DAILY PUZZLE", "calendar", PixelTheme.MINT,
                "One new puzzle for everybody, every day.",
                "Solve it to earn coins and keep your streak alive.",
                "Miss a day and a freeze shield saves your streak, if you have one.",
                "You can replay it for fun, but it pays once."),
        TIMED("timed", "TIMED RUN", "clock", PixelTheme.SKY,
                "You have two minutes on a fresh board.",
                "Swipe letters on the wheel to spell words. Longer words score more.",
                "Clear the whole board early to bank bonus points for the time you have left.",
                "Your best score is saved. A +30 Seconds power-up adds time, but then it can't set a record."),
        LIVES("lives", "NINE LIVES", "heart", PixelTheme.ROSE,
                "Boards come one after another. You start with nine lives.",
                "A wrong word costs a life. Clear a board and you move on to a harder one.",
                "You win a life back every five boards. Out of lives? Spend coins to keep going once.",
                "Your run is ranked by score."),
        TOWER("tower", "WORD TOWER", "tower", 0xFF8E7FD0,
                "Climb floor by floor. You get three lives.",
                "Every two floors the words get longer, so short words stop counting.",
                "A wrong word costs a life. Each floor you clear pays coins.",
                "Your highest floor is saved."),
        RUSH("rush", "BOSS RUSH", "swords", 0xFFE8604C,
                "Every boss you have beaten comes back, one after another.",
                "You have one set of lives for the whole rush, and a wrong word costs one.",
                "Bosses still strike with their special attacks, so watch the board.",
                "You are ranked by how many bosses you beat in one go."),
        MEGA("mega", "MEGA BOARD", "star", PixelTheme.GOLD,
                "A giant board that opens every weekend.",
                "There is no timer. Your found words are saved, so you can finish it over both days.",
                "Solving it pays coins and season XP."),
        DUEL("duel", "WORD DUEL", "swords", PixelTheme.ROSE,
                "You and an opponent race on the very same board for 90 seconds.",
                "Every word you find scores points. The highest score when the time runs out wins.",
                "Play the bot, or race up to three friends on the same Wi-Fi."),
        RACE("race", "WI-FI RACE", "cloud", PixelTheme.SKY,
                "Race friends on the same Wi-Fi, no internet needed.",
                "One person hosts and the others join from the list. The host presses start.",
                "Everybody plays the same board. The best score when time runs out wins."),
        TWISTS("twists", "TILE TWISTS", "sparkle", PixelTheme.GOLD,
                "Hard levels have twists on their tiles.",
                "Frozen words are locked in ice. Find another word that crosses them to thaw them.",
                "Gold tiles pay extra coins when you fill them in.",
                "A spark word glows. Find it among your first few words for a big coin prize."),
        BOSS("boss", "BOSS FIGHT", "swords", PixelTheme.ROSE,
                "A boss guards the end of every chapter. Every word you find hurts it.",
                "It strikes back every few words, or when you spell something that is not a word.",
                "Attacks freeze a word, plunge one into darkness, or steal a letter from your wheel. Find another word to undo them.",
                "You cannot lose a boss fight. You just have to outlast it."),
        FISH("fish", "FISH HUNT", "sparkle", PixelTheme.SKY,
                "A monthly event. Fish hide under letters on campaign levels and dailies.",
                "Fill in the letter to catch the fish. Catch enough to unlock prizes.",
                "Check the Season Pass page for your catch and the prizes."),
        POWERUPS("powerups", "POWER-UPS", "bolt", PixelTheme.GOLD,
                "Power-ups help when you are stuck.",
                "Pick a Letter lets you tap any hidden letter to show it.",
                "Reveal Word fills in a whole word.",
                "+30 Seconds adds time in a Timed Run. You earn them from rewards, spins and the season pass.",
                "In the Journey, each one counts against your stars, like a hint."),
        JAR("jar", "PIGGY JAR", "coin", PixelTheme.GOLD,
                "Some words you find are not on the board. They are bonus words.",
                "Every bonus word drops into your piggy jar.",
                "When the jar is full it opens at the end of a level and pays coins."),
        SPIN("spin", "DAILY SPIN", "sparkle", PixelTheme.MINT,
                "Spin the wheel once a day for free.",
                "Prizes are coins, hints and power-ups. Keep spinning for a rare legendary prize."),
        SEASON("season", "SEASON PASS", "ticket", PixelTheme.ROSE,
                "Every month is a new season. Earn XP from levels, dailies, quests and the weekend board.",
                "Each tier you reach pays a reward on the free track.",
                "Unlock premium to get a second reward on every tier.",
                "Tap CLAIM ALL to collect everything that is ready. Anything unclaimed is paid when the season ends."),
        STREAK("streak", "YOUR STREAK", "flame", PixelTheme.ROSE,
                "Solve the daily puzzle every day to build a streak.",
                "Your flame grows as the streak does, and milestones pay rewards.",
                "A freeze shield saves your streak if you miss one day. You earn one every seven days."),
        HOME("home", "CAT HOME", "home", PixelTheme.MINT,
                "This is your cat's room. Buy furniture and themes in the shop below.",
                "Drag furniture to move it. Tap the share button to send a photo.");

        final String key, title, icon;
        final int accent;
        final String[] lines;

        Topic(String key, String title, String icon, int accent, String... lines) {
            this.key = key;
            this.title = title;
            this.icon = icon;
            this.accent = accent;
            this.lines = lines;
        }
    }

    private Explain() {}

    static boolean seen(Topic topic) {
        return Settings.explained(topic.key);
    }

    /** Shows the card if the player has not seen it, then runs {@code then}. Runs it straight away if they have. */
    static void first(BaseActivity context, Topic topic, Runnable then) {
        if (seen(topic)) {
            if (then != null) then.run();
            return;
        }
        Settings.setExplained(topic.key);
        card(context, topic, "LET'S GO", then).show();
    }

    /** The card itself, for reading again from Settings. */
    static PixelModal card(BaseActivity context, Topic topic, String button, Runnable then) {
        PixelModal m = new PixelModal(context).title(topic.title).icon(topic.icon).accent(topic.accent).cancelable(false)
                .heading("HOW IT WORKS");
        m.body(topic.lines[0]);
        for (int i = 1; i < topic.lines.length; i++) m.note((topic.lines.length > 2 ? i + ".  " : "") + topic.lines[i]);
        return m.primary(button, "check", then);
    }
}
