package com.example.hometempreaturemonitor

import android.content.Context
import androidx.room.*
import java.util.*

@Entity
data class Temperature(
    @PrimaryKey(autoGenerate = true) val uid: Int,
    @ColumnInfo(name = "time") val time: String?,
    @ColumnInfo(name = "value") val value: String?
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
}

@Database(entities = [Temperature::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun temperatureDao(): TemperatureDao
}

class DataStorage() {
    private var db: RoomDatabase? = null
    private var temperatureDao: TemperatureDao? = null

    companion object {
        val instance = DataStorage()
    }

    fun init(context: Context) {
        val localDb = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "database-temperatures"
        ).build()
        temperatureDao = localDb.temperatureDao()
        db = localDb
    }

    fun getLastRecord(): Temperature? {
        return temperatureDao?.getLastRecord()
    }

    fun insert(date: Date, value: String?) {
        temperatureDao?.insert(Temperature(0, date.toString(), value))
    }
}