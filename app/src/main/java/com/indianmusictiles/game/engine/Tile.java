package com.indianmusictiles.game.engine;

public class Tile {
    public enum Type { TAP, HOLD }
    public enum State { ACTIVE, HIT, MISSED, RECYCLED }

    private int lane;
    private float beatTime;
    private Type type;
    private State state;
    private float holdDuration;
    private float yPosition;
    private float height;
    private boolean holdPressed;
    private float holdProgress;

    public Tile() {
        reset();
    }

    public void reset() {
        this.lane = 0;
        this.beatTime = 0;
        this.type = Type.TAP;
        this.state = State.RECYCLED;
        this.holdDuration = 0;
        this.yPosition = -200;
        this.height = 0;
        this.holdPressed = false;
        this.holdProgress = 0;
    }

    public void init(int lane, float beatTime, Type type, float holdDuration) {
        this.lane = lane;
        this.beatTime = beatTime;
        this.type = type;
        this.state = State.ACTIVE;
        this.holdDuration = holdDuration;
        this.yPosition = -200;
        this.holdPressed = false;
        this.holdProgress = 0;
    }

    public int getLane() { return lane; }
    public float getBeatTime() { return beatTime; }
    public Type getType() { return type; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    public float getHoldDuration() { return holdDuration; }
    public float getYPosition() { return yPosition; }
    public void setYPosition(float y) { this.yPosition = y; }
    public float getHeight() { return height; }
    public void setHeight(float h) { this.height = h; }
    public boolean isHoldPressed() { return holdPressed; }
    public void setHoldPressed(boolean p) { this.holdPressed = p; }
    public float getHoldProgress() { return holdProgress; }
    public void setHoldProgress(float p) { this.holdProgress = p; }

    public boolean isActive() { return state == State.ACTIVE; }

    public float getEndTime() {
        return type == Type.HOLD ? beatTime + holdDuration : beatTime;
    }
}
