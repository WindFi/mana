package me.sunzheng.mana.account;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import me.sunzheng.mana.R;
import me.sunzheng.mana.utils.PreferenceManager;

/**
 * Created by Sun on 2017/7/6.
 */

public class HostDialogFragment extends DialogFragment {
    private final static String TAG = HostDialogFragment.class.getSimpleName();
    SharedPreferences sharedPreferences;
    TextInputEditText mTextInputEidtText;
    OnButtonClickListener listener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Please type the host");
        // Edited: Overriding onCreateView is not necessary in your case
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.fragment_hostdialog, null);
        builder.setView(view);
        mTextInputEidtText = (TextInputEditText) view.findViewById(R.id.dialog_host_textinputedittext);
        sharedPreferences = getActivity().getSharedPreferences(PreferenceManager.Global.STR_SP_NAME, Context.MODE_PRIVATE);
        if (!TextUtils.isEmpty(sharedPreferences.getString(PreferenceManager.Global.STR_KEY_HOST, "")))
            mTextInputEidtText.setText(sharedPreferences.getString(PreferenceManager.Global.STR_KEY_HOST, ""));
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String key = mTextInputEidtText.getText().toString();
                // on success
                Log.i(TAG, key);
                sharedPreferences.edit().putString(PreferenceManager.Global.STR_KEY_HOST, key).commit();
                if (listener != null)
                    listener.onSave();
            }
        });
        builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (listener != null)
                    listener.onCancel();
            }
        });

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnButtonClickListener)
            listener = (OnButtonClickListener) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (listener != null)
            listener = null;
    }

    public interface OnButtonClickListener {
        void onSave();

        void onCancel();
    }
}
