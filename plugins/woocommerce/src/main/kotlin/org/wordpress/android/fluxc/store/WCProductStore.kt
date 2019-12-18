package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.WCProductAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductImageModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.DATE_DESC
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCProductStore @Inject constructor(dispatcher: Dispatcher, private val wcProductRestClient: ProductRestClient) :
        Store(dispatcher) {
    companion object {
        const val NUM_REVIEWS_PER_FETCH = 25
        const val DEFAULT_PRODUCT_PAGE_SIZE = 25
        const val DEFAULT_PRODUCT_VARIATIONS_PAGE_SIZE = 25
        val DEFAULT_PRODUCT_SORTING = DATE_DESC
    }

    class FetchSingleProductPayload(
        var site: SiteModel,
        var remoteProductId: Long
    ) : Payload<BaseNetworkError>()

    class FetchProductsPayload(
        var site: SiteModel,
        var pageSize: Int = DEFAULT_PRODUCT_PAGE_SIZE,
        var offset: Int = 0,
        var sorting: ProductSorting = DEFAULT_PRODUCT_SORTING,
        var remoteProductIds: List<Long>? = null
    ) : Payload<BaseNetworkError>()

    class SearchProductsPayload(
        var site: SiteModel,
        var searchQuery: String,
        var pageSize: Int = DEFAULT_PRODUCT_PAGE_SIZE,
        var offset: Int = 0,
        var sorting: ProductSorting = DEFAULT_PRODUCT_SORTING
    ) : Payload<BaseNetworkError>()

    class FetchProductVariationsPayload(
        var site: SiteModel,
        var remoteProductId: Long,
        var pageSize: Int = DEFAULT_PRODUCT_VARIATIONS_PAGE_SIZE,
        var offset: Int = 0
    ) : Payload<BaseNetworkError>()

    class FetchProductReviewsPayload(
        var site: SiteModel,
        var offset: Int = 0,
        var reviewIds: List<Long>? = null,
        var productIds: List<Long>? = null,
        var filterByStatus: List<String>? = null
    ) : Payload<BaseNetworkError>()

    class FetchSingleProductReviewPayload(
        var site: SiteModel,
        var remoteReviewId: Long
    ) : Payload<BaseNetworkError>()

    class UpdateProductReviewStatusPayload(
        var site: SiteModel,
        var remoteReviewId: Long,
        var newStatus: String
    ) : Payload<BaseNetworkError>()

    class UpdateProductImagesPayload(
        var site: SiteModel,
        var remoteProductId: Long,
        var imageList: List<WCProductImageModel>
    ) : Payload<BaseNetworkError>()

    class UpdateProductPayload(
        var site: SiteModel,
        val product: WCProductModel
    ) : Payload<BaseNetworkError>()

    enum class ProductErrorType {
        INVALID_PARAM,
        INVALID_REVIEW_ID,
        INVALID_IMAGE_ID,
        DUPLICATE_SKU,
        GENERIC_ERROR;

        companion object {
            private val reverseMap = values().associateBy(ProductErrorType::name)
            fun fromString(type: String) = reverseMap[type.toUpperCase(Locale.US)] ?: GENERIC_ERROR
        }
    }

    class ProductError(val type: ProductErrorType = GENERIC_ERROR, val message: String = "") : OnChangedError

    enum class ProductSorting {
        TITLE_ASC,
        TITLE_DESC,
        DATE_ASC,
        DATE_DESC
    }

    class RemoteProductPayload(
        val product: WCProductModel,
        val site: SiteModel
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            product: WCProductModel,
            site: SiteModel
        ) : this(product, site) {
            this.error = error
        }
    }

    class RemoteProductListPayload(
        val site: SiteModel,
        val products: List<WCProductModel> = emptyList(),
        var offset: Int = 0,
        var loadedMore: Boolean = false,
        var canLoadMore: Boolean = false
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel
        ) : this(site) {
            this.error = error
        }
    }

    class RemoteSearchProductsPayload(
        var site: SiteModel,
        var searchQuery: String,
        var products: List<WCProductModel> = emptyList(),
        var offset: Int = 0,
        var loadedMore: Boolean = false,
        var canLoadMore: Boolean = false
    ) : Payload<ProductError>() {
        constructor(error: ProductError, site: SiteModel, query: String) : this(site, query) {
            this.error = error
        }
    }

    class RemoteUpdateProductImagesPayload(
        var site: SiteModel,
        val product: WCProductModel
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel,
            product: WCProductModel
        ) : this(site, product) {
            this.error = error
        }
    }

    class RemoteUpdateProductPayload(
        var site: SiteModel,
        val product: WCProductModel
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel,
            product: WCProductModel
        ) : this(site, product) {
            this.error = error
        }
    }

    class RemoteProductVariationsPayload(
        val site: SiteModel,
        val remoteProductId: Long,
        val variations: List<WCProductVariationModel> = emptyList(),
        var offset: Int = 0,
        var loadedMore: Boolean = false,
        var canLoadMore: Boolean = false
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel,
            remoteProductId: Long
        ) : this(site, remoteProductId) {
            this.error = error
        }
    }

    class RemoteProductReviewPayload(
        val site: SiteModel,
        val productReview: WCProductReviewModel? = null
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel
        ) : this(site) {
            this.error = error
        }
    }

    class FetchProductReviewsResponsePayload(
        val site: SiteModel,
        val reviews: List<WCProductReviewModel> = emptyList(),
        val filterProductIds: List<Long>? = null,
        val filterByStatus: List<String>? = null,
        val loadedMore: Boolean = false,
        val canLoadMore: Boolean = false
    ) : Payload<ProductError>() {
        constructor(error: ProductError, site: SiteModel) : this(site) { this.error = error }
    }

    // OnChanged events
    class OnProductChanged(
        var rowsAffected: Int,
        var canLoadMore: Boolean = false
    ) : OnChanged<ProductError>() {
        var causeOfChange: WCProductAction? = null
    }

    class OnProductsSearched(
        var searchQuery: String = "",
        var searchResults: List<WCProductModel> = emptyList(),
        var canLoadMore: Boolean = false
    ) : OnChanged<ProductError>()

    class OnProductReviewChanged(
        var rowsAffected: Int,
        var canLoadMore: Boolean = false
    ) : OnChanged<ProductError>() {
        var causeOfChange: WCProductAction? = null
    }

    class OnProductImagesChanged(
        var rowsAffected: Int,
        var remoteProductId: Long
    ) : OnChanged<ProductError>() {
        var causeOfChange: WCProductAction? = null
    }

    class OnProductUpdated(
        var rowsAffected: Int,
        var remoteProductId: Long
    ) : OnChanged<ProductError>() {
        var causeOfChange: WCProductAction? = null
    }

    /**
     * returns the corresponding product from the database as a [WCProductModel].
     */
    fun getProductByRemoteId(site: SiteModel, remoteProductId: Long): WCProductModel? =
            ProductSqlUtils.getProductByRemoteId(site, remoteProductId)

    /**
     * returns true if the corresponding product exists in the database
     */
    fun geProductExistsByRemoteId(site: SiteModel, remoteProductId: Long) =
            ProductSqlUtils.geProductExistsByRemoteId(site, remoteProductId)

    /**
     * returns a list of variations for a specific product in the database
     */
    fun getVariationsForProduct(site: SiteModel, remoteProductId: Long): List<WCProductVariationModel> =
            ProductSqlUtils.getVariationsForProduct(site, remoteProductId)

    /**
     * returns a list of [WCProductModel] for the give [SiteModel] and [remoteProductIds]
     * if it exists in the database
     */
    fun getProductsByRemoteIds(site: SiteModel, remoteProductIds: List<Long>): List<WCProductModel> =
            ProductSqlUtils.getProductsByRemoteIds(site, remoteProductIds)

    fun getProductsForSite(site: SiteModel, sortType: ProductSorting = DEFAULT_PRODUCT_SORTING) =
            ProductSqlUtils.getProductsForSite(site, sortType)

    fun deleteProductsForSite(site: SiteModel) = ProductSqlUtils.deleteProductsForSite(site)

    fun getProductReviewsForSite(site: SiteModel): List<WCProductReviewModel> =
            ProductSqlUtils.getProductReviewsForSite(site)

    fun getProductReviewsForProductAndSiteId(localSiteId: Int, remoteProductId: Long): List<WCProductReviewModel> =
            ProductSqlUtils.getProductReviewsForProductAndSiteId(localSiteId, remoteProductId)

    fun getProductReviewByRemoteId(
        localSiteId: Int,
        remoteReviewId: Long
    ): WCProductReviewModel? = ProductSqlUtils
            .getProductReviewByRemoteId(localSiteId, remoteReviewId)

    fun deleteProductReviewsForSite(site: SiteModel) = ProductSqlUtils.deleteAllProductReviewsForSite(site)

    fun deleteAllProductReviews() = ProductSqlUtils.deleteAllProductReviews()

    fun deleteProductImage(site: SiteModel, remoteProductId: Long, remoteMediaId: Long) =
            ProductSqlUtils.deleteProductImage(site, remoteProductId, remoteMediaId)

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? WCProductAction ?: return
        when (actionType) {
            // remote actions
            WCProductAction.FETCH_SINGLE_PRODUCT ->
                fetchSingleProduct(action.payload as FetchSingleProductPayload)
            WCProductAction.FETCH_PRODUCTS ->
                fetchProducts(action.payload as FetchProductsPayload)
            WCProductAction.SEARCH_PRODUCTS ->
                searchProducts(action.payload as SearchProductsPayload)
            WCProductAction.FETCH_PRODUCT_VARIATIONS ->
                fetchProductVariations(action.payload as FetchProductVariationsPayload)
            WCProductAction.FETCH_PRODUCT_REVIEWS ->
                fetchProductReviews(action.payload as FetchProductReviewsPayload)
            WCProductAction.FETCH_SINGLE_PRODUCT_REVIEW ->
                fetchSingleProductReview(action.payload as FetchSingleProductReviewPayload)
            WCProductAction.UPDATE_PRODUCT_REVIEW_STATUS ->
                updateProductReviewStatus(action.payload as UpdateProductReviewStatusPayload)
            WCProductAction.UPDATE_PRODUCT_IMAGES ->
                updateProductImages(action.payload as UpdateProductImagesPayload)
            WCProductAction.UPDATE_PRODUCT ->
                updateProduct(action.payload as UpdateProductPayload)

            // remote responses
            WCProductAction.FETCHED_SINGLE_PRODUCT ->
                handleFetchSingleProductCompleted(action.payload as RemoteProductPayload)
            WCProductAction.FETCHED_PRODUCTS ->
                handleFetchProductsCompleted(action.payload as RemoteProductListPayload)
            WCProductAction.SEARCHED_PRODUCTS ->
                handleSearchProductsCompleted(action.payload as RemoteSearchProductsPayload)
            WCProductAction.FETCHED_PRODUCT_VARIATIONS ->
                handleFetchProductVariationsCompleted(action.payload as RemoteProductVariationsPayload)
            WCProductAction.FETCHED_PRODUCT_REVIEWS ->
                handleFetchProductReviews(action.payload as FetchProductReviewsResponsePayload)
            WCProductAction.FETCHED_SINGLE_PRODUCT_REVIEW ->
                handleFetchSingleProductReview(action.payload as RemoteProductReviewPayload)
            WCProductAction.UPDATED_PRODUCT_REVIEW_STATUS ->
                handleUpdateProductReviewStatus(action.payload as RemoteProductReviewPayload)
            WCProductAction.UPDATED_PRODUCT_IMAGES ->
                handleUpdateProductImages(action.payload as RemoteUpdateProductImagesPayload)
            WCProductAction.UPDATED_PRODUCT ->
                handleUpdateProduct(action.payload as RemoteUpdateProductPayload)
        }
    }

    override fun onRegister() = AppLog.d(T.API, "WCProductStore onRegister")

    private fun fetchSingleProduct(payload: FetchSingleProductPayload) {
        with(payload) { wcProductRestClient.fetchSingleProduct(site, remoteProductId) }
    }

    private fun fetchProducts(payload: FetchProductsPayload) {
        with(payload) {
            wcProductRestClient.fetchProducts(site, pageSize, offset, sorting, remoteProductIds = remoteProductIds)
        }
    }

    private fun searchProducts(payload: SearchProductsPayload) {
        with(payload) { wcProductRestClient.searchProducts(site, searchQuery, pageSize, offset, sorting) }
    }

    private fun fetchProductVariations(payload: FetchProductVariationsPayload) {
        with(payload) { wcProductRestClient.fetchProductVariations(site, remoteProductId, pageSize, offset) }
    }

    private fun fetchProductReviews(payload: FetchProductReviewsPayload) {
        with(payload) { wcProductRestClient.fetchProductReviews(site, offset, reviewIds, productIds, filterByStatus) }
    }

    private fun fetchSingleProductReview(payload: FetchSingleProductReviewPayload) {
        with(payload) { wcProductRestClient.fetchProductReviewById(site, remoteReviewId) }
    }

    private fun updateProductReviewStatus(payload: UpdateProductReviewStatusPayload) {
        with(payload) { wcProductRestClient.updateProductReviewStatus(site, remoteReviewId, newStatus) }
    }

    private fun updateProductImages(payload: UpdateProductImagesPayload) {
        with(payload) { wcProductRestClient.updateProductImages(site, remoteProductId, imageList) }
    }

    private fun updateProduct(payload: UpdateProductPayload) {
        with(payload) {
            val storedProduct = getProductByRemoteId(site, product.remoteProductId)
            wcProductRestClient.updateProduct(site, storedProduct, product)
        }
    }

    private fun handleFetchSingleProductCompleted(payload: RemoteProductPayload) {
        val onProductChanged: OnProductChanged

        if (payload.isError) {
            onProductChanged = OnProductChanged(0).also { it.error = payload.error }
        } else {
            val rowsAffected = ProductSqlUtils.insertOrUpdateProduct(payload.product)
            onProductChanged = OnProductChanged(rowsAffected)
        }

        onProductChanged.causeOfChange = WCProductAction.FETCH_SINGLE_PRODUCT
        emitChange(onProductChanged)
    }

    private fun handleFetchProductsCompleted(payload: RemoteProductListPayload) {
        val onProductChanged: OnProductChanged

        if (payload.isError) {
            onProductChanged = OnProductChanged(0).also { it.error = payload.error }
        } else {
            // remove the existing products for this site if this is the first page of results, otherwise
            // products deleted outside of the app will persist
            if (payload.offset == 0) {
                ProductSqlUtils.deleteProductsForSite(payload.site)
            }
            val rowsAffected = ProductSqlUtils.insertOrUpdateProducts(payload.products)
            onProductChanged = OnProductChanged(rowsAffected, canLoadMore = payload.canLoadMore)
        }

        onProductChanged.causeOfChange = WCProductAction.FETCH_PRODUCTS
        emitChange(onProductChanged)
    }

    private fun handleSearchProductsCompleted(payload: RemoteSearchProductsPayload) {
        val onProductsSearched = if (payload.isError) {
            OnProductsSearched(payload.searchQuery)
        } else {
            OnProductsSearched(payload.searchQuery, payload.products, payload.canLoadMore)
        }
        emitChange(onProductsSearched)
    }

    private fun handleFetchProductVariationsCompleted(payload: RemoteProductVariationsPayload) {
        val onProductChanged: OnProductChanged

        if (payload.isError) {
            onProductChanged = OnProductChanged(0).also { it.error = payload.error }
        } else {
            // delete product variations for site if this is the first page of results, otherwise
            // product variations deleted outside of the app will persist
            if (payload.offset == 0) {
                ProductSqlUtils.deleteVariationsForProduct(payload.site, payload.remoteProductId)
            }

            val rowsAffected = ProductSqlUtils.insertOrUpdateProductVariations(payload.variations)
            onProductChanged = OnProductChanged(rowsAffected, canLoadMore = payload.canLoadMore)
        }

        onProductChanged.causeOfChange = WCProductAction.FETCH_PRODUCT_VARIATIONS
        emitChange(onProductChanged)
    }

    private fun handleFetchProductReviews(payload: FetchProductReviewsResponsePayload) {
        val onProductReviewChanged: OnProductReviewChanged

        if (payload.isError) {
            onProductReviewChanged = OnProductReviewChanged(0).also { it.error = payload.error }
        } else {
            // Clear existing product reviews if this is a fresh fetch (loadMore = false).
            // This is the simplest way to keep our local reviews in sync with remote reviews
            // in case of deletions.
            if (!payload.loadedMore) {
                ProductSqlUtils.deleteAllProductReviewsForSite(payload.site)
            }
            val rowsAffected = ProductSqlUtils.insertOrUpdateProductReviews(payload.reviews)
            onProductReviewChanged = OnProductReviewChanged(rowsAffected, canLoadMore = payload.canLoadMore)
        }

        onProductReviewChanged.causeOfChange = WCProductAction.FETCH_PRODUCT_REVIEWS
        emitChange(onProductReviewChanged)
    }

    private fun handleFetchSingleProductReview(payload: RemoteProductReviewPayload) {
        val onProductReviewChanged: OnProductReviewChanged

        if (payload.isError) {
            onProductReviewChanged = OnProductReviewChanged(0).also { it.error = payload.error }
        } else {
            val rowsAffected = payload.productReview?.let {
                ProductSqlUtils.insertOrUpdateProductReview(it)
            } ?: 0
            onProductReviewChanged = OnProductReviewChanged(rowsAffected)
        }

        onProductReviewChanged.causeOfChange = WCProductAction.FETCH_SINGLE_PRODUCT_REVIEW
        emitChange(onProductReviewChanged)
    }

    private fun handleUpdateProductReviewStatus(payload: RemoteProductReviewPayload) {
        val onProductReviewChanged: OnProductReviewChanged

        if (payload.isError) {
            onProductReviewChanged = OnProductReviewChanged(0).also { it.error = payload.error }
        } else {
            val rowsAffected = payload.productReview?.let { review ->
                if (review.status == "spam" || review.status == "trash") {
                    // Delete this review from the database
                    ProductSqlUtils.deleteProductReview(review)
                } else {
                    // Insert or update in the database
                    ProductSqlUtils.insertOrUpdateProductReview(review)
                }
            } ?: 0
            onProductReviewChanged = OnProductReviewChanged(rowsAffected)
        }

        onProductReviewChanged.causeOfChange = WCProductAction.UPDATE_PRODUCT_REVIEW_STATUS
        emitChange(onProductReviewChanged)
    }

    private fun handleUpdateProductImages(payload: RemoteUpdateProductImagesPayload) {
        val onProductImagesChanged: OnProductImagesChanged

        if (payload.isError) {
            onProductImagesChanged = OnProductImagesChanged(0, payload.product.remoteProductId).also {
                it.error = payload.error
            }
        } else {
            val rowsAffected = ProductSqlUtils.insertOrUpdateProduct(payload.product)
            onProductImagesChanged = OnProductImagesChanged(rowsAffected, payload.product.remoteProductId)
        }

        onProductImagesChanged.causeOfChange = WCProductAction.UPDATED_PRODUCT_IMAGES
        emitChange(onProductImagesChanged)
    }

    private fun handleUpdateProduct(payload: RemoteUpdateProductPayload) {
        val onProductUpdated: OnProductUpdated

        if (payload.isError) {
            onProductUpdated = OnProductUpdated(0, payload.product.remoteProductId)
                    .also { it.error = payload.error }
        } else {
            val rowsAffected = ProductSqlUtils.insertOrUpdateProduct(payload.product)
            onProductUpdated = OnProductUpdated(rowsAffected, payload.product.remoteProductId)
        }

        onProductUpdated.causeOfChange = WCProductAction.UPDATED_PRODUCT
        emitChange(onProductUpdated)
    }
}
