package org.wordpress.android.fluxc.example.ui.leaderboards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.databinding.FragmentWooLeaderboardsBinding
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.StoreSelectingFragment
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.entity.TopPerformerProductEntity
import org.wordpress.android.fluxc.store.WCLeaderboardsStore
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity.YEARS
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.utils.DateUtils
import javax.inject.Inject

class WooLeaderboardsFragment : StoreSelectingFragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore
    @Inject internal lateinit var wcLeaderboardsStore: WCLeaderboardsStore

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentWooLeaderboardsBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentWooLeaderboardsBinding.bind(view)) {
            setupFetchButtons()
            bindCacheRetrievalButtons()
        }
    }

    private fun FragmentWooLeaderboardsBinding.setupFetchButtons() {
        fetchProductLeaderboardsOfDay.setOnClickListener {
            launchProductLeaderboardsRequest(DAYS)
        }
        fetchProductLeaderboardsOfWeek.setOnClickListener {
            launchProductLeaderboardsRequest(WEEKS)
        }
        fetchProductLeaderboardsOfMonth.setOnClickListener {
            launchProductLeaderboardsRequest(MONTHS)
        }
        fetchProductLeaderboardsOfYear.setOnClickListener {
            launchProductLeaderboardsRequest(YEARS)
        }
    }

    private fun FragmentWooLeaderboardsBinding.bindCacheRetrievalButtons() {
        retrieveCachedLeaderboardsOfDay.setOnClickListener {
            launchProductLeaderboardsCacheRetrieval(DAYS)
        }
        retrieveCachedLeaderboardsOfWeek.setOnClickListener {
            launchProductLeaderboardsCacheRetrieval(WEEKS)
        }
        retrieveCachedLeaderboardsOfMonth.setOnClickListener {
            launchProductLeaderboardsCacheRetrieval(MONTHS)
        }
        retrieveCachedLeaderboardsOfYear.setOnClickListener {
            launchProductLeaderboardsCacheRetrieval(YEARS)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun launchProductLeaderboardsRequest(unit: StatsGranularity) {
        coroutineScope.launch {
            try {
                takeAsyncRequestWithValidSite {
                    wcLeaderboardsStore.fetchTopPerformerProducts(
                        site = it,
                        startDate = unit.startDateTime(it),
                        endDate = unit.endDateTime(it)
                    )
                }
                    ?.model
                    ?.let { logLeaderboardResponse(it, unit) }
                    ?: prependToLog("Couldn't fetch Products Leaderboards.")
            } catch (ex: Exception) {
                prependToLog("Couldn't fetch Products Leaderboards. Error: ${ex.message}")
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun launchProductLeaderboardsCacheRetrieval(unit: StatsGranularity) {
        coroutineScope.launch {
            try {
                takeAsyncRequestWithValidSite {
                    wcLeaderboardsStore.fetchTopPerformerProducts(
                        site = it,
                        startDate = unit.startDateTime(it),
                        endDate = unit.endDateTime(it)
                    )
                }
                    ?.model
                    ?.let { logLeaderboardResponse(it, unit) }
                    ?: prependToLog("Couldn't fetch Products Leaderboards.")
            } catch (ex: Exception) {
                prependToLog("Couldn't fetch Products Leaderboards. Error: ${ex.message}")
            }
        }
    }

    private fun logLeaderboardResponse(model: List<TopPerformerProductEntity>, unit: StatsGranularity) {
        model.forEach {
            prependToLog("  Top Performer Product id: ${it.productId ?: "Product id not available"}")
            prependToLog("  Top Performer Product name: ${it.name ?: "Product name not available"}")
            prependToLog("  Top Performer currency: ${it.currency ?: "Currency not available"}")
            prependToLog("  Top Performer quantity: ${it.quantity ?: "Quantity not available"}")
            prependToLog("  Top Performer total: ${it.total ?: "total not available"}")
            prependToLog("  --------- Product ---------")
        }
        prependToLog("========== Top Performers of the $unit =========")
    }

    private suspend inline fun <T> takeAsyncRequestWithValidSite(crossinline action: suspend (SiteModel) -> T) =
        selectedSite?.let {
            withContext(Dispatchers.Default) {
                action(it)
            }
        }

    private fun StatsGranularity.startDateTime(site: SiteModel) = when (this) {
        DAYS -> DateUtils.getStartDateForSite(site, DateUtils.getStartOfCurrentDay())
        WEEKS -> DateUtils.getFirstDayOfCurrentWeekBySite(site)
        MONTHS -> DateUtils.getFirstDayOfCurrentMonthBySite(site)
        YEARS -> DateUtils.getFirstDayOfCurrentYearBySite(site)
        else -> error("The sample code does not support the $this granularity.")
    }

    private fun StatsGranularity.endDateTime(site: SiteModel) = when (this) {
        DAYS -> DateUtils.getEndDateForSite(site)
        WEEKS -> DateUtils.getLastDayOfCurrentWeekForSite(site)
        MONTHS -> DateUtils.getLastDayOfCurrentMonthForSite(site)
        YEARS -> DateUtils.getLastDayOfCurrentYearForSite(site)
        else -> error("The sample code does not support the $this granularity.")
    }
}
