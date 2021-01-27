package org.wordpress.android.fluxc.model.leaderboards

import com.google.gson.Gson
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardProductItem
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.persistence.ProductSqlUtils.geProductExistsByRemoteId
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import javax.inject.Inject

class WCProductLeaderboardsMapper @Inject constructor() {
    private val gson = Gson()

    suspend fun map(
        response: LeaderboardsApiResponse,
        site: SiteModel,
        productStore: WCProductStore,
        unit: StatsGranularity
    ) = response.products
            ?.takeIf { it.isNotEmpty() }
            ?.mapNotNull { it.productId }
            ?.asProductList(site, productStore)
            ?.mapNotNull { product ->
                response.products
                        ?.find { it.productId == product.remoteProductId }
                        ?.let { product.toWCTopPerformerProductModel(it, site, unit) }
            }.orEmpty()

    /**
     * This method fetch and request all Products from the IDs described by the
     * List<Long>, but it only requests to the site products who doesn't exist
     * inside the database to avoid unnecessary data traffic.
     *
     * Please note that we must request first the local products and second the
     * remote products, if we invert this order the remotely fetched products will
     * be inserted inside the database by the [WCProductStore] natural behavior, and
     * when we fetch the local products after that we will end up unnecessarily duplicating
     * data, since they will be both fetched remotely and locally.
     */
    private suspend fun List<Long>.asProductList(
        site: SiteModel,
        productStore: WCProductStore
    ): List<WCProductModel> {
        val locallyFetchedProducts = this
                .filter { geProductExistsByRemoteId(site, it) }
                .mapNotNull { ProductSqlUtils.getProductByRemoteId(site, it) }

        val remotelyFetchedProducts = this
                .filter { geProductExistsByRemoteId(site, it).not() }
                .takeIf { it.isNotEmpty() }
                ?.let { productStore.fetchProductListSynced(site, it) }
                .orEmpty()

        return mutableListOf<WCProductModel>().apply {
            addAll(remotelyFetchedProducts)
            addAll(locallyFetchedProducts)
        }.toList()
    }

    private fun WCProductModel.toWCTopPerformerProductModel(
        productItem: LeaderboardProductItem,
        site: SiteModel,
        unit: StatsGranularity
    ) = WCTopPerformerProductModel(
            gson.toJson(this),
            productItem.currency.toString(),
            productItem.quantity?.toIntOrNull() ?: 0,
            productItem.total?.toDoubleOrNull() ?: 0.0,
            site.id,
            unit.toString()
    )
}
