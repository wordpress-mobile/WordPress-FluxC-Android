package org.wordpress.android.fluxc.example.ui.products

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_batch_update_variations.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.example.R.layout
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.BatchProductVariationsApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStockStatus
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.BatchUpdateVariationsPayload
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.utils.DateUtils
import java.util.Calendar
import javax.inject.Inject

class WooBatchUpdateVariationsFragment : Fragment() {
    @Inject internal lateinit var wcProductStore: WCProductStore
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    private var selectedSitePosition: Int = -1

    private lateinit var variationsUpdatePayloadBuilder: BatchUpdateVariationsPayload.Builder

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
    ): View? =
        inflater.inflate(layout.fragment_woo_batch_update_variations, container, false)

    @Suppress("LongMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        update_product_info.setOnClickListener {
            val site = getWCSite()
            if (site == null) {
                prependToLog("No valid site found...doing nothing")
                return@setOnClickListener
            }

            val productId = getProductIdInput()
            if (productId == null) {
                prependToLog("Product with id is empty or has wrong format...doing nothing")
                return@setOnClickListener
            }
            val product = wcProductStore.getProductByRemoteId(site, productId)
            if (product == null) {
                prependToLog("Product with id: $productId not found in DB. Did you forget to fetch?...doing nothing")
                return@setOnClickListener
            }

            val variationsIds: Collection<Long> = getVariationsIdsInput()
            if (variationsIds.isEmpty()) {
                prependToLog("Variations are empty or have wrong format...doing nothing")
                return@setOnClickListener
            }
            variationsIds.forEach { variationId ->
                val variation = wcProductStore.getVariationByRemoteId(site, productId, variationId)
                if (variation == null) {
                    val msg = "Variation with id: $variationId not found in DB. " +
                        "Did you forget to fetch?...doing nothing"
                    prependToLog(msg)
                    return@setOnClickListener
                }
            }

            variationsUpdatePayloadBuilder = BatchUpdateVariationsPayload.Builder(
                site,
                productId,
                variationsIds
            )

            status.text = "Selected product: $productId. Variation ids: ${variationsIds.joinToString("|")}"

            enableVariationModificationInputs()
        }

        stock_status_button.setOnClickListener {
            activity?.supportFragmentManager?.let { fm ->
                val items = CoreProductStockStatus.values().map { it.value }
                val dialog = ListSelectorDialog.newInstance(this, items, LIST_RESULT_CODE_STOCK_STATUS, null)
                dialog.show(fm, "StockStatusDialog")
            }
        }

        date_on_sale_from.setOnClickListener {
            showDatePickerDialog(date_on_sale_from.text.toString()) { _, y, m, d ->
                date_on_sale_from.text = DateUtils.getFormattedDateString(y, m, d)
                variationsUpdatePayloadBuilder.startOfSale(date_on_sale_from.text.toString())
            }
        }

        date_on_sale_to.setOnClickListener {
            showDatePickerDialog(date_on_sale_to.text.toString()) { _, y, m, d ->
                date_on_sale_to.text = DateUtils.getFormattedDateString(y, m, d)
                variationsUpdatePayloadBuilder.endOfSale(date_on_sale_to.text.toString())
            }
        }

        invoke_button.setOnClickListener {
            val payload = buildPayload()
            runBatchUpdate(payload)
        }
    }

    private fun getProductIdInput() = try {
        product_id.getText().toLong()
    } catch (e: NumberFormatException) {
        null
    }

    private fun getVariationsIdsInput() = try {
        variations_ids.getText().split(",").map(String::toLong)
    } catch (e: NumberFormatException) {
        emptyList()
    }

    @Suppress("ComplexMethod")
    private fun buildPayload(): BatchUpdateVariationsPayload = with(variationsUpdatePayloadBuilder) {
        with(regular_price.getText()) {
            if (isNotEmpty()) variationsUpdatePayloadBuilder.regularPrice(this)
        }
        with(sale_price.getText()) {
            if (isNotEmpty()) variationsUpdatePayloadBuilder.salePrice(this)
        }
        with(stock_quantity.getText()) {
            if (isNotEmpty()) variationsUpdatePayloadBuilder.stockQuantity(this.toInt())
        }
        with(weight.getText()) {
            if (isNotEmpty()) variationsUpdatePayloadBuilder.weight(this)
        }
        val length = length.getText()
        val width = width.getText()
        val height = height.getText()
        if (length.isNotEmpty() || width.isNotEmpty() || height.isNotEmpty()) {
            variationsUpdatePayloadBuilder.dimensions(length = length, width = width, height = height)
        }
        with(shipping_class_id.getText()) {
            if (isNotEmpty()) variationsUpdatePayloadBuilder.shippingClassId(this)
        }
        with(shipping_class.getText()) {
            if (isNotEmpty()) variationsUpdatePayloadBuilder.shippingClassSlug(this)
        }
        build()
    }

    private fun runBatchUpdate(payload: BatchUpdateVariationsPayload) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result: WooResult<BatchProductVariationsApiResponse> =
                wcProductStore.batchUpdateVariations(payload)

            withContext(Dispatchers.Main) {
                if (result.isError) {
                    prependToLog("Error: ${result.error.message}")
                } else {
                    val count = result.model?.updatedVariations?.count() ?: 0
                    prependToLog("Success: $count variations updated")
                }
            }
        }
    }

    private fun enableVariationModificationInputs() {
        arrayOf(
            regular_price,
            sale_price,
            date_on_sale_from,
            date_on_sale_to,
            stock_quantity,
            stock_status_button,
            weight,
            shipping_class_id,
            shipping_class,
            invoke_button,
            width,
            height,
            length
        ).forEach { it.isEnabled = true }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ListSelectorDialog.LIST_SELECTOR_REQUEST_CODE) {
            val selectedItem = data?.getStringExtra(ListSelectorDialog.ARG_LIST_SELECTED_ITEM)
            when (resultCode) {
                LIST_RESULT_CODE_STOCK_STATUS -> {
                    selectedItem?.let { name ->
                        stock_status_button.text = name
                        variationsUpdatePayloadBuilder.stockStatus(
                            CoreProductStockStatus.values().first { it.value == name }
                        )
                    }
                }
            }
        }
    }

    private fun showDatePickerDialog(dateString: String?, listener: OnDateSetListener) {
        val date = if (dateString.isNullOrEmpty()) DateUtils.getCurrentDateString() else dateString
        val calendar = DateUtils.getCalendarInstance(date)
        DatePickerDialog(
            requireActivity(), listener, calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE)
        )
        .show()
    }

    private fun getWCSite() = wooCommerceStore.getWooCommerceSites().getOrNull(selectedSitePosition)

    companion object {
        const val ARG_SELECTED_SITE_POS = "ARG_SELECTED_SITE_POS"

        const val LIST_RESULT_CODE_STOCK_STATUS = 101

        fun newInstance(selectedSitePosition: Int): WooBatchUpdateVariationsFragment {
            val fragment = WooBatchUpdateVariationsFragment()
            val args = Bundle()
            args.putInt(ARG_SELECTED_SITE_POS, selectedSitePosition)
            fragment.arguments = args
            return fragment
        }
    }
}
