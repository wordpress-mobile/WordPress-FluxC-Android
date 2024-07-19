package org.wordpress.android.fluxc.example

import android.R.layout
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import org.wordpress.android.fluxc.example.databinding.DialogWcAddOrderShipmentTrackingBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.utils.DateUtils
import java.util.Calendar
import java.util.Date

class WCAddOrderShipmentTrackingDialog : DialogFragment() {
    companion object {
        private const val WC_ADD_ORDER_SHIPMENT_TRACKING_REQUEST_CODE = 200

        @JvmStatic
        fun newInstance(
            fragment: Fragment,
            site: SiteModel,
            order: OrderEntity,
            providers: List<String>
        ) = WCAddOrderShipmentTrackingDialog().apply {
            setTargetFragment(fragment, WC_ADD_ORDER_SHIPMENT_TRACKING_REQUEST_CODE)
            this.site = site
            this.order = order
            this.providers = providers
        }
    }

    interface Listener {
        fun onTrackingSubmitted(
            site: SiteModel,
            order: OrderEntity,
            tracking: WCOrderShipmentTrackingModel,
            isCustomProvider: Boolean
        )
    }

    lateinit var listener: Listener
    lateinit var site: SiteModel
    lateinit var order: OrderEntity
    lateinit var providers: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listener = targetFragment as Listener
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = DialogWcAddOrderShipmentTrackingBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val allProviders = mutableListOf("Custom")
        allProviders.addAll(1, providers)

        with(DialogWcAddOrderShipmentTrackingBinding.bind(view)) {
            trackingCboProvider.adapter = ArrayAdapter<String>(
                requireActivity(), layout.simple_dropdown_item_1line, allProviders)
            trackingCboProvider.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    trackingCboProvider.setSelection(0)
                }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    parent?.getItemAtPosition(position)?.let { provider ->
                        if (provider == "Custom") {
                            trackingProviderName.visibility = View.VISIBLE
                            trackingLinkName.visibility = View.VISIBLE
                            trackingLblTrackingLink.visibility = View.VISIBLE
                            trackingLblProviderName.visibility = View.VISIBLE
                        } else {
                            trackingProviderName.visibility = View.GONE
                            trackingLinkName.visibility = View.GONE
                            trackingLblTrackingLink.visibility = View.GONE
                            trackingLblProviderName.visibility = View.GONE
                        }
                    }
                }
            }

            val startDate = DateUtils.getCurrentDateString()
            trackingDateShipped.text = startDate
            trackingDateShipped.setOnClickListener {
                displayDialog(
                    DateUtils.getCalendarInstance(trackingDateShipped.text.toString()), trackingDateShipped)
            }

            trackingOk.setOnClickListener {
                val isCustomSelected = trackingCboProvider.selectedItem == "Custom"
                val tracking = WCOrderShipmentTrackingModel().apply {
                    if (isCustomSelected) {
                        this.trackingProvider = trackingProviderName.text.toString()
                        this.trackingLink = trackingLinkName.text.toString()
                    } else {
                        this.trackingProvider = trackingCboProvider.selectedItem.toString()
                    }
                    this.dateShipped = trackingDateShipped.text.toString()
                    this.trackingNumber = trackingNumberName.text.toString()
                }
                listener.onTrackingSubmitted(site, order, tracking, isCustomProvider = isCustomSelected)
                dismiss()
            }

            trackingCancel.setOnClickListener {
                dismiss()
            }
        }
    }

    private fun displayDialog(
        calendar: Calendar,
        button: Button
    ) {
        val datePicker = DatePickerDialog(requireActivity(),
                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                    button.text = DateUtils.getFormattedDateString(year, month, dayOfMonth)
                }, calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DATE))
        datePicker.datePicker.maxDate = Date().time
        datePicker.show()
    }
}
