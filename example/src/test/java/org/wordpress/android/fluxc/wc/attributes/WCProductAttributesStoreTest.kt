package org.wordpress.android.fluxc.wc.attributes

import com.nhaarman.mockitokotlin2.mock
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
    fun `fetch attributes should call mapper once`() {
    }

    @Test
    fun `fetch attributes should return WooResult correctly`() {
    }

    @Test
    fun `fetch attributes from a invalid site ID should return WooResult with error`() {
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
