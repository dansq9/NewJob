package app.ascend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.ascend.ui.AppStart
import app.ascend.ui.AppViewModel
import app.ascend.ui.navigation.Routes
import app.ascend.ui.navigation.Tab
import app.ascend.ui.screens.copilot.CopilotScreen
import app.ascend.ui.screens.games.GamesScreen
import app.ascend.ui.screens.home.HomeScreen
import app.ascend.ui.screens.interviews.InterviewsScreen
import app.ascend.ui.screens.jobdetail.JobDetailScreen
import app.ascend.ui.screens.jobs.JobsScreen
import app.ascend.ui.screens.mock.MockScreen
import app.ascend.ui.screens.onboarding.OnboardingScreen
import app.ascend.ui.screens.paywall.PaywallScreen
import app.ascend.ui.screens.resume.ResumeScreen
import app.ascend.ui.screens.tracker.TrackerScreen
import app.ascend.ui.theme.AscendColors
import app.ascend.ui.theme.AscendTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { appViewModel.start.value is AppStart.Loading }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AscendTheme {
                val state by appViewModel.start.collectAsStateWithLifecycle()
                when (val s = state) {
                    AppStart.Loading -> Unit // splash stays
                    is AppStart.Ready -> AscendRoot(startOnboarding = !s.onboarded)
                }
            }
        }
    }
}

@Composable
private fun AscendRoot(startOnboarding: Boolean) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBars = currentRoute in Tab.entries.map { it.route }

    Scaffold(
        containerColor = AscendColors.Bg,
        // Tab screens add their own status-bar padding; full-screen routes manage
        // their own insets via their Scaffold. So the outer Scaffold only contributes
        // the bottom-nav height (when shown) and doesn't double-inset content.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { if (showBars) AscendBottomBar(nav, currentRoute) },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = if (startOnboarding) Routes.ONBOARDING else Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(onDone = {
                    nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                })
            }
            composable(Routes.HOME) { HomeScreen(nav) }
            composable(Routes.JOBS) { JobsScreen(nav) }
            composable(Routes.TRACKER) { TrackerScreen(nav) }
            composable(Routes.INTERVIEWS) { InterviewsScreen(nav) }
            composable(Routes.JOB_DETAIL) { JobDetailScreen(nav) }
            composable(Routes.RESUME) { ResumeScreen(nav) }
            composable(Routes.MOCK) { MockScreen(nav) }
            composable(Routes.COPILOT) { CopilotScreen(nav) }
            composable(Routes.GAMES) { GamesScreen(nav) }
            composable(Routes.PAYWALL) { PaywallScreen(onClose = { nav.popBackStack() }) }
        }
    }
}

@Composable
private fun AscendBottomBar(nav: androidx.navigation.NavController, currentRoute: String?) {
    NavigationBar(containerColor = AscendColors.Card, tonalElevation = 0.dp) {
        Tab.entries.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    nav.navigate(tab.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(if (selected) tab.selectedIcon else tab.icon, tab.label) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AscendColors.Indigo,
                    selectedTextColor = AscendColors.Indigo,
                    indicatorColor = AscendColors.ChipIndigo,
                    unselectedIconColor = AscendColors.Muted2,
                    unselectedTextColor = AscendColors.Muted2,
                ),
            )
        }
    }
}
