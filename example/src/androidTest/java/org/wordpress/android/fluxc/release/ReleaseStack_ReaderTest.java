package org.wordpress.android.fluxc.release;

import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.junit.Test;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.ReaderActionBuilder;
import org.wordpress.android.fluxc.network.rest.wpcom.reader.ReaderRestClient;
import org.wordpress.android.fluxc.store.ReaderStore;
import org.wordpress.android.fluxc.store.ReaderStore.OnReaderSitesSearched;
import org.wordpress.android.fluxc.store.ReaderStore.ReaderSearchSitesPayload;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReleaseStack_ReaderTest extends ReleaseStack_Base {
    @Inject ReaderStore mReaderStore;

    private enum TestEvents {
        NONE,
        READER_SEARCH_SITES
    }

    private static final String SEARCH_TERM = "dogs";
    private TestEvents mNextEvent;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);

        init();

        mNextEvent = TestEvents.NONE;
    }

    @Test
    public void testFetchStockMediaList() throws InterruptedException {
        mNextEvent = TestEvents.READER_SEARCH_SITES;
        searchReaderSites(SEARCH_TERM, 0);
        // TODO: search with offset
    }

    private void searchReaderSites(@NonNull String searchTerm, int offset) throws InterruptedException {
        ReaderSearchSitesPayload payload = new ReaderSearchSitesPayload(searchTerm, offset);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(ReaderActionBuilder.newReaderSearchSitesAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onReaderSitesSearched(OnReaderSitesSearched event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: "
                                     + event.error.type);
        }

        assertEquals(mNextEvent, TestEvents.READER_SEARCH_SITES);
        assertEquals(event.feeds.size(), ReaderRestClient.NUM_SEARCH_RESULTS);

        mCountDownLatch.countDown();
    }
}
