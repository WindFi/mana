package me.sunzheng.mana.core.net.v2.database

import androidx.room.*
import java.util.*

@Dao
interface BangumiDao {
    @Query("SELECT * FROM bangumi WHERE type = :type")
    fun queryList(type: Int = 2): List<BangumiEntity>

    @Query("SELECT * FROM bangumi WHERE id = :id")
    fun queryById(id: UUID): BangumiEntity

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
    fun queryList(status: Int, userName: String): List<BangumiAndFavorites>

    @Transaction
//    @Query("SELECT * FROM bangumi WHERE id IN (SELECT bangumiId FROM favorite WHERE favorite.status = :status AND userName = :userName)")
    @Query("SELECT * FROM favorite WHERE favorite.status = :status AND userName = :userName")
    fun queryBangumiList(status: Int, userName: String): List<BangumiAndFavorites>

    @Transaction
    @Query("SELECT * FROM favorite WHERE bangumiId=:bangumiId AND userName = :userName")
    fun queryByBangumiId(bangumiId: UUID, userName: String): BangumiAndFavorites?

    @Update
    fun update(vararg model: FavriouteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg model: FavriouteEntity)

    @Delete
    fun delete(vararg model: FavriouteEntity)

}

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episode WHERE bangumiId = :bangumiId AND status = :status ORDER BY updateTime DESC")
    fun queryListByBangumiId(bangumiId: UUID, status: Int): List<EpisodeEntity>

    @Query("SELECT * FROM episode WHERE id = :id")
    fun queryById(id: UUID): EpisodeEntity?

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
    fun queryById(id: UUID): VideoFileEntity?

    @Query("SELECT * FROM videofile WHERE bangumiId = :bangumiId")
    fun queryListByBangumiId(bangumiId: UUID): List<VideoFileEntity>?

    @Query("SELECT * FROM videofile WHERE episodeId = :episodeId ")
    fun queryByEpisodeId(episodeId: UUID): VideoFileEntity?

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
    fun queryById(id: UUID): WatchProgressEntity?

    @Query("SELECT * FROM watchprogress WHERE bangumiId = :bangumiId")
    fun queryListByBangumiId(bangumiId: UUID): List<WatchProgressEntity>?

    @Query("SELECT * FROM watchprogress WHERE episodeId = :episodeId ")
    fun queryByEpisodeId(episodeId: UUID): WatchProgressEntity?

    @Update
    fun update(vararg model: WatchProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg model: WatchProgressEntity)

    @Delete
    fun delete(vararg model: WatchProgressEntity)
}

@Dao
interface OnAirDao {
    @Query("SELECT * FROM onair, bangumi WHERE onair.bangumiId = bangumi.id  AND bangumi.type = :type")
    fun queryList(type: Int): List<BangumiEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg model: RelationOnAir)

    @Query("delete FROM onair")
    fun deleteAll()
}