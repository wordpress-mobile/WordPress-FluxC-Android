package org.wordpress.android.fluxc.model.leaderboards

import com.google.gson.Gson
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardProductItem
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class WCProductLeaderboardsMapper @Inject constructor() {
    private val gson = Gson()

    suspend fun map(
        response: LeaderboardsApiResponse,
        site: SiteModel,
        productStore: WCProductStore
    ) = response.products
            ?.takeIf { it.isNotEmpty() }
            ?.mapNotNull { it.productId }
            ?.let { productStore.fetchProductListSynced(site, it) }
            ?.mapNotNull { product ->
                response.products
                        ?.find { it.productId == product.remoteProductId }
                        ?.let { product.toWCTopPerformerProductModel(it, site) }
            }.orEmpty()

    private fun WCProductModel.toWCTopPerformerProductModel(
        productItem: LeaderboardProductItem,
        site: SiteModel
    ) = WCTopPerformerProductModel(
            gson.toJson(this),
            productItem.currency.toString(),
            productItem.quantity?.toIntOrNull() ?: 0,
            productItem.total?.toDoubleOrNull() ?: 0.0,
            site.id
    )
}
