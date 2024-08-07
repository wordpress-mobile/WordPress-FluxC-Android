package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.entity.MetaDataEntity

@Dao
abstract class MetaDataDao {
    @Insert(onConflict = REPLACE)
    abstract fun insertOrUpdateMetaData(metaDataEntity: MetaDataEntity)

    @Query("SELECT * FROM MetaData WHERE parentId = :parentId AND localSiteId = :localSiteId")
    abstract suspend fun getMetaData(
        parentId: Long,
        localSiteId: LocalId
    ): List<MetaDataEntity>

    @Query("SELECT * FROM MetaData WHERE parentId = :parentId AND localSiteId = :localSiteId AND isDisplayable = 1")
    abstract suspend fun getDisplayableMetaData(
        parentId: Long,
        localSiteId: LocalId
    ): List<MetaDataEntity>

    @Query("SELECT COUNT(*) FROM MetaData WHERE parentId = :parentId AND localSiteId = :localSiteId")
    abstract suspend fun getMetaDataCount(parentId: Long, localSiteId: LocalId): Int

    @Query(
        """
        SELECT COUNT(*) FROM MetaData 
        WHERE parentId = :parentId AND localSiteId = :localSiteId AND isDisplayable = 1
        """
    )
    abstract suspend fun getDisplayableMetaDataCount(parentId: Long, localSiteId: LocalId): Int

    @Transaction
    @Query("DELETE FROM MetaData WHERE localSiteId = :localSiteId AND parentId = :parentId")
    abstract fun deleteMetaData(localSiteId: LocalId, parentId: Long)

    @Transaction
    open fun updateMetaData(
        parentId: Long,
        localSiteId: LocalId,
        metaData: List<MetaDataEntity>
    ) {
        deleteMetaData(localSiteId, parentId)
        metaData.forEach {
            insertOrUpdateMetaData(it)
        }
    }

    suspend fun hasMetaData(parentId: Long, localSiteId: LocalId): Boolean {
        return getMetaDataCount(parentId, localSiteId) > 0
    }
}
