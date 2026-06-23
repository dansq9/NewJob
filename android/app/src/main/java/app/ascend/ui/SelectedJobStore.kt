package app.ascend.ui

import app.ascend.data.model.Job
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Lightweight hand-off of the tapped Job to the detail screen (results aren't persisted). */
@Singleton
class SelectedJobStore @Inject constructor() {
    val selected = MutableStateFlow<Job?>(null)
    fun select(job: Job) { selected.value = job }
}
