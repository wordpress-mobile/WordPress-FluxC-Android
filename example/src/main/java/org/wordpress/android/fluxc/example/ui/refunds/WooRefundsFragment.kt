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
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.R.layout
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.model.refunds.RefundModel
import org.wordpress.android.fluxc.store.RefundsStore
import org.wordpress.android.fluxc.store.RefundsStore.RefundResult
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooRefundsFragment : Fragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var refundsStore: RefundsStore
    @Inject internal lateinit var ordersStore: WCOrderStore
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(layout.fragment_woo_refunds, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        create_full_refund.setOnClickListener {
            getFirstWCSite()?.let { site ->
                GlobalScope.launch {
                    val response = refundsStore.createRefund(
                            site,
                            order_number.text.toString().toLong(),
                            refund_amount.text.toString().toBigDecimal()
                    )
                    withContext(Dispatchers.Main) {
                        response.error?.original?.let {
                            prependToLog(it.name)
                        }
                        response.model?.let { refund ->
                            prependToLog("Refund: $refund")
                        }
                    }
                }
            }
        }

        fetch_refund.setOnClickListener {
            getFirstWCSite()?.let { site ->
                GlobalScope.launch {
                    val response = refundsStore.fetchRefund(
                            site,
                            order_number.text.toString().toLong(),
                            refund_number.text.toString().toLong()
                    )
                    printRefund(response)
                }
            }
        }


        fetch_all_refunds.setOnClickListener {
            getFirstWCSite()?.let { site ->
                GlobalScope.launch {
                    val response = refundsStore.fetchAllRefunds(
                            site,
                            order_number.text.toString().toLong()
                    )
                    withContext(Dispatchers.Main) {
                        response.error?.original?.let {
                            prependToLog(it.name)
                        }
                        response.model?.forEach { refund ->
                            prependToLog("Refund: $refund")
                        }
                    }
                }
            }
        }
    }

    private suspend fun printRefund(response: RefundResult<RefundModel>) {
        withContext(Dispatchers.Main) {
            response.error?.original?.let {
                prependToLog(it.name)
            }
            response.model?.let { refund ->
                prependToLog("Refund: $refund")
            }
        }
    }

    private fun getFirstWCSite() = wooCommerceStore.getWooCommerceSites().getOrNull(0)
}
