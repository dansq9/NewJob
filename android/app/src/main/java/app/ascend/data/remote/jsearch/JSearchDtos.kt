package app.ascend.data.remote.jsearch

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JSearchResponse(
    val status: String? = null,
    val data: List<JSearchJob> = emptyList(),
)

/** Subset of the JSearch /search job object that the app consumes. */
@Serializable
data class JSearchJob(
    @SerialName("job_id") val jobId: String? = null,
    @SerialName("job_title") val title: String? = null,
    @SerialName("employer_name") val employer: String? = null,
    @SerialName("employer_logo") val employerLogo: String? = null,
    @SerialName("job_employment_type") val employmentType: String? = null,
    @SerialName("job_apply_link") val applyLink: String? = null,
    @SerialName("job_is_remote") val isRemote: Boolean? = null,
    @SerialName("job_city") val city: String? = null,
    @SerialName("job_state") val state: String? = null,
    @SerialName("job_country") val country: String? = null,
    @SerialName("job_description") val description: String? = null,
    @SerialName("job_posted_at_timestamp") val postedAtTimestamp: Long? = null,
    @SerialName("job_min_salary") val minSalary: Double? = null,
    @SerialName("job_max_salary") val maxSalary: Double? = null,
    @SerialName("job_salary_period") val salaryPeriod: String? = null,
)
