package me.sunzheng.mana

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.RadioButton
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import me.sunzheng.mana.FeedbackActivity
import me.sunzheng.mana.core.net.Status
import me.sunzheng.mana.databinding.ActivityFeedbackBinding
import me.sunzheng.mana.feedback.FeedbackViewModel
import javax.inject.Inject

@AndroidEntryPoint
class FeedbackActivity @Inject constructor() : AppCompatActivity() {
    var episodeId: String? = null
    var videoFileId: String? = null

    var feedbackStringMap: HashMap<Int, String>? = HashMap()
    val viewModel by viewModels<FeedbackViewModel>()
    val binding: ActivityFeedbackBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_feedback)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        var bundle = savedInstanceState ?: intent.extras!!
        episodeId = bundle.getString(ARGS_EPISODE_ID_STR)
        videoFileId = bundle.getString(ARGS_VIDEOFILE_ID_STR)
        
        // Check if error message is provided (from playback error)
        val errorMessage = bundle.getString("error_message")
        val stackTrace = bundle.getString("error_stack_trace")
        
        if (!TextUtils.isEmpty(errorMessage)) {
            // Build complete error message with stack trace if available
            val completeErrorMessage = buildString {
                append(errorMessage)
                if (!TextUtils.isEmpty(stackTrace)) {
                    append("\n\n堆栈跟踪:\n")
                    append(stackTrace)
                }
            }
            
            // Pre-fill the feedback edit text with complete error message
            binding.feedbackRadiogroup.check(R.id.feedback_etc_radiobutton)
            binding.feedbackEdittext.setText(completeErrorMessage)
            // Show FAB since text is not empty
            fabShow()
        }

        binding.feedbackRadiogroup.setOnCheckedChangeListener { _, checkedId ->
            binding.feedbackEdittext.isFocusable = checkedId == R.id.feedback_etc_radiobutton
            binding.feedbackEdittext.isFocusableInTouchMode =
                checkedId == R.id.feedback_etc_radiobutton
            if (checkedId != R.id.feedback_etc_radiobutton) {
                binding.feedbackEdittext.setText("")
                fabShow()
            } else {
                fabHide()
                binding.feedbackEdittext.requestFocus()
            }
        }
        binding.feedbackEdittext.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.feedbackRadiogroup.check(R.id.feedback_etc_radiobutton)
                showSoftInputKeyboard()
            } else {
                hideSoftInputKeyboard()
            }
        }
        binding.feedbackEdittext.setOnClickListener { binding.feedbackRadiogroup.check(R.id.feedback_etc_radiobutton) }
        binding.feedbackEdittext.setOnEditorActionListener(OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                binding.fab.performClick()
                return@OnEditorActionListener true
            }
            false
        })
        binding.feedbackEdittext.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (!binding.feedbackEdittext.isFocusable) return
                if (s.isEmpty()) {
                    fabHide()
                } else {
                    fabShow()
                }
            }
        })
        binding.fab.setOnClickListener(View.OnClickListener { v ->
            val feedbackResult = handleString(binding.feedbackRadiogroup.checkedRadioButtonId)
            if (TextUtils.isEmpty(feedbackResult)) {
                Snackbar.make(v, feedbackResult + sendFromString(), Snackbar.LENGTH_SHORT).show()
                return@OnClickListener
            }
            hideSoftInputKeyboard()
            // Send feedback message (includes error message if from playback error)
            viewModel.sendFeedback(feedbackResult)
            binding.progressbar.isVisible = true
            viewModel.submit(episodeId = episodeId, videoFileId = videoFileId).observe(this) {
//                Toast.makeText(this@FeedbackActivity, "Sending...", Toast.LENGTH_SHORT).show()
                binding.progressbar.isVisible = false
                when (it.code) {
                    Status.SUCCESS -> {
                        finish()
                    }

                    Status.ERROR -> {
                        it.message?.run {
                            Toast.makeText(this@FeedbackActivity, it.toString(), Toast.LENGTH_SHORT)
                                .show()
                        }
                    }

                    else -> {}
                }
            }
        })
        initSelectString()
        initViewState()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(ARGS_EPISODE_ID_STR, episodeId)
        outState.putString(ARGS_EPISODE_ID_STR, videoFileId)
        super.onSaveInstanceState(outState)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                hideSoftInputKeyboard()
                onBackPressedDispatcher.onBackPressed()
                return true
            }

            else -> {}
        }
        return super.onOptionsItemSelected(item)
    }

    fun initViewState() {
        fabHide()
    }

    fun initSelectString() {
        if (feedbackStringMap == null) {
            feedbackStringMap = HashMap(binding.feedbackRadiogroup.childCount - 1)
        }
        for (i in binding.feedbackRadiogroup.childCount - 1 downTo -1 + 1) {
            val v = binding.feedbackRadiogroup.getChildAt(i)
            if (v is RadioButton) {
                feedbackStringMap!![v.getId()] = v.text.toString()
            }
        }
    }

    fun handleString(id: Int): String? {
        return if (id == binding.feedbackEtcRadiobutton.id) {
            binding.feedbackEdittext.text.toString()
        } else if (feedbackStringMap != null && feedbackStringMap!!.containsKey(id)) {
            feedbackStringMap!![id]
        } else {
            ""
        }
    }

    fun sendFromString(): String {
        return "----send from ${getString(applicationInfo.labelRes)}"
    }

    fun fabShow() {
        if (binding.fab.isVisible) return
        val anim = AnimationUtils.loadAnimation(binding.fab.context, R.anim.slide_in_up)
        anim.interpolator = LinearOutSlowInInterpolator()
        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                binding.fab.show()
            }

            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {}
        })
        binding.fab.startAnimation(anim)
    }

    fun fabHide() {
        if (!binding.fab.isVisible) return
        val anim = AnimationUtils.loadAnimation(binding.fab.context, R.anim.slide_out_down)
        anim.interpolator = FastOutSlowInInterpolator()
        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                binding.fab.hide()
            }

            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {}
        })
        binding.fab.startAnimation(anim)
    }

    fun showSoftInputKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.feedbackEdittext, InputMethodManager.SHOW_FORCED)
    }


    fun hideSoftInputKeyboard() {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(binding.feedbackEdittext.windowToken, 0)
    }

    companion object {
        private const val ARGS_EPISODE_ID_STR = "episodeId"
        private const val ARGS_VIDEOFILE_ID_STR = "videoFileId"

        @JvmStatic
        fun newInstance(context: Context?, episodeId: String?, videoFileId: String?): Intent {
            val intent = Intent(context, FeedbackActivity::class.java)
            val extras = Bundle()
            extras.putString(ARGS_EPISODE_ID_STR, episodeId)
            extras.putString(ARGS_VIDEOFILE_ID_STR, videoFileId)
            intent.putExtras(extras)
            return intent
        }
    }
}