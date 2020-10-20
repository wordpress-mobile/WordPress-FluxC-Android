package org.wordpress.android.fluxc.store.stats.time

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode.Top
import org.wordpress.android.fluxc.model.stats.time.ReferrersModel
import org.wordpress.android.fluxc.model.stats.time.TimeStatsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.persistence.ReferrersSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnReportReferrerAsSpam
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.STATS
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReferrersStore
@Inject constructor(
    private val restClient: ReferrersRestClient,
    private val sqlUtils: ReferrersSqlUtils,
    private val timeStatsMapper: TimeStatsMapper,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchReferrers(
        site: SiteModel,
        granularity: StatsGranularity,
        limitMode: Top,
        date: Date,
        forced: Boolean = false
    ) = coroutineEngine.withDefaultContext(STATS, this, "fetchReferrers") {
        if (!forced && sqlUtils.hasFreshRequest(site, granularity, date, limitMode.limit)) {
            return@withDefaultContext OnStatsFetched(getReferrers(site, granularity, limitMode, date), cached = true)
        }
        val payload = restClient.fetchReferrers(site, granularity, date, limitMode.limit + 1, forced)
        return@withDefaultContext when {
            payload.isError -> OnStatsFetched(payload.error)
            payload.response != null -> {
                val model = timeStatsMapper.map(payload.response, limitMode)
                sqlUtils.insert(site, granularity, limitMode.limit, date, model)
                OnStatsFetched(model)
            }
            else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
        }
    }

    fun getReferrers(site: SiteModel, granularity: StatsGranularity, limitMode: Top, date: Date) =
            coroutineEngine.run(STATS, this, "getReferrers") {
                sqlUtils.select(site, granularity, limitMode.limit, date)
            }

    suspend fun reportReferrerAsSpam(
        site: SiteModel,
        domain: String,
        granularity: StatsGranularity,
        limitMode: Top,
        date: Date
    ) = coroutineEngine.withDefaultContext(STATS, this, "reportReferrerAsSpam") {
        val payload = restClient.reportReferrerAsSpam(site, domain)

        if (payload.response != null || payload.error.type == StatsErrorType.ALREADY_SPAMMED) {
            updateCacheWithMarkedSpam(site, granularity, limitMode, date, domain, true)
        }
        return@withDefaultContext when {
            payload.isError -> OnReportReferrerAsSpam(payload.error)
            payload.response != null -> OnReportReferrerAsSpam(payload.response)
            else -> OnReportReferrerAsSpam(StatsError(INVALID_RESPONSE))
        }
    }

    suspend fun unreportReferrerAsSpam(
        site: SiteModel,
        domain: String,
        granularity: StatsGranularity,
        limitMode: Top,
        date: Date
    ) = coroutineEngine.withDefaultContext(STATS, this, "unreportReferrerAsSpam") {
        val payload = restClient.unreportReferrerAsSpam(site, domain)

        if (payload.response != null || payload.error.type == StatsErrorType.ALREADY_SPAMMED) {
            updateCacheWithMarkedSpam(site, granularity, limitMode, date, domain, false)
        }
        return@withDefaultContext when {
            payload.isError -> OnReportReferrerAsSpam(payload.error)
            payload.response != null -> OnReportReferrerAsSpam(payload.response)
            else -> OnReportReferrerAsSpam(StatsError(INVALID_RESPONSE))
        }
    }

    private fun updateCacheWithMarkedSpam(
        site: SiteModel,
        granularity: StatsGranularity,
        limitMode: Top,
        date: Date,
        domain: String,
        spam: Boolean
    ) {
        val currentModel = sqlUtils.select(site, granularity, limitMode.limit, date)
        if (currentModel != null) {
            val markedModel = setSelectForSpam(currentModel, domain, spam)
            sqlUtils.insert(site, granularity, limitMode.limit, date, markedModel)
        }
    }

    fun setSelectForSpam(model: ReferrersModel, domain: String, spam: Boolean): ReferrersModel {
        val updatedModel = model.copy(groups = model.groups.map { group ->
            var hasChange = false
            val isMarkedAsSpam = if (group.url == domain || group.name == domain) {
                hasChange = true
                spam
            } else {
                group.markedAsSpam
            }
            val updatedReferrers = group.referrers.map { referrer ->
                if (referrer.url == domain) {
                    hasChange = true
                    referrer.copy(markedAsSpam = spam)
                } else {
                    referrer
                }
            }
            if (hasChange) {
                group.copy(markedAsSpam = isMarkedAsSpam, referrers = updatedReferrers)
            } else {
                group
            }
        })
        return if (updatedModel != model) {
            updatedModel
        } else {
            model
        }
    }
}
