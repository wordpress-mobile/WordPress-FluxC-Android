package org.wordpress.android.fluxc.example.ui.products

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.databinding.FragmentWooAddonsBinding
import org.wordpress.android.fluxc.model.SiteModel
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
        private const val SELECTED_SITE_LOCAL_ID = "selected_site_local_id"

        @JvmStatic
        fun show(fragmentManager: FragmentManager, selectedSiteId: Int) =
                WooAddonsTestFragment().apply {
                    arguments = Bundle().apply {
                        this.putInt(SELECTED_SITE_LOCAL_ID, selectedSiteId)
                    }
                }.show(fragmentManager, null)
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
        val view = FragmentWooAddonsBinding.inflate(inflater, container, false).root

        addonsResult = view.findViewById(R.id.addons_result)
        return view
    }

    lateinit var selectedProduct: WCProductModel
    lateinit var addonsResult: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val selectedSiteRemoteId = requireArguments().getInt(SELECTED_SITE_LOCAL_ID)
        val selectedSite = siteStore.getSiteByLocalId(selectedSiteRemoteId)!!

        with(FragmentWooAddonsBinding.bind(view)) {
            addonsProductRemoteIdApply.setOnClickListener {
                val selectedProductRemoteId = addonsProductRemoteId.text.toString().toLong()
                lifecycleScope.launch {
                    selectedProduct = wcProductStore.getProductByRemoteId(selectedSite, selectedProductRemoteId) ?: run {
                        wcProductStore.fetchSingleProduct(FetchSingleProductPayload(selectedSite, selectedProductRemoteId))
                        wcProductStore.getProductByRemoteId(selectedSite, selectedProductRemoteId)!!
                    }

                    startObserving(selectedSite, selectedProduct)
                }
            }

            addonsFetchProduct.setOnClickListener {
                coroutineScope.launch {
                    wcProductStore.fetchSingleProduct(
                        FetchSingleProductPayload(
                            selectedSite,
                            selectedProduct.remoteProductId
                        )
                    )
                }
            }

            addonsFetchGlobal.setOnClickListener {
                coroutineScope.launch {
                    wcAddonsStore.fetchAllGlobalAddonsGroups(selectedSite)
                }
            }
        }
    }

    private fun startObserving(siteModel: SiteModel, productModel: WCProductModel) {
        coroutineScope.launch {
            wcAddonsStore.observeAllAddonsForProduct(
                    siteModel,
                    productModel
            ).collect {
                addonsResult.text = it.joinToString { addon ->
                    "\n- \"${addon.name}\" of type ${addon::class.simpleName}"
                }
            }
        }
    }
}
