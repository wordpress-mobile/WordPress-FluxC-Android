package org.wordpress.android.fluxc.example

import android.R.layout
import android.app.DatePickerDialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.dialog_custom_stats.*
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import java.util.Calendar

class CustomStatsDialog : DialogFragment() {
    companion object {
        @JvmStatic
        fun newInstance(listener: Listener) = CustomStatsDialog().apply { this.listener = listener }
    }

    interface Listener {
        fun onSubmitted(startDate: String, endDate: String, granularity: StatsGranularity)
    }

    var listener: Listener? = null

    override fun onResume() {
        super.onResume()
        dialog.window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_custom_stats, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stats_granularity.adapter =
                ArrayAdapter<StatsGranularity>(activity, layout.simple_dropdown_item_1line, StatsGranularity.values())


        stats_from_date.setOnClickListener {
            val now = getCalendarInstance()
            val datePicker = DatePickerDialog(requireActivity(), DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                stats_label_from.text = String.format("Start Date selected: %s", getFormattedDate(year, month, dayOfMonth))
            },
                    now.get(Calendar.YEAR),now.get(Calendar.MONTH),now.get(Calendar.DAY_OF_MONTH))
            datePicker.datePicker.maxDate = now.timeInMillis
            datePicker.show()
        }

        stats_to_date.setOnClickListener {
            val now = getCalendarInstance()
            val datePicker = DatePickerDialog(requireActivity(), DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                stats_label_to.text = String.format("End Date selected: %s", getFormattedDate(year, month, dayOfMonth))
            },
                    now.get(Calendar.YEAR),now.get(Calendar.MONTH),now.get(Calendar.DAY_OF_MONTH))
            datePicker.datePicker.maxDate = now.timeInMillis
            datePicker.show()
        }

        stats_dialog_ok.setOnClickListener {
            listener?.let {
                val startDate = stats_label_from.text.let { stats_label_from.text.split(": ")[1] }
                val endDate = stats_label_to.text.let { stats_label_to.text.split(": ")[1] }
                val granularity: StatsGranularity = stats_granularity.selectedItem as StatsGranularity
                it.onSubmitted(startDate, endDate, granularity)
//                Toast.makeText(requireContext(), "Start Date: $startDate, End Date: $endDate, Granularity: ${granularity.name}", LENGTH_LONG).show()
            }
            dismiss()
        }

        stats_dialog_cancel.setOnClickListener {
            dismiss()
        }
    }

    private fun getFormattedDate(year: Int, month: Int, dayOfMonth: Int) : String {
        val monthOfYear = month + 1
        return String.format("$year-$monthOfYear-$dayOfMonth")
    }

    private fun getCalendarInstance() = Calendar.getInstance()
}
