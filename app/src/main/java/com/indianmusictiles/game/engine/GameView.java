package com.indianmusictiles.game.engine;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final int NUM_LANES = 4;
    private static final int TARGET_FPS = 60;
    private static final long FRAME_PERIOD = 1000 / TARGET_FPS;

    public enum GameState { LOADING, COUNTDOWN, PLAYING, PAUSED, GAME_OVER, SONG_COMPLETE }

    public interface GameCallback {
        float getCurrentAudioTime();
        void onTouchEvent(MotionEvent event);
        GameState getGameState();
        int getScore();
        int getCombo();
        List<Tile> getActiveTiles();
        TileSpawner getTileSpawner();
        String getHitFeedback();
        long getHitFeedbackTime();
    }

    private Thread gameThread;
    private boolean running;
    private GameCallback gameCallback;

    // Paints
    private final Paint bgPaint;
    private final Paint laneDividerPaint;
    private final Paint tilePaint;
    private final Paint holdTilePaint;
    private final Paint hitGlowPaint;
    private final Paint tapZonePaint;
    private final Paint tapZoneLinePaint;
    private final Paint notePaint;
    private final Paint feedbackPaint;

    // Layout
    private int screenWidth;
    private int screenHeight;
    private float laneWidth;
    private float tapZoneY;
    private float tileHeight;

    // Hit feedback
    private final int[] laneGlowAlpha = new int[NUM_LANES];

    public GameView(Context context) {
        this(context, null);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        setFocusable(true);

        bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#0D0D1A"));

        laneDividerPaint = new Paint();
        laneDividerPaint.setColor(Color.parseColor("#1A1A3E"));
        laneDividerPaint.setStrokeWidth(2f);

        tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tilePaint.setColor(Color.parseColor("#1A1A1A"));

        holdTilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        hitGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        tapZonePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tapZonePaint.setColor(Color.parseColor("#15FFFFFF"));

        tapZoneLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tapZoneLinePaint.setColor(Color.parseColor("#F5C842"));
        tapZoneLinePaint.setStrokeWidth(4f);

        notePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        notePaint.setColor(Color.WHITE);
        notePaint.setTextSize(32f);
        notePaint.setTextAlign(Paint.Align.CENTER);
        notePaint.setTypeface(Typeface.DEFAULT_BOLD);

        feedbackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        feedbackPaint.setTextSize(48f);
        feedbackPaint.setTextAlign(Paint.Align.CENTER);
        feedbackPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    }

    public void setGameCallback(GameCallback callback) {
        this.gameCallback = callback;
    }

    public float getTapZoneY() { return tapZoneY; }
    public float getTileHeight() { return tileHeight; }
    public int getScreenWidth() { return screenWidth; }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        screenWidth = getWidth();
        screenHeight = getHeight();
        laneWidth = (float) screenWidth / NUM_LANES;
        tapZoneY = screenHeight * 0.82f;
        tileHeight = screenHeight * 0.18f;

        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        screenWidth = width;
        screenHeight = height;
        laneWidth = (float) screenWidth / NUM_LANES;
        tapZoneY = screenHeight * 0.82f;
        tileHeight = screenHeight * 0.18f;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        try {
            if (gameThread != null) {
                gameThread.join(500);
            }
        } catch (InterruptedException ignored) {}
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameCallback != null) {
            gameCallback.onTouchEvent(event);
        }
        return true;
    }

    @Override
    public void run() {
        while (running) {
            long startTime = System.currentTimeMillis();

            Canvas canvas = null;
            try {
                canvas = getHolder().lockCanvas();
                if (canvas != null) {
                    synchronized (getHolder()) {
                        drawGame(canvas);
                    }
                }
            } finally {
                if (canvas != null) {
                    try {
                        getHolder().unlockCanvasAndPost(canvas);
                    } catch (Exception ignored) {}
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            long sleepTime = FRAME_PERIOD - elapsed;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    private void drawGame(Canvas canvas) {
        if (gameCallback == null) return;

        // Background
        canvas.drawRect(0, 0, screenWidth, screenHeight, bgPaint);

        // Lane dividers
        for (int i = 1; i < NUM_LANES; i++) {
            float x = i * laneWidth;
            canvas.drawLine(x, 0, x, screenHeight, laneDividerPaint);
        }

        // Lane glow effects
        for (int i = 0; i < NUM_LANES; i++) {
            if (laneGlowAlpha[i] > 0) {
                hitGlowPaint.setColor(Color.argb(laneGlowAlpha[i], 245, 200, 66));
                float x = i * laneWidth;
                canvas.drawRect(x, 0, x + laneWidth, screenHeight, hitGlowPaint);
                laneGlowAlpha[i] = Math.max(0, laneGlowAlpha[i] - 5);
            }
        }

        // Tap zone
        canvas.drawRect(0, tapZoneY, screenWidth, tapZoneY + tileHeight, tapZonePaint);
        canvas.drawLine(0, tapZoneY, screenWidth, tapZoneY, tapZoneLinePaint);

        GameState state = gameCallback.getGameState();
        if (state != GameState.PLAYING && state != GameState.PAUSED) return;

        float currentTime = gameCallback.getCurrentAudioTime();
        TileSpawner spawner = gameCallback.getTileSpawner();
        if (spawner == null) return;

        List<Tile> tiles = gameCallback.getActiveTiles();
        if (tiles == null) return;

        float speed = spawner.getTileSpeed();
        float lookAhead = spawner.getLookAheadTime();

        // Draw tiles
        for (Tile tile : tiles) {
            if (!tile.isActive() && tile.getState() != Tile.State.HIT) continue;

            float timeDelta = tile.getBeatTime() - currentTime;
            float normalizedPos = 1.0f - (timeDelta / lookAhead);
            float tileY = normalizedPos * tapZoneY - tileHeight;
            float tileX = tile.getLane() * laneWidth;

            tile.setYPosition(tileY);

            if (tileY > screenHeight + tileHeight) continue;
            if (tileY < -tileHeight * 2) continue;

            RectF tileRect = new RectF(
                    tileX + 4,
                    tileY,
                    tileX + laneWidth - 4,
                    tileY + tileHeight - 4
            );

            if (tile.getType() == Tile.Type.HOLD) {
                // Draw hold tile (taller + gradient)
                float holdEndTime = tile.getBeatTime() + tile.getHoldDuration();
                float holdEndDelta = holdEndTime - currentTime;
                float holdEndNorm = 1.0f - (holdEndDelta / lookAhead);
                float holdEndY = holdEndNorm * tapZoneY - tileHeight;

                RectF holdRect = new RectF(
                        tileX + 8,
                        Math.min(tileY, holdEndY),
                        tileX + laneWidth - 8,
                        Math.max(tileY + tileHeight, holdEndY + tileHeight) - 4
                );

                holdTilePaint.setShader(new LinearGradient(
                        tileX, holdRect.top, tileX, holdRect.bottom,
                        Color.parseColor("#7B2FF7"),
                        Color.parseColor("#2196F3"),
                        Shader.TileMode.CLAMP
                ));
                canvas.drawRoundRect(holdRect, 12, 12, holdTilePaint);
                holdTilePaint.setShader(null);

                // Note icon
                notePaint.setTextSize(28f);
                canvas.drawText("♪", holdRect.centerX(), holdRect.centerY() + 10, notePaint);
            } else {
                // Draw tap tile
                canvas.drawRoundRect(tileRect, 8, 8, tilePaint);

                // Note icon
                notePaint.setTextSize(32f);
                canvas.drawText("♪", tileRect.centerX(), tileRect.centerY() + 12, notePaint);
            }

            // Hit flash
            if (tile.getState() == Tile.State.HIT) {
                laneGlowAlpha[tile.getLane()] = 80;
            }
        }

        // Draw hit feedback text
        String feedback = gameCallback.getHitFeedback();
        long feedbackTime = gameCallback.getHitFeedbackTime();
        if (feedback != null && System.currentTimeMillis() - feedbackTime < 500) {
            float alpha = 1.0f - (System.currentTimeMillis() - feedbackTime) / 500f;
            int a = (int)(alpha * 255);
            if (feedback.equals("PERFECT!")) {
                feedbackPaint.setColor(Color.argb(a, 245, 200, 66));
            } else if (feedback.equals("GOOD")) {
                feedbackPaint.setColor(Color.argb(a, 76, 175, 80));
            } else {
                feedbackPaint.setColor(Color.argb(a, 244, 67, 54));
            }
            float feedbackY = tapZoneY - 40 - (1 - alpha) * 30;
            canvas.drawText(feedback, screenWidth / 2f, feedbackY, feedbackPaint);
        }
    }

    public void stopThread() {
        running = false;
    }

    public void triggerLaneGlow(int lane) {
        if (lane >= 0 && lane < NUM_LANES) {
            laneGlowAlpha[lane] = 100;
        }
    }
}
