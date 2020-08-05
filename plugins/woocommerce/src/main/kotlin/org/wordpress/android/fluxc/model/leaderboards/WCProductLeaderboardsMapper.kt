package org.wordpress.android.fluxc.model.leaderboards

import com.google.gson.Gson
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardProductItem
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.persistence.ProductSqlUtils.geProductExistsByRemoteId
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
            ?.asProductList(site, productStore)
            ?.mapNotNull { product ->
                response.products
                        ?.find { it.productId == product.remoteProductId }
                        ?.let { product.toWCTopPerformerProductModel(it, site) }
            }.orEmpty()

    private suspend fun List<Long>.asProductList(
        site: SiteModel,
        productStore: WCProductStore
    ): List<WCProductModel> {
        val remotelyFetchedProducts = this
                .filter { geProductExistsByRemoteId(site, it).not() }
                .takeIf { it.isNotEmpty() }
                ?.let { productStore.fetchProductListSynced(site, it) }
                .orEmpty()

        val locallyFetchedProducts = this
                .filter { geProductExistsByRemoteId(site, it) }
                .mapNotNull { ProductSqlUtils.getProductByRemoteId(site, it) }

        return mutableListOf<WCProductModel>().apply {
            addAll(remotelyFetchedProducts)
            addAll(locallyFetchedProducts)
        }.toList()
    }

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
