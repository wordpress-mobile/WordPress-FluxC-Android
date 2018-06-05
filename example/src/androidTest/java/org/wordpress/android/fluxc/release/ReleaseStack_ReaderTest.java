package org.wordpress.android.fluxc.release;

import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.junit.Test;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.ReaderActionBuilder;
import org.wordpress.android.fluxc.model.ReaderFeedModel;
import org.wordpress.android.fluxc.store.ReaderStore;
import org.wordpress.android.fluxc.store.ReaderStore.OnReaderSitesSearched;
import org.wordpress.android.fluxc.store.ReaderStore.ReaderSearchSitesPayload;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReleaseStack_ReaderTest extends ReleaseStack_Base {
    @Inject ReaderStore mReaderStore;

    private enum TestEvents {
        NONE,
        READER_SEARCH_SITES,
        READER_SEARCH_SITES_WITH_OFFSET
    }

    private List<ReaderFeedModel> mFirstSearchResults;

    private static final String SEARCH_TERM = "dogs";
    private static final int NUM_RESULTS = 10;
    private TestEvents mNextEvent;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);

        init();

        mNextEvent = TestEvents.NONE;
    }

    @Test
    public void testReaderSearchSites() throws InterruptedException {
        mNextEvent = TestEvents.READER_SEARCH_SITES;
        searchReaderSites(SEARCH_TERM, NUM_RESULTS, 0);

        mNextEvent = TestEvents.READER_SEARCH_SITES_WITH_OFFSET;
        searchReaderSites(SEARCH_TERM, NUM_RESULTS, NUM_RESULTS);
    }

    private void searchReaderSites(@NonNull String searchTerm, int count, int offset) throws InterruptedException {
        ReaderSearchSitesPayload payload = new ReaderSearchSitesPayload(searchTerm, count, offset);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(ReaderActionBuilder.newReaderSearchSitesAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private boolean isInFirstSetOfResults(@NonNull ReaderFeedModel feed) {
        for (ReaderFeedModel otherFeed : mFirstSearchResults) {
            if (otherFeed.getFeedId() == feed.getFeedId()) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onReaderSitesSearched(OnReaderSitesSearched event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: "
                                     + event.error.type);
        }

        if (mNextEvent == TestEvents.READER_SEARCH_SITES) {
            mFirstSearchResults = event.feeds;
            assertEquals(event.feeds.size(), NUM_RESULTS);
            assertTrue(event.canLoadMore);
        } else if (mNextEvent == TestEvents.READER_SEARCH_SITES_WITH_OFFSET) {
            List<ReaderFeedModel> feeds = event.feeds;
            boolean areThereDups = false;
            for (ReaderFeedModel feed : event.feeds) {
                if (isInFirstSetOfResults(feed)) {
                    areThereDups = true;
                    break;
                }
            }
            assertFalse(areThereDups);
        } else {
            throw new AssertionError("Wrong event received");
        }

        mCountDownLatch.countDown();
    }
}
