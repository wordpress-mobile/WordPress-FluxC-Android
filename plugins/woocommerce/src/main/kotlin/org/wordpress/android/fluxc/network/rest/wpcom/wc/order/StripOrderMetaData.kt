package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import org.wordpress.android.fluxc.model.metadata.WCMetaData
import javax.inject.Inject

class StripOrderMetaData @Inject internal constructor() {
    operator fun invoke(metaData: List<WCMetaData>): List<WCMetaData> {
        return metaData
            .filter {
                (it.isDisplayable || it.key in WCMetaData.SUPPORTED_KEYS)
                        && !it.isJson
            }
    }
}
