package org.wordpress.android.fluxc.example.ui.products

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_leaderboards.*
import kotlinx.android.synthetic.main.fragment_woo_leaderboards.buttonContainer
import kotlinx.android.synthetic.main.fragment_woo_product_attribute.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.StoreSelectorDialog
import org.wordpress.android.fluxc.example.utils.toggleSiteDependentButtons
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.AttributeApiResponse
import org.wordpress.android.fluxc.store.WCProductAttributesStore
import javax.inject.Inject

class WooProductAttributeFragment : Fragment(), StoreSelectorDialog.Listener {
    @Inject internal lateinit var wcAttributesStore: WCProductAttributesStore

    private var selectedPos: Int = -1
    private var selectedSite: SiteModel? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onSiteSelected(site: SiteModel, pos: Int) {
        selectedSite = site
        selectedPos = pos
        buttonContainer.toggleSiteDependentButtons()
        attributes_selected_site.text = site.name ?: site.displayName
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            inflater.inflate(R.layout.fragment_woo_product_attribute, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        attributes_select_site.setOnClickListener(::onProductAttributesSelectSiteButtonClicked)
        fetch_product_attributes.setOnClickListener(::launchFullAttributeListRequest)
    }

    private fun onProductAttributesSelectSiteButtonClicked(view: View) {
        fragmentManager?.let { fm ->
            StoreSelectorDialog.newInstance(this, selectedPos)
                    .show(fm, "StoreSelectorDialog")
        }
    }

    private fun launchFullAttributeListRequest(view: View) = coroutineScope.launch {
        try {
            takeAsyncRequestWithValidSite { wcAttributesStore.fetchStoreAttributes(it) }
                    ?.model
                    ?.let { logAttributeListResponse(it) }
        } catch (ex: Exception) {
            prependToLog("Couldn't fetch Products Attributes. Error: ${ex.message}")
        }
    }

    private fun logAttributeListResponse(model: List<AttributeApiResponse>) {
        model.forEach {
            prependToLog("  Attribute slug: ${it.slug ?: "Slug not available"}")
            prependToLog("  Attribute type: ${it.type ?: "Type not available"}")
            prependToLog("  Attribute name: ${it.name ?: "Attribute name not available"}")
            prependToLog("  Attribute id: ${it.id ?: "Attribute id not available"}")
            prependToLog("  --------- Attribute ---------")
        }
        prependToLog("========== Full Site Attribute list =========")
    }

    private suspend inline fun <T> takeAsyncRequestWithValidSite(crossinline action: suspend (SiteModel) -> T) =
            selectedSite?.let {
                withContext(Dispatchers.Default) {
                    action(it)
                }
            }
}
