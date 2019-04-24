package org.wordpress.android.fluxc.example

import android.arch.paging.PagedListAdapter
import android.support.annotation.LayoutRes
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.fluxc.example.WCOrderListItemUIType.LoadingItem
import org.wordpress.android.fluxc.example.WCOrderListItemUIType.WCOrderListUIItem
import org.wordpress.android.fluxc.utils.DateUtils

private const val VIEW_TYPE_ORDER_ITEM = 0
private const val VIEW_TYPE_LOADING = 1

class OrderListAdapter : PagedListAdapter<WCOrderListItemUIType, ViewHolder>(OrderListDiffItemCallback) {
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is WCOrderListUIItem -> VIEW_TYPE_ORDER_ITEM
            is LoadingItem -> VIEW_TYPE_LOADING
            null -> VIEW_TYPE_LOADING // Placeholder by paged list
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ORDER_ITEM -> WCOrderItemUIViewHolder(R.layout.list_item_woo_order, parent)
            VIEW_TYPE_LOADING -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_skeleton, parent, false)
                LoadingViewHolder(view)
            }
            else -> {
                // Fail fast if a new view type is added so the we can handle it
                throw IllegalStateException("The view type '$viewType' needs to be handled")
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder is WCOrderItemUIViewHolder) {
            val item = getItem(position)
            assert(item is WCOrderListUIItem) {
                "If we are presenting WCOrderItemUIViewHolder, the item has to be of type WCOrderListUIItem " +
                        "for position: $position"
            }
            holder.onBind((item as WCOrderListUIItem))
        }
    }
}

/**
 * 1. It looks like all the items are loading when only some of them are shown which triggers load more very early
 * 2. Whenever the data changes it looks like the whole UI is refreshed regardless of if the item is changed or not
 */

private val OrderListDiffItemCallback = object : DiffUtil.ItemCallback<WCOrderListItemUIType>() {
    override fun areItemsTheSame(oldItem: WCOrderListItemUIType, newItem: WCOrderListItemUIType): Boolean {
        if (oldItem is LoadingItem && newItem is LoadingItem) {
            return oldItem.remoteId == newItem.remoteId
        }
        if (oldItem is WCOrderListUIItem && newItem is WCOrderListUIItem) {
            return oldItem.remoteOrderId == newItem.remoteOrderId
        }
        if (oldItem is LoadingItem && newItem is WCOrderListUIItem) {
            return oldItem.remoteId == newItem.remoteOrderId
        }
        return false
    }

    override fun areContentsTheSame(oldItem: WCOrderListItemUIType, newItem: WCOrderListItemUIType): Boolean {
        if (oldItem is LoadingItem && newItem is LoadingItem) {
            return oldItem.remoteId == newItem.remoteId
        }
        if (oldItem is WCOrderListUIItem && newItem is WCOrderListUIItem) {
            // AS is lying, it's not actually smart casting, so we have to do it :sigh:
            return (oldItem as WCOrderListUIItem) == (newItem as WCOrderListUIItem)
        }
        return false
    }
}

private class WCOrderItemUIViewHolder(
    @LayoutRes layout: Int,
    parentView: ViewGroup
) : RecyclerView.ViewHolder(LayoutInflater.from(parentView.context).inflate(layout, parentView, false)) {
    private val orderNumberTv: TextView = itemView.findViewById(R.id.woo_order_number)
    private val orderDateTv: TextView = itemView.findViewById(R.id.woo_order_date)
    private val orderStatusTv: TextView = itemView.findViewById(R.id.woo_order_status)
    fun onBind(orderUIItem: WCOrderListUIItem) {
        orderNumberTv.text = orderUIItem.orderNumber
        orderDateTv.text = orderUIItem.dateCreated
        orderStatusTv.text = orderUIItem.status
    }
}

private class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view)
