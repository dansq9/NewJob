package app.ascend.data.repo

import app.ascend.data.local.TrackedJobEntity
import app.ascend.data.local.TrackerDao
import app.ascend.data.model.Job
import app.ascend.data.model.TrackStage
import app.ascend.data.model.WorkType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class TrackedJob(val job: Job, val stage: TrackStage)

@Singleton
class TrackerRepository @Inject constructor(
    private val dao: TrackerDao,
) {
    val tracked: Flow<List<TrackedJob>> = dao.observeAll().map { list -> list.map { it.toTracked() } }

    suspend fun stageOf(jobId: String): TrackStage? = dao.get(jobId)?.let { TrackStage.from(it.stage) }

    suspend fun save(job: Job, stage: TrackStage = TrackStage.SAVED) =
        dao.upsert(job.toEntity(stage))

    suspend fun setStage(jobId: String, stage: TrackStage) =
        dao.updateStage(jobId, stage.name, System.currentTimeMillis())

    suspend fun remove(jobId: String) = dao.delete(jobId)
}

private fun TrackedJobEntity.toTracked() = TrackedJob(
    job = Job(
        id = jobId, title = title, company = company, logoUrl = logoUrl,
        location = location, workType = runCatching { WorkType.valueOf(workType) }.getOrDefault(WorkType.UNKNOWN),
        employmentType = employmentType, salary = salary, postedAgo = null,
        description = null, applyUrl = applyUrl,
    ),
    stage = TrackStage.from(stage) ?: TrackStage.SAVED,
)

private fun Job.toEntity(stage: TrackStage) = TrackedJobEntity(
    jobId = id, title = title, company = company, logoUrl = logoUrl, location = location,
    workType = workType.name, employmentType = employmentType, salary = salary,
    applyUrl = applyUrl, stage = stage.name, updatedAt = System.currentTimeMillis(),
)
