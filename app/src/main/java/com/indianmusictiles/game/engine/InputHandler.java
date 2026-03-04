package com.indianmusictiles.game.engine;

import android.view.MotionEvent;
import java.util.List;

public class InputHandler {
    private static final int NUM_LANES = 4;
    private static final float HIT_TOLERANCE_PERFECT = 0.08f;
    private static final float HIT_TOLERANCE_GOOD = 0.15f;
    private static final float MISS_THRESHOLD = 0.3f;

    public interface InputCallback {
        void onTileHit(Tile tile, ScoreSystem.HitResult result);
        void onTileMiss(Tile tile);
        void onMisTap(int lane);
    }

    private final int screenWidth;
    private final float tapZoneY;
    private final float tapZoneHeight;
    private final InputCallback callback;

    public InputHandler(int screenWidth, float tapZoneY, float tapZoneHeight, InputCallback callback) {
        this.screenWidth = screenWidth;
        this.tapZoneY = tapZoneY;
        this.tapZoneHeight = tapZoneHeight;
        this.callback = callback;
    }

    public boolean handleTouch(MotionEvent event, List<Tile> activeTiles, float currentTime) {
        if (event.getAction() != MotionEvent.ACTION_DOWN &&
            event.getAction() != MotionEvent.ACTION_POINTER_DOWN) {
            return false;
        }

        int pointerIndex = event.getActionIndex();
        float touchX = event.getX(pointerIndex);
        float touchY = event.getY(pointerIndex);

        int lane = getLaneFromX(touchX);
        if (lane < 0 || lane >= NUM_LANES) return false;

        // Find the closest active tile in this lane
        Tile bestTile = null;
        float bestTimeDiff = Float.MAX_VALUE;

        for (Tile tile : activeTiles) {
            if (tile.getLane() == lane && tile.isActive()) {
                float timeDiff = Math.abs(tile.getBeatTime() - currentTime);
                if (timeDiff < bestTimeDiff && timeDiff < MISS_THRESHOLD) {
                    bestTile = tile;
                    bestTimeDiff = timeDiff;
                }
            }
        }

        if (bestTile != null) {
            float timingError = bestTile.getBeatTime() - currentTime;
            ScoreSystem.HitResult result;
            if (Math.abs(timingError) <= HIT_TOLERANCE_PERFECT) {
                result = ScoreSystem.HitResult.PERFECT;
            } else if (Math.abs(timingError) <= HIT_TOLERANCE_GOOD) {
                result = ScoreSystem.HitResult.GOOD;
            } else {
                result = ScoreSystem.HitResult.MISS;
            }
            bestTile.setState(Tile.State.HIT);
            callback.onTileHit(bestTile, result);
            return true;
        } else {
            // Tapped empty lane — mis-tap
            callback.onMisTap(lane);
            return false;
        }
    }

    public void checkMissedTiles(List<Tile> activeTiles, float currentTime) {
        for (Tile tile : activeTiles) {
            if (tile.isActive() && currentTime - tile.getBeatTime() > MISS_THRESHOLD) {
                tile.setState(Tile.State.MISSED);
                callback.onTileMiss(tile);
            }
        }
    }

    private int getLaneFromX(float x) {
        float laneWidth = (float) screenWidth / NUM_LANES;
        return (int) (x / laneWidth);
    }
}
