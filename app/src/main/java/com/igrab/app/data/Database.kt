package com.igrab.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ──────────────────────────────────────────────
// Type Converters
// ──────────────────────────────────────────────
class Converters {
    @TypeConverter fun statusToString(s: DownloadStatus) = s.name
    @TypeConverter fun stringToStatus(s: String) = DownloadStatus.valueOf(s)
}

// ──────────────────────────────────────────────
// DAO
// ──────────────────────────────────────────────
@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun allJobs(): Flow<List<DownloadJob>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getById(id: Long): DownloadJob?

    @Insert
    suspend fun insert(job: DownloadJob): Long

    @Update
    suspend fun update(job: DownloadJob)

    @Delete
    suspend fun delete(job: DownloadJob)

    @Query("DELETE FROM downloads")
    suspend fun deleteAll()
}

// ──────────────────────────────────────────────
// Database
// ──────────────────────────────────────────────
@Database(entities = [DownloadJob::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "igrab.db")
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
        }
    }
}
