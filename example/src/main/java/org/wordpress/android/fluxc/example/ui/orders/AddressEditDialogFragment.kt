package org.wordpress.android.fluxc.example.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_address_edit_dialog.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.example.databinding.FragmentAddressEditDialogBinding
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.orders.AddressEditDialogFragment.AddressType.BILLING
import org.wordpress.android.fluxc.example.ui.orders.AddressEditDialogFragment.AddressType.SHIPPING
import org.wordpress.android.fluxc.example.ui.orders.AddressEditDialogFragment.Mode.Add
import org.wordpress.android.fluxc.example.ui.orders.AddressEditDialogFragment.Mode.Edit
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderAddress
import org.wordpress.android.fluxc.model.order.OrderAddress.Billing
import org.wordpress.android.fluxc.model.order.OrderAddress.Shipping
import org.wordpress.android.fluxc.store.OrderUpdateStore
import javax.inject.Inject

class AddressEditDialogFragment : DaggerFragment() {
    companion object {
        // These instantiations won't work with screen rotation or process death scenarios.
        // They are just for the sake of simplification of the example
        @JvmStatic
        fun newInstanceForEditing(order: WCOrderModel): AddressEditDialogFragment =
                AddressEditDialogFragment().apply {
                    this.selectedOrder = order
                    this.mode = Edit
                }

        @JvmStatic
        fun newInstanceForCreation(addressType: AddressType, listener: (OrderAddress) -> Unit):
                AddressEditDialogFragment =
                AddressEditDialogFragment().apply {
                    this.selectedOrder = WCOrderModel()
                    this.mode = Add
                    this.addressListener = listener
                    this.currentAddressType.value = addressType
                }
    }

    @Inject lateinit var orderUpdateStore: OrderUpdateStore

    enum class AddressType {
        SHIPPING, BILLING
    }
    enum class Mode {
        Edit, Add
    }

    private lateinit var mode: Mode
    private var currentAddressType = MutableStateFlow(SHIPPING)

    private lateinit var selectedOrder: WCOrderModel
    var originalBillingEmail = ""

    private var addressListener: ((OrderAddress) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentAddressEditDialogBinding.inflate(inflater).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentAddressEditDialogBinding.bind(view)

        if (mode == Add) {
            binding.addressTypeSwitch.visibility = View.GONE
            binding.sendBothAddressesUpdate.visibility = View.GONE
            binding.sendUpdate.text = "Save"
            binding.addressTypeTitle.visibility = View.VISIBLE
            binding.addressTypeTitle.text = if (currentAddressType.value == BILLING) {
                "Enter a billing address"
            } else {
                "Enter a shipping address"
            }
        }

        binding.addressTypeSwitch.setOnCheckedChangeListener { _, isChecked ->
            currentAddressType.value = if (isChecked) BILLING else SHIPPING
        }

        viewLifecycleOwner.lifecycleScope.launch {
            currentAddressType.collect { addressType ->
                originalBillingEmail = selectedOrder.billingEmail
                when (addressType) {
                    SHIPPING -> {
                        binding.addressTypeSwitch.text =
                                "Edit shipping address for order ${selectedOrder.remoteOrderId}"
                        binding.firstName.setText(selectedOrder.shippingFirstName)
                        binding.lastName.setText(selectedOrder.shippingLastName)
                        binding.company.setText(selectedOrder.shippingCompany)
                        binding.address1.setText(selectedOrder.shippingAddress1)
                        binding.address2.setText(selectedOrder.shippingAddress2)
                        binding.city.setText(selectedOrder.shippingCity)
                        binding.state.setText(selectedOrder.shippingState)
                        binding.postcode.setText(selectedOrder.shippingPostcode)
                        binding.phone.setText(selectedOrder.shippingPhone)
                        binding.email.visibility = View.INVISIBLE
                    }
                    BILLING -> {
                        binding.addressTypeSwitch.text = "Edit billing address for order ${selectedOrder.remoteOrderId}"
                        binding.firstName.setText(selectedOrder.billingFirstName)
                        binding.lastName.setText(selectedOrder.billingLastName)
                        binding.company.setText(selectedOrder.billingCompany)
                        binding.address1.setText(selectedOrder.billingAddress1)
                        binding.address2.setText(selectedOrder.billingAddress2)
                        binding.city.setText(selectedOrder.billingCity)
                        binding.state.setText(selectedOrder.billingState)
                        binding.postcode.setText(selectedOrder.billingPostcode)
                        binding.phone.setText(selectedOrder.billingPhone)
                        binding.email.apply {
                            setText(selectedOrder.billingEmail)
                            visibility = View.VISIBLE
                        }
                    }
                }
            }
        }

        sendUpdate.setOnClickListener {
            val newAddress = when (currentAddressType.value) {
                SHIPPING -> generateShippingAddressModel(binding)
                BILLING -> generateBillingAddressModel(binding)
            }

            when (mode) {
                Edit -> {
                    lifecycleScope.launch {
                        orderUpdateStore.updateOrderAddress(
                                orderLocalId = LocalId(selectedOrder.id),
                                newAddress = newAddress
                        ).collect {
                            prependToLog("${it::class.simpleName} - Error: ${it.event.error?.message}")
                        }
                    }
                }
                Add -> {
                    parentFragmentManager.popBackStack()
                    addressListener?.invoke(newAddress)
                }
            }
        }

        sendBothAddressesUpdate.setOnClickListener {
            lifecycleScope.launch {
                orderUpdateStore.updateBothOrderAddresses(
                        orderLocalId = LocalId(selectedOrder.id),
                        shippingAddress = generateShippingAddressModel(binding),
                        billingAddress = generateBillingAddressModel(binding)
                ).collect {
                    prependToLog("${it::class.simpleName} - Error: ${it.event.error?.message}")
                }
            }
        }
    }

    private fun generateBillingAddressModel(binding: FragmentAddressEditDialogBinding) = Billing(
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
                    .takeIf { it.isNotEmpty() }
                    ?: originalBillingEmail
    )

    private fun generateShippingAddressModel(binding: FragmentAddressEditDialogBinding) = Shipping(
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
}
