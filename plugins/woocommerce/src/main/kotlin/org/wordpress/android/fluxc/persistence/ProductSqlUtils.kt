package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCProductModelTable
import com.wellsql.generated.WCProductReviewModelTable
import com.wellsql.generated.WCProductVariationModelTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.store.WCProductStore.Companion.DEFAULT_PRODUCT_SORTING
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.DATE_ASC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.DATE_DESC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.TITLE_ASC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.TITLE_DESC

object ProductSqlUtils {
    fun insertOrUpdateProduct(product: WCProductModel): Int {
        val productResult = WellSql.select(WCProductModel::class.java)
                .where().beginGroup()
                .equals(WCProductModelTable.ID, product.id)
                .or()
                .beginGroup()
                .equals(WCProductModelTable.REMOTE_PRODUCT_ID, product.remoteProductId)
                .equals(WCProductModelTable.LOCAL_SITE_ID, product.localSiteId)
                .endGroup()
                .endGroup().endWhere()
                .asModel.firstOrNull()

        return if (productResult == null) {
            // Insert
            WellSql.insert(product).asSingleTransaction(true).execute()
            1
        } else {
            // Update
            val oldId = productResult.id
            WellSql.update(WCProductModel::class.java).whereId(oldId)
                    .put(product, UpdateAllExceptId(WCProductModel::class.java)).execute()
        }
    }

    fun insertOrUpdateProducts(products: List<WCProductModel>): Int {
        var rowsAffected = 0
        products.forEach {
            rowsAffected += insertOrUpdateProduct(it)
        }
        return rowsAffected
    }

    fun getProductByRemoteId(site: SiteModel, remoteProductId: Long): WCProductModel? {
        return WellSql.select(WCProductModel::class.java)
                .where().beginGroup()
                .equals(WCProductModelTable.REMOTE_PRODUCT_ID, remoteProductId)
                .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
                .endGroup().endWhere()
                .asModel.firstOrNull()
    }

    fun getProductsByRemoteIds(site: SiteModel, remoteProductIds: List<Long>): List<WCProductModel> {
        return WellSql.select(WCProductModel::class.java)
                .where().beginGroup()
                .isIn(WCProductModelTable.REMOTE_PRODUCT_ID, remoteProductIds)
                .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
                .endGroup().endWhere()
                .asModel
    }

    fun geProductExistsByRemoteId(site: SiteModel, remoteProductId: Long): Boolean {
        return WellSql.select(WCProductModel::class.java)
                .where().beginGroup()
                .equals(WCProductModelTable.REMOTE_PRODUCT_ID, remoteProductId)
                .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
                .endGroup().endWhere()
                .exists()
    }

    fun getProductsForSite(
        site: SiteModel,
        sortType: ProductSorting = DEFAULT_PRODUCT_SORTING
    ): List<WCProductModel> {
        val sortOrder = when (sortType) {
            TITLE_ASC, DATE_ASC -> SelectQuery.ORDER_ASCENDING
            TITLE_DESC, DATE_DESC -> SelectQuery.ORDER_DESCENDING
        }
        val sortField = when (sortType) {
            TITLE_ASC, TITLE_DESC -> WCProductModelTable.NAME
            DATE_ASC, DATE_DESC -> WCProductModelTable.DATE_CREATED
        }
        return WellSql.select(WCProductModel::class.java)
                .where()
                .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .orderBy(sortField, sortOrder)
                .asModel
    }

    fun deleteProductsForSite(site: SiteModel): Int {
        return WellSql.delete(WCProductModel::class.java)
                .where().beginGroup()
                .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
                .endGroup()
                .endWhere()
                .execute()
    }

    fun insertOrUpdateProductVariation(variation: WCProductVariationModel): Int {
        val result = WellSql.select(WCProductVariationModel::class.java)
                .where().beginGroup()
                .equals(WCProductVariationModelTable.ID, variation.id)
                .or()
                .beginGroup()
                .equals(WCProductVariationModelTable.REMOTE_PRODUCT_ID, variation.remoteProductId)
                .equals(WCProductVariationModelTable.REMOTE_VARIATION_ID, variation.remoteVariationId)
                .equals(WCProductVariationModelTable.LOCAL_SITE_ID, variation.localSiteId)
                .endGroup()
                .endGroup().endWhere()
                .asModel.firstOrNull()

        return if (result == null) {
            // Insert
            WellSql.insert(variation).asSingleTransaction(true).execute()
            1
        } else {
            // Update
            val oldId = result.id
            WellSql.update(WCProductVariationModel::class.java).whereId(oldId)
                    .put(variation, UpdateAllExceptId(WCProductVariationModel::class.java)).execute()
        }
    }

    fun insertOrUpdateProductVariations(variations: List<WCProductVariationModel>): Int {
        var rowsAffected = 0
        variations.forEach {
            rowsAffected += insertOrUpdateProductVariation(it)
        }
        return rowsAffected
    }

    fun getVariationsForProduct(site: SiteModel, remoteProductId: Long): List<WCProductVariationModel> {
        return WellSql.select(WCProductVariationModel::class.java)
                .where()
                .beginGroup()
                .equals(WCProductVariationModelTable.REMOTE_PRODUCT_ID, remoteProductId)
                .equals(WCProductVariationModelTable.LOCAL_SITE_ID, site.id)
                .endGroup().endWhere()
                .orderBy(WCProductVariationModelTable.DATE_CREATED, SelectQuery.ORDER_DESCENDING)
                .asModel
    }

    fun deleteVariationsForProduct(site: SiteModel, remoteProductId: Long): Int {
        return WellSql.delete(WCProductVariationModel::class.java)
                .where().beginGroup()
                .equals(WCProductVariationModelTable.LOCAL_SITE_ID, site.id)
                .equals(WCProductVariationModelTable.REMOTE_PRODUCT_ID, remoteProductId)
                .endGroup()
                .endWhere()
                .execute()
    }

    fun getProductCountForSite(site: SiteModel): Long {
        return WellSql.select(WCProductModel::class.java)
                .where()
                .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .count()
    }

    fun insertOrUpdateProductReviews(productReviews: List<WCProductReviewModel>): Int {
        var rowsAffected = 0
        productReviews.forEach {
            rowsAffected += insertOrUpdateProductReview(it)
        }
        return rowsAffected
    }

    fun insertOrUpdateProductReview(productReview: WCProductReviewModel): Int {
        val result = WellSql.select(WCProductReviewModel::class.java)
                .where().beginGroup()
                .equals(WCProductReviewModelTable.ID, productReview.id)
                .or()
                .beginGroup()
                .equals(WCProductReviewModelTable.REMOTE_PRODUCT_REVIEW_ID, productReview.remoteProductReviewId)
                .equals(WCProductReviewModelTable.LOCAL_SITE_ID, productReview.localSiteId)
                .endGroup()
                .endGroup().endWhere()
                .asModel.firstOrNull()

        return if (result == null) {
            // Insert
            WellSql.insert(productReview).asSingleTransaction(true).execute()
            1
        } else {
            // Update
            val oldId = result.id
            WellSql.update(WCProductReviewModel::class.java).whereId(oldId)
                    .put(productReview, UpdateAllExceptId(WCProductReviewModel::class.java)).execute()
        }
    }

    fun deleteProductReview(productReview: WCProductReviewModel) =
            WellSql.delete(WCProductReviewModel::class.java)
                    .where()
                    .equals(WCProductReviewModelTable.REMOTE_PRODUCT_REVIEW_ID, productReview.remoteProductReviewId)
                    .endWhere().execute()

    fun getProductReviewByRemoteId(
        localSiteId: Int,
        remoteReviewId: Long
    ): WCProductReviewModel? {
        return WellSql.select(WCProductReviewModel::class.java)
                .where()
                .beginGroup()
                .equals(WCProductReviewModelTable.LOCAL_SITE_ID, localSiteId)
                .equals(WCProductReviewModelTable.REMOTE_PRODUCT_REVIEW_ID, remoteReviewId)
                .endGroup()
                .endWhere()
                .asModel.firstOrNull()
    }

    fun getProductReviewsForSite(site: SiteModel): List<WCProductReviewModel> {
        return WellSql.select(WCProductReviewModel::class.java)
                .where()
                .equals(WCProductReviewModelTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .orderBy(WCProductReviewModelTable.DATE_CREATED, SelectQuery.ORDER_DESCENDING)
                .asModel
    }

    fun getProductReviewsForProductAndSiteId(
        localSiteId: Int,
        remoteProductId: Long
    ): List<WCProductReviewModel> {
        return WellSql.select(WCProductReviewModel::class.java)
                .where().beginGroup()
                .equals(WCProductReviewModelTable.REMOTE_PRODUCT_ID, remoteProductId)
                .equals(WCProductReviewModelTable.LOCAL_SITE_ID, localSiteId)
                .endGroup().endWhere()
                .orderBy(WCProductReviewModelTable.DATE_CREATED, SelectQuery.ORDER_DESCENDING)
                .asModel
    }

    fun deleteAllProductReviewsForSite(site: SiteModel): Int {
        return WellSql.delete(WCProductReviewModel::class.java)
                .where()
                .equals(WCProductReviewModelTable.LOCAL_SITE_ID, site.id)
                .or()
                .equals(WCProductReviewModelTable.LOCAL_SITE_ID, 0) // Should never happen, but sanity cleanup
                .endWhere().execute()
    }

    fun deleteAllProductReviews() = WellSql.delete(WCProductReviewModel::class.java).execute()
}
