package org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards

import android.annotation.TargetApi
import android.os.Build
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
        private val itemRows: Array<LeaderboardItemRow>? = null
    ) {
        private val itemHtmlTag by lazy {
            itemRows
                    ?.first()
                    ?.display
        }

        val link by lazy {
            fromHtmlWithSafeApiCall(itemHtmlTag)
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

        @TargetApi(Build.VERSION_CODES.N)
        private fun fromHtmlWithSafeApiCall(source: String?) = source
                ?.takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.N }?.let {
                    Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY)
                } ?: Html.fromHtml(source)
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
