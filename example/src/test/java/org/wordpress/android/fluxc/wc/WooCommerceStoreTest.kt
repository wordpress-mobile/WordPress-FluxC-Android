package org.wordpress.android.fluxc.wc

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.yarolegovich.wellsql.WellSql
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult.Valid
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress.Type.DESTINATION
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress.Type.ORIGIN
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.plugins.WooPluginRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.plugins.WooPluginRestClient.FetchPluginsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.plugins.WooPluginRestClient.FetchPluginsResponse.PluginModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelRestClient
import org.wordpress.android.fluxc.persistence.WCPluginSqlUtils.WCPluginModel
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import kotlin.test.assertEquals

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WooCommerceStoreTest {
    private val appContext = RuntimeEnvironment.application.applicationContext
    private val restClient = mock<WooPluginRestClient>()
    private val wooCommerceStore = WooCommerceStore(appContext, Dispatcher(), initCoroutineEngine(), restClient, mock())
    private val error = WooError(INVALID_RESPONSE, NETWORK_ERROR, "Invalid site ID")
    private val site = SiteModel().apply { id = 1 }

    private val response = FetchPluginsResponse(
            listOf(
                    PluginModel("woocommerce-services", "1.0", true, "Woo Services"),
                    PluginModel("other", "2.0", false, "Other Plugin")
            )
    )

    @Before
    fun setUp() {
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(WCPluginModel::class.java, SiteModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun testGetWooCommerceSites() {
        val nonWooSite = SiteModel().apply { siteId = 42 }
        WellSql.insert(nonWooSite).execute()

        assertEquals(0, wooCommerceStore.getWooCommerceSites().size)

        val wooJetpackSite = SiteModel().apply {
            siteId = 43
            hasWooCommerce = true
            setIsWPCom(false)
        }
        WellSql.insert(wooJetpackSite).execute()

        assertEquals(1, wooCommerceStore.getWooCommerceSites().size)

        val wooAtomicSite = SiteModel().apply {
            siteId = 44
            hasWooCommerce = true
            setIsWPCom(true)
        }
        WellSql.insert(wooAtomicSite).execute()

        assertEquals(2, wooCommerceStore.getWooCommerceSites().size)
    }

    @Test
    fun `test fetching woo services plugin info`() = test {
        val result = getPlugin()
        val expectedModel = response.plugins.map { WCPluginModel(site, it).apply { id = 1 } }.first()
        Assertions.assertThat(result.model).isEqualTo(expectedModel)

        val invalidRequestResult = getPlugin(true)
        Assertions.assertThat(invalidRequestResult.model).isNull()
        Assertions.assertThat(invalidRequestResult.error).isEqualTo(error)
    }

    private suspend fun getPlugin(isError: Boolean = false): WooResult<WCPluginModel> {
        val payload = WooPayload(response)
        if (isError) {
            whenever(restClient.fetchInstalledPlugins(any())).thenReturn(WooPayload(error))
        } else {
            whenever(restClient.fetchInstalledPlugins(any())).thenReturn(payload)
        }
        return wooCommerceStore.fetchWooCommerceServicesPluginInfo(site)
    }
}
