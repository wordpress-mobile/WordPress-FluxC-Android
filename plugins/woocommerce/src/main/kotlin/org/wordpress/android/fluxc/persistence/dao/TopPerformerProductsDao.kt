package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.persistence.entity.TopPerformerProductEntity

@Dao
interface TopPerformerProductsDao {
    @Query("SELECT * FROM TopPerformerProducts WHERE granularity = :granularity AND siteId = :siteId")
    fun observeTopPerformerProducts(
        siteId: Long,
        granularity: String
    ): Flow<List<TopPerformerProductEntity>>

    @Query("SELECT * FROM TopPerformerProducts WHERE granularity = :granularity AND siteId = :siteId")
    suspend fun getTopPerformerProductsFor(
        siteId: Long,
        granularity: String
    ): List<TopPerformerProductEntity>

    @Query("SELECT * FROM TopPerformerProducts WHERE siteId = :siteId")
    suspend fun getTopPerformerProductsForSite(
        siteId: Long
    ): List<TopPerformerProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TopPerformerProductEntity)

    @Query("DELETE FROM TopPerformerProducts WHERE granularity = :granularity AND siteId = :siteId")
    suspend fun deleteAllFor(siteId: Long, granularity: String)

    @Transaction
    suspend fun updateTopPerformerProductsFor(
        siteId: Long,
        granularity: String,
        topPerformerProducts: List<TopPerformerProductEntity>
    ) {
        deleteAllFor(siteId, granularity)
        topPerformerProducts.forEach { topPerformerProduct ->
            insert(topPerformerProduct)
        }
    }

    @Transaction
    suspend fun updateTopPerformerProductsForSite(
        siteId: Long,
        topPerformerProducts: List<TopPerformerProductEntity>
    ) {
        topPerformerProducts.forEach { topPerformerProduct ->
            insert(topPerformerProduct)
        }
    }
}
