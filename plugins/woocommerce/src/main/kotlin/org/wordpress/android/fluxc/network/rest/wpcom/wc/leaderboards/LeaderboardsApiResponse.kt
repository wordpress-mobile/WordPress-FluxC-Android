package org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards

import org.wordpress.android.fluxc.network.Response

class LeaderboardsApiResponse : Response {
    val id: String? = null
    val label: String? = null
    val rows: Array<Array<LeaderboardItem>>? = null

    class LeaderboardItem {
        val display: String? = null
        val value: String? = null
    }
}
