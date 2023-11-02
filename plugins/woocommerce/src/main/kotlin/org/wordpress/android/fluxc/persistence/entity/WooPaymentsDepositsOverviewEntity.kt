package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey
import org.wordpress.android.fluxc.model.LocalOrRemoteId

@Entity(tableName = "WooPaymentsDepositsOverview")
data class WooPaymentsDepositsOverviewEntity(
    @PrimaryKey(autoGenerate = false)
    val localSiteId: LocalOrRemoteId.LocalId,

    @Embedded
    val account: WooPaymentsAccountDepositSummaryEntity?
)

data class WooPaymentsAccountDepositSummaryEntity(
    val depositsEnabled: Boolean?,
    val depositsBlocked: Boolean?,
    @Embedded
    val depositsSchedule: WooPaymentsDepositsSchedule?,
    val defaultCurrency: String?
)

data class WooPaymentsDepositsSchedule(
    val delayDays: Int?,
    val interval: String?
)

@Entity(
    tableName = "WooPaymentsDeposits",
    foreignKeys = [ForeignKey(
        entity = WooPaymentsDepositsOverviewEntity::class,
        parentColumns = ["localSiteId"],
        childColumns = ["localSiteId"],
        onDelete = CASCADE
    )]
)
data class WooPaymentsDepositEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val localSiteId: LocalOrRemoteId.LocalId,

    val depositId: String?,
    val date: Long?,
    val type: String?,
    val amount: Int?,
    val status: String?,
    val bankAccount: String?,
    val currency: String?,
    val automatic: Boolean?,
    val fee: Long?,
    val feePercentage: Double?,
    val created: Long?,

    val depositType: DepositType,
)

enum class DepositType {
    LAST_PAID,
    NEXT_SCHEDULED
}

@Entity(
    tableName = "WooPaymentsManualDeposits",
    foreignKeys = [ForeignKey(
        entity = WooPaymentsDepositsOverviewEntity::class,
        parentColumns = ["localSiteId"],
        childColumns = ["localSiteId"],
        onDelete = CASCADE
    )]
)
data class WooPaymentsManualDepositEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val localSiteId: LocalOrRemoteId.LocalId,

    val currency: String?,
    val date: Long?
)