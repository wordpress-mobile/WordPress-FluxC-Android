package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

/**
 * Standard Core WooCommerce order statuses
 */
enum class OrderStatus(val label: String, val value: String) {
    ALL("All", "any"),
    PENDING("Pending Payment", "pending"),
    ON_HOLD("On-Hold", "on-hold"),
    PROCESSING("Processing", "processing"),
    COMPLETED("Completed", "completed"),
    CANCELLED("Cancelled", "cancelled"),
    REFUNDED("Refunded", "refunded"),
    FAILED("Failed", "failed");

    companion object {
        private val map = OrderStatus.values().associateBy(OrderStatus::label);

        /**
         * Convert the label value back into an OrderStatus object
         */
        fun fromLabel(label: String) = map[label]
    }
}
