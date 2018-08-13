package org.wordpress.android.fluxc.portfolio

import com.nhaarman.mockito_kotlin.KArgumentCaptor
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.PostAction.FETCH_PORTFOLIOS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.portfolio.PortfolioModel
import org.wordpress.android.fluxc.model.portfolio.PortfolioStatus
import org.wordpress.android.fluxc.model.portfolio.PortfolioStatus.DRAFT
import org.wordpress.android.fluxc.model.portfolio.PortfolioStatus.PUBLISHED
import org.wordpress.android.fluxc.model.portfolio.PortfolioStatus.SCHEDULED
import org.wordpress.android.fluxc.model.portfolio.PortfolioStatus.TRASHED
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.PortfolioStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchPostsPayload
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged

@RunWith(MockitoJUnitRunner::class)
class PortfolioStoreTest {
    @Mock
    lateinit var postStore: PostStore
    @Mock
    lateinit var dispatcher: Dispatcher
    @Mock
    lateinit var site: SiteModel
    private lateinit var actionCaptor: KArgumentCaptor<Action<Any>>

    private val query = "que"
    private val portfolioWithoutQuery = initPortfolio(1, 10, "portfolio 1")
    private val portfolioWithQuery = initPortfolio(2, title = "portfolio2 start $query end ")
    private val portfolioWithoutTitle = initPortfolio(3, 10)
    private val differentPortfolioTypes = listOf(
            initPortfolio(1, 0, "portfolio 1", "publish"),
            initPortfolio(2, 0, "portfolio 2", "draft"),
            initPortfolio(3, 0, "portfolio 3", "future"),
            initPortfolio(4, 0, "portfolio 4", "trash"),
            initPortfolio(5, 0, "portfolio 5", "private"),
            initPortfolio(6, 0, "portfolio 6", "pending"),
            initPortfolio(7, 0, "portfolio 7", "draft")
    )

    private lateinit var store: PortfolioStore

    @Before
    fun setUp() {
        actionCaptor = argumentCaptor()
        val portfolios = listOf(portfolioWithoutQuery, portfolioWithQuery, portfolioWithoutTitle)
        whenever(postStore.getPortfoliosForSite(site)).thenReturn(portfolios)
        store = PortfolioStore(postStore, dispatcher)
    }

    @Test
    fun searchFindsAllResultsContainingText() {
        val result = runBlocking { store.search(site, query) }

        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo(portfolioWithQuery.title)
    }

    @Test
    fun searchFilitersOutUnknownStatus() {
        whenever(postStore.getPortfoliosForSite(site)).thenReturn(listOf(initPortfolio(1, title = query, status = "foo")))

        val result = runBlocking { store.search(site, query) }

        assertThat(result).isEmpty()
    }

    @Test
    fun searchOrdersResultsByStatus() {
        val trashStatus = "trash"
        val draftStatus = "draft"
        val publishStatus = "publish"
        val futureStatus = "future"
        val title = "title"
        val trashedSite1 = initPortfolio(1, title = title, status = trashStatus)
        val draftSite1 = initPortfolio(2, title = title, status = draftStatus)
        val publishedSite1 = initPortfolio(3, title = title, status = publishStatus)
        val scheduledSite1 = initPortfolio(4, title = title, status = futureStatus)
        val scheduledSite2 = initPortfolio(5, title = title, status = futureStatus)
        val publishedSite2 = initPortfolio(6, title = title, status = publishStatus)
        val draftSite2 = initPortfolio(7, title = title, status = draftStatus)
        val trashedSite2 = initPortfolio(8, title = title, status = trashStatus)
        val portfolios = listOf(
                trashedSite1,
                draftSite1,
                publishedSite1,
                scheduledSite1,
                scheduledSite2,
                publishedSite2,
                draftSite2,
                trashedSite2
        )
        whenever(postStore.getPortfoliosForSite(site)).thenReturn(portfolios)

        val result = runBlocking { store.groupedSearch(site, title) }

        assertThat(result.keys).contains(PUBLISHED, DRAFT, SCHEDULED, TRASHED)
        assertPortfolio(result, 0, 3, PortfolioStatus.PUBLISHED)
        assertPortfolio(result, 1, 6, PortfolioStatus.PUBLISHED)
        assertPortfolio(result, 0, 2, PortfolioStatus.DRAFT)
        assertPortfolio(result, 1, 7, PortfolioStatus.DRAFT)
        assertPortfolio(result, 0, 4, PortfolioStatus.SCHEDULED)
        assertPortfolio(result, 1, 5, PortfolioStatus.SCHEDULED)
        assertPortfolio(result, 0, 1, PortfolioStatus.TRASHED)
        assertPortfolio(result, 1, 8, PortfolioStatus.TRASHED)
    }

    private fun assertPortfolio(map: Map<PortfolioStatus, List<PortfolioModel>>, position: Int, id: Int, status: PortfolioStatus) {
        val portfolio = map[status]?.get(position)
        assertThat(portfolio).isNotNull()
        assertThat(portfolio!!.id).isEqualTo(id)
        assertThat(portfolio.status).isEqualTo(status)
    }

    @Test
    fun emptySearchResultWhenNothingContainsQuery() {
        val result = runBlocking { store.search(site, "foo") }

        assertThat(result).isEmpty()
    }

    @Test
    fun loadsPortfoliosFromDb() {
        val portfolios = runBlocking { store.loadPortfoliosFromDb(site) }

        assertThat(portfolios).hasSize(3)
        assertThat(portfolios[0].id).isEqualTo(portfolioWithoutQuery.id)
        assertThat(portfolios[1].id).isEqualTo(portfolioWithQuery.id)
        assertThat(portfolios[2].id).isEqualTo(portfolioWithoutTitle.id)
    }

    @Test
    fun requestPortfoliosFetchesFromServerAndReturnsEvent() = runBlocking {
        val expected = OnPostChanged(5, false)
        expected.causeOfChange = FETCH_PORTFOLIOS
        var event: OnPostChanged? = null
        val job = launch {
            event = store.requestPortfoliosFromServer(site)
        }
        delay(10)
        store.onPostChanged(expected)
        delay(10)
        job.join()

        assertThat(expected).isEqualTo(event)
        verify(dispatcher).dispatch(any())
    }

    @Test
    fun requestPortfoliosFetchesPaginatedFromServerAndReturnsSecondEvent() = runBlocking<Unit> {
        val firstEvent = OnPostChanged(5, true)
        val lastEvent = OnPostChanged(5, false)
        firstEvent.causeOfChange = FETCH_PORTFOLIOS
        lastEvent.causeOfChange = FETCH_PORTFOLIOS
        var event: OnPostChanged? = null
        val job = launch {
            event = store.requestPortfoliosFromServer(site)
        }
        delay(10)
        store.onPostChanged(firstEvent)
        delay(10)
        store.onPostChanged(lastEvent)
        delay(10)
        job.join()

        assertThat(lastEvent).isEqualTo(event)
        verify(dispatcher, times(2)).dispatch(actionCaptor.capture())
        val firstPayload = actionCaptor.firstValue.payload as FetchPostsPayload
        assertThat(firstPayload.site).isEqualTo(site)
        assertThat(firstPayload.loadMore).isEqualTo(false)
        val lastPayload = actionCaptor.lastValue.payload as FetchPostsPayload
        assertThat(lastPayload.site).isEqualTo(site)
        assertThat(lastPayload.loadMore).isEqualTo(true)
    }

    @Test
    fun requestPortfoliosAndVerifyAllPortfolioTypesPresent() = runBlocking<Unit> {
        val event = OnPostChanged(4, false)
        event.causeOfChange = FETCH_PORTFOLIOS
        launch {
            store.requestPortfoliosFromServer(site)
        }
        delay(10)
        store.onPostChanged(event)
        delay(10)

        verify(dispatcher, times(1)).dispatch(actionCaptor.capture())
        val payload = actionCaptor.firstValue.payload as FetchPostsPayload
        assertThat(payload.site).isEqualTo(site)

        val portfolioTypes = payload.statusTypes
        assertThat(portfolioTypes.size).isEqualTo(4)
        assertThat(portfolioTypes.filter { it == PostStatus.PUBLISHED }.size).isEqualTo(1)
        assertThat(portfolioTypes.filter { it == PostStatus.DRAFT }.size).isEqualTo(1)
        assertThat(portfolioTypes.filter { it == PostStatus.TRASHED }.size).isEqualTo(1)
        assertThat(portfolioTypes.filter { it == PostStatus.SCHEDULED }.size).isEqualTo(1)

        whenever(postStore.getPortfoliosForSite(site))
                .thenReturn(differentPortfolioTypes.filter { payload.statusTypes.contains(PostStatus.fromPost(it)) })

        val portfolios = store.loadPortfoliosFromDb(site)

        assertThat(portfolios.size).isEqualTo(5)
        assertThat(portfolios.filter { it.status == PUBLISHED }.size).isEqualTo(1)
        assertThat(portfolios.filter { it.status == DRAFT }.size).isEqualTo(2)
        assertThat(portfolios.filter { it.status == TRASHED }.size).isEqualTo(1)
        assertThat(portfolios.filter { it.status == SCHEDULED }.size).isEqualTo(1)
    }

    private fun initPortfolio(
        id: Int,
        parentId: Long? = null,
        title: String? = null,
        status: String? = "draft"
    ): PostModel {
        val portfolio = PostModel()
        portfolio.id = id
        parentId?.let {
            portfolio.parentId = parentId
        }
        title?.let {
            portfolio.title = it
        }
        status?.let {
            portfolio.status = status
        }
        return portfolio
    }
}
