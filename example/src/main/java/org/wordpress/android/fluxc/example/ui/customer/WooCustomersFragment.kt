package org.wordpress.android.fluxc.example.ui.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_woo_customer.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.replaceFragment
import org.wordpress.android.fluxc.example.ui.customer.creation.WooCustomerCreationFragment
import org.wordpress.android.fluxc.example.ui.customer.search.WooCustomersSearchBuilderFragment
import org.wordpress.android.fluxc.example.ui.StoreSelectingFragment
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.store.WCCustomerStore
import javax.inject.Inject

class WooCustomersFragment : StoreSelectingFragment() {
    @Inject internal lateinit var wcCustomerStore: WCCustomerStore

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_woo_customer, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnPrintCutomer.setOnClickListener { printCustomerById() }
        btnFetchCustomerList.setOnClickListener {
            replaceFragment(WooCustomersSearchBuilderFragment.newInstance(selectedSite!!.id))
        }
        btnCreateCustomer.setOnClickListener {
            replaceFragment(WooCustomerCreationFragment.newInstance(selectedSite!!.id))
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
}
