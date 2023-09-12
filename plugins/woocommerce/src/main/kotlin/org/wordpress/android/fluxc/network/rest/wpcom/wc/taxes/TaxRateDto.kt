package org.wordpress.android.fluxc.network.rest.wpcom.wc.taxes

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.taxes.TaxRateEntity

data class TaxRateDto (
    @SerializedName("id") val id: Long,
    @SerializedName("country") val country: String?,
    @SerializedName("state" )val state: String?,
    @SerializedName("postcode") val postCode: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("postcodes") val postCodes: List<String>?,
    @SerializedName("cities") val cities: List<String>?,
    @SerializedName("rate") val rate: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("priority") val priority: Int?,
    @SerializedName("compound") val compound: Boolean?,
    @SerializedName("shipping") val shipping: Boolean?,
    @SerializedName("order") val order: Int?,
    @SerializedName("class") val taxClass: String?,
) {
    fun toDataModel(localSiteId: LocalId): TaxRateEntity =
        TaxRateEntity(
            id = RemoteId(id),
            localSiteId = localSiteId,
            country = country,
            state = state,
            postcode = postCode,
            city = city,
            rate = rate,
            name = name,
            taxClass = taxClass,
        )
}


