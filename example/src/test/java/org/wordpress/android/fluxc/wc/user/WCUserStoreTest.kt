package org.wordpress.android.fluxc.wc.user

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
import org.wordpress.android.fluxc.model.user.WCUserMapper
import org.wordpress.android.fluxc.model.user.WCUserRole
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.user.WCUserRestClient
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WCUserStore
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCUserStoreTest {
    private val restClient = mock <WCUserRestClient>()
    private val site = SiteModel().apply { id = 321 }
    private val errorSite = SiteModel().apply { id = 123 }
    private val mapper = WCUserMapper()
    private lateinit var store: WCUserStore

    private val sampleUserApiResponse = WCUserTestUtils.generateSampleUApiResponse()
    private val error = WooError(INVALID_RESPONSE, NETWORK_ERROR, "Invalid site ID")

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(
                        SiteModel::class.java
                ),
                WellSqlConfig.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()

        store = WCUserStore(
                restClient,
                initCoroutineEngine(),
                mapper
        )

        // Insert the site into the db so it's available later when testing user role
        SiteSqlUtils.insertOrUpdateSite(site)
    }

    @Test
    fun `fetch user role`() = test {
        val result = fetchUserRole()
        val userRole = mapper.map(sampleUserApiResponse!!)
        assertThat(result.model?.size).isEqualTo(userRole.size)
        assertThat(result.model).isEqualTo(userRole)
        assertThat(result.model?.get(0)?.isSupported() == true)

        val invalidRequestResult = store.fetchUserRole(errorSite)
        assertThat(invalidRequestResult.model).isNull()
        assertThat(invalidRequestResult.error).isEqualTo(error)
    }

    private suspend fun fetchUserRole(): WooResult<List<WCUserRole>> {
        val fetchUserRolePayload = WooPayload(sampleUserApiResponse)
        whenever(restClient.fetchUserInfo(site)).thenReturn(fetchUserRolePayload)
        whenever(restClient.fetchUserInfo(errorSite)).thenReturn(WooPayload(error))
        return store.fetchUserRole(site)
    }
}
