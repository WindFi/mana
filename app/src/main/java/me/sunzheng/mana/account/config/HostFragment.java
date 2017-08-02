package me.sunzheng.mana.account.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import me.sunzheng.mana.R;
import me.sunzheng.mana.account.AccountContrant;
import me.sunzheng.mana.utils.PreferenceManager;

/**
 * Created by Sun on 2017/7/6.
 */

public class HostFragment extends Fragment implements AccountContrant.Start.View {
    private final static String TAG = HostFragment.class.getSimpleName();
    SharedPreferences sharedPreferences;
    TextInputEditText mTextInputEidtText;
    OnButtonClickListener listener;
    AppCompatButton mButton;
    AccountContrant.Start.Presenter presenter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hostdialog, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sharedPreferences = getActivity().getSharedPreferences(PreferenceManager.Global.STR_SP_NAME, Context.MODE_PRIVATE);
        mTextInputEidtText = (TextInputEditText) view.findViewById(R.id.dialog_host_textinputedittext);
        mButton = (AppCompatButton) view.findViewById(R.id.dialog_host_button);
        if (!TextUtils.isEmpty(sharedPreferences.getString(PreferenceManager.Global.STR_KEY_HOST, "")))
            mTextInputEidtText.setText(sharedPreferences.getString(PreferenceManager.Global.STR_KEY_HOST, ""));
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharedPreferences.edit().putString(PreferenceManager.Global.STR_KEY_HOST, mTextInputEidtText.getText().toString()).commit();
                if (listener != null)
                    listener.onSave();
            }
        });
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

    @Override
    public void setPresenter(AccountContrant.Start.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void onGetStart() {
        listener.onSave();
    }

    public interface OnButtonClickListener {
        void onSave();
    }
}
