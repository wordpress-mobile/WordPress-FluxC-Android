package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.entity.MetaDataEntity

@Dao
abstract class MetaDataDao {
    @Insert(onConflict = REPLACE)
    abstract fun insertOrUpdateMetaData(vararg metaDataEntity: MetaDataEntity)

    @Query("SELECT * FROM MetaData WHERE parentItemId = :parentItemId AND localSiteId = :localSiteId")
    abstract suspend fun getMetaData(
        localSiteId: LocalId,
        parentItemId: Long
    ): List<MetaDataEntity>

    @Query("SELECT * FROM MetaData WHERE parentItemId = :parentItemId AND localSiteId = :localSiteId")
    abstract fun observeMetaData(
        localSiteId: LocalId,
        parentItemId: Long
    ): Flow<List<MetaDataEntity>>

    @Query("SELECT * FROM MetaData WHERE id = :id AND parentItemId = :parentItemId AND localSiteId = :localSiteId")
    abstract suspend fun getMetaData(
        localSiteId: LocalId,
        parentItemId: Long,
        id: Long
    ): MetaDataEntity?

    @Query("""
        SELECT * FROM MetaData
        WHERE parentItemId = :parentItemId 
        AND localSiteId = :localSiteId 
        AND `key` NOT LIKE '\_%'
        ESCAPE '\'
        """)
    abstract suspend fun getDisplayableMetaData(
        localSiteId: LocalId,
        parentItemId: Long
    ): List<MetaDataEntity>

    @Query("SELECT COUNT(*) FROM MetaData WHERE parentItemId = :parentItemId AND localSiteId = :localSiteId")
    abstract suspend fun getMetaDataCount(parentItemId: Long, localSiteId: LocalId): Int

    @Query(
        """
        SELECT COUNT(*) FROM MetaData 
        WHERE parentItemId = :parentItemId 
        AND localSiteId = :localSiteId 
        AND `key` NOT LIKE '\_%'
        ESCAPE '\'
        """
    )
    abstract suspend fun getDisplayableMetaDataCount(
        localSiteId: LocalId,
        parentItemId: Long
    ): Int

    @Transaction
    @Query("DELETE FROM MetaData WHERE localSiteId = :localSiteId AND parentItemId = :parentItemId")
    abstract suspend fun deleteMetaData(localSiteId: LocalId, parentItemId: Long)

    @Transaction
    open suspend fun updateMetaData(
        parentItemId: Long,
        localSiteId: LocalId,
        metaData: List<MetaDataEntity>
    ) {
        deleteMetaData(localSiteId, parentItemId)
        @Suppress("SpreadOperator")
        insertOrUpdateMetaData(*metaData.toTypedArray())
    }

    suspend fun hasMetaData(parentItemId: Long, localSiteId: LocalId): Boolean {
        return getMetaDataCount(parentItemId, localSiteId) > 0
    }
}
