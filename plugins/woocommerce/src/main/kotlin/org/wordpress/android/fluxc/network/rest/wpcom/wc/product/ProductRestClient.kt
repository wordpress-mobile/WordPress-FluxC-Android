package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductImageModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.model.WCProductShippingClassModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComErrorListener
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequest
import org.wordpress.android.fluxc.network.utils.getString
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.Companion.DEFAULT_PRODUCT_PAGE_SIZE
import org.wordpress.android.fluxc.store.WCProductStore.Companion.DEFAULT_PRODUCT_SHIPPING_CLASS_PAGE_SIZE
import org.wordpress.android.fluxc.store.WCProductStore.Companion.DEFAULT_PRODUCT_SORTING
import org.wordpress.android.fluxc.store.WCProductStore.Companion.DEFAULT_PRODUCT_VARIATIONS_PAGE_SIZE
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductReviewsResponsePayload
import org.wordpress.android.fluxc.store.WCProductStore.ProductError
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.DATE_ASC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.DATE_DESC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.TITLE_ASC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.TITLE_DESC
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductListPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductReviewPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductShippingClassListPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductShippingClassPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductSkuAvailabilityPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductVariationsPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteSearchProductsPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdateProductImagesPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdateProductPayload
import java.util.HashMap
import javax.inject.Singleton

@Singleton
class ProductRestClient(
    appContext: Context,
    private val dispatcher: Dispatcher,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    /**
     * Makes a GET request to `/wp-json/wc/v3/products/shipping_classes/[remoteShippingClassId]`
     * to fetch a single product shipping class
     *
     * Dispatches a WCProductAction.FETCHED_SINGLE_PRODUCT_SHIPPING_CLASS action with the result
     *
     * @param [remoteShippingClassId] Unique server id of the shipping class to fetch
     */
    fun fetchSingleProductShippingClass(site: SiteModel, remoteShippingClassId: Long) {
        val url = WOOCOMMERCE.products.shipping_classes.id(remoteShippingClassId).pathV3
        val responseType = object : TypeToken<ProductShippingClassApiResponse>() {}.type
        val params = emptyMap<String, String>()
        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params, responseType,
                { response: ProductShippingClassApiResponse? ->
                    response?.let {
                        val newModel = productShippingClassResponseToProductShippingClassModel(
                                it, site
                        ).apply { localSiteId = site.id }
                        val payload = RemoteProductShippingClassPayload(newModel, site)
                        dispatcher.dispatch(WCProductActionBuilder.newFetchedSingleProductShippingClassAction(payload))
                    }
                },
                WPComErrorListener { networkError ->
                    val productError = networkErrorToProductError(networkError)
                    val payload = RemoteProductShippingClassPayload(
                            productError,
                            WCProductShippingClassModel().apply { this.remoteShippingClassId = remoteShippingClassId },
                            site
                    )
                    dispatcher.dispatch(WCProductActionBuilder.newFetchedSingleProductShippingClassAction(payload))
                },
                { request: WPComGsonRequest<*> -> add(request) })
        add(request)
    }

    /**
     * Makes a GET request to `GET /wp-json/wc/v3/products/shipping_classes` to fetch
     * product shipping classes for a site
     *
     * Dispatches a WCProductAction.FETCHED_PRODUCT_SHIPPING_CLASS_LIST action with the result
     *
     * @param [site] The site to fetch product shipping class list for
     */
    fun fetchProductShippingClassList(
        site: SiteModel,
        pageSize: Int = DEFAULT_PRODUCT_SHIPPING_CLASS_PAGE_SIZE,
        offset: Int = 0
    ) {
        val url = WOOCOMMERCE.products.shipping_classes.pathV3
        val responseType = object : TypeToken<List<ProductShippingClassApiResponse>>() {}.type
        val params = mutableMapOf(
                "per_page" to pageSize.toString(),
                "offset" to offset.toString()
        )

        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params, responseType,
                { response: List<ProductShippingClassApiResponse>? ->
                    val shippingClassList = response?.map {
                        productShippingClassResponseToProductShippingClassModel(it, site)
                    }.orEmpty()

                    val loadedMore = offset > 0
                    val canLoadMore = shippingClassList.size == pageSize
                    val payload = RemoteProductShippingClassListPayload(
                            site, shippingClassList, offset, loadedMore, canLoadMore
                    )
                    dispatcher.dispatch(WCProductActionBuilder.newFetchedProductShippingClassListAction(payload))
                },
                WPComErrorListener { networkError ->
                    val productError = networkErrorToProductError(networkError)
                    val payload = RemoteProductShippingClassListPayload(productError, site)
                    dispatcher.dispatch(WCProductActionBuilder.newFetchedProductShippingClassListAction(payload))
                },
                { request: WPComGsonRequest<*> -> add(request) })
        add(request)
    }

    /**
     * Makes a GET request to `/wp-json/wc/v3/products/[remoteProductId]` to fetch a single product
     *
     * Dispatches a WCProductAction.FETCHED_SINGLE_PRODUCT action with the result
     *
     * @param [remoteProductId] Unique server id of the product to fetch
     */
    fun fetchSingleProduct(site: SiteModel, remoteProductId: Long) {
        val url = WOOCOMMERCE.products.id(remoteProductId).pathV3
        val responseType = object : TypeToken<ProductApiResponse>() {}.type
        val params = emptyMap<String, String>()
        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params, responseType,
                { response: ProductApiResponse? ->
                    response?.let {
                        val newModel = productResponseToProductModel(it).apply {
                            localSiteId = site.id
                        }
                        val payload = RemoteProductPayload(newModel, site)
                        dispatcher.dispatch(WCProductActionBuilder.newFetchedSingleProductAction(payload))
                    }
                },
                WPComErrorListener { networkError ->
                    val productError = networkErrorToProductError(networkError)
                    val payload = RemoteProductPayload(
                            productError,
                            WCProductModel().apply { this.remoteProductId = remoteProductId },
                            site
                    )
                    dispatcher.dispatch(WCProductActionBuilder.newFetchedSingleProductAction(payload))
                },
                { request: WPComGsonRequest<*> -> add(request) })
        add(request)
    }

    /**
     * Makes a GET call to `/wc/v3/products` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * retrieving a list of products for the given WooCommerce [SiteModel].
     *
     * Dispatches a [WCProductAction.FETCHED_PRODUCTS] action with the resulting list of products.
     */
    fun fetchProducts(
        site: SiteModel,
        pageSize: Int = DEFAULT_PRODUCT_PAGE_SIZE,
        offset: Int = 0,
        sortType: ProductSorting = DEFAULT_PRODUCT_SORTING,
        searchQuery: String? = null,
        remoteProductIds: List<Long>? = null
    ) {
        // orderby (string) Options: date, id, include, title and slug. Default is date.
        val orderBy = when (sortType) {
            TITLE_ASC, TITLE_DESC -> "title"
            DATE_ASC, DATE_DESC -> "date"
        }
        val sortOrder = when (sortType) {
            TITLE_ASC, DATE_ASC -> "asc"
            TITLE_DESC, DATE_DESC -> "desc"
        }

        val url = WOOCOMMERCE.products.pathV3
        val responseType = object : TypeToken<List<ProductApiResponse>>() {}.type
        val params = mutableMapOf(
                "per_page" to pageSize.toString(),
                "orderby" to orderBy,
                "order" to sortOrder,
                "offset" to offset.toString(),
                "search" to (searchQuery ?: ""))
        remoteProductIds?.let { ids ->
            params.put("include", ids.map { it }.joinToString())
        }

        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params, responseType,
                { response: List<ProductApiResponse>? ->
                    val productModels = response?.map {
                        productResponseToProductModel(it).apply { localSiteId = site.id }
                    }.orEmpty()

                    val loadedMore = offset > 0
                    val canLoadMore = productModels.size == pageSize
                    if (searchQuery == null) {
                        val payload = RemoteProductListPayload(
                                site,
                                productModels,
                                offset,
                                loadedMore,
                                canLoadMore
                        )
                        dispatcher.dispatch(WCProductActionBuilder.newFetchedProductsAction(payload))
                    } else {
                        val payload = RemoteSearchProductsPayload(
                                site,
                                searchQuery,
                                productModels,
                                offset,
                                loadedMore,
                                canLoadMore
                        )
                        dispatcher.dispatch(WCProductActionBuilder.newSearchedProductsAction(payload))
                    }
                },
                WPComErrorListener { networkError ->
                    val productError = networkErrorToProductError(networkError)
                    if (searchQuery == null) {
                        val payload = RemoteProductListPayload(productError, site)
                        dispatcher.dispatch(WCProductActionBuilder.newFetchedProductsAction(payload))
                    } else {
                        val payload = RemoteSearchProductsPayload(productError, site, searchQuery)
                        dispatcher.dispatch(WCProductActionBuilder.newSearchedProductsAction(payload))
                    }
                },
                { request: WPComGsonRequest<*> -> add(request) })
        add(request)
    }

    fun searchProducts(
        site: SiteModel,
        searchQuery: String,
        pageSize: Int = DEFAULT_PRODUCT_PAGE_SIZE,
        offset: Int = 0,
        sorting: ProductSorting = DEFAULT_PRODUCT_SORTING
    ) {
        fetchProducts(site, pageSize, offset, sorting, searchQuery)
    }

    /**
     * Makes a GET call to `/wc/v3/products` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * for a given [SiteModel] and [sku] to check if this [sku] already exists on the site
     *
     * Dispatches a [WCProductAction.FETCHED_PRODUCT_SKU_AVAILABILITY] action with the availability for the [sku].
     */
    fun fetchProductSkuAvailability(
        site: SiteModel,
        sku: String
    ) {
        val url = WOOCOMMERCE.products.pathV3
        val responseType = object : TypeToken<List<ProductApiResponse>>() {}.type
        val params = mutableMapOf("sku" to sku, "_fields" to "sku")

        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params, responseType,
                { response: List<ProductApiResponse>? ->
                    val available = response?.isEmpty() ?: false
                    val payload = RemoteProductSkuAvailabilityPayload(site, sku, available)
                    dispatcher.dispatch(WCProductActionBuilder.newFetchedProductSkuAvailabilityAction(payload))
                },
                WPComErrorListener { networkError ->
                    val productError = networkErrorToProductError(networkError)
                    // If there is a network error of some sort that prevents us from knowing if a sku is available
                    // then just consider sku as available
                    val payload = RemoteProductSkuAvailabilityPayload(productError, site, sku, true)
                    dispatcher.dispatch(WCProductActionBuilder.newFetchedProductSkuAvailabilityAction(payload))
                },
                { request: WPComGsonRequest<*> -> add(request) })
        add(request)
    }

    /**
     * Makes a GET request to `POST /wp-json/wc/v3/products/[productId]/variations` to fetch
     * variations for a product
     *
     * Dispatches a WCProductAction.FETCHED_PRODUCT_VARIATIONS action with the result
     *
     * @param [productId] Unique server id of the product
     *
     * Variations by default are sorted by `menu_order` with sorting order = desc.
     * i.e. `orderby` = `menu_order` and `order` = `desc`
     *
     * We do not pass `orderby` field in the request here because the API does not support `orderby`
     * with `menu_order` as value. But we still need to pass `order` field to the API request in order to
     * preserve the sorting order when fetching multiple pages of variations.
     *
     */
    fun fetchProductVariations(
        site: SiteModel,
        productId: Long,
        pageSize: Int = DEFAULT_PRODUCT_VARIATIONS_PAGE_SIZE,
        offset: Int = 0
    ) {
        val url = WOOCOMMERCE.products.id(productId).variations.pathV3
        val responseType = object : TypeToken<List<ProductVariationApiResponse>>() {}.type
        val params = mutableMapOf(
                "per_page" to pageSize.toString(),
                "offset" to offset.toString(),
                "order" to "asc")

        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params, responseType,
                { response: List<ProductVariationApiResponse>? ->
                    val variationModels = response?.map {
                        productVariationResponseToProductVariationModel(it).apply {
                            localSiteId = site.id
                            remoteProductId = productId
                        }
                    }.orEmpty()

                    val loadedMore = offset > 0
                    val canLoadMore = variationModels.size == pageSize
                    val payload = RemoteProductVariationsPayload(
                            site, productId, variationModels, offset, loadedMore, canLoadMore
                    )
                    dispatcher.dispatch(WCProductActionBuilder.newFetchedProductVariationsAction(payload))
                },
                WPComErrorListener { networkError ->
                    val productError = networkErrorToProductError(networkError)
                    val payload = RemoteProductVariationsPayload(
                            productError,
                            site,
                            productId
                    )
                    dispatcher.dispatch(WCProductActionBuilder.newFetchedProductVariationsAction(payload))
                },
                { request: WPComGsonRequest<*> -> add(request) })
        add(request)
    }

    /**
     * Makes a PUT request to `/wp-json/wc/v3/products/remoteProductId` to update a product
     *
     * Dispatches a WCProductAction.UPDATED_PRODUCT action with the result
     *
     * @param [site] The site to fetch product reviews for
     * @param [storedWCProductModel] the stored model to compare with the [updatedProductModel]
     * @param [updatedProductModel] the product model that contains the update
     */
    fun updateProduct(
        site: SiteModel,
        storedWCProductModel: WCProductModel?,
        updatedProductModel: WCProductModel
    ) {
        val remoteProductId = updatedProductModel.remoteProductId
        val url = WOOCOMMERCE.products.id(remoteProductId).pathV3
        val responseType = object : TypeToken<ProductApiResponse>() {}.type
        val body = productModelToProductJsonBody(storedWCProductModel, updatedProductModel)

        val request = JetpackTunnelGsonRequest.buildPutRequest(url, site.siteId, body, responseType,
                { response: ProductApiResponse? ->
                    response?.let {
                        val newModel = productResponseToProductModel(it).apply {
                            localSiteId = site.id
                        }
                        val payload = RemoteUpdateProductPayload(site, newModel)
                        dispatcher.dispatch(WCProductActionBuilder.newUpdatedProductAction(payload))
                    }
                },
                WPComErrorListener { networkError ->
                    val productError = networkErrorToProductError(networkError)
                    val payload = RemoteUpdateProductPayload(
                            productError,
                            site,
                            WCProductModel().apply { this.remoteProductId = remoteProductId }
                    )
                    dispatcher.dispatch(WCProductActionBuilder.newUpdatedProductAction(payload))
                })
        add(request)
    }

    /**
     * Makes a PUT request to `/wp-json/wc/v3/products/[remoteProductId]` to replace a product's images
     * with the passed media list
     *
     * Dispatches a WCProductAction.UPDATED_PRODUCT_IMAGES action with the result
     *
     * @param [site] The site to fetch product reviews for
     * @param [remoteProductId] Unique server id of the product to update
     * @param [imageList] list of product images to assign to the product
     */
    fun updateProductImages(site: SiteModel, remoteProductId: Long, imageList: List<WCProductImageModel>) {
        val url = WOOCOMMERCE.products.id(remoteProductId).pathV3
        val responseType = object : TypeToken<ProductApiResponse>() {}.type

        // build json list of images
        val jsonBody = JsonArray()
        for (image in imageList) {
            jsonBody.add(image.toJson())
        }
        val body = HashMap<String, Any>()
        body["id"] = remoteProductId
        body["images"] = jsonBody

        val request = JetpackTunnelGsonRequest.buildPutRequest(url, site.siteId, body, responseType,
                { response: ProductApiResponse? ->
                    response?.let {
                        val newModel = productResponseToProductModel(it).apply {
                            localSiteId = site.id
                        }
                        val payload = RemoteUpdateProductImagesPayload(site, newModel)
                        dispatcher.dispatch(WCProductActionBuilder.newUpdatedProductImagesAction(payload))
                    }
                },
                WPComErrorListener { networkError ->
                    val productError = networkErrorToProductError(networkError)
                    val payload = RemoteUpdateProductImagesPayload(
                            productError,
                            site,
                            WCProductModel().apply { this.remoteProductId = remoteProductId }
                    )
                    dispatcher.dispatch(WCProductActionBuilder.newUpdatedProductImagesAction(payload))
                })
        add(request)
    }

    /**
     * Makes a GET call to `/wc/v3/products/reviews` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * retrieving a list of product reviews for a given WooCommerce [SiteModel].
     *
     * The number of reviews to fetch is defined in [WCProductStore.NUM_REVIEWS_PER_FETCH], and retrieving older
     * reviews is done by passing an [offset].
     *
     * Dispatches a [WCProductAction.FETCHED_PRODUCT_REVIEWS]
     *
     * @param [site] The site to fetch product reviews for
     * @param [offset] The offset to use for the fetch
     * @param [reviewIds] Optional. A list of remote product review ID's to fetch
     * @param [productIds] Optional. A list of remote product ID's to fetch product reviews for
     * @param [filterByStatus] Optional. A list of product review statuses to fetch
     */
    fun fetchProductReviews(
        site: SiteModel,
        offset: Int,
        reviewIds: List<Long>? = null,
        productIds: List<Long>? = null,
        filterByStatus: List<String>? = null
    ) {
        val statusFilter = filterByStatus?.joinToString { it } ?: "all"

        val url = WOOCOMMERCE.products.reviews.pathV3
        val responseType = object : TypeToken<List<ProductReviewApiResponse>>() {}.type
        val params = mutableMapOf(
                "per_page" to WCProductStore.NUM_REVIEWS_PER_FETCH.toString(),
                "offset" to offset.toString(),
                "status" to statusFilter)
        reviewIds?.let { ids ->
            params.put("include", ids.map { it }.joinToString())
        }
        productIds?.let { ids ->
            params.put("product", ids.map { it }.joinToString())
        }
        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params, responseType,
                { response: List<ProductReviewApiResponse>? ->
                    response?.let {
                        val reviews = it.map { review ->
                            productReviewResponseToProductReviewModel(review).apply { localSiteId = site.id }
                        }
                        val canLoadMore = reviews.size == WCProductStore.NUM_REVIEWS_PER_FETCH
                        val loadedMore = offset > 0
                        val payload = FetchProductReviewsResponsePayload(
                                site, reviews, productIds, filterByStatus, loadedMore, canLoadMore)
                        dispatcher.dispatch(WCProductActionBuilder.newFetchedProductReviewsAction(payload))
                    }
                },
                WPComErrorListener { networkError ->
                    val productReviewError = networkErrorToProductError(networkError)
                    val payload = FetchProductReviewsResponsePayload(productReviewError, site)
                    dispatcher.dispatch(WCProductActionBuilder.newFetchedProductReviewsAction(payload))
                },
                { request: WPComGsonRequest<*> -> add(request) })
        add(request)
    }

    /**
     * Makes a GET call to `/wc/v3/products/reviews/<id>` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * retrieving a product review by it's remote ID for a given WooCommerce [SiteModel].
     *
     * Dispatches a [WCProductAction.FETCHED_SINGLE_PRODUCT_REVIEW]
     *
     * @param [site] The site to fetch product reviews for
     * @param [remoteReviewId] The remote id of the review to fetch
     */
    fun fetchProductReviewById(site: SiteModel, remoteReviewId: Long) {
        val url = WOOCOMMERCE.products.reviews.id(remoteReviewId).pathV3
        val responseType = object : TypeToken<ProductReviewApiResponse>() {}.type
        val params = emptyMap<String, String>()
        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params, responseType,
                { response: ProductReviewApiResponse? ->
                    response?.let {
                        val review = productReviewResponseToProductReviewModel(response).apply {
                            localSiteId = site.id
                        }
                        val payload = RemoteProductReviewPayload(site, review)
                        dispatcher.dispatch(WCProductActionBuilder.newFetchedSingleProductReviewAction(payload))
                    }
                },
                WPComErrorListener { networkError ->
                    val productReviewError = networkErrorToProductError(networkError)
                    val payload = RemoteProductReviewPayload(error = productReviewError, site = site)
                    dispatcher.dispatch(WCProductActionBuilder.newFetchedSingleProductReviewAction(payload))
                },
                { request: WPComGsonRequest<*> -> add(request) })
        add(request)
    }

    /**
     * Makes a PUT call to `/wc/v3/products/reviews/<id>` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * updating the status for the given product review to [newStatus].
     *
     * Dispatches a [WCProductAction.UPDATED_PRODUCT_REVIEW_STATUS]
     *
     * @param [site] The site to fetch product reviews for
     * @param [remoteReviewId] The remote ID of the product review to be updated
     * @param [newStatus] The new status to update the product review to
     */
    fun updateProductReviewStatus(site: SiteModel, remoteReviewId: Long, newStatus: String) {
        val url = WOOCOMMERCE.products.reviews.id(remoteReviewId).pathV3
        val responseType = object : TypeToken<ProductReviewApiResponse>() {}.type
        val params = mapOf("status" to newStatus)
        val request = JetpackTunnelGsonRequest.buildPutRequest(url, site.siteId, params, responseType,
                { response: ProductReviewApiResponse? ->
                    response?.let {
                        val review = productReviewResponseToProductReviewModel(response).apply {
                            localSiteId = site.id
                        }
                        val payload = RemoteProductReviewPayload(site, review)
                        dispatcher.dispatch(WCProductActionBuilder.newUpdatedProductReviewStatusAction(payload))
                    }
                },
                WPComErrorListener { networkError ->
                    val productReviewError = networkErrorToProductError(networkError)
                    val payload = RemoteProductReviewPayload(productReviewError, site)
                    dispatcher.dispatch(WCProductActionBuilder.newUpdatedProductReviewStatusAction(payload))
                })
        add(request)
    }

    /**
     * Build json body of product items to be updated to the backend.
     *
     * This method checks if there is a cached version of the product stored locally.
     * If not, it generates a new product model for the same product ID, with default fields
     * and verifies that the [updatedProductModel] has fields that are different from the default
     * fields of [productModel]. This is to ensure that we do not update product fields that do not contain any changes
     */
    private fun productModelToProductJsonBody(
        productModel: WCProductModel?,
        updatedProductModel: WCProductModel
    ): HashMap<String, Any> {
        val body = HashMap<String, Any>()

        val storedWCProductModel = productModel ?: WCProductModel().apply {
            remoteProductId = updatedProductModel.remoteProductId
        }
        if (storedWCProductModel.description != updatedProductModel.description) {
            body["description"] = updatedProductModel.description
        }
        if (storedWCProductModel.name != updatedProductModel.name) {
            body["name"] = updatedProductModel.name
        }
        if (storedWCProductModel.sku != updatedProductModel.sku) {
            body["sku"] = updatedProductModel.sku
        }
        if (storedWCProductModel.manageStock != updatedProductModel.manageStock) {
            body["manage_stock"] = updatedProductModel.manageStock
        }

        // only allowed to change the following params if manageStock is enabled
        if (updatedProductModel.manageStock) {
            if (storedWCProductModel.stockQuantity != updatedProductModel.stockQuantity) {
                body["stock_quantity"] = updatedProductModel.stockQuantity
            }
            if (storedWCProductModel.backorders != updatedProductModel.backorders) {
                body["backorders"] = updatedProductModel.backorders
            }
        }
        if (storedWCProductModel.stockStatus != updatedProductModel.stockStatus) {
            body["stock_status"] = updatedProductModel.stockStatus
        }
        if (storedWCProductModel.soldIndividually != updatedProductModel.soldIndividually) {
            body["sold_individually"] = updatedProductModel.soldIndividually
        }
        if (storedWCProductModel.regularPrice != updatedProductModel.regularPrice) {
            body["regular_price"] = updatedProductModel.regularPrice
        }
        if (storedWCProductModel.salePrice != updatedProductModel.salePrice) {
            body["sale_price"] = updatedProductModel.salePrice
        }
        if (storedWCProductModel.dateOnSaleFromGmt != updatedProductModel.dateOnSaleFromGmt) {
            body["date_on_sale_from_gmt"] = updatedProductModel.dateOnSaleFromGmt
        }
        if (storedWCProductModel.dateOnSaleToGmt != updatedProductModel.dateOnSaleToGmt) {
            body["date_on_sale_to_gmt"] = updatedProductModel.dateOnSaleToGmt
        }
        if (storedWCProductModel.taxStatus != updatedProductModel.taxStatus) {
            body["tax_status"] = updatedProductModel.taxStatus
        }
        if (storedWCProductModel.taxClass != updatedProductModel.taxClass) {
            body["tax_class"] = updatedProductModel.taxClass
        }
        if (storedWCProductModel.weight != updatedProductModel.weight) {
            body["weight"] = updatedProductModel.weight
        }

        val dimensionsBody = mutableMapOf<String, String>()
        if (storedWCProductModel.height != updatedProductModel.height) {
            dimensionsBody["height"] = updatedProductModel.height
        }
        if (storedWCProductModel.width != updatedProductModel.width) {
            dimensionsBody["width"] = updatedProductModel.width
        }
        if (storedWCProductModel.length != updatedProductModel.length) {
            dimensionsBody["length"] = updatedProductModel.length
        }
        if (dimensionsBody.isNotEmpty()) {
            body["dimensions"] = dimensionsBody
        }
        if (storedWCProductModel.shippingClass != updatedProductModel.shippingClass) {
            body["shipping_class"] = updatedProductModel.shippingClass
        }
        if (storedWCProductModel.shortDescription != updatedProductModel.shortDescription) {
            body["short_description"] = updatedProductModel.shortDescription
        }
        if (storedWCProductModel.images != updatedProductModel.images) {
            body["images"] = JsonArray().also {
                for (image in updatedProductModel.getImages()) {
                    it.add(image.toJson())
                }
            }
        }

        return body
    }

    private fun productShippingClassResponseToProductShippingClassModel(
        response: ProductShippingClassApiResponse,
        site: SiteModel
    ): WCProductShippingClassModel {
        return WCProductShippingClassModel().apply {
            remoteShippingClassId = response.id
            localSiteId = site.id
            name = response.name ?: ""
            slug = response.slug ?: ""
            description = response.description ?: ""
        }
    }

    private fun productResponseToProductModel(response: ProductApiResponse): WCProductModel {
        return WCProductModel().apply {
            remoteProductId = response.id ?: 0
            name = response.name ?: ""
            slug = response.slug ?: ""
            permalink = response.permalink ?: ""

            dateCreated = response.date_created ?: ""
            dateModified = response.date_modified ?: ""

            dateOnSaleFrom = response.date_on_sale_from ?: ""
            dateOnSaleTo = response.date_on_sale_to ?: ""
            dateOnSaleFromGmt = response.date_on_sale_from_gmt ?: ""
            dateOnSaleToGmt = response.date_on_sale_to_gmt ?: ""

            type = response.type ?: ""
            status = response.status ?: ""
            featured = response.featured
            catalogVisibility = response.catalog_visibility ?: ""
            description = response.description ?: ""
            shortDescription = response.short_description ?: ""
            sku = response.sku ?: ""

            price = response.price ?: ""
            regularPrice = response.regular_price ?: ""
            salePrice = response.sale_price ?: ""
            onSale = response.on_sale
            totalSales = response.total_sales

            virtual = response.virtual
            downloadable = response.downloadable
            downloadLimit = response.download_limit
            downloadExpiry = response.download_expiry
            externalUrl = response.external_url ?: ""

            taxStatus = response.tax_status ?: ""
            taxClass = response.tax_class ?: ""

            // variations may have "parent" here if inventory is enabled for the parent but not the variation
            manageStock = response.manage_stock?.let {
                it == "true" || it == "parent"
            } ?: false

            stockQuantity = response.stock_quantity
            stockStatus = response.stock_status ?: ""

            backorders = response.backorders ?: ""
            backordersAllowed = response.backorders_allowed
            backordered = response.backordered
            soldIndividually = response.sold_individually
            weight = response.weight ?: ""

            shippingRequired = response.shipping_required
            shippingTaxable = response.shipping_taxable
            shippingClass = response.shipping_class ?: ""
            shippingClassId = response.shipping_class_id

            reviewsAllowed = response.reviews_allowed
            averageRating = response.average_rating ?: ""
            ratingCount = response.rating_count

            parentId = response.parent_id
            purchaseNote = response.purchase_note ?: ""

            categories = response.categories?.toString() ?: ""
            tags = response.tags?.toString() ?: ""
            images = response.images?.toString() ?: ""
            attributes = response.attributes?.toString() ?: ""
            variations = response.variations?.toString() ?: ""
            downloads = response.downloads?.toString() ?: ""
            relatedIds = response.related_ids?.toString() ?: ""
            crossSellIds = response.cross_sell_ids?.toString() ?: ""
            upsellIds = response.upsell_ids?.toString() ?: ""

            response.dimensions?.asJsonObject?.let { json ->
                length = json.getString("length") ?: ""
                width = json.getString("width") ?: ""
                height = json.getString("height") ?: ""
            }
        }
    }

    private fun productVariationResponseToProductVariationModel(
        response: ProductVariationApiResponse
    ): WCProductVariationModel {
        return WCProductVariationModel().apply {
            remoteVariationId = response.id
            permalink = response.permalink ?: ""

            dateCreated = response.date_created ?: ""
            dateModified = response.date_modified ?: ""

            status = response.status ?: ""
            description = response.description ?: ""
            sku = response.sku ?: ""

            price = response.price ?: ""
            regularPrice = response.regular_price ?: ""
            salePrice = response.sale_price ?: ""
            onSale = response.on_sale

            virtual = response.virtual
            downloadable = response.downloadable
            purchasable = response.purchasable

            manageStock = response.manage_stock
            stockQuantity = response.stock_quantity
            stockStatus = response.stock_status ?: ""

            attributes = response.attributes?.toString() ?: ""

            weight = response.weight ?: ""
            menuOrder = response.menu_order

            response.dimensions?.asJsonObject?.let { json ->
                length = json.getString("length") ?: ""
                width = json.getString("width") ?: ""
                height = json.getString("height") ?: ""
            }

            response.image?.let {
                (it as? JsonObject)?.let { json ->
                    imageUrl = json.getString("src") ?: ""
                } ?: ""
            }
        }
    }

    private fun productReviewResponseToProductReviewModel(response: ProductReviewApiResponse): WCProductReviewModel {
        return WCProductReviewModel().apply {
            remoteProductReviewId = response.id
            remoteProductId = response.product_id
            dateCreated = response.date_created_gmt?.let { "${it}Z" } ?: ""
            status = response.status ?: ""
            reviewerName = response.reviewer ?: ""
            reviewerEmail = response.reviewer_email ?: ""
            review = response.review ?: ""
            rating = response.rating
            verified = response.verified
            reviewerAvatarsJson = response.reviewer_avatar_urls?.toString() ?: ""
        }
    }

    private fun networkErrorToProductError(wpComError: WPComGsonNetworkError): ProductError {
        val productErrorType = when (wpComError.apiError) {
            "rest_invalid_param" -> ProductErrorType.INVALID_PARAM
            "woocommerce_rest_review_invalid_id" -> ProductErrorType.INVALID_REVIEW_ID
            "woocommerce_product_invalid_image_id" -> ProductErrorType.INVALID_IMAGE_ID
            "product_invalid_sku" -> ProductErrorType.DUPLICATE_SKU
            else -> ProductErrorType.fromString(wpComError.apiError)
        }
        return ProductError(productErrorType, wpComError.message)
    }
}
