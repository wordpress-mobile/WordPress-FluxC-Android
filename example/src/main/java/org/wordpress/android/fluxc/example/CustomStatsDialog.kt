package org.wordpress.android.fluxc.example

import android.R.layout
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.dialog_custom_stats.*
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.utils.DateUtils
import java.util.Calendar
import java.util.Date

class CustomStatsDialog : DialogFragment() {
    enum class WCOrderStatsAction {
        FETCH_CUSTOM_ORDER_STATS,
        FETCH_CUSTOM_ORDER_STATS_FORCED,
        FETCH_CUSTOM_VISITOR_STATS,
        FETCH_CUSTOM_VISITOR_STATS_FORCED
    }

    companion object {
        @JvmStatic
        fun newInstance(
            fragment: Fragment,
            startDate: String?,
            endDate: String?,
            unit: String?,
            wcOrderStatsAction: WCOrderStatsAction
        ) = CustomStatsDialog().apply {
            setTargetFragment(fragment, 100)
            this.startDate = startDate
            this.endDate = endDate
            this.granularity = unit
            this.wcOrderStatsAction = wcOrderStatsAction
        }
    }

    interface Listener {
        fun onSubmitted(
            startDate: String,
            endDate: String,
            granularity: StatsGranularity,
            wcOrderStatsAction: WCOrderStatsAction?
        )
    }

    var listener: Listener? = null
    var startDate: String? = null
    var endDate: String? = null
    var granularity: String? = null
    var wcOrderStatsAction: WCOrderStatsAction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listener = targetFragment as Listener
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("start_date", stats_from_date.text.toString())
        outState.putString("end_date", stats_to_date.text.toString())
        outState.putInt("granularity", (stats_granularity.selectedItem as StatsGranularity).ordinal)
        outState.putString("action", wcOrderStatsAction.toString())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_custom_stats, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stats_granularity.adapter =
                ArrayAdapter<StatsGranularity>(
                        requireActivity(), layout.simple_dropdown_item_1line, StatsGranularity.values())

        if (savedInstanceState != null) {
            stats_from_date.text = savedInstanceState.getString("start_date")
            stats_to_date.text = savedInstanceState.getString("end_date")
            stats_granularity.setSelection(savedInstanceState.getInt("granularity"))
            wcOrderStatsAction = savedInstanceState.getString("action")?.let {
                WCOrderStatsAction.valueOf(it.toUpperCase())
            }
        } else {
            val startDate = startDate?.let { it } ?: DateUtils.getCurrentDateString()
            stats_from_date.text = startDate

            val endDate = endDate?.let { it } ?: DateUtils.getCurrentDateString()
            stats_to_date.text = endDate

            granularity?.let {
                stats_granularity.setSelection(StatsGranularity.fromString(it).ordinal)
            }
        }

        stats_from_date.setOnClickListener {
            displayDialog(
                    DateUtils.getCalendarInstance(stats_from_date.text.toString()),
                    stats_from_date)
        }

        stats_to_date.setOnClickListener {
            displayDialog(
                    DateUtils.getCalendarInstance(stats_to_date.text.toString()),
                    stats_to_date)
        }

        stats_dialog_ok.setOnClickListener {
            val startDate = stats_from_date.text.toString()
            val endDate = stats_to_date.text.toString()
            val granularity: StatsGranularity = stats_granularity.selectedItem as StatsGranularity

            listener?.onSubmitted(startDate, endDate, granularity, wcOrderStatsAction)
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
                    button.text = DateUtils.getFormattedDateString(year, month, dayOfMonth)
                }, calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DATE))
        datePicker.datePicker.maxDate = Date().time
        datePicker.show()
    }
}
