package org.wordpress.android.fluxc.persistence.mappers

import org.wordpress.android.fluxc.model.addons.WCProductAddonModel
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel.AddOnDisplay
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel.AddOnPriceType
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel.AddOnRestrictionsType
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel.AddOnTitleFormat
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel.AddOnType
import org.wordpress.android.fluxc.persistence.entity.AddonEntity

internal fun WCProductAddonModel.toAddonEntity(globalGroupLocalId: Long): AddonEntity {
    return AddonEntity(
            globalGroupLocalId = globalGroupLocalId,
            titleFormat = this.titleFormat?.toLocalEntity(),
            descriptionEnabled = true,
            restrictionsType = this.restrictionsType?.toLocalEntity(),
            adjustPrice = this.adjustPrice?.toBooleanOrNull(),
            priceType = this.priceType?.toLocalEntity(),
            type = this.type?.toLocalEntity(),
            display = this.display?.toLocalEntity(),
            name = this.name,
            description = this.description,
            required = this.required?.toBooleanOrNull(),
            position = this.position?.toIntOrNull(),
            restrictions = this.restrictions?.toBooleanOrNull(),
            price = this.price,
            min = this.min?.toInt(),
            max = this.max?.toInt()
    )
}

internal fun AddOnPriceType.toLocalEntity(): AddonEntity.PriceType {
    return when (this) {
        AddOnPriceType.FlatFee -> AddonEntity.PriceType.FlatFee
        AddOnPriceType.PercentageBased -> AddonEntity.PriceType.PercentageBased
        AddOnPriceType.QuantityBased -> AddonEntity.PriceType.QuantityBased
    }
}

private fun AddOnTitleFormat.toLocalEntity(): AddonEntity.TitleFormat {
    return when (this) {
        AddOnTitleFormat.Label -> AddonEntity.TitleFormat.Label
        AddOnTitleFormat.Heading -> AddonEntity.TitleFormat.Heading
        AddOnTitleFormat.Hide -> AddonEntity.TitleFormat.Hide
    }
}

private fun AddOnRestrictionsType.toLocalEntity(): AddonEntity.RestrictionsType {
    return when (this) {
        AddOnRestrictionsType.AnyText -> AddonEntity.RestrictionsType.AnyText
        AddOnRestrictionsType.OnlyLetters -> AddonEntity.RestrictionsType.OnlyLetters
        AddOnRestrictionsType.OnlyNumbers -> AddonEntity.RestrictionsType.OnlyNumbers
        AddOnRestrictionsType.OnlyLettersNumbers -> AddonEntity.RestrictionsType.OnlyLettersNumbers
        AddOnRestrictionsType.Email -> AddonEntity.RestrictionsType.Email
    }
}

private fun AddOnType.toLocalEntity(): AddonEntity.Type {
    return when (this) {
        AddOnType.MultipleChoice -> AddonEntity.Type.MultipleChoice
        AddOnType.Checkbox -> AddonEntity.Type.Checkbox
        AddOnType.CustomText -> AddonEntity.Type.CustomText
        AddOnType.CustomTextArea -> AddonEntity.Type.CustomTextArea
        AddOnType.FileUpload -> AddonEntity.Type.FileUpload
        AddOnType.CustomPrice -> AddonEntity.Type.CustomPrice
        AddOnType.InputMultiplier -> AddonEntity.Type.InputMultiplier
        AddOnType.Heading -> AddonEntity.Type.Heading
    }
}

private fun AddOnDisplay.toLocalEntity(): AddonEntity.Display {
    return when (this) {
        AddOnDisplay.Dropdown -> AddonEntity.Display.Dropdown
        AddOnDisplay.RadioButton -> AddonEntity.Display.RadioButton
        AddOnDisplay.Images -> AddonEntity.Display.Images
    }
}

private fun String.toBooleanOrNull() = when (this) {
    "1" -> true
    "0" -> false
    else -> null
}
