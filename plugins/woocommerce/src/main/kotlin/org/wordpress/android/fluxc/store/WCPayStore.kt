package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.network.rest.wpcom.wc.pay.PayRestClient
import org.wordpress.android.fluxc.tools.CoroutineEngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCPayStore @Inject constructor(
    private val coroutineEngine: CoroutineEngine,
    private val payRestClient: PayRestClient
)
