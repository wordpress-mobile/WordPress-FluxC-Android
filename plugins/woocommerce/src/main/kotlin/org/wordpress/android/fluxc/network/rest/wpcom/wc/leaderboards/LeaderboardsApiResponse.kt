package org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards

import android.text.Html
import android.text.SpannableStringBuilder
import android.text.style.URLSpan
import org.wordpress.android.fluxc.network.Response
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse.Type.UNKNOWN

class LeaderboardsApiResponse : Response {
    private val rows: Array<Array<LeaderboardItem>>? = null
    val id: String? = null
    val label: String? = null
    val type: Type
        get() = Type.values()
                .firstOrNull { it.value == id }
                ?: UNKNOWN
    val items
        get() = rows
                ?.firstOrNull()
                ?.toList()

    inner class LeaderboardItem {
        val display: String? = null
        val value: String? = null
        val link
            get() = Html.fromHtml(display)
                    .run { this as? SpannableStringBuilder }
                    ?.spansAsList()
                    ?.firstOrNull()
                    ?.url

        val itemId
            get() = Regex("([?&])([^=]+)=([^&]+)")
                    .takeIf { it.containsMatchIn(link.orEmpty()) }
                    ?.find(link.orEmpty())

        private fun SpannableStringBuilder.spansAsList() =
                getSpans(0, length, URLSpan::class.java)
                        .toList()
    }

    enum class Type(val value: String) {
        CUSTOMERS("customers"),
        COUPONS("coupons"),
        CATEGORIES("categories"),
        PRODUCTS("products"),
        UNKNOWN("unknown")
    }
}
