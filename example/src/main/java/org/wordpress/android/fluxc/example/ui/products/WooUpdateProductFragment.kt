package org.wordpress.android.fluxc.example.ui.products

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_update_product.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.R.layout
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnProductUpdated
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductPayload
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooUpdateProductFragment : Fragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wcProductStore: WCProductStore
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    private var selectedSitePosition: Int = -1
    private var selectedProductModel: WCProductModel? = null

    companion object {
        const val ARG_SELECTED_SITE_POS = "ARG_SELECTED_SITE_POS"

        fun newInstance(selectedSitePosition: Int): WooUpdateProductFragment {
            val fragment = WooUpdateProductFragment()
            val args = Bundle()
            args.putInt(ARG_SELECTED_SITE_POS, selectedSitePosition)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            selectedSitePosition = it.getInt(ARG_SELECTED_SITE_POS, 0)
        }
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(layout.fragment_woo_update_product, container, false)

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        super.onStop()
        dispatcher.unregister(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        product_enter_product_id.setOnClickListener {
            showSingleLineDialog(activity, "Enter the remoteProductId of product to fetch:") { editText ->
                val selectedRemoteProductId = editText.text.toString().toLongOrNull()
                selectedRemoteProductId?.let { id ->
                    updateSelectedProductId(id)
                    enableProductDependentButtons()
                    product_entered_product_id.text = editText.text.toString()
                } ?: prependToLog("No valid remoteProductId defined...doing nothing")
            }
        }

        update_product_description.setOnClickListener {
            getWCSite()?.let { site ->
                showSingleLineDialog(activity, "Enter product description:") { editText ->
                    selectedProductModel?.let { productModel ->
                        productModel.description = editText.text.toString()
                        val payload = UpdateProductPayload(site, productModel)
                        dispatcher.dispatch(WCProductActionBuilder.newUpdateProductAction(payload))
                    } ?: prependToLog("No valid remoteProductId defined...doing nothing")
                }
            }
        }
    }

    private fun updateSelectedProductId(remoteProductId: Long) {
        getWCSite()?.let {
            selectedProductModel = wcProductStore.getProductByRemoteId(it, remoteProductId)
                    ?: WCProductModel().apply { this.remoteProductId = remoteProductId }
        } ?: prependToLog("No valid site found...doing nothing")
    }

    private fun getWCSite() = wooCommerceStore.getWooCommerceSites().getOrNull(selectedSitePosition)

    private fun enableProductDependentButtons() {
        for (i in 0 until buttonContainer.childCount) {
            val child = buttonContainer.getChildAt(i)
            if (child is Button) {
                child.isEnabled = true
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductUpdated(event: OnProductUpdated) {
        if (event.isError) {
            prependToLog("Error from " + event.causeOfChange + " - error: " + event.error.type)
            return
        }
        prependToLog("Product updated ${event.rowsAffected}")
    }
}
