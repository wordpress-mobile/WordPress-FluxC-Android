package org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards

import org.wordpress.android.fluxc.network.Response
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse.Type.CATEGORIES
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse.Type.COUPONS
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse.Type.CUSTOMERS
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse.Type.PRODUCTS

class LeaderboardsApiResponse : Response {
    private val rows: Array<Array<LeaderboardItem>>? = null
    val id: String? = null
    val label: String? = null
    val type: Type?
        get() = when (id) {
            "customers" -> CUSTOMERS
            "coupons" -> COUPONS
            "categories" -> CATEGORIES
            "products" -> PRODUCTS
            else -> null
        }
    val items
        get() = rows
                ?.firstOrNull()
                ?.toList()

    class LeaderboardItem {
        val display: String? = null
        val value: String? = null
    }

    enum class Type {
        CUSTOMERS,
        COUPONS,
        CATEGORIES,
        PRODUCTS
    }
}
