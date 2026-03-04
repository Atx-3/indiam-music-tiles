package com.indianmusictiles.game.cache;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.LruCache;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.indianmusictiles.game.engine.BeatMap;

import java.lang.reflect.Type;
import java.util.Map;

public class CacheManager {
    private static final String TAG = "CacheManager";
    private static final String PREFS_NAME = "beat_cache";
    private static final String CACHE_KEYS = "cache_keys";
    private static final int MAX_CACHE_SIZE = 100;

    private final SharedPreferences prefs;
    private final Gson gson;
    private final LruCache<String, BeatMap> memoryCache;

    public CacheManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.memoryCache = new LruCache<>(20);
    }

    public BeatMap get(String songHash) {
        // Check memory cache first
        BeatMap cached = memoryCache.get(songHash);
        if (cached != null) {
            Log.d(TAG, "Memory cache hit: " + songHash);
            return cached;
        }

        // Check disk cache
        String json = prefs.getString("beat_" + songHash, null);
        if (json != null) {
            try {
                BeatMap beatMap = gson.fromJson(json, BeatMap.class);
                memoryCache.put(songHash, beatMap);
                Log.d(TAG, "Disk cache hit: " + songHash);
                return beatMap;
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse cached beat map", e);
                prefs.edit().remove("beat_" + songHash).apply();
            }
        }

        return null;
    }

    public void put(String songHash, BeatMap beatMap) {
        memoryCache.put(songHash, beatMap);

        try {
            String json = gson.toJson(beatMap);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("beat_" + songHash, json);

            // Track keys for LRU eviction
            String keysJson = prefs.getString(CACHE_KEYS, "[]");
            Type type = new TypeToken<java.util.ArrayList<String>>(){}.getType();
            java.util.ArrayList<String> keys = gson.fromJson(keysJson, type);

            keys.remove(songHash);
            keys.add(0, songHash);

            // Evict oldest if over limit
            while (keys.size() > MAX_CACHE_SIZE) {
                String oldKey = keys.remove(keys.size() - 1);
                editor.remove("beat_" + oldKey);
            }

            editor.putString(CACHE_KEYS, gson.toJson(keys));
            editor.apply();

            Log.d(TAG, "Cached beat map: " + songHash + " (" + beatMap.getBeatCount() + " beats)");
        } catch (Exception e) {
            Log.e(TAG, "Failed to cache beat map", e);
        }
    }

    public boolean has(String songHash) {
        return memoryCache.get(songHash) != null ||
               prefs.contains("beat_" + songHash);
    }

    public void clear() {
        memoryCache.evictAll();
        prefs.edit().clear().apply();
    }
}
