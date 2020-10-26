package org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import org.wordpress.android.fluxc.network.utils.toMap
import java.util.Locale
import javax.inject.Singleton

@Singleton
class ShippingLabelRestClient
constructor(
    dispatcher: Dispatcher,
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder,
    appContext: Context?,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchShippingLabelsForOrder(
        orderId: Long,
        site: SiteModel
    ): WooPayload<ShippingLabelApiResponse> {
        val url = WOOCOMMERCE.connect.label.order(orderId).pathV1

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
                this,
                site,
                url,
                emptyMap(),
                ShippingLabelApiResponse::class.java
        )
        return when (response) {
            is JetpackSuccess -> {
                WooPayload(response.data)
            }
            is JetpackError -> {
                WooPayload(response.error.toWooError())
            }
        }
    }

    suspend fun refundShippingLabelForOrder(
        site: SiteModel,
        orderId: Long,
        remoteShippingLabelId: Long
    ): WooPayload<ShippingLabelApiResponse> {
        val url = WOOCOMMERCE.connect.label.order(orderId).shippingLabelId(remoteShippingLabelId).refund.pathV1

        val response = jetpackTunnelGsonRequestBuilder.syncPostRequest(
                this,
                site,
                url,
                emptyMap(),
                ShippingLabelApiResponse::class.java
        )
        return when (response) {
            is JetpackSuccess -> {
                WooPayload(response.data)
            }
            is JetpackError -> {
                WooPayload(response.error.toWooError())
            }
        }
    }

    suspend fun printShippingLabel(
        site: SiteModel,
        paperSize: String,
        remoteShippingLabelId: Long
    ): WooPayload<PrintShippingLabelApiResponse> {
        val url = WOOCOMMERCE.connect.label.print.pathV1
        val params = mapOf(
                "paper_size" to paperSize,
                "label_id_csv" to remoteShippingLabelId.toString(),
                "caption_csv" to "",
                "json" to "true"
        )

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
                this,
                site,
                url,
                params,
                PrintShippingLabelApiResponse::class.java
        )
        return when (response) {
            is JetpackSuccess -> {
                WooPayload(response.data)
            }
            is JetpackError -> {
                WooPayload(response.error.toWooError())
            }
        }
    }

    suspend fun verifyAddress(
        site: SiteModel,
        address: ShippingLabelAddress,
        type: ShippingLabelAddress.Type
    ): WooPayload<VerifyAddressResponse> {
        val url = WOOCOMMERCE.connect.normalize_address.pathV1
        val params = mapOf(
                "address" to address.toMap(),
                "type" to type.name.toLowerCase(Locale.ROOT)
        )

        val response = jetpackTunnelGsonRequestBuilder.syncPostRequest(
                this,
                site,
                url,
                params,
                VerifyAddressResponse::class.java
        )
        return when (response) {
            is JetpackSuccess -> {
                WooPayload(response.data)
            }
            is JetpackError -> {
                WooPayload(response.error.toWooError())
            }
        }
    }

    suspend fun getPackageTypes(
        site: SiteModel
    ): WooPayload<GetPackageTypesResponse> {
        val url = WOOCOMMERCE.connect.packages.pathV1

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
                this,
                site,
                url,
                emptyMap(),
                GetPackageTypesResponse::class.java
        )
        return when (response) {
            is JetpackSuccess -> {
                WooPayload(response.data)
            }
            is JetpackError -> {
                WooPayload(response.error.toWooError())
            }
        }
    }

    data class PrintShippingLabelApiResponse(
        val mimeType: String,
        val b64Content: String,
        val success: Boolean
    )

    data class VerifyAddressResponse(
        @SerializedName("success") val isSuccess: Boolean,
        @SerializedName("is_trivial_normalization") val isTrivialNormalization: Boolean,
        @SerializedName("normalized") val suggestedAddress: ShippingLabelAddress?,
        @SerializedName("field_errors") val error: Error?
    ) {
        data class Error(
            @SerializedName("general") val message: String?,
            @SerializedName("address") val address: String?
        )
    }

    data class GetPackageTypesResponse(
        @SerializedName("success") val isSuccess: Boolean,
        val storeOptions: StoreOptions,
        val formSchema: FormSchema,
        val formData: FormData
    ) {
        companion object {
            private val gson by lazy { Gson() }
        }

        data class StoreOptions(
            @SerializedName("currency_symbol") val currency: String,
            @SerializedName("dimension_unit") val dimensionUnit: String,
            @SerializedName("weight_unit") val weightUnit: String,
            @SerializedName("origin_country") val originCountry: String
        )

        data class FormData(
            @SerializedName("custom") val customData: List<CustomData>,
            @SerializedName("predefined") val predefinedJson: JsonElement
        ) {
            data class CustomData(
                val name: String,
                @SerializedName("inner_dimensions") val innerDimensions: String?,
                @SerializedName("outer_dimensions") val outerDimensions: String?,
                @SerializedName("box_weight") val boxWeight: Float?,
                @SerializedName("max_weight") val maxWeight: Float?,
                @SerializedName("is_user_defined") val isUserDefined: Boolean?,
                @SerializedName("is_letter") val isLetter: Boolean
            )

            val predefinedData: Map<String, List<String?>>
                get() {
                    val responseType = object : TypeToken<Map<String, List<String?>>>() {}.type
                    return gson.fromJson(predefinedJson, responseType) as? Map<String, List<String?>> ?: emptyMap()
                }
        }

        data class FormSchema(
            @SerializedName("custom") val customSchema: CustomSchema?,
            @SerializedName("predefined") val predefinedJson: JsonElement?
        ) {
            data class CustomSchema(
                val title: String,
                val description: String
            )

            data class PackageOption(
                val title: String,
                val definitions: List<PackageDefinition>
            ) {
                data class PackageDefinition(
                    val id: String,
                    val name: String,
                    val dimensions: Any?,
                    @SerializedName("outer_dimensions") val outerDimensions: String,
                    @SerializedName("inner_dimensions") val innerDimensions: String,
                    @SerializedName("box_weight") val boxWeight: Float?,
                    @SerializedName("max_weight") val maxWeight: Float?,
                    @SerializedName("is_letter") val isLetter: Boolean,
                    @SerializedName("is_flat_rate") val isFlatRate: Boolean?,
                    @SerializedName("can_ship_international") val canShipInternational: Boolean?,
                    @SerializedName("group_id") val groupId: String?,
                    @SerializedName("service_group_ids") val serviceGroupIds: List<String>?
                )
            }

            val predefinedSchema: Map<String, Map<String, PackageType>>
                get() {
                    val responseType = object : TypeToken<Map<String, Map<String, PackageType>>>() {}.type
                    return gson.fromJson(predefinedJson, responseType) as?
                            Map<String, Map<String, PackageType>> ?: emptyMap()
                }
        }
    }
}
