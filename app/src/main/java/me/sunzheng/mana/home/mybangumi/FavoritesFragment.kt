package me.sunzheng.mana.home.mybangumi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import me.sunzheng.mana.BangumiDetailsActivity
import me.sunzheng.mana.R
import me.sunzheng.mana.core.net.ApiResponse
import me.sunzheng.mana.core.net.v2.ApiService
import me.sunzheng.mana.core.net.v2.NetworkBoundResource
import me.sunzheng.mana.core.net.v2.database.BangumiDao
import me.sunzheng.mana.core.net.v2.database.BangumiEntity
import me.sunzheng.mana.core.net.v2.database.FavirouteDao
import me.sunzheng.mana.core.net.v2.database.FavriouteEntity
import me.sunzheng.mana.databinding.FragmentMyfavoritesBinding
import me.sunzheng.mana.home.mybangumi.wrapper.FavoriteWrapper
import me.sunzheng.mana.home.onair.OnAirItemRecyclerViewAdapter
import java.util.*
import javax.inject.Inject
import javax.inject.Named

/**
 * Created by Sun on 2017/6/22.
 */
@AndroidEntryPoint
class FavoritesFragment @Inject constructor() : Fragment() {
    companion object {
        @JvmStatic
        val ARGS_DATA_PARCEL_ARRAY = "${FavoritesFragment::class.java.simpleName}_data"

        @JvmStatic
        fun newInstance(list: List<BangumiEntity>) = FavoritesFragment().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(
                    ARGS_DATA_PARCEL_ARRAY,
                    ArrayList<BangumiEntity>(list)
                )
            }
        }
    }

    lateinit var binding: FragmentMyfavoritesBinding
    lateinit var args: Bundle
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMyfavoritesBinding.inflate(inflater)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        args = savedInstanceState?.getBundle("args") ?: requireArguments()
        args.getParcelableArrayList<BangumiEntity>(ARGS_DATA_PARCEL_ARRAY)?.let {
            OnAirItemRecyclerViewAdapter(it) { v, _, _, m ->
                if (m is BangumiEntity)
                    BangumiDetailsActivity.newInstance(
                        requireActivity(),
                        m,
                        v.findViewById(R.id.item_album)
                    )
            }
        }?.run {
            binding.recyclerView.adapter = this
            binding.recyclerView.itemAnimator = DefaultItemAnimator()
            binding.recyclerView.addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                )
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBundle("args", args)
        super.onSaveInstanceState(outState)
    }
}

@HiltViewModel
class MyFavoriteViewModel @Inject constructor() : ViewModel() {
    @Inject
    lateinit var favriouteDao: FavirouteDao

    @Inject
    lateinit var bangumiDao: BangumiDao

    @Inject
    lateinit var apiService: ApiService

    @Named("userName")
    @Inject
    lateinit var userName: String
    val repository: FavoritesRepository by lazy {
        FavoritesRepository().also {
            it.favriouteDao = favriouteDao
            it.bangumiDao = bangumiDao
            it.apiService = apiService
            it.userName = userName
        }
    }

    fun filter(status: Int = 0, page: Int = 0) = repository.query(status, page)
}

class FavoritesRepository {
    lateinit var favriouteDao: FavirouteDao
    lateinit var bangumiDao: BangumiDao
    lateinit var apiService: ApiService
    lateinit var userName: String
    fun query(status: Int, page: Int) =
        object : NetworkBoundResource<List<BangumiEntity>, FavoriteWrapper>() {
            override fun saveCallResult(item: FavoriteWrapper) {
                item.data?.forEach {
                    bangumiDao.insert(Gson().fromJson(Gson().toJson(it), BangumiEntity::class.java))
                }
                item.data.map {
                    FavriouteEntity(
                        UUID.fromString(it.id),
                        status = it.favoriteStatus,
                        userName = userName,
                        it.unwatched_count
                    )
                }.forEach {
                    favriouteDao.insert(it)
                }
            }

            override fun shouldFetch(data: List<BangumiEntity>?): Boolean = true

            override fun loadFromDb(): LiveData<List<BangumiEntity>> =
                favriouteDao.queryBangumiList(status, userName)

            override fun createCall(): LiveData<ApiResponse<FavoriteWrapper>> =
                apiService.listMyBangumi(status)
        }.asLiveData()
}