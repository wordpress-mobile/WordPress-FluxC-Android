package org.wordpress.android.fluxc.model.payments.woo

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

@Entity(tableName = "WooPaymentsDepositsOverview")
data class WooPaymentsDepositsOverviewEntity(
    @PrimaryKey(autoGenerate = false)
    val localSiteId: LocalId,

    val account: AccountEntity?,
    val balance: BalanceEntity?,
    val deposit: DepositEntity?
) {
    @Entity
    data class AccountEntity(
        val defaultCurrency: String?,
        val depositsBlocked: Boolean?,
        val depositsEnabled: Boolean?,
        val depositsSchedule: DepositsScheduleEntity?
    ) {
        @Entity
        data class DepositsScheduleEntity(
            val delayDays: Int?,
            val interval: String?
        )
    }

    @Entity
    data class BalanceEntity(
        val available: List<AvailableEntity>?,
        val instant: List<InstantEntity>?,
        val pending: List<PendingEntity>?
    ) {
        @Entity
        data class AvailableEntity(
            val amount: Int?,
            val currency: String?,
            val sourceTypes: SourceTypesEntity?
        )

        @Entity
        data class InstantEntity(
            val amount: Int?,
            val currency: String?,
            val fee: Int?,
            val feePercentage: Double?,
            val net: Int?,
            val transactionIds: List<String>?
        )

        @Entity
        data class PendingEntity(
            val amount: Int?,
            val currency: String?,
            val depositsCount: Int?,
            val sourceTypes: SourceTypesEntity?
        )

        @Entity
        data class SourceTypesEntity(
            val card: Int?
        )
    }

    @Entity
    data class DepositEntity(
        val lastManualDeposits: List<ManualDeposit>?,
        val lastPaid: List<LastPaidEntity>?,
        val nextScheduled: List<NextScheduledEntity>?
    ) {
        @Entity
        data class LastPaidEntity(
            val amount: Int?,
            val automatic: Boolean?,
            val bankAccount: String?,
            val created: Int?,
            val currency: String?,
            val date: Long?,
            val fee: Int?,
            val feePercentage: Int?,
            val accountId: String?,
            val status: String?,
            val type: String?
        )

        @Entity
        data class NextScheduledEntity(
            val amount: Int?,
            val automatic: Boolean?,
            val bankAccount: String?,
            val created: Int?,
            val currency: String?,
            val date: Long?,
            val fee: Int?,
            val feePercentage: Int?,
            val accountId: String?,
            val status: String?,
            val type: String?
        )

        @Entity
        data class ManualDeposit(
            val currency: String?,
            val date: String?
        )
    }
}
