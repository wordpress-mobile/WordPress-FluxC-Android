package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.CommentActionBuilder;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.persistence.CommentSqlUtils;
import org.wordpress.android.fluxc.store.CommentStore;
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

public class ReleaseStack_CommentTestWPCom extends ReleaseStack_WPComBase {
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
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Verify it was inserted in the DB
        List<CommentModel> comments = CommentSqlUtils.getCommentsForSite(mSite);
        assertEquals(mNewComment.getId(), comments.get(0).getId());
    }

    public void testInstantiateAndCreateNewComment() throws InterruptedException {
        // New Comment
        InstantiateCommentPayload payload1 = new InstantiateCommentPayload(mSite);
        mNextEvent = TEST_EVENTS.COMMENT_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newInstantiateCommentAction(payload1));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Edit comment instance
        mNewComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");

        // Create new Comment
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        RemoteCreateCommentPayload payload2 = new RemoteCreateCommentPayload(mSite, mPosts.get(0), mNewComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload2));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check comment has been modified in the DB
        CommentModel comment = mCommentStore.getCommentByLocalId(mNewComment.getId());
        assertTrue(comment.getContent().contains(mNewComment.getContent()));
    }

    public void testDeleteOnceComment() throws InterruptedException {
        // New Comment
        InstantiateCommentPayload payload1 = new InstantiateCommentPayload(mSite);
        mNextEvent = TEST_EVENTS.COMMENT_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newInstantiateCommentAction(payload1));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Edit comment instance
        mNewComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");

        // Create new Comment
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        RemoteCreateCommentPayload payload2 = new RemoteCreateCommentPayload(mSite, mPosts.get(0), mNewComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload2));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check comment has been modified in the DB
        CommentModel comment = mCommentStore.getCommentByLocalId(mNewComment.getId());
        assertTrue(comment.getContent().contains(mNewComment.getContent()));

        // Delete
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        RemoteCommentPayload payload3 = new RemoteCommentPayload(mSite, comment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newDeleteCommentAction(payload3));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Make sure the comment is still here but state changed
        comment = mCommentStore.getCommentByLocalId(mNewComment.getId());
        assertEquals(CommentStatus.TRASH.toString(), comment.getStatus());
    }

    public void testDeleteTwiceComment() throws InterruptedException {
        // New Comment
        InstantiateCommentPayload payload1 = new InstantiateCommentPayload(mSite);
        mNextEvent = TEST_EVENTS.COMMENT_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newInstantiateCommentAction(payload1));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Edit comment instance
        mNewComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");

        // Create new Comment
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        RemoteCreateCommentPayload payload2 = new RemoteCreateCommentPayload(mSite, mPosts.get(0), mNewComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload2));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check comment has been modified in the DB
        CommentModel comment = mCommentStore.getCommentByLocalId(mNewComment.getId());
        assertTrue(comment.getContent().contains(mNewComment.getContent()));

        // Delete once (ie. move to trash)
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        RemoteCommentPayload payload3 = new RemoteCommentPayload(mSite, comment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newDeleteCommentAction(payload3));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Delete twice (ie. real delete)
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newDeleteCommentAction(payload3));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

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
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Edit comment instance
        mNewComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");

        // Create new Comment
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        RemoteCreateCommentPayload payload2 = new RemoteCreateCommentPayload(mSite, mPosts.get(0), mNewComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload2));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Dispatch the same payload (with the same comment), we should get a 409 error "comment_duplicate"
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED_ERROR;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload2));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testInstantiateAndCreateReplyComment() throws InterruptedException {
        // New Comment
        InstantiateCommentPayload payload1 = new InstantiateCommentPayload(mSite);
        mNextEvent = TEST_EVENTS.COMMENT_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newInstantiateCommentAction(payload1));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS , TimeUnit.MILLISECONDS));

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
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check comment has been modified in the DB
        CommentModel comment = mCommentStore.getCommentByLocalId(mNewComment.getId());
        // Using .contains() in the assert below because server might wrap the response in <p>
        assertTrue(comment.getContent().contains(mNewComment.getContent()));
        assertNotSame(mNewComment.getRemoteCommentId(), comment.getRemoteCommentId());
        assertNotSame(mNewComment.getRemoteSiteId(), comment.getRemoteSiteId());
    }

    // Note: This test is not specific to WPCOM (local changes only)
    public void testInstantiateUpdateAndRemoveComment() throws InterruptedException {
        // New Comment
        InstantiateCommentPayload payload = new InstantiateCommentPayload(mSite);
        mNextEvent = TEST_EVENTS.COMMENT_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newInstantiateCommentAction(payload));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Edit comment instance
        mNewComment.setContent("You should send 1.21 gigawatts into the flux capacitor.");

        // Update
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newUpdateCommentAction(mNewComment));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check the last comment we get from the DB is the same
        List<CommentModel> comments = CommentSqlUtils.getCommentsForSite(mSite);
        assertEquals(mNewComment.getContent(), comments.get(0).getContent());

        // Remove that comment
        mDispatcher.dispatch(CommentActionBuilder.newRemoveCommentAction(mNewComment));

        // Check the last comment we get from the DB is different
        comments = CommentSqlUtils.getCommentsForSite(mSite);
        if (comments.size() != 0) {
            assertNotSame(mNewComment.getId(), comments.get(0).getId());
        }
    }

    public void testFetchComments() throws InterruptedException {
        FetchCommentsPayload payload = new FetchCommentsPayload(mSite);
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newFetchCommentsAction(payload));
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testEditValidComment() throws InterruptedException {
        fetchFirstComments();

        // Get first comment
        CommentModel firstComment = mComments.get(0);

        // Edit the comment
        firstComment.setContent("If we could somehow harness this lightning... "
                                + "channel it into the flux capacitor... it just might work.");
        firstComment.setStatus(CommentStatus.APPROVED.toString());

        // Push the edited comment
        RemoteCommentPayload pushCommentPayload = new RemoteCommentPayload(mSite, firstComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newPushCommentAction(pushCommentPayload));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
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
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Subscribe
    public void onCommentChanged(CommentStore.OnCommentChanged event) {
        List<CommentModel> comments = mCommentStore.getCommentsForSite(mSite);
        if (event.isError()) {
            AppLog.i(T.TESTS, "event error type: " + event.error.type);
            if (mNextEvent != TEST_EVENTS.COMMENT_CHANGED_ERROR) {
                assertTrue("onCommentChanged Error", false);
            }
            mCountDownLatch.countDown();
            return;
        }
        AppLog.i(T.TESTS, "comments count " + comments.size());
        assertEquals(TEST_EVENTS.COMMENT_CHANGED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onCommentInstantiated(CommentStore.OnCommentInstantiated event) {
        mNewComment = event.comment;
        assertTrue(event.comment.getId() != 0);
        assertEquals(TEST_EVENTS.COMMENT_INSTANTIATED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onPostChanged(OnPostChanged event) {
        mPosts = mPostStore.getPostsForSite(mSite);
        assertEquals(mNextEvent, TEST_EVENTS.POSTS_FETCHED);
        mCountDownLatch.countDown();
    }

    // Private methods

    private void fetchFirstComments() throws InterruptedException {
        if (mComments != null) {
            return;
        }
        FetchCommentsPayload payload = new FetchCommentsPayload(mSite);
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newFetchCommentsAction(payload));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        mComments = mCommentStore.getCommentsForSite(mSite);
    }

    private void fetchFirstPosts() throws InterruptedException {
        mNextEvent = TEST_EVENTS.POSTS_FETCHED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(PostActionBuilder.newFetchPostsAction(new PostStore.FetchPostsPayload(mSite, false)));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
