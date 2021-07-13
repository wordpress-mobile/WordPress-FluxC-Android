package org.wordpress.android.fluxc.list

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.paging.DataSource.InvalidatedCallback
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.list.PagedListFactory
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
internal class PagedListFactoryTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    val dispatcher = TestCoroutineDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `create factory triggers create data source`() {
        val mockCreateDataSource = mock<() -> TestInternalPagedListDataSource>()
        whenever(mockCreateDataSource.invoke()).thenReturn(mock())
        val pagedListFactory = PagedListFactory(mockCreateDataSource, mock())

        pagedListFactory.create()

        verify(mockCreateDataSource, times(1)).invoke()
    }

    @Test
    fun `invalidate triggers create data source`() = runBlocking {
        val mockCreateDataSource = mock<() -> TestInternalPagedListDataSource>()
        whenever(mockCreateDataSource.invoke()).thenReturn(mock())
        val invalidatedCallback = mock<InvalidatedCallback>()

        val pagedListFactory = PagedListFactory(mockCreateDataSource, initCoroutineEngine())
        val currentSource = pagedListFactory.create()
        currentSource.addInvalidatedCallback(invalidatedCallback)

        pagedListFactory.invalidate()

        verify(invalidatedCallback, times(1)).onInvalidated()
    }
}
