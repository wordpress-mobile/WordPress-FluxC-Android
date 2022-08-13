package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import org.wordpress.android.fluxc.model.WCMetaData

object OrderMappingConst {
    const val CHARGE_ID_KEY = "_charge_id"
    const val SHIPPING_PHONE_KEY = "_shipping_phone"

    /**
     * Verify if the Metadata key is a internal store attribute
     * true if the `key` starts with the `_` character
     * null if the key is null and it's not possible to verify if it's internal or not
     * false otherwise
     */
    internal val WCMetaData.isInternalAttribute
        get() = key?.startsWith('_')
}
