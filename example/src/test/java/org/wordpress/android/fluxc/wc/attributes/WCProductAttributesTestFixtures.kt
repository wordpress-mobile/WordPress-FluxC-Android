package org.wordpress.android.fluxc.wc.attributes

import com.google.gson.Gson
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.AttributeApiResponse

object WCProductAttributesTestFixtures {
    val stubSite = SiteModel().apply { id = 321 }

    val attributeDeleteResponse
        get() = "wc/product-attribute-delete.json".fileNameToJson()

    val attributeCreateResponse
        get() = "wc/product-attribute-create.json".fileNameToJson()

    val attributeUpdateResponse
        get() = "wc/product-attribute-update.json".fileNameToJson()

    private fun String.fileNameToJson() =
            UnitTestUtils.getStringFromResourceFile(
                    this@WCProductAttributesTestFixtures.javaClass,
                    this
            )?.let { Gson().fromJson(it, AttributeApiResponse::class.java) }
}
