package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.WCMetaData
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.Response
import org.wordpress.android.fluxc.network.utils.getString
import org.wordpress.android.fluxc.persistence.entity.ProductEntity
import org.wordpress.android.fluxc.persistence.entity.ProductEntity.BackorderStatus
import org.wordpress.android.fluxc.persistence.entity.ProductEntity.StockStatus
import org.wordpress.android.fluxc.persistence.entity.ProductEntity.TaxStatus
import org.wordpress.android.fluxc.persistence.entity.ProductEntity.Type
import org.wordpress.android.util.DateTimeUtils

class ProductDto(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("name") val name: String? = null,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("permalink") val permalink: String? = null,

    @SerializedName("date_created") val dateCreated: String? = null,
    @SerializedName("date_modified") val dateModified: String? = null,

    @SerializedName("type") val type: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("featured") val isFeatured: Boolean? = null,
    @SerializedName("catalog_visibility") val catalogVisibility: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("short_description") val shortDescription: String? = null,
    @SerializedName("sku") val sku: String? = null,

    @SerializedName("price") val price: String? = null,
    @SerializedName("regular_price") val regularPrice: String? = null,
    @SerializedName("sale_price") val salePrice: String? = null,
    @SerializedName("on_sale") val isOnSale: Boolean? = null,
    @SerializedName("total_sales") val totalSales: Long? = null,
    @SerializedName("purchasable") val isPurchasable: Boolean? = null,

    @SerializedName("virtual") val isVirtual: Boolean? = null,
    @SerializedName("downloadable") val isDownloadable: Boolean? = null,
    @SerializedName("download_limit") val downloadLimit: Long? = null,
    @SerializedName("download_expiry") val downloadExpiry: Int? = null,

    @SerializedName("external_url") val externalUrl: String? = null,
    @SerializedName("button_text") val buttonText: String? = null,

    @SerializedName("tax_status") val taxStatus: String? = null,
    @SerializedName("tax_class") val taxClass: String? = null,

    @SerializedName("manage_stock") val isStockManaged: Boolean? = null,
    @SerializedName("stock_quantity") val stockQuantity: Double? = null,
    @SerializedName("stock_status") val stockStatus: String? = null,

    @SerializedName("date_on_sale_from_gmt") val dateOnSaleFromGmt: String? = null,
    @SerializedName("date_on_sale_to_gmt") val dateOnSaleToGmt: String? = null,

    @SerializedName("backorders") val backorders: String? = null,
    @SerializedName("backorders_allowed") val areBackordersAllowed: Boolean? = null,
    @SerializedName("backordered") val isBackordered: Boolean? = null,

    @SerializedName("sold_individually") val isSoldIndividually: Boolean? = null,
    @SerializedName("weight") val weight: String? = null,
    @SerializedName("dimensions") val dimensions: DimensionsDto? = null,

    @SerializedName("shipping_required") val isShippingRequired: Boolean? = null,
    @SerializedName("shipping_taxable") val isShippingTaxable: Boolean? = null,
    @SerializedName("shipping_class") val shippingClass: String? = null,
    @SerializedName("shipping_class_id") val shippingClassId: Int? = null,

    @SerializedName("reviews_allowed") val areReviewsAllowed: Boolean? = null,
    @SerializedName("average_rating") val averageRating: String? = null,
    @SerializedName("rating_count") val ratingCount: Int? = null,

    @SerializedName("parent_id") val parentId: Int? = null,
    @SerializedName("menu_order") val menuOrder: Int? = null,
    @SerializedName("purchase_note") val purchaseNote: String? = null,

    @SerializedName("categories") val categories: List<ProductCategoryDto>? = null,
    @SerializedName("tags") val tags: List<ProductTagDto>? = null,
    @SerializedName("images") val images: List<ProductImageDto>? = null,
    @SerializedName("attributes") val attributes: List<ProductAttributeDto>? = null,
    @SerializedName("variations") val variationIds: List<Long>? = null,
    @SerializedName("downloads") val downloads: List<ProductDownloadDto>? = null,
    @SerializedName("related_ids") val relatedProductIds: List<Long>? = null,
    @SerializedName("cross_sell_ids") val crossSellIds: List<Long>? = null,
    @SerializedName("upsell_ids") val upsellIds: List<Long>? = null,
    @SerializedName("grouped_products") val groupedProductIds: List<Long>? = null,
    @SerializedName("meta_data") val metadata: List<WCMetaData>? = null
) {
    fun toDataModel(siteId: Long): ProductEntity =
        ProductEntity(
            id,
            siteId,
            name,
            slug,
            permalink,
            DateTimeUtils.dateUTCFromIso8601(dateCreated),
            DateTimeUtils.dateUTCFromIso8601(dateModified),
            type?.let { Type.fromString(it) },
            status?.let { CoreProductStatus.fromValue(it) },
            isFeatured,
            catalogVisibility?.let { CoreProductVisibility.fromValue(it) },
            description,
            shortDescription,
            sku,
            price,
            regularPrice,
            salePrice,
            isOnSale,
            totalSales,
            isPurchasable,
            DateTimeUtils.dateUTCFromIso8601(dateOnSaleFromGmt),
            DateTimeUtils.dateUTCFromIso8601(dateOnSaleToGmt),
            isVirtual,
            isDownloadable,
            downloadLimit,
            downloadExpiry,
            isSoldIndividually,
            externalUrl,
            buttonText,
            TaxStatus.fromString(taxStatus),
            taxClass,
            isStockManaged,
            stockQuantity,
            StockStatus.fromString(stockStatus),
            BackorderStatus.fromString(backorders),
            areBackordersAllowed,
            isBackordered,
            isShippingRequired,
            isShippingTaxable,
            shippingClass,
            shippingClassId,
            areReviewsAllowed,
            averageRating,
            ratingCount,
            parentId,
            purchaseNote,
            menuOrder,
            weight?.toFloatOrNull(),
            dimensions?.length?.toFloatOrNull(),
            dimensions?.width?.toFloatOrNull(),
            dimensions?.height?.toFloatOrNull()
        )

    data class DimensionsDto(
        @SerializedName("length") val length: String,
        @SerializedName("width") val width: String,
        @SerializedName("height") val height: String
    )
}
