package org.wordpress.android.fluxc.example.ui.products

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.example.databinding.FragmentWooBatchGenerateVariationsBinding
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
    ): View = FragmentWooBatchGenerateVariationsBinding.inflate(inflater, container, false).root

    @Suppress("LongMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentWooBatchGenerateVariationsBinding.bind(view)) {
            enableVariationModificationInputs(false)
            updateProductInfo.setOnClickListener {
                val site = wooCommerceStore.getWooCommerceSites().getOrNull(selectedSitePosition)
                if (site == null) {
                    prependToLog("No valid site found...doing nothing")
                    return@setOnClickListener
                }

                val productId = productId.getText().toLongOrNull()
                if (productId == null) {
                    prependToLog("Product with id is empty or has wrong format...doing nothing")
                    return@setOnClickListener
                }
                val product = wcProductStore.getProductByRemoteId(site, productId)
                if (product == null) {
                    prependToLog(
                        "Product with id: $productId not found in DB. " +
                            "Did you forget to fetch?...doing nothing"
                    )
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
            addAttribute.setOnClickListener {
                val attrIdText = attrId.getText().toLongOrNull()
                val attrNameText = attrName.getText()
                val attrOptionText = attrOption.getText()

                attrId.setText("")
                attrName.setText("")
                attrOption.setText("")

                if (attrIdText == null || attrNameText.isEmpty()) {
                    prependToLog("Invalid attribute params...doing nothing")
                    return@setOnClickListener
                }
                val variationOption = ProductVariantOption(attrIdText, attrNameText, attrOptionText)
                currentAttributes.add(variationOption)
                displayAttributes()
            }
            addVariation.setOnClickListener {
                variations.add(currentAttributes.toList())
                currentAttributes.clear()
                attrStatus.text = ""
                displayVariations()
            }

            generateVariation.setOnClickListener { runBatchCreate(payload) }
        }
    }

    private fun FragmentWooBatchGenerateVariationsBinding.displayVariations() {
        val variationsString = variationsToString()
        variationStatus.text = if (variationsString.isNotEmpty()) {
            "Current variations: \n $variationsString"
        } else {
            ""
        }
    }

    private fun FragmentWooBatchGenerateVariationsBinding.displayAttributes() {
        val currentAttributesString = attributesToString(currentAttributes, "\n")
        attrStatus.text = if (currentAttributesString.isNotEmpty()) {
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

    private fun FragmentWooBatchGenerateVariationsBinding.runBatchCreate(payload: BatchGenerateVariationsPayload) {
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
                productId.setText("")
                status.text = ""
                variationStatus.text = ""
                attrStatus.text = ""
                variations.clear()
                enableVariationModificationInputs(false)
            }
        }
    }

    private fun FragmentWooBatchGenerateVariationsBinding.enableVariationModificationInputs(value: Boolean) {
        attrId.isEnabled = value
        attrName.isEnabled = value
        attrOption.isEnabled = value
        addAttribute.isEnabled = value
        addVariation.isEnabled = value
        generateVariation.isEnabled = value
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
