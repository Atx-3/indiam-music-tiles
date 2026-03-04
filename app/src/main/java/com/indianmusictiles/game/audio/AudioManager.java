package com.indianmusictiles.game.audio;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.content.ContentUris;

import com.indianmusictiles.game.model.SongInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AudioManager {
    private static final String TAG = "AudioManager";
    private static final Uri ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart");

    private final Context context;
    private MediaPlayer mediaPlayer;
    private boolean isPrepared = false;
    private boolean isPlaying = false;

    public AudioManager(Context context) {
        this.context = context;
    }

    public List<SongInfo> scanDeviceSongs() {
        List<SongInfo> songs = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " +
                MediaStore.Audio.Media.DURATION + " > 30000"; // > 30 seconds

        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        try (Cursor cursor = resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder)) {

            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String title = cursor.getString(titleCol);
                    String artist = cursor.getString(artistCol);
                    long duration = cursor.getLong(durationCol);
                    long albumId = cursor.getLong(albumIdCol);

                    Uri contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                    Uri albumArtUri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId);

                    songs.add(new SongInfo(title, artist, duration, contentUri, albumArtUri));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scanning songs", e);
        }

        return songs;
    }

    public void prepareAudio(Uri uri) throws IOException {
        release();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setDataSource(context, uri);
        mediaPlayer.prepare();
        isPrepared = true;
    }

    public void play() {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.start();
            isPlaying = true;
        }
    }

    public void pause() {
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
        }
    }

    public void resume() {
        if (mediaPlayer != null && isPrepared && !isPlaying) {
            mediaPlayer.start();
            isPlaying = true;
        }
    }

    public void seekTo(int positionMs) {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.seekTo(positionMs);
        }
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null && isPrepared) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public int getDuration() {
        if (mediaPlayer != null && isPrepared) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    public boolean isPlaying() {
        return mediaPlayer != null && isPlaying && mediaPlayer.isPlaying();
    }

    public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(listener);
        }
    }

    public void release() {
        if (mediaPlayer != null) {
            if (isPlaying) {
                try {
                    mediaPlayer.stop();
                } catch (Exception ignored) {}
            }
            mediaPlayer.release();
            mediaPlayer = null;
            isPrepared = false;
            isPlaying = false;
        }
    }
}
