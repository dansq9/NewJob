package app.ascend.ui.screens.resume

import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot hand-off of a voice transcript from the voice-capture screen into the guided builder
 * (the two live on different nav entries, so they can't share a ViewModel). Consumed once.
 */
@Singleton
class ResumeDraftStore @Inject constructor() {
    private var pendingTranscript: String? = null

    fun setTranscript(text: String) { pendingTranscript = text }

    /** Returns the pending transcript once, then clears it. */
    fun consumeTranscript(): String? {
        val t = pendingTranscript
        pendingTranscript = null
        return t
    }
}
