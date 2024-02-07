package org.wordpress.android.fluxc.store.blaze

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeAdForecast
import org.wordpress.android.fluxc.model.blaze.BlazeAdSuggestion
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignsModel
import org.wordpress.android.fluxc.model.blaze.BlazeTargetingDevice
import org.wordpress.android.fluxc.model.blaze.BlazeTargetingLanguage
import org.wordpress.android.fluxc.model.blaze.BlazeTargetingLocation
import org.wordpress.android.fluxc.model.blaze.BlazeTargetingTopic
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsError
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsFetchedPayload
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsUtils
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCreationRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.Campaign
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.CampaignStats
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.ContentConfig
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao.BlazeCampaignEntity
import org.wordpress.android.fluxc.persistence.blaze.BlazeTargetingDao
import org.wordpress.android.fluxc.persistence.blaze.BlazeTargetingDeviceEntity
import org.wordpress.android.fluxc.persistence.blaze.BlazeTargetingLanguageEntity
import org.wordpress.android.fluxc.persistence.blaze.BlazeTargetingTopicEntity
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import java.util.Date
import kotlin.time.Duration.Companion.days

const val SITE_ID = 1L

/* Campaign */
const val CAMPAIGN_ID = 1
const val TITLE = "title"
const val IMAGE_URL = "imageUrl"
const val CREATED_AT = "2023-06-02T00:00:00.000Z"
const val END_DATE = "2023-06-02T00:00:00.000Z"
const val UI_STATUS = "rejected"
const val BUDGET_CENTS = 5000L
const val IMPRESSIONS = 0L
const val CLICKS = 0L

const val PAGE = 1
const val TOTAL_ITEMS = 1
const val TOTAL_PAGES = 1

private val CONTENT_CONFIG_RESPONSE = ContentConfig(
    title = TITLE,
    imageUrl = IMAGE_URL
)

private val CONTENT_CAMPAIGN_STATS = CampaignStats(
    impressionsTotal = 0,
    clicksTotal = 0
)

private val CAMPAIGN_RESPONSE = Campaign(
    campaignId = CAMPAIGN_ID,
    createdAt = CREATED_AT,
    endDate = END_DATE,
    budgetCents = BUDGET_CENTS,
    uiStatus = UI_STATUS,
    contentConfig = CONTENT_CONFIG_RESPONSE,
    campaignStats = CONTENT_CAMPAIGN_STATS,
    targetUrn = "urn:wpcom:post:199247490:9"
)

private val BLAZE_CAMPAIGNS_RESPONSE = BlazeCampaignsResponse(
    campaigns = listOf(CAMPAIGN_RESPONSE),
    page = PAGE,
    totalItems = TOTAL_ITEMS,
    totalPages = TOTAL_PAGES
)

private val BLAZE_CAMPAIGN_MODEL = BlazeCampaignEntity(
    siteId = SITE_ID,
    campaignId = CAMPAIGN_ID,
    title = TITLE,
    imageUrl = IMAGE_URL,
    createdAt = BlazeCampaignsUtils.stringToDate(CREATED_AT),
    endDate = BlazeCampaignsUtils.stringToDate(END_DATE),
    uiStatus = UI_STATUS,
    budgetCents = BUDGET_CENTS,
    impressions = IMPRESSIONS,
    clicks = CLICKS,
    targetUrn = "urn:wpcom:post:199247490:9"
)
private val BLAZE_CAMPAIGNS_MODEL = BlazeCampaignsModel(
    campaigns = listOf(BLAZE_CAMPAIGN_MODEL.toDomainModel()),
    page = PAGE,
    totalItems = TOTAL_ITEMS,
    totalPages = TOTAL_PAGES
)

private val NO_RESULTS_BLAZE_CAMPAIGNS_MODEL = BlazeCampaignsModel(
    campaigns = listOf(),
    page = 1,
    totalItems = 0,
    totalPages = 0
)

class BlazeCampaignsStoreTest {
    private val restClient: BlazeCampaignsRestClient = mock()
    private val creationRestClient: BlazeCreationRestClient = mock()
    private val blazeCampaignsDao: BlazeCampaignsDao = mock()
    private val blazeTargetingDao: BlazeTargetingDao = mock()
    private val siteModel = SiteModel().apply { siteId = SITE_ID }

    private lateinit var store: BlazeCampaignsStore

    private val successResponse = BLAZE_CAMPAIGNS_RESPONSE
    private val errorResponse = BlazeCampaignsError(type = GENERIC_ERROR)

    @Before
    fun setUp() {
        store = BlazeCampaignsStore(
            restClient = restClient,
            creationRestClient = creationRestClient,
            campaignsDao = blazeCampaignsDao,
            targetingDao = blazeTargetingDao,
            coroutineEngine = initCoroutineEngine()
        )
    }

    @Test
    fun `given success, when fetch blaze campaigns is triggered, then values are inserted`() =
        test {
            val payload = BlazeCampaignsFetchedPayload(successResponse)
            whenever(restClient.fetchBlazeCampaigns(siteModel, PAGE)).thenReturn(payload)

            store.fetchBlazeCampaigns(siteModel, PAGE)

            verify(blazeCampaignsDao).insertCampaignsAndPageInfoForSite(
                SITE_ID,
                BLAZE_CAMPAIGNS_MODEL
            )
        }

    @Test
    fun `given error, when fetch blaze campaigns is triggered, then error result is returned`() =
        test {
            whenever(restClient.fetchBlazeCampaigns(any(), any())).thenReturn(
                BlazeCampaignsFetchedPayload(errorResponse)
            )
            val result = store.fetchBlazeCampaigns(siteModel)

            verifyNoInteractions(blazeCampaignsDao)
            assertThat(result.model).isNull()
            assertEquals(GENERIC_ERROR, result.error.type)
            assertNull(result.error.message)
        }

    @Test
    fun `given unmatched site, when get is triggered, then empty campaigns list returned`() = test {
        whenever(blazeCampaignsDao.getCampaignsAndPaginationForSite(SITE_ID)).thenReturn(
            NO_RESULTS_BLAZE_CAMPAIGNS_MODEL
        )

        val result = store.getBlazeCampaigns(siteModel)

        assertThat(result).isNotNull
        assertThat(result.campaigns).isEmpty()
        assertEquals(result.page, 1)
        assertEquals(result.totalPages, 0)
        assertEquals(result.totalItems, 0)
    }

    @Test
    fun `given matched site, when get recent is triggered, then campaign is returned`() = test {
        whenever(blazeCampaignsDao.getMostRecentCampaignForSite(SITE_ID)).thenReturn(
            BLAZE_CAMPAIGN_MODEL
        )

        val result = store.getMostRecentBlazeCampaign(siteModel)

        assertThat(result).isNotNull
        assertEquals(result?.campaignId, CAMPAIGN_ID)
        assertEquals(result?.title, TITLE)
        assertEquals(result?.imageUrl, IMAGE_URL)
        assertEquals(result?.createdAt, BlazeCampaignsUtils.stringToDate(CREATED_AT))
        assertEquals(result?.endDate, BlazeCampaignsUtils.stringToDate(END_DATE))
        assertEquals(result?.uiStatus, UI_STATUS)
        assertEquals(result?.budgetCents, BUDGET_CENTS)
        assertEquals(result?.impressions, IMPRESSIONS)
        assertEquals(result?.clicks, CLICKS)
    }

    @Test
    fun `given unmatched site, when get recent is triggered, then campaign is returned`() = test {
        val result = store.getMostRecentBlazeCampaign(siteModel)

        assertThat(result).isNull()
    }

    @Test
    fun `when fetching targeting locations, then locations are returned`() = test {
        whenever(creationRestClient.fetchTargetingLocations(any(), any(), any())).thenReturn(
            BlazeCreationRestClient.BlazePayload(
                List(10) {
                    BlazeTargetingLocation(
                        id = it.toLong(),
                        name = "location",
                        type = "city",
                        parent = null
                    )
                }
            )
        )

        val locations = store.fetchBlazeTargetingLocations(siteModel, "query")

        assertThat(locations.isError).isFalse()
        assertThat(locations.model).isNotNull
        assertThat(locations.model?.size).isEqualTo(10)
    }

    @Test
    fun `when fetching targeting topics, then persist data in DB`() = test {
        whenever(creationRestClient.fetchTargetingTopics(any(), any())).thenReturn(
            BlazeCreationRestClient.BlazePayload(
                List(10) {
                    BlazeTargetingTopic(
                        id = it.toString(),
                        description = "Topic $it"
                    )
                }
            )
        )

        store.fetchBlazeTargetingTopics(siteModel)

        verify(blazeTargetingDao).replaceTopics(any())
    }

    @Test
    fun `when observing targeting topics, then return data from DB`() = test {
        whenever(blazeTargetingDao.observeTopics(any())).thenReturn(
            flowOf(
                List(10) {
                    BlazeTargetingTopicEntity(
                        id = it.toString(),
                        description = "Topic $it",
                        locale = "en"
                    )
                }
            )
        )

        val topics = store.observeBlazeTargetingTopics().first()

        assertThat(topics).isNotNull
        assertThat(topics.size).isEqualTo(10)
    }

    @Test
    fun `when fetching targeting languages, then persist data in DB`() = test {
        whenever(creationRestClient.fetchTargetingLanguages(any(), any())).thenReturn(
            BlazeCreationRestClient.BlazePayload(
                List(10) {
                    BlazeTargetingLanguage(
                        id = it.toString(),
                        name = "Language $it"
                    )
                }
            )
        )

        store.fetchBlazeTargetingLanguages(siteModel)

        verify(blazeTargetingDao).replaceLanguages(any())
    }

    @Test
    fun `when observing targeting languages, then return data from DB`() = test {
        whenever(blazeTargetingDao.observeLanguages(any())).thenReturn(
            flowOf(
                List(10) {
                    BlazeTargetingLanguageEntity(
                        id = it.toString(),
                        name = "Language $it",
                        locale = "en"
                    )
                }
            )
        )

        val languages = store.observeBlazeTargetingLanguages().first()

        assertThat(languages).isNotNull
        assertThat(languages.size).isEqualTo(10)
    }

    @Test
    fun `when fetching targeting devices, then persist data in DB`() = test {
        whenever(creationRestClient.fetchTargetingDevices(any(), any())).thenReturn(
            BlazeCreationRestClient.BlazePayload(
                List(10) {
                    BlazeTargetingDevice(
                        id = it.toString(),
                        name = "Device $it"
                    )
                }
            )
        )

        store.fetchBlazeTargetingDevices(siteModel)

        verify(blazeTargetingDao).replaceDevices(any())
    }

    @Test
    fun `when observing targeting devices, then return data from DB`() = test {
        whenever(blazeTargetingDao.observeDevices(any())).thenReturn(
            flowOf(
                List(10) {
                    BlazeTargetingDeviceEntity(
                        id = it.toString(),
                        name = "Device $it",
                        locale = "en"
                    )
                }
            )
        )

        val devices = store.observeBlazeTargetingDevices().first()

        assertThat(devices).isNotNull
        assertThat(devices.size).isEqualTo(10)
    }

    @Test
    fun `when fetching targeting ad suggestions, then return data successfully`() = test {
        val suggestions = List(10) {
            BlazeAdSuggestion(
                tagLine = it.toString(),
                description = "Ad $it"
            )
        }

        whenever(creationRestClient.fetchAdSuggestions(any(), any())).thenReturn(
            BlazeCreationRestClient.BlazePayload(suggestions)
        )

        val suggestionsResult = store.fetchBlazeAdSuggestions(siteModel, 1L)

        assertThat(suggestionsResult.isError).isFalse()
        assertThat(suggestionsResult.model).isEqualTo(suggestions)
    }

    @Test
    fun `when fetching ad forecast, then return data successfully`() = test {
        val forecast = BlazeAdForecast(
            minImpressions = 100,
            maxImpressions = 200
        )

        whenever(
            creationRestClient.fetchAdForecast(
                site = any(),
                startDate = any(),
                endDate = any(),
                totalBudget = any(),
                timeZoneId = any(),
                targetingParameters = anyOrNull()
            )
        ).thenReturn(
            BlazeCreationRestClient.BlazePayload(forecast)
        )

        val forecastResult = store.fetchBlazeAdForecast(
            siteModel,
            Date(),
            Date(System.currentTimeMillis() + 7.days.inWholeMilliseconds),
            100.0,
        )

        assertThat(forecastResult.isError).isFalse()
        assertThat(forecastResult.model).isEqualTo(forecast)
    }
}
