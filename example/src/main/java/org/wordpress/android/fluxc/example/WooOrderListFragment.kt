package org.wordpress.android.fluxc.example

import android.arch.lifecycle.Observer
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import dagger.android.support.AndroidSupportInjection
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.WCOrderListItemUIType.LoadingItem
import org.wordpress.android.fluxc.example.WCOrderListItemUIType.WCOrderListUIItem
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.list.datasource.ListItemDataSourceInterface
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderListPayload
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout
import javax.inject.Inject

class WooOrderListFragment : Fragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore
    @Inject internal lateinit var wcOrderStore: WCOrderStore
    @Inject internal lateinit var listStore: ListStore

    private var swipeToRefreshHelper: SwipeToRefreshHelper? = null

    private var swipeRefreshLayout: CustomSwipeRefreshLayout? = null
    private var progressLoadMore: ProgressBar? = null

    private val orderListAdapter: OrderListAdapter = OrderListAdapter()

    private val orderListDescriptor by lazy {
        WCOrderListDescriptor(
                site = wooCommerceStore.getWooCommerceSites()[0], // crash if site is not there
                statusFilter = null
        )
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_woo_order_list, container, false)

//        swipeRefreshLayout = view.findViewById(R.id.ptr_layout)
//        progressLoadMore = view.findViewById(R.id.progress)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)

        recyclerView?.layoutManager = LinearLayoutManager(context)
        // TODO: Add item decoration
        recyclerView?.adapter = orderListAdapter

        // TODO: create the swipe to refresh layout

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pagedListWrapper = listStore.getList(
                listDescriptor = orderListDescriptor,
                dataSource = WCOrderListItemDataSource(dispatcher, wcOrderStore),
                lifecycle = lifecycle
        )
//        pagedListWrapper.fetchFirstPage()
        pagedListWrapper.isLoadingMore.observe(this, Observer {
            it?.let { isLoadingMore ->
                progressLoadMore?.visibility = if (isLoadingMore) View.VISIBLE else View.GONE
            }
        })
        pagedListWrapper.data.observe(this, Observer {
            it?.let { orderListData ->
                orderListAdapter.submitList(orderListData)
            }
        })
    }
}

sealed class WCOrderListItemUIType {
    class LoadingItem(val remoteId: RemoteId) : WCOrderListItemUIType()
    data class WCOrderListUIItem(
        val remoteOrderId: RemoteId,
        val orderNumber: String,
        val status: String,
        val dateCreated: String
    ) : WCOrderListItemUIType()
}

private class WCOrderListItemDataSource(
    val dispatcher: Dispatcher,
    val wcOrderStore: WCOrderStore
) : ListItemDataSourceInterface<WCOrderListDescriptor, RemoteId, WCOrderListItemUIType> {
    override fun getItemsAndFetchIfNecessary(
        listDescriptor: WCOrderListDescriptor,
        itemIdentifiers: List<RemoteId>
    ): List<WCOrderListItemUIType> {
        val ordersMap = wcOrderStore.getOrdersForDescriptor(listDescriptor, itemIdentifiers)
        return itemIdentifiers.map { remoteId ->
            ordersMap[remoteId].let { order ->
                // TODO: Fetch missing items (also talk about why we need to prevent duplicate requests)
                if (order == null) {
                    LoadingItem(remoteId)
                } else {
                    WCOrderListUIItem(
                            remoteOrderId = RemoteId(order.remoteOrderId),
                            orderNumber = order.number,
                            status = order.status,
                            dateCreated = order.dateCreated
                    )
                }
            }
        }
    }

    override fun getItemIdentifiers(
        listDescriptor: WCOrderListDescriptor,
        remoteItemIds: List<RemoteId>,
        isListFullyFetched: Boolean
    ): List<RemoteId> = remoteItemIds

    override fun fetchList(listDescriptor: WCOrderListDescriptor, offset: Long) {
        val fetchOrderListPayload = FetchOrderListPayload(listDescriptor, offset)
        dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderListAction(fetchOrderListPayload))
    }
}
