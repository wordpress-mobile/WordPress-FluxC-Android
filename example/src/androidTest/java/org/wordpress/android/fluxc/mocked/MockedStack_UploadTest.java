package org.wordpress.android.fluxc.mocked;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaUploadModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.PostUploadModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.module.MockedNetworkModule;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;
import org.wordpress.android.fluxc.store.UploadStore;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.fluxc.utils.WellSqlUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Tests using a Mocked Network app component. Test the Store itself and not the underlying network component(s).
 */
public class MockedStack_UploadTest extends MockedStack_Base {
    private static final String POST_DEFAULT_TITLE = "UploadTest base post";
    private static final String POST_DEFAULT_DESCRIPTION = "Hi there, I'm a post from FluxC!";

    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;
    @Inject PostStore mPostStore;
    @Inject UploadStore mUploadStore;

    private PostModel mPost;
    private boolean mExpectingMediaError;
    private CountDownLatch mCountDownLatch;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Inject
        mMockedNetworkAppComponent.inject(this);
        // Register
        mDispatcher.register(this);
    }

    public void testUploadMedia() throws InterruptedException {
        MediaModel testMedia = newMediaModel(BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE);
        startSuccessfulMediaUpload(testMedia, getTestSite());
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testUploadMediaError() throws InterruptedException {
        MediaModel testMedia = newMediaModel(BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE);
        startFailingMediaUpload(testMedia, getTestSite());
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        TestUtils.waitFor(50);

        // Confirm that the corresponding MediaUploadModel's state has been updated automatically
        MediaUploadModel mediaUploadModel = mUploadStore.getMediaUploadModelForMediaModel(testMedia);
        assertNotNull(mediaUploadModel);
        assertEquals(0F, mediaUploadModel.getProgress());
        assertEquals(MediaUploadModel.FAILED, mediaUploadModel.getUploadState());
    }

    public void testRegisterPostAndUploadMediaWithError() throws InterruptedException {
        SiteModel site = getTestSite();

        // Instantiate new post
        createNewPost(site);
        setupPostAttributes();

        // Start uploading media
        MediaModel testMedia = newMediaModel(BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE);
        testMedia.setLocalPostId(mPost.getId());
        startFailingMediaUpload(testMedia, site);

        // Wait for the event to be processed by the UploadStore
        TestUtils.waitFor(50);

        MediaUploadModel mediaUploadModel = mUploadStore.getMediaUploadModelForMediaModel(testMedia);
        assertNotNull(mediaUploadModel);
        assertEquals(MediaUploadModel.UPLOADING, mediaUploadModel.getUploadState());

        // Register the post with the UploadStore and verify that it exists and has the right state
        List<MediaModel> mediaModelList = new ArrayList<>();
        mediaModelList.add(testMedia);
        mUploadStore.registerPostModel(mPost, mediaModelList);

        // PostUploadModel exists and has correct state and associated media
        assertEquals(1, mUploadStore.getPendingPosts().size());
        PostUploadModel postUploadModel = mUploadStore.getPostUploadModelForPostModel(mPost);
        assertNotNull(postUploadModel);
        assertEquals(PostUploadModel.PENDING, postUploadModel.getUploadState());
        Set<Integer> associatedMedia = postUploadModel.getAssociatedMediaIdSet();
        assertEquals(1, associatedMedia.size());
        assertTrue(associatedMedia.contains(testMedia.getId()));

        // MediaUploadModel exists and has correct state
        mediaUploadModel = mUploadStore.getMediaUploadModelForMediaModel(testMedia);
        assertEquals(MediaUploadModel.UPLOADING, mediaUploadModel.getUploadState());

        // UploadStore returns the correct sets of media for the post by type
        assertEquals(1, mUploadStore.getUploadingMediaForPost(mPost).size());
        assertEquals(0, mUploadStore.getCompletedMediaForPost(mPost).size());
        assertEquals(0, mUploadStore.getFailedMediaForPost(mPost).size());

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        TestUtils.waitFor(50);

        // Media upload completed
        // PostUploadModel still exists and has correct state and associated media
        assertEquals(0, mUploadStore.getPendingPosts().size());
        assertEquals(1, mUploadStore.getCancelledPosts().size());
        postUploadModel = mUploadStore.getPostUploadModelForPostModel(mPost);
        assertNotNull(postUploadModel);
        assertEquals(PostUploadModel.CANCELLED, postUploadModel.getUploadState());
        associatedMedia = postUploadModel.getAssociatedMediaIdSet();
        assertEquals(1, associatedMedia.size());
        assertTrue(associatedMedia.contains(testMedia.getId()));

        // MediaUploadModel still exists and has correct state
        mediaUploadModel = mUploadStore.getMediaUploadModelForMediaModel(testMedia);
        assertEquals(MediaUploadModel.FAILED, mediaUploadModel.getUploadState());

        // UploadStore returns the correct sets of media for the post by type
        assertEquals(0, mUploadStore.getUploadingMediaForPost(mPost).size());
        assertEquals(0, mUploadStore.getCompletedMediaForPost(mPost).size());
        assertEquals(1, mUploadStore.getFailedMediaForPost(mPost).size());

        // Upload post to site
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(PostActionBuilder.newPushPostAction(new RemotePostPayload(mPost, site)));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(site));

        // Since the post upload completed successfully, the PostUploadModel should have been deleted
        assertEquals(0, mUploadStore.getPendingPosts().size());
        assertEquals(0, mUploadStore.getFailedPosts().size());
        assertEquals(0, mUploadStore.getCancelledPosts().size());
        assertNull(mUploadStore.getPostUploadModelForPostModel(uploadedPost));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onPostUploaded(OnPostUploaded event) {
        if (event.post == null) {
            throw new AssertionError("Unexpected null post");
        }

        if (event.isError()) {
            throw new AssertionError("Unexpected error: " + event.error.type + " - " + event.error.message);
        }

        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaUploaded(OnMediaUploaded event) {
        if (event.media == null) {
            throw new AssertionError("Unexpected null media");
        }

        if (event.isError()) {
            assertTrue(mExpectingMediaError);
            mCountDownLatch.countDown();
            return;
        }

        if (event.completed) {
            assertFalse(mExpectingMediaError);
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

    private PostModel createNewPost(SiteModel site) throws InterruptedException {
        mPost = mPostStore.instantiatePostModel(site, false);
        return mPost;
    }

    private void setupPostAttributes() {
        mPost.setTitle(POST_DEFAULT_TITLE);
        mPost.setContent(POST_DEFAULT_DESCRIPTION);
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
        mExpectingMediaError = false;
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
    }

    private void startFailingMediaUpload(MediaModel media, SiteModel site) {
        media.setAuthorId(MockedNetworkModule.MEDIA_FAILURE_AUTHOR_CODE);

        MediaPayload payload = new MediaPayload(site, media);
        mCountDownLatch = new CountDownLatch(1);
        mExpectingMediaError = true;
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
    }
}
