package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.JsonArray
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.ProductWithMetaData
import org.wordpress.android.fluxc.model.StripProductMetaData
import org.wordpress.android.fluxc.model.WCMetaData
import org.wordpress.android.fluxc.model.WCMetaData.BundleMetadataKeys.BUNDLE_MAX_SIZE
import org.wordpress.android.fluxc.model.WCMetaData.BundleMetadataKeys.BUNDLE_MIN_SIZE
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.utils.getString
import javax.inject.Inject

class ProductDtoMapper @Inject constructor(
    private val stripProductMetaData: StripProductMetaData
) {
    fun mapToModel(localSiteId: LocalOrRemoteId.LocalId, dto: ProductDto): ProductWithMetaData {
        val model = WCProductModel().apply {
            this.localSiteId = localSiteId.value
            remoteProductId = dto.id ?: 0
            name = dto.name ?: ""
            slug = dto.slug ?: ""
            permalink = dto.permalink ?: ""

            dateCreated = dto.date_created ?: ""
            dateModified = dto.date_modified ?: ""

            dateOnSaleFrom = dto.date_on_sale_from ?: ""
            dateOnSaleTo = dto.date_on_sale_to ?: ""
            dateOnSaleFromGmt = dto.date_on_sale_from_gmt ?: ""
            dateOnSaleToGmt = dto.date_on_sale_to_gmt ?: ""

            type = dto.type ?: ""
            status = dto.status ?: ""
            featured = dto.featured
            catalogVisibility = dto.catalog_visibility ?: ""
            description = dto.description ?: ""
            shortDescription = dto.short_description ?: ""
            sku = dto.sku ?: ""

            price = dto.price ?: ""
            regularPrice = dto.regular_price ?: ""
            salePrice = dto.sale_price ?: ""
            onSale = dto.on_sale
            totalSales = dto.total_sales
            purchasable = dto.purchasable

            virtual = dto.virtual
            downloadable = dto.downloadable
            downloadLimit = dto.download_limit
            downloadExpiry = dto.download_expiry

            externalUrl = dto.external_url ?: ""
            buttonText = dto.button_text ?: ""

            taxStatus = dto.tax_status ?: ""
            taxClass = dto.tax_class ?: ""

            // variations may have "parent" here if inventory is enabled for the parent but not the variation
            manageStock = dto.manage_stock?.let {
                it == "true" || it == "parent"
            } ?: false

            stockQuantity = dto.stock_quantity ?: 0.0

            stockStatus = dto.stock_status ?: ""

            backorders = dto.backorders ?: ""
            backordersAllowed = dto.backorders_allowed
            backordered = dto.backordered
            soldIndividually = dto.sold_individually
            weight = dto.weight ?: ""

            shippingRequired = dto.shipping_required
            shippingTaxable = dto.shipping_taxable
            shippingClass = dto.shipping_class ?: ""
            shippingClassId = dto.shipping_class_id

            reviewsAllowed = dto.reviews_allowed
            averageRating = dto.average_rating ?: ""
            ratingCount = dto.rating_count

            parentId = dto.parent_id
            menuOrder = dto.menu_order
            purchaseNote = dto.purchase_note ?: ""

            categories = dto.categories?.toString() ?: ""
            tags = dto.tags?.toString() ?: ""
            images = dto.images?.toString() ?: ""
            attributes = dto.attributes?.toString() ?: ""
            variations = dto.variations?.toString() ?: ""
            downloads = dto.downloads?.toString() ?: ""
            relatedIds = dto.related_ids?.toString() ?: ""
            crossSellIds = dto.cross_sell_ids?.toString() ?: ""
            upsellIds = dto.upsell_ids?.toString() ?: ""
            groupedProductIds = dto.grouped_products?.toString() ?: ""
            bundledItems = dto.bundled_items?.toString() ?: ""
            compositeComponents = dto.composite_components?.toString() ?: ""
            minAllowedQuantity = dto.min_quantity?.toInt() ?: -1
            maxAllowedQuantity = dto.max_quantity?.let {
                if (it.isEmpty()) "0" else it
            }?.toInt() ?: -1
            groupOfQuantity = dto.group_of_quantity?.toInt() ?: -1

            combineVariationQuantities = dto.combine_variations?.let {
                it == "yes"
            } ?: false

            metadata = stripProductMetaData(dto.metadata?.toString() ?: "")
            isSampleProduct = dto.metadata?.any {
                val metaDataEntry = if (it.isJsonObject) it.asJsonObject else null
                metaDataEntry?.let { json ->
                    json.getString(WCMetaData.KEY) == "_headstart_post"
                            && json.getString(WCMetaData.VALUE) == "_hs_extra"
                } ?: false
            } ?: false

            dto.dimensions?.asJsonObject?.let { json ->
                length = json.getString("length") ?: ""
                width = json.getString("width") ?: ""
                height = json.getString("height") ?: ""
            }
        }

        return ProductWithMetaData(
            product = dto.applyBundledProductChanges(model),
            metaData = dto.metadata?.mapNotNull {
                runCatching { WCMetaData.fromJson(it.asJsonObject) }.getOrNull()
            } ?: emptyList()
        )
    }

    private fun ProductApiResponse.applyBundledProductChanges(model: WCProductModel): WCProductModel {
        if (this.type == CoreProductType.BUNDLE.value) {
            val response = this
            if ((response.bundle_stock_status in CoreProductStockStatus.ALL_VALUES).not()) {
                model.specialStockStatus = response.bundle_stock_status ?: ""
            }
            val hasBundleMinQuantityRule = response.bundle_min_size.isNullOrEmpty().not()
            val hasBundleMaxQuantityRule = response.bundle_max_size.isNullOrEmpty().not()
            val hasBundleQuantityRules = hasBundleMinQuantityRule || hasBundleMaxQuantityRule

            if (hasBundleQuantityRules) {
                val metadata = response.metadata ?: JsonArray()
                response.bundle_max_size?.let { value ->
                    WCMetaData.addAsMetadata(metadata, BUNDLE_MAX_SIZE, value)
                }
                response.bundle_min_size?.let { value ->
                    WCMetaData.addAsMetadata(metadata, BUNDLE_MIN_SIZE, value)
                }
                model.metadata = metadata.toString()
            }
        }
        return model
    }
}
