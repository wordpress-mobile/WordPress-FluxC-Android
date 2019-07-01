package org.wordpress.android.fluxc.wc.product

import org.wordpress.android.fluxc.model.WCProductModel

object ProductTestUtils {
    fun generateSampleProduct(
        remoteId: Long,
        type: String = "simple",
        name: String = "",
        virtual: Boolean = false,
        siteId: Int = 6
    ): WCProductModel {
        return WCProductModel().apply {
            remoteProductId = remoteId
            localSiteId = siteId
            this.type = type
            this.name = name
            this.virtual = virtual
        }
    }
}
