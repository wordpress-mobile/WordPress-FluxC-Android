package org.wordpress.android.fluxc.example.ui.storecreation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.example.databinding.FragmentWooStoreCreationBinding
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.StoreSelectingFragment
import org.wordpress.android.fluxc.network.rest.wpcom.wc.storecreation.ShoppingCartRestClient.ShoppingCart.CartProduct
import org.wordpress.android.fluxc.network.rest.wpcom.wc.storecreation.ShoppingCartStore
import javax.inject.Inject

class WooStoreCreationFragment : StoreSelectingFragment() {
    @Inject internal lateinit var shoppingCartStore: ShoppingCartStore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentWooStoreCreationBinding.inflate(inflater, container, false).root

    @Suppress("LongMethod", "ComplexMethod", "TooGenericExceptionCaught")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentWooStoreCreationBinding.bind(view)) {
            btnAddPlanToCart.setOnClickListener {
                selectedSite?.let { site ->
                    prependToLog("Adding eCommerce plan to a shopping cart for site ${site.id}")
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val eCommerceProduct = CartProduct(
                                productId = 1021,
                                extra = mapOf(
                                    "context" to "signup",
                                    "signup_flow" to "ecommerce-monthly"
                                )
                            )
                            val response = shoppingCartStore.addProductToCart(
                                site.siteId,
                                eCommerceProduct
                            )
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
}
