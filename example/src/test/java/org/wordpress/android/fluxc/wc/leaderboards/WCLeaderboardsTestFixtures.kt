package org.wordpress.android.fluxc.wc.leaderboards

import com.google.gson.Gson
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductApiResponse

object WCLeaderboardsTestFixtures {
    val stubSite = SiteModel().apply { id = 321 }

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

    private fun String.asProductModelJsonString() = Gson().let { gson ->
        UnitTestUtils.getStringFromResourceFile(this@WCLeaderboardsTestFixtures.javaClass, this)
                ?.let { gson.fromJson(it, ProductApiResponse::class.java) }
                ?.asProductModel()
                ?.let { gson.toJson(it) }
                .orEmpty()
    }

    val generateStubbedProductIdList = listOf(14L, 22L, 15L)

    private val stubbedProductJsonString by lazy {
        Gson().let { gson ->
            "wc/top-performer-product-1.json".asProductModelJsonString()
        }
    }

    val stubbedTopPerformersList by lazy {
        listOf(
                WCTopPerformerProductModel(
                        "wc/top-performer-product-1.json".asProductModelJsonString(),
                        "currency 0",
                        0,
                        0.0,
                        321,
                        "DAYS"
                ),
                WCTopPerformerProductModel(
                        "wc/top-performer-product-2.json".asProductModelJsonString(),
                        "currency 1",
                        1,
                        1.0,
                        321,
                        "DAYS"
                ),
                WCTopPerformerProductModel(
                        "wc/top-performer-product-3.json".asProductModelJsonString(),
                        "currency 2",
                        2,
                        2.0,
                        321,
                        "DAYS"
                )
        )
    }

    val duplicatedTopPerformersList by lazy {
        listOf(
                WCTopPerformerProductModel(
                        stubbedProductJsonString,
                        "currency 0",
                        0,
                        0.0,
                        321,
                        "DAYS"
                ),
                WCTopPerformerProductModel(
                        stubbedProductJsonString,
                        "currency 0",
                        0,
                        0.0,
                        321,
                        "DAYS"
                ),
                WCTopPerformerProductModel(
                        stubbedProductJsonString,
                        "currency 0",
                        0,
                        0.0,
                        321,
                        "DAYS"
                )
        )
    }
}
