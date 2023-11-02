package org.wordpress.android.fluxc.model.payments.woo

import org.wordpress.android.fluxc.persistence.entity.WooPaymentsDepositEntity
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsDepositsOverviewEntity
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsManualDepositEntity

data class WooPaymentsDepositsOverviewComposedEntities(
    val overview: WooPaymentsDepositsOverviewEntity,
    val lastPaidDeposits: List<WooPaymentsDepositEntity>?,
    val nextScheduledDeposits: List<WooPaymentsDepositEntity>?,
    val lastManualDeposits: List<WooPaymentsManualDepositEntity>?
)