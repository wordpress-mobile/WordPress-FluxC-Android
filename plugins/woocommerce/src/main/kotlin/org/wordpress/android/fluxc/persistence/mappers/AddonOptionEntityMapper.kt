package org.wordpress.android.fluxc.persistence.mappers

import org.wordpress.android.fluxc.model.addons.WCProductAddonModel.ProductAddonOption
import org.wordpress.android.fluxc.persistence.entity.AddonOptionEntity

internal fun ProductAddonOption.toAddonOptionEntity(addonLocalId: Long): AddonOptionEntity {
    return AddonOptionEntity(
            addonLocalId = addonLocalId,
            priceType = this.priceType?.toLocalEntity(),
            label = this.label,
            price = this.price,
            image = this.image
    )
}
