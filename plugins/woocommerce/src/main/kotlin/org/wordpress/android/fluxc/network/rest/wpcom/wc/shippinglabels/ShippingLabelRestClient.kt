package org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.CustomPackage
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.PredefinedOption
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelPackage
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelPackageData
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import org.wordpress.android.fluxc.network.utils.toMap
import java.math.BigDecimal
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ShippingLabelRestClient @Inject constructor(
    dispatcher: Dispatcher,
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
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

    suspend fun checkShippingLabelCreationEligibility(
        site: SiteModel,
        orderId: Long,
        canCreatePackage: Boolean,
        canCreatePaymentMethod: Boolean,
        canCreateCustomsForm: Boolean
    ): WooPayload<SLCreationEligibilityApiResponse> {
        val url = WOOCOMMERCE.connect.label.order(orderId).creation_eligibility.pathV1
        val params = mapOf(
                "can_create_package" to canCreatePackage.toString(),
                "can_create_payment_method" to canCreatePaymentMethod.toString(),
                "can_create_customs_form" to canCreateCustomsForm.toString()
        )
        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
                this,
                site,
                url,
                params,
                SLCreationEligibilityApiResponse::class.java
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

    suspend fun getAccountSettings(
        site: SiteModel
    ): WooPayload<AccountSettingsApiResponse> {
        val url = WOOCOMMERCE.connect.account.settings.pathV1

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
                this,
                site,
                url,
                emptyMap(),
                AccountSettingsApiResponse::class.java
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

    suspend fun updateAccountSettings(site: SiteModel, request: UpdateSettingsApiRequest): WooPayload<Boolean> {
        val url = WOOCOMMERCE.connect.account.settings.pathV1

        val response = jetpackTunnelGsonRequestBuilder.syncPostRequest(
                this,
                site,
                url,
                request.toMap(),
                JsonObject::class.java
        )
        return when (response) {
            is JetpackSuccess -> {
                WooPayload(response.data!!["success"].asBoolean)
            }
            is JetpackError -> {
                WooPayload(response.error.toWooError())
            }
        }
    }

    suspend fun getShippingRates(
        site: SiteModel,
        orderId: Long,
        origin: ShippingLabelAddress,
        destination: ShippingLabelAddress,
        packages: List<ShippingLabelPackage>
    ): WooPayload<ShippingRatesApiResponse> {
        val url = WOOCOMMERCE.connect.label.order(orderId).rates.pathV1

        val params = mapOf(
            "origin" to origin.toMap(),
            "destination" to destination.toMap(),
            "packages" to packages.map { it.toMap() }
        )

        val response = jetpackTunnelGsonRequestBuilder.syncPostRequest(
                this,
                site,
                url,
                params,
                ShippingRatesApiResponse::class.java
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

    suspend fun purchaseShippingLabels(
        site: SiteModel,
        orderId: Long,
        origin: ShippingLabelAddress,
        destination: ShippingLabelAddress,
        packagesData: List<WCShippingLabelPackageData>
    ): WooPayload<ShippingLabelStatusApiResponse> {
        val url = WOOCOMMERCE.connect.label.order(orderId).pathV1

        val params = mapOf(
                "async" to true,
                "origin" to origin.toMap(),
                "destination" to destination.toMap(),
                "packages" to packagesData.map { it.toMap() }
        )

        val response = jetpackTunnelGsonRequestBuilder.syncPostRequest(
                this,
                site,
                url,
                params,
                ShippingLabelStatusApiResponse::class.java
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

    suspend fun fetchShippingLabelsStatus(
        site: SiteModel,
        orderId: Long,
        labelIds: List<Long>
    ): WooPayload<ShippingLabelStatusApiResponse> {
        val url = WOOCOMMERCE.connect.label.order(orderId).shippingLabels(labelIds.joinToString(separator = ",")).pathV1
        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
                this,
                site,
                url,
                emptyMap(),
                ShippingLabelStatusApiResponse::class.java
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

    // Supports both creating custom package(s), or activating existing predefined package(s).
    // Creating singular or multiple items, as well as mixed items at once are supported.
    suspend fun createPackages(
        site: SiteModel,
        customPackages: List<CustomPackage> = emptyList(),
        predefinedOptions: List<PredefinedOption> = emptyList()
    ): WooPayload<Boolean> {
        // We need at least one of the lists to not be empty to continue with API call.
        if(customPackages.isEmpty() && predefinedOptions.isEmpty()) {
            return WooPayload(false)
        }

        val url = WOOCOMMERCE.connect.packages.pathV1

        // 1. Mapping for custom packages
        // Here we convert each CustomPackage instance into a Map with proper key names.
        // This list of Maps will then be used as the "customs" key's value in the API request.
        val mappedCustomPackages = customPackages.map {
            mapOf(
                    "name" to it.title,
                    "is_letter" to it.isLetter,
                    "inner_dimensions" to it.dimensions,
                    "box_weight" to it.boxWeight,

                    /*
                    In wp-admin, The two values below are not user-editable but are saved during
                    package creation with hardcoded values. Here we replicate the same behavior.
                     */
                    "is_user_defined" to true,
                    "max_weight" to 0
            )
        }

        // 2. Mapping for predefined options.
        // First, grab all unique carriers. distinct() is used because predefinedOptions can contain
        // multiple PredefinedOption with the same carrier name. (For example, "USPS Priority Mail Express Boxes"
        // and "USPS Priority Mail Boxes" are two different options with the same "USPS" carrier".
        val carriers = predefinedOptions.map { it.carrier }.distinct()

        // Next, build a predefinedParam Map replicating the required JSON request structure.
        // It should be like the following:
        //
        //    "carrier_1": [ "package_1", "package_2", ... ],
        //    "carrier_2": [ "package_3", "package_4", "package_5" ... ],
        //
        val predefinedParam = mutableMapOf<String, List<String>>()
        carriers.forEach { carrier ->
            val packageIds = mutableListOf<String>()
            // Get all predefined options having the same carrier.
            val predefinedOptionsForThisCarrier = predefinedOptions.filter { it.carrier == carrier }
            // Get all package id(s) included in all options.
            predefinedOptionsForThisCarrier.forEach { option ->
                option.predefinedPackages.forEach {
                    packageIds.add(it.id)
                }
            }
            predefinedParam[carrier] = packageIds
        }

        val params = mapOf(
                "custom" to mappedCustomPackages,
                "predefined" to predefinedParam
        )

        val response = jetpackTunnelGsonRequestBuilder.syncPostRequest(
                this,
                site,
                url,
                params,
                JsonObject::class.java
        )

        return when (response) {
            is JetpackSuccess -> {
                WooPayload(response.data!!["success"].asBoolean)
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

    data class ShippingRatesApiResponse(
        @SerializedName("success") val isSuccess: Boolean,
        @SerializedName("rates") private val boxesJson: JsonElement
    ) {
        companion object {
            private val gson by lazy { Gson() }
        }

        val boxes: Map<String, Map<String, ShippingOption>>
            get() {
                val responseType = object : TypeToken<Map<String, Map<String, ShippingOption>>>() {}.type
                return gson.fromJson(boxesJson, responseType) as? Map<String, Map<String, ShippingOption>> ?: emptyMap()
            }

        data class ShippingOption(
            val rates: List<Rate>
        ) {
            data class Rate(
                val title: String,
                val insurance: BigDecimal,
                val rate: BigDecimal,
                @SerializedName("rate_id") val rateId: String,
                @SerializedName("service_id") val serviceId: String,
                @SerializedName("carrier_id") val carrierId: String,
                @SerializedName("shipment_id") val shipmentId: String,
                @SerializedName("tracking") val hasTracking: Boolean,
                @SerializedName("retail_rate") val retailRate: BigDecimal,
                @SerializedName("is_selected") val isSelected: Boolean,
                @SerializedName("free_pickup") val isPickupFree: Boolean,
                @SerializedName("delivery_days") val deliveryDays: Int,
                @SerializedName("delivery_date_guaranteed") val deliveryDateGuaranteed: Boolean,
                @SerializedName("delivery_date") val deliveryDate: Date?
            )
        }
    }

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

            val predefinedSchema: Map<String, Map<String, PackageOption>>
                get() {
                    val responseType = object : TypeToken<Map<String, Map<String, PackageOption>>>() {}.type
                    return gson.fromJson(predefinedJson, responseType) as?
                            Map<String, Map<String, PackageOption>> ?: emptyMap()
                }
        }
    }
}
