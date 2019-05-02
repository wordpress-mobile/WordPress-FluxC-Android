package org.wordpress.android.fluxc.persistence.room

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.Query
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType

@Dao
interface StatsRequestDao {
    @Insert(onConflict = REPLACE)
    fun insertOrReplace(statsBlock: StatsRequest)

    @Query(
            """
        DELETE from StatsRequest
        WHERE localSiteId == :localSiteId
        AND blockType == :blockType
        AND statsType == :statsType
        AND (:date IS NULL OR date == :date)
        AND (:postId IS NULL OR postId == :postId)
        """
    )
    fun delete(localSiteId: Int, blockType: BlockType, statsType: StatsType, date: String? = null, postId: Long? = null)

    @Query(
            """
        SELECT * FROM StatsRequest
        WHERE localSiteId == :localSiteId
        AND blockType == :blockType
        AND statsType == :statsType
        AND (:date IS NULL OR date == :date)
        AND (:postId IS NULL OR postId == :postId)
        AND (:after IS NULL OR :after > timeStamp)
        AND (:requestedItems IS NULL OR :requestedItems >= requestedItems)
        LIMIT 1
        """
    )
    fun select(
        localSiteId: Int,
        blockType: BlockType,
        statsType: StatsType,
        date: String? = null,
        postId: Long? = null,
        after: Long? = null,
        requestedItems: Int? = null
    ): StatsRequest?

    @Entity(tableName = "StatsRequest")
    data class StatsRequest(
        @PrimaryKey(autoGenerate = true) var id: Long? = null,
        @ColumnInfo var localSiteId: Int,
        @ColumnInfo var blockType: BlockType,
        @ColumnInfo var statsType: StatsType,
        @ColumnInfo var date: String?,
        @ColumnInfo var postId: Long?,
        @ColumnInfo var timeStamp: Long,
        @ColumnInfo var requestedItems: Int?
    )
}
