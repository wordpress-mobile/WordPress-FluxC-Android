package org.wordpress.android.fluxc.wc.leaderboards

import com.google.gson.Gson
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse

object WCLeaderboardsTestFixtures {
    fun generateSampleShippingLabelApiResponse() =
            UnitTestUtils
                    .getStringFromResourceFile(this.javaClass, "wc/leaderboards-response-example.json")
                    ?.run { Gson().fromJson(this, Array<LeaderboardsApiResponse>::class.java) }

    val stubbedTopPerformersList by lazy {
        listOf(
                WCTopPerformerProductModel(
                        0,
                        "info 0",
                        "currency 0",
                        0,
                        0.0
                ),
                WCTopPerformerProductModel(
                        1,
                        "info 1",
                        "currency 1",
                        1,
                        1.0
                ),
                WCTopPerformerProductModel(
                        2,
                        "info 2",
                        "currency 2",
                        2,
                        2.0
                )
        )
    }
}
