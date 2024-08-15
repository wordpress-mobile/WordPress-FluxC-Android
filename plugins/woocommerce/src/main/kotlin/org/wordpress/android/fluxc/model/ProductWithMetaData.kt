package org.wordpress.android.fluxc.model

data class ProductWithMetaData(
    val product: WCProductModel,
    val metaData: List<WCMetaData> = emptyList()
)
