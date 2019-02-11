package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComErrorListener
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequest
import org.wordpress.android.fluxc.store.WCProductStore.ProductError
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductPayload
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

    private fun productResponseToProductModel(response: ProductApiResponse): WCProductModel {
        return WCProductModel().apply {
            remoteProductId = response.id ?: 0
            name = response.name ?: ""
            slug = response.slug ?: ""
            permalink = response.permalink ?: ""

            date_created = response.date_created ?: ""
            date_created_gmt = response.date_created_gmt ?: ""
            date_modified = response.date_modified ?: ""
            date_modified_gmt = response.date_modified_gmt ?: ""

            type = response.type ?: ""
            status = response.status ?: ""
            featured = response.featured
            catalog_visibility = response.catalog_visibility ?: ""
            description = response.description ?: ""
            short_description = response.short_description ?: ""
            sku = response.sku ?: ""

            price = response.price ?: ""
            price_html = response.price_html ?: ""
            regular_price = response.regular_price ?: ""
            sale_price = response.sale_price ?: ""
            date_on_sale_from = response.date_on_sale_from ?: ""
            date_on_sale_from_gmt = response.date_on_sale_from_gmt ?: ""
            date_on_sale_to = response.date_on_sale_to ?: ""
            date_on_sale_to_gmt = response.date_on_sale_to_gmt ?: ""
            on_sale = response.on_sale
            total_sales = response.total_sales

            virtual = response.virtual
            downloadable = response.downloadable
            downloads = response.downloads.toString()
            download_limit = response.download_limit
            download_expiry = response.download_expiry

            external_url = response.external_url ?: ""
            button_text = response.button_text ?: ""

            tax_status = response.tax_status ?: ""
            tax_class = response.tax_class ?: ""

            manage_stock = response.manage_stock
            stock_quantity = response.stock_quantity
            stock_status = response.stock_status ?: ""

            backorders = response.backorders ?: ""
            backorders_allowed = response.backorders_allowed
            backordered = response.backordered
            sold_individually = response.sold_individually
            weight = response.weight ?: ""
            dimensions = response.dimensions.toString()

            shipping_required = response.shipping_required
            shipping_taxable = response.shipping_taxable
            shipping_class = response.shipping_class ?: ""
            shipping_class_id = response.shipping_class_id

            reviews_allowed = response.reviews_allowed
            average_rating = response.average_rating ?: ""
            rating_count = response.rating_count

            related_ids = response.related_ids.toString()
            upsell_ids = response.upsell_ids.toString()
            cross_sell_ids = response.cross_sell_ids.toString()

            parent_id = response.parent_id
            purchase_note = response.purchase_note ?: ""
            menu_order = response.menu_order

            categories = response.categories.toString()
            tags = response.tags.toString()
            images = response.images.toString()
            attributes = response.attributes.toString()
            default_attributes = response.default_attributes.toString()
            variations = response.variations.toString()
            grouped_products = response.grouped_products.toString()
            meta_data = response.meta_data.toString()
        }
    }

    private fun networkErrorToProductError(wpComError: WPComGsonNetworkError): ProductError {
        val productErrorType = when (wpComError.apiError) {
            "rest_invalid_param" -> ProductErrorType.INVALID_PARAM
            else -> ProductErrorType.fromString(wpComError.apiError)
        }
        return ProductError(productErrorType, wpComError.message)
    }
}
