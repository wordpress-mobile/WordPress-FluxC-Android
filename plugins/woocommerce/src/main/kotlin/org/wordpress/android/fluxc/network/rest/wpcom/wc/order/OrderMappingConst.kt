package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import org.wordpress.android.fluxc.model.WCMetaData

object OrderMappingConst {
    const val CHARGE_ID_KEY = "_charge_id"
    const val SHIPPING_PHONE_KEY = "_shipping_phone"
    internal val WCMetaData.isNotInternalAttributeData
        get() = key.first() != '_'
}
