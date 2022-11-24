package org.wordpress.android.fluxc.example.ui.storecreation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_woo_store_creation.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.StoreSelectingFragment
import org.wordpress.android.fluxc.network.rest.wpcom.wc.storecreation.ShoppingCartStore
import javax.inject.Inject

class WooStoreCreationFragment : StoreSelectingFragment() {
    @Inject internal lateinit var shoppingCartStore: ShoppingCartStore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_woo_store_creation, container, false)

    @Suppress("LongMethod", "ComplexMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnAddPlanToCart.setOnClickListener {
            selectedSite?.let { site ->
                prependToLog("Adding eCommerce plan to a shopping cart for site ${site.id}")
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val response = shoppingCartStore.addWooCommercePlanToCart(site.siteId)
                        withContext(Dispatchers.Main) {
                            response.error?.let {
                                prependToLog("${it.type}: ${it.message}")
                            }
                            response.model?.let {
                                prependToLog("Shopping cart content: $it")
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            prependToLog("Error: ${e.message}")
                        }
                    }
                }
            }
        }
    }
}
