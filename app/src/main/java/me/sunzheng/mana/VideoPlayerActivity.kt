package me.sunzheng.mana

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.media.AudioManager.STREAM_MUSIC
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v4.media.MediaDescriptionCompat
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import androidx.core.util.PatternsCompat
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.ui.PlayerView
import java.io.File
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.sunzheng.mana.core.net.Status
import me.sunzheng.mana.core.net.v2.database.EpisodeEntity
import me.sunzheng.mana.core.net.v2.database.VideoFileEntity
import me.sunzheng.mana.core.net.v2.database.WatchProgressEntity
import me.sunzheng.mana.core.net.v2.parseExtractorMediaSource
import me.sunzheng.mana.core.net.v2.parseMediaItem
import me.sunzheng.mana.core.net.v2.parseMediaItemWithMetadata
import me.sunzheng.mana.core.net.v2.routing.BackendDto
import me.sunzheng.mana.core.net.v2.routing.ROUTING_LOG_TAG
import me.sunzheng.mana.core.net.v2.routing.RouteResult
import me.sunzheng.mana.core.net.v2.routing.RoutingDataSource
import me.sunzheng.mana.core.net.v2.routing.RoutingPreference
import me.sunzheng.mana.core.net.v2.routing.RoutingPreferences
import me.sunzheng.mana.core.net.v2.routing.RoutingResolver
import me.sunzheng.mana.core.net.v2.showToast
import me.sunzheng.mana.core.net.v2.toUUID
import me.sunzheng.mana.databinding.FragmentVideoPlayerBinding
import me.sunzheng.mana.utils.HostUtil
import me.sunzheng.mana.utils.PreferenceManager.Global
import me.sunzheng.mana.videoplayer.GestureActionListener
import me.sunzheng.mana.videoplayer.GestureHandler
import me.sunzheng.mana.videoplayer.MediaDescriptionAdapter2
import me.sunzheng.mana.videoplayer.MediaSessionManager
import me.sunzheng.mana.videoplayer.NodeAdapter
import me.sunzheng.mana.videoplayer.NodeItem
import me.sunzheng.mana.videoplayer.PlaybackRoutingManager
import me.sunzheng.mana.videoplayer.PlaybackStateManager
import me.sunzheng.mana.videoplayer.PlayerController
import me.sunzheng.mana.videoplayer.StallDetector
import me.sunzheng.mana.videoplayer.VideoPlaybackService
import me.sunzheng.mana.videoplayer.VideoPlayerConfig
import me.sunzheng.mana.videoplayer.VideoPlayerVideoModel
import java.util.Formatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class VideoPlayerActivity @Inject constructor() : AppCompatActivity() {
    companion object {
        @JvmStatic
        val KEY_POSITION_INT = VideoPlayerConfig.KEY_POSITION_INT

        @JvmStatic
        val KEY_BANGUMI_ID_STR = VideoPlayerConfig.KEY_BANGUMI_ID_STR

        @JvmStatic
        val KEY_EPISODE_ID_STR = VideoPlayerConfig.KEY_EPISODE_ID_STR

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
        if (Build.VERSION.SDK_INT >= 26) {
            if (resources.configuration.isScreenWideColorGamut) {
                window.colorMode = ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                VideoPlayerFragment().apply { arguments = intent.extras })
            .commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Notify Service to release resources when Activity is destroyed
        // This ensures MediaController, MediaSession, and PlayerController are properly cleaned up
        val releaseIntent = Intent(this, me.sunzheng.mana.videoplayer.VideoPlaybackService::class.java)
        releaseIntent.action = me.sunzheng.mana.videoplayer.VideoPlaybackService.ACTION_RELEASE
        startService(releaseIntent)
        
        // Stop the service after releasing resources
        val stopIntent = Intent(this, me.sunzheng.mana.videoplayer.VideoPlaybackService::class.java)
        stopIntent.action = me.sunzheng.mana.videoplayer.VideoPlaybackService.ACTION_STOP
        stopService(stopIntent)
    }
}

@UnstableApi
@AndroidEntryPoint
class VideoPlayerFragment : Fragment() {

    @Inject
    lateinit var database: me.sunzheng.mana.core.net.v2.database.AppDatabase

    @Inject
    lateinit var routingDataSource: RoutingDataSource

    @Inject
    lateinit var routingPreferences: RoutingPreferences

    companion object {

        @JvmStatic
        val KEY_POSITION_INT = "${VideoPlayerActivity::class.simpleName}_position"

        @JvmStatic
        val KEY_BANGUMI_ID_STR = "${VideoPlayerActivity::class.simpleName}_bangumiId"

        @JvmStatic
        val KEY_EPISODE_ID_STR = VideoPlayerConfig.KEY_EPISODE_ID_STR

        @JvmStatic
        val AUTOSAVE_INTERVAL_MILLION = VideoPlayerConfig.AUTOSAVE_INTERVAL_MS

        @JvmStatic
        val MESSAGE_DEFAULT_WHAT_INT = VideoPlayerConfig.MESSAGE_WATCH_PROGRESS

        /** FR-6 故障转移触发类型。 */
        const val KIND_STALL = "stall"
        const val KIND_ERROR = "error"
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

    /**
     * Media session manager for connecting to VideoPlaybackService.
     */
    private val mediaSessionManager: MediaSessionManager by lazy {
        MediaSessionManager(requireContext())
    }

    /**
     * MediaController from service. This is the only way to access the player.
     * Player is managed entirely by VideoPlaybackService.
     */
    private var mediaController: Player? = null


    /**
     * Temporary cache for dataSourceFactory.
     * Must be released in onDestroyView to avoid SimpleCache conflicts when Fragment is recreated.
     */
    private var tempCache: SimpleCache? = null

    /**
     * Data source factory - needed for parsing media items before they're set on the player.
     * Note: Uses a different cache directory to avoid SimpleCache conflicts with the service's PlayerController.
     * The service's PlayerController uses externalCacheDir/mediaCache, this uses cacheDir/temp_mediaCache.
     * In the future, this could be provided by the service or a shared factory instance.
     */
    private var dataSourceFactory: DataSource.Factory? = null

    /**
     * 播放节点路由编排（FR-1/3/4/5/6）。会话级状态（排除集、故障转移预算）都在它内部，只留内存。
     */
    private val routingManager: PlaybackRoutingManager by lazy {
        PlaybackRoutingManager(
            context = requireContext(),
            dataSource = routingDataSource,
            preferences = routingPreferences,
            host = viewModel.host,
            viewModel = viewModel
        )
    }

    /** 长加载判定（FR-6）。 */
    private var stallDetector: StallDetector? = null

    /** 节点列表适配器（FR-2）。 */
    private var nodeAdapter: NodeAdapter? = null

    /** 当前播放资源的 canonical URL（= HostUtil.makeUp 的结果）。 */
    private var currentCanonicalUrl: String? = null

    /** 当前 MediaDescription（Metadata 与封面都从它取）。 */
    private var currentMediaDescription: MediaDescriptionCompat? = null

    /** 已加载的封面地址，切节点时复用，避免封面丢失。 */
    private var currentArtworkUri: android.net.Uri? = null

    /**
     * 待生效的手动切源（需求方 2026-10-12 调整 FR-4）：偏好立刻写，但**正在看画面时不打断**，
     * 等下一次缓冲（或暂停/切集/出错）再真正换源；没有画面帧时直接换。
     */
    private var pendingManualSwitch: PendingManualSwitch? = null

    /** 手动换源序号：连点多个节点时只应用最后一次，过期响应直接丢弃。 */
    private var switchRequestId = 0

    /** 当前 MediaItem 是否已渲染过首帧（无画面时不需要等缓冲）。 */
    private var hasRenderedFrame = false

    private var probeStarted = false

    /** 同一次播放错误只允许跳一次反馈页（FR-6 冲突条款）。 */
    private var feedbackNavigated = false

    /** 触发故障转移的最近一次播放错误（用于失败后保留原有反馈页行为）。 */
    private var lastPlaybackError: PlaybackException? = null

    /** 换源时要保留的位置与播放态；位置在**真正换源的那一刻**现读。 */
    private data class SwitchState(val positionMs: Long, val playWhenReady: Boolean)

    private data class PendingManualSwitch(
        val canonicalUrl: String,
        val preference: RoutingPreference,
        val requestId: Int,
        /** 本次选择对应展示文案（"自动（由服务器选择）" 或节点名），用于切换结果提示。 */
        val selectedLabel: String
    )

    /**
     * Playback state manager for tracking watch progress and auto-save.
     * Will be initialized after service connection.
     */
    private var playbackStateManager: PlaybackStateManager? = null

    /**
     * Gesture handler for brightness, volume, and seek controls.
     * Will be initialized after service connection.
     */
    private var gestureHandler: GestureHandler? = null

    val isAutoPlay: Boolean by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getBoolean(VideoPlayerConfig.PREF_KEY_AUTOPLAY, false)
    }
    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.run {
                when (action) {
                    AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                        // Pause playback when headphones are disconnected or audio output changes
                        // Audio focus is managed by VideoPlaybackService
                        mediaController?.let { p ->
                            if (p.isPlaying) {
                                p.pause()
                            }
                        }
                    }

                    // ConnectivityManager.CONNECTIVITY_ACTION is deprecated in API 28+
                    // Use NetworkCallback instead if network state monitoring is needed
                    // For now, removed as it's not actively used
                }
            }
        }
    }
    val audioManager: AudioManager by lazy {
        requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    val viewModel by activityViewModels<VideoPlayerVideoModel>()
    val onBackPressedCallback: OnBackPressedCallback by lazy {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressed()
            }
        }
    }
    // MediaSession is now managed by VideoPlaybackService
    // No need for local MediaSession here

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

        binding.player.keepScreenOn = true

        // Reset auto-play flag on view creation (allows auto-play after configuration changes)
        hasAutoPlayed = false

        // Initialize dataSourceFactory early to avoid lazy initialization issues
        initializeDataSourceFactory()

        // Connect to playback service
        connectToService()

        setup()
        loadConfig()
        binding.sourceList.adapter = ArrayAdapter<VideoFileEntity>(
            requireContext(),
            R.layout.item_source_listview,
            R.id.title
        )
        // Observe mediaDescription to prepare metadata before setting MediaItem
        viewModel.mediaDescritionLiveData.observe(viewLifecycleOwner) { mediaDescriptionCompat ->
            currentMediaDescription = mediaDescriptionCompat
        }

        viewModel.videoFileLiveData.observe(viewLifecycleOwner) { videoFile ->
            mediaController?.let { p ->
                dataSourceFactory?.let { factory ->
                    // Validate and complete video URL using HostUtil
                    val videoUri = videoFile.url?.let { url ->
                        try {
                            // Use HostUtil.makeUp() to validate and complete the URL
                            val validatedUrl = HostUtil.makeUp(
                                viewModel.host,
                                url
                            )
                            validatedUrl.toUri()
                        } catch (e: Exception) {
                            Log.e("VideoPlayer", "Failed to validate video URL: $url", e)
                            null
                        }
                    }

                    videoUri?.let { canonicalUri ->
                        val mediaDesc = currentMediaDescription
                        val canonical = routingManager.canonicalUrlOf(videoFile.url)
                        if (canonical == null) {
                            // FR-8 / §11：host 空白 → 不做路由，保持 HostUtil 既有行为
                            Log.d(ROUTING_LOG_TAG, "skip routing reason=blank-host") // L2
                            applyPlaybackUri(canonicalUri, null, mediaDesc)
                            return@let
                        }
                        if (canonical != currentCanonicalUrl) {
                            // 资源变了（切集/切源）：待生效的手动换源作废，新资源会用最新偏好重新解析
                            pendingManualSwitch = null
                            switchRequestId++
                            hasRenderedFrame = false
                        }
                        currentCanonicalUrl = canonical
                        // L1；canonical 变化 = 切集/切源 → 重置排除集与故障转移预算（§6.3）
                        routingManager.startSession(canonical)
                        startProbeIfNeeded(canonical)
                        if (routingManager.needRoute()) { // L7
                            lifecycleScope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    routingManager.resolveIfNeeded(canonical)
                                }
                                val uri = (result as? RouteResult.Routed)
                                    ?.playbackUrl?.toUri() ?: canonicalUri
                                applyPlaybackUri(uri, null, mediaDesc)
                            }
                        } else {
                            // FR-5：auto 且正常起播 → 直接用 canonical（与改动前一致，不额外请求、不加延迟）
                            applyPlaybackUri(canonicalUri, null, mediaDesc)
                        }
                    }
                }
            }
        }
        var bundle = savedInstanceState?.getBundle("args") ?: requireArguments()
        // Get bangumiId from bundle (will be updated from episodeEntity when episode loads)
        val initialBangumiId = bundle.getString(KEY_BANGUMI_ID_STR, "")
        viewModel.bangumiId = initialBangumiId
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
                                    playbackStateManager?.updateWatchProgressEntity(this)
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

                            // Start tracking watch progress for this episode
                            val episodeEntity =
                                mediaDescriptionCompat.extras?.getParcelable<EpisodeEntity>("raw")
                            episodeEntity?.let {
                                // Fix: Use bangumiId from episodeEntity if available
                                val bangumiId = it.bangumiId?.toString() ?: viewModel.bangumiId
                                if (bangumiId.isNotEmpty()) {
                                    viewModel.bangumiId = bangumiId
                                }
                                playbackStateManager?.startTracking(
                                    episodeEntity = it,
                                    watchProgressEntity = viewModel.watchProgressLiveData?.value,
                                    bangumiId = bangumiId.takeIf { id -> id.isNotEmpty() }
                                )
                            }

                        }

                        Status.ERROR -> it?.message?.run {
                            showToast(this)
                        }

                        Status.LOADING -> {
                            if (it.data?.size ?: 0 > 0) {
                                it.data?.takeIf { list -> list.isNotEmpty() }
                                    ?.firstOrNull()
                                    ?.let { videoFile ->
                                        dataSourceFactory?.let { factory ->
                                            videoFile.parseExtractorMediaSource(
                                                viewModel.host,
                                                factory
                                            )
                                        }
                                    }?.run {
                                        mediaController?.prepare()
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
                var model = this.getItem(position) as MediaDescriptionCompat
                var label =
                    if (viewModel.isJaFirst || TextUtils.isEmpty(model.title)) model.subtitle else model.title
                binding.toolbar.title = label
                binding.listviewEpisode
                viewModel.mediaDescritionLiveData.postValue(model)

                // Update tracking when episode changes
                val episodeEntity = model.extras?.getParcelable<EpisodeEntity>("raw")
                episodeEntity?.let {
                    val bangumiId = it.bangumiId?.toString() ?: viewModel.bangumiId
                    if (bangumiId.isNotEmpty()) {
                        viewModel.bangumiId = bangumiId
                    }
                    // Stop previous tracking and start new one
                    playbackStateManager?.stopTracking()
                    playbackStateManager?.startTracking(
                        episodeEntity = it,
                        watchProgressEntity = viewModel.watchProgressLiveData?.value,
                        bangumiId = bangumiId.takeIf { id -> id.isNotEmpty() }
                    )
                }
            }
        }
        binding.player.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener {
            binding.appbarlayout.isVisible = it == View.VISIBLE
        })
        binding.player.controllerHideOnTouch = true
        binding.player.controllerAutoShow = true
        binding.player.controllerShowTimeoutMs =
            VideoPlayerConfig.CONTROLLER_AUTO_SHOW_TIMEOUT_MS.toInt()
        binding.player.showController()
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
//        binding.toolbar.setNavigationContentDescription(R.string.nav_app_bar_navigate_up_description)
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
        // FR-1/§7.1：未探测到能力前隐藏"节点"入口；已缓存 yes（上次会话探测过）则先显示
        binding.toolbar.menu.findItem(R.id.action_node)?.isVisible =
            routingPreferences.capability() == RoutingPreferences.CAPABILITY_YES
        setupNodePanel()
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

                R.id.action_node -> {
                    showNodeListView()
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

    /**
     * Checks if background playback is enabled in user preferences.
     * @return true if background playback is enabled, false otherwise (default)
     */
    private fun isBackgroundPlaybackEnabled(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getBoolean(getString(R.string.pref_key_background_playback), false)
    }

    override fun onStart() {
        super.onStart()
        val allowBackground = isBackgroundPlaybackEnabled()
        if (allowBackground) {
            // If background playback is enabled, playback may already be continuing
            // Reconnect to service if needed, and ensure playback continues
            mediaController?.playWhenReady = true
        } else {
            // If background playback is disabled, check if auto-play should trigger
            // Auto-play should only trigger once when player becomes ready (handled in listener)
            // Here we just ensure we don't interfere with auto-play logic
            // If player is already ready and hasn't auto-played yet, the listener will handle it
        }
    }

    override fun onStop() {
        super.onStop()
        val allowBackground = isBackgroundPlaybackEnabled()
        if (!allowBackground) {
            // If background playback is disabled, pause playback when going to background
            mediaController?.playWhenReady = false
        }
        // If background playback is enabled, don't pause - let VideoPlaybackService handle it
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
            mediaController?.seekTo(convert.toLong())
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

    @SuppressLint("WrongThread")
    @WorkerThread
    override fun onDestroyView() {
        requireActivity().unregisterReceiver(broadcastReceiver)
        playbackStateManager?.release()

        // L18：结束本次播放会话，内存态（排除集、预算、待恢复位置）全部丢弃
        Log.d(ROUTING_LOG_TAG, "session end reason=view-destroyed")
        stallDetector?.cancel()
        stallDetector = null
        pendingManualSwitch = null
        nodeAdapter = null

        // Remove player listener to avoid memory leak
        playerListener?.let { mediaController?.removeListener(it) }
        playerListener = null
        mediaSessionManager.disconnect()
        mediaController = null

        // Release tempCache to avoid SimpleCache conflicts when Fragment is recreated
        tempCache?.let { cache ->
            try {
                cache.release()
            } catch (e: Exception) {
                // Cache may already be released, ignore
            }
            tempCache = null
        }
        dataSourceFactory = null

        // Stop the playback service when Fragment is destroyed
        // This ensures the service is properly cleaned up when the video player is closed
        val allowBackground = isBackgroundPlaybackEnabled()
        if (!allowBackground) {
            // If background playback is disabled, stop the service
            val intent = Intent(requireContext(), VideoPlaybackService::class.java)
            intent.action = VideoPlaybackService.ACTION_STOP
            requireContext().stopService(intent)
        } else {
            // If background playback is enabled, service will continue running
            // But we still disconnect the MediaController
        }

        // Audio focus is managed by VideoPlaybackService
        super.onDestroyView()
    }

    /**
     * Initializes dataSourceFactory with a temporary cache.
     * Must be called in onViewCreated to ensure proper lifecycle management.
     */
    private fun initializeDataSourceFactory() {
        if (dataSourceFactory == null) {
            // Use a temporary cache directory that won't conflict with the service's cache
            // Service uses: externalCacheDir/mediaCache
            // This uses: cacheDir/temp_mediaCache (different directory, no conflict)
            val tempCacheDir = File(requireContext().cacheDir, "temp_mediaCache")
            tempCache = SimpleCache(
                tempCacheDir,
                LeastRecentlyUsedCacheEvictor(VideoPlayerConfig.MAX_CACHE_SIZE_BYTES),
                StandaloneDatabaseProvider(requireContext())
            )
            dataSourceFactory = CacheDataSource.Factory()
                .setCache(tempCache!!)
                .setCacheWriteDataSinkFactory {
                    CacheDataSink.Factory()
                        .setBufferSize(VideoPlayerConfig.CACHE_BUFFER_SIZE_BYTES.toInt())
                        .createDataSink()
                }
                .setCacheReadDataSourceFactory(
                    DefaultHttpDataSource.Factory()
                        .setUserAgent(
                            Util.getUserAgent(
                                requireContext(),
                                requireContext().packageName
                            )
                        )
                )
        }
    }

    /**
     * Connects to VideoPlaybackService and initializes components.
     */
    private fun connectToService() {
        mediaSessionManager.connect(
            onConnected = { controller ->
                mediaController = controller
                binding.player.player = controller

                // Initialize components that depend on player
                playbackStateManager =
                    PlaybackStateManager(controller, viewModel, viewLifecycleOwner)
                gestureHandler = GestureHandler(requireActivity(), object : GestureActionListener {
                    override fun onSingleTap() {
                        singleClick()
                    }

                    override fun onDoubleTap() {
                        playState()
                    }

                    override fun onBrightnessChange(deltaValue: Float) {
                        adjustBrightness(deltaValue)
                    }

                    override fun onVolumeChange(deltaValue: Float) {
                        adjustVolume(deltaValue)
                    }

                    override fun onSeekChange(deltaValue: Float) {
                        seekTo(deltaValue)
                    }
                })

                // Set up touch listener for gestures
                binding.player.setOnTouchListener { _, event ->
                    gestureHandler?.gestureDetector?.onTouchEvent(event) ?: false
                }

                // Set up player listeners
                setupPlayerListeners()
            },
            onError = { error ->
                Log.e(
                    "VideoPlayerFragment",
                    "Failed to connect to playback service: ${error.message}",
                    error
                )
                // Service connection is required - show error to user
                // Player is managed entirely by VideoPlaybackService
            }
        )
    }


    /**
     * Flag to track if auto-play has been triggered for this page entry.
     * Reset in onViewCreated to allow auto-play after configuration changes.
     */
    private var hasAutoPlayed = false

    /**
     * Player listener for UI updates.
     * Stored to allow removal to prevent memory leaks.
     */
    private var playerListener: Player.Listener? = null

    /**
     * Sets up player listeners (moved from setup()).
     */
    private fun setupPlayerListeners() {
        // Remove existing listener to avoid memory leak
        playerListener?.let { mediaController?.removeListener(it) }

        // FR-6：长加载判定（会话级；onDestroyView 里 cancel）
        if (stallDetector == null) {
            stallDetector = StallDetector(
                scope = viewLifecycleOwner.lifecycleScope,
                onSustainedStall = { elapsedMs -> onSustainedStall(elapsedMs) }
            )
        }

        playerListener = object : Player.Listener {
            var isInitialized = false
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                Log.d("VideoPlayerFragment", "playState:${playbackState}")
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        stallDetector?.onBufferingStart()
                        // 待生效的手动换源：正好利用缓冲点换源，不额外打断画面
                        flushManualSwitch()
                    }

                    Player.STATE_READY -> {
                        isInitialized = true
                        stallDetector?.onReady()

                        // Auto-play on first ready state (only once per page entry)
                        if (!hasAutoPlayed) {
                            mediaController?.let { p ->
                                // Always set playWhenReady to true for auto-play
                                // This ensures playback starts automatically when player is ready
                                p.playWhenReady = true
                                Log.d(
                                    "VideoPlayerFragment",
                                    "Auto-play triggered: playWhenReady = true, isPlaying = ${p.isPlaying}"
                                )
                            }
                            hasAutoPlayed = true
                        }

                        // 首帧已渲染（决定手动换源是否需要等缓冲）+ 允许"下一次（新的）播放错误"再跳反馈页
                        hasRenderedFrame = true
                        feedbackNavigated = false
                    }

                    Player.STATE_ENDED -> {
                        stallDetector?.cancel()
                        if (!isInitialized) {
                            return
                        }
                        if (isAutoPlay) {
                            var currentPosition = viewModel.position.value ?: 0
                            if (currentPosition > 0) {
                                playItem(--currentPosition)
                            }
                        } else {
                            requireActivity().finish()
                        }
                    }
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                super.onPlayWhenReadyChanged(playWhenReady, reason)
                // FR-6：用户主动暂停期间不计时
                if (playWhenReady) stallDetector?.onResumed() else stallDetector?.onPaused()
                // 暂停时换源没有可见打断 → 待生效的手动换源立刻执行
                if (!playWhenReady) flushManualSwitch()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                binding.progressbar.isVisible = !isPlaying
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                Log.e("VideoPlayerFragment", "Playback error occurred", error)
                lastPlaybackError = error
                // 有待生效的手动换源 → 用户的选择优先，直接换源（不走自动故障转移/反馈页）
                if (pendingManualSwitch != null) {
                    flushManualSwitch()
                    return
                }
                // FR-6 冲突条款：IO 类错误 + 路由可用 + 预算未用尽 → 先尝试换节点，不立刻跳反馈页
                if (isIoError(error.errorCode) &&
                    viewModel.routingSupportedLiveData.value == true &&
                    routingManager.canFailover()
                ) {
                    Log.d(
                        ROUTING_LOG_TAG,
                        "failover trigger kind=$KIND_ERROR elapsedMs=0 errorCode=${error.errorCode} " +
                            "currentBackend=${routingManager.currentBackend()?.id ?: "-"}"
                    ) // L15
                    attemptFailover(KIND_ERROR, 0L, error.errorCode)
                    return
                }
                navigateToFeedback(error)
            }
        }

        playerListener?.let { mediaController?.addListener(it) }
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

    // ==================================== 播放节点路由（FR-1/2/3/4/5/6） ====================================

    private fun setupNodePanel() {
        val adapter = NodeAdapter(requireContext()) { item -> onNodeSelected(item) }
        nodeAdapter = adapter
        binding.nodeList.adapter = adapter
        binding.nodeList.setOnItemClickListener { _, _, position, _ ->
            adapter.dispatchSelection(position)
        }
        viewModel.routingSupportedLiveData.observe(viewLifecycleOwner) { supported ->
            // FR-1：能力 no → 隐藏入口；unknown（还没探测）保持隐藏
            if (supported != null) {
                binding.toolbar.menu.findItem(R.id.action_node)?.isVisible = supported
            }
        }
        viewModel.currentNodeLiveData.observe(viewLifecycleOwner) { backend ->
            updateNodeMenuTitle(backend)
        }
        viewModel.nodeListLiveData.observe(viewLifecycleOwner) { items ->
            adapter.submit(items)
            val index = adapter.indexOfCurrent()
            if (index >= 0) binding.nodeList.setItemChecked(index, true)
        }
    }

    /** FR-1：进入播放器后、首个 videoFile 加载时后台探测一次；探测绝不阻塞播放。 */
    private fun startProbeIfNeeded(canonical: String) {
        if (probeStarted) return
        probeStarted = true
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { routingManager.probeCapability(canonical) }
            updateNodeMenuTitle(routingManager.currentBackend())
        }
    }

    /**
     * 设置播放地址：URI 未变不重载（避免重新缓冲，保持既有行为）；变了则把 [switch] 里的起始位置
     * 随 MediaItem 一起传下去，从目标位置直接起播（不再"先 0 后 seek"），并按需恢复播放/暂停态。
     */
    private fun applyPlaybackUri(
        uri: android.net.Uri,
        switch: SwitchState?,
        mediaDesc: MediaDescriptionCompat?
    ) {
        val p = mediaController ?: return
        val currentUri = p.currentMediaItem?.localConfiguration?.uri
        if (currentUri?.toString() == uri.toString()) {
            if (switch != null) {
                Log.d(
                    ROUTING_LOG_TAG,
                    "switch start positionMs=${switch.positionMs} playWhenReady=${switch.playWhenReady}"
                ) // L14
                if (switch.positionMs > 0) p.seekTo(switch.positionMs)
                p.playWhenReady = switch.playWhenReady
            }
            Log.d(
                "VideoPlayer",
                "MediaItem URI unchanged, skipping setMediaItem to avoid re-buffering. Current URI: " +
                    RoutingResolver.normalizedCacheKey(currentUri.toString())
            )
            return
        }

        val initialItem = if (mediaDesc != null) {
            uri.parseMediaItemWithMetadata(
                title = mediaDesc.title?.toString(),
                artist = mediaDesc.subtitle?.toString(),
                artworkUri = currentArtworkUri
            )
        } else {
            uri.parseMediaItem()
        }
        if (switch != null) {
            Log.d(
                ROUTING_LOG_TAG,
                "switch start positionMs=${switch.positionMs} playWhenReady=${switch.playWhenReady}"
            ) // L14
        }
        if (switch != null) {
            p.setMediaItem(initialItem, switch.positionMs)
        } else {
            p.setMediaItem(initialItem)
        }
        hasRenderedFrame = false
        // 不打印签名播放地址（FR-11）：只打剔除 __mira_route 后的归一化地址
        Log.d(
            "VideoPlayer",
            "Set MediaItem (URI changed), title: ${mediaDesc?.title}, artist: ${mediaDesc?.subtitle}, " +
                "URI: ${RoutingResolver.normalizedCacheKey(uri.toString())}"
        )
        p.prepare()
        if (switch != null) p.playWhenReady = switch.playWhenReady
        Log.d("VideoPlayer", "Player prepare() called, will trigger auto-play when ready")
        loadArtworkIfNeeded(uri, mediaDesc, p)
    }

    private fun loadArtworkIfNeeded(
        uri: android.net.Uri,
        mediaDesc: MediaDescriptionCompat?,
        p: Player
    ) {
        if (mediaDesc == null) return
        val episodeEntity = mediaDesc.extras?.getParcelable<EpisodeEntity>("raw") ?: return
        val bangumiId = episodeEntity.bangumiId ?: return
        lifecycleScope.launch {
            try {
                val bangumiEntity = database.bangumiDao().queryById(bangumiId)
                Log.d(
                    "VideoPlayer",
                    "Queried BangumiEntity: ${bangumiEntity?.nameCn}, coverImage: ${bangumiEntity?.coverImage?.url}, cover: ${bangumiEntity?.cover}"
                )
                val coverUrl = bangumiEntity?.coverImage?.url ?: bangumiEntity?.cover ?: return@launch
                Log.d("VideoPlayer", "Found cover URL: $coverUrl")
                val fullCoverUrl = HostUtil.makeUp(viewModel.host, coverUrl)
                currentArtworkUri = fullCoverUrl.toUri()

                val updatedItem = uri.parseMediaItemWithMetadata(
                    title = mediaDesc.title?.toString(),
                    artist = mediaDesc.subtitle?.toString(),
                    artworkUri = fullCoverUrl.toUri()
                )
                val currentIndex = p.currentMediaItemIndex
                if (currentIndex >= 0 && currentIndex < p.mediaItemCount) {
                    p.replaceMediaItem(currentIndex, updatedItem)
                    Log.d("VideoPlayer", "Updated MediaItem with cover: $fullCoverUrl")
                } else {
                    p.setMediaItem(updatedItem)
                    Log.d("VideoPlayer", "Set MediaItem with cover: $fullCoverUrl")
                }
            } catch (e: Exception) {
                Log.w("VideoPlayer", "Failed to load Bangumi cover for notification", e)
            }
        }
    }

    /**
     * 真正执行一次手动换源（延迟策略的落点）：位置与播放态**在此刻现读**，
     * 起始位置随 MediaItem 一起交给播放器，不做"先 0 后 seek"。
     */
    private fun flushManualSwitch(pending: PendingManualSwitch? = pendingManualSwitch) {
        if (pending == null) return
        if (pending === pendingManualSwitch) pendingManualSwitch = null
        val canonical = currentCanonicalUrl
        if (canonical == null || canonical != pending.canonicalUrl) {
            // 资源已经变了（切集/切源）：本次待生效的换源作废
            return
        }
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                routingManager.resolveForManual(canonical, pending.preference)
            }
            // 用户又选了别的节点 → 丢弃这次过期响应
            if (pending.requestId != switchRequestId) return@launch

            val p = mediaController
            val state = SwitchState(
                positionMs = p?.currentPosition ?: 0L,
                playWhenReady = p?.playWhenReady == true
            )
            if (result is RouteResult.Routed) {
                applyPlaybackUri(
                    result.playbackUrl.toUri(),
                    state,
                    currentMediaDescription
                )
                val actual = result.backend
                if (pending.preference.isBackend && actual != null &&
                    actual.id != pending.preference.backendId
                ) {
                    // FR-3/AC-6：服务端回退 → 提示实际使用的节点
                    showToast(
                        getString(
                            R.string.node_fallback_to,
                            pending.selectedLabel,
                            actual.displayLabel
                        )
                    )
                } else {
                    showToast(
                        getString(
                            R.string.node_switched_to,
                            actual?.displayLabel ?: getString(R.string.node_auto)
                        )
                    )
                }
            } else {
                // 路由失败 → 回落 canonical，仍然保位续播（FR-8）
                applyPlaybackUri(canonical.toUri(), state, currentMediaDescription)
                showToast(getString(R.string.node_switched_to, pending.selectedLabel))
            }
            updateNodeMenuTitle(routingManager.currentBackend())
            routingManager.publishCachedNodes()
        }
    }

    /** FR-2：打开节点面板（先渲染缓存，再拉取最新）。 */
    fun showNodeListView() {
        hideControllerUI()
        showViewAnimation(binding.nodeListRoot)
        viewModel.isListShowing.postValue(true)
        routingManager.publishCachedNodes()
        val canonical = currentCanonicalUrl ?: return
        lifecycleScope.launch {
            val fresh = withContext(Dispatchers.IO) { routingManager.refreshBackends(canonical) }
            if (fresh == null && routingPreferences.cachedBackends() == null) {
                showToast(getString(R.string.node_list_empty))
            }
        }
    }

    private fun hideNodePanel() {
        hideViewWithAnimation(binding.nodeListRoot)
        viewModel.isListShowing.postValue(false)
    }

    /**
     * FR-3/FR-4（2026-10-12 调整）：点击节点（或"自动"）→ **立即写全局偏好**；
     * 正在看画面时不打断，登记为"待生效"，等下一次缓冲/暂停/切集/出错再真正换源；
     * 没有画面帧或已暂停时直接换。按下不弹提示，真正切换时才 toast。
     */
    private fun onNodeSelected(item: NodeItem) {
        val canonical = currentCanonicalUrl ?: return
        val selectedLabel: String
        when (item) {
            is NodeItem.Auto -> {
                routingPreferences.setAuto()
                selectedLabel = getString(R.string.node_auto)
                Log.d(ROUTING_LOG_TAG, "manual auto") // L13
            }

            is NodeItem.Backend -> {
                if (!item.enabled) return
                routingPreferences.setBackend(item.backend.id, item.backend.label)
                selectedLabel = item.backend.displayLabel
                Log.d(
                    ROUTING_LOG_TAG,
                    "manual select backendId=${item.backend.id} label=$selectedLabel"
                ) // L13
            }

            is NodeItem.Group -> return
        }
        hideNodePanel()

        val pending = PendingManualSwitch(
            canonicalUrl = canonical,
            preference = routingPreferences.preference(),
            requestId = ++switchRequestId,
            selectedLabel = selectedLabel
        )
        // G1：只有在"画面正在播且播放器处于 READY"时才值得延迟——
        // onPlaybackStateChanged 只在状态"变化"时回调（media3 源码：Called when the value ... changes），
        // 若点选时播放器已经卡在 STATE_BUFFERING，就不会再有一次"进入缓冲"的变化 → 必须当场换源。
        val controller = mediaController
        val playing = controller?.playWhenReady == true
        val ready = controller?.playbackState == Player.STATE_READY
        if (playing && hasRenderedFrame && ready) {
            // 正在看画面 → 不打断：等下一次缓冲（或暂停/切集/出错）再换源
            pendingManualSwitch = pending
        } else {
            // 没有画面帧（还没起播/黑屏）、已暂停、或已经卡在缓冲中 → 立刻换源，没有额外可见打断
            flushManualSwitch(pending)
        }
    }

    /** FR-7：入口文案随状态变化（自动 / 实际节点 label）。 */
    private fun updateNodeMenuTitle(backend: BackendDto?) {
        val item = binding.toolbar.menu.findItem(R.id.action_node) ?: return
        val state = backend?.displayLabel
            ?: routingPreferences.preference().label
            ?: getString(R.string.node_auto)
        item.title = getString(R.string.title_action_node_state, state)
        item.tooltipText = item.title
    }

    /** FR-6：长加载触发故障转移。 */
    private fun onSustainedStall(elapsedMs: Long) {
        if (viewModel.routingSupportedLiveData.value != true) return
        if (!routingManager.canFailover()) {
            Log.d(
                ROUTING_LOG_TAG,
                "failover exhausted count=${routingManager.failoverCount()} action=canonical"
            ) // L17
            return
        }
        Log.d(
            ROUTING_LOG_TAG,
            "failover trigger kind=$KIND_STALL elapsedMs=$elapsedMs errorCode=0 " +
                "currentBackend=${routingManager.currentBackend()?.id ?: "-"}"
        ) // L15
        attemptFailover(KIND_STALL, elapsedMs, 0)
    }

    /** FR-6：执行一次自动故障转移（有界：每资源 10 分钟 ≤2 次、间隔 ≥30s）。 */
    private fun attemptFailover(kind: String, elapsedMs: Long, errorCode: Int) {
        val canonical = currentCanonicalUrl ?: return
        val failedBackendId = routingManager.currentBackend()?.id
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                routingManager.failover(canonical, failedBackendId)
            }
            // 触发点本身就在缓冲/出错，位置与播放态在此刻现读（L14 由 applyPlaybackUri 打）
            val p = mediaController
            val state = SwitchState(
                positionMs = p?.currentPosition ?: 0L,
                playWhenReady = p?.playWhenReady == true
            )
            when (result) {
                is RouteResult.Routed -> {
                    applyPlaybackUri(
                        result.playbackUrl.toUri(),
                        state,
                        currentMediaDescription
                    )
                    val actual = result.backend?.displayLabel ?: getString(R.string.node_auto)
                    val preferred = routingPreferences.preference()
                    if (kind == KIND_STALL && preferred.isBackend &&
                        result.backend?.id != preferred.backendId
                    ) {
                        showToast(
                            getString(
                                R.string.node_fallback_to,
                                preferred.label ?: preferred.backendId ?: "",
                                actual
                            )
                        )
                    } else {
                        showToast(getString(R.string.node_switched_to, actual))
                    }
                }

                is RouteResult.Fallback -> {
                    if (kind == KIND_ERROR) {
                        // 换节点失败 → 保留原有"跳反馈页"行为（同一次错误只跳一次，FR-6）
                        lastPlaybackError?.let { navigateToFeedback(it) }
                    } else if (currentPlaybackUrl() != canonical) {
                        // 预算用尽/失败 → 回落 canonical 播放一次后停止自动重试
                        applyPlaybackUri(
                            canonical.toUri(),
                            state,
                            currentMediaDescription
                        )
                    }
                }
            }
            updateNodeMenuTitle(routingManager.currentBackend())
            routingManager.publishCachedNodes()
        }
    }

    private fun currentPlaybackUrl(): String? =
        mediaController?.currentMediaItem?.localConfiguration?.uri?.toString()

    /** 原有反馈页跳转（同一次播放错误只跳一次）。 */
    private fun navigateToFeedback(error: PlaybackException) {
        if (feedbackNavigated) return
        feedbackNavigated = true

        // Get error information
        val errorMessage = buildString {
            append("播放器错误: ")
            append(error.message ?: "Unknown error")
            append("\n错误代码: ${error.errorCode}")
            append("\n错误名称: ${error.errorCodeName}")
            if (error.cause != null) {
                append("\n原因: ${error.cause?.message}")
            }
        }

        // Get stack trace separately
        val stackTrace = try {
            error.stackTraceToString()
        } catch (e: Exception) {
            Log.e("VideoPlayerFragment", "Failed to get stack trace", e)
            // Fallback: try to get stack trace from cause
            error.cause?.stackTraceToString() ?: "无法获取堆栈跟踪"
        }

        // Get current episode ID and video file ID
        val episodeId = viewModel.mediaDescritionLiveData.value?.mediaId
        val videoFileId = viewModel.videoFileLiveData.value?.id?.toString()

        // Open FeedbackActivity with error information
        val intent = FeedbackActivity.newInstance(
            requireContext(),
            episodeId,
            videoFileId
        )
        // Add error message and stack trace as separate extras
        intent.putExtra("error_message", errorMessage)
        intent.putExtra("error_stack_trace", stackTrace)
        startActivity(intent)
    }

    /** media3 的 IO 类错误码：ERROR_CODE_IO_UNSPECIFIED(2000) .. IO_READ_POSITION_OUT_OF_RANGE(2008)。 */
    private fun isIoError(errorCode: Int): Boolean = errorCode in 2000..2008

    fun showViewAnimation(view: View) {
        if (view == null || view.visibility == View.VISIBLE) {
            return
        }
        view.visibility = View.VISIBLE
        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_right)
        view.animation = animation
        if (view is ListView) {
            val listView = view
            // checkedItemPosition 为 -1（还没有选中项）时不做无意义的滚动
            listView.post {
                val checked = listView.checkedItemPosition
                if (checked >= 0) listView.smoothScrollToPosition(checked)
            }
        }
    }

    fun onBackPressed(): Boolean {
        return if (isListViewShowing()) {
            hideViewWithAnimation(binding.listviewEpisode)
            hideViewWithAnimation(binding.sourceListRoot)
            hideViewWithAnimation(binding.nodeListRoot)
            viewModel.isListShowing.postValue(false)
            true
        } else false
    }

    @OptIn(UnstableApi::class)
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
                    pausePlay()
                }
            }
        }
        // Player listener will be set up in setupPlayerListeners() after service connection
        // Player is managed by Service, don't set playWhenReady here
        binding.player.setOnTouchListener { _, event ->
            gestureHandler?.gestureDetector?.onTouchEvent(
                event
            ) ?: false
        }

        // Observe brightness changes and update UI
        viewModel.brighnessLiveData.observe(viewLifecycleOwner) {
            val per: Float =
                it / VideoPlayerConfig.BRIGHTNESS_ADJUSTMENT_FACTOR * VideoPlayerConfig.MAX_BRIGHTNESS
            var currentBrightness =
                requireActivity().window.attributes.screenBrightness * VideoPlayerConfig.MAX_BRIGHTNESS
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
                .coerceAtMost(VideoPlayerConfig.MAX_BRIGHTNESS)
                .coerceAtLeast(VideoPlayerConfig.MIN_BRIGHTNESS)

            val layoutpars = requireActivity().window.attributes
            layoutpars.screenBrightness = currentBrightness / VideoPlayerConfig.MAX_BRIGHTNESS
            requireActivity().window.attributes = layoutpars
            showBrightnessVal((currentBrightness / 2.55).toInt())
        }
        viewModel.soundLiveData.observe(viewLifecycleOwner) {
            var currentVol = audioManager.getStreamVolume(STREAM_MUSIC)
            val maxVol = audioManager.getStreamMaxVolume(STREAM_MUSIC)
            currentVol += it.toInt()
            currentVol = currentVol.coerceAtMost(maxVol).coerceAtLeast(0)
            audioManager.setStreamVolume(STREAM_MUSIC, currentVol, 0)
            showVolumeVal(currentVol * VideoPlayerConfig.VOLUME_DISPLAY_MULTIPLIER)
        }
        viewModel.seekPositionLiveData.observe(viewLifecycleOwner) {
            showControllerUI()
            mediaController?.let { p ->
                var d = p.currentPosition + it * VideoPlayerConfig.SEEK_STEP_DURATION_MS
                binding.viewgroupProgress.isVisible = true
                binding.textviewExoPosition.text =
                    "${
                        Util.getStringForTime(
                            StringBuilder(),
                            Formatter(StringBuilder(), Locale.getDefault()),
                            p.currentPosition.coerceAtLeast(0)
                        )
                    }/${
                        Util.getStringForTime(
                            StringBuilder(),
                            Formatter(StringBuilder(), Locale.getDefault()),
                            p.duration
                        )
                    }"
                p.seekTo(d)
            }
            binding.viewgroupProgress.postDelayed({
                binding.viewgroupProgress.isVisible = false
            }, VideoPlayerConfig.PROGRESS_INDICATOR_DURATION_MS)
        }
    }

    fun showBrightnessVal(value: Int) {
        binding.imageviewValue.setImageResource(
            if (value < VideoPlayerConfig.BRIGHTNESS_LOW_THRESHOLD) R.drawable.brightness_low
            else if (value < VideoPlayerConfig.BRIGHTNESS_HALF_THRESHOLD) R.drawable.brightness_half
            else R.drawable.brightness_high
        )
        internalShowUI(value)
    }

    fun showVolumeVal(value: Int) {
        binding.imageviewValue.setImageResource(
            if (value < VideoPlayerConfig.BRIGHTNESS_LOW_THRESHOLD) R.drawable.volume_down
            else if (value < VideoPlayerConfig.BRIGHTNESS_HALF_THRESHOLD) R.drawable.volume_half
            else R.drawable.volume_up
        )
        internalShowUI(value)
    }

    fun internalShowUI(value: Int) {
        binding.viewgroupValue.isVisible = true
        binding.value = value
        binding.viewgroupValue.postDelayed({
            binding.viewgroupValue.isVisible = false
        }, VideoPlayerConfig.VALUE_INDICATOR_DURATION_MS)
    }

    fun hideViewWithAnimation(view: View) {
        binding.root.postDelayed({
            view.isVisible = false
            val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_out_right)
            view.animation = animation
        }, VideoPlayerConfig.HIDE_ANIMATION_DELAY_MS)
    }

    private fun singleClick() {
        if (binding.listviewEpisode.isVisible || binding.sourceListRoot.isVisible ||
            binding.nodeListRoot.isVisible
        ) {
            hideViewWithAnimation(binding.listviewEpisode)
            hideViewWithAnimation(binding.sourceListRoot)
            hideViewWithAnimation(binding.nodeListRoot)
            viewModel.isListShowing.postValue(false)
            return
        }
        if (binding.player.isControllerFullyVisible) {
            hideControllerUI()
        } else {
            showControllerUI()
        }
    }

    private fun playState() {
        viewModel.isPlaying.postValue(viewModel.isPlaying.value?.not())
    }

    /**
     * Adjusts brightness based on gesture delta.
     */
    private fun adjustBrightness(deltaValue: Float) {
        viewModel.brighnessLiveData.postValue(deltaValue)
    }

    /**
     * Adjusts volume based on gesture delta.
     */
    private fun adjustVolume(deltaValue: Float) {
        viewModel.soundLiveData.postValue(deltaValue)
    }

    /**
     * Seeks playback position based on gesture delta.
     */
    private fun seekTo(deltaValue: Float) {
        viewModel.seekPositionLiveData.postValue(deltaValue.toLong())
    }

    private fun startPlay() {
        // Audio focus is managed by VideoPlaybackService
        mediaController?.let { p ->
            p.playWhenReady = true
            p.play()
        }
    }
    private fun pausePlay() {
        mediaController?.pause()
    }
    private fun stopPlay() {
        // Audio focus is managed by VideoPlaybackService
        mediaController?.stop()
    }

    private fun isListViewShowing() =
        binding.listviewEpisode.isVisible || binding.sourceListRoot.isVisible ||
            binding.nodeListRoot.isVisible
}

// Legacy VideoControllerListener interface removed - now using GestureActionListener
// Legacy NormalGenstureDetector class removed - now using GestureHandler