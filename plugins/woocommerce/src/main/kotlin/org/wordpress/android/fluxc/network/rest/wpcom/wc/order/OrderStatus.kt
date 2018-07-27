package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

enum class OrderStatus {
    // Standard core WooCommerce order statuses
    PENDING {
        override fun toString(): String {
            return "pending"
        }
    },
    ON_HOLD {
        override fun toString(): String {
            return "on-hold"
        }
    },
    PROCESSING {
        override fun toString(): String {
            return "processing"
        }
    },
    COMPLETED {
        override fun toString(): String {
            return "completed"
        }
    },
    CANCELLED {
        override fun toString(): String {
            return "cancelled"
        }
    },
    REFUNDED {
        override fun toString(): String {
            return "refunded"
        }
    },
    FAILED {
        override fun toString(): String {
            return "failed"
        }
    }
}
