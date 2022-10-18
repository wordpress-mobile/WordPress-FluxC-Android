package org.wordpress.android.fluxc.wc.settings

import org.wordpress.android.fluxc.JsonLoaderUtils.jsonFileAs
import org.wordpress.android.fluxc.network.discovery.RootWPAPIRestResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.SiteSettingOptionResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.SiteSettingsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.system.WooSystemRestClient.SSRResponse

object WCSettingsTestUtils {
    fun getSiteSettingsResponse() =
        "wc/site-settings-general-response.json"
            .jsonFileAs(Array<SiteSettingsResponse>::class.java)
            ?.toList()

    fun getSiteProductSettingsResponse() =
        "wc/product-settings-response.json"
            .jsonFileAs(Array<SiteSettingsResponse>::class.java)
            ?.toList()

    fun getSiteSettingOptionResponse() =
        "wc/site-setting-option-response.json"
            .jsonFileAs(SiteSettingOptionResponse::class.java)

    fun getSSRResponse() =
        "wc/system-status.json"
            .jsonFileAs(SSRResponse::class.java)

    fun getSupportedApiVersionResponse() =
        RootWPAPIRestResponse().apply {
            authentication = Authentication()
            namespaces = arrayListOf("wc/v3")
        }

    fun getUnsupportedApiVersionResponse() =
        RootWPAPIRestResponse().apply {
            authentication = Authentication()
            namespaces = arrayListOf("wc/v1")
        }
}
