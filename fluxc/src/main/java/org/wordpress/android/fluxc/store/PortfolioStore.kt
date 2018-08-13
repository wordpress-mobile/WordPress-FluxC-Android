package org.wordpress.android.fluxc.store

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.PostAction
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.portfolio.PortfolioModel
import org.wordpress.android.fluxc.model.portfolio.PortfolioStatus
import org.wordpress.android.fluxc.model.portfolio.PortfolioStatus.DRAFT
import org.wordpress.android.fluxc.model.portfolio.PortfolioStatus.PUBLISHED
import org.wordpress.android.fluxc.model.portfolio.PortfolioStatus.SCHEDULED
import org.wordpress.android.fluxc.model.portfolio.PortfolioStatus.UNKNOWN
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.PostStore.FetchPostsPayload
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import java.util.SortedMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

@Singleton
class PortfolioStore @Inject constructor(private val postStore: PostStore, private val dispatcher: Dispatcher) {
    private var postLoadContinuation: Continuation<OnPostChanged>? = null
    private var site: SiteModel? = null

    init {
        dispatcher.register(this)
    }

    suspend fun search(site: SiteModel, searchQuery: String): List<PortfolioModel> = withContext(CommonPool) {
        postStore.getPortfoliosForSite(site)
                .filterNotNull()
                .map { PortfolioModel(it) }
                .filter { it.status != UNKNOWN && it.title.toLowerCase().contains(searchQuery.toLowerCase()) }
    }

    suspend fun groupedSearch(
        site: SiteModel,
        searchQuery: String
    ): SortedMap<PortfolioStatus, List<PortfolioModel>> = withContext(CommonPool) {
        val list = search(site, searchQuery)
                .groupBy { it.status }
        list
                .toSortedMap(Comparator { previous, next ->
                    when {
                        previous == next -> 0
                        previous == PUBLISHED -> -1
                        next == PUBLISHED -> 1
                        previous == DRAFT -> -1
                        next == DRAFT -> 1
                        previous == SCHEDULED -> -1
                        next == SCHEDULED -> 1
                        else -> {
                            throw IllegalArgumentException("Unexpected Portfolio type")
                        }
                    }
                })
    }

    suspend fun loadPortfoliosFromDb(site: SiteModel): List<PortfolioModel> = withContext(CommonPool) {
        val portfolios = postStore.getPortfoliosForSite(site).filter { it != null }
        portfolios.map { PortfolioModel(it) }
    }

    suspend fun requestPortfoliosFromServer(site: SiteModel): OnPostChanged = suspendCoroutine { cont ->
        this.site = site
        postLoadContinuation = cont
        fetchPortfolios(site, false)
    }

    private fun fetchPortfolios(site: SiteModel, loadMore: Boolean) {
        val payload = FetchPostsPayload(site, loadMore,
                listOf(PostStatus.DRAFT, PostStatus.PUBLISHED, PostStatus.SCHEDULED, PostStatus.TRASHED))
        dispatcher.dispatch(PostActionBuilder.newFetchPortfoliosAction(payload))
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostChanged(event: OnPostChanged) {
        if (event.causeOfChange == PostAction.FETCH_PORTFOLIOS) {
            if (event.canLoadMore && site != null) {
                fetchPortfolios(site!!, true)
            } else {
                postLoadContinuation?.resume(event)
                postLoadContinuation = null
            }
        }
    }
}
