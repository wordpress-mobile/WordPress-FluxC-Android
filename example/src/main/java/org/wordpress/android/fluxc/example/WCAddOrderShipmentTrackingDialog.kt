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
import kotlinx.android.synthetic.main.dialog_wc_add_order_shipment_tracking.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.utils.DateUtils
import java.util.Calendar
import java.util.Date

class WCAddOrderShipmentTrackingDialog : DialogFragment() {
    companion object {
        @JvmStatic
        fun newInstance(
            fragment: Fragment,
            site: SiteModel,
            order: WCOrderModel,
            providers: List<String>
        ) = WCAddOrderShipmentTrackingDialog().apply {
            setTargetFragment(fragment, 200)
            this.site = site
            this.order = order
            this.providers = providers
        }
    }

    interface Listener {
        fun onTrackingSubmitted(
            site: SiteModel,
            order: WCOrderModel,
            tracking: WCOrderShipmentTrackingModel,
            isCustomProvider: Boolean
        )
    }

    lateinit var listener: Listener
    lateinit var site: SiteModel
    lateinit var order: WCOrderModel
    lateinit var providers: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listener = targetFragment as Listener
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_wc_add_order_shipment_tracking, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val allProviders = mutableListOf("Custom")
        allProviders.addAll(1, providers)

        tracking_cboProvider.adapter = ArrayAdapter<String>(
                requireActivity(), layout.simple_dropdown_item_1line, allProviders)
        tracking_cboProvider.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                tracking_cboProvider.setSelection(0)
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                parent?.getItemAtPosition(position)?.let { provider ->
                    if (provider == "Custom") {
                        tracking_providerName.visibility = View.VISIBLE
                        tracking_link.visibility = View.VISIBLE
                        tracking_lblTrackingLink.visibility = View.VISIBLE
                        tracking_lblProviderName.visibility = View.VISIBLE
                    } else {
                        tracking_providerName.visibility = View.GONE
                        tracking_link.visibility = View.GONE
                        tracking_lblTrackingLink.visibility = View.GONE
                        tracking_lblProviderName.visibility = View.GONE
                    }
                }
            }
        }

        val startDate = DateUtils.getCurrentDateString()
        tracking_dateShipped.text = startDate
        tracking_dateShipped.setOnClickListener {
            displayDialog(
                    DateUtils.getCalendarInstance(tracking_dateShipped.text.toString()), tracking_dateShipped)
        }

        tracking_ok.setOnClickListener {
            val isCustomSelected = tracking_cboProvider.selectedItem == "Custom"
            val tracking = WCOrderShipmentTrackingModel().apply {
                if (isCustomSelected) {
                    this.trackingProvider = tracking_providerName.text.toString()
                    this.trackingLink = tracking_link.text.toString()
                } else {
                    this.trackingProvider = tracking_cboProvider.selectedItem.toString()
                }
                this.dateShipped = tracking_dateShipped.text.toString()
                this.trackingNumber = tracking_number.text.toString()
            }
            listener.onTrackingSubmitted(site, order, tracking, isCustomProvider = isCustomSelected)
            dismiss()
        }

        tracking_cancel.setOnClickListener {
            dismiss()
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
