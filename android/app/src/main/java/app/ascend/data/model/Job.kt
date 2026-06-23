package app.ascend.data.model

/** Work arrangement, mirrors the prototype's coloured work pill. */
enum class WorkType(val label: String) { REMOTE("Remote"), HYBRID("Hybrid"), ONSITE("On-site"), UNKNOWN("") }

/** Pipeline stage for the Job Tracker. */
enum class TrackStage(val label: String) {
    SAVED("Saved"), APPLIED("Applied"), INTERVIEW("Interview"), OFFER("Offer"), CLOSED("Closed");
    companion object { fun from(name: String?) = entries.firstOrNull { it.name == name } }
}

/** Domain job, decoupled from the JSearch wire format. */
data class Job(
    val id: String,
    val title: String,
    val company: String,
    val logoUrl: String?,
    val location: String,
    val workType: WorkType,
    val employmentType: String?,    // "Full-time", "Contract", …
    val salary: String?,            // pre-formatted, e.g. "$155k – $185k"
    val postedAgo: String?,         // "2d ago"
    val description: String?,
    val applyUrl: String?,
    val matchPercent: Int? = null,  // supplied by the Ascend platform API when available
)
