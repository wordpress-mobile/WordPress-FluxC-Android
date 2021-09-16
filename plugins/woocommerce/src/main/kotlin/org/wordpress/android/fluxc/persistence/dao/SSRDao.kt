package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.WCSSRModel
import org.wordpress.android.fluxc.persistence.entity.SSREntity

@Dao
abstract class SSRDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSSR(ssrEntity: SSREntity)

    @Query("SELECT * FROM SSREntity WHERE remoteSiteId = :remoteSiteId LIMIT 1")
    abstract fun observeSSRForSite(remoteSiteId: Long): Flow<WCSSRModel>
}
