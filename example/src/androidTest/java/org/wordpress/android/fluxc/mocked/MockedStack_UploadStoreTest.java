package org.wordpress.android.fluxc.mocked;

import junit.framework.Assert;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaUploadModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.persistence.UploadSqlUtils;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Tests using a Mocked Network app component. Test the Store itself and not the underlying network component(s).
 *
 * Tests the interactions between the MediaStore/PostStore and the UploadStore, without directly injecting the
 * UploadStore in the test class.
 */
public class MockedStack_UploadStoreTest extends MockedStack_Base {
    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;

    private enum TestEvents {
        NONE,
        UPLOADED_MEDIA,
        MEDIA_ERROR
    }

    private TestEvents mNextEvent;
    private CountDownLatch mCountDownLatch;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Inject
        mMockedNetworkAppComponent.inject(this);
        // Register
        mDispatcher.register(this);
        mNextEvent = TestEvents.NONE;
    }

    public void testUploadMedia() throws InterruptedException {
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        startSuccessfulMediaUpload(testMedia, getTestSite());
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Confirm that the corresponding MediaUploadModel's state has been updated automatically
        // (confirming that the UploadStore was spun up once we set up the MediaStore)
        MediaUploadModel mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        Assert.assertNotNull(mediaUploadModel);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaUploaded(OnMediaUploaded event) {
        AppLog.i(T.API, "Received OnMediaUploaded");

        if (event.media == null) {
            throw new AssertionError("Unexpected null media");
        }

        if (event.isError()) {
            Assert.assertEquals(TestEvents.MEDIA_ERROR, mNextEvent);
            mCountDownLatch.countDown();
            return;
        }

        if (event.completed) {
            Assert.assertEquals(TestEvents.UPLOADED_MEDIA, mNextEvent);
            mCountDownLatch.countDown();
        }
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

        return testMedia;
    }

    private SiteModel getTestSite() {
        SiteModel site = new SiteModel();
        site.setIsWPCom(true);
        site.setSiteId(6426253);
        return site;
    }

    private void startSuccessfulMediaUpload(MediaModel media, SiteModel site) {
        MediaPayload payload = new MediaPayload(site, media);
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.UPLOADED_MEDIA;
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
    }

    private static MediaUploadModel getMediaUploadModelForMediaModel(MediaModel mediaModel) {
        return UploadSqlUtils.getMediaUploadModelForLocalId(mediaModel.getId());
    }
}
