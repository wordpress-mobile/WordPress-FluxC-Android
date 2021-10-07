package org.wordpress.android.fluxc.example.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_woo_product_attribute.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.StoreSelectingFragment
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.attribute.WCGlobalAttributeModel
import org.wordpress.android.fluxc.model.attribute.terms.WCAttributeTermModel
import org.wordpress.android.fluxc.store.WCGlobalAttributeStore
import javax.inject.Inject

class WooProductAttributeFragment : StoreSelectingFragment() {
    @Inject internal lateinit var wcAttributesStore: WCGlobalAttributeStore

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_woo_product_attribute, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetch_product_attributes.setOnClickListener(::onFetchAttributesListClicked)
        fetch_product_attributes_from_db.setOnClickListener(::onFetchCachedAttributesListClicked)
        create_product_attributes.setOnClickListener(::onCreateAttributeButtonClicked)
        delete_product_attributes.setOnClickListener(::onDeleteAttributeButtonClicked)
        update_product_attributes.setOnClickListener(::onUpdateAttributeButtonClicked)
        fetch_product_single_attribute.setOnClickListener(::onFetchAttributeButtonClicked)
        fetch_term_for_attribute.setOnClickListener(::onFetchAttributeTermsButtonClicked)
        create_term_for_attribute.setOnClickListener(::onCreateAttributeTermButtonClicked)
    }

    private fun onCreateAttributeButtonClicked(view: View) {
        try {
            showSingleLineDialog(
                    activity,
                    "Enter the attribute name you want to create:"
            ) { editText ->
                coroutineScope.launch {
                    takeAsyncRequestWithValidSite {
                        wcAttributesStore.createAttribute(it, editText.text.toString())
                    }?.apply {
                        model?.let { logSingleAttributeResponse(it) }
                                ?.let { prependToLog("========== Attribute Created =========") }
                                ?: takeIf { isError }?.let {
                                    prependToLog("Failed to create Attribute. Error: ${error.message}")
                                }
                    } ?: prependToLog("Failed to create Attribute. Error: Unknown")
                }
            }
        } catch (ex: Exception) {
            prependToLog("Couldn't create Attributes. Error: ${ex.message}")
        }
    }

    private fun onDeleteAttributeButtonClicked(view: View) {
        try {
            showSingleLineDialog(
                    activity,
                    "Enter the attribute ID you want to remove:"
            ) { editText ->
                coroutineScope.launch {
                    takeAsyncRequestWithValidSite {
                        wcAttributesStore.deleteAttribute(
                                it,
                                editText.text.toString().toLongOrNull() ?: 0
                        )
                    }?.apply {
                        model?.let { logSingleAttributeResponse(it) }
                                ?.let { prependToLog("========== Attribute Deleted =========") }
                                ?: takeIf { isError }?.let {
                                    prependToLog("Failed to delete Attribute. Error: ${error.message}")
                                }
                    } ?: prependToLog("Failed to delete Attribute. Error: Unknown")
                }
            }
        } catch (ex: Exception) {
            prependToLog("Couldn't create Attributes. Error: ${ex.message}")
        }
    }

    private fun onUpdateAttributeButtonClicked(view: View) {
        try {
            showSingleLineDialog(
                    activity,
                    "Enter the attribute ID you want to update:"
            ) { attributeIdEditText ->
                showSingleLineDialog(
                        activity,
                        "Enter the attribute new name:"
                ) { attributeNameEditText ->
                    coroutineScope.launch {
                        takeAsyncRequestWithValidSite {
                            wcAttributesStore.updateAttribute(
                                    it,
                                    attributeIdEditText.text.toString().toLongOrNull() ?: 0,
                                    attributeNameEditText.text.toString()
                            )
                        }?.apply {
                            model?.let { logSingleAttributeResponse(it) }
                                    ?.let { prependToLog("========== Attribute Updated =========") }
                                    ?: takeIf { isError }?.let {
                                        prependToLog("Failed to update Attribute. Error: ${error.message}")
                                    }
                        } ?: prependToLog("Failed to update Attribute. Error: Unknown")
                    }
                }
            }
        } catch (ex: Exception) {
            prependToLog("Couldn't create Attributes. Error: ${ex.message}")
        }
    }

    private fun onFetchAttributeButtonClicked(view: View) {
        try {
            showSingleLineDialog(
                    activity,
                    "Enter the attribute ID you want to remove:"
            ) { editText ->
                coroutineScope.launch {
                    takeAsyncRequestWithValidSite {
                        wcAttributesStore.fetchAttribute(
                                it,
                                editText.text.toString().toLongOrNull() ?: 0
                        )
                    }?.apply {
                        model?.let { logSingleAttributeResponse(it) }
                                ?.let { prependToLog("========== Attribute Fetched =========") }
                                ?: takeIf { isError }?.let {
                                    prependToLog("Failed to delete Attribute. Error: ${error.message}")
                                }
                    } ?: prependToLog("Failed to delete Attribute. Error: Unknown")
                }
            }
        } catch (ex: Exception) {
            prependToLog("Couldn't create Attributes. Error: ${ex.message}")
        }
    }

    private fun onFetchAttributeTermsButtonClicked(view: View) {
        try {
            showSingleLineDialog(
                    activity,
                    "Enter the attribute ID you want to fetch terms:"
            ) { editText ->
                coroutineScope.launch {
                    takeAsyncRequestWithValidSite {
                        wcAttributesStore.fetchAttributeTerms(
                                it,
                                editText.text.toString().toLongOrNull() ?: 0
                        )
                    }?.apply {
                        model?.forEach {
                            logSingleAttributeTermResponse(it) }
                                ?.let { prependToLog("========== Attribute Terms Fetched =========") }
                                ?: takeIf { isError }?.let {
                                    prependToLog("Failed to fetch Terms. Error: ${error.message}")
                                }
                    } ?: prependToLog("Failed to fetch Terms. Error: Unknown")
                }
            }
        } catch (ex: Exception) {
            prependToLog("Couldn't create Attributes. Error: ${ex.message}")
        }
    }

    private fun onCreateAttributeTermButtonClicked(view: View) {
        try {
            showSingleLineDialog(
                    activity,
                    "Enter the attribute ID you want to add terms:"
            ) { attributeIdEditText ->
                showSingleLineDialog(
                        activity,
                        "Enter the term name you want to create:"
                ) { termEditText ->
                    coroutineScope.launch {
                        takeAsyncRequestWithValidSite {
                            wcAttributesStore.createOptionValueForAttribute(
                                    it,
                                    attributeIdEditText.text.toString().toLongOrNull() ?: 0,
                                    termEditText.text.toString()
                            )
                        }?.apply {
                            model?.let { logSingleAttributeResponse(it) }
                                    ?.let { prependToLog("========== Attribute Term Created =========") }
                                    ?: takeIf { isError }?.let {
                                        prependToLog("Failed to delete Attribute. Error: ${error.message}")
                                    }
                        } ?: prependToLog("Failed to create Attribute Term. Error: Unknown")
                    }
                }
            }
        } catch (ex: Exception) {
            prependToLog("Couldn't create Attribute Term. Error: ${ex.message}")
        }
    }

    private fun onFetchAttributesListClicked(view: View) = coroutineScope.launch {
        try {
            takeAsyncRequestWithValidSite {
                    wcAttributesStore.fetchStoreAttributes(it)
            }
                    ?.model
                    ?.let { logAttributeListResponse(it) }
        } catch (ex: Exception) {
            prependToLog("Couldn't fetch Products Attributes. Error: ${ex.message}")
        }
    }

    private fun onFetchCachedAttributesListClicked(view: View) = coroutineScope.launch {
        try {
            takeAsyncRequestWithValidSite {
                wcAttributesStore.loadCachedStoreAttributes(it)
            }
                    ?.model
                    ?.let { logAttributeListResponse(it) }
        } catch (ex: Exception) {
            prependToLog("Couldn't fetch Products Attributes. Error: ${ex.message}")
        }
    }

    private fun logSingleAttributeResponse(response: WCGlobalAttributeModel) {
        response.let {
            response.terms
                    ?.filterNotNull()
                    ?.forEachIndexed { index, wcAttributeTermModel ->
                logSingleAttributeTermResponse(wcAttributeTermModel, index)
            }
            prependToLog("  Attribute slug: ${it.slug.ifEmpty { "Slug not available" }}")
            prependToLog("  Attribute type: ${it.type.ifEmpty { "Type not available" }}")
            prependToLog("  Attribute name: ${it.name.ifEmpty { "Attribute name not available" }}")
            prependToLog("  Attribute remote id: ${it.remoteId}")
            prependToLog("  --------- Attribute ---------")
        }
    }

    private fun logSingleAttributeTermResponse(response: WCAttributeTermModel, termIndex: Int? = null) {
        response.let {
            prependToLog("    Term name: ${it.name}")
            prependToLog("    Term id: ${it.remoteId}")
            prependToLog("    --------- Attribute Term ${termIndex ?: ""} ---------")
        }
    }

    private fun logAttributeListResponse(model: List<WCGlobalAttributeModel>) {
        model.forEach(::logSingleAttributeResponse)
        prependToLog("========== Full Site Attribute list =========")
    }

    private suspend inline fun <T> takeAsyncRequestWithValidSite(crossinline action: suspend (SiteModel) -> T) =
            selectedSite?.let {
                withContext(Dispatchers.Default) {
                    action(it)
                }
            }
}
