package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TaxonomyModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.store.TaxonomyStore;
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyError;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_TaxonomyTestXMLRPC extends ReleaseStack_Base {
    @Inject Dispatcher mDispatcher;
    @Inject TaxonomyStore mTaxonomyStore;

    private CountDownLatch mCountDownLatch;
    private static SiteModel sSite;

    private TaxonomyError mLastTaxonomyError;

    private enum TEST_EVENTS {
        NONE,
        CATEGORIES_FETCHED,
        TAGS_FETCHED,
        TERMS_FETCHED,
        ERROR_INVALID_TAXONOMY,
        ERROR_UNAUTHORIZED,
        ERROR_GENERIC
    }
    private TEST_EVENTS mNextEvent;

    {
        sSite = new SiteModel();
        sSite.setId(1);
        sSite.setSelfHostedSiteId(0);
        sSite.setUsername(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE);
        sSite.setPassword(BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE);
        sSite.setXmlRpcUrl(BuildConfig.TEST_WPORG_URL_SH_SIMPLE_ENDPOINT);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        // Register
        mDispatcher.register(this);
        // Reset expected test event
        mNextEvent = TEST_EVENTS.NONE;

        mLastTaxonomyError = null;
    }

    public void testFetchCategories() throws InterruptedException {
        mNextEvent = TEST_EVENTS.CATEGORIES_FETCHED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchCategoriesAction(sSite));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        List<TermModel> categories = mTaxonomyStore.getCategoriesForSite(sSite);
        assertTrue(categories.size() > 0);
    }

    public void testFetchTags() throws InterruptedException {
        mNextEvent = TEST_EVENTS.TAGS_FETCHED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchTagsAction(sSite));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        List<TermModel> tags = mTaxonomyStore.getTagsForSite(sSite);
        assertTrue(tags.size() > 0);
    }

    public void testFetchTermsForInvalidTaxonomy() throws InterruptedException {
        mNextEvent = TEST_EVENTS.ERROR_GENERIC;
        mCountDownLatch = new CountDownLatch(1);

        TaxonomyModel taxonomyModel = new TaxonomyModel();
        taxonomyModel.setName("roads");

        TaxonomyStore.FetchTermsPayload payload = new TaxonomyStore.FetchTermsPayload(sSite, taxonomyModel);
        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchTermsAction(payload));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // TODO: This will fail for non-English sites - we should be checking for an INVALID_TAXONOMY error instead
        // (once we make the fixes needed for TaxonomyXMLRPCClient to correctly identify taxonomy errors)
        assertEquals("Invalid taxonomy.", mLastTaxonomyError.message);
    }

    @Subscribe
    public void onTaxonomyChanged(TaxonomyStore.OnTaxonomyChanged event) {
        AppLog.i(T.API, "Received OnTaxonomyChanged, causeOfChange: " + event.causeOfChange);
        if (event.isError()) {
            AppLog.i(T.API, "OnTaxonomyChanged has error: " + event.error.type + " - " + event.error.message);
            mLastTaxonomyError = event.error;
            if (mNextEvent.equals(TEST_EVENTS.ERROR_INVALID_TAXONOMY)) {
                assertEquals(TaxonomyStore.TaxonomyErrorType.INVALID_TAXONOMY, event.error.type);
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TEST_EVENTS.ERROR_GENERIC)) {
                assertEquals(TaxonomyStore.TaxonomyErrorType.GENERIC_ERROR, event.error.type);
                mCountDownLatch.countDown();
            }
            return;
        }
        switch (event.causeOfChange) {
            case FETCH_CATEGORIES:
                if (mNextEvent.equals(TEST_EVENTS.CATEGORIES_FETCHED)) {
                    AppLog.i(T.API, "Fetched " + event.rowsAffected + " categories");
                    mCountDownLatch.countDown();
                }
                break;
            case FETCH_TAGS:
                if (mNextEvent.equals(TEST_EVENTS.TAGS_FETCHED)) {
                    AppLog.i(T.API, "Fetched " + event.rowsAffected + " tags");
                    mCountDownLatch.countDown();
                }
                break;
            case FETCH_TERMS:
                if (mNextEvent.equals(TEST_EVENTS.TERMS_FETCHED)) {
                    AppLog.i(T.API, "Fetched " + event.rowsAffected + " " + event.taxonomyName + " terms");
                    mCountDownLatch.countDown();
                }
        }
    }
}
