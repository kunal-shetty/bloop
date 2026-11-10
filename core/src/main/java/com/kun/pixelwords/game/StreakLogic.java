package com.kun.pixelwords.game;

/**
 * Daily streak rules. Days are epoch-day numbers (LocalDate.toEpochDay()), so there is no timezone
 * or clock logic in here and it is trivial to test.
 *
 * - Playing the daily puzzle on consecutive days grows the streak.
 * - Missing exactly one day is forgiven if the player has a streak freeze (one is earned every 7 days, max 3).
 * - Missing more than that resets the streak to 1 on the next completed daily.
 */
public final class StreakLogic {

    private StreakLogic() {}

    public static final int FREEZE_EVERY_DAYS = 7;
    public static final int MAX_FREEZES = 3;

    /** Immutable streak state. lastDay is -1 if the daily has never been completed. */
    public static final class State {
        public final int streak;
        public final int best;
        public final long lastDay;
        public final int freezes;

        public State(int streak, int best, long lastDay, int freezes) {
            this.streak = streak;
            this.best = best;
            this.lastDay = lastDay;
            this.freezes = freezes;
        }

        public static State fresh() {
            return new State(0, 0, -1, 0);
        }
    }

    /** Call when the player completes the daily puzzle for {@code today}. */
    public static State onDailyCompleted(State s, long today) {
        if (s.lastDay == today) return s;

        int streak;
        int freezes = s.freezes;
        if (s.lastDay == today - 1) {
            streak = s.streak + 1;
        } else if (s.lastDay == today - 2 && s.freezes > 0 && s.streak > 0) {
            streak = s.streak + 1;
            freezes--;
        } else {
            streak = 1;
        }

        if (streak > 0 && streak % FREEZE_EVERY_DAYS == 0) {
            freezes = Math.min(MAX_FREEZES, freezes + 1);
        }
        return new State(streak, Math.max(s.best, streak), today, freezes);
    }

    /** The streak to display on {@code today}: it only reads as alive if it can still be continued. */
    public static int displayStreak(State s, long today) {
        if (s.lastDay < 0) return 0;
        if (s.lastDay == today || s.lastDay == today - 1) return s.streak;
        if (s.lastDay == today - 2 && s.freezes > 0) return s.streak;
        return 0;
    }

    /** Smallest streak worth offering a rescue for. */
    public static final int MIN_RESCUE_STREAK = 3;

    /** True if one day was missed with no freeze to cover it, so a watched video could still save a good streak. */
    public static boolean canRescue(State s, long today) {
        return s.streak >= MIN_RESCUE_STREAK && s.lastDay == today - 2 && s.freezes == 0;
    }

    /** The same streak with one more freeze, which then covers the missed day. */
    public static State withFreeze(State s) {
        return new State(s.streak, s.best, s.lastDay, Math.min(MAX_FREEZES, s.freezes + 1));
    }

    /** True if the player has not yet done today's daily and their streak is at risk of ending tonight. */
    public static boolean atRisk(State s, long today) {
        return s.streak > 0 && s.lastDay == today - 1;
    }
}
