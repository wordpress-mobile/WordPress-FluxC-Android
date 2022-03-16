package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

data class ProductCategoryDto(
    val id: Long = 0L,
    val name: String? = null,
    val slug: String? = null,
    val description: String? = null,
    val count: Int = 0
)
