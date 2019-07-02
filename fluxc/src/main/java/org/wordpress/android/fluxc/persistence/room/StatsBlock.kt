package org.wordpress.android.fluxc.persistence.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType

@Entity(tableName = "StatsBlock")
data class StatsBlock(
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    @ColumnInfo var localSiteId: Int,
    @ColumnInfo var blockType: BlockType,
    @ColumnInfo var statsType: StatsType,
    @ColumnInfo var date: String?,
    @ColumnInfo var postId: Long?,
    @ColumnInfo var json: String
)
