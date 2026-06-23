package app.ascend.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {
    @Query("SELECT * FROM tracked_jobs ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<TrackedJobEntity>>

    @Query("SELECT * FROM tracked_jobs WHERE jobId = :id LIMIT 1")
    suspend fun get(id: String): TrackedJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(job: TrackedJobEntity)

    @Query("UPDATE tracked_jobs SET stage = :stage, updatedAt = :now WHERE jobId = :id")
    suspend fun updateStage(id: String, stage: String, now: Long)

    @Query("DELETE FROM tracked_jobs WHERE jobId = :id")
    suspend fun delete(id: String)
}
