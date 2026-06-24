package app.ascend.di

import app.ascend.BuildConfig
import app.ascend.data.remote.jsearch.JSearchApi
import app.ascend.data.remote.platform.AscendApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides @Singleton
    fun logging(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    /** RapidAPI/JSearch client — injects the RapidAPI auth headers on every call. */
    @Provides @Singleton @Named("jsearch")
    fun jsearchClient(logging: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val req = chain.request().newBuilder()
                    .addHeader("X-RapidAPI-Key", BuildConfig.RAPIDAPI_KEY)
                    .addHeader("X-RapidAPI-Host", BuildConfig.JSEARCH_HOST)
                    .build()
                chain.proceed(req)
            })
            .addInterceptor(logging)
            .build()

    /** Ascend platform client — Bearer auth (token wired from session store later). */
    @Provides @Singleton @Named("platform")
    fun platformClient(logging: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                // TODO: pull the signed-in user's token from a session/DataStore.
                val token: String? = null
                val builder = chain.request().newBuilder()
                if (token != null) builder.addHeader("Authorization", "Bearer $token")
                chain.proceed(builder.build())
            })
            .addInterceptor(logging)
            .build()

    @Provides @Singleton
    fun jsearchApi(@Named("jsearch") client: OkHttpClient, json: Json): JSearchApi =
        Retrofit.Builder()
            .baseUrl("https://${BuildConfig.JSEARCH_HOST}/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(JSearchApi::class.java)

    @Provides @Singleton
    fun ascendApi(@Named("platform") client: OkHttpClient, json: Json): AscendApi =
        Retrofit.Builder()
            .baseUrl(BuildConfig.ASCEND_API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AscendApi::class.java)
}
