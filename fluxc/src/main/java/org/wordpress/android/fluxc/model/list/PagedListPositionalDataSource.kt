package org.wordpress.android.fluxc.model.list

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.wordpress.android.fluxc.model.list.datasource.InternalPagedListDataSource

/**
 * A positional data source for [LIST_ITEM].
 *
 * @param dataSource Describes how to take certain actions such as fetching list for the item type [LIST_ITEM].
 */
class PagedListPositionalDataSource<LIST_DESCRIPTOR : ListDescriptor, ITEM_IDENTIFIER : Any, LIST_ITEM: Any>(
    private val dataSource: InternalPagedListDataSource<LIST_DESCRIPTOR, ITEM_IDENTIFIER, LIST_ITEM>
) : PagingSource<Int, LIST_ITEM>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, LIST_ITEM> {
        val index = params.key ?: 0
        val (firstIndex, lastIndex) = when (params) {
            is LoadParams.Prepend -> {
                val lastIndex = index
                val firstIndex = (lastIndex - params.loadSize).coerceAtLeast(0)
                Pair(firstIndex, lastIndex)
            }
            is LoadParams.Append, is LoadParams.Refresh -> {
                val firstIndex = index
                val lastIndex = (firstIndex + params.loadSize).coerceAtMost(dataSource.totalSize)
                Pair(firstIndex, lastIndex)
            }
        }

        val items = if (firstIndex == lastIndex) {
            emptyList()
        } else {
            dataSource.getItemsInRange(firstIndex, lastIndex)
        }
        val previousKey = if (firstIndex == 0) params.key else null
        val nextKey = if (lastIndex < dataSource.totalSize) lastIndex else null

        return LoadResult.Page(items, previousKey, nextKey)
    }

    override fun getRefreshKey(state: PagingState<Int, LIST_ITEM>): Int? {
        return state.anchorPosition
    }
}
