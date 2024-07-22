package org.wordpress.android.fluxc.example

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dagger.android.AndroidInjection
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.TimeGroup.GROUP_FUTURE
import org.wordpress.android.fluxc.example.TimeGroup.GROUP_OLDER_MONTH
import org.wordpress.android.fluxc.example.TimeGroup.GROUP_OLDER_TWO_DAYS
import org.wordpress.android.fluxc.example.TimeGroup.GROUP_OLDER_WEEK
import org.wordpress.android.fluxc.example.TimeGroup.GROUP_TODAY
import org.wordpress.android.fluxc.example.TimeGroup.GROUP_YESTERDAY
import org.wordpress.android.fluxc.example.WCOrderListItemIdentifier.OrderIdentifier
import org.wordpress.android.fluxc.example.WCOrderListItemIdentifier.SectionHeaderIdentifier
import org.wordpress.android.fluxc.example.WCOrderListItemUIType.LoadingItem
import org.wordpress.android.fluxc.example.WCOrderListItemUIType.SectionHeader
import org.wordpress.android.fluxc.example.WCOrderListItemUIType.WCOrderListUIItem
import org.wordpress.android.fluxc.example.databinding.ActivityWcOrderListBinding
import org.wordpress.android.fluxc.example.utils.DateUtils
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.WCOrderSummaryModel
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.model.list.datasource.ListItemDataSourceInterface
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.WCOrderFetcher
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderListPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderSummariesFetched
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import javax.inject.Inject

class WCOrderListActivity : AppCompatActivity() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore
    @Inject internal lateinit var wcOrderStore: WCOrderStore
    @Inject internal lateinit var listStore: ListStore

    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var progressLoadMore: ProgressBar? = null
    private var pagedListWrapper: PagedListWrapper<WCOrderListItemUIType>? = null
    private val orderListAdapter: OrderListAdapter = OrderListAdapter()

    private lateinit var binding: ActivityWcOrderListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        binding = ActivityWcOrderListBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        dispatcher.register(this)

        val orderListDescriptor = WCOrderListDescriptor(
                site = wooCommerceStore.getWooCommerceSites()[0], // crash if site is not there
                statusFilter = null,
                searchQuery = ""
        )

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

        binding.orderSearchSubmit.setOnClickListener {
            val descriptor = WCOrderListDescriptor(
                    site = wooCommerceStore.getWooCommerceSites()[0], // crash if site is not there
                    statusFilter = binding.orderFilter.text.toString(),
                    searchQuery = binding.orderSearchQuery.text.toString(),
                    excludeFutureOrders = binding.excludeFutureOrders.isChecked
            )
            loadList(descriptor)
        }

        binding.orderSearchClear.setOnClickListener {
            binding.orderSearchQuery.text.clear()
            binding.orderFilter.text.clear()
            binding.excludeFutureOrders.isChecked = false

            val descriptor = WCOrderListDescriptor(
                    site = wooCommerceStore.getWooCommerceSites()[0], // crash if site is not there
                    excludeFutureOrders = binding.excludeFutureOrders.isChecked
            )
            loadList(descriptor)
        }
    }

    override fun onStop() {
        super.onStop()
        dispatcher.unregister(this)
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
                dataSource = WCOrderListItemDataSource(dispatcher, wcOrderStore),
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

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onOrderSummariesFetched(event: OnOrderSummariesFetched) {
        AppLog.d(T.TESTS, "Received list changed event. Total duration = ${event.duration}.")
    }
}

sealed class WCOrderListItemIdentifier {
    class SectionHeaderIdentifier(val title: TimeGroup) : WCOrderListItemIdentifier()
    class OrderIdentifier(val orderId: Long) : WCOrderListItemIdentifier()
}

sealed class WCOrderListItemUIType {
    class SectionHeader(val title: TimeGroup) : WCOrderListItemUIType()
    class LoadingItem(val orderId: Long) : WCOrderListItemUIType()
    data class WCOrderListUIItem(
        val orderId: Long,
        val orderNumber: String,
        val status: String,
        val orderName: String,
        val orderTotal: String,
        val dateCreated: String
    ) : WCOrderListItemUIType()

    @Suppress("ComplexMethod")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        return if (this is SectionHeader && other is SectionHeader) {
            this.title == other.title
        } else if (this is LoadingItem && other is LoadingItem) {
            this.orderId == other.orderId
        } else if (this is WCOrderListUIItem && other is WCOrderListUIItem) {
            this.orderId == other.orderId &&
                    this.status == other.status &&
                    this.dateCreated == other.dateCreated &&
                    this.orderNumber == other.orderNumber &&
                    this.orderTotal == other.orderTotal &&
                    this.orderName == other.orderName
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

enum class TimeGroup {
    GROUP_FUTURE,
    GROUP_TODAY,
    GROUP_YESTERDAY,
    GROUP_OLDER_TWO_DAYS,
    GROUP_OLDER_WEEK,
    GROUP_OLDER_MONTH;

    companion object {
        @Suppress("MagicNumber")
        fun getTimeGroupForDate(date: Date): TimeGroup {
            val dateToday = Date()
            return when {
                date.after(DateTimeUtils.nowUTC()) -> GROUP_FUTURE
                date < DateUtils.addMonths(dateToday, -1) -> GROUP_OLDER_MONTH
                date < DateUtils.addWeeks(dateToday, -1) -> GROUP_OLDER_WEEK
                date < DateUtils.addDays(dateToday, -2) -> GROUP_OLDER_TWO_DAYS
                DateUtils.isSameDay(DateUtils.addDays(dateToday, -2), date) -> GROUP_OLDER_TWO_DAYS
                DateUtils.isSameDay(DateUtils.addDays(dateToday, -1), date) -> GROUP_YESTERDAY
                else -> GROUP_TODAY
            }
        }
    }
}

private class WCOrderListItemDataSource(
    val dispatcher: Dispatcher,
    val wcOrderStore: WCOrderStore
) : ListItemDataSourceInterface<WCOrderListDescriptor, WCOrderListItemIdentifier, WCOrderListItemUIType> {
    private val fetcher = WCOrderFetcher(dispatcher)

    override fun getItemsAndFetchIfNecessary(
        listDescriptor: WCOrderListDescriptor,
        itemIdentifiers: List<WCOrderListItemIdentifier>
    ): List<WCOrderListItemUIType> {
        val remoteItemIds = itemIdentifiers.mapNotNull { (it as? OrderIdentifier)?.orderId }
        val ordersMap = wcOrderStore.getOrdersForDescriptor(listDescriptor, remoteItemIds)
        // Fetch missing items
        fetcher.fetchOrders(
                site = listDescriptor.site,
                orderIds = remoteItemIds.filter { !ordersMap.containsKey(it) }
        )

        val mapSummary = { orderId: Long ->
            ordersMap[orderId].let { order ->
                if (order == null) {
                    LoadingItem(orderId)
                } else {
                    WCOrderListUIItem(
                            orderId = order.orderId,
                            orderNumber = order.number,
                            status = order.status,
                            orderName = "${order.billingFirstName} ${order.billingLastName}",
                            orderTotal = order.total,
                            dateCreated = order.dateCreated)
                }
            }
        }
        return itemIdentifiers.map { identifier ->
            when (identifier) {
                is OrderIdentifier -> mapSummary(identifier.orderId)
                is SectionHeaderIdentifier -> SectionHeader(title = identifier.title)
            }
        }
    }

    @Suppress("ComplexMethod")
    override fun getItemIdentifiers(
        listDescriptor: WCOrderListDescriptor,
        itemIds: List<RemoteId>,
        isListFullyFetched: Boolean
    ): List<WCOrderListItemIdentifier> {
        val orderSummaries = wcOrderStore.getOrderSummariesByRemoteOrderIds(
                listDescriptor.site,
                itemIds.map { it.value }
        )
        .let { summariesByRemoteId ->
            itemIds.mapNotNull { summariesByRemoteId[it] }
        }

        val listFuture = mutableListOf<OrderIdentifier>()
        val listToday = mutableListOf<OrderIdentifier>()
        val listYesterday = mutableListOf<OrderIdentifier>()
        val listTwoDays = mutableListOf<OrderIdentifier>()
        val listWeek = mutableListOf<OrderIdentifier>()
        val listMonth = mutableListOf<OrderIdentifier>()
        val mapToRemoteOrderIdentifier = { summary: WCOrderSummaryModel ->
            OrderIdentifier(summary.orderId)
        }
        orderSummaries.forEach {
            // Default to today if the date cannot be parsed
            val date: Date = DateTimeUtils.dateUTCFromIso8601(it.dateCreated) ?: DateTimeUtils.nowUTC()

            // Check if future-dated orders should be excluded from the results list.
            if (listDescriptor.excludeFutureOrders) {
                val currentUtcDate = DateTimeUtils.nowUTC()
                if (date.after(currentUtcDate)) {
                    // This order is dated for the future so skip adding it to the list
                    return@forEach
                }
            }

            when (TimeGroup.getTimeGroupForDate(date)) {
                GROUP_FUTURE -> listFuture.add(mapToRemoteOrderIdentifier(it))
                GROUP_TODAY -> listToday.add(mapToRemoteOrderIdentifier(it))
                GROUP_YESTERDAY -> listYesterday.add(mapToRemoteOrderIdentifier(it))
                GROUP_OLDER_TWO_DAYS -> listTwoDays.add(mapToRemoteOrderIdentifier(it))
                GROUP_OLDER_WEEK -> listWeek.add(mapToRemoteOrderIdentifier(it))
                GROUP_OLDER_MONTH -> listMonth.add(mapToRemoteOrderIdentifier(it))
            }
        }

        val allItems = mutableListOf<WCOrderListItemIdentifier>()
        if (listFuture.isNotEmpty()) {
            allItems += listOf(SectionHeaderIdentifier(GROUP_FUTURE)) + listFuture
        }

        if (listToday.isNotEmpty()) {
            allItems += listOf(SectionHeaderIdentifier(GROUP_TODAY)) + listToday
        }
        if (listYesterday.isNotEmpty()) {
            allItems += listOf(SectionHeaderIdentifier(GROUP_YESTERDAY)) + listYesterday
        }
        if (listTwoDays.isNotEmpty()) {
            allItems += listOf(SectionHeaderIdentifier(GROUP_OLDER_TWO_DAYS)) + listTwoDays
        }
        if (listWeek.isNotEmpty()) {
            allItems += listOf(SectionHeaderIdentifier(GROUP_OLDER_WEEK)) + listWeek
        }
        if (listMonth.isNotEmpty()) {
            allItems += listOf(SectionHeaderIdentifier(GROUP_OLDER_MONTH)) + listMonth
        }
        return allItems
    }

    override fun fetchList(listDescriptor: WCOrderListDescriptor, offset: Long) {
        val fetchOrderListPayload = FetchOrderListPayload(listDescriptor, offset)
        dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderListAction(fetchOrderListPayload))
    }
}
