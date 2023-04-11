package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.persistence.entity.BundledProductEntity

@Dao
interface BundledProductsDao {
    @Query("SELECT * FROM BundledProduct WHERE productId = :productId AND localSiteId = :localSiteId")
    fun observeBundledProducts(
        localSiteId: LocalOrRemoteId.LocalId,
        productId: LocalOrRemoteId.RemoteId
    ): Flow<List<BundledProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BundledProductEntity)

    @Query("DELETE FROM BundledProduct WHERE productId = :productId AND localSiteId = :localSiteId")
    suspend fun deleteAllFor(localSiteId: LocalOrRemoteId.LocalId, productId: LocalOrRemoteId.RemoteId)

    @Transaction
    suspend fun updateBundledProductsFor(
        localSiteId: LocalOrRemoteId.LocalId,
        productId: LocalOrRemoteId.RemoteId,
        bundledProducts: List<BundledProductEntity>
    ) {
        deleteAllFor(localSiteId, productId)
        bundledProducts.forEach { bundledProduct ->
            insert(bundledProduct)
        }
    }
}
