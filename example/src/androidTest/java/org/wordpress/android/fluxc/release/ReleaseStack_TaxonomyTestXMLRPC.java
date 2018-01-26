package org.wordpress.android.fluxc.release;

import junit.framework.Assert;

import org.apache.commons.lang3.RandomStringUtils;
import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder;
import org.wordpress.android.fluxc.model.TaxonomyModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.store.TaxonomyStore;
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchTermsPayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTaxonomyChanged;
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
        TERM_UPLOADED,
        TERM_DELETED,
        ERROR_INVALID_TAXONOMY,
        ERROR_DUPLICATE,
        ERROR_UNAUTHORIZED,
        ERROR_GENERIC
    }

    private TestEvents mNextEvent;
    private TaxonomyError mLastTaxonomyError;

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

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        List<TermModel> categories = mTaxonomyStore.getCategoriesForSite(sSite);
        Assert.assertTrue(categories.size() > 0);
    }

    public void testFetchTags() throws InterruptedException {
        mNextEvent = TestEvents.TAGS_FETCHED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchTagsAction(sSite));

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        List<TermModel> tags = mTaxonomyStore.getTagsForSite(sSite);
        Assert.assertTrue(tags.size() > 0);
    }

    public void testFetchTermsForInvalidTaxonomy() throws InterruptedException {
        mNextEvent = TestEvents.ERROR_GENERIC;
        mCountDownLatch = new CountDownLatch(1);

        TaxonomyModel taxonomyModel = new TaxonomyModel();
        taxonomyModel.setName("roads");

        FetchTermsPayload payload = new FetchTermsPayload(sSite, taxonomyModel);
        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchTermsAction(payload));

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // TODO: This will fail for non-English sites - we should be checking for an INVALID_TAXONOMY error instead
        // (once we make the fixes needed for TaxonomyXMLRPCClient to correctly identify taxonomy errors)
        Assert.assertEquals("Invalid taxonomy.", mLastTaxonomyError.message);
    }

    public void testFetchSingleCategory() throws InterruptedException {
        mNextEvent = TestEvents.TERM_UPDATED;
        mCountDownLatch = new CountDownLatch(1);

        TermModel term = new TermModel();
        term.setTaxonomy(TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY);
        term.setSlug("uncategorized");
        term.setRemoteTermId(1);
        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchTermAction(new RemoteTermPayload(term, sSite)));

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        Assert.assertEquals(1, WellSqlUtils.getTotalTermsCount());
        TermModel fetchedTerm = mTaxonomyStore.getCategoriesForSite(sSite).get(0);

        Assert.assertEquals("uncategorized", fetchedTerm.getSlug());
        Assert.assertNotSame(0, fetchedTerm.getRemoteTermId());
    }

    public void testUploadNewCategory() throws InterruptedException {
        // Instantiate new category
        TermModel term = createNewCategory();
        setupTermAttributes(term);

        // Upload new term to site
        uploadTerm(term);

        TermModel uploadedTerm = mTaxonomyStore.getCategoriesForSite(sSite).get(0);

        Assert.assertEquals(1, WellSqlUtils.getTotalTermsCount());
        Assert.assertEquals(1, mTaxonomyStore.getCategoriesForSite(sSite).size());

        Assert.assertNotSame(0, uploadedTerm.getRemoteTermId());
    }

    public void testUpdateExistingCategory() throws InterruptedException {
        TermModel term = createNewCategory();
        testUpdateExistingTerm(term);
    }

    public void testUploadNewTag() throws InterruptedException {
        // Instantiate new tag
        TermModel term = createNewTag();
        setupTermAttributes(term);

        // Upload new term to site
        uploadTerm(term);

        TermModel uploadedTerm = mTaxonomyStore.getTagsForSite(sSite).get(0);

        Assert.assertEquals(1, WellSqlUtils.getTotalTermsCount());
        Assert.assertEquals(1, mTaxonomyStore.getTagsForSite(sSite).size());

        Assert.assertNotSame(0, uploadedTerm.getRemoteTermId());
    }

    public void testUpdateExistingTag() throws InterruptedException {
        TermModel term = createNewTag();
        testUpdateExistingTerm(term);
    }

    public void testUploadNewCategoryAsTerm() throws InterruptedException {
        TaxonomyModel taxonomyModel = new TaxonomyModel();
        taxonomyModel.setName(TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY);

        // Instantiate new term
        TermModel termModel = createNewTerm(taxonomyModel);
        setupTermAttributes(termModel);

        // Upload new term to site
        uploadTerm(termModel);

        TermModel uploadedTerm = mTaxonomyStore.getCategoriesForSite(sSite).get(0);

        Assert.assertEquals(1, WellSqlUtils.getTotalTermsCount());
        Assert.assertEquals(1, mTaxonomyStore.getCategoriesForSite(sSite).size());

        Assert.assertNotSame(0, uploadedTerm.getRemoteTermId());
    }

    public void testUploadTermForInvalidTaxonomy() throws InterruptedException {
        TaxonomyModel taxonomyModel = new TaxonomyModel();
        taxonomyModel.setName("roads");

        // Instantiate new term
        TermModel term = createNewTerm(taxonomyModel);
        setupTermAttributes(term);

        mNextEvent = TestEvents.ERROR_GENERIC;
        mCountDownLatch = new CountDownLatch(1);

        RemoteTermPayload pushPayload = new RemoteTermPayload(term, sSite);
        mDispatcher.dispatch(TaxonomyActionBuilder.newPushTermAction(pushPayload));

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        TermModel failedTerm = mTaxonomyStore.getTermsForSite(sSite, "roads").get(0);
        Assert.assertEquals(0, failedTerm.getRemoteTermId());

        // TODO: This will fail for non-English sites - we should be checking for an INVALID_TAXONOMY error instead
        // (once we make the fixes needed for TaxonomyXMLRPCClient to correctly identify taxonomy errors)
        Assert.assertEquals("Invalid taxonomy.", mLastTaxonomyError.message);
    }

    public void testUploadNewCategoryDuplicate() throws InterruptedException {
        // Instantiate new category
        TermModel term = createNewCategory();
        setupTermAttributes(term);

        // Upload new term to site
        uploadTerm(term);

        // Upload the same term again after setting it's remote id to 0 so it doesn't trigger an update
        term.setRemoteTermId(0);
        mNextEvent = TestEvents.ERROR_GENERIC;
        mCountDownLatch = new CountDownLatch(1);

        RemoteTermPayload pushPayload = new RemoteTermPayload(term, sSite);
        mDispatcher.dispatch(TaxonomyActionBuilder.newPushTermAction(pushPayload));

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // TODO: This will fail for non-English sites - we should be checking for a DUPLICATE error instead
        // (once we make the fixes needed for TaxonomyXMLRPCClient to correctly identify taxonomy errors)
        Assert.assertEquals("A term with the name provided already exists with this parent.", mLastTaxonomyError.message);
    }

    public void testDeleteTag() throws InterruptedException {
        TermModel term = createNewTag();
        setupTermAttributes(term);

        uploadTerm(term);
        Assert.assertEquals(1, WellSqlUtils.getTotalTermsCount());

        term = mTaxonomyStore.getTagsForSite(sSite).get(0);
        deleteTerm(term);
        Assert.assertEquals(0, WellSqlUtils.getTotalTermsCount());
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onTaxonomyChanged(OnTaxonomyChanged event) {
        AppLog.i(T.API, "Received OnTaxonomyChanged, causeOfChange: " + event.causeOfChange);
        if (event.isError()) {
            AppLog.i(T.API, "OnTaxonomyChanged has error: " + event.error.type + " - " + event.error.message);
            mLastTaxonomyError = event.error;
            if (mNextEvent.equals(TestEvents.ERROR_INVALID_TAXONOMY)) {
                Assert.assertEquals(TaxonomyErrorType.INVALID_TAXONOMY, event.error.type);
                mCountDownLatch.countDown();
                return;
            } else if (mNextEvent.equals(TestEvents.ERROR_UNAUTHORIZED)) {
                Assert.assertEquals(TaxonomyErrorType.UNAUTHORIZED, event.error.type);
                mCountDownLatch.countDown();
                return;
            } else if (mNextEvent.equals(TestEvents.ERROR_GENERIC)) {
                Assert.assertEquals(TaxonomyErrorType.GENERIC_ERROR, event.error.type);
                mCountDownLatch.countDown();
                return;
            }
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
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
            case REMOVE_TERM:
                if (mNextEvent.equals(TestEvents.TERM_DELETED)) {
                    AppLog.i(T.API, "Deleted " + event.rowsAffected + " term");
                    mCountDownLatch.countDown();
                }
                break;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onTermUploaded(OnTermUploaded event) {
        AppLog.i(T.API, "Received OnTermUploaded");
        if (event.isError()) {
            AppLog.i(T.API, "OnTermUploaded has error: " + event.error.type + " - " + event.error.message);
            mLastTaxonomyError = event.error;
            if (mNextEvent.equals(TestEvents.ERROR_INVALID_TAXONOMY)) {
                Assert.assertEquals(TaxonomyErrorType.INVALID_TAXONOMY, event.error.type);
                mCountDownLatch.countDown();
                return;
            } else if (mNextEvent.equals(TestEvents.ERROR_DUPLICATE)) {
                Assert.assertEquals(TaxonomyErrorType.DUPLICATE, event.error.type);
                mCountDownLatch.countDown();
                return;
            } else if (mNextEvent.equals(TestEvents.ERROR_UNAUTHORIZED)) {
                Assert.assertEquals(TaxonomyErrorType.UNAUTHORIZED, event.error.type);
                mCountDownLatch.countDown();
                return;
            } else if (mNextEvent.equals(TestEvents.ERROR_GENERIC)) {
                Assert.assertEquals(TaxonomyErrorType.GENERIC_ERROR, event.error.type);
                mCountDownLatch.countDown();
                return;
            }
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        Assert.assertEquals(TestEvents.TERM_UPLOADED, mNextEvent);
        Assert.assertNotSame(0, event.term.getRemoteTermId());

        mCountDownLatch.countDown();
    }

    private void setupTermAttributes(TermModel term) {
        term.setName(TERM_DEFAULT_NAME + "-" + RandomStringUtils.randomAlphanumeric(4));
        term.setDescription(TERM_DEFAULT_DESCRIPTION);
    }

    private TermModel createNewCategory() throws InterruptedException {
        TermModel term = mTaxonomyStore.instantiateCategory(sSite);

        Assert.assertEquals(0, term.getRemoteTermId());
        Assert.assertNotSame(0, term.getId());
        Assert.assertNotSame(0, term.getLocalSiteId());

        return term;
    }

    private TermModel createNewTag() throws InterruptedException {
        TermModel term = mTaxonomyStore.instantiateTag(sSite);

        Assert.assertEquals(0, term.getRemoteTermId());
        Assert.assertNotSame(0, term.getId());
        Assert.assertNotSame(0, term.getLocalSiteId());

        return term;
    }

    private TermModel createNewTerm(TaxonomyModel taxonomy) throws InterruptedException {
        TermModel term = mTaxonomyStore.instantiateTerm(sSite, taxonomy);

        Assert.assertEquals(0, term.getRemoteTermId());
        Assert.assertNotSame(0, term.getId());
        Assert.assertNotSame(0, term.getLocalSiteId());

        return term;
    }

    private void uploadTerm(TermModel term) throws InterruptedException {
        mNextEvent = TestEvents.TERM_UPLOADED;
        mCountDownLatch = new CountDownLatch(1);

        RemoteTermPayload pushPayload = new RemoteTermPayload(term, sSite);
        mDispatcher.dispatch(TaxonomyActionBuilder.newPushTermAction(pushPayload));

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void testUpdateExistingTerm(TermModel term) throws InterruptedException {
        setupTermAttributes(term);

        // Upload new term to site
        uploadTerm(term);

        TermModel uploadedTerm = mTaxonomyStore.getTermsForSite(sSite, term.getTaxonomy()).get(0);
        Assert.assertEquals(1, WellSqlUtils.getTotalTermsCount());
        Assert.assertNotSame(0, uploadedTerm.getRemoteTermId());

        String newDescription = "newDescription";
        Assert.assertFalse(newDescription.equals(uploadedTerm.getDescription()));
        uploadedTerm.setDescription(newDescription);

        uploadTerm(uploadedTerm);
        Assert.assertEquals(1, WellSqlUtils.getTotalTermsCount()); // make sure we still have only one term
        TermModel updatedTerm = mTaxonomyStore.getTermsForSite(sSite, term.getTaxonomy()).get(0);
        Assert.assertEquals(updatedTerm.getRemoteTermId(), uploadedTerm.getRemoteTermId());
        Assert.assertEquals(updatedTerm.getDescription(), newDescription);
    }

    private void deleteTerm(TermModel term) throws InterruptedException {
        mNextEvent = TestEvents.TERM_DELETED;
        mCountDownLatch = new CountDownLatch(1);

        RemoteTermPayload pushPayload = new RemoteTermPayload(term, sSite);
        mDispatcher.dispatch(TaxonomyActionBuilder.newDeleteTermAction(pushPayload));

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
