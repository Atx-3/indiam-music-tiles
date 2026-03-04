package com.indianmusictiles.game.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.indianmusictiles.game.R;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnPlay = findViewById(R.id.btnPlay);
        Button btnRecent = findViewById(R.id.btnRecentlyPlayed);
        ImageButton btnSettings = findViewById(R.id.btnSettings);

        btnPlay.setOnClickListener(v -> {
            if (checkAudioPermission()) {
                navigateToSongSelect();
            } else {
                requestAudioPermission();
            }
        });

        btnRecent.setOnClickListener(v -> {
            if (checkAudioPermission()) {
                navigateToSongSelect();
            } else {
                requestAudioPermission();
            }
        });

        btnSettings.setOnClickListener(v -> {
            // Settings placeholder
        });

        // Animate play button
        btnPlay.postDelayed(() -> {
            btnPlay.animate()
                .scaleX(1.05f).scaleY(1.05f)
                .setDuration(800)
                .withEndAction(() ->
                    btnPlay.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(800)
                        .start())
                .start();
        }, 500);
    }

    private boolean checkAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_AUDIO}, PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                navigateToSongSelect();
            }
        }
    }

    private void navigateToSongSelect() {
        Intent intent = new Intent(this, SongSelectActivity.class);
        startActivity(intent);
    }
}
