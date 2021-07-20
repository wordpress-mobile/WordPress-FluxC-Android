package org.wordpress.android.fluxc.network.rest.wpcom.wc.addons

class ProductAddonApiResponse {
    val type: AddOnType? = null
    val display: String? = null
    val name: String? = null
    val titleFormat: String? = null
    val descriptionEnabled: String? = null
    val description: String? = null
    val required: String? = null
    val position: String? = null
    val restrictions: String? = null
    val restrictionsType: String? = null
    val adjustPrice: String? = null
    val priceType: String? = null
    val price: String? = null
    val min: String? = null
    val max: String? = null
    val options: String? = null
}

class AddOnType {
    val multipleChoice: String? = null
    val checkbox: String? = null
    val customText: String? = null
    val customTextArea: String? = null
    val fileUpload: String? = null
    val customPrice: String? = null
    val inputMultiplier: String? = null
    val heading: String? = null
}