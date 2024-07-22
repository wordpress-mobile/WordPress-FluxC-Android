package org.wordpress.android.fluxc.example.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.example.databinding.FragmentWooProductAttributeBinding
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentWooProductAttributeBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentWooProductAttributeBinding.bind(view)) {
            fetchProductAttributes.setOnClickListener { onFetchAttributesListClicked() }
            fetchProductAttributesFromDb.setOnClickListener { onFetchCachedAttributesListClicked() }
            createProductAttributes.setOnClickListener { onCreateAttributeButtonClicked() }
            deleteProductAttributes.setOnClickListener { onDeleteAttributeButtonClicked() }
            updateProductAttributes.setOnClickListener { onUpdateAttributeButtonClicked() }
            fetchProductSingleAttribute.setOnClickListener { onFetchAttributeButtonClicked() }
            fetchTermForAttribute.setOnClickListener { onFetchAttributeTermsButtonClicked() }
            createTermForAttribute.setOnClickListener { onCreateAttributeTermButtonClicked() }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun onCreateAttributeButtonClicked() {
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

    @Suppress("TooGenericExceptionCaught")
    private fun onDeleteAttributeButtonClicked() {
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

    @Suppress("TooGenericExceptionCaught")
    private fun onUpdateAttributeButtonClicked() {
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

    @Suppress("TooGenericExceptionCaught")
    private fun onFetchAttributeButtonClicked() {
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

    @Suppress("TooGenericExceptionCaught")
    private fun onFetchAttributeTermsButtonClicked() {
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

    @Suppress("TooGenericExceptionCaught")
    private fun onCreateAttributeTermButtonClicked() {
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

    @Suppress("TooGenericExceptionCaught")
    private fun onFetchAttributesListClicked() = coroutineScope.launch {
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

    @Suppress("TooGenericExceptionCaught")
    private fun onFetchCachedAttributesListClicked() = coroutineScope.launch {
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
