package me.sunzheng.mana.home.bangumi

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.appbar.CollapsingToolbarLayout
import dagger.hilt.android.AndroidEntryPoint
import me.sunzheng.mana.BangumiDetailsActivity
import me.sunzheng.mana.FavoriteCompact
import me.sunzheng.mana.R
import me.sunzheng.mana.VideoPlayerActivity
import me.sunzheng.mana.core.net.Status
import me.sunzheng.mana.core.net.v2.database.BangumiEntity
import me.sunzheng.mana.core.net.v2.parseMediaDescription
import me.sunzheng.mana.core.net.v2.showToast
import me.sunzheng.mana.databinding.FragmentBangumidetailsBinding
import me.sunzheng.mana.utils.ArrarysResourceUtils
import me.sunzheng.mana.utils.PreferenceManager

@AndroidEntryPoint
class BangumiDetailsFragment : Fragment() {
    companion object {
        @JvmStatic
        val ARGS_DATA_PARCELABLE = "${BangumiDetailsFragment::class.java.simpleName}_data"
        fun newInstance(data: BangumiEntity?) = BangumiDetailsFragment().apply {
            arguments = Bundle().apply { putParcelable(ARGS_DATA_PARCELABLE, data) }
        }
    }
    var mBannerImageView: ImageView? = null
    var mHeaderCollapsingToolbarLayout: CollapsingToolbarLayout? = null
    var mToolbar: Toolbar? = null
    var mCNTitleTextView: AppCompatTextView? = null
    var mRecyclerView: RecyclerView? = null
    var sharedPreferences: SharedPreferences? = null

    lateinit var binding: FragmentBangumidetailsBinding

    val viewModel by viewModels<BangumiDetailsViewModel>()
    lateinit var args: Bundle
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.fragment_bangumidetails,
            container,
            false
        )
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = requireActivity().getSharedPreferences(
            PreferenceManager.Global.STR_SP_NAME,
            Context.MODE_PRIVATE
        )
        args = savedInstanceState?.getBundle("args") ?: requireArguments()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(view)
        initContent(view)
        args.getParcelable<BangumiEntity>(ARGS_DATA_PARCELABLE)?.run {
            var name = sharedPreferences
                ?.getString(PreferenceManager.Global.STR_USERNAME, null)!!
            viewModel.queryBangumiAndFavorite(id, userName = name)
                .observe(viewLifecycleOwner) { dataView ->
                    binding.bangumiModel = dataView
                    binding.bangumiModel?.entity?.run {
                        setOriginName(this.airWeekday.toInt(), this.airDate!!)
                        setFavouriteStatus(dataView!!.state.status)
                        // TODO: 2021/12/18 test it
                    }
                }
            viewModel.fetchEpisodeList(id, name).observe(viewLifecycleOwner) { it ->
                when (it.code) {
                    Status.SUCCESS -> {
//                        it.data?.forEach {
//                            Log.i("bbk","${it.episodeEntity.id?.toString()?:"episodeId:null"}/${it.watchProgress?.id?.toString()?:"watchprogress:null"}")
//                        }
                        binding.episodeModels = it.data
                        var adapter = binding.recyclerView.adapter as EpisodeAdapter
                        adapter.submitList(it.data?.sortedBy { episode -> episode.episodeEntity.bgmEpsId }
                            ?.reversed())
                    }
                    Status.ERROR -> {
                        it.message?.run {
                            showToast(this)
                        }
                    }
                    Status.LOADING -> {
                        binding.episodeModels = it.data
                        binding.recyclerView.adapter =
                            EpisodeAdapter { _, position, id, model ->
                                Handler(Looper.getMainLooper())
                                    .postDelayed({
                                        VideoPlayerActivity.newInstance(
                                            requireContext(),
                                            this.id,
                                            binding.episodeModels!!.size - (position + 1),
                                            binding.episodeModels!!.sortedBy { it.episodeEntity.episodeNo }
                                                .reversed().map {
                                                it.episodeEntity.parseMediaDescription("")
                                            }
                                        ).run {
                                            requireContext().startActivity(this)
                                        }
                                    }, 300)
                            }
                        var adapter = binding.recyclerView.adapter as EpisodeAdapter
                        adapter.submitList(it.data)
                    }
                }
            }
        }
    }

    private val supportActionBar: ActionBar?
        private get() = (requireActivity() as AppCompatActivity?)!!.supportActionBar

    private fun setSupportActionBar(toolbar: Toolbar?) {
        (activity as AppCompatActivity?)!!.setSupportActionBar(toolbar)
    }

    private fun initToolbar(view: View) {
        mToolbar = view.findViewById(R.id.toolbar)
        mBannerImageView = view.findViewById(R.id.banner_imageview)
        mHeaderCollapsingToolbarLayout = view.findViewById(R.id.header_collaspingtoolbarlayout)
        Glide.with(requireActivity())
            .load(requireArguments().getString(BangumiDetailsActivity.ARGS_ABLUM_URL_STR))
            .into(binding.bannerImageview)
        setSupportActionBar(mToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title =
            arguments?.getString(BangumiDetailsActivity.ARGS_TITLE_STR, "") ?: ""
    }

    private fun initContent(view: View) {
        mRecyclerView = view.findViewById(R.id.recycler_view)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.bangumidetailsAblumImageview.transitionName =
                BangumiDetailsActivity.PAIR_IMAGE_STR
        }
        Glide.with(requireContext())
            .load(requireArguments().getString(BangumiDetailsActivity.ARGS_ABLUM_URL_STR))
            .into(binding.bangumidetailsAblumImageview)
        requireArguments().getString(BangumiDetailsActivity.ARGS_TITLE_STR)
            ?.takeIf { !TextUtils.isEmpty(it) }?.run {
                setName(this)
            }
        binding.bangumidetailsFaviortestatusTextview.setOnClickListener {
            val popupMenu = PopupMenu(
                requireContext(), binding.bangumidetailsFaviortestatusTextview
            )
            popupMenu.inflate(R.menu.favorite)
            popupMenu.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                if (item.title == binding.bangumidetailsFaviortestatusTextview.text) return@OnMenuItemClickListener false

                var status = 0
                if (item.itemId == R.id.pop_uncollection) {
                    status = 0
                } else if (item.itemId == R.id.pop_wish) {
                    status = 1
                } else if (item.itemId == R.id.pop_watched) {
                    status = 2
                } else if (item.itemId == R.id.pop_watching) {
                    status = 3
                } else if (item.itemId == R.id.pop_pause) {
                    status = 4
                } else if (item.itemId == R.id.pop_abanoned) {
                    status = 5
                }
                viewModel.updateBangumiFavoriteState(
                    binding.bangumiModel!!.entity.id,
                    status
                ).observe(viewLifecycleOwner) {
                    when (it.code) {
                        Status.SUCCESS -> {
                            binding.bangumiModel?.state?.status = status
                            setFavouriteStatus(status)
                        }
                        Status.LOADING -> {
                            setFavouriteStatus(status)
                        }
                        Status.ERROR -> {
                            setFavouriteStatus(binding.bangumiModel?.state?.status ?: 0)
                        }
                    }
                }
                true
            })
            popupMenu.show()
        }
        binding.recyclerView.isNestedScrollingEnabled = false
        //add padding buttom
        // TODO: 2021/12/3 ????底边距
//        binding.bangumidetailsHeaderConstraint.setPadding(
//            0,
//            0,
//            0,
//            getNavigationBarHeight(resources.configuration.orientation)
//        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.bangumidetailsHeaderConstraint.setPadding(
            0,
            0,
            0,
            getNavigationBarHeight(newConfig.orientation)
        )
    }

    fun setOriginName(day: Int, etc: CharSequence) {
//        mBangumiDetails.getAirWeekday(), mBangumiDetails.getAirDate()
        val dayInWeek = ArrarysResourceUtils.dayInWeek(activity, day)
        val color = ContextCompat.getColor(requireActivity(), R.color.colorPrimary)
        val resultString =
            requireActivity().getString(R.string.formatter_day_airdate, etc, dayInWeek)
        //        SpannableStringBuilder spannableStringBuilder=new SpannableStringBuilder(resultString);
//        spannableStringBuilder.setSpan(new ForegroundColorSpan(color),resultString.indexOf(dayInWeek),resultString.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        binding.bangumidetailsOriginnameTextview.text = resultString
    }

    fun setFavouriteStatus(status: Int) {
        FavoriteCompact.setFavorite(status, binding.bangumidetailsFaviortestatusTextview)
    }

    fun setName(name: CharSequence) {
        if (mCNTitleTextView == null) return
        mCNTitleTextView!!.text = name
    }



    private fun getNavigationBarHeight(orientation: Int): Int {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) return 0
        val resources = requireContext().resources
        val id = resources.getIdentifier(
            if (orientation == Configuration.ORIENTATION_PORTRAIT) "navigation_bar_height" else "navigation_bar_height_landscape",
            "dimen",
            "android"
        )
        return if (id > 0) {
            resources.getDimensionPixelSize(id)
        } else 0
    }
}