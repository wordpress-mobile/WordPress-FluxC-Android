package org.wordpress.android.fluxc.example.ui.shippinglabels

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_shippinglabels.verify_address
import kotlinx.android.synthetic.main.fragment_woo_verify_address.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult.InvalidRequest
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult.InvalidAddress
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult.Valid
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress.Type.DESTINATION
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress.Type.ORIGIN
import org.wordpress.android.fluxc.store.WCShippingLabelStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooVerifyAddressFragment : Fragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore
    @Inject internal lateinit var wcShippingLabelStore: WCShippingLabelStore

    private lateinit var selectedSite: SiteModel

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    companion object {
        fun newInstance(
            site: SiteModel
        ) = WooVerifyAddressFragment().apply {
            this.selectedSite = site
        }
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_woo_verify_address, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        verify_address.setOnClickListener {
            if (address_country.getText().isBlank()) {
                prependToLog("Validation error: Country is required (2-letter acronym) ")
            } else {
                coroutineScope.launch {
                    val result = wcShippingLabelStore.verifyAddress(
                            selectedSite,
                            ShippingLabelAddress(
                                    address_company.getText(),
                                    address_name.getText(),
                                    address_phone.getText(),
                                    address_country.getText(),
                                    address_state.getText(),
                                    address_address1.getText(),
                                    address_address2.getText(),
                                    address_city.getText(),
                                    address_zip.getText()
                            ),
                            if (destination.isChecked) DESTINATION else ORIGIN
                    )
                    when {
                        result.isError -> {
                            prependToLog("${result.error.message}")
                        }
                        result.model is Valid -> {
                            prependToLog("${(result.model as Valid).suggestedAddress}")
                        }
                        result.model is InvalidAddress -> {
                            prependToLog("Address error: ${(result.model as InvalidAddress).message}")
                        }
                        result.model is InvalidRequest -> {
                            prependToLog("Request error: ${(result.model as InvalidRequest).message}")
                        }
                    }
                }
            }
        }
    }
}
