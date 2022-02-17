package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.wordpress.android.fluxc.persistence.entity.ProductCategoryEntity

@Dao
abstract class ProductCategoriesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertProductCategory(entity: ProductCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertProductCategories(entities: List<ProductCategoryEntity>)

    @Query("SELECT * FROM ProductCategories p JOIN CouponsAndProductCategories c " +
        "ON p.id = c.productCategoryId WHERE c.isExcluded = :areExcluded " +
        "AND c.couponId = :couponId ORDER BY p.id")
    abstract fun getCouponProductCategories(
        couponId: Long,
        areExcluded: Boolean
    ): List<ProductCategoryEntity>

    @Query("SELECT * FROM ProductCategories WHERE siteId = :siteId AND id IN (:categoryIds) " +
        "ORDER BY id")
    abstract fun getProductCategoriesByIds(
        siteId: Long,
        categoryIds: List<Long>
    ): List<ProductCategoryEntity>
}
