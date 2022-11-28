package me.sunzheng.mana

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.*
import android.net.ConnectivityManager
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ListView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import dagger.hilt.android.AndroidEntryPoint
import me.sunzheng.mana.core.net.Status
import me.sunzheng.mana.core.net.v2.database.EpisodeEntity
import me.sunzheng.mana.core.net.v2.parseExtractorMediaSource
import me.sunzheng.mana.core.net.v2.showToast
import me.sunzheng.mana.databinding.ActivityVideoPlayerBinding
import me.sunzheng.mana.videoplayer.VideoController
import me.sunzheng.mana.videoplayer.VideoPlayerVideoModel
import javax.inject.Inject

@AndroidEntryPoint
class VideoPlayerActivity @Inject constructor() : AppCompatActivity(), VideoController {
    companion object {
        @JvmStatic
        val KEY_ITEMS_PARCEL = "${VideoPlayerActivity::class.simpleName}_items"

        @JvmStatic
        val KEY_POSITION_INT = "${VideoPlayerActivity::class.simpleName}_position"
        fun newInstance(context: Context, position: Int, itemList: List<EpisodeEntity>) = Bundle()
            .apply {
                putInt(KEY_POSITION_INT, position)
                putParcelableArrayList(KEY_ITEMS_PARCEL, ArrayList(itemList))
            }.let {
                Intent(context, VideoPlayerActivity::class.java).apply {
                    putExtras(it)
                }
            }
    }

    val binding: ActivityVideoPlayerBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_video_player)
    }
    val player: ExoPlayer by lazy {
        ExoPlayerFactory.newSimpleInstance(
            this,
            DefaultTrackSelector(AdaptiveTrackSelection.Factory())
        )
    }
    val dataSourceFactory: DataSource.Factory by lazy {
        DefaultDataSourceFactory(
            this,
            Util.getUserAgent(this, packageName),
            DefaultBandwidthMeter()
        )
    }
    val isAutoPlay: Boolean by lazy {
        PreferenceManager.getDefaultSharedPreferences(this).getBoolean("isAutoplay", false)
    }
    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.run {
                when (action) {
                    AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {

                    }
                    ConnectivityManager.CONNECTIVITY_ACTION -> {
                        // TODO: Deprecated
                    }
                }
            }
        }
    }
    val audioManager: AudioManager by lazy {
        getSystemService(AUDIO_SERVICE) as AudioManager
    }
    lateinit var dataSource: DataSource

    val audioRequest: AudioFocusRequest.Builder by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).apply {
            setOnAudioFocusChangeListener {
                when (it) {
                    AUDIOFOCUS_LOSS -> {

                    }
                    AUDIOFOCUS_LOSS_TRANSIENT -> {

                    }
                    AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {

                    }
                    AUDIOFOCUS_GAIN -> {

                    }
                }
            }
        }
    }
    val viewModel by viewModels<VideoPlayerVideoModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.title = ""

        binding.player.player = player
        binding.player.keepScreenOn = true
        setup()
        var bundle = savedInstanceState?.getBundle("args") ?: intent.extras

        bundle?.getParcelableArrayList<EpisodeEntity>(KEY_ITEMS_PARCEL)
            ?.run {
                viewModel.position.observe(this@VideoPlayerActivity) {
                    viewModel.fetchVideoFiles(this[it].id).observe(this@VideoPlayerActivity) {
                        when (it.code) {
                            Status.SUCCESS -> {
                                if (!player.playWhenReady) {
                                    it.data?.videoFile?.parseExtractorMediaSource(dataSourceFactory)
                                        ?.run {
                                            player.prepare(this)
                                        }
                                }
                            }
                            Status.ERROR -> it?.message?.run {
                                showToast(this)
                            }


                            Status.LOADING -> {
                                it.data?.videoFile?.parseExtractorMediaSource(dataSourceFactory)
                                    ?.run {
                                        player.prepare(this)
                                        binding.player.hideController()
                                    }
                            }
                        }
                    }
                }
            }

        var position = bundle?.getInt(KEY_POSITION_INT, 0)
        viewModel.position.postValue(position)

        WindowInsetsControllerCompat(
            window,
            binding.player
        ).hide(android.view.WindowInsets.Type.statusBars())
    }

    override fun onDestroy() {
        unregisterReceiver(broadcastReceiver)
        audioManager.abandonAudioFocusRequest(audioRequest.build())
        super.onDestroy()
        player.release()
    }

    override fun onBackPressed() {
        if (!isListViewShowing()) {
            super.onBackPressed()
        }
        binding.listviewEpisode.isVisible = false
        binding.sourceListRoot.isVisible = false
    }

    private fun setup() {
        volumeControlStream = AudioManager.STREAM_MUSIC
        audioManager.requestAudioFocus(audioRequest.build())
        IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY).run {
            registerReceiver(broadcastReceiver, this)
        }
        viewModel.isPlaying.observe(this) {
            when (it) {
                true -> {
                    startPlay()
                }
                false -> {
                    stopPlay()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            super.onBackPressed()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun onVideoResize(vWidth: Int, vHeight: Int) {
        val size = windowManager.currentWindowMetrics.bounds
        val scaleX = size.left.toFloat() / vWidth
        val scaleY = size.bottom.toFloat() / vHeight
        val scaleRate = scaleX / scaleY
        binding.player.resizeMode =
            if (scaleRate < 1) AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH else AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
    }

    override fun setVolume(detaVal: Float) {
        var currentVol = audioManager.getStreamVolume(STREAM_MUSIC)
        var maxVol = audioManager.getStreamMaxVolume(STREAM_MUSIC)
        currentVol += detaVal.toInt()
        currentVol = 0.coerceAtLeast(maxVol.coerceAtMost(currentVol))
        audioManager.setStreamVolume(STREAM_MUSIC, currentVol, 0)
        // TODO: showUI
    }

    override fun seekTo(value: Long) {
        player.seekTo((player.currentPosition + value * 5000).coerceAtLeast(0))
        // TODO: showUI
    }

    override fun playState() {
        viewModel.isPlaying.postValue(viewModel.isPlaying.value?.not())
    }

    private fun startPlay() {
        player.playWhenReady = false
    }

    private fun stopPlay() {
        player.stop()
    }

    override fun setBrightness(detaVal: Float) {
        var per = detaVal / 17 * 255.0f
        var current = window.attributes.screenBrightness * 255.0f
        if (current < 0) {
            current = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 0)
                .toFloat()
        }
        current += per
        current = current.coerceAtMost(255f).coerceAtLeast(0f)
        var layoutpars = window.attributes
        layoutpars.screenBrightness = current / 255.0f
        window.attributes = layoutpars
        // TODO: showUI 
    }

    private fun isListViewShowing() =
        binding.listviewEpisode.isVisible || binding.sourceListRoot.isVisible

    private fun viewWithAnimation(view: View) = view.run {
        animation = AnimationUtils.loadAnimation(
            context,
            if (isVisible) R.anim.slide_out_right else R.anim.slide_in_right
        )
        if (this is ListView && !isVisible) {
            post {
                this.smoothScrollToPosition(this.checkedItemPosition)
            }
        }
        isVisible = isVisible.not()
    }
}