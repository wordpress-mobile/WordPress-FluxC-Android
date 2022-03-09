package org.wordpress.android.fluxc.persistence.converters

import androidx.room.TypeConverter
import org.wordpress.android.fluxc.persistence.entity.ProductEntity

class ProductTaxStatusConverter {
    @TypeConverter
    fun fromProductTaxStatus(value: ProductEntity.TaxStatus) = value.toString()

    @TypeConverter
    fun toProductTaxStatus(value: String) = ProductEntity.TaxStatus.fromString(value)
}
