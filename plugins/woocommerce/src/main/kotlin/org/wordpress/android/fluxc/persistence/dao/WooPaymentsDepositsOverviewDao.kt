package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverviewComposedEntities
import org.wordpress.android.fluxc.persistence.entity.DepositType
import org.wordpress.android.fluxc.persistence.entity.DepositType.LAST_PAID
import org.wordpress.android.fluxc.persistence.entity.DepositType.NEXT_SCHEDULED
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsDepositEntity
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsDepositsOverviewEntity
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsManualDepositEntity

@Dao
interface WooPaymentsDepositsOverviewDao {
    @Transaction
    suspend fun getOverviewComposed(localSiteId: LocalId) =
        WooPaymentsDepositsOverviewComposedEntities(
            overview = getWooPaymentsDepositsOverviewEntity(localSiteId),
            lastPaidDeposits = getDeposits(localSiteId, LAST_PAID),
            nextScheduledDeposits = getDeposits(localSiteId, NEXT_SCHEDULED),
            lastManualDeposits = getWooPaymentsManualDeposits(localSiteId)
        )

    @Query("SELECT * FROM WooPaymentsDepositsOverview WHERE localSiteId = :localSiteId")
    suspend fun getWooPaymentsDepositsOverviewEntity(localSiteId: LocalId): WooPaymentsDepositsOverviewEntity

    @Query(
        """
        SELECT * FROM WooPaymentsDeposits 
        WHERE localSiteId = :localSiteId AND depositType = :type
    """
    )
    fun getDeposits(
        localSiteId: LocalId,
        type: DepositType
    ): List<WooPaymentsDepositEntity>

    @Query(
        """
        SELECT * FROM WooPaymentsDeposits 
        WHERE localSiteId = :localSiteId
    """
    )
    fun getWooPaymentsManualDeposits(localSiteId: LocalId): List<WooPaymentsManualDepositEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverview(overview: WooPaymentsDepositsOverviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeposit(deposit: WooPaymentsDepositEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManualDeposit(manualDeposit: WooPaymentsManualDepositEntity)

    @Transaction
    suspend fun insertOverviewAll(
        localSiteId: LocalId,
        overviewEntity: WooPaymentsDepositsOverviewEntity,
        lastPaidDepositsEntities: List<WooPaymentsDepositEntity>,
        nextScheduledDepositsEntities: List<WooPaymentsDepositEntity>,
        manualDepositEntities: List<WooPaymentsManualDepositEntity>
    ) {
        insertOverview(overviewEntity)

        lastPaidDepositsEntities.forEach { insertDeposit(it) }
        nextScheduledDepositsEntities.forEach { insertDeposit(it) }
        manualDepositEntities.forEach { insertManualDeposit(it) }
    }

    @Query("DELETE FROM WooPaymentsDepositsOverview WHERE localSiteId = :localSiteId")
    suspend fun delete(localSiteId: LocalId)
}

