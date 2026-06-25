package app.ascend.games.engine.games.common

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Creates a game [androidx.lifecycle.AndroidViewModel] with an explicit Application-backed
 * factory.
 *
 * The brain-game VMs take an `Application` in their constructor (they are AndroidViewModels).
 * Resolving them through the bare `viewModel()` default factory off a NavBackStackEntry inside
 * a Hilt host can fail to supply the Application to that constructor, crashing the moment a
 * game screen opens. Passing [ViewModelProvider.AndroidViewModelFactory] explicitly guarantees
 * the Application is provided, so every game screen opens reliably. The VM is still scoped to
 * the current (NavBackStackEntry) ViewModelStore.
 */
@Composable
inline fun <reified VM : ViewModel> gameViewModel(): VM {
    val app = LocalContext.current.applicationContext as Application
    return viewModel(factory = ViewModelProvider.AndroidViewModelFactory(app))
}
