package org.wordpress.android.fluxc.example.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_address_edit_dialog.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.example.databinding.FragmentAddressEditDialogBinding
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.orders.AddressEditDialogFragment.EditTypeState.BILLING
import org.wordpress.android.fluxc.example.ui.orders.AddressEditDialogFragment.EditTypeState.SHIPPING
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderAddress
import org.wordpress.android.fluxc.store.OrderUpdateStore
import javax.inject.Inject

class AddressEditDialogFragment : DaggerFragment() {
    @Inject lateinit var orderUpdateStore: OrderUpdateStore

    enum class EditTypeState {
        SHIPPING, BILLING
    }

    private var currentAddressType = MutableStateFlow(SHIPPING)
    private lateinit var binding: FragmentAddressEditDialogBinding

    var selectedOrder = MutableStateFlow<WCOrderModel?>(null)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentAddressEditDialogBinding.inflate(inflater).apply {
        binding = this
        addressTypeSwitch.setOnCheckedChangeListener { _, isChecked ->
            currentAddressType.value = if (isChecked) BILLING else SHIPPING
        }
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CoroutineScope(Dispatchers.Main).launch {
            currentAddressType.combine(selectedOrder) { addressType, order ->
                when (addressType) {
                    SHIPPING -> {
                        binding.addressTypeSwitch.text = "Edit shipping address for order ${order?.remoteOrderId}"
                        binding.firstName.setText(order?.shippingFirstName)
                        binding.lastName.setText(order?.shippingLastName)
                        binding.company.setText(order?.shippingCompany)
                        binding.address1.setText(order?.shippingAddress1)
                        binding.address2.setText(order?.shippingAddress2)
                        binding.city.setText(order?.shippingCity)
                        binding.state.setText(order?.shippingState)
                        binding.postcode.setText(order?.shippingPostcode)
                        binding.phone.setText(order?.billingPhone)
                        binding.email.visibility = View.INVISIBLE
                    }
                    BILLING -> {
                        binding.addressTypeSwitch.text = "Edit billing address for order ${order?.remoteOrderId}"
                        binding.firstName.setText(order?.billingFirstName)
                        binding.lastName.setText(order?.billingLastName)
                        binding.company.setText(order?.billingCompany)
                        binding.address1.setText(order?.billingAddress1)
                        binding.address2.setText(order?.billingAddress2)
                        binding.city.setText(order?.billingCity)
                        binding.state.setText(order?.billingState)
                        binding.postcode.setText(order?.billingPostcode)
                        binding.phone.setText(order?.billingPhone)
                        binding.email.apply {
                            setText(order?.billingEmail)
                            visibility = View.VISIBLE
                        }
                    }
                }
            }.collect()
        }

        sendUpdate.setOnClickListener {
            selectedOrder.value?.let { order ->
                CoroutineScope(Dispatchers.IO).launch {
                    val newAddress = when (currentAddressType.value) {
                        SHIPPING -> OrderAddress.Shipping(
                                firstName = binding.firstName.text.toString(),
                                lastName = binding.lastName.text.toString(),
                                company = binding.company.text.toString(),
                                address1 = binding.address1.text.toString(),
                                address2 = binding.address2.text.toString(),
                                city = binding.city.text.toString(),
                                state = binding.state.text.toString(),
                                postcode = binding.postcode.text.toString(),
                                country = binding.country.text.toString(),
                                phone = binding.phone.text.toString()
                        )
                        BILLING -> OrderAddress.Billing(
                                firstName = binding.firstName.text.toString(),
                                lastName = binding.lastName.text.toString(),
                                company = binding.company.text.toString(),
                                address1 = binding.address1.text.toString(),
                                address2 = binding.address2.text.toString(),
                                city = binding.city.text.toString(),
                                state = binding.state.text.toString(),
                                postcode = binding.postcode.text.toString(),
                                country = binding.country.text.toString(),
                                phone = binding.phone.text.toString(),
                                email = binding.email.text.toString()
                        )
                    }

                    orderUpdateStore.updateOrderAddress(
                            orderLocalId = LocalId(order.id),
                            newAddress = newAddress
                    ).collect {
                        CoroutineScope(Dispatchers.Main).launch {
                            prependToLog("${it::class.simpleName} - Error: ${it.event.error?.message}")
                        }
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(order: WCOrderModel) = AddressEditDialogFragment().also { fragment ->
            CoroutineScope(Dispatchers.IO).launch {
                fragment.selectedOrder.value = order
            }
        }
    }
}
