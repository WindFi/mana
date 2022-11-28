package me.sunzheng.mana.core.net.v2.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import me.sunzheng.mana.core.CoverImage
import java.text.SimpleDateFormat
import java.util.*

@Database(
    entities = [BangumiEntity::class, FavriouteEntity::class, WatchProgressEntity::class, EpisodeEntity::class, VideoFileEntity::class, RelationOnAir::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(UUIDTypeConvert::class, CovertImageConvert::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bangumiDao(): BangumiDao
    abstract fun favriouteDao(): FavirouteDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun onAirDao(): OnAirDao
    abstract fun videoFileDao(): VideoFileDao
}


object UUIDTypeConvert {
    @TypeConverter
    fun fromUUID(uuid: UUID?) = uuid?.toString()

    @TypeConverter
    fun uuidFromString(uuid: String?) = uuid?.let { UUID.fromString(it) } ?: null
}

object CovertImageConvert {
    @TypeConverter
    fun fromObject(obj: CoverImage?) = Gson().toJson(obj)

    @TypeConverter
    fun objFromString(json: String?) =
        json?.let { Gson().fromJson(it, CoverImage::class.java) } ?: null
}

object DateConvert {
    @TypeConverter
    fun fromDate(date: Date) = date.time

    @TypeConverter
    fun dateFromLong(str: Long) = str.let {
        SimpleDateFormat("yyyy-MM-dd").format(Date(str))
    }
}