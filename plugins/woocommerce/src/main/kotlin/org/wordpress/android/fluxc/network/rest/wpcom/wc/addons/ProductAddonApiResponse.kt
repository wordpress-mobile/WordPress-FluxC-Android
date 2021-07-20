package org.wordpress.android.fluxc.network.rest.wpcom.wc.addons

import org.wordpress.android.fluxc.network.Response

class ProductAddonApiResponse : Response {
    val type: AddOnType? = null
    val display: AddOnDisplay? = null
    val name: String? = null
    val titleFormat: AddOnTitleFormat? = null
    val descriptionEnabled: String? = null
    val description: String? = null
    val required: String? = null
    val position: String? = null
    val restrictions: String? = null
    val restrictionsType: AddOnRestrictionsType? = null
    val adjustPrice: String? = null
    val priceType: AddOnPriceType? = null
    val price: String? = null
    val min: String? = null
    val max: String? = null
    val options: Array<ProductAddonOptionApiResponse>? = null

    enum class AddOnType {
        MultipleChoice,
        Checkbox,
        CustomText,
        CustomTextArea,
        FileUpload,
        CustomPrice,
        InputMultiplier,
        Heading
    }

    enum class AddOnDisplay {
        Dropdown,
        RadioButton,
        Images
    }

    enum class AddOnTitleFormat {
        Label,
        Heading,
        Hide
    }

    enum class AddOnRestrictionsType {
        AnyText,
        OnlyLetters,
        OnlyNumbers,
        OnlyLettersNumbers,
        Email
    }

    enum class AddOnPriceType {
        FlatFee,
        QuantityBased,
        PercentageBased
    }
}