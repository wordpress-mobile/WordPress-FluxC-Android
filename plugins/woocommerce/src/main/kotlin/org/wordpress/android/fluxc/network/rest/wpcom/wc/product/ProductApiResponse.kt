package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.WCMetaData
import org.wordpress.android.fluxc.model.WCMetaData.BundleMetadataKeys.BUNDLE_MAX_SIZE
import org.wordpress.android.fluxc.model.WCMetaData.BundleMetadataKeys.BUNDLE_MIN_SIZE
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.utils.getString
import org.wordpress.android.fluxc.utils.NonNegativeDoubleJsonDeserializer
import org.wordpress.android.fluxc.utils.PrimitiveBooleanJsonDeserializer

typealias ProductDto = ProductApiResponse

@Suppress("ConstructorParameterNaming")
data class ProductApiResponse(
    val id: Long? = null,
    val localSiteId: Int = 0,
    val name: String? = null,
    val slug: String? = null,
    val permalink: String? = null,
    val date_created: String? = null,
    val date_modified: String? = null,
    val type: String? = null,
    val status: String? = null,
    val featured: Boolean = false,
    val catalog_visibility: String? = null,
    val description: String? = null,
    val short_description: String? = null,
    val sku: String? = null,
    val price: String? = null,
    val regular_price: String? = null,
    val sale_price: String? = null,
    val on_sale: Boolean = false,
    val total_sales: Long = 0L,
    @JsonAdapter(PrimitiveBooleanJsonDeserializer::class)
    val purchasable: Boolean = false,
    val virtual: Boolean = false,
    val downloadable: Boolean = false,
    val download_limit: Long = 0L,
    val download_expiry: Int = 0,
    val external_url: String? = null,
    val button_text: String? = null,
    val tax_status: String? = null,
    val tax_class: String? = null,
    val manage_stock: String? = null,
    @JsonAdapter(NonNegativeDoubleJsonDeserializer::class)
    val stock_quantity:Double? = 0.0,
    val stock_status: String? = null,
    val date_on_sale_from: String? = null,
    val date_on_sale_to: String? = null,
    val date_on_sale_from_gmt: String? = null,
    val date_on_sale_to_gmt: String? = null,
    val backorders: String? = null,
    val backorders_allowed:Boolean = false,
    val backordered:Boolean = false,
    @JsonAdapter(PrimitiveBooleanJsonDeserializer::class)
    val sold_individually:Boolean = false,
    val weight: String? = null,
    val dimensions: JsonElement? = null,
    val shipping_required: Boolean = false,
    val shipping_taxable:Boolean = false,
    val shipping_class: String? = null,
    val shipping_class_id:Int = 0,
    val reviews_allowed:Boolean = true,
    val average_rating: String? = null,
    val rating_count:Int = 0,
    val parent_id:Long = 0L,
    val menu_order:Int = 0,
    val purchase_note: String? = null,
    val categories: JsonElement? = null,
    val tags: JsonElement? = null,
    val images: JsonElement? = null,
    val attributes: JsonElement? = null,
    val variations: JsonElement? = null,
    val downloads: JsonElement? = null,
    val related_ids: JsonElement? = null,
    val cross_sell_ids: JsonElement? = null,
    val upsell_ids: JsonElement? = null,
    val grouped_products: JsonElement? = null,
    @SerializedName("meta_data")
    val metadata: JsonArray? = null,
    val bundle_stock_quantity: String? = null,
    val bundle_stock_status: String? = null,
    val bundled_items: JsonArray? = null,
    val composite_components: JsonArray? = null,
    val bundle_min_size: String? = null,
    val bundle_max_size: String? = null,
) {
    @Suppress("LongMethod", "ComplexMethod")
    fun asProductModel(): WCProductModel {
        val response = this
        val isBundledProduct = response.type == CoreProductType.BUNDLE.value
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
            purchasable = response.purchasable

            virtual = response.virtual
            downloadable = response.downloadable
            downloadLimit = response.download_limit
            downloadExpiry = response.download_expiry

            externalUrl = response.external_url ?: ""
            buttonText = response.button_text ?: ""

            taxStatus = response.tax_status ?: ""
            taxClass = response.tax_class ?: ""

            // variations may have "parent" here if inventory is enabled for the parent but not the variation
            manageStock = response.manage_stock?.let {
                it == "true" || it == "parent"
            } ?: false

            stockQuantity = response.stock_quantity ?: 0.0

            stockStatus = response.stock_status ?: ""
            if (isBundledProduct && (response.bundle_stock_status in CoreProductStockStatus.ALL_VALUES).not()) {
                specialStockStatus = response.bundle_stock_status ?: ""
            }

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
            menuOrder = response.menu_order
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
            groupedProductIds = response.grouped_products?.toString() ?: ""
            metadata = if (
                isBundledProduct
                && response.metadata != null
                && (response.bundle_min_size.isNullOrEmpty().not() || response.bundle_max_size.isNullOrEmpty().not())
            ) {
                response.bundle_max_size?.let { value ->
                    WCMetaData.addAsMetadata(response.metadata, BUNDLE_MAX_SIZE, value)
                }
                response.bundle_min_size?.let { value ->
                    WCMetaData.addAsMetadata(response.metadata, BUNDLE_MIN_SIZE, value)
                }
                response.metadata.toString()
            } else {
                response.metadata?.toString() ?: ""
            }
            bundledItems = response.bundled_items?.toString() ?: ""
            compositeComponents = response.composite_components?.toString() ?: ""

            response.dimensions?.asJsonObject?.let { json ->
                length = json.getString("length") ?: ""
                width = json.getString("width") ?: ""
                height = json.getString("height") ?: ""
            }
        }
    }
}
