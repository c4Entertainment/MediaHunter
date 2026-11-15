package com.c4entertainment.mediahunter.data.network

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

/** Services that need an API key. */
enum class ApiService { Tmdb, Rawg }

/** A request needed a key and there was none: not built in, not set in Settings. */
class MissingApiKeyException(val service: ApiService) : IOException("No $service API key. Add one in Settings.")

/** The service answered 401 or 403, so the key in use was refused. */
class RejectedApiKeyException(val service: ApiService) : IOException("The $service API key was rejected.")

/** The API-key problem behind [this] failure, if that is what it was. */
fun Throwable.apiKeyProblem(): ApiKeyProblem? = when (this) {
    is MissingApiKeyException -> ApiKeyProblem.Missing(service)
    is RejectedApiKeyException -> ApiKeyProblem.Rejected(service)
    else -> null
}

sealed interface ApiKeyProblem {
    val service: ApiService
    data class Missing(override val service: ApiService) : ApiKeyProblem
    data class Rejected(override val service: ApiService) : ApiKeyProblem
}

/**
 * The user's own keys, mirrored into memory by an app-scoped collector so a
 * request only reads a volatile field. Interceptors run on OkHttp's worker
 * threads, so waiting briefly for the very first read never blocks the UI.
 */
@Singleton
class UserApiKeys @Inject constructor(userSettings: UserSettingsRepository) {

    @Volatile
    private var tmdb: String = ""

    @Volatile
    private var rawg: String = ""
    private val loaded = CountDownLatch(1)

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            userSettings.userSettings
                .map { it.tmdbApiKey to it.rawgApiKey }
                .distinctUntilChanged()
                .collect { (tmdbKey, rawgKey) ->
                    tmdb = tmdbKey
                    rawg = rawgKey
                    loaded.countDown()
                }
        }
    }

    fun userKey(service: ApiService): String {
        loaded.await(FIRST_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        return when (service) {
            ApiService.Tmdb -> tmdb
            ApiService.Rawg -> rawg
        }
    }

    private companion object {
        const val FIRST_READ_TIMEOUT_SECONDS = 2L
    }
}

/**
 * Sets the [param] query parameter of every request to the user's own key when
 * they set one in Settings, else to [builtInKey]. Fails with
 * [MissingApiKeyException] when there is no key, and with
 * [RejectedApiKeyException] when the service refuses the one used.
 */
class ApiKeyInterceptor(
    private val service: ApiService,
    private val param: String,
    private val builtInKey: String,
    private val keys: UserApiKeys,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val key = keys.userKey(service).ifEmpty { builtInKey }
        if (key.isEmpty()) throw MissingApiKeyException(service)
        val request = chain.request()
        val url = request.url.newBuilder().setQueryParameter(param, key).build()
        val response = chain.proceed(request.newBuilder().url(url).build())
        if (response.code == 401 || response.code == 403) {
            response.close()
            throw RejectedApiKeyException(service)
        }
        return response
    }
}
