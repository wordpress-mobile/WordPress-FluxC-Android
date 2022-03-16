package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

data class ProductTagDto(
    val id: Long = 0L,
    val name: String? = null,
    val slug: String? = null,
    var parent: Long? = null
)
