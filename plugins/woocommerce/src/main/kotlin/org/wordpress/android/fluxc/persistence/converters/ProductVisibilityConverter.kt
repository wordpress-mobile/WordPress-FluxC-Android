package org.wordpress.android.fluxc.persistence.converters

import androidx.room.TypeConverter
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductVisibility

class ProductVisibilityConverter {
    @TypeConverter
    fun fromProductVisibility(value: CoreProductVisibility) = value.value

    @TypeConverter
    fun toProductVisibility(value: String) = CoreProductVisibility.fromValue(value)
}
