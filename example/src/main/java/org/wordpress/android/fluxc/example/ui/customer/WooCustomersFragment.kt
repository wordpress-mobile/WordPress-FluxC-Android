package org.wordpress.android.fluxc.example.ui.customer

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_customer.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.StoreSelectorDialog.Listener
import org.wordpress.android.fluxc.example.ui.common.showStoreSelectorDialog
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.example.utils.toggleSiteDependentButtons
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCCustomerStore
import javax.inject.Inject

class WooCustomersFragment : Fragment() {
    @Inject internal lateinit var wcCustomerStore: WCCustomerStore

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private var selectedPos: Int = -1
    private var selectedSite: SiteModel? = null

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_woo_customer, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnCustomerSelectSite.setOnClickListener { selectStore() }
        btnPrintCutomer.setOnClickListener { printCustomerById() }
        btnFetchCustomerList.setOnClickListener {
        }
    }

    private fun printCustomerById() {
        val site = selectedSite!!
        showSingleLineDialog(
                activity, "Enter the remote customer Id:", isNumeric = true
        ) { remoteIdEditText ->
            if (remoteIdEditText.text.isEmpty()) {
                prependToLog("Remote Id is null so doing nothing")
                return@showSingleLineDialog
            }

            val remoteId = remoteIdEditText.text.toString().toLong()
            prependToLog("Submitting request to print customer with id: $remoteId")

            coroutineScope.launch {
                withContext(Dispatchers.Default) {
                    wcCustomerStore.fetchSingleCustomer(site, remoteId)
                }.run {
                    error?.let { prependToLog("${it.type}: ${it.message}") }
                    if (model != null) {
                        prependToLog("Customer data: ${this.model}")
                    } else {
                        prependToLog("Customer with id $remoteId is missing")
                    }
                }
            }
        }
    }

    private fun selectStore() {
        showStoreSelectorDialog(selectedPos, object : Listener {
            override fun onSiteSelected(site: SiteModel, pos: Int) {
                selectedSite = site
                selectedPos = pos
                llButtonContainer.toggleSiteDependentButtons(true)
                tvCustomerSelectedSite.text = site.name ?: site.displayName
            }
        })
    }
}
