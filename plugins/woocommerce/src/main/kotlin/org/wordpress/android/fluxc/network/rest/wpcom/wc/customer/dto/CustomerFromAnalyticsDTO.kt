package org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto

import com.google.gson.annotations.SerializedName

data class CustomerFromAnalyticsDTO(
    @SerializedName("avg_order_value")
    val avgOrderValue: Double,
    @SerializedName("city")
    val city: String,
    @SerializedName("country")
    val country: String,
    @SerializedName("date_last_active")
    val dateLastActive: String,
    @SerializedName("date_last_active_gmt")
    val dateLastActiveGmt: String,
    @SerializedName("date_last_order")
    val dateLastOrder: String,
    @SerializedName("date_registered")
    val dateRegistered: String,
    @SerializedName("date_registered_gmt")
    val dateRegisteredGmt: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("id")
    val id: Int,
    @SerializedName("_links")
    val links: Links,
    @SerializedName("name")
    val name: String,
    @SerializedName("orders_count")
    val ordersCount: Int,
    @SerializedName("postcode")
    val postcode: String,
    @SerializedName("state")
    val state: String,
    @SerializedName("total_spend")
    val totalSpend: Double,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("username")
    val username: String
) {
    data class Links(
        @SerializedName("collection")
        val collection: List<Collection>,
        @SerializedName("customer")
        val customer: List<Customer>
    ) {
        data class Collection(
            @SerializedName("href")
            val href: String
        )

        data class Customer(
            @SerializedName("href")
            val href: String
        )
    }
}
