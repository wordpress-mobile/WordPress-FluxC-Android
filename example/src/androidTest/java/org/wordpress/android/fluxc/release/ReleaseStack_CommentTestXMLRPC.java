package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.CommentActionBuilder;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.store.CommentStore;
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsPayload;
import org.wordpress.android.fluxc.store.CommentStore.InstantiateCommentPayload;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentPayload;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCreateCommentPayload;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_CommentTestXMLRPC extends ReleaseStack_XMLRPCBase {
    @Inject CommentStore mCommentStore;
    @Inject PostStore mPostStore;

    private List<PostModel> mPosts;
    private List<CommentModel> mComments;
    private CommentModel mNewComment;

    private enum TEST_EVENTS {
        NONE,
        POSTS_FETCHED,
        COMMENT_INSTANTIATED,
        COMMENT_CHANGED,
        COMMENT_CHANGED_ERROR,
        COMMENT_CHANGED_UNKNOWN_COMMENT,
    }
    private TEST_EVENTS mNextEvent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);

        // Register and initialize sSite
        init();
        // Fetch first posts
        fetchFirstPosts();
        mNextEvent = TEST_EVENTS.NONE;
    }

    public void testFetchComments() throws InterruptedException {
        FetchCommentsPayload payload = new FetchCommentsPayload(sSite, 10, 0);
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newFetchCommentsAction(payload));
        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testFetchComment() throws InterruptedException {
        fetchFirstComments();
        // Get first comment
        CommentModel firstComment = mComments.get(0);
        // Pull the first comments
        RemoteCommentPayload fetchCommentPayload = new RemoteCommentPayload(sSite, firstComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newFetchCommentAction(fetchCommentPayload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testInstantiateAndCreateNewComment() throws InterruptedException {
        // New Comment
        InstantiateCommentPayload payload1 = new InstantiateCommentPayload(sSite);
        mNextEvent = TEST_EVENTS.COMMENT_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newInstantiateCommentAction(payload1));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Edit comment instance
        mNewComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");

        // Create new Comment
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        RemoteCreateCommentPayload payload2 = new RemoteCreateCommentPayload(sSite, mPosts.get(0), mNewComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload2));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check comment has been modified in the DB
        CommentModel comment = mCommentStore.getCommentByLocalId(mNewComment.getId());
        assertTrue(comment.getContent().contains(mNewComment.getContent()));
    }

    public void testInstantiateAndCreateNewCommentDuplicate() throws InterruptedException {
        // New Comment
        InstantiateCommentPayload payload1 = new InstantiateCommentPayload(sSite);
        mNextEvent = TEST_EVENTS.COMMENT_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newInstantiateCommentAction(payload1));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Edit comment instance
        mNewComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");

        // Create new Comment
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        RemoteCreateCommentPayload payload2 = new RemoteCreateCommentPayload(sSite, mPosts.get(0), mNewComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload2));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Same comment again
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED_ERROR;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload2));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testReplyToAnUnknownComment() throws InterruptedException {
        CommentModel fakeComment = new CommentModel();
        CommentModel newComment = new CommentModel();
        // Fake data
        newComment.setAuthorName("test");
        newComment.setAuthorEmail("test");
        newComment.setContent("test");
        newComment.setAuthorUrl("test");

        mNextEvent = TEST_EVENTS.COMMENT_CHANGED_UNKNOWN_COMMENT;
        RemoteCreateCommentPayload payload = new RemoteCreateCommentPayload(sSite, fakeComment, newComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testInstantiateAndCreateReplyComment() throws InterruptedException {
        // New Comment
        InstantiateCommentPayload payload1 = new InstantiateCommentPayload(sSite);
        mNextEvent = TEST_EVENTS.COMMENT_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newInstantiateCommentAction(payload1));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS , TimeUnit.MILLISECONDS));

        // Edit comment instance
        mNewComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");

        // Fetch existing comments and get first comment
        fetchFirstComments();
        CommentModel firstComment = mComments.get(0);

        // Create new Reply to that first comment
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        RemoteCreateCommentPayload payload2 = new RemoteCreateCommentPayload(sSite, firstComment, mNewComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload2));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check comment has been modified in the DB
        CommentModel comment = mCommentStore.getCommentByLocalId(mNewComment.getId());
        assertEquals(comment.getContent(), mNewComment.getContent());
        assertEquals(comment.getRemoteParentCommentId(), firstComment.getRemoteCommentId());
    }

    public void testEditValidComment() throws InterruptedException {
        fetchFirstComments();

        // Get first comment
        CommentModel firstComment = mComments.get(0);

        // Edit the comment
        firstComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");
        firstComment.setStatus(CommentStatus.APPROVED.toString());

        // Push the edited comment
        RemoteCommentPayload pushCommentPayload = new RemoteCommentPayload(sSite, firstComment);
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        mDispatcher.dispatch(CommentActionBuilder.newPushCommentAction(pushCommentPayload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        CommentModel comment = mCommentStore.getCommentByLocalId(firstComment.getId());
        assertEquals(comment.getContent(), firstComment.getContent());
    }

    public void testEditInvalidComment() throws InterruptedException {
        fetchFirstComments();

        // Get first comment
        CommentModel firstComment = mComments.get(0);

        // Edit the comment
        firstComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");
        firstComment.setRemoteCommentId(-1); // set an incorrect id

        // Push the edited comment
        RemoteCommentPayload pushCommentPayload = new RemoteCommentPayload(sSite, firstComment);
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED_ERROR;
        mDispatcher.dispatch(CommentActionBuilder.newPushCommentAction(pushCommentPayload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testDeleteCommentOnce() throws InterruptedException {
        // New Comment
        InstantiateCommentPayload payload1 = new InstantiateCommentPayload(sSite);
        mNextEvent = TEST_EVENTS.COMMENT_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newInstantiateCommentAction(payload1));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Edit comment instance
        mNewComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");

        // Create new Comment
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        RemoteCreateCommentPayload payload2 = new RemoteCreateCommentPayload(sSite, mPosts.get(0), mNewComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload2));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check comment has been modified in the DB
        CommentModel comment = mCommentStore.getCommentByLocalId(mNewComment.getId());
        assertTrue(comment.getContent().contains(mNewComment.getContent()));

        // Delete
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        RemoteCommentPayload payload3 = new RemoteCommentPayload(sSite, comment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newDeleteCommentAction(payload3));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Make sure the comment is still here but state changed
        comment = mCommentStore.getCommentByLocalId(mNewComment.getId());
        assertEquals(CommentStatus.TRASH.toString(), comment.getStatus());
    }

    public void testDeleteCommentTwice() throws InterruptedException {
        // New Comment
        InstantiateCommentPayload payload1 = new InstantiateCommentPayload(sSite);
        mNextEvent = TEST_EVENTS.COMMENT_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newInstantiateCommentAction(payload1));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Edit comment instance
        mNewComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");

        // Create new Comment
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        RemoteCreateCommentPayload payload2 = new RemoteCreateCommentPayload(sSite, mPosts.get(0), mNewComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload2));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check comment has been modified in the DB
        CommentModel comment = mCommentStore.getCommentByLocalId(mNewComment.getId());
        assertTrue(comment.getContent().contains(mNewComment.getContent()));

        // Delete once (ie. move to trash)
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        RemoteCommentPayload payload3 = new RemoteCommentPayload(sSite, comment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newDeleteCommentAction(payload3));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Delete twice (ie. real delete)
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newDeleteCommentAction(payload3));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Make sure the comment was deleted (local test only, but should mean it was deleted correctly on the server)
        comment = mCommentStore.getCommentByLocalId(mNewComment.getId());
        assertEquals(null, comment);
    }

    // OnChanged Events

    @SuppressWarnings("unused")
    @Subscribe
    public void onCommentChanged(CommentStore.OnCommentChanged event) {
        List<CommentModel> comments = mCommentStore.getCommentsForSite(sSite, CommentStatus.ALL);
        if (event.isError()) {
            AppLog.i(T.TESTS, "event error type: " + event.error.type);
            if (mNextEvent == TEST_EVENTS.COMMENT_CHANGED_UNKNOWN_COMMENT) {
                assertEquals(event.error.type, CommentErrorType.GENERIC_ERROR);
            } else if (mNextEvent == TEST_EVENTS.COMMENT_CHANGED_ERROR) {
                assertEquals(event.error.type, CommentErrorType.GENERIC_ERROR);
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
        mPosts = mPostStore.getPostsForSite(sSite);
        assertEquals(mNextEvent, TEST_EVENTS.POSTS_FETCHED);
        mCountDownLatch.countDown();
    }

    // Private methods

    private void fetchFirstComments() throws InterruptedException {
        if (mComments != null) {
            return;
        }
        FetchCommentsPayload payload = new FetchCommentsPayload(sSite, 10, 0);
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newFetchCommentsAction(payload));
        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        mComments = mCommentStore.getCommentsForSite(sSite, CommentStatus.ALL);
    }

    private void fetchFirstPosts() throws InterruptedException {
        mNextEvent = TEST_EVENTS.POSTS_FETCHED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(PostActionBuilder.newFetchPostsAction(new PostStore.FetchPostsPayload(sSite, false)));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
