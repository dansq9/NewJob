package app.ascend.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A resume in the user's library (file metadata + last ATS result). */
@Entity(tableName = "resumes")
data class ResumeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val uri: String,
    val sizeBytes: Long?,
    val mime: String?,
    val addedAt: Long,
    val atsScore: Int? = null,
    val optimizedForJobId: String? = null,
)
