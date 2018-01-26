package org.wordpress.android.fluxc.mocked;

import junit.framework.Assert;

import org.greenrobot.eventbus.Subscribe;
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
import org.wordpress.android.fluxc.module.MockedNetworkModule;
import org.wordpress.android.fluxc.persistence.UploadSqlUtils;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
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

    public void testUploadMedia() throws InterruptedException {
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        startSuccessfulMediaUpload(testMedia, getTestSite());
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Confirm that the corresponding MediaUploadModel's state has been updated automatically
        MediaUploadModel mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        Assert.assertNotNull(mediaUploadModel);
        Assert.assertEquals(1F, mUploadStore.getUploadProgressForMedia(testMedia));
        Assert.assertEquals(MediaUploadModel.COMPLETED, mediaUploadModel.getUploadState());
    }

    public void testUploadMediaError() throws InterruptedException {
        MediaModel testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        startFailingMediaUpload(testMedia, getTestSite());
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Wait for the event to be processed by the UploadStore
        TestUtils.waitFor(50);

        // Confirm that the corresponding MediaUploadModel's state has been updated automatically
        MediaUploadModel mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        Assert.assertNotNull(mediaUploadModel);
        Assert.assertEquals(0F, mUploadStore.getUploadProgressForMedia(testMedia));
        Assert.assertEquals(MediaUploadModel.FAILED, mediaUploadModel.getUploadState());
    }

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
        Assert.assertNotNull(mediaUploadModel);
        Assert.assertEquals(MediaUploadModel.UPLOADING, mediaUploadModel.getUploadState());

        // Register the post with the UploadStore and verify that it exists and has the right state
        List<MediaModel> mediaModelList = new ArrayList<>();
        mediaModelList.add(testMedia);
        mUploadStore.registerPostModel(mPost, mediaModelList);
        Assert.assertTrue(mUploadStore.isRegisteredPostModel(mPost));

        // PostUploadModel exists and has correct state and associated media
        Assert.assertEquals(1, mUploadStore.getPendingPosts().size());
        PostUploadModel postUploadModel = getPostUploadModelForPostModel(mPost);
        Assert.assertNotNull(postUploadModel);
        Assert.assertEquals(PostUploadModel.PENDING, postUploadModel.getUploadState());
        Assert.assertTrue(mUploadStore.isPendingPost(mPost));
        Set<Integer> associatedMedia = postUploadModel.getAssociatedMediaIdSet();
        Assert.assertEquals(1, associatedMedia.size());
        Assert.assertTrue(associatedMedia.contains(testMedia.getId()));

        // MediaUploadModel exists and has correct state
        mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        Assert.assertEquals(MediaUploadModel.UPLOADING, mediaUploadModel.getUploadState());

        // UploadStore returns the correct sets of media for the post by type
        Assert.assertEquals(1, mUploadStore.getUploadingMediaForPost(mPost).size());
        Assert.assertEquals(0, mUploadStore.getCompletedMediaForPost(mPost).size());
        Assert.assertEquals(0, mUploadStore.getFailedMediaForPost(mPost).size());

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Media upload completed
        // PostUploadModel still exists and has correct state and associated media
        Assert.assertEquals(0, mUploadStore.getPendingPosts().size());
        Assert.assertEquals(1, mUploadStore.getCancelledPosts().size());
        postUploadModel = getPostUploadModelForPostModel(mPost);
        Assert.assertNotNull(postUploadModel);
        Assert.assertEquals(PostUploadModel.CANCELLED, postUploadModel.getUploadState());
        Assert.assertTrue(mUploadStore.isCancelledPost(mPost));
        associatedMedia = postUploadModel.getAssociatedMediaIdSet();
        Assert.assertEquals(1, associatedMedia.size());
        Assert.assertTrue(associatedMedia.contains(testMedia.getId()));

        // MediaUploadModel still exists and has correct state
        mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        Assert.assertEquals(MediaUploadModel.FAILED, mediaUploadModel.getUploadState());

        // UploadStore returns the correct sets of media for the post by type
        Assert.assertEquals(0, mUploadStore.getUploadingMediaForPost(mPost).size());
        Assert.assertEquals(0, mUploadStore.getCompletedMediaForPost(mPost).size());
        Assert.assertEquals(1, mUploadStore.getFailedMediaForPost(mPost).size());

        // Clean up failed media manually
        clearMedia(mPost, mUploadStore.getFailedMediaForPost(mPost));

        // UploadStore returns the correct sets of media for the post by type
        Assert.assertEquals(0, mUploadStore.getUploadingMediaForPost(mPost).size());
        Assert.assertEquals(0, mUploadStore.getCompletedMediaForPost(mPost).size());
        Assert.assertEquals(0, mUploadStore.getFailedMediaForPost(mPost).size());

        // Upload post to site (pretend we've removed the failed media from the post content)
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.UPLOADED_POST;
        mDispatcher.dispatch(PostActionBuilder.newPushPostAction(new RemotePostPayload(mPost, site)));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        Assert.assertEquals(1, WellSqlUtils.getTotalPostsCount());
        Assert.assertEquals(1, mPostStore.getPostsCountForSite(site));

        // Since the post upload completed successfully, the PostUploadModel should have been deleted
        Assert.assertEquals(0, mUploadStore.getPendingPosts().size());
        Assert.assertEquals(0, mUploadStore.getFailedPosts().size());
        Assert.assertEquals(0, mUploadStore.getCancelledPosts().size());
        Assert.assertEquals(0, mUploadStore.getAllRegisteredPosts().size());
        Assert.assertNull(getPostUploadModelForPostModel(uploadedPost));
        Assert.assertFalse(mUploadStore.isPendingPost(mPost));
    }

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
        Assert.assertNotNull(mediaUploadModel);
        Assert.assertEquals(MediaUploadModel.UPLOADING, mediaUploadModel.getUploadState());

        // Register the post with the UploadStore and verify that it exists and has the right state
        List<MediaModel> mediaModelList = new ArrayList<>();
        mediaModelList.add(testMedia);
        mUploadStore.registerPostModel(mPost, mediaModelList);
        Assert.assertTrue(mUploadStore.isRegisteredPostModel(mPost));

        mNextEvent = TestEvents.CANCELLED_POST;
        mDispatcher.dispatch(UploadActionBuilder.newCancelPostAction(mPost));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // The media upload action we already started will be next, reset the CountDownLatch and mNextEvent
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.UPLOADED_MEDIA;

        // The Post should be cancelled (the media upload should continue unaffected)
        Assert.assertEquals(0, mUploadStore.getPendingPosts().size());
        Assert.assertEquals(0, mUploadStore.getFailedPosts().size());
        Assert.assertEquals(1, mUploadStore.getCancelledPosts().size());
        Assert.assertEquals(1, mUploadStore.getAllRegisteredPosts().size());

        // The media upload should be unaffected
        mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        Assert.assertNotNull(mediaUploadModel);
        Assert.assertEquals(MediaUploadModel.UPLOADING, mediaUploadModel.getUploadState());

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Media upload completed
        // PostUploadModel still exists and has correct state and associated media
        Assert.assertEquals(0, mUploadStore.getPendingPosts().size());
        Assert.assertEquals(1, mUploadStore.getCancelledPosts().size());
        Assert.assertEquals(1, mUploadStore.getAllRegisteredPosts().size());
        PostUploadModel postUploadModel = getPostUploadModelForPostModel(mPost);
        Assert.assertNotNull(postUploadModel);
        Assert.assertEquals(PostUploadModel.CANCELLED, postUploadModel.getUploadState());
        Assert.assertTrue(mUploadStore.isCancelledPost(mPost));
        Set<Integer> associatedMedia = postUploadModel.getAssociatedMediaIdSet();
        Assert.assertEquals(1, associatedMedia.size());
        Assert.assertTrue(associatedMedia.contains(testMedia.getId()));

        // MediaUploadModel still exists and has correct state
        mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        Assert.assertEquals(MediaUploadModel.COMPLETED, mediaUploadModel.getUploadState());

        // UploadStore returns the correct sets of media for the post by type
        Assert.assertEquals(0, mUploadStore.getUploadingMediaForPost(mPost).size());
        Assert.assertEquals(1, mUploadStore.getCompletedMediaForPost(mPost).size());
        Assert.assertEquals(0, mUploadStore.getFailedMediaForPost(mPost).size());
    }

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
        Assert.assertTrue(mUploadStore.isRegisteredPostModel(mPost));

        // MediaUploadModel exists and has correct state
        MediaUploadModel mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        Assert.assertEquals(MediaUploadModel.UPLOADING, mediaUploadModel.getUploadState());

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Media upload completed
        // PostUploadModel still exists and has correct state and associated media
        Assert.assertTrue(mUploadStore.isCancelledPost(mPost));

        // MediaUploadModel still exists and has correct state
        mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        Assert.assertEquals(MediaUploadModel.FAILED, mediaUploadModel.getUploadState());

        // Clean up failed media manually
        clearMedia(mPost, mUploadStore.getFailedMediaForPost(mPost));

        // Upload a new media item to the cancelled post
        testMedia = newMediaModel(getSampleImagePath(), MediaUtils.MIME_TYPE_IMAGE);
        testMedia.setLocalPostId(mPost.getId());
        startFailingMediaUpload(testMedia, site);

        // Wait for the event to be processed by the UploadStore
        TestUtils.waitFor(50);

        // The cancelled post should be cleared since we've now modified it
        // There's no pending post either, because we haven't re-registered one yet
        Assert.assertEquals(0, mUploadStore.getCancelledPosts().size());
        Assert.assertEquals(0, mUploadStore.getFailedPosts().size());
        Assert.assertEquals(0, mUploadStore.getPendingPosts().size());
        Assert.assertEquals(0, mUploadStore.getAllRegisteredPosts().size());

        Assert.assertNull(getPostUploadModelForPostModel(mPost));
    }

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
        Assert.assertTrue(mUploadStore.isRegisteredPostModel(mPost));

        // MediaUploadModel exists and has correct state
        MediaUploadModel mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        Assert.assertEquals(MediaUploadModel.UPLOADING, mediaUploadModel.getUploadState());

        // Manually set the MediaModel to FAILED
        // This might happen, e.g., if the app using FluxC was terminated, and any 'UPLOADING' MediaModels are set
        // to FAILED on reload to ensure consistency
        testMedia.setUploadState(MediaModel.MediaUploadState.FAILED);
        mNextEvent = TestEvents.MEDIA_CHANGED;
        mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(testMedia));

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Wait for the event to be processed by the UploadStore
        TestUtils.waitFor(50);

        // The MediaUploadModel should have been set to FAILED automatically via the UPDATE_MEDIA action
        mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        Assert.assertEquals(MediaUploadModel.FAILED, mediaUploadModel.getUploadState());
        Assert.assertNotNull(mediaUploadModel.getMediaError());
        Assert.assertEquals(0F, mUploadStore.getUploadProgressForMedia(testMedia));

        // The PostUploadModel should have been set to CANCELLED automatically via the UPDATE_MEDIA action
        PostUploadModel postUploadModel = getPostUploadModelForPostModel(mPost);
        Assert.assertEquals(PostUploadModel.CANCELLED, postUploadModel.getUploadState());

        // Reset expected event back to the UPLOADED_MEDIA we were expecting at the start,
        // and finish off the events for this test
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.UPLOADED_MEDIA;
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
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

        Assert.assertEquals(TestEvents.UPLOADED_POST, mNextEvent);
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
            Assert.assertEquals(TestEvents.MEDIA_ERROR, mNextEvent);
            mCountDownLatch.countDown();
            return;
        }

        if (event.completed) {
            Assert.assertEquals(TestEvents.UPLOADED_MEDIA, mNextEvent);
            mCountDownLatch.countDown();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaChanged(OnMediaChanged event) {
        AppLog.i(T.API, "Received OnMediaChanged");

        Assert.assertEquals(TestEvents.MEDIA_CHANGED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onUploadChanged(OnUploadChanged event) {
        AppLog.i(T.API, "Received OnUploadChanged");

        if (mNextEvent.equals(TestEvents.CANCELLED_POST)) {
            Assert.assertEquals(UploadAction.CANCEL_POST, event.cause);
            mCountDownLatch.countDown();
        } else if (mNextEvent.equals(TestEvents.CLEARED_MEDIA)) {
            Assert.assertEquals(UploadAction.CLEAR_MEDIA_FOR_POST, event.cause);
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
        MediaPayload payload = new MediaPayload(site, media);
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.UPLOADED_MEDIA;
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
    }

    private void startFailingMediaUpload(MediaModel media, SiteModel site) {
        media.setAuthorId(MockedNetworkModule.MEDIA_FAILURE_AUTHOR_CODE);

        MediaPayload payload = new MediaPayload(site, media);
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.MEDIA_ERROR;
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
    }

    private void clearMedia(PostModel post, Set<MediaModel> media) throws InterruptedException {
        ClearMediaPayload clearMediaPayload = new ClearMediaPayload(post, media);
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.CLEARED_MEDIA;
        mDispatcher.dispatch(UploadActionBuilder.newClearMediaForPostAction(clearMediaPayload));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private static MediaUploadModel getMediaUploadModelForMediaModel(MediaModel mediaModel) {
        return UploadSqlUtils.getMediaUploadModelForLocalId(mediaModel.getId());
    }

    private static PostUploadModel getPostUploadModelForPostModel(PostModel postModel) {
        return UploadSqlUtils.getPostUploadModelForLocalId(postModel.getId());
    }
}
