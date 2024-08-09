package org.wordpress.android.fluxc.release

import com.google.gson.JsonArray
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.action.WCProductAction
import org.wordpress.android.fluxc.example.BuildConfig
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.ProductWithMetaData
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.model.WCProductFileModel
import org.wordpress.android.fluxc.model.WCProductImageModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductVisibility
import org.wordpress.android.fluxc.persistence.MediaSqlUtils
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.OnMediaListFetched
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.AddProductTagsPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductCategoriesPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductPasswordPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductReviewsPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductShippingClassListPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductTagsPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductVariationsPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductsPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductReviewPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductShippingClassPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductCategoryChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductCreated
import org.wordpress.android.fluxc.store.WCProductStore.OnProductImagesChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductPasswordChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductShippingClassesChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductTagChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductUpdated
import org.wordpress.android.fluxc.store.WCProductStore.RemoteAddProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductImagesPayload
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductPasswordPayload
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.UpdateVariationPayload
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject
import kotlin.random.Random

class ReleaseStack_WCProductTest : ReleaseStack_WCBase() {
    internal enum class TestEvent {
        NONE,
        FETCHED_PRODUCTS,
        FETCHED_PRODUCT_SHIPPING_CLASS_LIST,
        FETCHED_SINGLE_PRODUCT_SHIPPING_CLASS,
        FETCHED_PRODUCT_PASSWORD,
        UPDATED_PRODUCT,
        UPDATED_PRODUCT_REVIEW_STATUS,
        UPDATED_PRODUCT_IMAGES,
        UPDATED_PRODUCT_PASSWORD,
        FETCH_PRODUCT_CATEGORIES,
        ADDED_PRODUCT_CATEGORY,
        FETCHED_PRODUCT_TAGS,
        ADDED_PRODUCT_TAGS,
        ADD_PRODUCT,
        ADDED_PRODUCT
    }

    @Inject internal lateinit var productStore: WCProductStore

    @Inject internal lateinit var mediaStore: MediaStore // must be injected for onMediaListFetched()

    private var nextEvent: TestEvent = TestEvent.NONE
    private val productModel = WCProductModel(8).apply {
        remoteProductId = BuildConfig.TEST_WC_PRODUCT_ID.toLong()
        dateCreated = "2018-04-20T15:45:14Z"
        taxStatus = "taxable"
        stockStatus = "instock"
        backorders = "yes"
        images = "[]"
    }
    private val productModelWithVariations = WCProductModel(8).apply {
        remoteProductId = BuildConfig.TEST_WC_PRODUCT_WITH_VARIATIONS_ID.toLong()
        dateCreated = "2018-04-20T15:45:14Z"
    }

    private val variationModel = WCProductVariationModel().apply {
        remoteVariationId = 759
        remoteProductId = BuildConfig.TEST_WC_PRODUCT_WITH_VARIATIONS_ID.toLong()
        dateCreated = "2018-04-20T15:45:14Z"
        taxStatus = "taxable"
        stockStatus = "instock"
        image = ""
    }

    private val remoteProductReviewId = BuildConfig.TEST_WC_PRODUCT_REVIEW_ID.toLong()

    private val updatedPassword = "password"

    private var lastEvent: OnProductChanged? = null
    private var lastProductCategoryEvent: OnProductCategoryChanged? = null
    private var lastShippingClassEvent: OnProductShippingClassesChanged? = null
    private var lastProductTagEvent: OnProductTagChanged? = null
    private var lastAddNewProductEvent: OnProductCreated? = null

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp(false)
        mReleaseStackAppComponent.inject(this)
        init()
        nextEvent = TestEvent.NONE
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchSingleProduct() = runBlocking {
        // remove all products for this site and verify there are none
        ProductSqlUtils.deleteProductsForSite(sSite)
        assertEquals(ProductSqlUtils.getProductCountForSite(sSite), 0)

        productStore.fetchSingleProduct(
            FetchSingleProductPayload(
                sSite,
                productModel.remoteProductId
            )
        )

        // Verify results
        val fetchedProduct = productStore.getProductByRemoteId(sSite, productModel.remoteProductId)
        assertNotNull(fetchedProduct)
        assertEquals(fetchedProduct!!.remoteProductId, productModel.remoteProductId)

        // Verify there's only one product for this site
        assertEquals(ProductSqlUtils.getProductCountForSite(sSite), 1)
        Unit
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchSingleVariation() = runBlocking {
        // remove all variation for this site and verify there are none
        ProductSqlUtils.deleteVariationsForProduct(
            sSite,
            productModelWithVariations.remoteProductId
        )
        assertEquals(
            ProductSqlUtils.getVariationsForProduct(
                sSite,
                productModelWithVariations.remoteProductId
            ).size,
            0
        )

        val result = productStore.fetchSingleVariation(
            sSite,
            variationModel.remoteProductId,
            variationModel.remoteVariationId
        )

        assertEquals(result.remoteProductId, variationModel.remoteProductId)
        assertEquals(result.remoteVariationId, variationModel.remoteVariationId)

        // Verify results
        val fetchedVariation = productStore.getVariationByRemoteId(
            sSite,
            variationModel.remoteProductId,
            variationModel.remoteVariationId
        )
        assertNotNull(fetchedVariation)
        assertEquals(fetchedVariation!!.remoteProductId, variationModel.remoteProductId)
        assertEquals(fetchedVariation.remoteVariationId, variationModel.remoteVariationId)

        // Verify there's only one variation for this site
        assertEquals(
            1,
            ProductSqlUtils.getVariationsForProduct(sSite, variationModel.remoteProductId).size
        )
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchProducts() {
        // remove all products for this site and verify there are none
        ProductSqlUtils.deleteProductsForSite(sSite)
        assertEquals(ProductSqlUtils.getProductCountForSite(sSite), 0)

        nextEvent = TestEvent.FETCHED_PRODUCTS
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
            WCProductActionBuilder
                .newFetchProductsAction(FetchProductsPayload(sSite))
        )
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Verify results
        val fetchedProducts = productStore.getProductsForSite(sSite)
        assertNotEquals(fetchedProducts.size, 0)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchProductVariations() = runBlocking {
        // remove all variations for this product and verify there are none
        ProductSqlUtils.deleteVariationsForProduct(
            sSite,
            productModelWithVariations.remoteProductId
        )
        assertEquals(
            ProductSqlUtils.getVariationsForProduct(
                sSite,
                productModelWithVariations.remoteProductId
            ).size, 0
        )

        productStore.fetchProductVariations(
            FetchProductVariationsPayload(
                sSite,
                productModelWithVariations.remoteProductId
            )
        )

        // Verify results
        val fetchedVariations = productStore.getVariationsForProduct(
            sSite,
            productModelWithVariations.remoteProductId
        )
        assertNotEquals(fetchedVariations.size, 0)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchProductShippingClassesForSite() {
        /*
         * TEST 1: Fetch product shipping classes for site
         */
        // Remove all product shipping classes from the database
        ProductSqlUtils.deleteProductShippingClassListForSite(sSite)
        assertEquals(0, ProductSqlUtils.getProductShippingClassListForSite(sSite.id).size)

        nextEvent = TestEvent.FETCHED_PRODUCT_SHIPPING_CLASS_LIST
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
            WCProductActionBuilder.newFetchProductShippingClassListAction(
                FetchProductShippingClassListPayload(sSite)
            )
        )
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Verify results
        val fetchedShippingClasses = productStore.getShippingClassListForSite(sSite)
        assertTrue(fetchedShippingClasses.isNotEmpty())
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchProductShippingClassByRemoteIdForSite() {
        /*
         * TEST 1: Fetch product shipping class for site
         */
        // Remove all product shipping classes from the database
        val remoteShippingClassId = 31L
        ProductSqlUtils.deleteProductShippingClassListForSite(sSite)
        assertEquals(0, ProductSqlUtils.getProductShippingClassListForSite(sSite.id).size)

        nextEvent = TestEvent.FETCHED_SINGLE_PRODUCT_SHIPPING_CLASS
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
            WCProductActionBuilder.newFetchSingleProductShippingClassAction(
                FetchSingleProductShippingClassPayload(sSite, remoteShippingClassId)
            )
        )
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Verify results
        val fetchedShippingClasses = productStore.getShippingClassByRemoteId(
            sSite, remoteShippingClassId
        )
        assertNotNull(fetchedShippingClasses)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchProductCategories() {
        // Remove all product categories from the database
        ProductSqlUtils.deleteAllProductCategories()
        assertEquals(0, ProductSqlUtils.getProductCategoriesForSite(sSite).size)

        nextEvent = TestEvent.FETCH_PRODUCT_CATEGORIES
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
            WCProductActionBuilder.newFetchProductCategoriesAction(
                FetchProductCategoriesPayload(
                    sSite
                )
            )
        )
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Verify results
        val fetchAllCategories = productStore.getProductCategoriesForSite(sSite)
        assertTrue(fetchAllCategories.isNotEmpty())
    }

    @Throws(InterruptedException::class)
    @Test
    fun testAddProductCategory() = runBlocking {
        // Remove all product categories from the database
        ProductSqlUtils.deleteAllProductCategories()
        assertEquals(0, ProductSqlUtils.getProductCategoriesForSite(sSite).size)

        nextEvent = TestEvent.ADDED_PRODUCT_CATEGORY
        mCountDownLatch = CountDownLatch(1)

        val productCategoryModel = WCProductCategoryModel().apply {
            // duplicate category names fail in the API level so added a random number next to the "Test"
            name = "Test" + Random.nextInt(0, 10000)
        }
        productStore.addProductCategory(sSite, productCategoryModel)

        // Verify results
        val fetchAllCategories = productStore.getProductCategoriesForSite(sSite)
        assertTrue(fetchAllCategories.isNotEmpty())
        assertTrue(fetchAllCategories.size == 1)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchProductReviews() = runBlocking {
        /*
         * TEST 1: Fetch product reviews for site
         */
        // Remove all product reviews from the database
        productStore.deleteAllProductReviews()
        assertEquals(0, ProductSqlUtils.getProductReviewsForSite(sSite).size)

        productStore.fetchProductReviews(
            FetchProductReviewsPayload(sSite, offset = 0),
            deletePreviouslyCachedReviews = false
        )

        // Verify results
        val fetchedReviewsAll = productStore.getProductReviewsForSite(sSite)
        assertTrue(fetchedReviewsAll.isNotEmpty())

        /*
         * TEST 2: Fetch product reviews matching a list of review ID's
         */
        // Store a couple of the IDs from the previous test
        val idsToFetch = fetchedReviewsAll.take(3).map { it.remoteProductReviewId }

        // Remove all product reviews from the database
        productStore.deleteAllProductReviews()
        assertEquals(0, ProductSqlUtils.getProductReviewsForSite(sSite).size)

        productStore.fetchProductReviews(
            FetchProductReviewsPayload(sSite, reviewIds = idsToFetch, offset = 0),
            deletePreviouslyCachedReviews = false
        )

        // Verify results
        val fetchReviewsId = productStore.getProductReviewsForSite(sSite)
        assertEquals(idsToFetch.size, fetchReviewsId.size)

        /*
         * TEST 3: Fetch product reviews for a list of product
         */
        // Store a couple of the IDs from the previous test
        val productIdsToFetch = fetchedReviewsAll.take(3).map { it.remoteProductId }

        // Check to see how many reviews currently exist for these product IDs before deleting
        // from the database
        val reviewsByProduct = productIdsToFetch.map {
            productStore.getProductReviewsForProductAndSiteId(
                sSite.id,
                it
            )
        }

        // Remove all product reviews from the database
        productStore.deleteAllProductReviews()
        assertEquals(0, ProductSqlUtils.getProductReviewsForSite(sSite).size)

        productStore.fetchProductReviews(
            FetchProductReviewsPayload(
                sSite,
                productIds = productIdsToFetch,
                offset = 0
            ),
            deletePreviouslyCachedReviews = false
        )

        // Verify results
        val fetchedReviewsForProduct = productStore.getProductReviewsForSite(sSite)
        assertEquals(reviewsByProduct.size, fetchedReviewsForProduct.size)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testUpdateProductPassword() {
        // first dispatch a request to update the password - note that this will fail for private products
        nextEvent = TestEvent.UPDATED_PRODUCT_PASSWORD
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
            WCProductActionBuilder
                .newUpdateProductPasswordAction(
                    UpdateProductPasswordPayload(
                        sSite,
                        productModel.remoteProductId,
                        updatedPassword
                    )
                )
        )
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // then dispatch a request to fetch it so we can make sure it's the same we just updated to
        nextEvent = TestEvent.FETCHED_PRODUCT_PASSWORD
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
            WCProductActionBuilder
                .newFetchProductPasswordAction(
                    FetchProductPasswordPayload(
                        sSite,
                        productModel.remoteProductId
                    )
                )
        )
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchSingleProductAndUpdateReview() = runBlocking {
        // Remove all product reviews from the database
        productStore.deleteAllProductReviews()
        assertEquals(0, ProductSqlUtils.getProductReviewsForSite(sSite).size)

        productStore.fetchSingleProductReview(
            FetchSingleProductReviewPayload(
                sSite,
                remoteProductReviewId
            )
        )

        // Verify results
        val review = productStore.getProductReviewByRemoteId(sSite.id, remoteProductReviewId)
        assertNotNull(review)

        // Update review status to spam - should get deleted from db
        review?.let {
            val newStatus = "spam"
            productStore.updateProductReviewStatus(sSite, it.remoteProductReviewId, newStatus)

            // Verify results - review should be deleted from db
            val savedReview = productStore
                .getProductReviewByRemoteId(sSite.id, remoteProductReviewId)
            assertNull(savedReview)
        }

        // Update review status to approved - should get added to db
        review?.let {
            val newStatus = "approved"
            nextEvent = TestEvent.UPDATED_PRODUCT_REVIEW_STATUS
            productStore.updateProductReviewStatus(sSite, it.remoteProductReviewId, newStatus)

            // Verify results
            val savedReview = productStore
                .getProductReviewByRemoteId(sSite.id, remoteProductReviewId)
            assertNotNull(savedReview)
            assertEquals(newStatus, savedReview!!.status)
        }

        // Update review status to trash - should get deleted from db
        review?.let {
            val newStatus = "trash"
            productStore.updateProductReviewStatus(sSite, it.remoteProductReviewId, newStatus)

            // Verify results - review should be deleted from db
            val savedReview = productStore
                .getProductReviewByRemoteId(sSite.id, remoteProductReviewId)
            assertNull(savedReview)
        }

        // Update review status to hold - should get added to db
        review?.let {
            val newStatus = "hold"
            productStore.updateProductReviewStatus(sSite, it.remoteProductReviewId, newStatus)

            // Verify results
            val savedReview = productStore
                .getProductReviewByRemoteId(sSite.id, remoteProductReviewId)
            assertNotNull(savedReview)
            assertEquals(newStatus, savedReview!!.status)
        }
        Unit
    }

    @Throws(InterruptedException::class)
    @Test
    fun testUpdateProductImages() {
        // first get the list of this site's media, and if it's empty fetch a single media model
        var siteMedia = MediaSqlUtils.getAllSiteMedia(sSite)
        if (siteMedia.isEmpty()) {
            fetchFirstMedia()
            siteMedia = MediaSqlUtils.getAllSiteMedia(sSite)
            assertTrue(siteMedia.isNotEmpty())
        }

        val mediaModelForProduct = siteMedia[0]

        nextEvent = TestEvent.UPDATED_PRODUCT_IMAGES
        mCountDownLatch = CountDownLatch(1)
        val imageList = ArrayList<WCProductImageModel>().also {
            it.add(WCProductImageModel.fromMediaModel(mediaModelForProduct))
        }
        mDispatcher.dispatch(
            WCProductActionBuilder.newUpdateProductImagesAction(
                UpdateProductImagesPayload(sSite, productModel.remoteProductId, imageList)
            )
        )
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        val updatedProduct = productStore.getProductByRemoteId(sSite, productModel.remoteProductId)
        assertNotNull(updatedProduct)

        val updatedImageList = updatedProduct!!.getImageListOrEmpty()
        assertNotNull(updatedImageList)
        assertEquals(updatedImageList.size, 1)

        val updatedImage = updatedImageList[0]
        assertEquals(updatedImage.id, mediaModelForProduct.mediaId)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testUpdateProduct() {
        val updatedProductDesc = "Testing updating product description"
        productModel.description = updatedProductDesc

        val updatedProductName = "Product I"
        productModel.name = updatedProductName

        val updatedProductStatus = CoreProductStatus.PUBLISH.value
        productModel.status = updatedProductStatus

        val updatedProductVisibility = CoreProductVisibility.HIDDEN.value
        productModel.catalogVisibility = updatedProductVisibility

        val updatedProductFeatured = false
        productModel.featured = updatedProductFeatured

        val updatedProductSlug = "product-slug"
        productModel.slug = updatedProductSlug

        val updatedProductReviewsAllowed = true
        productModel.reviewsAllowed = updatedProductReviewsAllowed

        val updatedProductVirtual = false
        productModel.virtual = updatedProductVirtual

        val updateProductPurchaseNote = "Test purchase note"
        productModel.purchaseNote = updateProductPurchaseNote

        val updatedProductMenuOrder = 5
        productModel.menuOrder = updatedProductMenuOrder

        val updatedGroupedProductIds = "[770,771]"
        productModel.groupedProductIds = updatedGroupedProductIds

        val updatedCrossSellProductIds = "[1,2,3]"
        productModel.crossSellIds = updatedCrossSellProductIds

        val updatedUpsellProductIds = "[1,2,3,4]"
        productModel.upsellIds = updatedUpsellProductIds

        val updatedProductType = "grouped"
        productModel.type = updatedProductType

        nextEvent = TestEvent.UPDATED_PRODUCT
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
            WCProductActionBuilder.newUpdateProductAction(UpdateProductPayload(sSite, productModel))
        )
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        val updatedProduct = productStore.getProductByRemoteId(sSite, productModel.remoteProductId)
        assertNotNull(updatedProduct)
        assertEquals(updatedProductDesc, updatedProduct?.description)
        assertEquals(productModel.remoteProductId, updatedProduct?.remoteProductId)
        assertEquals(updatedProductName, updatedProduct?.name)
        assertEquals(updatedProductStatus, updatedProduct?.status)
        assertEquals(updatedProductVisibility, updatedProduct?.catalogVisibility)
        assertEquals(updatedProductFeatured, updatedProduct?.featured)
        assertEquals(updatedProductSlug, updatedProduct?.slug)
        assertEquals(updatedProductReviewsAllowed, updatedProduct?.reviewsAllowed)
        assertEquals(updatedProductVirtual, updatedProduct?.virtual)
        assertEquals(updateProductPurchaseNote, updatedProduct?.purchaseNote)
        assertEquals(updatedProductMenuOrder, updatedProduct?.menuOrder)
        assertEquals(updatedGroupedProductIds, updatedProduct?.groupedProductIds)
        assertEquals(updatedCrossSellProductIds, updatedProduct?.crossSellIds)
        assertEquals(updatedUpsellProductIds, updatedProduct?.upsellIds)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testUpdateDownloadableFiles() {
        // Prepare the product
        productModel.status = CoreProductStatus.PUBLISH.value
        productModel.catalogVisibility = CoreProductVisibility.VISIBLE.value
        productModel.groupedProductIds = "[]"

        // Update the type
        val updatedProductType = CoreProductType.SIMPLE.value
        productModel.type = updatedProductType

        // Update the downloads attributes
        val updatedProductDownloadableFlag = true
        productModel.downloadable = updatedProductDownloadableFlag

        val downloadableFile = WCProductFileModel(id = null, name = "file", url = "http://url")
        productModel.downloads = JsonArray().apply {
            add(downloadableFile.toJson())
        }.toString()

        val updatedProductDownloadLimit = 10L
        productModel.downloadLimit = updatedProductDownloadLimit

        val updatedProductDownloadExpiry = 365
        productModel.downloadExpiry = updatedProductDownloadExpiry

        nextEvent = TestEvent.UPDATED_PRODUCT
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
            WCProductActionBuilder.newUpdateProductAction(UpdateProductPayload(sSite, productModel))
        )
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        val updatedProduct = productStore.getProductByRemoteId(sSite, productModel.remoteProductId)

        assertEquals(updatedProductType, updatedProduct?.type)
        assertEquals(updatedProductDownloadableFlag, updatedProduct?.downloadable)
        assertEquals(1, updatedProduct?.getDownloadableFiles()?.size)
        assertEquals(downloadableFile.name, updatedProduct?.getDownloadableFiles()?.get(0)?.name)
        assertEquals(downloadableFile.url, updatedProduct?.getDownloadableFiles()?.get(0)?.url)
        assertEquals(updatedProductDownloadLimit, updatedProduct?.downloadLimit)
        assertEquals(updatedProductDownloadExpiry, updatedProduct?.downloadExpiry)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testUpdateVariation() = runBlocking {
        val updatedVariationStatus = CoreProductStatus.PUBLISH.value
        variationModel.status = updatedVariationStatus

        val updatedVariationMenuOrder = 5
        variationModel.menuOrder = updatedVariationMenuOrder

        val updatedVariationRegularPrice = "123"
        variationModel.regularPrice = updatedVariationRegularPrice

        val updatedVariationSalePrice = "12"
        variationModel.salePrice = updatedVariationSalePrice

        withTimeout(TestUtils.DEFAULT_TIMEOUT_MS.toLong()) {
            productStore.updateVariation(UpdateVariationPayload(sSite, variationModel))
        }

        val updatedVariation = productStore.getVariationByRemoteId(
            sSite,
            variationModel.remoteProductId,
            variationModel.remoteVariationId
        )
        assertNotNull(updatedVariation)
        assertEquals(variationModel.remoteProductId, updatedVariation?.remoteProductId)
        assertEquals(updatedVariationStatus, updatedVariation?.status)
        assertEquals(updatedVariationMenuOrder, updatedVariation?.menuOrder)
        assertEquals(updatedVariationRegularPrice, updatedVariation?.regularPrice)
        assertEquals(updatedVariationSalePrice, updatedVariation?.salePrice)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchProductTagsForSite() {
        // Remove all product tags from the database
        ProductSqlUtils.deleteProductTagsForSite(sSite)
        assertEquals(0, ProductSqlUtils.getProductTagsForSite(sSite.id).size)

        nextEvent = TestEvent.FETCHED_PRODUCT_TAGS
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
            WCProductActionBuilder.newFetchProductTagsAction(
                FetchProductTagsPayload(sSite)
            )
        )
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Verify results
        val fetchedTags = productStore.getTagsForSite(sSite)
        assertTrue(fetchedTags.isNotEmpty())
    }

    @Throws(InterruptedException::class)
    @Test
    fun testAddProductTags() {
        // Remove all product tags from the database
        ProductSqlUtils.deleteProductTagsForSite(sSite)
        assertEquals(0, ProductSqlUtils.getProductTagsForSite(sSite.id).size)

        nextEvent = TestEvent.ADDED_PRODUCT_TAGS
        mCountDownLatch = CountDownLatch(1)

        val productTags = listOf("Test" + Date().time, "Test1" + Date().time)
        mDispatcher.dispatch(
            WCProductActionBuilder.newAddProductTagsAction(
                AddProductTagsPayload(sSite, productTags)
            )
        )
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Verify results
        val fetchAllTags = productStore.getProductTagsByNames(sSite, productTags)
        assertTrue(fetchAllTags.isNotEmpty())
        assertTrue(fetchAllTags.size == 2)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testAddNewProduct() {
        val newProductName = "New product name"
        productModel.name = newProductName

        val newProductType = "simple"
        productModel.type = newProductType

        val newProductRegularPrice = "simple"
        productModel.regularPrice = newProductRegularPrice

        val newProductDesc = "New product description"
        productModel.description = newProductDesc

        val newProductShortDesc = "New short desc"
        productModel.shortDescription = newProductShortDesc

        val newProductCategories = "[1,2]"
        productModel.categories = newProductCategories

        val newProductImages = "[image1,image2]"
        productModel.images = newProductImages

        nextEvent = TestEvent.ADDED_PRODUCT
        mCountDownLatch = CountDownLatch(1)

        mDispatcher.dispatch(
            WCProductActionBuilder.newAddedProductAction(
                RemoteAddProductPayload(
                    sSite,
                    ProductWithMetaData(productModel)
                )
            )
        )
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))
    }

    @Test
    fun testFetchProductsCountShouldReturnAllProductsCount() = runBlocking {
        val result = productStore.fetchProductsCount(sSite)

        assertFalse(result.isError)
        assertEquals(42L, result.model)
    }

    /**
     * Used by the update images test to fetch a single media model for this site
     */
    @Throws(InterruptedException::class)
    private fun fetchFirstMedia() {
        mCountDownLatch = CountDownLatch(1)
        val payload = MediaStore.FetchMediaListPayload(sSite, 1, false)
        mDispatcher.dispatch(MediaActionBuilder.newFetchMediaListAction(payload))
        mCountDownLatch.await()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductChanged(event: OnProductChanged) {
        event.error?.let {
            throw AssertionError("OnProductChanged has unexpected error: " + it.type)
        }

        lastEvent = event

        when (event.causeOfChange) {
            WCProductAction.FETCH_PRODUCTS -> {
                assertEquals(TestEvent.FETCHED_PRODUCTS, nextEvent)
                mCountDownLatch.countDown()
            }

            else -> throw AssertionError("Unexpected cause of change: " + event.causeOfChange)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMediaListFetched(event: OnMediaListFetched) {
        event.error?.let {
            throw AssertionError("WCProductTest.onMediaListFetched has unexpected error: ${it.type}, ${it.message}")
        }
        mCountDownLatch.countDown()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductImagesChanged(event: OnProductImagesChanged) {
        event.error?.let {
            throw AssertionError("OnProductImagesChanged has unexpected error: ${it.type}, ${it.message}")
        }

        assertEquals(TestEvent.UPDATED_PRODUCT_IMAGES, nextEvent)
        mCountDownLatch.countDown()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductUpdated(event: OnProductUpdated) {
        event.error?.let {
            throw AssertionError("OnProductUpdated has unexpected error: ${it.type}, ${it.message}")
        }

        assertEquals(TestEvent.UPDATED_PRODUCT, nextEvent)
        mCountDownLatch.countDown()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductPasswordChanged(event: OnProductPasswordChanged) {
        /**
         * For now we don't verify the password here because testUpdateProduct() previously
         * updated the product to have a private status, and setting the password for private
         * products always fails silently (we get HTTP 200, but the password isn't changed).
         * Down the road we can re-enable the password verification.
         */
        if (event.isError) {
            event.error?.let {
                throw AssertionError("onProductPasswordChanged has unexpected error: ${it.type}, ${it.message}")
            }
        } else if (event.causeOfChange == WCProductAction.FETCH_PRODUCT_PASSWORD) {
            assertEquals(TestEvent.FETCHED_PRODUCT_PASSWORD, nextEvent)
            // assertEquals(updatedPassword, event.password)
        } else if (event.causeOfChange == WCProductAction.UPDATE_PRODUCT_PASSWORD) {
            assertEquals(TestEvent.UPDATED_PRODUCT_PASSWORD, nextEvent)
            // assertEquals(updatedPassword, event.password)
        }

        mCountDownLatch.countDown()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductShippingClassesChanged(event: OnProductShippingClassesChanged) {
        event.error?.let {
            throw AssertionError(
                "OnProductShippingClassesChanged has unexpected error: ${it.type}, ${it.message}"
            )
        }

        lastShippingClassEvent = event

        when (event.causeOfChange) {
            WCProductAction.FETCH_SINGLE_PRODUCT_SHIPPING_CLASS -> {
                assertEquals(TestEvent.FETCHED_SINGLE_PRODUCT_SHIPPING_CLASS, nextEvent)
                mCountDownLatch.countDown()
            }

            WCProductAction.FETCH_PRODUCT_SHIPPING_CLASS_LIST -> {
                assertEquals(TestEvent.FETCHED_PRODUCT_SHIPPING_CLASS_LIST, nextEvent)
                mCountDownLatch.countDown()
            }

            else -> throw AssertionError("Unexpected cause of change: " + event.causeOfChange)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductCategoriesChanged(event: OnProductCategoryChanged) {
        event.error?.let {
            throw AssertionError("OnProductCategoryChanged has unexpected error: " + it.type)
        }

        lastProductCategoryEvent = event

        when (event.causeOfChange) {
            WCProductAction.FETCH_PRODUCT_CATEGORIES -> {
                assertEquals(TestEvent.FETCH_PRODUCT_CATEGORIES, nextEvent)
                mCountDownLatch.countDown()
            }

            WCProductAction.ADDED_PRODUCT_CATEGORY -> {
                assertEquals(TestEvent.ADDED_PRODUCT_CATEGORY, nextEvent)
                mCountDownLatch.countDown()
            }

            else -> throw AssertionError("Unexpected cause of change: " + event.causeOfChange)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductTagChanged(event: OnProductTagChanged) {
        event.error?.let {
            throw AssertionError(
                "OnProductTagChanged has unexpected error: ${it.type}, ${it.message}"
            )
        }

        lastProductTagEvent = event

        when (event.causeOfChange) {
            WCProductAction.FETCH_PRODUCT_TAGS -> {
                assertEquals(TestEvent.FETCHED_PRODUCT_TAGS, nextEvent)
                mCountDownLatch.countDown()
            }

            WCProductAction.ADDED_PRODUCT_TAGS -> {
                assertEquals(TestEvent.ADDED_PRODUCT_TAGS, nextEvent)
                mCountDownLatch.countDown()
            }

            else -> throw AssertionError("Unexpected cause of change: " + event.causeOfChange)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductCreated(event: OnProductCreated) {
        event.error?.let {
            throw AssertionError("OnProductCreated has unexpected error: " + it.type)
        }

        lastAddNewProductEvent = event

        when (event.causeOfChange) {
            WCProductAction.ADDED_PRODUCT -> {
                assertEquals(TestEvent.ADDED_PRODUCT, nextEvent)
                assertEquals(event.remoteProductId, productModel.remoteProductId)
                mCountDownLatch.countDown()
            }

            else -> throw AssertionError("Unexpected cause of change: " + event.causeOfChange)
        }
    }
}
