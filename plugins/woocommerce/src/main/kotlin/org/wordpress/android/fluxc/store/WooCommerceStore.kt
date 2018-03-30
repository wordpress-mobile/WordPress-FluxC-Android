package org.wordpress.android.fluxc.store

import com.wellsql.generated.SiteModelTable
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooCommerceStore @Inject constructor(dispatcher: Dispatcher) : Store(dispatcher) {
    override fun onRegister() {
        AppLog.d(T.API, "WooCommerceStore onRegister")
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {}

    fun getWooCommerceSites(): MutableList<SiteModel> =
            SiteSqlUtils.getSitesWith(SiteModelTable.HAS_WOO_COMMERCE, true).asModel
}
