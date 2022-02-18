package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.wordpress.android.fluxc.persistence.entity.ProductEntity

@Dao
abstract class ProductsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateProduct(entity: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateProducts(entities: List<ProductEntity>)

    @Query("SELECT * FROM Products p JOIN CouponsAndProducts c ON p.id = c.productId " +
        "WHERE c.isExcluded = :areExcluded AND c.couponId = :couponId ORDER BY p.id")
    abstract fun getCouponProducts(couponId: Long, areExcluded: Boolean): List<ProductEntity>

    @Query("SELECT * FROM Products WHERE siteId = :siteId AND id IN (:productIds) ORDER BY id")
    abstract fun getProductsByIds(siteId: Long, productIds: List<Long>): List<ProductEntity>

    @Query("DELETE FROM Products WHERE siteId = :siteId")
    abstract suspend fun deleteAllProducts(siteId: Long)

    @Query("DELETE FROM Products WHERE siteId = :siteId AND id = :productId")
    abstract suspend fun deleteProductByProductId(siteId: Long, productId: Long)
}
