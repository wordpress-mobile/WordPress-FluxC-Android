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
import android.widget.Button
import kotlinx.android.synthetic.main.dialog_custom_stats.*
import org.wordpress.android.fluxc.model.WCOrderStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import java.util.Calendar
import java.util.Date

class CustomStatsDialog : DialogFragment() {
    companion object {
        @JvmStatic
        fun newInstance(listener: Listener, wcOrderStatsModel: WCOrderStatsModel?) = CustomStatsDialog().apply {
            this.listener = listener
            this.wcOrderStatsModel = wcOrderStatsModel
        }
    }

    interface Listener {
        fun onSubmitted(startDate: String, endDate: String, granularity: StatsGranularity)
    }

    var listener: Listener? = null
    var wcOrderStatsModel: WCOrderStatsModel? = null

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

        wcOrderStatsModel?.startDate?.let { stats_from_date.text = it }
        wcOrderStatsModel?.endDate?.let { stats_to_date.text = it }
        wcOrderStatsModel?.unit?.let {
            stats_granularity.setSelection(StatsGranularity.fromString(it).ordinal
            )
        }

        stats_from_date.setOnClickListener {
            displayDialog(getCalendarInstance(wcOrderStatsModel?.startDate), stats_from_date)
        }

        stats_to_date.setOnClickListener {
            displayDialog(getCalendarInstance(wcOrderStatsModel?.endDate), stats_to_date)
        }

        stats_dialog_ok.setOnClickListener {
            val startDate = stats_from_date.text.toString()
            val endDate = stats_to_date.text.toString()
            val granularity: StatsGranularity = stats_granularity.selectedItem as StatsGranularity

            listener?.onSubmitted(startDate, endDate, granularity)
            dismiss()
        }

        stats_dialog_cancel.setOnClickListener {
            dismiss()
        }
    }

    private fun displayDialog(
        calendar: Calendar,
        button: Button
    ) {
        val datePicker = DatePickerDialog(requireActivity(),
                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                    button.text = getFormattedDate(year, month, dayOfMonth)
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        datePicker.datePicker.maxDate = Date().time
        datePicker.show()
    }

    private fun getFormattedDate(year: Int, month: Int, dayOfMonth: Int): String {
        val monthOfYear = month + 1
        return String.format("$year-$monthOfYear-$dayOfMonth")
    }

    private fun getCalendarInstance(value: String?): Calendar {
        val cal = Calendar.getInstance()
        if (!value.isNullOrBlank()) {
            cal.set(value!!.split("-")[0].toInt(),
                    (value.split("-")[1].toInt()) - 1,
                    value.split("-")[2].toInt())
        }
        return cal
    }
}
