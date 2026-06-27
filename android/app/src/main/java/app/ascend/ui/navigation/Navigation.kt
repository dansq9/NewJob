package app.ascend.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val JOBS = "jobs"
    const val TRACKER = "tracker"
    const val INTERVIEWS = "interviews"
    const val JOB_DETAIL = "job/{jobId}"
    /** Build a job-detail route for a specific job id. */
    fun jobDetail(jobId: String) = "job/$jobId"
    /** The Resume hub — Build / Edit / Check-for-a-job chooser. */
    const val RESUME = "resume"
    /** The optimizer (score & fix vs a job). Job-Detail deep-links straight here. */
    const val RESUME_OPTIMIZE = "resume/optimize"
    /** Build a new resume — method chooser (voice / guided form / upload). */
    const val RESUME_BUILD = "resume/build"
    /** The guided-form builder (Contact → Summary → Experience → Education → Skills). */
    const val RESUME_BUILD_FORM = "resume/build/form"
    /** Edit a saved resume — the resume library. */
    const val RESUME_EDIT = "resume/edit"
    const val MOCK = "mock"
    const val COPILOT = "copilot"
    const val GAMES = "games"
    const val PAYWALL = "paywall"
    const val SETTINGS = "settings"
}

enum class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    HOME(Routes.HOME, "Home", Icons.Outlined.Home, Icons.Filled.Home),
    JOBS(Routes.JOBS, "Jobs", Icons.Outlined.Work, Icons.Filled.Work),
    TRACKER(Routes.TRACKER, "Tracker", Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
    INTERVIEWS(Routes.INTERVIEWS, "Interviews", Icons.Outlined.GraphicEq, Icons.Filled.GraphicEq),
}
