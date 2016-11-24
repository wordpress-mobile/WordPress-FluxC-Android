package org.wordpress.android.fluxc.release;

import org.apache.commons.lang.RandomStringUtils;
import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.MediaListPayload;
import org.wordpress.android.fluxc.store.MediaStore.UploadMediaPayload;
import org.wordpress.android.fluxc.utils.MediaUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_MediaTestWPCom extends ReleaseStack_WPComBase {
    @Inject MediaStore mMediaStore;

    enum TestEvents {
        DELETED_MEDIA,
        FETCHED_ALL_MEDIA,
        FETCHED_KNOWN_IMAGES,
        PUSHED_MEDIA,
        UPLOADED_MEDIA,
        PUSH_ERROR
    }

    private TestEvents mExpectedEvent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);

        // Authenticate, fetch sites and initialize sSite
        init();
    }

    public void testDeleteMedia() throws InterruptedException {
        // we first need to upload a new media to delete it
        MediaModel testMedia = newMediaModel(sSite, BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE);
        UploadMediaPayload payload = new UploadMediaPayload(sSite, testMedia);
        mExpectedEvent = TestEvents.UPLOADED_MEDIA;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        List<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(testMedia);
        MediaListPayload deletePayload = new MediaListPayload(MediaAction.DELETE_MEDIA, sSite, mediaList);
        mExpectedEvent = TestEvents.DELETED_MEDIA;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newDeleteMediaAction(deletePayload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testFetchAllMedia() throws InterruptedException {
        MediaListPayload fetchPayload = new MediaListPayload(MediaAction.FETCH_ALL_MEDIA, sSite, null);
        mExpectedEvent = TestEvents.FETCHED_ALL_MEDIA;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newFetchAllMediaAction(fetchPayload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testFetchSpecificMedia() throws InterruptedException {
        String knownImageIds = BuildConfig.TEST_WPCOM_IMAGE_IDS_TEST1;
        String[] splitIds = knownImageIds.split(",");
        List<MediaModel> mediaList = new ArrayList<>();
        for (String id : splitIds) {
            MediaModel media = new MediaModel();
            media.setMediaId(Long.valueOf(id));
            mediaList.add(media);
        }
        MediaListPayload payload = new MediaListPayload(MediaAction.FETCH_MEDIA, sSite, mediaList);
        mExpectedEvent = TestEvents.FETCHED_KNOWN_IMAGES;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newFetchMediaAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testPushExistingMedia() throws InterruptedException {
        MediaModel testMedia = new MediaModel();
        // use existing media
        testMedia.setMediaId(Long.parseLong(BuildConfig.TEST_WPCOM_IMAGE_ID_TO_CHANGE));
        // create a random title
        testMedia.setTitle(RandomStringUtils.randomAlphabetic(8));
        List<MediaModel> media = new ArrayList<>();
        media.add(testMedia);
        MediaListPayload payload = new MediaListPayload(MediaAction.PUSH_MEDIA, sSite, media);
        mExpectedEvent = TestEvents.PUSHED_MEDIA;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newPushMediaAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testPushNewMedia() throws InterruptedException {
        MediaModel testMedia = newMediaModel(sSite, BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE);
        List<MediaModel> media = new ArrayList<>();
        media.add(testMedia);
        MediaListPayload payload = new MediaListPayload(MediaAction.PUSH_MEDIA, sSite, media);
        mExpectedEvent = TestEvents.PUSH_ERROR;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newPushMediaAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testUploadImage() throws InterruptedException {
        MediaModel media = newMediaModel(sSite, BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE);
        UploadMediaPayload payload = new UploadMediaPayload(sSite, media);
        mExpectedEvent = TestEvents.UPLOADED_MEDIA;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testUploadVideo() throws InterruptedException {
        MediaModel media = newMediaModel(sSite, BuildConfig.TEST_LOCAL_VIDEO, MediaUtils.MIME_TYPE_VIDEO);
        UploadMediaPayload payload = new UploadMediaPayload(sSite, media);
        mExpectedEvent = TestEvents.UPLOADED_MEDIA;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaUploaded(MediaStore.OnMediaUploaded event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        if (event.progress >= 1.f) {
            assertEquals(TestEvents.UPLOADED_MEDIA, mExpectedEvent);
            mCountDownLatch.countDown();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaChanged(MediaStore.OnMediaChanged event) {
        if (event.isError()) {
            if (mExpectedEvent == TestEvents.PUSH_ERROR) {
                assertEquals(event.cause, MediaAction.PUSH_MEDIA);
            } else {
                throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
            }
            mCountDownLatch.countDown();
            return;
        }
        if (event.cause == MediaAction.FETCH_ALL_MEDIA) {
            assertEquals(TestEvents.FETCHED_ALL_MEDIA, mExpectedEvent);
        } else if (event.cause == MediaAction.FETCH_MEDIA) {
            if (eventHasKnownImages(event)) {
                assertEquals(TestEvents.FETCHED_KNOWN_IMAGES, mExpectedEvent);
            }
        } else if (event.cause == MediaAction.PUSH_MEDIA) {
            assertEquals(TestEvents.PUSHED_MEDIA, mExpectedEvent);
        } else if (event.cause == MediaAction.DELETE_MEDIA) {
            assertEquals(TestEvents.DELETED_MEDIA, mExpectedEvent);
        }
        mCountDownLatch.countDown();
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

    private MediaModel newMediaModel(SiteModel site, String mediaPath, String mimeType) {
        final String testTitle = "Test Title";
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
        testMedia.setSiteId(site.getSiteId());

        return testMedia;
    }
}
