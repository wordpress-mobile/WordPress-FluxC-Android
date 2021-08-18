package org.wordpress.android.fluxc.domain

import org.wordpress.android.fluxc.domain.Addon.HasAdjustablePrice.Price

data class AddonOption(
    val price: Price.Adjusted,
    val label: String?,
    val image: String?
)
