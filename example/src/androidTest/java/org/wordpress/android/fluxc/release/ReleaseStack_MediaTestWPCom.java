package org.wordpress.android.fluxc.release;

import android.annotation.SuppressLint;

import junit.framework.Assert;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListPayload;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaListFetched;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

@SuppressLint("UseSparseArrays")
public class ReleaseStack_MediaTestWPCom extends ReleaseStack_WPComBase {
    @Inject MediaStore mMediaStore;

    private enum TestEvents {
        CANCELED_MEDIA,
        DELETED_MEDIA,
        FETCHED_MEDIA_LIST,
        FETCHED_MEDIA_IMAGE_LIST,
        FETCHED_KNOWN_IMAGES,
        PUSHED_MEDIA,
        REMOVED_MEDIA,
        UPLOADED_MEDIA,
        UPLOADED_MULTIPLE_MEDIA, // these don't exist in FluxC, but are an artifact to wait for all uploads to finish
        UPLOADED_MULTIPLE_MEDIA_WITH_CANCEL, // same as above
        PUSH_ERROR
    }

    private TestEvents mNextEvent;
    private long mLastUploadedId = -1L;

    private List<Long> mUploadedIds = new ArrayList<>();
    private Map<Integer, MediaModel> mUploadedMediaModels = new HashMap<>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);

        // Authenticate, fetch sites and initialize sSite
        init();
    }

    public void testDeleteMedia() throws InterruptedException {
        // upload media to guarantee media exists
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        mNextEvent = TestEvents.UPLOADED_MEDIA;
        uploadMedia(testMedia);

        // verify and set media ID
        Assert.assertTrue(mLastUploadedId >= 0);
        testMedia.setMediaId(mLastUploadedId);
        Assert.assertNotNull(mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId()));

        // delete media and verify it's not in the store
        mNextEvent = TestEvents.DELETED_MEDIA;
        deleteMedia(testMedia);
        assertNull(mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId()));
    }

    public void testFetchMediaList() throws InterruptedException {
        // upload media to guarantee media exists
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        mNextEvent = TestEvents.UPLOADED_MEDIA;
        uploadMedia(testMedia);

        // verify and set media ID
        Assert.assertTrue(mLastUploadedId >= 0);
        testMedia.setMediaId(mLastUploadedId);
        Assert.assertNotNull(mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId()));

        // remove all local media and verify store is empty
        mNextEvent = TestEvents.REMOVED_MEDIA;
        removeAllSiteMedia();
        Assert.assertTrue(mMediaStore.getAllSiteMedia(sSite).isEmpty());

        // fetch media list and verify store is not empty
        mNextEvent = TestEvents.FETCHED_MEDIA_LIST;
        fetchMediaList();
        Assert.assertFalse(mMediaStore.getAllSiteMedia(sSite).isEmpty());

        // remove all media again
        mNextEvent = TestEvents.REMOVED_MEDIA;
        removeAllSiteMedia();
        Assert.assertTrue(mMediaStore.getAllSiteMedia(sSite).isEmpty());

        // fetch only images, verify store is not empty and contains only images
        mNextEvent = TestEvents.FETCHED_MEDIA_IMAGE_LIST;
        fetchMediaImageList();
        List<MediaModel> mediaList = mMediaStore.getSiteImages(sSite);
        Assert.assertFalse(mediaList.isEmpty());
        Assert.assertTrue(mMediaStore.getSiteMediaCount(sSite) == mediaList.size());

        // delete test image
        mNextEvent = TestEvents.DELETED_MEDIA;
        deleteMedia(testMedia);
    }

    public void testFetchMedia() throws InterruptedException {
        // upload media to guarantee media exists
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        mNextEvent = TestEvents.UPLOADED_MEDIA;
        uploadMedia(testMedia);

        // verify and set media ID
        Assert.assertTrue(mLastUploadedId >= 0);
        testMedia.setMediaId(mLastUploadedId);
        Assert.assertNotNull(mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId()));

        // remove local media and verify it's not in the store
        mNextEvent = TestEvents.REMOVED_MEDIA;
        removeMedia(testMedia);
        assertNull(mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId()));

        // fetch test media from remote and verify it's in the store
        mNextEvent = TestEvents.FETCHED_KNOWN_IMAGES;
        fetchMedia(testMedia);
        Assert.assertNotNull(mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId()));

        // delete test image
        mNextEvent = TestEvents.DELETED_MEDIA;
        deleteMedia(testMedia);
    }

    public void testEditMedia() throws InterruptedException {
        // upload media to guarantee media exists
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        mNextEvent = TestEvents.UPLOADED_MEDIA;
        uploadMedia(testMedia);

        // verify and set media ID
        Assert.assertTrue(mLastUploadedId >= 0);
        testMedia.setMediaId(mLastUploadedId);
        Assert.assertNotNull(mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId()));

        // create a random title and push changes
        testMedia.setTitle(RandomStringUtils.randomAlphabetic(8));
        mNextEvent = TestEvents.PUSHED_MEDIA;
        pushMedia(testMedia);

        // verify store media has been updated
        MediaModel storeMedia = mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId());
        Assert.assertNotNull(storeMedia);
        Assert.assertEquals(testMedia.getTitle(), storeMedia.getTitle());

        // delete test image
        mNextEvent = TestEvents.DELETED_MEDIA;
        deleteMedia(testMedia);
    }

    public void testEditNonexistentMedia() throws InterruptedException {
        // create media with invalid ID
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        testMedia.setMediaId(-1);

        // push media and verify
        mNextEvent = TestEvents.PUSH_ERROR;
        pushMedia(testMedia);
        assertNull(mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId()));
    }

    public void testUploadImage() throws InterruptedException {
        // upload media to guarantee media exists
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        mNextEvent = TestEvents.UPLOADED_MEDIA;
        uploadMedia(testMedia);

        // verify and set media ID
        Assert.assertTrue(mLastUploadedId >= 0);
        testMedia.setMediaId(mLastUploadedId);
        Assert.assertNotNull(mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId()));

        // delete test image
        mNextEvent = TestEvents.DELETED_MEDIA;
        deleteMedia(testMedia);
    }

    public void testUploadImageAttachedToPost() throws InterruptedException {
        // Upload media attached to remotely saved post
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        testMedia.setLocalPostId(5);
        testMedia.setPostId(1);
        mNextEvent = TestEvents.UPLOADED_MEDIA;
        uploadMedia(testMedia);

        testMedia.setMediaId(mLastUploadedId);
        MediaModel uploadedMedia = mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId());
        Assert.assertNotNull(uploadedMedia);
        Assert.assertEquals(1, uploadedMedia.getPostId());
        Assert.assertEquals(5, uploadedMedia.getLocalPostId());

        mNextEvent = TestEvents.DELETED_MEDIA;
        deleteMedia(testMedia);

        // Upload media attached to a local draft
        testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        testMedia.setLocalPostId(5);
        mNextEvent = TestEvents.UPLOADED_MEDIA;
        uploadMedia(testMedia);

        testMedia.setMediaId(mLastUploadedId);
        uploadedMedia = mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId());
        Assert.assertNotNull(uploadedMedia);
        Assert.assertEquals(0, uploadedMedia.getPostId());
        Assert.assertEquals(5, uploadedMedia.getLocalPostId());

        mNextEvent = TestEvents.DELETED_MEDIA;
        deleteMedia(testMedia);
    }

    public void testCancelImageUpload() throws InterruptedException {
        // First, try canceling an image with the default behavior (canceled image is deleted from the store)
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.CANCELED_MEDIA;
        MediaPayload payload = new MediaPayload(sSite, testMedia);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));

        // Wait a bit and issue the cancel command
        TestUtils.waitFor(1000);

        CancelMediaPayload cancelPayload = new CancelMediaPayload(sSite, testMedia);
        mDispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(cancelPayload));

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        Assert.assertEquals(0, mMediaStore.getSiteMediaCount(sSite));

        // Now, try canceling with delete=false (canceled image should be marked as failed and kept in the store)
        testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.CANCELED_MEDIA;
        payload = new MediaPayload(sSite, testMedia);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));

        // Wait a bit and issue the cancel command
        TestUtils.waitFor(1000);

        cancelPayload = new CancelMediaPayload(sSite, testMedia, false);
        mDispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(cancelPayload));

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        Assert.assertEquals(1, mMediaStore.getSiteMediaCount(sSite));
        MediaModel canceledMedia = mMediaStore.getMediaWithLocalId(testMedia.getId());
        Assert.assertEquals(MediaUploadState.FAILED.toString(), canceledMedia.getUploadState());
    }

    public void testUploadMultipleImages() throws InterruptedException {
        // upload media to guarantee media exists
        mUploadedIds = new ArrayList<>();
        mNextEvent = TestEvents.UPLOADED_MULTIPLE_MEDIA;

        mUploadedMediaModels = new HashMap<>();
        // here we use the newMediaModel() with id builder, as we need it to identify uploads
        addMediaModelToUploadArray("Test media 1");
        addMediaModelToUploadArray("Test media 2");
        addMediaModelToUploadArray("Test media 3");
        addMediaModelToUploadArray("Test media 4");
        addMediaModelToUploadArray("Test media 5");

        // upload media, dispatching all at a time (not waiting for each to finish)
        uploadMultipleMedia(new ArrayList<>(mUploadedMediaModels.values()));

        // verify all have been uploaded
        Assert.assertEquals(mUploadedMediaModels.size(), mUploadedIds.size());
        Assert.assertEquals(mUploadedMediaModels.size(),
                mMediaStore.getSiteMediaWithState(sSite, MediaUploadState.UPLOADED).size());

        // verify they exist in the MediaStore
        Iterator<MediaModel> iterator = mUploadedMediaModels.values().iterator();
        while (iterator.hasNext()) {
            MediaModel media = iterator.next();
            Assert.assertNotNull(mMediaStore.getSiteMediaWithId(sSite, media.getMediaId()));
        }

        // delete test images (bear in mind this is done sequentially)
        mNextEvent = TestEvents.DELETED_MEDIA;
        iterator = mUploadedMediaModels.values().iterator();
        while (iterator.hasNext()) {
            MediaModel media = iterator.next();
            deleteMedia(media);
        }
    }

    public void testUploadMultipleImagesAndCancel() throws InterruptedException {
        // upload media to guarantee media exists
        mUploadedIds = new ArrayList<>();
        mNextEvent = TestEvents.UPLOADED_MULTIPLE_MEDIA_WITH_CANCEL;

        mUploadedMediaModels = new HashMap<>();
        // here we use the newMediaModel() with id builder, as we need it to identify uploads
        addMediaModelToUploadArray("Test media 1");
        addMediaModelToUploadArray("Test media 2");
        addMediaModelToUploadArray("Test media 3");
        addMediaModelToUploadArray("Test media 4");
        addMediaModelToUploadArray("Test media 5");

        // use this variable to test cancelling 1, 2, 3, 4 or all 5 uploads
        int amountToCancel = 4;

        // upload media, dispatching all at a time (not waiting for each to finish)
        // also cancel (and delete) the first n=`amountToCancel` media uploads
        uploadMultipleMedia(new ArrayList<>(mUploadedMediaModels.values()), amountToCancel, true);

        // verify how many have been uploaded
        Assert.assertEquals(mUploadedMediaModels.size() - amountToCancel, mUploadedIds.size());

        // verify each one of the remaining, non-cancelled uploads exist in the MediaStore
        for (long mediaId : mUploadedIds) {
            Assert.assertNotNull(mMediaStore.getSiteMediaWithId(sSite, mediaId));
        }

        // Only completed uploads should exist in the store
        Assert.assertEquals(mUploadedIds.size(), mMediaStore.getSiteMediaCount(sSite));
        // The number of uploaded media in the store should match our records of how many were not cancelled
        Assert.assertEquals(mUploadedIds.size(), mMediaStore.getSiteMediaWithState(sSite, MediaUploadState.UPLOADED).size());

        // delete test images (bear in mind this is done sequentially)
        mNextEvent = TestEvents.DELETED_MEDIA;
        for (MediaModel media : mUploadedMediaModels.values()) {
            // delete only successfully uploaded test images
            if (mUploadedIds.contains(media.getMediaId())) {
                deleteMedia(media);
            }
        }
    }

    public void testUploadMultipleImagesAndCancelWithoutDeleting() throws InterruptedException {
        // upload media to guarantee media exists
        mUploadedIds = new ArrayList<>();
        mNextEvent = TestEvents.UPLOADED_MULTIPLE_MEDIA_WITH_CANCEL;

        mUploadedMediaModels = new HashMap<>();
        // here we use the newMediaModel() with id builder, as we need it to identify uploads
        addMediaModelToUploadArray("Test media 1");
        addMediaModelToUploadArray("Test media 2");
        addMediaModelToUploadArray("Test media 3");
        addMediaModelToUploadArray("Test media 4");
        addMediaModelToUploadArray("Test media 5");

        // use this variable to test cancelling 1, 2, 3, 4 or all 5 uploads
        int amountToCancel = 4;

        // upload media, dispatching all at a time (not waiting for each to finish)
        // also cancel (without deleting) the first n=`amountToCancel` media uploads
        uploadMultipleMedia(new ArrayList<>(mUploadedMediaModels.values()), amountToCancel, false);

        // verify how many have been uploaded
        Assert.assertEquals(mUploadedMediaModels.size() - amountToCancel, mUploadedIds.size());

        // verify each one of the remaining, non-cancelled uploads exist in the MediaStore
        for (long mediaId : mUploadedIds) {
            Assert.assertNotNull(mMediaStore.getSiteMediaWithId(sSite, mediaId));
        }

        // All the original uploads should exist in the store, whether cancelled or not
        Assert.assertEquals(mUploadedMediaModels.size(), mMediaStore.getSiteMediaCount(sSite));
        // The number of uploaded media in the store should match our records of how many were not cancelled
        Assert.assertEquals(mUploadedIds.size(), mMediaStore.getSiteMediaWithState(sSite, MediaUploadState.UPLOADED).size());
        // All cancelled media should have a FAILED state
        Assert.assertEquals(amountToCancel, mMediaStore.getSiteMediaWithState(sSite, MediaUploadState.FAILED).size());

        // delete test images (bear in mind this is done sequentially)
        mNextEvent = TestEvents.DELETED_MEDIA;
        for (MediaModel media : mUploadedMediaModels.values()) {
            // delete only successfully uploaded test images
            if (mUploadedIds.contains(media.getMediaId())) {
                deleteMedia(media);
            }
        }
    }

    public void testUploadVideo() throws InterruptedException {
        // upload media to guarantee media exists
        MediaModel testMedia = newMediaModel(getSampleVideoPath(), MediaUtils.MIME_TYPE_VIDEO);
        mNextEvent = TestEvents.UPLOADED_MEDIA;
        uploadMedia(testMedia);

        // verify and set media ID
        Assert.assertTrue(mLastUploadedId >= 0);
        testMedia.setMediaId(mLastUploadedId);
        Assert.assertNotNull(mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId()));

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
            if (mNextEvent == TestEvents.CANCELED_MEDIA
                    || mNextEvent == TestEvents.UPLOADED_MULTIPLE_MEDIA_WITH_CANCEL) {
                mCountDownLatch.countDown();
            } else {
                throw new AssertionError("Unexpected cancellation for media: " + event.media.getId());
            }
        } else if (event.completed) {
            if (mNextEvent == TestEvents.UPLOADED_MULTIPLE_MEDIA_WITH_CANCEL) {
                mUploadedIds.add(event.media.getMediaId());
                // now update our own map object with the new media id
                MediaModel media = mUploadedMediaModels.get(event.media.getId());
                if (media != null) {
                    media.setMediaId(event.media.getMediaId());
                } else {
                    AppLog.e(AppLog.T.MEDIA, "mediamodel not found: " + event.media.getId());
                }
                Assert.assertNotNull(media);
            } else if (mNextEvent == TestEvents.UPLOADED_MULTIPLE_MEDIA) {
                mUploadedIds.add(event.media.getMediaId());
                // now update our own map object with the new media id
                MediaModel media = mUploadedMediaModels.get(event.media.getId());
                if (media != null) {
                    media.setMediaId(event.media.getMediaId());
                } else {
                    AppLog.e(AppLog.T.MEDIA, "mediamodel not found: " + event.media.getId());
                }
                Assert.assertNotNull(media);
            } else if (mNextEvent == TestEvents.UPLOADED_MEDIA) {
                mLastUploadedId = event.media.getMediaId();
            } else {
                throw new AssertionError("Unexpected completion for media: " + event.media.getId());
            }
            mCountDownLatch.countDown();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaChanged(OnMediaChanged event) {
        if (event.isError()) {
            if (event.cause == MediaAction.PUSH_MEDIA) {
                Assert.assertEquals(TestEvents.PUSH_ERROR, mNextEvent);
            } else {
                throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
            }
            mCountDownLatch.countDown();
            return;
        }
        if (event.cause == MediaAction.FETCH_MEDIA) {
            if (eventHasKnownImages(event)) {
                Assert.assertEquals(TestEvents.FETCHED_KNOWN_IMAGES, mNextEvent);
            }
        } else if (event.cause == MediaAction.PUSH_MEDIA) {
            Assert.assertEquals(TestEvents.PUSHED_MEDIA, mNextEvent);
        } else if (event.cause == MediaAction.DELETE_MEDIA) {
            Assert.assertEquals(TestEvents.DELETED_MEDIA, mNextEvent);
        } else if (event.cause == MediaAction.REMOVE_MEDIA) {
            Assert.assertEquals(TestEvents.REMOVED_MEDIA, mNextEvent);
        } else {
            throw new AssertionError("Unexpected event: " + event.cause);
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaListFetched(OnMediaListFetched event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        boolean isMediaListEvent = mNextEvent == TestEvents.FETCHED_MEDIA_LIST
                || mNextEvent == TestEvents.FETCHED_MEDIA_IMAGE_LIST;
        Assert.assertTrue(isMediaListEvent);
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

    private void addMediaModelToUploadArray(String title) {
        MediaModel mediaModel = newMediaModel(title, getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        mUploadedMediaModels.put(mediaModel.getId(), mediaModel);
    }

    private MediaModel newMediaModel(String mediaPath, String mimeType) {
        return newMediaModel("Test Title", mediaPath, mimeType);
    }

    private MediaModel newMediaModel(String testTitle, String mediaPath, String mimeType) {
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
        testMedia.setLocalSiteId(sSite.getId());

        return testMedia;
    }

    private void pushMedia(MediaModel media) throws InterruptedException {
        MediaPayload payload = new MediaPayload(sSite, media);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newPushMediaAction(payload));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchMediaList() throws InterruptedException {
        FetchMediaListPayload fetchPayload = new FetchMediaListPayload(
                sSite, MediaStore.DEFAULT_NUM_MEDIA_PER_FETCH, false);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newFetchMediaListAction(fetchPayload));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchMediaImageList() throws InterruptedException {
        FetchMediaListPayload fetchPayload = new FetchMediaListPayload(
                sSite, MediaStore.DEFAULT_NUM_MEDIA_PER_FETCH, false, MediaUtils.MIME_TYPE_IMAGE);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newFetchMediaListAction(fetchPayload));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchMedia(MediaModel media) throws InterruptedException {
        MediaPayload fetchPayload = new MediaPayload(sSite, media, null);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newFetchMediaAction(fetchPayload));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void uploadMedia(MediaModel media) throws InterruptedException {
        MediaPayload payload = new MediaPayload(sSite, media);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void uploadMultipleMedia(List<MediaModel> mediaList) throws InterruptedException {
        uploadMultipleMedia(mediaList, 0, false);
    }

    private void uploadMultipleMedia(List<MediaModel> mediaList, int howManyFirstToCancel, boolean delete)
            throws InterruptedException {
        mCountDownLatch = new CountDownLatch(mediaList.size());
        for (MediaModel media : mediaList) {
            MediaPayload payload = new MediaPayload(sSite, media);
            mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
        }

        if (howManyFirstToCancel > 0 && howManyFirstToCancel <= mediaList.size()) {
            // wait a bit and issue the cancel command
            TestUtils.waitFor(1000);

            // we'e only cancelling the first n=howManyFirstToCancel uploads
            for (int i = 0; i < howManyFirstToCancel; i++) {
                MediaModel media = mediaList.get(i);
                CancelMediaPayload payload = new CancelMediaPayload(sSite, media, delete);
                mDispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload));
            }
        }

        Assert.assertTrue(mCountDownLatch.await(TestUtils.MULTIPLE_UPLOADS_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void deleteMedia(MediaModel media) throws InterruptedException {
        MediaPayload deletePayload = new MediaPayload(sSite, media);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newDeleteMediaAction(deletePayload));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void removeMedia(MediaModel media) throws InterruptedException {
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newRemoveMediaAction(media));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
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
