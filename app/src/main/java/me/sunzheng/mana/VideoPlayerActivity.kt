package me.sunzheng.mana

import android.annotation.SuppressLint
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
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.Util
import dagger.hilt.android.AndroidEntryPoint
import me.sunzheng.mana.core.net.Status
import me.sunzheng.mana.core.net.v2.database.EpisodeEntity
import me.sunzheng.mana.core.net.v2.database.VideoFileEntity
import me.sunzheng.mana.core.net.v2.database.WatchProgressEntity
import me.sunzheng.mana.core.net.v2.parseExtractorMediaSource
import me.sunzheng.mana.core.net.v2.showToast
import me.sunzheng.mana.core.net.v2.toUUID
import me.sunzheng.mana.databinding.FragmentVideoPlayerBinding
import me.sunzheng.mana.utils.PreferenceManager.Global
import me.sunzheng.mana.videoplayer.MediaDescriptionAdapter2
import me.sunzheng.mana.videoplayer.VideoPlayerVideoModel
import java.io.File
import java.util.Formatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class VideoPlayerActivity @Inject constructor() : AppCompatActivity() {
    companion object {
        @JvmStatic
        val KEY_POSITION_INT = "${VideoPlayerActivity::class.simpleName}_position"

        @JvmStatic
        val KEY_BANGUMI_ID_STR = "${VideoPlayerActivity::class.simpleName}_bangumiId"

        @JvmStatic
        val KEY_EPISODE_ID_STR = "${VideoPlayerActivity::class.simpleName}_episodeId"

        fun newInstance(
            context: Context,
            bangumiId: UUID,
            episodeId: UUID,
            position: Int
        ) =
            Bundle()
                .apply {
                    putInt(KEY_POSITION_INT, position)
                    putString(KEY_EPISODE_ID_STR, episodeId.toString())
                    putString(KEY_BANGUMI_ID_STR, bangumiId.toString())
                }.let {
                    Intent(context, VideoPlayerActivity::class.java).apply {
                        putExtras(it)
                    }
                }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                VideoPlayerFragment().apply { arguments = intent.extras })
            .commit()
    }
}

@AndroidEntryPoint
class VideoPlayerFragment : Fragment(), VideoControllerListener {
    companion object {

        @JvmStatic
        val KEY_POSITION_INT = "${VideoPlayerActivity::class.simpleName}_position"

        @JvmStatic
        val KEY_BANGUMI_ID_STR = "${VideoPlayerActivity::class.simpleName}_bangumiId"

        @JvmStatic
        val KEY_EPISODE_ID_STR = "${VideoPlayerActivity::class.simpleName}_episodeId"

        @JvmStatic
        val AUTOSAVE_INTERVAL_MILLION = 1000 * 60L

        @JvmStatic
        val MESSAGE_DEFAULT_WHAT_INT = 1
        fun newInstance(
            context: Context,
            bangumiId: UUID,
            episodeId: UUID,
            position: Int
        ) =
            Bundle()
                .apply {
                    putInt(KEY_POSITION_INT, position)
                    putString(KEY_EPISODE_ID_STR, episodeId.toString())
                    putString(KEY_BANGUMI_ID_STR, bangumiId.toString())
                }.let {
                    Intent(context, VideoPlayerActivity::class.java).apply {
                        putExtras(it)
                    }
                }
    }

    lateinit var binding: FragmentVideoPlayerBinding

    private val player: ExoPlayer by lazy {
        ExoPlayerFactory.newSimpleInstance(
            requireContext(),
            DefaultRenderersFactory(requireContext()),
            DefaultTrackSelector(AdaptiveTrackSelection.Factory()),
            loaderController
        )
    }

    private val cache: Cache by lazy {
        SimpleCache(cacheFile, LeastRecentlyUsedCacheEvictor(1024 * 1024 * 256))
    }
    private val dataSourceFactory: DataSource.Factory by lazy {
        CacheDataSourceFactory(
            cache,
            DefaultHttpDataSourceFactory(
                Util.getUserAgent(
                    requireContext(),
                    requireContext().packageName
                )
            ),
            CacheDataSource.FLAG_BLOCK_ON_CACHE
        )
    }
    private val cacheFile: File by lazy {
        File(requireContext().externalCacheDir, "mediaCache")
    }
    private val loaderController: LoadControl by lazy {
        DefaultLoadControl.Builder().apply {
            setBufferDurationsMs(1000 * 20, 1000 * 60 * 24, 1000 * 10, 1000 * 10)
        }.createDefaultLoadControl()
    }
    val isAutoPlay: Boolean by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getBoolean("isAutoplay", false)
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
        requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
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

    val viewModel by activityViewModels<VideoPlayerVideoModel>()
    val genstureDetector: GestureDetectorCompat by lazy {
        GestureDetectorCompat(requireContext(), NormalGenstureDetector(requireActivity(), this))
    }
    val logWatchProgressRunnable: Runnable by lazy {
        Runnable {
            var episodeEntity =
                viewModel.mediaDescritionLiveData.value?.extras!!.getParcelable<EpisodeEntity>("raw")
            var m = viewModel.watchProgressLiveData?.value
            episodeEntity?.run {
                viewModel.updateWatchProgress(
                    bangumiId = viewModel.bangumiId,
                    episodeEntity = episodeEntity,
                    lastWatchPosition = (binding.player.player.currentPosition / 1000).toFloat(),
                    duration = (binding.player.player.duration / 1000).toFloat(),
                    watchprocessEntity = m
                ).observe(viewLifecycleOwner) {

                }
            }
        }
    }
    val mHandler: Handler by lazy {
        Handler(Looper.getMainLooper()) {
            when (it.what) {
                1 -> {
                    logWatchProgressRunnable.run()
                    mHandler.sendEmptyMessageDelayed(
                        MESSAGE_DEFAULT_WHAT_INT,
                        AUTOSAVE_INTERVAL_MILLION
                    )
                    true
                }

                else -> false
            }
        }
    }
    val onBackPressedCallback: OnBackPressedCallback by lazy {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressed()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVideoPlayerBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.player.player = player
        binding.player.keepScreenOn = true
        setup()
        loadConfig()
        binding.sourceList.adapter = ArrayAdapter<VideoFileEntity>(
            requireContext(),
            R.layout.item_source_listview,
            R.id.title
        )
        viewModel.videoFileLiveData.observe(viewLifecycleOwner) {
            it.parseExtractorMediaSource(viewModel.host, dataSourceFactory).run {
                player.prepare(this)
            }
        }
        var bundle = savedInstanceState?.getBundle("args") ?: requireArguments()
//   TODO:=================这里需要优化 前面的bangumiId 没有从episodeEntity 里面传过来=================================
        viewModel.bangumiId = bundle.getString(KEY_BANGUMI_ID_STR, "")
//===============================================================================================================
        viewModel.mediaDescritionLiveData.observe(viewLifecycleOwner) { mediaDescriptionCompat ->
            (binding.sourceList.adapter as ArrayAdapter<VideoFileEntity>).clear()
            viewModel.fetchVideoFiles(mediaDescriptionCompat.mediaId!!.toUUID())
                .observe(viewLifecycleOwner) {
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
                            mHandler.removeMessages(MESSAGE_DEFAULT_WHAT_INT)
                            mHandler.sendEmptyMessageDelayed(
                                MESSAGE_DEFAULT_WHAT_INT,
                                AUTOSAVE_INTERVAL_MILLION
                            )

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
                                        binding.toolbar.title = mediaDescriptionCompat.title
                                        binding.player.hideController()
                                    }
                            }
                        }
                    }
                }
        }
        viewModel.position.observe(viewLifecycleOwner) { position ->
            binding.listviewEpisode.adapter?.run {
                Log.i("aabb", "position: $position")
                var model = this.getItem(position) as MediaDescriptionCompat
                var label =
                    if (viewModel.isJaFirst || TextUtils.isEmpty(model.title)) model.subtitle else model.title
                binding.toolbar.title = label
                binding.listviewEpisode
                viewModel.mediaDescritionLiveData.postValue(model)
            }
        }

        binding.player.setControllerVisibilityListener {
            binding.appbarlayout.isVisible = it == View.VISIBLE
//            when (it) {
//                View.VISIBLE -> binding.appbarlayout
//                else -> supportActionBar?.hide()
//            }
        }
        binding.player.controllerAutoShow = true
        binding.player.controllerShowTimeoutMs = 3000
//==================================== init?====================================
        binding.listviewEpisode.onItemClickListener =
            OnItemClickListener { parent, view, position, id ->
                parent?.run {
                    if (viewModel.position.value != position) {
                        viewModel.position.postValue(position)
                    }
                    viewModel.isListShowing.postValue(false)
                    hideViewWithAnimation(this)
                }
            }
        val initEpisodeId = bundle.getString(KEY_EPISODE_ID_STR, "").toUUID()
        viewModel.fetchEpisodeList(viewModel.bangumiId.toUUID()).observe(viewLifecycleOwner) {
            when (it.code) {
                Status.SUCCESS -> {
                    it.data?.run {
                        appendEpisode(it.data)
                        if (viewModel.isPlaying.value != true) {
                            findPositionByEpisodeId(initEpisodeId).takeIf { position -> position > -1 }
                                ?.run {
                                    playItem(this)
                                }
                        }
                    }
                }

                Status.LOADING -> {
                    it.data?.run {
                        appendEpisode(it.data)
                        findPositionByEpisodeId(initEpisodeId).takeIf { position -> position > -1 }
                            ?.run {
                                playItem(this)
                            }
                    }
                }

                Status.ERROR -> {
                    it.message?.run {
                        requireActivity().showToast(this)
                    }
                }
            }
        }
//        ===================================================================
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowInsetsController =
                WindowCompat.getInsetsController(
                    requireActivity().window,
                    requireActivity().window.decorView
                )
            // Configure the behavior of the hidden system bars.
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            // Add a listener to update the behavior of the toggle fullscreen button when
            // the system bars are hidden or revealed.
            ViewCompat.setOnApplyWindowInsetsListener(requireActivity().window.decorView) { view, windowInsets ->
                // You can hide the caption bar even when the other system bars are visible.
                // To account for this, explicitly check the visibility of navigationBars()
                // and statusBars() rather than checking the visibility of systemBars().
                if (windowInsets.isVisible(WindowInsetsCompat.Type.navigationBars())
                    || windowInsets.isVisible(WindowInsetsCompat.Type.statusBars())
                ) {
                    // Hide both the status bar and the navigation bar.
                    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                }
                WindowInsetsCompat.toWindowInsetsCompat(view.onApplyWindowInsets(windowInsets.toWindowInsets()))
            }
        } else {
            var flag = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
            if (Build.VERSION.SDK_INT > 15) {
                flag = flag or (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_FULLSCREEN) // hide status bar
            }
            if (Build.VERSION.SDK_INT > 18) {
                flag = flag or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
            binding.player.systemUiVisibility = flag
        }
        binding.toolbar.setNavigationContentDescription(R.string.nav_app_bar_navigate_up_description)
        binding.toolbar.navigationIcon = DrawerArrowDrawable(requireContext()).apply {
            progress = 1f
        }

        binding.toolbar.setNavigationOnClickListener { requireActivity().onBackPressed() }
        viewModel.isListShowing.observe(viewLifecycleOwner) {
            onBackPressedCallback.isEnabled = it
        }
        binding.toolbar.inflateMenu(R.menu.video_menu)
        val sourceMenu = binding.toolbar.menu.findItem(R.id.action_source)
        sourceMenu.isVisible = false
        binding.toolbar.setOnMenuItemClickListener { it ->
            when (it.itemId) {
                android.R.id.home -> {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
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
                        requireContext(),
                        episodeId = mediaId!!,
                        videoFileId = videoFileId
                    ).run {
                        startActivity(this)
                    }
                    true
                }

                else -> false
            }
        }
//=======================================================================
    }

    private fun appendEpisode(items: List<MediaDescriptionCompat>) {
        var list = items.sortedBy { it.extras?.getParcelable<EpisodeEntity>("raw")?.episodeNo }
        binding.listviewEpisode.adapter =
            binding.listviewEpisode.adapter ?: MediaDescriptionAdapter2(requireContext(), list)
        var adapter = binding.listviewEpisode.adapter as MediaDescriptionAdapter2
        adapter.addAll(items)
        adapter.list.reverse()
        adapter.notifyDataSetChanged()
    }

    private fun findPositionByEpisodeId(episodeId: UUID): Int {
        var adapter = binding.listviewEpisode.adapter as MediaDescriptionAdapter2
        return adapter.list.firstOrNull { episodeId.toString() == it.mediaId!! }?.let {
            var index = adapter.list.indexOf(it)
            Log.i("aabb", "$index")
            index
        }?.let {
            it
        } ?: -1
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            onBackPressedCallback
        )
    }

    override fun onDetach() {
        super.onDetach()
        onBackPressedCallback.isEnabled = false
    }

    fun playItem(position: Int) {
        binding.listviewEpisode.performItemClick(
            binding.listviewEpisode.adapter.getView(
                position,
                null,
                null
            ), position, binding.listviewEpisode.adapter.getItemId(position)
        )
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
        var s = PreferenceManager.getDefaultSharedPreferences(requireContext())
        viewModel.isJaFirst = s.getBoolean(getString(Global.RES_JA_FIRST_BOOL), false)
    }

    override fun onDestroyView() {

        requireActivity().unregisterReceiver(broadcastReceiver)
        player.release()
        cache.release()
        mHandler.removeMessages(MESSAGE_DEFAULT_WHAT_INT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(audioRequest.build())
        }
        super.onDestroyView()
    }

    fun showControllerUI() {
        binding.player.showController()
    }

    fun hideControllerUI() {
        binding.player.hideController()
    }

    fun showSourceListView() {
        hideControllerUI()
        showViewAnimation(binding.sourceListRoot)
        viewModel.isListShowing.postValue(true)
    }

    fun showEpisodeListView() {
        hideControllerUI()
        showViewAnimation(binding.listviewEpisode)
        viewModel.isListShowing.postValue(true)
    }

    fun showViewAnimation(view: View) {
        if (view == null || view.visibility == View.VISIBLE) {
            return
        }
        view.visibility = View.VISIBLE
        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_right)
        view.animation = animation
        if (view is ListView) {
            val listView = view
            listView.post { listView.smoothScrollToPosition(listView.checkedItemPosition) }
        }
    }

    fun onBackPressed(): Boolean {
        return if (isListViewShowing()) {
            hideViewWithAnimation(binding.listviewEpisode)
            hideViewWithAnimation(binding.sourceListRoot)
            viewModel.isListShowing.postValue(false)
            true
        } else false
    }

    private fun setup() {
        requireActivity().volumeControlStream = STREAM_MUSIC
        IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY).run {
            requireActivity().registerReceiver(broadcastReceiver, this)
        }
        viewModel.isPlaying.observe(viewLifecycleOwner) {
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
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                super.onPlayerStateChanged(playWhenReady, playbackState)
                when (playbackState) {
                    Player.STATE_ENDED -> {
                        if (isAutoPlay) {
                            var currentPosition = viewModel.position.value!!
                            if (currentPosition > 0) {
                                playItem(--currentPosition)
                            } else {
                                // TODO: 优化 finish
                                requireActivity().finish()
                            }
                        } else {
                            requireActivity().finish()
                        }
                    }
                }
            }

            override fun onLoadingChanged(isLoading: Boolean) {
                if (isLoading)
                    binding.progressbar.show()
                else
                    binding.progressbar.hide()
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
        viewModel.brighnessLiveData.observe(viewLifecycleOwner) {
            val per: Float = it / 17 * 255.0f
            var currentBrightness = requireActivity().window.attributes.screenBrightness * 255f
            if (currentBrightness < 0) {
                currentBrightness =
                    Settings.System.getInt(
                        requireContext().contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS,
                        0
                    )
                        .toFloat()
            }
            currentBrightness += per
            currentBrightness = currentBrightness
                .coerceAtMost(255f)
                .coerceAtLeast(0f)

            val layoutpars = requireActivity().window.attributes
            layoutpars.screenBrightness = currentBrightness / 255.0f
            requireActivity().window.attributes = layoutpars
            showBrightnessVal((currentBrightness / 2.55).toInt())
        }
        viewModel.soundLiveData.observe(viewLifecycleOwner) {
            var currentVol = audioManager.getStreamVolume(STREAM_MUSIC)
            val maxVol = audioManager.getStreamMaxVolume(STREAM_MUSIC)
            currentVol += it.toInt()
            currentVol = currentVol.coerceAtMost(maxVol).coerceAtLeast(0)
            audioManager.setStreamVolume(STREAM_MUSIC, currentVol, 0)
            showVolumeVal(currentVol * 4)
        }
        viewModel.seekPositionLiveData.observe(viewLifecycleOwner) {
            showControllerUI()
            var d = player.currentPosition + it * 5000
            binding.viewgroupProgress.isVisible = true
            binding.textviewExoPosition.text =
                "${
                    Util.getStringForTime(
                        StringBuilder(),
                        Formatter(StringBuilder(), Locale.getDefault()),
                        player.currentPosition.coerceAtLeast(0)
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(audioRequest.build())
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

    fun hideViewWithAnimation(view: View) {
        binding.root.postDelayed({
            view.isVisible = false
            val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_out_right)
            view.animation = animation
        }, 50)
    }

    override fun singleClick() {
        if (binding.listviewEpisode.isVisible || binding.sourceListRoot.isVisible) {
            hideViewWithAnimation(binding.listviewEpisode)
            hideViewWithAnimation(binding.sourceListRoot)
            viewModel.isListShowing.postValue(false)
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
        player.playWhenReady = true
        player.retry()
    }

    private fun stopPlay() {
        player.stop()
    }

    private fun isListViewShowing() =
        binding.listviewEpisode.isVisible || binding.sourceListRoot.isVisible

    /**
     * unused
     */
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