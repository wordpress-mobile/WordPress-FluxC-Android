package org.wordpress.android.fluxc.wc.attributes

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.product.attributes.WCProductAttributeMapper
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.AttributeApiResponse
import org.wordpress.android.fluxc.wc.attributes.WCProductAttributesTestFixtures.attributeCreateResponse
import org.wordpress.android.fluxc.wc.attributes.WCProductAttributesTestFixtures.attributesFullListResponse

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCProductAttributeMapperTest {
    private lateinit var mapperUnderTest: WCProductAttributeMapper

    @Before
    fun setUp() {
        mapperUnderTest = WCProductAttributeMapper()
    }

    @Test
    fun `mapToAttributeModel should never allow null values`() {
        AttributeApiResponse()
                .let {
                    mapperUnderTest.mapToAttributeModel(it)
                }.let { result ->
                    assertThat(result).isNotNull
                    assertThat(result.id).isNotNull
                    assertThat(result.name).isNotNull
                    assertThat(result.slug).isNotNull
                    assertThat(result.type).isNotNull
                    assertThat(result.orderBy).isNotNull
                    assertThat(result.hasArchives).isNotNull
                }
    }

    @Test
    fun `mapToAttributeModelList should parse list correctly`() {
        mapperUnderTest.mapToAttributeModelList(attributesFullListResponse!!)
                .let { result ->
                    assertThat(result).isNotNull
                    assertThat(result.size).isEqualTo(2)
                }
    }

    @Test
    fun `mapToAttributeModel should parse correctly`() {
        mapperUnderTest.mapToAttributeModel(attributeCreateResponse!!)
                .let { result ->
                    assertThat(result).isNotNull
                    assertThat(result.id).isEqualTo(1)
                    assertThat(result.name).isEqualTo("Color")
                    assertThat(result.slug).isEqualTo("pa_color")
                    assertThat(result.type).isEqualTo("select")
                    assertThat(result.orderBy).isEqualTo("menu_order")
                    assertThat(result.hasArchives).isEqualTo(true)
                }
    }
}