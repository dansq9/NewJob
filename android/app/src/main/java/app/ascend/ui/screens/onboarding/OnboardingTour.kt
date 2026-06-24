package app.ascend.ui.screens.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ascend.R
import app.ascend.ui.theme.AscendColors

/** Tour card content — built ONLY from already-localized strings (no new translation keys). */
private data class TourCard(@StringRes val title: Int, @StringRes val body: Int)

private val TOUR_CARDS = listOf(
    TourCard(R.string.onboarding_welcome_title, R.string.onboarding_welcome_sub),
    TourCard(R.string.home_qa_resume, R.string.home_qa_resume_sub),
    TourCard(R.string.home_qa_copilot, R.string.home_qa_copilot_sub),
    TourCard(R.string.home_qa_mock, R.string.home_qa_mock_sub),
    TourCard(R.string.home_qa_games, R.string.home_qa_games_sub),
)

/**
 * Full-screen onboarding tour overlay. Shows the first [cardCount] localized cards one at a
 * time. Honest branded content only — no fake CTA / reward copy. Skip is offered only when
 * [showSkip] && ![forceCompletion]. Resolves via [onResolve] on completion or skip; callers
 * then continue onboarding. Never blocks: if [cardCount] <= 0 it resolves immediately.
 */
@Composable
fun OnboardingTour(
    cardCount: Int,
    showSkip: Boolean,
    forceCompletion: Boolean,
    onView: (cardIndex: Int) -> Unit,
    onSkip: (cardIndex: Int) -> Unit,
    onComplete: (cardsSeen: Int) -> Unit,
    onResolve: () -> Unit,
) {
    val total = cardCount.coerceIn(0, TOUR_CARDS.size)
    if (total <= 0) {
        LaunchedEffect(Unit) { onResolve() }
        return
    }
    var index by rememberSaveable { mutableIntStateOf(0) }
    val card = TOUR_CARDS[index]
    LaunchedEffect(index) { onView(index + 1) }   // card_index is 1-based

    Surface(Modifier.fillMaxSize(), color = AscendColors.Bg) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp)) {
            // Skip (top-right) only when allowed.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (showSkip && !forceCompletion) {
                    TextButton(onClick = { onSkip(index + 1); onResolve() }) {
                        Text(stringResource(R.string.action_skip), color = AscendColors.Muted2)
                    }
                } else {
                    Spacer(Modifier.height(48.dp))
                }
            }
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(card.title), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                    color = AscendColors.Ink, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(card.body), fontSize = 15.sp, color = AscendColors.Muted,
                    textAlign = TextAlign.Center, lineHeight = 22.sp)
            }
            // Progress dots
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(total) { i ->
                    Box(
                        Modifier.padding(horizontal = 4.dp).size(8.dp).clip(RoundedCornerShape(4.dp))
                            .background(if (i == index) AscendColors.Indigo else AscendColors.Line),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (index < total - 1) index++ else { onComplete(total); onResolve() }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp).navigationBarsPadding(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo),
            ) {
                Text(
                    stringResource(if (index < total - 1) R.string.action_continue else R.string.action_done),
                    fontWeight = FontWeight.Bold, fontSize = 16.sp,
                )
            }
        }
    }
}
