package org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.woo

import com.google.gson.annotations.SerializedName

data class WooPaymentsDepositsOverviewApiResponse(
    @SerializedName("account")
    val account: Account?,
    @SerializedName("balance")
    val balance: Balance?,
    @SerializedName("deposit")
    val deposit: Deposit?
) {
    data class Account(
        @SerializedName("default_currency")
        val defaultCurrency: String?,
        @SerializedName("deposits_blocked")
        val depositsBlocked: Boolean?,
        @SerializedName("deposits_enabled")
        val depositsEnabled: Boolean?,
        @SerializedName("deposits_schedule")
        val depositsSchedule: DepositsSchedule?
    ) {
        data class DepositsSchedule(
            @SerializedName("delay_days")
            val delayDays: Int?,
            @SerializedName("interval")
            val interval: String?
        )
    }

    data class Balance(
        @SerializedName("available")
        val available: List<Available?>?,
        @SerializedName("instant")
        val instant: List<Instant?>?,
        @SerializedName("pending")
        val pending: List<Pending?>?
    ) {
        data class Available(
            @SerializedName("amount")
            val amount: Int?,
            @SerializedName("currency")
            val currency: String?,
            @SerializedName("source_types")
            val sourceTypes: SourceTypes?
        ) {
            data class SourceTypes(
                @SerializedName("card")
                val card: Int?
            )
        }

        data class Instant(
            @SerializedName("amount")
            val amount: Int?,
            @SerializedName("currency")
            val currency: String?,
            @SerializedName("fee")
            val fee: Int?,
            @SerializedName("fee_percentage")
            val feePercentage: Double?,
            @SerializedName("net")
            val net: Int?,
            @SerializedName("transaction_ids")
            val transactionIds: List<String?>?
        )

        data class Pending(
            @SerializedName("amount")
            val amount: Int?,
            @SerializedName("currency")
            val currency: String?,
            @SerializedName("deposits_count")
            val depositsCount: Int?,
            @SerializedName("source_types")
            val sourceTypes: SourceTypes?
        ) {
            data class SourceTypes(
                @SerializedName("card")
                val card: Int?
            )
        }
    }

    data class Deposit(
        @SerializedName("last_manual_deposits")
        val lastManualDeposits: List<Any?>?,
        @SerializedName("last_paid")
        val lastPaid: List<LastPaid?>?,
        @SerializedName("next_scheduled")
        val nextScheduled: List<NextScheduled?>?
    ) {
        data class LastPaid(
            @SerializedName("amount")
            val amount: Int?,
            @SerializedName("automatic")
            val automatic: Boolean?,
            @SerializedName("bankAccount")
            val bankAccount: String?,
            @SerializedName("created")
            val created: Int?,
            @SerializedName("currency")
            val currency: String?,
            @SerializedName("date")
            val date: Long?,
            @SerializedName("fee")
            val fee: Int?,
            @SerializedName("fee_percentage")
            val feePercentage: Int?,
            @SerializedName("id")
            val accountId: String?,
            @SerializedName("status")
            val status: String?,
            @SerializedName("type")
            val type: String?
        )

        data class NextScheduled(
            @SerializedName("amount")
            val amount: Int?,
            @SerializedName("automatic")
            val automatic: Boolean?,
            @SerializedName("bankAccount")
            val bankAccount: String?,
            @SerializedName("created")
            val created: Int?,
            @SerializedName("currency")
            val currency: String?,
            @SerializedName("date")
            val date: Long?,
            @SerializedName("fee")
            val fee: Int?,
            @SerializedName("fee_percentage")
            val feePercentage: Int?,
            @SerializedName("id")
            val accountId: String?,
            @SerializedName("status")
            val status: String?,
            @SerializedName("type")
            val type: String?
        )

        data class ManualDeposit(
            @SerializedName("currency")
            val currency: String?,
            @SerializedName("date")
            val date: String?
        )
    }
}