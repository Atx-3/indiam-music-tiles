package com.indianmusictiles.game.engine;

import java.util.List;
import java.util.ArrayList;

public class BeatMap {
    private float bpm;
    private List<Float> beats;

    public BeatMap() {
        this.bpm = 120f;
        this.beats = new ArrayList<>();
    }

    public BeatMap(float bpm, List<Float> beats) {
        this.bpm = bpm;
        this.beats = beats != null ? beats : new ArrayList<>();
    }

    public float getBpm() { return bpm; }
    public void setBpm(float bpm) { this.bpm = bpm; }

    public List<Float> getBeats() { return beats; }
    public void setBeats(List<Float> beats) { this.beats = beats; }

    public int getBeatCount() { return beats.size(); }

    public float getDurationSeconds() {
        if (beats.isEmpty()) return 0;
        return beats.get(beats.size() - 1);
    }
}
