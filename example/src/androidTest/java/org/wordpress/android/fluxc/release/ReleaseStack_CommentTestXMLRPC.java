package org.wordpress.android.fluxc.release;

import junit.framework.Assert;

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
import org.wordpress.android.fluxc.store.CommentStore.OnCommentChanged;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentPayload;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCreateCommentPayload;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.FetchPostsPayload;
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

    private enum TestEvents {
        NONE,
        POSTS_FETCHED,
        COMMENT_CHANGED,
        COMMENT_CHANGED_ERROR,
        COMMENT_CHANGED_UNKNOWN_COMMENT,
    }

    private TestEvents mNextEvent;
    private List<PostModel> mPosts;
    private List<CommentModel> mComments;
    private CommentModel mNewComment;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);

        // Authenticate, fetch sites and initialize sSite.
        init();
        // Fetch first posts
        fetchFirstPosts();
        mNextEvent = TestEvents.NONE;
    }

    public void testFetchComments() throws InterruptedException {
        FetchCommentsPayload payload = new FetchCommentsPayload(sSite, 10, 0);
        mNextEvent = TestEvents.COMMENT_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newFetchCommentsAction(payload));
        // Wait for a network response / onChanged event
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testFetchComment() throws InterruptedException {
        fetchFirstComments();
        // Get first comment
        CommentModel firstComment = mComments.get(0);
        // Pull the first comments
        RemoteCommentPayload fetchCommentPayload = new RemoteCommentPayload(sSite, firstComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newFetchCommentAction(fetchCommentPayload));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testInstantiateAndCreateNewComment() throws InterruptedException {
        // New Comment
        createNewComment();

        // Edit comment instance
        mNewComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");

        // Create new Comment
        mNextEvent = TestEvents.COMMENT_CHANGED;
        RemoteCreateCommentPayload payload = new RemoteCreateCommentPayload(sSite, mPosts.get(0), mNewComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check comment has been modified in the DB
        CommentModel comment = mCommentStore.getCommentByLocalId(mNewComment.getId());
        Assert.assertTrue(comment.getContent().contains(mNewComment.getContent()));
    }

    public void testInstantiateAndCreateNewCommentDuplicate() throws InterruptedException {
        // New Comment
        createNewComment();

        // Edit comment instance
        mNewComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");

        // Create new Comment
        mNextEvent = TestEvents.COMMENT_CHANGED;
        RemoteCreateCommentPayload payload = new RemoteCreateCommentPayload(sSite, mPosts.get(0), mNewComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Same comment again
        mNextEvent = TestEvents.COMMENT_CHANGED_ERROR;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testReplyToAnUnknownComment() throws InterruptedException {
        CommentModel fakeComment = new CommentModel();
        CommentModel newComment = new CommentModel();
        // Fake data
        newComment.setAuthorName("test");
        newComment.setAuthorEmail("test");
        newComment.setContent("test");
        newComment.setAuthorUrl("test");

        mNextEvent = TestEvents.COMMENT_CHANGED_UNKNOWN_COMMENT;
        RemoteCreateCommentPayload payload = new RemoteCreateCommentPayload(sSite, fakeComment, newComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testInstantiateAndCreateReplyComment() throws InterruptedException {
        // New Comment
        createNewComment();

        // Edit comment instance
        mNewComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");

        // Fetch existing comments and get first comment
        fetchFirstComments();
        CommentModel firstComment = mComments.get(0);

        // Create new Reply to that first comment
        mNextEvent = TestEvents.COMMENT_CHANGED;
        RemoteCreateCommentPayload payload = new RemoteCreateCommentPayload(sSite, firstComment, mNewComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check comment has been modified in the DB
        CommentModel comment = mCommentStore.getCommentByLocalId(mNewComment.getId());
        Assert.assertEquals(comment.getContent(), mNewComment.getContent());
        Assert.assertEquals(comment.getRemoteParentCommentId(), firstComment.getRemoteCommentId());
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
        mNextEvent = TestEvents.COMMENT_CHANGED;
        mDispatcher.dispatch(CommentActionBuilder.newPushCommentAction(pushCommentPayload));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        CommentModel comment = mCommentStore.getCommentByLocalId(firstComment.getId());
        Assert.assertEquals(comment.getContent(), firstComment.getContent());
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
        mNextEvent = TestEvents.COMMENT_CHANGED_ERROR;
        mDispatcher.dispatch(CommentActionBuilder.newPushCommentAction(pushCommentPayload));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testDeleteCommentOnce() throws InterruptedException {
        // New Comment
        createNewComment();

        // Edit comment instance
        mNewComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");

        // Create new Comment
        mNextEvent = TestEvents.COMMENT_CHANGED;
        RemoteCreateCommentPayload payload1 = new RemoteCreateCommentPayload(sSite, mPosts.get(0), mNewComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload1));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check comment has been modified in the DB
        CommentModel comment = mCommentStore.getCommentByLocalId(mNewComment.getId());
        Assert.assertTrue(comment.getContent().contains(mNewComment.getContent()));

        // Delete
        mNextEvent = TestEvents.COMMENT_CHANGED;
        RemoteCommentPayload payload2 = new RemoteCommentPayload(sSite, comment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newDeleteCommentAction(payload2));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Make sure the comment is still here but state changed
        comment = mCommentStore.getCommentByLocalId(mNewComment.getId());
        Assert.assertEquals(CommentStatus.TRASH.toString(), comment.getStatus());
    }

    public void testDeleteCommentTwice() throws InterruptedException {
        // New Comment
        createNewComment();

        // Edit comment instance
        mNewComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");

        // Create new Comment
        mNextEvent = TestEvents.COMMENT_CHANGED;
        RemoteCreateCommentPayload payload1 = new RemoteCreateCommentPayload(sSite, mPosts.get(0), mNewComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload1));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check comment has been modified in the DB
        CommentModel comment = mCommentStore.getCommentByLocalId(mNewComment.getId());
        Assert.assertTrue(comment.getContent().contains(mNewComment.getContent()));

        // Delete once (ie. move to trash)
        mNextEvent = TestEvents.COMMENT_CHANGED;
        RemoteCommentPayload payload2 = new RemoteCommentPayload(sSite, comment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newDeleteCommentAction(payload2));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Delete twice (ie. real delete)
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newDeleteCommentAction(payload2));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Make sure the comment was deleted (local test only, but should mean it was deleted correctly on the server)
        comment = mCommentStore.getCommentByLocalId(mNewComment.getId());
        Assert.assertEquals(null, comment);
    }

    // OnChanged Events

    @SuppressWarnings("unused")
    @Subscribe
    public void onCommentChanged(OnCommentChanged event) {
        List<CommentModel> comments = mCommentStore.getCommentsForSite(sSite, true, CommentStatus.ALL);
        if (event.isError()) {
            AppLog.i(T.TESTS, "event error type: " + event.error.type);
            if (mNextEvent == TestEvents.COMMENT_CHANGED_UNKNOWN_COMMENT) {
                Assert.assertEquals(event.error.type, CommentErrorType.GENERIC_ERROR);
            } else if (mNextEvent == TestEvents.COMMENT_CHANGED_ERROR) {
                Assert.assertEquals(event.error.type, CommentErrorType.GENERIC_ERROR);
            } else {
                throw new AssertionError("Error occurred for event: " + mNextEvent + " with type: " + event.error.type);
            }
            mCountDownLatch.countDown();
            return;
        }
        AppLog.i(T.TESTS, "comments count " + comments.size());
        Assert.assertEquals(TestEvents.COMMENT_CHANGED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onPostChanged(OnPostChanged event) {
        mPosts = mPostStore.getPostsForSite(sSite);
        Assert.assertEquals(mNextEvent, TestEvents.POSTS_FETCHED);
        mCountDownLatch.countDown();
    }

    // Private methods

    private CommentModel createNewComment() {
        CommentModel comment = mCommentStore.instantiateCommentModel(sSite);

        Assert.assertNotNull(comment);
        Assert.assertTrue(comment.getId() != 0);

        mNewComment = comment;
        return comment;
    }

    private void fetchFirstComments() throws InterruptedException {
        if (mComments != null) {
            return;
        }
        FetchCommentsPayload payload = new FetchCommentsPayload(sSite, 10, 0);
        mNextEvent = TestEvents.COMMENT_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newFetchCommentsAction(payload));
        // Wait for a network response / onChanged event
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        mComments = mCommentStore.getCommentsForSite(sSite, true, CommentStatus.ALL);
    }

    private void fetchFirstPosts() throws InterruptedException {
        mNextEvent = TestEvents.POSTS_FETCHED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(PostActionBuilder.newFetchPostsAction(new FetchPostsPayload(sSite, false)));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
