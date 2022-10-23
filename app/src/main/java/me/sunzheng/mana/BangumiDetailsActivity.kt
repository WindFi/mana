package me.sunzheng.mana

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import dagger.hilt.android.AndroidEntryPoint
import me.sunzheng.mana.BangumiDetailsActivity
import me.sunzheng.mana.core.net.v2.database.BangumiEntity
import me.sunzheng.mana.home.bangumi.BangumiDetailsFragment
import javax.inject.Inject

/**
 * I wannt passed the image from anothers
 */
@AndroidEntryPoint
class BangumiDetailsActivity @Inject constructor() : AppCompatActivity() {
    companion object {
        const val ARGS_ID_STR = "id"
        const val ARGS_ABLUM_URL_STR = "imageurl"
        const val ARGS_TITLE_STR = "title"
        const val PAIR_IMAGE_STR = "pair_image"

        @JvmStatic
        fun newInstance(
            activity: Activity,
            bangumiEntity: BangumiEntity?,
            vararg imageView: View?
        ) {
            val intent = Intent(activity, BangumiDetailsActivity::class.java)
            val extras = Bundle()
            extras.putParcelable(ARGS_ID_STR, bangumiEntity)
            intent.putExtras(extras)
            val pair0 = Pair.create(
                imageView[0], PAIR_IMAGE_STR
            )
            val optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
                activity, pair0
            )
            ActivityCompat.startActivity(activity, intent, optionsCompat.toBundle())
        }
    }

    lateinit var fragment: BangumiDetailsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bangumi_details)
        var extras = savedInstanceState?.getBundle("extras") ?: intent.extras
        extras?.getParcelable<BangumiEntity>(ARGS_ID_STR)?.run {
            supportFragmentManager.beginTransaction()
                .replace(R.id.contentPanel, BangumiDetailsFragment.newInstance(this), "fragment")
                .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }


}