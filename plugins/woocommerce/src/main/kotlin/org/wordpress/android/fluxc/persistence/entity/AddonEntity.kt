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
    val type: Type,
    val display: Display? = null,
    val name: String,
    val titleFormat: TitleFormat,
    val description: String,
    val descriptionEnabled: Boolean,
    val required: Boolean,
    val position: Int,
    val restrictions: Restrictions? = null,
    val priceAdjusted: Boolean? = null,
    val priceType: PriceType? = null,
    val price: String? = null,
    val min: Long? = null,
    val max: Long? = null
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
        Select,
        RadioButton,
        Images
    }

    enum class TitleFormat {
        Label,
        Heading,
        Hide
    }

    enum class Restrictions {
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
