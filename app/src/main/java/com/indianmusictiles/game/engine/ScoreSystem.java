package com.indianmusictiles.game.engine;

public class ScoreSystem {
    private static final int POINTS_PER_TAP = 10;
    private static final int COMBO_BONUS = 50;
    private static final int COMBO_BONUS_INTERVAL = 10;

    private int score;
    private int combo;
    private int maxCombo;
    private int perfectCount;
    private int goodCount;
    private int missCount;
    private int totalBeats;

    public ScoreSystem() {
        reset();
    }

    public void reset() {
        score = 0;
        combo = 0;
        maxCombo = 0;
        perfectCount = 0;
        goodCount = 0;
        missCount = 0;
        totalBeats = 0;
    }

    public enum HitResult {
        PERFECT, GOOD, MISS
    }

    public HitResult registerHit(float timingError) {
        totalBeats++;
        float absError = Math.abs(timingError);

        if (absError <= 0.15f) { // 150ms window for PERFECT
            combo++;
            if (combo > maxCombo) maxCombo = combo;

            int points = POINTS_PER_TAP;
            if (absError <= 0.08f) { // Tighter window
                perfectCount++;
                points = POINTS_PER_TAP * 2;
            } else {
                goodCount++;
            }

            // Combo bonus
            if (combo > 0 && combo % COMBO_BONUS_INTERVAL == 0) {
                points += COMBO_BONUS;
            }

            // Combo multiplier
            if (combo >= 50) points *= 3;
            else if (combo >= 20) points *= 2;
            else if (combo >= 10) points = (int)(points * 1.5f);

            score += points;
            return absError <= 0.08f ? HitResult.PERFECT : HitResult.GOOD;
        } else {
            // Miss
            missCount++;
            combo = 0;
            return HitResult.MISS;
        }
    }

    public void registerMiss() {
        totalBeats++;
        missCount++;
        combo = 0;
    }

    public int getScore() { return score; }
    public int getCombo() { return combo; }
    public int getMaxCombo() { return maxCombo; }
    public int getPerfectCount() { return perfectCount; }
    public int getGoodCount() { return goodCount; }
    public int getMissCount() { return missCount; }

    public int getStarRating() {
        if (totalBeats == 0) return 0;
        float accuracy = (float)(perfectCount + goodCount) / totalBeats;
        if (accuracy >= 0.95f && missCount == 0) return 3;
        if (accuracy >= 0.80f) return 2;
        if (accuracy >= 0.50f) return 1;
        return 0;
    }

    public boolean isGameOver() {
        return missCount > 0;
    }
}
