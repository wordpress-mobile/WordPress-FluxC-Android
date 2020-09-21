package org.wordpress.android.fluxc.release

import org.greenrobot.eventbus.Subscribe
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.generated.StockMediaActionBuilder
import org.wordpress.android.fluxc.model.StockMediaModel
import org.wordpress.android.fluxc.network.rest.wpcom.stockmedia.StockMediaRestClient
import org.wordpress.android.fluxc.release.ReleaseStack_StockMediaTest.TestEvents.FETCHED_STOCK_MEDIA_LIST_PAGE_ONE
import org.wordpress.android.fluxc.release.ReleaseStack_StockMediaTest.TestEvents.FETCHED_STOCK_MEDIA_LIST_PAGE_TWO
import org.wordpress.android.fluxc.release.ReleaseStack_StockMediaTest.TestEvents.NONE
import org.wordpress.android.fluxc.store.StockMediaStore
import org.wordpress.android.fluxc.store.StockMediaStore.FetchStockMediaListPayload
import org.wordpress.android.fluxc.store.StockMediaStore.OnStockMediaListFetched
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MEDIA
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

class ReleaseStack_StockMediaTest : ReleaseStack_WPComBase() {
    @Inject lateinit var stockMediaStore: StockMediaStore

    private enum class TestEvents {
        NONE, FETCHED_STOCK_MEDIA_LIST_PAGE_ONE, FETCHED_STOCK_MEDIA_LIST_PAGE_TWO
    }

    private var mNextEvent: TestEvents? = null
    @Throws(Exception::class) override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        init()
        mNextEvent = NONE
    }

    private var mFirstPageMedia: List<StockMediaModel>? = null
    @Test @Throws(InterruptedException::class) fun testFetchStockMediaList() {
        mNextEvent = FETCHED_STOCK_MEDIA_LIST_PAGE_ONE
        fetchStockMediaList(SEARCH_TERM, 1)
        mNextEvent = FETCHED_STOCK_MEDIA_LIST_PAGE_TWO
        fetchStockMediaList(SEARCH_TERM, 2)
    }

    @Throws(InterruptedException::class) private fun fetchStockMediaList(searchTerm: String, page: Int) {
        val fetchPayload = FetchStockMediaListPayload(searchTerm, page)
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(StockMediaActionBuilder.newFetchStockMediaAction(fetchPayload))
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))
    }

    private fun isInFirstPageOfResults(media: StockMediaModel): Boolean {
        for (modelFromFirstPage in mFirstPageMedia!!) {
            if (modelFromFirstPage == media) {
                AppLog.w(
                        MEDIA, "Found dup stock media with ID " + media.id
                        + " (" + modelFromFirstPage.id + ")"
                )
                return true
            }
        }
        return false
    }

    @Subscribe
    fun onStockMediaListFetched(event: OnStockMediaListFetched) {
        if (event.isError) {
            throw AssertionError(
                    "Unexpected error occurred with type: "
                            + event.error!!.type
            )
        }
        val isPageOne = event.nextPage == 2
        val isPageTwo = event.nextPage == 3
        if (isPageOne) {
            assertEquals(mNextEvent, FETCHED_STOCK_MEDIA_LIST_PAGE_ONE)
        } else if (isPageTwo) {
            assertEquals(mNextEvent, FETCHED_STOCK_MEDIA_LIST_PAGE_TWO)
        } else {
            throw AssertionError("Wrong next page received: " + event.nextPage)
        }
        assertEquals(
                event.mediaList.size,
                StockMediaRestClient.DEFAULT_NUM_STOCK_MEDIA_PER_FETCH
        )

        // remember the results if this is the first page, otherwise make sure the second page
        // isn't the same as the first page (note that dups between pages are possible)
        if (isPageOne) {
            mFirstPageMedia = event.mediaList
        } else {
            var areBothPagesTheSame = true
            for (media in event.mediaList) {
                if (!isInFirstPageOfResults(media)) {
                    areBothPagesTheSame = false
                    break
                }
            }
            Assert.assertFalse(areBothPagesTheSame)
        }
        mCountDownLatch.countDown()
    }

    companion object {
        private const val SEARCH_TERM = "dogs"
    }
}
