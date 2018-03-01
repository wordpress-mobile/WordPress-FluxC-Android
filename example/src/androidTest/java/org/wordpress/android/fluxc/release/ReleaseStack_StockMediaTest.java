package org.wordpress.android.fluxc.release;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.junit.Test;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.StockMediaActionBuilder;
import org.wordpress.android.fluxc.model.StockMediaModel;
import org.wordpress.android.fluxc.store.StockMediaStore;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressLint("UseSparseArrays")
public class ReleaseStack_StockMediaTest extends ReleaseStack_WPComBase {
    @Inject
    StockMediaStore mStockMediaStore;

    private enum TestEvents {
        FETCHED_STOCK_MEDIA_LIST_PAGE_ONE,
        FETCHED_STOCK_MEDIA_LIST_PAGE_TWO
    }

    private TestEvents mNextEvent;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        init();
    }

    private static final String SEARCH_TERM = "beach";
    private List<StockMediaModel> mFirstPageMedia;

    @Test
    public void testFetchStockMediaList() throws InterruptedException {
        mNextEvent = TestEvents.FETCHED_STOCK_MEDIA_LIST_PAGE_ONE;
        fetchStockMediaList(SEARCH_TERM, 1);

        mNextEvent = TestEvents.FETCHED_STOCK_MEDIA_LIST_PAGE_TWO;
        fetchStockMediaList(SEARCH_TERM, 2);
    }

    private void fetchStockMediaList(@NonNull String searchTerm, int page) throws InterruptedException {
        StockMediaStore.FetchStockMediaListPayload fetchPayload =
                new StockMediaStore.FetchStockMediaListPayload(searchTerm, page);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(StockMediaActionBuilder.newFetchStockMediaAction(fetchPayload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private boolean isInFirstPageOfResults(@NonNull StockMediaModel media) {
        for (StockMediaModel modelFromFirstPage : mFirstPageMedia) {
            if (modelFromFirstPage.equals(media)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onStockMediaListFetched(StockMediaStore.OnStockMediaListFetched event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: "
                    + event.error.type);
        }

        boolean isPageOne = mNextEvent == TestEvents.FETCHED_STOCK_MEDIA_LIST_PAGE_ONE;
        boolean isPageTwo = mNextEvent == TestEvents.FETCHED_STOCK_MEDIA_LIST_PAGE_TWO;

        assertTrue(isPageOne || isPageTwo);
        assertFalse(event.mediaList == null);
        assertTrue(event.mediaList.size() == StockMediaStore.DEFAULT_NUM_STOCK_MEDIA_PER_FETCH);

        // remember the results if this is the first page, otherwise make sure the second page
        // doesn't contain any of the first page results
        if (isPageOne) {
            mFirstPageMedia = event.mediaList;
        } else {
            for (StockMediaModel media : event.mediaList) {
                assertFalse(isInFirstPageOfResults(media));
            }
        }

        mCountDownLatch.countDown();
    }
}
