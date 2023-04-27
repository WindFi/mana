package me.sunzheng.mana.account.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.util.PatternsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;

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
        mTextInputEidtText = view.findViewById(R.id.dialog_host_textinputedittext);
        mButton = view.findViewById(R.id.dialog_host_button);
        mTextInputEidtText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    mButton.performClick();
                }
                return false;
            }
        });
        if (!TextUtils.isEmpty(sharedPreferences.getString(PreferenceManager.Global.STR_KEY_HOST, "")))
            mTextInputEidtText.setText(sharedPreferences.getString(PreferenceManager.Global.STR_KEY_HOST, ""));
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mTextInputEidtText.getText().toString()) || !PatternsCompat.WEB_URL.matcher(mTextInputEidtText.getText().toString()).matches()) {
                    Toast.makeText(getActivity(), getString(R.string.error_invalid_host), Toast.LENGTH_SHORT).show();
                    return;
                }
                sharedPreferences.edit().putString(PreferenceManager.Global.STR_KEY_HOST, mTextInputEidtText.getText().toString()).commit();
                InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
//                if back from next fragment and click button  will crash
//                sovled seed https://stackoverflow.com/questions/19069448/null-pointer-error-with-hidesoftinputfromwindow
//                inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                inputManager.hideSoftInputFromWindow(mButton.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                mTextInputEidtText.clearFocus();
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
