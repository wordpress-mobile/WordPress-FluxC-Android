package org.wordpress.android.fluxc.model.coupon

import java.util.Date

data class UpdateCouponRequest(
    val code: String? = null,
    val amount: String? = null,
    val discountType: String? = null,
    val description: String? = null,
    val dateExpires: Date? = null,
    val dateExpiresGmt: Date? = null,
    val usageCount: Int? = null,
    val minimumAmount: String? = null,
    val maximumAmount: String? = null,
    val productIds: List<Long>? = null,
    val excludedProductIds: List<Long>? = null,
    val isShippingFree: Boolean? = null,
    val productCategoryIds: List<Long>? = null,
    val excludedProductCategoryIds: List<Long>? = null,
    val usageLimit: Int? = null,
    val usageLimitPerUser: Int? = null,
    val limitUsageToXItems: Int? = null,
    val restrictedEmails: List<String>? = null,
    val isForIndividualUse: Boolean? = null,
    val areSaleItemsExcluded: Boolean? = null,
    val usedBy: List<String>? = null
)
