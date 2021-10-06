package org.wordpress.android.fluxc.example.ui.customer.search

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_woo_customers_search.*
import org.wordpress.android.fluxc.example.R.layout
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooCustomersSearchFragment : Fragment() {
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore
    @Inject internal lateinit var listStore: ListStore
    @Inject internal lateinit var listItemDataSource: WooCustomersListItemDataSource
    @Inject internal lateinit var customersAdapter: WooCustomersSearchAdapter

    private val siteId by lazy { requireArguments().getInt(KEY_SELECTED_SITE_ID) }
    private val searchParams by lazy { requireArguments().getParcelable(KEY_SEARCH_PARAMS) as SearchParams? }
    private val pagedListWrapper by lazy {
        if (searchParams == null) prependToLog("SearchParams: $searchParams were null.")

        val descriptor = SearchCustomerListDescriptor(
                customerSite = getSelectedSite(),
                customerSearchQuery = searchParams!!.searchQuery,
                customerEmail = searchParams!!.email,
                customerRole = searchParams!!.role,
                customerRemoteCustomerIds = searchParams!!.includeIds,
                customerExcludedCustomerIds = searchParams!!.excludeIds
        )
        listStore.getList(
                listDescriptor = descriptor,
                dataSource = listItemDataSource,
                lifecycle = lifecycle
        )
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(layout.fragment_woo_customers_search, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvCustomersSearch.apply {
            adapter = customersAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        loadList()
    }

    private fun loadList() {
        val lifecycleOwner = viewLifecycleOwner
        with(pagedListWrapper) {
            data.removeObservers(lifecycleOwner)
            isLoadingMore.removeObservers(lifecycleOwner)
            isFetchingFirstPage.removeObservers(lifecycleOwner)
            listError.removeObservers(lifecycleOwner)
            isEmpty.removeObservers(lifecycleOwner)

            data.observe(lifecycleOwner, {
                it?.let { customersAdapter.submitList(it) }
            })

            fetchFirstPage()
        }
    }

    private fun getSelectedSite() = wooCommerceStore.getWooCommerceSites().find { it.id == siteId }!!

    companion object {
        fun newInstance(siteId: Int, searchParams: SearchParams) = WooCustomersSearchFragment().apply {
            arguments = Bundle().apply {
                putInt(KEY_SELECTED_SITE_ID, siteId)
                putParcelable(KEY_SEARCH_PARAMS, searchParams)
            }
        }
    }

    @Parcelize
    data class SearchParams(
        val searchQuery: String?,
        val includeIds: List<Long>,
        val excludeIds: List<Long>,
        val email: String?,
        val role: String?
    ) : Parcelable
}

private const val KEY_SELECTED_SITE_ID = "selected_site_id"
private const val KEY_SEARCH_PARAMS = "search_params"
