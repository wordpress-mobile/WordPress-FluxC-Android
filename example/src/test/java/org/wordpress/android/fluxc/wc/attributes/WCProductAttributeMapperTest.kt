package org.wordpress.android.fluxc.wc.attributes

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.yarolegovich.wellsql.WellSql
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.product.attributes.WCProductAttributeMapper
import org.wordpress.android.fluxc.model.product.attributes.terms.WCAttributeTermModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.ProductAttributeRestClient
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.wc.attributes.WCProductAttributesTestFixtures.attributeCreateResponse
import org.wordpress.android.fluxc.wc.attributes.WCProductAttributesTestFixtures.attributeTermsFullListResponse
import org.wordpress.android.fluxc.wc.attributes.WCProductAttributesTestFixtures.attributesFullListResponse
import org.wordpress.android.fluxc.wc.attributes.WCProductAttributesTestFixtures.stubSite

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCProductAttributeMapperTest {
    private lateinit var mapperUnderTest: WCProductAttributeMapper
    private lateinit var attributesRestClient: ProductAttributeRestClient

    @Before
    fun setUp() {
        SingleStoreWellSqlConfigForTests(
                RuntimeEnvironment.application.applicationContext,
                listOf(SiteModel::class.java, WCAttributeTermModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE
        ).let {
            WellSql.init(it)
            it.reset()
        }

        attributesRestClient = mock()
        mapperUnderTest = WCProductAttributeMapper(attributesRestClient)
    }

    @Test
    fun `mapToAttributeModel should never allow null values`() = test {
        attributeCreateResponse
                ?.let {
                    configureAttributeRestClientMock(it.id?.toIntOrNull() ?: 0)
                    mapperUnderTest.responseToAttributeModel(it, stubSite)
                }?.let { result ->
                    assertThat(result).isNotNull
                    assertThat(result.remoteId).isNotNull
                    assertThat(result.name).isNotNull
                    assertThat(result.slug).isNotNull
                    assertThat(result.type).isNotNull
                    assertThat(result.orderBy).isNotNull
                    assertThat(result.hasArchives).isNotNull
                } ?: fail("Result shouldn't be null")
    }

    @Test
    fun `mapToAttributeModelList should parse list correctly`() = test {
        configureAttributeRestClientMock(1)
        configureAttributeRestClientMock(2)
        mapperUnderTest.responseToAttributeModelList(attributesFullListResponse!!, stubSite)
                .let { result ->
                    assertThat(result).isNotNull
                    assertThat(result.size).isEqualTo(2)
                }
    }

    @Test
    fun `mapToAttributeModel should parse correctly`() = test {
        configureAttributeRestClientMock(1)
        mapperUnderTest.responseToAttributeModel(attributeCreateResponse!!, stubSite)
                ?.let { result ->
                    assertThat(result).isNotNull
                    assertThat(result.remoteId).isEqualTo(1)
                    assertThat(result.name).isEqualTo("Color")
                    assertThat(result.slug).isEqualTo("pa_color")
                    assertThat(result.type).isEqualTo("select")
                    assertThat(result.orderBy).isEqualTo("menu_order")
                    assertThat(result.hasArchives).isEqualTo(true)
                } ?: fail("Result shouldn't be null")
    }

    private suspend fun configureAttributeRestClientMock(attributeID: Int) {
        mock<ProductAttributeRestClient>().apply {
            whenever(attributesRestClient.fetchAllAttributeTerms(stubSite, attributeID.toLong()))
                    .thenReturn(WooPayload(attributeTermsFullListResponse))
        }
    }
}
