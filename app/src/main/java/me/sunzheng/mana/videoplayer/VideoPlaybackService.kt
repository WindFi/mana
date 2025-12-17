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
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import android.os.ResultReceiver
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
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
        
        /**
         * Action to release resources (MediaController, MediaSession, PlayerController).
         * Called when VideoPlayerActivity is destroyed to clean up resources.
         */
        const val ACTION_RELEASE = "me.sunzheng.mana.videoplayer.RELEASE"
        
        /**
         * Action to get SessionToken from service.
         * Client sends Intent with this action and ResultReceiver in extras.
         * Service responds with SessionToken via ResultReceiver.
         */
        const val ACTION_GET_SESSION_TOKEN = "me.sunzheng.mana.videoplayer.GET_SESSION_TOKEN"
        
        /**
         * Action broadcast when MediaController is ready.
         * Service broadcasts this when MediaController is created.
         */
        const val ACTION_MEDIA_CONTROLLER_READY = "me.sunzheng.mana.videoplayer.MEDIA_CONTROLLER_READY"
        
        /**
         * Extra key for ResultReceiver in Intent.
         */
        const val EXTRA_RESULT_RECEIVER = "me.sunzheng.mana.videoplayer.EXTRA_RESULT_RECEIVER"
        
        /**
         * Result code for successful MediaSession ready notification.
         */
        const val RESULT_SESSION_TOKEN_SUCCESS = 1
        
        /**
         * Result code for failed MediaSession ready notification.
         */
        const val RESULT_SESSION_TOKEN_FAILED = 0
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
    
    /**
     * MediaController instance managed by this service.
     * Created when service starts and released when service is destroyed.
     */
    private var mediaController: MediaController? = null
    
    /**
     * Future for MediaController creation.
     */
    private var controllerFuture: ListenableFuture<MediaController>? = null
    
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
        
        // Create MediaController for this service
        createMediaController()
    }
    
    /**
     * Creates MediaController for this service.
     * MediaController is managed by the service and can be accessed by clients.
     */
    private fun createMediaController() {
        val sessionToken = SessionToken(
            this,
            android.content.ComponentName(this, VideoPlaybackService::class.java)
        )
        
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        
        controllerFuture?.addListener(
            {
                try {
                    val controller = controllerFuture?.get()
                    if (controller != null) {
                        mediaController = controller
                        Log.d("VideoPlaybackService", "MediaController created successfully")
                        // Broadcast that MediaController is ready
                        sendMediaControllerReadyBroadcast()
                    } else {
                        Log.e("VideoPlaybackService", "MediaController is null")
                    }
                } catch (e: Exception) {
                    Log.e("VideoPlaybackService", "Failed to create MediaController", e)
                }
            },
            MoreExecutors.directExecutor()
        )
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
        
        // Release MediaController
        controllerFuture?.let { future ->
            MediaController.releaseFuture(future as java.util.concurrent.Future<out MediaController>)
        }
        controllerFuture = null
        mediaController = null
        
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        playerController?.release()
        playerController = null
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
            }
            ACTION_GET_SESSION_TOKEN -> {
                handleGetSessionToken(intent)
            }
            ACTION_RELEASE -> {
                handleRelease()
            }
            // ACTION_START is no longer needed here since onCreate() handles startForeground()
            // This ensures startForeground() is always called within 5 seconds
        }
        return START_STICKY
    }
    
    /**
     * Handles ACTION_GET_SESSION_TOKEN request.
     * Extracts ResultReceiver from Intent and sends MediaSession ready status back.
     * Client should create SessionToken using ComponentName after receiving success.
     */
    private fun handleGetSessionToken(intent: Intent) {
        val resultReceiver = intent.getParcelableExtra<ResultReceiver>(EXTRA_RESULT_RECEIVER)
        if (resultReceiver == null) {
            Log.e("VideoPlaybackService", "ACTION_GET_SESSION_TOKEN: ResultReceiver is null")
            return
        }
        
        val session = mediaSession
        if (session == null) {
            Log.e("VideoPlaybackService", "ACTION_GET_SESSION_TOKEN: MediaSession is not initialized")
            val bundle = Bundle().apply {
                putString("error", "MediaSession is not initialized")
            }
            resultReceiver.send(RESULT_SESSION_TOKEN_FAILED, bundle)
            return
        }
        
        // MediaSession is ready, client can create SessionToken using ComponentName
        // Return success status
        val bundle = Bundle().apply {
            putBoolean("ready", true)
        }
        resultReceiver.send(RESULT_SESSION_TOKEN_SUCCESS, bundle)
        Log.d("VideoPlaybackService", "ACTION_GET_SESSION_TOKEN: MediaSession is ready")
    }
    
    /**
     * Handles ACTION_RELEASE request.
     * Releases MediaController, MediaSession, and PlayerController resources.
     * Called when VideoPlayerActivity is destroyed.
     */
    private fun handleRelease() {
        Log.d("VideoPlaybackService", "ACTION_RELEASE: Releasing resources")
        
        // Remove player listener to prevent memory leak
        audioFocusPlayerListener?.let { listener ->
            playerController?.player?.removeListener(listener)
        }
        audioFocusPlayerListener = null
        
        // Abandon audio focus
        abandonAudioFocus()
        
        // Release MediaController
        controllerFuture?.let { future ->
            MediaController.releaseFuture(future as java.util.concurrent.Future<out MediaController>)
        }
        controllerFuture = null
        mediaController = null
        
        // Release MediaSession
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        
        // Release PlayerController
        playerController?.release()
        playerController = null
        
        Log.d("VideoPlaybackService", "ACTION_RELEASE: Resources released successfully")
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
     * Gets the MediaController instance managed by this service.
     * Returns null if MediaController is not yet created or service is destroyed.
     * 
     * Note: MediaController creation is asynchronous. Use getMediaControllerAsync() 
     * to wait for MediaController to be ready.
     */
    fun getMediaController(): MediaController? = mediaController
    
    /**
     * Gets the MediaController asynchronously.
     * Returns a ListenableFuture that completes when MediaController is ready.
     * 
     * @return ListenableFuture<MediaController> that completes when controller is ready
     * 
     * @deprecated Use ACTION_GET_SESSION_TOKEN Intent action instead.
     * This method is kept for backward compatibility but should not be used.
     */
    @Deprecated("Use ACTION_GET_SESSION_TOKEN Intent action instead")
    fun getMediaControllerAsync(): ListenableFuture<MediaController>? = controllerFuture
    
    /**
     * Sends broadcast when MediaController is ready.
     * Clients can optionally register BroadcastReceiver to be notified.
     */
    private fun sendMediaControllerReadyBroadcast() {
        val intent = Intent(ACTION_MEDIA_CONTROLLER_READY).apply {
            setPackage(packageName)
        }
        sendBroadcast(intent)
        Log.d("VideoPlaybackService", "Broadcast sent: ACTION_MEDIA_CONTROLLER_READY")
    }
    
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

