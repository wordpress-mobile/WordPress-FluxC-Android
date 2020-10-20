package org.wordpress.android.fluxc.example.ui.shippinglabels

import android.content.Context
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_shippinglabels.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.replaceFragment
import org.wordpress.android.fluxc.example.ui.StoreSelectorDialog
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.example.utils.toggleSiteDependentButtons
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCShippingLabelStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class WooShippingLabelFragment : Fragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore
    @Inject internal lateinit var wcShippingLabelStore: WCShippingLabelStore

    private var selectedPos: Int = -1
    private var selectedSite: SiteModel? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_woo_shippinglabels, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        shipping_labels_select_site.setOnClickListener {
            showSiteSelectorDialog(selectedPos, object : StoreSelectorDialog.Listener {
                override fun onSiteSelected(site: SiteModel, pos: Int) {
                    selectedSite = site
                    selectedPos = pos
                    buttonContainer.toggleSiteDependentButtons(true)
                    shipping_labels_selected_site.text = site.name ?: site.displayName
                }
            })
        }

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
                        activity, "Enter the remote shipping Label Id:", isNumeric = true
                ) { remoteIdEditText ->
                    if (remoteIdEditText.text.isEmpty()) {
                        prependToLog("Remote Id is null so doing nothing")
                        return@showSingleLineDialog
                    }

                    val remoteId = remoteIdEditText.text.toString().toLong()
                    prependToLog("Submitting request to print shipping label for id $remoteId")

                    coroutineScope.launch {
                        try {
                            val response = withContext(Dispatchers.Default) {
                                // the paper size can be label, legal, letter
                                // For the example app, the default is set as label
                                wcShippingLabelStore.printShippingLabel(site, "label", remoteId)
                            }
                            response.error?.let {
                                prependToLog("${it.type}: ${it.message}")
                            }
                            response.model?.let { base64Content ->
                                // Since this function is used only by Woo testers and the Woo app
                                // only supports API > 21, it's fine to add a check here to support devices
                                // above API 19
                                if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
                                    writePDFToFile(base64Content)?.let { openWebView(it) }
                                }
                            }
                        } catch (e: Exception) {
                            prependToLog("Error: ${e.message}")
                        }
                    }
                }
            }
        }

        verify_address.setOnClickListener {
            selectedSite?.let { site ->
                replaceFragment(WooVerifyAddressFragment.newInstance(site))
            }
        }

        verify_address.setOnClickListener {
            selectedSite?.let { site ->
                coroutineScope.launch {
                    val result = wcShippingLabelStore.getPackageTypes(site)
                    prependToLog("$result")
                }
            }
        }
    }

    private fun showSiteSelectorDialog(selectedPos: Int, listener: StoreSelectorDialog.Listener) {
        fragmentManager?.let { fm ->
            val dialog = StoreSelectorDialog.newInstance(listener, selectedPos)
            dialog.show(fm, "StoreSelectorDialog")
        }
    }

    /**
     * Creates a temporary file for storing captured photos
     */
    @RequiresApi(VERSION_CODES.KITKAT)
    private fun createTempPdfFile(context: Context): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
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

    /**
     * Since this function is used only by Woo testers and the Woo app
     * only supports API > 21, it's fine to leave this method to support only
     * API 19 and above.
     */
    @RequiresApi(VERSION_CODES.KITKAT)
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

    private fun openWebView(file: File) {
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
}
