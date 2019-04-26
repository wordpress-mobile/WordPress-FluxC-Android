package org.wordpress.android.fluxc.example

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ProgressBar
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.fragment_woo_order_list.*
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.WCOrderListItemUIType.LoadingItem
import org.wordpress.android.fluxc.example.WCOrderListItemUIType.WCOrderListUIItem
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.model.list.datasource.ListItemDataSourceInterface
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderListPayload
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout
import javax.inject.Inject

class WCOrderListActivity : AppCompatActivity() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore
    @Inject internal lateinit var wcOrderStore: WCOrderStore
    @Inject internal lateinit var listStore: ListStore

    private var swipeRefreshLayout: CustomSwipeRefreshLayout? = null
    private var progressLoadMore: ProgressBar? = null
    private var pagedListWrapper: PagedListWrapper<WCOrderListItemUIType>? = null
    private val orderListAdapter: OrderListAdapter = OrderListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wc_order_list)

        swipeRefreshLayout = findViewById(R.id.ptr_layout)
        progressLoadMore = findViewById(R.id.progress)

        findViewById<RecyclerView>(R.id.recycler_view)?.apply {
            adapter = orderListAdapter
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    override fun onStart() {
        super.onStart()

        val orderListDescriptor = WCOrderListDescriptor(
                site = wooCommerceStore.getWooCommerceSites()[0], // crash if site is not there
                statusFilter = null,
                searchQuery = "")

        loadList(orderListDescriptor)

        swipeRefreshLayout?.apply {
            setColorSchemeColors(
                    ContextCompat.getColor(this@WCOrderListActivity, android.R.color.holo_blue_bright),
                    ContextCompat.getColor(this@WCOrderListActivity, android.R.color.holo_green_light),
                    ContextCompat.getColor(this@WCOrderListActivity, android.R.color.holo_orange_dark))
            setOnRefreshListener {
                pagedListWrapper?.fetchFirstPage()
            }
        }

        order_search_submit.setOnClickListener {
            val descriptor = WCOrderListDescriptor(
                    site = wooCommerceStore.getWooCommerceSites()[0], // crash if site is not there
                    statusFilter = null,
                    searchQuery = order_search_query.text.toString())
            loadList(descriptor)
        }

        order_search_clear.setOnClickListener {
            order_search_query.text.clear()
            val descriptor = WCOrderListDescriptor(
                    site = wooCommerceStore.getWooCommerceSites()[0], // crash if site is not there
                    statusFilter = null,
                    searchQuery = null)
            loadList(descriptor)
        }
    }

    private fun loadList(descriptor: WCOrderListDescriptor) {
        pagedListWrapper?.apply {
            val lifecycleOwner = this@WCOrderListActivity
            data.removeObservers(lifecycleOwner)
            isLoadingMore.removeObservers(lifecycleOwner)
            isFetchingFirstPage.removeObservers(lifecycleOwner)
            listError.removeObservers(lifecycleOwner)
            isEmpty.removeObservers(lifecycleOwner)
        }

        pagedListWrapper = listStore.getList(
                listDescriptor = descriptor,
                dataSource = WCOrderListItemDataSource(dispatcher, wcOrderStore, lifecycle),
                lifecycle = lifecycle
        ).also { wrapper ->
            wrapper.fetchFirstPage()
            wrapper.isLoadingMore.observe(this, Observer {
                it?.let { isLoadingMore ->
                    progressLoadMore?.visibility = if (isLoadingMore) View.VISIBLE else View.GONE
                }
            })
            wrapper.isFetchingFirstPage.observe(this, Observer {
                swipeRefreshLayout?.isRefreshing = it == true
            })
            wrapper.data.observe(this, Observer {
                it?.let { orderListData ->
                    orderListAdapter.submitList(orderListData)
                }
            })
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
