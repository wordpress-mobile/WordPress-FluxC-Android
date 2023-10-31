package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.woo.WooPaymentsDepositsOverviewApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.woo.WooPaymentsRestClient
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCWooPaymentsStore @Inject constructor(
    private val coroutineEngine: CoroutineEngine,
    private val restClient: WooPaymentsRestClient,
) {
    suspend fun fetchConnectionToken(site: SiteModel): WooPayload<WooPaymentsDepositsOverviewApiResponse> =
        coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchDepositsOverview") {
            restClient.fetchDepositsOverview(site)
        }
}
