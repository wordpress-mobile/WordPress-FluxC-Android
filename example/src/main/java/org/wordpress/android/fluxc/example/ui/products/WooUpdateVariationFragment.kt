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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.databinding.FragmentWooUpdateVariationBinding
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.FloatingLabelEditText
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog.Companion.ARG_LIST_SELECTED_ITEM
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog.Companion.LIST_SELECTOR_REQUEST_CODE
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
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

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private var binding: FragmentWooUpdateVariationBinding? = null

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWooUpdateVariationBinding.inflate(inflater, container, false)
        return binding!!.root
    }

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

    @Suppress("LongMethod", "ComplexMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.productEnterProductId?.setOnClickListener {
            showSingleLineDialog(activity, "Enter the remoteProductId of product to fetch:") { editText ->
                selectedRemoteProductId = editText.text.toString().toLongOrNull()
                selectedRemoteProductId?.let { productId ->
                    showSingleLineDialog(activity, "Enter the remoteVariation of variation to fetch:") { editText ->
                        selectedRemoteVariationId = editText.text.toString().toLongOrNull()
                        selectedRemoteVariationId?.let { variationId ->
                            binding?.updateSelectedProductId(productId, variationId)
                        } ?: prependToLog("No valid remoteVariation defined...doing nothing")
                    }
                } ?: prependToLog("No valid remoteProductId defined...doing nothing")
            }
        }

        binding?.productDescription?.onTextChanged { selectedVariationModel?.description = it }
        binding?.productSku?.onTextChanged { selectedVariationModel?.sku = it }
        binding?.productRegularPrice?.onTextChanged { selectedVariationModel?.regularPrice = it }
        binding?.productSalePrice?.onTextChanged { selectedVariationModel?.salePrice = it }
        binding?.productWidth?.onTextChanged { selectedVariationModel?.width = it }
        binding?.productHeight?.onTextChanged { selectedVariationModel?.height = it }
        binding?.productLength?.onTextChanged { selectedVariationModel?.length = it }
        binding?.productWeight?.onTextChanged { selectedVariationModel?.weight = it }
        binding?.productStockQuantity?.onTextChanged {
            if (it.isNotEmpty()) { selectedVariationModel?.stockQuantity = it.toDouble() }
        }

        binding?.productManageStock?.setOnCheckedChangeListener { _, isChecked ->
            selectedVariationModel?.manageStock = isChecked
            for (i in 0 until binding?.manageStockContainer?.childCount!!) {
                val child = binding?.manageStockContainer?.getChildAt(i)
                if (child is Button || child is FloatingLabelEditText) {
                    child.isEnabled = isChecked
                }
            }
        }

        binding?.variationVisibility?.setOnCheckedChangeListener { _, isChecked ->
            selectedVariationModel?.status = if (isChecked) "publish" else "private"
        }

        binding?.productTaxStatus?.setOnClickListener {
            showListSelectorDialog(
                    CoreProductTaxStatus.values().map { it.value }.toList(),
                    LIST_RESULT_CODE_TAX_STATUS, selectedVariationModel?.taxStatus
            )
        }

        binding?.productStockStatus?.setOnClickListener {
            showListSelectorDialog(
                    CoreProductStockStatus.values().map { it.value }.toList(),
                    LIST_RESULT_CODE_STOCK_STATUS, selectedVariationModel?.stockStatus
            )
        }

        binding?.productBackOrders?.setOnClickListener {
            showListSelectorDialog(
                    CoreProductBackOrders.values().map { it.value }.toList(),
                    LIST_RESULT_CODE_BACK_ORDERS, selectedVariationModel?.backorders
            )
        }

        binding?.productFromDate?.setOnClickListener {
            showDatePickerDialog(binding?.productFromDate?.text.toString(), { _, year, month, dayOfMonth ->
                binding?.productFromDate?.text = DateUtils.getFormattedDateString(year, month, dayOfMonth)
                selectedVariationModel?.dateOnSaleFromGmt = binding?.productFromDate?.text.toString()
            })
        }

        binding?.productToDate?.setOnClickListener {
            showDatePickerDialog(binding?.productToDate?.text.toString(), { _, year, month, dayOfMonth ->
                binding?.productToDate?.text = DateUtils.getFormattedDateString(year, month, dayOfMonth)
                selectedVariationModel?.dateOnSaleToGmt = binding?.productToDate?.text.toString()
            })
        }

        binding?.productUpdate?.setOnClickListener {
            getWCSite()?.let { site ->
                if (selectedVariationModel?.remoteProductId != null &&
                    selectedVariationModel?.remoteVariationId != null) {
                    coroutineScope.launch {
                        val result = wcProductStore.updateVariation(
                            UpdateVariationPayload(
                                site,
                                selectedVariationModel!!
                            )
                        )
                        if (result.isError) {
                            prependToLog("Updating Variation Failed: " + result.error.type)
                        } else {
                            prependToLog("Variation updated ${result.rowsAffected}")
                        }
                    }
                } else {
                    prependToLog("No valid remoteProductId or remoteVariationId defined...doing nothing")
                }
            } ?: prependToLog("No site found...doing nothing")
        }

        binding?.productMenuOrder?.onTextChanged { selectedVariationModel?.menuOrder = StringUtils.stringToInt(it) }

        savedInstanceState?.let { bundle ->
            selectedRemoteProductId = bundle.getLong(ARG_SELECTED_PRODUCT_ID)
            selectedRemoteVariationId = bundle.getLong(ARG_SELECTED_VARIATION_ID)
            selectedSitePosition = bundle.getInt(ARG_SELECTED_SITE_POS)
        }

        if (selectedRemoteProductId != null && selectedRemoteVariationId != null) {
            binding?.updateSelectedProductId(selectedRemoteProductId!!, selectedRemoteVariationId!!)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LIST_SELECTOR_REQUEST_CODE) {
            val selectedItem = data?.getStringExtra(ARG_LIST_SELECTED_ITEM)
            when (resultCode) {
                LIST_RESULT_CODE_TAX_STATUS -> {
                    selectedItem?.let {
                        binding?.productTaxStatus?.text = it
                        selectedVariationModel?.taxStatus = it
                    }
                }
                LIST_RESULT_CODE_STOCK_STATUS -> {
                    selectedItem?.let {
                        binding?.productStockStatus?.text = it
                        selectedVariationModel?.stockStatus = it
                    }
                }
                LIST_RESULT_CODE_BACK_ORDERS -> {
                    selectedItem?.let {
                        binding?.productBackOrders?.text = it
                        selectedVariationModel?.backorders = it
                    }
                }
            }
        }
    }

    private fun FragmentWooUpdateVariationBinding.updateSelectedProductId(remoteProductId: Long, remoteVariationId: Long) {
        getWCSite()?.let { siteModel ->
            enableProductDependentButtons()
            productEnteredProductId.text = "P: $remoteProductId, V: $remoteVariationId"

            selectedVariationModel = wcProductStore.getVariationByRemoteId(
                    siteModel,
                    remoteProductId,
                    remoteVariationId
            )?.also {
                productDescription.setText(it.description)
                productSku.setText(it.sku)
                productRegularPrice.setText(it.regularPrice)
                productSalePrice.setText(it.salePrice)
                productWidth.setText(it.width)
                productHeight.setText(it.height)
                productLength.setText(it.length)
                productWeight.setText(it.weight)
                productTaxStatus.text = it.taxStatus
                productFromDate.text = it.dateOnSaleFromGmt.split('T')[0]
                productToDate.text = it.dateOnSaleToGmt.split('T')[0]
                productManageStock.isChecked = it.manageStock
                productStockStatus.text = it.stockStatus
                productBackOrders.text = it.backorders
                productStockQuantity.setText(it.stockQuantity.toString())
                productStockQuantity.isEnabled = productManageStock.isChecked
                productMenuOrder.setText(it.menuOrder.toString())
                variationVisibility.isChecked = it.status == "publish"
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

    private fun FragmentWooUpdateVariationBinding.enableProductDependentButtons() {
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

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}
