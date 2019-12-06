package org.wordpress.android.fluxc.example.ui.products

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.content.Context
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
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog.Listener
import org.wordpress.android.fluxc.example.utils.onTextChanged
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductBackOrders
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStockStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductTaxStatus
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnProductUpdated
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductPayload
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.utils.DateUtils
import java.util.Calendar
import javax.inject.Inject

class WooUpdateProductFragment : Fragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wcProductStore: WCProductStore
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    private var selectedSitePosition: Int = -1
    private var selectedProductModel: WCProductModel? = null

    companion object {
        const val ARG_SELECTED_SITE_POS = "ARG_SELECTED_SITE_POS"

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        product_enter_product_id.setOnClickListener {
            showSingleLineDialog(activity, "Enter the remoteProductId of product to fetch:") { editText ->
                val selectedRemoteProductId = editText.text.toString().toLongOrNull()
                selectedRemoteProductId?.let { id ->
                    updateSelectedProductId(id)
                    enableProductDependentButtons()
                    product_entered_product_id.text = editText.text.toString()
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
                if (child is Button || child is EditText) {
                    child.isEnabled = isChecked
                }
            }
        }

        product_tax_status.setOnClickListener {
            showListSelectorDialog(CoreProductTaxStatus.values().map { it.value }.toList(), object : Listener {
                override fun onListItemSelected(selectedItem: String?) {
                    selectedItem?.let {
                        product_tax_status.text = it
                        selectedProductModel?.taxStatus = it
                    }
                }
            })
        }

        product_stock_status.setOnClickListener {
            showListSelectorDialog(CoreProductStockStatus.values().map { it.value }.toList(), object : Listener {
                override fun onListItemSelected(selectedItem: String?) {
                    selectedItem?.let {
                        product_stock_status.text = it
                        selectedProductModel?.stockStatus = it
                    }
                }
            })
        }

        product_back_orders.setOnClickListener {
            showListSelectorDialog(CoreProductBackOrders.values().map { it.value }.toList(), object : Listener {
                override fun onListItemSelected(selectedItem: String?) {
                    selectedItem?.let {
                        product_back_orders.text = it
                        selectedProductModel?.backorders = it
                    }
                }
            })
        }

        product_from_date.setOnClickListener {
            showDatePickerDialog(product_from_date.text.toString(), OnDateSetListener { _, year, month, dayOfMonth ->
                product_from_date.text = DateUtils.getFormattedDateString(year, month, dayOfMonth)
                selectedProductModel?.dateOnSaleFrom = product_from_date.text.toString()
            })
        }

        product_to_date.setOnClickListener {
            showDatePickerDialog(product_to_date.text.toString(), OnDateSetListener { _, year, month, dayOfMonth ->
                product_to_date.text = DateUtils.getFormattedDateString(year, month, dayOfMonth)
                selectedProductModel?.dateOnSaleTo = product_to_date.text.toString()
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
    }

    private fun updateSelectedProductId(remoteProductId: Long) {
        getWCSite()?.let { siteModel ->
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
                product_from_date.text = it.dateOnSaleFrom.split('T')[0]
                product_to_date.text = it.dateOnSaleTo.split('T')[0]
                product_manage_stock.isChecked = it.manageStock
                product_stock_status.text = it.stockStatus
                product_back_orders.text = it.backorders
                product_stock_quantity.setText(it.stockQuantity.toString())
            } ?: WCProductModel().apply { this.remoteProductId = remoteProductId }
        } ?: prependToLog("No valid site found...doing nothing")
    }

    private fun showListSelectorDialog(listItems: List<String>, listener: Listener) {
        fragmentManager?.let { fm ->
            val dialog = ListSelectorDialog.newInstance(listItems, listener)
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
