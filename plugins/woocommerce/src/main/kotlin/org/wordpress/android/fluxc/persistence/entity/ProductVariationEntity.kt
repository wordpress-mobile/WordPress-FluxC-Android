package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import org.wordpress.android.fluxc.model.WCProductVariationModel

@Entity(
    tableName = "ProductVariations",
    primaryKeys = ["id", "siteId"]
)
data class ProductVariationEntity(
    val id: Long,
    val productId: Long,
    val siteId: Long,
    val permalink: String? = null,

    val dateCreated: String? = null,
    val dateModified: String? = null,

    val status: String? = null,
    val description: String? = null,
    val sku: String? = null,

    val price: String? = null,
    val regularPrice: String? = null,
    val salePrice: String? = null,
    val isOnSale: Boolean? = null,
    val isPurchasable: Boolean? = null,

    val dateOnSaleFromGmt: String? = null,
    val dateOnSaleToGmt: String? = null,

    val isVirtual: Boolean? = null,
    val isDownloadable: Boolean? = null,
    val downloadLimit: Long? = null,
    val downloadExpiry: Int? = null,

    val taxStatus: String? = null, // taxable, shipping, none
    val taxClass: String? = null,

    val isStockManaged: Boolean? = null,
    val stockQuantity: Double? = null,
    val stockStatus: String? = null, // instock, outofstock, onbackorder

    val backorders: String? = null, // no, notify, yes
    val areBackordersAllowed: Boolean? = null,
    val isBackordered: Boolean? = null,

    val shippingClass: String? = null,
    val shippingClassId: Int? = null,

    val menuOrder: Int? = null,

    val weight: Float? = null,
    val length: Float? = null,
    val width: Float? = null,
    val height: Float? = null,

    val firstImageUrl: String? = null
)

fun WCProductVariationModel.toDataModel(siteId: Long): ProductVariationEntity =
    ProductVariationEntity(
        remoteVariationId,
        remoteProductId,
        siteId,
        permalink,
        dateCreated,
        dateModified,
        status,
        description,
        sku,
        price,
        regularPrice,
        salePrice,
        onSale,
        purchasable,
        dateOnSaleFromGmt,
        dateOnSaleToGmt,
        virtual,
        downloadable,
        downloadLimit,
        downloadExpiry,
        taxStatus,
        taxClass,
        manageStock,
        stockQuantity,
        stockStatus,
        backorders,
        backordersAllowed,
        backordered,
        shippingClass,
        shippingClassId,
        menuOrder,
        weight.toFloatOrNull(),
        length.toFloatOrNull(),
        width.toFloatOrNull(),
        height.toFloatOrNull(),
        getImageModel()?.src
    )
