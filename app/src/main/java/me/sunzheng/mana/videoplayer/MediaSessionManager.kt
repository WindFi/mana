package me.sunzheng.mana.videoplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import me.sunzheng.mana.videoplayer.VideoPlaybackService

/**
 * Manages MediaController connection to VideoPlaybackService.
 * Handles service connection lifecycle and provides access to MediaController.
 */
class MediaSessionManager(private val context: Context) {
    
    /**
     * MediaController reference from service.
     * MediaController is managed by VideoPlaybackService, not by this manager.
     */
    private var mediaController: MediaController? = null
    
    /**
     * Connects to the VideoPlaybackService and gets MediaController using Intent actions.
     * Uses ACTION_GET_SESSION_TOKEN to get SessionToken, then creates MediaController.
     * 
     * @param onConnected Callback when connection is established
     * @param onError Callback when connection fails
     */
    fun connect(
        onConnected: (MediaController) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        // Start the service if not already running
        // MediaSessionService will handle foreground service lifecycle automatically
        val startIntent = Intent(context, VideoPlaybackService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(startIntent)
        } else {
            context.startService(startIntent)
        }
        
        // Create ResultReceiver to receive SessionToken from Service
        val resultReceiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                if (resultCode == VideoPlaybackService.RESULT_SESSION_TOKEN_SUCCESS) {
                    // MediaSession is ready, create SessionToken using ComponentName
                    val sessionToken = SessionToken(
                        context,
                        ComponentName(context, VideoPlaybackService::class.java)
                    )
                    
                    // Create MediaController using SessionToken
                    val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
                    controllerFuture.addListener(
                        {
                            try {
                                val controller = controllerFuture.get()
                                if (controller != null) {
                                    // Store reference (but don't manage lifecycle - Service does that)
                                    mediaController = controller
                                    onConnected(controller)
                                } else {
                                    onError(IllegalStateException("MediaController is null"))
                                }
                            } catch (e: Exception) {
                                onError(e)
                            }
                        },
                        MoreExecutors.directExecutor()
                    )
                } else {
                    // Failed to get SessionToken, retry after a delay
                    val errorMsg = resultData?.getString("error") ?: "Unknown error"
                    Handler(Looper.getMainLooper()).postDelayed({
                        connect(onConnected, onError)
                    }, 100)
                }
            }
        }
        
        // Send Intent with ACTION_GET_SESSION_TOKEN to Service
        val intent = Intent(context, VideoPlaybackService::class.java).apply {
            action = VideoPlaybackService.ACTION_GET_SESSION_TOKEN
            putExtra(VideoPlaybackService.EXTRA_RESULT_RECEIVER, resultReceiver)
        }
        context.startService(intent)
    }
    
    /**
     * Disconnects from the service.
     * Note: MediaController lifecycle is managed by VideoPlaybackService,
     * so we only clear our reference here.
     */
    fun disconnect() {
        // Clear reference only - Service manages MediaController lifecycle
        mediaController = null
    }
    
    /**
     * Gets the current MediaController instance.
     * Returns null if not connected.
     */
    fun getController(): MediaController? = mediaController
    
    /**
     * Gets the player from MediaController.
     * MediaController implements Player interface directly.
     * Returns null if not connected.
     */
    fun getPlayer(): Player? = mediaController
    
    /**
     * Checks if currently connected to the service.
     */
    fun isConnected(): Boolean = mediaController != null
}

