package me.sunzheng.mana.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import me.sunzheng.mana.databinding.FragmentEmptyBinding
import me.sunzheng.mana.databinding.FragmentLoadingBinding

class EmptyFragment : Fragment() {
    lateinit var binding: FragmentEmptyBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEmptyBinding.inflate(inflater)
        return binding.root
    }
}

class LoadingFragment : Fragment() {
    lateinit var binding: FragmentLoadingBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoadingBinding.inflate(inflater)
        return binding.root
    }
}