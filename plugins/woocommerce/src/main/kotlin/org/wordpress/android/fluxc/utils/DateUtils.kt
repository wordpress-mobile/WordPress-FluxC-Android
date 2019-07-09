package org.wordpress.android.fluxc.utils

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private const val DATE_FORMAT_DEFAULT = "yyyy-MM-dd"
    private const val TIME_FORMAT_START = "T00:00:00"
    private const val TIME_FORMAT_END = "T23:59:59"

    /**
     * Given a [SiteModel] and a [String] compatible with [SimpleDateFormat]
     * and a {@param dateString}
     * returns a formatted date that accounts for the site's timezone setting.
     *
     */
    fun getDateTimeForSite(
        site: SiteModel,
        pattern: String,
        dateString: String?
    ): String {
        val currentDate = Date()

        if (dateString.isNullOrEmpty()) {
            return SiteUtils.getDateTimeForSite(site, pattern, currentDate)
        }

        /*
         * Since only date is provided without the time,
         * by default the time is set to the start of the day.
         *
         * This might cause timezone issues so getting the current time
         * and setting this time to the date value
         * */
        val now = Calendar.getInstance()
        now.time = currentDate

        val date = getDateFromString(dateString)
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
        calendar.add(Calendar.MINUTE, now.get(Calendar.MINUTE))
        calendar.add(Calendar.SECOND, now.get(Calendar.SECOND))
        return SiteUtils.getDateTimeForSite(site, pattern, calendar.time)
    }

    /**
     * returns a [Date] instance
     * based on {@param pattern} and {@param dateString}
     */
    fun getDateFromString(dateString: String): Date {
        val dateFormat = SimpleDateFormat(DATE_FORMAT_DEFAULT, Locale.ROOT)
        return dateFormat.parse(dateString)
    }

    /**
     * returns a [String] formatted
     * based on {@param pattern} and {@param date}
     */
    fun formatDate(
        pattern: String,
        date: Date
    ): String {
        val dateFormat = SimpleDateFormat(pattern, Locale.ROOT)
        return dateFormat.format(date)
    }

    /**
     * Given a [SimpleDateFormat] instance and
     * the [String] start date string, returns a [Calendar] instance.
     *
     * The start date time is set to 00:00:00
     *
     */
    fun getStartDateCalendar(startDate: Date): Calendar {
        val cal1 = Calendar.getInstance()
        cal1.time = startDate
        cal1.set(Calendar.HOUR_OF_DAY, 0)
        cal1.set(Calendar.MINUTE, 0)
        cal1.set(Calendar.SECOND, 0)
        cal1.set(Calendar.MILLISECOND, 0)

        return cal1
    }

    /**
     * Given a [SimpleDateFormat] instance and
     * the [String] end date string, returns a [Calendar] instance.
     *
     * The end date time is set to 23:59:59
     *
     */
    fun getEndDateCalendar(endDate: Date): Calendar {
        val cal2 = Calendar.getInstance()
        cal2.time = endDate
        cal2.set(Calendar.HOUR_OF_DAY, 23)
        cal2.set(Calendar.MINUTE, 59)
        cal2.set(Calendar.SECOND, 59)
        cal2.set(Calendar.MILLISECOND, 59)
        return cal2
    }

    /**
     * Given a [Calendar] instance for startDate and endDate,
     * returns a quantity that is calculated based on [StatsGranularity.DAYS]
     */
    fun getQuantityInDays(
        startDateCalendar: Calendar,
        endDateCalendar: Calendar
    ): Long {
        val millis1 = startDateCalendar.timeInMillis
        val millis2 = endDateCalendar.timeInMillis

        val diff = Math.abs(millis2 - millis1)
        return Math.ceil(diff / (24 * 60 * 60 * 1000).toDouble()).toLong()
    }

    /**
     * Given a [Calendar] instance for startDate and endDate,
     * returns a quantity that is calculated based on [StatsGranularity.WEEKS]
     */
    fun getQuantityInWeeks(
        startDateCalendar: Calendar,
        endDateCalendar: Calendar
    ): Long {
        /*
         * start date: if day of week is greater than 1: set to 1
         * end date: if day of week is less than 7: set to 7
         *
         * This logic is to handle half week scenarios, for instance if the
         * start date = 2019-01-25 and end date = 2019-01-28 - the difference
         * in weeks should be 2 since the dates are actually in two different weeks
         *
         * */
        if (startDateCalendar.get(Calendar.DAY_OF_WEEK) > 1) {
            startDateCalendar.set(Calendar.DAY_OF_WEEK, 1)
        }
        if (endDateCalendar.get(Calendar.DAY_OF_WEEK) < 1) {
            endDateCalendar.set(Calendar.DAY_OF_WEEK, 7)
        }

        val diffInDays = getQuantityInDays(startDateCalendar, endDateCalendar).toDouble()
        return Math.ceil(diffInDays / 7).toLong()
    }

    /**
     * Given a [Calendar] instance for startDate and endDate,
     * returns a quantity that is calculated based on [StatsGranularity.MONTHS]
     */
    fun getQuantityInMonths(
        startDateCalendar: Calendar,
        endDateCalendar: Calendar
    ): Long {
        /*
         * start date: if day of month is greater than 1: set to 1
         * end date: if day of month is less than the maximum day of month for that particular month:
         * set to maximum day of month for that particular month
         *
         * This is to handle scenarios where the start date such as if start date = 12/31/18 and end date = 1/1/19,
         * the default difference in months would be 1, but it should be 2 since these are two separate months
         * */
        if (startDateCalendar.get(Calendar.DAY_OF_MONTH) > 1) {
            startDateCalendar.set(Calendar.DAY_OF_MONTH, 1)
        }
        if (endDateCalendar.get(Calendar.DAY_OF_MONTH) < endDateCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)) {
            endDateCalendar.set(Calendar.DAY_OF_MONTH, endDateCalendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        }

        var diff: Long = 0
        if (endDateCalendar.after(startDateCalendar)) {
            while (endDateCalendar.after(startDateCalendar)) {
                if (endDateCalendar.after(startDateCalendar)) {
                    diff++
                }
                startDateCalendar.add(Calendar.MONTH, 1)
            }
        }
        return Math.abs(diff)
    }

    /**
     * Given a [Calendar] instance for startDate and endDate,
     * returns a quantity that is calculated based on [StatsGranularity.YEARS]
     */
    fun getQuantityInYears(
        startDateCalendar: Calendar,
        endDateCalendar: Calendar
    ): Long {
        /*
         * start date: if day of year is greater than 1: set to 1
         * end date: if day of year is less than the maximum day of year for that particular year:
         * set to maximum day of year for that particular year
         *
         * This is to handle scenarios where the start date such as if start date = 12/31/18 and end date = 1/1/19,
         * the default difference in years would be 1, but it should be 2 since these are two separate years
         * */
        if (startDateCalendar.get(Calendar.DAY_OF_YEAR) > 1) {
            startDateCalendar.set(Calendar.DAY_OF_YEAR, 1)
        }
        if (endDateCalendar.get(Calendar.DAY_OF_YEAR) < endDateCalendar.getActualMaximum(Calendar.DAY_OF_YEAR)) {
            endDateCalendar.set(Calendar.DAY_OF_YEAR, endDateCalendar.getActualMaximum(Calendar.DAY_OF_YEAR))
        }

        var diff: Long = 0
        if (endDateCalendar.after(startDateCalendar)) {
            while (endDateCalendar.after(startDateCalendar)) {
                if (endDateCalendar.after(startDateCalendar)) {
                    diff++
                }
                startDateCalendar.add(Calendar.YEAR, 1)
            }
        }
        return Math.abs(diff)
    }

    /**
     * returns string of the current date
     * in the format: YYYY-MM-dd
     *
     */
    fun getCurrentDateString(): String {
        val calendar = Calendar.getInstance()
        calendar.time = Date()
        val dayOfMonth = calendar.get(Calendar.DATE)
        val year = calendar.get(Calendar.YEAR)
        val monthOfYear = calendar.get(Calendar.MONTH)
        return getFormattedDateString(year, monthOfYear, dayOfMonth)
    }

    /**
     * Given a year integer, month integer, date integer,
     * returns string in the format: YYYY-MM-dd
     *
     */
    fun getFormattedDateString(year: Int, month: Int, dayOfMonth: Int): String {
        return String.format("%d-%02d-%02d", year, (month + 1), dayOfMonth)
    }

    /**
     * Given a date string of format YYYY-MM-dd, returns a [Calendar] instance of the same
     *
     */
    fun getCalendarInstance(value: String): Calendar {
        val (year, month, day) = value.split("-").map { it.toInt() }
        return Calendar.getInstance().apply { set(year, month - 1, day) }
    }

    /**
     * Format the date for UTC and return as string
     */
    fun formatGmtAsUtcDateString(gmtVal: String): String = "${gmtVal}Z"

    /**
     * Given a [SiteModel] and a [dateString] in format yyyy-MM-dd,
     * returns a formatted date that accounts for the site's timezone setting,
     * in the format yyy-MM-ddThh:mm:ss with the time always set to the start of the [dateString]
     */
    fun getStartDateForSite(site: SiteModel, dateString: String): String {
        val formattedDateString = getDateTimeForSite(site, DATE_FORMAT_DEFAULT, dateString)
        return "$formattedDateString$TIME_FORMAT_START"
    }

    /**
     * Given a [SiteModel],
     * returns a formatted date that accounts for the site's timezone setting,
     * in the format yyy-MM-ddThh:mm:ss with the time always set to the end of the
     * current date
     */
    fun getEndDateForSite(site: SiteModel): String {
        val formattedDateString = getDateTimeForSite(site, DATE_FORMAT_DEFAULT, null)
        return "$formattedDateString$TIME_FORMAT_END"
    }
}
