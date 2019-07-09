package org.wordpress.android.fluxc.example.utils

import org.wordpress.android.fluxc.utils.DateUtils.formatDate
import java.util.Calendar

object DateUtils {
    private const val DATE_TIME_FORMAT_DEFAULT = "yyyy-MM-dd"
    fun getStartOfCurrentDay(): String {
        val cal = Calendar.getInstance()
        return formatDate(DATE_TIME_FORMAT_DEFAULT, cal.time)
    }

    fun getFirstDayOfCurrentWeek(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.getActualMinimum(Calendar.DAY_OF_WEEK))
        return formatDate(DATE_TIME_FORMAT_DEFAULT, cal.time)
    }

    fun getFirstDayOfCurrentMonth(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH))
        return formatDate(DATE_TIME_FORMAT_DEFAULT, cal.time)
    }

    fun getFirstDayOfCurrentYear(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_YEAR, cal.getActualMinimum(Calendar.DAY_OF_YEAR))
        return formatDate(DATE_TIME_FORMAT_DEFAULT, cal.time)
    }
}
