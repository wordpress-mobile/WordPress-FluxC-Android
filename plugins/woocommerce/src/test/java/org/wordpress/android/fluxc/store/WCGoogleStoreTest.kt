package org.wordpress.android.fluxc.store

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.google.WCGoogleAdsCampaignMapper
import org.wordpress.android.fluxc.model.google.WCGoogleAdsProgramsMapper
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.PARSE_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.google.GoogleAdsCampaignDTO
import org.wordpress.android.fluxc.network.rest.wpcom.wc.google.GoogleAdsTotalsDTO
import org.wordpress.android.fluxc.network.rest.wpcom.wc.google.WCGoogleAdsProgramsDTO
import org.wordpress.android.fluxc.network.rest.wpcom.wc.google.WCGoogleRestClient
import org.wordpress.android.fluxc.store.WCGoogleStore.TotalsType.CLICKS
import org.wordpress.android.fluxc.store.WCGoogleStore.TotalsType.SALES
import org.wordpress.android.fluxc.store.WCGoogleStore.TotalsType.SPEND
import org.wordpress.android.fluxc.utils.initCoroutineEngine

@ExperimentalCoroutinesApi
class WCGoogleStoreTest {

    @Mock
    lateinit var restClient: WCGoogleRestClient

    @Mock
    lateinit var wcGoogleAdsCampaignMapper: WCGoogleAdsCampaignMapper

    private lateinit var wcGoogleStore: WCGoogleStore
    private val siteModel = SiteModel()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        wcGoogleStore = WCGoogleStore(
            restClient = restClient,
            coroutineEngine = initCoroutineEngine(),
            campaignMapper = wcGoogleAdsCampaignMapper,
            programsMapper = WCGoogleAdsProgramsMapper()
        )
    }

    @Test
    fun `when programs response is a single page, then return the data directly`() = runBlockingTest {
        // Given
        val mockPrograms = createProgramsPage()
        val expectedModel = WCGoogleAdsProgramsMapper().mapToModel(mockPrograms)

        whenever(restClient.fetchAllPrograms(
            siteModel,
            "2020-01-01",
            "2020-12-31",
            "sales,clicks",
            "sales",
            null
        )).thenReturn(WooPayload(mockPrograms))

        // When
        val result = wcGoogleStore.fetchAllPrograms(
            site = siteModel,
            startDate = "2020-01-01",
            endDate = "2020-12-31",
            totals = listOf(SALES, CLICKS)
        )

        // Then
        assertFalse(result.isError)
        assertNotNull(result.model)
        assertEquals(result.model, expectedModel)
    }

    @Test
    fun `when programs response is paginated, requested everything and return the sum`() = runBlockingTest  {
        // Given
        val firstPage = createProgramsPage(pageNumber = 1)
        val pageTwo = createProgramsPage(pageNumber = 2)

        whenever(restClient.fetchAllPrograms(
            siteModel,
            "2020-01-01",
            "2020-12-31",
            "sales,clicks",
            "sales",
            null
        )).thenReturn(WooPayload(firstPage))

        whenever(restClient.fetchAllPrograms(
            siteModel,
            "2020-01-01",
            "2020-12-31",
            "sales,clicks",
            "sales",
            "nextPageToken"
        )).thenReturn(WooPayload(null))

        // When
        val result = wcGoogleStore.fetchAllPrograms(
            site = siteModel,
            startDate = "2020-01-01",
            endDate = "2020-12-31",
            totals = listOf(SALES, CLICKS)
        )

        // Then
        assertFalse(result.isError)
        assertNotNull(result.model)
    }

    @Test
    fun `when programs response is null, then return error`() = runBlockingTest {
        whenever(restClient.fetchAllPrograms(siteModel, "2020-01-01", "2020-12-31", "sales", "sales", null))
            .thenReturn(WooPayload(null))
        val result = wcGoogleStore.fetchAllPrograms(siteModel, "2020-01-01", "2020-12-31", listOf(SALES, SPEND))
        assertNull(result.model)
    }

    @Test
    fun `when programs response is error, then return error`() = runBlockingTest {
        whenever(restClient.fetchAllPrograms(siteModel, "2020-01-01", "2020-12-31", "sales", "sales", null))
            .thenReturn(WooPayload(WooError(
                type = GENERIC_ERROR,
                original = PARSE_ERROR
            )))
        val result = wcGoogleStore.fetchAllPrograms(siteModel, "2020-01-01", "2020-12-31", listOf(SALES))
        assertTrue(result.isError)
    }

    private fun createProgramsPage(pageNumber: Int = 1) = WCGoogleAdsProgramsDTO(
        campaigns = listOf(
            GoogleAdsCampaignDTO(
                id = pageNumber.toLong(),
                name = "campaign${pageNumber}",
                status = "active",
                subtotals = GoogleAdsTotalsDTO(
                    sales = pageNumber * 100.0,
                    spend = pageNumber * 100.0,
                    impressions = pageNumber * 100.0,
                    clicks = pageNumber * 100.0,
                    conversions = pageNumber * 100.0
                )
            ),
            GoogleAdsCampaignDTO(
                id = (pageNumber + 1).toLong(),
                name = "campaign${pageNumber + 1}",
                status = "active",
                subtotals = GoogleAdsTotalsDTO(
                    sales = pageNumber * 200.0,
                    spend = pageNumber * 200.0,
                    impressions = pageNumber * 200.0,
                    clicks = pageNumber * 200.0,
                    conversions = pageNumber * 200.0
                )
            )
        ),
        intervals = emptyList(),
        totals = GoogleAdsTotalsDTO(
            sales = pageNumber * 100.0,
            spend = pageNumber * 100.0,
            impressions = pageNumber * 100.0,
            clicks = pageNumber * 100.0,
            conversions = pageNumber * 100.0
        ),
        nextPageToken = "nextPageToken"
    )
}