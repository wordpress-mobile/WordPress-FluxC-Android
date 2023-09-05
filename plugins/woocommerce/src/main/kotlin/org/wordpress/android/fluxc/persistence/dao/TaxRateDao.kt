package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import org.wordpress.android.fluxc.model.taxes.TaxRateEntity

@Dao
interface TaxRateDao {
    @Query("SELECT * FROM TaxRate WHERE localSiteId = :localSiteId")
    suspend fun getTaxRateList(localSiteId: Long): List<TaxRateEntity>
}
