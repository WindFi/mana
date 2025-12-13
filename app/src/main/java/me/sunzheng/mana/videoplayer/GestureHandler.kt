package me.sunzheng.mana.videoplayer

import android.app.Activity
import android.graphics.Point
import android.util.DisplayMetrics
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.WindowManager
import androidx.core.view.GestureDetectorCompat
import me.sunzheng.mana.videoplayer.VideoPlayerConfig

/**
 * Callback interface for gesture actions.
 * Implemented by Fragment to handle UI updates and player control.
 */
interface GestureActionListener {
    /**
     * Called when user performs a single tap.
     */
    fun onSingleTap()
    
    /**
     * Called when user performs a double tap (toggle play/pause).
     */
    fun onDoubleTap()
    
    /**
     * Called when brightness should be adjusted.
     * @param deltaValue The change in brightness (positive = brighter, negative = darker)
     */
    fun onBrightnessChange(deltaValue: Float)
    
    /**
     * Called when volume should be adjusted.
     * @param deltaValue The change in volume (positive = louder, negative = quieter)
     */
    fun onVolumeChange(deltaValue: Float)
    
    /**
     * Called when seek position should be changed.
     * @param deltaValue The change in position (positive = forward, negative = backward)
     */
    fun onSeekChange(deltaValue: Float)
}

/**
 * Handles gesture detection and processing for video player.
 * Detects brightness, volume, and seek gestures, and notifies listener.
 * 
 * Note: GestureHandler doesn't need direct access to player,
 * it only notifies the listener which handles player control.
 */
class GestureHandler(
    private val activity: Activity,
    private val listener: GestureActionListener
) {
    
    companion object {
        /**
         * Edge margin in density-independent pixels (dp).
         * Used to avoid gesture detection at screen edges.
         */
        private const val EDGE_MARGIN_DP = 21f
        
        /**
         * Top margin in density-independent pixels (dp).
         * Used to avoid gesture detection in status bar area.
         */
        private const val TOP_MARGIN_DP = 101f
        
        /**
         * Converts density-independent pixels (dp) to pixels (px).
         * @param dp The value in dp
         * @param density The display density from DisplayMetrics
         * @return The value in pixels
         */
        private fun dpToPx(dp: Float, density: Float): Int {
            return (dp * density + 0.5f).toInt()
        }
    }
    
    /**
     * Edge margin in pixels, calculated from dp based on screen density.
     */
    private val EDGE_MARGIN_PX: Int by lazy {
        val displayMetrics: DisplayMetrics = activity.resources.displayMetrics
        dpToPx(EDGE_MARGIN_DP, displayMetrics.density)
    }
    
    /**
     * Top margin in pixels, calculated from dp based on screen density.
     */
    private val TOP_MARGIN_PX: Int by lazy {
        val displayMetrics: DisplayMetrics = activity.resources.displayMetrics
        dpToPx(TOP_MARGIN_DP, displayMetrics.density)
    }
    
    /**
     * Gesture detector for recognizing gestures.
     */
    val gestureDetector: GestureDetectorCompat by lazy {
        GestureDetectorCompat(activity, VideoGestureListener())
    }
    
    /**
     * Window manager for getting screen dimensions.
     */
    private val windowManager: WindowManager by lazy {
        activity.getSystemService(Activity.WINDOW_SERVICE) as WindowManager
    }
    
    /**
     * Internal gesture listener implementation.
     */
    private inner class VideoGestureListener : GestureDetector.SimpleOnGestureListener() {
        
        private val measureLength = VideoPlayerConfig.GESTURE_MEASURE_LENGTH
        private var isScrolling = false
        private var isValid = false
        private var sourceX = 0.0f
        private var sourceY = 0.0f
        private var isLeft = false
        private var isVertical = false
        
        override fun onDown(e: MotionEvent): Boolean {
            isScrolling = false
            Log.d("GestureHandler", "onDown")
            return true
        }
        
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            Log.d("GestureHandler", "onSingleTapConfirmed")
            listener.onSingleTap()
            return true
        }
        
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (e1 == null || e2 == null) {
                return false
            }
            
            if (!isScrolling) {
                // Determine scroll direction
                isVertical = Math.abs(distanceX) < Math.abs(distanceY)
                isScrolling = true
                sourceX = e1.x
                sourceY = e1.y
                
                // Validate gesture area (avoid edges and status bar)
                val screenSize = Point()
                windowManager.defaultDisplay.getSize(screenSize)
                isValid = e1.x > EDGE_MARGIN_PX && e1.x < screenSize.x - EDGE_MARGIN_PX && e1.y > TOP_MARGIN_PX
                isLeft = isVertical && e1.x < screenSize.x / 2
                
                return true
            } else {
                if (!isValid) return true
                
                if (!isVertical) {
                    // Horizontal scroll = seek
                    val unit = ((e2.x - sourceX) / measureLength).toInt().toFloat()
                    if (Math.abs(unit) > 0) {
                        sourceX = e2.x
                        listener.onSeekChange(unit)
                    }
                } else {
                    // Vertical scroll = brightness (left) or volume (right)
                    val unit = ((sourceY - e2.y) / measureLength).toInt().toFloat()
                    if (Math.abs(unit) > 0) {
                        sourceY = e2.y
                        if (isLeft) {
                            listener.onBrightnessChange(unit)
                        } else {
                            listener.onVolumeChange(unit)
                        }
                    }
                }
            }
            return true
        }
        
        override fun onDoubleTap(e: MotionEvent): Boolean {
            Log.d("GestureHandler", "onDoubleTap")
            listener.onDoubleTap()
            return super.onDoubleTap(e)
        }
    }
}

