package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TaxonomyModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.TaxonomyStore;
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchTermsPayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.RemoteTermPayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyErrorType;
import org.wordpress.android.fluxc.utils.WellSqlUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_TaxonomyTestWPCom extends ReleaseStack_Base {
    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject TaxonomyStore mTaxonomyStore;

    private CountDownLatch mCountDownLatch;
    private static SiteModel sSite;

    private enum TEST_EVENTS {
        NONE,
        SITE_CHANGED,
        CATEGORIES_FETCHED,
        TAGS_FETCHED,
        TERMS_FETCHED,
        TERM_UPDATED,
        ERROR_INVALID_TAXONOMY,
        ERROR_UNAUTHORIZED,
        ERROR_GENERIC
    }
    private TEST_EVENTS mNextEvent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        // Register
        mDispatcher.register(this);
        // Reset expected test event
        mNextEvent = TEST_EVENTS.NONE;

        if (mAccountStore.getAccessToken().isEmpty()) {
            authenticate();
        }

        if (sSite == null) {
            fetchSites();
            sSite = mSiteStore.getSites().get(0);
        }
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
        mNextEvent = TEST_EVENTS.ERROR_INVALID_TAXONOMY;
        mCountDownLatch = new CountDownLatch(1);

        TaxonomyModel taxonomyModel = new TaxonomyModel();
        taxonomyModel.setName("roads");

        FetchTermsPayload payload = new FetchTermsPayload(sSite, taxonomyModel);
        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchTermsAction(payload));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testFetchSingleCategory() throws InterruptedException {
        mNextEvent = TEST_EVENTS.TERM_UPDATED;
        mCountDownLatch = new CountDownLatch(1);

        TermModel term = new TermModel();
        term.setTaxonomy("category");
        term.setSlug("uncategorized");
        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchTermAction(new RemoteTermPayload(term, sSite)));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals(1, WellSqlUtils.getTotalTermsCount());
        TermModel fetchedTerm = mTaxonomyStore.getTermsForSite(sSite, "category").get(0);

        assertEquals("uncategorized", fetchedTerm.getSlug());
        assertNotSame(0, fetchedTerm.getRemoteTermId());
    }

    private void authenticate() throws InterruptedException {
        // Authenticate a test user (actual credentials declared in gradle.properties)
        AccountStore.AuthenticatePayload payload =
                new AccountStore.AuthenticatePayload(BuildConfig.TEST_WPCOM_USERNAME_TEST1,
                        BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        mCountDownLatch = new CountDownLatch(1);

        // Correct user we should get an OnAuthenticationChanged message
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchSites() throws InterruptedException {
        // Fetch sites from REST API, and wait for onSiteChanged event
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TEST_EVENTS.SITE_CHANGED;
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Subscribe
    public void onAuthenticationChanged(AccountStore.OnAuthenticationChanged event) {
        assertFalse(event.isError());
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onSiteChanged(SiteStore.OnSiteChanged event) {
        AppLog.i(T.TESTS, "site count " + mSiteStore.getSitesCount());
        if (event.isError()) {
            AppLog.i(T.TESTS, "event error type: " + event.error.type);
            return;
        }
        assertTrue(mSiteStore.hasSite());
        assertTrue(mSiteStore.hasWPComSite());
        assertEquals(TEST_EVENTS.SITE_CHANGED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onTaxonomyChanged(TaxonomyStore.OnTaxonomyChanged event) {
        AppLog.i(T.API, "Received OnTaxonomyChanged, causeOfChange: " + event.causeOfChange);
        if (event.isError()) {
            AppLog.i(T.API, "OnTaxonomyChanged has error: " + event.error.type + " - " + event.error.message);
            if (mNextEvent.equals(TEST_EVENTS.ERROR_INVALID_TAXONOMY)) {
                assertEquals(TaxonomyErrorType.INVALID_TAXONOMY, event.error.type);
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TEST_EVENTS.ERROR_GENERIC)) {
                assertEquals(TaxonomyErrorType.GENERIC_ERROR, event.error.type);
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
                break;
            case UPDATE_TERM:
                if (mNextEvent.equals(TEST_EVENTS.TERM_UPDATED)) {
                    AppLog.i(T.API, "Fetched " + event.rowsAffected + " term");
                    mCountDownLatch.countDown();
                }
                break;
        }
    }
}
