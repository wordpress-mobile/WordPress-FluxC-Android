package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey
import androidx.room.Relation

data class AddonWithOptions(
    @Embedded val addon: AddonEntity,
    @Relation(
            parentColumn = "addonLocalId",
            entityColumn = "addonLocalId"
    )
    val options: List<AddonOptionEntity>
)

@Entity(
        foreignKeys = [ForeignKey(
                entity = GlobalAddonGroupEntity::class,
                parentColumns = ["globalGroupLocalId"],
                childColumns = ["globalGroupLocalId"],
                onDelete = CASCADE
        )]
)
data class AddonEntity(
    @PrimaryKey(autoGenerate = true) val addonLocalId: Long = 0,
    val globalGroupLocalId: Long,
    val type: Type?,
    val display: Display?,
    val name: String?,
    val titleFormat: TitleFormat?,
    val descriptionEnabled: Boolean?,
    val description: String?,
    val required: Boolean?,
    val position: Int?,
    val restrictions: Boolean?,
    val restrictionsType: RestrictionsType?,
    val adjustPrice: Boolean?,
    val priceType: PriceType?,
    val price: String?,
    val min: Int?,
    val max: Int?
) {
    enum class Type {
        MultipleChoice,
        Checkbox,
        CustomText,
        CustomTextArea,
        FileUpload,
        CustomPrice,
        InputMultiplier,
        Heading
    }

    enum class Display {
        Dropdown,
        RadioButton,
        Images
    }

    enum class TitleFormat {
        Label,
        Heading,
        Hide
    }

    enum class RestrictionsType {
        AnyText,
        OnlyLetters,
        OnlyNumbers,
        OnlyLettersNumbers,
        Email
    }

    enum class PriceType {
        FlatFee,
        QuantityBased,
        PercentageBased
    }
}
