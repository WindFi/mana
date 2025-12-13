package me.sunzheng.mana.videoplayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.AUDIOFOCUS_GAIN
import android.media.AudioManager.AUDIOFOCUS_LOSS
import android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
import android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
import android.media.AudioManager.STREAM_MUSIC
import android.os.Build
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import me.sunzheng.mana.R
import me.sunzheng.mana.videoplayer.PlayerController
import me.sunzheng.mana.videoplayer.VideoPlayerConfig

/**
 * MediaSessionService for background video playback.
 * Manages ExoPlayer instance and MediaSession for background playback support.
 * 
 * Follows Android Media3 best practices for background playback:
 * https://developer.android.com/media/media3/session/background-playback
 */
@OptIn(UnstableApi::class)
class VideoPlaybackService : MediaSessionService() {
    
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "video_playback_channel"
        private const val NOTIFICATION_ID = 1
        
        /**
         * Action to start the service.
         */
        const val ACTION_START = "me.sunzheng.mana.videoplayer.START"
        
        /**
         * Action to stop the service.
         */
        const val ACTION_STOP = "me.sunzheng.mana.videoplayer.STOP"
    }
    
    /**
     * Player controller managing ExoPlayer instance.
     */
    private var playerController: PlayerController? = null
    
    /**
     * MediaSession for exposing playback control to system and other apps.
     */
    private var mediaSession: MediaSession? = null
    
    /**
     * Audio manager for handling audio focus.
     */
    private val audioManager: AudioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    
    /**
     * Remembers if playback is active before losing audio focus.
     * Used to restore playback state when audio focus is regained.
     */
    private var isPlayingBeforeFocusLoss = false
    
    /**
     * Audio focus request for Android O+.
     */
    private var audioFocusRequest: AudioFocusRequest? = null
    
    /**
     * Flag to track if audio focus has been requested (for Android < O).
     */
    private var hasAudioFocus = false
    
    /**
     * Player listener for audio focus management.
     * Stored to allow removal to prevent memory leaks.
     */
    private var audioFocusPlayerListener: Player.Listener? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        // Start foreground service immediately to avoid ANR
        // Must be called within 5 seconds of startForegroundService()
        // This ensures we meet Android's requirement for foreground services
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Initialize player controller
        playerController = PlayerController(this)
        
        // Create MediaSession with custom callback for controller configuration
        mediaSession = MediaSession.Builder(this, playerController!!.player)
            .setId("${VideoPlayerConfig.MEDIA_SESSION_ID_PREFIX}_${System.currentTimeMillis()}")
            .setCallback(MediaSessionCallback())
            .build()
        
        // Set up player listener to request audio focus when playback starts
        audioFocusPlayerListener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    // Request audio focus when playback starts
                    requestAudioFocus()
                } else {
                    // Abandon audio focus when playback stops
                    abandonAudioFocus()
                }
            }
        }
        playerController!!.player.addListener(audioFocusPlayerListener!!)
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession ?: throw IllegalStateException("MediaSession not initialized")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Remove player listener to prevent memory leak
        audioFocusPlayerListener?.let { listener ->
            playerController?.player?.removeListener(listener)
        }
        audioFocusPlayerListener = null
        
        // Abandon audio focus
        abandonAudioFocus()
        
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        playerController?.release()
        playerController = null
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
            }
            // ACTION_START is no longer needed here since onCreate() handles startForeground()
            // This ensures startForeground() is always called within 5 seconds
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }
    
    /**
     * Gets the player controller instance.
     * Used by Fragment to access player via MediaController.
     */
    fun getPlayerController(): PlayerController? = playerController
    
    /**
     * Gets the MediaSession instance.
     */
    fun getMediaSession(): MediaSession? = mediaSession
    
    /**
     * Creates the notification channel for Android O+.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.app_name), // Use app name as channel name
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Video playback controls"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Creates the foreground service notification.
     * Note: MediaSessionService handles notification updates automatically via MediaSession.
     * This method is kept for potential custom notification needs.
     */
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Playing video")
            .setSmallIcon(android.R.drawable.ic_media_play) // Use system media icon
            .setOngoing(true)
            .build()
    }
    
    /**
     * MediaSession callback for configuring available commands per controller type.
     */
    private inner class MediaSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            // Configure commands for media notification controller (system UI)
            if (session.isMediaNotificationController(controller)) {
                // Use default commands for media notification
                // Can be customized later with specific commands and button preferences
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
            }
            
            // Default commands for all other controllers (app UI, etc.)
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
        }
        
        override fun onAddMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val future = SettableFuture.create<MutableList<MediaItem>>()
            future.set(mediaItems)
            return future
        }
    }
    
    /**
     * Requests audio focus for playback.
     */
    private fun requestAudioFocus() {
        // Don't request if already requested
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest != null) {
                return
            }
        } else {
            if (hasAudioFocus) {
                return
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AUDIOFOCUS_GAIN).apply {
                setOnAudioFocusChangeListener { focusChange ->
                    playerController?.player?.let { player ->
                        when (focusChange) {
                            AUDIOFOCUS_LOSS -> {
                                // Permanent loss of audio focus - pause playback
                                if (player.isPlaying) {
                                    isPlayingBeforeFocusLoss = true
                                    player.pause()
                                }
                            }

                            AUDIOFOCUS_LOSS_TRANSIENT -> {
                                // Temporary loss of audio focus - pause playback
                                if (player.isPlaying) {
                                    isPlayingBeforeFocusLoss = true
                                    player.pause()
                                }
                            }

                            AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                                // Can continue playing at lower volume, but we'll pause for better UX
                                if (player.isPlaying) {
                                    isPlayingBeforeFocusLoss = true
                                    player.pause()
                                }
                            }

                            AUDIOFOCUS_GAIN -> {
                                // Regained audio focus - resume playback if it was playing before
                                if (isPlayingBeforeFocusLoss) {
                                    isPlayingBeforeFocusLoss = false
                                    player.play()
                                }
                            }
                        }
                    }
                }
            }.build()
            val result = audioManager.requestAudioFocus(audioFocusRequest!!)
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // Audio focus granted
            }
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                { focusChange ->
                    playerController?.player?.let { player ->
                        when (focusChange) {
                            AUDIOFOCUS_LOSS -> {
                                if (player.isPlaying) {
                                    isPlayingBeforeFocusLoss = true
                                    player.pause()
                                }
                                hasAudioFocus = false
                            }

                            AUDIOFOCUS_LOSS_TRANSIENT -> {
                                if (player.isPlaying) {
                                    isPlayingBeforeFocusLoss = true
                                    player.pause()
                                }
                            }

                            AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                                if (player.isPlaying) {
                                    isPlayingBeforeFocusLoss = true
                                    player.pause()
                                }
                            }

                            AUDIOFOCUS_GAIN -> {
                                if (isPlayingBeforeFocusLoss) {
                                    isPlayingBeforeFocusLoss = false
                                    player.play()
                                }
                            }
                        }
                    }
                },
                AudioManager.STREAM_MUSIC,
                AUDIOFOCUS_GAIN
            )
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                hasAudioFocus = true
            }
        }
    }
    
    /**
     * Abandons audio focus.
     */
    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
                audioFocusRequest = null
            }
        } else {
            if (hasAudioFocus) {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
                hasAudioFocus = false
            }
        }
        // Reset playback state flag
        isPlayingBeforeFocusLoss = false
    }
}

