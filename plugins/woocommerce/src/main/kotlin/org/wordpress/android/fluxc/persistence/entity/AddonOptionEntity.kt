package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalPriceType

@Entity(
        foreignKeys = [ForeignKey(
                entity = AddonEntity::class,
                parentColumns = ["addonLocalId"],
                childColumns = ["addonLocalId"],
                onDelete = CASCADE
        )]
)
data class AddonOptionEntity(
    @PrimaryKey(autoGenerate = true) val addonOptionLocalId: Long = 0,
    val addonLocalId: Long,
    val priceType: LocalPriceType,
    val label: String?,
    val price: String?,
    val image: String?
)
