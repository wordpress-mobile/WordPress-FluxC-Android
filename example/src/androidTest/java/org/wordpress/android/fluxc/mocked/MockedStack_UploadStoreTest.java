package org.wordpress.android.fluxc.mocked;

import org.greenrobot.eventbus.Subscribe;
import org.junit.Ignore;
import org.junit.Test;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaUploadModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor;
import org.wordpress.android.fluxc.persistence.UploadSqlUtils;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.store.MediaStore.UploadMediaPayload;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests using a Mocked Network app component. Test the Store itself and not the underlying network component(s).
 * <p>
 * Tests the interactions between the MediaStore/PostStore and the UploadStore, without directly injecting the
 * UploadStore in the test class.
 */
@SuppressWarnings("NewClassNamingConvention")
public class MockedStack_UploadStoreTest extends MockedStack_Base {
    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;

    @Inject ResponseMockingInterceptor mInterceptor;

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

    @Test
    @Ignore
    public void testUploadMedia() throws InterruptedException {
        MediaModel testMedia = newMediaModel(getSampleImagePath());
        startSuccessfulMediaUpload(testMedia, getTestSite());
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Confirm that the corresponding MediaUploadModel's state has been updated automatically
        // (confirming that the UploadStore was spun up once we set up the MediaStore)
        MediaUploadModel mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        assertNotNull(mediaUploadModel);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaUploaded(OnMediaUploaded event) {
        AppLog.i(T.API, "Received OnMediaUploaded");

        if (event.media == null) {
            throw new AssertionError("Unexpected null media");
        }

        if (event.isError()) {
            assertEquals(TestEvents.MEDIA_ERROR, mNextEvent);
            mCountDownLatch.countDown();
            return;
        }

        if (event.completed) {
            assertEquals(TestEvents.UPLOADED_MEDIA, mNextEvent);
            mCountDownLatch.countDown();
        }
    }

    private MediaModel newMediaModel(String mediaPath) {
        MediaModel testMedia = new MediaModel(
                0,
                null,
                mediaPath.substring(mediaPath.lastIndexOf("/")),
                mediaPath,
                mediaPath.substring(mediaPath.lastIndexOf(".") + 1),
                "image/jpeg",
                "Test Title",
                null
        );
        testMedia.setDescription("Test Description");
        testMedia.setCaption("Test Caption");
        testMedia.setAlt("Test Alt");

        return mMediaStore.instantiateMediaModel(testMedia);
    }

    private SiteModel getTestSite() {
        SiteModel site = new SiteModel();
        site.setIsWPCom(true);
        site.setSiteId(6426253);
        return site;
    }

    private void startSuccessfulMediaUpload(MediaModel media, SiteModel site) {
        mInterceptor.respondWith("media-upload-response-success.json");

        UploadMediaPayload payload = new UploadMediaPayload(site, media, true);
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.UPLOADED_MEDIA;
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
    }

    private static MediaUploadModel getMediaUploadModelForMediaModel(MediaModel mediaModel) {
        return UploadSqlUtils.getMediaUploadModelForLocalId(mediaModel.getId());
    }
}
