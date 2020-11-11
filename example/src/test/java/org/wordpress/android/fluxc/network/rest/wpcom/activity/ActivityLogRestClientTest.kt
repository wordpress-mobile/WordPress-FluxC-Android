package org.wordpress.android.fluxc.network.rest.wpcom.activity

import com.android.volley.RequestQueue
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.after
import com.nhaarman.mockitokotlin2.any
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
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient.ActivitiesResponse
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient.ActivitiesResponse.Page
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient.RewindResponse
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient.RewindStatusResponse
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.ActivityLogStore.ActivityLogErrorType
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchActivityLogPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchedActivityLogPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchedRewindStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindStatusErrorType
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.util.DateTimeUtils
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class ActivityLogRestClientTest {
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var requestPayload: FetchActivityLogPayload
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent
    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var activityRestClient: ActivityLogRestClient
    private val siteId: Long = 12
    private val number = 10
    private val offset = 0

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        activityRestClient = ActivityLogRestClient(dispatcher,
                wpComGsonRequestBuilder,
                null,
                requestQueue,
                accessToken,
                userAgent)
        whenever(requestPayload.site).thenReturn(site)
    }

    @Test
    fun fetchActivity_passesCorrectParamToBuildRequest() = test {
        initFetchActivity()
        val afterMillis = 1603860428000
        val beforeMillis = 1603961628000
        val payload = FetchActivityLogPayload(
                site,
                false,
                Date(afterMillis),
                Date(beforeMillis),
                listOf("post", "attachment")
        )

        activityRestClient.fetchActivity(payload, number, offset)

        assertEquals(urlCaptor.firstValue, "https://public-api.wordpress.com/wpcom/v2/sites/$siteId/activity/")
        with(paramsCaptor.firstValue) {
            assertEquals("1", this["page"])
            assertEquals("$number", this["number"])
            assertEquals(DateTimeUtils.iso8601FromDate(Date(afterMillis)), this["after"])
            assertEquals(DateTimeUtils.iso8601FromDate(Date(beforeMillis)), this["before"])
            assertEquals("post", this["group[0]"])
            assertEquals("attachment", this["group[1]"])
        }
    }

    @Test
    fun fetchActivity_passesOnlyNonEmptyParamsToBuildRequest() = test {
        initFetchActivity()
        val payload = FetchActivityLogPayload(
                site,
                false,
                after = null,
                before = null,
                groups = listOf()
        )
        activityRestClient.fetchActivity(payload, number, offset)

        assertEquals(urlCaptor.firstValue, "https://public-api.wordpress.com/wpcom/v2/sites/$siteId/activity/")
        with(paramsCaptor.firstValue) {
            assertEquals("1", this["page"])
            assertEquals("$number", this["number"])
            assertEquals(2, this.size)
        }
    }

    @Test
    fun fetchActivity_dispatchesResponseOnSuccess() = test {
        val response = ActivitiesResponse(1, "response", ACTIVITY_RESPONSE_PAGE)
        initFetchActivity(response)

        val payload = activityRestClient.fetchActivity(requestPayload, number, offset)

        with(payload) {
            assertEquals(this.number, number)
            assertEquals(this.offset, offset)
            assertEquals(this.totalItems, 1)
            assertEquals(this.site, site)
            assertEquals(this.activityLogModels.size, 1)
            assertNull(this.error)
            with(this.activityLogModels[0]) {
                assertEquals(this.activityID, ACTIVITY_RESPONSE.activity_id)
                assertEquals(this.gridicon, ACTIVITY_RESPONSE.gridicon)
                assertEquals(this.name, ACTIVITY_RESPONSE.name)
                assertEquals(this.published, ACTIVITY_RESPONSE.published)
                assertEquals(this.rewindID, ACTIVITY_RESPONSE.rewind_id)
                assertEquals(this.rewindable, ACTIVITY_RESPONSE.is_rewindable)
                assertEquals(this.content, ACTIVITY_RESPONSE.content)
                assertEquals(this.actor?.avatarURL, ACTIVITY_RESPONSE.actor?.icon?.url)
                assertEquals(this.actor?.wpcomUserID, ACTIVITY_RESPONSE.actor?.wpcom_user_id)
            }
        }
    }

    @Test
    fun fetchActivity_dispatchesErrorOnMissingActivityId() = test {
        val failingPage = Page(listOf(ACTIVITY_RESPONSE.copy(activity_id = null)))
        val activitiesResponse = ActivitiesResponse(1, "response", failingPage)
        initFetchActivity(activitiesResponse)

        val payload = activityRestClient.fetchActivity(requestPayload, number, offset)

        assertEmittedActivityError(payload, ActivityLogErrorType.MISSING_ACTIVITY_ID)
    }

    @Test
    fun fetchActivity_dispatchesErrorOnMissingSummary() = test {
        val failingPage = Page(listOf(ACTIVITY_RESPONSE.copy(summary = null)))
        val activitiesResponse = ActivitiesResponse(1, "response", failingPage)
        initFetchActivity(activitiesResponse)

        val payload = activityRestClient.fetchActivity(requestPayload, number, offset)

        assertEmittedActivityError(payload, ActivityLogErrorType.MISSING_SUMMARY)
    }

    @Test
    fun fetchActivity_dispatchesErrorOnMissingContentText() = test {
        val emptyContent = FormattableContent(null)
        val failingPage = Page(listOf(ACTIVITY_RESPONSE.copy(content = emptyContent)))
        val activitiesResponse = ActivitiesResponse(1, "response", failingPage)
        initFetchActivity(activitiesResponse)

        val payload = activityRestClient.fetchActivity(requestPayload, number, offset)

        assertEmittedActivityError(payload, ActivityLogErrorType.MISSING_CONTENT_TEXT)
    }

    @Test
    fun fetchActivity_dispatchesErrorOnMissingPublishedDate() = test {
        val failingPage = Page(listOf(ACTIVITY_RESPONSE.copy(published = null)))
        val activitiesResponse = ActivitiesResponse(1, "response", failingPage)
        initFetchActivity(activitiesResponse)

        val payload = activityRestClient.fetchActivity(requestPayload, number, offset)

        assertEmittedActivityError(payload, ActivityLogErrorType.MISSING_PUBLISHED_DATE)
    }

    @Test
    fun fetchActivity_dispatchesErrorOnFailure() = test {
        initFetchActivity(error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.NETWORK_ERROR)))

        val payload = activityRestClient.fetchActivity(requestPayload, number, offset)

        assertEmittedActivityError(payload, ActivityLogErrorType.GENERIC_ERROR)
    }

    @Test
    fun fetchActivityRewind_dispatchesResponseOnSuccess() = test {
        val state = RewindStatusModel.State.ACTIVE
        val rewindResponse = REWIND_STATUS_RESPONSE.copy(state = state.value)
        initFetchRewindStatus(rewindResponse)

        val payload = activityRestClient.fetchActivityRewind(site)

        with(payload) {
            assertEquals(this.site, site)
            assertNull(this.error)
            assertNotNull(this.rewindStatusModelResponse)
            this.rewindStatusModelResponse?.apply {
                assertEquals(this.reason, REWIND_STATUS_RESPONSE.reason)
                assertEquals(this.state, state)
                assertNotNull(this.rewind)
                this.rewind?.apply {
                    assertEquals(this.status.value, REWIND_RESPONSE.status)
                }
            }
        }
    }

    @Test
    fun fetchActivityRewind_dispatchesGenericErrorOnFailure() = test {
        initFetchRewindStatus(error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)))

        val payload = activityRestClient.fetchActivityRewind(site)

        assertEmittedRewindStatusError(payload, RewindStatusErrorType.GENERIC_ERROR)
    }

    @Test
    fun fetchActivityRewind_dispatchesErrorOnWrongState() = test {
        initFetchRewindStatus(REWIND_STATUS_RESPONSE.copy(state = "wrong"))

        val payload = activityRestClient.fetchActivityRewind(site)

        assertEmittedRewindStatusError(payload, RewindStatusErrorType.INVALID_RESPONSE)
    }

    @Test
    fun fetchActivityRewind_dispatchesErrorOnMissingRestoreId() = test {
        initFetchRewindStatus(REWIND_STATUS_RESPONSE.copy(rewind = REWIND_RESPONSE.copy(rewind_id = null)))

        val payload = activityRestClient.fetchActivityRewind(site)

        assertEmittedRewindStatusError(payload, RewindStatusErrorType.MISSING_REWIND_ID)
    }

    @Test
    fun fetchActivityRewind_dispatchesErrorOnWrongRestoreStatus() = test {
        initFetchRewindStatus(REWIND_STATUS_RESPONSE.copy(rewind = REWIND_RESPONSE.copy(status = "wrong")))

        val payload = activityRestClient.fetchActivityRewind(site)

        assertEmittedRewindStatusError(payload, RewindStatusErrorType.INVALID_REWIND_STATE)
    }

    @Test
    fun postRewindOperation() = test {
        val restoreId = 10L
        val response = RewindResponse(restoreId, true, null)
        initPostRewind(response)

        val payload = activityRestClient.rewind(site, "rewindId")

        assertEquals(restoreId, payload.restoreId)
    }

    @Test
    fun postRewindOperationError() = test {
        initPostRewind(error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)))

        val payload = activityRestClient.rewind(site, "rewindId")

        assertTrue(payload.isError)
    }

    @Test
    fun postRewindApiError() = test {
        val restoreId = 10L
        initPostRewind(RewindResponse(restoreId, false, "error"))

        val payload = activityRestClient.rewind(site, "rewindId")

        assertTrue(payload.isError)
    }

    private fun assertEmittedActivityError(payload: FetchedActivityLogPayload, errorType: ActivityLogErrorType) {
        with(payload) {
            assertEquals(this.number, number)
            assertEquals(this.offset, offset)
            assertEquals(this.site, site)
            assertTrue(this.isError)
            assertEquals(this.error.type, errorType)
        }
    }

    private fun assertEmittedRewindStatusError(payload: FetchedRewindStatePayload, errorType: RewindStatusErrorType) {
        with(payload) {
            assertEquals(this.site, site)
            assertTrue(this.isError)
            assertEquals(errorType, this.error.type)
        }
    }

    private suspend fun initFetchActivity(
        data: ActivitiesResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<ActivitiesResponse> {
        val nonNullData = data ?: mock()
        val response = if (error != null) Response.Error<ActivitiesResponse>(error) else Success(nonNullData)
        whenever(wpComGsonRequestBuilder.syncGetRequest(
                eq(activityRestClient),
                urlCaptor.capture(),
                paramsCaptor.capture(),
                eq(ActivitiesResponse::class.java),
                eq(false),
                any(),
                eq(false))
        ).thenReturn(response)
        whenever(site.siteId).thenReturn(siteId)
        return response
    }

    private suspend fun initFetchRewindStatus(
        data: RewindStatusResponse? = null,
        error: WPComGsonNetworkError? = null
    ):
            Response<RewindStatusResponse> {
        val nonNullData = data ?: mock()
        val response = if (error != null) Response.Error<RewindStatusResponse>(error) else Success(nonNullData)
        whenever(wpComGsonRequestBuilder.syncGetRequest(
                eq(activityRestClient),
                urlCaptor.capture(),
                paramsCaptor.capture(),
                eq(RewindStatusResponse::class.java),
                eq(false),
                any(),
                eq(false))).thenReturn(response)
        whenever(site.siteId).thenReturn(siteId)
        return response
    }

    private suspend fun initPostRewind(
        data: RewindResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<RewindResponse> {
        val nonNullData = data ?: mock()
        val response = if (error != null) Response.Error<RewindResponse>(error) else Success(nonNullData)

        whenever(wpComGsonRequestBuilder.syncPostRequest(
                eq(activityRestClient),
                urlCaptor.capture(),
                eq(null),
                eq(mapOf()),
                eq(RewindResponse::class.java)
        )).thenReturn(response)
        whenever(site.siteId).thenReturn(siteId)
        return response
    }
}
