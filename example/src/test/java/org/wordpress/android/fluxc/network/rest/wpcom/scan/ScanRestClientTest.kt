package org.wordpress.android.fluxc.network.rest.wpcom.scan

import com.android.volley.RequestQueue
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel.Credentials
import org.wordpress.android.fluxc.model.scan.ScanStateModel.State
import org.wordpress.android.fluxc.model.scan.threat.ThreatMapper
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.scan.threat.Threat
import org.wordpress.android.fluxc.store.ScanStore.FetchedScanStatePayload
import org.wordpress.android.fluxc.store.ScanStore.ScanStartErrorType
import org.wordpress.android.fluxc.store.ScanStore.ScanStateErrorType
import org.wordpress.android.fluxc.test

@RunWith(MockitoJUnitRunner::class)
class ScanRestClientTest {
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var threatMapper: ThreatMapper

    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var scanRestClient: ScanRestClient
    private val siteId: Long = 12

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        scanRestClient = ScanRestClient(
            wpComGsonRequestBuilder,
            threatMapper,
            dispatcher,
            null,
            requestQueue,
            accessToken,
            userAgent
        )
    }

    @Test
    fun `fetch scan state builds correct request url`() = test {
        val successResponseJson =
            UnitTestUtils.getStringFromResourceFile(javaClass, JP_SCAN_DAILY_SCAN_IDLE_WITH_THREATS_JSON)
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        initFetchScanState(scanResponse)

        scanRestClient.fetchScanState(site)

        assertEquals(urlCaptor.firstValue, "$API_BASE_PATH/sites/${site.siteId}/scan/")
    }

    @Test
    fun `fetch scan state dispatches response on success`() = test {
        val successResponseJson =
            UnitTestUtils.getStringFromResourceFile(javaClass, JP_SCAN_DAILY_SCAN_IDLE_WITH_THREATS_JSON)
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        initFetchScanState(scanResponse)

        val payload = scanRestClient.fetchScanState(site)

        with(payload) {
            assertEquals(site, this@ScanRestClientTest.site)
            assertNull(error)
            assertNotNull(scanStateModel)
            requireNotNull(scanStateModel).apply {
                assertEquals(state, State.fromValue(requireNotNull(scanResponse.state)))
                assertEquals(hasCloud, requireNotNull(scanResponse.hasCloud))
                assertNull(reason)
                assertNotNull(credentials)
                assertNotNull(threats)
                mostRecentStatus?.apply {
                    assertEquals(progress, scanResponse.mostRecentStatus?.progress)
                    assertEquals(startDate, scanResponse.mostRecentStatus?.startDate)
                    assertEquals(duration, scanResponse.mostRecentStatus?.duration)
                    assertEquals(error, scanResponse.mostRecentStatus?.error)
                    assertEquals(isInitial, scanResponse.mostRecentStatus?.isInitial)
                }
                currentStatus?.apply {
                    assertEquals(progress, scanResponse.mostRecentStatus?.progress)
                    assertEquals(startDate, scanResponse.mostRecentStatus?.startDate)
                    assertEquals(isInitial, scanResponse.mostRecentStatus?.isInitial)
                }
                credentials?.forEachIndexed { index, creds ->
                    creds.apply {
                        assertEquals(type, scanResponse.credentials?.get(index)?.type)
                        assertEquals(role, scanResponse.credentials?.get(index)?.role)
                        assertEquals(host, scanResponse.credentials?.get(index)?.host)
                        assertEquals(port, scanResponse.credentials?.get(index)?.port)
                        assertEquals(user, scanResponse.credentials?.get(index)?.user)
                        assertEquals(path, scanResponse.credentials?.get(index)?.path)
                        assertEquals(stillValid, scanResponse.credentials?.get(index)?.stillValid)
                    }
                }
                assertEquals(threats?.size, scanResponse.threats?.size)
            }
        }
    }

    @Test
    fun `fetch scan state dispatches most recent status for idle state`() = test {
        val successResponseJson = UnitTestUtils.getStringFromResourceFile(javaClass, JP_COMPLETE_SCAN_IDLE_JSON)
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        initFetchScanState(scanResponse)

        val payload = scanRestClient.fetchScanState(site)

        with(payload) {
            assertNotNull(scanStateModel?.mostRecentStatus)
            assertEquals(scanStateModel?.state, State.IDLE)
        }
    }

    @Test
    fun `fetch scan state dispatches empty creds when server creds not setup for site with scan capability`() = test {
        val successResponseJson = UnitTestUtils.getStringFromResourceFile(
            javaClass,
            JP_SCAN_DAILY_SCAN_IDLE_WITH_THREAT_WITHOUT_SERVER_CREDS_JSON
        )
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        initFetchScanState(scanResponse)

        val payload = scanRestClient.fetchScanState(site)

        with(payload) {
            assertEquals(scanStateModel?.credentials, emptyList<Credentials>())
            assertEquals(scanStateModel?.state, State.IDLE)
        }
    }

    @Test
    fun `fetch scan state dispatches empty threats if no threats found for site with scan capability`() = test {
        val successResponseJson = UnitTestUtils.getStringFromResourceFile(javaClass, JP_COMPLETE_SCAN_IDLE_JSON)

        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        initFetchScanState(scanResponse)

        val payload = scanRestClient.fetchScanState(site)

        with(payload) {
            assertEquals(scanStateModel?.threats, emptyList<Threat>())
            assertEquals(scanStateModel?.state, State.IDLE)
        }
    }

    @Test
    fun `fetch scan state dispatches current progress status for scanning state`() = test {
        val successResponseJson = UnitTestUtils.getStringFromResourceFile(javaClass, JP_COMPLETE_SCAN_SCANNING_JSON)
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        initFetchScanState(scanResponse)

        val payload = scanRestClient.fetchScanState(site)

        with(payload) {
            assertNotNull(scanStateModel?.currentStatus)
            assertEquals(scanStateModel?.state, State.SCANNING)
        }
    }

    @Test
    fun `fetch scan state dispatches reason, null threats and creds for scan unavailable state`() = test {
        val successResponseJson = UnitTestUtils.getStringFromResourceFile(
            javaClass,
            JP_BACKUP_DAILY_SCAN_UNAVAILABLE_JSON
        )
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        initFetchScanState(scanResponse)

        val payload = scanRestClient.fetchScanState(site)

        with(payload) {
            assertNull(scanStateModel?.credentials)
            assertNull(scanStateModel?.threats)
            assertNotNull(scanStateModel?.reason)
            assertEquals(scanStateModel?.state, State.UNAVAILABLE)
        }
    }

    @Test
    fun `fetch scan state dispatches generic error on failure`() = test {
        initFetchScanState(error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)))

        val payload = scanRestClient.fetchScanState(site)

        assertEmittedScanStatusError(payload, ScanStateErrorType.GENERIC_ERROR)
    }

    @Test
    fun `fetch scan state dispatches error on wrong state`() = test {
        val successResponseJson = UnitTestUtils.getStringFromResourceFile(javaClass, JP_COMPLETE_SCAN_IDLE_JSON)
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)

        initFetchScanState(scanResponse?.copy(state = "wrong"))

        val payload = scanRestClient.fetchScanState(site)

        assertEmittedScanStatusError(payload, ScanStateErrorType.INVALID_RESPONSE)
    }

    @Test
    fun `start scan builds correct request url`() = test {
        val scanStartResponse = ScanStartResponse(success = true)
        initStartScan(scanStartResponse)

        scanRestClient.startScan(site)

        assertEquals(urlCaptor.firstValue, "$API_BASE_PATH/sites/${site.siteId}/scan/enqueue/")
    }

    @Test
    fun `start scan dispatches response on success`() = test {
        val scanStartResponse = ScanStartResponse(success = true)
        initStartScan(scanStartResponse)

        val payload = scanRestClient.startScan(site)

        with(payload) {
            assertEquals(site, this@ScanRestClientTest.site)
            assertNull(error)
        }
    }

    @Test
    fun `start scan dispatches api error on failure from api`() = test {
        val errorResponseJson =
            UnitTestUtils.getStringFromResourceFile(javaClass, JP_BACKUP_DAILY_START_SCAN_ERROR_JSON)
        val startScanResponse = getStartScanResponseFromJsonString(errorResponseJson)
        initStartScan(startScanResponse)

        val payload = scanRestClient.startScan(site)

        with(payload) {
            assertEquals(site, this@ScanRestClientTest.site)
            assertTrue(isError)
            assertEquals(ScanStartErrorType.API_ERROR, error.type)
        }
    }

    private fun assertEmittedScanStatusError(payload: FetchedScanStatePayload, errorType: ScanStateErrorType) {
        with(payload) {
            assertEquals(site, this@ScanRestClientTest.site)
            assertTrue(isError)
            assertEquals(errorType, error.type)
        }
    }

    private fun getScanStateResponseFromJsonString(json: String): ScanStateResponse {
        val responseType = object : TypeToken<ScanStateResponse>() {}.type
        return Gson().fromJson(json, responseType) as ScanStateResponse
    }

    private fun getStartScanResponseFromJsonString(json: String): ScanStartResponse {
        val responseType = object : TypeToken<ScanStartResponse>() {}.type
        return Gson().fromJson(json, responseType) as ScanStartResponse
    }

    private suspend fun initFetchScanState(
        data: ScanStateResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<ScanStateResponse> {
        val nonNullData = data ?: mock()
        val response = if (error != null) Response.Error(error) else Success(nonNullData)
        whenever(
            wpComGsonRequestBuilder.syncGetRequest(
                eq(scanRestClient),
                urlCaptor.capture(),
                eq(mapOf()),
                eq(ScanStateResponse::class.java),
                eq(false),
                any(),
                eq(false)
            )
        ).thenReturn(response)
        whenever(site.siteId).thenReturn(siteId)

        val threatModel = mock<ThreatModel>()
        whenever(threatMapper.map(any())).thenReturn(threatModel)

        return response
    }

    private suspend fun initStartScan(
        data: ScanStartResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<ScanStartResponse> {
        val nonNullData = data ?: mock()
        val response = if (error != null) Response.Error(error) else Success(nonNullData)
        whenever(
            wpComGsonRequestBuilder.syncPostRequest(
                eq(scanRestClient),
                urlCaptor.capture(),
                eq(mapOf()),
                anyOrNull(),
                eq(ScanStartResponse::class.java)
            )
        ).thenReturn(response)
        whenever(site.siteId).thenReturn(siteId)
        return response
    }

    companion object {
        private const val API_BASE_PATH = "https://public-api.wordpress.com/wpcom/v2"
        private const val JP_COMPLETE_SCAN_IDLE_JSON = "wp/jetpack/scan/jp-complete-scan-idle.json"
        private const val JP_COMPLETE_SCAN_SCANNING_JSON = "wp/jetpack/scan/jp-complete-scan-scanning.json"
        private const val JP_BACKUP_DAILY_SCAN_UNAVAILABLE_JSON =
            "wp/jetpack/scan/jp-backup-daily-scan-unavailable.json"
        private const val JP_SCAN_DAILY_SCAN_IDLE_WITH_THREATS_JSON =
            "wp/jetpack/scan/jp-scan-daily-scan-idle-with-threat.json"
        private const val JP_SCAN_DAILY_SCAN_IDLE_WITH_THREAT_WITHOUT_SERVER_CREDS_JSON =
            "wp/jetpack/scan/jp-scan-daily-scan-idle-with-threat-without-server-creds.json"
        private const val JP_BACKUP_DAILY_START_SCAN_ERROR_JSON =
            "wp/jetpack/scan/jp-backup-daily-start-scan-error.json"
    }
}
