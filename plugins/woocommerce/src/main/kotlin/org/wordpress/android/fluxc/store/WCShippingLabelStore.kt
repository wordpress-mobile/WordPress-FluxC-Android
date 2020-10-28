package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult.InvalidAddress
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult.InvalidRequest
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult.Valid
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.CustomPackage
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.PredefinedOption
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.PredefinedOption.PredefinedPackage
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelMapper
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelRestClient.GetPackageTypesResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelRestClient.GetPackageTypesResponse.FormSchema.PackageOption.PackageDefinition
import org.wordpress.android.fluxc.persistence.WCShippingLabelSqlUtils
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCShippingLabelStore @Inject constructor(
    private val restClient: ShippingLabelRestClient,
    private val coroutineEngine: CoroutineEngine,
    private val mapper: WCShippingLabelMapper
) {
    /**
     * returns a list of shipping labels for an order from the database
     */
    fun getShippingLabelsForOrder(
        site: SiteModel,
        orderId: Long
    ): List<WCShippingLabelModel> =
            WCShippingLabelSqlUtils.getShippingClassesForOrder(site.id, orderId)

    fun getShippingLabelById(
        site: SiteModel,
        orderId: Long,
        remoteShippingLabelId: Long
    ): WCShippingLabelModel? =
            WCShippingLabelSqlUtils.getShippingLabelById(site.id, orderId, remoteShippingLabelId)

    suspend fun fetchShippingLabelsForOrder(
        site: SiteModel,
        orderId: Long
    ): WooResult<List<WCShippingLabelModel>> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchShippingLabelsForOrder") {
            val response = restClient.fetchShippingLabelsForOrder(orderId, site)
            return@withDefaultContext when {
                response.isError -> {
                    WooResult(response.error)
                }
                response.result != null -> {
                    val shippingLabels = mapper.map(response.result, site)

                    // delete existing shipping labels for the order before adding incoming entries
                    WCShippingLabelSqlUtils.deleteShippingLabelsForOrder(orderId)
                    WCShippingLabelSqlUtils.insertOrUpdateShippingLabels(shippingLabels)
                    WooResult(shippingLabels)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun refundShippingLabelForOrder(
        site: SiteModel,
        orderId: Long,
        remoteShippingLabelId: Long
    ): WooResult<Boolean> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "refundShippingLabelForOrder") {
            val response = restClient.refundShippingLabelForOrder(site, orderId, remoteShippingLabelId)
            return@withDefaultContext when {
                response.isError -> {
                    WooResult(response.error)
                }
                response.result != null -> {
                    WooResult(response.result.success)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun printShippingLabel(
        site: SiteModel,
        paperSize: String,
        remoteShippingLabelId: Long
    ): WooResult<String> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "printShippingLabel") {
            val response = restClient.printShippingLabel(site, paperSize, remoteShippingLabelId)
            return@withDefaultContext when {
                response.isError -> {
                    WooResult(response.error)
                }
                response.result?.success == true -> {
                    WooResult(response.result.b64Content)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun verifyAddress(
        site: SiteModel,
        address: ShippingLabelAddress,
        type: ShippingLabelAddress.Type
    ): WooResult<WCAddressVerificationResult> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "verifyAddress") {
            val response = restClient.verifyAddress(site, address, type)
            return@withDefaultContext if (response.isError) {
                WooResult(response.error.apply { message = message ?: "" })
            } else if (response.result?.error != null) {
                if (!response.result.error.address.isNullOrBlank()) {
                    WooResult(InvalidAddress(response.result.error.address))
                } else {
                    WooResult(InvalidRequest(response.result.error.message ?: ""))
                }
            } else if (response.result?.suggestedAddress != null && response.result.isSuccess) {
                WooResult(Valid(response.result.suggestedAddress))
            } else {
                WooResult(WooError(GENERIC_ERROR, UNKNOWN, "Unknown error"))
            }
        }
    }

    suspend fun getPackageTypes(
        site: SiteModel
    ): WooResult<WCPackagesResult> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "getPackageTypes") {
            val response = restClient.getPackageTypes(site)
            return@withDefaultContext when {
                response.isError -> {
                    WooResult(response.error)
                }
                response.result?.isSuccess == true -> {
                    val customPackages = getCustomPackages(response.result)
                    val predefinedOptions = getPredefinedOptions(response.result)
                    WooResult(WCPackagesResult(customPackages, predefinedOptions))
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    private fun getPredefinedOptions(result: GetPackageTypesResponse): List<PredefinedOption> {
        val predefinedOptions = mutableListOf<PredefinedOption>()
        result.formSchema.predefinedSchema.entries.forEach { provider ->
            provider.value.forEach { option ->
                result.formData.predefinedData[provider.key]?.let { packageIds ->
                    val predefinedPackages = getPredefinedPackages(packageIds, option.value.definitions)
                    if (predefinedPackages.isNotEmpty()) {
                        predefinedOptions.add(PredefinedOption(option.value.title, predefinedPackages))
                    }
                }
            }
        }
        return predefinedOptions
    }

    private fun getPredefinedPackages(
        packageIds: List<String?>,
        packageDefinitions: List<PackageDefinition>
    ): List<PredefinedPackage> {
        val predefinedPackages = mutableListOf<PredefinedPackage>()
        packageIds.forEach { packageId ->
            packageDefinitions.firstOrNull { it.id == packageId }?.let { definition ->
                predefinedPackages.add(
                    PredefinedPackage(definition.name, definition.isLetter, definition.outerDimensions)
                )
            }
        }
        return predefinedPackages
    }

    private fun getCustomPackages(result: GetPackageTypesResponse): List<CustomPackage> {
        return result.formData.customData.map {
            CustomPackage(it.name, it.isLetter, it.outerDimensions ?: it.innerDimensions ?: "")
        }
    }
}
