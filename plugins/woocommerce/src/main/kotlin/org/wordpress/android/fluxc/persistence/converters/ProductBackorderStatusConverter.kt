package org.wordpress.android.fluxc.persistence.converters

import androidx.room.TypeConverter
import org.wordpress.android.fluxc.persistence.entity.ProductEntity

class ProductBackorderStatusConverter {
    @TypeConverter
    fun fromProductBackorderStatus(value: ProductEntity.BackorderStatus) = value.toString()

    @TypeConverter
    fun toProductBackorderStatus(value: String) = ProductEntity.BackorderStatus.fromString(value)
}
