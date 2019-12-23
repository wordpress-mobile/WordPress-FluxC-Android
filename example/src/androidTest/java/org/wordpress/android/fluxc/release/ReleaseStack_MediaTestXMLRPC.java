package org.wordpress.android.fluxc.release;

import android.annotation.SuppressLint;

import org.apache.commons.lang3.RandomStringUtils;
import org.greenrobot.eventbus.Subscribe;
import org.junit.Test;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListPayload;
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaListFetched;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.store.MediaStore.UploadMediaPayload;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressLint("UseSparseArrays")
public class ReleaseStack_MediaTestXMLRPC extends ReleaseStack_XMLRPCBase {
    @SuppressWarnings("unused")
    @Inject AccountStore mAccountStore;
    @SuppressWarnings("unused")
    @Inject HTTPAuthManager mHTTPAuthManager;
    @SuppressWarnings("unused")
    @Inject MemorizingTrustManager mMemorizingTrustManager;
    @Inject MediaStore mMediaStore;

    private enum TestEvents {
        NONE,
        CANCELED_MEDIA,
        DELETED_MEDIA,
        FETCHED_MEDIA_LIST,
        FETCHED_MEDIA_IMAGE_LIST,
        FETCHED_MEDIA,
        PUSHED_MEDIA,
        REMOVED_MEDIA,
        UPLOADED_MEDIA,
        UPLOADED_MULTIPLE_MEDIA, // these don't exist in FluxC, but are an artifact to wait for all uploads to finish
        UPLOADED_MULTIPLE_MEDIA_WITH_CANCEL, // same as above
        NULL_ERROR,
        MALFORMED_ERROR,
        NOT_FOUND_ERROR
    }

    private TestEvents mNextEvent;
    private long mLastUploadedId = -1L;

    private List<Long> mUploadedIds = new ArrayList<>();
    private Map<Integer, MediaModel> mUploadedMediaModels = new HashMap<>();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);

        // Register and initialize sSite
        init();
        mNextEvent = TestEvents.NONE;
    }

    @Test
    public void testDeleteMedia() throws InterruptedException {
        // upload media to guarantee media exists
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
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

    @Test
    public void testDeleteMediaThatDoesNotExist() throws InterruptedException {
        MediaModel testMedia = new MediaModel();
        testMedia.setMediaId(9999999L);
        mNextEvent = TestEvents.NOT_FOUND_ERROR;
        deleteMedia(testMedia);
    }

    @Test
    public void testFetchMediaList() throws InterruptedException {
        // upload media to guarantee media exists
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
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

        // remove all media again
        mNextEvent = TestEvents.REMOVED_MEDIA;
        removeAllSiteMedia();
        assertTrue(mMediaStore.getAllSiteMedia(sSite).isEmpty());

        // fetch only images, verify store is not empty and contains only images
        mNextEvent = TestEvents.FETCHED_MEDIA_IMAGE_LIST;
        fetchMediaImageList();
        List<MediaModel> mediaList = mMediaStore.getSiteImages(sSite);
        assertFalse(mediaList.isEmpty());
        assertEquals(mediaList.size(), mMediaStore.getSiteMediaCount(sSite));

        // delete test image
        mNextEvent = TestEvents.DELETED_MEDIA;
        deleteMedia(testMedia);
    }

    @Test
    public void testFetchMediaThatDoesNotExist() throws InterruptedException {
        List<Long> mediaIds = new ArrayList<>();
        mediaIds.add(9999999L);
        mediaIds.add(9999989L);
        mNextEvent = TestEvents.NOT_FOUND_ERROR;
        for (Long id : mediaIds) {
            MediaModel media = new MediaModel();
            media.setMediaId(id);
            fetchMedia(media);
        }
    }

    @Test
    public void testFetchMediaThatExists() throws InterruptedException {
        // upload media to guarantee media exists
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
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
        mNextEvent = TestEvents.FETCHED_MEDIA;
        fetchMedia(testMedia);
        assertNotNull(mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId()));

        // delete test image
        mNextEvent = TestEvents.DELETED_MEDIA;
        deleteMedia(testMedia);
    }

    @Test
    public void testEditMedia() throws InterruptedException {
        // upload media to guarantee media exists
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
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

    @Test
    public void testUploadImage() throws InterruptedException {
        // upload media to guarantee media exists
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
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

    @Test
    public void testUploadImageAttachedToPost() throws InterruptedException {
        // Upload media attached to remotely saved post
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        testMedia.setLocalPostId(5);
        testMedia.setPostId(1);
        mNextEvent = TestEvents.UPLOADED_MEDIA;
        uploadMedia(testMedia);

        testMedia.setMediaId(mLastUploadedId);
        MediaModel uploadedMedia = mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId());
        assertNotNull(uploadedMedia);
        assertEquals(1, uploadedMedia.getPostId());
        assertEquals(5, uploadedMedia.getLocalPostId());

        mNextEvent = TestEvents.DELETED_MEDIA;
        deleteMedia(testMedia);

        // Upload media attached to a local draft
        testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        testMedia.setLocalPostId(5);
        mNextEvent = TestEvents.UPLOADED_MEDIA;
        uploadMedia(testMedia);

        testMedia.setMediaId(mLastUploadedId);
        uploadedMedia = mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId());
        assertNotNull(uploadedMedia);
        assertEquals(0, uploadedMedia.getPostId());
        assertEquals(5, uploadedMedia.getLocalPostId());

        mNextEvent = TestEvents.DELETED_MEDIA;
        deleteMedia(testMedia);
    }

    @Test
    public void testCancelImageUpload() throws InterruptedException {
        // First, try canceling an image with the default behavior (canceled image is deleted from the store)
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.CANCELED_MEDIA;
        UploadMediaPayload payload = new UploadMediaPayload(sSite, testMedia, true);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));

        // Wait a bit and issue the cancel command
        TestUtils.waitFor(1000);

        CancelMediaPayload cancelPayload = new CancelMediaPayload(sSite, testMedia);
        mDispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(cancelPayload));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals(0, mMediaStore.getSiteMediaCount(sSite));

        // Now, try canceling with delete=false (canceled image should be marked as failed and kept in the store)
        testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.CANCELED_MEDIA;
        payload = new UploadMediaPayload(sSite, testMedia, true);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));

        // Wait a bit and issue the cancel command
        TestUtils.waitFor(1000);

        cancelPayload = new CancelMediaPayload(sSite, testMedia, false);
        mDispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(cancelPayload));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals(1, mMediaStore.getSiteMediaCount(sSite));
        MediaModel canceledMedia = mMediaStore.getMediaWithLocalId(testMedia.getId());
        assertEquals(MediaUploadState.FAILED.toString(), canceledMedia.getUploadState());
    }

    @Test
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
        assertEquals(mUploadedMediaModels.size() - amountToCancel, mUploadedIds.size());

        // verify each one of the remaining, non-cancelled uploads exist in the MediaStore
        for (long mediaId : mUploadedIds) {
            assertNotNull(mMediaStore.getSiteMediaWithId(sSite, mediaId));
        }

        // Only completed uploads should exist in the store
        assertEquals(mUploadedIds.size(), mMediaStore.getSiteImageCount(sSite));
        // The number of uploaded media in the store should match our records of how many were not cancelled
        assertEquals(mUploadedIds.size(), mMediaStore.getSiteMediaWithState(sSite, MediaUploadState.UPLOADED).size());

        // delete test images (bear in mind this is done sequentially)
        mNextEvent = TestEvents.DELETED_MEDIA;
        for (MediaModel media : mUploadedMediaModels.values()) {
            // delete only successfully uploaded test images
            if (mUploadedIds.contains(media.getMediaId())) {
                deleteMedia(media);
            }
        }
    }

    @Test
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
        assertEquals(mUploadedMediaModels.size() - amountToCancel, mUploadedIds.size());

        // verify each one of the remaining, non-cancelled uploads exist in the MediaStore
        for (long mediaId : mUploadedIds) {
            assertNotNull(mMediaStore.getSiteMediaWithId(sSite, mediaId));
        }

        // All the original uploads should exist in the store, whether cancelled or not
        assertEquals(mUploadedMediaModels.size(), mMediaStore.getSiteMediaCount(sSite));
        // The number of uploaded media in the store should match our records of how many were not cancelled
        assertEquals(mUploadedIds.size(), mMediaStore.getSiteMediaWithState(sSite, MediaUploadState.UPLOADED).size());
        // All cancelled media should have a FAILED state
        assertEquals(amountToCancel, mMediaStore.getSiteMediaWithState(sSite, MediaUploadState.FAILED).size());

        // delete test images (bear in mind this is done sequentially)
        mNextEvent = TestEvents.DELETED_MEDIA;
        for (MediaModel media : mUploadedMediaModels.values()) {
            // delete only successfully uploaded test images
            if (mUploadedIds.contains(media.getMediaId())) {
                deleteMedia(media);
            }
        }
    }

    @Test
    public void testUploadVideo() throws InterruptedException {
        // upload media to guarantee media exists
        MediaModel testMedia = newMediaModel(getSampleVideoPath(), MediaUtils.MIME_TYPE_VIDEO);
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

    @Test
    public void testUploadImageOlderWordPress() throws InterruptedException {
        // Before WordPress 4.4, a separate call to wp.getMediaItem was necessary after wp.uploadFile completed.
        // This is a regression test making sure we're falling back to that behaviour when expected field are missing
        // (using a WordPress 4.3 site).
        SiteModel site = new SiteModel();
        site.setId(2);
        site.setSelfHostedSiteId(0);
        site.setUsername(BuildConfig.TEST_WPORG_USERNAME_SH_WORDPRESS_4_3);
        site.setPassword(BuildConfig.TEST_WPORG_PASSWORD_SH_WORDPRESS_4_3);
        site.setXmlRpcUrl(BuildConfig.TEST_WPORG_URL_SH_WORDPRESS_4_3_ENDPOINT);

        // upload media to guarantee media exists
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        mNextEvent = TestEvents.UPLOADED_MEDIA;

        UploadMediaPayload payload = new UploadMediaPayload(site, testMedia, true);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // verify and set media ID
        assertTrue(mLastUploadedId >= 0);
        testMedia.setMediaId(mLastUploadedId);
        assertNotNull(mMediaStore.getSiteMediaWithId(site, testMedia.getMediaId()));

        // delete test image
        mNextEvent = TestEvents.DELETED_MEDIA;
        MediaPayload deletePayload = new MediaPayload(site, testMedia);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newDeleteMediaAction(deletePayload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testUploadVideoOlderWordPress() throws InterruptedException {
        // Before WordPress 4.4, a separate call to wp.getMediaItem was necessary after wp.uploadFile completed.
        // This is a regression test making sure we're falling back to that behaviour when expected field are missing
        // (using a WordPress 4.3 site).
        SiteModel site = new SiteModel();
        site.setId(2);
        site.setSelfHostedSiteId(0);
        site.setUsername(BuildConfig.TEST_WPORG_USERNAME_SH_WORDPRESS_4_3);
        site.setPassword(BuildConfig.TEST_WPORG_PASSWORD_SH_WORDPRESS_4_3);
        site.setXmlRpcUrl(BuildConfig.TEST_WPORG_URL_SH_WORDPRESS_4_3_ENDPOINT);

        // upload media to guarantee media exists
        MediaModel testMedia = newMediaModel(getSampleVideoPath(), MediaUtils.MIME_TYPE_VIDEO);
        mNextEvent = TestEvents.UPLOADED_MEDIA;

        UploadMediaPayload payload = new UploadMediaPayload(site, testMedia, true);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // verify and set media ID
        assertTrue(mLastUploadedId >= 0);
        testMedia.setMediaId(mLastUploadedId);
        assertNotNull(mMediaStore.getSiteMediaWithId(site, testMedia.getMediaId()));

        // delete test image
        mNextEvent = TestEvents.DELETED_MEDIA;
        MediaPayload deletePayload = new MediaPayload(site, testMedia);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newDeleteMediaAction(deletePayload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
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
                assertNotNull(media);
            } else if (mNextEvent == TestEvents.UPLOADED_MULTIPLE_MEDIA) {
                mUploadedIds.add(event.media.getMediaId());
                // now update our own map object with the new media id
                MediaModel media = mUploadedMediaModels.get(event.media.getId());
                if (media != null) {
                    media.setMediaId(event.media.getMediaId());
                } else {
                    AppLog.e(AppLog.T.MEDIA, "mediamodel not found: " + event.media.getId());
                }
                assertNotNull(media);
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
        AppLog.d(AppLog.T.TESTS, "tests.onMediaChanged: " + event.cause);

        if (event.isError()) {
            if (event.error.type == MediaErrorType.NULL_MEDIA_ARG) {
                assertEquals(TestEvents.NULL_ERROR, mNextEvent);
            } else if (event.error.type == MediaErrorType.MALFORMED_MEDIA_ARG) {
                assertEquals(TestEvents.MALFORMED_ERROR, mNextEvent);
            } else if (event.error.type == MediaErrorType.NOT_FOUND) {
                assertEquals(TestEvents.NOT_FOUND_ERROR, mNextEvent);
            } else {
                throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
            }
        } else {
            if (event.cause == MediaAction.FETCH_MEDIA) {
                assertEquals(TestEvents.FETCHED_MEDIA, mNextEvent);
            } else if (event.cause == MediaAction.PUSH_MEDIA) {
                assertEquals(TestEvents.PUSHED_MEDIA, mNextEvent);
            } else if (event.cause == MediaAction.DELETE_MEDIA) {
                assertEquals(TestEvents.DELETED_MEDIA, mNextEvent);
            } else if (event.cause == MediaAction.REMOVE_MEDIA) {
                assertEquals(TestEvents.REMOVED_MEDIA, mNextEvent);
            } else {
                throw new AssertionError("Unexpected event: " + event.cause);
            }
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
        assertTrue(isMediaListEvent);
        mCountDownLatch.countDown();
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
        testMedia.setFileExtension(mediaPath.substring(mediaPath.lastIndexOf(".") + 1));
        testMedia.setMimeType(mimeType + testMedia.getFileExtension());
        testMedia.setFileName(mediaPath.substring(mediaPath.lastIndexOf("/")));
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
        FetchMediaListPayload fetchPayload = new FetchMediaListPayload(
                sSite, MediaStore.DEFAULT_NUM_MEDIA_PER_FETCH, false);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newFetchMediaListAction(fetchPayload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchMediaImageList() throws InterruptedException {
        FetchMediaListPayload fetchPayload = new FetchMediaListPayload(
                sSite, MediaStore.DEFAULT_NUM_MEDIA_PER_FETCH, false, MediaUtils.MIME_TYPE_IMAGE);
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
        UploadMediaPayload payload = new UploadMediaPayload(sSite, media, true);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void uploadMultipleMedia(List<MediaModel> mediaList, int howManyFirstToCancel, boolean delete)
            throws InterruptedException {
        mCountDownLatch = new CountDownLatch(mediaList.size());
        for (MediaModel media : mediaList) {
            // Don't strip location, as all media are the same file and we end up with concurrent read/writes
            UploadMediaPayload payload = new UploadMediaPayload(sSite, media, false);
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

        assertTrue(mCountDownLatch.await(TestUtils.MULTIPLE_UPLOADS_TIMEOUT_MS, TimeUnit.MILLISECONDS));
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
