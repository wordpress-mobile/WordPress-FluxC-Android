package org.wordpress.android.fluxc.example.ui.shippinglabels

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.fragment_woo_shippinglabels.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.replaceFragment
import org.wordpress.android.fluxc.example.ui.StoreSelectingFragment
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.generated.WCCoreActionBuilder
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.model.shippinglabels.WCContentType
import org.wordpress.android.fluxc.model.shippinglabels.WCCustomsItem
import org.wordpress.android.fluxc.model.shippinglabels.WCNonDeliveryOption
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.CustomPackage
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.PredefinedOption
import org.wordpress.android.fluxc.model.shippinglabels.WCRestrictionType
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingAccountSettings
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelPackage
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelPackageData
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelPaperSize
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingPackageCustoms
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersByIdsPayload
import org.wordpress.android.fluxc.store.WCShippingLabelStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WooCommerceStore.WooPlugin.WOO_SERVICES
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.math.BigDecimal
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.ceil

class WooShippingLabelFragment : StoreSelectingFragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore
    @Inject internal lateinit var wcShippingLabelStore: WCShippingLabelStore
    @Inject internal lateinit var wcOrderStore: WCOrderStore

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_woo_shippinglabels, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetch_shipping_labels.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(activity, "Enter the order ID:", isNumeric = true) { orderEditText ->
                    if (orderEditText.text.isEmpty()) {
                        prependToLog("OrderId is null so doing nothing")
                        return@showSingleLineDialog
                    }

                    val orderId = orderEditText.text.toString().toLong()
                    prependToLog("Submitting request to fetch shipping labels for order $orderId")
                    coroutineScope.launch {
                        try {
                            val response = withContext(Dispatchers.Default) {
                                wcShippingLabelStore.fetchShippingLabelsForOrder(site, orderId)
                            }
                            response.error?.let {
                                prependToLog("${it.type}: ${it.message}")
                            }
                            response.model?.let {
                                val labelIds = it.map { it.remoteShippingLabelId }.joinToString(",")
                                prependToLog("Order $orderId has ${it.size} shipping labels with ids $labelIds")
                            }
                        } catch (e: Exception) {
                            prependToLog("Error: ${e.message}")
                        }
                    }
                }
            }
        }

        refund_shipping_label.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(activity, "Enter the order ID:", isNumeric = true) { orderEditText ->
                    if (orderEditText.text.isEmpty()) {
                        prependToLog("OrderId is null so doing nothing")
                        return@showSingleLineDialog
                    }

                    val orderId = orderEditText.text.toString().toLong()
                    showSingleLineDialog(
                            activity, "Enter the remote shipping Label Id:", isNumeric = true
                    ) { remoteIdEditText ->
                        if (remoteIdEditText.text.isEmpty()) {
                            prependToLog("Remote Id is null so doing nothing")
                            return@showSingleLineDialog
                        }

                        val remoteId = remoteIdEditText.text.toString().toLong()
                        prependToLog("Submitting request to refund shipping label for order $orderId with id $remoteId")

                        coroutineScope.launch {
                            try {
                                val response = withContext(Dispatchers.Default) {
                                    wcShippingLabelStore.refundShippingLabelForOrder(site, orderId, remoteId)
                                }
                                response.error?.let {
                                    prependToLog("${it.type}: ${it.message}")
                                }
                                response.model?.let {
                                    prependToLog(
                                            "Refund for $orderId with shipping label $remoteId is ${response.model}"
                                    )
                                }
                            } catch (e: Exception) {
                                prependToLog("Error: ${e.message}")
                            }
                        }
                    }
                }
            }
        }

        print_shipping_label.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(
                        activity, "Enter one or multiple shipping Label Ids (separated by commas):", isNumeric = false
                ) { remoteIdEditText ->
                    if (remoteIdEditText.text.isEmpty()) {
                        prependToLog("Remote Id is null so doing nothing")
                        return@showSingleLineDialog
                    }

                    val remoteIds = try {
                        remoteIdEditText.text.split(",").map { it.trim().toLong() }
                    } catch (e: Exception) {
                        prependToLog("please check that your input is valid")
                        return@showSingleLineDialog
                    }
                    prependToLog(
                            "Submitting request to print shipping label(s) ${remoteIds.joinToString(" and ")}"
                    )

                    coroutineScope.launch {
                        try {
                            val response = withContext(Dispatchers.Default) {
                                if (remoteIds.size == 1) {
                                    // Use the single label function here to confirm that it's working as well
                                    wcShippingLabelStore.printShippingLabel(site, "label", remoteIds.first())
                                } else {
                                    wcShippingLabelStore.printShippingLabels(site, "label", remoteIds)
                                }
                            }
                            response.error?.let {
                                prependToLog("${it.type}: ${it.message}")
                            }
                            response.model?.let { base64Content ->
                                writePDFToFile(base64Content)?.let { openPdfReader(it) }
                            }
                        } catch (e: Exception) {
                            prependToLog("Error: ${e.message}")
                        }
                    }
                }
            }
        }

        print_commercial_invoice.setOnClickListener {
            selectedSite?.let { site ->
                coroutineScope.launch {
                    val orderId = showSingleLineDialog(requireActivity(), "Enter the order ID", isNumeric = true)
                            ?.toLong()

                    val labelId = showSingleLineDialog(requireActivity(), "Enter the label ID", isNumeric = true)
                            ?.toLong()

                    if (orderId == null || labelId == null) {
                        prependToLog(
                                "One of the submitted parameters is null\n" +
                                        "Order ID: $orderId\n" +
                                        "Label ID: $labelId"
                        )
                        return@launch
                    }

                    val label: WCShippingLabelModel? = wcShippingLabelStore.getShippingLabelById(site, orderId, labelId)
                            ?: suspend {
                                prependToLog("Fetching label")
                                wcShippingLabelStore.fetchShippingLabelsForOrder(site, orderId)
                                wcShippingLabelStore.getShippingLabelById(site, orderId, labelId)
                            }.invoke()

                    if (label == null) {
                        prependToLog("Couldn't find a label with the id $labelId")
                        return@launch
                    }

                    if (label.commercialInvoiceUrl.isNullOrEmpty()) {
                        prependToLog(
                                "The label doesn't have a commercial invoice URL, " +
                                        "please make sure to use a international label with a carrier that requires " +
                                        "a commercial invoice URL (DHL for example)"
                        )
                        return@launch
                    }

                    prependToLog("Downloading the commercial invoice")
                    val invoiceFile = withContext(Dispatchers.IO) {
                        downloadUrl(label.commercialInvoiceUrl!!)
                    }
                    invoiceFile?.let {
                        openPdfReader(it)
                    }
                }
            }
        }

        check_creation_eligibility.setOnClickListener {
            selectedSite?.let {
                coroutineScope.launch {
                    val orderId = showSingleLineDialog(requireActivity(), "Order Id?", isNumeric = true)?.toLong()
                    if (orderId == null) {
                        prependToLog("Please enter a valid order id")
                        return@launch
                    }
                    val canCreatePackage = showSingleLineDialog(
                            requireActivity(),
                            "Can Create Package? (true or false)"
                    ).toBoolean()
                    val canCreatePaymentMethod = showSingleLineDialog(
                            requireActivity(),
                            "Can Create Payment Method? (true or false)"
                    ).toBoolean()
                    val canCreateCustomsForm = showSingleLineDialog(
                            requireActivity(),
                            "Can Create Customs Form? (true or false)"
                    ).toBoolean()

                    var eligibility = wcShippingLabelStore.isOrderEligibleForShippingLabelCreation(
                            site = it,
                            orderId = orderId,
                            canCreatePackage = canCreatePackage,
                            canCreatePaymentMethod = canCreatePaymentMethod,
                            canCreateCustomsForm = canCreateCustomsForm
                    )
                    if (eligibility == null) {
                        prependToLog("Fetching eligibility")
                        val result = wcShippingLabelStore.fetchShippingLabelCreationEligibility(
                                site = it,
                                orderId = orderId,
                                canCreatePackage = canCreatePackage,
                                canCreatePaymentMethod = canCreatePaymentMethod,
                                canCreateCustomsForm = canCreateCustomsForm
                        )
                        if (result.isError) {
                            prependToLog("${result.error.type}: ${result.error.message}")
                            return@launch
                        }
                        eligibility = result.model!!
                    }

                    prependToLog(
                            "The order is ${if (!eligibility.isEligible) "not " else ""}eligible " +
                                    "for Shipping Labels Creation"
                    )
                    if (!eligibility.isEligible) {
                        prependToLog("Reason for non eligibility: ${eligibility.reason}")
                    }
                }
            }
        }

        verify_address.setOnClickListener {
            selectedSite?.let { site ->
                replaceFragment(WooVerifyAddressFragment.newInstance(site))
            }
        }

        get_packages.setOnClickListener {
            selectedSite?.let { site ->
                coroutineScope.launch {
                    val result = withContext(Dispatchers.Default) {
                        wcShippingLabelStore.getPackageTypes(site)
                    }
                    result.error?.let {
                        prependToLog("${it.type}: ${it.message}")
                    }
                    result.model?.let {
                        prependToLog("$it")
                    }
                }
            }
        }

        create_custom_package.setOnClickListener {
            selectedSite?.let { site ->
                coroutineScope.launch {
                    val customPackageName = showSingleLineDialog(
                            requireActivity(),
                            "Enter Package name"
                    ).toString()
                    val customPackageDimension = showSingleLineDialog(
                            requireActivity(),
                            "Enter Package dimensions"
                    ).toString()
                    val customPackageIsLetter = showSingleLineDialog(
                            requireActivity(),
                            "Is it a letter? (true or false)"
                    ).toBoolean()
                    val customPackageWeightText = showSingleLineDialog(
                            requireActivity(),
                            "Enter Package weight"
                    ).toString()

                    val customPackageWeight = customPackageWeightText.toFloatOrNull()
                    if (customPackageWeight == null) {
                        prependToLog("Invalid float value for package weight: $customPackageWeightText\n")
                    } else {
                        val result = withContext(Dispatchers.Default) {
                            val customPackage = CustomPackage(
                                    title = customPackageName,
                                    isLetter = customPackageIsLetter,
                                    dimensions = customPackageDimension,
                                    boxWeight = customPackageWeight
                            )
                            wcShippingLabelStore.createPackages(
                                    site = site,
                                    customPackages = listOf(customPackage),
                                    predefinedPackages = emptyList()
                            )
                        }
                        result.error?.let {
                            prependToLog("${it.type}: ${it.message}")
                        }
                        result.model?.let {
                            prependToLog("Custom package created: $it")
                        }
                    }
                }
            }
        }

        // This test gets all available predefined options, picks one at random, then activates one package in it.
        // The end result can either it succeeds (if it hasn't been activated before), or it won't. For the
        // latter case, the button can be re-tried again by tester.
        activate_predefined_package.setOnClickListener {
            prependToLog("Grabbing all available predefined package options...")
            selectedSite?.let { site ->
                coroutineScope.launch {
                    val allPredefinedOptions = mutableListOf<PredefinedOption>()
                    val allPredefinedResult = withContext(Dispatchers.Default) {
                        wcShippingLabelStore.getAllPredefinedOptions(site)
                    }

                    allPredefinedResult.error?.let {
                        prependToLog("${it.type}: ${it.message}")
                        return@launch
                    }
                    allPredefinedResult.model?.let {
                        allPredefinedOptions.addAll(it)
                    }

                    // Pick a random Option, and then pick only one Package inside of it.
                    val randomOption = allPredefinedOptions.random()
                    val randomParam = PredefinedOption(
                            title = randomOption.title,
                            carrier = randomOption.carrier,
                            predefinedPackages = listOf(randomOption.predefinedPackages.random())
                    )
                    prependToLog(
                            "Activating ${randomParam.predefinedPackages.first().id} from ${randomParam.carrier}..."
                    )

                    val result = withContext(Dispatchers.Default) {
                        wcShippingLabelStore.createPackages(
                                site = site,
                                customPackages = emptyList(),
                                predefinedPackages = listOf(randomParam)
                        )
                    }
                    result.error?.let {
                        prependToLog("${it.type}: ${it.message}")
                        return@launch
                    }
                    result.model?.let {
                        prependToLog("Predefined package activated: $it")
                    }
                }
            }
        }

        get_shipping_rates.setOnClickListener {
            selectedSite?.let { site ->
                coroutineScope.launch {
                    val orderId = showSingleLineDialog(requireActivity(), "Enter order id:", isNumeric = true)
                            ?.toLong() ?: return@launch

                    val (order, origin, destination) = loadData(site, orderId)

                    if (order == null) {
                        prependToLog("Couldn't retrieve order data")
                        return@launch
                    }
                    if (origin == null || destination == null) {
                        prependToLog(
                                "Invalid origin or destination address:\n" +
                                        "Origin:\n$origin\nDestination:\n$destination"
                        )
                        return@launch
                    }
                    var name: String
                    showSingleLineDialog(activity, "Enter package name:") { text ->
                        name = text.text.toString()

                        var height: Float?
                        showSingleLineDialog(activity, "Enter height:", isNumeric = true) { h ->
                            height = h.text.toString().toFloatOrNull()

                            var width: Float?
                            showSingleLineDialog(activity, "Enter width:", isNumeric = true) { w ->
                                width = w.text.toString().toFloatOrNull()

                                var length: Float?
                                showSingleLineDialog(activity, "Enter length:", isNumeric = true) { l ->
                                    length = l.text.toString().toFloatOrNull()

                                    var weight: Float?
                                    showSingleLineDialog(activity, "Enter weight:", isNumeric = true) { t ->
                                        weight = t.text.toString().toFloatOrNull()

                                        val box: ShippingLabelPackage?
                                        if (height == null || width == null || length == null || weight == null) {
                                            prependToLog(
                                                    "Invalid package parameters:\n" +
                                                            "Height: $height\n" +
                                                            "Width: $width\n" +
                                                            "Length: $length\n" +
                                                            "Weight: $weight"
                                            )
                                        } else {
                                            box = ShippingLabelPackage(
                                                    name,
                                                    "medium_flat_box_top",
                                                    height!!,
                                                    length!!,
                                                    width!!,
                                                    weight!!
                                            )

                                            coroutineScope.launch {
                                                val result = wcShippingLabelStore.getShippingRates(
                                                        site,
                                                        order.remoteOrderId,
                                                        origin,
                                                        destination,
                                                        listOf(box),
                                                        null
                                                )

                                                result.error?.let {
                                                    prependToLog("${it.type}: ${it.message}")
                                                }
                                                result.model?.let {
                                                    prependToLog("$it")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        purchase_label.setOnClickListener {
            selectedSite?.let { site ->
                coroutineScope.launch {
                    val orderId = showSingleLineDialog(requireActivity(), "Enter order id:", isNumeric = true)
                            ?.toLong() ?: return@launch

                    val (order, origin, destination) = loadData(site, orderId)

                    if (order == null) {
                        prependToLog("Couldn't retrieve order data")
                        return@launch
                    }
                    if (origin == null || destination == null) {
                        prependToLog(
                                "Invalid origin or destination address:\n" +
                                        "Origin:\n$origin\nDestination:\n$destination"
                        )
                        return@launch
                    }

                    val boxId = showSingleLineDialog(
                            requireActivity(), "Enter box Id", defaultValue = "medium_flat_box_top"
                    )
                    val height = showSingleLineDialog(requireActivity(), "Enter height:", isNumeric = true)
                            ?.toFloat()
                    val width = showSingleLineDialog(requireActivity(), "Enter width:", isNumeric = true)
                            ?.toFloat()
                    val length = showSingleLineDialog(requireActivity(), "Enter length:", isNumeric = true)
                            ?.toFloat()
                    val weight = showSingleLineDialog(requireActivity(), "Enter weight:", isNumeric = true)
                            ?.toFloat()
                    if (boxId == null || height == null || width == null || length == null || weight == null) {
                        prependToLog(
                                "Invalid package parameters:\n" +
                                        "BoxId: $boxId\n" +
                                        "Height: $height\n" +
                                        "Width: $width\n" +
                                        "Length: $length\n" +
                                        "Weight: $weight"
                        )
                        return@launch
                    }
                    prependToLog("Retrieving rates")

                    val box = ShippingLabelPackage(
                            "default",
                            boxId,
                            height,
                            length,
                            width,
                            weight
                    )

                    val isInternational = destination.country != origin.country
                    val customsData = if (isInternational) {
                        val customsItems =
                                order.getLineItemList().map {
                                    val quantity = it.quantity ?: 0f
                                    WCCustomsItem(
                                            productId = it.productId!!,
                                            description = it.name.orEmpty(),
                                            value = (it.price?.toBigDecimal() ?: BigDecimal.ZERO),
                                            quantity = ceil(quantity).toInt(),
                                            weight = 1f,
                                            hsTariffNumber = null,
                                            originCountry = "US"
                                    )
                                }

                        WCShippingPackageCustoms(
                                id = "default",
                                contentsType = if (isInternational) WCContentType.Merchandise else null,
                                restrictionType = if (isInternational) WCRestrictionType.None else null,
                                itn = "AES X20160406131357",
                                nonDeliveryOption = if (isInternational) WCNonDeliveryOption.Return else null,
                                customsItems = customsItems
                        )
                    } else null

                    val ratesResult = wcShippingLabelStore.getShippingRates(
                            site,
                            order.remoteOrderId,
                            if (isInternational) origin.copy(phone = "0000000000") else origin,
                            if (isInternational) destination.copy(phone = "0000000000") else destination,
                            listOf(box),
                            customsData?.let { listOf(it) }
                    )
                    if (ratesResult.isError) {
                        prependToLog(
                                "Getting rates failed: " +
                                        "${ratesResult.error.type}: ${ratesResult.error.message}"
                        )
                        return@launch
                    }
                    if (ratesResult.model!!.packageRates.isEmpty() ||
                            ratesResult.model!!.packageRates.first().shippingOptions.isEmpty() ||
                            ratesResult.model!!.packageRates.first().shippingOptions.first().rates.isEmpty()) {
                        prependToLog("Couldn't find rates for the given input, please try with different parameters")
                        return@launch
                    }
                    val rate = ratesResult.model!!.packageRates.first().shippingOptions.first().rates.first()
                    val packageData = WCShippingLabelPackageData(
                            id = "default",
                            boxId = boxId,
                            isLetter = box.isLetter,
                            length = length,
                            height = height,
                            width = width,
                            weight = weight,
                            shipmentId = rate.shipmentId,
                            rateId = rate.rateId,
                            serviceId = rate.serviceId,
                            serviceName = rate.title,
                            carrierId = rate.carrierId,
                            products = order.getLineItemList().map { it.productId!! }
                    )
                    prependToLog("Purchasing label")
                    val result = wcShippingLabelStore.purchaseShippingLabels(
                            site,
                            order.remoteOrderId,
                            if (isInternational) origin.copy(phone = "0000000000") else origin,
                            if (isInternational) destination.copy(phone = "0000000000") else destination,
                            listOf(packageData),
                            customsData?.let { listOf(it) },
                            emailReceipts = true
                    )

                    result.error?.let {
                        prependToLog("${it.type}: ${it.message}")
                    }
                    result.model?.let {
                        val label = it.first()
                        prependToLog(
                                "Purchased a shipping label with the following details:\n" +
                                        "Order Id: ${label.remoteOrderId}\n" +
                                        "Products: ${label.productNames}\n" +
                                        "Label Id: ${label.remoteShippingLabelId}\n" +
                                        "Price: ${label.rate}"
                        )
                    }
                }
            }
        }

        get_shipping_plugin_info.setOnClickListener {
            selectedSite?.let { site ->
                coroutineScope.launch {
                    val result = withContext(Dispatchers.Default) {
                        wooCommerceStore.fetchSitePlugins(site)
                    }
                    result.error?.let {
                        prependToLog("${it.type}: ${it.message}")
                    }
                    val plugin = wooCommerceStore.getSitePlugin(site, WOO_SERVICES)
                    plugin?.let {
                        prependToLog("$it")
                    }
                }
            }
        }

        get_account_settings.setOnClickListener {
            selectedSite?.let { site ->
                coroutineScope.launch {
                    val result = wcShippingLabelStore.getAccountSettings(site)
                    result.error?.let {
                        prependToLog("${it.type}: ${it.message}")
                    }
                    if (result.model != null) {
                        prependToLog("${result.model}")
                    } else {
                        prependToLog("The WooCommerce services plugin is not installed")
                    }
                }
            }
        }
        update_account_settings.setOnClickListener {
            selectedSite?.let { site ->
                coroutineScope.launch {
                    val result = wcShippingLabelStore.getAccountSettings(site)
                    result.error?.let {
                        prependToLog("Can't fetch account settings\n${it.type}: ${it.message}")
                    }
                    if (result.model != null) {
                        showAccountSettingsDialog(site, result.model!!)
                    } else {
                        prependToLog("The WooCommerce services plugin is not installed")
                    }
                }
            }
        }
    }

    private fun showAccountSettingsDialog(selectedSite: SiteModel, accountSettings: WCShippingAccountSettings) {
        val dialog = AlertDialog.Builder(requireContext()).let {
            it.setView(R.layout.dialog_wc_shipping_label_settings)
            it.show()
        }
        dialog.findViewById<CheckBox>(R.id.enabled_checkbox)?.isChecked = accountSettings.isCreatingLabelsEnabled
        dialog.findViewById<EditText>(R.id.payment_method_id)?.setText(
                accountSettings.selectedPaymentMethodId?.toString() ?: ""
        )
        dialog.findViewById<CheckBox>(R.id.email_receipts_checkbox)?.isChecked = accountSettings.isEmailReceiptEnabled
        dialog.findViewById<Spinner>(R.id.paper_size_spinner)?.let {
            val items = listOf("label", "legal", "letter")
            it.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, items)
            it.setSelection(items.indexOf(accountSettings.paperSize.stringValue))
        }
        dialog.findViewById<Button>(R.id.save_button)?.setOnClickListener {
            dialog.hide()
            coroutineScope.launch {
                val result = wcShippingLabelStore.updateAccountSettings(
                        selectedSite,
                        isCreatingLabelsEnabled = dialog.findViewById<CheckBox>(R.id.enabled_checkbox)?.isChecked,
                        selectedPaymentMethodId = dialog.findViewById<EditText>(R.id.payment_method_id)?.text
                                ?.toString()?.ifEmpty { null }?.toInt(),
                        isEmailReceiptEnabled = dialog.findViewById<CheckBox>(R.id.email_receipts_checkbox)?.isChecked,
                        paperSize = dialog.findViewById<Spinner>(R.id.paper_size_spinner)?.selectedItem?.let {
                            WCShippingLabelPaperSize.fromString(it as String)
                        }
                )
                dialog.dismiss()
                result.error?.let {
                    prependToLog("${it.type}: ${it.message}")
                }
                if (result.model == true) {
                    prependToLog("Settings updated")
                } else {
                    prependToLog("The WooCommerce services plugin is not installed")
                }
            }
        }
    }

    private suspend fun loadData(site: SiteModel, orderId: Long):
            Triple<WCOrderModel?, ShippingLabelAddress?, ShippingLabelAddress?> {
        prependToLog("Loading shipping data...")

        dispatcher.dispatch(WCCoreActionBuilder.newFetchSiteSettingsAction(site))

        val payload = FetchOrdersByIdsPayload(site, listOf(RemoteId(orderId)))
        dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersByIdsAction(payload))

        delay(5000)

        val origin = wooCommerceStore.getSiteSettings(site)?.let {
            ShippingLabelAddress(
                    name = "Test Name",
                    address = it.address,
                    city = it.city,
                    postcode = it.postalCode,
                    state = it.stateCode,
                    country = it.countryCode
            )
        }

        val order = wcOrderStore.getOrderByIdentifier(OrderIdentifier(site.id, orderId))
        val destination = order?.getShippingAddress()?.let {
            ShippingLabelAddress(
                    name = "${it.firstName} ${it.lastName}",
                    address = it.address1,
                    city = it.city,
                    postcode = it.postcode,
                    state = it.state,
                    country = it.country
            )
        }

        return Triple(order, origin, destination)
    }

    /**
     * Creates a temporary file for storing captured photos
     */
    private fun createTempPdfFile(context: Context): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = context.externalCacheDir
        return try {
            File.createTempFile(
                    "PDF_${timeStamp}_",
                    ".pdf",
                    storageDir
            )
        } catch (ex: IOException) {
            ex.printStackTrace()
            prependToLog("Error when creating temp file: ${ex.message}")
            null
        }
    }

    private fun writePDFToFile(base64Content: String): File? {
        return try {
            createTempPdfFile(requireContext())?.let { file ->
                val out = FileOutputStream(file)
                val pdfAsBytes = Base64.decode(base64Content, 0)
                out.write(pdfAsBytes)
                out.flush()
                out.close()
                file
            }
        } catch (e: Exception) {
            e.printStackTrace()
            prependToLog("Error when writing pdf to file: ${e.message}")
            null
        }
    }

    private fun openPdfReader(file: File) {
        val authority = requireContext().applicationContext.packageName + ".provider"
        val fileUri = FileProvider.getUriForFile(
                requireContext(),
                authority,
                file
        )

        val sendIntent = Intent(Intent.ACTION_VIEW)
        sendIntent.setDataAndType(fileUri, "application/pdf")
        sendIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(sendIntent)
    }

    private fun downloadUrl(url: String): File? {
        return try {
            URL(url).openConnection().inputStream.use { inputStream ->
                createTempPdfFile(requireContext())?.let { file ->
                    file.outputStream().use { output ->
                        inputStream.copyTo(output)
                    }
                    file
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            prependToLog("Error downloading the file: ${e.message}")
            null
        }
    }
}
