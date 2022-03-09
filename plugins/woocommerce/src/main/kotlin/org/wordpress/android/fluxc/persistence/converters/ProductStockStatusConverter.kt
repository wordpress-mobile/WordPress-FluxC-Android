package org.wordpress.android.fluxc.persistence.converters

import androidx.room.TypeConverter
import org.wordpress.android.fluxc.persistence.entity.ProductEntity

class ProductStockStatusConverter {
    @TypeConverter
    fun fromProductStockStatus(value: ProductEntity.StockStatus) = value.toString()

    @TypeConverter
    fun toProductStockStatus(value: String) = ProductEntity.StockStatus.fromString(value)
}
