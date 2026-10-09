package me.sunzheng.mana.videoplayer

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors

/**
 * Manages MediaController connection to VideoPlaybackService.
 * Handles service connection lifecycle and provides access to MediaController.
 */
class MediaSessionManager(private val context: Context) {

    /**
     * MediaController connected to VideoPlaybackService.
     * Building the controller binds the service.
     */
    private var mediaController: MediaController? = null

    /**
     * Connects to VideoPlaybackService by building a MediaController.
     * The controller binds the service. Media3 shows the playback notification and enters the foreground when playback is ongoing.
     *
     * @param onConnected Callback when connection is established
     * @param onError Callback when connection fails
     */
    fun connect(
        onConnected: (MediaController) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, VideoPlaybackService::class.java)
        )
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                try {
                    val controller = controllerFuture.get()
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
     * Disconnects from the service.
     * Clears the local controller reference. Playback resources stay in VideoPlaybackService until release.
     */
    fun disconnect() {
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
