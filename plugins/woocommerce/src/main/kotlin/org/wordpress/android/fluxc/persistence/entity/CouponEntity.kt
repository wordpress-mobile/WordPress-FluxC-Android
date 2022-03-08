package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Relation

@Entity(
    tableName = "Coupons",
    primaryKeys = ["id", "siteId"],
    indices = [Index("id", "siteId")]
)
data class CouponEntity(
    val id: Long,
    val siteId: Long,
    val code: String? = null,
    val dateCreated: String? = null,
    val dateCreatedGmt: String? = null,
    val dateModified: String? = null,
    val dateModifiedGmt: String? = null,
    val discountType: String? = null,
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
    val minimumAmount: String? = null,
    val maximumAmount: String? = null
)

data class CouponWithEmails(
    @Embedded val couponEntity: CouponEntity,
    @Relation(parentColumn = "id", entityColumn = "couponId")
    val restrictedEmails: List<CouponEmailEntity>
)

data class CouponDataModel(
    val couponEntity: CouponEntity,
    val products: List<ProductEntity>,
    val excludedProducts: List<ProductEntity>,
    val categories: List<ProductCategoryEntity>,
    val excludedCategories: List<ProductCategoryEntity>,
    val restrictedEmails: List<CouponEmailEntity>
)
