package org.wordpress.android.fluxc.wc.settings

import org.wordpress.android.fluxc.JsonLoaderUtils.jsonFileAs
import org.wordpress.android.fluxc.network.rest.wpcom.wc.SiteSettingsResponse

object WCSettingsTestUtils {
    fun getSiteSettingsResponse() =
        "wc/site-settings-general-response.json"
            .jsonFileAs(Array<SiteSettingsResponse>::class.java)
            ?.toList()

    fun getSiteProductSettingsResponse() =
        "wc/product-settings-response.json"
            .jsonFileAs(Array<SiteSettingsResponse>::class.java)
            ?.toList()
}
