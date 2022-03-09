package org.wordpress.android.fluxc.persistence.converters

import androidx.room.TypeConverter
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStatus

class ProductStatusConverter {
    @TypeConverter
    fun fromProductStatus(value: CoreProductStatus) = value.value

    @TypeConverter
    fun toProductStatus(value: String) = CoreProductStatus.fromValue(value)
}
