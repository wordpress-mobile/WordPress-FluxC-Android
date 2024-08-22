package org.wordpress.android.fluxc.example.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.ADDED_PRODUCT_CATEGORY
import org.wordpress.android.fluxc.action.WCProductAction.ADDED_PRODUCT_TAGS
import org.wordpress.android.fluxc.action.WCProductAction.DELETED_PRODUCT
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCTS
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCT_CATEGORIES
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCT_TAGS
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_SINGLE_PRODUCT_SHIPPING_CLASS
import org.wordpress.android.fluxc.example.databinding.FragmentWooProductsBinding
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.replaceFragment
import org.wordpress.android.fluxc.example.ui.StoreSelectingFragment
import org.wordpress.android.fluxc.example.ui.metadata.CustomFieldsFragment
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.model.WCProductImageModel
import org.wordpress.android.fluxc.model.metadata.MetaDataParentItemType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStockStatus
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.WCAddonsStore
import org.wordpress.android.fluxc.store.WCProductReportsStore
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.AddProductTagsPayload
import org.wordpress.android.fluxc.store.WCProductStore.DeleteProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductCategoriesPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductReviewsPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductShippingClassListPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductSkuAvailabilityPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductTagsPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductVariationsPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductsPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductReviewPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductShippingClassPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductCategoryChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductImagesChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductShippingClassesChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductSkuAvailabilityChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductTagChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductsSearched
import org.wordpress.android.fluxc.store.WCProductStore.SearchProductsPayload
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductImagesPayload
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@Suppress("LargeClass")
class WooProductsFragment : StoreSelectingFragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wcProductStore: WCProductStore
    @Inject lateinit var addonsStore: WCAddonsStore
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore
    @Inject internal lateinit var mediaStore: MediaStore
    @Inject internal lateinit var productStockReportStore: WCProductReportsStore

    private var pendingFetchSingleProductShippingClassRemoteId: Long? = null

    private var pendingFetchProductVariationsProductRemoteId: Long? = null
    private var pendingFetchSingleProductVariationOffset: Int = 0

    private var pendingFetchProductShippingClassListOffset: Int = 0
    private var pendingFetchProductCategoriesOffset: Int = 0
    private var pendingFetchProductTagsOffset: Int = 0

    private var enteredCategoryName: String? = null
    private val enteredTagNames: MutableList<String> = mutableListOf()

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private var binding: FragmentWooProductsBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWooProductsBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    @Suppress("LongMethod", "ComplexMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.fetchSingleProduct?.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(
                    activity,
                    "Enter the remoteProductId of product to fetch:"
                ) { editText ->
                    editText.text.toString().toLongOrNull()?.let { remoteProductId ->
                        prependToLog("Submitting request to fetch product by remoteProductID $remoteProductId")
                        coroutineScope.launch {
                            val result = wcProductStore.fetchSingleProduct(
                                FetchSingleProductPayload(
                                    site,
                                    remoteProductId
                                )
                            )

                            val product =
                                wcProductStore.getProductByRemoteId(site, result.remoteProductId)
                            product?.let {
                                val numVariations = it.getVariationIdList().size
                                if (numVariations > 0) {
                                    prependToLog("Single product with $numVariations variations fetched! ${it.name}")
                                } else {
                                    prependToLog("Single product fetched! ${it.name}")
                                }
                            }
                                ?: prependToLog("WARNING: Fetched product not found in the local database!")
                        }
                    } ?: prependToLog("No valid remoteOrderId defined...doing nothing")
                }
            }
        }

        binding?.fetchSingleVariation?.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(
                    activity,
                    "Enter the remoteProductId of variation to fetch:"
                ) { productIdText ->
                    val productRemoteId = productIdText.text.toString().toLongOrNull()
                    productRemoteId?.let { productId ->
                        showSingleLineDialog(
                            activity,
                            "Enter the remoteVariationId of variation to fetch:"
                        ) { variationIdText ->
                            variationIdText.text.toString().toLongOrNull()?.let { variationId ->
                                coroutineScope.launch {
                                    prependToLog(
                                        "Submitting request to fetch product by " +
                                            "remoteProductId $productRemoteId, " +
                                            "remoteVariationProductID $variationId"
                                    )
                                    val result = wcProductStore.fetchSingleVariation(
                                        site,
                                        productId,
                                        variationId
                                    )
                                    prependToLog("Fetching single variation " +
                                        "${result.error?.let { "failed" } ?: "was successful"}"
                                    )
                                    val variation = wcProductStore.getVariationByRemoteId(
                                        site,
                                        result.remoteProductId,
                                        result.remoteVariationId
                                    )
                                    variation?.let {
                                        prependToLog("Variation with id! ${it.remoteVariationId} found in local db")
                                    }
                                        ?: prependToLog("WARNING: Fetched product not found in the local database!")
                                }
                            } ?: prependToLog("No valid remoteVariationId defined...doing nothing")
                        }
                    } ?: prependToLog("No valid remoteProductId defined...doing nothing")
                }
            }
        }

        binding?.fetchProductSkuAvailability?.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(
                    activity,
                    "Enter a product SKU:"
                ) { editText ->
                    val payload = FetchProductSkuAvailabilityPayload(site, editText.text.toString())
                    dispatcher.dispatch(
                        WCProductActionBuilder.newFetchProductSkuAvailabilityAction(
                            payload
                        )
                    )
                }
            }
        }

        binding?.fetchProducts?.setOnClickListener {
            selectedSite?.let { site ->
                val payload = FetchProductsPayload(site)
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductsAction(payload))
            }
        }

        binding?.fetchProductsWithFilters?.setOnClickListener {
            replaceFragment(WooProductFiltersFragment.newInstance(selectedPos))
        }

        binding?.fetchSpecificProducts?.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(
                    activity,
                    "Enter remote product IDs, separated by comma:"
                ) { editText ->
                    val ids = editText.text.toString().replace(" ", "").split(",").mapNotNull {
                        val id = it.toLongOrNull()
                        if (id == null) {
                            prependToLog("$it is not a valid remote product ID, ignoring...")
                        }
                        id
                    }

                    if (ids.isNotEmpty()) {
                        coroutineScope.launch {
                            val result = wcProductStore.fetchProducts(
                                site,
                                includedProductIds = ids
                            )
                            if (result.isError) {
                                prependToLog("Fetching products failed: ${result.error.message}")
                            } else {
                                val products = wcProductStore.getProductsByRemoteIds(site, ids)
                                prependToLog("${products.size} were fetched")
                                prependToLog("$products")
                            }
                        }
                    } else {
                        prependToLog("No valid product IDs...doing nothing")
                    }
                }
            }
        }

        binding?.searchProducts?.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(
                    activity,
                    "Enter a search query:"
                ) { editText ->
                    val payload = SearchProductsPayload(
                        site = site,
                        searchQuery = editText.text.toString()
                    )
                    dispatcher.dispatch(WCProductActionBuilder.newSearchProductsAction(payload))
                }
            }
        }

        binding?.searchProductsSku?.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(
                    activity,
                    "Enter a SKU to search for:"
                ) { editText ->
                    val payload = SearchProductsPayload(
                        site = site,
                        searchQuery = editText.text.toString(),
                        skuSearchOptions = WCProductStore.SkuSearchOptions.PartialMatch
                    )
                    dispatcher.dispatch(WCProductActionBuilder.newSearchProductsAction(payload))
                }
            }
        }

        binding?.fetchProductVariations?.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(
                    activity,
                    "Enter the remoteProductId of product to fetch variations:"
                ) { editText ->
                    editText.text.toString().toLongOrNull()?.let { id ->
                        coroutineScope.launch {
                            pendingFetchProductVariationsProductRemoteId = id
                            prependToLog("Submitting request to fetch product variations by remoteProductID $id")
                            val result = wcProductStore.fetchProductVariations(
                                FetchProductVariationsPayload(
                                    site,
                                    id
                                )
                            )
                            prependToLog(
                                "Fetched ${result.rowsAffected} product variants. " +
                                    "More variants available ${result.canLoadMore}"
                            )
                            if (result.canLoadMore) {
                                pendingFetchSingleProductVariationOffset += result.rowsAffected
                                binding?.loadMoreProductVariations?.visibility = View.VISIBLE
                                binding?.loadMoreProductVariations?.isEnabled = true
                            } else {
                                pendingFetchSingleProductVariationOffset = 0
                                binding?.loadMoreProductVariations?.isEnabled = false
                            }
                        }
                    } ?: prependToLog("No valid remoteProductId defined...doing nothing")
                }
            }
        }

        binding?.loadMoreProductVariations?.setOnClickListener {
            selectedSite?.let { site ->
                pendingFetchProductVariationsProductRemoteId?.let { id ->
                    coroutineScope.launch {
                        prependToLog("Submitting offset request to fetch product variations by remoteProductID $id")
                        val payload = FetchProductVariationsPayload(
                            site, id, offset = pendingFetchSingleProductVariationOffset
                        )
                        val result = wcProductStore.fetchProductVariations(payload)
                        prependToLog(
                            "Fetched ${result.rowsAffected} product variants. " +
                                "More variants available ${result.canLoadMore}"
                        )
                        if (result.canLoadMore) {
                            pendingFetchSingleProductVariationOffset += result.rowsAffected
                            binding?.loadMoreProductVariations?.visibility = View.VISIBLE
                            binding?.loadMoreProductVariations?.isEnabled = true
                        } else {
                            pendingFetchSingleProductVariationOffset = 0
                            binding?.loadMoreProductVariations?.isEnabled = false
                        }
                    }
                } ?: prependToLog("No valid remoteProductId defined...doing nothing")
            }
        }

        binding?.fetchReviewsForProduct?.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(
                    activity,
                    "Enter the remoteProductId of product to fetch reviews:"
                ) { editText ->
                    val remoteProductId = editText.text.toString().toLongOrNull()
                    remoteProductId?.let { id ->
                        coroutineScope.launch {
                            prependToLog("Submitting request to fetch product reviews for remoteProductID $id")
                            val result = wcProductStore.fetchProductReviews(
                                FetchProductReviewsPayload(
                                    site,
                                    productIds = listOf(remoteProductId)
                                ),
                                deletePreviouslyCachedReviews = false
                            )
                            prependToLog("Fetched ${result.rowsAffected} product reviews")
                        }
                    } ?: prependToLog("No valid remoteProductId defined...doing nothing")
                }
            }
        }

        binding?.fetchAllReviews?.setOnClickListener {
            selectedSite?.let { site ->
                coroutineScope.launch {
                    prependToLog("Submitting request to fetch product reviews for site ${site.id}")
                    val payload = FetchProductReviewsPayload(site)
                    val result = wcProductStore.fetchProductReviews(
                        payload,
                        deletePreviouslyCachedReviews = false
                    )
                    prependToLog("Fetched ${result.rowsAffected} product reviews")
                }
            }
        }

        binding?.fetchReviewById?.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(
                    activity,
                    "Enter the remoteReviewId of the review to fetch:"
                ) { editText ->
                    val reviewId = editText.text.toString().toLongOrNull()
                    reviewId?.let { id ->
                        coroutineScope.launch {
                            prependToLog("Submitting request to fetch product review for ID $id")
                            val payload = FetchSingleProductReviewPayload(site, id)
                            val result = wcProductStore.fetchSingleProductReview(payload)
                            if (!result.isError) {
                                prependToLog("Fetched ${result.rowsAffected} single product review")
                            } else {
                                prependToLog("Fetching single product review FAILED")
                            }
                        }
                    } ?: prependToLog("No valid remoteReviewId defined...doing nothing")
                }
            }
        }

        binding?.updateReviewStatus?.setOnClickListener {
            selectedSite?.let { site ->
                coroutineScope.launch {
                    val id = showSingleLineDialog(
                        activity = requireActivity(),
                        message = "Enter the remoteReviewId of the review",
                        isNumeric = true
                    )?.toLongOrNull()
                    if (id == null) {
                        prependToLog("Please enter a valid id")
                        return@launch
                    }
                    val newStatus = showSingleLineDialog(
                        activity = requireActivity(),
                        message = "Enter the new status: (approved|hold|spam|trash)"
                    )
                    if (newStatus == null) {
                        prependToLog("Please enter a valid status")
                        return@launch
                    }

                    val result = wcProductStore.updateProductReviewStatus(
                        site = site, reviewId = id, newStatus = newStatus
                    )

                    if (!result.isError) {
                        prependToLog("Product Review status updated successfully")
                    } else {
                        prependToLog(
                            "Product Review status update failed, " +
                                "${result.error.type} ${result.error.message}"
                        )
                    }
                }
            }
        }

        binding?.fetchProductShippingClass?.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(
                    activity,
                    "Enter the remoteShippingClassId of the site to fetch:"
                ) { editText ->
                    pendingFetchSingleProductShippingClassRemoteId =
                        editText.text.toString().toLongOrNull()
                    pendingFetchSingleProductShippingClassRemoteId?.let { id ->
                        prependToLog("Submitting request to fetch product shipping class for ID $id")
                        val payload = FetchSingleProductShippingClassPayload(site, id)
                        dispatcher.dispatch(
                            WCProductActionBuilder.newFetchSingleProductShippingClassAction(
                                payload
                            )
                        )
                    } ?: prependToLog("No valid remoteShippingClassId defined...doing nothing")
                }
            }
        }

        binding?.fetchProductShippingClasses?.setOnClickListener {
            selectedSite?.let { site ->
                prependToLog("Submitting request to fetch product shipping classes for site ${site.id}")
                val payload = FetchProductShippingClassListPayload(site)
                dispatcher.dispatch(
                    WCProductActionBuilder.newFetchProductShippingClassListAction(
                        payload
                    )
                )
            }
        }

        binding?.loadMoreProductShippingClasses?.setOnClickListener {
            selectedSite?.let { site ->
                prependToLog("Submitting offset request to fetch product shipping classes for site ${site.id}")
                val payload = FetchProductShippingClassListPayload(
                    site, offset = pendingFetchProductShippingClassListOffset
                )
                dispatcher.dispatch(
                    WCProductActionBuilder.newFetchProductShippingClassListAction(
                        payload
                    )
                )
            }
        }

        binding?.fetchProductCategories?.setOnClickListener {
            selectedSite?.let { site ->
                prependToLog("Submitting request to fetch product categories for site ${site.id}")
                val payload = FetchProductCategoriesPayload(site)
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductCategoriesAction(payload))
            }
        }

        binding?.observeProductCategories?.setOnClickListener {
            selectedSite?.let { site ->
                coroutineScope.launch {
                    val categories = wcProductStore.observeCategories(site).first()
                    prependToLog("Categories: $categories")
                }
            }
        }

        binding?.loadMoreProductCategories?.setOnClickListener {
            selectedSite?.let { site ->
                prependToLog("Submitting offset request to fetch product categories for site ${site.id}")
                val payload = FetchProductCategoriesPayload(
                    site, offset = pendingFetchProductCategoriesOffset
                )
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductCategoriesAction(payload))
            }
        }

        binding?.addProductCategory?.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(
                    activity,
                    "Enter a category name:"
                ) { editText ->
                    enteredCategoryName = editText.text.toString()
                    if (!enteredCategoryName.isNullOrEmpty()) {
                        prependToLog("Submitting request to add product category")
                        coroutineScope.launch {
                            val wcProductCategoryModel = WCProductCategoryModel().apply {
                                name = enteredCategoryName!!
                            }
                            wcProductStore.addProductCategory(site, wcProductCategoryModel)
                        }
                    } else {
                        prependToLog("No category name entered...doing nothing")
                    }
                }
            }
        }

        binding?.fetchProductTags?.setOnClickListener {
            showSingleLineDialog(
                activity,
                "Enter a search query, leave blank for none:"
            ) { editText ->
                val searchQuery = editText.text.toString()
                selectedSite?.let { site ->
                    prependToLog("Submitting request to fetch product tags for site ${site.id}")
                    val payload = FetchProductTagsPayload(site, searchQuery = searchQuery)
                    dispatcher.dispatch(WCProductActionBuilder.newFetchProductTagsAction(payload))
                }
            }
        }

        binding?.loadMoreProductTags?.setOnClickListener {
            selectedSite?.let { site ->
                prependToLog("Submitting offset request to fetch product tags for site ${site.id}")
                val payload = FetchProductTagsPayload(site, offset = pendingFetchProductTagsOffset)
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductTagsAction(payload))
            }
        }

        binding?.addProductTags?.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(
                    activity,
                    "Enter tag name:"
                ) { editTextTagName1 ->
                    showSingleLineDialog(
                        activity,
                        "Enter another tag name:"
                    ) { editTextTagName2 ->
                        val tagName1 = editTextTagName1.text.toString()
                        val tagName2 = editTextTagName2.text.toString()
                        if (tagName1.isNotEmpty() && tagName2.isNotEmpty()) {
                            enteredTagNames.add(tagName1)
                            enteredTagNames.add(tagName2)
                            prependToLog("Submitting request to add product tags for site ${site.id}")
                            val payload = AddProductTagsPayload(site, enteredTagNames)
                            dispatcher.dispatch(
                                WCProductActionBuilder.newAddProductTagsAction(
                                    payload
                                )
                            )
                        } else {
                            prependToLog("Tag name is empty. Doing nothing..")
                        }
                    }
                }
            }
        }

        binding?.testAddOns?.setOnClickListener {
            selectedSite?.let { site ->
                WooAddonsTestFragment.show(childFragmentManager, site.id)
            }
        }

        binding?.updateProductImages?.setOnClickListener {
            showSingleLineDialog(
                activity,
                "Enter the remoteProductId of the product to update images:"
            ) { editTextProduct ->
                editTextProduct.text.toString().toLongOrNull()?.let { productId ->
                    showSingleLineDialog(
                        activity,
                        "Enter the mediaId of the image to assign to the product:"
                    ) { editTextMedia ->
                        editTextMedia.text.toString().toLongOrNull()?.let { mediaId ->
                            updateProductImages(productId, mediaId)
                        }
                    }
                }
            }
        }

        binding?.updateProduct?.setOnClickListener {
            replaceFragment(WooUpdateProductFragment.newInstance(selectedPos))
        }

        binding?.updateVariation?.setOnClickListener {
            replaceFragment(WooUpdateVariationFragment.newInstance(selectedPos))
        }

        binding?.addNewProduct?.setOnClickListener {
            replaceFragment(
                WooUpdateProductFragment.newInstance(
                    selectedPos,
                    isAddNewProduct = true
                )
            )
        }

        binding?.deleteProduct?.setOnClickListener {
            showSingleLineDialog(
                activity,
                "Enter the remoteProductId of the product to delete:"
            ) { editTextProduct ->
                editTextProduct.text.toString().toLongOrNull()?.let { productId ->
                    selectedSite?.let { site ->
                        val payload = DeleteProductPayload(site, productId)
                        dispatcher.dispatch(WCProductActionBuilder.newDeleteProductAction(payload))
                    }
                }
            }
        }

        binding?.batchUpdateVariations?.setOnClickListener {
            replaceFragment(WooBatchUpdateVariationsFragment.newInstance(selectedPos))
        }

        binding?.batchGenerateVariations?.setOnClickListener {
            replaceFragment(WooBatchGenerateVariationsFragment.newInstance(selectedPos))
        }

        binding?.fetchProductStockReport?.setOnClickListener {
            selectedSite?.let { site ->
                prependToLog(
                    "Submitting request to fetch product stock for stock status:" +
                        " ${CoreProductStockStatus.LOW_STOCK}"
                )
                coroutineScope.launch {
                    val result = productStockReportStore.fetchProductStockReport(
                        site,
                        CoreProductStockStatus.LOW_STOCK
                    )
                    if (result.isError) {
                        prependToLog("Fetching product stock failed: ${result.error.message}")
                    } else {
                        val productStockItems = result.model!!
                        prependToLog("Fetched ${productStockItems.size} product stock items")
                        productStockItems.forEach {
                            prependToLog("${it.name} has ${it.stockQuantity} in stock")
                        }
                    }
                }
            }
        }

        binding?.customFields?.setOnClickListener {
            selectedSite?.let { site ->
                lifecycleScope.launch {
                    val orderId = showSingleLineDialog(
                        activity = requireActivity(),
                        message = "Please enter the product id",
                        isNumeric = true
                    )?.toLongOrNull() ?: return@launch

                    replaceFragment(
                        CustomFieldsFragment.newInstance(
                            siteId = site.localId(),
                            parentItemId = orderId,
                            parentItemType = MetaDataParentItemType.PRODUCT
                        )
                    )
                }
            }
        }
    }

    /**
     * Note that this will replace all this product's images with a single image, as defined by mediaId. Also note
     * that the media must already be cached for this to work (ie: you may need to go to the first screen in the
     * example app, tap Media, then ensure the media is fetched)
     */
    private fun updateProductImages(productId: Long, mediaId: Long) {
        selectedSite?.let { site ->
            mediaStore.getSiteMediaWithId(site, mediaId)?.let { media ->
                prependToLog("Submitting request to update product images")
                val imageList = ArrayList<WCProductImageModel>().also {
                    it.add(WCProductImageModel.fromMediaModel(media))
                }
                val payload = UpdateProductImagesPayload(site, productId, imageList)
                dispatcher.dispatch(WCProductActionBuilder.newUpdateProductImagesAction(payload))
            } ?: prependToLog(("Not a valid media id"))
        } ?: prependToLog(("No site selected"))
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        super.onStop()
        dispatcher.unregister(this)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductChanged(event: OnProductChanged) {
        if (event.isError) {
            prependToLog("Error from " + event.causeOfChange + " - error: " + event.error.type)
            return
        }

        selectedSite?.let { site ->
            when (event.causeOfChange) {
                FETCH_PRODUCTS -> {
                    prependToLog("Fetched ${event.rowsAffected} products")
                }

                DELETED_PRODUCT -> {
                    prependToLog("${event.rowsAffected} product deleted")
                }

                else -> prependToLog("Product store was updated from a " + event.causeOfChange)
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductsSearched(event: OnProductsSearched) {
        if (event.isError) {
            prependToLog("Error searching products - error: " + event.error.type)
        } else {
            prependToLog("Found ${event.searchResults.size} products matching ${event.searchQuery}")
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductSkuAvailabilityChanged(event: OnProductSkuAvailabilityChanged) {
        if (event.isError) {
            prependToLog("Error searching product sku availability - error: " + event.error.type)
        } else {
            prependToLog("Sku ${event.sku} available for site ${selectedSite?.name}: ${event.available}")
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductImagesChanged(event: OnProductImagesChanged) {
        if (event.isError) {
            prependToLog("Error updating product images - error: " + event.error.type)
        } else {
            prependToLog("Product images updated")
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductCategoriesChanged(event: OnProductCategoryChanged) {
        if (event.isError) {
            prependToLog("Error from " + event.causeOfChange + " - error: " + event.error.type)
            return
        }

        selectedSite?.let { site ->
            when (event.causeOfChange) {
                FETCH_PRODUCT_CATEGORIES -> {
                    prependToLog(
                        "Fetched ${event.rowsAffected} product categories. " +
                            "More categories available ${event.canLoadMore}"
                    )

                    if (event.canLoadMore) {
                        pendingFetchProductCategoriesOffset += event.rowsAffected
                        binding?.loadMoreProductCategories?.visibility = View.VISIBLE
                        binding?.loadMoreProductCategories?.isEnabled = true
                    } else {
                        pendingFetchProductCategoriesOffset = 0
                        binding?.loadMoreProductCategories?.isEnabled = false
                    }
                }

                ADDED_PRODUCT_CATEGORY -> {
                    val category = enteredCategoryName?.let {
                        wcProductStore.getProductCategoryByNameAndParentId(site, it)
                    }
                    prependToLog("${event.rowsAffected} product category added with name: ${category?.name}")
                }

                else -> {}
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductShippingClassesChanged(event: OnProductShippingClassesChanged) {
        if (event.isError) {
            prependToLog("Error from " + event.causeOfChange + " - error: " + event.error.type)
            return
        }

        selectedSite?.let { site ->
            when (event.causeOfChange) {
                FETCH_SINGLE_PRODUCT_SHIPPING_CLASS -> logFetchSingleProductShippingClass(site)
                else -> checkProductShippingClassesAndLoadMore(event)
            }
        }
    }

    private fun logFetchSingleProductShippingClass(site: SiteModel) {
        pendingFetchSingleProductShippingClassRemoteId?.let { remoteId ->
            pendingFetchSingleProductShippingClassRemoteId = null
            val productShippingClass = wcProductStore.getShippingClassByRemoteId(site, remoteId)
            productShippingClass?.let {
                prependToLog("Single product shipping class fetched! ${it.name}")
            } ?: prependToLog("WARNING: Fetched shipping class not found in the local database!")
        }
    }

    private fun checkProductShippingClassesAndLoadMore(event: OnProductShippingClassesChanged) {
        prependToLog(
            "Fetched ${event.rowsAffected} product shipping classes. " +
                "More shipping classes available ${event.canLoadMore}"
        )

        if (event.canLoadMore) {
            pendingFetchProductShippingClassListOffset += event.rowsAffected
            binding?.loadMoreProductShippingClasses?.visibility = View.VISIBLE
            binding?.loadMoreProductShippingClasses?.isEnabled = true
        } else {
            pendingFetchProductShippingClassListOffset = 0
            binding?.loadMoreProductShippingClasses?.isEnabled = false
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductTagChanged(event: OnProductTagChanged) {
        if (event.isError) {
            prependToLog("Error from " + event.causeOfChange + " - error: " + event.error.type)
            return
        }

        selectedSite?.let { site ->
            when (event.causeOfChange) {
                FETCH_PRODUCT_TAGS -> {
                    prependToLog("Fetched ${event.rowsAffected} product tags. More tags available ${event.canLoadMore}")
                    if (event.canLoadMore) {
                        pendingFetchProductTagsOffset += event.rowsAffected
                        binding?.loadMoreProductTags?.visibility = View.VISIBLE
                        binding?.loadMoreProductTags?.isEnabled = true
                    } else {
                        pendingFetchProductTagsOffset = 0
                        binding?.loadMoreProductTags?.isEnabled = false
                    }
                }

                ADDED_PRODUCT_TAGS -> {
                    val tags = wcProductStore.getProductTagsByNames(site, enteredTagNames)
                    val tagNames = tags.map { it.name }.joinToString(",")
                    prependToLog("${event.rowsAffected} product tags added for $tagNames")
                    if (enteredTagNames.size > event.rowsAffected) {
                        prependToLog("Error occurred when trying to add some product tags")
                    }
                }

                else -> {}
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}
