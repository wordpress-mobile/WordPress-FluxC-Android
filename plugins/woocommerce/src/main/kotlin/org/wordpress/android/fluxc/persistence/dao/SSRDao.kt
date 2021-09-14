package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.wordpress.android.fluxc.model.WCSSRModel
import org.wordpress.android.fluxc.persistence.entity.SSREntity

@Dao
abstract class SSRDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSSR(ssrEntity: SSREntity): Long

    @Query("SELECT * FROM SSREntity WHERE localSiteId = :localSiteId LIMIT 1")
    abstract suspend fun getSSRbySite(localSiteId: Int): WCSSRModel
}
