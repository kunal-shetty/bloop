package com.kun.pixelwords.game;

import com.kun.pixelwords.generator.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * One boss battle on top of a {@link GameSession}. Every word left is a point of the boss's health, and
 * it strikes every few words, or when the player spells something wrong.
 *
 * It can't be lost, and it can never leave the board unsolvable. Only one effect is ever active, and it
 * ends with the next word found. Every effect is only used when at least one word stays within reach.
 */
public final class BossFight {

    /** How often a hand-made boss strikes, in words found. */
    public static final int USUAL_PACE = 3;
    /** The last word on the board is never attacked, so the fight can always end. */
    private static final int FEWEST_WORDS_TO_ATTACK = 2;

    private final Boss.Attack[] attacks;
    private final int wordsPerAttack;
    private final GameSession session;
    private final Level level;
    private final Random rnd;
    private int foundBefore;
    private int wordsSinceAttack;
    private int attacksMade;
    private List<int[]> darkCells = new ArrayList<>();
    private char stolen;

    /** The boss of a region. */
    public BossFight(Region region, GameSession session) {
        this(region.bossAttacks(), region.bossPace(), session);
    }

    /** A boss that takes these attacks in turn, striking every so many words. */
    public BossFight(Boss.Attack[] attacks, int wordsPerAttack, GameSession session) {
        this.attacks = attacks;
        this.wordsPerAttack = wordsPerAttack;
        this.session = session;
        this.level = session.level();
        this.rnd = new Random(level.fingerprint());
        foundBefore = session.foundGridWords().size();
    }

    public int health() {
        return level.placements.size() - session.foundGridWords().size();
    }

    public int maxHealth() {
        return level.placements.size();
    }

    /** The cells hidden by darkness right now. */
    public List<int[]> darkCells() {
        return darkCells;
    }

    /** The letter stolen from the wheel right now, or 0. Only one copy of it is gone. */
    public char stolenLetter() {
        return stolen;
    }

    private boolean hasEffect() {
        return stolen != 0 || !darkCells.isEmpty() || session.bossFrozenWord() >= 0;
    }

    private void clearEffect() {
        stolen = 0;
        darkCells = new ArrayList<>();
        session.setBossFrozenWord(-1);
    }

    /** Call after anything that might have found a word. Returns the attack it set off, or null. */
    public Boss.Attack onBoardChanged() {
        int found = session.foundGridWords().size();
        if (found == foundBefore) return null;
        wordsSinceAttack += found - foundBefore;
        foundBefore = found;
        clearEffect();
        return wordsSinceAttack >= wordsPerAttack ? strike() : null;
    }

    /** Call when the player spells something that isn't a word. Returns the attack it set off, or null. */
    public Boss.Attack onWrongWord() {
        return hasEffect() ? null : strike();
    }

    private Boss.Attack strike() {
        if (health() < FEWEST_WORDS_TO_ATTACK) return null;
        Boss.Attack attack = attacks[attacksMade % attacks.length];
        boolean landed;
        switch (attack) {
            case FREEZE:
                landed = freeze();
                break;
            case DARKEN:
                landed = darken();
                break;
            case STEAL:
                landed = steal();
                break;
            default:
                landed = true; // the wheel just needs shuffling, which the screen does
                break;
        }
        if (!landed) return null;
        wordsSinceAttack = 0;
        attacksMade++;
        return attack;
    }

    private List<Level.Placement> wordsLeft() {
        List<Level.Placement> left = new ArrayList<>();
        for (Level.Placement p : level.placements) if (!session.foundGridWords().contains(p.word)) left.add(p);
        return left;
    }

    private boolean freeze() {
        List<Level.Placement> left = wordsLeft();
        session.setBossFrozenWord(level.placements.indexOf(left.get(rnd.nextInt(left.size()))));
        return true;
    }

    /** Hides a found word, by preference one crossing a word still to find, so its letters are missed. */
    private boolean darken() {
        List<Level.Placement> found = session.foundPlacements();
        if (found.isEmpty()) return false;
        Collections.shuffle(found, rnd);
        Level.Placement pick = found.get(0);
        for (Level.Placement p : found) {
            boolean useful = false;
            for (Level.Placement other : wordsLeft()) useful |= TileMods.crosses(p, other);
            if (useful) {
                pick = p;
                break;
            }
        }
        for (int i = 0; i < pick.word.length(); i++) darkCells.add(new int[]{pick.rowAt(i), pick.colAt(i)});
        return true;
    }

    /** Takes a letter that at least one word still to find can do without. */
    private boolean steal() {
        List<Character> wheel = new ArrayList<>();
        for (char c : level.letters.toCharArray()) wheel.add(c);
        Collections.shuffle(wheel, rnd);
        for (char c : wheel) {
            for (Level.Placement p : wordsLeft()) {
                if (spellable(p.word, c)) {
                    stolen = c;
                    return true;
                }
            }
        }
        return false;
    }

    /** True if the wheel, with one copy of {@code missing} gone, can spell the word. 0 means nothing is gone. */
    public boolean spellable(String word, char missing) {
        StringBuilder wheel = new StringBuilder(level.letters.toLowerCase(java.util.Locale.ROOT));
        if (missing != 0) wheel.deleteCharAt(wheel.indexOf(String.valueOf(Character.toLowerCase(missing))));
        for (char c : word.toCharArray()) {
            int at = wheel.indexOf(String.valueOf(c));
            if (at < 0) return false;
            wheel.deleteCharAt(at);
        }
        return true;
    }
}
