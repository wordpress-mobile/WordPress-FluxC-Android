package org.wordpress.android.fluxc.release;

import org.apache.commons.lang.RandomStringUtils;
import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder;
import org.wordpress.android.fluxc.model.TaxonomyModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.store.TaxonomyStore;
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchTermsPayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.InstantiateTermPayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTaxonomyChanged;
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTermInstantiated;
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTermUploaded;
import org.wordpress.android.fluxc.store.TaxonomyStore.RemoteTermPayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyError;
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyErrorType;
import org.wordpress.android.fluxc.utils.WellSqlUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_TaxonomyTestXMLRPC extends ReleaseStack_XMLRPCBase {
    @Inject TaxonomyStore mTaxonomyStore;

    private static final String TERM_DEFAULT_NAME = "fluxc-example";
    private static final String TERM_DEFAULT_DESCRIPTION = "A term from FluxC";

    private enum TestEvents {
        NONE,
        CATEGORIES_FETCHED,
        TAGS_FETCHED,
        TERMS_FETCHED,
        TERM_UPDATED,
        TERM_INSTANTIATED,
        TERM_UPLOADED,
        ERROR_INVALID_TAXONOMY,
        ERROR_UNAUTHORIZED,
        ERROR_GENERIC
    }

    private TestEvents mNextEvent;
    private TaxonomyError mLastTaxonomyError;
    private TermModel mTerm;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);

        // Fetch sites and initialize sSite
        init();
        // Reset expected test event
        mNextEvent = TestEvents.NONE;

        mLastTaxonomyError = null;
    }

    public void testFetchCategories() throws InterruptedException {
        mNextEvent = TestEvents.CATEGORIES_FETCHED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchCategoriesAction(sSite));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        List<TermModel> categories = mTaxonomyStore.getCategoriesForSite(sSite);
        assertTrue(categories.size() > 0);
    }

    public void testFetchTags() throws InterruptedException {
        mNextEvent = TestEvents.TAGS_FETCHED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchTagsAction(sSite));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        List<TermModel> tags = mTaxonomyStore.getTagsForSite(sSite);
        assertTrue(tags.size() > 0);
    }

    public void testFetchTermsForInvalidTaxonomy() throws InterruptedException {
        mNextEvent = TestEvents.ERROR_GENERIC;
        mCountDownLatch = new CountDownLatch(1);

        TaxonomyModel taxonomyModel = new TaxonomyModel();
        taxonomyModel.setName("roads");

        FetchTermsPayload payload = new FetchTermsPayload(sSite, taxonomyModel);
        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchTermsAction(payload));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // TODO: This will fail for non-English sites - we should be checking for an INVALID_TAXONOMY error instead
        // (once we make the fixes needed for TaxonomyXMLRPCClient to correctly identify taxonomy errors)
        assertEquals("Invalid taxonomy.", mLastTaxonomyError.message);
    }

    public void testFetchSingleCategory() throws InterruptedException {
        mNextEvent = TestEvents.TERM_UPDATED;
        mCountDownLatch = new CountDownLatch(1);

        TermModel term = new TermModel();
        term.setTaxonomy("category");
        term.setSlug("uncategorized");
        term.setRemoteTermId(1);
        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchTermAction(new RemoteTermPayload(term, sSite)));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals(1, WellSqlUtils.getTotalTermsCount());
        TermModel fetchedTerm = mTaxonomyStore.getTermsForSite(sSite, "category").get(0);

        assertEquals("uncategorized", fetchedTerm.getSlug());
        assertNotSame(0, fetchedTerm.getRemoteTermId());
    }

    public void testUploadNewCategory() throws InterruptedException {
        // Instantiate new category
        createNewCategory();
        setupTermAttributes();

        // Upload new term to site
        uploadTerm(mTerm);

        TermModel uploadedTerm = mTaxonomyStore.getTermsForSite(sSite, "category").get(0);

        assertEquals(1, WellSqlUtils.getTotalTermsCount());
        assertEquals(1, mTaxonomyStore.getTermsForSite(sSite, "category").size());

        assertNotSame(0, uploadedTerm.getRemoteTermId());
    }

    public void testUploadNewTag() throws InterruptedException {
        // Instantiate new tag
        createNewTag();
        setupTermAttributes();

        // Upload new term to site
        uploadTerm(mTerm);

        TermModel uploadedTerm = mTaxonomyStore.getTermsForSite(sSite, "post_tag").get(0);

        assertEquals(1, WellSqlUtils.getTotalTermsCount());
        assertEquals(1, mTaxonomyStore.getTermsForSite(sSite, "post_tag").size());

        assertNotSame(0, uploadedTerm.getRemoteTermId());
    }

    public void testUploadNewCategoryAsTerm() throws InterruptedException {
        TaxonomyModel taxonomyModel = new TaxonomyModel();
        taxonomyModel.setName("category");

        // Instantiate new term
        createNewTerm(taxonomyModel);
        setupTermAttributes();

        // Upload new term to site
        uploadTerm(mTerm);

        TermModel uploadedTerm = mTaxonomyStore.getTermsForSite(sSite, "category").get(0);

        assertEquals(1, WellSqlUtils.getTotalTermsCount());
        assertEquals(1, mTaxonomyStore.getTermsForSite(sSite, "category").size());

        assertNotSame(0, uploadedTerm.getRemoteTermId());
    }

    public void testUploadTermForInvalidTaxonomy() throws InterruptedException {
        TaxonomyModel taxonomyModel = new TaxonomyModel();
        taxonomyModel.setName("roads");

        // Instantiate new term
        createNewTerm(taxonomyModel);
        setupTermAttributes();

        mNextEvent = TestEvents.ERROR_GENERIC;
        mCountDownLatch = new CountDownLatch(1);

        RemoteTermPayload pushPayload = new RemoteTermPayload(mTerm, sSite);
        mDispatcher.dispatch(TaxonomyActionBuilder.newPushTermAction(pushPayload));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        TermModel failedTerm = mTaxonomyStore.getTermsForSite(sSite, "roads").get(0);
        assertEquals(0, failedTerm.getRemoteTermId());

        // TODO: This will fail for non-English sites - we should be checking for an INVALID_TAXONOMY error instead
        // (once we make the fixes needed for TaxonomyXMLRPCClient to correctly identify taxonomy errors)
        assertEquals("Invalid taxonomy.", mLastTaxonomyError.message);
    }

    @Subscribe
    public void onTaxonomyChanged(OnTaxonomyChanged event) {
        AppLog.i(T.API, "Received OnTaxonomyChanged, causeOfChange: " + event.causeOfChange);
        if (event.isError()) {
            AppLog.i(T.API, "OnTaxonomyChanged has error: " + event.error.type + " - " + event.error.message);
            mLastTaxonomyError = event.error;
            if (mNextEvent.equals(TestEvents.ERROR_INVALID_TAXONOMY)) {
                assertEquals(TaxonomyErrorType.INVALID_TAXONOMY, event.error.type);
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TestEvents.ERROR_GENERIC)) {
                assertEquals(TaxonomyErrorType.GENERIC_ERROR, event.error.type);
                mCountDownLatch.countDown();
            }
            return;
        }
        switch (event.causeOfChange) {
            case FETCH_CATEGORIES:
                if (mNextEvent.equals(TestEvents.CATEGORIES_FETCHED)) {
                    AppLog.i(T.API, "Fetched " + event.rowsAffected + " categories");
                    mCountDownLatch.countDown();
                }
                break;
            case FETCH_TAGS:
                if (mNextEvent.equals(TestEvents.TAGS_FETCHED)) {
                    AppLog.i(T.API, "Fetched " + event.rowsAffected + " tags");
                    mCountDownLatch.countDown();
                }
                break;
            case FETCH_TERMS:
                if (mNextEvent.equals(TestEvents.TERMS_FETCHED)) {
                    AppLog.i(T.API, "Fetched " + event.rowsAffected + " " + event.taxonomyName + " terms");
                    mCountDownLatch.countDown();
                }
                break;
            case UPDATE_TERM:
                if (mNextEvent.equals(TestEvents.TERM_UPDATED)) {
                    AppLog.i(T.API, "Fetched " + event.rowsAffected + " term");
                    mCountDownLatch.countDown();
                }
                break;
        }
    }

    @Subscribe
    public void onTermUploaded(OnTermUploaded event) {
        AppLog.i(T.API, "Received OnTermUploaded");
        if (event.isError()) {
            AppLog.i(T.API, "OnTermUploaded has error: " + event.error.type + " - " + event.error.message);
            mLastTaxonomyError = event.error;
            // TODO this case can't happen at the moment...but maybe should leave it in for future anyway
            if (mNextEvent.equals(TestEvents.ERROR_INVALID_TAXONOMY)) {
                assertEquals(TaxonomyErrorType.INVALID_TAXONOMY, event.error.type);
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TestEvents.ERROR_GENERIC)) {
                assertEquals(TaxonomyErrorType.GENERIC_ERROR, event.error.type);
                mCountDownLatch.countDown();
            }
            return;
        }
        assertEquals(TestEvents.TERM_UPLOADED, mNextEvent);
        assertNotSame(0, event.term.getRemoteTermId());

        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onTermInstantiated(OnTermInstantiated event) {
        AppLog.i(T.API, "Received OnTermInstantiated");
        assertEquals(TestEvents.TERM_INSTANTIATED, mNextEvent);

        assertEquals(0, event.term.getRemoteTermId());
        assertNotSame(0, event.term.getId());
        assertNotSame(0, event.term.getLocalSiteId());

        mTerm = event.term;
        mCountDownLatch.countDown();
    }

    private void setupTermAttributes() {
        mTerm.setName(TERM_DEFAULT_NAME + "-" + RandomStringUtils.randomAlphanumeric(4));
        mTerm.setDescription(TERM_DEFAULT_DESCRIPTION);
    }

    private void createNewCategory() throws InterruptedException {
        // Instantiate new category
        mNextEvent = TestEvents.TERM_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(TaxonomyActionBuilder.newInstantiateCategoryAction(sSite));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void createNewTag() throws InterruptedException {
        // Instantiate new tag
        mNextEvent = TestEvents.TERM_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(TaxonomyActionBuilder.newInstantiateTagAction(sSite));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void createNewTerm(TaxonomyModel taxonomy) throws InterruptedException {
        // Instantiate new term
        mNextEvent = TestEvents.TERM_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);

        InstantiateTermPayload payload = new InstantiateTermPayload(sSite, taxonomy);
        mDispatcher.dispatch(TaxonomyActionBuilder.newInstantiateTermAction(payload));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void uploadTerm(TermModel term) throws InterruptedException {
        mNextEvent = TestEvents.TERM_UPLOADED;
        mCountDownLatch = new CountDownLatch(1);

        RemoteTermPayload pushPayload = new RemoteTermPayload(term, sSite);
        mDispatcher.dispatch(TaxonomyActionBuilder.newPushTermAction(pushPayload));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
