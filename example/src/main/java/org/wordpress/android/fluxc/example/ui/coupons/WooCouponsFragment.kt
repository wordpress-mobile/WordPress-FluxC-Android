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
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.StoreSelectingFragment
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.CouponStore
import javax.inject.Inject

class WooCouponsFragment : StoreSelectingFragment() {
    @Inject internal lateinit var store: CouponStore

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_woo_coupons, container, false)

    override fun onSiteSelected(site: SiteModel) {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(State.STARTED) {
                store.observeCoupons(site).collect { coupons ->
                    val codes = coupons.map { it.couponEntity.code }.joinToString()
                    prependToLog("Coupons changed: [$codes]")
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnFetchCoupons.setOnClickListener {
            coroutineScope.launch {
                store.fetchCoupons(selectedSite!!)
            }
        }
    }
}
