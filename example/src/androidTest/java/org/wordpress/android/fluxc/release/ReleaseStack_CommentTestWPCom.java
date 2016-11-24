package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.CommentActionBuilder;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.persistence.CommentSqlUtils;
import org.wordpress.android.fluxc.store.CommentStore;
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsPayload;
import org.wordpress.android.fluxc.store.CommentStore.InstantiateCommentPayload;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentPayload;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCreateCommentPayload;
import org.wordpress.android.fluxc.store.CommentStore.RemoteLikeCommentPayload;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_CommentTestWPCom extends ReleaseStack_WPComBase {
    @Inject CommentStore mCommentStore;
    @Inject PostStore mPostStore;

    private List<CommentModel> mComments;
    private CommentModel mNewComment;
    private PostModel mFirstPost;

    private enum TEST_EVENTS {
        NONE,
        POSTS_FETCHED,
        COMMENT_INSTANTIATED,
        COMMENT_CHANGED,
        COMMENT_CHANGED_ERROR,
        COMMENT_CHANGED_UNKNOWN_COMMENT,
        COMMENT_CHANGED_UNKNOWN_POST,
        COMMENT_CHANGED_DUPLICATE_COMMENT,
    }

    private TEST_EVENTS mNextEvent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);

        // Authenticate, fetch sites and initialize mSite.
        init();
        // Fetch first posts
        fetchFirstPosts();
        // Init mNextEvent
        mNextEvent = TEST_EVENTS.NONE;
    }

    // Note: This test is not specific to WPCOM (local changes only)
    public void testInstantiateComment() throws InterruptedException {
        // New Comment
        InstantiateCommentPayload payload = new InstantiateCommentPayload(mSite);
        mNextEvent = TEST_EVENTS.COMMENT_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newInstantiateCommentAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Verify it was inserted in the DB
        List<CommentModel> comments = CommentSqlUtils.getCommentsForSite(mSite, CommentStatus.ALL);
        assertEquals(mNewComment.getId(), comments.get(0).getId());
    }

    // Note: This test is not specific to WPCOM (local changes only)
    public void testInstantiateUpdateAndRemoveComment() throws InterruptedException {
        // New Comment
        InstantiateCommentPayload payload = new InstantiateCommentPayload(mSite);
        mNextEvent = TEST_EVENTS.COMMENT_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newInstantiateCommentAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Edit comment instance
        mNewComment.setContent("You should send 1.21 gigawatts into the flux capacitor.");

        // Update
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newUpdateCommentAction(mNewComment));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check the last comment we get from the DB is the same
        List<CommentModel> comments = CommentSqlUtils.getCommentsForSite(mSite, CommentStatus.ALL);
        assertEquals(mNewComment.getContent(), comments.get(0).getContent());

        // Remove that comment
        mDispatcher.dispatch(CommentActionBuilder.newRemoveCommentAction(mNewComment));

        // Check the last comment we get from the DB is different
        comments = CommentSqlUtils.getCommentsForSite(mSite, CommentStatus.ALL);
        if (comments.size() != 0) {
            assertNotSame(mNewComment.getId(), comments.get(0).getId());
        }
    }

    public void testRemoveAllComments() throws InterruptedException {
        fetchFirstComments();

        int count = mCommentStore.getNumberOfCommentsForSite(mSite, CommentStatus.ALL);
        assertNotSame(0, count); // Only work if the site has at least one comment.

        // Remove all comments for this site
        mDispatcher.dispatch(CommentActionBuilder.newRemoveCommentsAction(mSite));

        count = mCommentStore.getNumberOfCommentsForSite(mSite, CommentStatus.ALL);
        assertEquals(0, count);
    }

    public void testInstantiateAndCreateNewComment() throws InterruptedException {
        // New Comment
        InstantiateCommentPayload payload1 = new InstantiateCommentPayload(mSite);
        mNextEvent = TEST_EVENTS.COMMENT_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newInstantiateCommentAction(payload1));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Edit comment instance
        mNewComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");

        // Create new Comment
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        RemoteCreateCommentPayload payload2 = new RemoteCreateCommentPayload(mSite, mFirstPost, mNewComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload2));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check comment has been modified in the DB
        CommentModel comment = mCommentStore.getCommentByLocalId(mNewComment.getId());
        assertTrue(comment.getContent().contains(mNewComment.getContent()));
    }

    public void testLikeAndUnlikeComment() throws InterruptedException {
        // Fetch existing comments and get first comment
        fetchFirstComments();
        CommentModel firstComment = mComments.get(0);

        // Like comment
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newLikeCommentAction(new RemoteLikeCommentPayload(mSite,
                firstComment, true)));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check comment has been modified in the DB
        CommentModel comment = mCommentStore.getCommentByLocalId(firstComment.getId());
        assertTrue(comment.getILike());

        // Unlike comment
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newLikeCommentAction(new RemoteLikeCommentPayload(mSite,
                firstComment, false)));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check comment has been modified in the DB
        comment = mCommentStore.getCommentByLocalId(firstComment.getId());
        assertFalse(comment.getILike());
    }

    public void testDeleteCommentOnce() throws InterruptedException {
        // New Comment
        InstantiateCommentPayload payload1 = new InstantiateCommentPayload(mSite);
        mNextEvent = TEST_EVENTS.COMMENT_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newInstantiateCommentAction(payload1));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Edit comment instance
        mNewComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");

        // Create new Comment
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        RemoteCreateCommentPayload payload2 = new RemoteCreateCommentPayload(mSite, mFirstPost, mNewComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload2));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check comment has been modified in the DB
        CommentModel comment = mCommentStore.getCommentByLocalId(mNewComment.getId());
        assertTrue(comment.getContent().contains(mNewComment.getContent()));

        // Delete
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        RemoteCommentPayload payload3 = new RemoteCommentPayload(mSite, comment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newDeleteCommentAction(payload3));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Make sure the comment is still here but state changed
        comment = mCommentStore.getCommentByLocalId(mNewComment.getId());
        assertEquals(CommentStatus.TRASH.toString(), comment.getStatus());
    }

    public void testDeleteCommentTwice() throws InterruptedException {
        // New Comment
        InstantiateCommentPayload payload1 = new InstantiateCommentPayload(mSite);
        mNextEvent = TEST_EVENTS.COMMENT_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newInstantiateCommentAction(payload1));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Edit comment instance
        mNewComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");

        // Create new Comment
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        RemoteCreateCommentPayload payload2 = new RemoteCreateCommentPayload(mSite, mFirstPost, mNewComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload2));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check comment has been modified in the DB
        CommentModel comment = mCommentStore.getCommentByLocalId(mNewComment.getId());
        assertTrue(comment.getContent().contains(mNewComment.getContent()));

        // Delete once (ie. move to trash)
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        RemoteCommentPayload payload3 = new RemoteCommentPayload(mSite, comment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newDeleteCommentAction(payload3));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Delete twice (ie. real delete)
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newDeleteCommentAction(payload3));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Make sure the comment was deleted (local test only, but should mean it was deleted correctly on the server)
        comment = mCommentStore.getCommentByLocalId(mNewComment.getId());
        assertEquals(comment, null);
    }

    public void testErrorDuplicatedComment() throws InterruptedException {
        // New Comment
        InstantiateCommentPayload payload1 = new InstantiateCommentPayload(mSite);
        mNextEvent = TEST_EVENTS.COMMENT_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newInstantiateCommentAction(payload1));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Edit comment instance
        mNewComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");

        // Create new Comment
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        RemoteCreateCommentPayload payload2 = new RemoteCreateCommentPayload(mSite, mFirstPost, mNewComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload2));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Dispatch the same payload (with the same comment), we should get a 409 error "comment_duplicate"
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED_DUPLICATE_COMMENT;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload2));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testNewCommentToAnUnknownPost() throws InterruptedException {
        CommentModel newComment = new CommentModel();

        PostModel fakePost = mFirstPost.clone();
        fakePost.setRemotePostId(111111111111111111L);

        mNextEvent = TEST_EVENTS.COMMENT_CHANGED_UNKNOWN_POST;
        RemoteCreateCommentPayload payload = new RemoteCreateCommentPayload(mSite, fakePost, newComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testReplyToAnUnknownComment() throws InterruptedException {
        CommentModel fakeComment = new CommentModel();
        CommentModel newComment = new CommentModel();

        mNextEvent = TEST_EVENTS.COMMENT_CHANGED_UNKNOWN_COMMENT;
        RemoteCreateCommentPayload payload = new RemoteCreateCommentPayload(mSite, fakeComment, newComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testInstantiateAndCreateReplyComment() throws InterruptedException {
        // New Comment
        InstantiateCommentPayload payload1 = new InstantiateCommentPayload(mSite);
        mNextEvent = TEST_EVENTS.COMMENT_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newInstantiateCommentAction(payload1));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Edit comment instance
        mNewComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");

        // Fetch existing comments and get first comment
        fetchFirstComments();
        CommentModel firstComment = mComments.get(0);

        // Create new Reply to that first comment
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        RemoteCreateCommentPayload payload2 = new RemoteCreateCommentPayload(mSite, firstComment, mNewComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload2));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check comment has been modified in the DB
        CommentModel comment = mCommentStore.getCommentByLocalId(mNewComment.getId());
        // Using .contains() in the assert below because server might wrap the response in <p>
        assertTrue(comment.getContent().contains(mNewComment.getContent()));
        assertNotSame(mNewComment.getRemoteCommentId(), comment.getRemoteCommentId());
        assertNotSame(mNewComment.getRemoteSiteId(), comment.getRemoteSiteId());
    }

    public void testFetchComments() throws InterruptedException {
        FetchCommentsPayload payload = new FetchCommentsPayload(mSite, 10, 0);
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newFetchCommentsAction(payload));
        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testFetchComment() throws InterruptedException {
        fetchFirstComments();
        CommentModel firstComment = mComments.get(0);

        RemoteCommentPayload payload = new RemoteCommentPayload(mSite, firstComment);
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newFetchCommentAction(payload));
        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testEditValidComment() throws InterruptedException {
        fetchFirstComments();

        // Get first comment
        CommentModel firstComment = mComments.get(0);

        // Edit the comment
        firstComment.setContent("If we could somehow harness this lightning: " + (new Random()).nextFloat() * 10);
        firstComment.setStatus(CommentStatus.APPROVED.toString());

        // Push the edited comment
        RemoteCommentPayload pushCommentPayload = new RemoteCommentPayload(mSite, firstComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newPushCommentAction(pushCommentPayload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Make sure it was edited
        RemoteCommentPayload payload = new RemoteCommentPayload(mSite, firstComment);
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newFetchCommentAction(payload));
        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        CommentModel updatedComment = CommentSqlUtils.getCommentByLocalCommentId(firstComment.getId());
        assertNotNull(updatedComment);
        assertTrue(updatedComment.getContent().contains(firstComment.getContent()));
    }

    public void testEditInvalidComment() throws InterruptedException {
        CommentModel comment = new CommentModel();
        comment.setContent("");
        comment.setDatePublished("");
        comment.setStatus("approved");
        // Try to push the invalid comment
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED_ERROR;
        RemoteCommentPayload pushCommentPayload = new RemoteCommentPayload(mSite, comment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newPushCommentAction(pushCommentPayload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onCommentChanged(CommentStore.OnCommentChanged event) {
        List<CommentModel> comments = mCommentStore.getCommentsForSite(mSite, CommentStatus.ALL);
        if (event.isError()) {
            AppLog.i(T.TESTS, "event error type: " + event.error.type);
            if (mNextEvent == TEST_EVENTS.COMMENT_CHANGED_UNKNOWN_COMMENT) {
                assertEquals(event.error.type, CommentErrorType.UNKNOWN_COMMENT);
            } else if (mNextEvent == TEST_EVENTS.COMMENT_CHANGED_UNKNOWN_POST) {
                assertEquals(event.error.type, CommentErrorType.UNKNOWN_POST);
            } else if (mNextEvent == TEST_EVENTS.COMMENT_CHANGED_DUPLICATE_COMMENT) {
                assertEquals(event.error.type, CommentErrorType.DUPLICATE_COMMENT);
            } else if (mNextEvent == TEST_EVENTS.COMMENT_CHANGED_ERROR) {
                assertEquals(event.error.type, CommentErrorType.UNKNOWN_COMMENT);
            } else {
                throw new AssertionError("Error occurred for event: " + mNextEvent + " with type: " + event.error.type);
            }
            mCountDownLatch.countDown();
            return;
        }
        AppLog.i(T.TESTS, "comments count " + comments.size());
        assertEquals(TEST_EVENTS.COMMENT_CHANGED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onCommentInstantiated(CommentStore.OnCommentInstantiated event) {
        mNewComment = event.comment;
        assertNotNull(mNewComment);
        assertTrue(event.comment.getId() != 0);
        assertEquals(TEST_EVENTS.COMMENT_INSTANTIATED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onPostChanged(OnPostChanged event) {
        List<PostModel> posts = mPostStore.getPostsForSite(mSite);
        mFirstPost = getFirstPublishedPost(posts);
        assertEquals(mNextEvent, TEST_EVENTS.POSTS_FETCHED);
        mCountDownLatch.countDown();
    }

    private PostModel getFirstPublishedPost(List<PostModel> posts) {
        for (PostModel post : posts) {
            if (PostStatus.fromPost(post) == PostStatus.PUBLISHED) {
                return post;
            }
        }
        return null;
    }

    // Private methods

    private void fetchFirstComments() throws InterruptedException {
        if (mComments != null) {
            return;
        }
        FetchCommentsPayload payload = new FetchCommentsPayload(mSite, 10, 0);
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newFetchCommentsAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        mComments = mCommentStore.getCommentsForSite(mSite, CommentStatus.ALL);
    }

    private void fetchFirstPosts() throws InterruptedException {
        mNextEvent = TEST_EVENTS.POSTS_FETCHED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(PostActionBuilder.newFetchPostsAction(new PostStore.FetchPostsPayload(mSite, false)));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
