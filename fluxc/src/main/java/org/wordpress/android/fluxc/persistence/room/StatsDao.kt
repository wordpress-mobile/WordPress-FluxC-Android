package org.wordpress.android.fluxc.persistence.room

import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.PrimaryKey
import androidx.room.Query
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType

@Dao
interface StatsDao {
    @Insert(onConflict = REPLACE)
    fun insertOrReplace(statsBlock: StatsBlock)

    @Query("""
        DELETE from StatsBlock
        WHERE localSiteId == :localSiteId
        AND blockType == :blockType
        AND statsType == :statsType
        AND (:date IS NULL OR date == :date)
        """)
    fun delete(localSiteId: Int, blockType: BlockType, statsType: StatsType, date: String? = null)

    @Query("""
        SELECT * FROM StatsBlock
        WHERE localSiteId == :localSiteId
        AND blockType == :blockType
        AND statsType == :statsType
        AND (:date IS NULL OR date == :date)
        AND (:postId IS NULL OR postId == :postId)
        """)
    fun liveSelectAll(
        localSiteId: Int,
        blockType: BlockType,
        statsType: StatsType,
        date: String? = null,
        postId: Long? = null
    ): LiveData<List<StatsBlock>>

    @Query("""
        SELECT * FROM StatsBlock
        WHERE localSiteId == :localSiteId
        AND blockType == :blockType
        AND statsType == :statsType
        AND (:date IS NULL OR date == :date)
        AND (:postId IS NULL OR postId == :postId)
        LIMIT 1
        """)
    fun liveSelect(
        localSiteId: Int,
        blockType: BlockType,
        statsType: StatsType,
        date: String? = null,
        postId: Long? = null
    ): LiveData<StatsBlock>

    @Query("""
        SELECT * FROM StatsBlock
        WHERE localSiteId == :localSiteId
        AND blockType == :blockType
        AND statsType == :statsType
        AND (:date IS NULL OR date == :date)
        AND (:postId IS NULL OR postId == :postId)
        LIMIT 1
        """)
    fun select(
        localSiteId: Int,
        blockType: BlockType,
        statsType: StatsType,
        date: String? = null,
        postId: Long? = null
    ): StatsBlock?

    @Entity(tableName = "StatsBlock")
    data class StatsBlock(
        @PrimaryKey(autoGenerate = true) var id: Long?,
        @ColumnInfo var localSiteId: Int,
        @ColumnInfo var blockType: BlockType,
        @ColumnInfo var statsType: StatsType,
        @ColumnInfo var date: String?,
        @ColumnInfo var postId: Long,
        @ColumnInfo var json: String

    )
}
