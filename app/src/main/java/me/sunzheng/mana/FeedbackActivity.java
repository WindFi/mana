package me.sunzheng.mana;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatRadioButton;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;

import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.HomeContract;
import me.sunzheng.mana.home.feedback.FeedbackPresenterImpl;
import me.sunzheng.mana.utils.App;

public class FeedbackActivity extends AppCompatActivity implements HomeContract.Feedback.View {
    private final static String ARGS_EPISODE_ID_STR = "episodeId";
    private final static String ARGS_VIDEOFILE_ID_STR = "videoFileId";

    HomeContract.Feedback.Presenter mPresenter;
    String episodeId, videoFileId;
    RadioGroup mRadioGroup;
    AppCompatRadioButton mRadioButton0, mRadioButton1, mRadioButton2, mEtcRadioButton;
    AppCompatEditText mEditText;
    FloatingActionButton fab;
    HashMap<Integer, String> feedbackStringMap = new HashMap<>();

    public static Intent newInstance(Context context, String episodeId, String videoFileId) {
        Intent intent = new Intent(context, FeedbackActivity.class);
        Bundle extras = new Bundle();

        extras.putString(ARGS_EPISODE_ID_STR, episodeId);
        extras.putString(ARGS_VIDEOFILE_ID_STR, videoFileId);

        intent.putExtras(extras);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (savedInstanceState == null)
            savedInstanceState = getIntent().getExtras();
        episodeId = savedInstanceState.getString(ARGS_EPISODE_ID_STR);
        videoFileId = savedInstanceState.getString(ARGS_EPISODE_ID_STR);
        setPresenter(new FeedbackPresenterImpl(episodeId, videoFileId, this, ((App) getApplication()).getRetrofit().create(HomeApiService.Feedback.class)));

        mRadioGroup = (RadioGroup) findViewById(R.id.feedback_radiogroup);
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                mEditText.setEnabled(checkedId == R.id.feedback_etc_radiobutton);
                mEditText.setFocusable(checkedId == R.id.feedback_etc_radiobutton);
                mEditText.setFocusableInTouchMode(checkedId == R.id.feedback_etc_radiobutton);
                if (checkedId != R.id.feedback_etc_radiobutton) {
                    mEditText.setText("");
                    fabShow();
                } else {
                    fabHide();
                    mEditText.performClick();
                }
                mEditText.setEnabled(true);
            }
        });
        mRadioButton0 = (AppCompatRadioButton) findViewById(R.id.feedback_radiobutton_0);
        mRadioButton1 = (AppCompatRadioButton) findViewById(R.id.feedback_radiobutton_1);
        mRadioButton2 = (AppCompatRadioButton) findViewById(R.id.feedback_radiobutton_2);
        mEtcRadioButton = (AppCompatRadioButton) findViewById(R.id.feedback_etc_radiobutton);

        mEditText = (AppCompatEditText) findViewById(R.id.feedback_edittext);
        mEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mRadioGroup.check(mEtcRadioButton.getId());
                    showSoftInputKeyboard();
                } else {
                    hideSoftInputKeyboard();
                }
            }
        });
        mEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditText.setFocusable(true);
                mEditText.setFocusableInTouchMode(true);
                mEditText.requestFocus();
            }
        });
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    fab.performClick();
                    return true;
                }
                return false;
            }
        });
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!mEditText.isFocusable())
                    return;
                if (s.length() < 1) {
                    fabHide();
                } else {
                    fabShow();
                }
            }
        });
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String feedbackResult = handleString(mRadioGroup.getCheckedRadioButtonId());
                if (TextUtils.isEmpty(feedbackResult)) {
                    // TODO: 2018/3/1 replace hard coding use resId
                    Snackbar.make(v, feedbackResult, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                hideSoftInputKeyboard();
                mPresenter.setMessage(feedbackResult);
                mPresenter.submit();
            }
        });
        initSelectString();
        initViewState();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(ARGS_EPISODE_ID_STR, episodeId);
        outState.putString(ARGS_EPISODE_ID_STR, videoFileId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void setPresenter(HomeContract.Feedback.Presenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public void showToast(CharSequence message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void finishSelf() {
        finish();
    }

    @Override
    public void showProgressIntractor(boolean active) {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    void initViewState() {
        fabHide();
    }

    void initSelectString() {
        if (feedbackStringMap == null) {
            feedbackStringMap = new HashMap<>(mRadioGroup.getChildCount() - 1);
        }
        for (int i = mRadioGroup.getChildCount() - 1; i > -1; i--) {
            View v = mRadioGroup.getChildAt(i);
            if (v instanceof RadioButton) {
                feedbackStringMap.put(v.getId(), ((RadioButton) v).getText().toString());
            }
        }
    }

    String handleString(int id) {
        if (id == mEtcRadioButton.getId()) {
            return mEditText.getText().toString();
        } else if (feedbackStringMap != null && feedbackStringMap.containsKey(id)) {
            return feedbackStringMap.get(id);
        } else {
            return "";
        }
    }

    void fabShow() {
        if (fab == null || fab.getVisibility() == View.VISIBLE)
            return;
        Animation anim = AnimationUtils.loadAnimation(fab.getContext(), R.anim.slide_in_up);
        anim.setInterpolator(new LinearOutSlowInInterpolator());
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                fab.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        fab.startAnimation(anim);
    }

    void fabHide() {
        if (fab == null || fab.getVisibility() != View.VISIBLE)
            return;
        Animation anim = AnimationUtils.loadAnimation(fab.getContext(), R.anim.slide_out_down);
        anim.setInterpolator(new FastOutSlowInInterpolator());
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                fab.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        fab.startAnimation(anim);
    }

    void showSoftInputKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(getCurrentFocus(), InputMethodManager.SHOW_FORCED);
    }

    void hideSoftInputKeyboard() {
        if (getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
        }
    }
}
