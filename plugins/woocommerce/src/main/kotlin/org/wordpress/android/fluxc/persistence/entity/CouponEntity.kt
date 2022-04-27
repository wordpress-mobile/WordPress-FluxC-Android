package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Relation
import java.math.BigDecimal

@Entity(
    tableName = "Coupons",
    primaryKeys = ["id", "siteId"],
    indices = [Index("id", "siteId")]
)
data class CouponEntity(
    val id: Long,
    val siteId: Long,
    val code: String? = null,
    val amount: BigDecimal? = null,
    val dateCreated: String? = null,
    val dateCreatedGmt: String? = null,
    val dateModified: String? = null,
    val dateModifiedGmt: String? = null,
    val discountType: DiscountType? = null,
    val description: String? = null,
    val dateExpires: String? = null,
    val dateExpiresGmt: String? = null,
    val usageCount: Int? = null,
    val isForIndividualUse: Boolean? = null,
    val usageLimit: Int? = null,
    val usageLimitPerUser: Int? = null,
    val limitUsageToXItems: Int? = null,
    val isShippingFree: Boolean? = null,
    val areSaleItemsExcluded: Boolean? = null,
    val minimumAmount: BigDecimal? = null,
    val maximumAmount: BigDecimal? = null
) {
    sealed class DiscountType(val value: String) {
        object Percent : DiscountType("percent")
        object FixedCart : DiscountType("fixed_cart")
        object FixedProduct : DiscountType("fixed_product")

        companion object {
            fun fromString(value: String?): DiscountType {
                return when (value) {
                    Percent.value -> Percent
                    FixedProduct.value -> FixedProduct
                    else -> FixedCart
                }
            }
        }

        override fun toString() = value
    }
}

data class CouponWithEmails(
    @Embedded val coupon: CouponEntity,
    @Relation(parentColumn = "id", entityColumn = "couponId")
    val restrictedEmails: List<CouponEmailEntity>
)

data class CouponDataModel(
    val coupon: CouponEntity,
    val products: List<ProductEntity>,
    val excludedProducts: List<ProductEntity>,
    val categories: List<ProductCategoryEntity>,
    val excludedCategories: List<ProductCategoryEntity>,
    val restrictedEmails: List<CouponEmailEntity>
)
