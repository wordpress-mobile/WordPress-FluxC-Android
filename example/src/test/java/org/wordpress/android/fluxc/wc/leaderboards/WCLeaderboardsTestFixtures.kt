package org.wordpress.android.fluxc.wc.leaderboards

import com.google.gson.Gson
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.reports.ReportsProductApiResponse

object WCLeaderboardsTestFixtures {
    val stubSite = SiteModel().apply {
        id = 321
        origin = SiteModel.ORIGIN_XMLRPC
    }

    fun generateSampleProductList() = listOf(
            "wc/top-performer-product-1.json",
            "wc/top-performer-product-2.json",
            "wc/top-performer-product-3.json"
    )
            .mapNotNull { fileName ->
                UnitTestUtils.getStringFromResourceFile(this.javaClass, fileName)
                        ?.let { Gson().fromJson(it, ProductApiResponse::class.java) }
            }.map { it.asProductModel() }

    fun generateSampleLeaderboardsApiResponse() =
            UnitTestUtils
                    .getStringFromResourceFile(this.javaClass, "wc/leaderboards-response-example.json")
                    ?.run { Gson().fromJson(this, Array<LeaderboardsApiResponse>::class.java) }

    fun generateSampleTopPerformerApiResponse() =
        UnitTestUtils
            .getStringFromResourceFile(this.javaClass, "wc/reports-product-response-example.json")
            ?.run { Gson().fromJson(this, Array<ReportsProductApiResponse>::class.java) }

    val generateStubbedProductIdList = listOf(14L, 22L, 15L)
}
