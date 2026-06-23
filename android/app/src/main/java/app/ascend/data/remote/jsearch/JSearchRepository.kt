package app.ascend.data.remote.jsearch

import app.ascend.core.Resource
import app.ascend.data.model.Job
import app.ascend.data.model.WorkType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class JSearchRepository @Inject constructor(
    private val api: JSearchApi,
) {
    suspend fun search(
        query: String,
        location: String? = null,
        page: Int = 1,
        remoteOnly: Boolean = false,
        employmentTypes: List<String> = emptyList(),
        datePosted: String = "all",
    ): Resource<List<Job>> = withContext(Dispatchers.IO) {
        try {
            val q = listOfNotNull(query.trim().ifBlank { null }, location?.trim()?.ifBlank { null })
                .joinToString(" in ")
                .ifBlank { "jobs" }
            val resp = api.search(
                query = q,
                page = page,
                remoteOnly = if (remoteOnly) true else null,
                employmentTypes = employmentTypes.joinToString(",").ifBlank { null },
                datePosted = datePosted,
            )
            Resource.Success(resp.data.mapNotNull { it.toJob() })
        } catch (t: Throwable) {
            Resource.Error(t.message ?: "Couldn't load jobs", t)
        }
    }
}

internal fun JSearchJob.toJob(): Job? {
    val id = jobId ?: return null
    val title = title ?: return null
    return Job(
        id = id,
        title = title,
        company = employer ?: "—",
        logoUrl = employerLogo,
        location = buildLocation(),
        workType = when {
            isRemote == true -> WorkType.REMOTE
            city != null || state != null -> WorkType.ONSITE
            else -> WorkType.UNKNOWN
        },
        employmentType = employmentType?.toReadableEmployment(),
        salary = formatSalary(),
        postedAgo = postedAtTimestamp?.let { relativeTime(it) },
        description = description,
        applyUrl = applyLink,
    )
}

private fun JSearchJob.buildLocation(): String {
    val parts = listOfNotNull(city, state, country).filter { it.isNotBlank() }
    val base = if (parts.isEmpty()) "" else parts.take(2).joinToString(", ")
    return when {
        isRemote == true && base.isNotEmpty() -> "Remote · $base"
        isRemote == true -> "Remote"
        else -> base.ifEmpty { "—" }
    }
}

private fun String.toReadableEmployment(): String = when (uppercase()) {
    "FULLTIME" -> "Full-time"
    "PARTTIME" -> "Part-time"
    "CONTRACTOR" -> "Contract"
    "INTERN" -> "Internship"
    else -> lowercase().replaceFirstChar { it.uppercase() }
}

private fun JSearchJob.formatSalary(): String? {
    val lo = minSalary ?: return null
    val hi = maxSalary
    fun fmt(v: Double): String = if (v >= 1000) "$${(v / 1000).roundToInt()}k" else "$${v.roundToInt()}"
    val period = when (salaryPeriod?.uppercase()) {
        "HOUR" -> " / hr"; "MONTH" -> " / mo"; else -> ""
    }
    return if (hi != null && hi > lo) "${fmt(lo)} – ${fmt(hi)}$period" else "${fmt(lo)}$period"
}

private fun relativeTime(timestampSec: Long): String {
    val deltaMs = System.currentTimeMillis() - timestampSec * 1000
    val days = TimeUnit.MILLISECONDS.toDays(deltaMs)
    val hours = TimeUnit.MILLISECONDS.toHours(deltaMs)
    return when {
        days >= 7 -> "${days / 7}w ago"
        days >= 1 -> "${days}d ago"
        hours >= 1 -> "${hours}h ago"
        else -> "just now"
    }
}
