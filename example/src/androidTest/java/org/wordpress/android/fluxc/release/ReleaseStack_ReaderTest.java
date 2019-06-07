package org.wordpress.android.fluxc.release;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.junit.Test;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.ReaderActionBuilder;
import org.wordpress.android.fluxc.model.ReaderSiteModel;
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
        READER_SEARCH_SITES_WITH_OFFSET,
        READER_SEARCH_SITES_WITH_EMPTY_RESULTS
    }

    private List<ReaderSiteModel> mFirstSearchResults;

    private static final String SEARCH_TERM = "dog";
    private static final String SEARCH_TERM_EMPTY_RESULTS = "asdjkgadahjsdgjadgajhdgajhjkasdkjgasdjgasjdgadjhgagdj";
    private static final int NUM_RESULTS = 10;
    private TestEvents mNextEvent;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        init();
        mFirstSearchResults = null;
        mNextEvent = TestEvents.NONE;
    }

    @Test
    public void testReaderSearchSites() throws InterruptedException {
        searchReaderSites(TestEvents.READER_SEARCH_SITES, SEARCH_TERM, NUM_RESULTS, 0);

        int offset = NUM_RESULTS + 1;
        searchReaderSites(TestEvents.READER_SEARCH_SITES_WITH_OFFSET, SEARCH_TERM, NUM_RESULTS, offset);
    }

    @Test
    public void testReaderSearchSitesWithEmptyResults() throws InterruptedException {
        searchReaderSites(TestEvents.READER_SEARCH_SITES_WITH_EMPTY_RESULTS, SEARCH_TERM_EMPTY_RESULTS, NUM_RESULTS, 0);
    }

    private void searchReaderSites(@NonNull TestEvents event,
                                   @NonNull String searchTerm,
                                   int count,
                                   int offset) throws InterruptedException {
        ReaderSearchSitesPayload payload = new ReaderSearchSitesPayload(searchTerm, count, offset, false);
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = event;
        mDispatcher.dispatch(ReaderActionBuilder.newReaderSearchSitesAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private boolean isSiteInSiteList(@NonNull List<ReaderSiteModel> siteList, @NonNull ReaderSiteModel site) {
        for (ReaderSiteModel otherSite : siteList) {
            if (otherSite.getFeedId() == site.getFeedId()) {
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
            mFirstSearchResults = event.sites;
            assertEquals(event.sites.size(), NUM_RESULTS);
            assertTrue(event.canLoadMore);
        } else if (mNextEvent == TestEvents.READER_SEARCH_SITES_WITH_OFFSET) {
            boolean areThereDups = false;
            for (ReaderSiteModel site : event.sites) {
                if (isSiteInSiteList(mFirstSearchResults, site)) {
                    areThereDups = true;
                    break;
                }
            }
            assertFalse(areThereDups);
        } else if (mNextEvent == TestEvents.READER_SEARCH_SITES_WITH_EMPTY_RESULTS) {
            assertTrue(event.sites.isEmpty());
            assertFalse(event.canLoadMore);
        } else {
            throw new AssertionError("Wrong event received");
        }

        mCountDownLatch.countDown();
    }
}
