package org.wordpress.android.fluxc.persistence.converters

import androidx.room.TypeConverter
import org.wordpress.android.fluxc.persistence.entity.ProductEntity

class ProductTypeConverter {
    @TypeConverter
    fun fromProductType(value: ProductEntity.Type) = value.value

    @TypeConverter
    fun toProductType(value: String) = ProductEntity.Type.fromString(value)
}
