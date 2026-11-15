package com.c4entertainment.mediahunter.data.network

import com.c4entertainment.mediahunter.BuildConfig
import com.c4entertainment.mediahunter.domain.repository.UserSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Thrown when a TMDB request has no key at all: none built in and none set in Settings. */
class MissingTmdbKeyException : IOException("No TMDB API key. Add one in Settings.")

/**
 * Sets the `api_key` query parameter of every TMDB request to the user's own
 * key when they set one in Settings, else to the key built into the app.
 *
 * The setting is mirrored into memory by an app-scoped collector, so a request
 * only reads a volatile field. Interceptors run on OkHttp's worker threads, so
 * waiting briefly for the very first read of the setting never blocks the UI.
 */
@Singleton
class TmdbApiKeyInterceptor @Inject constructor(
    userSettings: UserSettingsRepository,
) : Interceptor {

    @Volatile
    private var userKey: String = ""
    private val loaded = CountDownLatch(1)

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            userSettings.userSettings
                .map { it.tmdbApiKey }
                .distinctUntilChanged()
                .collect {
                    userKey = it
                    loaded.countDown()
                }
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        loaded.await(FIRST_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        val key = userKey.ifEmpty { BuildConfig.TMDB_API_KEY }
        if (key.isEmpty()) throw MissingTmdbKeyException()
        val request = chain.request()
        val url = request.url.newBuilder().setQueryParameter("api_key", key).build()
        return chain.proceed(request.newBuilder().url(url).build())
    }

    private companion object {
        const val FIRST_READ_TIMEOUT_SECONDS = 2L
    }
}
