package org.wordpress.android.fluxc.model.taxes

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

@Entity(tableName = "TaxBasedOnSetting")
data class TaxBasedOnSettingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val localSiteId: LocalId,
    val selectedOption: String,
    val availableOptions: String,
)
