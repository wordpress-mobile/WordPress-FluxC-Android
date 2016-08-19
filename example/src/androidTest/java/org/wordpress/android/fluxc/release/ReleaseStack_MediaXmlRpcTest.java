package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 */
public class ReleaseStack_MediaXmlRpcTest extends ReleaseStack_Base {
    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;
    @Inject MediaStore mMediaStore;
    @Inject HTTPAuthManager mHTTPAuthManager;
    @Inject MemorizingTrustManager mMemorizingTrustManager;

    private enum TEST_EVENTS {
        NONE,
        AUTHENTICATION_CHANGED,
        SITE_CHANGED,
        PULLED_ALL_MEDIA,
        UPLOADED_MEDIA
    }

    private TEST_EVENTS mExpectedEvent;
    private CountDownLatch mCountDownLatch;
    private AccountStore.OnDiscoverySucceeded mDiscovered;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        // Register
        mDispatcher.register(this);
        mExpectedEvent = TEST_EVENTS.NONE;
    }

    public void testPullAllMedia() throws InterruptedException {
        getSiteInfo(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE,
                BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE,
                BuildConfig.TEST_WPORG_URL_SH_SIMPLE_ENDPOINT);

        MediaStore.PullMediaPayload mediaPayload = new MediaStore.PullMediaPayload(mSiteStore.getSites().get(0), null);
        mExpectedEvent = TEST_EVENTS.PULLED_ALL_MEDIA;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newPullAllMediaAction(mediaPayload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testUploadMedia() throws InterruptedException {
        getSiteInfo(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE,
                BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE,
                BuildConfig.TEST_WPORG_URL_SH_SIMPLE_ENDPOINT);

        final String testTitle = "Test Title";
        final String testDescription = "Test Description";
        final String testCaption = "Test Caption";
        final String testAlt = "Test Alt";

        SiteModel site = mSiteStore.getSites().get(0);
        List<MediaModel> media = new ArrayList<>();
        MediaModel testMedia = new MediaModel();
        String imagePath = BuildConfig.TEST_LOCAL_IMAGE;
        testMedia.setFilePath(imagePath);
        testMedia.setFileExtension(imagePath.substring(imagePath.lastIndexOf(".") + 1, imagePath.length()));
        testMedia.setMimeType(MediaUtils.MIME_TYPE_IMAGE + testMedia.getFileExtension());
        testMedia.setFileName(imagePath.substring(imagePath.lastIndexOf("/"), imagePath.length()));
        testMedia.setTitle(testTitle);
        testMedia.setDescription(testDescription);
        testMedia.setCaption(testCaption);
        testMedia.setAlt(testAlt);
        testMedia.setBlogId(site.getDotOrgSiteId());
        media.add(testMedia);

        MediaStore.ChangeMediaPayload payload = new MediaStore.ChangeMediaPayload(site, media);
        mExpectedEvent = TEST_EVENTS.UPLOADED_MEDIA;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void discoverEndpoint(String username, String password, String url) throws InterruptedException {
        SiteStore.RefreshSitesXMLRPCPayload discoverPayload = new SiteStore.RefreshSitesXMLRPCPayload();
        discoverPayload.username = username;
        discoverPayload.password = password;
        discoverPayload.url = url;
        mExpectedEvent = TEST_EVENTS.AUTHENTICATION_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(discoverPayload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void getSiteInfo(String username, String password, String endpoint) throws InterruptedException {
        if (mDiscovered == null) discoverEndpoint(username, password, endpoint);

        SiteStore.RefreshSitesXMLRPCPayload payload = new SiteStore.RefreshSitesXMLRPCPayload();
        payload.username = username;
        payload.password = password;
        payload.url = mDiscovered.xmlRpcEndpoint;
        mExpectedEvent = TEST_EVENTS.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Subscribe
    public void onDiscoverySucceeded(AccountStore.OnDiscoverySucceeded event) {
        assertEquals(TEST_EVENTS.AUTHENTICATION_CHANGED, mExpectedEvent);
        mDiscovered = event;
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onMediaChanged(MediaStore.OnMediaChanged event) {
        if (event.causeOfChange == MediaAction.PULL_ALL_MEDIA) {
            assertEquals(TEST_EVENTS.PULLED_ALL_MEDIA, mExpectedEvent);
        } else if (event.causeOfChange == MediaAction.UPLOAD_MEDIA) {
            assertEquals(TEST_EVENTS.UPLOADED_MEDIA, mExpectedEvent);
        }
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onSiteChanged(SiteStore.OnSiteChanged event) {
        AppLog.i(AppLog.T.TESTS, "site count " + mSiteStore.getSitesCount());
        assertEquals(true, mSiteStore.hasSite());
        assertEquals(true, mSiteStore.hasDotOrgSite());
        assertEquals(TEST_EVENTS.SITE_CHANGED, mExpectedEvent);
        mCountDownLatch.countDown();
    }
}
