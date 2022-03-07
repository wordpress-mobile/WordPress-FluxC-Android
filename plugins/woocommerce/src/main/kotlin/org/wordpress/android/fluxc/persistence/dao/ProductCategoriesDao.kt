package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.persistence.entity.ProductCategoryEntity

@Dao
abstract class ProductCategoriesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateProductCategory(entity: ProductCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateProductCategories(entities: List<ProductCategoryEntity>)

    @Query("SELECT * FROM ProductCategories p JOIN CouponsAndProductCategories c " +
        "ON p.id = c.productCategoryId WHERE c.isExcluded = :areExcluded " +
        "AND c.couponId = :couponId AND p.siteId = :siteId ORDER BY p.id")
    abstract fun getCouponProductCategories(
        siteId: Long,
        couponId: Long,
        areExcluded: Boolean
    ): List<ProductCategoryEntity>

    @Query("SELECT * FROM ProductCategories p JOIN CouponsAndProductCategories c " +
        "ON p.id = c.productCategoryId WHERE c.isExcluded = :areExcluded " +
        "AND c.couponId = :couponId AND p.siteId = :siteId ORDER BY p.id")
    abstract fun observeCouponProductCategories(
        siteId: Long,
        couponId: Long,
        areExcluded: Boolean
    ): Flow<List<ProductCategoryEntity>>

    @Query("SELECT * FROM ProductCategories WHERE siteId = :siteId AND id IN (:categoryIds) " +
        "ORDER BY id")
    abstract fun getProductCategoriesByIds(
        siteId: Long,
        categoryIds: List<Long>
    ): List<ProductCategoryEntity>

    @Query("DELETE FROM ProductCategories WHERE siteId = :siteId")
    abstract suspend fun deleteAllProductCategories(siteId: Long)
}
