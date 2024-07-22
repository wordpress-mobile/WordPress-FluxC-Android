package org.wordpress.android.fluxc.example.ui.customer.search

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.fluxc.example.MainExampleActivity
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.databinding.ListItemWooCustomerBinding
import org.wordpress.android.fluxc.example.ui.customer.search.CustomerListItemType.CustomerItem
import org.wordpress.android.fluxc.example.ui.customer.search.CustomerListItemType.LoadingItem
import javax.inject.Inject

private const val VIEW_TYPE_ITEM = 0
private const val VIEW_TYPE_LOADING = 1

class WooCustomersSearchAdapter @Inject constructor(
    context: MainExampleActivity
) : PagedListAdapter<CustomerListItemType, RecyclerView.ViewHolder>(customerListDiffItemCallback) {
    private val layoutInflater = LayoutInflater.from(context)

    @Suppress("UseCheckOrError")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ITEM -> {
                CustomerItemViewHolder(
                    ListItemWooCustomerBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            VIEW_TYPE_LOADING -> LoadingViewHolder(inflateView(R.layout.list_item_skeleton, parent))
            else -> throw IllegalStateException("View type $viewType is not supported")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
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
            else -> VIEW_TYPE_LOADING
        }
    }

    private class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view)
    private class CustomerItemViewHolder(
        val binding: ListItemWooCustomerBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun onBind(item: CustomerItem) {
            with(binding) {
                tvCustomerRemoteId.text = "ID: ${item.remoteCustomerId}"
                tvCustomerName.text = "${item.firstName} ${item.lastName}"
                tvCustomerEmail.text = item.email
                tvCustomerRole.text = item.role
            }
        }
    }
}

private val customerListDiffItemCallback = object : DiffUtil.ItemCallback<CustomerListItemType>() {
    override fun areItemsTheSame(oldItem: CustomerListItemType, newItem: CustomerListItemType): Boolean {
        if (oldItem is LoadingItem && newItem is LoadingItem) {
            return oldItem.remoteCustomerId == newItem.remoteCustomerId
        }
        return if (oldItem is CustomerItem && newItem is CustomerItem) {
            oldItem.remoteCustomerId == newItem.remoteCustomerId
        } else {
            false
        }
    }

    override fun areContentsTheSame(oldItem: CustomerListItemType, newItem: CustomerListItemType): Boolean {
        if (oldItem is LoadingItem && newItem is LoadingItem) return true
        return if (oldItem is CustomerItem && newItem is CustomerItem) {
            oldItem.firstName == newItem.firstName &&
                    oldItem.lastName == newItem.lastName &&
                    oldItem.email == newItem.email &&
                    oldItem.role == newItem.role
        } else {
            false
        }
    }
}

sealed class CustomerListItemType {
    data class LoadingItem(val remoteCustomerId: Long) : CustomerListItemType()
    data class CustomerItem(
        val remoteCustomerId: Long,
        val firstName: String?,
        val lastName: String,
        val email: String,
        val role: String
    ) : CustomerListItemType()
}
