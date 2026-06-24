package app.ascend.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ResumeDao {
    @Query("SELECT * FROM resumes ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<ResumeEntity>>

    @Query("SELECT * FROM resumes WHERE id = :id LIMIT 1")
    suspend fun get(id: String): ResumeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(resume: ResumeEntity)

    @Query("UPDATE resumes SET atsScore = :score, optimizedForJobId = :jobId WHERE id = :id")
    suspend fun setAtsScore(id: String, score: Int?, jobId: String?)

    @Query("DELETE FROM resumes WHERE id = :id")
    suspend fun delete(id: String)
}
