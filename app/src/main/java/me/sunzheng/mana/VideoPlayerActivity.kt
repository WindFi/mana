package me.sunzheng.mana

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Point
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.AUDIOFOCUS_GAIN
import android.media.AudioManager.AUDIOFOCUS_LOSS
import android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
import android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
import android.media.AudioManager.STREAM_MUSIC
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.support.v4.media.MediaDescriptionCompat
import android.text.TextUtils
import android.util.Log
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import dagger.hilt.android.AndroidEntryPoint
import me.sunzheng.mana.core.net.Status
import me.sunzheng.mana.core.net.v2.database.EpisodeEntity
import me.sunzheng.mana.core.net.v2.database.VideoFileEntity
import me.sunzheng.mana.core.net.v2.database.WatchProgressEntity
import me.sunzheng.mana.core.net.v2.parseExtractorMediaSource
import me.sunzheng.mana.core.net.v2.showToast
import me.sunzheng.mana.core.net.v2.toUUID
import me.sunzheng.mana.databinding.ActivityVideoPlayerBinding
import me.sunzheng.mana.utils.PreferenceManager.Global
import me.sunzheng.mana.videoplayer.MediaDescriptionAdapter2
import me.sunzheng.mana.videoplayer.VideoPlayerVideoModel
import java.io.File
import java.util.Formatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

// TODO: 缓存
@AndroidEntryPoint
class VideoPlayerActivity @Inject constructor() : AppCompatActivity(), VideoControllerListener {
    companion object {
        @JvmStatic
        val KEY_ITEMS_PARCEL = "${VideoPlayerActivity::class.simpleName}_items"

        @JvmStatic
        val KEY_POSITION_INT = "${VideoPlayerActivity::class.simpleName}_position"

        @JvmStatic
        val KEY_BANGUMI_ID_STR = "${VideoPlayerActivity::class.simpleName}_bangumiId"

        @JvmStatic
        val AUTOSAVE_INTERVAL_MILLION = 1000 * 60L
        fun newInstance(
            context: Context,
            bangumiId: UUID,
            position: Int,
            itemList: List<MediaDescriptionCompat>
        ) =
            Bundle()
                .apply {
                    putInt(KEY_POSITION_INT, position)
                    putParcelableArrayList(KEY_ITEMS_PARCEL, ArrayList(itemList))
                    putString(KEY_BANGUMI_ID_STR, bangumiId.toString())
                }.let {
                    Intent(context, VideoPlayerActivity::class.java).apply {
                        putExtras(it)
                    }
                }
    }

    val binding: ActivityVideoPlayerBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_video_player)
    }
    private val player: ExoPlayer by lazy {
        ExoPlayerFactory.newSimpleInstance(
            this,
            DefaultTrackSelector(AdaptiveTrackSelection.Factory())
        )
    }
    private val dataSourceFactory: DataSource.Factory by lazy {
//        var cache = SimpleCache(cacheFile, LeastRecentlyUsedCacheEvictor(1024 * 1024 * 256))
//        CacheDataSourceFactory(
//            cache,
//            DefaultHttpDataSourceFactory(Util.getUserAgent(this, packageName)),
//            CacheDataSource.FLAG_BLOCK_ON_CACHE
//        )
        DefaultDataSourceFactory(this, Util.getUserAgent(this, packageName))
    }
    private val cacheFile: File by lazy {
        File(externalCacheDir, "mediaCache")
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
        AudioFocusRequest.Builder(AUDIOFOCUS_GAIN).apply {
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
    val genstureDetector: GestureDetectorCompat by lazy {
        GestureDetectorCompat(this, NormalGenstureDetector(this, this))
    }
    val logWatchProgressRunnable = Runnable {
        var episodeEntity =
            viewModel.mediaDescritionLiveData.value?.extras!!.getParcelable<EpisodeEntity>("raw")
        var m = viewModel.watchProgressLiveData?.value
        episodeEntity?.run {
            Log.i(this@VideoPlayerActivity.javaClass.simpleName, "log watchprogress:${bangumiId}")
            viewModel.updateWatchProgress(
                bangumiId = viewModel.bangumiId,
                episodeEntity = episodeEntity,
                lastWatchPosition = (binding.player.player.currentPosition / 1000).toFloat(),
                duration = (binding.player.player.duration / 1000).toFloat(),
                watchprocessEntity = m
            ).observe(this@VideoPlayerActivity) {
            }
        }
    }
    val mHandler: Handler by lazy {
        Handler(Looper.getMainLooper()) {
            when (it.what) {
                1 -> {
                    logWatchProgressRunnable.run()
                    mHandler.sendEmptyMessageDelayed(1, AUTOSAVE_INTERVAL_MILLION)
                    true
                }

                else -> false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.title = ""

        binding.player.player = player
        binding.player.keepScreenOn = true
        setup()
        loadConfig()
        binding.sourceList.adapter = ArrayAdapter<VideoFileEntity>(
            this@VideoPlayerActivity,
            R.layout.item_source_listview,
            R.id.title
        )
        viewModel.videoFileLiveData.observe(this) {
            it.parseExtractorMediaSource(viewModel.host, dataSourceFactory).run {
                player.prepare(this)
            }
        }
        var bundle = savedInstanceState?.getBundle("args") ?: intent.extras!!
//   TODO:=================这里需要优化 前面的bangumiId 没有从episodeEntity 里面传过来=================================
        viewModel.bangumiId = bundle.getString(KEY_BANGUMI_ID_STR, "")
//===============================================================================================================
        viewModel.mediaDescritionLiveData.observe(this) { mediaDescriptionCompat ->
            (binding.sourceList.adapter as ArrayAdapter<VideoFileEntity>).clear()
            viewModel.fetchVideoFiles(mediaDescriptionCompat.mediaId!!.toUUID())
                .observe(this@VideoPlayerActivity) {
                    when (it.code) {
                        Status.SUCCESS -> {
                            it.data?.firstOrNull()?.run {
                                viewModel.videoFileLiveData.postValue(this)
                            }
                            mediaDescriptionCompat.mediaId?.toUUID()?.run {
                                viewModel.fetchWatchProgress(this)?.run {
                                    showWatchprogress(this)
                                }
                            }
                            (binding.sourceList.adapter as ArrayAdapter<VideoFileEntity>).addAll(
                                it.data!!
                            )
                            binding.sourceList.setOnItemClickListener { parent, view, position, id ->
                                it.data.get(position).run {
                                    viewModel.videoFileLiveData.postValue(this)
                                }
                            }
                            mHandler.removeCallbacksAndMessages(logWatchProgressRunnable)
                            mHandler.sendEmptyMessageDelayed(1, AUTOSAVE_INTERVAL_MILLION)
                        }

                        Status.ERROR -> it?.message?.run {
                            showToast(this)
                        }

                        Status.LOADING -> {
                            if (it.data?.size ?: 0 > 0) {
                                it.data?.takeIf { list -> list.isNotEmpty() }
                                    ?.firstOrNull()
                                    ?.parseExtractorMediaSource(
                                        viewModel.host,
                                        dataSourceFactory
                                    )?.run {
                                        player.prepare(this)
                                        supportActionBar?.title = mediaDescriptionCompat.title
                                        binding.player.hideController()
                                    }
                            }
                        }
                    }
                }
        }
        bundle.getParcelableArrayList<MediaDescriptionCompat>(KEY_ITEMS_PARCEL)
            ?.run {
                binding.listviewEpisode.adapter =
                    MediaDescriptionAdapter2(
                        this@VideoPlayerActivity,
                        this
                    )
                viewModel.position.observe(this@VideoPlayerActivity) { position ->
                    var model = this[position]
                    var label =
                        if (viewModel.isJaFirst || TextUtils.isEmpty(model.title)) model.subtitle else model.title
                    supportActionBar?.title = label
                    viewModel.mediaDescritionLiveData.postValue(model)
                }

            }

        binding.listviewEpisode.onItemClickListener =
            OnItemClickListener { parent, view, position, id ->
                parent?.run {
                    viewModel.position.postValue(position)
                    hideViewWithAnimation(this)
                }
            }
        binding.player.setControllerVisibilityListener {
            when (it) {
                View.VISIBLE -> supportActionBar?.show()
                else -> supportActionBar?.hide()
            }
        }
        binding.player.controllerAutoShow = true
        binding.player.controllerShowTimeoutMs = 3000
//==================================== init?====================================
        var position = bundle.getInt(KEY_POSITION_INT, 0)
        var offset = binding.listviewEpisode.count - position - 1
        viewModel.position.postValue(position)
        binding.listviewEpisode.performItemClick(
            binding.listviewEpisode.adapter.getView(
                offset,
                null,
                null
            ), offset, binding.listviewEpisode.adapter.getItemId(offset)
        )

        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            WindowInsetsControllerCompat(
                window,
                binding.player
            ).hide(
                WindowInsets.Type.statusBars()
                    .or(WindowInsets.Type.navigationBars())
            )
            insets
        }
//=======================================================================
    }

    override fun onStart() {
        binding.player.player.playWhenReady = true
        super.onStart()
    }

    override fun onStop() {
        binding.player.player.playWhenReady = false
        super.onStop()
    }

    fun showWatchprogress(entity: WatchProgressEntity) {
        var convert = entity.lastWatchPosition * 1000
        binding.viewgroupWatchprogress.isVisible = true
        binding.textviewWatchprogress.text = String.format(
            getString(R.string.text_seekto),
            Util.getStringForTime(
                StringBuilder(),
                Formatter(StringBuilder(), Locale.getDefault()),
                convert.toLong()
            )
        )
        binding.imageviewClose.setOnClickListener {
            binding.viewgroupWatchprogress.isVisible = false
        }
        binding.textviewWatchprogress.setOnClickListener { v ->
            binding.viewgroupWatchprogress.isVisible = false
            binding.player.player.seekTo(convert.toLong())
        }
        binding.viewgroupWatchprogress.postDelayed({
            binding.viewgroupWatchprogress.isVisible = false
        }, 3000)
        viewModel.watchProgressLiveData!!.postValue(entity)
    }

    fun loadConfig() {
        var s = PreferenceManager.getDefaultSharedPreferences(this)
        viewModel.isJaFirst = s.getBoolean(getString(Global.RES_JA_FIRST_BOOL), false)
    }

    override fun onDestroy() {
        unregisterReceiver(broadcastReceiver)
        audioManager.abandonAudioFocusRequest(audioRequest.build())
        super.onDestroy()
        player.release()
        mHandler.removeCallbacksAndMessages(logWatchProgressRunnable)
    }

    fun showControllerUI() {
        binding.player.showController()
    }

    fun hideControllerUI() {
        binding.player.hideController()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.video_menu, menu)
        val sourceMenu = menu!!.findItem(R.id.action_source)
        sourceMenu.isVisible = false
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            super.onBackPressed()
            true
        }

        R.id.action_list -> {
            showEpisodeListView()
            true
        }

        R.id.action_source -> {
            showSourceListView()
            true
        }

        R.id.action_feedback -> {
            var mediaId = viewModel.mediaDescritionLiveData.value!!.mediaId
            var videoFileId = viewModel.videoFileLiveData.value!!.id.toString()
            FeedbackActivity.newInstance(
                this@VideoPlayerActivity,
                episodeId = mediaId!!,
                videoFileId = videoFileId
            ).run {
                startActivity(this)
            }
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    fun showSourceListView() {
        hideControllerUI()
        showViewAnimation(binding.sourceListRoot)
    }

    fun showEpisodeListView() {
        hideControllerUI()
        showViewAnimation(binding.listviewEpisode)
    }

    fun showViewAnimation(view: View) {
        if (view == null || view.visibility == View.VISIBLE) {
            return
        }
        view.visibility = View.VISIBLE
        val animation = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        view.animation = animation
        if (view is ListView) {
            val listView = view
            listView.post { listView.smoothScrollToPosition(listView.checkedItemPosition) }
        }
    }

    override fun onBackPressed() {
        if (!isListViewShowing()) {
            super.onBackPressed()
        }
        binding.listviewEpisode.isVisible = false
        binding.sourceListRoot.isVisible = false
    }

    private fun setup() {
        volumeControlStream = STREAM_MUSIC
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
        binding.player.player.addListener(object : Player.EventListener {
            override fun onLoadingChanged(isLoading: Boolean) {
                binding.progressbar.isVisible = isLoading
            }

            override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
                super.onTimelineChanged(timeline, manifest, reason)
            }

            override fun onPlayerError(error: ExoPlaybackException?) {
                error?.run {
                    Log.i(
                        "${VideoPlayerActivity::class.java.simpleName}",
                        "${this.sourceException.message}"
                    )
                    showToast(this.sourceException.message ?: this.sourceException.toString())
                }

                super.onPlayerError(error)
            }

        })
        binding.player.player.playWhenReady = true
        binding.player.setOnTouchListener { v, event -> genstureDetector.onTouchEvent(event) }
        viewModel.brighnessLiveData.observe(this) {
            val per: Float = it / 17 * 255.0f
            var currentBrightness = window.attributes.screenBrightness * 255f
            if (currentBrightness < 0) {
                currentBrightness =
                    Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 0)
                        .toFloat()
            }
            currentBrightness += per
            currentBrightness = currentBrightness
                .coerceAtMost(255f)
                .coerceAtLeast(0f)

            val layoutpars = window.attributes
            layoutpars.screenBrightness = currentBrightness / 255.0f
            window.attributes = layoutpars
            showBrightnessVal((currentBrightness / 2.55).toInt())
        }
        viewModel.soundLiveData.observe(this) {
            var currentVol = audioManager.getStreamVolume(STREAM_MUSIC)
            val maxVol = audioManager.getStreamMaxVolume(STREAM_MUSIC)
            currentVol += it.toInt()
            currentVol = currentVol.coerceAtMost(maxVol).coerceAtLeast(0)
            audioManager.setStreamVolume(STREAM_MUSIC, currentVol, 0)
            showVolumeVal(currentVol * 4)
        }
        viewModel.seekPositionLiveData.observe(this) {
            showControllerUI()
            var d = player.currentPosition + it * 5000
            binding.viewgroupProgress.isVisible = true
            binding.textviewExoPosition.text =
                "${
                    Util.getStringForTime(
                        StringBuilder(),
                        Formatter(StringBuilder(), Locale.getDefault()),
                        player.currentPosition
                    )
                }/${
                    Util.getStringForTime(
                        StringBuilder(),
                        Formatter(StringBuilder(), Locale.getDefault()),
                        player.duration
                    )
                }"
            player.seekTo(d)
            binding.viewgroupProgress.postDelayed({
                binding.viewgroupProgress.isVisible = false
            }, 3000)
        }
    }

    fun showBrightnessVal(value: Int) {
        binding.imageviewValue.setImageResource(if (value < 30) R.drawable.brightness_low else if (value < 75) R.drawable.brightness_half else R.drawable.brightness_high)
        internalShowUI(value)
    }

    fun showVolumeVal(value: Int) {
        binding.imageviewValue.setImageResource(if (value < 30) R.drawable.volume_down else if (value < 75) R.drawable.volume_half else R.drawable.volume_up)
        internalShowUI(value)
    }

    fun internalShowUI(value: Int) {
        binding.viewgroupValue.isVisible = true
        binding.value = value
        binding.viewgroupValue.postDelayed({
            binding.viewgroupValue.isVisible = false
        }, 3000)
    }


    private fun onVideoResize(vWidth: Int, vHeight: Int) {
        val size = windowManager.currentWindowMetrics.bounds
        val scaleX = size.left.toFloat() / vWidth
        val scaleY = size.bottom.toFloat() / vHeight
        val scaleRate = scaleX / scaleY
        binding.player.resizeMode =
            if (scaleRate < 1) AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH else AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
    }

    fun hideViewWithAnimation(view: View) {
        binding.root.postDelayed({
            view.isVisible = false
            val animation = AnimationUtils.loadAnimation(this, R.anim.slide_out_right)
            view.animation = animation
        }, 50)
    }

    override fun singleClick() {
        if (binding.listviewEpisode.isVisible || binding.sourceListRoot.isVisible) {
            hideViewWithAnimation(binding.listviewEpisode)
            hideViewWithAnimation(binding.sourceListRoot)
            return
        }
        if (binding.player.isControllerVisible) {
            hideControllerUI()
        } else {
            showControllerUI()
        }
    }

    override fun playState() {
        viewModel.isPlaying.postValue(viewModel.isPlaying.value?.not())
    }

    override fun brighness(deltaValue: Float) {
        viewModel.brighnessLiveData.postValue(deltaValue)
    }

    override fun sound(deltaValue: Float) {
        viewModel.soundLiveData.postValue(deltaValue)
    }

    override fun seekTo(deltaValue: Float) {
        viewModel.seekPositionLiveData.postValue(deltaValue.toLong())
    }

    private fun startPlay() {
        player.playWhenReady = false
    }

    private fun stopPlay() {
        player.stop()
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
        display
    }
}

interface VideoControllerListener {
    fun singleClick()
    fun playState()
    fun brighness(deltaValue: Float)
    fun sound(deltaValue: Float)
    fun seekTo(deltaValue: Float)
}

class NormalGenstureDetector(
    val context: Activity,
    val controller: VideoControllerListener? = null
) : SimpleOnGestureListener() {
    val windowManager: WindowManager by lazy {
        context.windowManager
    }
    val MEASURE_LENGTH = 72.0f
    var isScrolling = false
    var isValid = false
    var sourceX = 0.0f
    var sourceY = 0.0f
    var isLeft = false
    var isVertical = false

    override fun onDown(e: MotionEvent): Boolean {
        isScrolling = false
        Log.i("${NormalGenstureDetector::class.simpleName}", "onDown")
        return true
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        Log.i("${NormalGenstureDetector::class.simpleName}", "onSingleTapConfirmed")
        controller?.singleClick()
        return true
    }

    override fun onScroll(
        e1: MotionEvent,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
//        var mu = areaDetector(e1,e2)
//        return isScrolling && isInVaildArea(e1)&&controller.scroll(mu,e1!!,if(distanceX>distanceY)distanceX else distanceY)
        var str = e1.let { "e1.x:${it.x} " }
        str += e1.let { "e1.y:${e1.y} " }
        str += e2.let { "e2.x:${e2.x} " }
        str += e2.let { "e2.y:${e2.y} " }
        str += "distanceX:$distanceX distanceY:$distanceY"
        Log.i("${NormalGenstureDetector::class.simpleName}", "onScroll:$str")
//        横屏的时候 坐标系也旋转过来了 所以要先进行xy判断 再判断distance
        if (e1 == null || e2 == null) {
            return false
        }
        var ev1 = e1
        var ev2 = e2
        if (!isScrolling) {
            isVertical = Math.abs(distanceX) < Math.abs(distanceY)
            isScrolling = true
            sourceX = e1.x
            sourceY = e1.y
            val p = Point()
            windowManager.defaultDisplay.getSize(p)
            isValid = e1.x > 21 && e1.x < p.x - 21 && e1.y > 101
            isLeft = isVertical && e1.x < p.x / 2
            return true
        } else {
            if (!isValid) return true
            if (!isVertical) {
                val unit: Float =
                    ((e2.x - sourceX) / MEASURE_LENGTH).toInt().toFloat()
                if (Math.abs(unit) > 0) {
                    sourceX = e2.x
                }
                controller?.seekTo(unit)
            } else {
                val unit: Float =
                    ((sourceY - e2.y) / MEASURE_LENGTH).toInt().toFloat()
                if (Math.abs(unit) > 0) {
                    sourceY = e2.y
                }
                if (isLeft) {
                    controller?.brighness(unit)
                } else {
                    controller?.sound(unit)
                }
            }
        }
        return true
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        Log.i("${NormalGenstureDetector::class.simpleName}", "onDoubleTap")
        controller?.playState()
        return super.onDoubleTap(e)
    }
}