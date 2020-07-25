package org.wordpress.android.fluxc.example.ui.products

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_update_variation.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.R.layout
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.FloatingLabelEditText
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog.Companion.ARG_LIST_SELECTED_ITEM
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog.Companion.LIST_SELECTOR_REQUEST_CODE
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductBackOrders
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStockStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductTaxStatus
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnVariationUpdated
import org.wordpress.android.fluxc.store.WCProductStore.UpdateVariationPayload
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.utils.DateUtils
import org.wordpress.android.util.StringUtils
import java.util.Calendar
import javax.inject.Inject

class WooUpdateVariationFragment : Fragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wcProductStore: WCProductStore
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    private var selectedSitePosition: Int = -1
    private var selectedRemoteProductId: Long? = null
    private var selectedRemoteVariationId: Long? = null
    private var selectedVariationModel: WCProductVariationModel? = null

    companion object {
        const val ARG_SELECTED_SITE_POS = "ARG_SELECTED_SITE_POS"
        const val ARG_SELECTED_PRODUCT_ID = "ARG_SELECTED_PRODUCT_ID"
        const val ARG_SELECTED_VARIATION_ID = "ARG_SELECTED_VARIATION_ID"
        const val LIST_RESULT_CODE_TAX_STATUS = 101
        const val LIST_RESULT_CODE_STOCK_STATUS = 102
        const val LIST_RESULT_CODE_BACK_ORDERS = 103

        fun newInstance(selectedSitePosition: Int): WooUpdateVariationFragment {
            val fragment = WooUpdateVariationFragment()
            val args = Bundle()
            args.putInt(ARG_SELECTED_SITE_POS, selectedSitePosition)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            selectedSitePosition = it.getInt(ARG_SELECTED_SITE_POS, 0)
        }
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(layout.fragment_woo_update_variation, container, false)

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        super.onStop()
        dispatcher.unregister(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ARG_SELECTED_SITE_POS, selectedSitePosition)
        selectedRemoteProductId?.let { outState.putLong(ARG_SELECTED_PRODUCT_ID, it) }
        selectedRemoteVariationId?.let { outState.putLong(ARG_SELECTED_VARIATION_ID, it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        product_enter_product_id.setOnClickListener {
            showSingleLineDialog(activity, "Enter the remoteProductId of product to fetch:") { editText ->
                selectedRemoteProductId = editText.text.toString().toLongOrNull()
                selectedRemoteProductId?.let { productId ->
                    showSingleLineDialog(activity, "Enter the remoteVariation of variation to fetch:") { editText ->
                        selectedRemoteVariationId = editText.text.toString().toLongOrNull()
                        selectedRemoteVariationId?.let { variationId ->
                            updateSelectedProductId(productId, variationId)
                        } ?: prependToLog("No valid remoteVariation defined...doing nothing")
                    }
                } ?: prependToLog("No valid remoteProductId defined...doing nothing")
            }
        }

        product_description.onTextChanged { selectedVariationModel?.description = it }
        product_sku.onTextChanged { selectedVariationModel?.sku = it }
        product_regular_price.onTextChanged { selectedVariationModel?.regularPrice = it }
        product_sale_price.onTextChanged { selectedVariationModel?.salePrice = it }
        product_width.onTextChanged { selectedVariationModel?.width = it }
        product_height.onTextChanged { selectedVariationModel?.height = it }
        product_length.onTextChanged { selectedVariationModel?.length = it }
        product_weight.onTextChanged { selectedVariationModel?.weight = it }
        product_stock_quantity.onTextChanged {
            if (it.isNotEmpty()) { selectedVariationModel?.stockQuantity = it.toInt() }
        }

        product_manage_stock.setOnCheckedChangeListener { _, isChecked ->
            selectedVariationModel?.manageStock = isChecked
            for (i in 0 until manageStockContainer.childCount) {
                val child = manageStockContainer.getChildAt(i)
                if (child is Button || child is FloatingLabelEditText) {
                    child.isEnabled = isChecked
                }
            }
        }

        variation_visibility.setOnCheckedChangeListener { _, isChecked ->
            selectedVariationModel?.status = if (isChecked) "publish" else "private"
        }

        product_tax_status.setOnClickListener {
            showListSelectorDialog(
                    CoreProductTaxStatus.values().map { it.value }.toList(),
                    LIST_RESULT_CODE_TAX_STATUS, selectedVariationModel?.taxStatus
            )
        }

        product_stock_status.setOnClickListener {
            showListSelectorDialog(
                    CoreProductStockStatus.values().map { it.value }.toList(),
                    LIST_RESULT_CODE_STOCK_STATUS, selectedVariationModel?.stockStatus
            )
        }

        product_back_orders.setOnClickListener {
            showListSelectorDialog(
                    CoreProductBackOrders.values().map { it.value }.toList(),
                    LIST_RESULT_CODE_BACK_ORDERS, selectedVariationModel?.backorders
            )
        }

        product_from_date.setOnClickListener {
            showDatePickerDialog(product_from_date.text.toString(), OnDateSetListener { _, year, month, dayOfMonth ->
                product_from_date.text = DateUtils.getFormattedDateString(year, month, dayOfMonth)
                selectedVariationModel?.dateOnSaleFromGmt = product_from_date.text.toString()
            })
        }

        product_to_date.setOnClickListener {
            showDatePickerDialog(product_to_date.text.toString(), OnDateSetListener { _, year, month, dayOfMonth ->
                product_to_date.text = DateUtils.getFormattedDateString(year, month, dayOfMonth)
                selectedVariationModel?.dateOnSaleToGmt = product_to_date.text.toString()
            })
        }

        product_update.setOnClickListener {
            getWCSite()?.let { site ->
                if (selectedVariationModel?.remoteProductId != null &&
                        selectedVariationModel?.remoteVariationId != null) {
                    val payload = UpdateVariationPayload(site, selectedVariationModel!!)
                    dispatcher.dispatch(WCProductActionBuilder.newUpdateVariationAction(payload))
                } else {
                    prependToLog("No valid remoteProductId or remoteVariationId defined...doing nothing")
                }
            } ?: prependToLog("No site found...doing nothing")
        }

        product_menu_order.onTextChanged { selectedVariationModel?.menuOrder = StringUtils.stringToInt(it) }

        savedInstanceState?.let { bundle ->
            selectedRemoteProductId = bundle.getLong(ARG_SELECTED_PRODUCT_ID)
            selectedRemoteVariationId = bundle.getLong(ARG_SELECTED_VARIATION_ID)
            selectedSitePosition = bundle.getInt(ARG_SELECTED_SITE_POS)
        }

        if (selectedRemoteProductId != null && selectedRemoteVariationId != null) {
            updateSelectedProductId(selectedRemoteProductId!!, selectedRemoteVariationId!!)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LIST_SELECTOR_REQUEST_CODE) {
            val selectedItem = data?.getStringExtra(ARG_LIST_SELECTED_ITEM)
            when (resultCode) {
                LIST_RESULT_CODE_TAX_STATUS -> {
                    selectedItem?.let {
                        product_tax_status.text = it
                        selectedVariationModel?.taxStatus = it
                    }
                }
                LIST_RESULT_CODE_STOCK_STATUS -> {
                    selectedItem?.let {
                        product_stock_status.text = it
                        selectedVariationModel?.stockStatus = it
                    }
                }
                LIST_RESULT_CODE_BACK_ORDERS -> {
                    selectedItem?.let {
                        product_back_orders.text = it
                        selectedVariationModel?.backorders = it
                    }
                }
            }
        }
    }

    private fun updateSelectedProductId(remoteProductId: Long, remoteVariationId: Long) {
        getWCSite()?.let { siteModel ->
            enableProductDependentButtons()
            product_entered_product_id.text = "P: $remoteProductId, V: $remoteVariationId"

            selectedVariationModel = wcProductStore.getVariationByRemoteId(
                    siteModel,
                    remoteProductId,
                    remoteVariationId
            )?.also {
                product_description.setText(it.description)
                product_sku.setText(it.sku)
                product_regular_price.setText(it.regularPrice)
                product_sale_price.setText(it.salePrice)
                product_width.setText(it.width)
                product_height.setText(it.height)
                product_length.setText(it.length)
                product_weight.setText(it.weight)
                product_tax_status.text = it.taxStatus
                product_from_date.text = it.dateOnSaleFromGmt.split('T')[0]
                product_to_date.text = it.dateOnSaleToGmt.split('T')[0]
                product_manage_stock.isChecked = it.manageStock
                product_stock_status.text = it.stockStatus
                product_back_orders.text = it.backorders
                product_stock_quantity.setText(it.stockQuantity.toString())
                product_stock_quantity.isEnabled = product_manage_stock.isChecked
                product_menu_order.setText(it.menuOrder.toString())
                variation_visibility.isChecked = it.status == "publish"
            } ?: WCProductVariationModel().apply {
                this.remoteProductId = remoteProductId
                this.remoteVariationId = remoteVariationId
                prependToLog("Variation not found in the DB. Did you fetch the variations?")
            }
        } ?: prependToLog("No valid site found...doing nothing")
    }

    private fun showListSelectorDialog(listItems: List<String>, resultCode: Int, selectedItem: String?) {
        fragmentManager?.let { fm ->
            val dialog = ListSelectorDialog.newInstance(
                    this, listItems, resultCode, selectedItem
            )
            dialog.show(fm, "ListSelectorDialog")
        }
    }

    private fun showDatePickerDialog(dateString: String?, listener: OnDateSetListener) {
        val date = if (dateString.isNullOrEmpty()) {
            DateUtils.getCurrentDateString()
        } else dateString
        val calendar = DateUtils.getCalendarInstance(date)
        DatePickerDialog(requireActivity(), listener, calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE))
                .show()
    }

    private fun getWCSite() = wooCommerceStore.getWooCommerceSites().getOrNull(selectedSitePosition)

    private fun enableProductDependentButtons() {
        for (i in 0 until productContainer.childCount) {
            val child = productContainer.getChildAt(i)
            if (child is Button || child is EditText) {
                child.isEnabled = true
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductUpdated(event: OnVariationUpdated) {
        if (event.isError) {
            prependToLog("Error from " + event.causeOfChange + " - error: " + event.error.type)
            return
        }
        prependToLog("Variation updated ${event.rowsAffected}")
    }
}
