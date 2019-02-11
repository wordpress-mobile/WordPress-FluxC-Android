package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import javax.inject.Inject

class WCProductStore @Inject constructor(dispatcher: Dispatcher, private val wcProductRestClient: ProductRestClient)
    : Store(dispatcher) {
    companion object {
        const val NUM_PRODUCTS_PER_FETCH = 25
    }

    override fun onAction(action: Action<*>?) {
        TODO("not implemented")
    }

    override fun onRegister() {
        TODO("not implemented")
    }
}
