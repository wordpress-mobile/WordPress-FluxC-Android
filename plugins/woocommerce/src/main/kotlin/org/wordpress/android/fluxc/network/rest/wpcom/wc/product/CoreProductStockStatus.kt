package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

/**
 * Standard Core WooCommerce product stock statuses
 */
enum class CoreProductStockStatus(val value: String) {
    IN_STOCK("instock"),
    OUT_OF_STOCK("outofstock"),
    ON_BACK_ORDER("onbackorder");

    companion object {
        private val valueMap = values().associateBy(CoreProductStockStatus::value)

        /**
         * Convert the base value into the associated CoreProductStockStatus object
         */
        fun fromValue(value: String) = valueMap[value]
    }
}
