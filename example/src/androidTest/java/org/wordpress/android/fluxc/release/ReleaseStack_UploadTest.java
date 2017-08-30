package org.wordpress.android.fluxc.release;

import android.annotation.SuppressLint;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.action.UploadAction;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.generated.UploadActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState;
import org.wordpress.android.fluxc.model.MediaUploadModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.PostUploadModel;
import org.wordpress.android.fluxc.persistence.UploadSqlUtils;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded;
import org.wordpress.android.fluxc.store.PostStore.PostErrorType;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;
import org.wordpress.android.fluxc.store.UploadStore;
import org.wordpress.android.fluxc.store.UploadStore.ClearMediaPayload;
import org.wordpress.android.fluxc.store.UploadStore.OnUploadChanged;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.fluxc.utils.WellSqlUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

@SuppressLint("UseSparseArrays")
public class ReleaseStack_UploadTest extends ReleaseStack_WPComBase {
    private static final String POST_DEFAULT_TITLE = "UploadTest base post";
    private static final String POST_DEFAULT_DESCRIPTION = "Hi there, I'm a post from FluxC!";

    @Inject MediaStore mMediaStore;
    @Inject PostStore mPostStore;
    @Inject UploadStore mUploadStore;

    private enum TestEvents {
        CANCELED_MEDIA,
        DELETED_MEDIA,
        PUSHED_MEDIA,
        REMOVED_MEDIA,
        UPLOADED_MEDIA,
        MEDIA_ERROR_MALFORMED,
        UPLOADED_POST,
        POST_ERROR_UNKNOWN,
        CLEARED_MEDIA
    }

    private PostModel mPost;
    private TestEvents mNextEvent;
    private long mLastUploadedId = -1L;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);

        // Authenticate, fetch sites and initialize sSite
        init();
    }

    public void testUploadImage() throws InterruptedException {
        // Start uploading media
        MediaModel testMedia = newMediaModel(BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE);
        mNextEvent = TestEvents.UPLOADED_MEDIA;
        MediaPayload payload = new MediaPayload(sSite, testMedia);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));

        // Wait for the event to be processed by the UploadStore
        TestUtils.waitFor(50);

        // Check that a MediaUploadModel has been registered
        MediaUploadModel mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        assertNotNull(mediaUploadModel);
        assertEquals(MediaUploadModel.UPLOADING, mediaUploadModel.getUploadState());

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Verify and set media ID
        assertTrue(mLastUploadedId >= 0);
        testMedia.setMediaId(mLastUploadedId);
        assertNotNull(mMediaStore.getSiteMediaWithId(sSite, testMedia.getMediaId()));

        // Confirm that the corresponding MediaUploadModel's state has been updated automatically
        mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        assertEquals(1F, mUploadStore.getUploadProgressForMedia(testMedia));
        assertEquals(MediaUploadModel.COMPLETED, mediaUploadModel.getUploadState());

        // Delete test image
        mNextEvent = TestEvents.DELETED_MEDIA;
        deleteMedia(testMedia);

        // Since the MediaUploadModel keys are linked to MediaModels, the corresponding MediaUploadModel should
        // have been deleted as well
        assertNull(getMediaUploadModelForMediaModel(testMedia));
    }

    public void testUploadInvalidMedia() throws InterruptedException {
        // Start uploading media
        MediaModel testMedia = newMediaModel(BuildConfig.TEST_LOCAL_IMAGE, "not-even-close/to-an-actual-mime-type");
        mNextEvent = TestEvents.MEDIA_ERROR_MALFORMED;
        MediaPayload payload = new MediaPayload(sSite, testMedia);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // MediaModel and MediaUploadModel should both exist and be marked as FAILED
        MediaModel mediaModel = mMediaStore.getMediaWithLocalId(testMedia.getId());
        assertNotNull(mediaModel);
        assertEquals(MediaUploadState.FAILED, MediaUploadState.fromString(mediaModel.getUploadState()));

        MediaUploadModel mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        assertNotNull(mediaUploadModel);
        assertEquals(MediaUploadModel.FAILED, mediaUploadModel.getUploadState());

        // Remove local test image
        mNextEvent = TestEvents.REMOVED_MEDIA;
        removeMedia(testMedia);

        // Since the MediaUploadModel keys are linked to MediaModels, the corresponding MediaUploadModel should
        // have been deleted as well
        assertNull(getMediaUploadModelForMediaModel(testMedia));
    }

    public void testCancelImageUpload() throws InterruptedException {
        // First, try canceling an image with the default behavior (canceled image is deleted from the store)
        MediaModel testMedia = newMediaModel(BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE);
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.CANCELED_MEDIA;
        MediaPayload payload = new MediaPayload(sSite, testMedia);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));

        // Wait a bit and issue the cancel command
        TestUtils.waitFor(1000);

        CancelMediaPayload cancelPayload = new CancelMediaPayload(sSite, testMedia);
        mDispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(cancelPayload));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals(0, mMediaStore.getSiteMediaCount(sSite));

        // The delete flag was on, so there should be no associated MediaUploadModel
        MediaUploadModel mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        assertNull(mediaUploadModel);

        // Now, try canceling with delete=false (canceled image should be marked as failed and kept in the store)
        testMedia = newMediaModel(BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE);
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.CANCELED_MEDIA;
        payload = new MediaPayload(sSite, testMedia);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));

        // Wait a bit and issue the cancel command
        TestUtils.waitFor(1000);

        cancelPayload = new CancelMediaPayload(sSite, testMedia, false);
        mDispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(cancelPayload));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals(1, mMediaStore.getSiteMediaCount(sSite));
        MediaModel canceledMedia = mMediaStore.getMediaWithLocalId(testMedia.getId());
        assertEquals(MediaUploadState.FAILED.toString(), canceledMedia.getUploadState());

        // The delete flag was off, so we can expect an associated MediaUploadModel
        mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        assertNotNull(mediaUploadModel);
        assertEquals(MediaUploadModel.FAILED, mediaUploadModel.getUploadState());
    }

    public void testRegisterAndUploadPost() throws InterruptedException {
        // Instantiate new post
        createNewPost();
        setupPostAttributes();

        // Register the post with the UploadStore and verify that it exists
        mUploadStore.registerPostModel(mPost, Collections.<MediaModel>emptyList());

        assertEquals(1, mUploadStore.getPendingPosts().size());
        PostUploadModel postUploadModel = getPostUploadModelForPostModel(mPost);
        assertNotNull(postUploadModel);
        assertEquals(PostUploadModel.PENDING, postUploadModel.getUploadState());
        assertTrue(mUploadStore.isPendingPost(mPost));

        // Upload new post to site
        uploadPost(mPost);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        // Since the post upload completed successfully, the PostUploadModel should have been deleted
        assertEquals(0, mUploadStore.getPendingPosts().size());
        assertNull(getPostUploadModelForPostModel(uploadedPost));
    }

    public void testRegisterAndUploadInvalidPost() throws InterruptedException {
        createNewPost();
        setupPostAttributes();
        mPost.setIsLocalDraft(false);
        mPost.setIsLocallyChanged(false);
        mPost.setRemotePostId(7428748); // Set to a non-existent remote ID

        // Register the post with the UploadStore and verify that it exists
        mUploadStore.registerPostModel(mPost, Collections.<MediaModel>emptyList());

        PostUploadModel postUploadModel = getPostUploadModelForPostModel(mPost);
        assertNotNull(postUploadModel);
        assertEquals(PostUploadModel.PENDING, postUploadModel.getUploadState());
        assertTrue(mUploadStore.isPendingPost(mPost));

        mNextEvent = TestEvents.POST_ERROR_UNKNOWN;
        mCountDownLatch = new CountDownLatch(1);

        RemotePostPayload pushPayload = new RemotePostPayload(mPost, sSite);
        mDispatcher.dispatch(PostActionBuilder.newPushPostAction(pushPayload));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        // Since the post upload had an error, the PostUploadModel should still exist and be marked as FAILED
        assertEquals(0, mUploadStore.getPendingPosts().size());
        assertEquals(1, mUploadStore.getFailedPosts().size());
        postUploadModel = getPostUploadModelForPostModel(uploadedPost);
        assertEquals(PostUploadModel.FAILED, postUploadModel.getUploadState());
        assertTrue(mUploadStore.isFailedPost(mPost));
        assertEquals(PostErrorType.UNKNOWN_POST, PostErrorType.fromString(postUploadModel.getErrorType()));
    }

    public void testRegisterPostAndUploadMedia() throws InterruptedException {
        // Start uploading media
        MediaModel testMedia = newMediaModel(BuildConfig.TEST_LOCAL_IMAGE, MediaUtils.MIME_TYPE_IMAGE);
        mNextEvent = TestEvents.UPLOADED_MEDIA;
        MediaPayload payload = new MediaPayload(sSite, testMedia);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));

        // Wait for the event to be processed by the UploadStore
        TestUtils.waitFor(50);

        // Instantiate new post
        createNewPost();
        setupPostAttributes();

        // Register the post with the UploadStore and verify that it exists and has the right state
        List<MediaModel> mediaModelList = new ArrayList<>();
        mediaModelList.add(testMedia);
        mUploadStore.registerPostModel(mPost, mediaModelList);

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
        MediaUploadModel mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        assertEquals(MediaUploadModel.UPLOADING, mediaUploadModel.getUploadState());

        // UploadStore returns the correct sets of media for the post by type
        assertEquals(1, mUploadStore.getUploadingMediaForPost(mPost).size());
        assertEquals(0, mUploadStore.getCompletedMediaForPost(mPost).size());
        assertEquals(0, mUploadStore.getFailedMediaForPost(mPost).size());

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Media upload completed
        // PostUploadModel still exists and has correct state and associated media
        postUploadModel = getPostUploadModelForPostModel(mPost);
        assertNotNull(postUploadModel);
        assertEquals(PostUploadModel.PENDING, postUploadModel.getUploadState());
        assertTrue(mUploadStore.isPendingPost(mPost));
        associatedMedia = postUploadModel.getAssociatedMediaIdSet();
        assertEquals(1, associatedMedia.size());
        assertTrue(associatedMedia.contains(testMedia.getId()));

        // MediaUploadModel still exists and has correct state
        mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        assertEquals(MediaUploadModel.COMPLETED, mediaUploadModel.getUploadState());

        // UploadStore returns the correct sets of media for the post by type
        assertEquals(0, mUploadStore.getUploadingMediaForPost(mPost).size());
        assertEquals(1, mUploadStore.getCompletedMediaForPost(mPost).size());
        assertEquals(0, mUploadStore.getFailedMediaForPost(mPost).size());

        // Remove the completed media references from the post
        clearMedia(mPost, mUploadStore.getCompletedMediaForPost(mPost));

        // PostUploadModel still exists and has correct state and associated media
        assertEquals(1, mUploadStore.getPendingPosts().size());
        postUploadModel = getPostUploadModelForPostModel(mPost);
        assertNotNull(postUploadModel);
        assertEquals(PostUploadModel.PENDING, postUploadModel.getUploadState());
        assertTrue(mUploadStore.isPendingPost(mPost));
        assertTrue(postUploadModel.getAssociatedMediaIdSet().isEmpty());

        // MediaUploadModel should have been deleted
        mediaUploadModel = getMediaUploadModelForMediaModel(testMedia);
        assertNull(mediaUploadModel);

        // UploadStore returns the correct sets of media for the post by type
        assertEquals(0, mUploadStore.getUploadingMediaForPost(mPost).size());
        assertEquals(0, mUploadStore.getCompletedMediaForPost(mPost).size());
        assertEquals(0, mUploadStore.getFailedMediaForPost(mPost).size());

        // Upload post to site
        uploadPost(mPost);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        // Since the post upload completed successfully, the PostUploadModel should have been deleted
        assertEquals(0, mUploadStore.getPendingPosts().size());
        assertNull(getPostUploadModelForPostModel(uploadedPost));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaUploaded(OnMediaUploaded event) {
        if (event.isError()) {
            if (mNextEvent == TestEvents.MEDIA_ERROR_MALFORMED) {
                assertEquals(MediaErrorType.MALFORMED_MEDIA_ARG, event.error.type);
                mCountDownLatch.countDown();
                return;
            }
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        if (event.canceled) {
            if (mNextEvent == TestEvents.CANCELED_MEDIA) {
                mCountDownLatch.countDown();
            } else {
                throw new AssertionError("Unexpected cancellation for media: " + event.media.getId());
            }
        } else if (event.completed) {
             if (mNextEvent == TestEvents.UPLOADED_MEDIA) {
                mLastUploadedId = event.media.getMediaId();
            } else {
                throw new AssertionError("Unexpected completion for media: " + event.media.getId());
            }
            mCountDownLatch.countDown();
        } else {
            // General checks that the UploadStore is keeping an updated progress value for the media
            assertTrue(mUploadStore.getUploadProgressForMedia(event.media) > 0F);
            assertTrue(mUploadStore.getUploadProgressForMedia(event.media) < 1F);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaChanged(OnMediaChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        if (event.cause == MediaAction.PUSH_MEDIA) {
            assertEquals(TestEvents.PUSHED_MEDIA, mNextEvent);
        } else if (event.cause == MediaAction.DELETE_MEDIA) {
            assertEquals(TestEvents.DELETED_MEDIA, mNextEvent);
        } else if (event.cause == MediaAction.REMOVE_MEDIA) {
            assertEquals(TestEvents.REMOVED_MEDIA, mNextEvent);
        } else {
            throw new AssertionError("Unexpected event: " + event.cause);
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onPostUploaded(OnPostUploaded event) {
        AppLog.i(T.API, "Received OnPostUploaded");
        if (event.isError()) {
            AppLog.i(T.API, "OnPostUploaded has error: " + event.error.type + " - " + event.error.message);
            if (mNextEvent == TestEvents.POST_ERROR_UNKNOWN) {
                assertEquals(PostErrorType.UNKNOWN_POST, event.error.type);
                mCountDownLatch.countDown();
            } else {
                throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
            }
            return;
        }
        assertEquals(TestEvents.UPLOADED_POST, mNextEvent);
        assertFalse(event.post.isLocalDraft());
        assertFalse(event.post.isLocallyChanged());
        assertNotSame(0, event.post.getRemotePostId());

        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onUploadChanged(OnUploadChanged event) {
        AppLog.i(T.API, "Received OnUploadChanged");
        if (event.isError()) {
            if (event.error.postError != null) {
                AppLog.i(T.API, "OnUploadChanged has post error: " + event.error.postError.type + " - "
                        + event.error.postError.message);
                throw new AssertionError("Unexpected error occurred with type: " + event.error.postError.type);
            } else if (event.error.mediaError != null) {
                AppLog.i(T.API, "OnUploadChanged has media error: " + event.error.mediaError.type + " - "
                        + event.error.mediaError.message);
                throw new AssertionError("Unexpected error occurred with type: " + event.error.mediaError.type);
            } else {
                AppLog.i(T.API, "OnUploadChanged has null error");
                throw new AssertionError("Unexpected error occurred");
            }
        }

        if (mNextEvent.equals(TestEvents.CLEARED_MEDIA)) {
            assertEquals(UploadAction.CLEAR_MEDIA, event.cause);
            mCountDownLatch.countDown();
        } else {
            throw new AssertionError("Unexpected OnUploadChanged event with cause: " + event.cause);
        }
    }

    private PostModel createNewPost() throws InterruptedException {
        mPost = mPostStore.instantiatePostModel(sSite, false);
        return mPost;
    }

    private void setupPostAttributes() {
        mPost.setTitle(POST_DEFAULT_TITLE);
        mPost.setContent(POST_DEFAULT_DESCRIPTION);
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

    private void uploadPost(PostModel post) throws InterruptedException {
        mNextEvent = TestEvents.UPLOADED_POST;
        mCountDownLatch = new CountDownLatch(1);

        RemotePostPayload pushPayload = new RemotePostPayload(post, sSite);
        mDispatcher.dispatch(PostActionBuilder.newPushPostAction(pushPayload));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void clearMedia(PostModel post, Set<MediaModel> media) throws InterruptedException {
        mNextEvent = TestEvents.CLEARED_MEDIA;
        mCountDownLatch = new CountDownLatch(1);
        ClearMediaPayload clearMediaPayload = new ClearMediaPayload(post, media);
        mDispatcher.dispatch(UploadActionBuilder.newClearMediaAction(clearMediaPayload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private static MediaUploadModel getMediaUploadModelForMediaModel(MediaModel mediaModel) {
        return UploadSqlUtils.getMediaUploadModelForLocalId(mediaModel.getId());
    }

    private static PostUploadModel getPostUploadModelForPostModel(PostModel postModel) {
        return UploadSqlUtils.getPostUploadModelForLocalId(postModel.getId());
    }
}
