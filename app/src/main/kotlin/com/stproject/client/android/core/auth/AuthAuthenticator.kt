package com.stproject.client.android.core.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthAuthenticator
    @Inject
    constructor(
        private val tokenStore: AuthTokenStore,
        private val refreshCoordinator: AuthRefreshCoordinator,
    ) : Authenticator {
        override fun authenticate(
            route: Route?,
            response: Response,
        ): Request? {
            val request = response.request
            val path = request.url.encodedPath
            if (path.contains("/auth/")) {
                return null
            }
            if (responseCount(response) >= MAX_RETRY_COUNT) {
                return null
            }
            if (request.header(AUTH_RETRY_HEADER) == AUTH_RETRY_VALUE) {
                return null
            }

            val requestAccess =
                request.header("Authorization")
                    ?.removePrefix("Bearer ")
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
            val latestAccess = tokenStore.getAccessToken()
            if (!latestAccess.isNullOrBlank() && latestAccess != requestAccess) {
                return request.newBuilder()
                    .header("Authorization", "Bearer $latestAccess")
                    .header(AUTH_RETRY_HEADER, AUTH_RETRY_VALUE)
                    .build()
            }

            val refreshed =
                runBlocking {
                    refreshCoordinator.refreshIfNeeded(requestAccess)
                } ?: return null

            return request.newBuilder()
                .header("Authorization", "Bearer ${refreshed.accessToken}")
                .header(AUTH_RETRY_HEADER, AUTH_RETRY_VALUE)
                .build()
        }

        private fun responseCount(response: Response): Int {
            var result = 1
            var prior = response.priorResponse
            while (prior != null) {
                result += 1
                prior = prior.priorResponse
            }
            return result
        }

        private companion object {
            private const val MAX_RETRY_COUNT = 2
            private const val AUTH_RETRY_HEADER = "X-Auth-Retry"
            private const val AUTH_RETRY_VALUE = "1"
        }
    }
