package org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards

import android.annotation.TargetApi
import android.os.Build
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.style.URLSpan
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse.LeaderboardItemRow
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse.Type.PRODUCTS

class LeaderboardProductItem(
    private val itemRows: Array<LeaderboardItemRow>? = null
) {
    val quantity
        get() = itemRows
                ?.second()
                ?.value

    val total
        get() = itemRows
                ?.third()
                ?.value

    val currency by lazy {
        priceAmountHtmlTag
                ?.split(">")
                ?.firstOrNull { it.contains("&#") }
                ?.split(";")
                ?.filter { it.contains("&#") }
                ?.reduce { total, new -> "$total$new" }
                ?.run { fromHtmlWithSafeApiCall(this) }
    }

    val productId by lazy {
        link
                ?.split("&")
                ?.firstOrNull { it.contains("${PRODUCTS.value}=", true) }
                ?.split("=")
                ?.last()
                ?.toLongOrNull()
    }

    private val link by lazy {
        fromHtmlWithSafeApiCall(itemHtmlTag)
                .run { this as? SpannableStringBuilder }
                ?.spansAsList()
                ?.firstOrNull()
                ?.url
    }

    private val itemHtmlTag by lazy {
        itemRows
                ?.first()
                ?.display
    }

    private val priceAmountHtmlTag by lazy {
        itemRows
                ?.third()
                ?.display
    }

    fun SpannableStringBuilder.spansAsList() =
            getSpans(0, length, URLSpan::class.java)
                    .toList()

    @TargetApi(Build.VERSION_CODES.N)
    private fun fromHtmlWithSafeApiCall(source: String?) = source
            ?.takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.N }?.let {
                Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY)
            } ?: Html.fromHtml(source)

    private fun Array<LeaderboardItemRow>.second() =
            takeIf { isNotEmpty() && size > 1 }
                    ?.let { this[1] }

    private fun Array<LeaderboardItemRow>.third() =
            takeIf { isNotEmpty() && size > 2 }
                    ?.let { this[2] }
}
