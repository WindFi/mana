package me.sunzheng.mana

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.sunzheng.mana.core.net.v2.database.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*


@RunWith(AndroidJUnit4::class)
class DataBaseTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()
    private var userName: String = "test"
    private lateinit var favriouteDao: FavirouteDao
    private lateinit var bangumiDao: BangumiDao

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        favriouteDao = db.favriouteDao()
        bangumiDao = db.bangumiDao()
    }

    @After
    fun close() {
        db.close()
    }

    var bangumiList = (1 until 20).map {
        BangumiEntity(
            id = UUID.randomUUID(),
            name = "${it}",
            type = 2,
            status = 1
        )
    }

    @Test
    fun insertBangumi() {
        bangumiList.forEach {
            bangumiDao.insert(it)
        }
        var result = LiveDataTestUtil.getValue(bangumiDao.queryList(type = 2, status = 1))
        assert(result?.size == bangumiList.size)

        bangumiList.map { FavriouteEntity(bangumiId = it.id, status = 3, userName = userName) }
            .forEach {
                favriouteDao.insert(it)
            }
        FavriouteEntity(bangumiList[0].id, status = 3, userName = "null").run {
            favriouteDao.insert(this)
        }
        FavriouteEntity(bangumiList[0].id, status = 4, userName = userName).run {
            favriouteDao.insert(this)
        }
        var mResult = LiveDataTestUtil.getValue(favriouteDao.queryBangumiList(3, userName))
            ?.sortedBy { it.name?.toInt() }
        assert(mResult?.size == bangumiList.size)
    }
}