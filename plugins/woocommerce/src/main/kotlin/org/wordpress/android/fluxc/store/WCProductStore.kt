package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.WCProductAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.GENERIC_ERROR
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
    }

    class FetchSingleProductPayload(
        var site: SiteModel,
        var remoteProductId: Long
    ) : Payload<BaseNetworkError>()

    class FetchProductVariationsPayload(
        var site: SiteModel,
        var remoteProductId: Long
    ) : Payload<BaseNetworkError>()

    class FetchProductReviewsPayload(
        var site: SiteModel,
        var offset: Int = 0,
        var productIds: List<Long>? = null,
        var filterByStatus: List<String>? = null
    ) : Payload<BaseNetworkError>()

    class FetchSingleProductReviewPayload(
        var site: SiteModel,
        var remoteReviewId: Long
    ) : Payload<BaseNetworkError>()

    class UpdateProductReviewStatusPayload(
        var site: SiteModel,
        var productReview: WCProductReviewModel,
        var newStatus: String
    ) : Payload<BaseNetworkError>()

    enum class ProductErrorType {
        INVALID_PARAM,
        INVALID_REVIEW_ID,
        GENERIC_ERROR;

        companion object {
            private val reverseMap = values().associateBy(ProductErrorType::name)
            fun fromString(type: String) = reverseMap[type.toUpperCase(Locale.US)] ?: GENERIC_ERROR
        }
    }

    class ProductError(val type: ProductErrorType = GENERIC_ERROR, val message: String = "") : OnChangedError

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

    class RemoteProductVariationsPayload(
        val site: SiteModel,
        val remoteProductId: Long,
        val variations: List<WCProductVariationModel> = emptyList()
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
            site: SiteModel,
            productReview: WCProductReviewModel
        ) : this(site, productReview) {
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

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? WCProductAction ?: return
        when (actionType) {
            // remote actions
            WCProductAction.FETCH_SINGLE_PRODUCT ->
                fetchSingleProduct(action.payload as FetchSingleProductPayload)
            WCProductAction.FETCH_PRODUCT_VARIATIONS ->
                fetchProductVariations(action.payload as FetchProductVariationsPayload)
            WCProductAction.FETCH_PRODUCT_REVIEWS ->
                fetchProductReviews(action.payload as FetchProductReviewsPayload)
            WCProductAction.FETCH_SINGLE_PRODUCT_REVIEW ->
                fetchSingleProductReview(action.payload as FetchSingleProductReviewPayload)
            WCProductAction.UPDATE_PRODUCT_REVIEW_STATUS ->
                updateProductReviewStatus(action.payload as UpdateProductReviewStatusPayload)

            // remote responses
            WCProductAction.FETCHED_SINGLE_PRODUCT ->
                handleFetchSingleProductCompleted(action.payload as RemoteProductPayload)
            WCProductAction.FETCHED_PRODUCT_VARIATIONS ->
                handleFetchProductVariationsCompleted(action.payload as RemoteProductVariationsPayload)
            WCProductAction.FETCHED_PRODUCT_REVIEWS ->
                handleFetchProductReviews(action.payload as FetchProductReviewsResponsePayload)
            WCProductAction.FETCHED_SINGLE_PRODUCT_REVIEW ->
                handleFetchSingleProductReview(action.payload as RemoteProductReviewPayload)
            WCProductAction.UPDATED_PRODUCT_REVIEW_STATUS ->
                handleUpdateProductReviewStatus(action.payload as RemoteProductReviewPayload)
        }
    }

    override fun onRegister() = AppLog.d(T.API, "WCProductStore onRegister")

    private fun fetchSingleProduct(payload: FetchSingleProductPayload) {
        with(payload) { wcProductRestClient.fetchSingleProduct(site, remoteProductId) }
    }

    private fun fetchProductVariations(payload: FetchProductVariationsPayload) {
        with(payload) { wcProductRestClient.fetchProductVariations(site, remoteProductId) }
    }

    private fun fetchProductReviews(payload: FetchProductReviewsPayload) {
        with(payload) { wcProductRestClient.fetchProductReviews(site, offset, productIds, filterByStatus) }
    }

    private fun fetchSingleProductReview(payload: FetchSingleProductReviewPayload) {
        with(payload) { wcProductRestClient.fetchProductReviewById(site, remoteReviewId) }
    }

    private fun updateProductReviewStatus(payload: UpdateProductReviewStatusPayload) {
        with(payload) { wcProductRestClient.updateProductReviewStatus(site, productReview, newStatus) }
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

    private fun handleFetchProductVariationsCompleted(payload: RemoteProductVariationsPayload) {
        val onProductChanged: OnProductChanged

        if (payload.isError) {
            onProductChanged = OnProductChanged(0).also { it.error = payload.error }
        } else {
            val rowsAffected = ProductSqlUtils.insertOrUpdateProductVariations(payload.variations)
            onProductChanged = OnProductChanged(rowsAffected)
        }

        onProductChanged.causeOfChange = WCProductAction.FETCH_PRODUCT_VARIATIONS
        emitChange(onProductChanged)
    }

    private fun handleFetchProductReviews(payload: FetchProductReviewsResponsePayload) {
        val onProductReviewChanged: OnProductChanged

        if (payload.isError) {
            onProductReviewChanged = OnProductChanged(0).also { it.error = payload.error }
        } else {
            val rowsAffected = ProductSqlUtils.insertOrUpdateProductReviews(payload.reviews)
            onProductReviewChanged = OnProductChanged(rowsAffected)
        }

        onProductReviewChanged.causeOfChange = WCProductAction.FETCH_PRODUCT_REVIEWS
        emitChange(onProductReviewChanged)
    }

    private fun handleFetchSingleProductReview(payload: RemoteProductReviewPayload) {
        val onProductReviewChanged: OnProductChanged

        if (payload.isError) {
            onProductReviewChanged = OnProductChanged(0).also { it.error = payload.error }
        } else {
            val rowsAffected = payload.productReview?.let {
                ProductSqlUtils.insertOrUpdateProductReview(it)
            } ?: 0
            onProductReviewChanged = OnProductChanged(rowsAffected)
        }

        onProductReviewChanged.causeOfChange = WCProductAction.FETCH_SINGLE_PRODUCT_REVIEW
        emitChange(onProductReviewChanged)
    }

    private fun handleUpdateProductReviewStatus(payload: RemoteProductReviewPayload) {
        val onProductReviewChanged: OnProductChanged

        if (payload.isError) {
            onProductReviewChanged = OnProductChanged(0).also { it.error = payload.error }
        } else {
            val rowsAffected = payload.productReview?.let {
                ProductSqlUtils.insertOrUpdateProductReview(it)
            } ?: 0
            onProductReviewChanged = OnProductChanged(rowsAffected)
        }

        onProductReviewChanged.causeOfChange = WCProductAction.UPDATE_PRODUCT_REVIEW_STATUS
        emitChange(onProductReviewChanged)
    }
}
