package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.wordpress.android.fluxc.persistence.entity.SSREntity

@Dao
abstract class SSRDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSSR(ssrEntity: SSREntity): Long

    @Query("SELECT * FROM SSREntity WHERE localSiteId = :localSiteId")
    abstract suspend fun getSSRbySite(localSiteId: Long)
}
