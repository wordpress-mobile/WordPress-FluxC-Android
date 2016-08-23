package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.annotations.action.Action;
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

public class ReleaseStack_MediaXmlRpcTest extends ReleaseStack_Base {
    private final String TEST_TITLE = "Test Title";
    private final String TEST_DESCRIPTION = "Test Description";
    private final String TEST_CAPTION = "Test Caption";
    private final String TEST_ALT = "Test Alt";

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
        PUSHED_MEDIA,
        PULLED_ALL_MEDIA,
        PULLED_MEDIA,
        DELETED_MEDIA,
        UPLOADED_MEDIA,
        NULL_ERROR,
        MALFORMED_ERROR,
        NOT_FOUND_ERROR
    }

    private TEST_EVENTS mExpectedEvent;
    private CountDownLatch mCountDownLatch;
    private AccountStore.OnDiscoverySucceeded mDiscovered;
    private List<Long> mExpectedIds;
    private long mLastUploadedId = -1L;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        // Register
        mDispatcher.register(this);
        mExpectedEvent = TEST_EVENTS.NONE;

        getSiteInfo(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE,
                BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE,
                BuildConfig.TEST_WPORG_URL_SH_SIMPLE_ENDPOINT);
    }

    /**
     * Push action attempted with null media.
     */
    public void testPushNullMedia() throws InterruptedException {
        pushMedia(mSiteStore.getSites().get(0), null, TEST_EVENTS.NULL_ERROR);
    }

    /**
     * Push action that supplies media without required data should result in a malformed exception.
     */
    public void testPushMalformedMedia() throws InterruptedException {
        final MediaModel testMedia = getTestMedia(TEST_TITLE, null, null, null);
        pushMedia(mSiteStore.getSites().get(0), testMedia, TEST_EVENTS.MALFORMED_ERROR);
    }

    /**
     * Push action that references media that exists remotely should update remote properties and
     * not trigger an upload.
     */
    public void testPushMediaChanges() throws InterruptedException {
        // pull site media
        SiteModel site = mSiteStore.getSites().get(0);
        long siteId = site.getSiteId();
        pullAllMedia(site);

        // some media is expected
        List<MediaModel> siteMedia = mMediaStore.getAllSiteMedia(siteId);
        assertFalse(siteMedia.isEmpty());

        // store existing properties for restoration
        final MediaModel testMedia = siteMedia.get(0);
        final long testId = testMedia.getMediaId();
        final String mediaTitle = testMedia.getTitle();
        final String mediaDescription = testMedia.getDescription();
        final String mediaCaption = testMedia.getCaption();
        final String mediaAlt = testMedia.getAlt();

        // update properties to test pushing changes
        final String newTitle = mediaTitle + TestUtils.randomString(5);
        final String newDescription = mediaDescription + TestUtils.randomString(5);
        final String newCaption = mediaCaption + TestUtils.randomString(5);
        final String newAlt = mediaAlt + TestUtils.randomString(5);
        testMedia.setTitle(newTitle);
        testMedia.setDescription(newDescription);
        testMedia.setCaption(newCaption);
        testMedia.setAlt(newAlt);

        // push changes
        pushMedia(site, testMedia, TEST_EVENTS.PUSHED_MEDIA);

        // verify local media has changes
        final MediaModel updatedMedia = mMediaStore.getSiteMediaWithId(siteId, testId);
        assertNotNull(updatedMedia);
        assertEquals(updatedMedia.getTitle(), newTitle);
        assertEquals(updatedMedia.getDescription(), newDescription);
        assertEquals(updatedMedia.getCaption(), newCaption);
        assertEquals(updatedMedia.getAlt(), newAlt);

        // reset media properties
        testMedia.setTitle(mediaTitle);
        testMedia.setDescription(mediaDescription);
        testMedia.setCaption(mediaCaption);
        testMedia.setAlt(mediaAlt);
        pushMedia(site, testMedia, TEST_EVENTS.PUSHED_MEDIA);

        // verify restored media properties
        final MediaModel restoredMedia = mMediaStore.getSiteMediaWithId(siteId, testId);
        assertEquals(restoredMedia.getTitle(), mediaTitle);
        assertEquals(restoredMedia.getDescription(), mediaDescription);
        assertEquals(restoredMedia.getCaption(), mediaCaption);
        assertEquals(restoredMedia.getAlt(), mediaAlt);
    }

    /**
     * Push action that references media that does not exist remotely should trigger an upload of
     * the new media.
     */
    public void testPushNewMedia() throws InterruptedException {
        // create new media item for local image
        SiteModel site = mSiteStore.getSites().get(0);
        final String TEST_ID = TEST_DESCRIPTION + TestUtils.randomString(5);
        final MediaModel testMedia = getTestMedia(TEST_TITLE, TEST_ID, TEST_CAPTION, TEST_ALT);
        String imagePath = BuildConfig.TEST_LOCAL_IMAGE;
        testMedia.setFilePath(imagePath);
        testMedia.setFileName(MediaUtils.getFileName(imagePath));
        testMedia.setFileExtension(MediaUtils.getExtension(imagePath));
        testMedia.setMimeType(MediaUtils.MIME_TYPE_IMAGE + testMedia.getFileExtension());
        testMedia.setBlogId(site.getDotOrgSiteId());

        // push media item, expecting an upload event to trigger
        pushMedia(site, testMedia, TEST_EVENTS.UPLOADED_MEDIA);

        // delete media
        List<MediaModel> siteMedia = mMediaStore.getAllSiteMedia(site.getDotOrgSiteId());
        assertNotNull(siteMedia);
        assertFalse(siteMedia.isEmpty());
        for (MediaModel media : siteMedia) {
            if (TEST_ID.equals(media.getDescription())) {
                deleteMedia(site, media, TEST_EVENTS.DELETED_MEDIA);
            }
        }
    }

    /**
     * Upload a local image.
     */
    public void testUploadImage() throws InterruptedException {
        SiteModel site = mSiteStore.getSites().get(0);
        List<MediaModel> media = new ArrayList<>();

        MediaModel testMedia = getTestMedia(TEST_TITLE, TEST_DESCRIPTION, TEST_CAPTION, TEST_ALT);
        String imagePath = BuildConfig.TEST_LOCAL_IMAGE;
        testMedia.setFilePath(imagePath);
        testMedia.setFileName(MediaUtils.getFileName(imagePath));
        testMedia.setFileExtension(MediaUtils.getExtension(imagePath));
        testMedia.setMimeType(MediaUtils.MIME_TYPE_IMAGE + testMedia.getFileExtension());
        testMedia.setBlogId(site.getDotOrgSiteId());
        media.add(testMedia);

        uploadMedia(site, media);
    }

    /**
     * Upload a local video.
     */
    public void testUploadVideo() throws InterruptedException {
        SiteModel site = mSiteStore.getSites().get(0);
        List<MediaModel> media = new ArrayList<>();

        MediaModel testMedia = getTestMedia(TEST_TITLE, TEST_DESCRIPTION, TEST_CAPTION, TEST_ALT);
        String videoPath = BuildConfig.TEST_LOCAL_VIDEO;
        testMedia.setFilePath(videoPath);
        testMedia.setFileName(MediaUtils.getFileName(videoPath));
        testMedia.setFileExtension(MediaUtils.getExtension(videoPath));
        testMedia.setMimeType(MediaUtils.MIME_TYPE_VIDEO + testMedia.getFileExtension());
        testMedia.setBlogId(site.getDotOrgSiteId());
        media.add(testMedia);

        uploadMedia(site, media);
    }

    /**
     * Pull all action should gather all media from a site.
     */
    public void testPullAllMedia() throws InterruptedException {
        pullAllMedia(mSiteStore.getSites().get(0));
    }

    /**
     * Pull action with no media supplied results in a media exception.
     */
    public void testPullNullMedia() throws InterruptedException {
        pullSpecificMedia(mSiteStore.getSites().get(0), null, TEST_EVENTS.NULL_ERROR);
    }

    /**
     * Pull action with media that does not exist results in a media not found exception.
     */
    public void testPullMediaThatDoesNotExist() throws InterruptedException {
        SiteModel site = mSiteStore.getSites().get(0);
        List<Long> mediaIds = new ArrayList<>();
        mediaIds.add(9999999L);
        mediaIds.add(9999989L);
        pullSpecificMedia(site, mediaIds, TEST_EVENTS.NOT_FOUND_ERROR);
    }

    /**
     * Pull action should gather only media items whose ID is in the given filter.
     */
    public void testPullMediaThatExists() throws InterruptedException {
        // get all site media
        SiteModel site = mSiteStore.getSites().get(0);
        pullAllMedia(site);

        final List<MediaModel> siteMedia = mMediaStore.getAllSiteMedia(site.getSiteId());
        assertFalse(siteMedia.isEmpty());

        // pull half of the media
        final int half = siteMedia.size() / 2;
        final List<Long> halfMediaIds = new ArrayList<>(half);
        for (int i = 0; i < half; ++i) {
            halfMediaIds.add(siteMedia.get(i).getMediaId());
        }
        pullSpecificMedia(site, halfMediaIds, TEST_EVENTS.PULLED_MEDIA);
    }

    /**
     * Delete action on null media results in a null error.
     */
    public void testDeleteNullMedia() throws InterruptedException {
        deleteMedia(mSiteStore.getSites().get(0), null, TEST_EVENTS.NULL_ERROR);
    }

    /**
     * Delete action on media that doesn't exist should not result in an exception.
     */
    public void testDeleteMediaThatDoesNotExist() throws InterruptedException {
        SiteModel site = mSiteStore.getSites().get(0);
        MediaModel testMedia = new MediaModel();
        testMedia.setMediaId(9999999L);
        deleteMedia(site, testMedia, TEST_EVENTS.NOT_FOUND_ERROR);
    }

    /**
     * Delete action on media that exists should result in no media on a pull request.
     */
    public void testDeleteMediaThatExists() throws InterruptedException {
        // upload media
        SiteModel site = mSiteStore.getSites().get(0);
        List<MediaModel> media = new ArrayList<>();
        MediaModel testMedia = getTestMedia(TEST_TITLE, TEST_DESCRIPTION, TEST_CAPTION, TEST_ALT);
        String imagePath = BuildConfig.TEST_LOCAL_IMAGE;
        testMedia.setFilePath(imagePath);
        testMedia.setFileName(MediaUtils.getFileName(imagePath));
        testMedia.setFileExtension(MediaUtils.getExtension(imagePath));
        testMedia.setMimeType(MediaUtils.MIME_TYPE_IMAGE + testMedia.getFileExtension());
        testMedia.setBlogId(site.getDotOrgSiteId());
        media.add(testMedia);
        uploadMedia(site, media);
        assertTrue(mLastUploadedId > 0);
        testMedia.setMediaId(mLastUploadedId);
        deleteMedia(site, testMedia, TEST_EVENTS.DELETED_MEDIA);
    }

    private MediaModel getTestMedia(String title, String description, String caption, String alt) {
        MediaModel media = new MediaModel();
        media.setTitle(title);
        media.setDescription(description);
        media.setCaption(caption);
        media.setAlt(alt);
        return media;
    }

    private void dispatchAction(TEST_EVENTS expectedEvent, Action action, int count) throws InterruptedException {
        mExpectedEvent = expectedEvent;
        mCountDownLatch = new CountDownLatch(count);
        mDispatcher.dispatch(action);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void discoverEndpoint(String username, String password, String url) throws InterruptedException {
        SiteStore.RefreshSitesXMLRPCPayload discoverPayload = new SiteStore.RefreshSitesXMLRPCPayload();
        discoverPayload.username = username;
        discoverPayload.password = password;
        discoverPayload.url = url;
        dispatchAction(TEST_EVENTS.AUTHENTICATION_CHANGED, AuthenticationActionBuilder.newDiscoverEndpointAction(discoverPayload), 1);
    }

    private void getSiteInfo(String username, String password, String endpoint) throws InterruptedException {
        if (mDiscovered == null) discoverEndpoint(username, password, endpoint);

        SiteStore.RefreshSitesXMLRPCPayload payload = new SiteStore.RefreshSitesXMLRPCPayload();
        payload.username = username;
        payload.password = password;
        payload.url = mDiscovered.xmlRpcEndpoint;
        dispatchAction(TEST_EVENTS.SITE_CHANGED, SiteActionBuilder.newFetchSitesXmlRpcAction(payload), 1);
    }

    private void pushMedia(SiteModel site, MediaModel media, TEST_EVENTS expectedEvent) throws InterruptedException {
        List<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(media);
        MediaStore.ChangeMediaPayload payload = new MediaStore.ChangeMediaPayload(site, mediaList);
        dispatchAction(expectedEvent, MediaActionBuilder.newPushMediaAction(payload), 1);
    }

    private void uploadMedia(SiteModel site, List<MediaModel> media) throws InterruptedException {
        MediaStore.ChangeMediaPayload payload = new MediaStore.ChangeMediaPayload(site, media);
        dispatchAction(TEST_EVENTS.UPLOADED_MEDIA, MediaActionBuilder.newUploadMediaAction(payload), 1);
    }

    private void pullAllMedia(SiteModel site) throws InterruptedException {
        MediaStore.PullMediaPayload mediaPayload = new MediaStore.PullMediaPayload(site, null);
        dispatchAction(TEST_EVENTS.PULLED_ALL_MEDIA, MediaActionBuilder.newPullAllMediaAction(mediaPayload), 1);
    }

    private void pullSpecificMedia(SiteModel site, List<Long> mediaIds, TEST_EVENTS expectedEvent) throws InterruptedException {
        mExpectedIds = mediaIds;
        MediaStore.PullMediaPayload mediaPayload = new MediaStore.PullMediaPayload(site, mediaIds);
        dispatchAction(expectedEvent, MediaActionBuilder.newPullMediaAction(mediaPayload), mExpectedIds.size());
    }

    private void deleteMedia(SiteModel site, MediaModel media, TEST_EVENTS expectedEvent) throws InterruptedException {
        List<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(media);
        MediaStore.ChangeMediaPayload deletePayload = new MediaStore.ChangeMediaPayload(site, mediaList);
        dispatchAction(expectedEvent, MediaActionBuilder.newDeleteMediaAction(deletePayload), 1);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onDiscoverySucceeded(AccountStore.OnDiscoverySucceeded event) {
        assertEquals(TEST_EVENTS.AUTHENTICATION_CHANGED, mExpectedEvent);
        mDiscovered = event;
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaChanged(MediaStore.OnMediaChanged event) {
        AppLog.d(AppLog.T.TESTS, "tests.onMediaChanged: " + event.causeOfChange);

        if (event.causeOfChange == MediaAction.PULL_ALL_MEDIA) {
            assertEquals(TEST_EVENTS.PULLED_ALL_MEDIA, mExpectedEvent);
        } else if (event.causeOfChange == MediaAction.UPLOAD_MEDIA) {
            assertEquals(TEST_EVENTS.UPLOADED_MEDIA, mExpectedEvent);
            mLastUploadedId = event.media.get(0).getMediaId();
        } else if (event.causeOfChange == MediaAction.PUSH_MEDIA) {
            assertEquals(TEST_EVENTS.PUSHED_MEDIA, mExpectedEvent);
        } else if (event.causeOfChange == MediaAction.DELETE_MEDIA) {
            assertEquals(TEST_EVENTS.DELETED_MEDIA, mExpectedEvent);
        } else if (event.causeOfChange == MediaAction.PULL_MEDIA) {
            assertEquals(TEST_EVENTS.PULLED_MEDIA, mExpectedEvent);
            if (mExpectedIds != null) {
                assertTrue(mExpectedIds.contains(event.media.get(0).getMediaId()));
            }
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSiteChanged(SiteStore.OnSiteChanged event) {
        AppLog.i(AppLog.T.TESTS, "site count " + mSiteStore.getSitesCount());
        assertEquals(true, mSiteStore.hasDotOrgSite());
        assertEquals(TEST_EVENTS.SITE_CHANGED, mExpectedEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaError(MediaStore.OnMediaError event) {
        if (event.mediaError == MediaStore.MediaError.NULL_MEDIA_ARG) {
            assertEquals(TEST_EVENTS.NULL_ERROR, mExpectedEvent);
        } else if (event.mediaError == MediaStore.MediaError.MALFORMED_MEDIA_ARG) {
            assertEquals(TEST_EVENTS.MALFORMED_ERROR, mExpectedEvent);
        } else if (event.mediaError == MediaStore.MediaError.MEDIA_NOT_FOUND) {
            assertEquals(TEST_EVENTS.NOT_FOUND_ERROR, mExpectedEvent);
        }
        mCountDownLatch.countDown();
    }
}
