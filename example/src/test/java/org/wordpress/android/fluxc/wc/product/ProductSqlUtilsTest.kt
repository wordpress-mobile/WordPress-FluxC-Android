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
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.model.WCProductShippingClassModel
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
                        WCProductCategoryModel::class.java,
                        WCProductShippingClassModel::class.java,
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
    fun testInsertOrUpdateProducts() {
        val site = SiteModel().apply { id = 2 }
        val products = ArrayList<WCProductModel>().apply {
            this.add(ProductTestUtils.generateSampleProduct(40, siteId = site.id))
            this.add(ProductTestUtils.generateSampleProduct(41, siteId = site.id))
            this.add(ProductTestUtils.generateSampleProduct(42, siteId = site.id))
        }

        // Delete all products for this site, then test inserting the above products
        ProductSqlUtils.deleteProductsForSite(site)
        val insertedProductCount = ProductSqlUtils.insertOrUpdateProducts(products)
        assertEquals(3, insertedProductCount)
        val storedProductsCount = ProductSqlUtils.getProductCountForSite(site)
        assertEquals(3, storedProductsCount)
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
    fun testInsertOrUpdateProductShippingClass() {
        val shippingClass = ProductTestUtils.generateProductList(site.id)[0]
        assertNotNull(shippingClass)

        // Test inserting a product shipping class
        var rowsAffected = ProductSqlUtils.insertOrUpdateProductShippingClass(shippingClass)
        assertEquals(1, rowsAffected)
        var savedShippingClassList = ProductSqlUtils
                .getProductShippingClassListForSite(site.id)
        assertEquals(savedShippingClassList.size, 1)
        assertEquals(savedShippingClassList[0].localSiteId, shippingClass.localSiteId)
        assertEquals(savedShippingClassList[0].remoteShippingClassId, shippingClass.remoteShippingClassId)
        assertEquals(savedShippingClassList[0].name, shippingClass.name)
        assertEquals(savedShippingClassList[0].slug, shippingClass.slug)
        assertEquals(savedShippingClassList[0].description, shippingClass.description)

        // Test updating the same product shipping class
        shippingClass.apply {
            name = "Test shipping class"
        }
        rowsAffected = ProductSqlUtils.insertOrUpdateProductShippingClass(shippingClass)
        assertEquals(1, rowsAffected)
        savedShippingClassList = ProductSqlUtils.getProductShippingClassListForSite(site.id)
        assertEquals(savedShippingClassList.size, 1)
        assertEquals(savedShippingClassList[0].localSiteId, shippingClass.localSiteId)
        assertEquals(savedShippingClassList[0].remoteShippingClassId, shippingClass.remoteShippingClassId)
        assertEquals(savedShippingClassList[0].name, shippingClass.name)
        assertEquals(savedShippingClassList[0].slug, shippingClass.slug)
        assertEquals(savedShippingClassList[0].description, shippingClass.description)
    }

    @Test
    fun testInsertOrUpdateProductShippingClassList() {
        val shippingClassList = ProductTestUtils.generateProductList(site.id)
        assertTrue(shippingClassList.isNotEmpty())

        // Insert product shipping class list
        val rowsAffected = ProductSqlUtils.insertOrUpdateProductShippingClassList(shippingClassList)
        assertEquals(shippingClassList.size, rowsAffected)
    }

    @Test
    fun testGetProductShippingClassListForSite() {
        val shippingClassList = ProductTestUtils.generateProductList(site.id)
        assertTrue(shippingClassList.isNotEmpty())

        // Insert product shipping class list
        val rowsAffected = ProductSqlUtils.insertOrUpdateProductShippingClassList(shippingClassList)
        assertEquals(shippingClassList.size, rowsAffected)

        // Get shipping class list for site and verify
        val savedShippingClassListExists = ProductSqlUtils.getProductShippingClassListForSite(site.id)
        assertEquals(shippingClassList.size, savedShippingClassListExists.size)

        // Get shipping class list for a site that does not exist
        val nonExistingSite = SiteModel().apply { id = 400 }
        val savedShippingClassList = ProductSqlUtils.getProductShippingClassListForSite(nonExistingSite.id)
        assertEquals(0, savedShippingClassList.size)
    }

    @Test
    fun testGetProductShippingClassByRemoteShippingId() {
        val shippingClass = ProductTestUtils.generateSampleProductShippingClass(
                remoteId = 40, siteId = site.id
        )

        // Insert product shipping class list
        val rowsAffected = ProductSqlUtils.insertOrUpdateProductShippingClass(shippingClass)
        assertEquals(1, rowsAffected)

        // Get shipping class for site and remoteId and verify
        val savedShippingClassExists = ProductSqlUtils.getProductShippingClassByRemoteId(
                shippingClass.remoteShippingClassId, site.id
        )
        assertEquals(shippingClass.remoteShippingClassId, savedShippingClassExists?.remoteShippingClassId)
        assertEquals(shippingClass.name, savedShippingClassExists?.name)
        assertEquals(shippingClass.description, savedShippingClassExists?.description)
        assertEquals(shippingClass.slug, savedShippingClassExists?.slug)
        assertEquals(shippingClass.localSiteId, savedShippingClassExists?.localSiteId)

        // Get shipping class for a site that does not exist
        val nonExistingSite = SiteModel().apply { id = 400 }
        val savedShippingClass = ProductSqlUtils.getProductShippingClassByRemoteId(
                25, nonExistingSite.id
        )
        assertNull(savedShippingClass)

        // Get shipping class for a site that does not exist
        val nonExistingRemoteId = 25L
        val nonExistentShippingClass = ProductSqlUtils.getProductShippingClassByRemoteId(
                nonExistingRemoteId, site.id
        )
        assertNull(nonExistentShippingClass)
    }

    @Test
    fun testDeleteProductShippingListForSite() {
        val shippingClassList = ProductTestUtils.generateProductList(site.id)

        var rowsAffected = ProductSqlUtils.insertOrUpdateProductShippingClassList(shippingClassList)
        assertEquals(shippingClassList.size, rowsAffected)

        // Verify products inserted
        var savedShippingClassList = ProductSqlUtils.getProductShippingClassListForSite(site.id)
        assertEquals(shippingClassList.size, savedShippingClassList.size)

        // Delete shipping class list for site and verify
        rowsAffected = ProductSqlUtils.deleteProductShippingClassListForSite(site)
        assertEquals(shippingClassList.size, rowsAffected)
        savedShippingClassList = ProductSqlUtils.getProductShippingClassListForSite(site.id)
        assertEquals(0, savedShippingClassList.size)
    }

    @Test
    fun testDeleteSiteDeletesProductShippingClassList() {
        val shippingClassList = ProductTestUtils.generateProductList(site.id)
        assertTrue(shippingClassList.isNotEmpty())

        val rowsAffected = ProductSqlUtils.insertOrUpdateProductShippingClassList(shippingClassList)
        assertEquals(shippingClassList.size, rowsAffected)

        // Verify products inserted
        var savedShippingClassList = ProductSqlUtils.getProductShippingClassListForSite(site.id)
        assertEquals(shippingClassList.size, savedShippingClassList.size)

        // Delete site and verify shipping class list  deleted via foreign key constraint
        SiteSqlUtils.deleteSite(site)
        savedShippingClassList = ProductSqlUtils.getProductShippingClassListForSite(site.id)
        assertEquals(0, savedShippingClassList.size)
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
    fun testGetProductsForSiteWithFilterOptions() {
        val productFilterOptions = mapOf(
                ProductFilterOption.STOCK_STATUS to "instock",
                ProductFilterOption.STATUS to "publish",
                ProductFilterOption.TYPE to "simple"
        )

        val product1 = ProductTestUtils.generateSampleProduct(40)
        val product2 = ProductTestUtils.generateSampleProduct(41)
        val product3 = ProductTestUtils.generateSampleProduct(42, stockStatus = "onbackorder")

        ProductSqlUtils.insertOrUpdateProduct(product1)
        ProductSqlUtils.insertOrUpdateProduct(product2)
        ProductSqlUtils.insertOrUpdateProduct(product3)

        val site = SiteModel().apply { id = product1.localSiteId }
        val products = ProductSqlUtils.getProductsByFilterOptions(site, productFilterOptions)
        assertEquals(2, products.size)

        // insert products with the same productId but for a different site
        val differentSiteProduct1 = ProductTestUtils.generateSampleProduct(40, siteId = 10)
        val differentSiteProduct2 = ProductTestUtils.generateSampleProduct(
                41, siteId = 10, type = "grouped"
        )
        val differentSiteProduct3 = ProductTestUtils.generateSampleProduct(
                2, siteId = 10, status = "pending"
        )

        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct1)
        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct2)
        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct3)

        // verify that the products for the first site is still 2
        assertEquals(2, ProductSqlUtils.getProductsByFilterOptions(site, productFilterOptions).size)

        // verify that the products for the second site is 3
        val site2 = SiteModel().apply { id = differentSiteProduct1.localSiteId }
        val differentSiteProducts = ProductSqlUtils.getProductsByFilterOptions(site2, productFilterOptions)
        assertEquals(1, differentSiteProducts.size)
    }

    @Test
    fun testInsertOrUpdateProductReview() {
        val review = getProductReviews(site.id)[0]
        assertNotNull(review)

        // Test inserting a product review
        var rowsAffected = ProductSqlUtils.insertOrUpdateProductReview(review)
        assertEquals(1, rowsAffected)
        var savedReview = ProductSqlUtils.getProductReviewByRemoteId(
                site.id, review.remoteProductReviewId
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
                site.id, review.remoteProductReviewId
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

    @Test
    fun testInsertOrUpdateProductCategory() {
        val category = getProductCategories(site.id)[0]
        assertNotNull(category)

        // Test inserting a product category
        var rowsAffected = ProductSqlUtils.insertOrUpdateProductCategory(category)
        assertEquals(1, rowsAffected)
        var savedCategory = ProductSqlUtils.getProductCategoryByRemoteId(
                site.id, category.remoteCategoryId
        )
        assertNotNull(savedCategory)
        assertEquals(category.remoteCategoryId, savedCategory.remoteCategoryId)
        assertEquals(category.name, savedCategory.name)
        assertEquals(category.slug, savedCategory.slug)
        assertEquals(category.parent, savedCategory.parent)
        assertEquals(category.localSiteId, savedCategory.localSiteId)

        // Test updating the same product category
        category.apply {
            name = "foo"
        }
        rowsAffected = ProductSqlUtils.insertOrUpdateProductCategory(category)
        assertEquals(1, rowsAffected)
        savedCategory = ProductSqlUtils.getProductCategoryByRemoteId(
                site.id, category.remoteCategoryId
        )
        assertNotNull(savedCategory)
        assertEquals(category.name, savedCategory.name)
    }

    @Test
    fun testInsertOrUpdateProductCategories() {
        val productCategories = getProductCategories(site.id)
        assertTrue(productCategories.isNotEmpty())

        // Insert all product categories
        val rowsAffected = ProductSqlUtils.insertOrUpdateProductCategories(productCategories)
        assertEquals(productCategories.size, rowsAffected)
    }

    @Test
    fun testGetProductCategoriesForSite() {
        val categories = getProductCategories(site.id)
        assertTrue(categories.isNotEmpty())

        // Insert all product categories
        val rowsAffected = ProductSqlUtils.insertOrUpdateProductCategories(categories)
        assertEquals(categories.size, rowsAffected)

        // Get all product categories for site and verify
        val savedCategoriesExist = ProductSqlUtils.getProductCategoriesForSite(site)
        assertEquals(categories.size, savedCategoriesExist.size)

        // Get all product categories for a site that do not exist
        val savedCategories = ProductSqlUtils.getProductCategoriesForSite(SiteModel().apply { id = 400 })
        assertEquals(0, savedCategories.size)
    }

    @Test
    fun testDeleteAllProductCategories() {
        val categories = getProductCategories(site.id)
        assertTrue(categories.isNotEmpty())
        var rowsAffected = ProductSqlUtils.insertOrUpdateProductCategories(categories)
        assertEquals(categories.size, rowsAffected)

        // Verify categories inserted
        var savedCategories = ProductSqlUtils.getProductCategoriesForSite(site)
        assertEquals(categories.size, savedCategories.size)

        // Delete all categories and verify
        rowsAffected = ProductSqlUtils.deleteAllProductCategories()
        assertEquals(categories.size, rowsAffected)
        savedCategories = ProductSqlUtils.getProductCategoriesForSite(site)
        assertEquals(0, savedCategories.size)
    }

    private fun getProductCategories(localSiteId: Int): List<WCProductCategoryModel> {
        val categoryJson = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/product-categories.json")
        return ProductTestUtils.getProductCategoriesFromJsonString(categoryJson, localSiteId)
    }
}
