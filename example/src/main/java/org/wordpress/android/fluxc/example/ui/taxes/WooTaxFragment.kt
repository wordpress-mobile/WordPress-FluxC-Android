package org.wordpress.android.fluxc.example.ui.taxes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_woo_taxes.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.StoreSelectingFragment
import org.wordpress.android.fluxc.store.WCTaxStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooTaxFragment : StoreSelectingFragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wcTaxStore: WCTaxStore
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_woo_taxes, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetch_tax_class.setOnClickListener {
            selectedSite?.let { site ->
                prependToLog("Submitting request to fetch tax classes for site ${site.id}")
                GlobalScope.launch(Dispatchers.Main) {
                    supervisorScope {
                        try {
                            val response = withContext(Dispatchers.Default) {
                                wcTaxStore.fetchTaxClassList(site)
                            }
                            response.error?.let {
                                prependToLog("${it.type}: ${it.message}")
                            }
                            response.model?.let {
                                prependToLog("Site has ${it.size} tax classes")
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
