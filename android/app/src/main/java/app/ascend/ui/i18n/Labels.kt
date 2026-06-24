package app.ascend.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.ascend.R
import app.ascend.data.model.TrackStage
import app.ascend.data.model.WorkType

/** Localized display label for a [WorkType] (UNKNOWN renders empty, matching the old behavior). */
@Composable
fun label(workType: WorkType): String = when (workType) {
    WorkType.REMOTE -> stringResource(R.string.worktype_remote)
    WorkType.HYBRID -> stringResource(R.string.worktype_hybrid)
    WorkType.ONSITE -> stringResource(R.string.worktype_onsite)
    WorkType.UNKNOWN -> ""
}

/** Localized display label for a pipeline [TrackStage]. */
@Composable
fun label(stage: TrackStage): String = stringResource(
    when (stage) {
        TrackStage.SAVED -> R.string.stage_saved
        TrackStage.APPLIED -> R.string.stage_applied
        TrackStage.INTERVIEW -> R.string.stage_interview
        TrackStage.OFFER -> R.string.stage_offer
        TrackStage.CLOSED -> R.string.stage_closed
    }
)
