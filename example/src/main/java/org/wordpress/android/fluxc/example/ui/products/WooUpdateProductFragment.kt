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
import kotlinx.android.synthetic.main.fragment_woo_update_product.*
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
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductBackOrders
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStockStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductTaxStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductVisibility
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnProductUpdated
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductPayload
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.utils.DateUtils
import org.wordpress.android.util.StringUtils
import java.util.Calendar
import javax.inject.Inject

class WooUpdateProductFragment : Fragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wcProductStore: WCProductStore
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    private var selectedSitePosition: Int = -1
    private var selectedRemoteProductId: Long? = null
    private var selectedProductModel: WCProductModel? = null

    companion object {
        const val ARG_SELECTED_SITE_POS = "ARG_SELECTED_SITE_POS"
        const val ARG_SELECTED_PRODUCT_ID = "ARG_SELECTED_PRODUCT_ID"
        const val LIST_RESULT_CODE_TAX_STATUS = 101
        const val LIST_RESULT_CODE_STOCK_STATUS = 102
        const val LIST_RESULT_CODE_BACK_ORDERS = 103
        const val LIST_RESULT_CODE_VISIBILITY = 104
        const val LIST_RESULT_CODE_STATUS = 105

        fun newInstance(selectedSitePosition: Int): WooUpdateProductFragment {
            val fragment = WooUpdateProductFragment()
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

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(layout.fragment_woo_update_product, container, false)

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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        product_enter_product_id.setOnClickListener {
            showSingleLineDialog(activity, "Enter the remoteProductId of product to fetch:") { editText ->
                selectedRemoteProductId = editText.text.toString().toLongOrNull()
                selectedRemoteProductId?.let { id ->
                    updateSelectedProductId(id)
                } ?: prependToLog("No valid remoteProductId defined...doing nothing")
            }
        }

        product_name.onTextChanged { selectedProductModel?.name = it }
        product_description.onTextChanged { selectedProductModel?.description = it }
        product_sku.onTextChanged { selectedProductModel?.sku = it }
        product_short_desc.onTextChanged { selectedProductModel?.shortDescription = it }
        product_regular_price.onTextChanged { selectedProductModel?.regularPrice = it }
        product_sale_price.onTextChanged { selectedProductModel?.salePrice = it }
        product_width.onTextChanged { selectedProductModel?.width = it }
        product_height.onTextChanged { selectedProductModel?.height = it }
        product_length.onTextChanged { selectedProductModel?.length = it }
        product_weight.onTextChanged { selectedProductModel?.weight = it }
        product_stock_quantity.onTextChanged {
            if (it.isNotEmpty()) { selectedProductModel?.stockQuantity = it.toInt() }
        }

        product_sold_individually.setOnCheckedChangeListener { _, isChecked ->
            selectedProductModel?.soldIndividually = isChecked
        }

        product_manage_stock.setOnCheckedChangeListener { _, isChecked ->
            selectedProductModel?.manageStock = isChecked
            for (i in 0 until manageStockContainer.childCount) {
                val child = manageStockContainer.getChildAt(i)
                if (child is Button || child is FloatingLabelEditText) {
                    child.isEnabled = isChecked
                }
            }
        }

        product_tax_status.setOnClickListener {
            showListSelectorDialog(
                    CoreProductTaxStatus.values().map { it.value }.toList(),
                    LIST_RESULT_CODE_TAX_STATUS, selectedProductModel?.taxStatus
            )
        }

        product_stock_status.setOnClickListener {
            showListSelectorDialog(
                    CoreProductStockStatus.values().map { it.value }.toList(),
                    LIST_RESULT_CODE_STOCK_STATUS, selectedProductModel?.stockStatus
            )
        }

        product_back_orders.setOnClickListener {
            showListSelectorDialog(
                    CoreProductBackOrders.values().map { it.value }.toList(),
                    LIST_RESULT_CODE_BACK_ORDERS, selectedProductModel?.backorders
            )
        }

        product_from_date.setOnClickListener {
            showDatePickerDialog(product_from_date.text.toString(), OnDateSetListener { _, year, month, dayOfMonth ->
                product_from_date.text = DateUtils.getFormattedDateString(year, month, dayOfMonth)
                selectedProductModel?.dateOnSaleFromGmt = product_from_date.text.toString()
            })
        }

        product_to_date.setOnClickListener {
            showDatePickerDialog(product_to_date.text.toString(), OnDateSetListener { _, year, month, dayOfMonth ->
                product_to_date.text = DateUtils.getFormattedDateString(year, month, dayOfMonth)
                selectedProductModel?.dateOnSaleToGmt = product_to_date.text.toString()
            })
        }

        product_update.setOnClickListener {
            getWCSite()?.let { site ->
                if (selectedProductModel?.remoteProductId != null) {
                    val payload = UpdateProductPayload(site, selectedProductModel!!)
                    dispatcher.dispatch(WCProductActionBuilder.newUpdateProductAction(payload))
                } else {
                    prependToLog("No valid remoteProductId defined...doing nothing")
                }
            } ?: prependToLog("No site found...doing nothing")
        }

        product_status.setOnClickListener {
            showListSelectorDialog(
                    CoreProductStatus.values().map { it.value }.toList(),
                    LIST_RESULT_CODE_STATUS, selectedProductModel?.status
            )
        }

        product_catalog_visibility.setOnClickListener {
            showListSelectorDialog(
                    CoreProductVisibility.values().map { it.value }.toList(),
                    LIST_RESULT_CODE_VISIBILITY, selectedProductModel?.catalogVisibility
            )
        }

        product_is_featured.setOnCheckedChangeListener { _, isChecked ->
            selectedProductModel?.featured = isChecked
        }

        product_reviews_allowed.setOnCheckedChangeListener { _, isChecked ->
            selectedProductModel?.reviewsAllowed = isChecked
        }

        product_purchase_note.onTextChanged { selectedProductModel?.purchaseNote = it }

        product_slug.onTextChanged { selectedProductModel?.slug = it }

        product_menu_order.onTextChanged { selectedProductModel?.menuOrder = StringUtils.stringToInt(it) }

        savedInstanceState?.let { bundle ->
            selectedRemoteProductId = bundle.getLong(ARG_SELECTED_PRODUCT_ID)
            selectedSitePosition = bundle.getInt(ARG_SELECTED_SITE_POS)
            selectedRemoteProductId?.let { updateSelectedProductId(it) }
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
                        selectedProductModel?.taxStatus = it
                    }
                }
                LIST_RESULT_CODE_STOCK_STATUS -> {
                    selectedItem?.let {
                        product_stock_status.text = it
                        selectedProductModel?.stockStatus = it
                    }
                }
                LIST_RESULT_CODE_BACK_ORDERS -> {
                    selectedItem?.let {
                        product_back_orders.text = it
                        selectedProductModel?.backorders = it
                    }
                }
                LIST_RESULT_CODE_STATUS -> {
                    selectedItem?.let {
                        product_status.text = it
                        selectedProductModel?.status = it
                    }
                }
                LIST_RESULT_CODE_VISIBILITY -> {
                    selectedItem?.let {
                        product_catalog_visibility.text = it
                        selectedProductModel?.catalogVisibility = it
                    }
                }
            }
        }
    }

    private fun updateSelectedProductId(remoteProductId: Long) {
        getWCSite()?.let { siteModel ->
            enableProductDependentButtons()
            product_entered_product_id.text = remoteProductId.toString()

            selectedProductModel = wcProductStore.getProductByRemoteId(siteModel, remoteProductId)?.also {
                product_name.setText(it.name)
                product_description.setText(it.description)
                product_sku.setText(it.sku)
                product_short_desc.setText(it.shortDescription)
                product_regular_price.setText(it.regularPrice)
                product_sale_price.setText(it.salePrice)
                product_width.setText(it.width)
                product_height.setText(it.height)
                product_length.setText(it.length)
                product_weight.setText(it.weight)
                product_tax_status.text = it.taxStatus
                product_sold_individually.isChecked = it.soldIndividually
                product_from_date.text = it.dateOnSaleFromGmt.split('T')[0]
                product_to_date.text = it.dateOnSaleToGmt.split('T')[0]
                product_manage_stock.isChecked = it.manageStock
                product_stock_status.text = it.stockStatus
                product_back_orders.text = it.backorders
                product_stock_quantity.setText(it.stockQuantity.toString())
                product_stock_quantity.isEnabled = product_manage_stock.isChecked
                product_catalog_visibility.text = it.catalogVisibility
                product_status.text = it.status
                product_slug.setText(it.slug)
                product_is_featured.isChecked = it.featured
                product_reviews_allowed.isChecked = it.reviewsAllowed
                product_purchase_note.setText(it.purchaseNote)
                product_menu_order.setText(it.menuOrder.toString())
            } ?: WCProductModel().apply { this.remoteProductId = remoteProductId }
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
    fun onProductUpdated(event: OnProductUpdated) {
        if (event.isError) {
            prependToLog("Error from " + event.causeOfChange + " - error: " + event.error.type)
            return
        }
        prependToLog("Product updated ${event.rowsAffected}")
    }
}
