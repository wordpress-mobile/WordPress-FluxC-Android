package org.wordpress.android.fluxc.utils

import dagger.Reusable
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import javax.inject.Inject

@Reusable
class DateTimeUtilsWrapper @Inject constructor() {
    private val timeZone: TimeZone
        get() = TimeZone.currentSystemDefault()

    fun timestampFromIso8601(strDate: String?) = DateTimeUtils.timestampFromIso8601(strDate)
    fun iso8601UTCFromDate(date: Date?): String? = DateTimeUtils.iso8601UTCFromDate(date)

    fun iso8601UTCStartDateText(date: Date): String =
        with(Instant.fromEpochMilliseconds(date.time)) {
            LocalDateTime(
                toLocalDateTime(timeZone).date,
                LocalTime(0, 0, 0)
            ).toInstant(timeZone).toString()
        }

    fun iso8601UTCEndDateText(date: Date) = with(Instant.fromEpochMilliseconds(date.time)) {
        LocalDateTime(
            toLocalDateTime(timeZone).date,
            LocalTime(23, 59, 59)
        ).toInstant(timeZone)
            .toString()
    }
}
