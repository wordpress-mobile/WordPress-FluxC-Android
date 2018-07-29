package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

/**
 * Standard Core WooCommerce order statuses
 */
enum class OrderStatus(val index: Int, val label: String, val value: String?) {
    ALL(0, "All", null),
    PENDING(1, "Pending Payment", "pending"),
    PROCESSING(2, "Processing", "processing"),
    ON_HOLD(3, "On-Hold", "on-hold"),
    COMPLETED(4, "Completed", "completed"),
    CANCELLED(5, "Cancelled", "cancelled"),
    REFUNDED(6, "Refunded", "refunded"),
    FAILED(7, "Failed", "failed");

    companion object {
        private val labelMap = OrderStatus.values().associateBy(OrderStatus::label)
        private val indexMap = OrderStatus.values().associateBy(OrderStatus::index)

        /**
         * Convert the label value back into the associated OrderStatus object
         */
        fun fromLabel(label: String) = labelMap[label]

        /**
         * Convert the index back into the associated OrderStatus object
         */
        fun fromIndex(index: Int) = indexMap[index]
    }
}
