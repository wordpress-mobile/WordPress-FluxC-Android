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
        """)
    fun selectAll(
        localSiteId: Int,
        blockType: BlockType,
        statsType: StatsType,
        date: String? = null,
        postId: Long? = null
    ): List<StatsBlock>

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
        @ColumnInfo var postId: Long?,
        @ColumnInfo var json: String

    )
}
