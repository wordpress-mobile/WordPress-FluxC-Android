package org.wordpress.android.fluxc.mocked;

import com.google.gson.JsonObject;

import org.greenrobot.eventbus.Subscribe;
import org.junit.Test;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.action.UploadAction;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.generated.UploadActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaUploadModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.PostUploadModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor;
import org.wordpress.android.fluxc.persistence.UploadSqlUtils;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.store.MediaStore.UploadMediaPayload;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;
import org.wordpress.android.fluxc.store.UploadStore;
import org.wordpress.android.fluxc.store.UploadStore.ClearMediaPayload;
import org.wordpress.android.fluxc.store.UploadStore.OnUploadChanged;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.fluxc.utils.WellSqlUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

    @Inject ResponseMockingInterceptor mInterceptor;

    private enum TestEvents {
        NONE,
        UPLOADED_POST,
        UPLOADED_MEDIA,
        MEDIA_CHANGED,
        MEDIA_ERROR,
        CANCELLED_POST,
        CLEARED_MEDIA
    }

    private PostModel mPost;
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
    public void testUploadMedia() throws InterruptedException {
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        startSuccessfulMediaUpload(testMedia, getTestSite());
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Confirm that the corresponding MediaUploadModel's state has been updated automatically
        MediaUploadModel mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        assertNotNull(mediaUploadModel);
        assertEquals(1F, mUploadStore.getUploadProgressForMedia(testMedia), 0.1);
        assertEquals(MediaUploadModel.COMPLETED, mediaUploadModel.getUploadState());
    }

    @Test
    public void testUploadMediaError() throws InterruptedException {
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        startFailingMediaUpload(testMedia, getTestSite());
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Wait for the event to be processed by the UploadStore
        TestUtils.waitFor(50);

        // Confirm that the corresponding MediaUploadModel's state has been updated automatically
        MediaUploadModel mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        assertNotNull(mediaUploadModel);
        assertEquals(0F, mUploadStore.getUploadProgressForMedia(testMedia), 0.1);
        assertEquals(MediaUploadModel.FAILED, mediaUploadModel.getUploadState());
    }

    @Test
    public void testRegisterPostAndUploadMediaWithError() throws InterruptedException {
        SiteModel site = getTestSite();

        // Instantiate new post
        createNewPost(site);
        setupPostAttributes();

        // Start uploading media
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        testMedia.setLocalPostId(mPost.getId());
        startFailingMediaUpload(testMedia, site);

        // Wait for the event to be processed by the UploadStore
        TestUtils.waitFor(50);

        MediaUploadModel mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        assertNotNull(mediaUploadModel);
        assertEquals(MediaUploadModel.UPLOADING, mediaUploadModel.getUploadState());

        // Register the post with the UploadStore and verify that it exists and has the right state
        List<MediaModel> mediaModelList = new ArrayList<>();
        mediaModelList.add(testMedia);
        mUploadStore.registerPostModel(mPost, mediaModelList);
        assertTrue(mUploadStore.isRegisteredPostModel(mPost));

        // PostUploadModel exists and has correct state and associated media
        assertEquals(1, mUploadStore.getPendingPosts().size());
        PostUploadModel postUploadModel = getPostUploadModelForPostModel(mPost);
        assertNotNull(postUploadModel);
        assertEquals(PostUploadModel.PENDING, postUploadModel.getUploadState());
        assertTrue(mUploadStore.isPendingPost(mPost));
        Set<Integer> associatedMedia = postUploadModel.getAssociatedMediaIdSet();
        assertEquals(1, associatedMedia.size());
        assertTrue(associatedMedia.contains(testMedia.getId()));

        // MediaUploadModel exists and has correct state
        mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        assertEquals(MediaUploadModel.UPLOADING, mediaUploadModel.getUploadState());

        // UploadStore returns the correct sets of media for the post by type
        assertEquals(1, mUploadStore.getUploadingMediaForPost(mPost).size());
        assertEquals(0, mUploadStore.getCompletedMediaForPost(mPost).size());
        assertEquals(0, mUploadStore.getFailedMediaForPost(mPost).size());

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Media upload completed
        // PostUploadModel still exists and has correct state and associated media
        assertEquals(0, mUploadStore.getPendingPosts().size());
        assertEquals(1, mUploadStore.getCancelledPosts().size());
        postUploadModel = getPostUploadModelForPostModel(mPost);
        assertNotNull(postUploadModel);
        assertEquals(PostUploadModel.CANCELLED, postUploadModel.getUploadState());
        assertTrue(mUploadStore.isCancelledPost(mPost));
        associatedMedia = postUploadModel.getAssociatedMediaIdSet();
        assertEquals(1, associatedMedia.size());
        assertTrue(associatedMedia.contains(testMedia.getId()));

        // MediaUploadModel still exists and has correct state
        mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        assertEquals(MediaUploadModel.FAILED, mediaUploadModel.getUploadState());

        // UploadStore returns the correct sets of media for the post by type
        assertEquals(0, mUploadStore.getUploadingMediaForPost(mPost).size());
        assertEquals(0, mUploadStore.getCompletedMediaForPost(mPost).size());
        assertEquals(1, mUploadStore.getFailedMediaForPost(mPost).size());

        // Clean up failed media manually
        clearMedia(mPost, mUploadStore.getFailedMediaForPost(mPost));

        // UploadStore returns the correct sets of media for the post by type
        assertEquals(0, mUploadStore.getUploadingMediaForPost(mPost).size());
        assertEquals(0, mUploadStore.getCompletedMediaForPost(mPost).size());
        assertEquals(0, mUploadStore.getFailedMediaForPost(mPost).size());

        // Upload post to site (pretend we've removed the failed media from the post content)
        mInterceptor.respondWith("post-upload-response-success.json");
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.UPLOADED_POST;
        mDispatcher.dispatch(PostActionBuilder.newPushPostAction(new RemotePostPayload(mPost, site)));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(site));

        // Since the post upload completed successfully, the PostUploadModel should have been deleted
        assertEquals(0, mUploadStore.getPendingPosts().size());
        assertEquals(0, mUploadStore.getFailedPosts().size());
        assertEquals(0, mUploadStore.getCancelledPosts().size());
        assertEquals(0, mUploadStore.getAllRegisteredPosts().size());
        assertNull(getPostUploadModelForPostModel(uploadedPost));
        assertFalse(mUploadStore.isPendingPost(mPost));
    }

    @Test
    public void testRegisterPostAndUploadMediaWithPostCancellation() throws InterruptedException {
        SiteModel site = getTestSite();

        // Instantiate new post
        createNewPost(site);
        setupPostAttributes();

        // Start uploading media
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        testMedia.setLocalPostId(mPost.getId());
        startSuccessfulMediaUpload(testMedia, site);

        // Wait for the event to be processed by the UploadStore
        TestUtils.waitFor(50);

        MediaUploadModel mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        assertNotNull(mediaUploadModel);
        assertEquals(MediaUploadModel.UPLOADING, mediaUploadModel.getUploadState());

        // Register the post with the UploadStore and verify that it exists and has the right state
        List<MediaModel> mediaModelList = new ArrayList<>();
        mediaModelList.add(testMedia);
        mUploadStore.registerPostModel(mPost, mediaModelList);
        assertTrue(mUploadStore.isRegisteredPostModel(mPost));

        mNextEvent = TestEvents.CANCELLED_POST;
        mDispatcher.dispatch(UploadActionBuilder.newCancelPostAction(mPost));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // The media upload action we already started will be next, reset the CountDownLatch and mNextEvent
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.UPLOADED_MEDIA;

        // The Post should be cancelled (the media upload should continue unaffected)
        assertEquals(0, mUploadStore.getPendingPosts().size());
        assertEquals(0, mUploadStore.getFailedPosts().size());
        assertEquals(1, mUploadStore.getCancelledPosts().size());
        assertEquals(1, mUploadStore.getAllRegisteredPosts().size());

        // The media upload should be unaffected
        mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        assertNotNull(mediaUploadModel);
        assertEquals(MediaUploadModel.UPLOADING, mediaUploadModel.getUploadState());

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Media upload completed
        // PostUploadModel still exists and has correct state and associated media
        assertEquals(0, mUploadStore.getPendingPosts().size());
        assertEquals(1, mUploadStore.getCancelledPosts().size());
        assertEquals(1, mUploadStore.getAllRegisteredPosts().size());
        PostUploadModel postUploadModel = getPostUploadModelForPostModel(mPost);
        assertNotNull(postUploadModel);
        assertEquals(PostUploadModel.CANCELLED, postUploadModel.getUploadState());
        assertTrue(mUploadStore.isCancelledPost(mPost));
        Set<Integer> associatedMedia = postUploadModel.getAssociatedMediaIdSet();
        assertEquals(1, associatedMedia.size());
        assertTrue(associatedMedia.contains(testMedia.getId()));

        // MediaUploadModel still exists and has correct state
        mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        assertEquals(MediaUploadModel.COMPLETED, mediaUploadModel.getUploadState());

        // UploadStore returns the correct sets of media for the post by type
        assertEquals(0, mUploadStore.getUploadingMediaForPost(mPost).size());
        assertEquals(1, mUploadStore.getCompletedMediaForPost(mPost).size());
        assertEquals(0, mUploadStore.getFailedMediaForPost(mPost).size());
    }

    @Test
    public void testUploadMediaInCancelledPost() throws InterruptedException {
        SiteModel site = getTestSite();

        // Instantiate new post
        createNewPost(site);
        setupPostAttributes();

        // Start uploading media
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        testMedia.setLocalPostId(mPost.getId());
        startFailingMediaUpload(testMedia, site);

        // Wait for the event to be processed by the UploadStore
        TestUtils.waitFor(50);

        // Register the post with the UploadStore and verify that it exists and has the right state
        List<MediaModel> mediaModelList = new ArrayList<>();
        mediaModelList.add(testMedia);
        mUploadStore.registerPostModel(mPost, mediaModelList);
        assertTrue(mUploadStore.isRegisteredPostModel(mPost));

        // MediaUploadModel exists and has correct state
        MediaUploadModel mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        assertEquals(MediaUploadModel.UPLOADING, mediaUploadModel.getUploadState());

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Media upload completed
        // PostUploadModel still exists and has correct state and associated media
        assertTrue(mUploadStore.isCancelledPost(mPost));

        // MediaUploadModel still exists and has correct state
        mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        assertEquals(MediaUploadModel.FAILED, mediaUploadModel.getUploadState());

        // Clean up failed media manually
        clearMedia(mPost, mUploadStore.getFailedMediaForPost(mPost));

        // Upload a new media item to the cancelled post
        testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        testMedia.setLocalPostId(mPost.getId());
        startFailingMediaUpload(testMedia, site);

        // Wait for the event to be processed by the UploadStore
        TestUtils.waitFor(50);

        // The cancelled post should not be cleared after a new media has been associated
        assertEquals(0, mUploadStore.getFailedPosts().size());
        assertEquals(0, mUploadStore.getPendingPosts().size());
        assertEquals(1, mUploadStore.getCancelledPosts().size());
        assertEquals(1, mUploadStore.getAllRegisteredPosts().size());

        PostUploadModel postUploadModel = getPostUploadModelForPostModel(mPost);
        assertEquals(postUploadModel.getUploadState(), PostUploadModel.CANCELLED);
    }


    @Test
    public void testPostErrorAndCancellationCounter() throws InterruptedException {
        SiteModel site = getTestSite();

        // Instantiate new post
        createNewPost(site);
        setupPostAttributes();

        // Start uploading a media
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        testMedia.setLocalPostId(mPost.getId());

        // Register the post with the UploadStore and verify that it exists and has the right state
        List<MediaModel> mediaModelList = new ArrayList<>();
        mediaModelList.add(testMedia);
        mUploadStore.registerPostModel(mPost, mediaModelList);
        assertTrue(mUploadStore.isRegisteredPostModel(mPost));

        // Check there is no error before starting the media upload
        assertEquals(0, mUploadStore.getNumberOfPostUploadErrorsOrCancellations(mPost));

        startFailingMediaUpload(testMedia, site);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check the post has been cancelled and the counter is now 1
        assertTrue(mUploadStore.isCancelledPost(mPost));
        assertEquals(1, mUploadStore.getNumberOfPostUploadErrorsOrCancellations(mPost));

        startFailingMediaUpload(testMedia, site);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check the counter is still 1 since we didn't re-register the post
        assertTrue(mUploadStore.isCancelledPost(mPost));
        assertEquals(1, mUploadStore.getNumberOfPostUploadErrorsOrCancellations(mPost));

        // Re-register the post (it should reset the state) and retry to upload a media (with failure)
        mUploadStore.registerPostModel(mPost, mediaModelList);
        startFailingMediaUpload(testMedia, site);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // The post should be cancelled and counter incremented
        assertTrue(mUploadStore.isCancelledPost(mPost));
        assertEquals(2, mUploadStore.getNumberOfPostUploadErrorsOrCancellations(mPost));
    }

    @Test
    public void testUpdateMediaModelState() throws InterruptedException {
        SiteModel site = getTestSite();

        // Instantiate new post
        createNewPost(site);
        setupPostAttributes();

        // Start uploading media
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        testMedia.setLocalPostId(mPost.getId());
        startSuccessfulMediaUpload(testMedia, site);

        // Wait for the event to be processed by the UploadStore
        TestUtils.waitFor(50);

        // Register the post with the UploadStore and verify that it exists and has the right state
        List<MediaModel> mediaModelList = new ArrayList<>();
        mediaModelList.add(testMedia);
        mUploadStore.registerPostModel(mPost, mediaModelList);
        assertTrue(mUploadStore.isRegisteredPostModel(mPost));

        // MediaUploadModel exists and has correct state
        MediaUploadModel mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        assertEquals(MediaUploadModel.UPLOADING, mediaUploadModel.getUploadState());

        // Manually set the MediaModel to FAILED
        // This might happen, e.g., if the app using FluxC was terminated, and any 'UPLOADING' MediaModels are set
        // to FAILED on reload to ensure consistency
        testMedia.setUploadState(MediaModel.MediaUploadState.FAILED);
        mNextEvent = TestEvents.MEDIA_CHANGED;
        mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(testMedia));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Wait for the event to be processed by the UploadStore
        TestUtils.waitFor(50);

        // The MediaUploadModel should have been set to FAILED automatically via the UPDATE_MEDIA action
        mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        assertEquals(MediaUploadModel.FAILED, mediaUploadModel.getUploadState());
        assertNotNull(mediaUploadModel.getMediaError());
        assertEquals(0F, mUploadStore.getUploadProgressForMedia(testMedia), 0.1);

        // The PostUploadModel should have been set to CANCELLED automatically via the UPDATE_MEDIA action
        PostUploadModel postUploadModel = getPostUploadModelForPostModel(mPost);
        assertEquals(PostUploadModel.CANCELLED, postUploadModel.getUploadState());

        // Reset expected event back to the UPLOADED_MEDIA we were expecting at the start,
        // and finish off the events for this test
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.UPLOADED_MEDIA;
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onPostUploaded(OnPostUploaded event) {
        AppLog.i(T.API, "Received OnPostUploaded");

        if (event.post == null) {
            throw new AssertionError("Unexpected null post");
        }

        if (event.isError()) {
            throw new AssertionError("Unexpected error: " + event.error.type + " - " + event.error.message);
        }

        assertEquals(TestEvents.UPLOADED_POST, mNextEvent);
        mCountDownLatch.countDown();
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

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaChanged(OnMediaChanged event) {
        AppLog.i(T.API, "Received OnMediaChanged");

        assertEquals(TestEvents.MEDIA_CHANGED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onUploadChanged(OnUploadChanged event) {
        AppLog.i(T.API, "Received OnUploadChanged");

        if (mNextEvent.equals(TestEvents.CANCELLED_POST)) {
            assertEquals(UploadAction.CANCEL_POST, event.cause);
            mCountDownLatch.countDown();
        } else if (mNextEvent.equals(TestEvents.CLEARED_MEDIA)) {
            assertEquals(UploadAction.CLEAR_MEDIA_FOR_POST, event.cause);
            mCountDownLatch.countDown();
        } else {
            throw new AssertionError("Unexpected OnUploadChanged event with cause: " + event.cause);
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
        mInterceptor.respondWith("media-upload-response-success.json");

        UploadMediaPayload payload = new UploadMediaPayload(site, media, true);
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.UPLOADED_MEDIA;
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
    }

    private void startFailingMediaUpload(MediaModel media, SiteModel site) {
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.addProperty("error", "invalid_token");
        jsonResponse.addProperty("message", "The OAuth2 token is invalid.");
        mInterceptor.respondWithError(jsonResponse);

        UploadMediaPayload payload = new UploadMediaPayload(site, media, true);
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.MEDIA_ERROR;
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
    }

    private void clearMedia(PostModel post, Set<MediaModel> media) throws InterruptedException {
        ClearMediaPayload clearMediaPayload = new ClearMediaPayload(post, media);
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.CLEARED_MEDIA;
        mDispatcher.dispatch(UploadActionBuilder.newClearMediaForPostAction(clearMediaPayload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private static MediaUploadModel getMediaUploadModelForMediaModel(MediaModel mediaModel) {
        return UploadSqlUtils.getMediaUploadModelForLocalId(mediaModel.getId());
    }

    private static PostUploadModel getPostUploadModelForPostModel(PostModel postModel) {
        return UploadSqlUtils.getPostUploadModelForLocalId(postModel.getId());
    }
}
