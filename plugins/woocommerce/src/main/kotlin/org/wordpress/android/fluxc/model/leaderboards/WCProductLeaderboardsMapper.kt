package org.wordpress.android.fluxc.model.leaderboards

import com.google.gson.Gson
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse.Type.PRODUCTS
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class WCProductLeaderboardsMapper @Inject constructor(

) {
    suspend fun map(
        response: LeaderboardsApiResponse,
        site: SiteModel,
        productStore: WCProductStore
    ) = response.takeIf { it.type == PRODUCTS }
            ?.items
            ?.mapNotNull { it.resolveItemIdByType(PRODUCTS) }
            ?.run { productStore.fetchProductListSynced(site, this) }
            ?.let { WCProductLeaderboardsModel(it.hashCode(), Gson().toJson(it)) }
}
