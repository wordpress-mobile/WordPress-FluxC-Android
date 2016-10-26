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

public class ReleaseStack_MediaTestXMLRPC extends ReleaseStack_Base {
    private final String TEST_TITLE = "Test Title";
    private final String TEST_DESCRIPTION = "Test Description";
    private final String TEST_CAPTION = "Test Caption";
    private final String TEST_ALT = "Test Alt";

    @SuppressWarnings("unused") @Inject AccountStore mAccountStore;
    @SuppressWarnings("unused") @Inject HTTPAuthManager mHTTPAuthManager;
    @SuppressWarnings("unused") @Inject MemorizingTrustManager mMemorizingTrustManager;
    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;
    @Inject SiteStore mSiteStore;

    private enum TEST_EVENTS {
        NONE,
        AUTHENTICATION_CHANGED,
        SITE_CHANGED,
        PUSHED_MEDIA,
        FETCHED_ALL_MEDIA,
        FETCHED_MEDIA,
        DELETED_MEDIA,
        UPLOADED_MEDIA,
        NULL_ERROR,
        MALFORMED_ERROR,
        NOT_FOUND_ERROR
    }

    private SiteModel mSite;
    private TEST_EVENTS mExpectedEvent;
    private TEST_EVENTS mNextExpectedEvent;
    private CountDownLatch mCountDownLatch;
    private AccountStore.OnDiscoveryResponse mDiscovered;
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
        mSite = mSiteStore.getSites().get(0);
        pushMedia(mSite, null, TEST_EVENTS.NULL_ERROR);
    }

    /**
     * Push action that supplies media without required data should result in a malformed exception.
     */
    public void testPushMalformedMedia() throws InterruptedException {
        mSite = mSiteStore.getSites().get(0);
        final MediaModel testMedia = getTestMedia(TEST_TITLE, null, null, null);
        pushMedia(mSite, testMedia, TEST_EVENTS.MALFORMED_ERROR);
    }

    /**
     * Push action that references media that exists remotely should update remote properties and
     * not trigger an upload.
     */
    public void testPushMediaChanges() throws InterruptedException {
        // fetch site media
        mSite = mSiteStore.getSites().get(0);
        fetchAllMedia(mSite);

        // some media is expected
        List<MediaModel> siteMedia = mMediaStore.getAllSiteMedia(mSite);
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
        pushMedia(mSite, testMedia, TEST_EVENTS.PUSHED_MEDIA);

        // verify local media has changes
        final MediaModel updatedMedia = mMediaStore.getSiteMediaWithId(mSite, testId);
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
        pushMedia(mSite, testMedia, TEST_EVENTS.PUSHED_MEDIA);

        // verify restored media properties
        final MediaModel restoredMedia = mMediaStore.getSiteMediaWithId(mSite, testId);
        assertEquals(restoredMedia.getTitle(), mediaTitle);
        assertEquals(restoredMedia.getDescription(), mediaDescription);
        assertEquals(restoredMedia.getCaption(), mediaCaption);
        assertEquals(restoredMedia.getAlt(), mediaAlt);
    }

    /**
     * Upload a local image.
     */
    public void testUploadImage() throws InterruptedException {
        mSite = mSiteStore.getSites().get(0);

        MediaModel media = getTestMedia(TEST_TITLE, TEST_DESCRIPTION, TEST_CAPTION, TEST_ALT);
        String imagePath = BuildConfig.TEST_LOCAL_IMAGE;
        media.setFilePath(imagePath);
        media.setFileName(MediaUtils.getFileName(imagePath));
        media.setFileExtension(MediaUtils.getExtension(imagePath));
        media.setMimeType(MediaUtils.MIME_TYPE_IMAGE + media.getFileExtension());
        media.setSiteId(mSite.getSelfHostedSiteId());

        uploadMedia(mSite, media);
    }

    /**
     * Upload a local video.
     */
    public void testUploadVideo() throws InterruptedException {
        mSite = mSiteStore.getSites().get(0);

        MediaModel media = getTestMedia(TEST_TITLE, TEST_DESCRIPTION, TEST_CAPTION, TEST_ALT);
        String videoPath = BuildConfig.TEST_LOCAL_VIDEO;
        media.setFilePath(videoPath);
        media.setFileName(MediaUtils.getFileName(videoPath));
        media.setFileExtension(MediaUtils.getExtension(videoPath));
        media.setMimeType(MediaUtils.MIME_TYPE_VIDEO + media.getFileExtension());
        media.setSiteId(mSite.getSelfHostedSiteId());

        uploadMedia(mSite, media);
    }

    /**
     * Fetch all action should gather all media from a site.
     */
    public void testFetchAllMedia() throws InterruptedException {
        mSite = mSiteStore.getSites().get(0);
        fetchAllMedia(mSite);
    }

    /**
     * Fetch action with no media supplied results in a media exception.
     */
    public void testFetchNullMedia() throws InterruptedException {
        mSite = mSiteStore.getSites().get(0);
        fetchSpecificMedia(mSite, null, TEST_EVENTS.NULL_ERROR);
    }

    /**
     * Fetch action with media that does not exist results in a media not found exception.
     */
    public void testFetchMediaThatDoesNotExist() throws InterruptedException {
        mSite = mSiteStore.getSites().get(0);
        List<Long> mediaIds = new ArrayList<>();
        mediaIds.add(9999999L);
        mediaIds.add(9999989L);
        fetchSpecificMedia(mSite, mediaIds, TEST_EVENTS.NOT_FOUND_ERROR);
    }

    /**
     * Fetch action should gather only media items whose ID is in the given filter.
     */
    public void testFetchMediaThatExists() throws InterruptedException {
        // get all site media
        mSite = mSiteStore.getSites().get(0);
        fetchAllMedia(mSite);

        final List<MediaModel> siteMedia = mMediaStore.getAllSiteMedia(mSite);
        assertFalse(siteMedia.isEmpty());

        // fetch half of the media
        final int half = siteMedia.size() / 2;
        final List<Long> halfMediaIds = new ArrayList<>(half);
        for (int i = 0; i < half; ++i) {
            halfMediaIds.add(siteMedia.get(i).getMediaId());
        }
        fetchSpecificMedia(mSite, halfMediaIds, TEST_EVENTS.FETCHED_MEDIA);
    }

    /**
     * Delete action on null media results in a null error.
     */
    public void testDeleteNullMedia() throws InterruptedException {
        mSite = mSiteStore.getSites().get(0);
        deleteMedia(mSite, null, TEST_EVENTS.NULL_ERROR, 1);
    }

    /**
     * Delete action on media that doesn't exist should not result in an exception.
     */
    public void testDeleteMediaThatDoesNotExist() throws InterruptedException {
        mSite = mSiteStore.getSites().get(0);
        MediaModel testMedia = new MediaModel();
        testMedia.setMediaId(9999999L);
        deleteMedia(mSite, testMedia, TEST_EVENTS.NOT_FOUND_ERROR, 1);
    }

    /**
     * Delete action on media that exists should result in no media on a fetch request.
     */
    public void testDeleteMediaThatExists() throws InterruptedException {
        // upload media
        mSite = mSiteStore.getSites().get(0);
        MediaModel media = getTestMedia(TEST_TITLE, TEST_DESCRIPTION, TEST_CAPTION, TEST_ALT);
        String imagePath = BuildConfig.TEST_LOCAL_IMAGE;
        media.setFilePath(imagePath);
        media.setFileName(MediaUtils.getFileName(imagePath));
        media.setFileExtension(MediaUtils.getExtension(imagePath));
        media.setMimeType(MediaUtils.MIME_TYPE_IMAGE + media.getFileExtension());
        media.setSiteId(mSite.getSelfHostedSiteId());
        MediaStore.UploadMediaPayload payload = new MediaStore.UploadMediaPayload(mSite, media);
        mExpectedEvent = TEST_EVENTS.UPLOADED_MEDIA;
        mNextExpectedEvent = TEST_EVENTS.DELETED_MEDIA;
        mCountDownLatch = new CountDownLatch(2);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
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
        if (count > 0) {
            mCountDownLatch = new CountDownLatch(count);
        }
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
        MediaStore.MediaListPayload payload = new MediaStore.MediaListPayload(MediaAction.PUSH_MEDIA, site, mediaList);
        dispatchAction(expectedEvent, MediaActionBuilder.newPushMediaAction(payload), 1);
    }

    private void uploadMedia(SiteModel site, MediaModel media) throws InterruptedException {
        MediaStore.UploadMediaPayload payload = new MediaStore.UploadMediaPayload(site, media);
        dispatchAction(TEST_EVENTS.UPLOADED_MEDIA, MediaActionBuilder.newUploadMediaAction(payload), 1);
    }

    private void fetchAllMedia(SiteModel site) throws InterruptedException {
        MediaStore.MediaListPayload mediaPayload = new MediaStore.MediaListPayload(MediaAction.FETCH_ALL_MEDIA, site, null);
        dispatchAction(TEST_EVENTS.FETCHED_ALL_MEDIA, MediaActionBuilder.newFetchAllMediaAction(mediaPayload), 1);
    }

    private void fetchSpecificMedia(SiteModel site, List<Long> mediaIds, TEST_EVENTS expectedEvent) throws InterruptedException {
        mExpectedIds = mediaIds;
        if (mExpectedIds == null) {
            MediaStore.MediaListPayload mediaPayload = new MediaStore.MediaListPayload(MediaAction.FETCH_MEDIA, site, null);
            dispatchAction(expectedEvent, MediaActionBuilder.newFetchMediaAction(mediaPayload), 1);
        } else {
            int size = mExpectedIds.size();
            List<MediaModel> mediaList = new ArrayList<>();
            for (Long id : mediaIds) {
                MediaModel media = new MediaModel();
                media.setMediaId(id);
                mediaList.add(media);
            }
            MediaStore.MediaListPayload mediaPayload = new MediaStore.MediaListPayload(MediaAction.FETCH_MEDIA, site, mediaList);
            dispatchAction(expectedEvent, MediaActionBuilder.newFetchMediaAction(mediaPayload), size);
        }
    }

    private void deleteMedia(SiteModel site, MediaModel media, TEST_EVENTS expectedEvent, int num) throws InterruptedException {
        List<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(media);
        MediaStore.MediaListPayload deletePayload = new MediaStore.MediaListPayload(MediaAction.DELETE_MEDIA, site, mediaList);
        dispatchAction(expectedEvent, MediaActionBuilder.newDeleteMediaAction(deletePayload), num);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onDiscoverySucceeded(AccountStore.OnDiscoveryResponse event) {
        assertEquals(TEST_EVENTS.AUTHENTICATION_CHANGED, mExpectedEvent);
        mDiscovered = event;
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaUploaded(MediaStore.OnMediaUploaded event) throws InterruptedException {
        assertFalse(event.isError());
        mLastUploadedId = event.media.getMediaId();
        if (event.completed) {
            assertEquals(TEST_EVENTS.UPLOADED_MEDIA, mExpectedEvent);
            if (mNextExpectedEvent != null) {
                if (mNextExpectedEvent == TEST_EVENTS.DELETED_MEDIA) {
                    event.media.setMediaId(mLastUploadedId);
                    deleteMedia(mSite, event.media, mNextExpectedEvent, -1);
                    mNextExpectedEvent = null;
                }
            } else {
                assertTrue(mLastUploadedId > 0);
            }
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaChanged(MediaStore.OnMediaChanged event) {
        AppLog.d(AppLog.T.TESTS, "tests.onMediaChanged: " + event.cause);

        if (event.isError()) {
            if (event.error.type == MediaStore.MediaErrorType.NULL_MEDIA_ARG) {
                assertEquals(TEST_EVENTS.NULL_ERROR, mExpectedEvent);
            } else if (event.error.type == MediaStore.MediaErrorType.MALFORMED_MEDIA_ARG) {
                assertEquals(TEST_EVENTS.MALFORMED_ERROR, mExpectedEvent);
            } else if (event.error.type == MediaStore.MediaErrorType.MEDIA_NOT_FOUND) {
                assertEquals(TEST_EVENTS.NOT_FOUND_ERROR, mExpectedEvent);
            }
        } else {
            if (event.cause == MediaAction.FETCH_ALL_MEDIA) {
                assertEquals(TEST_EVENTS.FETCHED_ALL_MEDIA, mExpectedEvent);
            } else if (event.cause == MediaAction.FETCH_MEDIA) {
                assertEquals(TEST_EVENTS.FETCHED_MEDIA, mExpectedEvent);
                if (mExpectedIds != null) {
                    assertTrue(mExpectedIds.contains(event.media.get(0).getMediaId()));
                }
            } else if (event.cause == MediaAction.PUSH_MEDIA) {
                assertEquals(TEST_EVENTS.PUSHED_MEDIA, mExpectedEvent);
            } else if (event.cause == MediaAction.DELETE_MEDIA) {
                assertEquals(TEST_EVENTS.DELETED_MEDIA, mExpectedEvent);
            }
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSiteChanged(SiteStore.OnSiteChanged event) {
        AppLog.i(AppLog.T.TESTS, "site count " + mSiteStore.getSitesCount());
        assertEquals(true, mSiteStore.hasSelfHostedSite());
        assertEquals(TEST_EVENTS.SITE_CHANGED, mExpectedEvent);
        mCountDownLatch.countDown();
    }
}
