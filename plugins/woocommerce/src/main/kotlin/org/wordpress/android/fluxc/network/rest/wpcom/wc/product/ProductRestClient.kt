package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.model.WCProductImageModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.model.WCProductShippingClassModel
import org.wordpress.android.fluxc.model.WCProductTagModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostWPComRestResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.*
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.Companion.DEFAULT_CATEGORY_SORTING
import org.wordpress.android.fluxc.store.WCProductStore.Companion.DEFAULT_PRODUCT_CATEGORY_PAGE_SIZE
import org.wordpress.android.fluxc.store.WCProductStore.Companion.DEFAULT_PRODUCT_PAGE_SIZE
import org.wordpress.android.fluxc.store.WCProductStore.Companion.DEFAULT_PRODUCT_SHIPPING_CLASS_PAGE_SIZE
import org.wordpress.android.fluxc.store.WCProductStore.Companion.DEFAULT_PRODUCT_SORTING
import org.wordpress.android.fluxc.store.WCProductStore.Companion.DEFAULT_PRODUCT_TAGS_PAGE_SIZE
import org.wordpress.android.fluxc.store.WCProductStore.Companion.DEFAULT_PRODUCT_VARIATIONS_PAGE_SIZE
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductReviewsResponsePayload
import org.wordpress.android.fluxc.store.WCProductStore.ProductCategorySorting
import org.wordpress.android.fluxc.store.WCProductStore.ProductCategorySorting.NAME_DESC
import org.wordpress.android.fluxc.store.WCProductStore.ProductError
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.DATE_ASC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.DATE_DESC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.TITLE_ASC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.TITLE_DESC
import org.wordpress.android.fluxc.store.WCProductStore.RemoteAddProductCategoryResponsePayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteAddProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteAddProductTagsResponsePayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteDeleteProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductCategoriesPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductListPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductPasswordPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductReviewPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductShippingClassListPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductShippingClassPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductSkuAvailabilityPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductTagsPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductVariationsPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteSearchProductsPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdateProductImagesPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdateProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdateVariationPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdatedProductPasswordPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteVariationPayload
import org.wordpress.android.fluxc.utils.handleResult
import org.wordpress.android.fluxc.utils.putIfNotEmpty
import org.wordpress.android.fluxc.utils.putIfNotNull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Suppress("LargeClass")
@Singleton
class ProductRestClient @Inject constructor(
    appContext: Context,
    private val dispatcher: Dispatcher,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder,
    private val wooNetwork: WooNetwork
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
                { networkError ->
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
                { networkError ->
                    val productError = networkErrorToProductError(networkError)
                    val payload = RemoteProductShippingClassListPayload(productError, site)
                    dispatcher.dispatch(WCProductActionBuilder.newFetchedProductShippingClassListAction(payload))
                },
                { request: WPComGsonRequest<*> -> add(request) })
        add(request)
    }

    /**
     * Makes a GET request to `GET /wp-json/wc/v3/products/tags` to fetch
     * product tags for a site
     *
     * Dispatches a WCProductAction.FETCHED_PRODUCT_TAGS action with the result
     *
     * @param [site] The site to fetch product shipping class list for
     * @param [pageSize] The size of the tags needed from the API response
     * @param [offset] The page number passed to the API
     */
    fun fetchProductTags(
        site: SiteModel,
        pageSize: Int = DEFAULT_PRODUCT_TAGS_PAGE_SIZE,
        offset: Int = 0,
        searchQuery: String? = null
    ) {
        val url = WOOCOMMERCE.products.tags.pathV3
        val responseType = object : TypeToken<List<ProductTagApiResponse>>() {}.type
        val params = mutableMapOf(
                "per_page" to pageSize.toString(),
                "offset" to offset.toString()
        ).putIfNotEmpty("search" to searchQuery)

        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params, responseType,
                { response: List<ProductTagApiResponse>? ->
                    val tags = response?.map {
                        productTagApiResponseToProductTagModel(it, site)
                    }.orEmpty()

                    val loadedMore = offset > 0
                    val canLoadMore = tags.size == pageSize
                    val payload = RemoteProductTagsPayload(site, tags, offset, loadedMore, canLoadMore, searchQuery)
                    dispatcher.dispatch(WCProductActionBuilder.newFetchedProductTagsAction(payload))
                },
                { networkError ->
                    val productError = networkErrorToProductError(networkError)
                    val payload = RemoteProductTagsPayload(productError, site)
                    dispatcher.dispatch(WCProductActionBuilder.newFetchedProductTagsAction(payload))
                },
                { request: WPComGsonRequest<*> -> add(request) })
        add(request)
    }

    /**
     * Makes a POST request to `POST /wp-json/wc/v3/products/tags/batch` to add
     * product tags for a site
     *
     * Dispatches a [WCProductAction.ADDED_PRODUCT_TAGS] action with the result
     *
     * @param [site] The site to fetch product shipping class list for
     * @param [tags] The list of tag names that needed to be added to the site
     */
    fun addProductTags(
        site: SiteModel,
        tags: List<String>
    ) {
        val url = WOOCOMMERCE.products.tags.batch.pathV3
        val responseType = object : TypeToken<BatchAddProductTagApiResponse>() {}.type
        val params = mutableMapOf(
                "create" to tags.map { mapOf("name" to it) }
        )

        val request = JetpackTunnelGsonRequest.buildPostRequest(url, site.siteId, params, responseType,
                { response: BatchAddProductTagApiResponse? ->
                    val addedTags = response?.addedTags?.map {
                        productTagApiResponseToProductTagModel(it, site)
                    }.orEmpty()

                    val payload = RemoteAddProductTagsResponsePayload(site, addedTags)
                    dispatcher.dispatch(WCProductActionBuilder.newAddedProductTagsAction(payload))
                },
                { networkError ->
                    val productError = networkErrorToProductError(networkError)
                    val payload = RemoteAddProductTagsResponsePayload(productError, site)
                    dispatcher.dispatch(WCProductActionBuilder.newAddedProductTagsAction(payload))
                })
        add(request)
    }

    /**
     * Makes a GET request to `/wp-json/wc/v3/products/[remoteProductId]` to fetch a single product
     *
     * Dispatches a WCProductAction.FETCHED_SINGLE_PRODUCT action with the result
     *
     * @param [remoteProductId] Unique server id of the product to fetch
     */
    suspend fun fetchSingleProduct(site: SiteModel, remoteProductId: Long): RemoteProductPayload {
        val url = WOOCOMMERCE.products.id(remoteProductId).pathV3

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
                this,
                site,
                url,
                emptyMap(),
                ProductApiResponse::class.java
        )

        return when (response) {
            is JetpackSuccess -> {
                response.data?.let {
                    val newModel = it.asProductModel().apply {
                        localSiteId = site.id
                    }
                    RemoteProductPayload(newModel, site)
                } ?: RemoteProductPayload(
                        ProductError(GENERIC_ERROR, "Success response with empty data"),
                        WCProductModel().apply { this.remoteProductId = remoteProductId },
                        site
                )
            }
            is JetpackError -> {
                val productError = networkErrorToProductError(response.error)
                RemoteProductPayload(
                        productError,
                        WCProductModel().apply { this.remoteProductId = remoteProductId },
                        site
                )
            }
        }
    }

    /**
     * Makes a GET request to `/wp-json/wc/v3/products/[remoteProductId]/variations/[remoteVariationId]` to fetch
     * a single product variation
     *
     *
     * @param [remoteProductId] Unique server id of the product to fetch
     * @param [remoteVariationId] Unique server id of the variation to fetch
     */
    suspend fun fetchSingleVariation(
        site: SiteModel,
        remoteProductId: Long,
        remoteVariationId: Long
    ): RemoteVariationPayload {
        val url = WOOCOMMERCE.products.id(remoteProductId).variations.variation(remoteVariationId).pathV3
        val params = emptyMap<String, String>()

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
            this, site, url, params, ProductVariationApiResponse::class.java
        )

        return when (response) {
            is JetpackSuccess -> {
                val productData = response.data
                if (productData != null) {
                    RemoteVariationPayload(
                        productData.asProductVariationModel().apply {
                            this.remoteProductId = remoteProductId
                            localSiteId = site.id
                        },
                        site
                    )
                } else {
                    RemoteVariationPayload(
                        ProductError(GENERIC_ERROR, "Success response with empty data"),
                        WCProductVariationModel().apply {
                            this.remoteProductId = remoteProductId
                            this.remoteVariationId = remoteVariationId
                        },
                        site
                    )
                }
            }
            is JetpackError -> {
                RemoteVariationPayload(
                    networkErrorToProductError(response.error),
                    WCProductVariationModel().apply {
                        this.remoteProductId = remoteProductId
                        this.remoteVariationId = remoteVariationId
                    },
                    site
                )
            }
        }
    }

    /**
     * Makes a GET call to `/wc/v3/products` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * retrieving a list of products for the given WooCommerce [SiteModel].
     *
     * Dispatches a [WCProductAction.FETCHED_PRODUCTS] action with the resulting list of products.
     */
    @Suppress("LongMethod")
    fun fetchProducts(
        site: SiteModel,
        pageSize: Int = DEFAULT_PRODUCT_PAGE_SIZE,
        offset: Int = 0,
        sortType: ProductSorting = DEFAULT_PRODUCT_SORTING,
        searchQuery: String? = null,
        isSkuSearch: Boolean = false,
        includedProductIds: List<Long>? = null,
        filterOptions: Map<ProductFilterOption, String>? = null,
        excludedProductIds: List<Long>? = null
    ) {
        val url = WOOCOMMERCE.products.pathV3
        val responseType = object : TypeToken<List<ProductApiResponse>>() {}.type
        val params = buildProductParametersMap(
            pageSize = pageSize,
            sortType = sortType,
            offset = offset,
            searchQuery = searchQuery,
            isSkuSearch = isSkuSearch,
            includedProductIds = includedProductIds,
            excludedProductIds = excludedProductIds,
            filterOptions = filterOptions
        )

        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params, responseType,
                { response: List<ProductApiResponse>? ->
                    val productModels = response?.map {
                        it.asProductModel().apply { localSiteId = site.id }
                    }.orEmpty()

                    val loadedMore = offset > 0
                    val canLoadMore = productModels.size == pageSize
                    if (searchQuery == null) {
                        val payload = RemoteProductListPayload(
                                site,
                                productModels,
                                offset,
                                loadedMore,
                                canLoadMore,
                                includedProductIds,
                                excludedProductIds
                        )
                        dispatcher.dispatch(WCProductActionBuilder.newFetchedProductsAction(payload))
                    } else {
                        val payload = RemoteSearchProductsPayload(
                            site = site,
                            searchQuery = searchQuery,
                            isSkuSearch = isSkuSearch,
                            products = productModels,
                            offset = offset,
                            loadedMore = loadedMore,
                            canLoadMore = canLoadMore
                        )
                        dispatcher.dispatch(WCProductActionBuilder.newSearchedProductsAction(payload))
                    }
                },
                { networkError ->
                    val productError = networkErrorToProductError(networkError)
                    if (searchQuery == null) {
                        val payload = RemoteProductListPayload(productError, site)
                        dispatcher.dispatch(WCProductActionBuilder.newFetchedProductsAction(payload))
                    } else {
                        val payload = RemoteSearchProductsPayload(
                            error = productError,
                            site = site,
                            query = searchQuery,
                            skuSearch = isSkuSearch,
                            filterOptions = filterOptions
                        )
                        dispatcher.dispatch(WCProductActionBuilder.newSearchedProductsAction(payload))
                    }
                },
                { request: WPComGsonRequest<*> -> add(request) })
        add(request)
    }

    fun searchProducts(
        site: SiteModel,
        searchQuery: String,
        isSkuSearch: Boolean = false,
        pageSize: Int = DEFAULT_PRODUCT_PAGE_SIZE,
        offset: Int = 0,
        sorting: ProductSorting = DEFAULT_PRODUCT_SORTING,
        excludedProductIds: List<Long>? = null,
        filterOptions: Map<ProductFilterOption, String>? = null
    ) {
        fetchProducts(
            site = site,
            pageSize = pageSize,
            offset = offset,
            sortType = sorting,
            searchQuery = searchQuery,
            isSkuSearch = isSkuSearch,
            excludedProductIds = excludedProductIds,
            filterOptions = filterOptions
        )
    }

    /**
     * Makes a GET call to `/wc/v3/products` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * retrieving a list of products for the given WooCommerce [SiteModel].
     *
     * but requiring this call to be suspended so the return call be synced within the coroutine job
     */
    suspend fun fetchProductsWithSyncRequest(
        site: SiteModel,
        pageSize: Int = DEFAULT_PRODUCT_PAGE_SIZE,
        offset: Int = 0,
        sortType: ProductSorting = DEFAULT_PRODUCT_SORTING,
        includedProductIds: List<Long>? = null,
        excludedProductIds: List<Long>? = null,
        searchQuery: String? = null,
        isSkuSearch: Boolean = false,
        filterOptions: Map<ProductFilterOption, String>? = null
    ): WooPayload<List<WCProductModel>> {
        val params = buildProductParametersMap(
            pageSize = pageSize,
            sortType = sortType,
            offset = offset,
            searchQuery = searchQuery,
            isSkuSearch = isSkuSearch,
            includedProductIds = includedProductIds,
            excludedProductIds = excludedProductIds,
            filterOptions = filterOptions
        )

        return WOOCOMMERCE.products.pathV3.requestProductTo(site, params).handleResultFrom(site)
    }

    private suspend fun String.requestProductTo(
        site: SiteModel,
        params: Map<String, String>
    ) = jetpackTunnelGsonRequestBuilder.syncGetRequest(
            this@ProductRestClient,
            site,
            this,
            params,
            Array<ProductApiResponse>::class.java
    )

    private fun JetpackResponse<Array<ProductApiResponse>>.handleResultFrom(site: SiteModel) =
        when (this) {
            is JetpackSuccess -> {
                data
                    ?.map {
                        it.asProductModel()
                            .apply { localSiteId = site.id }
                    }
                    .orEmpty()
                    .let { WooPayload(it.toList()) }
            }
            is JetpackError -> {
                WooPayload(error.toWooError())
            }
        }

    /**
     * Makes a GET call to `/wc/v3/products/categories` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * retrieving a list of product categories for the given WooCommerce [SiteModel].
     *
     * but requiring this call to be suspended so the return call be synced within the coroutine job
     */
    suspend fun fetchProductsCategoriesWithSyncRequest(
        site: SiteModel,
        includedCategoryIds: List<Long> = emptyList(),
        excludedCategoryIds: List<Long> = emptyList(),
        searchQuery: String? = null,
        pageSize: Int = DEFAULT_PRODUCT_CATEGORY_PAGE_SIZE,
        productCategorySorting: ProductCategorySorting = DEFAULT_CATEGORY_SORTING,
        offset: Int = 0
    ): WooPayload<List<WCProductCategoryModel>> {
        val sortOrder = when (productCategorySorting) {
            NAME_DESC -> "desc"
            else -> "asc"
        }

        val params = mutableMapOf(
            "per_page" to pageSize.toString(),
            "offset" to offset.toString(),
            "order" to sortOrder,
            "orderby" to "name"
        ).putIfNotEmpty("search" to searchQuery)
        if (includedCategoryIds.isNotEmpty()) {
            params["include"] = includedCategoryIds.map { it }.joinToString()
        }
        if (excludedCategoryIds.isNotEmpty()) {
            params["exclude"] = excludedCategoryIds.map { it }.joinToString()
        }

        return WOOCOMMERCE.products.categories.pathV3
            .requestCategoryTo(site, params)
            .handleResultFrom(site)
    }

    @JvmName("handleResultFromProductCategoryApiResponse")
    private fun JetpackResponse<Array<ProductCategoryApiResponse>>.handleResultFrom(site: SiteModel) =
        when (this) {
            is JetpackSuccess -> {
                data
                    ?.map {
                        it.asProductCategoryModel()
                            .apply { localSiteId = site.id }
                    }
                    .orEmpty()
                    .let { WooPayload(it.toList()) }
            }
            is JetpackError -> {
                WooPayload(error.toWooError())
            }
        }

    @JvmName("handleResultFromProductVariationApiResponse")
    private fun JetpackResponse<Array<ProductVariationApiResponse>>.handleResultFrom(
        site: SiteModel,
        productId: Long
    ) =
        when (this) {
            is JetpackSuccess -> {
                data
                    ?.map {
                        it.asProductVariationModel()
                            .apply {
                                localSiteId = site.id
                                remoteProductId = productId
                            }
                    }
                    .orEmpty()
                    .let { WooPayload(it.toList()) }
            }
            is JetpackError -> {
                WooPayload(error.toWooError())
            }
        }

    private suspend fun String.requestCategoryTo(
        site: SiteModel,
        params: Map<String, String>
    ) = jetpackTunnelGsonRequestBuilder.syncGetRequest(
        this@ProductRestClient,
        site,
        this,
        params,
        Array<ProductCategoryApiResponse>::class.java
    )

    private fun buildProductParametersMap(
        pageSize: Int,
        sortType: ProductSorting,
        offset: Int,
        searchQuery: String?,
        isSkuSearch: Boolean,
        includedProductIds: List<Long>? = null,
        excludedProductIds: List<Long>? = null,
        filterOptions: Map<ProductFilterOption, String>? = null
    ): MutableMap<String, String> {
        val params = mutableMapOf(
            "per_page" to pageSize.toString(),
            "orderby" to sortType.asOrderByParameter(),
            "order" to sortType.asSortOrderParameter(),
            "offset" to offset.toString()
        )

        includedProductIds?.let { includedIds ->
            params.putIfNotEmpty("include" to includedIds.map { it }.joinToString())
        }
        excludedProductIds?.let { excludedIds ->
            params.putIfNotEmpty("exclude" to excludedIds.map { it }.joinToString())
        }
        filterOptions?.let { options ->
            params.putAll(options.map { it.key.toString() to it.value })
        }

        if (searchQuery.isNullOrEmpty().not()) {
            if (isSkuSearch) {
                params["sku"] = searchQuery!! // full SKU match
                params["search_sku"] = searchQuery // partial SKU match, added in core v6.6
            } else {
                params["search"] = searchQuery!!
            }
        }

        return params
    }

    private fun buildVariationParametersMap(
        pageSize: Int,
        offset: Int,
        searchQuery: String?,
        ids: List<Long>,
        excludedProductIds: List<Long>
    ) = mutableMapOf(
            "per_page" to pageSize.toString(),
            "orderby" to "date",
            "order" to "asc",
            "offset" to offset.toString()
        ).putIfNotEmpty("search" to searchQuery)
            .putIfNotEmpty("include" to ids.map { it }.joinToString())
            .putIfNotEmpty("exclude" to excludedProductIds.map { it }.joinToString())

    private fun ProductSorting.asOrderByParameter() = when (this) {
        TITLE_ASC, TITLE_DESC -> "title"
        DATE_ASC, DATE_DESC -> "date"
    }

    private fun ProductSorting.asSortOrderParameter() = when (this) {
        TITLE_ASC, DATE_ASC -> "asc"
        TITLE_DESC, DATE_DESC -> "desc"
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
                { networkError ->
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
     * Makes a WP.COM GET request to `/sites/$site/posts/$post_ID` to fetch just the password for a product
     */
    fun fetchProductPassword(site: SiteModel, remoteProductId: Long) {
        val url = WPCOMREST.sites.site(site.siteId).posts.post(remoteProductId).urlV1_1
        val params = mutableMapOf("fields" to "password")

        val request = WPComGsonRequest.buildGetRequest(url,
                params,
                PostWPComRestResponse::class.java,
                { response ->
                    val payload = RemoteProductPasswordPayload(remoteProductId, site, response.password ?: "")
                    dispatcher.dispatch(WCProductActionBuilder.newFetchedProductPasswordAction(payload))
                }
        ) { networkError ->
            val payload = RemoteProductPasswordPayload(remoteProductId, site, "")
            payload.error = networkErrorToProductError(networkError)
            dispatcher.dispatch(WCProductActionBuilder.newFetchedProductPasswordAction(payload))
        }
        add(request)
    }

    /**
     * Makes a WP.COM POST request to `/sites/$site/posts/$post_ID` to update just the password for a product
     */
    fun updateProductPassword(site: SiteModel, remoteProductId: Long, password: String) {
        val url = WPCOMREST.sites.site(site.siteId).posts.post(remoteProductId).urlV1_2
        val body = mapOf(
                "password" to password
        )

        val request = WPComGsonRequest.buildPostRequest(url,
                body,
                PostWPComRestResponse::class.java,
                { response ->
                    val payload = RemoteUpdatedProductPasswordPayload(remoteProductId, site, response.password ?: "")
                    dispatcher.dispatch(WCProductActionBuilder.newUpdatedProductPasswordAction(payload))
                }
        ) { networkError ->
            val payload = RemoteUpdatedProductPasswordPayload(remoteProductId, site, "")
            payload.error = networkErrorToProductError(networkError)
            dispatcher.dispatch(WCProductActionBuilder.newUpdatedProductPasswordAction(payload))
        }
        request.addQueryParameter("context", "edit")
        request.addQueryParameter("fields", "password")
        add(request)
    }

    /**
     * Makes a GET request to `POST /wp-json/wc/v3/products/[productId]/variations` to fetch
     * variations for a product
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
    suspend fun fetchProductVariations(
        site: SiteModel,
        productId: Long,
        pageSize: Int = DEFAULT_PRODUCT_VARIATIONS_PAGE_SIZE,
        offset: Int = 0
    ): RemoteProductVariationsPayload {
        val url = WOOCOMMERCE.products.id(productId).variations.pathV3
        val params = mutableMapOf(
                "per_page" to pageSize.toString(),
                "offset" to offset.toString(),
                "order" to "asc",
                "orderby" to "date"
        )

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
            this, site, url, params, Array<ProductVariationApiResponse>::class.java
        )
        when (response) {
            is JetpackSuccess -> {
                val variationModels = response.data?.map {
                    it.asProductVariationModel().apply {
                        localSiteId = site.id
                        remoteProductId = productId
                    }
                }.orEmpty()

                val loadedMore = offset > 0
                val canLoadMore = variationModels.size == pageSize
                return RemoteProductVariationsPayload(
                    site, productId, variationModels, offset, loadedMore, canLoadMore
                )
            }
            is JetpackError -> {
                val productError = networkErrorToProductError(response.error)
                return RemoteProductVariationsPayload(
                    productError,
                    site,
                    productId
                )
            }
        }
    }

    /**
     * Makes a GET call to `/wp-json/wc/v3/products/[productId]/variations` via the Jetpack tunnel
     * (see [JetpackTunnelGsonRequest]), retrieving a list of variations for the given WooCommerce [SiteModel]
     * and product.
     *
     * @param [productId] Unique server id of the product
     *
     * but requiring this call to be suspended so the return call be synced within the coroutine job
     *
     */
    suspend fun fetchProductVariationsWithSyncRequest(
        site: SiteModel,
        productId: Long,
        pageSize: Int,
        offset: Int,
        includedVariationIds: List<Long> = emptyList(),
        searchQuery: String? = null,
        excludedVariationIds: List<Long> = emptyList()
    ): WooPayload<List<WCProductVariationModel>> {
        val params = buildVariationParametersMap(
            pageSize,
            offset,
            searchQuery,
            includedVariationIds,
            excludedVariationIds
        )

        return WOOCOMMERCE.products.id(productId).variations.pathV3
            .requestProductVariationTo(site, params)
            .handleResultFrom(site, productId)
    }

    private suspend fun String.requestProductVariationTo(
        site: SiteModel,
        params: Map<String, String>
    ) = jetpackTunnelGsonRequestBuilder.syncGetRequest(
        this@ProductRestClient,
        site,
        this,
        params,
        Array<ProductVariationApiResponse>::class.java
    )

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
                        val newModel = it.asProductModel().apply {
                            localSiteId = site.id
                        }
                        val payload = RemoteUpdateProductPayload(site, newModel)
                        dispatcher.dispatch(WCProductActionBuilder.newUpdatedProductAction(payload))
                    }
                },
                { networkError ->
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
     * Makes a PUT request to `/wp-json/wc/v3/products/remoteProductId` to update a product
     *
     * @param [site] The site to fetch product reviews for
     * @param [storedWCProductVariationModel] the stored model to compare with the [updatedProductVariationModel]
     * @param [updatedProductVariationModel] the product model that contains the update
     */
    suspend fun updateVariation(
        site: SiteModel,
        storedWCProductVariationModel: WCProductVariationModel?,
        updatedProductVariationModel: WCProductVariationModel
    ): RemoteUpdateVariationPayload {
        val remoteProductId = updatedProductVariationModel.remoteProductId
        val remoteVariationId = updatedProductVariationModel.remoteVariationId
        val url = WOOCOMMERCE.products.id(remoteProductId).variations.variation(remoteVariationId).pathV3
        val body = variantModelToProductJsonBody(storedWCProductVariationModel, updatedProductVariationModel)

        val response = jetpackTunnelGsonRequestBuilder.syncPutRequest(
            this, site, url, body, ProductVariationApiResponse::class.java
        )
        return when (response) {
            is JetpackSuccess -> {
                response.data?.let {
                    val newModel = it.asProductVariationModel().apply {
                        this.remoteProductId = remoteProductId
                        localSiteId = site.id
                    }
                    RemoteUpdateVariationPayload(site, newModel)
                } ?: RemoteUpdateVariationPayload(
                    ProductError(GENERIC_ERROR, "Success response with empty data"),
                    site,
                    WCProductVariationModel().apply {
                        this.remoteProductId = remoteProductId
                        this.remoteVariationId = remoteVariationId
                    }
                )
            }
            is JetpackError -> {
                val productError = networkErrorToProductError(response.error)
                RemoteUpdateVariationPayload(
                    productError,
                    site,
                    WCProductVariationModel().apply {
                        this.remoteProductId = remoteProductId
                        this.remoteVariationId = remoteVariationId
                    }
                )
            }
        }
    }

    suspend fun batchUpdateProducts(
        site: SiteModel,
        existingToUpdatedProducts: Map<WCProductModel, WCProductModel>,
    ): WooPayload<BatchProductApiResponse> = WOOCOMMERCE.products.batch.pathV3
        .let { url ->
            val body = buildMap {
                putIfNotNull("update" to existingToUpdatedProducts
                    .mapNotNull { (existing, updated) ->
                        productModelToProductJsonBody(
                            productModel = existing, updatedProductModel = updated
                        ).let { updateProperties ->
                            if (updateProperties.isNotEmpty()) {
                                updateProperties.plus("id" to updated.remoteProductId)
                            } else {
                                null
                            }
                        }
                    }
                    .takeIf { map -> map.isNotEmpty() }
                )
            }

            // No changes need to be executed, no need to call the API
            if (body.isEmpty()) return@let WooPayload()

            jetpackTunnelGsonRequestBuilder.syncPostRequest(
                this@ProductRestClient,
                site,
                url,
                body,
                BatchProductApiResponse::class.java
            ).handleResult()
        }

    /**
     * Makes a POST request to `/wp-json/wc/v3/products/[WCProductModel.remoteProductId]/variations/batch`
     * This API helps you to batch create, update and delete multiple product variations.
     *
     * @param [site] The site containing the product
     * @param productId Id of the product.
     * @param createVariations list of product variations to create.
     * The Map keys correspond to the names of variation properties.
     * The Map values are the variation properties values.
     * @param updateVariations list of product variations to update.
     * The Map keys correspond to the names of variation properties.
     * The Map values are the variation properties values.
     * @param deleteVariations list of product variations ids to delete.
     *
     * @return Instance of [BatchProductVariationsApiResponse].
     */
    suspend fun batchUpdateVariations(
        site: SiteModel,
        productId: Long,
        createVariations: List<Map<String, Any>>? = null,
        updateVariations: List<Map<String, Any>>? = null,
        deleteVariations: List<Long>? = null
    ): WooPayload<BatchProductVariationsApiResponse> =
        WOOCOMMERCE.products.id(productId).variations.batch.pathV3
            .let { url ->
                val body = buildMap {
                    putIfNotNull("create" to createVariations)
                    putIfNotNull("update" to updateVariations)
                    putIfNotNull("delete" to deleteVariations)
                }

                require(body.isNotEmpty())

                jetpackTunnelGsonRequestBuilder.syncPostRequest(
                    this@ProductRestClient,
                    site,
                    url,
                    body,
                    BatchProductVariationsApiResponse::class.java
                ).handleResult()
            }

    /**
     * Makes a POST request to `/wp-json/wc/v3/products` to create
     * a empty variation to a given variable product
     *
     * @param [site] The site containing the product
     * @param [productId] the ID of the variable product to create the empty variation
     */
    suspend fun generateEmptyVariation(
        site: SiteModel,
        productId: Long,
        attributesJson: String
    ) = WOOCOMMERCE.products.id(productId).variations.pathV3
            .let { url ->
                jetpackTunnelGsonRequestBuilder.syncPostRequest(
                        this@ProductRestClient,
                        site,
                        url,
                        mapOf("attributes" to JsonParser().parse(attributesJson).asJsonArray),
                        ProductVariationApiResponse::class.java
                ).handleResult()
            }

    /**
     * Makes a DELETE request to `/wp-json/wc/v3/products/<id>` to delete a product
     *
     * @param [site] The site containing the product
     * @param [productId] the ID of the variable product who holds the variation to be deleted
     * @param [variationId] the ID of the variation model to delete
     *
     * Force delete option is not available as Variation doesn't support trashing
     */
    suspend fun deleteVariation(
        site: SiteModel,
        productId: Long,
        variationId: Long
    ) = WOOCOMMERCE.products.id(productId).variations.variation(variationId).pathV3
            .let { url ->
                jetpackTunnelGsonRequestBuilder.syncDeleteRequest(
                        this@ProductRestClient,
                        site,
                        url,
                        ProductVariationApiResponse::class.java
                ).handleResult()
            }

    /**
     * Makes a PUT request to
     * `/wp-json/wc/v3/products/[WCProductModel.remoteProductId]/variations/[WCProductVariationModel.remoteVariationId]`
     * to replace a variation's attributes with [WCProductVariationModel.attributes]
     *
     * Returns a WooPayload with the Api response as result
     *
     * @param [site] The site to update the given variation attributes
     * @param [attributesJson] Locally updated product variation to be sent
     */

    suspend fun updateVariationAttributes(
        site: SiteModel,
        productId: Long,
        variationId: Long,
        attributesJson: String
    ) = WOOCOMMERCE.products.id(productId).variations.variation(variationId).pathV3
                .let { url ->
                    jetpackTunnelGsonRequestBuilder.syncPutRequest(
                            this@ProductRestClient,
                            site,
                            url,
                            mapOf("attributes" to JsonParser().parse(attributesJson).asJsonArray),
                            ProductVariationApiResponse::class.java
                    ).handleResult()
                }

    /**
     * Makes a PUT request to `/wp-json/wc/v3/products/[WCProductModel.remoteProductId]`
     * to replace a product's attributes with [WCProductModel.attributes]
     *
     * Returns a WooPayload with the Api response as result
     *
     * @param [site] The site to update the given product attributes
     */

    suspend fun updateProductAttributes(
        site: SiteModel,
        productId: Long,
        attributesJson: String
    ) = WOOCOMMERCE.products.id(productId).pathV3
            .let { url ->
                jetpackTunnelGsonRequestBuilder.syncPutRequest(
                        this,
                        site,
                        url,
                        mapOf("attributes" to JsonParser().parse(attributesJson).asJsonArray),
                        ProductApiResponse::class.java
                ).handleResult()
            }

    /**
     * Makes a PUT request to `/wp-json/wc/v3/products/[remoteProductId]` to replace a product's images
     * with the passed media list
     *
     * Dispatches a WCProductAction.UPDATED_PRODUCT_IMAGES action with the result
     *
     * @param [site] The site to update product images for
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
                        val newModel = it.asProductModel().apply {
                            localSiteId = site.id
                        }
                        val payload = RemoteUpdateProductImagesPayload(site, newModel)
                        dispatcher.dispatch(WCProductActionBuilder.newUpdatedProductImagesAction(payload))
                    }
                },
                { networkError ->
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
     * Makes a GET call to `/wc/v3/products/categories` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * retrieving a list of product categories for a given WooCommerce [SiteModel].
     *
     * The number of categories to fetch is defined in [WCProductStore.DEFAULT_PRODUCT_CATEGORY_PAGE_SIZE],
     * and retrieving older categories is done by passing an [offset].
     *
     * Dispatches a [WCProductAction.FETCHED_PRODUCT_CATEGORIES]
     *
     * @param [site] The site to fetch product categories for
     * @param [offset] The offset to use for the fetch
     * @param [productCategorySorting] Optional. The sorting type of the categories
     */
    fun fetchProductCategories(
        site: SiteModel,
        pageSize: Int = DEFAULT_PRODUCT_CATEGORY_PAGE_SIZE,
        offset: Int = 0,
        productCategorySorting: ProductCategorySorting? = DEFAULT_CATEGORY_SORTING
    ) {
        val sortOrder = when (productCategorySorting) {
            NAME_DESC -> "desc"
            else -> "asc"
        }

        val url = WOOCOMMERCE.products.categories.pathV3
        val responseType = object : TypeToken<List<ProductCategoryApiResponse>>() {}.type
        val params = mutableMapOf(
                "per_page" to pageSize.toString(),
                "offset" to offset.toString(),
                "order" to sortOrder,
                "orderby" to "name"
        )
        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params, responseType,
                { response: List<ProductCategoryApiResponse>? ->
                    response?.let {
                        val categories = it.map { category ->
                            category.asProductCategoryModel().apply { localSiteId = site.id }
                        }
                        val canLoadMore = categories.size == pageSize
                        val loadedMore = offset > 0
                        val payload = RemoteProductCategoriesPayload(
                                site, categories, offset, loadedMore, canLoadMore
                        )
                        dispatcher.dispatch(WCProductActionBuilder.newFetchedProductCategoriesAction(payload))
                    }
                },
                { networkError ->
                    val productCategoryError = networkErrorToProductError(networkError)
                    val payload = RemoteProductCategoriesPayload(productCategoryError, site)
                    dispatcher.dispatch(WCProductActionBuilder.newFetchedProductCategoriesAction(payload))
                },
                { request: WPComGsonRequest<*> -> add(request) })
        add(request)
    }

    /**
     * Posts a new Add Category record to the API for a category.
     *
     * Makes a POST call `/wc/v3/products/categories/id` to save a Category record via the Jetpack tunnel.
     * Returns a [WCProductCategoryModel] on successful response.
     *
     * Dispatches [WCProductAction.ADDED_PRODUCT_CATEGORY] action with the results.
     */
    fun addProductCategory(
        site: SiteModel,
        category: WCProductCategoryModel
    ) {
        val url = WOOCOMMERCE.products.categories.pathV3

        val responseType = object : TypeToken<ProductCategoryApiResponse>() {}.type
        val params = mutableMapOf(
                "name" to category.name,
                "parent" to category.parent.toString()
        )
        val request = JetpackTunnelGsonRequest.buildPostRequest(url, site.siteId, params, responseType,
                { response: ProductCategoryApiResponse? ->
                    val categoryResponse = response?.let {
                        it.asProductCategoryModel().apply {
                            localSiteId = site.id
                        }
                    }
                    val payload = RemoteAddProductCategoryResponsePayload(site, categoryResponse)
                    dispatcher.dispatch(WCProductActionBuilder.newAddedProductCategoryAction(payload))
                },
                { networkError ->
                    val productCategorySaveError = networkErrorToProductError(networkError)
                    val payload = RemoteAddProductCategoryResponsePayload(productCategorySaveError, site, category)
                    dispatcher.dispatch(WCProductActionBuilder.newAddedProductCategoryAction(payload))
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
     * @param [site] The site to fetch product reviews for
     * @param [offset] The offset to use for the fetch
     * @param [reviewIds] Optional. A list of remote product review ID's to fetch
     * @param [productIds] Optional. A list of remote product ID's to fetch product reviews for
     * @param [filterByStatus] Optional. A list of product review statuses to fetch
     */
    suspend fun fetchProductReviews(
        site: SiteModel,
        offset: Int,
        reviewIds: List<Long>? = null,
        productIds: List<Long>? = null,
        filterByStatus: List<String>? = null
    ): FetchProductReviewsResponsePayload {
        val statusFilter = filterByStatus?.joinToString { it } ?: "all"

        val url = WOOCOMMERCE.products.reviews.pathV3
        val params = mutableMapOf(
                "per_page" to WCProductStore.NUM_REVIEWS_PER_FETCH.toString(),
                "offset" to offset.toString(),
                "status" to statusFilter
        )
        reviewIds?.let { ids ->
            params.put("include", ids.map { it }.joinToString())
        }
        productIds?.let { ids ->
            params.put("product", ids.map { it }.joinToString())
        }

        val response = wooNetwork.executeGetGsonRequest(
                site = site,
                path = url,
                clazz = Array<ProductReviewApiResponse>::class.java,
                params = params
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                val productData = response.data
                if (productData != null) {
                    val reviews = productData.map { review ->
                        productReviewResponseToProductReviewModel(review).apply { localSiteId = site.id }
                    }
                    FetchProductReviewsResponsePayload(
                        site,
                        reviews,
                        productIds,
                        filterByStatus,
                        offset > 0,
                        reviews.size == WCProductStore.NUM_REVIEWS_PER_FETCH
                    )
                } else {
                    FetchProductReviewsResponsePayload(
                        ProductError(
                            GENERIC_ERROR,
                            "Success response with empty data"
                        ),
                        site
                    )
                }
            }
            is WPAPIResponse.Error -> {
                FetchProductReviewsResponsePayload(
                    wpAPINetworkErrorToProductError(response.error),
                    site
                )
            }
        }
    }

    /**
     * Makes a GET call to `/wc/v3/products/reviews/<id>` via the Jetpack tunnel (see [JetpackTunnelGsonRequestBuilder])
     * retrieving a product review by it's remote ID for a given WooCommerce [SiteModel].
     *
     *
     * @param [site] The site to fetch product reviews for
     * @param [remoteReviewId] The remote id of the review to fetch
     */
    suspend fun fetchProductReviewById(site: SiteModel, remoteReviewId: Long): RemoteProductReviewPayload {
        val url = WOOCOMMERCE.products.reviews.id(remoteReviewId).pathV3
        val response = wooNetwork.executeGetGsonRequest(
                site = site,
                path = url,
                clazz = ProductReviewApiResponse::class.java
        )

        return when (response) {
            is WPAPIResponse.Success  -> {
                response.data?.let {
                    val review = productReviewResponseToProductReviewModel(it).apply {
                        localSiteId = site.id
                    }
                    RemoteProductReviewPayload(site, review)
                } ?: RemoteProductReviewPayload(
                        error = ProductError(GENERIC_ERROR, "Success response with empty data"),
                        site = site
                )
            }
            is WPAPIResponse.Error  -> {
                val productReviewError = wpAPINetworkErrorToProductError(response.error)
                RemoteProductReviewPayload(error = productReviewError, site = site)
            }
        }
    }

    /**
     * Makes a PUT call to `/wc/v3/products/reviews/<id>` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * updating the status for the given product review to [newStatus].
     *
     *
     * @param [site] The site to fetch product reviews for
     * @param [remoteReviewId] The remote ID of the product review to be updated
     * @param [newStatus] The new status to update the product review to
     *
     * @return [WooPayload] with the updated [WCProductReviewModel]
     */
    suspend fun updateProductReviewStatus(
        site: SiteModel,
        remoteReviewId: Long,
        newStatus: String
    ): WooPayload<WCProductReviewModel> {
        val url = WOOCOMMERCE.products.reviews.id(remoteReviewId).pathV3
        val params = mapOf("status" to newStatus)
        val response = wooNetwork.executePutGsonRequest(
                site = site,
                path = url,
                clazz = ProductReviewApiResponse::class.java,
                body = params
        )

        return when (response) {
            is WPAPIResponse.Success  -> {
                response.data?.let {
                    val review = productReviewResponseToProductReviewModel(it).apply {
                        localSiteId = site.id
                    }
                    WooPayload(review)
                } ?: WooPayload(
                    error = WooError(
                        type = WooErrorType.GENERIC_ERROR,
                        original = GenericErrorType.UNKNOWN,
                        message = "Success response with empty data"
                    )
                )
            }
            is WPAPIResponse.Error -> WooPayload(error = response.error.toWooError())
        }
    }

    /**
     * Makes a POST request to `/wp-json/wc/v3/products` to add a product
     *
     * Dispatches a [WCProductAction.ADDED_PRODUCT] action with the result
     *
     * @param [site] The site to fetch product reviews for
     * @param [productModel] the new product model
     */
    fun addProduct(
        site: SiteModel,
        productModel: WCProductModel
    ) {
        val url = WOOCOMMERCE.products.pathV3
        val responseType = object : TypeToken<ProductApiResponse>() {}.type
        val params = productModelToProductJsonBody(null, productModel)

        val request = JetpackTunnelGsonRequest.buildPostRequest(
                wpApiEndpoint = url,
                siteId = site.siteId,
                body = params,
                type = responseType,
                listener = { response: ProductApiResponse? ->
                    // success
                    response?.let { product ->
                        val newModel = product.asProductModel().apply {
                            id = product.id?.toInt() ?: 0
                            localSiteId = site.id
                        }
                        val payload = RemoteAddProductPayload(site, newModel)
                        dispatcher.dispatch(WCProductActionBuilder.newAddedProductAction(payload))
                    }
                },
                errorListener = { networkError ->
                    // error
                    val productError = networkErrorToProductError(networkError)
                    val payload = RemoteAddProductPayload(
                            productError,
                            site,
                            WCProductModel()
                    )
                    dispatcher.dispatch(WCProductActionBuilder.newAddedProductAction(payload))
                }
        )
        add(request)
    }

    /**
     * Makes a DELETE request to `/wp-json/wc/v3/products/<id>` to delete a product
     *
     * Dispatches a [WCProductAction.DELETED_PRODUCT] action with the result
     *
     * @param [site] The site containing the product
     * @param [remoteProductId] the ID of the product model to delete
     * @param [forceDelete] whether to permanently delete the product (will be sent to trash if false)
     */
    fun deleteProduct(
        site: SiteModel,
        remoteProductId: Long,
        forceDelete: Boolean = false
    ) {
        val url = WOOCOMMERCE.products.id(remoteProductId).pathV3
        val responseType = object : TypeToken<ProductApiResponse>() {}.type
        val params = mapOf("force" to forceDelete.toString())
        val request = JetpackTunnelGsonRequest.buildDeleteRequest(url, site.siteId, params, responseType,
                { response: ProductApiResponse? ->
                    response?.let {
                        val payload = RemoteDeleteProductPayload(site, remoteProductId)
                        dispatcher.dispatch(WCProductActionBuilder.newDeletedProductAction(payload))
                    }
                },
                { networkError ->
                    val productError = networkErrorToProductError(networkError)
                    val payload = RemoteDeleteProductPayload(
                            productError,
                            site,
                            remoteProductId
                    )
                    dispatcher.dispatch(WCProductActionBuilder.newDeletedProductAction(payload))
                }
        )
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
    @Suppress("LongMethod", "ComplexMethod", "SwallowedException", "TooGenericExceptionCaught")
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
        if (storedWCProductModel.type != updatedProductModel.type) {
            body["type"] = updatedProductModel.type
        }
        if (storedWCProductModel.sku != updatedProductModel.sku) {
            body["sku"] = updatedProductModel.sku
        }
        if (storedWCProductModel.status != updatedProductModel.status) {
            body["status"] = updatedProductModel.status
        }
        if (storedWCProductModel.catalogVisibility != updatedProductModel.catalogVisibility) {
            body["catalog_visibility"] = updatedProductModel.catalogVisibility
        }
        if (storedWCProductModel.slug != updatedProductModel.slug) {
            body["slug"] = updatedProductModel.slug
        }
        if (storedWCProductModel.featured != updatedProductModel.featured) {
            body["featured"] = updatedProductModel.featured
        }
        if (storedWCProductModel.manageStock != updatedProductModel.manageStock) {
            body["manage_stock"] = updatedProductModel.manageStock
        }
        if (storedWCProductModel.externalUrl != updatedProductModel.externalUrl) {
            body["external_url"] = updatedProductModel.externalUrl
        }
        if (storedWCProductModel.buttonText != updatedProductModel.buttonText) {
            body["button_text"] = updatedProductModel.buttonText
        }

        // only allowed to change the following params if manageStock is enabled
        if (updatedProductModel.manageStock) {
            if (storedWCProductModel.stockQuantity != updatedProductModel.stockQuantity) {
                // Conversion/rounding down because core API only accepts Int value for stock quantity.
                // On the app side, make sure it only allows whole decimal quantity when updating, so that
                // there's no undesirable conversion effect.
                body["stock_quantity"] = updatedProductModel.stockQuantity.toInt()
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
        if (!storedWCProductModel.hasSameImages(updatedProductModel)) {
            val updatedImages = updatedProductModel.getImageListOrEmpty()
            body["images"] = JsonArray().also {
                for (image in updatedImages) {
                    it.add(image.toJson())
                }
            }
        }
        if (storedWCProductModel.reviewsAllowed != updatedProductModel.reviewsAllowed) {
            body["reviews_allowed"] = updatedProductModel.reviewsAllowed
        }
        if (storedWCProductModel.virtual != updatedProductModel.virtual) {
            body["virtual"] = updatedProductModel.virtual
        }
        if (storedWCProductModel.purchaseNote != updatedProductModel.purchaseNote) {
            body["purchase_note"] = updatedProductModel.purchaseNote
        }
        if (storedWCProductModel.menuOrder != updatedProductModel.menuOrder) {
            body["menu_order"] = updatedProductModel.menuOrder
        }
        if (!storedWCProductModel.hasSameCategories(updatedProductModel)) {
            val updatedCategories = updatedProductModel.getCategoryList()
            body["categories"] = JsonArray().also {
                for (category in updatedCategories) {
                    it.add(category.toJson())
                }
            }
        }
        if (!storedWCProductModel.hasSameTags(updatedProductModel)) {
            val updatedTags = updatedProductModel.getTagList()
            body["tags"] = JsonArray().also {
                for (tag in updatedTags) {
                    it.add(tag.toJson())
                }
            }
        }
        if (storedWCProductModel.groupedProductIds != updatedProductModel.groupedProductIds) {
            body["grouped_products"] = updatedProductModel.getGroupedProductIdList()
        }
        if (storedWCProductModel.crossSellIds != updatedProductModel.crossSellIds) {
            body["cross_sell_ids"] = updatedProductModel.getCrossSellProductIdList()
        }
        if (storedWCProductModel.upsellIds != updatedProductModel.upsellIds) {
            body["upsell_ids"] = updatedProductModel.getUpsellProductIdList()
        }
        if (storedWCProductModel.downloadable != updatedProductModel.downloadable) {
            body["downloadable"] = updatedProductModel.downloadable
        }
        if (storedWCProductModel.downloadLimit != updatedProductModel.downloadLimit) {
            body["download_limit"] = updatedProductModel.downloadLimit
        }
        if (storedWCProductModel.downloadExpiry != updatedProductModel.downloadExpiry) {
            body["download_expiry"] = updatedProductModel.downloadExpiry
        }
        if (!storedWCProductModel.hasSameDownloadableFiles(updatedProductModel)) {
            val updatedFiles = updatedProductModel.getDownloadableFiles()
            body["downloads"] = JsonArray().apply {
                updatedFiles.forEach { file ->
                    add(file.toJson())
                }
            }
        }
        if (!storedWCProductModel.hasSameAttributes(updatedProductModel)) {
            JsonParser().apply {
                body["attributes"] = try {
                    parse(updatedProductModel.attributes).asJsonArray
                } catch (ex: Exception) {
                    JsonArray()
                }
            }
        }

        return body
    }

    /**
     * Build json body of product items to be updated to the backend.
     *
     * This method checks if there is a cached version of the product stored locally.
     * If not, it generates a new product model for the same product ID, with default fields
     * and verifies that the [updatedVariationModel] has fields that are different from the default
     * fields of [variationModel]. This is to ensure that we do not update product fields that do not contain any
     * changes.
     */
    @Suppress("ForbiddenComment", "LongMethod", "ComplexMethod", "SwallowedException", "TooGenericExceptionCaught")
    private fun variantModelToProductJsonBody(
        variationModel: WCProductVariationModel?,
        updatedVariationModel: WCProductVariationModel
    ): HashMap<String, Any> {
        val body = HashMap<String, Any>()

        val storedVariationModel = variationModel ?: WCProductVariationModel().apply {
            remoteProductId = updatedVariationModel.remoteProductId
            remoteVariationId = updatedVariationModel.remoteVariationId
        }
        if (storedVariationModel.description != updatedVariationModel.description) {
            body["description"] = updatedVariationModel.description
        }
        if (storedVariationModel.sku != updatedVariationModel.sku) {
            body["sku"] = updatedVariationModel.sku
        }
        if (storedVariationModel.status != updatedVariationModel.status) {
            body["status"] = updatedVariationModel.status
        }
        if (storedVariationModel.manageStock != updatedVariationModel.manageStock) {
            body["manage_stock"] = updatedVariationModel.manageStock
        }

        // only allowed to change the following params if manageStock is enabled
        if (updatedVariationModel.manageStock) {
            if (storedVariationModel.stockQuantity != updatedVariationModel.stockQuantity) {
                // Conversion/rounding down because core API only accepts Int value for stock quantity.
                // On the app side, make sure it only allows whole decimal quantity when updating, so that
                // there's no undesirable conversion effect.
                body["stock_quantity"] = updatedVariationModel.stockQuantity.toInt()
            }
            if (storedVariationModel.backorders != updatedVariationModel.backorders) {
                body["backorders"] = updatedVariationModel.backorders
            }
        }
        if (storedVariationModel.stockStatus != updatedVariationModel.stockStatus) {
            body["stock_status"] = updatedVariationModel.stockStatus
        }
        if (storedVariationModel.regularPrice != updatedVariationModel.regularPrice) {
            body["regular_price"] = updatedVariationModel.regularPrice
        }
        if (storedVariationModel.salePrice != updatedVariationModel.salePrice) {
            body["sale_price"] = updatedVariationModel.salePrice
        }
        if (storedVariationModel.dateOnSaleFromGmt != updatedVariationModel.dateOnSaleFromGmt) {
            body["date_on_sale_from_gmt"] = updatedVariationModel.dateOnSaleFromGmt
        }
        if (storedVariationModel.dateOnSaleToGmt != updatedVariationModel.dateOnSaleToGmt) {
            body["date_on_sale_to_gmt"] = updatedVariationModel.dateOnSaleToGmt
        }
        if (storedVariationModel.taxStatus != updatedVariationModel.taxStatus) {
            body["tax_status"] = updatedVariationModel.taxStatus
        }
        if (storedVariationModel.taxClass != updatedVariationModel.taxClass) {
            body["tax_class"] = updatedVariationModel.taxClass
        }
        if (storedVariationModel.weight != updatedVariationModel.weight) {
            body["weight"] = updatedVariationModel.weight
        }

        val dimensionsBody = mutableMapOf<String, String>()
        if (storedVariationModel.height != updatedVariationModel.height) {
            dimensionsBody["height"] = updatedVariationModel.height
        }
        if (storedVariationModel.width != updatedVariationModel.width) {
            dimensionsBody["width"] = updatedVariationModel.width
        }
        if (storedVariationModel.length != updatedVariationModel.length) {
            dimensionsBody["length"] = updatedVariationModel.length
        }
        if (dimensionsBody.isNotEmpty()) {
            body["dimensions"] = dimensionsBody
        }
        if (storedVariationModel.shippingClass != updatedVariationModel.shippingClass) {
            body["shipping_class"] = updatedVariationModel.shippingClass
        }
        // TODO: Once removal is supported, we can remove the extra isNotBlank() condition
        if (storedVariationModel.image != updatedVariationModel.image && updatedVariationModel.image.isNotBlank()) {
            body["image"] = updatedVariationModel.getImageModel()?.toJson() ?: ""
        }
        if (storedVariationModel.menuOrder != updatedVariationModel.menuOrder) {
            body["menu_order"] = updatedVariationModel.menuOrder
        }
        if (storedVariationModel.attributes != updatedVariationModel.attributes) {
            JsonParser().apply {
                body["attributes"] = try {
                    parse(updatedVariationModel.attributes).asJsonArray
                } catch (ex: Exception) {
                    JsonArray()
                }
            }
        }
        return body
    }

    private fun productTagApiResponseToProductTagModel(
        response: ProductTagApiResponse,
        site: SiteModel
    ): WCProductTagModel {
        return WCProductTagModel().apply {
            remoteTagId = response.id
            localSiteId = site.id
            name = response.name ?: ""
            slug = response.slug ?: ""
            description = response.description ?: ""
            count = response.count
        }
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
            "woocommerce_rest_product_invalid_id" -> ProductErrorType.INVALID_PRODUCT_ID
            "rest_invalid_param" -> ProductErrorType.INVALID_PARAM
            "woocommerce_rest_review_invalid_id" -> ProductErrorType.INVALID_REVIEW_ID
            "woocommerce_product_invalid_image_id" -> ProductErrorType.INVALID_IMAGE_ID
            "product_invalid_sku" -> ProductErrorType.DUPLICATE_SKU
            "term_exists" -> ProductErrorType.TERM_EXISTS
            "woocommerce_variation_invalid_image_id" -> ProductErrorType.INVALID_VARIATION_IMAGE_ID
            else -> ProductErrorType.fromString(wpComError.apiError)
        }
        return ProductError(productErrorType, wpComError.message)
    }

    private fun wpAPINetworkErrorToProductError(wpAPINetworkError: WPAPINetworkError): ProductError {
        val productErrorType = when (wpAPINetworkError.errorCode) {
            "woocommerce_rest_product_invalid_id" -> ProductErrorType.INVALID_PRODUCT_ID
            "rest_invalid_param" -> ProductErrorType.INVALID_PARAM
            "woocommerce_rest_review_invalid_id" -> ProductErrorType.INVALID_REVIEW_ID
            "woocommerce_product_invalid_image_id" -> ProductErrorType.INVALID_IMAGE_ID
            "product_invalid_sku" -> ProductErrorType.DUPLICATE_SKU
            "term_exists" -> ProductErrorType.TERM_EXISTS
            "woocommerce_variation_invalid_image_id" -> ProductErrorType.INVALID_VARIATION_IMAGE_ID
            else -> ProductErrorType.fromString(wpAPINetworkError.errorCode.orEmpty())
        }
        return ProductError(productErrorType, wpAPINetworkError.message)
    }
}
