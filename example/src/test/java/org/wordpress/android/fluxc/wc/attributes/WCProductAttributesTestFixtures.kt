package org.wordpress.android.fluxc.wc.attributes

import com.google.gson.Gson
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.product.attributes.WCProductAttributeModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.AttributeApiResponse

object WCProductAttributesTestFixtures {
    val stubSite = SiteModel().apply { id = 321 }

    val attributeDeleteResponse by lazy {
        "wc/product-attribute-delete.json"
                .jsonFileAs(AttributeApiResponse::class.java)
    }

    val attributeCreateResponse by lazy {
        "wc/product-attribute-create.json"
                .jsonFileAs(AttributeApiResponse::class.java)
    }

    val attributeUpdateResponse by lazy {
        "wc/product-attribute-update.json"
                .jsonFileAs(AttributeApiResponse::class.java)
    }

    val attributesFullListResponse by lazy {
        "wc/product-attributes-all.json"
                .jsonFileAs(Array<AttributeApiResponse>::class.java)
    }

    val parsedAttributesList by lazy {
        listOf(
                WCProductAttributeModel(
                        1,
                        0,
                        "Color",
                        "pa_color",
                        "select",
                        "menu_order",
                        true
                ),
                WCProductAttributeModel(
                        2,
                        0,
                        "Size",
                        "pa_size",
                        "select",
                        "menu_order",
                        false
                )
        )
    }

    val parsedCreateAttributeResponse by lazy {
        WCProductAttributeModel(
                1,
                0,
                "Color",
                "pa_color",
                "select",
                "menu_order",
                true
        )
    }

    private fun <T> String.jsonFileAs(clazz: Class<T>) =
            UnitTestUtils.getStringFromResourceFile(
                    this@WCProductAttributesTestFixtures.javaClass,
                    this
            )?.let { Gson().fromJson(it, clazz) }
}
