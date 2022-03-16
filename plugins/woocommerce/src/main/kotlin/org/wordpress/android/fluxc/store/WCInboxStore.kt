package org.wordpress.android.fluxc.store

import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.inbox.InboxNoteDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.inbox.InboxRestClient
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCInboxStore @Inject constructor(
    private val restClient: InboxRestClient,
    private val coroutineEngine: CoroutineEngine
) {
    companion object {
        const val DEFAULT_PAGE_SIZE = 50
        const val DEFAULT_PAGE = 1
    }

    suspend fun fetchNotes(
        site: SiteModel,
        page: Int = CouponStore.DEFAULT_PAGE,
        pageSize: Int = CouponStore.DEFAULT_PAGE_SIZE
    ): WooResult<List<InboxNoteDto>> {
        return coroutineEngine.withDefaultContext(API, this, "fetchCoupons") {
            val response = restClient.fetchNotes(site, page, pageSize)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    WooResult(response.result.toList())
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }
}
