package org.wordpress.android.fluxc.wc.attributes

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.yarolegovich.wellsql.WellSql
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.product.attributes.WCProductAttributeMapper
import org.wordpress.android.fluxc.model.product.attributes.WCProductAttributeModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.ProductAttributeRestClient
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WCProductAttributesStore
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.fluxc.wc.attributes.WCProductAttributesTestFixtures.attributeCreateResponse
import org.wordpress.android.fluxc.wc.attributes.WCProductAttributesTestFixtures.attributeDeleteResponse
import org.wordpress.android.fluxc.wc.attributes.WCProductAttributesTestFixtures.attributeUpdateResponse
import org.wordpress.android.fluxc.wc.attributes.WCProductAttributesTestFixtures.attributesFullListResponse
import org.wordpress.android.fluxc.wc.attributes.WCProductAttributesTestFixtures.parsedAttributesList
import org.wordpress.android.fluxc.wc.attributes.WCProductAttributesTestFixtures.parsedCreateAttributeResponse
import org.wordpress.android.fluxc.wc.attributes.WCProductAttributesTestFixtures.parsedDeleteAttributeResponse
import org.wordpress.android.fluxc.wc.attributes.WCProductAttributesTestFixtures.parsedUpdateAttributeResponse
import org.wordpress.android.fluxc.wc.attributes.WCProductAttributesTestFixtures.stubSite

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCProductAttributesStoreTest {
    private lateinit var storeUnderTest: WCProductAttributesStore
    private lateinit var restClient: ProductAttributeRestClient
    private lateinit var mapper: WCProductAttributeMapper

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(SiteModel::class.java, WCProductAttributeModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()
        initMocks()
        createStoreUnderTest()
    }

    @Test
    fun `fetch attributes with empty result should return WooError`() = test {
        whenever(restClient.fetchProductFullAttributesList(stubSite))
                .thenReturn(WooPayload(emptyArray()))
        val result = storeUnderTest.fetchStoreAttributes(stubSite)
        assertThat(result.model).isNull()
        assertThat(result.error).isNotNull
    }

    @Test
    fun `fetch attributes should call mapper once`() = test {
        mapper = spy()
        createStoreUnderTest()
        whenever(restClient.fetchProductFullAttributesList(stubSite))
                .thenReturn(WooPayload(attributesFullListResponse))

        storeUnderTest.fetchStoreAttributes(stubSite)
        verify(mapper).mapToAttributeModelList(attributesFullListResponse!!, stubSite)
    }

    @Test
    fun `fetch attributes should return WooResult correctly`() = test {
        whenever(restClient.fetchProductFullAttributesList(stubSite))
                .thenReturn(WooPayload(attributesFullListResponse))

        whenever(mapper.mapToAttributeModelList(attributesFullListResponse!!, stubSite))
                .thenReturn(parsedAttributesList)

        storeUnderTest.fetchStoreAttributes(stubSite).let { result ->
            assertThat(result.model).isNotNull
            assertThat(result.model).isEqualTo(parsedAttributesList)
            assertThat(result.error).isNull()
        }
    }

    @Test
    fun `create Attribute should return WooResult with parsed entity`() = test {
        val expectedResult = WCProductAttributeModel(
                1,
                321,
                "Color",
                "pa_color",
                "select",
                "menu_order",
                true
        )

        whenever(restClient.postNewAttribute(stubSite, with(expectedResult) {
            mapOf(
                    "name" to name,
                    "slug" to slug,
                    "type" to type,
                    "order_by" to orderBy,
                    "has_archives" to hasArchives.toString()
            )
        })).thenReturn(WooPayload(attributeCreateResponse))

        whenever(mapper.mapToAttributeModel(attributeCreateResponse!!, stubSite))
                .thenReturn(parsedCreateAttributeResponse)

        storeUnderTest.createAttribute(
                site = stubSite,
                name = expectedResult.name,
                slug = expectedResult.slug,
                type = expectedResult.type,
                orderBy = expectedResult.orderBy,
                hasArchives = expectedResult.hasArchives
        ).let { result ->
            assertThat(result.model).isNotNull
            assertThat(result.model).isEqualTo(expectedResult)
            assertThat(result.error).isNull()
        }
    }

    @Test
    fun `delete Attribute should return WooResult with parsed entity`() = test {
        val expectedResult = WCProductAttributeModel(
                17,
                321,
                "Size",
                "pa_size",
                "select",
                "name",
                true
        )

        whenever(restClient.deleteExistingAttribute(stubSite, 17))
                .thenReturn(WooPayload(attributeDeleteResponse))

        whenever(mapper.mapToAttributeModel(attributeDeleteResponse!!, stubSite))
                .thenReturn(parsedDeleteAttributeResponse)

        storeUnderTest.deleteAttribute(
                site = stubSite,
                attributeID = 17
        ).let { result ->
            assertThat(result.model).isNotNull
            assertThat(result.model).isEqualTo(expectedResult)
            assertThat(result.error).isNull()
        }
    }

    @Test
    fun `update Attribute should return WooResult with parsed entity`() = test {
        val expectedResult = WCProductAttributeModel(
                99,
                321,
                "test_name",
                "pa_test",
                "test_type",
                "test",
                false
        )

        whenever(
                restClient.updateExistingAttribute(stubSite, 99,
                        with(expectedResult) {
                            mapOf(
                                    "id" to "99",
                                    "name" to name,
                                    "slug" to slug,
                                    "type" to type,
                                    "order_by" to orderBy,
                                    "has_archives" to hasArchives.toString()
                            )
                        })
        ).thenReturn(WooPayload(attributeUpdateResponse))

        whenever(mapper.mapToAttributeModel(attributeUpdateResponse!!, stubSite))
                .thenReturn(parsedUpdateAttributeResponse)

        storeUnderTest.updateAttribute(
                site = stubSite,
                attributeID = 99,
                name = expectedResult.name,
                slug = expectedResult.slug,
                type = expectedResult.type,
                orderBy = expectedResult.orderBy,
                hasArchives = expectedResult.hasArchives
        ).let { result ->
            assertThat(result.model).isNotNull
            assertThat(result.model).isEqualTo(expectedResult)
            assertThat(result.error).isNull()
        }
    }

    private fun initMocks() {
        restClient = mock()
        mapper = mock()
    }

    private fun createStoreUnderTest() =
            WCProductAttributesStore(
                    restClient,
                    mapper,
                    initCoroutineEngine()
            ).apply { storeUnderTest = this }
}
