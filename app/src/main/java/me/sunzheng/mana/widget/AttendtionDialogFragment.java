package me.sunzheng.mana.widget;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import me.sunzheng.mana.R;

public class AttendtionDialogFragment extends DialogFragment {
    public final static String ARGS_MESSAGE_STR = "message";
    public final static String ARGS_TITLE_POSITIVE_STR = "positive";
    public final static String ARGS_TITLE_NEGATIVE_STR = "negative";


    @Nullable
    private PositiveClickListener positiveClickListener;
    @Nullable
    private NegativeClickListener negativeClickListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null)
            savedInstanceState = getArguments();
        String positiveTitle, negativeTitle, message;
        positiveTitle = savedInstanceState == null || TextUtils.isEmpty(savedInstanceState.getString(ARGS_TITLE_POSITIVE_STR)) ? getString(android.R.string.ok) : savedInstanceState.getString(ARGS_TITLE_POSITIVE_STR);
        negativeTitle = savedInstanceState == null || TextUtils.isEmpty(savedInstanceState.getString(ARGS_TITLE_NEGATIVE_STR)) ? getString(android.R.string.cancel) : savedInstanceState.getString(ARGS_TITLE_NEGATIVE_STR);
        message = savedInstanceState == null || TextUtils.isEmpty(savedInstanceState.getString(ARGS_MESSAGE_STR)) ? getString(R.string.app_name) : savedInstanceState.getString(ARGS_MESSAGE_STR);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setMessage(message)
                .setPositiveButton(positiveTitle, (dialogInterface, i) -> {
                    if (positiveClickListener != null)
                        positiveClickListener.onPositiveClick(dialogInterface, i);
                })
                .setNegativeButton(negativeTitle, (dialogInterface, i) -> {
                    if (negativeClickListener != null)
                        negativeClickListener.onNegativeClick(dialogInterface, i);
                });
        builder.setCancelable(false);
        return builder.create();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        requireDialog().setCanceledOnTouchOutside(false);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof PositiveClickListener) {
            this.positiveClickListener = (PositiveClickListener) context;
        }
        if (context instanceof NegativeClickListener) {
            this.negativeClickListener = (NegativeClickListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.positiveClickListener = null;
        this.negativeClickListener = null;
    }

    public interface PositiveClickListener {
        void onPositiveClick(DialogInterface dialog, int which);
    }

    public interface NegativeClickListener {
        void onNegativeClick(DialogInterface dialog, int which);
    }
}