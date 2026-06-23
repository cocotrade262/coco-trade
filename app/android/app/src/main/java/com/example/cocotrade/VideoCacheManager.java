package com.example.cocotrade;

import android.content.Context;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import java.io.File;

@UnstableApi
public class VideoCacheManager {
    private static SimpleCache sDownloadCache;
    private static final long MAX_CACHE_SIZE = 100 * 1024 * 1024; // 100MB

    public static synchronized SimpleCache getCache(Context context) {
        if (sDownloadCache == null) {
            File cacheDir = new File(context.getCacheDir(), "video_cache");
            LeastRecentlyUsedCacheEvictor evictor = new LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE);
            sDownloadCache = new SimpleCache(cacheDir, evictor, new StandaloneDatabaseProvider(context));
        }
        return sDownloadCache;
    }
}
