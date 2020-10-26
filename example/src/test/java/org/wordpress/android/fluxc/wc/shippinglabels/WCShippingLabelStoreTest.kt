package org.wordpress.android.fluxc.wc.shippinglabels

import com.nhaarman.mockitokotlin2.any
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
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult.Valid
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.CustomPackage
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.PredefinedOption
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.PredefinedOption.PredefinedPackage
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelMapper
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress.Type.DESTINATION
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress.Type.ORIGIN
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelRestClient.VerifyAddressResponse
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

    private val address = ShippingLabelAddress(country = "CA", address = "1370 Lewisham Dr.")
    private val successfulVerifyAddressApiResponse = VerifyAddressResponse(
            isSuccess = true,
            isTrivialNormalization = false,
            suggestedAddress = address,
            error = null
    )

    private val samplePackagesApiResponse = WCShippingLabelTestUtils.generateSampleGetPackagesApiResponse()

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

    @Test
    fun `verify shipping address`() = test {
        val result = verifyAddress(ORIGIN)
        assertThat(result.model).isEqualTo(Valid(address))

        val invalidRequestResult = verifyAddress(DESTINATION)
        assertThat(invalidRequestResult.model).isNull()
        assertThat(invalidRequestResult.error).isEqualTo(error)
    }

    @Test
    fun `get packages`() = test {
        val expectedResult = WCPackagesResult(
                listOf(
                        CustomPackage("Krabica", false, "1 x 2 x 3"),
                        CustomPackage("Obalka", true, "2 x 3 x 4")
                ),
                listOf(
                        PredefinedOption("USPS Priority Mail Flat Rate Boxes",
                                listOf(
                                        PredefinedPackage(
                                                "Small Flat Rate Box",
                                                false,
                                                "21.91 x 13.65 x 4.13"
                                        ),
                                        PredefinedPackage(
                                                "Medium Flat Rate Box 1, Top Loading",
                                                false,
                                                "28.57 x 22.22 x 15.24"
                                        )
                                )
                        ),
                        PredefinedOption(
                                "DHL Express",
                                listOf(PredefinedPackage(
                                        "Large Padded Pouch",
                                        true,
                                        "30.22 x 35.56 x 2.54"
                                ))
                        )
                )
        )
        val result = getPackages()
        assertThat(result.model).isEqualTo(expectedResult)

        val invalidRequestResult = getPackages(true)
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

    private suspend fun verifyAddress(type: ShippingLabelAddress.Type): WooResult<WCAddressVerificationResult> {
        val verifyAddressPayload = WooPayload(successfulVerifyAddressApiResponse)
        whenever(restClient.verifyAddress(
                site,
                address,
                ORIGIN
        )).thenReturn(verifyAddressPayload)

        whenever(restClient.verifyAddress(
                site,
                address,
                DESTINATION
        )).thenReturn(WooPayload(error))

        return store.verifyAddress(site, address, type)
    }

    private suspend fun getPackages(isError: Boolean = false): WooResult<WCPackagesResult> {
        val getPackagesPayload = WooPayload(samplePackagesApiResponse)
        if (isError) {
            whenever(restClient.getPackageTypes(any())).thenReturn(WooPayload(error))
        } else {
            whenever(restClient.getPackageTypes(any())).thenReturn(getPackagesPayload)
        }
        return store.getPackageTypes(site)
    }
}
