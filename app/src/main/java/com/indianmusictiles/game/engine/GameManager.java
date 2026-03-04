package com.indianmusictiles.game.engine;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;

import com.indianmusictiles.game.audio.AudioManager;
import com.indianmusictiles.game.cache.CacheManager;

import java.util.List;

public class GameManager implements GameView.GameCallback, InputHandler.InputCallback {
    private static final String TAG = "GameManager";

    public interface GameEventListener {
        void onStateChanged(GameView.GameState state);
        void onScoreChanged(int score, int combo);
        void onGameOver(int score, int maxCombo, int stars, String songTitle, String songArtist);
        void onSongComplete(int score, int maxCombo, int stars, String songTitle, String songArtist);
        void onAnalysisProgress(int percent);
        void onHitFeedback(String feedback);
    }

    private final Context context;
    private final AudioManager audioManager;
    private final BeatAnalyzer beatAnalyzer;
    private final CacheManager cacheManager;
    private final ScoreSystem scoreSystem;
    private final Handler mainHandler;
    private final GameEventListener listener;

    private GameView gameView;
    private TileSpawner tileSpawner;
    private InputHandler inputHandler;

    private GameView.GameState currentState = GameView.GameState.LOADING;
    private String hitFeedback;
    private long hitFeedbackTime;

    private String songTitle;
    private String songArtist;
    private Uri songUri;
    private String songHash;

    public GameManager(Context context, GameEventListener listener) {
        this.context = context;
        this.listener = listener;
        this.audioManager = new AudioManager(context);
        this.beatAnalyzer = new BeatAnalyzer(context);
        this.cacheManager = new CacheManager(context);
        this.scoreSystem = new ScoreSystem();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void init(GameView gameView, Uri songUri, String title, String artist, String hash) {
        this.gameView = gameView;
        this.songUri = songUri;
        this.songTitle = title;
        this.songArtist = artist;
        this.songHash = hash;

        gameView.setGameCallback(this);
        setState(GameView.GameState.LOADING);

        // Check cache
        BeatMap cachedBeatMap = cacheManager.get(songHash);
        if (cachedBeatMap != null) {
            Log.d(TAG, "Using cached beat map for " + title);
            onBeatMapReady(cachedBeatMap);
        } else {
            // Analyze song
            beatAnalyzer.analyze(songUri, new BeatAnalyzer.AnalysisCallback() {
                @Override
                public void onProgress(int percent) {
                    mainHandler.post(() -> listener.onAnalysisProgress(percent));
                }

                @Override
                public void onComplete(BeatMap beatMap) {
                    cacheManager.put(songHash, beatMap);
                    mainHandler.post(() -> onBeatMapReady(beatMap));
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Beat analysis failed: " + error);
                }
            });
        }
    }

    private void onBeatMapReady(BeatMap beatMap) {
        try {
            tileSpawner = new TileSpawner(beatMap);
            audioManager.prepareAudio(songUri);

            int screenWidth = gameView.getScreenWidth();
            float tapZoneY = gameView.getTapZoneY();
            float tileHeight = gameView.getTileHeight();

            if (screenWidth > 0) {
                inputHandler = new InputHandler(screenWidth, tapZoneY, tileHeight, this);
            }

            scoreSystem.reset();
            startCountdown();
        } catch (Exception e) {
            Log.e(TAG, "Failed to prepare game", e);
        }
    }

    private void startCountdown() {
        setState(GameView.GameState.COUNTDOWN);
        mainHandler.postDelayed(this::startPlaying, 2000);
    }

    private void startPlaying() {
        setState(GameView.GameState.PLAYING);
        audioManager.play();

        audioManager.setOnCompletionListener(mp -> {
            mainHandler.post(this::onSongFinished);
        });
    }

    public void update() {
        if (currentState != GameView.GameState.PLAYING) return;
        if (tileSpawner == null || inputHandler == null) return;

        float currentTime = getCurrentAudioTime();
        tileSpawner.update(currentTime);
        inputHandler.checkMissedTiles(tileSpawner.getActiveTiles(), currentTime);

        if (tileSpawner.isFinished(currentTime)) {
            onSongFinished();
        }
    }

    private void onSongFinished() {
        setState(GameView.GameState.SONG_COMPLETE);
        audioManager.pause();
        int stars = scoreSystem.getStarRating();
        mainHandler.post(() -> listener.onSongComplete(
                scoreSystem.getScore(), scoreSystem.getMaxCombo(), stars, songTitle, songArtist));
    }

    // InputHandler.InputCallback
    @Override
    public void onTileHit(Tile tile, ScoreSystem.HitResult result) {
        ScoreSystem.HitResult scoreResult = scoreSystem.registerHit(
                tile.getBeatTime() - getCurrentAudioTime());

        gameView.triggerLaneGlow(tile.getLane());

        String feedback;
        switch (scoreResult) {
            case PERFECT:
                feedback = "PERFECT!";
                break;
            case GOOD:
                feedback = "GOOD";
                break;
            default:
                feedback = "MISS";
                break;
        }

        hitFeedback = feedback;
        hitFeedbackTime = System.currentTimeMillis();

        mainHandler.post(() -> {
            listener.onScoreChanged(scoreSystem.getScore(), scoreSystem.getCombo());
            listener.onHitFeedback(feedback);
        });

        if (scoreResult == ScoreSystem.HitResult.MISS) {
            handleGameOver();
        }
    }

    @Override
    public void onTileMiss(Tile tile) {
        scoreSystem.registerMiss();
        hitFeedback = "MISS";
        hitFeedbackTime = System.currentTimeMillis();
        handleGameOver();
    }

    @Override
    public void onMisTap(int lane) {
        // Mis-tap penalty - game over
        scoreSystem.registerMiss();
        hitFeedback = "MISS";
        hitFeedbackTime = System.currentTimeMillis();
        handleGameOver();
    }

    private void handleGameOver() {
        setState(GameView.GameState.GAME_OVER);
        audioManager.pause();
        int stars = scoreSystem.getStarRating();
        mainHandler.post(() -> listener.onGameOver(
                scoreSystem.getScore(), scoreSystem.getMaxCombo(), stars, songTitle, songArtist));
    }

    // GameView.GameCallback implementation
    @Override
    public float getCurrentAudioTime() {
        return audioManager.getCurrentPosition() / 1000f;
    }

    @Override
    public void onTouchEvent(MotionEvent event) {
        if (currentState == GameView.GameState.PLAYING && inputHandler != null) {
            update();
            inputHandler.handleTouch(event, tileSpawner.getActiveTiles(), getCurrentAudioTime());
        }
    }

    @Override
    public GameView.GameState getGameState() {
        return currentState;
    }

    @Override
    public int getScore() {
        return scoreSystem.getScore();
    }

    @Override
    public int getCombo() {
        return scoreSystem.getCombo();
    }

    @Override
    public List<Tile> getActiveTiles() {
        return tileSpawner != null ? tileSpawner.getActiveTiles() : null;
    }

    @Override
    public TileSpawner getTileSpawner() {
        return tileSpawner;
    }

    @Override
    public String getHitFeedback() {
        return hitFeedback;
    }

    @Override
    public long getHitFeedbackTime() {
        return hitFeedbackTime;
    }

    private void setState(GameView.GameState state) {
        this.currentState = state;
        mainHandler.post(() -> listener.onStateChanged(state));
    }

    public void pause() {
        if (currentState == GameView.GameState.PLAYING) {
            setState(GameView.GameState.PAUSED);
            audioManager.pause();
        }
    }

    public void resume() {
        if (currentState == GameView.GameState.PAUSED) {
            setState(GameView.GameState.PLAYING);
            audioManager.resume();
        }
    }

    public void release() {
        audioManager.release();
        if (tileSpawner != null) tileSpawner.reset();
        if (gameView != null) gameView.stopThread();
    }
}
