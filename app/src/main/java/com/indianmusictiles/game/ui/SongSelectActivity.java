package com.indianmusictiles.game.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.indianmusictiles.game.R;
import com.indianmusictiles.game.audio.AudioManager;
import com.indianmusictiles.game.model.SongInfo;

import java.util.List;

public class SongSelectActivity extends AppCompatActivity implements SongAdapter.OnSongClickListener {
    private static final int PERMISSION_REQUEST_CODE = 101;

    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private LinearLayout emptyState;
    private EditText searchInput;
    private Button btnGrantPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_select);

        recyclerView = findViewById(R.id.songRecyclerView);
        emptyState = findViewById(R.id.emptyState);
        searchInput = findViewById(R.id.searchInput);
        btnGrantPermission = findViewById(R.id.btnGrantPermission);
        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageButton btnFilter = findViewById(R.id.btnFilter);

        adapter = new SongAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        btnFilter.setOnClickListener(v -> {
            if (searchInput.getVisibility() == View.VISIBLE) {
                searchInput.setVisibility(View.GONE);
                searchInput.setText("");
            } else {
                searchInput.setVisibility(View.VISIBLE);
                searchInput.requestFocus();
            }
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnGrantPermission.setOnClickListener(v -> requestAudioPermission());

        loadSongs();
    }

    private void loadSongs() {
        if (checkAudioPermission()) {
            AudioManager audioManager = new AudioManager(this);
            List<SongInfo> songs = audioManager.scanDeviceSongs();
            if (songs.isEmpty()) {
                showEmptyState("No songs found on device");
            } else {
                emptyState.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.setSongs(songs);
            }
        } else {
            showEmptyState("Permission required to access music files");
            btnGrantPermission.setVisibility(View.VISIBLE);
        }
    }

    private void showEmptyState(String message) {
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        TextView emptyText = findViewById(R.id.emptyText);
        emptyText.setText(message);
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
                btnGrantPermission.setVisibility(View.GONE);
                loadSongs();
            }
        }
    }

    @Override
    public void onSongClick(SongInfo song) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("song_uri", song.getContentUri().toString());
        intent.putExtra("song_title", song.getTitle());
        intent.putExtra("song_artist", song.getArtist());
        intent.putExtra("song_hash", song.getUriHash());
        startActivity(intent);
    }
}
