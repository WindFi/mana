package me.sunzheng.mana;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatRadioButton;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
//        getActionBar().setDisplayHomeAsUpEnabled(true);
        if (savedInstanceState == null)
            savedInstanceState = getIntent().getExtras();

        episodeId = savedInstanceState.getString(ARGS_EPISODE_ID_STR);
        videoFileId = savedInstanceState.getString(ARGS_EPISODE_ID_STR);
        setPresenter(new FeedbackPresenterImpl(episodeId, videoFileId, this, ((App) getApplication()).getRetrofit().create(HomeApiService.Feedback.class)));

        mRadioGroup = (RadioGroup) findViewById(R.id.feedback_radiogroup);
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId != R.id.feedback_etc_radiobutton) {
                    mEditText.setText("");
                    return;
                }
                mEditText.setFocusable(checkedId == R.id.feedback_etc_radiobutton);
                mEditText.setFocusableInTouchMode(checkedId == R.id.feedback_etc_radiobutton);
                mEditText.requestFocus();
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
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String feedbackResult = handleString(mRadioGroup.getCheckedRadioButtonId());
                if (TextUtils.isEmpty(feedbackResult)) {
                    // TODO: 2018/3/1 replace hard coding use resId
                    Snackbar.make(v, "result must be not null", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                mPresenter.setMessage(feedbackResult);
                mPresenter.submit();
            }
        });
        initSelectString();
        initViewState();
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
//        mRadioGroup.check(mRadioButton0.getId());
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
}
