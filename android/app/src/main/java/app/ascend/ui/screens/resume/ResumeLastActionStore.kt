package app.ascend.ui.screens.resume

import javax.inject.Inject
import javax.inject.Singleton

/** Which resume action the user last entered — powers the hub's "Continue where you left off" chip. */
enum class ResumeAction { OPTIMIZE, BUILD, EDIT }

/**
 * Remembers the last resume action this session (in-memory; resets on cold start). The hub reads it
 * to offer a one-tap "continue" chip WITHOUT replacing the 3-card hub (per the persona panel: the
 * hub stays the default landing; the chip is additive, and never deep-links into a single resume).
 */
@Singleton
class ResumeLastActionStore @Inject constructor() {
    var last: ResumeAction? = null
        private set

    fun mark(action: ResumeAction) { last = action }
}
