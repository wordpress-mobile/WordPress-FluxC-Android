package org.wordpress.android.fluxc.example.ui.shippinglabels

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_shippinglabels.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.StoreSelectorDialog
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCShippingLabelStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooShippingLabelFragment : Fragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore
    @Inject internal lateinit var wcShippingLabelStore: WCShippingLabelStore

    private var selectedPos: Int = -1
    private var selectedSite: SiteModel? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_woo_shippinglabels, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        shipping_labels_select_site.setOnClickListener {
            showSiteSelectorDialog(selectedPos, object : StoreSelectorDialog.Listener {
                override fun onSiteSelected(site: SiteModel, pos: Int) {
                    selectedSite = site
                    selectedPos = pos
                    toggleSiteDependentButtons(true)
                    shipping_labels_selected_site.text = site.name ?: site.displayName
                }
            })
        }

        fetch_shipping_labels.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(activity, "Enter the order ID:") { orderEditText ->
                    val orderId = orderEditText.text.toString().toLong()
                    prependToLog("Submitting request to fetch shipping labels for order $orderId")
                    coroutineScope.launch {
                        try {
                            val response = withContext(Dispatchers.Default) {
                                wcShippingLabelStore.fetchShippingLabelsForOrder(site, orderId)
                            }
                            response.error?.let {
                                prependToLog("${it.type}: ${it.message}")
                            }
                            response.model?.let {
                                prependToLog("Order $orderId has ${it.size} shipping labels")
                            }
                        } catch (e: Exception) {
                            prependToLog("Error: ${e.message}")
                        }
                    }
                }
            }
        }

        refund_shipping_label.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(activity, "Enter the order ID:") { orderEditText ->
                    if (orderEditText.text.isEmpty()) {
                        prependToLog("OrderId is null so doing nothing")
                        return@showSingleLineDialog
                    }

                    val orderId = orderEditText.text.toString().toLong()
                    showSingleLineDialog(activity, "Enter the remote shipping Label Id:") { remoteIdEditText ->
                        if (remoteIdEditText.text.isEmpty()) {
                            prependToLog("Remote Id is null so doing nothing")
                            return@showSingleLineDialog
                        }

                        val remoteId = remoteIdEditText.text.toString().toLong()
                        prependToLog("Submitting request to refund shipping label for order $orderId with id $remoteId")

                        coroutineScope.launch {
                            try {
                                val response = withContext(Dispatchers.Default) {
                                    wcShippingLabelStore.refundShippingLabelForOrder(site, orderId, remoteId)
                                }
                                response.error?.let {
                                    prependToLog("${it.type}: ${it.message}")
                                }
                                response.model?.let {
                                    prependToLog(
                                            "Refund for $orderId with shipping label $remoteId is ${response.model}"
                                    )
                                }
                            } catch (e: Exception) {
                                prependToLog("Error: ${e.message}")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showSiteSelectorDialog(selectedPos: Int, listener: StoreSelectorDialog.Listener) {
        fragmentManager?.let { fm ->
            val dialog = StoreSelectorDialog.newInstance(listener, selectedPos)
            dialog.show(fm, "StoreSelectorDialog")
        }
    }

    private fun toggleSiteDependentButtons(enabled: Boolean) {
        for (i in 0 until buttonContainer.childCount) {
            val child = buttonContainer.getChildAt(i)
            if (child is Button) {
                child.isEnabled = enabled
            }
        }
    }
}
