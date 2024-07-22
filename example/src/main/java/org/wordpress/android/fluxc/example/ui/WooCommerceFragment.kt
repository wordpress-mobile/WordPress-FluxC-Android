package org.wordpress.android.fluxc.example.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.example.LogUtils
import org.wordpress.android.fluxc.example.databinding.FragmentWoocommerceBinding
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.replaceFragment
import org.wordpress.android.fluxc.example.ui.coupons.WooCouponsFragment
import org.wordpress.android.fluxc.example.ui.customer.WooCustomersFragment
import org.wordpress.android.fluxc.example.ui.gateways.WooGatewaysFragment
import org.wordpress.android.fluxc.example.ui.helpsupport.WooHelpSupportFragment
import org.wordpress.android.fluxc.example.ui.leaderboards.WooLeaderboardsFragment
import org.wordpress.android.fluxc.example.ui.onboarding.WooOnboardingFragment
import org.wordpress.android.fluxc.example.ui.orders.WooOrdersFragment
import org.wordpress.android.fluxc.example.ui.products.WooProductAttributeFragment
import org.wordpress.android.fluxc.example.ui.products.WooProductsFragment
import org.wordpress.android.fluxc.example.ui.refunds.WooRefundsFragment
import org.wordpress.android.fluxc.example.ui.shippinglabels.WooShippingLabelFragment
import org.wordpress.android.fluxc.example.ui.stats.WooRevenueStatsFragment
import org.wordpress.android.fluxc.example.ui.storecreation.WooStoreCreationFragment
import org.wordpress.android.fluxc.example.ui.taxes.WooTaxFragment
import org.wordpress.android.fluxc.example.ui.wooadmin.WooAdminFragment
import org.wordpress.android.fluxc.store.WCDataStore
import org.wordpress.android.fluxc.store.WCGoogleStore
import org.wordpress.android.fluxc.store.WCUserStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

class WooCommerceFragment : StoreSelectingFragment() {
    @Inject lateinit var wooCommerceStore: WooCommerceStore
    @Inject lateinit var wooDataStore: WCDataStore
    @Inject lateinit var wooUserStore: WCUserStore
    @Inject lateinit var wooGoogleStore: WCGoogleStore

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentWoocommerceBinding.inflate(inflater, container, false).root

    @Suppress("LongMethod", "ComplexMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            // Fetch sites to make sure we have the correct status of WooCommerce installation
            wooCommerceStore.fetchWooCommerceSites()
        }

        with(FragmentWoocommerceBinding.bind(view)) {
            logSites.setOnClickListener {
                coroutineScope.launch {
                    prependToLog("Fetching WooCommerce sites")
                    val result = wooCommerceStore.fetchWooCommerceSites()
                    if (result.isError) {
                        prependToLog(
                            "Fetching WooCommerce sites failed, error message: " +
                                "${result.error.message}"
                        )
                    } else {
                        for (site in result.model!!) {
                            prependToLog(
                                site.name + ": " + if (site.isWpComStore) {
                                    "WP.com store"
                                } else {
                                    "Self-hosted store"
                                }
                            )
                            AppLog.i(T.API, LogUtils.toString(site))
                        }
                    }
                }
            }

            logWooApiVersions.setOnClickListener {
                selectedSite?.let { selectedSite ->
                    coroutineScope.launch {
                        val result = withContext(Dispatchers.Default) {
                            wooCommerceStore.fetchSupportedApiVersion(selectedSite)
                        }
                        result.error?.let {
                            prependToLog("Error in onApiVersionFetched: ${it.type} - ${it.message}")
                        }
                        result.model?.let {
                            val formattedVersion = it.apiVersion?.substringAfterLast("/")
                            prependToLog("Max Woo version for ${selectedSite.name}: $formattedVersion")
                        }
                    }
                }
            }

            fetchSettings.setOnClickListener {
                selectedSite?.let { site ->
                    coroutineScope.launch {
                        val result = withContext(Dispatchers.Default) {
                            wooCommerceStore.fetchSiteGeneralSettings(site)
                        }
                        result.error?.let {
                            prependToLog("Error fetching settings: ${it.type} - ${it.message}")
                        }
                        result.model?.let {
                            prependToLog(
                                "Updated site settings for ${site.name}:\n" +
                                    it.toString()
                            )
                        }
                    }
                } ?: showNoWCSitesToast()
            }

            fetchProductSettings.setOnClickListener {
                selectedSite?.let { site ->
                    coroutineScope.launch {
                        val result = withContext(Dispatchers.Default) {
                            wooCommerceStore.fetchSiteProductSettings(site)
                        }
                        result.error?.let {
                            prependToLog("Error in onWCProductSettingsChanged: ${it.type} - ${it.message}")
                        }
                        result.model?.let {
                            prependToLog(
                                "Updated product settings for ${site.name}: " +
                                    "weight unit = ${it.weightUnit}, dimension unit = ${it.dimensionUnit}"
                            )
                        } ?: prependToLog("Error getting product settings from db")
                    }
                } ?: showNoWCSitesToast()
            }

            getUserRole.setOnClickListener {
                selectedSite?.let { site ->
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
                selectedSite?.let {
                    replaceFragment(WooOrdersFragment())
                } ?: showNoWCSitesToast()
            }

            products.setOnClickListener {
                selectedSite?.let {
                    replaceFragment(WooProductsFragment())
                } ?: showNoWCSitesToast()
            }

            statsRevenue.setOnClickListener {
                selectedSite?.let {
                    replaceFragment(WooRevenueStatsFragment())
                } ?: showNoWCSitesToast()
            }

            refunds.setOnClickListener {
                selectedSite?.let {
                    replaceFragment(WooRefundsFragment())
                } ?: showNoWCSitesToast()
            }

            gateways.setOnClickListener {
                selectedSite?.let {
                    replaceFragment(WooGatewaysFragment())
                } ?: showNoWCSitesToast()
            }

            taxes.setOnClickListener {
                selectedSite?.let {
                    replaceFragment(WooTaxFragment())
                } ?: showNoWCSitesToast()
            }

            shippingLabels.setOnClickListener {
                selectedSite?.let {
                    replaceFragment(WooShippingLabelFragment())
                } ?: showNoWCSitesToast()
            }

            leaderboards.setOnClickListener {
                selectedSite?.let {
                    replaceFragment(WooLeaderboardsFragment())
                } ?: showNoWCSitesToast()
            }

            countries.setOnClickListener {
                launchCountriesRequest()
            }

            attributes.setOnClickListener {
                selectedSite?.let {
                    replaceFragment(WooProductAttributeFragment())
                } ?: showNoWCSitesToast()
            }

            customers.setOnClickListener {
                selectedSite?.let {
                    replaceFragment(WooCustomersFragment())
                } ?: showNoWCSitesToast()
            }

            storeCreation.setOnClickListener {
                replaceFragment(WooStoreCreationFragment())
            }

            coupons.setOnClickListener {
                replaceFragment(WooCouponsFragment())
            }

            helpSupport.setOnClickListener {
                selectedSite?.let {
                    replaceFragment(WooHelpSupportFragment())
                } ?: showNoWCSitesToast()
            }

            storeOnboarding.setOnClickListener {
                replaceFragment(WooOnboardingFragment())
            }

            wooAdmin.setOnClickListener {
                replaceFragment(WooAdminFragment())
            }

            wooGoogleAdsStatus.setOnClickListener {
                selectedSite?.let { selectedSite ->
                    prependToLog("Fetching Google Ads connection status...")
                    coroutineScope.launch {
                        val result = withContext(Dispatchers.Default) {
                            wooGoogleStore.isGoogleAdsAccountConnected(selectedSite)
                        }
                        result.error?.let {
                            prependToLog("Error in fetchGoogleAdsConnectionStatus: ${it.type} - ${it.message}")
                        }
                        result.model?.let {
                            prependToLog("Google Ads connection status: $it")
                        } ?: prependToLog("Couldn't fetch Google Ads connection status.")
                    }
                } ?: showNoWCSitesToast()
            }

            wooGoogleAdsCampaigns.setOnClickListener {
                selectedSite?.let { selectedSite ->
                    prependToLog("Fetching Google Ads campaigns...")
                    coroutineScope.launch {
                        val result = withContext(Dispatchers.Default) {
                            wooGoogleStore.fetchGoogleAdsCampaigns(selectedSite)
                        }
                        result.error?.let {
                            prependToLog("Error in fetchGoogleAdsCampaigns: ${it.type} - ${it.message}")
                        }
                        result.model?.let { campaigns ->
                            prependToLog("Fetched ${campaigns.size} Google Ads campaigns")
                            campaigns.forEach { campaign ->
                                prependToLog("Campaign ID: ${campaign.id}, " +
                                    "Name: ${campaign.name}, " +
                                    "Status: ${campaign.status}"
                                )
                            }
                        } ?: prependToLog("Couldn't fetch Google Ads campaigns.")
                    }
                } ?: showNoWCSitesToast()
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun launchCountriesRequest() {
        coroutineScope.launch {
            try {
                selectedSite?.let { selectedSite ->
                    wooDataStore.fetchCountriesAndStates(selectedSite).model?.let { country ->
                        country.filter { it.parentCode.isEmpty() }
                            .forEach { location ->
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

    private fun showNoWCSitesToast() {
        ToastUtils.showToast(activity, "No WooCommerce sites found for this account!")
    }
}
