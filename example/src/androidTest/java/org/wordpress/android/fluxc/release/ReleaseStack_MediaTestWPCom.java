package org.wordpress.android.fluxc.release;

import android.util.Log;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListPayload;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaListFetched;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.utils.MediaUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_MediaTestWPCom extends ReleaseStack_WPComBase {
    @Inject MediaStore mMediaStore;

    private enum TestEvents {
        DELETED_MEDIA,
        FETCHED_MEDIA_LIST,
        FETCHED_KNOWN_IMAGES,
        PUSHED_MEDIA,
        UPLOADED_MEDIA,
        UPLOADED_MUTIPLE_MEDIA,
        UPLOADED_MUTIPLE_MEDIA_WITH_CANCEL,
        PUSH_ERROR,
        REMOVED_MEDIA,
    }

    private TestEvents mNextEvent;
    private long mLastUploadedId = -1L;

    private ArrayList mUploadedIds = new ArrayList();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);

        // Authenticate, fetch sites and initialize sSite
        init();
    }

    public void testDeleteMedia() throws InterruptedException {
        // upload media to guarantee media exists
        MediaModel testMedia = newMediaModel(BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE);
        mNextEvent = TestEvents.UPLOADED_MEDIA;
        uploadMedia(testMedia);

        // verify and set media ID
        assertTrue(mLastUploadedId >= 0);
        testMedia.setMediaId(mLastUploadedId);
        assertNotNull(mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId()));

        // delete media and verify it's not in the store
        mNextEvent = TestEvents.DELETED_MEDIA;
        deleteMedia(testMedia);
        assertNull(mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId()));
    }

    public void testFetchMediaList() throws InterruptedException {
        // upload media to guarantee media exists
        MediaModel testMedia = newMediaModel(BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE);
        mNextEvent = TestEvents.UPLOADED_MEDIA;
        uploadMedia(testMedia);

        // verify and set media ID
        assertTrue(mLastUploadedId >= 0);
        testMedia.setMediaId(mLastUploadedId);
        assertNotNull(mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId()));

        // remove all local media and verify store is empty
        mNextEvent = TestEvents.REMOVED_MEDIA;
        removeAllSiteMedia();
        assertTrue(mMediaStore.getAllSiteMedia(sSite).isEmpty());

        // fetch media list and verify store is not empty
        mNextEvent = TestEvents.FETCHED_MEDIA_LIST;
        fetchMediaList();
        assertFalse(mMediaStore.getAllSiteMedia(sSite).isEmpty());

        // delete test image
        mNextEvent = TestEvents.DELETED_MEDIA;
        deleteMedia(testMedia);
    }

    public void testFetchMedia() throws InterruptedException {
        // upload media to guarantee media exists
        MediaModel testMedia = newMediaModel(BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE);
        mNextEvent = TestEvents.UPLOADED_MEDIA;
        uploadMedia(testMedia);

        // verify and set media ID
        assertTrue(mLastUploadedId >= 0);
        testMedia.setMediaId(mLastUploadedId);
        assertNotNull(mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId()));

        // remove local media and verify it's not in the store
        mNextEvent = TestEvents.REMOVED_MEDIA;
        removeMedia(testMedia);
        assertNull(mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId()));

        // fetch test media from remote and verify it's in the store
        mNextEvent = TestEvents.FETCHED_KNOWN_IMAGES;
        fetchMedia(testMedia);
        assertNotNull(mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId()));

        // delete test image
        mNextEvent = TestEvents.DELETED_MEDIA;
        deleteMedia(testMedia);
    }

    public void testEditMedia() throws InterruptedException {
        // upload media to guarantee media exists
        MediaModel testMedia = newMediaModel(BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE);
        mNextEvent = TestEvents.UPLOADED_MEDIA;
        uploadMedia(testMedia);

        // verify and set media ID
        assertTrue(mLastUploadedId >= 0);
        testMedia.setMediaId(mLastUploadedId);
        assertNotNull(mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId()));

        // create a random title and push changes
        testMedia.setTitle(RandomStringUtils.randomAlphabetic(8));
        mNextEvent = TestEvents.PUSHED_MEDIA;
        pushMedia(testMedia);

        // verify store media has been updated
        MediaModel storeMedia = mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId());
        assertNotNull(storeMedia);
        assertEquals(testMedia.getTitle(), storeMedia.getTitle());

        // delete test image
        mNextEvent = TestEvents.DELETED_MEDIA;
        deleteMedia(testMedia);
    }

    public void testEditNonexistentMedia() throws InterruptedException {
        // create media with invalid ID
        MediaModel testMedia = newMediaModel(BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE);
        testMedia.setMediaId(-1);

        // push media and verify
        mNextEvent = TestEvents.PUSH_ERROR;
        pushMedia(testMedia);
        assertNull(mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId()));
    }

    public void testUploadImage() throws InterruptedException {
        // upload media to guarantee media exists
        MediaModel testMedia = newMediaModel(BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE);
        mNextEvent = TestEvents.UPLOADED_MEDIA;
        uploadMedia(testMedia);

        // verify and set media ID
        assertTrue(mLastUploadedId >= 0);
        testMedia.setMediaId(mLastUploadedId);
        assertNotNull(mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId()));

        // delete test image
        mNextEvent = TestEvents.DELETED_MEDIA;
        deleteMedia(testMedia);
    }

    public void testUploadMultipleImages() throws InterruptedException {
        // upload media to guarantee media exists
        mUploadedIds = new ArrayList();
        mNextEvent = TestEvents.UPLOADED_MUTIPLE_MEDIA;

        ArrayList<MediaModel> mediaModels = new ArrayList<>();
        mediaModels.add(newMediaModel("Test media 1", BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE));
        mediaModels.add(newMediaModel("Test media 2", BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE));
        mediaModels.add(newMediaModel("Test media 3", BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE));
        mediaModels.add(newMediaModel("Test media 4", BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE));
        mediaModels.add(newMediaModel("Test media 5", BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE));

        // upload media, dispatching all at a time (not waiting for each to finish)
        uploadMultipleMedia(mediaModels, false);

        // verify all have been uploaded
        assertTrue(mUploadedIds.size() == mediaModels.size());

        // now set media ID to each one, verify they exist in the MediaStore
        for (int i=0; i < mediaModels.size(); i++) {
            MediaModel media = mediaModels.get(i);
            media.setMediaId((Long)mUploadedIds.get(i));
            assertNotNull(mMediaStore.getSiteMediaWithId(sSite, media.getMediaId()));
        }

        // delete test images (bear in mind this is done sequentially)
        mNextEvent = TestEvents.DELETED_MEDIA;
        for (int i=0; i < mediaModels.size(); i++) {
            MediaModel media = mediaModels.get(i);
            deleteMedia(media);
        }
    }

    public void testUploadMultipleImagesAndCancel() throws InterruptedException {
        // upload media to guarantee media exists
        mUploadedIds = new ArrayList();
        mNextEvent = TestEvents.UPLOADED_MUTIPLE_MEDIA_WITH_CANCEL;

        ArrayList<MediaModel> mediaModels = new ArrayList<>();
        mediaModels.add(newMediaModel("Test media 1", BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE));
        mediaModels.add(newMediaModel("Test media 2", BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE));
        mediaModels.add(newMediaModel("Test media 3", BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE));
        mediaModels.add(newMediaModel("Test media 4", BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE));
        mediaModels.add(newMediaModel("Test media 5", BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE));

        // upload media, dispatching all at a time (not waiting for each to finish)
        uploadMultipleMedia(mediaModels, true);

        // verify none have been uploaded
        assertTrue(mUploadedIds.size() == 0);
    }

    public void testUploadVideo() throws InterruptedException {
        // upload media to guarantee media exists
        MediaModel testMedia = newMediaModel(BuildConfig.TEST_LOCAL_VIDEO, MediaUtils.MIME_TYPE_VIDEO);
        mNextEvent = TestEvents.UPLOADED_MEDIA;
        uploadMedia(testMedia);

        // verify and set media ID
        assertTrue(mLastUploadedId >= 0);
        testMedia.setMediaId(mLastUploadedId);
        assertNotNull(mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId()));

        // delete test image
        mNextEvent = TestEvents.DELETED_MEDIA;
        deleteMedia(testMedia);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaUploaded(OnMediaUploaded event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        if (event.canceled) {
            assertEquals(TestEvents.UPLOADED_MUTIPLE_MEDIA_WITH_CANCEL, mNextEvent);
            mCountDownLatch.countDown();
        }
        else
        if (event.completed) {
            if (mNextEvent == TestEvents.UPLOADED_MUTIPLE_MEDIA) {
                assertEquals(TestEvents.UPLOADED_MUTIPLE_MEDIA, mNextEvent);
                Long mediaId = new Long(event.media.getMediaId());
                Log.d("TEST", "mediaId is: " + mediaId);
                mUploadedIds.add(mediaId);
            } else {
                assertEquals(TestEvents.UPLOADED_MEDIA, mNextEvent);
                mLastUploadedId = event.media.getMediaId();
            }
            mCountDownLatch.countDown();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaChanged(OnMediaChanged event) {
        if (event.isError()) {
            if (event.cause == MediaAction.PUSH_MEDIA) {
                assertEquals(TestEvents.PUSH_ERROR, mNextEvent);
            } else {
                throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
            }
            mCountDownLatch.countDown();
            return;
        }
        if (event.cause == MediaAction.FETCHED_MEDIA_LIST) {
            assertEquals(TestEvents.FETCHED_MEDIA_LIST, mNextEvent);
        } else if (event.cause == MediaAction.FETCH_MEDIA) {
            if (eventHasKnownImages(event)) {
                assertEquals(TestEvents.FETCHED_KNOWN_IMAGES, mNextEvent);
            }
        } else if (event.cause == MediaAction.PUSH_MEDIA) {
            assertEquals(TestEvents.PUSHED_MEDIA, mNextEvent);
        } else if (event.cause == MediaAction.DELETE_MEDIA) {
            assertEquals(TestEvents.DELETED_MEDIA, mNextEvent);
        } else if (event.cause == MediaAction.REMOVE_MEDIA) {
            assertEquals(TestEvents.REMOVED_MEDIA, mNextEvent);
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaListFetched(OnMediaListFetched event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(TestEvents.FETCHED_MEDIA_LIST, mNextEvent);
        mCountDownLatch.countDown();
    }

    private boolean eventHasKnownImages(OnMediaChanged event) {
        if (event == null || event.mediaList == null || event.mediaList.isEmpty()) return false;
        String[] splitIds = BuildConfig.TEST_WPCOM_IMAGE_IDS_TEST1.split(",");
        if (splitIds.length != event.mediaList.size()) return false;
        for (MediaModel mediaItem : event.mediaList) {
            if (!ArrayUtils.contains(splitIds, String.valueOf(mediaItem.getMediaId()))) return false;
        }
        return true;
    }

    private MediaModel newMediaModel(String mediaPath, String mimeType) {
        return newMediaModel("Test Title", mediaPath, mimeType);
    }

    private MediaModel newMediaModel(String testTitle, String mediaPath, String mimeType) {
        final String testDescription = "Test Description";
        final String testCaption = "Test Caption";
        final String testAlt = "Test Alt";

        MediaModel testMedia = new MediaModel();
        testMedia.setFilePath(mediaPath);
        testMedia.setFileExtension(mediaPath.substring(mediaPath.lastIndexOf(".") + 1, mediaPath.length()));
        testMedia.setMimeType(mimeType + testMedia.getFileExtension());
        testMedia.setFileName(mediaPath.substring(mediaPath.lastIndexOf("/"), mediaPath.length()));
        testMedia.setTitle(testTitle);
        testMedia.setDescription(testDescription);
        testMedia.setCaption(testCaption);
        testMedia.setAlt(testAlt);
        testMedia.setLocalSiteId(sSite.getId());

        return testMedia;
    }

    private void pushMedia(MediaModel media) throws InterruptedException {
        MediaPayload payload = new MediaPayload(sSite, media);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newPushMediaAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchMediaList() throws InterruptedException {
        FetchMediaListPayload fetchPayload = new FetchMediaListPayload(sSite, false);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newFetchMediaListAction(fetchPayload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchMedia(MediaModel media) throws InterruptedException {
        MediaPayload fetchPayload = new MediaPayload(sSite, media, null);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newFetchMediaAction(fetchPayload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void uploadMedia(MediaModel media) throws InterruptedException {
        MediaPayload payload = new MediaPayload(sSite, media);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void uploadMultipleMedia(List<MediaModel> mediaList, boolean cancel) throws InterruptedException {
        mCountDownLatch = new CountDownLatch(mediaList.size());
        for (MediaModel media : mediaList) {
            MediaPayload payload = new MediaPayload(sSite, media);
            mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
        }

        if (cancel) {
            // wait a bit and issue the cancel command
            TestUtils.waitFor(5000);

            for (MediaModel media : mediaList) {
                MediaPayload payload = new MediaPayload(sSite, media);
                mDispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload));
            }
        }

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void deleteMedia(MediaModel media) throws InterruptedException {
        MediaPayload deletePayload = new MediaPayload(sSite, media);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newDeleteMediaAction(deletePayload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void removeMedia(MediaModel media) throws InterruptedException {
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newRemoveMediaAction(media));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void removeAllSiteMedia() throws InterruptedException {
        List<MediaModel> allMedia = mMediaStore.getAllSiteMedia(sSite);
        if (!allMedia.isEmpty()) {
            for (MediaModel media : allMedia) {
                removeMedia(media);
            }
        }
    }
}
