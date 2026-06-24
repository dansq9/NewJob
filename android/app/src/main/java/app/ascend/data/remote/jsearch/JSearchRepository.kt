package app.ascend.data.remote.jsearch

import androidx.core.text.HtmlCompat
import app.ascend.R
import app.ascend.analytics.AnalyticsTracker
import app.ascend.core.Resource
import app.ascend.core.isOffline
import app.ascend.data.model.Job
import app.ascend.data.model.WorkType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class JSearchRepository @Inject constructor(
    private val api: JSearchApi,
    private val analytics: AnalyticsTracker,
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
        } catch (e: HttpException) {
            if (e.code() == 429) {
                Resource.Error("Daily search limit reached", e, rateLimited = true, messageRes = R.string.error_rate_limited)
            } else {
                analytics.recordError(e, mapOf("op" to "job_search", "http" to e.code()))
                Resource.Error("Couldn't load jobs (${e.code()})", e, messageRes = R.string.error_load_jobs)
            }
        } catch (t: Throwable) {
            val offline = t.isOffline()
            // Offline is an expected condition, not a bug — don't spam the crash backend with it.
            if (!offline) analytics.recordError(t, mapOf("op" to "job_search"))
            Resource.Error(
                if (offline) "Offline" else (t.message ?: "Couldn't load jobs"),
                t, offline = offline,
                messageRes = if (offline) R.string.error_offline else R.string.error_load_jobs,
            )
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
        description = description?.cleanHtml(),
        applyUrl = bestApplyLink(),
    )
}

/** JSearch descriptions sometimes contain HTML tags / entities — strip + unescape to plain text. */
private fun String.cleanHtml(): String =
    HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT)
        .toString()
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()

/** Prefer a direct (employer-site) apply link, else the first option, else the default. */
private fun JSearchJob.bestApplyLink(): String? =
    applyOptions.firstOrNull { it.isDirect == true }?.applyLink
        ?: applyOptions.firstOrNull()?.applyLink
        ?: applyLink

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
