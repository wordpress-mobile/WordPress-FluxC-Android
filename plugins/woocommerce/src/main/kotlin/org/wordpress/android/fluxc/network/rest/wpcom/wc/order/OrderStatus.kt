package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

/**
 * Standard Core WooCommerce order statuses
 */
enum class OrderStatus(val label: String, val value: String) {
    PENDING("Pending Payment", "pending"),
    ON_HOLD("On-Hold", "on-hold"),
    PROCESSING("Processing", "processing"),
    COMPLETED("Completed", "completed"),
    CANCELLED("Cancelled", "cancelled"),
    REFUNDED("Refunded", "refunded"),
    FAILED("Failed", "failed");
}
