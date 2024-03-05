package org.wordpress.android.fluxc.example.ui.leaderboards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_woo_leaderboards.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.R
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_woo_leaderboards, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFetchButtons()
        bindCacheRetrievalButtons()
    }

    private fun setupFetchButtons() {
        fetch_product_leaderboards_of_day.setOnClickListener {
            launchProductLeaderboardsRequest(DAYS)
        }
        fetch_product_leaderboards_of_week.setOnClickListener {
            launchProductLeaderboardsRequest(WEEKS)
        }
        fetch_product_leaderboards_of_month.setOnClickListener {
            launchProductLeaderboardsRequest(MONTHS)
        }
        fetch_product_leaderboards_of_year.setOnClickListener {
            launchProductLeaderboardsRequest(YEARS)
        }
    }

    private fun bindCacheRetrievalButtons() {
        retrieve_cached_leaderboards_of_day.setOnClickListener {
            launchProductLeaderboardsCacheRetrieval(DAYS)
        }
        retrieve_cached_leaderboards_of_week.setOnClickListener {
            launchProductLeaderboardsCacheRetrieval(WEEKS)
        }
        retrieve_cached_leaderboards_of_month.setOnClickListener {
            launchProductLeaderboardsCacheRetrieval(MONTHS)
        }
        retrieve_cached_leaderboards_of_year.setOnClickListener {
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
    }

    private fun StatsGranularity.endDateTime(site: SiteModel) = when (this) {
        DAYS -> DateUtils.getEndDateForSite(site)
        WEEKS -> DateUtils.getLastDayOfCurrentWeekForSite(site)
        MONTHS -> DateUtils.getLastDayOfCurrentMonthForSite(site)
        YEARS -> DateUtils.getLastDayOfCurrentYearForSite(site)
    }

    private fun StatsGranularity.datePeriod(site: SiteModel): String {
        val startDate = startDateTime(site)
        val endDate = endDateTime(site)
        return DateUtils.getDatePeriod(startDate, endDate)
    }
}
