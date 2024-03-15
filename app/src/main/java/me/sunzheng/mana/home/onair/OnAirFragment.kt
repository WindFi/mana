package me.sunzheng.mana.home.onair

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import me.sunzheng.mana.BangumiDetailsActivity
import me.sunzheng.mana.R
import me.sunzheng.mana.core.net.Resource
import me.sunzheng.mana.core.net.Status
import me.sunzheng.mana.core.net.v2.database.BangumiEntity
import me.sunzheng.mana.core.net.v2.showToast
import me.sunzheng.mana.databinding.FragmentItemListBinding
import me.sunzheng.mana.home.EmptyFragment
import me.sunzheng.mana.home.main.MainViewModel

/**
 * A fragment representing a list of Items.
 */
@AndroidEntryPoint
class OnAirFragment : Fragment() {
    companion object {
        const val INT_TYPE_ANIMATION = 2
        const val INT_TYPE_DRAMA = 6
        const val INT_ARGS_TYPE = "air_type"
        fun newInstance(type: Int) = Bundle().let {
            it.putInt(INT_ARGS_TYPE, type)
            OnAirFragment().apply {
                arguments = it
            }
        }
    }

    lateinit var binder: FragmentItemListBinding
    val viewModel by viewModels<MainViewModel>()
    var type = 0
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binder = FragmentItemListBinding.inflate(inflater)
        binder.lifecycleOwner = this
        return binder.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binder.swiperefreshlayout.setColorSchemeResources(R.color.colorPrimary)
        var args = savedInstanceState?.getBundle("args") ?: requireArguments()
        type = args.getInt(INT_ARGS_TYPE, 0)
        binder.swiperefreshlayout.setOnRefreshListener {
            fetch()
        }
        fetch()
    }

    fun fetch() {
        binder.swiperefreshlayout.isRefreshing = true
        viewModel.queryAir(type).observe(viewLifecycleOwner, observable)
    }

    fun remove() {
        viewModel.queryAir(type).removeObserver(observable)
    }
    var emptyFragment=EmptyFragment()
    var observable =
        Observer<Resource<List<BangumiEntity>>> { it ->
            binder.swiperefreshlayout.isRefreshing = it.code == Status.LOADING
            if (it.code != Status.LOADING) {
                remove()
            }
            when (it.code) {
                Status.LOADING -> {
                    childFragmentManager.beginTransaction().replace(binder.content.id,emptyFragment).commitNow()
                    it.data?.takeIf { that -> that.isNotEmpty() }?.run {
                        if (binder.recyclerview.adapter == null)
                            binder.recyclerview.adapter = OnAirItemRecyclerViewAdapter(
                                this
                            ) { v, _, _, m ->
                                when (m) {
                                    is BangumiEntity -> {
                                        BangumiDetailsActivity.newInstance(
                                            requireActivity(),
                                            m,
                                            v.findViewById(R.id.item_album)
                                        )
                                    }
                                }

                            }

                        if (binder.recyclerview.itemDecorationCount < 1) {
                            binder.recyclerview.addItemDecoration(
                                DividerItemDecoration(
                                    requireActivity(),
                                    DividerItemDecoration.VERTICAL
                                )
                            )
                        }
                    }
                }
                Status.ERROR -> {
                    showToast(it.message ?: "")
                }
                Status.SUCCESS -> {
//                if(binder.recyclerview.adapter==null)
                    it.data?.takeIf { that -> that.isNotEmpty() }?.run {
                        childFragmentManager.beginTransaction().remove(emptyFragment).commit()
                        binder.recyclerview.adapter = OnAirItemRecyclerViewAdapter(
                            this
                        ) { v, _, _, m ->
                            when (m) {
                                is BangumiEntity -> {
                                    BangumiDetailsActivity.newInstance(
                                        requireActivity(),
                                        m,
                                        v.findViewById(R.id.item_album)
                                    )
                                }
                            }
                        }
                    }
                    if (binder.recyclerview.itemDecorationCount < 1) {
                        binder.recyclerview.addItemDecoration(
                            DividerItemDecoration(
                                requireActivity(),
                                DividerItemDecoration.VERTICAL
                            )
                        )
                    }
                }
            }
        }
}