package dev.davwheat.mocndetector.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import dev.davwheat.mocndetector.db.mocninfo.MocnInfo
import dev.davwheat.mocndetector.db.mocninfo.MocnInfoDao
import dev.davwheat.mocndetector.services.RAT
import kotlinx.serialization.json.Json
import java.time.ZonedDateTime

@Database(entities = [MocnInfo::class], version = 1)
@TypeConverters(AppDatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mocnInfoDao(): MocnInfoDao
}


internal class AppDatabaseConverters {
    val json = Json

    @TypeConverter
    fun fromZonedDateTimeString(zonedDateTimeString: String): ZonedDateTime =
        ZonedDateTime.parse(zonedDateTimeString)

    @TypeConverter
    fun toZonedDateTimeString(zonedDateTime: ZonedDateTime): String = zonedDateTime.toString()

    @TypeConverter
    fun gciRatPairListToString(data: List<Triple<String, String, RAT>>): String? =
        json.encodeToString(data)

    @TypeConverter
    fun gciRatPairListFromString(data: String): List<Triple<String, String, RAT>> =
        json.decodeFromString(data)

    @TypeConverter
    fun toSetOfString(data: Set<String>): String =
        json.encodeToString(data)

    @TypeConverter
    fun fromSetOfString(data: String): Set<String> =
        json.decodeFromString(data)
}
