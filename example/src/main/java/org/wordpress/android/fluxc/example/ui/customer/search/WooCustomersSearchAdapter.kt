package org.wordpress.android.fluxc.example.ui.customer.search

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlinx.android.synthetic.main.list_item_woo_customer.view.*
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.ui.customer.search.CustomerListItemType.CustomerItem
import org.wordpress.android.fluxc.example.ui.customer.search.CustomerListItemType.LoadingItem
import javax.inject.Inject

private const val VIEW_TYPE_ITEM = 0
private const val VIEW_TYPE_LOADING = 1

class WooCustomersSearchAdapter @Inject constructor(context: Context) :
        PagedListAdapter<CustomerListItemType, ViewHolder>(customerListDiffItemCallback) {
    private val layoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ITEM -> CustomerItemViewHolder(inflateView(R.layout.list_item_woo_customer, parent))
            VIEW_TYPE_LOADING -> LoadingViewHolder(inflateView(R.layout.list_item_skeleton, parent))
            else -> throw IllegalStateException("View type $viewType is not supported")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is CustomerItemViewHolder) {
            holder.onBind((item as CustomerItem))
        }
    }

    private fun inflateView(@LayoutRes layoutId: Int, parent: ViewGroup) = layoutInflater.inflate(
            layoutId,
            parent,
            false
    )

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is CustomerItem -> VIEW_TYPE_ITEM
            is LoadingItem -> VIEW_TYPE_LOADING
            else -> throw IllegalStateException("item at position $position is not supported")
        }
    }

    private class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view)
    private class CustomerItemViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        @SuppressLint("SetTextI18n")
        fun onBind(item: CustomerItem) {
            view.tvCustomerRemoteId.text = item.remoteCustomerId.toString()
            view.tvCustomerName.text = "${item.firstName} ${item.lastName}"
            view.tvCustomerEmail.text = item.email
            view.tvCustomerRole.text = item.role
        }
    }
}

private val customerListDiffItemCallback = object : DiffUtil.ItemCallback<CustomerListItemType>() {
    override fun areItemsTheSame(oldItem: CustomerListItemType, newItem: CustomerListItemType): Boolean {
        if (oldItem == newItem && oldItem is LoadingItem) return true

        if (oldItem is CustomerItem && newItem is CustomerItem) {
            return oldItem.remoteCustomerId == newItem.remoteCustomerId
        }

        return false
    }

    override fun areContentsTheSame(oldItem: CustomerListItemType, newItem: CustomerListItemType): Boolean {
        if (oldItem == newItem && oldItem is LoadingItem) return true

        if (oldItem is CustomerItem && newItem is CustomerItem) {
            return oldItem.firstName == newItem.firstName &&
                    oldItem.lastName == newItem.lastName &&
                    oldItem.email == newItem.email &&
                    oldItem.role == newItem.role
        }
        return false
    }
}

sealed class CustomerListItemType {
    object LoadingItem : CustomerListItemType()
    data class CustomerItem(
        val remoteCustomerId: Long,
        val firstName: String?,
        val lastName: String,
        val email: String,
        val role: String
    ) : CustomerListItemType()
}
