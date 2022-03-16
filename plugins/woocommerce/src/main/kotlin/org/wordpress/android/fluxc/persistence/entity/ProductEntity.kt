package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import org.wordpress.android.fluxc.model.WCProductModel
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
data class ProductEntity(
    val id: Long,
    val siteId: Long,
    val name: String? = null,
    val slug: String? = null,
    val permalink: String? = null,

    val dateCreated: Date? = null,
    val dateModified: Date? = null,

    val type: Type? = null,
    val status: CoreProductStatus? = null,
    val isFeatured: Boolean? = null,
    val catalogVisibility: CoreProductVisibility? = null,
    val description: String? = null,
    val shortDescription: String? = null,
    val sku: String? = null,

    val price: String? = null,
    val regularPrice: String? = null,
    val salePrice: String? = null,
    val isOnSale: Boolean? = null,
    val totalSales: Long? = null,
    val isPurchasable: Boolean? = null,

    val dateOnSaleFromGmt: Date? = null,
    val dateOnSaleToGmt: Date? = null,

    val isVirtual: Boolean? = null,
    val isDownloadable: Boolean? = null,
    val downloadLimit: Long? = null,
    val downloadExpiry: Int? = null,
    val isSoldIndividually: Boolean? = null,

    val externalUrl: String? = null,
    val buttonText: String? = null,

    val taxStatus: TaxStatus? = null,
    val taxClass: String? = null,

    val isStockManaged: Boolean? = null,
    val stockQuantity: Double? = null,
    val stockStatus: StockStatus? = null,

    val backorderStatus: BackorderStatus? = null,
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
    val menuOrder: Int? = null,

    val weight: Float? = null,
    val length: Float? = null,
    val width: Float? = null,
    val height: Float? = null
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

    sealed class TaxStatus {
        object Taxable : TaxStatus()
        object Shipping : TaxStatus()
        object None : TaxStatus()
        data class Custom(val value: String) : TaxStatus()

        companion object {
            fun fromString(value: String?): TaxStatus {
                return when (value) {
                    CoreProductTaxStatus.TAXABLE.value -> Taxable
                    CoreProductTaxStatus.SHIPPING.value -> Shipping
                    CoreProductTaxStatus.NONE.value, null, "" -> None
                    else -> Custom(value)
                }
            }
        }

        override fun toString(): String {
            return when (this) {
                Taxable -> CoreProductTaxStatus.TAXABLE.value
                Shipping -> CoreProductTaxStatus.SHIPPING.value
                is Custom -> value
                else -> CoreProductTaxStatus.NONE.value
            }
        }
    }

    sealed class StockStatus {
        object InStock : StockStatus()
        object OutOfStock : StockStatus()
        object OnBackorder : StockStatus()
        data class Custom(val value: String) : StockStatus()

        companion object {
            fun fromString(value: String?): StockStatus {
                return when (value) {
                    CoreProductStockStatus.IN_STOCK.value, null, "" -> InStock
                    CoreProductStockStatus.OUT_OF_STOCK.value -> OutOfStock
                    CoreProductStockStatus.ON_BACK_ORDER.value -> OnBackorder
                    else -> Custom(value)
                }
            }
        }

        override fun toString(): String {
            return when (this) {
                OnBackorder -> CoreProductStockStatus.ON_BACK_ORDER.value
                OutOfStock -> CoreProductStockStatus.OUT_OF_STOCK.value
                is Custom -> value
                else -> CoreProductStockStatus.IN_STOCK.value
            }
        }
    }

    sealed class BackorderStatus {
        object No : BackorderStatus()
        object Yes : BackorderStatus()
        object Notify : BackorderStatus()
        data class Custom(val value: String) : BackorderStatus()

        companion object {
            fun fromString(value: String?): BackorderStatus {
                return when (value) {
                    CoreProductBackOrders.NO.value, null, "" -> No
                    CoreProductBackOrders.YES.value -> Yes
                    CoreProductBackOrders.NOTIFY.value -> Notify
                    else -> Custom(value)
                }
            }
        }

        override fun toString(): String {
            return when (this) {
                Yes -> CoreProductBackOrders.YES.value
                Notify -> CoreProductBackOrders.NOTIFY.value
                is Custom -> value
                else -> CoreProductBackOrders.NO.value
            }
        }
    }
}

fun WCProductModel.toDataModel(siteId: Long): ProductEntity =
    ProductEntity(
        remoteProductId,
        siteId,
        name,
        slug,
        permalink,
        DateTimeUtils.dateUTCFromIso8601(dateCreated),
        DateTimeUtils.dateUTCFromIso8601(dateModified),
        Type.fromString(type),
        CoreProductStatus.fromValue(status),
        featured,
        CoreProductVisibility.fromValue(catalogVisibility),
        description,
        shortDescription,
        sku,
        price,
        regularPrice,
        salePrice,
        onSale,
        totalSales,
        purchasable,
        DateTimeUtils.dateUTCFromIso8601(dateOnSaleFromGmt),
        DateTimeUtils.dateUTCFromIso8601(dateOnSaleToGmt),
        virtual,
        downloadable,
        downloadLimit,
        downloadExpiry,
        soldIndividually,
        externalUrl,
        buttonText,
        TaxStatus.fromString(taxStatus),
        taxClass,
        manageStock,
        stockQuantity,
        StockStatus.fromString(stockStatus),
        BackorderStatus.fromString(backorders),
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
        menuOrder,
        weight.toFloatOrNull(),
        length.toFloatOrNull(),
        width.toFloatOrNull(),
        height.toFloatOrNull()
    )