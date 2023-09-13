package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.taxes.TaxRateEntity

@Dao
interface TaxRateDao {
    @Query("SELECT * FROM TaxRate WHERE localSiteId = :localSiteId")
    suspend fun getTaxRates(localSiteId: LocalId): List<TaxRateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(taxRate: TaxRateEntity): Long

    @Query("DELETE FROM TaxRate WHERE localSiteId = :localSiteId")
    suspend fun deleteAll(localSiteId: LocalId)

    @Transaction
    @Query("SELECT * FROM TaxRate WHERE localSiteId = :localSiteId AND id = :taxRateId")
    suspend fun getTaxRate(localSiteId: LocalId, taxRateId: RemoteId): TaxRateEntity?
}
