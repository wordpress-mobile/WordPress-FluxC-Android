package org.wordpress.android.fluxc.example.ui.coupons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.example.databinding.FragmentWooCouponsBinding
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.replaceFragment
import org.wordpress.android.fluxc.example.ui.StoreSelectingFragment
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.CouponStore
import org.wordpress.android.fluxc.store.CouponStore.Companion.DEFAULT_PAGE
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.Date
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

class WooCouponsFragment : StoreSelectingFragment() {
    @Inject internal lateinit var store: CouponStore
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var couponPage = DEFAULT_PAGE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentWooCouponsBinding.inflate(inflater, container, false).root

    override fun onSiteSelected(site: SiteModel) {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(State.STARTED) {
                store.observeCoupons(site).collect { coupons ->
                    val codes = coupons.joinToString(",\n") {
                        "${it.coupon.code}(ID ${it.coupon.id})"
                    }
                    prependToLog("Coupons changed (${coupons.size}): \n$codes\n")
                }
            }
        }
    }

    @Suppress("LongMethod", "ComplexMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentWooCouponsBinding.bind(view)) {
            btnEnableCouponsFeature.setOnClickListener {
                coroutineScope.launch {
                    val result = wooCommerceStore.enableCoupons(selectedSite!!)
                    if (result) {
                        prependToLog("Coupons enabling succeeded.")
                    } else {
                        prependToLog("Coupons enabling failed.")
                    }
                }
            }

            btnFetchCoupons.setOnClickListener {
                coroutineScope.launch {
                    val result = store.fetchCoupons(selectedSite!!, couponPage++, pageSize = 3)
                    if (result.model == true) {
                        btnFetchCoupons.text = "Fetch More Coupons"
                    } else {
                        btnFetchCoupons.text = "Can't load more coupons"
                        btnFetchCoupons.isEnabled = false
                    }
                }
            }

            btnDeleteCoupon.setOnClickListener {
                selectedSite?.let { site ->
                    showSingleLineDialog(
                        activity,
                        "Enter a coupon ID to be deleted:"
                    ) { editText ->
                        editText.text.toString().toLongOrNull()?.let {
                            lifecycleScope.launch {
                                val result = store.deleteCoupon(site, it, false)
                                if (result.isError) {
                                    prependToLog("Coupon deletion failed: ${result.error.message}")
                                } else {
                                    prependToLog("Coupon successfully deleted")
                                }
                            }
                        } ?: prependToLog("Invalid coupon ID")
                    }
                }
            }

            btnFetchSingleCoupon.setOnClickListener {
                showSingleLineDialog(activity, "Enter the coupon ID to fetch:") { editText ->
                    editText.text.toString().toLongOrNull()?.let {
                        coroutineScope.launch {
                            val result = store.fetchCoupon(selectedSite!!, it)
                            if (result.isError) {
                                prependToLog("Coupon fetching failed: ${result.error.message}")
                            } else {
                                prependToLog(
                                    store.getCoupon(selectedSite!!, it)?.coupon?.toString()
                                        ?: "Coupon $it not found"
                                )
                            }
                        }
                    } ?: prependToLog("Invalid coupon ID")
                }
            }

            btnFetchCouponReport.setOnClickListener {
                coroutineScope.launch {
                    val couponId = showSingleLineDialog(
                        requireActivity(),
                        "Enter the coupon Id:",
                        isNumeric = true
                    )?.toLongOrNull()
                        ?: run {
                            prependToLog("Please enter a valid id")
                            return@launch
                        }

                    val reportResult = store.fetchCouponReport(selectedSite!!, couponId)

                    when {
                        reportResult.isError ->
                            prependToLog("Fetching report failed, ${reportResult.error.message}")

                        else -> {
                            val report = reportResult.model!!
                            val usageAmountFormatted = wooCommerceStore.formatCurrencyForDisplay(
                                report.amount.toDouble(),
                                selectedSite!!,
                                applyDecimalFormatting = true
                            )
                            prependToLog(
                                "Coupon was used ${report.ordersCount} times and " +
                                    "resulted in $usageAmountFormatted savings"
                            )
                        }
                    }
                }
            }

            btnFetchMostActiveCoupons.setOnClickListener {
                val site = selectedSite ?: return@setOnClickListener
                coroutineScope.launch {
                    val reportResult = store.fetchMostActiveCoupons(
                        site = site,
                        dateRange = Date().apply { time -= 30.days.inWholeMilliseconds }..Date(),
                        limit = 3
                    )
                    if (reportResult.isError) {
                        prependToLog("Fetching report failed, ${reportResult.error.message}")
                    } else {
                        prependToLog(
                            "Most active coupons in the last month:\n" +
                                reportResult.model!!.joinToString(",\n")
                        )
                    }
                }
            }

            btnSearchCoupons.setOnClickListener {
                showSingleLineDialog(activity, "Enter a search query:") { editText ->
                    coroutineScope.launch {
                        val query = editText.text.toString()
                        val searchResult = store.searchCoupons(selectedSite!!, query)
                        val title = "Coupon search (\"$query\") results:"
                        val results = searchResult.model!!.coupons.joinToString(",\n") {
                            "${it.coupon.code}(ID ${it.coupon.id})"
                        }
                        prependToLog("$title\n$results\n")
                    }
                }
            }

            btnUpdateCoupon.setOnClickListener {
                replaceFragment(WooUpdateCouponFragment.newInstance(selectedSite!!.siteId))
            }

            btnCreateCoupon.setOnClickListener {
                replaceFragment(WooUpdateCouponFragment.newInstance(selectedSite!!.siteId, true))
            }
        }
    }
}
