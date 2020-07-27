package org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards

import android.text.Html
import android.text.SpannableStringBuilder
import android.text.style.URLSpan
import org.wordpress.android.fluxc.network.Response
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse.Type.UNKNOWN

class LeaderboardsApiResponse : Response {
    private val rows: Array<Array<LeaderboardItemRow>>? = null
    val id: String? = null
    val label: String? = null
    val type: Type
        get() = Type.values()
                .firstOrNull { it.value == id }
                ?: UNKNOWN
    val items by lazy {
        rows
                ?.map { LeaderboardItem(it) }
                ?.toList()
    }

    class LeaderboardItem(
        val itemRows: Array<LeaderboardItemRow>? = null
    ) {
        val itemHtmlTag by lazy {
            itemRows
                    ?.first()
                    ?.display
        }

        val itemName by lazy {
            itemRows
                    ?.first()
                    ?.value
        }

        //TODO: fix deprecation issue
        val link by lazy {
            Html.fromHtml(itemHtmlTag)
                    .run { this as? SpannableStringBuilder }
                    ?.spansAsList()
                    ?.firstOrNull()
                    ?.url
        }

        fun resolveItemIdByType(type: Type) = link
                ?.split("&")
                ?.firstOrNull { it.contains("${type.value}=", true) }
                ?.split("=")
                ?.last()
                ?.toLongOrNull()

        private fun SpannableStringBuilder.spansAsList() =
                getSpans(0, length, URLSpan::class.java)
                        .toList()
    }

    class LeaderboardItemRow {
        val display: String? = null
        val value: String? = null
    }

    enum class Type(val value: String) {
        CUSTOMERS("customers"),
        COUPONS("coupons"),
        CATEGORIES("categories"),
        PRODUCTS("products"),
        UNKNOWN("unknown")
    }
}
