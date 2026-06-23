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
    const val JOB_DETAIL = "job"
    const val RESUME = "resume"
    const val MOCK = "mock"
    const val COPILOT = "copilot"
    const val GAMES = "games"
    const val PAYWALL = "paywall"
}

enum class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    HOME(Routes.HOME, "Home", Icons.Outlined.Home, Icons.Filled.Home),
    JOBS(Routes.JOBS, "Jobs", Icons.Outlined.Work, Icons.Filled.Work),
    TRACKER(Routes.TRACKER, "Tracker", Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
    INTERVIEWS(Routes.INTERVIEWS, "Interviews", Icons.Outlined.GraphicEq, Icons.Filled.GraphicEq),
}
