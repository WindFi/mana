package me.sunzheng.mana.videoplayer

import me.sunzheng.mana.VideoPlayerActivity

/**
 * Configuration constants for video player.
 * Centralizes all configuration values to improve maintainability.
 */
object VideoPlayerConfig {
    
    // ==================== Bundle Keys ====================
    
    /**
     * Bundle key for episode position in playlist.
     */
    val KEY_POSITION_INT = "${VideoPlayerActivity::class.simpleName}_position"
    
    /**
     * Bundle key for bangumi ID.
     */
    val KEY_BANGUMI_ID_STR = "${VideoPlayerActivity::class.simpleName}_bangumiId"
    
    /**
     * Bundle key for episode ID.
     */
    val KEY_EPISODE_ID_STR = "${VideoPlayerActivity::class.simpleName}_episodeId"
    
    // ==================== Watch Progress ====================
    
    /**
     * Interval for auto-saving watch progress in milliseconds.
     * Default: 1 minute (60,000 ms)
     */
    const val AUTOSAVE_INTERVAL_MS = 1000L * 60
    
    /**
     * Handler message ID for watch progress logging.
     */
    const val MESSAGE_WATCH_PROGRESS = 1
    
    // ==================== Cache Configuration ====================
    
    /**
     * Cache directory name for media files.
     */
    const val CACHE_DIR_NAME = "mediaCache"
    
    /**
     * Maximum cache size in bytes.
     * Default: 1 GB (1024 * 1024 * 1024 bytes)
     */
    const val MAX_CACHE_SIZE_BYTES = 1024L * 1024 * 1024
    
    /**
     * Cache data sink buffer size in bytes.
     * Default: 1 GB (1024 * 1024 * 1024 bytes)
     */
    const val CACHE_BUFFER_SIZE_BYTES = 1024L * 1024 * 1024
    
    // ==================== Load Control Configuration ====================
    
    /**
     * Minimum buffer duration in milliseconds.
     * Default: 20 seconds
     */
    const val MIN_BUFFER_DURATION_MS = 1000L * 20
    
    /**
     * Maximum buffer duration in milliseconds.
     * Default: 24 minutes (60 * 24 seconds)
     */
    const val MAX_BUFFER_DURATION_MS = 1000L * 60 * 24  // Duration in milliseconds
    
    /**
     * Buffer for playback start in milliseconds.
     * Default: 10 seconds
     */
    const val BUFFER_FOR_PLAYBACK_MS = 1000L * 10
    
    /**
     * Buffer for playback after rebuffer in milliseconds.
     * Default: 10 seconds
     */
    const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 1000L * 10
    
    // ==================== Preferences ====================
    
    /**
     * SharedPreferences key for auto-play setting.
     */
    const val PREF_KEY_AUTOPLAY = "isAutoplay"
    
    // ==================== Media Session ====================
    
    /**
     * Media session ID prefix.
     * Will be appended with timestamp for uniqueness.
     */
    const val MEDIA_SESSION_ID_PREFIX = "VideoPlayerSession"
    
    // ==================== UI Configuration ====================
    
    /**
     * Controller auto-show timeout in milliseconds.
     * Default: 3 seconds
     */
    const val CONTROLLER_AUTO_SHOW_TIMEOUT_MS = 3000L
    
    /**
     * Progress indicator visibility duration in milliseconds.
     * Default: 3 seconds
     */
    const val PROGRESS_INDICATOR_DURATION_MS = 3000L
    
    /**
     * Value indicator visibility duration in milliseconds.
     * Used for brightness and volume indicators.
     * Default: 3 seconds
     */
    const val VALUE_INDICATOR_DURATION_MS = 3000L
    
    /**
     * Animation delay for hiding views in milliseconds.
     * Default: 50 ms
     */
    const val HIDE_ANIMATION_DELAY_MS = 50L
    
    // ==================== Gesture Configuration ====================
    
    /**
     * Gesture measure length threshold.
     * Used for determining valid gesture area.
     */
    const val GESTURE_MEASURE_LENGTH = 72.0f
    
    // ==================== Brightness Configuration ====================
    
    /**
     * Brightness adjustment factor.
     * Used to convert gesture delta to brightness value.
     */
    const val BRIGHTNESS_ADJUSTMENT_FACTOR = 17.0f
    
    /**
     * Maximum brightness value (0-255).
     */
    const val MAX_BRIGHTNESS = 255f
    
    /**
     * Minimum brightness value (0-255).
     */
    const val MIN_BRIGHTNESS = 0f
    
    /**
     * Brightness threshold for low brightness icon.
     */
    const val BRIGHTNESS_LOW_THRESHOLD = 30
    
    /**
     * Brightness threshold for half brightness icon.
     */
    const val BRIGHTNESS_HALF_THRESHOLD = 75
    
    // ==================== Volume Configuration ====================
    
    /**
     * Volume display multiplier.
     * Used to convert volume level to display percentage.
     */
    const val VOLUME_DISPLAY_MULTIPLIER = 4
    
    // ==================== Seek Configuration ====================
    
    /**
     * Seek step duration in milliseconds.
     * Amount of time to seek forward/backward per gesture unit.
     */
    const val SEEK_STEP_DURATION_MS = 5000L
    
    // ==================== Watch Progress Thresholds ====================
    
    /**
     * Percentage threshold for marking episode as finished.
     * If user watches more than this percentage, episode is considered finished.
     * Default: 95%
     */
    const val EPISODE_FINISHED_THRESHOLD = 0.95f
}

