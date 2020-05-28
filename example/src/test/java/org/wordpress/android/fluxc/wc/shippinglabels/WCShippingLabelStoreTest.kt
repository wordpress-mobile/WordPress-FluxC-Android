package org.wordpress.android.fluxc.wc.shippinglabels

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
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelMapper
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelRestClient
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WCShippingLabelStore
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCShippingLabelStoreTest {
    private val restClient = mock<ShippingLabelRestClient>()
    private val orderId = 25L
    private val refundShippingLabelId = 12L
    private val site = SiteModel().apply { id = 321 }
    private val errorSite = SiteModel().apply { id = 123 }
    private val mapper = WCShippingLabelMapper()
    private lateinit var store: WCShippingLabelStore

    private val sampleShippingLabelApiResponse = WCShippingLabelTestUtils.generateSampleShippingLabelApiResponse()
    private val error = WooError(INVALID_RESPONSE, NETWORK_ERROR, "Invalid site ID")

    private val printPaperSize = "label"
    private val samplePrintShippingLabelApiResponse =
            WCShippingLabelTestUtils.generateSamplePrintShippingLabelApiResponse()

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(SiteModel::class.java, WCShippingLabelModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()

        store = WCShippingLabelStore(
                restClient,
                initCoroutineEngine(),
                mapper
        )

        // Insert the site into the db so it's available later when testing shipping labels
        SiteSqlUtils.insertOrUpdateSite(site)
    }

    @Test
    fun `fetch shipping labels for order`() = test {
        val result = fetchShippingLabelsForOrder()
        val shippingLabelModels = mapper.map(sampleShippingLabelApiResponse!!, site)

        assertThat(result.model?.size).isEqualTo(shippingLabelModels.size)
        assertThat(result.model?.first()?.localOrderId).isEqualTo(shippingLabelModels.first().localOrderId)
        assertThat(result.model?.first()?.localSiteId).isEqualTo(shippingLabelModels.first().localSiteId)
        assertThat(result.model?.first()?.remoteShippingLabelId)
                .isEqualTo(shippingLabelModels.first().remoteShippingLabelId)
        assertThat(result.model?.first()?.carrierId).isEqualTo(shippingLabelModels.first().carrierId)
        assertThat(result.model?.first()?.packageName).isEqualTo(shippingLabelModels.first().packageName)
        assertThat(result.model?.first()?.refundableAmount).isEqualTo(shippingLabelModels.first().refundableAmount)
        assertThat(result.model?.first()?.rate).isEqualTo(shippingLabelModels.first().rate)
        assertThat(result.model?.first()?.paperSize).isEqualTo(shippingLabelModels.first().paperSize)
        assertNotNull(result.model?.first()?.refund)

        val invalidRequestResult = store.fetchShippingLabelsForOrder(errorSite, orderId)
        assertThat(invalidRequestResult.model).isNull()
        assertThat(invalidRequestResult.error).isEqualTo(error)
    }

    @Test
    fun `get stored shipping labels for order`() = test {
        fetchShippingLabelsForOrder()

        val storedTaxClassList = store.getShippingLabelsForOrder(site, orderId)

        val shippingLabelModels = mapper.map(sampleShippingLabelApiResponse!!, site)
        assertThat(storedTaxClassList.size).isEqualTo(shippingLabelModels.size)
        assertThat(storedTaxClassList.first().localOrderId).isEqualTo(shippingLabelModels.first().localOrderId)
        assertThat(storedTaxClassList.first().localSiteId).isEqualTo(shippingLabelModels.first().localSiteId)
        assertThat(storedTaxClassList.first().remoteShippingLabelId)
                .isEqualTo(shippingLabelModels.first().remoteShippingLabelId)
        assertThat(storedTaxClassList.first().carrierId).isEqualTo(shippingLabelModels.first().carrierId)
        assertThat(storedTaxClassList.first().packageName).isEqualTo(shippingLabelModels.first().packageName)
        assertThat(storedTaxClassList.first().refundableAmount).isEqualTo(shippingLabelModels.first().refundableAmount)
        assertThat(storedTaxClassList.first().rate).isEqualTo(shippingLabelModels.first().rate)
        assertThat(storedTaxClassList.first().paperSize).isEqualTo(shippingLabelModels.first().paperSize)
        assertNotNull(storedTaxClassList.first().refund)

        val invalidRequestResult = store.getShippingLabelsForOrder(errorSite, orderId)
        assertThat(invalidRequestResult.size).isEqualTo(0)
    }

    @Test
    fun `refund shipping label for order`() = test {
        val result = refundShippingLabelForOrder()
        assertTrue(result.model!!)

        val invalidRequestResult = store.refundShippingLabelForOrder(errorSite, orderId, refundShippingLabelId)
        assertThat(invalidRequestResult.model).isNull()
        assertThat(invalidRequestResult.error).isEqualTo(error)
    }

    @Test
    fun `print shipping label for order`() = test {
        val result = printShippingLabelForOrder()
        assertThat(result.model).isEqualTo(samplePrintShippingLabelApiResponse?.b64Content)

        val invalidRequestResult = store.printShippingLabel(errorSite, printPaperSize, refundShippingLabelId)
        assertThat(invalidRequestResult.model).isNull()
        assertThat(invalidRequestResult.error).isEqualTo(error)
    }

    private suspend fun fetchShippingLabelsForOrder(): WooResult<List<WCShippingLabelModel>> {
        val fetchShippingLabelsPayload = WooPayload(sampleShippingLabelApiResponse)
        whenever(restClient.fetchShippingLabelsForOrder(orderId, site)).thenReturn(fetchShippingLabelsPayload)
        whenever(restClient.fetchShippingLabelsForOrder(orderId, errorSite)).thenReturn(WooPayload(error))
        return store.fetchShippingLabelsForOrder(site, orderId)
    }

    private suspend fun refundShippingLabelForOrder(): WooResult<Boolean> {
        val refundShippingLabelPayload = WooPayload(sampleShippingLabelApiResponse)
        whenever(restClient.refundShippingLabelForOrder(
                site, orderId, refundShippingLabelId
        )).thenReturn(refundShippingLabelPayload)

        whenever(restClient.refundShippingLabelForOrder(
                errorSite, orderId, refundShippingLabelId
        )).thenReturn(WooPayload(error))

        return store.refundShippingLabelForOrder(site, orderId, refundShippingLabelId)
    }

    private suspend fun printShippingLabelForOrder(): WooResult<String> {
        val printShippingLabelPayload = WooPayload(samplePrintShippingLabelApiResponse)
        whenever(restClient.printShippingLabel(
                site, printPaperSize, refundShippingLabelId
        )).thenReturn(printShippingLabelPayload)

        whenever(restClient.printShippingLabel(
                errorSite, printPaperSize, refundShippingLabelId
        )).thenReturn(WooPayload(error))

        return store.printShippingLabel(site, printPaperSize, refundShippingLabelId)
    }
}
