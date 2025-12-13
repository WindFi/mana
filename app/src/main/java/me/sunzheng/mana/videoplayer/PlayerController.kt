package me.sunzheng.mana.videoplayer

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import java.io.File

/**
 * Manages ExoPlayer instance, configuration, and playback control.
 * Handles player initialization, cache setup, data source configuration, and lifecycle management.
 */
@OptIn(UnstableApi::class)
class PlayerController(
    private val context: Context
) {
    
    /**
     * Cache directory file.
     */
    private val cacheFile: File by lazy {
        File(context.externalCacheDir, VideoPlayerConfig.CACHE_DIR_NAME)
    }
    
    /**
     * Load control for buffering configuration.
     */
    private val loadControl: LoadControl by lazy {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                VideoPlayerConfig.MIN_BUFFER_DURATION_MS.toInt(),
                VideoPlayerConfig.MAX_BUFFER_DURATION_MS.toInt(),
                VideoPlayerConfig.BUFFER_FOR_PLAYBACK_MS.toInt(),
                VideoPlayerConfig.BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS.toInt()
            )
            .build()
    }
    
    /**
     * Media cache for offline playback support.
     */
    val cache: Cache by lazy {
        SimpleCache(
            cacheFile,
            LeastRecentlyUsedCacheEvictor(VideoPlayerConfig.MAX_CACHE_SIZE_BYTES),
            StandaloneDatabaseProvider(context)
        )
    }
    
    /**
     * Data source factory with cache support.
     */
    val dataSourceFactory: DataSource.Factory by lazy {
        CacheDataSource.Factory()
            .setCache(cache)
            .setCacheWriteDataSinkFactory {
                CacheDataSink.Factory()
                    .setBufferSize(VideoPlayerConfig.CACHE_BUFFER_SIZE_BYTES.toInt())
                    .createDataSink()
            }
            .setCacheReadDataSourceFactory(
                DefaultHttpDataSource.Factory()
                    .setUserAgent(Util.getUserAgent(context, context.packageName))
            )
    }
    
    /**
     * ExoPlayer instance.
     */
    val player: Player by lazy {
        ExoPlayer.Builder(context)
            .setRenderersFactory(DefaultRenderersFactory(context))
            .setTrackSelector(DefaultTrackSelector(context))
            .setLoadControl(loadControl)
            .build()
    }
    
    /**
     * Prepares the player for playback.
     * Call this before starting playback.
     */
    fun prepare() {
        player.prepare()
    }
    
    /**
     * Starts playback.
     */
    fun play() {
        player.playWhenReady = true
        player.play()
    }
    
    /**
     * Pauses playback.
     */
    fun pause() {
        player.pause()
    }
    
    /**
     * Stops playback and releases resources.
     */
    fun stop() {
        player.stop()
    }
    
    /**
     * Releases the player and cache resources.
     * Call this when the player is no longer needed (e.g., in onDestroy).
     */
    fun release() {
        player.release()
        try {
            cache.release()
        } catch (e: Exception) {
            // Cache may already be released, ignore
        }
    }
    
    /**
     * Adds a player listener.
     * @param listener The listener to add
     */
    fun addListener(listener: Player.Listener) {
        player.addListener(listener)
    }
    
    /**
     * Removes a player listener.
     * @param listener The listener to remove
     */
    fun removeListener(listener: Player.Listener) {
        player.removeListener(listener)
    }
    
    /**
     * Seeks to a specific position.
     * @param positionMs The position in milliseconds
     */
    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }
    
    /**
     * Gets the current playback position.
     * @return Current position in milliseconds
     */
    fun getCurrentPosition(): Long {
        return player.currentPosition
    }
    
    /**
     * Gets the duration of the current media item.
     * @return Duration in milliseconds, or C.TIME_UNSET if unknown
     */
    fun getDuration(): Long {
        return player.duration
    }
    
    /**
     * Checks if the player is currently playing.
     * @return true if playing, false otherwise
     */
    fun isPlaying(): Boolean {
        return player.isPlaying
    }
    
    /**
     * Checks if playback is ready.
     * @return true if ready, false otherwise
     */
    fun isPlayWhenReady(): Boolean {
        return player.playWhenReady
    }
    
    /**
     * Sets whether playback should proceed when ready.
     * @param playWhenReady Whether to play when ready
     */
    fun setPlayWhenReady(playWhenReady: Boolean) {
        player.playWhenReady = playWhenReady
    }
}

