package com.indianmusictiles.game.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.indianmusictiles.game.R;
import com.indianmusictiles.game.engine.GameManager;
import com.indianmusictiles.game.engine.GameView;

public class GameActivity extends AppCompatActivity implements GameManager.GameEventListener {
    private GameManager gameManager;
    private GameView gameView;
    private FrameLayout loadingOverlay;
    private FrameLayout countdownOverlay;
    private TextView countdownText;
    private TextView scoreText;
    private TextView comboText;
    private ImageButton btnPause;

    private String songTitle;
    private String songArtist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_game);

        gameView = findViewById(R.id.gameView);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        countdownOverlay = findViewById(R.id.countdownOverlay);
        countdownText = findViewById(R.id.countdownText);
        scoreText = findViewById(R.id.scoreText);
        comboText = findViewById(R.id.comboText);
        btnPause = findViewById(R.id.btnPause);

        String uriString = getIntent().getStringExtra("song_uri");
        songTitle = getIntent().getStringExtra("song_title");
        songArtist = getIntent().getStringExtra("song_artist");
        String songHash = getIntent().getStringExtra("song_hash");

        Uri songUri = Uri.parse(uriString);

        btnPause.setOnClickListener(v -> {
            if (gameManager != null) {
                gameManager.pause();
            }
        });

        gameManager = new GameManager(this, this);

        // Wait for GameView surface to be ready, then init
        gameView.post(() -> {
            gameManager.init(gameView, songUri, songTitle, songArtist, songHash);
        });
    }

    @Override
    public void onStateChanged(GameView.GameState state) {
        runOnUiThread(() -> {
            switch (state) {
                case LOADING:
                    loadingOverlay.setVisibility(View.VISIBLE);
                    countdownOverlay.setVisibility(View.GONE);
                    break;
                case COUNTDOWN:
                    loadingOverlay.setVisibility(View.GONE);
                    countdownOverlay.setVisibility(View.VISIBLE);
                    startCountdownAnimation();
                    break;
                case PLAYING:
                    countdownOverlay.setVisibility(View.GONE);
                    loadingOverlay.setVisibility(View.GONE);
                    btnPause.setVisibility(View.VISIBLE);
                    break;
                case PAUSED:
                    countdownOverlay.setVisibility(View.VISIBLE);
                    countdownText.setText("▶");
                    countdownOverlay.setOnClickListener(v -> {
                        if (gameManager != null) {
                            gameManager.resume();
                        }
                    });
                    break;
                case GAME_OVER:
                case SONG_COMPLETE:
                    break;
            }
        });
    }

    private void startCountdownAnimation() {
        countdownText.setText("3");
        countdownText.postDelayed(() -> {
            countdownText.setText("2");
            countdownText.animate().scaleX(1.5f).scaleY(1.5f).setDuration(200)
                    .withEndAction(() ->
                            countdownText.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                    ).start();
        }, 600);
        countdownText.postDelayed(() -> {
            countdownText.setText("1");
            countdownText.animate().scaleX(1.5f).scaleY(1.5f).setDuration(200)
                    .withEndAction(() ->
                            countdownText.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                    ).start();
        }, 1200);
        countdownText.postDelayed(() -> {
            countdownText.setText("GO!");
            countdownText.animate().scaleX(2f).scaleY(2f).alpha(0f).setDuration(400)
                    .withEndAction(() -> {
                        countdownText.setAlpha(1f);
                        countdownText.setScaleX(1f);
                        countdownText.setScaleY(1f);
                    }).start();
        }, 1800);
    }

    @Override
    public void onScoreChanged(int score, int combo) {
        runOnUiThread(() -> {
            scoreText.setText(String.valueOf(score));
            if (combo > 1) {
                comboText.setText(combo + "x COMBO");
                comboText.setVisibility(View.VISIBLE);
            } else {
                comboText.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    public void onGameOver(int score, int maxCombo, int stars, String title, String artist) {
        runOnUiThread(() -> {
            Intent intent = new Intent(this, GameOverActivity.class);
            intent.putExtra("score", score);
            intent.putExtra("max_combo", maxCombo);
            intent.putExtra("stars", stars);
            intent.putExtra("song_title", title);
            intent.putExtra("song_artist", artist);
            intent.putExtra("is_game_over", true);
            intent.putExtra("song_uri", getIntent().getStringExtra("song_uri"));
            intent.putExtra("song_hash", getIntent().getStringExtra("song_hash"));
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onSongComplete(int score, int maxCombo, int stars, String title, String artist) {
        runOnUiThread(() -> {
            Intent intent = new Intent(this, GameOverActivity.class);
            intent.putExtra("score", score);
            intent.putExtra("max_combo", maxCombo);
            intent.putExtra("stars", stars);
            intent.putExtra("song_title", title);
            intent.putExtra("song_artist", artist);
            intent.putExtra("is_game_over", false);
            intent.putExtra("song_uri", getIntent().getStringExtra("song_uri"));
            intent.putExtra("song_hash", getIntent().getStringExtra("song_hash"));
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onAnalysisProgress(int percent) {
        // Could update a progress bar in the loading overlay
    }

    @Override
    public void onHitFeedback(String feedback) {
        // Handled by GameView
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameManager != null) {
            gameManager.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameManager != null) {
            gameManager.release();
        }
    }

    @Override
    public void onBackPressed() {
        if (gameManager != null) {
            gameManager.release();
        }
        super.onBackPressed();
    }
}
