package me.sunzheng.mana.account.config

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.util.PatternsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import com.google.android.material.snackbar.Snackbar
import me.sunzheng.mana.R
import me.sunzheng.mana.databinding.FragmentHostdialogBinding
import me.sunzheng.mana.utils.PreferenceManager

/**
 * Created by Sun on 2017/7/6.
 */
class HostFragment : Fragment() {
    lateinit var sharedPreferences: SharedPreferences

    val binding: FragmentHostdialogBinding by lazy {
        FragmentHostdialogBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPreferences = requireActivity().getSharedPreferences(
            PreferenceManager.Global.STR_SP_NAME,
            Context.MODE_PRIVATE
        )
        NavigationUI.setupWithNavController(binding.toolbar, findNavController())
        binding.dialogHostTextinputedittext.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_GO -> {
                    binding.dialogHostButton.performClick()
                    true
                }

                else -> false
            }
        }
        binding.dialogHostTextinputedittext.setText(
            sharedPreferences.getString(
                PreferenceManager.Global.STR_KEY_HOST,
                ""
            ) ?: ""
        )
        binding.dialogHostButton.setOnClickListener { v ->
            var host = binding.dialogHostTextinputedittext.text.toString()
            if (TextUtils.isEmpty(
                    host
                ) || !PatternsCompat.WEB_URL.matcher(host)
                    .matches()
            ) {
                Snackbar.make(v, R.string.error_invalid_host, Snackbar.LENGTH_SHORT)
                    .show()
            } else {
                sharedPreferences.edit().putString(
                    PreferenceManager.Global.STR_KEY_HOST,
                    host
                ).commit()
                val inputManager =
                    requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//                if back from next fragment and click button  will crash
//                sovled seed https://stackoverflow.com/questions/19069448/null-pointer-error-with-hidesoftinputfromwindow
//                inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                inputManager.hideSoftInputFromWindow(
                    binding.dialogHostTextinputedittext.windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS
                )
                binding.dialogHostTextinputedittext.clearFocus()
                findNavController().navigate(R.id.action_destination_host_to_login)
            }
        }

    }

    companion object {
        private val TAG = HostFragment::class.java.simpleName
    }
}