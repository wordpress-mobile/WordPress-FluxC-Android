package org.wordpress.android.fluxc.wc.attributes

import com.google.gson.Gson
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.AttributeApiResponse

object WCProductAttributesTestFixtures {
    val stubSite = SiteModel().apply { id = 321 }

    val attributeDeleteResponse
        get() = "wc/product-attribute-delete.json"
                .jsonFileAs(AttributeApiResponse::class.java)

    val attributeCreateResponse
        get() = "wc/product-attribute-create.json"
                .jsonFileAs(AttributeApiResponse::class.java)

    val attributeUpdateResponse
        get() = "wc/product-attribute-update.json"
                .jsonFileAs(AttributeApiResponse::class.java)

    val attributesFullList
        get() = "wc/product-attributes-all.json"
                .jsonFileAs(Array<AttributeApiResponse>::class.java)

    private fun <T> String.jsonFileAs(clazz: Class<T>) =
            UnitTestUtils.getStringFromResourceFile(
                    this@WCProductAttributesTestFixtures.javaClass,
                    this
            )?.let { Gson().fromJson(it, clazz) }
}
