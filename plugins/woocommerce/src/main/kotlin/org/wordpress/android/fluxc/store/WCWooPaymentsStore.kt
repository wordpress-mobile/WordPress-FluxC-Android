package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverview
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.woo.WooPaymentsRestClient
import org.wordpress.android.fluxc.persistence.dao.WooPaymentsDepositsOverviewDao
import org.wordpress.android.fluxc.persistence.mappers.WooPaymentsDepositsOverviewMapper
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCWooPaymentsStore @Inject constructor(
    private val coroutineEngine: CoroutineEngine,
    private val restClient: WooPaymentsRestClient,
    private val dao: WooPaymentsDepositsOverviewDao,
    private val mapper: WooPaymentsDepositsOverviewMapper
) {
    suspend fun fetchDepositsOverview(site: SiteModel): WooPayload<WooPaymentsDepositsOverview> =
        coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchDepositsOverview") {
            val result = restClient.fetchDepositsOverview(site)
            if (result.result != null) {
                WooPayload(mapper.mapApiResponseToModel(result.result))
            } else {
                WooPayload(result.error)
            }
        }

    suspend fun getDepositsOverviewAll(site: SiteModel): WooPaymentsDepositsOverview? =
        coroutineEngine.withDefaultContext(AppLog.T.API, this, "getDepositsOverviewAll") {
            val result = dao.get(site.localId())
            if (result != null) {
                mapper.mapEntityToModel(result)
            } else {
                null
            }
        }

    suspend fun insertDepositsOverview(
        site: SiteModel,
        depositsOverview: WooPaymentsDepositsOverview
    ) = coroutineEngine.withDefaultContext(AppLog.T.API, this, "insertDepositsOverview") {
        dao.insert(mapper.mapModelToEntity(depositsOverview, site))
    }

    suspend fun deleteDepositsOverview(site: SiteModel) =
        coroutineEngine.withDefaultContext(AppLog.T.API, this, "deleteDepositsOverview") {
            dao.delete(site.localId())
        }
}