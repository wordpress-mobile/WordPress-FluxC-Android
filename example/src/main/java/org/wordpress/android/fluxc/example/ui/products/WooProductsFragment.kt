package org.wordpress.android.fluxc.example.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_woo_products.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCT_VARIATIONS
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_SINGLE_PRODUCT
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_SINGLE_PRODUCT_REVIEW
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_SINGLE_PRODUCT_SHIPPING_CLASS
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_SINGLE_VARIATION
import org.wordpress.android.fluxc.action.WCProductAction.UPDATE_PRODUCT_REVIEW_STATUS
import org.wordpress.android.fluxc.example.R.layout
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.replaceFragment
import org.wordpress.android.fluxc.example.ui.StoreSelectingFragment
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.model.WCProductImageModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.WCAddonsStore
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.AddProductCategoryPayload
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
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleVariationPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductCategoryChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductImagesChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductShippingClassesChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductSkuAvailabilityChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductTagChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductsSearched
import org.wordpress.android.fluxc.store.WCProductStore.OnVariationChanged
import org.wordpress.android.fluxc.store.WCProductStore.SearchProductsPayload
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductImagesPayload
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooProductsFragment : StoreSelectingFragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wcProductStore: WCProductStore
    @Inject lateinit var addonsStore: WCAddonsStore
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore
    @Inject internal lateinit var mediaStore: MediaStore

    private var pendingFetchSingleProductRemoteId: Long? = null
    private var pendingFetchSingleVariationRemoteId: Long? = null
    private var pendingFetchSingleProductShippingClassRemoteId: Long? = null

    private var pendingFetchProductVariationsProductRemoteId: Long? = null
    private var pendingFetchSingleProductVariationOffset: Int = 0

    private var pendingFetchProductShippingClassListOffset: Int = 0
    private var pendingFetchProductCategoriesOffset: Int = 0
    private var pendingFetchProductTagsOffset: Int = 0

    private var enteredCategoryName: String? = null
    private val enteredTagNames: MutableList<String> = mutableListOf()

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(layout.fragment_woo_products, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetch_single_product.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(activity, "Enter the remoteProductId of product to fetch:") { editText ->
                    pendingFetchSingleProductRemoteId = editText.text.toString().toLongOrNull()
                    pendingFetchSingleProductRemoteId?.let { id ->
                        prependToLog("Submitting request to fetch product by remoteProductID $id")
                        val payload = FetchSingleProductPayload(site, id)
                        dispatcher.dispatch(WCProductActionBuilder.newFetchSingleProductAction(payload))
                    } ?: prependToLog("No valid remoteOrderId defined...doing nothing")
                }
            }
        }

        fetch_single_variation.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(
                        activity,
                        "Enter the remoteProductId of variation to fetch:"
                ) { productIdText ->
                    pendingFetchSingleProductRemoteId = productIdText.text.toString().toLongOrNull()
                    pendingFetchSingleProductRemoteId?.let { productId ->
                        showSingleLineDialog(
                                activity,
                                "Enter the remoteVariationId of variation to fetch:"
                        ) { variationIdText ->
                            pendingFetchSingleVariationRemoteId = variationIdText.text.toString().toLongOrNull()
                            pendingFetchSingleVariationRemoteId?.let { variationId ->
                                prependToLog("Submitting request to fetch product by " +
                                        "remoteProductId $pendingFetchSingleProductRemoteId, " +
                                        "remoteVariationProductID $variationId")
                                val payload = FetchSingleVariationPayload(site, productId, variationId)
                                dispatcher.dispatch(WCProductActionBuilder.newFetchSingleVariationAction(payload))
                            } ?: prependToLog("No valid remoteVariationId defined...doing nothing")
                        }
                    } ?: prependToLog("No valid remoteProductId defined...doing nothing")
                }
            }
        }

        fetch_product_sku_availability.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(
                        activity,
                        "Enter a product SKU:"
                ) { editText ->
                    val payload = FetchProductSkuAvailabilityPayload(site, editText.text.toString())
                    dispatcher.dispatch(WCProductActionBuilder.newFetchProductSkuAvailabilityAction(payload))
                }
            }
        }

        fetch_products.setOnClickListener {
            selectedSite?.let { site ->
                val payload = FetchProductsPayload(site)
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductsAction(payload))
            }
        }

        fetch_products_with_filters.setOnClickListener {
            replaceFragment(WooProductFiltersFragment.newInstance(selectedPos))
        }

        search_products.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(
                        activity,
                        "Enter a search query:"
                ) { editText ->
                    val payload = SearchProductsPayload(site, editText.text.toString())
                    dispatcher.dispatch(WCProductActionBuilder.newSearchProductsAction(payload))
                }
            }
        }

        fetch_product_variations.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(
                        activity,
                        "Enter the remoteProductId of product to fetch variations:"
                ) { editText ->
                    pendingFetchProductVariationsProductRemoteId = editText.text.toString().toLongOrNull()
                    pendingFetchProductVariationsProductRemoteId?.let { id ->
                        prependToLog("Submitting request to fetch product variations by remoteProductID $id")
                        val payload = FetchProductVariationsPayload(site, id)
                        dispatcher.dispatch(WCProductActionBuilder.newFetchProductVariationsAction(payload))
                    } ?: prependToLog("No valid remoteProductId defined...doing nothing")
                }
            }
        }

        load_more_product_variations.setOnClickListener {
            selectedSite?.let { site ->
                pendingFetchProductVariationsProductRemoteId?.let { id ->
                    prependToLog("Submitting offset request to fetch product variations by remoteProductID $id")
                    val payload = FetchProductVariationsPayload(
                            site, id, offset = pendingFetchSingleProductVariationOffset
                    )
                    dispatcher.dispatch(WCProductActionBuilder.newFetchProductVariationsAction(payload))
                } ?: prependToLog("No valid remoteProductId defined...doing nothing")
            }
        }

        fetch_reviews_for_product.setOnClickListener {
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
                                    )
                            )
                            prependToLog("Fetched ${result.rowsAffected} product reviews")
                        }
                    } ?: prependToLog("No valid remoteProductId defined...doing nothing")
                }
            }
        }

        fetch_all_reviews.setOnClickListener {
            selectedSite?.let { site ->
                coroutineScope.launch {
                    prependToLog("Submitting request to fetch product reviews for site ${site.id}")
                    val payload = FetchProductReviewsPayload(site)
                    val result = wcProductStore.fetchProductReviews(payload)
                    prependToLog("Fetched ${result.rowsAffected} product reviews")
                }
            }
        }

        fetch_review_by_id.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(
                        activity,
                        "Enter the remoteReviewId of the review to fetch:"
                ) { editText ->
                    val reviewId = editText.text.toString().toLongOrNull()
                    reviewId?.let { id ->
                        prependToLog("Submitting request to fetch product review for ID $id")
                        val payload = FetchSingleProductReviewPayload(site, id)
                        dispatcher.dispatch(WCProductActionBuilder.newFetchSingleProductReviewAction(payload))
                    } ?: prependToLog("No valid remoteReviewId defined...doing nothing")
                }
            }
        }

        fetch_product_shipping_class.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(
                        activity,
                        "Enter the remoteShippingClassId of the site to fetch:"
                ) { editText ->
                    pendingFetchSingleProductShippingClassRemoteId = editText.text.toString().toLongOrNull()
                    pendingFetchSingleProductShippingClassRemoteId?.let { id ->
                        prependToLog("Submitting request to fetch product shipping class for ID $id")
                        val payload = FetchSingleProductShippingClassPayload(site, id)
                        dispatcher.dispatch(WCProductActionBuilder.newFetchSingleProductShippingClassAction(payload))
                    } ?: prependToLog("No valid remoteShippingClassId defined...doing nothing")
                }
            }
        }

        fetch_product_shipping_classes.setOnClickListener {
            selectedSite?.let { site ->
                prependToLog("Submitting request to fetch product shipping classes for site ${site.id}")
                val payload = FetchProductShippingClassListPayload(site)
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductShippingClassListAction(payload))
            }
        }

        load_more_product_shipping_classes.setOnClickListener {
            selectedSite?.let { site ->
                prependToLog("Submitting offset request to fetch product shipping classes for site ${site.id}")
                val payload = FetchProductShippingClassListPayload(
                        site, offset = pendingFetchProductShippingClassListOffset
                )
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductShippingClassListAction(payload))
            }
        }

        fetch_product_categories.setOnClickListener {
            selectedSite?.let { site ->
                prependToLog("Submitting request to fetch product categories for site ${site.id}")
                val payload = FetchProductCategoriesPayload(site)
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductCategoriesAction(payload))
            }
        }

        load_more_product_categories.setOnClickListener {
            selectedSite?.let { site ->
                prependToLog("Submitting offset request to fetch product categories for site ${site.id}")
                val payload = FetchProductCategoriesPayload(
                        site, offset = pendingFetchProductCategoriesOffset
                )
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductCategoriesAction(payload))
            }
        }

        add_product_category.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(
                        activity,
                        "Enter a category name:"
                ) { editText ->
                    enteredCategoryName = editText.text.toString()
                    if (enteredCategoryName != null && enteredCategoryName?.isNotEmpty() == true) {
                        prependToLog("Submitting request to add product category")
                        val wcProductCategoryModel = WCProductCategoryModel().apply {
                            name = enteredCategoryName!!
                        }
                        val payload = AddProductCategoryPayload(site, wcProductCategoryModel)
                        dispatcher.dispatch(WCProductActionBuilder.newAddProductCategoryAction(payload))
                    } else {
                        prependToLog("No category name entered...doing nothing")
                    }
                }
            }
        }

        fetch_product_tags.setOnClickListener {
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

        load_more_product_tags.setOnClickListener {
            selectedSite?.let { site ->
                prependToLog("Submitting offset request to fetch product tags for site ${site.id}")
                val payload = FetchProductTagsPayload(site, offset = pendingFetchProductTagsOffset)
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductTagsAction(payload))
            }
        }

        add_product_tags.setOnClickListener {
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
                            dispatcher.dispatch(WCProductActionBuilder.newAddProductTagsAction(payload))
                        } else {
                            prependToLog("Tag name is empty. Doing nothing..")
                        }
                    }
                }
            }
        }

        test_add_ons.setOnClickListener {
            selectedSite?.let { site ->
                WooAddonsTestFragment.show(childFragmentManager, site.siteId)
            }
        }

        update_product_images.setOnClickListener {
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

        update_product.setOnClickListener {
            replaceFragment(WooUpdateProductFragment.newInstance(selectedPos))
        }

        update_variation.setOnClickListener {
            replaceFragment(WooUpdateVariationFragment.newInstance(selectedPos))
        }

        add_new_product.setOnClickListener {
            replaceFragment(WooUpdateProductFragment.newInstance(selectedPos, isAddNewProduct = true))
        }

        delete_product.setOnClickListener {
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
                FETCH_SINGLE_PRODUCT -> {
                    pendingFetchSingleProductRemoteId?.let { remoteId ->
                        pendingFetchSingleProductRemoteId = null
                        val product = wcProductStore.getProductByRemoteId(site, remoteId)
                        product?.let {
                            val numVariations = it.getNumVariations()
                            if (numVariations > 0) {
                                prependToLog("Single product with $numVariations variations fetched! ${it.name}")
                            } else {
                                prependToLog("Single product fetched! ${it.name}")
                            }
                        } ?: prependToLog("WARNING: Fetched product not found in the local database!")
                    }
                }
                FETCH_PRODUCTS -> {
                    prependToLog("Fetched ${event.rowsAffected} products")
                }
                FETCH_PRODUCT_VARIATIONS -> {
                    prependToLog("Fetched ${event.rowsAffected} product variants. " +
                            "More variants available ${event.canLoadMore}")
                    if (event.canLoadMore) {
                        pendingFetchSingleProductVariationOffset += event.rowsAffected
                        load_more_product_variations.visibility = View.VISIBLE
                        load_more_product_variations.isEnabled = true
                    } else {
                        pendingFetchSingleProductVariationOffset = 0
                        load_more_product_variations.isEnabled = false
                    }
                }
                FETCH_SINGLE_PRODUCT_REVIEW -> {
                    prependToLog("Fetched ${event.rowsAffected} single product review")
                }
                UPDATE_PRODUCT_REVIEW_STATUS -> {
                    prependToLog("${event.rowsAffected} product reviews updated")
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
    fun onVariationChanged(event: OnVariationChanged) {
        if (event.isError) {
            prependToLog("Error from " + event.causeOfChange + " - error: " + event.error.type)
            return
        }

        selectedSite?.let { site ->
            when (event.causeOfChange) {
                FETCH_SINGLE_VARIATION -> {
                    pendingFetchSingleVariationRemoteId = null
                    pendingFetchSingleProductRemoteId = null
                    val variation = wcProductStore.getVariationByRemoteId(
                            site,
                            event.remoteProductId,
                            event.remoteVariationId
                    )
                    variation?.let {
                        prependToLog("Single variation fetched! ${it.remoteVariationId}")
                    } ?: prependToLog("WARNING: Fetched product not found in the local database!")
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
                    prependToLog("Fetched ${event.rowsAffected} product categories. " +
                            "More categories available ${event.canLoadMore}")

                    if (event.canLoadMore) {
                        pendingFetchProductCategoriesOffset += event.rowsAffected
                        load_more_product_categories.visibility = View.VISIBLE
                        load_more_product_categories.isEnabled = true
                    } else {
                        pendingFetchProductCategoriesOffset = 0
                        load_more_product_categories.isEnabled = false
                    }
                }
                ADDED_PRODUCT_CATEGORY -> {
                    val category = enteredCategoryName?.let {
                        wcProductStore.getProductCategoryByNameAndParentId(site, it)
                    }
                    prependToLog("${event.rowsAffected} product category added with name: ${category?.name}")
                } else -> { }
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
                FETCH_SINGLE_PRODUCT_SHIPPING_CLASS -> {
                    pendingFetchSingleProductShippingClassRemoteId?.let { remoteId ->
                        pendingFetchSingleProductShippingClassRemoteId = null
                        val productShippingClass = wcProductStore.getShippingClassByRemoteId(site, remoteId)
                        productShippingClass?.let {
                            prependToLog("Single product shipping class fetched! ${it.name}")
                        } ?: prependToLog("WARNING: Fetched shipping class not found in the local database!")
                    }
                }
                else -> {
                    prependToLog("Fetched ${event.rowsAffected} product shipping classes. " +
                            "More shipping classes available ${event.canLoadMore}")

                    if (event.canLoadMore) {
                        pendingFetchProductShippingClassListOffset += event.rowsAffected
                        load_more_product_shipping_classes.visibility = View.VISIBLE
                        load_more_product_shipping_classes.isEnabled = true
                    } else {
                        pendingFetchProductShippingClassListOffset = 0
                        load_more_product_shipping_classes.isEnabled = false
                    }
                }
            }
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
                        load_more_product_tags.visibility = View.VISIBLE
                        load_more_product_tags.isEnabled = true
                    } else {
                        pendingFetchProductTagsOffset = 0
                        load_more_product_tags.isEnabled = false
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
                else -> { }
            }
        }
    }
}
