package org.wordpress.android.fluxc.wc.taxes

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
import org.wordpress.android.fluxc.model.taxes.WCTaxClassMapper
import org.wordpress.android.fluxc.model.taxes.WCTaxClassModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.taxes.WCTaxRestClient
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WCTaxStore
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCTaxStoreTest {
    private val restClient = mock<WCTaxRestClient>()
    private val site = SiteModel().apply { id = 321 }
    private val errorSite = SiteModel().apply { id = 123 }
    private val mapper = WCTaxClassMapper()
    private lateinit var store: WCTaxStore

    private val sampleTaxClassList = TaxTestUtils.generateSampleTaxClassApiResponse()
    private val error = WooError(INVALID_RESPONSE, NETWORK_ERROR, "Invalid site ID")

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(SiteModel::class.java, WCTaxClassModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()

        store = WCTaxStore(
                restClient,
                initCoroutineEngine(),
                mapper
        )

        // Insert the site into the db so it's available later when fetching tax classes
        SiteSqlUtils.insertOrUpdateSite(site)
    }

    @Test
    fun `fetch tax class list for site`() = test {
        val result = fetchTaxClassListForSite()

        assertThat(result.model?.size).isEqualTo(sampleTaxClassList.size)
        assertThat(result.model?.first()?.name).isEqualTo(mapper.map(sampleTaxClassList.first()).name)
        assertThat(result.model?.first()?.slug).isEqualTo(mapper.map(sampleTaxClassList.first()).slug)

        val invalidRequestResult = store.fetchTaxClassList(errorSite)
        assertThat(invalidRequestResult.model).isNull()
        assertThat(invalidRequestResult.error).isEqualTo(error)
    }

    @Test
    fun `get stored tax class list for site`() = test {
        fetchTaxClassListForSite()

        val storedTaxClassList = store.getTaxClassListForSite(site)

        assertThat(storedTaxClassList.size).isEqualTo(2)
        assertThat(storedTaxClassList.first().name).isEqualTo(mapper.map(sampleTaxClassList.first()).name)
        assertThat(storedTaxClassList.first().slug).isEqualTo(mapper.map(sampleTaxClassList.first()).slug)

        val invalidRequestResult = store.getTaxClassListForSite(errorSite)
        assertThat(invalidRequestResult.size).isEqualTo(0)
    }

    private suspend fun fetchTaxClassListForSite(): WooResult<List<WCTaxClassModel>> {
        val fetchTaxClassListPayload = WooPayload(sampleTaxClassList.toTypedArray())
        whenever(restClient.fetchTaxClassList(site)).thenReturn(fetchTaxClassListPayload)
        whenever(restClient.fetchTaxClassList(errorSite)).thenReturn(WooPayload(error))
        return store.fetchTaxClassList(site)
    }
}
