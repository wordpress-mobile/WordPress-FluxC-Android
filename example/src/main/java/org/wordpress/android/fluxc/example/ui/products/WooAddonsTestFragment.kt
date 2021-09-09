package org.wordpress.android.fluxc.example.ui.products

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_addons.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.R.layout
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WCAddonsStore
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductPayload
import javax.inject.Inject

class WooAddonsTestFragment : DialogFragment() {
    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var wcProductStore: WCProductStore
    @Inject lateinit var wcAddonsStore: WCAddonsStore
    @Inject lateinit var siteStore: SiteStore

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    companion object {
        private const val SELECTED_SITE_REMOTE_ID = "selected_site_remote_id"

        @JvmStatic
        fun show(fragmentManager: FragmentManager, selectedSiteRemoteId: Long) =
                WooAddonsTestFragment().apply {
                    arguments = Bundle().apply {
                        this.putLong(SELECTED_SITE_REMOTE_ID, selectedSiteRemoteId)
                    }
                }.show(fragmentManager, null)
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(layout.fragment_woo_addons, container, false)

        addonsResult = view!!.findViewById(R.id.addons_result)
        return view
    }

    lateinit var selectedProduct: WCProductModel
    lateinit var addonsResult: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val selectedSiteRemoteId = arguments!!.getLong(SELECTED_SITE_REMOTE_ID)
        val selectedSite = siteStore.getSiteBySiteId(selectedSiteRemoteId)!!

        addons_product_remote_id_apply.setOnClickListener {
            val selectedProductRemoteId = addons_product_remote_id.text.toString().toLong()
            selectedProduct = wcProductStore.getProductByRemoteId(selectedSite, selectedProductRemoteId)!!

            startObserving(selectedSiteRemoteId, selectedProduct)
        }

        addons_fetch_product.setOnClickListener {
            dispatcher.dispatch(
                    WCProductActionBuilder.newFetchSingleProductAction(
                            FetchSingleProductPayload(
                                    selectedSite,
                                    selectedProduct.remoteProductId
                            )
                    )
            )
        }

        addons_fetch_global.setOnClickListener {
            coroutineScope.launch {
                wcAddonsStore.fetchAllGlobalAddonsGroups(selectedSite)
            }
        }
    }

    private fun startObserving(selectedSiteRemoteId: Long, productModel: WCProductModel) {
        coroutineScope.launch {
            wcAddonsStore.observeAllAddonsForProduct(
                    selectedSiteRemoteId,
                    productModel
            ).collect {
                addonsResult.text = it.joinToString { addon ->
                    "\n- \"${addon.name}\" of type ${addon::class.simpleName}"
                }
            }
        }
    }
}
