package org.wordpress.android.fluxc.example

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.Observer
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
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

    private var swipeRefreshLayout: CustomSwipeRefreshLayout? = null
    private var progressLoadMore: ProgressBar? = null

    private val orderListAdapter: OrderListAdapter = OrderListAdapter()

    private val orderListDescriptor by lazy {
        WCOrderListDescriptor(
                site = wooCommerceStore.getWooCommerceSites()[0], // crash if site is not there
                statusFilter = null,
                searchQuery = ""
        )
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_woo_order_list, container, false)

        swipeRefreshLayout = view.findViewById(R.id.ptr_layout)
//        progressLoadMore = view.findViewById(R.id.progress)

        view.findViewById<RecyclerView>(R.id.recycler_view)?.apply {
            adapter = orderListAdapter
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pagedListWrapper = listStore.getList(
                listDescriptor = orderListDescriptor,
                dataSource = WCOrderListItemDataSource(dispatcher, wcOrderStore, lifecycle),
                lifecycle = lifecycle
        )
        pagedListWrapper.fetchFirstPage()
        pagedListWrapper.isLoadingMore.observe(this, Observer {
            it?.let { isLoadingMore ->
                progressLoadMore?.visibility = if (isLoadingMore) View.VISIBLE else View.GONE
            }
        })
        pagedListWrapper.isFetchingFirstPage.observe(this, Observer {
            swipeRefreshLayout?.isRefreshing = it == true
        })
        pagedListWrapper.data.observe(this, Observer {
            it?.let { orderListData ->
                orderListAdapter.submitList(orderListData)
            }
        })

        swipeRefreshLayout?.apply {
            activity?.let { act ->
                setColorSchemeColors(
                        ContextCompat.getColor(act, android.R.color.holo_blue_bright),
                        ContextCompat.getColor(act, android.R.color.holo_green_light),
                        ContextCompat.getColor(act, android.R.color.holo_orange_dark))
            }
            setOnRefreshListener {
                pagedListWrapper.fetchFirstPage()
            }
        }
    }
}

sealed class WCOrderListItemUIType {
    class LoadingItem(val remoteId: RemoteId) : WCOrderListItemUIType()
    data class WCOrderListUIItem(
        val remoteOrderId: RemoteId,
        val orderNumber: String,
        val status: String,
        val orderName: String,
        val orderTotal: String
    ) : WCOrderListItemUIType()
}

private class WCOrderListItemDataSource(
    val dispatcher: Dispatcher,
    val wcOrderStore: WCOrderStore,
    val lifecycle: Lifecycle
) : ListItemDataSourceInterface<WCOrderListDescriptor, RemoteId, WCOrderListItemUIType> {
    private val fetcher = WCOrderFetcher(lifecycle, dispatcher)

    override fun getItemsAndFetchIfNecessary(
        listDescriptor: WCOrderListDescriptor,
        itemIdentifiers: List<RemoteId>
    ): List<WCOrderListItemUIType> {
        val ordersMap = wcOrderStore.getOrdersForDescriptor(listDescriptor, itemIdentifiers)
        // Fetch missing items
        fetcher.fetchOrders(
                site = listDescriptor.site,
                remoteItemIds = itemIdentifiers.filter { !ordersMap.containsKey(it) }
        )

        return itemIdentifiers.map { remoteId ->
            ordersMap[remoteId].let { order ->
                if (order == null) {
                    LoadingItem(remoteId)
                } else {
                    WCOrderListUIItem(
                            remoteOrderId = RemoteId(order.remoteOrderId),
                            orderNumber = order.number,
                            status = order.status,
                            orderName = "${order.billingFirstName} ${order.billingLastName}",
                            orderTotal = order.total
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
