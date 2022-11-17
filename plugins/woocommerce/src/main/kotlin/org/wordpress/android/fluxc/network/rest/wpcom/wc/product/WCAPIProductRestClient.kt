package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wcapi.BaseWCAPIRestClient
import org.wordpress.android.fluxc.network.rest.wcapi.WCAPIGsonRequest
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.utils.putIfNotEmpty
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class WCAPIProductRestClient @Inject constructor(
        private val dispatcher: Dispatcher,
        @Named("regular") requestQueue: RequestQueue,
        userAgent: UserAgent
) : BaseWCAPIRestClient(dispatcher, requestQueue, userAgent) {

    fun addProduct(
            site: SiteModel,
            productModel: WCProductModel
    ) {
        val url = site.url + "/wp-json" + WOOCOMMERCE.products.pathV3
        val body = productModelToProductJsonBody(null, productModel)

        val responseType = object : TypeToken<ProductApiResponse>() {}.type

        val request = WCAPIGsonRequest(
                method = Request.Method.POST,
                url = url,
                params = emptyMap(),
                body = body,
                type = responseType,
                listener = { response: ProductApiResponse? ->
                    // success
                    response?.let { product ->
                        val newModel = product.asProductModel().apply {
                            id = product.id?.toInt() ?: 0
                            localSiteId = site.id
                        }
                        val payload = WCProductStore.RemoteAddProductPayload(site, newModel)
                        dispatcher.dispatch(WCProductActionBuilder.newAddedProductAction(payload))
                    }
                },
                errorListener = { networkError ->
                    // error
                    val productError = WCProductStore.ProductError(
                            WCProductStore.ProductErrorType.GENERIC_ERROR,
                            networkError.message
                    )
                    val payload = WCProductStore.RemoteAddProductPayload(
                            productError,
                            site,
                            WCProductModel()
                    )
                    dispatcher.dispatch(WCProductActionBuilder.newAddedProductAction(payload))
                }
        )

        request.addHeader("Authorization", "Basic $AUTH_KEY")
        add(request)

    }

    fun updateProduct(
            site: SiteModel,
            storedWCProductModel: WCProductModel?,
            updatedProductModel: WCProductModel
    ) {
        val remoteProductId = updatedProductModel.remoteProductId
        val url = site.url + "/wp-json" + WOOCOMMERCE.products.id(remoteProductId).pathV3
        val responseType = object : TypeToken<ProductApiResponse>() {}.type
        val body = productModelToProductJsonBody(storedWCProductModel, updatedProductModel)

        val request = WCAPIGsonRequest(
                method = Request.Method.PUT,
                url = url,
                params = emptyMap(),
                body = body,
                type = responseType,
                listener = { response: ProductApiResponse? ->
                    // success
                    response?.let { product ->
                        val newModel = product.asProductModel().apply {
                            localSiteId = site.id
                        }
                        val payload = WCProductStore.RemoteUpdateProductPayload(site, newModel)
                        dispatcher.dispatch(WCProductActionBuilder.newUpdatedProductAction(payload))
                    }
                },
                errorListener = { networkError ->
                    // error
                    val productError = WCProductStore.ProductError(
                            WCProductStore.ProductErrorType.GENERIC_ERROR,
                            networkError.message
                    )
                    val payload = WCProductStore.RemoteUpdateProductPayload(
                            productError,
                            site,
                            WCProductModel().apply { this.remoteProductId = remoteProductId }
                    )
                    dispatcher.dispatch(WCProductActionBuilder.newUpdatedProductAction(payload))
                }
        )

        request.addHeader("Authorization", "Basic $AUTH_KEY")
        add(request)
    }

    fun fetchProducts(
            site: SiteModel,
            pageSize: Int = WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE,
            offset: Int = 0,
            sortType: WCProductStore.ProductSorting = WCProductStore.DEFAULT_PRODUCT_SORTING,
            searchQuery: String? = null,
            isSkuSearch: Boolean = false,
            includedProductIds: List<Long>? = null,
            filterOptions: Map<WCProductStore.ProductFilterOption, String>? = null,
            excludedProductIds: List<Long>? = null
    ) {
        val url = site.url + "/wp-json" + WOOCOMMERCE.products.pathV3
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

        val request = WCAPIGsonRequest(
                method = Request.Method.GET,
                url = url,
                params = params,
                body = emptyMap(),
                type = responseType,
                listener = { response: List<ProductApiResponse>? ->
                    // success
                    val productModels = response?.map {
                        it.asProductModel().apply { localSiteId = site.id }
                    }.orEmpty()

                    val loadedMore = offset > 0
                    val canLoadMore = productModels.size == pageSize
                    if (searchQuery == null) {
                        val payload = WCProductStore.RemoteProductListPayload(
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
                        val payload = WCProductStore.RemoteSearchProductsPayload(
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
                errorListener = { networkError ->
                    // error
                    val productError = WCProductStore.ProductError(
                            WCProductStore.ProductErrorType.GENERIC_ERROR,
                            networkError.message
                    )
                    if (searchQuery == null) {
                        val payload = WCProductStore.RemoteProductListPayload(productError, site)
                        dispatcher.dispatch(WCProductActionBuilder.newFetchedProductsAction(payload))
                    } else {
                        val payload = WCProductStore.RemoteSearchProductsPayload(
                                error = productError,
                                site = site,
                                query = searchQuery,
                                skuSearch = isSkuSearch,
                                filterOptions = filterOptions
                        )
                        dispatcher.dispatch(WCProductActionBuilder.newSearchedProductsAction(payload))
                    }
                }
        )

        request.addHeader("Authorization", "Basic $AUTH_KEY")
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

    private fun buildProductParametersMap(
            pageSize: Int,
            sortType: WCProductStore.ProductSorting,
            offset: Int,
            searchQuery: String?,
            isSkuSearch: Boolean,
            includedProductIds: List<Long>? = null,
            excludedProductIds: List<Long>? = null,
            filterOptions: Map<WCProductStore.ProductFilterOption, String>? = null
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

    private fun WCProductStore.ProductSorting.asOrderByParameter() = when (this) {
        WCProductStore.ProductSorting.TITLE_ASC, WCProductStore.ProductSorting.TITLE_DESC -> "title"
        WCProductStore.ProductSorting.DATE_ASC, WCProductStore.ProductSorting.DATE_DESC -> "date"
    }

    private fun WCProductStore.ProductSorting.asSortOrderParameter() = when (this) {
        WCProductStore.ProductSorting.TITLE_ASC, WCProductStore.ProductSorting.DATE_ASC -> "asc"
        WCProductStore.ProductSorting.TITLE_DESC, WCProductStore.ProductSorting.DATE_DESC -> "desc"
    }
}
