package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.GENERIC_ERROR
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.Locale
import javax.inject.Inject

class WCProductStore @Inject constructor(dispatcher: Dispatcher, private val wcProductRestClient: ProductRestClient)
    : Store(dispatcher) {
    companion object {
        const val NUM_PRODUCTS_PER_FETCH = 25
    }

    class FetchSingleProductPayload(
        var site: SiteModel,
        var remoteProductId: Long
    ) : Payload<BaseNetworkError>()

    enum class ProductErrorType {
        INVALID_PARAM,
        GENERIC_ERROR;

        companion object {
            private val reverseMap = ProductErrorType.values().associateBy(ProductErrorType::name)
            fun fromString(type: String) = reverseMap[type.toUpperCase(Locale.US)] ?: GENERIC_ERROR
        }
    }

    class ProductError(val type: ProductErrorType = GENERIC_ERROR, val message: String = "") : OnChangedError

    class RemoteProductPayload(
        val product: WCProductModel,
        val site: SiteModel
    ) : Payload<ProductError>() {
        constructor(error: ProductError, product: WCProductModel, site: SiteModel) : this(product, site) { this.error = error }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        TODO("not implemented")
    }

    override fun onRegister() = AppLog.d(T.API, "WCProductStore onRegister")

    private fun fetchSingleProduct(payload: FetchSingleProductPayload) {
        with(payload) { wcProductRestClient.fetchSingleProduct(site, remoteProductId) }
    }
}
