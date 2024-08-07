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

    @Query("SELECT * FROM MetaData WHERE parentId = :parentId AND localSiteId = :localSiteId")
    abstract suspend fun getMetaData(
        localSiteId: LocalId,
        parentId: Long
    ): List<MetaDataEntity>

    @Query("SELECT * FROM MetaData WHERE parentId = :parentId AND localSiteId = :localSiteId")
    abstract fun observeMetaData(
        localSiteId: LocalId,
        parentId: Long
    ): Flow<List<MetaDataEntity>>

    @Query("SELECT * FROM MetaData WHERE id = :id AND parentId = :parentId AND localSiteId = :localSiteId")
    abstract suspend fun getMetaData(
        localSiteId: LocalId,
        parentId: Long,
        id: Long
    ): MetaDataEntity?

    @Query("""
        SELECT * FROM MetaData
        WHERE parentId = :parentId AND localSiteId = :localSiteId AND key NOT LIKE '_%'
        """)
    abstract suspend fun getDisplayableMetaData(
        localSiteId: LocalId,
        parentId: Long
    ): List<MetaDataEntity>

    @Query("SELECT COUNT(*) FROM MetaData WHERE parentId = :parentId AND localSiteId = :localSiteId")
    abstract suspend fun getMetaDataCount(parentId: Long, localSiteId: LocalId): Int

    @Query(
        """
        SELECT COUNT(*) FROM MetaData 
        WHERE parentId = :parentId AND localSiteId = :localSiteId AND key NOT LIKE '_%'
        """
    )
    abstract suspend fun getDisplayableMetaDataCount(
        localSiteId: LocalId,
        parentId: Long
    ): Int

    @Transaction
    @Query("DELETE FROM MetaData WHERE localSiteId = :localSiteId AND parentId = :parentId")
    abstract suspend fun deleteMetaData(localSiteId: LocalId, parentId: Long)

    @Transaction
    open suspend fun updateMetaData(
        parentId: Long,
        localSiteId: LocalId,
        metaData: List<MetaDataEntity>
    ) {
        deleteMetaData(localSiteId, parentId)
        insertOrUpdateMetaData(*metaData.toTypedArray())
    }

    suspend fun hasMetaData(parentId: Long, localSiteId: LocalId): Boolean {
        return getMetaDataCount(parentId, localSiteId) > 0
    }
}
