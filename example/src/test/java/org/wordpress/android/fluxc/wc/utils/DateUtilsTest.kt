package org.wordpress.android.fluxc.wc.utils

import org.junit.Test
import org.wordpress.android.fluxc.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.test.assertEquals

class DateUtilsTest {
    companion object {
        private const val DATE_FORMAT_DAY = "yyyy-MM-dd"
    }

    @Test
    fun testGetDateFromDateString() {
        val dateFormat = SimpleDateFormat(DATE_FORMAT_DAY, Locale.ROOT)

        val d1 = "2018-01-25"
        val date1 = DateUtils.getDateFromString(d1)
        assertEquals(dateFormat.parse(d1), date1)

        val d2 = "2018-01-28"
        val date2 = DateUtils.getDateFromString(d2)
        assertEquals(dateFormat.parse(d2), date2)

        var d3 = "2018-01-01"
        val date3 = DateUtils.getDateFromString(d3)
        assertEquals(dateFormat.parse(d3), date3)
    }
}
