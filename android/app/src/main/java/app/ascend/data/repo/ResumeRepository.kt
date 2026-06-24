package app.ascend.data.repo

import app.ascend.data.local.ProfileRepository
import app.ascend.data.local.ResumeDao
import app.ascend.data.local.ResumeEntity
import app.ascend.ui.util.PickedFile
import app.ascend.ui.util.RESUME_MIME
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** A resume the user has added to their library. */
data class ResumeRecord(
    val id: String,
    val name: String,
    val uri: String,
    val sizeBytes: Long?,
    val mime: String?,
    val addedAt: Long,
    val atsScore: Int?,
    val optimizedForJobId: String?,
)

/** Outcome of trying to add a picked file to the library. */
sealed interface AddResumeResult {
    data class Success(val record: ResumeRecord) : AddResumeResult
    data class Rejected(val reason: String) : AddResumeResult
}

@Singleton
class ResumeRepository @Inject constructor(
    private val dao: ResumeDao,
    private val profile: ProfileRepository,
) {
    val library: Flow<List<ResumeRecord>> =
        dao.observeAll().map { list -> list.map { it.toRecord() } }

    val selectedResumeId: Flow<String?> = profile.selectedResumeId

    suspend fun get(id: String): ResumeRecord? = dao.get(id)?.toRecord()

    /**
     * Returns a user-facing rejection reason, or null if the file is acceptable.
     * Accepts on a known resume MIME OR a known extension, so a missing/generic
     * MIME (e.g. application/octet-stream) falls back to the extension — and
     * casing like RESUME.PDF is handled. Shared by [add] and onboarding.
     */
    fun reasonToReject(file: PickedFile): String? {
        val mimeOk = file.mime != null && file.mime in RESUME_MIME
        val extOk = file.name.substringAfterLast('.', "").lowercase() in ALLOWED_EXTENSIONS
        if (!mimeOk && !extOk) return "Unsupported file. Upload a PDF, DOC, or DOCX."
        file.sizeBytes?.let { size ->
            if (size > MAX_BYTES) return "File is too large. Max ${MAX_BYTES / 1_000_000}MB."
            if (size <= 0L) return "That file looks empty."
        }
        return null
    }

    /** Validates type + size, persists the file metadata, and auto-selects it. */
    suspend fun add(file: PickedFile): AddResumeResult {
        reasonToReject(file)?.let { return AddResumeResult.Rejected(it) }
        val record = ResumeEntity(
            id = UUID.randomUUID().toString(),
            name = file.name,
            uri = file.uri.toString(),
            sizeBytes = file.sizeBytes,
            mime = mime,
            addedAt = System.currentTimeMillis(),
        )
        dao.upsert(record)
        profile.setSelectedResume(record.id)
        return AddResumeResult.Success(record.toRecord())
    }

    suspend fun select(id: String) = profile.setSelectedResume(id)

    suspend fun remove(id: String) {
        dao.delete(id)
        // If we removed the active resume, fall back to the most-recent remaining one (or clear).
        if (profile.selectedResumeIdOnce() == id) {
            val next = dao.observeAll().first().firstOrNull()?.id
            profile.setSelectedResume(next)
        }
    }

    suspend fun recordAtsScore(id: String, score: Int?, jobId: String?) =
        dao.setAtsScore(id, score, jobId)

    companion object {
        const val MAX_BYTES = 10_000_000L
        val ALLOWED_EXTENSIONS = setOf("pdf", "doc", "docx")
    }
}

private fun ResumeEntity.toRecord() = ResumeRecord(
    id = id, name = name, uri = uri, sizeBytes = sizeBytes, mime = mime,
    addedAt = addedAt, atsScore = atsScore, optimizedForJobId = optimizedForJobId,
)
