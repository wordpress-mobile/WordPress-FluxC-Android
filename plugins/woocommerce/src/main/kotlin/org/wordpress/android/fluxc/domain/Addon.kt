package org.wordpress.android.fluxc.domain

import org.wordpress.android.fluxc.domain.Addon.HasAdjustablePrice.Price

sealed class Addon {
    abstract val name: String
    abstract val titleFormat: TitleFormat
    abstract val description: String
    abstract val required: Boolean
    abstract val position: Int

    interface HasOptions {
        val options: List<AddonOption>
    }

    interface HasAdjustablePrice {
        val price: Price

        sealed class Price {
            data class Adjusted(val priceType: PriceType, val value: String) : Price() {
                enum class PriceType {
                    FlatFee,
                    QuantityBased,
                    PercentageBased
                }
            }

            object NotAdjusted : Price()
        }
    }

    data class CustomText(
        override val name: String,
        override val titleFormat: TitleFormat,
        override val description: String,
        override val required: Boolean,
        override val position: Int,
        override val price: Price,
        val restrictions: Restrictions,
        val characterLength: LongRange?
    ) : Addon(), HasAdjustablePrice {
        enum class Restrictions {
            AnyText,
            OnlyLetters,
            OnlyNumbers,
            OnlyLettersNumbers,
            Email
        }
    }

    data class CustomTextArea(
        override val name: String,
        override val titleFormat: TitleFormat,
        override val description: String,
        override val required: Boolean,
        override val position: Int,
        override val price: Price,
        val characterLength: LongRange?
    ) : Addon(), HasAdjustablePrice

    data class FileUpload(
        override val name: String,
        override val titleFormat: TitleFormat,
        override val description: String,
        override val required: Boolean,
        override val position: Int,
        override val price: Price
    ) : Addon(), HasAdjustablePrice

    data class InputMultiplier(
        override val name: String,
        override val titleFormat: TitleFormat,
        override val description: String,
        override val required: Boolean,
        override val position: Int,
        override val price: Price,
        val quantityRange: LongRange?
    ) : Addon(), HasAdjustablePrice

    data class CustomPrice(
        override val name: String,
        override val titleFormat: TitleFormat,
        override val description: String,
        override val required: Boolean,
        override val position: Int,
        val priceRange: LongRange?
    ) : Addon()

    data class MultipleChoice(
        override val name: String,
        override val titleFormat: TitleFormat,
        override val description: String,
        override val required: Boolean,
        override val position: Int,
        override val options: List<AddonOption>,
        val display: Display
    ) : Addon(), HasOptions {
        enum class Display {
            Select,
            RadioButton,
            Images
        }
    }

    data class Checkbox(
        override val name: String,
        override val titleFormat: TitleFormat,
        override val description: String,
        override val required: Boolean,
        override val position: Int,
        override val options: List<AddonOption>
    ) : Addon(), HasOptions

    data class Heading(
        override val name: String,
        override val titleFormat: TitleFormat,
        override val description: String,
        override val required: Boolean,
        override val position: Int
    ) : Addon()

    enum class TitleFormat {
        Label,
        Heading,
        Hide
    }
}
