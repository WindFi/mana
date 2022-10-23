package me.sunzheng.mana.home.mybangumi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import me.sunzheng.mana.R
import me.sunzheng.mana.core.net.Status
import me.sunzheng.mana.core.net.v2.showToast
import me.sunzheng.mana.databinding.FragmentFavoriteCompatBinding
import me.sunzheng.mana.home.EmptyFragment
import me.sunzheng.mana.home.LoadingFragment
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
    val viewModel: MyFavoriteViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFavoriteCompatBinding.inflate(inflater)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        childFragmentManager.beginTransaction().replace(R.id.replace_layout, LoadingFragment())
            .commit()
        requireArguments().run {
            var status = getInt(ARGS_STATUS_INT, -1)
            viewModel.filter(status).observe(viewLifecycleOwner) { it ->
                when (it.code) {
                    Status.SUCCESS -> {
                        var f = if (it.data.isNullOrEmpty()) {
                            EmptyFragment()
                        } else {
                            FavoritesFragment.newInstance(it.data.map { it.entity })
                        }
                        childFragmentManager.beginTransaction().replace(R.id.replace_layout, f)
                            .commit()
                    }
                    Status.LOADING -> {
                        var f = if (it.data.isNullOrEmpty()) {
                            EmptyFragment()
                        } else {
                            FavoritesFragment.newInstance(it.data.map { it.entity })
                        }
                        childFragmentManager.beginTransaction().replace(R.id.replace_layout, f)
                            .commit()
                    }
                    Status.ERROR -> {
                        showToast(it.message ?: "")
                    }
                }
            }
        }
    }
}
