package me.sunzheng.mana.home.mybangumi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import me.sunzheng.mana.BangumiDetailsActivity
import me.sunzheng.mana.R
import me.sunzheng.mana.core.net.Status
import me.sunzheng.mana.core.net.v2.database.BangumiEntity
import me.sunzheng.mana.core.net.v2.showToast
import me.sunzheng.mana.databinding.FragmentFavoriteCompatBinding
import me.sunzheng.mana.home.EmptyFragment
import me.sunzheng.mana.home.onair.OnAirItemRecyclerViewAdapter
import javax.inject.Inject

@AndroidEntryPoint
class FavriouteFragmentCompat @Inject constructor() : Fragment() {
    companion object {
        @JvmStatic
        val ARGS_STATUS_INT = "${FavriouteFragmentCompat::class.java.simpleName}_status"

        @JvmStatic
        fun newInstance(status: Int) = FavriouteFragmentCompat().apply {
            arguments = Bundle().apply {
                putInt(ARGS_STATUS_INT, status)
            }
        }
    }

    lateinit var binding: FragmentFavoriteCompatBinding
    val viewModel: MyFavoriteViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavoriteCompatBinding.inflate(inflater)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireArguments().run {
            var status = getInt(ARGS_STATUS_INT, -1)
            viewModel.filter(status).observe(viewLifecycleOwner) { it ->
                when (it.code) {
                    Status.SUCCESS -> {
                        var f = if (it.data.isNullOrEmpty()) {
                            EmptyFragment()
                        } else {
//                            FavoritesFragment.newInstance(it.data.map { it.entity })
                        }
                        binding.textview.isVisible = it.data.isNullOrEmpty()

                        it.data?.map { it.entity }?.run {
                            if (binding.adapter == null) binding.adapter =
                                OnAirItemRecyclerViewAdapter(this) { v, _, _, m ->
                                    if (m is BangumiEntity)
                                        BangumiDetailsActivity.newInstance(
                                            requireActivity(),
                                            m,
                                            v.findViewById(R.id.item_album)
                                        )
                                } else {
                                var adapter = binding.adapter
                                adapter?.mValues?.addAll(this)
                                adapter?.notifyDataSetChanged()
                            }
                        }

                    }

                    Status.LOADING -> {
                        it.data?.map { it.entity }?.run {
                            binding.adapter = OnAirItemRecyclerViewAdapter(this) { v, _, _, m ->
                                if (m is BangumiEntity)
                                    BangumiDetailsActivity.newInstance(
                                        requireActivity(),
                                        m,
                                        v.findViewById(R.id.item_album)
                                    )
                            }
                        }
                    }

                    Status.ERROR -> {
                        showToast(it.message ?: "")
                    }
                }
            }
        }
    }
}
