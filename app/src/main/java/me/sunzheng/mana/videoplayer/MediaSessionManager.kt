package me.sunzheng.mana.videoplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
     * MediaController for controlling playback via service.
     */
    private var mediaController: MediaController? = null
    
    /**
     * Future for MediaController connection.
     */
    private var controllerFuture: ListenableFuture<MediaController>? = null
    
    /**
     * Connects to the VideoPlaybackService and creates a MediaController.
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
        val intent = Intent(context, VideoPlaybackService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        
        // Create session token
        val sessionToken = SessionToken(
            context,
            ComponentName(context, VideoPlaybackService::class.java)
        )
        
        // Create MediaController
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        
        controllerFuture?.addListener(
            {
                try {
                    val controller = controllerFuture?.get()
                    if (controller != null) {
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
    }
    
    /**
     * Disconnects from the service and releases MediaController.
     */
    fun disconnect() {
        controllerFuture?.let { future ->
            MediaController.releaseFuture(future as java.util.concurrent.Future<out MediaController>)
        }
        controllerFuture = null
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

