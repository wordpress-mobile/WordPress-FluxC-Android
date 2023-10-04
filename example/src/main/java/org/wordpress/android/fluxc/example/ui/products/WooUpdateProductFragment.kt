package org.wordpress.android.fluxc.example.ui.products

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_woo_update_product.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction
import org.wordpress.android.fluxc.example.R.layout
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.replaceFragment
import org.wordpress.android.fluxc.example.ui.FloatingLabelEditText
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog.Companion.ARG_LIST_SELECTED_ITEM
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog.Companion.LIST_SELECTOR_REQUEST_CODE
import org.wordpress.android.fluxc.example.ui.products.WooProductCategoriesFragment.Companion.ARG_SELECTED_PRODUCT_CATEGORIES
import org.wordpress.android.fluxc.example.ui.products.WooProductCategoriesFragment.Companion.PRODUCT_CATEGORIES_REQUEST_CODE
import org.wordpress.android.fluxc.example.ui.products.WooProductDownloadsFragment.Companion.ARG_PRODUCT_DOWNLOADS
import org.wordpress.android.fluxc.example.ui.products.WooProductDownloadsFragment.Companion.PRODUCT_DOWNLOADS_REQUEST_CODE
import org.wordpress.android.fluxc.example.ui.products.WooProductDownloadsFragment.ProductFile
import org.wordpress.android.fluxc.example.ui.products.WooProductTagsFragment.Companion.ARG_SELECTED_PRODUCT_TAGS
import org.wordpress.android.fluxc.example.ui.products.WooProductTagsFragment.Companion.PRODUCT_TAGS_REQUEST_CODE
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductModel.ProductTriplet
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductBackOrders
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStockStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductTaxStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductVisibility
import org.wordpress.android.fluxc.store.WCGlobalAttributeStore
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.AddProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductPasswordPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductCreated
import org.wordpress.android.fluxc.store.WCProductStore.OnProductPasswordChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductUpdated
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductPasswordPayload
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductPayload
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.utils.DateUtils
import org.wordpress.android.util.StringUtils
import java.util.Calendar
import javax.inject.Inject

@Suppress("LargeClass")
class WooUpdateProductFragment : Fragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wcProductStore: WCProductStore
    @Inject internal lateinit var wcAttributesStore: WCGlobalAttributeStore
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    private var selectedSitePosition: Int = -1
    private var selectedRemoteProductId: Long? = null
    private var selectedProductModel: WCProductModel? = null
    private var password: String? = null
    private var selectedCategories: List<ProductCategory>? = null
    private var selectedTags: List<ProductTag>? = null
    private var isAddNewProduct: Boolean = false
    private var selectedProductDownloads: List<ProductFile>? = null
    private var attributesChanged: Boolean = false

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    companion object {
        const val ARG_SELECTED_SITE_POS = "ARG_SELECTED_SITE_POS"
        const val ARG_SELECTED_PRODUCT_ID = "ARG_SELECTED_PRODUCT_ID"
        const val ARG_SELECTED_CATEGORIES = "ARG_SELECTED_CATEGORIES"
        const val ARG_SELECTED_TAGS = "ARG_SELECTED_TAGS"
        const val ARG_SELECTED_DOWNLOADS = "ARG_SELECTED_DOWNLOADS"
        const val LIST_RESULT_CODE_TAX_STATUS = 101
        const val LIST_RESULT_CODE_STOCK_STATUS = 102
        const val LIST_RESULT_CODE_BACK_ORDERS = 103
        const val LIST_RESULT_CODE_VISIBILITY = 104
        const val LIST_RESULT_CODE_STATUS = 105
        const val LIST_RESULT_CODE_CATEGORIES = 106
        const val LIST_RESULT_CODE_TAGS = 107
        const val LIST_RESULT_CODE_PRODUCT_TYPE = 108
        const val IS_ADD_NEW_PRODUCT = "IS_ADD_NEW_PRODUCT"

        fun newInstance(selectedSitePosition: Int, isAddNewProduct: Boolean = false): WooUpdateProductFragment {
            val fragment = WooUpdateProductFragment()
            val args = Bundle()
            args.putInt(ARG_SELECTED_SITE_POS, selectedSitePosition)
            args.putBoolean(IS_ADD_NEW_PRODUCT, isAddNewProduct)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            selectedSitePosition = it.getInt(ARG_SELECTED_SITE_POS, 0)
            isAddNewProduct = it.getBoolean(IS_ADD_NEW_PRODUCT, false)
        }
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(layout.fragment_woo_update_product, container, false)

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        super.onStop()
        dispatcher.unregister(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ARG_SELECTED_SITE_POS, selectedSitePosition)
        selectedRemoteProductId?.let { outState.putLong(ARG_SELECTED_PRODUCT_ID, it) }
        selectedCategories?.let { outState.putParcelableArrayList(ARG_SELECTED_CATEGORIES, it as? ArrayList) }
        selectedTags?.let { outState.putParcelableArrayList(ARG_SELECTED_TAGS, it as? ArrayList) }
        selectedProductDownloads.let { outState.putParcelableArrayList(ARG_SELECTED_DOWNLOADS, it as ArrayList) }
    }

    @Suppress("LongMethod", "ComplexMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (selectedProductModel == null) {
            if (!isAddNewProduct) {
                product_enter_product_id.setOnClickListener {
                    showSingleLineDialog(activity, "Enter the remoteProductId of product to fetch:") { editText ->
                        selectedRemoteProductId = editText.text.toString().toLongOrNull()
                        selectedRemoteProductId?.let { id ->
                            updateSelectedProductId(id)
                        } ?: prependToLog("No valid remoteProductId defined...doing nothing")
                    }
                }
            } else {
                val product = WCProductModel().apply {
                    stockStatus = CoreProductStockStatus.IN_STOCK.value
                    status = "publish"
                    virtual = false
                    type = "simple"
                    taxStatus = "taxable"
                    catalogVisibility = "visible"
                }
                enableProductDependentButtons()
                updateProductProperties(product)
                selectedProductModel = product

                product_enter_product_id.visibility = View.GONE
                product_entered_product_id.visibility = View.GONE
            }
        } else {
            updateProductProperties(selectedProductModel!!)
        }

        grouped_product_ids.isEnabled = false
        select_grouped_product_ids.setOnClickListener {
            showSingleLineDialog(activity, "Enter a remoteProductId:") { editText ->
                editText.text.toString().toLongOrNull()?.let { id ->

                    val storedGroupedProductIds =
                            selectedProductModel?.getGroupedProductIdList()?.toMutableList() ?: mutableListOf()
                    storedGroupedProductIds.add(id)
                    selectedProductModel?.groupedProductIds = storedGroupedProductIds.joinToString(
                            separator = ",",
                            prefix = "[",
                            postfix = "]"
                    )
                    selectedProductModel?.groupedProductIds?.let { updateGroupedProductIds(it) }
                } ?: prependToLog("No valid remoteProductId defined...doing nothing")
            }
        }

        cross_sell_product_ids.isEnabled = false
        select_cross_sell_product_ids.setOnClickListener {
            showSingleLineDialog(activity, "Enter a remoteProductId:") { editText ->
                editText.text.toString().toLongOrNull()?.let { id ->
                    val storedCrossSellIds =
                            selectedProductModel?.getCrossSellProductIdList()?.toMutableList() ?: mutableListOf()
                    storedCrossSellIds.add(id)
                    selectedProductModel?.crossSellIds = storedCrossSellIds.joinToString(
                            separator = ",",
                            prefix = "[",
                            postfix = "]"
                    )
                    selectedProductModel?.crossSellIds?.let { updateCrossSellProductIds(it) }
                } ?: prependToLog("No valid remoteProductId defined...doing nothing")
            }
        }

        upsell_product_ids.isEnabled = false
        select_upsell_product_ids.setOnClickListener {
            showSingleLineDialog(activity, "Enter a remoteProductId:") { editText ->
                editText.text.toString().toLongOrNull()?.let { id ->
                    val storedUpsellIds =
                            selectedProductModel?.getUpsellProductIdList()?.toMutableList() ?: mutableListOf()
                    storedUpsellIds.add(id)
                    selectedProductModel?.upsellIds = storedUpsellIds.joinToString(
                            separator = ",",
                            prefix = "[",
                            postfix = "]"
                    )
                    selectedProductModel?.upsellIds?.let { updateUpsellProductIds(it) }
                } ?: prependToLog("No valid remoteProductId defined...doing nothing")
            }
        }

        product_name.onTextChanged { selectedProductModel?.name = it }
        product_description.onTextChanged { selectedProductModel?.description = it }
        product_sku.onTextChanged { selectedProductModel?.sku = it }
        product_short_desc.onTextChanged { selectedProductModel?.shortDescription = it }
        product_regular_price.onTextChanged { selectedProductModel?.regularPrice = it }
        product_sale_price.onTextChanged { selectedProductModel?.salePrice = it }
        product_width.onTextChanged { selectedProductModel?.width = it }
        product_height.onTextChanged { selectedProductModel?.height = it }
        product_length.onTextChanged { selectedProductModel?.length = it }
        product_weight.onTextChanged { selectedProductModel?.weight = it }
        product_stock_quantity.onTextChanged { text ->
            text.toDoubleOrNull()?.let { selectedProductModel?.stockQuantity = it }
        }

        product_sold_individually.setOnCheckedChangeListener { _, isChecked ->
            selectedProductModel?.soldIndividually = isChecked
        }

        product_manage_stock.setOnCheckedChangeListener { _, isChecked ->
            selectedProductModel?.manageStock = isChecked
            for (i in 0 until manageStockContainer.childCount) {
                val child = manageStockContainer.getChildAt(i)
                if (child is Button || child is FloatingLabelEditText) {
                    child.isEnabled = isChecked
                }
            }
        }

        product_type.setOnClickListener {
            showListSelectorDialog(
                    CoreProductType.values().map { it.value }.toList(),
                    LIST_RESULT_CODE_PRODUCT_TYPE, selectedProductModel?.type
            )
        }

        product_tax_status.setOnClickListener {
            showListSelectorDialog(
                    CoreProductTaxStatus.values().map { it.value }.toList(),
                    LIST_RESULT_CODE_TAX_STATUS, selectedProductModel?.taxStatus
            )
        }

        product_stock_status.setOnClickListener {
            showListSelectorDialog(
                    CoreProductStockStatus.values().map { it.value }.toList(),
                    LIST_RESULT_CODE_STOCK_STATUS, selectedProductModel?.stockStatus
            )
        }

        product_back_orders.setOnClickListener {
            showListSelectorDialog(
                    CoreProductBackOrders.values().map { it.value }.toList(),
                    LIST_RESULT_CODE_BACK_ORDERS, selectedProductModel?.backorders
            )
        }

        product_from_date.setOnClickListener {
            showDatePickerDialog(
                    product_from_date.text.toString(),
                    OnDateSetListener { _, year, month, dayOfMonth ->
                        product_from_date.text = DateUtils.getFormattedDateString(year, month, dayOfMonth)
                        selectedProductModel?.dateOnSaleFromGmt = product_from_date.text.toString()
                    })
        }

        product_to_date.setOnClickListener {
            showDatePickerDialog(product_to_date.text.toString(), OnDateSetListener { _, year, month, dayOfMonth ->
                product_to_date.text = DateUtils.getFormattedDateString(year, month, dayOfMonth)
                selectedProductModel?.dateOnSaleToGmt = product_to_date.text.toString()
            })
        }

        product_update.setOnClickListener {
            getWCSite()?.let { site ->
                // update categories only if new categories has been selected
                selectedCategories?.let {
                    selectedProductModel?.categories =
                            it.map { it.toProductTriplet().toJson() }.toString()
                }

                selectedTags?.let {
                    selectedProductModel?.tags =
                            it.map { it.toProductTriplet().toJson() }.toString()
                }

                selectedProductDownloads?.let {
                    selectedProductModel?.downloads =
                            it.map { it.toWCProductFileModel().toJson() }.toString()
                }

                if (isAddNewProduct) {
                    val payload = AddProductPayload(site, selectedProductModel!!)
                    dispatcher.dispatch(WCProductActionBuilder.newAddProductAction(payload))
                } else if (selectedProductModel?.remoteProductId != null) {
                    val payload = UpdateProductPayload(site, selectedProductModel!!)
                    dispatcher.dispatch(WCProductActionBuilder.newUpdateProductAction(payload))
                    val updatedPassword = product_password.getText()
                    if (updatedPassword != password) {
                        val passwordPayload = UpdateProductPasswordPayload(
                                site,
                                selectedProductModel?.remoteProductId!!,
                                updatedPassword
                        )
                        dispatcher.dispatch(WCProductActionBuilder.newUpdateProductPasswordAction(passwordPayload))
                    }
                } else {
                    prependToLog("No valid remoteProductId defined...doing nothing")
                }
            } ?: prependToLog("No site found...doing nothing")
        }

        product_status.setOnClickListener {
            showListSelectorDialog(
                    CoreProductStatus.values().map { it.value }.toList(),
                    LIST_RESULT_CODE_STATUS, selectedProductModel?.status
            )
        }

        product_catalog_visibility.setOnClickListener {
            showListSelectorDialog(
                    CoreProductVisibility.values().map { it.value }.toList(),
                    LIST_RESULT_CODE_VISIBILITY, selectedProductModel?.catalogVisibility
            )
        }

        select_product_categories.setOnClickListener {
            getWCSite()?.let {
                val categories = wcProductStore.getProductCategoriesForSite(it)
                        .map { ProductCategory(it.remoteCategoryId, it.name, it.slug) }

                val selectedProductCategories = selectedCategories ?: selectedProductModel?.getCategoryList()
                        ?.map { it.toProductCategory() }

                replaceFragment(
                        WooProductCategoriesFragment.newInstance(
                                fragment = this,
                                productCategories = categories,
                                resultCode = LIST_RESULT_CODE_CATEGORIES,
                                selectedProductCategories = selectedProductCategories?.toMutableList(),
                                selectedSitePosition = selectedSitePosition
                        )
                )
            }
        }

        select_product_tags.setOnClickListener {
            getWCSite()?.let {
                val tags = wcProductStore.getTagsForSite(it)
                        .map { ProductTag(it.remoteTagId, it.name, it.slug) }

                val selectedProductTags = selectedTags ?: selectedProductModel?.getTagList()
                        ?.map { it.toProductTag() }

                replaceFragment(
                        WooProductTagsFragment.newInstance(
                                fragment = this,
                                productTags = tags,
                                resultCode = LIST_RESULT_CODE_TAGS,
                                selectedProductTags = selectedProductTags?.toMutableList()
                        )
                )
            }
        }

        product_is_featured.setOnCheckedChangeListener { _, isChecked ->
            selectedProductModel?.featured = isChecked
        }

        product_reviews_allowed.setOnCheckedChangeListener { _, isChecked ->
            selectedProductModel?.reviewsAllowed = isChecked
        }

        product_is_virtual.setOnCheckedChangeListener { _, isChecked ->
            selectedProductModel?.virtual = isChecked
        }

        attach_attribute.setOnClickListener { onAttachAttributeToProductButtonClicked() }
        detach_attribute.setOnClickListener { onDetachAttributeFromProductButtonClicked() }
        generate_variation.setOnClickListener { onGenerateVariationButtonClicked() }
        delete_variation.setOnClickListener { onDeleteVariationButtonClicked() }

        product_purchase_note.onTextChanged { selectedProductModel?.purchaseNote = it }

        product_slug.onTextChanged { selectedProductModel?.slug = it }

        product_external_url.onTextChanged { selectedProductModel?.externalUrl = it }

        product_button_text.onTextChanged { selectedProductModel?.buttonText = it }

        product_menu_order.onTextChanged { selectedProductModel?.menuOrder = StringUtils.stringToInt(it) }

        product_downloadable_checkbox.setOnCheckedChangeListener { _, isChecked ->
            selectedProductModel?.downloadable = isChecked
            product_update_downloads_button.isEnabled = isChecked
            product_download_expiry.isEnabled = isChecked
            product_download_limit.isEnabled = isChecked
        }

        product_downloads.isEnabled = false
        product_update_downloads_button.setOnClickListener {
            replaceFragment(
                    WooProductDownloadsFragment.newInstance(
                            fragment = this,
                            productDownloads = selectedProductDownloads
                                    ?: selectedProductModel?.getDownloadableFiles()?.map {
                                        ProductFile(it.id, it.name, it.url)
                                    }
                                    ?: emptyList()
                    )
            )
        }

        product_download_expiry.onTextChanged { text ->
            text.toIntOrNull()?.let { selectedProductModel?.downloadExpiry = it }
        }

        product_download_limit.onTextChanged { text ->
            text.toLongOrNull()?.let { selectedProductModel?.downloadLimit = it }
        }

        savedInstanceState?.let { bundle ->
            selectedRemoteProductId = bundle.getLong(ARG_SELECTED_PRODUCT_ID)
            selectedSitePosition = bundle.getInt(ARG_SELECTED_SITE_POS)
            selectedCategories = bundle.getParcelableArrayList(ARG_SELECTED_CATEGORIES)
            selectedTags = bundle.getParcelableArrayList(ARG_SELECTED_TAGS)
            selectedProductDownloads = bundle.getParcelableArrayList(ARG_SELECTED_DOWNLOADS) ?: emptyList()
        }
        selectedRemoteProductId?.let { updateSelectedProductId(it) }
    }

    @Suppress("ComplexMethod")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LIST_SELECTOR_REQUEST_CODE) {
            val selectedItem = data?.getStringExtra(ARG_LIST_SELECTED_ITEM)
            when (resultCode) {
                LIST_RESULT_CODE_TAX_STATUS -> {
                    selectedItem?.let {
                        product_tax_status.text = it
                        selectedProductModel?.taxStatus = it
                    }
                }
                LIST_RESULT_CODE_STOCK_STATUS -> {
                    selectedItem?.let {
                        product_stock_status.text = it
                        selectedProductModel?.stockStatus = it
                    }
                }
                LIST_RESULT_CODE_BACK_ORDERS -> {
                    selectedItem?.let {
                        product_back_orders.text = it
                        selectedProductModel?.backorders = it
                    }
                }
                LIST_RESULT_CODE_STATUS -> {
                    selectedItem?.let {
                        product_status.text = it
                        selectedProductModel?.status = it
                    }
                }
                LIST_RESULT_CODE_PRODUCT_TYPE -> {
                    selectedItem?.let {
                        product_type.text = it
                        selectedProductModel?.type = it
                    }
                }
                LIST_RESULT_CODE_VISIBILITY -> {
                    selectedItem?.let {
                        product_catalog_visibility.text = it
                        selectedProductModel?.catalogVisibility = it
                    }
                }
            }
        } else if (requestCode == PRODUCT_CATEGORIES_REQUEST_CODE) {
            this.selectedCategories = data?.getParcelableArrayListExtra(ARG_SELECTED_PRODUCT_CATEGORIES)
        } else if (requestCode == PRODUCT_TAGS_REQUEST_CODE) {
            this.selectedTags = data?.getParcelableArrayListExtra(ARG_SELECTED_PRODUCT_TAGS)
        } else if (requestCode == PRODUCT_DOWNLOADS_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            this.selectedProductDownloads = data!!.getParcelableArrayListExtra(ARG_PRODUCT_DOWNLOADS)
        }
    }

    private fun updateGroupedProductIds(storedGroupedProductIds: String) {
        grouped_product_ids.setText(storedGroupedProductIds)
    }

    private fun updateCrossSellProductIds(storedCrossSellProductIds: String) {
        cross_sell_product_ids.setText(storedCrossSellProductIds)
    }

    private fun updateUpsellProductIds(storedUpsellProductIds: String) {
        upsell_product_ids.setText(storedUpsellProductIds)
    }

    @SuppressLint("SetTextI18n")
    private fun updateSelectedProductId(remoteProductId: Long) {
        getWCSite()?.let { siteModel ->
            enableProductDependentButtons()
            product_entered_product_id.text = remoteProductId.toString()

            // fetch the password for this product
            val action = WCProductActionBuilder
                    .newFetchProductPasswordAction(FetchProductPasswordPayload(siteModel, remoteProductId))
            dispatcher.dispatch(action)

            selectedProductModel = wcProductStore.getProductByRemoteId(siteModel, remoteProductId)?.also {
                updateProductProperties(it)
            } ?: WCProductModel().apply { this.remoteProductId = remoteProductId }
        } ?: prependToLog("No valid site found...doing nothing")
    }

    @Suppress("TooGenericExceptionCaught")
    private fun onAttachAttributeToProductButtonClicked() {
        try {
            showSingleLineDialog(
                    activity,
                    "Enter the attribute ID you want to attach:"
            ) { attributeIdEditText ->
                showSingleLineDialog(
                        activity,
                        "Enter the term name you want to attach:"
                ) { termEditText ->
                    coroutineScope.launch {
                        takeAsyncRequestWithValidSite { site ->
                            attributesChanged = true
                            handleProductAttributesSync(
                                    site,
                                    attributeIdEditText.text.toString().toLongOrNull() ?: 0,
                                    termEditText.text.toString(),
                                    selectedProductModel
                            )
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            prependToLog("Couldn't attach Attribute Term. Error: ${ex.message}")
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun onDetachAttributeFromProductButtonClicked() {
        try {
            showSingleLineDialog(
                    activity,
                    "Enter the attribute ID you want to detach:"
            ) { attributeIdEditText ->
                coroutineScope.launch {
                    takeAsyncRequestWithValidSite { site ->
                        selectedProductModel
                                ?.apply {
                                    removeAttribute(attributeIdEditText.text.toString().toIntOrNull() ?: 0)
                                }?.let { product ->
                                    attributesChanged = true
                                    WCProductActionBuilder.newUpdateProductAction(
                                            UpdateProductPayload(site, product)
                                    )
                                            .let { dispatcher.dispatch(it) }
                                } ?: prependToLog("Couldn't detach Attribute.")
                    }
                }
            }
        } catch (ex: Exception) {
            prependToLog("Couldn't detach Attribute Term. Error: ${ex.message}")
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun onGenerateVariationButtonClicked() {
        try {
            coroutineScope.launch {
                takeAsyncRequestWithValidSite { site ->
                    selectedProductModel?.let { wcProductStore.generateEmptyVariation(site, it) }
                }?.model?.let { prependToLog("Variation ${it.remoteVariationId} created") }
                        ?: prependToLog("Couldn't create Variation.")
            }
        } catch (ex: Exception) {
            prependToLog("Couldn't create Variation. Error: ${ex.message}")
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun onDeleteVariationButtonClicked() {
        try {
            showSingleLineDialog(
                    activity,
                    "Enter the variation ID you want to delete:"
            ) { variationIdEditText ->
                coroutineScope.launch {
                    takeAsyncRequestWithValidSite { site ->
                        variationIdEditText.text.toString().toLongOrNull()
                                ?.let { variationID ->
                                    wcProductStore.deleteVariation(
                                            site,
                                            selectedProductModel?.remoteProductId ?: 0L,
                                            variationID
                                    )
                                }
                    }?.model?.let { prependToLog("Variation ${it.remoteVariationId} deleted") }
                            ?: prependToLog("Couldn't delete Variation.")
                }
            }
        } catch (ex: Exception) {
            prependToLog("Couldn't delete Variation. Error: ${ex.message}")
        }
    }

    /***
     * This method will acquire the requested Attribute
     * with the respective Terms and assign to the Product the fetched
     * data with the selected Terms, efectively updating the Product with
     * a new Attribute + Terms set
     *
     * @param site in order to operate with the correct Woo Store
     * @param attributeId to fetch the selected Attribute
     * @param termName to update the product with the selected term
     * @param product as the product to update with the selected Attribute
     */
    private suspend fun handleProductAttributesSync(
        site: SiteModel,
        attributeId: Long,
        termName: String,
        product: WCProductModel?
    ) = wcAttributesStore.fetchAttribute(
            site = site,
            attributeID = attributeId,
            withTerms = true
    )
            .model
            ?.asProductAttributeModel(listOf(termName))
            ?.takeIf { it.options.isNotEmpty() }
            ?.apply {
                product?.attributeList
                        ?.firstOrNull { it.id == attributeId }
                        ?.options
                        ?.let { this.options.addAll(it) }
            }
            ?.run { product?.updateAttribute(this) }
            ?.let { updatedProduct ->
                WCProductActionBuilder
                        .newUpdateProductAction(
                                UpdateProductPayload(site, updatedProduct)
                        ).let { dispatcher.dispatch(it) }
            } ?: withContext(Dispatchers.Main) { prependToLog("Looks like this isn't a valid Term name") }

    private fun updateProductProperties(it: WCProductModel) {
        product_name.setText(it.name)
        product_description.setText(it.description)
        product_sku.setText(it.sku)
        product_short_desc.setText(it.shortDescription)
        product_regular_price.setText(it.regularPrice)
        product_sale_price.setText(it.salePrice)
        product_width.setText(it.width)
        product_height.setText(it.height)
        product_length.setText(it.length)
        product_weight.setText(it.weight)
        product_tax_status.text = it.taxStatus
        product_type.text = it.type
        product_sold_individually.isChecked = it.soldIndividually
        product_from_date.text = it.dateOnSaleFromGmt.split('T')[0]
        product_to_date.text = it.dateOnSaleToGmt.split('T')[0]
        product_manage_stock.isChecked = it.manageStock
        product_stock_status.text = it.stockStatus
        product_back_orders.text = it.backorders
        product_stock_quantity.setText(it.stockQuantity.toString())
        product_stock_quantity.isEnabled = product_manage_stock.isChecked
        product_catalog_visibility.text = it.catalogVisibility
        product_status.text = it.status
        product_slug.setText(it.slug)
        product_is_featured.isChecked = it.featured
        product_reviews_allowed.isChecked = it.reviewsAllowed
        product_is_virtual.isChecked = it.virtual
        product_purchase_note.setText(it.purchaseNote)
        product_menu_order.setText(it.menuOrder.toString())
        product_external_url.setText(it.externalUrl)
        product_button_text.setText(it.buttonText)
        updateGroupedProductIds(it.groupedProductIds)
        updateCrossSellProductIds(it.crossSellIds)
        updateUpsellProductIds(it.upsellIds)
        product_categories.setText(
                selectedCategories?.joinToString(", ") { it.name }
                        ?: it.getCommaSeparatedCategoryNames()
        )
        product_tags.setText(
                selectedTags?.joinToString(", ") { it.name }
                        ?: it.getCommaSeparatedTagNames()
        )
        product_downloadable_checkbox.isChecked = it.downloadable
        product_downloads.setText(
                "Files count: ${selectedProductDownloads?.size ?: it.getDownloadableFiles().size}"
        )
        product_update_downloads_button.isEnabled = it.downloadable
        product_download_limit.setText(it.downloadLimit.toString())
        product_download_limit.isEnabled = it.downloadable
        product_download_expiry.setText(it.downloadExpiry.toString())
        product_download_expiry.isEnabled = it.downloadable

        if (it.type == CoreProductType.VARIABLE.value) {
            attach_attribute.isEnabled = true
            detach_attribute.isEnabled = true
            generate_variation.isEnabled = true
            delete_variation.isEnabled = true
        }
    }

    private fun showListSelectorDialog(listItems: List<String>, resultCode: Int, selectedItem: String?) {
        fragmentManager?.let { fm ->
            val dialog = ListSelectorDialog.newInstance(
                    this, listItems, resultCode, selectedItem
            )
            dialog.show(fm, "ListSelectorDialog")
        }
    }

    private fun showDatePickerDialog(dateString: String?, listener: OnDateSetListener) {
        val date = if (dateString.isNullOrEmpty()) {
            DateUtils.getCurrentDateString()
        } else dateString
        val calendar = DateUtils.getCalendarInstance(date)
        DatePickerDialog(
                requireActivity(), listener, calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE)
        )
                .show()
    }

    private fun getWCSite() = wooCommerceStore.getWooCommerceSites().getOrNull(selectedSitePosition)

    private fun enableProductDependentButtons() {
        for (i in 0 until productContainer.childCount) {
            val child = productContainer.getChildAt(i)
            if (child is Button || child is EditText) {
                child.isEnabled = true
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductUpdated(event: OnProductUpdated) {
        if (event.isError) {
            prependToLog("Error from " + event.causeOfChange + " - error: " + event.error.type)
            return
        }
        prependToLog("Product updated ${event.rowsAffected}")

        selectedRemoteProductId?.let {
            selectedCategories = null
            selectedTags = null
            selectedProductDownloads = null
            updateSelectedProductId(it)
        }

        takeIf { attributesChanged }?.getWCSite()?.let { site ->
            wcProductStore.getProductByRemoteId(site, event.remoteProductId)?.let {
                logProduct(it)
                attributesChanged = false
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductPasswordChanged(event: OnProductPasswordChanged) {
        when {
            event.isError -> {
                event.error?.let {
                    prependToLog("onProductPasswordChanged has unexpected error: ${it.type}, ${it.message}")
                }
            }
            event.causeOfChange == WCProductAction.FETCH_PRODUCT_PASSWORD -> {
                prependToLog("Password fetched: ${event.password}")
                product_password.setText(event.password ?: "")
                password = event.password
            }
            event.causeOfChange == WCProductAction.UPDATE_PRODUCT_PASSWORD -> {
                prependToLog("Password updated: ${event.password}")
                product_password.setText(event.password ?: "")
                password = event.password
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductCreated(event: OnProductCreated) {
        if (event.isError) {
            prependToLog("Error from " + event.causeOfChange + " - error: " + event.error.type)
            return
        }
        prependToLog("Product created! ${event.rowsAffected} - new product id is: ${event.remoteProductId}")
    }

    private fun logProduct(product: WCProductModel) = product.apply {
        attributeList.forEach { logAttribute(it) }
        prependToLog("  Product slug: ${slug.ifEmpty { "Slug not available" }}")
        prependToLog("  Product type: ${type.ifEmpty { "Type not available" }}")
        prependToLog("  Product name: ${name.ifEmpty { "Product name not available" }}")
        prependToLog("  Product remote id: $remoteProductId")
        prependToLog("  --------- Product ---------")
    }

    private fun logAttribute(attribute: WCProductModel.ProductAttribute) = attribute.let {
        logAttributeOptions(attribute.options)
        prependToLog("  Attribute name: ${it.name.ifEmpty { "Attribute name not available" }}")
        prependToLog("  Attribute remote id: ${it.id}")
        prependToLog("  --------- Product Attribute ---------")
    }

    private fun logAttributeOptions(options: List<String>) {
        options.forEach { prependToLog("  $it") }
        prependToLog("  --------- Attribute Options ---------")
    }

    private suspend inline fun <T> takeAsyncRequestWithValidSite(crossinline action: suspend (SiteModel) -> T) =
            getWCSite()?.let {
                withContext(Dispatchers.Default) {
                    action(it)
                }
            }

    @Parcelize
    data class ProductCategory(
        val id: Long,
        val name: String,
        val slug: String
    ) : Parcelable {
        fun toProductTriplet(): ProductTriplet {
            return ProductTriplet(this.id, this.name, this.slug)
        }
    }

    @Parcelize
    data class ProductTag(
        val id: Long,
        val name: String,
        val slug: String
    ) : Parcelable {
        fun toProductTriplet(): ProductTriplet {
            return ProductTriplet(this.id, this.name, this.slug)
        }
    }

    private fun ProductTriplet.toProductCategory(): ProductCategory {
        return ProductCategory(this.id, this.name, this.slug)
    }

    private fun ProductTriplet.toProductTag(): ProductTag {
        return ProductTag(this.id, this.name, this.slug)
    }
}
