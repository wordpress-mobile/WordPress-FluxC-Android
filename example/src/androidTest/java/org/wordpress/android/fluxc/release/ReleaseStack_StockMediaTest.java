package org.wordpress.android.fluxc.release;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.junit.Test;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.StockMediaActionBuilder;
import org.wordpress.android.fluxc.model.StockMediaModel;
import org.wordpress.android.fluxc.store.StockMediaStore;
import org.wordpress.android.fluxc.store.StockMediaStore.FetchStockMediaListPayload;
import org.wordpress.android.fluxc.store.StockMediaStore.OnStockMediaListFetched;
import org.wordpress.android.util.AppLog;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.wordpress.android.fluxc.network.rest.wpcom.stockmedia.StockMediaRestClient
        .DEFAULT_NUM_STOCK_MEDIA_PER_FETCH;

public class ReleaseStack_StockMediaTest extends ReleaseStack_WPComBase {
    @Inject StockMediaStore mStockMediaStore;

    private enum TestEvents {
        NONE,
        FETCHED_STOCK_MEDIA_LIST_PAGE_ONE,
        FETCHED_STOCK_MEDIA_LIST_PAGE_TWO
    }

    private TestEvents mNextEvent;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);

        init();

        mNextEvent = TestEvents.NONE;
    }

    private static final String SEARCH_TERM = "dogs";

    private List<StockMediaModel> mFirstPageMedia;

    @Test
    public void testFetchStockMediaList() throws InterruptedException {
        mNextEvent = TestEvents.FETCHED_STOCK_MEDIA_LIST_PAGE_ONE;
        fetchStockMediaList(SEARCH_TERM, 1);

        mNextEvent = TestEvents.FETCHED_STOCK_MEDIA_LIST_PAGE_TWO;
        fetchStockMediaList(SEARCH_TERM, 2);
    }

    private void fetchStockMediaList(@NonNull String searchTerm, int page) throws InterruptedException {
        FetchStockMediaListPayload fetchPayload = new FetchStockMediaListPayload(searchTerm, page);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(StockMediaActionBuilder.newFetchStockMediaAction(fetchPayload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private boolean isInFirstPageOfResults(@NonNull StockMediaModel media) {
        for (StockMediaModel modelFromFirstPage : mFirstPageMedia) {
            if (modelFromFirstPage.equals(media)) {
                AppLog.w(AppLog.T.MEDIA, "Found dup stock media with ID " + media.getId()
                        + " (" + modelFromFirstPage.getId() + ")");
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onStockMediaListFetched(OnStockMediaListFetched event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: "
                    + event.error.type);
        }

        boolean isPageOne = event.nextPage == 2;
        boolean isPageTwo = event.nextPage == 3;

        if (isPageOne) {
            assertEquals(mNextEvent, TestEvents.FETCHED_STOCK_MEDIA_LIST_PAGE_ONE);
        } else if (isPageTwo) {
            assertEquals(mNextEvent, TestEvents.FETCHED_STOCK_MEDIA_LIST_PAGE_TWO);
        } else {
            throw new AssertionError("Wrong next page received: " + event.nextPage);
        }
        assertEquals(event.mediaList.size(), DEFAULT_NUM_STOCK_MEDIA_PER_FETCH);

        // remember the results if this is the first page, otherwise make sure the second page
        // isn't the same as the first page (note that dups between pages are possible)
        if (isPageOne) {
            mFirstPageMedia = event.mediaList;
        } else {
            boolean areBothPagesTheSame = true;
            for (StockMediaModel media : event.mediaList) {
                if (!isInFirstPageOfResults(media)) {
                    areBothPagesTheSame = false;
                    break;
                }
            }
            assertFalse(areBothPagesTheSame);
        }

        mCountDownLatch.countDown();
    }
}
