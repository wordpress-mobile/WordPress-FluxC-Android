package org.wordpress.android.fluxc.example.ui.products

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_products.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCTS
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCT_REVIEWS
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCT_VARIATIONS
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_SINGLE_PRODUCT
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_SINGLE_PRODUCT_REVIEW
import org.wordpress.android.fluxc.action.WCProductAction.UPDATE_PRODUCT_REVIEW_STATUS
import org.wordpress.android.fluxc.example.R.layout
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.replaceFragment
import org.wordpress.android.fluxc.example.ui.StoreSelectorDialog
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductImageModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductReviewsPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductShippingClassListPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductSkuAvailabilityPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductVariationsPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductsPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductReviewPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductImagesChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductShippingClassesChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductSkuAvailabilityChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductsSearched
import org.wordpress.android.fluxc.store.WCProductStore.SearchProductsPayload
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductImagesPayload
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooProductsFragment : Fragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wcProductStore: WCProductStore
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore
    @Inject internal lateinit var mediaStore: MediaStore

    private var selectedPos: Int = -1
    private var selectedSite: SiteModel? = null
    private var pendingFetchSingleProductRemoteId: Long? = null

    private var pendingFetchSingleProductVariationRemoteId: Long? = null
    private var pendingFetchSingleProductVariationOffset: Int = 0

    private var pendingFetchProductShippingClassListOffset: Int = 0

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(layout.fragment_woo_products, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stats_select_site.setOnClickListener {
            showSiteSelectorDialog(selectedPos, object : StoreSelectorDialog.Listener {
                override fun onSiteSelected(site: SiteModel, pos: Int) {
                    selectedSite = site
                    selectedPos = pos
                    toggleSiteDependentButtons(true)
                    stats_select_site.text = site.name ?: site.displayName
                }
            })
        }

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
                    pendingFetchSingleProductVariationRemoteId = editText.text.toString().toLongOrNull()
                    pendingFetchSingleProductVariationRemoteId?.let { id ->
                        prependToLog("Submitting request to fetch product variations by remoteProductID $id")
                        val payload = FetchProductVariationsPayload(site, id)
                        dispatcher.dispatch(WCProductActionBuilder.newFetchProductVariationsAction(payload))
                    } ?: prependToLog("No valid remoteProductId defined...doing nothing")
                }
            }
        }

        load_more_product_variations.setOnClickListener {
            selectedSite?.let { site ->
                pendingFetchSingleProductVariationRemoteId?.let { id ->
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
                        prependToLog("Submitting request to fetch product reviews for remoteProductID $id")
                        val payload = FetchProductReviewsPayload(site, productIds = listOf(remoteProductId))
                        dispatcher.dispatch(WCProductActionBuilder.newFetchProductReviewsAction(payload))
                    } ?: prependToLog("No valid remoteProductId defined...doing nothing")
                }
            }
        }

        fetch_all_reviews.setOnClickListener {
            selectedSite?.let { site ->
                prependToLog("Submitting request to fetch product reviews for site ${site.id}")
                val payload = FetchProductReviewsPayload(site)
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductReviewsAction(payload))
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
                FETCH_PRODUCT_REVIEWS -> {
                    prependToLog("Fetched ${event.rowsAffected} product reviews")
                }
                FETCH_SINGLE_PRODUCT_REVIEW -> {
                    prependToLog("Fetched ${event.rowsAffected} single product review")
                }
                UPDATE_PRODUCT_REVIEW_STATUS -> {
                    prependToLog("${event.rowsAffected} product reviews updated")
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
    fun onProductShippingClassesChanged(event: OnProductShippingClassesChanged) {
        if (event.isError) {
            prependToLog("Error fetching product shipping classes - error: " + event.error.type)
        } else {
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

    private fun showSiteSelectorDialog(selectedPos: Int, listener: StoreSelectorDialog.Listener) {
        fragmentManager?.let { fm ->
            val dialog = StoreSelectorDialog.newInstance(listener, selectedPos)
            dialog.show(fm, "StoreSelectorDialog")
        }
    }

    private fun toggleSiteDependentButtons(enabled: Boolean) {
        for (i in 0 until buttonContainer.childCount) {
            val child = buttonContainer.getChildAt(i)
            if (child is Button) {
                child.isEnabled = enabled
            }
        }
    }
}
