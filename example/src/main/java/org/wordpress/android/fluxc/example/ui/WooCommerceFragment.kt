package org.wordpress.android.fluxc.example.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woocommerce.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.LogUtils
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.replaceFragment
import org.wordpress.android.fluxc.example.ui.customer.WooCustomersFragment
import org.wordpress.android.fluxc.example.ui.gateways.WooGatewaysFragment
import org.wordpress.android.fluxc.example.ui.helpsupport.WooHelpSupportFragment
import org.wordpress.android.fluxc.example.ui.leaderboards.WooLeaderboardsFragment
import org.wordpress.android.fluxc.example.ui.orders.WooOrdersFragment
import org.wordpress.android.fluxc.example.ui.products.WooProductAttributeFragment
import org.wordpress.android.fluxc.example.ui.products.WooProductsFragment
import org.wordpress.android.fluxc.example.ui.refunds.WooRefundsFragment
import org.wordpress.android.fluxc.example.ui.shippinglabels.WooShippingLabelFragment
import org.wordpress.android.fluxc.example.ui.stats.WooRevenueStatsFragment
import org.wordpress.android.fluxc.example.ui.stats.WooStatsFragment
import org.wordpress.android.fluxc.example.ui.taxes.WooTaxFragment
import org.wordpress.android.fluxc.generated.WCCoreActionBuilder
import org.wordpress.android.fluxc.store.WCDataStore
import org.wordpress.android.fluxc.store.WCUserStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WooCommerceStore.OnApiVersionFetched
import org.wordpress.android.fluxc.store.WooCommerceStore.OnWCProductSettingsChanged
import org.wordpress.android.fluxc.store.WooCommerceStore.OnWCSiteSettingsChanged
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

class WooCommerceFragment : Fragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject lateinit var wooCommerceStore: WooCommerceStore
    @Inject lateinit var wooDataStore: WCDataStore
    @Inject lateinit var wooUserStore: WCUserStore

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_woocommerce, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        log_sites.setOnClickListener {
            for (site in wooCommerceStore.getWooCommerceSites()) {
                prependToLog(site.name + ": " + if (site.isWpComStore) "WP.com store" else "Self-hosted store")
                AppLog.i(T.API, LogUtils.toString(site))
            }
        }

        log_woo_api_versions.setOnClickListener {
            for (site in wooCommerceStore.getWooCommerceSites()) {
                dispatcher.dispatch(WCCoreActionBuilder.newFetchSiteApiVersionAction(site))
            }
        }

        fetch_settings.setOnClickListener {
            getFirstWCSite()?.let {
                dispatcher.dispatch(WCCoreActionBuilder.newFetchSiteSettingsAction(it))
            } ?: showNoWCSitesToast()
        }

        fetch_product_settings.setOnClickListener {
            getFirstWCSite()?.let {
                dispatcher.dispatch(WCCoreActionBuilder.newFetchProductSettingsAction(it))
            } ?: showNoWCSitesToast()
        }

        get_user_role.setOnClickListener {
            getFirstWCSite()?.let { site ->
                coroutineScope.launch {
                    val result = withContext(Dispatchers.Default) {
                        wooUserStore.fetchUserRole(site)
                    }
                    result.error?.let {
                        prependToLog("${it.type}: ${it.message}")
                    }
                    result.model?.let {
                        prependToLog("Current user is: ${it.roles}")
                    }
                }
            }
        }

        orders.setOnClickListener {
            getFirstWCSite()?.let {
                replaceFragment(WooOrdersFragment())
            } ?: showNoWCSitesToast()
        }

        products.setOnClickListener {
            getFirstWCSite()?.let {
                replaceFragment(WooProductsFragment())
            } ?: showNoWCSitesToast()
        }

        stats.setOnClickListener {
            getFirstWCSite()?.let {
                replaceFragment(WooStatsFragment())
            } ?: showNoWCSitesToast()
        }

        stats_revenue.setOnClickListener {
            getFirstWCSite()?.let {
                replaceFragment(WooRevenueStatsFragment())
            } ?: showNoWCSitesToast()
        }

        refunds.setOnClickListener {
            getFirstWCSite()?.let {
                replaceFragment(WooRefundsFragment())
            } ?: showNoWCSitesToast()
        }

        gateways.setOnClickListener {
            getFirstWCSite()?.let {
                replaceFragment(WooGatewaysFragment())
            } ?: showNoWCSitesToast()
        }

        taxes.setOnClickListener {
            getFirstWCSite()?.let {
                replaceFragment(WooTaxFragment())
            } ?: showNoWCSitesToast()
        }

        shipping_labels.setOnClickListener {
            getFirstWCSite()?.let {
                replaceFragment(WooShippingLabelFragment())
            } ?: showNoWCSitesToast()
        }

        leaderboards.setOnClickListener {
            getFirstWCSite()?.let {
                replaceFragment(WooLeaderboardsFragment())
            } ?: showNoWCSitesToast()
        }

        countries.setOnClickListener {
            launchCountriesRequest()
        }

        attributes.setOnClickListener {
            getFirstWCSite()?.let {
                replaceFragment(WooProductAttributeFragment())
            } ?: showNoWCSitesToast()
        }

        customers.setOnClickListener {
            getFirstWCSite()?.let {
                replaceFragment(WooCustomersFragment())
            } ?: showNoWCSitesToast()
        }

        help_support.setOnClickListener {
            getFirstWCSite()?.let {
                replaceFragment(WooHelpSupportFragment())
            } ?: showNoWCSitesToast()
        }
    }

    private fun launchCountriesRequest() {
        coroutineScope.launch {
            try {
                getFirstWCSite()?.let {
                    wooDataStore.fetchCountriesAndStates(it).model?.let { country ->
                        country.forEach { location ->
                            prependToLog(location.name)
                        }
                    }
                    ?: prependToLog("Couldn't fetch countries.")
                } ?: showNoWCSitesToast()
            } catch (ex: Exception) {
                prependToLog("Couldn't fetch countries. Error: ${ex.message}")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        super.onStop()
        dispatcher.unregister(this)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onApiVersionFetched(event: OnApiVersionFetched) {
        if (event.isError) {
            prependToLog("Error in onApiVersionFetched: ${event.error.type} - ${event.error.message}")
            return
        }

        with(event) {
            val formattedVersion = apiVersion.substringAfterLast("/")
            prependToLog("Max Woo version for ${site.name}: $formattedVersion")
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWCSiteSettingsChanged(event: OnWCSiteSettingsChanged) {
        if (event.isError) {
            prependToLog("Error in onWCSiteSettingsChanged: ${event.error.type} - ${event.error.message}")
            return
        }

        with(event) {
            prependToLog("Updated site settings for ${site.name}:\n" +
                    wooCommerceStore.getSiteSettings(site).toString()
            )
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWCProductSettingsChanged(event: OnWCProductSettingsChanged) {
        if (event.isError) {
            prependToLog("Error in onWCProductSettingsChanged: ${event.error.type} - ${event.error.message}")
            return
        }

        wooCommerceStore.getProductSettings(event.site)?.let { settings ->
            prependToLog(
                    "Updated product settings for ${event.site.name}: " +
                            "weight unit = ${settings.weightUnit}, dimension unit = ${settings.dimensionUnit}"
            )
        } ?: prependToLog("Error getting product settings from db")
    }

    private fun showNoWCSitesToast() {
        ToastUtils.showToast(activity, "No WooCommerce sites found for this account!")
    }

    private fun getFirstWCSite() = wooCommerceStore.getWooCommerceSites().getOrNull(0)
}
