package org.wordpress.android.fluxc.example.ui.products

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_batch_generate_variations.*
import kotlinx.android.synthetic.main.fragment_woo_batch_update_variations.product_id
import kotlinx.android.synthetic.main.fragment_woo_batch_update_variations.status
import kotlinx.android.synthetic.main.fragment_woo_batch_update_variations.update_product_info
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.example.R.layout
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.model.VariationAttributes
import org.wordpress.android.fluxc.model.WCProductVariationModel.ProductVariantOption
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.BatchProductVariationsApiResponse
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.BatchGenerateVariationsPayload
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooBatchGenerateVariationsFragment : Fragment() {
    @Inject internal lateinit var wcProductStore: WCProductStore
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    private var selectedSitePosition: Int = -1

    private lateinit var payload: BatchGenerateVariationsPayload
    private val currentAttributes = mutableListOf<ProductVariantOption>()
    private val variations = mutableListOf<VariationAttributes>()

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
        inflater.inflate(layout.fragment_woo_batch_generate_variations, container, false)

    @Suppress("LongMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        enableVariationModificationInputs(false)
        update_product_info.setOnClickListener {
            val site = wooCommerceStore.getWooCommerceSites().getOrNull(selectedSitePosition)
            if (site == null) {
                prependToLog("No valid site found...doing nothing")
                return@setOnClickListener
            }

            val productId = product_id.getText().toLongOrNull()
            if (productId == null) {
                prependToLog("Product with id is empty or has wrong format...doing nothing")
                return@setOnClickListener
            }
            val product = wcProductStore.getProductByRemoteId(site, productId)
            if (product == null) {
                prependToLog("Product with id: $productId not found in DB. Did you forget to fetch?...doing nothing")
                return@setOnClickListener
            }

            status.text = "Selected product: $productId"

            payload = BatchGenerateVariationsPayload(
                site = site,
                remoteProductId = productId,
                variations = variations
            )

            enableVariationModificationInputs(true)
        }
        add_attribute.setOnClickListener {
            val attrId = attr_id.getText().toLongOrNull()
            val attrName = attr_name.getText()
            val attrOption = attr_option.getText()

            attr_id.setText("")
            attr_name.setText("")
            attr_option.setText("")

            if (attrId == null || attrName.isEmpty()) {
                prependToLog("Invalid attribute params...doing nothing")
                return@setOnClickListener
            }
            val variationOption = ProductVariantOption(attrId, attrName, attrOption)
            currentAttributes.add(variationOption)
            displayAttributes()
        }
        add_variation.setOnClickListener {
            variations.add(currentAttributes.toList())
            currentAttributes.clear()
            attr_status.text = ""
            displayVariations()
        }

        generate_variation.setOnClickListener { runBatchCreate(payload) }
    }

    private fun displayVariations() {
        val variationsString = variationsToString()
        variation_status.text = if (variationsString.isNotEmpty()) {
            "Current variations: \n $variationsString"
        } else {
            ""
        }
    }

    private fun displayAttributes() {
        val currentAttributesString = attributesToString(currentAttributes, "\n")
        attr_status.text = if (currentAttributesString.isNotEmpty()) {
            "Current attribute: \n $currentAttributesString"
        } else {
            ""
        }
    }

    private fun variationsToString(): String {
        val stringBuffer = StringBuffer()
        variations.forEach { attributes ->
            val attributesString = attributesToString(attributes, " ")
            stringBuffer.append(attributesString)
            stringBuffer.append("\n")
        }
        return stringBuffer.toString()
    }

    private fun attributesToString(
        attributes: VariationAttributes,
        separator: String
    ): String {
        val stringBuffer = StringBuffer()
        attributes.forEach { attr ->
            stringBuffer.append("[${attr.id},${attr.name},${attr.option}]")
            stringBuffer.append(separator)
        }
        return stringBuffer.toString()
    }

    private fun runBatchCreate(payload: BatchGenerateVariationsPayload) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result: WooResult<BatchProductVariationsApiResponse> =
                wcProductStore.batchGenerateVariations(payload)

            withContext(Dispatchers.Main) {
                if (result.isError) {
                    prependToLog("Error: ${result.error.message}")
                } else {
                    val count = result.model?.createdVariations?.count() ?: 0
                    prependToLog("Success: $count variations created")
                }
                // reset view
                product_id.setText("")
                status.text = ""
                variation_status.text = ""
                attr_status.text = ""
                variations.clear()
                enableVariationModificationInputs(false)
            }
        }
    }

    private fun enableVariationModificationInputs(value: Boolean) {
        attr_id.isEnabled = value
        attr_name.isEnabled = value
        attr_option.isEnabled = value
        add_attribute.isEnabled = value
        add_variation.isEnabled = value
        generate_variation.isEnabled = value
    }

    companion object {
        const val ARG_SELECTED_SITE_POS = "ARG_SELECTED_SITE_POS"

        fun newInstance(selectedSitePosition: Int): WooBatchGenerateVariationsFragment {
            val fragment = WooBatchGenerateVariationsFragment()
            val args = Bundle()
            args.putInt(ARG_SELECTED_SITE_POS, selectedSitePosition)
            fragment.arguments = args
            return fragment
        }
    }
}
