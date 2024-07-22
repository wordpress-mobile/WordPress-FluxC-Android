package org.wordpress.android.fluxc.example.ui.refunds

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.databinding.FragmentWooRefundsBinding
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.StoreSelectingFragment
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooRefundsFragment : StoreSelectingFragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var refundsStore: WCRefundStore
    @Inject internal lateinit var ordersStore: WCOrderStore
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentWooRefundsBinding.inflate(inflater, container, false).root

    @Suppress("LongMethod", "TooGenericExceptionCaught")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentWooRefundsBinding.bind(view)) {
            createFullRefund.setOnClickListener {
                selectedSite?.let { site ->
                    showSingleLineDialog(activity, "Enter the order ID:") { orderEditText ->
                        showSingleLineDialog(activity, "Enter the refund amount:") { amountEditText ->
                            showSingleLineDialog(activity, "Enter refund reason:") { reasonEditText ->
                                lifecycleScope.launch(Dispatchers.IO) {
                                    try {
                                        val response = refundsStore.createAmountRefund(
                                            site,
                                            orderEditText.text.toString().toLong(),
                                            amountEditText.text.toString().toBigDecimal(),
                                            reasonEditText.text.toString()
                                        )
                                        withContext(Dispatchers.Main) {
                                            printRefund(response)
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

            fetchRefund.setOnClickListener {
                selectedSite?.let { site ->
                    showSingleLineDialog(activity, "Enter the order ID:") { orderEditText ->
                        showSingleLineDialog(activity, "Enter the refund ID:") { refundEditText ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    val response = refundsStore.fetchRefund(
                                        site,
                                        orderEditText.text.toString().toLong(),
                                        refundEditText.text.toString().toLong()
                                    )
                                    withContext(Dispatchers.Main) {
                                        printRefund(response)
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

            fetchAllRefunds.setOnClickListener {
                selectedSite?.let { site ->
                    showSingleLineDialog(activity, "Enter the order ID:") { orderEditText ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val response = refundsStore.fetchAllRefunds(
                                    site,
                                    orderEditText.text.toString().toLong()
                                )
                                withContext(Dispatchers.Main) {
                                    response.error?.let {
                                        prependToLog("${it.type}: ${it.message}")
                                    }
                                    response.model?.let {
                                        prependToLog("Order ${orderEditText.text} has ${it.size} refunds")
                                        it.forEach { refund ->
                                            prependToLog("Refund: $refund")
                                        }
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

    private fun printRefund(response: WooResult<WCRefundModel>) {
        response.error?.let {
            prependToLog("${it.type}: ${it.message}")
        }
        response.model?.let { refund ->
            prependToLog("Refund: $refund")
        }
    }
}
