package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import org.wordpress.android.fluxc.model.WCProductModel

@Entity(
    tableName = "Products",
    primaryKeys = ["id", "siteId"]
)
data class ProductEntity(
    val id: Long,
    val siteId: Long,
    val name: String? = null,
    val slug: String? = null,
    val permalink: String? = null,

    val dateCreated: String? = null,
    val dateModified: String? = null,

    val type: String? = null, // simple, grouped, external, variable
    val status: String? = null,
    val isFeatured: Boolean? = null,
    val catalogVisibility: String? = null, // visible, catalog, search, hidden
    val description: String? = null,
    val shortDescription: String? = null,
    val sku: String? = null,

    val price: String? = null,
    val regularPrice: String? = null,
    val salePrice: String? = null,
    val isOnSale: Boolean? = null,
    val totalSales: Long? = null,
    val isPurchasable: Boolean? = null,

    val dateOnSaleFrom: String? = null,
    val dateOnSaleTo: String? = null,
    val dateOnSaleFromGmt: String? = null,
    val dateOnSaleToGmt: String? = null,

    val isVirtual: Boolean? = null,
    val isDownloadable: Boolean? = null,
    val downloadLimit: Long? = null,
    val downloadExpiry: Int? = null,
    val isSoldIndividually: Boolean? = null,

    val externalUrl: String? = null,
    val buttonText: String? = null,

    val taxStatus: String? = null, // taxable, shipping, none
    val taxClass: String? = null,

    val isStockManaged: Boolean? = null,
    val stockQuantity: Double? = null,
    val stockStatus: String? = null, // instock, outofstock, onbackorder

    val backorders: String? = null, // no, notify, yes
    val areBackordersAllowed: Boolean? = null,
    val isBackordered: Boolean? = null,

    val isShippingRequired: Boolean? = null,
    val isShippingTaxable: Boolean? = null,
    val shippingClass: String? = null,
    val shippingClassId: Int? = null,

    val areReviewsAllowed: Boolean? = null,
    val averageRating: String? = null,
    val ratingCount: Int? = null,

    val parentId: Int? = null,
    val purchaseNote: String? = null,
    val menuOrder: Int? = null
)

fun WCProductModel.toDataModel(siteId: Long): ProductEntity =
    ProductEntity(
        remoteProductId,
        siteId,
        name,
        slug,
        permalink,
        dateCreated,
        dateModified,
        type,
        status,
        featured,
        catalogVisibility,
        description,
        shortDescription,
        sku,
        price,
        regularPrice,
        salePrice,
        onSale,
        totalSales,
        purchasable,
        dateOnSaleFrom,
        dateOnSaleTo,
        dateOnSaleFromGmt,
        dateOnSaleToGmt,
        virtual,
        downloadable,
        downloadLimit,
        downloadExpiry,
        soldIndividually,
        externalUrl,
        buttonText,
        taxStatus,
        taxClass,
        manageStock,
        stockQuantity,
        stockStatus,
        backorders,
        backordersAllowed,
        backordered,
        shippingRequired,
        shippingTaxable,
        shippingClass,
        shippingClassId,
        reviewsAllowed,
        averageRating,
        ratingCount,
        parentId,
        purchaseNote,
        menuOrder
    )
