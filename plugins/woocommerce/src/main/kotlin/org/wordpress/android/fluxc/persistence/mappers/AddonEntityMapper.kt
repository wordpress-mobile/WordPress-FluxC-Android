package org.wordpress.android.fluxc.persistence.mappers

import org.wordpress.android.fluxc.model.addons.WCProductAddonModel
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel.AddOnDisplay
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel.AddOnPriceType
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel.AddOnRestrictionsType
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel.AddOnTitleFormat
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel.AddOnType
import org.wordpress.android.fluxc.persistence.entity.AddonEntity

data class ProductBasedIdentification(
    val productRemoteId: Long,
    val siteRemoteId: Long
)

internal fun WCProductAddonModel.toAddonEntity(
    globalGroupLocalId: Long? = null,
    productBasedIdentification: ProductBasedIdentification? = null
): AddonEntity {
    if (globalGroupLocalId == null && productBasedIdentification == null) {
        throw IllegalStateException("Addon has to be identified with a Group or a Product")
    }

    return AddonEntity(
            globalGroupLocalId = globalGroupLocalId,
            productRemoteId = productBasedIdentification?.productRemoteId,
            siteRemoteId = productBasedIdentification?.siteRemoteId,
            titleFormat = this.titleFormat.toLocalEntity(),
            restrictions = this.restrictionsType?.toLocalEntity(),
            priceAdjusted = this.adjustPrice.asBoolean(),
            priceType = this.priceType?.toLocalEntity(),
            type = this.type.toLocalEntity(),
            display = this.display?.toLocalEntity(),
            name = this.name,
            description = this.description,
            descriptionEnabled = this.descriptionEnabled.asBoolean(),
            required = this.required.asBoolean(),
            position = this.position,
            price = this.price,
            min = this.min,
            max = this.max
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

private fun AddOnRestrictionsType.toLocalEntity(): AddonEntity.Restrictions {
    return when (this) {
        AddOnRestrictionsType.AnyText -> AddonEntity.Restrictions.AnyText
        AddOnRestrictionsType.OnlyLetters -> AddonEntity.Restrictions.OnlyLetters
        AddOnRestrictionsType.OnlyNumbers -> AddonEntity.Restrictions.OnlyNumbers
        AddOnRestrictionsType.OnlyLettersNumbers -> AddonEntity.Restrictions.OnlyLettersNumbers
        AddOnRestrictionsType.Email -> AddonEntity.Restrictions.Email
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
        AddOnDisplay.Select -> AddonEntity.Display.Select
        AddOnDisplay.RadioButton -> AddonEntity.Display.RadioButton
        AddOnDisplay.Images -> AddonEntity.Display.Images
    }
}

private fun Int.asBoolean(): Boolean {
    return when (this) {
        0 -> false
        1 -> true
        else -> false
    }
}
