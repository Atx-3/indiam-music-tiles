package com.indianmusictiles.game.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TileSpawner {
    private static final int NUM_LANES = 4;
    private static final float HOLD_THRESHOLD = 0.6f;

    private final BeatMap beatMap;
    private final ObjectPool<Tile> tilePool;
    private final List<Tile> activeTiles;
    private final Random random;
    private int nextBeatIndex;
    private float tileSpeed;
    private float lookAheadTime;
    private int lastLane = -1;

    public TileSpawner(BeatMap beatMap) {
        this.beatMap = beatMap;
        this.random = new Random();
        this.activeTiles = new ArrayList<>();
        this.nextBeatIndex = 0;

        this.tilePool = new ObjectPool<>(
                Tile::new,
                Tile::reset,
                50,
                100
        );

        // Scale speed based on BPM
        float bpm = beatMap.getBpm();
        if (bpm >= 160) {
            tileSpeed = 2.0f;
            lookAheadTime = 1.0f;
        } else if (bpm >= 120) {
            tileSpeed = 1.5f;
            lookAheadTime = 1.3f;
        } else {
            tileSpeed = 1.0f;
            lookAheadTime = 1.6f;
        }
    }

    public void update(float currentTime) {
        List<Float> beats = beatMap.getBeats();

        // Spawn new tiles
        while (nextBeatIndex < beats.size()) {
            float beatTime = beats.get(nextBeatIndex);
            if (beatTime - currentTime > lookAheadTime) {
                break;
            }

            Tile tile = tilePool.obtain();
            int lane = getNextLane();

            // Determine tile type
            Tile.Type type = Tile.Type.TAP;
            float holdDuration = 0;

            if (nextBeatIndex < beats.size() - 1) {
                float gap = beats.get(nextBeatIndex + 1) - beatTime;
                if (gap > HOLD_THRESHOLD && random.nextFloat() < 0.15f) {
                    type = Tile.Type.HOLD;
                    holdDuration = gap * 0.8f;
                }
            }

            tile.init(lane, beatTime, type, holdDuration);
            activeTiles.add(tile);
            nextBeatIndex++;
        }

        // Remove tiles that are done
        activeTiles.removeIf(tile -> {
            if (tile.getState() == Tile.State.HIT || tile.getState() == Tile.State.RECYCLED) {
                tilePool.recycle(tile);
                return true;
            }
            return false;
        });
    }

    private int getNextLane() {
        int lane;
        do {
            lane = random.nextInt(NUM_LANES);
        } while (lane == lastLane);
        lastLane = lane;
        return lane;
    }

    public List<Tile> getActiveTiles() {
        return activeTiles;
    }

    public float getTileSpeed() {
        return tileSpeed;
    }

    public float getLookAheadTime() {
        return lookAheadTime;
    }

    public void reset() {
        for (Tile tile : activeTiles) {
            tilePool.recycle(tile);
        }
        activeTiles.clear();
        nextBeatIndex = 0;
        lastLane = -1;
    }

    public boolean isFinished(float currentTime) {
        List<Float> beats = beatMap.getBeats();
        return nextBeatIndex >= beats.size() && activeTiles.isEmpty();
    }
}
