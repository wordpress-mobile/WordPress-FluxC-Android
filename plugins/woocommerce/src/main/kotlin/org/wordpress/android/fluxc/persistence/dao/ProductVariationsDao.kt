package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.persistence.entity.ProductEntity
import org.wordpress.android.fluxc.persistence.entity.ProductVariationEntity

@Dao
abstract class ProductVariationsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateProductVariation(entity: ProductVariationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateProductVariations(entities: List<ProductVariationEntity>)

    @Query("SELECT * FROM ProductVariations WHERE id = :productVariationId")
    abstract suspend fun getProductVariation(productVariationId: Long): ProductVariationEntity?

    @Query("SELECT * FROM ProductVariations WHERE id = :productVariationId")
    abstract fun observeProductVariation(productVariationId: Long): Flow<ProductVariationEntity?>

    @Query("SELECT * FROM ProductVariations WHERE siteId = :siteId AND productId = :productId " +
        "ORDER BY dateCreated ASC")
    abstract suspend fun getProductVariations(
        siteId: Long,
        productId: Long
    ): List<ProductVariationEntity>

    @Query("SELECT * FROM ProductVariations WHERE siteId = :siteId AND productId = :productId " +
        "ORDER BY dateCreated ASC")
    abstract fun observeProductVariations(
        siteId: Long,
        productId: Long
    ): Flow<List<ProductVariationEntity>>

    @Transaction
    @Query("SELECT * FROM ProductVariations WHERE siteId = :siteId AND productId = :productId " +
        "AND id IN (:variationIds) ORDER BY dateCreated ASC")
    abstract suspend fun getProductVariationsByIds(
        siteId: Long,
        productId: Long,
        variationIds: List<Long>
    ): List<ProductVariationEntity>

    @Query("DELETE FROM ProductVariations WHERE siteId = :siteId AND productId = :productId")
    abstract suspend fun deleteAllProductVariationsForProduct(siteId: Long, productId: Long)

    @Query("DELETE FROM ProductVariations WHERE siteId = :siteId AND id = :variationId")
    abstract suspend fun deleteProductVariationById(siteId: Long, variationId: Long)
}
