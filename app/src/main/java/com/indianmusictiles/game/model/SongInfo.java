package com.indianmusictiles.game.model;

import android.net.Uri;

public class SongInfo {
    private final String title;
    private final String artist;
    private final long duration;
    private final Uri contentUri;
    private final Uri albumArtUri;

    public SongInfo(String title, String artist, long duration, Uri contentUri, Uri albumArtUri) {
        this.title = title != null ? title : "Unknown";
        this.artist = artist != null ? artist : "Unknown Artist";
        this.duration = duration;
        this.contentUri = contentUri;
        this.albumArtUri = albumArtUri;
    }

    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public long getDuration() { return duration; }
    public Uri getContentUri() { return contentUri; }
    public Uri getAlbumArtUri() { return albumArtUri; }

    public String getFormattedDuration() {
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    public String getUriHash() {
        return String.valueOf(contentUri.toString().hashCode());
    }
}
