package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Relation
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductBackOrders
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStockStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductTaxStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductVisibility
import org.wordpress.android.fluxc.persistence.entity.ProductEntity.BackorderStatus
import org.wordpress.android.fluxc.persistence.entity.ProductEntity.StockStatus
import org.wordpress.android.fluxc.persistence.entity.ProductEntity.TaxStatus
import org.wordpress.android.fluxc.persistence.entity.ProductEntity.Type
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import java.util.Locale

@Entity(
    tableName = "Products",
    primaryKeys = ["id", "siteId"]
)
data class ProductVariationEntity(
    val id: Long,
    val productId: Long,
    val siteId: Long,
    val permalink: String? = null,

    val dateCreated: Date? = null,
    val dateModified: Date? = null,

    val status: CoreProductStatus? = null,
    val description: String? = null,
    val sku: String? = null,

    val price: String? = null,
    val regularPrice: String? = null,
    val salePrice: String? = null,
    val isOnSale: Boolean? = null,
    val isPurchasable: Boolean? = null,

    val dateOnSaleFromGmt: Date? = null,
    val dateOnSaleToGmt: Date? = null,

    val isVirtual: Boolean? = null,
    val isDownloadable: Boolean? = null,
    val downloadLimit: Long? = null,
    val downloadExpiry: Int? = null,

    val taxStatus: TaxStatus? = null,
    val taxClass: String? = null,

    val isStockManaged: Boolean? = null,
    val stockQuantity: Double? = null,
    val stockStatus: StockStatus? = null,

    val backorders: BackorderStatus? = null,
    val areBackordersAllowed: Boolean? = null,
    val isBackordered: Boolean? = null,

    val shippingClass: String? = null,
    val shippingClassId: Int? = null,

    val menuOrder: Int? = null,

    val weight: Float? = null,
    val length: Float? = null,
    val width: Float? = null,
    val height: Float? = null

    // downloads
    // attributes
    // image

//    @Column var categories = "" // array of categories
//    @Column var tags = "" // array of tags
//    @Column var images = "" // array of images
//    @Column var attributes = "" // array of attributes
//    @Column var variations = "" // array of variation IDs
//    @Column var downloads = "" // array of downloadable files
//    @Column var relatedIds = "" // array of related product IDs
//    @Column var crossSellIds = "" // array of cross-sell product IDs
//    @Column var upsellIds = "" // array of up-sell product IDs
//    @Column var groupedProductIds = "" // array of grouped product IDs

) {
    sealed class Type(open val value: String) {
        object Simple : Type(CoreProductType.SIMPLE.value)
        object Grouped : Type(CoreProductType.GROUPED.value)
        object External : Type(CoreProductType.EXTERNAL.value)
        object Variable : Type(CoreProductType.VARIABLE.value)
        data class Custom(override val value: String) : Type(value)

        companion object {
            fun fromString(value: String): Type {
                return when (value.toLowerCase(Locale.US)) {
                    CoreProductType.GROUPED.value -> Grouped
                    CoreProductType.EXTERNAL.value -> External
                    CoreProductType.VARIABLE.value -> Variable
                    CoreProductType.SIMPLE.value -> Simple
                    else -> Custom(value)
                }
            }
        }
    }
}

fun WCProductVariationModel.toDataModel(siteId: Long): ProductVariationEntity =
    ProductVariationEntity(
        remoteVariationId,
        remoteProductId,
        siteId,
        permalink,
        DateTimeUtils.dateUTCFromIso8601(dateCreated),
        DateTimeUtils.dateUTCFromIso8601(dateModified),
        CoreProductStatus.fromValue(status),
        description,
        sku,
        price,
        regularPrice,
        salePrice,
        onSale,
        purchasable,
        DateTimeUtils.dateUTCFromIso8601(dateOnSaleFromGmt),
        DateTimeUtils.dateUTCFromIso8601(dateOnSaleToGmt),
        virtual,
        downloadable,
        downloadLimit,
        downloadExpiry,
        TaxStatus.fromString(taxStatus),
        taxClass,
        manageStock,
        stockQuantity,
        StockStatus.fromString(stockStatus),
        BackorderStatus.fromString(backorders),
        backordersAllowed,
        backordered,
        shippingClass,
        shippingClassId,
        menuOrder,
        weight.toFloatOrNull(),
        length.toFloatOrNull(),
        width.toFloatOrNull(),
        height.toFloatOrNull()
    )

data class ProductDataModel(
    @Embedded val product: ProductEntity,
    @Relation(parentColumn = "id", entityColumn = "couponId")
    val categories: List<ProductCategoryEntity>
)