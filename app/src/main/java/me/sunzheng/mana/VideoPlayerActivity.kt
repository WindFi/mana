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
import me.sunzheng.mana.core.net.Status
import me.sunzheng.mana.core.net.v2.database.EpisodeEntity
import me.sunzheng.mana.core.net.v2.database.VideoFileEntity
import me.sunzheng.mana.core.net.v2.database.WatchProgressEntity
import me.sunzheng.mana.core.net.v2.parseExtractorMediaSource
import me.sunzheng.mana.core.net.v2.parseMediaItem
import me.sunzheng.mana.core.net.v2.parseMediaItemWithMetadata
import me.sunzheng.mana.core.net.v2.showToast
import me.sunzheng.mana.core.net.v2.toUUID
import me.sunzheng.mana.databinding.FragmentVideoPlayerBinding
import me.sunzheng.mana.utils.PreferenceManager.Global
import me.sunzheng.mana.videoplayer.GestureActionListener
import me.sunzheng.mana.videoplayer.GestureHandler
import me.sunzheng.mana.videoplayer.MediaDescriptionAdapter2
import me.sunzheng.mana.videoplayer.MediaSessionManager
import me.sunzheng.mana.videoplayer.PlaybackStateManager
import me.sunzheng.mana.videoplayer.PlayerController
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
        if(Build.VERSION.SDK_INT >=26){
            if(resources.configuration.isScreenWideColorGamut){
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
        // Stop and release VideoPlaybackService when Activity is destroyed
        // This ensures the service is properly cleaned up when the video player Activity is closed
        val intent = Intent(this, me.sunzheng.mana.videoplayer.VideoPlaybackService::class.java)
        intent.action = me.sunzheng.mana.videoplayer.VideoPlaybackService.ACTION_STOP
        stopService(intent)
    }
}

@UnstableApi
@AndroidEntryPoint
class VideoPlayerFragment : Fragment() {
    
    @Inject
    lateinit var database: me.sunzheng.mana.core.net.v2.database.AppDatabase
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
        // Store current mediaDescription for use when setting MediaItem
        var currentMediaDescription: MediaDescriptionCompat? = null
        
        // Observe mediaDescription to prepare metadata before setting MediaItem
        viewModel.mediaDescritionLiveData.observe(viewLifecycleOwner) { mediaDescriptionCompat ->
            currentMediaDescription = mediaDescriptionCompat
        }
        
        viewModel.videoFileLiveData.observe(viewLifecycleOwner) { videoFile ->
            mediaController?.let { p ->
                dataSourceFactory?.let { factory ->
                    // Get the video URI first
                    val videoUri = videoFile.url?.toUri()?.let {
                        val url = if (PatternsCompat.WEB_URL.matcher(it.toString()).find()) {
                            it.toString()
                        } else {
                            "${viewModel.host}${it}"
                        }
                        url.toUri()
                    }
                    
                    videoUri?.let { uri ->
                        // Get current mediaDescription for metadata
                        val mediaDesc = currentMediaDescription
                        
                        // Set MediaItem with title/artist first (cover will be added asynchronously)
                        val initialItem = if (mediaDesc != null) {
                            uri.parseMediaItemWithMetadata(
                                title = mediaDesc.title?.toString(),
                                artist = mediaDesc.subtitle?.toString()
                            )
                        } else {
                            uri.parseMediaItem()
                        }
                        p.setMediaItem(initialItem)
                        Log.d("VideoPlayer", "Set initial MediaItem, title: ${mediaDesc?.title}, artist: ${mediaDesc?.subtitle}")
                        
                        // Prepare player to load media and trigger auto-play
                        p.prepare()
                        Log.d("VideoPlayer", "Player prepare() called, will trigger auto-play when ready")
                        
                        // Load cover image asynchronously and update MediaItem
                        if (mediaDesc != null) {
                            val episodeEntity = mediaDesc.extras?.getParcelable<EpisodeEntity>("raw")
                            episodeEntity?.bangumiId?.let { bangumiId ->
                                lifecycleScope.launch {
                                    try {
                                        val bangumiEntity = database.bangumiDao().queryById(bangumiId)
                                        Log.d("VideoPlayer", "Queried BangumiEntity: ${bangumiEntity?.nameCn}, coverImage: ${bangumiEntity?.coverImage?.url}, cover: ${bangumiEntity?.cover}")
                                        val coverUrl = bangumiEntity?.coverImage?.url ?: bangumiEntity?.cover
                                        
                                        if (coverUrl != null) {
                                            Log.d("VideoPlayer", "Found cover URL: $coverUrl")
                                            val fullCoverUrl = me.sunzheng.mana.utils.HostUtil.makeUp(
                                                viewModel.host, 
                                                coverUrl
                                            )
                                            
                                            // Update MediaItem with cover metadata
                                            val updatedItem = uri.parseMediaItemWithMetadata(
                                                title = mediaDesc.title?.toString(),
                                                artist = mediaDesc.subtitle?.toString(),
                                                artworkUri = fullCoverUrl.toUri()
                                            )
                                            
                                            // Replace the current media item with updated metadata
                                            val currentIndex = p.currentMediaItemIndex
                                            if (currentIndex >= 0 && currentIndex < p.mediaItemCount) {
                                                p.replaceMediaItem(currentIndex, updatedItem)
                                                Log.d("VideoPlayer", "Updated MediaItem with cover: $fullCoverUrl")
                                            } else {
                                                // If no current item, set it directly
                                                p.setMediaItem(updatedItem)
                                                Log.d("VideoPlayer", "Set MediaItem with cover: $fullCoverUrl")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.w("VideoPlayer", "Failed to load Bangumi cover for notification", e)
                                        e.printStackTrace()
                                    }
                                }
                            }
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
                            val episodeEntity = mediaDescriptionCompat.extras?.getParcelable<EpisodeEntity>("raw")
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
        binding.player.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener{
            binding.appbarlayout.isVisible = it == View.VISIBLE
        })
        binding.player.controllerHideOnTouch = true
        binding.player.controllerAutoShow = true
        binding.player.controllerShowTimeoutMs = VideoPlayerConfig.CONTROLLER_AUTO_SHOW_TIMEOUT_MS.toInt()
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
            val intent = Intent(requireContext(), me.sunzheng.mana.videoplayer.VideoPlaybackService::class.java)
            intent.action = me.sunzheng.mana.videoplayer.VideoPlaybackService.ACTION_STOP
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
                        .setUserAgent(Util.getUserAgent(requireContext(), requireContext().packageName))
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
                playbackStateManager = PlaybackStateManager(controller, viewModel, viewLifecycleOwner)
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
                Log.e("VideoPlayerFragment", "Failed to connect to playback service: ${error.message}", error)
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
        
        playerListener = object : Player.Listener {
            var isInitialized = false
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                super.onPlayWhenReadyChanged(playWhenReady, reason)
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                Log.d("VideoPlayerFragment","playState:${playbackState}")
                when(playbackState){
                    Player.STATE_READY ->{
                        isInitialized=true
                        
                        // Auto-play on first ready state (only once per page entry)
                        if (!hasAutoPlayed) {
                            mediaController?.let { p ->
                                // Always set playWhenReady to true for auto-play
                                // This ensures playback starts automatically when player is ready
                                p.playWhenReady = true
                                Log.d("VideoPlayerFragment", "Auto-play triggered: playWhenReady = true, isPlaying = ${p.isPlaying}")
                            }
                            hasAutoPlayed = true
                        }
                    }
                    Player.STATE_ENDED -> {
                        if(!isInitialized){
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
            override fun onIsLoadingChanged(isLoading: Boolean) {
                // Only show CircleProgressBar when:
                // 1. Network is loading (isLoading == true)
                // 2. AND current position >= buffered position (cache exhausted)
                // This minimizes interruption to user viewing experience
                if (isLoading) {
                    val currentPosition = mediaController?.currentPosition ?: 0L
                    val bufferedPosition = mediaController?.bufferedPosition ?: 0L
                    // Show only when cache is exhausted (current position caught up with buffer)
                    binding.progressbar.isVisible = currentPosition >= bufferedPosition
                    Log.d("VideoPlayerFragment", "Loading: current=$currentPosition, buffered=$bufferedPosition, show=${currentPosition >= bufferedPosition}")
                } else {
                    binding.progressbar.isVisible = false
                }
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
                    stopPlay()
                }
            }
        }
        // Player listener will be set up in setupPlayerListeners() after service connection
        // Player is managed by Service, don't set playWhenReady here
        binding.player.setOnTouchListener { _, event -> gestureHandler?.gestureDetector?.onTouchEvent(event) ?: false }
        
        // Observe brightness changes and update UI
        viewModel.brighnessLiveData.observe(viewLifecycleOwner) {
            val per: Float = it / VideoPlayerConfig.BRIGHTNESS_ADJUSTMENT_FACTOR * VideoPlayerConfig.MAX_BRIGHTNESS
            var currentBrightness = requireActivity().window.attributes.screenBrightness * VideoPlayerConfig.MAX_BRIGHTNESS
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
        if (binding.listviewEpisode.isVisible || binding.sourceListRoot.isVisible) {
            hideViewWithAnimation(binding.listviewEpisode)
            hideViewWithAnimation(binding.sourceListRoot)
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

    private fun stopPlay() {
        // Audio focus is managed by VideoPlaybackService
        mediaController?.stop()
    }

    private fun isListViewShowing() =
        binding.listviewEpisode.isVisible || binding.sourceListRoot.isVisible
}

// Legacy VideoControllerListener interface removed - now using GestureActionListener
// Legacy NormalGenstureDetector class removed - now using GestureHandler