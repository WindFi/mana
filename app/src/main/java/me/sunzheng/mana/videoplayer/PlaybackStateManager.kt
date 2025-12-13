package me.sunzheng.mana.videoplayer

import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaDescriptionCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.media3.common.Player
import me.sunzheng.mana.core.net.v2.database.EpisodeEntity
import me.sunzheng.mana.core.net.v2.database.WatchProgressEntity
import me.sunzheng.mana.videoplayer.VideoPlayerConfig
import me.sunzheng.mana.videoplayer.VideoPlayerVideoModel

/**
 * Manages playback state tracking, watch progress management, and auto-save functionality.
 * Handles periodic saving of watch progress and coordinates with ViewModel.
 * 
 * Can work with either PlayerController (direct) or Player (from MediaController).
 */
class PlaybackStateManager(
    private val player: Player,
    private val viewModel: VideoPlayerVideoModel,
    private val lifecycleOwner: LifecycleOwner
) {
    
    /**
     * Handler for scheduling watch progress auto-save.
     */
    private val handler: Handler = Handler(Looper.getMainLooper())
    
    /**
     * Runnable that saves watch progress periodically.
     */
    private val saveWatchProgressRunnable = Runnable {
        saveWatchProgress()
        scheduleNextSave()
    }
    
    /**
     * Current episode entity being played.
     */
    private var currentEpisodeEntity: EpisodeEntity? = null
    
    /**
     * Current watch progress entity.
     */
    private var currentWatchProgressEntity: WatchProgressEntity? = null
    
    /**
     * Current bangumi ID.
     */
    var bangumiId: String = ""
        private set
    
    /**
     * Starts tracking watch progress for the current episode.
     * 
     * @param episodeEntity The episode being played
     * @param watchProgressEntity Current watch progress (if any)
     * @param bangumiId The bangumi ID (will be extracted from episodeEntity if not provided)
     */
    fun startTracking(
        episodeEntity: EpisodeEntity,
        watchProgressEntity: WatchProgressEntity? = null,
        bangumiId: String? = null
    ) {
        currentEpisodeEntity = episodeEntity
        currentWatchProgressEntity = watchProgressEntity
        
        // Fix: Extract bangumiId from episodeEntity if not provided
        this.bangumiId = bangumiId ?: (episodeEntity.bangumiId?.toString() ?: "")
        
        // Start auto-save schedule
        scheduleNextSave()
    }
    
    /**
     * Stops tracking watch progress.
     */
    fun stopTracking() {
        // Save progress one last time before stopping
        saveWatchProgress()
        
        // Cancel scheduled saves
        handler.removeCallbacks(saveWatchProgressRunnable)
        
        currentEpisodeEntity = null
        currentWatchProgressEntity = null
    }
    
    /**
     * Saves the current watch progress.
     */
    private fun saveWatchProgress() {
        val episodeEntity = currentEpisodeEntity ?: return
        val bangumiId = this.bangumiId.takeIf { it.isNotEmpty() } ?: return
        
        val currentPosition = player.currentPosition
        val duration = player.duration
        
        if (duration <= 0) return // Don't save if duration is unknown
        
        viewModel.updateWatchProgress(
            bangumiId = bangumiId,
            episodeEntity = episodeEntity,
            lastWatchPosition = (currentPosition / 1000).toFloat(),
            duration = (duration / 1000).toFloat(),
            watchprocessEntity = currentWatchProgressEntity
        ).observe(lifecycleOwner) {
            // Update current watch progress entity if we got a new one
            // Note: This would need to be handled based on the actual return type
        }
    }
    
    /**
     * Schedules the next auto-save.
     */
    private fun scheduleNextSave() {
        handler.removeCallbacks(saveWatchProgressRunnable)
        handler.postDelayed(
            saveWatchProgressRunnable,
            VideoPlayerConfig.AUTOSAVE_INTERVAL_MS
        )
    }
    
    /**
     * Manually triggers a save (e.g., when episode changes or playback stops).
     */
    fun saveNow() {
        saveWatchProgress()
    }
    
    /**
     * Updates the current watch progress entity.
     * Called when watch progress is fetched from the server.
     */
    fun updateWatchProgressEntity(watchProgressEntity: WatchProgressEntity?) {
        currentWatchProgressEntity = watchProgressEntity
    }
    
    /**
     * Gets the current episode entity.
     */
    fun getCurrentEpisodeEntity(): EpisodeEntity? = currentEpisodeEntity
    
    /**
     * Gets the current watch progress entity.
     */
    fun getCurrentWatchProgressEntity(): WatchProgressEntity? = currentWatchProgressEntity
    
    /**
     * Cleans up resources.
     * Should be called when the manager is no longer needed (e.g., in onDestroy).
     */
    fun release() {
        stopTracking()
        handler.removeCallbacksAndMessages(null)
    }
}

