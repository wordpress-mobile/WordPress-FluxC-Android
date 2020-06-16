package org.wordpress.android.fluxc.example.ui.gateways

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_gateways.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.model.gateways.WCGatewayModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCGatewayStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooGatewaysFragment : Fragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var gatewayStore: WCGatewayStore
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_woo_gateways, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetch_gateway.setOnClickListener {
            getFirstWCSite()?.let { site ->
                showSingleLineDialog(activity, "Enter the gateway ID:") { gatewayEditText ->
                    GlobalScope.launch(Dispatchers.Main) {
                        supervisorScope {
                            try {
                                val response = withContext(Dispatchers.Default) {
                                    gatewayStore.fetchGateway(site, gatewayEditText.text.toString())
                                }
                                printGateway(response)
                            } catch (e: Exception) {
                                prependToLog("Error: ${e.message}")
                            }
                        }
                    }
                }
            }
        }

        fetch_all_gateways.setOnClickListener {
            getFirstWCSite()?.let { site ->
                GlobalScope.launch(Dispatchers.Main) {
                    supervisorScope {
                        try {
                            val response = withContext(Dispatchers.Default) {
                                gatewayStore.fetchAllGateways(site)
                            }
                            response.error?.let {
                                prependToLog("${it.type}: ${it.message}")
                            }
                            response.model?.let {
                                prependToLog("Site has ${it.size} gateways")
                                it.forEach { gateway ->
                                    prependToLog("Gateway: $gateway")
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

    private fun printGateway(response: WooResult<WCGatewayModel>) {
        response.error?.let {
            prependToLog("${it.type}: ${it.message}")
        }
        response.model?.let { gateway ->
            prependToLog("Gateway: $gateway")
        }
    }

    private fun getFirstWCSite() = wooCommerceStore.getWooCommerceSites().getOrNull(0)
}
