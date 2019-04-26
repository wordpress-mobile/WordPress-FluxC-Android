package org.wordpress.android.fluxc.example

import android.app.Activity
import android.os.Bundle
import dagger.android.AndroidInjection
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WCOrderListActivity : Activity() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_wc_order_list)
    }
}
