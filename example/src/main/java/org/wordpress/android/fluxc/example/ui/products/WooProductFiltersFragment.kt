package org.wordpress.android.fluxc.example.ui.products

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_product_filters.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCTS
import org.wordpress.android.fluxc.example.R.layout
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog.Companion.ARG_LIST_SELECTED_ITEM
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog.Companion.LIST_SELECTOR_REQUEST_CODE
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStockStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductType
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductsPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption.STATUS
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption.STOCK_STATUS
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption.TYPE
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.io.Serializable
import javax.inject.Inject

class WooProductFiltersFragment : Fragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    private var selectedSiteId: Int = -1
    private var filterOptions: MutableMap<ProductFilterOption, String>? = null
    companion object {
        const val ARG_SELECTED_SITE_ID = "ARG_SELECTED_SITE_ID"
        const val ARG_SELECTED_FILTER_OPTIONS = "ARG_SELECTED_FILTER_OPTIONS"

        const val LIST_RESULT_CODE_STOCK_STATUS = 101
        const val LIST_RESULT_CODE_PRODUCT_TYPE = 102
        const val LIST_RESULT_CODE_PRODUCT_STATUS = 103

        @JvmStatic
        fun newInstance(selectedSitePosition: Int): WooProductFiltersFragment {
            return WooProductFiltersFragment().apply {
                this.selectedSiteId = selectedSitePosition
            }
        }
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        super.onStop()
        dispatcher.unregister(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(layout.fragment_woo_product_filters, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        savedInstanceState?.let { bundle -> selectedSiteId = bundle.getInt(ARG_SELECTED_SITE_ID) }
        filterOptions = savedInstanceState?.getSerializable(ARG_SELECTED_FILTER_OPTIONS)
                as? MutableMap<ProductFilterOption, String> ?: mutableMapOf()

        filter_stock_status.setOnClickListener {
            showListSelectorDialog(
                    CoreProductStockStatus.values().map { it.value }.toList(),
                    LIST_RESULT_CODE_STOCK_STATUS, filterOptions?.get(STOCK_STATUS)
            )
        }

        filter_by_status.setOnClickListener {
            showListSelectorDialog(
                    CoreProductStatus.values().map { it.value }.toList(),
                    LIST_RESULT_CODE_PRODUCT_STATUS, filterOptions?.get(STATUS)
            )
        }

        filter_by_type.setOnClickListener {
            showListSelectorDialog(
                    CoreProductType.values().map { it.value }.toList(),
                    LIST_RESULT_CODE_PRODUCT_TYPE, filterOptions?.get(TYPE)
            )
        }

        filter_products.setOnClickListener {
            getWCSite()?.let { site ->
                val payload = FetchProductsPayload(site, filterOptions = filterOptions)
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductsAction(payload))
            } ?: prependToLog("No valid siteId defined...doing nothing")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ARG_SELECTED_SITE_ID, selectedSiteId)
        outState.putSerializable(ARG_SELECTED_FILTER_OPTIONS, filterOptions as? Serializable)
    }

    private fun getWCSite() = wooCommerceStore.getWooCommerceSites().getOrNull(selectedSiteId)

    private fun showListSelectorDialog(listItems: List<String>, resultCode: Int, selectedItem: String? = null) {
        fragmentManager?.let { fm ->
            val dialog = ListSelectorDialog.newInstance(
                    this, listItems, resultCode, selectedItem
            )
            dialog.show(fm, "ListSelectorDialog")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LIST_SELECTOR_REQUEST_CODE) {
            val selectedItem = data?.getStringExtra(ARG_LIST_SELECTED_ITEM)
            when (resultCode) {
                LIST_RESULT_CODE_PRODUCT_TYPE -> {
                    selectedItem?.let { filterOptions?.put(TYPE, it) }
                }
                LIST_RESULT_CODE_STOCK_STATUS -> {
                    selectedItem?.let { filterOptions?.put(STOCK_STATUS, it) }
                }
                LIST_RESULT_CODE_PRODUCT_STATUS -> {
                    selectedItem?.let { filterOptions?.put(STATUS, it) }
                }
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductChanged(event: OnProductChanged) {
        if (event.isError) {
            prependToLog("Error from " + event.causeOfChange + " - error: " + event.error.type)
            return
        }

        if (event.causeOfChange == FETCH_PRODUCTS) {
            prependToLog("Fetched ${event.rowsAffected} products")
        }
    }
}
