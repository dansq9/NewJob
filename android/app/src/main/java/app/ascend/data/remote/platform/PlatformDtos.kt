package app.ascend.data.remote.platform

import kotlinx.serialization.Serializable

/**
 * Wire models for the Ascend web-platform API. These mirror the prototype's
 * feature data; adjust field names to match the real backend once finalised.
 */

// ---- Resume optimize (ATS) ----
@Serializable data class OptimizeRequest(val resumeId: String? = null, val jobId: String, val jobDescription: String? = null)
@Serializable data class OptimizeIssue(val title: String, val detail: String, val severity: String, val resolved: Boolean = false)
@Serializable data class OptimizeResponse(
    val atsScore: Int,
    val verdict: String,
    val verdictDetail: String,
    val issues: List<OptimizeIssue> = emptyList(),
    val optimizedScore: Int? = null,
    val downloadUrl: String? = null,
)

// ---- Resume generate ----
@Serializable data class GenerateRequest(val method: String, val transcript: String? = null, val fields: Map<String, String> = emptyMap())
@Serializable data class GenerateResponse(val resumeId: String, val downloadUrl: String? = null, val summary: String? = null)

// ---- Mock interview ----
@Serializable data class MockQuestion(val id: String, val prompt: String, val tag: String, val difficulty: String)
@Serializable data class MockStartRequest(val role: String, val company: String? = null, val count: Int = 5, val jobDescription: String? = null)
@Serializable data class MockStartResponse(val sessionId: String, val questions: List<MockQuestion>)
@Serializable data class MockAnswer(val questionId: String, val text: String)
@Serializable data class MockScoreRequest(val sessionId: String, val answers: List<MockAnswer>)
@Serializable data class MockAreaScore(val area: String, val score: Int)
@Serializable data class MockScoreResponse(
    val averageScore: Int,
    val areas: List<MockAreaScore> = emptyList(),
    val strengths: String? = null,
    val focusAreas: String? = null,
)

// ---- Live interview copilot ----
@Serializable data class CopilotContext(val role: String, val company: String? = null, val jobDescription: String? = null, val resumeId: String? = null)
@Serializable data class CopilotAnswerRequest(val context: CopilotContext, val question: String)
@Serializable data class CopilotSection(val label: String, val text: String)
@Serializable data class CopilotAnswerResponse(val question: String, val sections: List<CopilotSection>)

// ---- Job match scoring (optional, augments JSearch results) ----
@Serializable data class MatchRequest(val resumeId: String? = null, val jobIds: List<String>)
@Serializable data class MatchScore(val jobId: String, val matchPercent: Int)
@Serializable data class MatchResponse(val scores: List<MatchScore> = emptyList())
