package org.wordpress.android.fluxc.persistence.mappers

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverview
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverviewEntity
import org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.woo.WooPaymentsDepositsOverviewApiResponse
import javax.inject.Inject

class WooPaymentsDepositsOverviewMapper @Inject constructor() {
    fun mapApiResponseToModel(apiResponse: WooPaymentsDepositsOverviewApiResponse): WooPaymentsDepositsOverview {
        return WooPaymentsDepositsOverview(
            account = mapAccountToModel(apiResponse.account),
            balance = mapBalanceToModel(apiResponse.balance),
            deposit = mapDepositToModel(apiResponse.deposit)
        )
    }

    fun mapEntityToModel(entity: WooPaymentsDepositsOverviewEntity): WooPaymentsDepositsOverview {
        return WooPaymentsDepositsOverview(
            account = mapAccountEntityToModel(entity.account),
            balance = mapBalanceEntityToModel(entity.balance),
            deposit = mapDepositEntityToModel(entity.deposit)
        )
    }

    fun mapModelToEntity(
        model: WooPaymentsDepositsOverview,
        site: SiteModel
    ): WooPaymentsDepositsOverviewEntity {
        return WooPaymentsDepositsOverviewEntity(
            localSiteId = site.localId(),
            account = mapAccountModelToEntity(model.account),
            balance = mapBalanceModelToEntity(model.balance),
            deposit = mapDepositModelToEntity(model.deposit)
        )
    }

    private fun mapAccountModelToEntity(accountModel: WooPaymentsDepositsOverview.Account?): WooPaymentsDepositsOverviewEntity.AccountEntity? {
        return accountModel?.let {
            WooPaymentsDepositsOverviewEntity.AccountEntity(
                defaultCurrency = it.defaultCurrency,
                depositsBlocked = it.depositsBlocked,
                depositsEnabled = it.depositsEnabled,
                depositsSchedule = mapDepositsScheduleModelToEntity(it.depositsSchedule)
            )
        }
    }

    private fun mapDepositsScheduleModelToEntity(depositsScheduleModel: WooPaymentsDepositsOverview.Account.DepositsSchedule?): WooPaymentsDepositsOverviewEntity.AccountEntity.DepositsScheduleEntity? {
        return depositsScheduleModel?.let {
            WooPaymentsDepositsOverviewEntity.AccountEntity.DepositsScheduleEntity(
                delayDays = it.delayDays,
                interval = it.interval
            )
        }
    }

    private fun mapBalanceModelToEntity(balanceModel: WooPaymentsDepositsOverview.Balance?): WooPaymentsDepositsOverviewEntity.BalanceEntity? {
        return balanceModel?.let {
            WooPaymentsDepositsOverviewEntity.BalanceEntity(
                available = mapAvailableModelListToEntity(it.available),
                instant = mapInstantModelListToEntity(it.instant),
                pending = mapPendingModelListToEntity(it.pending)
            )
        }
    }

    private fun mapAvailableModelListToEntity(availableModelList: List<WooPaymentsDepositsOverview.Balance.Available>?): List<WooPaymentsDepositsOverviewEntity.BalanceEntity.AvailableEntity>? {
        return availableModelList?.map { availableModel ->
            availableModel.let {
                WooPaymentsDepositsOverviewEntity.BalanceEntity.AvailableEntity(
                    amount = it.amount,
                    currency = it.currency,
                    sourceTypes = mapSourceTypesModelToEntity(it.sourceTypes)
                )
            }
        }
    }

    private fun mapInstantModelListToEntity(instantModelList: List<WooPaymentsDepositsOverview.Balance.Instant>?): List<WooPaymentsDepositsOverviewEntity.BalanceEntity.InstantEntity>? {
        return instantModelList?.map { instantModel ->
            instantModel.let {
                WooPaymentsDepositsOverviewEntity.BalanceEntity.InstantEntity(
                    amount = it.amount,
                    currency = it.currency,
                    fee = it.fee,
                    feePercentage = it.feePercentage,
                    net = it.net,
                    transactionIds = it.transactionIds
                )
            }
        }
    }

    private fun mapPendingModelListToEntity(pendingModelList: List<WooPaymentsDepositsOverview.Balance.Pending>?): List<WooPaymentsDepositsOverviewEntity.BalanceEntity.PendingEntity>? {
        return pendingModelList?.map { pendingModel ->
            pendingModel.let {
                WooPaymentsDepositsOverviewEntity.BalanceEntity.PendingEntity(
                    amount = it.amount,
                    currency = it.currency,
                    depositsCount = it.depositsCount,
                    sourceTypes = mapSourceTypesModelToEntity(it.sourceTypes)
                )
            }
        }
    }

    private fun mapSourceTypesModelToEntity(sourceTypesModel: WooPaymentsDepositsOverview.Balance.SourceTypes?): WooPaymentsDepositsOverviewEntity.BalanceEntity.SourceTypesEntity? {
        return sourceTypesModel?.let {
            WooPaymentsDepositsOverviewEntity.BalanceEntity.SourceTypesEntity(
                card = it.card
            )
        }
    }

    private fun mapDepositModelToEntity(depositModel: WooPaymentsDepositsOverview.Deposit?): WooPaymentsDepositsOverviewEntity.DepositEntity? {
        return depositModel?.let {
            WooPaymentsDepositsOverviewEntity.DepositEntity(
                lastManualDeposits = mapManualDepositModelListToEntity(it.lastManualDeposits),
                lastPaid = mapLastPaidModelListToEntity(it.lastPaid),
                nextScheduled = mapNextScheduledModelListToEntity(it.nextScheduled)
            )
        }
    }

    private fun mapManualDepositModelListToEntity(manualDepositModelList: List<WooPaymentsDepositsOverview.Deposit.ManualDeposit>?): List<WooPaymentsDepositsOverviewEntity.DepositEntity.ManualDeposit>? {
        return manualDepositModelList?.map { manualDepositModel ->
            manualDepositModel.let {
                WooPaymentsDepositsOverviewEntity.DepositEntity.ManualDeposit(
                    currency = it.currency,
                    date = it.date
                )
            }
        }
    }

    private fun mapLastPaidModelListToEntity(lastPaidModelList: List<WooPaymentsDepositsOverview.Deposit.LastPaid>?): List<WooPaymentsDepositsOverviewEntity.DepositEntity.LastPaidEntity>? {
        return lastPaidModelList?.map { lastPaidModel ->
            lastPaidModel.let {
                WooPaymentsDepositsOverviewEntity.DepositEntity.LastPaidEntity(
                    amount = it.amount,
                    automatic = it.automatic,
                    bankAccount = it.bankAccount,
                    created = it.created,
                    currency = it.currency,
                    date = it.date,
                    fee = it.fee,
                    feePercentage = it.feePercentage,
                    accountId = it.accountId,
                    status = it.status,
                    type = it.type
                )
            }
        }
    }

    private fun mapNextScheduledModelListToEntity(nextScheduledModelList: List<WooPaymentsDepositsOverview.Deposit.NextScheduled>?): List<WooPaymentsDepositsOverviewEntity.DepositEntity.NextScheduledEntity>? {
        return nextScheduledModelList?.map { nextScheduledModel ->
            nextScheduledModel.let {
                WooPaymentsDepositsOverviewEntity.DepositEntity.NextScheduledEntity(
                    amount = it.amount,
                    automatic = it.automatic,
                    bankAccount = it.bankAccount,
                    created = it.created,
                    currency = it.currency,
                    date = it.date,
                    fee = it.fee,
                    feePercentage = it.feePercentage,
                    accountId = it.accountId,
                    status = it.status,
                    type = it.type
                )
            }
        }
    }

    private fun mapAccountEntityToModel(accountEntity: WooPaymentsDepositsOverviewEntity.AccountEntity?): WooPaymentsDepositsOverview.Account? {
        return accountEntity?.let {
            WooPaymentsDepositsOverview.Account(
                defaultCurrency = it.defaultCurrency,
                depositsBlocked = it.depositsBlocked,
                depositsEnabled = it.depositsEnabled,
                depositsSchedule = mapDepositsScheduleEntityToModel(it.depositsSchedule)
            )
        }
    }

    private fun mapDepositsScheduleEntityToModel(depositsScheduleEntity: WooPaymentsDepositsOverviewEntity.AccountEntity.DepositsScheduleEntity?): WooPaymentsDepositsOverview.Account.DepositsSchedule? {
        return depositsScheduleEntity?.let {
            WooPaymentsDepositsOverview.Account.DepositsSchedule(
                delayDays = it.delayDays,
                interval = it.interval
            )
        }
    }

    private fun mapBalanceEntityToModel(balanceEntity: WooPaymentsDepositsOverviewEntity.BalanceEntity?): WooPaymentsDepositsOverview.Balance? {
        return balanceEntity?.let {
            WooPaymentsDepositsOverview.Balance(
                available = mapAvailableEntityListToModel(it.available),
                instant = mapInstantEntityListToModel(it.instant),
                pending = mapPendingEntityListToModel(it.pending)
            )
        }
    }

    private fun mapAvailableEntityListToModel(availableEntityList: List<WooPaymentsDepositsOverviewEntity.BalanceEntity.AvailableEntity>?): List<WooPaymentsDepositsOverview.Balance.Available>? {
        return availableEntityList?.map { availableEntity ->
            availableEntity.let {
                WooPaymentsDepositsOverview.Balance.Available(
                    amount = it.amount,
                    currency = it.currency,
                    sourceTypes = mapSourceTypesEntityToModel(it.sourceTypes)
                )
            }
        }
    }

    private fun mapInstantEntityListToModel(instantEntityList: List<WooPaymentsDepositsOverviewEntity.BalanceEntity.InstantEntity>?): List<WooPaymentsDepositsOverview.Balance.Instant>? {
        return instantEntityList?.map { instantEntity ->
            instantEntity.let {
                WooPaymentsDepositsOverview.Balance.Instant(
                    amount = it.amount,
                    currency = it.currency,
                    fee = it.fee,
                    feePercentage = it.feePercentage,
                    net = it.net,
                    transactionIds = it.transactionIds
                )
            }
        }
    }

    private fun mapPendingEntityListToModel(pendingEntityList: List<WooPaymentsDepositsOverviewEntity.BalanceEntity.PendingEntity>?): List<WooPaymentsDepositsOverview.Balance.Pending>? {
        return pendingEntityList?.map { pendingEntity ->
            pendingEntity.let {
                WooPaymentsDepositsOverview.Balance.Pending(
                    amount = it.amount,
                    currency = it.currency,
                    depositsCount = it.depositsCount,
                    sourceTypes = mapSourceTypesEntityToModel(it.sourceTypes)
                )
            }
        }
    }

    private fun mapSourceTypesEntityToModel(sourceTypesEntity: WooPaymentsDepositsOverviewEntity.BalanceEntity.SourceTypesEntity?): WooPaymentsDepositsOverview.Balance.SourceTypes? {
        return sourceTypesEntity?.let {
            WooPaymentsDepositsOverview.Balance.SourceTypes(
                card = it.card
            )
        }
    }

    private fun mapDepositEntityToModel(depositEntity: WooPaymentsDepositsOverviewEntity.DepositEntity?): WooPaymentsDepositsOverview.Deposit? {
        return depositEntity?.let {
            WooPaymentsDepositsOverview.Deposit(
                lastManualDeposits = mapManualDepositEntityListToModel(it.lastManualDeposits),
                lastPaid = mapLastPaidEntityListToModel(it.lastPaid),
                nextScheduled = mapNextScheduledEntityListToModel(it.nextScheduled)
            )
        }
    }

    private fun mapManualDepositEntityListToModel(manualDepositEntityList: List<WooPaymentsDepositsOverviewEntity.DepositEntity.ManualDeposit>?): List<WooPaymentsDepositsOverview.Deposit.ManualDeposit>? {
        return manualDepositEntityList?.map { manualDepositEntity ->
            manualDepositEntity.let {
                WooPaymentsDepositsOverview.Deposit.ManualDeposit(
                    currency = it.currency,
                    date = it.date
                )
            }
        }
    }

    private fun mapLastPaidEntityListToModel(lastPaidEntityList: List<WooPaymentsDepositsOverviewEntity.DepositEntity.LastPaidEntity>?): List<WooPaymentsDepositsOverview.Deposit.LastPaid>? {
        return lastPaidEntityList?.map { lastPaidEntity ->
            lastPaidEntity.let {
                WooPaymentsDepositsOverview.Deposit.LastPaid(
                    amount = it.amount,
                    automatic = it.automatic,
                    bankAccount = it.bankAccount,
                    created = it.created,
                    currency = it.currency,
                    date = it.date,
                    fee = it.fee,
                    feePercentage = it.feePercentage,
                    accountId = it.accountId,
                    status = it.status,
                    type = it.type
                )
            }
        }
    }

    private fun mapNextScheduledEntityListToModel(nextScheduledEntityList: List<WooPaymentsDepositsOverviewEntity.DepositEntity.NextScheduledEntity>?): List<WooPaymentsDepositsOverview.Deposit.NextScheduled>? {
        return nextScheduledEntityList?.map { nextScheduledEntity ->
            nextScheduledEntity.let {
                WooPaymentsDepositsOverview.Deposit.NextScheduled(
                    amount = it.amount,
                    automatic = it.automatic,
                    bankAccount = it.bankAccount,
                    created = it.created,
                    currency = it.currency,
                    date = it.date,
                    fee = it.fee,
                    feePercentage = it.feePercentage,
                    accountId = it.accountId,
                    status = it.status,
                    type = it.type
                )
            }
        }
    }

    private fun mapAccountToModel(account: WooPaymentsDepositsOverviewApiResponse.Account?): WooPaymentsDepositsOverview.Account? {
        return account?.let {
            WooPaymentsDepositsOverview.Account(
                defaultCurrency = it.defaultCurrency,
                depositsBlocked = it.depositsBlocked,
                depositsEnabled = it.depositsEnabled,
                depositsSchedule = mapDepositsScheduleToModel(it.depositsSchedule)
            )
        }
    }

    private fun mapDepositsScheduleToModel(depositsSchedule: WooPaymentsDepositsOverviewApiResponse.Account.DepositsSchedule?): WooPaymentsDepositsOverview.Account.DepositsSchedule? {
        return depositsSchedule?.let {
            WooPaymentsDepositsOverview.Account.DepositsSchedule(
                delayDays = it.delayDays,
                interval = it.interval
            )
        }
    }

    private fun mapBalanceToModel(balance: WooPaymentsDepositsOverviewApiResponse.Balance?): WooPaymentsDepositsOverview.Balance? {
        return balance?.let {
            WooPaymentsDepositsOverview.Balance(
                available = mapAvailableListToModel(it.available),
                instant = mapInstantListToModel(it.instant),
                pending = mapPendingListToModel(it.pending)
            )
        }
    }

    private fun mapAvailableListToModel(availableList: List<WooPaymentsDepositsOverviewApiResponse.Balance.Available>?): List<WooPaymentsDepositsOverview.Balance.Available>? {
        return availableList?.map { available ->
            available.let {
                WooPaymentsDepositsOverview.Balance.Available(
                    amount = it.amount,
                    currency = it.currency,
                    sourceTypes = mapSourceTypesToModel(it.sourceTypes)
                )
            }
        }
    }

    private fun mapInstantListToModel(instantList: List<WooPaymentsDepositsOverviewApiResponse.Balance.Instant>?): List<WooPaymentsDepositsOverview.Balance.Instant>? {
        return instantList?.map { instant ->
            instant.let {
                WooPaymentsDepositsOverview.Balance.Instant(
                    amount = it.amount,
                    currency = it.currency,
                    fee = it.fee,
                    feePercentage = it.feePercentage,
                    net = it.net,
                    transactionIds = it.transactionIds
                )
            }
        }
    }

    private fun mapPendingListToModel(pendingList: List<WooPaymentsDepositsOverviewApiResponse.Balance.Pending>?): List<WooPaymentsDepositsOverview.Balance.Pending>? {
        return pendingList?.map { pending ->
            pending.let {
                WooPaymentsDepositsOverview.Balance.Pending(
                    amount = it.amount,
                    currency = it.currency,
                    depositsCount = it.depositsCount,
                    sourceTypes = mapSourceTypesToModel(it.sourceTypes)
                )
            }
        }
    }

    private fun mapSourceTypesToModel(sourceTypes: WooPaymentsDepositsOverviewApiResponse.Balance.SourceTypes?): WooPaymentsDepositsOverview.Balance.SourceTypes? {
        return sourceTypes?.let {
            WooPaymentsDepositsOverview.Balance.SourceTypes(
                card = it.card
            )
        }
    }

    private fun mapDepositToModel(deposit: WooPaymentsDepositsOverviewApiResponse.Deposit?): WooPaymentsDepositsOverview.Deposit? {
        return deposit?.let {
            WooPaymentsDepositsOverview.Deposit(
                lastManualDeposits = mapManualDepositListToModel(it.lastManualDeposits),
                lastPaid = mapLastPaidListToModel(it.lastPaid),
                nextScheduled = mapNextScheduledListToModel(it.nextScheduled)
            )
        }
    }

    private fun mapManualDepositListToModel(manualDepositList: List<WooPaymentsDepositsOverviewApiResponse.Deposit.ManualDeposit>?): List<WooPaymentsDepositsOverview.Deposit.ManualDeposit>? {
        return manualDepositList?.map { manualDeposit ->
            manualDeposit.let {
                WooPaymentsDepositsOverview.Deposit.ManualDeposit(
                    currency = it.currency,
                    date = it.date
                )
            }
        }
    }

    private fun mapLastPaidListToModel(lastPaidList: List<WooPaymentsDepositsOverviewApiResponse.Deposit.LastPaid>?): List<WooPaymentsDepositsOverview.Deposit.LastPaid>? {
        return lastPaidList?.map { lastPaid ->
            lastPaid.let {
                WooPaymentsDepositsOverview.Deposit.LastPaid(
                    amount = it.amount,
                    automatic = it.automatic,
                    bankAccount = it.bankAccount,
                    created = it.created,
                    currency = it.currency,
                    date = it.date,
                    fee = it.fee,
                    feePercentage = it.feePercentage,
                    accountId = it.accountId,
                    status = it.status,
                    type = it.type
                )
            }
        }
    }

    private fun mapNextScheduledListToModel(nextScheduledList: List<WooPaymentsDepositsOverviewApiResponse.Deposit.NextScheduled>?): List<WooPaymentsDepositsOverview.Deposit.NextScheduled>? {
        return nextScheduledList?.map { nextScheduled ->
            nextScheduled.let {
                WooPaymentsDepositsOverview.Deposit.NextScheduled(
                    amount = it.amount,
                    automatic = it.automatic,
                    bankAccount = it.bankAccount,
                    created = it.created,
                    currency = it.currency,
                    date = it.date,
                    fee = it.fee,
                    feePercentage = it.feePercentage,
                    accountId = it.accountId,
                    status = it.status,
                    type = it.type
                )
            }
        }
    }
}