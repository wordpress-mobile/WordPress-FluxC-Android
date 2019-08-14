package org.wordpress.android.fluxc.wc.product

import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class ProductSqlUtilsTest {
    val site = SiteModel().apply {
        email = "test@example.org"
        name = "Test Site"
        siteId = 24
    }

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(
                        WCProductModel::class.java,
                        WCProductReviewModel::class.java,
                        SiteModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE)
        WellSql.init(config)
        config.reset()

        // Insert the site into the db so it's available later for product
        // reviews
        SiteSqlUtils.insertOrUpdateSite(site)
    }

    @Test
    fun testInsertOrUpdateProduct() {
        val productModel = ProductTestUtils.generateSampleProduct(40)
        val site = SiteModel().apply { id = productModel.localSiteId }

        // Test inserting product
        ProductSqlUtils.insertOrUpdateProduct(productModel)
        val storedProductsCount = ProductSqlUtils.getProductCountForSite(site)
        assertEquals(1, storedProductsCount)

        // Test updating product
        val storedProduct = ProductSqlUtils.getProductByRemoteId(site, productModel.remoteProductId)
        storedProduct?.apply {
            name = "Anitaa Test"
            virtual = true
        }
        storedProduct?.also {
            ProductSqlUtils.insertOrUpdateProduct(it)
        }

        val updatedProductsCount = ProductSqlUtils.getProductCountForSite(site)
        assertEquals(1, updatedProductsCount)

        val updatedProduct = ProductSqlUtils.getProductByRemoteId(site, productModel.remoteProductId)
        assertEquals(storedProduct?.id, updatedProduct?.id)
        assertEquals(storedProduct?.name, updatedProduct?.name)
        assertEquals(storedProduct?.virtual, updatedProduct?.virtual)
    }

    @Test
    fun testGetProductsForSite() {
        // insert products for one site
        val site1 = SiteModel().apply { id = 2 }
        val product1 = ProductTestUtils.generateSampleProduct(40, siteId = site1.id)
        ProductSqlUtils.insertOrUpdateProduct(product1)

        // verify that it is stored
        val storedProduct = ProductSqlUtils.getProductByRemoteId(site1, product1.remoteProductId)
        assertEquals(product1.id, storedProduct?.id)
        assertEquals(product1.name, storedProduct?.name)
        assertEquals(product1.virtual, storedProduct?.virtual)

        // insert products for another site
        val site2 = SiteModel().apply { id = 10 }
        val product2 = ProductTestUtils.generateSampleProduct(43, siteId = site2.id)
        ProductSqlUtils.insertOrUpdateProduct(product2)

        // verify that it is stored
        val storedProduct2 = ProductSqlUtils.getProductByRemoteId(site2, product2.remoteProductId)
        assertEquals(product2.id, storedProduct2?.id)
        assertEquals(product2.name, storedProduct2?.name)
        assertEquals(product2.virtual, storedProduct2?.virtual)

        // add another product for site 1
        val product3 = ProductTestUtils.generateSampleProduct(43, siteId = site1.id)
        ProductSqlUtils.insertOrUpdateProduct(product3)

        // verify that the site 2 product size is still the same
        val storedProductForSite2Count = ProductSqlUtils.getProductCountForSite(site2)
        assertEquals(1, storedProductForSite2Count)

        // verify that the site 1 product is increases by 1
        val storedProductForSite1Count = ProductSqlUtils.getProductCountForSite(site1)
        assertEquals(2, storedProductForSite1Count)
    }

    @Test
    fun testGetProductsForSiteAndProductIds() {
        val productIds = listOf<Long>(40, 41, 2)

        val product1 = ProductTestUtils.generateSampleProduct(40)
        val product2 = ProductTestUtils.generateSampleProduct(41)
        val product3 = ProductTestUtils.generateSampleProduct(42)

        ProductSqlUtils.insertOrUpdateProduct(product1)
        ProductSqlUtils.insertOrUpdateProduct(product2)
        ProductSqlUtils.insertOrUpdateProduct(product3)

        val site = SiteModel().apply { id = product1.localSiteId }
        val products = ProductSqlUtils.getProductsByRemoteIds(site, productIds)
        assertEquals(2, products.size)

        // insert products with the same productId but for a different site
        val differentSiteProduct1 = ProductTestUtils.generateSampleProduct(40, siteId = 10)
        val differentSiteProduct2 = ProductTestUtils.generateSampleProduct(41, siteId = 10)
        val differentSiteProduct3 = ProductTestUtils.generateSampleProduct(2, siteId = 10)

        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct1)
        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct2)
        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct3)

        // verify that the products for the first site is still 2
        assertEquals(2, ProductSqlUtils.getProductsByRemoteIds(site, productIds).size)

        // verify that the products for the second site is 3
        val site2 = SiteModel().apply { id = differentSiteProduct1.localSiteId }
        val differentSiteProducts = ProductSqlUtils.getProductsByRemoteIds(site2, productIds)
        assertEquals(3, differentSiteProducts.size)
    }

    @Test
    fun testInsertOrUpdateProductReview() {
        val review = getProductReviews(site.id)[0]
        assertNotNull(review)

        // Test inserting a product review
        var rowsAffected = ProductSqlUtils.insertOrUpdateProductReview(review)
        assertEquals(1, rowsAffected)
        var savedReview = ProductSqlUtils.getProductReviewByRemoteId(
                site.id, review.remoteProductReviewId, review.remoteProductId
        )
        assertNotNull(savedReview)
        assertEquals(review.remoteProductReviewId, savedReview.remoteProductReviewId)
        assertEquals(review.verified, savedReview.verified)
        assertEquals(review.rating, savedReview.rating)
        assertEquals(review.reviewerEmail, savedReview.reviewerEmail)
        assertEquals(review.review, savedReview.review)
        assertEquals(review.reviewerName, savedReview.reviewerName)
        assertEquals(review.remoteProductId, savedReview.remoteProductId)
        assertEquals(review.dateCreated, savedReview.dateCreated)
        assertEquals(review.localSiteId, savedReview.localSiteId)
        assertEquals(review.reviewerAvatarsJson, savedReview.reviewerAvatarsJson)

        // Test updating the same product review
        review.apply {
            verified = !verified
        }
        rowsAffected = ProductSqlUtils.insertOrUpdateProductReview(review)
        assertEquals(1, rowsAffected)
        savedReview = ProductSqlUtils.getProductReviewByRemoteId(
                site.id, review.remoteProductReviewId, review.remoteProductId
        )
        assertNotNull(savedReview)
        assertEquals(review.verified, savedReview.verified)
    }

    @Test
    fun testInsertOrUpdateProductReviews() {
        val reviews = getProductReviews(site.id)
        assertTrue(reviews.isNotEmpty())

        // Insert all product reviews
        val rowsAffected = ProductSqlUtils.insertOrUpdateProductReviews(reviews)
        assertEquals(reviews.size, rowsAffected)
    }

    @Test
    fun testGetProductReviewsForSite() {
        val reviews = getProductReviews(site.id)
        assertTrue(reviews.isNotEmpty())

        // Insert all product reviews
        val rowsAffected = ProductSqlUtils.insertOrUpdateProductReviews(reviews)
        assertEquals(reviews.size, rowsAffected)

        // Get all product reviews for site and verify
        val savedReviewsExists = ProductSqlUtils.getProductReviewsForSite(site)
        assertEquals(reviews.size, savedReviewsExists.size)

        // Get all product reviews for a site that does not exist
        val savedReviews = ProductSqlUtils.getProductReviewsForSite(SiteModel().apply { id = 400 })
        assertEquals(0, savedReviews.size)
    }

    @Test
    fun testGetProductReviewsForProduct() {
        val productId = 18L // should be 3 products in the test products json config
        val reviews = getProductReviews(site.id)
        assertTrue(reviews.isNotEmpty())
        val rowsAffected = ProductSqlUtils.insertOrUpdateProductReviews(reviews)
        assertEquals(reviews.size, rowsAffected)

        // Get all reviews for existing product
        val savedReviewsForProductExisting = ProductSqlUtils
                .getProductReviewsForProductAndSiteId(site.id, productId)
        assertEquals(3, savedReviewsForProductExisting.size)

        // Get all reviews for non-existing product
        val savedReviewsForProduct = ProductSqlUtils
                .getProductReviewsForProductAndSiteId(site.id, 400)
        assertEquals(0, savedReviewsForProduct.size)
    }

    @Test
    fun testDeleteAllProductReviewsForSite() {
        val reviews = getProductReviews(site.id)
        assertTrue(reviews.isNotEmpty())
        var rowsAffected = ProductSqlUtils.insertOrUpdateProductReviews(reviews)
        assertEquals(reviews.size, rowsAffected)

        // Verify products inserted
        var savedReviews = ProductSqlUtils.getProductReviewsForSite(site)
        assertEquals(reviews.size, savedReviews.size)

        // Delete all reviews for site and verify
        rowsAffected = ProductSqlUtils.deleteAllProductReviewsForSite(site)
        assertEquals(reviews.size, rowsAffected)
        savedReviews = ProductSqlUtils.getProductReviewsForSite(site)
        assertEquals(0, savedReviews.size)
    }

    @Test
    fun testDeleteSiteDeletesAllProductReviews() {
        val reviews = getProductReviews(site.id)
        assertTrue(reviews.isNotEmpty())
        val rowsAffected = ProductSqlUtils.insertOrUpdateProductReviews(reviews)
        assertEquals(reviews.size, rowsAffected)

        // Verify products inserted
        var savedReviews = ProductSqlUtils.getProductReviewsForSite(site)
        assertEquals(reviews.size, savedReviews.size)

        // Delete site and verify reviews deleted via foreign key constraint
        SiteSqlUtils.deleteSite(site)
        savedReviews = ProductSqlUtils.getProductReviewsForSite(site)
        assertEquals(0, savedReviews.size)
    }

    @Test
    fun testDeleteAllProductReviews() {
        val reviews = getProductReviews(site.id)
        assertTrue(reviews.isNotEmpty())
        var rowsAffected = ProductSqlUtils.insertOrUpdateProductReviews(reviews)
        assertEquals(reviews.size, rowsAffected)

        // Verify products inserted
        var savedReviews = ProductSqlUtils.getProductReviewsForSite(site)
        assertEquals(reviews.size, savedReviews.size)

        // Delete all reviews and verify
        rowsAffected = ProductSqlUtils.deleteAllProductReviews()
        assertEquals(reviews.size, rowsAffected)
        savedReviews = ProductSqlUtils.getProductReviewsForSite(site)
        assertEquals(0, savedReviews.size)
    }

    private fun getProductReviews(localSiteId: Int): List<WCProductReviewModel> {
        val reviewJson = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/product-reviews.json")
        return ProductTestUtils.getProductReviewsFromJsonString(reviewJson, localSiteId)
    }
}
