package org.wordpress.android.fluxc.model.list

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.wordpress.android.fluxc.model.list.datasource.InternalPagedListDataSource
import kotlin.math.min

///**
// * A [DataSource.Factory] instance for `ListStore` lists.
// *
// * @param createDataSource A function that creates an instance of [InternalPagedListDataSource].
// */
//class PagedListFactory<LIST_DESCRIPTOR : ListDescriptor, ITEM_IDENTIFIER: Any, LIST_ITEM: Any>(
//    private val createDataSource: () -> InternalPagedListDataSource<LIST_DESCRIPTOR, ITEM_IDENTIFIER, LIST_ITEM>
//) : DataSource.Factory<Int, LIST_ITEM>() {
//    private var currentSource: PagedListPositionalDataSource<LIST_DESCRIPTOR, ITEM_IDENTIFIER, LIST_ITEM>? = null
//
//    override fun create(): DataSource<Int, LIST_ITEM> {
//        val source = PagedListPositionalDataSource(dataSource = createDataSource.invoke())
//        currentSource = source
//        return source
//    }
//
//    fun invalidate() {
//        currentSource?.invalidate()
//    }
//}

/**
 * A positional data source for [LIST_ITEM].
 *
 * @param dataSource Describes how to take certain actions such as fetching list for the item type [LIST_ITEM].
 */
class PagedListPositionalDataSource<LIST_DESCRIPTOR : ListDescriptor, ITEM_IDENTIFIER : Any, LIST_ITEM: Any>(
    private val dataSource: InternalPagedListDataSource<LIST_DESCRIPTOR, ITEM_IDENTIFIER, LIST_ITEM>
) : PagingSource<Int, LIST_ITEM>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, LIST_ITEM> {
        val startPosition = params.key ?: 0
        val endPosition = min(dataSource.totalSize, startPosition + params.loadSize)
        val items = if (startPosition == endPosition) {
            emptyList()
        } else {
            dataSource.getItemsInRange(startPosition, endPosition)
        }
        return LoadResult.Page(items, startPosition, startPosition + items.size)
    }

    override fun getRefreshKey(state: PagingState<Int, LIST_ITEM>): Int? {
        return state.anchorPosition
    }
}
