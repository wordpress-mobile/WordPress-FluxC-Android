package org.wordpress.android.fluxc.example.ui.leaderboards

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_leaderboards.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.StoreSelectorDialog
import org.wordpress.android.fluxc.example.utils.toggleSiteDependentButtons
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.store.WCLeaderboardsStore
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity.YEARS
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooLeaderboardsFragment : Fragment(), StoreSelectorDialog.Listener {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore
    @Inject internal lateinit var wcLeaderboardsStore: WCLeaderboardsStore

    private var selectedPos: Int = -1
    private var selectedSite: SiteModel? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_woo_leaderboards, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        leaderboards_select_site.setOnClickListener(::onLeaderboardsSelectSiteButtonClicked)
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

    private fun onLeaderboardsSelectSiteButtonClicked(view: View) {
        fragmentManager?.let { fm ->
            StoreSelectorDialog.newInstance(this, selectedPos)
                    .show(fm, "StoreSelectorDialog")
        }
    }

    private fun launchProductLeaderboardsRequest(unit: StatsGranularity) {
        coroutineScope.launch {
            try {
                takeAsyncRequestWithValidSite { wcLeaderboardsStore.fetchProductLeaderboards(it, unit) }
                        ?.model
                        ?.let { logLeaderboardResponse(it, unit) }
                        ?: prependToLog("Couldn't fetch Products Leaderboards.")
            } catch (ex: Exception) {
                prependToLog("Couldn't fetch Products Leaderboards. Error: ${ex.message}")
            }
        }
    }

    private fun launchProductLeaderboardsCacheRetrieval(unit: StatsGranularity) {
        coroutineScope.launch {
            try {
                takeAsyncRequestWithValidSite { wcLeaderboardsStore.fetchCachedProductLeaderboards(it, unit) }
                        ?.model
                        ?.let { logLeaderboardResponse(it, unit) }
                        ?: prependToLog("Couldn't fetch Products Leaderboards.")
            } catch (ex: Exception) {
                prependToLog("Couldn't fetch Products Leaderboards. Error: ${ex.message}")
            }
        }
    }

    override fun onSiteSelected(site: SiteModel, pos: Int) {
        selectedSite = site
        selectedPos = pos
        buttonContainer.toggleSiteDependentButtons()
        leaderboards_selected_site.text = site.name ?: site.displayName
    }

    private fun logLeaderboardResponse(model: List<WCTopPerformerProductModel>, unit: StatsGranularity) {
        model.forEach {
            prependToLog("  Top Performer Product id: ${it.product.remoteProductId ?: "Product id not available"}")
            prependToLog("  Top Performer Product name: ${it.product.name ?: "Product name not available"}")
            prependToLog("  Top Performer currency: ${it.currency ?: "Currency not available"}")
            prependToLog("  Top Performer quantity: ${it.quantity ?: "Quantity not available"}")
            prependToLog("  Top Performer total: ${it.total ?: "total not available"}")
            prependToLog("  Top Performer id: ${it.id ?: "ID not available"}")
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
}
