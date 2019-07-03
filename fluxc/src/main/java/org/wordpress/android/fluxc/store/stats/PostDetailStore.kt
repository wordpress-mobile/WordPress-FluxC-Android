package org.wordpress.android.fluxc.store.stats

import androidx.lifecycle.LiveData
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.PostDetailStatsMapper
import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.LatestPostInsightsRestClient
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.DetailedPostStatsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.utils.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class PostDetailStore
@Inject constructor(
    private val restClient: LatestPostInsightsRestClient,
    private val sqlUtils: DetailedPostStatsSqlUtils,
    private val coroutineContext: CoroutineContext,
    private val mapper: PostDetailStatsMapper
) {
    suspend fun fetchPostDetail(
        site: SiteModel,
        postId: Long,
        forced: Boolean = false
    ) = withContext(coroutineContext) {
        if (!forced && sqlUtils.hasFreshRequest(site, postId = postId)) {
            return@withContext OnStatsFetched(getPostDetail(site, postId), cached = true)
        }
        val payload = restClient.fetchPostStats(site, postId, forced)
        return@withContext when {
            payload.isError -> OnStatsFetched(payload.error)
            payload.response != null -> {
                sqlUtils.insert(site, payload.response, postId = postId)
                OnStatsFetched(mapper.map(payload.response))
            }
            else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
        }
    }

    fun getPostDetail(site: SiteModel, postId: Long): PostDetailStatsModel? {
        return sqlUtils.select(site, postId)?.let { mapper.map(it) }
    }

    fun livePostDetail(site: SiteModel, postId: Long): LiveData<PostDetailStatsModel> {
        return sqlUtils.liveSelect(site, postId).map { mapper.map(it) }
    }
}
