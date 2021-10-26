package com.example.hometempreaturemonitor

import android.content.Context
import android.util.Log
import androidx.room.*
import java.util.*

@Entity
data class Temperature(
    @PrimaryKey(autoGenerate = true) val uid: Int,
    @ColumnInfo(name = "time") val time: String?,
    @ColumnInfo(name = "temp") val temp: String?,
    @ColumnInfo(name = "humidity") val humidity: String?
)

@Dao
interface TemperatureDao {
    @Query("SELECT * FROM temperature")
    fun getAll(): List<Temperature>

    @Query("SELECT * FROM temperature WHERE uid IN (:recordIds)")
    fun loadAllByIds(recordIds: IntArray): List<Temperature>

    @Query("SELECT * FROM temperature ORDER BY uid DESC LIMIT 1;")
    fun getLastRecord(): Temperature

    @Insert
    fun insertAll(vararg records: Temperature)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(record: Temperature)

    @Delete
    fun delete(record: Temperature)

    @Query("SELECT COUNT(uid) FROM temperature")
    fun getRecordsCount(): Int

    @Query("DELETE FROM temperature WHERE uid NOT IN (SELECT uid FROM temperature ORDER BY uid LIMIT (:count))")
    fun deleteOutdated(count: Int)
}

@Database(entities = [Temperature::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun temperatureDao(): TemperatureDao
}

class TemperatureDataStorage() {
    private var db: RoomDatabase? = null
    private var temperatureDao: TemperatureDao? = null

    companion object {
        const val MAX_RECORDS_COUNT = 4 // TODO: actualise numbers
        const val RECORDS_TO_REMAIN = 2 // TODO: actualise numbers
        val instance = TemperatureDataStorage()
    }

    fun init(context: Context) {
        val localDb = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "database-temperatures"
        ).fallbackToDestructiveMigration().build()
        temperatureDao = localDb.temperatureDao()
        db = localDb
    }

    fun getLastRecord(): Temperature? {
        return temperatureDao?.getLastRecord()
    }

    fun deleteOutdatedRecords() {
        Log.i("Temperature Data Storage",
            "Current records count: ${temperatureDao?.getRecordsCount()}")
        if (temperatureDao!!.getRecordsCount() > MAX_RECORDS_COUNT) {
            temperatureDao!!.deleteOutdated(RECORDS_TO_REMAIN)
        }
    }

    fun insert(date: Date, temp: Float, humidity: Float) {
        if (temperatureDao != null) {
            deleteOutdatedRecords()
            temperatureDao?.insert(
                Temperature(
                    0, date.toString(),
                    temp.toString(), humidity.toString()
                )
            )
        }
    }
}