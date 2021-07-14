package org.wordpress.android.fluxc.model.list

import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadResult.Page
import androidx.paging.PagingState
import org.wordpress.android.fluxc.model.list.datasource.InternalPagedListDataSource
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T

class PagedListFactory<LIST_DESCRIPTOR : ListDescriptor, ITEM_IDENTIFIER : Any, LIST_ITEM : Any>(
    private val createDataSource: () -> PagedListPositionalDataSource<LIST_DESCRIPTOR, ITEM_IDENTIFIER, LIST_ITEM>
) {
    private var currentSource: PagedListPositionalDataSource<LIST_DESCRIPTOR, ITEM_IDENTIFIER, LIST_ITEM>? = null

    fun create(): PagedListPositionalDataSource<LIST_DESCRIPTOR, ITEM_IDENTIFIER, LIST_ITEM> {
        val source = createDataSource.invoke()
        currentSource = source
        return source
    }

    fun invalidate() {
        currentSource?.invalidate()
    }
}

/**
 * A positional data source for [LIST_ITEM].
 *
 * @param dataSource Describes how to take certain actions such as fetching list for the item type [LIST_ITEM].
 */
class PagedListPositionalDataSource<LIST_DESCRIPTOR : ListDescriptor, ITEM_IDENTIFIER : Any, LIST_ITEM : Any>(
    private val dataSource: InternalPagedListDataSource<LIST_DESCRIPTOR, ITEM_IDENTIFIER, LIST_ITEM>
) : PagingSource<Int, LIST_ITEM>() {
    init {
        registerInvalidatedCallback {
            AppLog.e(AppLog.T.API, "Invalidated")
        }
    }
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, LIST_ITEM> {
        AppLog.e(AppLog.T.API, "Loading $params: ${params.key} & ${params.loadSize} Thread: ${Thread.currentThread()}")
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
        val previousKey = if (firstIndex == 0) null else firstIndex
        val nextKey = if (lastIndex < dataSource.totalSize) lastIndex else null

        AppLog.e(AppLog.T.API, "$params firstIndex/lastIndex: $firstIndex/$lastIndex")
        AppLog.e(AppLog.T.API, "$params previousKey/nextKey: $previousKey/$nextKey")
        AppLog.e(AppLog.T.API, "$params itemSize: ${items.size}, totalSize: ${dataSource.totalSize}")

        // TODO: This is just a temporary replacement for boundary callback
        if (lastIndex + dataSource.listConfig.prefetchDistance > dataSource.totalSize) {
            dataSource.onItemAtEndLoaded()
        }

        return LoadResult.Page(items, previousKey, nextKey)
    }

    override fun getRefreshKey(state: PagingState<Int, LIST_ITEM>): Int? {
        state.pages.forEachIndexed { index, page ->
          AppLog.e(T.API, "getRefreshKey: page index $index - ${page.prevKey}/${page.nextKey}")
        }

        val position = state.anchorPosition?.let { anchorPosition ->
           val closestPageToAnchor = state.closestPageToPosition(anchorPosition)
           val closestItemToAnchor = state.closestItemToPosition(anchorPosition)
           val pageIndex = state.pages.indexOf(closestPageToAnchor)
           val anchorIndexInPage = closestPageToAnchor?.data?.indexOf(closestItemToAnchor)

           AppLog.e(T.API, "getRefreshKey: pageIndex $pageIndex anchorPosition ${anchorPosition} anchorIndexInPage $anchorIndexInPage")

           closestPageToAnchor?.prevKey?.let { prevKey ->
               anchorIndexInPage?.let { anchorIndexInPage
                   prevKey - dataSource.listConfig.dbPageSize / 2 + anchorIndexInPage.coerceAtLeast(0)
               } ?: prevKey
           }
        }
        AppLog.e(AppLog.T.API, "getRefreshKey: position $position")
        return position
    }
}
