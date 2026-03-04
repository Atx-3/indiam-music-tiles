package com.indianmusictiles.game.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.indianmusictiles.game.R;
import com.indianmusictiles.game.model.SongInfo;

import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
    public interface OnSongClickListener {
        void onSongClick(SongInfo song);
    }

    private List<SongInfo> songs;
    private List<SongInfo> allSongs;
    private final OnSongClickListener listener;

    public SongAdapter(OnSongClickListener listener) {
        this.listener = listener;
        this.songs = new ArrayList<>();
        this.allSongs = new ArrayList<>();
    }

    public void setSongs(List<SongInfo> songs) {
        this.allSongs = new ArrayList<>(songs);
        this.songs = new ArrayList<>(songs);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        if (query == null || query.isEmpty()) {
            songs = new ArrayList<>(allSongs);
        } else {
            String lower = query.toLowerCase();
            songs = new ArrayList<>();
            for (SongInfo song : allSongs) {
                if (song.getTitle().toLowerCase().contains(lower) ||
                    song.getArtist().toLowerCase().contains(lower)) {
                    songs.add(song);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        SongInfo song = songs.get(position);
        holder.bind(song);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    class SongViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final TextView artistView;
        private final TextView durationView;
        private final ImageView albumArt;
        private final TextView albumArtPlaceholder;

        SongViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.songTitle);
            artistView = itemView.findViewById(R.id.songArtist);
            durationView = itemView.findViewById(R.id.songDuration);
            albumArt = itemView.findViewById(R.id.albumArt);
            albumArtPlaceholder = itemView.findViewById(R.id.albumArtPlaceholder);
        }

        void bind(SongInfo song) {
            titleView.setText(song.getTitle());
            artistView.setText(song.getArtist());
            durationView.setText(song.getFormattedDuration());

            if (song.getAlbumArtUri() != null) {
                try {
                    albumArt.setImageURI(song.getAlbumArtUri());
                    if (albumArt.getDrawable() != null) {
                        albumArtPlaceholder.setVisibility(View.GONE);
                        albumArt.setVisibility(View.VISIBLE);
                    } else {
                        albumArt.setVisibility(View.GONE);
                        albumArtPlaceholder.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    albumArt.setVisibility(View.GONE);
                    albumArtPlaceholder.setVisibility(View.VISIBLE);
                }
            } else {
                albumArt.setVisibility(View.GONE);
                albumArtPlaceholder.setVisibility(View.VISIBLE);
            }

            itemView.setOnClickListener(v -> listener.onSongClick(song));

            // Simple press animation
            itemView.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).start();
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                        break;
                }
                return false;
            });
        }
    }
}
