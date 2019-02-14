package org.wordpress.android.fluxc.example

import android.R.layout
import android.app.DatePickerDialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
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
        fun newInstance(
            fragment: Fragment,
            wcOrderStatsModel: WCOrderStatsModel?,
            forced: Boolean = false
        ) = CustomStatsDialog().apply {
            setTargetFragment(fragment, 100)
            this.forced = forced
            this.wcOrderStatsModel = wcOrderStatsModel
        }
    }

    interface Listener {
        fun onSubmitted(startDate: String, endDate: String, granularity: StatsGranularity, forced: Boolean)
    }

    var forced: Boolean = false
    var listener: Listener? = null
    var wcOrderStatsModel: WCOrderStatsModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listener = targetFragment as Listener
    }

    override fun onResume() {
        super.onResume()
        dialog.window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("start_date", stats_from_date.text.toString())
        outState.putString("end_date", stats_to_date.text.toString())
        outState.putInt("granularity", (stats_granularity.selectedItem as StatsGranularity).ordinal)
        outState.putBoolean("forced", forced)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_custom_stats, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stats_granularity.adapter =
                ArrayAdapter<StatsGranularity>(activity, layout.simple_dropdown_item_1line, StatsGranularity.values())

        if (savedInstanceState != null) {
            stats_from_date.text = savedInstanceState.getString("start_date")
            stats_to_date.text = savedInstanceState.getString("end_date")
            stats_granularity.setSelection(savedInstanceState.getInt("granularity"))
            forced = savedInstanceState.getBoolean("forced")
        } else {
            val startDate = wcOrderStatsModel?.startDate ?: getCurrentDateString()
            stats_from_date.text = startDate

            val endDate = wcOrderStatsModel?.endDate ?: getCurrentDateString()
            stats_to_date.text = endDate

            wcOrderStatsModel?.unit?.let {
                stats_granularity.setSelection(StatsGranularity.fromString(it).ordinal)
            }
        }

        stats_from_date.setOnClickListener {
            displayDialog(getCalendarInstance(stats_from_date.text.toString()), stats_from_date)
        }

        stats_to_date.setOnClickListener {
            displayDialog(getCalendarInstance(stats_to_date.text.toString()), stats_to_date)
        }

        stats_dialog_ok.setOnClickListener {
            val startDate = stats_from_date.text.toString()
            val endDate = stats_to_date.text.toString()
            val granularity: StatsGranularity = stats_granularity.selectedItem as StatsGranularity

            listener?.onSubmitted(startDate, endDate, granularity, forced)
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
                }, calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DATE))
        datePicker.datePicker.maxDate = Date().time
        datePicker.show()
    }

    private fun getCurrentDateString(): String {
        val calendar = Calendar.getInstance()
        calendar.time = Date()
        val dayOfMonth = calendar.get(Calendar.DATE)
        val year = calendar.get(Calendar.YEAR)
        val monthOfYear = calendar.get(Calendar.MONTH)
        return getFormattedDate(year, monthOfYear, dayOfMonth)
    }

    private fun getFormattedDate(year: Int, month: Int, dayOfMonth: Int): String {
        return String.format("%d-%02d-%02d", year, (month + 1), dayOfMonth)
    }

    private fun getCalendarInstance(value: String): Calendar {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DATE, value.split("-")[2].toInt())
        cal.set(Calendar.MONTH, (value.split("-")[1].toInt()))
        cal.add(Calendar.MONTH, -1)
        cal.set(Calendar.YEAR, value.split("-")[0].toInt())
        return cal
    }
}
