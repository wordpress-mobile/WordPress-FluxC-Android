package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.network.rest.wpcom.mobilepay.MobilePayRestClient
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject

class MobilePayStore @Inject constructor(
    private val restClient: MobilePayRestClient,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun createOrder(
        productIdentifier: String,
        priceInCents: Int,
        currency: String,
        purchaseToken: String,
        appId: String,
        siteId: Long,
    ) = coroutineEngine.withDefaultContext(API, this, "createOrder") {
        restClient.createOrder(
            productIdentifier,
            priceInCents,
            currency,
            purchaseToken,
            appId,
            siteId,
        )
    }
}