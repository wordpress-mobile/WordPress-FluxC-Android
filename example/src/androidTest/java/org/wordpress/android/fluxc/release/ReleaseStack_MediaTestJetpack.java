package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.FetchAccountPayload;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;
import org.wordpress.android.fluxc.store.MediaStore.MediaRequestPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.FetchAllSitesPayload;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteRemoved;
import org.wordpress.android.fluxc.store.SiteStore.SiteRequestPayload;
import org.wordpress.android.fluxc.store.SiteStore.RemoveWpcomAndJetpackSitesPayload;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_MediaTestJetpack extends ReleaseStack_Base {
    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;
    @Inject MediaStore mMediaStore;

    private enum TestEvents {
        MEDIA_UPLOADED,
        ERROR_EXCEEDS_FILESIZE_LIMIT,
        ERROR_EXCEEDS_MEMORY_LIMIT,
        SITE_CHANGED,
        SITE_REMOVED,
        NONE
    }

    private TestEvents mNextEvent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        // Register
        init();
        // Reset expected test event
        mNextEvent = TestEvents.NONE;
    }

    public void testUploadMediaLowFilesizeLimit() throws InterruptedException {
        authenticateWPComAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_JETPACK_UPLOAD_LIMIT,
                BuildConfig.TEST_WPCOM_PASSWORD_JETPACK_UPLOAD_LIMIT);

        SiteModel site = mSiteStore.getSites().get(0);

        // Make a call to /sites/$site/ to pull all the Jetpack options
        fetchSite(site);
        site = mSiteStore.getSites().get(0);

        // Attempt to upload an image that exceeds the site's maximum upload_max_filesize or post_max_size
        MediaModel testMedia = newMediaModel(site, BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE);
        mNextEvent = TestEvents.ERROR_EXCEEDS_FILESIZE_LIMIT;
        uploadMedia(site, testMedia);

        signOutWPCom();
    }

    public void testUploadMediaLowMemoryLimit() throws InterruptedException {
        authenticateWPComAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPCOM_PASSWORD_SINGLE_JETPACK_ONLY);

        SiteModel site = mSiteStore.getSites().get(0);

        // Make a call to /sites/$site/ to pull all the Jetpack options
        fetchSite(site);
        site = mSiteStore.getSites().get(0);
        site.setMemoryLimit(1985); // Artificially set the site's memory limit, in bytes

        // Attempt to upload an image that exceeds the site's memory limit
        MediaModel testMedia = newMediaModel(site, BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE);
        mNextEvent = TestEvents.ERROR_EXCEEDS_MEMORY_LIMIT;
        uploadMedia(site, testMedia);

        signOutWPCom();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaUploaded(OnMediaUploaded event) {
        if (event.isError()) {
            if (event.error.type == MediaErrorType.EXCEEDS_FILESIZE_LIMIT) {
                assertEquals(TestEvents.ERROR_EXCEEDS_FILESIZE_LIMIT, mNextEvent);
                mCountDownLatch.countDown();
                return;
            } else if (event.error.type == MediaErrorType.EXCEEDS_MEMORY_LIMIT) {
                assertEquals(TestEvents.ERROR_EXCEEDS_MEMORY_LIMIT, mNextEvent);
                mCountDownLatch.countDown();
                return;
            }
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        if (event.completed) {
            assertEquals(TestEvents.MEDIA_UPLOADED, mNextEvent);
            mCountDownLatch.countDown();
        }
    }

    private MediaModel newMediaModel(SiteModel site, String mediaPath, String mimeType) {
        final String testTitle = "Test Title";
        final String testDescription = "Test Description";
        final String testCaption = "Test Caption";
        final String testAlt = "Test Alt";

        MediaModel testMedia = mMediaStore.instantiateMediaModel();
        testMedia.setFilePath(mediaPath);
        testMedia.setFileExtension(mediaPath.substring(mediaPath.lastIndexOf(".") + 1, mediaPath.length()));
        testMedia.setMimeType(mimeType + testMedia.getFileExtension());
        testMedia.setFileName(mediaPath.substring(mediaPath.lastIndexOf("/"), mediaPath.length()));
        testMedia.setTitle(testTitle);
        testMedia.setDescription(testDescription);
        testMedia.setCaption(testCaption);
        testMedia.setAlt(testAlt);
        testMedia.setLocalSiteId(site.getId());

        return testMedia;
    }

    private void uploadMedia(SiteModel site, MediaModel media) throws InterruptedException {
        MediaRequestPayload payload = new MediaRequestPayload(site, media);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatchAsk(MediaActionBuilder.newUploadMediaAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAccountChanged(OnAccountChanged event) {
        AppLog.d(T.TESTS, "Received OnAccountChanged event");
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSiteChanged(OnSiteChanged event) {
        AppLog.d(T.TESTS, "site count " + mSiteStore.getSitesCount());
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertTrue(mSiteStore.hasSite());
        assertEquals(TestEvents.SITE_CHANGED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSiteRemoved(OnSiteRemoved event) {
        AppLog.d(T.TESTS, "site count " + mSiteStore.getSitesCount());
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(TestEvents.SITE_REMOVED, mNextEvent);
        mCountDownLatch.countDown();
    }

    private void authenticateWPComAndFetchSites(String username, String password) throws InterruptedException {
        // Authenticate a test user (actual credentials declared in gradle.properties)
        AuthenticatePayload payload = new AuthenticatePayload(username, password);
        mCountDownLatch = new CountDownLatch(1);

        // Correct user we should get an OnAuthenticationChanged message
        mDispatcher.dispatchAsk(AuthenticationActionBuilder.newAuthenticateAction(payload));
        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Fetch account from REST API, and wait for OnAccountChanged event
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatchAsk(AccountActionBuilder.newFetchAccountAction(new FetchAccountPayload()));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Fetch sites from REST API, and wait for onSiteChanged event
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.SITE_CHANGED;
        mDispatcher.dispatchAsk(SiteActionBuilder.newFetchSitesAction(new FetchAllSitesPayload()));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchSite(SiteModel site) throws InterruptedException {
        mNextEvent = TestEvents.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatchAsk(SiteActionBuilder.newFetchSiteAction(new SiteRequestPayload(site)));
        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void signOutWPCom() throws InterruptedException {
        // Clear WP.com sites, and wait for OnSiteRemoved event
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.SITE_REMOVED;
        mDispatcher.dispatchAsk(
                SiteActionBuilder.newRemoveWpcomAndJetpackSitesAction(new RemoveWpcomAndJetpackSitesPayload()));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
