package org.wordpress.android.fluxc.model.taxes

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "TaxRate")
data class TaxRateEntity (
    @PrimaryKey val localSiteId: Long,
    val id: Long,
)
