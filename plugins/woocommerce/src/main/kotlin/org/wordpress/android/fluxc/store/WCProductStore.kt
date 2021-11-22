package org.wordpress.android.fluxc.store

import com.google.gson.Gson
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.WCProductAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.domain.Addon
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.model.WCProductImageModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.model.WCProductShippingClassModel
import org.wordpress.android.fluxc.model.WCProductTagModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.model.WCProductVariationModel.ProductVariantOption
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.mappers.MappingRemoteException
import org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.mappers.RemoteAddonMapper
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.persistence.ProductSqlUtils.deleteVariationsForProduct
import org.wordpress.android.fluxc.persistence.ProductSqlUtils.insertOrUpdateProductVariation
import org.wordpress.android.fluxc.persistence.dao.AddonsDao
import org.wordpress.android.fluxc.store.WCProductStore.ProductCategorySorting.NAME_ASC
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.TITLE_ASC
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.AppLog.T.API
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCProductStore @Inject constructor(
    dispatcher: Dispatcher,
    private val wcProductRestClient: ProductRestClient,
    private val coroutineEngine: CoroutineEngine,
    private val addonsDao: AddonsDao,
    private val logger: AppLogWrapper
) : Store(dispatcher) {
    companion object {
        const val NUM_REVIEWS_PER_FETCH = 25
        const val DEFAULT_PRODUCT_PAGE_SIZE = 25
        const val DEFAULT_PRODUCT_CATEGORY_PAGE_SIZE = 100
        const val DEFAULT_PRODUCT_VARIATIONS_PAGE_SIZE = 25
        const val DEFAULT_PRODUCT_SHIPPING_CLASS_PAGE_SIZE = 25
        const val DEFAULT_PRODUCT_TAGS_PAGE_SIZE = 100
        val DEFAULT_PRODUCT_SORTING = TITLE_ASC
        val DEFAULT_CATEGORY_SORTING = NAME_ASC
    }

    /**
     * Defines the filter options currently supported in the app
     */
    enum class ProductFilterOption {
        STOCK_STATUS, STATUS, TYPE, CATEGORY;

        override fun toString() = name.toLowerCase(Locale.US)
    }

    class FetchProductSkuAvailabilityPayload(
        var site: SiteModel,
        var sku: String
    ) : Payload<BaseNetworkError>()

    class FetchSingleProductPayload(
        var site: SiteModel,
        var remoteProductId: Long
    ) : Payload<BaseNetworkError>()

    class FetchSingleVariationPayload(
        var site: SiteModel,
        var remoteProductId: Long,
        var remoteVariationId: Long
    ) : Payload<BaseNetworkError>()

    class FetchProductsPayload(
        var site: SiteModel,
        var pageSize: Int = DEFAULT_PRODUCT_PAGE_SIZE,
        var offset: Int = 0,
        var sorting: ProductSorting = DEFAULT_PRODUCT_SORTING,
        var remoteProductIds: List<Long>? = null,
        var filterOptions: Map<ProductFilterOption, String>? = null,
        var excludedProductIds: List<Long>? = null
    ) : Payload<BaseNetworkError>()

    class SearchProductsPayload(
        var site: SiteModel,
        var searchQuery: String,
        var pageSize: Int = DEFAULT_PRODUCT_PAGE_SIZE,
        var offset: Int = 0,
        var sorting: ProductSorting = DEFAULT_PRODUCT_SORTING,
        var excludedProductIds: List<Long>? = null
    ) : Payload<BaseNetworkError>()

    class FetchProductVariationsPayload(
        var site: SiteModel,
        var remoteProductId: Long,
        var pageSize: Int = DEFAULT_PRODUCT_VARIATIONS_PAGE_SIZE,
        var offset: Int = 0
    ) : Payload<BaseNetworkError>()

    class FetchProductShippingClassListPayload(
        var site: SiteModel,
        var pageSize: Int = DEFAULT_PRODUCT_SHIPPING_CLASS_PAGE_SIZE,
        var offset: Int = 0
    ) : Payload<BaseNetworkError>()

    class FetchSingleProductShippingClassPayload(
        var site: SiteModel,
        var remoteShippingClassId: Long
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

    class FetchProductPasswordPayload(
        var site: SiteModel,
        var remoteProductId: Long
    ) : Payload<BaseNetworkError>()

    class UpdateProductPasswordPayload(
        var site: SiteModel,
        var remoteProductId: Long,
        var password: String
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

    class UpdateVariationPayload(
        var site: SiteModel,
        val variation: WCProductVariationModel
    ) : Payload<BaseNetworkError>()

    class FetchProductCategoriesPayload(
        var site: SiteModel,
        var pageSize: Int = DEFAULT_PRODUCT_CATEGORY_PAGE_SIZE,
        var offset: Int = 0,
        var productCategorySorting: ProductCategorySorting = DEFAULT_CATEGORY_SORTING
    ) : Payload<BaseNetworkError>()

    class AddProductCategoryPayload(
        val site: SiteModel,
        val category: WCProductCategoryModel
    ) : Payload<BaseNetworkError>()

    class FetchProductTagsPayload(
        var site: SiteModel,
        var pageSize: Int = DEFAULT_PRODUCT_TAGS_PAGE_SIZE,
        var offset: Int = 0,
        var searchQuery: String? = null
    ) : Payload<BaseNetworkError>()

    class AddProductTagsPayload(
        val site: SiteModel,
        val tags: List<String>
    ) : Payload<BaseNetworkError>()

    class AddProductPayload(
        var site: SiteModel,
        val product: WCProductModel
    ) : Payload<BaseNetworkError>()

    class DeleteProductPayload(
        var site: SiteModel,
        val remoteProductId: Long,
        val forceDelete: Boolean = false
    ) : Payload<BaseNetworkError>()

    enum class ProductErrorType {
        INVALID_PRODUCT_ID,
        INVALID_PARAM,
        INVALID_REVIEW_ID,
        INVALID_IMAGE_ID,
        DUPLICATE_SKU,

        // indicates duplicate term name. Currently only used when adding product categories
        TERM_EXISTS,

        // Happens if a store is running Woo 4.6 and below and tries to delete the product image
        // from a variation. See this PR for more detail:
        // https://github.com/woocommerce/woocommerce/pull/27299
        INVALID_VARIATION_IMAGE_ID,

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

    enum class ProductCategorySorting {
        NAME_ASC,
        NAME_DESC
    }

    class RemoteProductSkuAvailabilityPayload(
        val site: SiteModel,
        var sku: String,
        val available: Boolean
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel,
            sku: String,
            available: Boolean
        ) : this(site, sku, available) {
            this.error = error
        }
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

    class RemoteVariationPayload(
        val variation: WCProductVariationModel,
        val site: SiteModel
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            variation: WCProductVariationModel,
            site: SiteModel
        ) : this(variation, site) {
            this.error = error
        }
    }

    class RemoteProductPasswordPayload(
        val remoteProductId: Long,
        val site: SiteModel,
        val password: String
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            remoteProductId: Long,
            site: SiteModel,
            password: String
        ) : this(remoteProductId, site, password) {
            this.error = error
        }
    }

    class RemoteUpdatedProductPasswordPayload(
        val remoteProductId: Long,
        val site: SiteModel,
        val password: String
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            remoteProductId: Long,
            site: SiteModel,
            password: String
        ) : this(remoteProductId, site, password) {
            this.error = error
        }
    }

    class RemoteProductListPayload(
        val site: SiteModel,
        val products: List<WCProductModel> = emptyList(),
        var offset: Int = 0,
        var loadedMore: Boolean = false,
        var canLoadMore: Boolean = false,
        val remoteProductIds: List<Long>? = null,
        val excludedProductIds: List<Long>? = null
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

    class RemoteUpdateVariationPayload(
        var site: SiteModel,
        val variation: WCProductVariationModel
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel,
            variation: WCProductVariationModel
        ) : this(site, variation) {
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

    class RemoteProductShippingClassListPayload(
        val site: SiteModel,
        val shippingClassList: List<WCProductShippingClassModel> = emptyList(),
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

    class RemoteProductShippingClassPayload(
        val productShippingClassModel: WCProductShippingClassModel,
        val site: SiteModel
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            productShippingClassModel: WCProductShippingClassModel,
            site: SiteModel
        ) : this(productShippingClassModel, site) {
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
        constructor(error: ProductError, site: SiteModel) : this(site) {
            this.error = error
        }
    }

    class RemoteProductCategoriesPayload(
        val site: SiteModel,
        val categories: List<WCProductCategoryModel> = emptyList(),
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

    class RemoteAddProductCategoryResponsePayload(
        val site: SiteModel,
        val category: WCProductCategoryModel?
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel,
            category: WCProductCategoryModel?
        ) : this(site, category) {
            this.error = error
        }
    }

    class RemoteProductTagsPayload(
        val site: SiteModel,
        val tags: List<WCProductTagModel> = emptyList(),
        var offset: Int = 0,
        var loadedMore: Boolean = false,
        var canLoadMore: Boolean = false,
        var searchQuery: String? = null
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel
        ) : this(site) {
            this.error = error
        }
    }

    class RemoteAddProductTagsResponsePayload(
        val site: SiteModel,
        val tags: List<WCProductTagModel> = emptyList()
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel,
            addedTags: List<WCProductTagModel> = emptyList()
        ) : this(site, addedTags) {
            this.error = error
        }
    }

    class RemoteAddProductPayload(
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

    class RemoteDeleteProductPayload(
        var site: SiteModel,
        val remoteProductId: Long
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel,
            remoteProductId: Long
        ) : this(site, remoteProductId) {
            this.error = error
        }
    }

    // OnChanged events
    class OnProductChanged(
        var rowsAffected: Int,
        var remoteProductId: Long = 0L, // only set for fetching or deleting a single product
        var canLoadMore: Boolean = false
    ) : OnChanged<ProductError>() {
        var causeOfChange: WCProductAction? = null
    }

    class OnVariationChanged(
        var rowsAffected: Int,
        var remoteProductId: Long = 0L, // only set for fetching a single variation
        var remoteVariationId: Long = 0L, // only set for fetching a single variation
        var canLoadMore: Boolean = false
    ) : OnChanged<ProductError>() {
        var causeOfChange: WCProductAction? = null
    }

    class OnProductSkuAvailabilityChanged(
        var sku: String,
        var available: Boolean
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

    class OnProductShippingClassesChanged(
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

    class OnProductPasswordChanged(
        var remoteProductId: Long,
        var password: String?
    ) : OnChanged<ProductError>() {
        var causeOfChange: WCProductAction? = null
    }

    class OnProductUpdated(
        var rowsAffected: Int,
        var remoteProductId: Long
    ) : OnChanged<ProductError>() {
        var causeOfChange: WCProductAction? = null
    }

    class OnVariationUpdated(
        var rowsAffected: Int,
        var remoteProductId: Long,
        var remoteVariationId: Long
    ) : OnChanged<ProductError>() {
        var causeOfChange: WCProductAction? = null
    }

    class OnProductCategoryChanged(
        var rowsAffected: Int,
        var canLoadMore: Boolean = false
    ) : OnChanged<ProductError>() {
        var causeOfChange: WCProductAction? = null
    }

    class OnProductTagChanged(
        var rowsAffected: Int,
        var canLoadMore: Boolean = false
    ) : OnChanged<ProductError>() {
        var causeOfChange: WCProductAction? = null
    }

    class OnProductCreated(
        var rowsAffected: Int,
        var remoteProductId: Long = 0L
    ) : OnChanged<ProductError>() {
        var causeOfChange: WCProductAction? = null
    }

    /**
     * returns the corresponding product from the database as a [WCProductModel].
     */
    fun getProductByRemoteId(site: SiteModel, remoteProductId: Long): WCProductModel? =
            ProductSqlUtils.getProductByRemoteId(site, remoteProductId)

    /**
     * returns the corresponding variation from the database as a [WCProductVariationModel].
     */
    fun getVariationByRemoteId(
        site: SiteModel,
        remoteProductId: Long,
        remoteVariationId: Long
    ): WCProductVariationModel? =
            ProductSqlUtils.getVariationByRemoteId(site, remoteProductId, remoteVariationId)

    /**
     * returns true if the corresponding product exists in the database
     */
    fun geProductExistsByRemoteId(site: SiteModel, remoteProductId: Long) =
            ProductSqlUtils.geProductExistsByRemoteId(site, remoteProductId)

    /**
     * returns true if the product exists with this [sku] in the database
     */
    fun geProductExistsBySku(site: SiteModel, sku: String) =
            ProductSqlUtils.getProductExistsBySku(site, sku)

    /**
     * returns a list of variations for a specific product in the database
     */
    fun getVariationsForProduct(site: SiteModel, remoteProductId: Long): List<WCProductVariationModel> =
            ProductSqlUtils.getVariationsForProduct(site, remoteProductId)

    /**
     * returns a list of shipping classes for a specific site in the database
     */
    fun getShippingClassListForSite(site: SiteModel): List<WCProductShippingClassModel> =
            ProductSqlUtils.getProductShippingClassListForSite(site.id)

    /**
     * returns the corresponding product shipping class from the database as a [WCProductShippingClassModel].
     */
    fun getShippingClassByRemoteId(site: SiteModel, remoteShippingClassId: Long): WCProductShippingClassModel? =
            ProductSqlUtils.getProductShippingClassByRemoteId(remoteShippingClassId, site.id)

    /**
     * returns a list of [WCProductModel] for the give [SiteModel] and [remoteProductIds]
     * if it exists in the database
     */
    fun getProductsByRemoteIds(site: SiteModel, remoteProductIds: List<Long>): List<WCProductModel> =
            ProductSqlUtils.getProductsByRemoteIds(site, remoteProductIds)

    /**
     * returns a list of [WCProductModel] for the given [SiteModel] and [filterOptions]
     * if it exists in the database. To filter by category, make sure the [filterOptions] value
     * is the category ID in String.
     */
    fun getProductsByFilterOptions(
        site: SiteModel,
        filterOptions: Map<ProductFilterOption, String>,
        sortType: ProductSorting = DEFAULT_PRODUCT_SORTING,
        excludedProductIds: List<Long>? = null
    ): List<WCProductModel> =
            ProductSqlUtils.getProductsByFilterOptions(site, filterOptions, sortType, excludedProductIds)

    fun getProductsForSite(site: SiteModel, sortType: ProductSorting = DEFAULT_PRODUCT_SORTING) =
            ProductSqlUtils.getProductsForSite(site, sortType)

    fun deleteProductsForSite(site: SiteModel) = ProductSqlUtils.deleteProductsForSite(site)

    fun getProductReviewsForSite(site: SiteModel): List<WCProductReviewModel> =
            ProductSqlUtils.getProductReviewsForSite(site)

    fun getProductReviewsForProductAndSiteId(localSiteId: Int, remoteProductId: Long): List<WCProductReviewModel> =
            ProductSqlUtils.getProductReviewsForProductAndSiteId(localSiteId, remoteProductId)

    /**
     * returns the count of products for the given [SiteModel] and [remoteProductIds]
     * if it exists in the database
     */
    fun getProductCountByRemoteIds(site: SiteModel, remoteProductIds: List<Long>): Int =
            ProductSqlUtils.getProductCountByRemoteIds(site, remoteProductIds)

    /**
     * returns the count of virtual products for the given [SiteModel] and [remoteProductIds]
     * if it exists in the database
     */
    fun getVirtualProductCountByRemoteIds(site: SiteModel, remoteProductIds: List<Long>): Int =
            ProductSqlUtils.getVirtualProductCountByRemoteIds(site, remoteProductIds)

    /**
     * returns a list of tags for a specific site in the database
     */
    fun getTagsForSite(site: SiteModel): List<WCProductTagModel> =
            ProductSqlUtils.getProductTagsForSite(site.id)

    fun getProductTagsByNames(site: SiteModel, tagNames: List<String>) =
            ProductSqlUtils.getProductTagsByNames(site.id, tagNames)

    fun getProductTagByName(site: SiteModel, tagName: String) =
            ProductSqlUtils.getProductTagByName(site.id, tagName)

    fun getProductReviewByRemoteId(
        localSiteId: Int,
        remoteReviewId: Long
    ): WCProductReviewModel? = ProductSqlUtils
            .getProductReviewByRemoteId(localSiteId, remoteReviewId)

    fun deleteProductReviewsForSite(site: SiteModel) = ProductSqlUtils.deleteAllProductReviewsForSite(site)

    fun deleteAllProductReviews() = ProductSqlUtils.deleteAllProductReviews()

    fun deleteProductImage(site: SiteModel, remoteProductId: Long, remoteMediaId: Long) =
            ProductSqlUtils.deleteProductImage(site, remoteProductId, remoteMediaId)

    fun getProductCategoriesForSite(site: SiteModel, sortType: ProductCategorySorting = DEFAULT_CATEGORY_SORTING) =
            ProductSqlUtils.getProductCategoriesForSite(site, sortType)

    fun getProductCategoryByRemoteId(site: SiteModel, remoteId: Long) =
            ProductSqlUtils.getProductCategoryByRemoteId(site.id, remoteId)

    fun getProductCategoryByNameAndParentId(
        site: SiteModel,
        categoryName: String,
        parentId: Long = 0L
    ) = ProductSqlUtils.getProductCategoryByNameAndParentId(site.id, categoryName, parentId)

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? WCProductAction ?: return
        when (actionType) {
            // remote actions
            WCProductAction.FETCH_SINGLE_PRODUCT ->
                fetchSingleProduct(action.payload as FetchSingleProductPayload)
            WCProductAction.FETCH_SINGLE_VARIATION ->
                fetchSingleVariation(action.payload as FetchSingleVariationPayload)
            WCProductAction.FETCH_PRODUCT_SKU_AVAILABILITY ->
                fetchProductSkuAvailability(action.payload as FetchProductSkuAvailabilityPayload)
            WCProductAction.FETCH_PRODUCTS ->
                fetchProducts(action.payload as FetchProductsPayload)
            WCProductAction.SEARCH_PRODUCTS ->
                searchProducts(action.payload as SearchProductsPayload)
            WCProductAction.FETCH_PRODUCT_VARIATIONS ->
                fetchProductVariations(action.payload as FetchProductVariationsPayload)
            WCProductAction.UPDATE_PRODUCT_REVIEW_STATUS ->
                updateProductReviewStatus(action.payload as UpdateProductReviewStatusPayload)
            WCProductAction.UPDATE_PRODUCT_IMAGES ->
                updateProductImages(action.payload as UpdateProductImagesPayload)
            WCProductAction.UPDATE_PRODUCT ->
                updateProduct(action.payload as UpdateProductPayload)
            WCProductAction.UPDATE_VARIATION ->
                updateVariation(action.payload as UpdateVariationPayload)
            WCProductAction.FETCH_SINGLE_PRODUCT_SHIPPING_CLASS ->
                fetchProductShippingClass(action.payload as FetchSingleProductShippingClassPayload)
            WCProductAction.FETCH_PRODUCT_SHIPPING_CLASS_LIST ->
                fetchProductShippingClasses(action.payload as FetchProductShippingClassListPayload)
            WCProductAction.FETCH_PRODUCT_PASSWORD ->
                fetchProductPassword(action.payload as FetchProductPasswordPayload)
            WCProductAction.UPDATE_PRODUCT_PASSWORD ->
                updateProductPassword(action.payload as UpdateProductPasswordPayload)
            WCProductAction.FETCH_PRODUCT_CATEGORIES ->
                fetchProductCategories(action.payload as FetchProductCategoriesPayload)
            WCProductAction.ADD_PRODUCT_CATEGORY ->
                addProductCategory(action.payload as AddProductCategoryPayload)
            WCProductAction.FETCH_PRODUCT_TAGS ->
                fetchProductTags(action.payload as FetchProductTagsPayload)
            WCProductAction.ADD_PRODUCT_TAGS ->
                addProductTags(action.payload as AddProductTagsPayload)
            WCProductAction.ADD_PRODUCT ->
                addProduct(action.payload as AddProductPayload)
            WCProductAction.DELETE_PRODUCT ->
                deleteProduct(action.payload as DeleteProductPayload)

            // remote responses
            WCProductAction.FETCHED_SINGLE_PRODUCT ->
                handleFetchSingleProductCompleted(action.payload as RemoteProductPayload)
            WCProductAction.FETCHED_SINGLE_VARIATION ->
                handleFetchSingleVariationCompleted(action.payload as RemoteVariationPayload)
            WCProductAction.FETCHED_PRODUCT_SKU_AVAILABILITY ->
                handleFetchProductSkuAvailabilityCompleted(action.payload as RemoteProductSkuAvailabilityPayload)
            WCProductAction.FETCHED_PRODUCTS ->
                handleFetchProductsCompleted(action.payload as RemoteProductListPayload)
            WCProductAction.SEARCHED_PRODUCTS ->
                handleSearchProductsCompleted(action.payload as RemoteSearchProductsPayload)
            WCProductAction.FETCHED_PRODUCT_VARIATIONS ->
                handleFetchProductVariationsCompleted(action.payload as RemoteProductVariationsPayload)
            WCProductAction.UPDATED_PRODUCT_REVIEW_STATUS ->
                handleUpdateProductReviewStatus(action.payload as RemoteProductReviewPayload)
            WCProductAction.UPDATED_PRODUCT_IMAGES ->
                handleUpdateProductImages(action.payload as RemoteUpdateProductImagesPayload)
            WCProductAction.UPDATED_PRODUCT ->
                handleUpdateProduct(action.payload as RemoteUpdateProductPayload)
            WCProductAction.UPDATED_VARIATION ->
                handleUpdateVariation(action.payload as RemoteUpdateVariationPayload)
            WCProductAction.FETCHED_PRODUCT_SHIPPING_CLASS_LIST ->
                handleFetchProductShippingClassesCompleted(action.payload as RemoteProductShippingClassListPayload)
            WCProductAction.FETCHED_SINGLE_PRODUCT_SHIPPING_CLASS ->
                handleFetchProductShippingClassCompleted(action.payload as RemoteProductShippingClassPayload)
            WCProductAction.FETCHED_PRODUCT_PASSWORD ->
                handleFetchProductPasswordCompleted(action.payload as RemoteProductPasswordPayload)
            WCProductAction.UPDATED_PRODUCT_PASSWORD ->
                handleUpdatedProductPasswordCompleted(action.payload as RemoteUpdatedProductPasswordPayload)
            WCProductAction.FETCHED_PRODUCT_CATEGORIES ->
                handleFetchProductCategories(action.payload as RemoteProductCategoriesPayload)
            WCProductAction.ADDED_PRODUCT_CATEGORY ->
                handleAddProductCategory(action.payload as RemoteAddProductCategoryResponsePayload)
            WCProductAction.FETCHED_PRODUCT_TAGS ->
                handleFetchProductTagsCompleted(action.payload as RemoteProductTagsPayload)
            WCProductAction.ADDED_PRODUCT_TAGS ->
                handleAddProductTags(action.payload as RemoteAddProductTagsResponsePayload)
            WCProductAction.ADDED_PRODUCT ->
                handleAddNewProduct(action.payload as RemoteAddProductPayload)
            WCProductAction.DELETED_PRODUCT ->
                handleDeleteProduct(action.payload as RemoteDeleteProductPayload)
        }
    }

    suspend fun submitProductAttributeChanges(
        site: SiteModel,
        productId: Long,
        attributes: List<WCProductModel.ProductAttribute>
    ): WooResult<WCProductModel> =
            coroutineEngine?.withDefaultContext(API, this, "submitProductAttributes") {
                wcProductRestClient.updateProductAttributes(site, productId, Gson().toJson(attributes))
                        ?.asWooResult()
                        ?.model?.asProductModel()
                        ?.apply {
                            localSiteId = site.id
                            ProductSqlUtils.insertOrUpdateProduct(this)
                        }
                        ?.let { WooResult(it) }
            } ?: WooResult(WooError(WooErrorType.GENERIC_ERROR, UNKNOWN))

    suspend fun submitVariationAttributeChanges(
        site: SiteModel,
        productId: Long,
        variationId: Long,
        attributes: List<WCProductModel.ProductAttribute>
    ): WooResult<WCProductVariationModel> =
            coroutineEngine?.withDefaultContext(API, this, "submitVariationAttributes") {
                wcProductRestClient.updateVariationAttributes(site, productId, variationId, Gson().toJson(attributes))
                        ?.asWooResult()
                        ?.model?.asProductVariationModel()
                        ?.apply { insertOrUpdateProductVariation(this) }
                        ?.let { WooResult(it) }
            } ?: WooResult(WooError(WooErrorType.GENERIC_ERROR, UNKNOWN))

    suspend fun generateEmptyVariation(
        site: SiteModel,
        product: WCProductModel
    ): WooResult<WCProductVariationModel> =
            coroutineEngine?.withDefaultContext(API, this, "generateEmptyVariation") {
                product.attributeList
                        .filter { it.variation }
                        .map { ProductVariantOption(it.id, it.name, "") }
                        .let { Gson().toJson(it) }
                        .let { wcProductRestClient.generateEmptyVariation(site, product.remoteProductId, it) }
                        ?.asWooResult()
                        ?.model?.asProductVariationModel()
                        ?.apply { insertOrUpdateProductVariation(this) }
                        ?.let { WooResult(it) }
                        ?: WooResult(WooError(INVALID_RESPONSE, GenericErrorType.INVALID_RESPONSE))
            } ?: WooResult(WooError(WooErrorType.GENERIC_ERROR, UNKNOWN))

    suspend fun deleteVariation(
        site: SiteModel,
        productId: Long,
        variationId: Long
    ): WooResult<WCProductVariationModel> =
            coroutineEngine?.withDefaultContext(API, this, "deleteVariation") {
                wcProductRestClient.deleteVariation(site, productId, variationId)
                        ?.asWooResult()
                        ?.model?.asProductVariationModel()
                        ?.apply { deleteVariationsForProduct(site, productId) }
                        ?.let { WooResult(it) }
                        ?: WooResult(WooError(INVALID_RESPONSE, GenericErrorType.INVALID_RESPONSE))
            } ?: WooResult(WooError(WooErrorType.GENERIC_ERROR, UNKNOWN))

    override fun onRegister() = AppLog.d(API, "WCProductStore onRegister")

    private fun fetchSingleProduct(payload: FetchSingleProductPayload) {
        with(payload) { wcProductRestClient.fetchSingleProduct(site, remoteProductId) }
    }

    private fun fetchSingleVariation(payload: FetchSingleVariationPayload) {
        with(payload) { wcProductRestClient.fetchSingleVariation(site, remoteProductId, remoteVariationId) }
    }

    private fun fetchProductSkuAvailability(payload: FetchProductSkuAvailabilityPayload) {
        with(payload) { wcProductRestClient.fetchProductSkuAvailability(site, sku) }
    }

    private fun fetchProducts(payload: FetchProductsPayload) {
        with(payload) {
            wcProductRestClient.fetchProducts(
                    site, pageSize, offset, sorting,
                    remoteProductIds = remoteProductIds,
                    filterOptions = filterOptions,
                    excludedProductIds = excludedProductIds
            )
        }
    }

    suspend fun fetchProductListSynced(site: SiteModel, productIds: List<Long>): List<WCProductModel>? {
        return coroutineEngine?.withDefaultContext(API, this, "fetchProductList") {
            wcProductRestClient.fetchProductsWithSyncRequest(site = site, remoteProductIds = productIds)?.result
        }?.also {
            ProductSqlUtils.insertOrUpdateProducts(it)
        }
    }

    private fun searchProducts(payload: SearchProductsPayload) {
        with(payload) {
            wcProductRestClient.searchProducts(
                    site, searchQuery, pageSize, offset, sorting, excludedProductIds
            )
        }
    }

    private fun fetchProductVariations(payload: FetchProductVariationsPayload) {
        with(payload) { wcProductRestClient.fetchProductVariations(site, remoteProductId, pageSize, offset) }
    }

    private fun fetchProductShippingClass(payload: FetchSingleProductShippingClassPayload) {
        with(payload) { wcProductRestClient.fetchSingleProductShippingClass(site, remoteShippingClassId) }
    }

    private fun fetchProductShippingClasses(payload: FetchProductShippingClassListPayload) {
        with(payload) { wcProductRestClient.fetchProductShippingClassList(site, pageSize, offset) }
    }

    suspend fun fetchProductReviews(payload: FetchProductReviewsPayload): OnProductReviewChanged {
        return coroutineEngine.withDefaultContext(API, this, "fetchProductReviews") {
            val response = with(payload) {
                wcProductRestClient.fetchProductReviews(site, offset, reviewIds, productIds, filterByStatus)
            }

            val onProductReviewChanged = if (response.isError) {
                OnProductReviewChanged(0).also { it.error = response.error }
            } else {
                // Clear existing product reviews if this is a fresh fetch (loadMore = false).
                // This is the simplest way to keep our local reviews in sync with remote reviews
                // in case of deletions.
                if (!response.loadedMore) {
                    ProductSqlUtils.deleteAllProductReviewsForSite(response.site)
                }
                val rowsAffected = ProductSqlUtils.insertOrUpdateProductReviews(response.reviews)
                OnProductReviewChanged(rowsAffected, canLoadMore = response.canLoadMore)
            }

            onProductReviewChanged
        }
    }

    suspend fun fetchSingleProductReview(payload: FetchSingleProductReviewPayload): OnProductReviewChanged {
        return coroutineEngine.withDefaultContext(API, this, "fetchSingleProductReview") {
            val result = wcProductRestClient.fetchProductReviewById(payload.site, payload.remoteReviewId)

            return@withDefaultContext if (result.isError) {
                OnProductReviewChanged(0).also { it.error = result.error }
            } else {
                val rowsAffected = result.productReview?.let {
                    ProductSqlUtils.insertOrUpdateProductReview(it)
                } ?: 0
                OnProductReviewChanged(rowsAffected)
            }
        }
    }

    private fun fetchProductPassword(payload: FetchProductPasswordPayload) {
        with(payload) { wcProductRestClient.fetchProductPassword(site, remoteProductId) }
    }

    private fun updateProductPassword(payload: UpdateProductPasswordPayload) {
        with(payload) { wcProductRestClient.updateProductPassword(site, remoteProductId, password) }
    }

    private fun updateProductReviewStatus(payload: UpdateProductReviewStatusPayload) {
        with(payload) { wcProductRestClient.updateProductReviewStatus(site, remoteReviewId, newStatus) }
    }

    private fun updateProductImages(payload: UpdateProductImagesPayload) {
        with(payload) { wcProductRestClient.updateProductImages(site, remoteProductId, imageList) }
    }

    private fun fetchProductCategories(payloadProduct: FetchProductCategoriesPayload) {
        with(payloadProduct) {
            wcProductRestClient.fetchProductCategories(
                    site, pageSize, offset, productCategorySorting
            )
        }
    }

    private fun addProductCategory(payload: AddProductCategoryPayload) {
        with(payload) { wcProductRestClient.addProductCategory(site, category) }
    }

    private fun fetchProductTags(payload: FetchProductTagsPayload) {
        with(payload) { wcProductRestClient.fetchProductTags(site, pageSize, offset, searchQuery) }
    }

    private fun addProductTags(payload: AddProductTagsPayload) {
        with(payload) { wcProductRestClient.addProductTags(site, tags) }
    }

    private fun updateProduct(payload: UpdateProductPayload) {
        with(payload) {
            val storedProduct = getProductByRemoteId(site, product.remoteProductId)
            wcProductRestClient.updateProduct(site, storedProduct, product)
        }
    }

    private fun updateVariation(payload: UpdateVariationPayload) {
        with(payload) {
            val storedVariation = getVariationByRemoteId(site, variation.remoteProductId, variation.remoteVariationId)
            wcProductRestClient.updateVariation(site, storedVariation, variation)
        }
    }

    private fun addProduct(payload: AddProductPayload) {
        with(payload) {
            wcProductRestClient.addProduct(site, product)
        }
    }

    private fun deleteProduct(payload: DeleteProductPayload) {
        with(payload) {
            wcProductRestClient.deleteProduct(site, remoteProductId, forceDelete)
        }
    }

    private fun handleFetchSingleProductCompleted(payload: RemoteProductPayload) {
        val onProductChanged: OnProductChanged

        if (payload.isError) {
            onProductChanged = OnProductChanged(0).also {
                it.error = payload.error
                it.remoteProductId = payload.product.remoteProductId
            }
        } else {
            val rowsAffected = ProductSqlUtils.insertOrUpdateProduct(payload.product)
            onProductChanged = OnProductChanged(rowsAffected).also {
                it.remoteProductId = payload.product.remoteProductId
            }

            // TODO: 18/08/2021 @wzieba add tests
            coroutineEngine?.launch(T.DB, this, "cacheProductAddons") {
                val domainAddons = mapProductAddonsToDomain(payload.product.addons)
                addonsDao.cacheProductAddons(
                        productRemoteId = payload.product.remoteProductId,
                        siteRemoteId = payload.site.siteId,
                        addons = domainAddons
                )
            }
        }

        onProductChanged.causeOfChange = WCProductAction.FETCH_SINGLE_PRODUCT
        emitChange(onProductChanged)
    }

    private fun mapProductAddonsToDomain(remoteAddons: Array<RemoteAddonDto>?): List<Addon> {
        return remoteAddons.orEmpty()
                .toList()
                .mapNotNull { remoteAddonDto ->
                    try {
                        RemoteAddonMapper.toDomain(remoteAddonDto)
                    } catch (exception: MappingRemoteException) {
                        logger.e(API, "Exception while parsing $remoteAddonDto: ${exception.message}")
                        null
                    }
                }
    }

    private fun handleFetchSingleVariationCompleted(payload: RemoteVariationPayload) {
        val onVariationChanged: OnVariationChanged

        if (payload.isError) {
            onVariationChanged = OnVariationChanged(0).also {
                it.error = payload.error
                it.remoteProductId = payload.variation.remoteProductId
                it.remoteVariationId = payload.variation.remoteVariationId
            }
        } else {
            val rowsAffected = insertOrUpdateProductVariation(payload.variation)
            onVariationChanged = OnVariationChanged(rowsAffected).also {
                it.remoteProductId = payload.variation.remoteProductId
                it.remoteVariationId = payload.variation.remoteVariationId
            }
        }

        onVariationChanged.causeOfChange = WCProductAction.FETCH_SINGLE_VARIATION
        emitChange(onVariationChanged)
    }

    private fun handleFetchProductSkuAvailabilityCompleted(payload: RemoteProductSkuAvailabilityPayload) {
        val onProductSkuAvailabilityChanged = OnProductSkuAvailabilityChanged(payload.sku, payload.available)
        if (payload.isError) {
            onProductSkuAvailabilityChanged.also { it.error = payload.error }
        }
        onProductSkuAvailabilityChanged.causeOfChange = WCProductAction.FETCH_PRODUCT_SKU_AVAILABILITY
        emitChange(onProductSkuAvailabilityChanged)
    }

    private fun handleFetchProductsCompleted(payload: RemoteProductListPayload) {
        val onProductChanged: OnProductChanged

        if (payload.isError) {
            onProductChanged = OnProductChanged(0).also { it.error = payload.error }
        } else {
            // remove the existing products for this site if this is the first page of results
            // or if the remoteProductIds or excludedProductIds are null, otherwise
            // products deleted outside of the app will persist
            if (payload.offset == 0 && payload.remoteProductIds == null && payload.excludedProductIds == null) {
                ProductSqlUtils.deleteProductsForSite(payload.site)
            }
            val rowsAffected = ProductSqlUtils.insertOrUpdateProducts(payload.products)
            onProductChanged = OnProductChanged(rowsAffected, canLoadMore = payload.canLoadMore)

            // TODO: 18/08/2021 @wzieba add tests
            coroutineEngine?.launch(T.DB, this, "cacheProductsAddons") {
                payload.products.forEach { product ->

                    val domainAddons = mapProductAddonsToDomain(product.addons)

                    addonsDao.cacheProductAddons(
                            productRemoteId = product.remoteProductId,
                            siteRemoteId = payload.site.siteId,
                            addons = domainAddons
                    )
                }
            }
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

    private fun handleFetchProductShippingClassesCompleted(payload: RemoteProductShippingClassListPayload) {
        val onProductShippingClassesChanged = if (payload.isError) {
            OnProductShippingClassesChanged(0).also { it.error = payload.error }
        } else {
            // delete product shipping class list for site if this is the first page of results, otherwise
            // shipping class list deleted outside of the app will persist
            if (payload.offset == 0) {
                ProductSqlUtils.deleteProductShippingClassListForSite(payload.site)
            }

            val rowsAffected = ProductSqlUtils.insertOrUpdateProductShippingClassList(payload.shippingClassList)
            OnProductShippingClassesChanged(rowsAffected, canLoadMore = payload.canLoadMore)
        }
        onProductShippingClassesChanged.causeOfChange = WCProductAction.FETCH_PRODUCT_SHIPPING_CLASS_LIST
        emitChange(onProductShippingClassesChanged)
    }

    private fun handleFetchProductShippingClassCompleted(payload: RemoteProductShippingClassPayload) {
        val onProductShippingClassesChanged = if (payload.isError) {
            OnProductShippingClassesChanged(0).also { it.error = payload.error }
        } else {
            val rowsAffected = ProductSqlUtils.insertOrUpdateProductShippingClass(payload.productShippingClassModel)
            OnProductShippingClassesChanged(rowsAffected)
        }
        onProductShippingClassesChanged.causeOfChange = WCProductAction.FETCH_SINGLE_PRODUCT_SHIPPING_CLASS
        emitChange(onProductShippingClassesChanged)
    }

    private fun handleFetchProductPasswordCompleted(payload: RemoteProductPasswordPayload) {
        val onProductPasswordChanged = if (payload.isError) {
            OnProductPasswordChanged(payload.remoteProductId, "").also { it.error = payload.error }
        } else {
            OnProductPasswordChanged(payload.remoteProductId, payload.password)
        }
        onProductPasswordChanged.causeOfChange = WCProductAction.FETCH_PRODUCT_PASSWORD
        emitChange(onProductPasswordChanged)
    }

    private fun handleUpdatedProductPasswordCompleted(payload: RemoteUpdatedProductPasswordPayload) {
        val onProductPasswordUpdated = if (payload.isError) {
            OnProductPasswordChanged(payload.remoteProductId, null).also { it.error = payload.error }
        } else {
            OnProductPasswordChanged(payload.remoteProductId, payload.password)
        }
        onProductPasswordUpdated.causeOfChange = WCProductAction.UPDATE_PRODUCT_PASSWORD
        emitChange(onProductPasswordUpdated)
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

    private fun handleUpdateVariation(payload: RemoteUpdateVariationPayload) {
        val onVariationUpdated: OnVariationUpdated

        if (payload.isError) {
            onVariationUpdated = OnVariationUpdated(
                    0,
                    payload.variation.remoteProductId,
                    payload.variation.remoteVariationId
            )
                    .also { it.error = payload.error }
        } else {
            val rowsAffected = insertOrUpdateProductVariation(payload.variation)
            onVariationUpdated = OnVariationUpdated(
                    rowsAffected,
                    payload.variation.remoteProductId,
                    payload.variation.remoteVariationId
            )
        }

        onVariationUpdated.causeOfChange = WCProductAction.UPDATED_VARIATION
        emitChange(onVariationUpdated)
    }

    private fun handleFetchProductCategories(payload: RemoteProductCategoriesPayload) {
        val onProductCategoryChanged: OnProductCategoryChanged

        if (payload.isError) {
            onProductCategoryChanged = OnProductCategoryChanged(0).also { it.error = payload.error }
        } else {
            // Clear existing product categories if this is a fresh fetch (loadMore = false).
            // This is the simplest way to keep our local categories in sync with remote categories
            // in case of deletions.
            if (!payload.loadedMore) {
                ProductSqlUtils.deleteAllProductCategoriesForSite(payload.site)
            }
            val rowsAffected = ProductSqlUtils.insertOrUpdateProductCategories(payload.categories)
            onProductCategoryChanged = OnProductCategoryChanged(rowsAffected, canLoadMore = payload.canLoadMore)
        }

        onProductCategoryChanged.causeOfChange = WCProductAction.FETCH_PRODUCT_CATEGORIES
        emitChange(onProductCategoryChanged)
    }

    private fun handleAddProductCategory(payload: RemoteAddProductCategoryResponsePayload) {
        val onProductCategoryChanged: OnProductCategoryChanged

        if (payload.isError) {
            onProductCategoryChanged = OnProductCategoryChanged(0).also { it.error = payload.error }
        } else {
            val rowsAffected = payload.category?.let { ProductSqlUtils.insertOrUpdateProductCategory(it) } ?: 0
            onProductCategoryChanged = OnProductCategoryChanged(rowsAffected)
        }

        onProductCategoryChanged.causeOfChange = WCProductAction.ADDED_PRODUCT_CATEGORY
        emitChange(onProductCategoryChanged)
    }

    private fun handleFetchProductTagsCompleted(payload: RemoteProductTagsPayload) {
        val onProductTagsChanged = if (payload.isError) {
            OnProductTagChanged(0).also { it.error = payload.error }
        } else {
            // delete product tags for site if this is the first page of results, otherwise
            // tags deleted outside of the app will persist
            if (payload.offset == 0 && payload.searchQuery.isNullOrEmpty()) {
                ProductSqlUtils.deleteProductTagsForSite(payload.site)
            }

            val rowsAffected = ProductSqlUtils.insertOrUpdateProductTags(payload.tags)
            OnProductTagChanged(rowsAffected, canLoadMore = payload.canLoadMore)
        }
        onProductTagsChanged.causeOfChange = WCProductAction.FETCH_PRODUCT_TAGS
        emitChange(onProductTagsChanged)
    }

    private fun handleAddProductTags(payload: RemoteAddProductTagsResponsePayload) {
        val onProductTagsChanged: OnProductTagChanged
        if (payload.isError) {
            onProductTagsChanged = OnProductTagChanged(0).also { it.error = payload.error }
        } else {
            val rowsAffected = ProductSqlUtils.insertOrUpdateProductTags(payload.tags.filter { it.name.isNotEmpty() })
            onProductTagsChanged = OnProductTagChanged(rowsAffected)
        }

        onProductTagsChanged.causeOfChange = WCProductAction.ADDED_PRODUCT_TAGS
        emitChange(onProductTagsChanged)
    }

    private fun handleAddNewProduct(payload: RemoteAddProductPayload) {
        val onProductCreated: OnProductCreated

        if (payload.isError) {
            onProductCreated = OnProductCreated(0, payload.product.remoteProductId).also { it.error = payload.error }
        } else {
            val rowsAffected = ProductSqlUtils.insertOrUpdateProduct(payload.product)
            onProductCreated = OnProductCreated(rowsAffected, payload.product.remoteProductId)
        }

        onProductCreated.causeOfChange = WCProductAction.ADDED_PRODUCT
        emitChange(onProductCreated)
    }

    private fun handleDeleteProduct(payload: RemoteDeleteProductPayload) {
        val onProductChanged: OnProductChanged

        if (payload.isError) {
            onProductChanged = OnProductChanged(0).also { it.error = payload.error }
        } else {
            val rowsAffected = ProductSqlUtils.deleteProduct(payload.site, payload.remoteProductId)
            onProductChanged = OnProductChanged(rowsAffected, payload.remoteProductId)
        }

        onProductChanged.causeOfChange = WCProductAction.DELETED_PRODUCT
        emitChange(onProductChanged)
    }
}
