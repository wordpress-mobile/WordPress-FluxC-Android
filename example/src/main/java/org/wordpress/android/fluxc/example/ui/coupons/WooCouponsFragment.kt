package org.wordpress.android.fluxc.example.ui.coupons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.android.synthetic.main.fragment_woo_coupons.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.StoreSelectingFragment
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.CouponStore
import org.wordpress.android.fluxc.store.CouponStore.Companion.DEFAULT_PAGE
import javax.inject.Inject

class WooCouponsFragment : StoreSelectingFragment() {
    @Inject internal lateinit var store: CouponStore
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var couponPage = DEFAULT_PAGE

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_woo_coupons, container, false)

    override fun onSiteSelected(site: SiteModel) {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(State.STARTED) {
                store.observeCoupons(site).collect { coupons ->
                    val codes = coupons.joinToString(",\n") {
                        "${it.couponEntity.code}(ID ${it.couponEntity.id})"
                    }
                    prependToLog("Coupons changed (${coupons.size}): \n$codes\n")
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                        store.fetchCoupon(selectedSite!!, it)
                        prependToLog(
                            store.observeCoupon(selectedSite!!, it)
                                .first()?.couponEntity?.toString() ?: "Coupon $it not found"
                        )
                    }
                } ?: prependToLog("Invalid coupon ID")
            }
        }
    }
}
