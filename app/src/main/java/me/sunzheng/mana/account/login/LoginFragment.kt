package me.sunzheng.mana.account.login

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import me.sunzheng.mana.MainActivity
import me.sunzheng.mana.account.AccountViewModel
import me.sunzheng.mana.core.net.Status
import me.sunzheng.mana.core.net.v2.showToast
import me.sunzheng.mana.databinding.FragmentLoginBinding
import me.sunzheng.mana.utils.PreferenceManager

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [LoginFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [LoginFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LoginFragment : Fragment() {
    lateinit var sharedPreferences: SharedPreferences
    val binding: FragmentLoginBinding by lazy {
        FragmentLoginBinding.inflate(layoutInflater)
    }
    val viewModel by activityViewModels<AccountViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = requireActivity().getSharedPreferences(
            PreferenceManager.Global.STR_SP_NAME,
            Context.MODE_PRIVATE
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding.button1.setOnClickListener {
            try {
                val inputManager =
                    requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputManager.hideSoftInputFromWindow(
                    binding.loginPasswordTextinputedittext.windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS
                )
                viewModel.siginIn(
                    binding.loginUsernameTextinputedittext.text.toString(),
                    binding.loginPasswordTextinputedittext.text.toString(),
                    binding.checkbox.isChecked
                ).observe(viewLifecycleOwner) {
                    when (it.code) {
                        Status.SUCCESS -> {
                            onLoginSuccess()
                        }

                        Status.ERROR -> {
                            it.message?.run {
                                showToast(this)
                            }
                        }

                        Status.LOADING -> {

                        }
                    }
                }
            } catch (e: IllegalArgumentException) {
                showToast(e.localizedMessage)
            }
        }
        binding.checkbox.isChecked = sharedPreferences.getBoolean(
            PreferenceManager.Global.BOOL_IS_REMEMBERD,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val isRememberd =
            sharedPreferences.getBoolean(PreferenceManager.Global.BOOL_IS_REMEMBERD, false)
        if (isRememberd) onLoginSuccess()
    }

    fun onLoginSuccess() {
        sharedPreferences.edit()
            .putBoolean(
                PreferenceManager.Global.BOOL_IS_REMEMBERD,
                binding.checkbox.isChecked
            )
            .putString(
                PreferenceManager.Global.STR_USERNAME,
                binding.loginUsernameTextinputedittext.text.toString()
            )
            .putString(
                PreferenceManager.Global.STR_PASSWORD,
                binding.loginPasswordTextinputedittext.text.toString()
            )
            .commit()
//        findNavController().navigate(R.id.action_destination_login)
        startActivity(Intent(requireContext(), MainActivity::class.java))
        requireActivity().finish()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment LoginFragment.
         */
        @JvmStatic
        fun newInstance(): LoginFragment {
            return LoginFragment()
        }
    }
}