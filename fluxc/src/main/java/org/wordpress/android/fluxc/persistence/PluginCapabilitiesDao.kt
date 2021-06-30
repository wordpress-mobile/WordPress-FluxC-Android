package org.wordpress.android.fluxc.persistence

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Dao
abstract class PluginCapabilitiesDao {
    @Query("SELECT * FROM PluginCapabilities WHERE localSiteId = :siteId")
    abstract fun getBySiteId(siteId: Int): PluginCapabilities?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(type: PluginCapabilities): Long

    @Entity(tableName = "PluginCapabilities")
    data class PluginCapabilities(
        @PrimaryKey
        var localSiteId: Int,
        var autoUpdateFiles: Boolean = false
    )
}
