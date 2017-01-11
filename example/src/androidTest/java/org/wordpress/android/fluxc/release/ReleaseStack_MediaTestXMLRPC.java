package org.wordpress.android.fluxc.release;

//import org.greenrobot.eventbus.Subscribe;
//import org.wordpress.android.fluxc.TestUtils;
//import org.wordpress.android.fluxc.action.MediaAction;
//import org.wordpress.android.fluxc.annotations.action.Action;
//import org.wordpress.android.fluxc.example.BuildConfig;
//import org.wordpress.android.fluxc.generated.MediaActionBuilder;
//import org.wordpress.android.fluxc.model.MediaModel;
//import org.wordpress.android.fluxc.network.HTTPAuthManager;
//import org.wordpress.android.fluxc.network.MemorizingTrustManager;
//import org.wordpress.android.fluxc.store.AccountStore;
//import org.wordpress.android.fluxc.store.MediaStore;
//import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;
//import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged;
//import org.wordpress.android.fluxc.store.MediaStore.MediaListPayload;
//import org.wordpress.android.fluxc.store.MediaStore.UploadMediaPayload;
//import org.wordpress.android.fluxc.utils.MediaUtils;
//import org.wordpress.android.util.AppLog;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.TimeUnit;
//
//import javax.inject.Inject;

/**
 PUSH_MEDIA
    1. Pushing a null media list results in error of type NULL_MEDIA_ARG
        {@link #testPushNullMedia()}
    2. Pushing a media list in which not all media has required data results in error of type MALFORMED_MEDIA_ARG
        {@link #testPushMalformedMedia()}
    3. Pushing changes to existing remote media results in OnMediaChanged caused by PUSH_MEDIA
        {@link #testPushMediaChanges()}

 UPLOAD_MEDIA
    1. Uploading an image results in OnMediaUploaded caused by UPLOAD_MEDIA
        {@link #testUploadImage()}
    2. Uploading a video results in OnMediaUploaded caused by UPLOAD_MEDIA
        {@link #testUploadVideo()}

 FETCH_ALL_MEDIA
    1. Fetching all media results in OnMediaChanged caused by FETCH_ALL_MEDIA
        {@link #testFetchAllMedia()}

 FETCH_MEDIA
    1. Fetching a null media list results in error of type NULL_MEDIA_ARG
        {@link #testFetchNullMedia()}
    2. Fetching media that doesn't exist remotely results in error of type MEDIA_NOT_FOUND
        {@link #testFetchMediaThatDoesNotExist()}
    3. Fetching valid media results in OnMediaChanged caused by FETCH_MEDIA
        {@link #testFetchMediaThatExists()}

 DELETE_MEDIA
    1. Deleting a null media list results in error of type NULL_MEDIA_ARG
        {@link #testDeleteNullMedia()}
    2. Deleting media that doesn't exist remotely results in error of type MEDIA_NOT_FOUND
        {@link #testDeleteMediaThatDoesNotExist()}
    3. Deleting valid media results in OnMediaChanged caused by DELETE_MEDIA
        {@link #testDeleteMediaThatExists()}
 */

public class ReleaseStack_MediaTestXMLRPC extends ReleaseStack_XMLRPCBase {
//    private static final String TEST_TITLE = "Test Title";
//    private static final String TEST_DESCRIPTION = "Test Description";
//    private static final String TEST_CAPTION = "Test Caption";
//    private static final String TEST_ALT = "Test Alt";
//
//    @SuppressWarnings("unused") @Inject AccountStore mAccountStore;
//    @SuppressWarnings("unused") @Inject HTTPAuthManager mHTTPAuthManager;
//    @SuppressWarnings("unused") @Inject MemorizingTrustManager mMemorizingTrustManager;
//    @Inject MediaStore mMediaStore;
//
//    private enum TestEvents {
//        NONE,
//        PUSHED_MEDIA,
//        FETCHED_ALL_MEDIA,
//        FETCHED_MEDIA,
//        DELETED_MEDIA,
//        UPLOADED_MEDIA,
//        NULL_ERROR,
//        MALFORMED_ERROR,
//        NOT_FOUND_ERROR
//    }
//
//    private TestEvents mNextEvent;
//    private TestEvents mNextExpectedEvent;
//    private List<Long> mExpectedIds;
//
//    @Override
//    protected void setUp() throws Exception {
//        super.setUp();
//        mReleaseStackAppComponent.inject(this);
//
//        // Register and initialize sSite
//        init();
//        mNextEvent = TestEvents.NONE;
//    }
//
//    public void testPushNullMedia() throws InterruptedException {
//        pushMedia(null, TestEvents.NULL_ERROR);
//    }
//
//    public void testPushMalformedMedia() throws InterruptedException {
//        final MediaModel testMedia = getTestMedia(TEST_TITLE, TEST_DESCRIPTION, TEST_CAPTION, TEST_ALT);
//        testMedia.setMediaId(-1);
//        pushMedia(testMedia, TestEvents.MALFORMED_ERROR);
//    }
//
//    public void testPushMediaChanges() throws InterruptedException {
//        // fetch site media
//        fetchAllMedia();
//
//        // some media is expected
//        List<MediaModel> siteMedia = mMediaStore.getAllSiteMedia(sSite);
//        assertFalse(siteMedia.isEmpty());
//
//        // store existing properties for restoration
//        final MediaModel testMedia = siteMedia.get(0);
//        final long testId = testMedia.getMediaId();
//        final String mediaTitle = testMedia.getTitle();
//        final String mediaDescription = testMedia.getDescription();
//        final String mediaCaption = testMedia.getCaption();
//        final String mediaAlt = testMedia.getAlt();
//
//        // update properties to test pushing changes
//        final String newTitle = mediaTitle + TestUtils.randomString(5);
//        final String newDescription = mediaDescription + TestUtils.randomString(5);
//        final String newCaption = mediaCaption + TestUtils.randomString(5);
//        final String newAlt = mediaAlt + TestUtils.randomString(5);
//        testMedia.setTitle(newTitle);
//        testMedia.setDescription(newDescription);
//        testMedia.setCaption(newCaption);
//        testMedia.setAlt(newAlt);
//
//        // push changes
//        pushMedia(testMedia, TestEvents.PUSHED_MEDIA);
//
//        // verify local media has changes
//        final MediaModel updatedMedia = mMediaStore.getSiteMediaWithId(sSite, testId);
//        assertNotNull(updatedMedia);
//        assertEquals(updatedMedia.getTitle(), newTitle);
//        assertEquals(updatedMedia.getDescription(), newDescription);
//        assertEquals(updatedMedia.getCaption(), newCaption);
//        assertEquals(updatedMedia.getAlt(), newAlt);
//
//        // reset media properties
//        testMedia.setTitle(mediaTitle);
//        testMedia.setDescription(mediaDescription);
//        testMedia.setCaption(mediaCaption);
//        testMedia.setAlt(mediaAlt);
//        pushMedia(testMedia, TestEvents.PUSHED_MEDIA);
//
//        // verify restored media properties
//        final MediaModel restoredMedia = mMediaStore.getSiteMediaWithId(sSite, testId);
//        assertEquals(restoredMedia.getTitle(), mediaTitle);
//        assertEquals(restoredMedia.getDescription(), mediaDescription);
//        assertEquals(restoredMedia.getCaption(), mediaCaption);
//        assertEquals(restoredMedia.getAlt(), mediaAlt);
//    }
//
//    public void testUploadImage() throws InterruptedException {
//        MediaModel media = getTestMedia(TEST_TITLE, TEST_DESCRIPTION, TEST_CAPTION, TEST_ALT);
//        String imagePath = BuildConfig.TEST_LOCAL_IMAGE;
//        media.setFilePath(imagePath);
//        media.setFileName(MediaUtils.getFileName(imagePath));
//        media.setFileExtension(MediaUtils.getExtension(imagePath));
//        media.setMimeType(MediaUtils.MIME_TYPE_IMAGE + media.getFileExtension());
//        media.setSiteId(sSite.getSelfHostedSiteId());
//
//        uploadMedia(media);
//    }
//
//    public void testUploadVideo() throws InterruptedException {
//        MediaModel media = getTestMedia(TEST_TITLE, TEST_DESCRIPTION, TEST_CAPTION, TEST_ALT);
//        String videoPath = BuildConfig.TEST_LOCAL_VIDEO;
//        media.setFilePath(videoPath);
//        media.setFileName(MediaUtils.getFileName(videoPath));
//        media.setFileExtension(MediaUtils.getExtension(videoPath));
//        media.setMimeType(MediaUtils.MIME_TYPE_VIDEO + media.getFileExtension());
//        media.setSiteId(sSite.getSelfHostedSiteId());
//
//        uploadMedia(media);
//    }
//
//    public void testFetchAllMedia() throws InterruptedException {
//        fetchAllMedia();
//    }
//
//    public void testFetchNullMedia() throws InterruptedException {
//        fetchSpecificMedia(null, TestEvents.NULL_ERROR);
//    }
//
//    public void testFetchMediaThatDoesNotExist() throws InterruptedException {
//        List<Long> mediaIds = new ArrayList<>();
//        mediaIds.add(9999999L);
//        mediaIds.add(9999989L);
//        fetchSpecificMedia(mediaIds, TestEvents.NOT_FOUND_ERROR);
//    }
//
//    public void testFetchMediaThatExists() throws InterruptedException {
//        // get all site media
//        fetchAllMedia();
//
//        final List<MediaModel> siteMedia = mMediaStore.getAllSiteMedia(sSite);
//        assertFalse(siteMedia.isEmpty());
//
//        // fetch half of the media
//        final int half = siteMedia.size() / 2;
//        final List<Long> halfMediaIds = new ArrayList<>(half);
//        for (int i = 0; i < half; ++i) {
//            halfMediaIds.add(siteMedia.get(i).getMediaId());
//        }
//        fetchSpecificMedia(halfMediaIds, TestEvents.FETCHED_MEDIA);
//    }
//
//    public void testDeleteNullMedia() throws InterruptedException {
//        deleteMedia(null, TestEvents.NULL_ERROR, 1);
//    }
//
//    public void testDeleteMediaThatDoesNotExist() throws InterruptedException {
//        MediaModel testMedia = new MediaModel();
//        testMedia.setMediaId(9999999L);
//        deleteMedia(testMedia, TestEvents.NOT_FOUND_ERROR, 1);
//    }
//
//    public void testDeleteMediaThatExists() throws InterruptedException {
//        // upload media
//        MediaModel media = getTestMedia(TEST_TITLE, TEST_DESCRIPTION, TEST_CAPTION, TEST_ALT);
//        String imagePath = BuildConfig.TEST_LOCAL_IMAGE;
//        media.setFilePath(imagePath);
//        media.setFileName(MediaUtils.getFileName(imagePath));
//        media.setFileExtension(MediaUtils.getExtension(imagePath));
//        media.setMimeType(MediaUtils.MIME_TYPE_IMAGE + media.getFileExtension());
//        media.setSiteId(sSite.getSelfHostedSiteId());
//        UploadMediaPayload payload = new UploadMediaPayload(sSite, media);
//        mNextEvent = TestEvents.UPLOADED_MEDIA;
//        mNextExpectedEvent = TestEvents.DELETED_MEDIA;
//        mCountDownLatch = new CountDownLatch(2);
//        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
//        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
//    }
//
//    @SuppressWarnings("unused")
//    @Subscribe
//    public void onMediaUploaded(MediaStore.OnMediaUploaded event) throws InterruptedException {
//        if (event.completed) {
//            assertEquals(TestEvents.UPLOADED_MEDIA, mNextEvent);
//            if (mNextExpectedEvent == TestEvents.DELETED_MEDIA) {
//                deleteMedia(event.media, mNextExpectedEvent, -1);
//                mNextExpectedEvent = null;
//            } else {
//                assertTrue(event.media.getMediaId() > 0);
//            }
//            mCountDownLatch.countDown();
//        } else if (event.isError()) {
//            mCountDownLatch.countDown();
//        }
//    }
//
//    @SuppressWarnings("unused")
//    @Subscribe
//    public void onMediaChanged(OnMediaChanged event) {
//        AppLog.d(AppLog.T.TESTS, "tests.onMediaChanged: " + event.cause);
//
//        if (event.isError()) {
//            if (event.error.type == MediaErrorType.NULL_MEDIA_ARG) {
//                assertEquals(TestEvents.NULL_ERROR, mNextEvent);
//            } else if (event.error.type == MediaErrorType.MALFORMED_MEDIA_ARG) {
//                assertEquals(TestEvents.MALFORMED_ERROR, mNextEvent);
//            } else if (event.error.type == MediaErrorType.MEDIA_NOT_FOUND) {
//                assertEquals(TestEvents.NOT_FOUND_ERROR, mNextEvent);
//            } else {
//                throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
//            }
//        } else {
//            if (event.cause == MediaAction.FETCH_ALL_MEDIA) {
//                assertEquals(TestEvents.FETCHED_ALL_MEDIA, mNextEvent);
//            } else if (event.cause == MediaAction.FETCH_MEDIA) {
//                assertEquals(TestEvents.FETCHED_MEDIA, mNextEvent);
//                if (mExpectedIds != null) {
//                    assertTrue(mExpectedIds.contains(event.media.get(0).getMediaId()));
//                }
//            } else if (event.cause == MediaAction.PUSH_MEDIA) {
//                assertEquals(TestEvents.PUSHED_MEDIA, mNextEvent);
//            } else if (event.cause == MediaAction.DELETE_MEDIA) {
//                assertEquals(TestEvents.DELETED_MEDIA, mNextEvent);
//            }
//        }
//        mCountDownLatch.countDown();
//    }
//
//    private MediaModel getTestMedia(String title, String description, String caption, String alt) {
//        MediaModel media = new MediaModel();
//        media.setTitle(title);
//        media.setDescription(description);
//        media.setCaption(caption);
//        media.setAlt(alt);
//        return media;
//    }
//
//    private void dispatchAction(TestEvents expectedEvent, Action action, int count) throws InterruptedException {
//        mNextEvent = expectedEvent;
//        if (count > 0) {
//            mCountDownLatch = new CountDownLatch(count);
//        }
//        mDispatcher.dispatch(action);
//        if (count > 0) {
//            assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
//        }
//    }
//
//    private void pushMedia(MediaModel media, TestEvents expectedEvent) throws InterruptedException {
//        List<MediaModel> mediaList = new ArrayList<>();
//        mediaList.add(media);
//        MediaListPayload payload = new MediaListPayload(MediaAction.PUSH_MEDIA, sSite, mediaList);
//        dispatchAction(expectedEvent, MediaActionBuilder.newPushMediaAction(payload), 1);
//    }
//
//    private void uploadMedia(MediaModel media) throws InterruptedException {
//        UploadMediaPayload payload = new UploadMediaPayload(sSite, media);
//        dispatchAction(TestEvents.UPLOADED_MEDIA, MediaActionBuilder.newUploadMediaAction(payload), 1);
//    }
//
//    private void fetchAllMedia() throws InterruptedException {
//        MediaListPayload mediaPayload = new MediaListPayload(MediaAction.FETCH_ALL_MEDIA, sSite, null);
//        dispatchAction(TestEvents.FETCHED_ALL_MEDIA, MediaActionBuilder.newFetchAllMediaAction(mediaPayload), 1);
//    }
//
//    private void fetchSpecificMedia(List<Long> mediaIds, TestEvents expectedEvent)
//            throws InterruptedException {
//        mExpectedIds = mediaIds;
//        if (mExpectedIds == null) {
//            MediaListPayload mediaPayload = new MediaListPayload(MediaAction.FETCH_MEDIA, sSite, null);
//            dispatchAction(expectedEvent, MediaActionBuilder.newFetchMediaAction(mediaPayload), 1);
//        } else {
//            int size = mExpectedIds.size();
//            List<MediaModel> mediaList = new ArrayList<>();
//            for (Long id : mediaIds) {
//                MediaModel media = new MediaModel();
//                media.setMediaId(id);
//                mediaList.add(media);
//            }
//            MediaListPayload mediaPayload = new MediaListPayload(MediaAction.FETCH_MEDIA, sSite, mediaList);
//            dispatchAction(expectedEvent, MediaActionBuilder.newFetchMediaAction(mediaPayload), size);
//        }
//    }
//
//    private void deleteMedia(MediaModel media, TestEvents expectedEvent, int num)
//            throws InterruptedException {
//        List<MediaModel> mediaList = new ArrayList<>();
//        mediaList.add(media);
//        MediaListPayload deletePayload = new MediaListPayload(MediaAction.DELETE_MEDIA, sSite, mediaList);
//        dispatchAction(expectedEvent, MediaActionBuilder.newDeleteMediaAction(deletePayload), num);
//    }
}
