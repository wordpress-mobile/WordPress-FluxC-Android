package org.wordpress.android.fluxc.release;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yarolegovich.wellsql.WellSql;

import org.greenrobot.eventbus.Subscribe;
import org.junit.Test;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.example.utils.RandomStringUtils;
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TaxonomyModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.store.TaxonomyStore;
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchTermsPayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTaxonomyChanged;
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTermUploaded;
import org.wordpress.android.fluxc.store.TaxonomyStore.RemoteTermPayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyErrorType;
import org.wordpress.android.fluxc.utils.WellSqlUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("NewClassNamingConvention")
public class ReleaseStack_TaxonomyTestWPCom extends ReleaseStack_WPComBase {
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

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);

        // Authenticate, fetch sites and initialize sSite
        init();
        // Init mNextEvent
        mNextEvent = TestEvents.NONE;
    }

    @Test
    public void testFetchCategories() throws InterruptedException {
        mNextEvent = TestEvents.CATEGORIES_FETCHED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchCategoriesAction(sSite));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        List<TermModel> categories = mTaxonomyStore.getCategoriesForSite(sSite);
        assertTrue(categories.size() > 0);
    }

    @Test
    public void testFetchTags() throws InterruptedException {
        mNextEvent = TestEvents.TAGS_FETCHED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchTagsAction(sSite));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        List<TermModel> tags = mTaxonomyStore.getTagsForSite(sSite);
        assertTrue(tags.size() > 0);
    }

    @Test
    public void testFetchTermsForInvalidTaxonomy() throws InterruptedException {
        mNextEvent = TestEvents.ERROR_INVALID_TAXONOMY;
        mCountDownLatch = new CountDownLatch(1);

        TaxonomyModel taxonomyModel = new TaxonomyModel("roads");

        FetchTermsPayload payload = new FetchTermsPayload(sSite, taxonomyModel);
        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchTermsAction(payload));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testFetchSingleCategory() throws InterruptedException {
        mNextEvent = TestEvents.TERM_UPDATED;
        mCountDownLatch = new CountDownLatch(1);

        TermModel term = new TermModel();
        term.setTaxonomy(TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY);
        term.setSlug("uncategorized");
        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchTermAction(new RemoteTermPayload(term, sSite)));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals(1, WellSqlUtils.getTotalTermsCount());
        TermModel fetchedTerm = mTaxonomyStore.getCategoriesForSite(sSite).get(0);

        assertEquals("uncategorized", fetchedTerm.getSlug());
        assertNotSame(0, fetchedTerm.getRemoteTermId());
    }

    @Test
    public void testUploadNewCategory() throws InterruptedException {
        // Instantiate new category
        TermModel term = createNewCategory();
        setupTermAttributes(term);

        // Upload new term to site
        uploadTerm(term);

        TermModel uploadedTerm = mTaxonomyStore.getCategoriesForSite(sSite).get(0);

        assertEquals(1, WellSqlUtils.getTotalTermsCount());
        assertEquals(1, mTaxonomyStore.getCategoriesForSite(sSite).size());

        assertNotSame(0, uploadedTerm.getRemoteTermId());
    }

    @Test
    public void testUpdateExistingCategory() throws InterruptedException {
        TermModel term = createNewCategory();
        checkUpdateExistingTerm(term);
    }

    @Test
    public void testUploadNewTag() throws InterruptedException {
        // Instantiate new tag
        TermModel term = createNewTag();
        setupTermAttributes(term);

        // Upload new term to site
        uploadTerm(term);

        TermModel uploadedTerm = mTaxonomyStore.getTagsForSite(sSite).get(0);

        assertEquals(1, WellSqlUtils.getTotalTermsCount());
        assertEquals(1, mTaxonomyStore.getTagsForSite(sSite).size());

        assertNotSame(0, uploadedTerm.getRemoteTermId());
    }

    @Test
    public void testUpdateExistingTag() throws InterruptedException {
        TermModel term = createNewTag();
        checkUpdateExistingTerm(term);
    }

    @Test
    public void testDeleteTag() throws InterruptedException {
        TermModel term = createNewTag();
        setupTermAttributes(term);

        uploadTerm(term);
        assertEquals(1, WellSqlUtils.getTotalTermsCount());

        term = mTaxonomyStore.getTagsForSite(sSite).get(0);
        deleteTerm(term);
        assertEquals(0, WellSqlUtils.getTotalTermsCount());
    }

    @Test
    public void testUploadNewCategoryAsTerm() throws InterruptedException {
        TaxonomyModel taxonomyModel = new TaxonomyModel(TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY);

        // Instantiate new term
        TermModel term = createNewTerm(taxonomyModel);
        setupTermAttributes(term);

        // Upload new term to site
        uploadTerm(term);

        TermModel uploadedTerm = mTaxonomyStore.getCategoriesForSite(sSite).get(0);

        assertEquals(1, WellSqlUtils.getTotalTermsCount());
        assertEquals(1, mTaxonomyStore.getCategoriesForSite(sSite).size());

        assertNotSame(0, uploadedTerm.getRemoteTermId());
    }

    @Test
    public void testUploadTermForInvalidTaxonomy() throws InterruptedException {
        TaxonomyModel taxonomyModel = new TaxonomyModel("roads");

        // Instantiate new term
        TermModel term = createNewTerm(taxonomyModel);
        setupTermAttributes(term);

        mNextEvent = TestEvents.ERROR_INVALID_TAXONOMY;
        mCountDownLatch = new CountDownLatch(1);

        RemoteTermPayload pushPayload = new RemoteTermPayload(term, sSite);
        mDispatcher.dispatch(TaxonomyActionBuilder.newPushTermAction(pushPayload));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        TermModel failedTerm = mTaxonomyStore.getTermsForSite(sSite, "roads").get(0);
        assertEquals(0, failedTerm.getRemoteTermId());
    }

    @Test
    public void testUploadNewCategoryDuplicate() throws InterruptedException {
        // Instantiate new category
        TermModel term = createNewCategory();
        setupTermAttributes(term);

        // Upload new term to site
        uploadTerm(term);

        // Upload the same term again
        mNextEvent = TestEvents.ERROR_DUPLICATE;
        mCountDownLatch = new CountDownLatch(1);

        RemoteTermPayload pushPayload = new RemoteTermPayload(term, sSite);
        mDispatcher.dispatch(TaxonomyActionBuilder.newPushTermAction(pushPayload));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    // TODO: Add tests for existing custom taxonomies

    @SuppressWarnings("unused")
    @Subscribe
    public void onTaxonomyChanged(@NonNull OnTaxonomyChanged event) {
        AppLog.i(T.API, "Received OnTaxonomyChanged, causeOfChange: " + event.causeOfChange);
        if (event.isError()) {
            AppLog.i(T.API, "OnTaxonomyChanged has error: " + event.error.type + " - " + event.error.message);
            if (mNextEvent.equals(TestEvents.ERROR_INVALID_TAXONOMY)) {
                assertEquals(TaxonomyErrorType.INVALID_TAXONOMY, event.error.type);
                mCountDownLatch.countDown();
                return;
            } else if (mNextEvent.equals(TestEvents.ERROR_UNAUTHORIZED)) {
                assertEquals(TaxonomyErrorType.UNAUTHORIZED, event.error.type);
                mCountDownLatch.countDown();
                return;
            } else if (mNextEvent.equals(TestEvents.ERROR_GENERIC)) {
                assertEquals(TaxonomyErrorType.GENERIC_ERROR, event.error.type);
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
                    AppLog.i(T.API, "Fetched " + event.rowsAffected + " terms");
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
            case FETCH_TERM:
            case PUSH_TERM:
            case DELETE_TERM:
            case FETCHED_TERMS:
            case FETCHED_TERM:
            case PUSHED_TERM:
            case DELETED_TERM:
            case REMOVE_ALL_TERMS:
                break;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onTermUploaded(@NonNull OnTermUploaded event) {
        AppLog.i(T.API, "Received OnTermUploaded");
        if (event.isError()) {
            AppLog.i(T.API, "OnTermUploaded has error: " + event.error.type + " - " + event.error.message);
            if (mNextEvent.equals(TestEvents.ERROR_INVALID_TAXONOMY)) {
                assertEquals(TaxonomyErrorType.INVALID_TAXONOMY, event.error.type);
                mCountDownLatch.countDown();
                return;
            } else if (mNextEvent.equals(TestEvents.ERROR_DUPLICATE)) {
                assertEquals(TaxonomyErrorType.DUPLICATE, event.error.type);
                mCountDownLatch.countDown();
                return;
            } else if (mNextEvent.equals(TestEvents.ERROR_UNAUTHORIZED)) {
                assertEquals(TaxonomyErrorType.UNAUTHORIZED, event.error.type);
                mCountDownLatch.countDown();
                return;
            } else if (mNextEvent.equals(TestEvents.ERROR_GENERIC)) {
                assertEquals(TaxonomyErrorType.GENERIC_ERROR, event.error.type);
                mCountDownLatch.countDown();
                return;
            }
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(TestEvents.TERM_UPLOADED, mNextEvent);
        assertNotSame(0, event.term.getRemoteTermId());

        mCountDownLatch.countDown();
    }

    private void setupTermAttributes(@NonNull TermModel term) {
        term.setName(TERM_DEFAULT_NAME + "-" + RandomStringUtils.randomAlphanumeric(4));
        term.setDescription(TERM_DEFAULT_DESCRIPTION);
    }

    @NonNull
    private TermModel createNewCategory() {
        TermModel term = instantiateTermModel(sSite, TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY);

        if (term != null) {
            assertEquals(0, term.getRemoteTermId());
            assertNotSame(0, term.getId());
            assertNotSame(0, term.getLocalSiteId());
        } else {
            fail("Failed to instantiate new category!");
        }

        return term;
    }

    @NonNull
    private TermModel createNewTag() {
        TermModel term = instantiateTermModel(sSite, TaxonomyStore.DEFAULT_TAXONOMY_TAG);

        if (term != null) {
            assertEquals(0, term.getRemoteTermId());
            assertNotSame(0, term.getId());
            assertNotSame(0, term.getLocalSiteId());
        } else {
            fail("Failed to instantiate new tag!");
        }

        return term;
    }

    @NonNull
    private TermModel createNewTerm(@NonNull TaxonomyModel taxonomy) {
        TermModel term = instantiateTermModel(sSite, taxonomy.getName());

        if (term != null) {
            assertEquals(0, term.getRemoteTermId());
            assertNotSame(0, term.getId());
            assertNotSame(0, term.getLocalSiteId());
        } else {
            fail("Failed to instantiate new term!");
        }

        return term;
    }

    private void uploadTerm(TermModel term) throws InterruptedException {
        mNextEvent = TestEvents.TERM_UPLOADED;
        mCountDownLatch = new CountDownLatch(1);

        RemoteTermPayload pushPayload = new RemoteTermPayload(term, sSite);
        mDispatcher.dispatch(TaxonomyActionBuilder.newPushTermAction(pushPayload));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void deleteTerm(TermModel term) throws InterruptedException {
        mNextEvent = TestEvents.TERM_DELETED;
        mCountDownLatch = new CountDownLatch(1);

        RemoteTermPayload pushPayload = new RemoteTermPayload(term, sSite);
        mDispatcher.dispatch(TaxonomyActionBuilder.newDeleteTermAction(pushPayload));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void checkUpdateExistingTerm(TermModel term) throws InterruptedException {
        setupTermAttributes(term);

        // Upload new term to site
        uploadTerm(term);

        TermModel uploadedTerm = mTaxonomyStore.getTermsForSite(sSite, term.getTaxonomy()).get(0);
        assertEquals(1, WellSqlUtils.getTotalTermsCount());
        assertNotSame(0, uploadedTerm.getRemoteTermId());

        String newDescription = "newDescription";
        assertNotEquals(newDescription, uploadedTerm.getDescription());
        uploadedTerm.setDescription(newDescription);

        uploadTerm(uploadedTerm);
        assertEquals(1, WellSqlUtils.getTotalTermsCount()); // make sure we still have only one term
        TermModel updatedTerm = mTaxonomyStore.getTermsForSite(sSite, term.getTaxonomy()).get(0);
        assertEquals(updatedTerm.getRemoteTermId(), uploadedTerm.getRemoteTermId());
        assertEquals(updatedTerm.getDescription(), newDescription);
    }

    @Nullable
    private TermModel instantiateTermModel(@NonNull SiteModel site, @NonNull String taxonomyName) {
        TermModel newTerm = new TermModel();
        newTerm.setLocalSiteId(site.getId());
        newTerm.setTaxonomy(taxonomyName);

        // Insert the term into the db, updating the object to include the local ID
        TermModel insertedTerm = insertTermForResult(newTerm);

        // id is set to -1 if insertion fails
        if (insertedTerm.getId() == -1) {
            return null;
        }
        return insertedTerm;
    }

    @NonNull
    public static TermModel insertTermForResult(@NonNull TermModel term) {
        WellSql.insert(term).asSingleTransaction(true).execute();
        return term;
    }
}
