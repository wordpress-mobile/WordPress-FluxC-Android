package org.wordpress.android.fluxc.persistence.converters

import androidx.room.TypeConverter
import java.math.BigDecimal

class BigDecimalConverter {
    @TypeConverter
    fun bigDecimalToString(input: BigDecimal) = input.toPlainString()

    @TypeConverter
    fun stringToBigDecimal(input: String) = input.toBigDecimalOrNull() ?: BigDecimal.ZERO
}
