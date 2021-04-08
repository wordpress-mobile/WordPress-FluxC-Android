package org.wordpress.android.fluxc.store

import kotlinx.coroutines.delay
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult.InvalidAddress
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult.InvalidRequest
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult.Valid
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.CustomPackage
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.PredefinedOption
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.PredefinedOption.PredefinedPackage
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingAccountSettings
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelCreationEligibility
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelMapper
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelPackage
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelPackageData
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelPaperSize
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingRatesResult
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingRatesResult.ShippingOption
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingRatesResult.ShippingPackage
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.SERVER_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.API_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.LabelItem
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelRestClient.GetPackageTypesResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelRestClient.GetPackageTypesResponse.FormSchema.PackageOption.PackageDefinition
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelStatusApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.UpdateSettingsApiRequest
import org.wordpress.android.fluxc.persistence.WCShippingLabelSqlUtils
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.Poller
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

    fun isOrderEligibleForShippingLabelCreation(
        site: SiteModel,
        orderId: Long,
        canCreatePackage: Boolean,
        canCreatePaymentMethod: Boolean,
        canCreateCustomsForm: Boolean
    ): WCShippingLabelCreationEligibility? {
        val eligibility = WCShippingLabelSqlUtils.getSLCreationEligibilityForOrder(site.id, orderId)
                ?: return null

        return if (eligibility.canCreatePackage == canCreatePackage &&
                eligibility.canCreatePaymentMethod == canCreatePaymentMethod &&
                eligibility.canCreateCustomsForm == canCreateCustomsForm) {
            eligibility
        } else {
            null
        }
    }

    suspend fun fetchShippingLabelCreationEligibility(
        site: SiteModel,
        orderId: Long,
        canCreatePackage: Boolean,
        canCreatePaymentMethod: Boolean,
        canCreateCustomsForm: Boolean
    ): WooResult<WCShippingLabelCreationEligibility> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchShippingLabelCreationEligibility") {
            val response = restClient.checkShippingLabelCreationEligibility(
                    site = site,
                    orderId = orderId,
                    canCreatePackage = canCreatePackage,
                    canCreatePaymentMethod = canCreatePaymentMethod,
                    canCreateCustomsForm = canCreateCustomsForm
            )
            return@withDefaultContext when {
                response.isError -> {
                    WooResult(response.error)
                }
                response.result != null -> {
                    val eligibility = WCShippingLabelCreationEligibility().apply {
                        this.localSiteId = site.id
                        this.remoteOrderId = orderId
                        this.canCreatePackage = canCreatePackage
                        this.canCreatePaymentMethod = canCreatePaymentMethod
                        this.canCreateCustomsForm = canCreateCustomsForm
                        this.isEligible = response.result.isEligible
                        this.reason = response.result.reason
                    }
                    WCShippingLabelSqlUtils.insertOrUpdateSLCreationEligibility(eligibility)

                    WooResult(eligibility)
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

    suspend fun getShippingRates(
        site: SiteModel,
        orderId: Long,
        origin: ShippingLabelAddress,
        destination: ShippingLabelAddress,
        packages: List<ShippingLabelPackage>
    ): WooResult<WCShippingRatesResult> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "getShippingRates") {
            val response = restClient.getShippingRates(site, orderId, origin, destination, packages)
            return@withDefaultContext when {
                response.isError -> {
                    WooResult(response.error)
                }
                response.result?.isSuccess == true -> {
                    val packageRates: List<ShippingPackage> = response.result.boxes.map { box ->
                        ShippingPackage(
                                box.key,
                                box.value.entries.map { option ->
                                    ShippingOption(option.key, option.value.rates)
                                }
                        )
                    }
                    WooResult(WCShippingRatesResult(packageRates))
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
                        PredefinedPackage(
                                id = definition.id,
                                title = definition.name,
                                isLetter = definition.isLetter,
                                dimensions = definition.outerDimensions
                        )
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

    suspend fun getAccountSettings(
        site: SiteModel
    ): WooResult<WCShippingAccountSettings> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "getAccountSettings") {
            val response = restClient.getAccountSettings(site)
            return@withDefaultContext when {
                response.isError -> {
                    WooResult(response.error)
                }
                response.result?.success == true -> {
                    WooResult(
                            WCShippingAccountSettings(
                                    isCreatingLabelsEnabled = response.result.formData.isCreatingLabelsEnabled,
                                    isEmailReceiptEnabled = response.result.formData.isPaymentReceiptEnabled,
                                    paperSize = WCShippingLabelPaperSize.fromString(response.result.formData.paperSize),
                                    canEditSettings = response.result.formMeta.canEditSettings,
                                    canManagePayments = response.result.formMeta.canManagePayments,
                                    storeOwnerName = response.result.formMeta.storeOwnerName,
                                    storeOwnerUserName = response.result.formMeta.storeOwnerUserName,
                                    storeOwnerWpcomUserName = response.result.formMeta.storeOwnerWpcomUserName,
                                    storeOwnerWpcomEmail = response.result.formMeta.storeOwnerWpcomEmail,
                                    selectedPaymentMethodId = response.result.formData.selectedPaymentId,
                                    paymentMethods = response.result.formMeta.paymentMethods.orEmpty(),
                                    lastUsedBoxId = response.result.userMeta.lastBoxId
                            )
                    )
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun updateAccountSettings(
        site: SiteModel,
        isCreatingLabelsEnabled: Boolean? = null,
        selectedPaymentMethodId: Int? = null,
        isEmailReceiptEnabled: Boolean? = null,
        paperSize: WCShippingLabelPaperSize? = null
    ): WooResult<Boolean> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "updateSettings") {
            val request = UpdateSettingsApiRequest(
                    isCreatingLabelsEnabled = isCreatingLabelsEnabled,
                    selectedPaymentMethodId = selectedPaymentMethodId,
                    isEmailReceiptEnabled = isEmailReceiptEnabled,
                    paperSize = paperSize?.stringValue
            )
            val response = restClient.updateAccountSettings(site, request)
            return@withDefaultContext when {
                response.isError -> WooResult(response.error)
                response.result == true -> WooResult(true)
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun purchaseShippingLabels(
        site: SiteModel,
        orderId: Long,
        origin: ShippingLabelAddress,
        destination: ShippingLabelAddress,
        packagesData: List<WCShippingLabelPackageData>
    ): WooResult<List<WCShippingLabelModel>> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "purchaseShippingLabels") {
            val response = restClient.purchaseShippingLabels(site, orderId, origin, destination, packagesData)
            return@withDefaultContext when {
                response.isError -> WooResult(response.error)
                response.result?.labels != null -> {
                    delay(2000)
                    val labelsStatusResponse = pollShippingLabelsForPurchase(
                            site,
                            orderId,
                            response.result.labels.map { it.labelId!! }
                    )
                    if (labelsStatusResponse.isError) {
                        WooResult(labelsStatusResponse.error)
                    } else {
                        val shippingLabels = mapper.map(
                                labelsStatusResponse.result!!,
                                orderId,
                                origin,
                                destination,
                                site
                        )
                        WCShippingLabelSqlUtils.insertOrUpdateShippingLabels(shippingLabels)
                        WooResult(shippingLabels)
                    }
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    private suspend fun pollShippingLabelsForPurchase(
        site: SiteModel,
        orderId: Long,
        labelIds: List<Long>
    ): WooPayload<ShippingLabelStatusApiResponse> {
        val poller = Poller(delayInMs = 1000, maxRetries = 3)
        val remainingLabels: MutableList<Long> = labelIds.toMutableList()
        val doneLabels: MutableList<LabelItem> = mutableListOf()

        val response = poller.poll(
                request = {
                    restClient.fetchShippingLabelsStatus(
                            site,
                            orderId,
                            remainingLabels.toList()
                    )
                },
                predicate = { response ->
                    // Stop the polling if the response has an error after the retries
                    if (response.isError || response.result!!.labels == null) return@poll true
                    // Stop the polling if the purchase of one of the labels failed
                    if (response.result.labels!!.any { it.status == LabelItem.STATUS_ERROR }) return@poll true

                    val purchasedLabels = response.result.labels.filter { it.status == LabelItem.STATUS_PURCHASED }
                    doneLabels.addAll(purchasedLabels)
                    remainingLabels.removeAll { labelId ->
                        purchasedLabels.any { it.labelId == labelId }
                    }
                    remainingLabels.isEmpty()
                }
        )

        return when {
            response.isError -> {
                response
            }
            response.result?.labels?.any { it.status == LabelItem.STATUS_ERROR } == true -> {
                WooPayload(
                        WooError(
                                API_ERROR,
                                SERVER_ERROR,
                                message = response.result.labels.first { it.status == LabelItem.STATUS_ERROR }.error
                        )
                )
            }
            else -> {
                WooPayload(ShippingLabelStatusApiResponse(isSuccess = true, labels = doneLabels))
            }
        }
    }
}
