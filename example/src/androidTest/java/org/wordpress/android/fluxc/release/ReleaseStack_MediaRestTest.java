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
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.utils.MediaUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_MediaRestTest extends ReleaseStack_Base {
    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;
    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;

    enum TEST_EVENTS {
        FETCHED_ALL_MEDIA,
        FETCHED_KNOWN_IMAGES,
        UPLOADED_MEDIA
    }

    private TEST_EVENTS mExpectedEvent;
    private CountDownLatch mCountDownLatch;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        mDispatcher.register(this);
    }

    @Override
    public void tearDown() throws Exception {
        mDispatcher.unregister(this);
        super.tearDown();
    }

    public void testPullAllMedia() throws InterruptedException {
        loginAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);

        SiteModel site = mSiteStore.getSites().get(0);
        MediaStore.PullMediaPayload fetchPayload = new MediaStore.PullMediaPayload(site, null);
        mExpectedEvent = TEST_EVENTS.FETCHED_ALL_MEDIA;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newPullAllMediaAction(fetchPayload));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testPullSpecificMedia() throws InterruptedException {
        loginAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);

        String knownImageIds = BuildConfig.TEST_WPCOM_IMAGE_IDS_TEST1;
        String[] splitIds = knownImageIds.split(",");
        List<Long> idList = new ArrayList<>();
        for (String id : splitIds) {
            idList.add(Long.valueOf(id));
        }
        SiteModel site = mSiteStore.getSites().get(0);
        MediaStore.PullMediaPayload payload = new MediaStore.PullMediaPayload(site, idList);
        mExpectedEvent = TEST_EVENTS.FETCHED_KNOWN_IMAGES;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newPullMediaAction(payload));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testUploadImage() throws InterruptedException {
        loginAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);

        final String testTitle = "Test Title";
        final String testDescription = "Test Description";
        final String testCaption = "Test Caption";
        final String testAlt = "Test Alt";

        SiteModel site = mSiteStore.getSites().get(0);
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

        List<MediaModel> media = new ArrayList<>();
        media.add(testMedia);
        MediaStore.ChangeMediaPayload payload = new MediaStore.ChangeMediaPayload(site, media);
        mExpectedEvent = TEST_EVENTS.UPLOADED_MEDIA;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testUploadVideo() throws InterruptedException {
        loginAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);

        final String testTitle = "Test Title";
        final String testDescription = "Test Description";
        final String testCaption = "Test Caption";
        final String testAlt = "Test Alt";

        SiteModel site = mSiteStore.getSites().get(0);
        MediaModel testMedia = new MediaModel();
        String videoPath = BuildConfig.TEST_LOCAL_VIDEO;
        testMedia.setFilePath(videoPath);
        testMedia.setFileExtension(videoPath.substring(videoPath.lastIndexOf(".") + 1, videoPath.length()));
        testMedia.setMimeType(MediaUtils.MIME_TYPE_VIDEO + testMedia.getFileExtension());
        testMedia.setFileName(videoPath.substring(videoPath.lastIndexOf("/"), videoPath.length()));
        testMedia.setTitle(testTitle);
        testMedia.setDescription(testDescription);
        testMedia.setCaption(testCaption);
        testMedia.setAlt(testAlt);
        testMedia.setBlogId(site.getDotOrgSiteId());

        List<MediaModel> media = new ArrayList<>();
        media.add(testMedia);
        MediaStore.ChangeMediaPayload payload = new MediaStore.ChangeMediaPayload(site, media);
        mExpectedEvent = TEST_EVENTS.UPLOADED_MEDIA;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Subscribe
    public void onMediaChanged(MediaStore.OnMediaChanged event) {
        if (event.causeOfChange == MediaAction.PULL_ALL_MEDIA) {
            assertEquals(TEST_EVENTS.FETCHED_ALL_MEDIA, mExpectedEvent);
        } else if (event.causeOfChange == MediaAction.PULL_MEDIA) {
            if (eventHasKnownImages(event)) {
                assertEquals(TEST_EVENTS.FETCHED_KNOWN_IMAGES, mExpectedEvent);
            }
        } else if (event.causeOfChange == MediaAction.UPLOAD_MEDIA) {
            assertEquals(TEST_EVENTS.UPLOADED_MEDIA, mExpectedEvent);
        }
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onAuthenticationChanged(AccountStore.OnAuthenticationChanged event) {
        assertEquals(false, event.isError());
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onSiteChanged(SiteStore.OnSiteChanged event) {
        assertEquals(true, mSiteStore.hasDotComSite());
        mCountDownLatch.countDown();
    }

    private void loginAndFetchSites(String username, String password) throws InterruptedException {
        AccountStore.AuthenticatePayload payload =
                new AccountStore.AuthenticatePayload(username, password);

        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private boolean eventHasKnownImages(MediaStore.OnMediaChanged event) {
        if (event == null || event.media == null || event.media.isEmpty()) return false;
        String[] splitIds = BuildConfig.TEST_WPCOM_IMAGE_IDS_TEST1.split(",");
        if (splitIds.length != event.media.size()) return false;
        for (String id : splitIds) {
            if (!event.media.contains(Long.valueOf(id))) return false;
        }
        return true;
    }
}
