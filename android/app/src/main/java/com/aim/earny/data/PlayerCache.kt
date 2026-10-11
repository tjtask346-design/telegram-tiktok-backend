package com.aim.earny.data

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * App-wide singleton media cache shared by the main player and the preloader.
 * Media3 requires a single SimpleCache instance per cache directory.
 */
object PlayerCache {
    private const val MAX_BYTES = 300L * 1024 * 1024  // 300 MB
    private var instance: SimpleCache? = null

    @Synchronized
    fun get(ctx: Context): SimpleCache {
        instance?.let { return it }
        val dir = File(ctx.cacheDir, "earny_media")
        if (!dir.exists()) dir.mkdirs()
        val cache = SimpleCache(
            dir,
            LeastRecentlyUsedCacheEvictor(MAX_BYTES),
            StandaloneDatabaseProvider(ctx.applicationContext)
        )
        instance = cache
        return cache
    }
}
