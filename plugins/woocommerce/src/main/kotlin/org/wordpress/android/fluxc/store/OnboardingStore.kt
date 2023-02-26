package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.onboarding.OnboardingRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.onboarding.TaskDto
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingStore @Inject constructor(
    private val restClient: OnboardingRestClient,
    private val coroutineEngine: CoroutineEngine,
) {
    private companion object {
        const val ONBOARDING_TASKS_KEY = "setup"
    }

    suspend fun fetchOnboardingTasks(site: SiteModel): WooResult<List<TaskDto>> =
        coroutineEngine.withDefaultContext(API, this, "fetchOnboardingTasks") {
            val response = restClient.fetchOnboardingTasks(site)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> WooResult(
                    response.result
                        .firstOrNull { it.id == ONBOARDING_TASKS_KEY }
                        ?.tasks ?: emptyList()
                )
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
}


