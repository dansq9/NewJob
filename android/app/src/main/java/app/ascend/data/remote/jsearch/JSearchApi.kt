package app.ascend.data.remote.jsearch

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * JSearch (RapidAPI). Auth headers (X-RapidAPI-Key / X-RapidAPI-Host) are added
 * by an OkHttp interceptor — see NetworkModule.
 */
interface JSearchApi {
    @GET("search")
    suspend fun search(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("num_pages") numPages: Int = 1,
        @Query("date_posted") datePosted: String = "all",   // all | today | 3days | week | month
        @Query("remote_jobs_only") remoteOnly: Boolean? = null,
        @Query("employment_types") employmentTypes: String? = null, // FULLTIME,PARTTIME,CONTRACTOR,INTERN
    ): JSearchResponse
}
