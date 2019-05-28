package org.wordpress.android.fluxc.example

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.util.Log
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersByIdsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrdersFetchedByIds
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCOrderFetcher @Inject constructor(
    private val lifecycle: Lifecycle,
    private val dispatcher: Dispatcher
) : LifecycleObserver {
    private val ongoingRequests = HashSet<RemoteId>()

    init {
        dispatcher.register(this)
        lifecycle.addObserver(this)
    }

    /**
     * Handles the [Lifecycle.Event.ON_DESTROY] event to cleanup the registration for dispatcher and removing the
     * observer for lifecycle.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onDestroy() {
        lifecycle.removeObserver(this)
        dispatcher.unregister(this)
    }

    fun fetchOrders(site: SiteModel, remoteItemIds: List<RemoteId>) {
        val idsToFetch = remoteItemIds.filter {
            // ignore duplicate requests
            !ongoingRequests.contains(it)
        }
        if (idsToFetch.isNotEmpty()) {
            ongoingRequests.addAll(idsToFetch)
            val payload = FetchOrdersByIdsPayload(site = site, remoteIds = idsToFetch)
            dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersByIdsAction(payload))
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onOrdersFetchedById(event: OnOrdersFetchedByIds) {
        if (event.isError) {
            Log.e(WCOrderFetcher::class.java.simpleName,
                    "Error fetching orders by remoteOrderId: ${event.error.message}")
            return
        }
        ongoingRequests.removeAll(event.orderIds)
    }
}
