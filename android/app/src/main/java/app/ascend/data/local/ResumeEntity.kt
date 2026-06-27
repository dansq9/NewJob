package app.ascend.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A resume in the user's library (file metadata + last ATS result + editable identity). */
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
    /** User-editable display title; falls back to [name] when null. */
    val title: String? = null,
    /** Last time the user edited/renamed this resume (separate from [addedAt]). */
    val updatedAt: Long = 0L,
    /** Where the resume came from: "uploaded" (a file pointer) or "built" (in-app structured). */
    val source: String = SOURCE_UPLOADED,
    /** Structured resume JSON for built resumes; null for uploaded file pointers. */
    val content: String? = null,
) {
    companion object {
        const val SOURCE_UPLOADED = "uploaded"
        const val SOURCE_BUILT = "built"
    }
}
