package com.indianmusictiles.game.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.indianmusictiles.game.R;

public class GameOverActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over);

        TextView resultTitle = findViewById(R.id.resultTitle);
        TextView songTitle = findViewById(R.id.songTitle);
        TextView songArtist = findViewById(R.id.songArtist);
        TextView finalScore = findViewById(R.id.finalScore);
        TextView maxCombo = findViewById(R.id.maxCombo);
        TextView star1 = findViewById(R.id.star1);
        TextView star2 = findViewById(R.id.star2);
        TextView star3 = findViewById(R.id.star3);
        ImageButton btnHome = findViewById(R.id.btnHome);
        Button btnRetry = findViewById(R.id.btnRetry);
        ImageButton btnShare = findViewById(R.id.btnShare);

        // Get data from intent
        int score = getIntent().getIntExtra("score", 0);
        int maxComboVal = getIntent().getIntExtra("max_combo", 0);
        int stars = getIntent().getIntExtra("stars", 0);
        String title = getIntent().getStringExtra("song_title");
        String artist = getIntent().getStringExtra("song_artist");
        boolean isGameOver = getIntent().getBooleanExtra("is_game_over", true);

        // Set title
        resultTitle.setText(isGameOver ? "GAME OVER" : "COMPLETE!");
        resultTitle.setTextColor(getColor(isGameOver ? R.color.accent_purple : R.color.accent_gold));

        // Set song info
        songTitle.setText(title);
        songArtist.setText(artist);

        // Set score
        finalScore.setText(String.valueOf(score));
        maxCombo.setText(String.valueOf(maxComboVal));

        // Set stars with animation
        int starColor = getColor(R.color.star_filled);
        if (stars >= 1) animateStar(star1, starColor, 300);
        if (stars >= 2) animateStar(star2, starColor, 600);
        if (stars >= 3) animateStar(star3, starColor, 900);

        // Buttons
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        btnRetry.setOnClickListener(v -> {
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("song_uri", getIntent().getStringExtra("song_uri"));
            intent.putExtra("song_title", title);
            intent.putExtra("song_artist", artist);
            intent.putExtra("song_hash", getIntent().getStringExtra("song_hash"));
            startActivity(intent);
            finish();
        });

        btnShare.setOnClickListener(v -> {
            String shareText = String.format(
                    "I scored %d points on %s in Indian Music Tiles! 🎵\n%s combo! Can you beat me?",
                    score, title, maxComboVal);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            startActivity(Intent.createChooser(shareIntent, "Share score"));
        });

        // Entrance animation
        View rootView = findViewById(android.R.id.content);
        rootView.setAlpha(0f);
        rootView.animate().alpha(1f).setDuration(500).start();
    }

    private void animateStar(TextView star, int color, int delay) {
        star.postDelayed(() -> {
            star.setTextColor(color);
            star.setScaleX(0f);
            star.setScaleY(0f);
            star.animate()
                    .scaleX(1.3f).scaleY(1.3f)
                    .setDuration(300)
                    .withEndAction(() ->
                            star.animate()
                                    .scaleX(1f).scaleY(1f)
                                    .setDuration(200)
                                    .start()
                    ).start();
        }, delay);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
