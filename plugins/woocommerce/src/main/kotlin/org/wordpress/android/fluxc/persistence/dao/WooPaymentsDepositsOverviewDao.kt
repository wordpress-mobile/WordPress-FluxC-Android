package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverviewEntity

@Dao
interface WooPaymentsDepositsOverviewDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(depositsOverview: WooPaymentsDepositsOverviewEntity)

    @Query("SELECT * FROM WooPaymentsDepositsOverview")
    suspend fun getDepositsOverviewAll(): WooPaymentsDepositsOverviewEntity?

    @Query("DELETE FROM WooPaymentsDepositsOverview")
    suspend fun deleteDepositsOverviewAll()
}

