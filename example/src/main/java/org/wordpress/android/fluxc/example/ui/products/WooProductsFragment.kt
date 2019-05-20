package org.wordpress.android.fluxc.example.ui.products

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_products.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCT_VARIATIONS
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_SINGLE_PRODUCT
import org.wordpress.android.fluxc.example.R.layout
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductVariationsPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooProductsFragment : Fragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wcProductStore: WCProductStore
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    private var pendingFetchSingleProductRemoteId: Long? = null

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(layout.fragment_woo_products, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetch_single_product.setOnClickListener {
            getFirstWCSite()?.let { site ->
                showSingleLineDialog(activity, "Enter the remoteProductId of product to fetch:") { editText ->
                    pendingFetchSingleProductRemoteId = editText.text.toString().toLongOrNull()
                    pendingFetchSingleProductRemoteId?.let { id ->
                        prependToLog("Submitting request to fetch product by remoteProductID $id")
                        val payload = FetchSingleProductPayload(site, id)
                        dispatcher.dispatch(WCProductActionBuilder.newFetchSingleProductAction(payload))
                    } ?: prependToLog("No valid remoteOrderId defined...doing nothing")
                }
            }
        }

        fetch_product_variations.setOnClickListener {
            getFirstWCSite()?.let { site ->
                showSingleLineDialog(
                        activity,
                        "Enter the remoteProductId of product to fetch variations:"
                ) { editText ->
                    val remoteProductId = editText.text.toString().toLongOrNull()
                    remoteProductId?.let { id ->
                        prependToLog("Submitting request to fetch product variations by remoteProductID $id")
                        val payload = FetchProductVariationsPayload(site, id)
                        dispatcher.dispatch(WCProductActionBuilder.newFetchProductVariationsAction(payload))
                    } ?: prependToLog("No valid remoteProductId defined...doing nothing")
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        super.onStop()
        dispatcher.unregister(this)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductChanged(event: OnProductChanged) {
        if (event.isError) {
            prependToLog("Error from " + event.causeOfChange + " - error: " + event.error.type)
            return
        }

        getFirstWCSite()?.let { site ->
            when (event.causeOfChange) {
                FETCH_SINGLE_PRODUCT -> {
                    pendingFetchSingleProductRemoteId?.let { remoteId ->
                        pendingFetchSingleProductRemoteId = null
                        val product = wcProductStore.getProductByRemoteId(site, remoteId)
                        product?.let {
                            prependToLog("Single product fetched successfully! ${it.name}")
                        } ?: prependToLog("WARNING: Fetched product not found in the local database!")
                    }
                }
                FETCH_PRODUCT_VARIATIONS -> {
                    prependToLog("Fetched ${event.rowsAffected} product variations")
                }
                else -> prependToLog("Product store was updated from a " + event.causeOfChange)
            }
        }
    }

    private fun getFirstWCSite() = wooCommerceStore.getWooCommerceSites().getOrNull(0)
}
