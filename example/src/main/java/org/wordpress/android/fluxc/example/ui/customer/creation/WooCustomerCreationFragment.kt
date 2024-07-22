package org.wordpress.android.fluxc.example.ui.customer.creation

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.databinding.FragmentWooCustomerCreationBinding
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.store.WCCustomerStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooCustomerCreationFragment : Fragment(R.layout.fragment_woo_customer_creation) {
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore
    @Inject internal lateinit var wcCustomerStore: WCCustomerStore

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val site by lazy {
        wooCommerceStore.getWooCommerceSites().find { it.id == requireArguments().getInt(KEY_SELECTED_SITE_ID) }!!
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentWooCustomerCreationBinding.bind(view)) {
            btnCustomerCreate.setOnClickListener {
                coroutineScope.launch {
                    val customerModel = buildModel()
                    prependToLog("Creating a customer $customerModel")
                    withContext(Dispatchers.Default) {
                        wcCustomerStore.createCustomer(site, customerModel)
                    }.run {
                        error?.let { prependToLog("${it.type}: ${it.message}") }
                        if (model != null) {
                            prependToLog("Created customer: $model")
                        }
                    }
                }
            }
        }
    }

    private fun FragmentWooCustomerCreationBinding.buildModel() = WCCustomerModel().apply {
        firstName = etCustomerFirstName.text.toString()
        lastName = etCustomerLastName.text.toString()
        email = etCustomerEmail.text.toString()
        username = etCustomerUsername.text.toString()

        billingAddress1 = "969 Market"
        billingCity = "San Francisco"
        billingCompany = ""
        billingCountry = "US"
        billingEmail = email
        billingFirstName = firstName
        billingLastName = lastName
        billingPhone = "(555) 555-5555"
        billingPostcode = "94103"
        billingState = "CA"

        shippingAddress1 = "969 Market"
        shippingCity = "San Francisco"
        shippingCompany = ""
        shippingCountry = "US"
        shippingFirstName = firstName
        shippingLastName = lastName
        shippingPostcode = "94103"
        shippingState = "CA"
    }

    companion object {
        fun newInstance(siteId: Int) = WooCustomerCreationFragment().apply {
            arguments = Bundle().apply { putInt(KEY_SELECTED_SITE_ID, siteId) }
        }
    }
}

private const val KEY_SELECTED_SITE_ID = "selected_site_id"
