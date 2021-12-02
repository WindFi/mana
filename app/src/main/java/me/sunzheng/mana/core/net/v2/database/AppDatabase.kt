package me.sunzheng.mana.core.net.v2.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import me.sunzheng.mana.core.CoverImage
import java.util.*

@Database(
    entities = [BangumiEntity::class, FavriouteEntity::class, WatchProgressEntity::class, EpisodeEntity::class, VideoFileEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(UUIDTypeConvert::class, CovertImageConvert::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bangumiDao(): BangumiDao
    abstract fun favriouteDao(): FavirouteDao
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