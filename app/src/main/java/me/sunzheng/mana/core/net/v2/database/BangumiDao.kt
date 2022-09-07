package me.sunzheng.mana.core.net.v2.database

import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.*

@Dao
interface BangumiDao {
    @Query("SELECT * FROM bangumi WHERE type = :type AND status = :status")
    fun queryList(type: Int = 2, status: Int = 1): LiveData<List<BangumiEntity>>

    @Query("SELECT * FROM bangumi WHERE id = :id")
    fun queryById(id: UUID): LiveData<BangumiEntity>

    @Update
    fun update(vararg model: BangumiEntity)

    @Delete
    fun delete(vararg model: BangumiEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg model: BangumiEntity): List<Long>
}

@Dao
interface FavirouteDao {
    @Transaction
    @Query("SELECT * FROM favorite WHERE status = :status AND userName = :userName")
    fun queryList(status: Int, userName: String): LiveData<List<BangumiAndFavorites>>

    @Query("SELECT * FROM bangumi WHERE id IN (SELECT bangumiId FROM favorite WHERE favorite.status = :status AND userName = :userName)")
    fun queryBangumiList(status: Int, userName: String): LiveData<List<BangumiEntity>>

    @Query("SELECT * FROM favorite WHERE bangumiId=:bangumiId")
    fun queryByBangumiId(bangumiId: UUID): LiveData<FavriouteEntity?>

    @Update
    fun update(vararg model: FavriouteEntity)

    @Insert
    fun insert(vararg model: FavriouteEntity)

    @Delete
    fun delete(vararg model: FavriouteEntity)
}

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episode WHERE bangumiId = :bangumiId AND status = :status ORDER BY updateTime DESC")
    fun queryListByBangumiId(bangumiId: UUID, status: Int): LiveData<List<EpisodeEntity>>

    @Query("SELECT * FROM episode WHERE id = :id")
    fun queryById(id: UUID): LiveData<EpisodeEntity?>

    @Update
    fun update(vararg model: EpisodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg model: EpisodeEntity)

    @Delete
    fun delete(vararg model: EpisodeEntity)
}

@Dao
interface VideoFileDao {
    @Query("SELECT * FROM videofile WHERE id = :id ")
    fun queryById(id: UUID): LiveData<VideoFileEntity?>

    @Query("SELECT * FROM videofile WHERE bangumiId = :bangumiId")
    fun queryListByBangumiId(bangumiId: UUID): LiveData<List<VideoFileEntity>?>

    @Query("SELECT * FROM videofile WHERE episodeId = :episodeId ")
    fun queryByEpisodeId(episodeId: UUID): LiveData<VideoFileEntity?>

    @Update
    fun update(vararg model: VideoFileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg model: VideoFileEntity)

    @Delete
    fun delete(vararg model: VideoFileEntity)
}

@Dao
interface WatchProgressDao {
    @Query("SELECT * FROM watchprogress WHERE id = :id ")
    fun queryById(id: UUID): LiveData<WatchProgressEntity?>

    @Query("SELECT * FROM watchprogress WHERE bangumiId = :bangumiId")
    fun queryListByBangumiId(bangumiId: UUID): LiveData<List<WatchProgressEntity>?>

    @Query("SELECT * FROM watchprogress WHERE episodeId = :episodeId ")
    fun queryByEpisodeId(episodeId: UUID): LiveData<WatchProgressEntity?>

    @Update
    fun update(vararg model: WatchProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg model: WatchProgressEntity)

    @Delete
    fun delete(vararg model: WatchProgressEntity)
}
