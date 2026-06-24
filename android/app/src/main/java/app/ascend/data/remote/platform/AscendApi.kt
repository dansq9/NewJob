package app.ascend.data.remote.platform

import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Ascend web-platform API — powers the AI features. Base URL + auth come from
 * BuildConfig.ASCEND_API_BASE_URL and the auth interceptor (NetworkModule).
 *
 * NOTE: endpoint paths are placeholders matching the prototype's flows; align
 * them with the real backend routes when the platform contract is finalised.
 */
interface AscendApi {
    @POST("v1/resume/optimize")
    suspend fun optimizeResume(@Body body: OptimizeRequest): OptimizeResponse

    @POST("v1/resume/generate")
    suspend fun generateResume(@Body body: GenerateRequest): GenerateResponse

    @POST("v1/interview/mock/start")
    suspend fun startMock(@Body body: MockStartRequest): MockStartResponse

    @POST("v1/interview/mock/score")
    suspend fun scoreMock(@Body body: MockScoreRequest): MockScoreResponse

    @POST("v1/interview/copilot/answer")
    suspend fun copilotAnswer(@Body body: CopilotAnswerRequest): CopilotAnswerResponse

    @POST("v1/jobs/match")
    suspend fun matchJobs(@Body body: MatchRequest): MatchResponse
}
