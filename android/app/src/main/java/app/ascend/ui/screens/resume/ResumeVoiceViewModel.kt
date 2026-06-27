package app.ascend.ui.screens.resume

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Stashes a captured voice transcript for the guided builder to pick up. */
@HiltViewModel
class ResumeVoiceViewModel @Inject constructor(
    private val draftStore: ResumeDraftStore,
) : ViewModel() {
    fun stash(text: String) = draftStore.setTranscript(text)
}
