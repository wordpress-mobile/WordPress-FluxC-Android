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
            ?.mapNotNull { productItem ->
                productItem.productId
                        ?.let { productStore.fetchSingleProductSynced(site, it) }
                        ?.parseToWCTopPerformerProductModel(productItem)
            }

    private fun WCProductModel.parseToWCTopPerformerProductModel(
        productItem: LeaderboardProductItem
    ): WCTopPerformerProductModel {
        return WCTopPerformerProductModel(
                hashCode(),
                gson.toJson(this),
                productItem.currency.toString(),
                productItem.quantity?.toIntOrNull() ?: 0,
                productItem.total?.toDoubleOrNull() ?: 0.0
        )
    }
}
