package me.sunzheng.mana

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import me.sunzheng.mana.utils.PreferenceManager

class AuthenrizeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var sharedPreferences = requireActivity().getSharedPreferences(
            PreferenceManager.Global.STR_SP_NAME,
            AppCompatActivity.MODE_PRIVATE
        )
        if (sharedPreferences.getBoolean(
                PreferenceManager.Global.BOOL_IS_REMEMBERD,
                false
            )
        ) {
            findNavController().navigate(R.id.action_destination_authenrize_to_main)
        } else {
            findNavController().navigate(R.id.action_destination_authenrize_to_host)
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }
}