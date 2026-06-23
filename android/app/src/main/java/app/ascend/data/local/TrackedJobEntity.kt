package app.ascend.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracked_jobs")
data class TrackedJobEntity(
    @PrimaryKey val jobId: String,
    val title: String,
    val company: String,
    val logoUrl: String?,
    val location: String,
    val workType: String,
    val employmentType: String?,
    val salary: String?,
    val applyUrl: String?,
    val stage: String,          // TrackStage name
    val updatedAt: Long,
)
