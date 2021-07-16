package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Singleton

@Singleton
class WCAddonStore {
    suspend fun fetchProductAddons(
        site: SiteModel,
        productID: Long
    ) {

    }
}