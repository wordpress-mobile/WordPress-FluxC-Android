package org.wordpress.android.fluxc.example.ui.refunds

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_refunds.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.R.layout
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooRefundsFragment : Fragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var refundsStore: WCRefundStore
    @Inject internal lateinit var ordersStore: WCOrderStore
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(layout.fragment_woo_refunds, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        create_full_refund.setOnClickListener {
            getFirstWCSite()?.let { site ->
                showSingleLineDialog(activity, "Enter the order ID:") { orderEditText ->
                    showSingleLineDialog(activity, "Enter the refund amount:") { amountEditText ->
                        showSingleLineDialog(activity, "Enter refund reason:") { reasonEditText ->
                            GlobalScope.launch(Dispatchers.Main) {
                                try {
                                    val response = withContext(Dispatchers.Default) {
                                        refundsStore.createAmountRefund(
                                                site,
                                                orderEditText.text.toString().toLong(),
                                                amountEditText.text.toString().toBigDecimal(),
                                                reasonEditText.text.toString()
                                        )
                                    }
                                    printRefund(response)
                                } catch (e: Exception) {
                                    prependToLog("Error: ${e.message}")
                                }
                            }
                        }
                    }
                }
            }
        }

        fetch_refund.setOnClickListener {
            getFirstWCSite()?.let { site ->
                showSingleLineDialog(activity, "Enter the order ID:") { orderEditText ->
                    showSingleLineDialog(activity, "Enter the refund ID:") { refundEditText ->
                        GlobalScope.launch(Dispatchers.Main) {
                            supervisorScope {
                                try {
                                    val response = withContext(Dispatchers.Default) {
                                        refundsStore.fetchRefund(
                                                site,
                                                orderEditText.text.toString().toLong(),
                                                refundEditText.text.toString().toLong()
                                        )
                                    }
                                    printRefund(response)
                                } catch (e: Exception) {
                                    prependToLog("Error: ${e.message}")
                                }
                            }
                        }
                    }
                }
            }
        }

        fetch_all_refunds.setOnClickListener {
            getFirstWCSite()?.let { site ->
                showSingleLineDialog(activity, "Enter the order ID:") { orderEditText ->
                    GlobalScope.launch(Dispatchers.Main) {
                        supervisorScope {
                            try {
                                val response = withContext(Dispatchers.Default) {
                                    refundsStore.fetchAllRefunds(
                                            site,
                                            orderEditText.text.toString().toLong()
                                    )
                                }
                                response.error?.let {
                                    prependToLog("${it.type}: ${it.message}")
                                }
                                response.model?.let {
                                    prependToLog("Order ${orderEditText.text} has ${it.size} refunds")
                                    it.forEach { refund ->
                                        prependToLog("Refund: $refund")
                                    }
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

    private fun printRefund(response: WooResult<WCRefundModel>) {
        response.error?.let {
            prependToLog("${it.type}: ${it.message}")
        }
        response.model?.let { refund ->
            prependToLog("Refund: $refund")
        }
    }

    private fun getFirstWCSite() = wooCommerceStore.getWooCommerceSites().getOrNull(0)
}
