package org.wordpress.android.fluxc.model.payments.woo

data class WooPaymentsDepositsOverview(
    val account: Account?,
    val balance: Balance?,
    val deposit: Deposit?
) {
    data class Account(
        val defaultCurrency: String?,
        val depositsBlocked: Boolean?,
        val depositsEnabled: Boolean?,
        val depositsSchedule: DepositsSchedule?
    ) {
        data class DepositsSchedule(
            val delayDays: Int?,
            val interval: String?
        )
    }

    data class Balance(
        val available: List<Available>?,
        val instant: List<Instant>?,
        val pending: List<Pending>?
    ) {
        data class Available(
            val amount: Int?,
            val currency: String?,
            val sourceTypes: SourceTypes?
        )

        data class Instant(
            val amount: Int?,
            val currency: String?,
            val fee: Int?,
            val feePercentage: Double?,
            val net: Int?,
            val transactionIds: List<String>?
        )

        data class Pending(
            val amount: Int?,
            val currency: String?,
            val depositsCount: Int?,
            val sourceTypes: SourceTypes?
        )

        data class SourceTypes(
            val card: Int?
        )
    }

    data class Deposit(
        val lastManualDeposits: List<ManualDeposit>?,
        val lastPaid: List<LastPaid>?,
        val nextScheduled: List<NextScheduled>?
    ) {
        data class LastPaid(
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

        data class NextScheduled(
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

        data class ManualDeposit(
            val currency: String?,
            val date: String?
        )
    }
}
