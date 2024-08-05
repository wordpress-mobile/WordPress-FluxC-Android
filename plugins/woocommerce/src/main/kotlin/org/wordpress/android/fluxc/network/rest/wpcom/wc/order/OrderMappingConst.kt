package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import org.wordpress.android.fluxc.model.WCMetaData

object OrderMappingConst {
    const val CHARGE_ID_KEY = "_charge_id"
    const val PAYMENT_INTENT_ID_KEY = "_intent_id"
    const val SHIPPING_PHONE_KEY = "_shipping_phone"
    const val RECEIPT_URL_KEY = "receipt_url"
    /**
     * Verify if the Metadata key is not null or a internal store attribute
     * @return false if the `key` is null or starts with the `_` character
     * @return true otherwise
     */
    internal val WCMetaData.isDisplayableAttribute
        get() = key?.startsWith('_')?.not() ?: false
}
