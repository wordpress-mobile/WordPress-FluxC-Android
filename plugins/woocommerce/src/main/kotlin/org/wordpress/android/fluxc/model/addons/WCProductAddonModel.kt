package org.wordpress.android.fluxc.model.addons

import com.google.gson.annotations.SerializedName

data class WCProductAddonModel(
    @SerializedName("title_format")
    val titleFormat: AddOnTitleFormat,
    @SerializedName("description_enable")
    val descriptionEnabled: Int,
    @SerializedName("restrictions_type")
    val restrictionsType: AddOnRestrictionsType? = null,
    @SerializedName("adjust_price")
    val adjustPrice: Int,
    @SerializedName("price_type")
    val priceType: AddOnPriceType? = null,

    val type: AddOnType,
    val display: AddOnDisplay? = null,
    val name: String,
    val description: String,
    val required: Int,
    val position: Int,
    val price: String? = null,
    val min: Long,
    val max: Long,
    val options: List<ProductAddonOption>? = null
) {
    enum class AddOnType {
        @SerializedName("multiple_choice") MultipleChoice,
        @SerializedName("checkbox") Checkbox,
        @SerializedName("custom_text") CustomText,
        @SerializedName("custom_textarea") CustomTextArea,
        @SerializedName("file_upload") FileUpload,
        @SerializedName("custom_price") CustomPrice,
        @SerializedName("input_multiplier") InputMultiplier,
        @SerializedName("heading") Heading
    }

    enum class AddOnDisplay {
        @SerializedName("select") Select,
        @SerializedName("radiobutton") RadioButton,
        @SerializedName("images") Images
    }

    enum class AddOnTitleFormat {
        @SerializedName("label") Label,
        @SerializedName("heading") Heading,
        @SerializedName("hide") Hide
    }

    enum class AddOnRestrictionsType {
        @SerializedName("any_text") AnyText,
        @SerializedName("only_letters") OnlyLetters,
        @SerializedName("only_numbers") OnlyNumbers,
        @SerializedName("only_letters_numbers") OnlyLettersNumbers,
        @SerializedName("email") Email
    }

    enum class AddOnPriceType {
        @SerializedName("flat_fee") FlatFee,
        @SerializedName("quantity_based") QuantityBased,
        @SerializedName("percentage_based") PercentageBased
    }

    data class ProductAddonOption(
        @SerializedName("price_type")
        val priceType: AddOnPriceType,

        val label: String? = null,
        val price: String? = null,
        val image: String? = null
    )
}
