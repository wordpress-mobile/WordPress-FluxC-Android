package org.wordpress.android.fluxc.example.ui.shippinglabels

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.databinding.FragmentWooVerifyAddressBinding
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult.InvalidAddress
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult.InvalidRequest
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentWooVerifyAddressBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentWooVerifyAddressBinding.bind(view)) {
            verifyAddress.setOnClickListener {
                if (addressCountry.getText().isBlank()) {
                    prependToLog("Validation error: Country is required (2-letter acronym) ")
                } else {
                    coroutineScope.launch {
                        val result = wcShippingLabelStore.verifyAddress(
                            selectedSite,
                            ShippingLabelAddress(
                                addressCompany.getText(),
                                addressName.getText(),
                                addressPhone.getText(),
                                addressCountry.getText(),
                                addressState.getText(),
                                addressAddress1.getText(),
                                addressAddress2.getText(),
                                addressCity.getText(),
                                addressZip.getText()
                            ),
                            if (destination.isChecked) DESTINATION else ORIGIN
                        )
                        when {
                            result.isError -> {
                                prependToLog("${result.error.message}")
                            }
                            result.model is Valid -> {
                                val model = result.model as Valid
                                prependToLog("Suggested address: ${model.suggestedAddress}\n" +
                                    "Trivial Change: ${model.isTrivialNormalization}")
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
}
