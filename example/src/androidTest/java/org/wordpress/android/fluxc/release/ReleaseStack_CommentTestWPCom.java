package org.wordpress.android.fluxc.release;

import com.yarolegovich.wellsql.SelectQuery;

import junit.framework.Assert;

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
import org.wordpress.android.fluxc.store.CommentStore.OnCommentChanged;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentPayload;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCreateCommentPayload;
import org.wordpress.android.fluxc.store.CommentStore.RemoteLikeCommentPayload;
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

public class ReleaseStack_CommentTestWPCom extends ReleaseStack_WPComBase {
    @Inject CommentStore mCommentStore;
    @Inject PostStore mPostStore;

    private enum TestEvents {
        NONE,
        POSTS_FETCHED,
        COMMENT_CHANGED,
        COMMENT_CHANGED_ERROR,
        COMMENT_CHANGED_UNKNOWN_COMMENT,
        COMMENT_CHANGED_UNKNOWN_POST,
        COMMENT_CHANGED_DUPLICATE_COMMENT,
    }

    private TestEvents mNextEvent;
    private List<CommentModel> mComments;
    private CommentModel mNewComment;
    private PostModel mFirstPost;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);

        // Authenticate, fetch sites and initialize sSite
        init();
        // Fetch first posts
        fetchFirstPosts();
        // Init mNextEvent
        mNextEvent = TestEvents.NONE;
    }

    // Note: This test is not specific to WPCOM (local changes only)
    public void testInstantiateComment() throws InterruptedException {
        // New Comment
        createNewComment();

        // Verify it was inserted in the DB
        List<CommentModel> comments = CommentSqlUtils.getCommentsForSite(sSite, SelectQuery.ORDER_ASCENDING,
                CommentStatus.ALL);
        Assert.assertEquals(mNewComment.getId(), comments.get(0).getId());
    }

    // Note: This test is not specific to WPCOM (local changes only)
    public void testInstantiateUpdateAndRemoveComment() throws InterruptedException {
        // New Comment
        createNewComment();

        // Edit comment instance
        mNewComment.setContent("You should send 1.21 gigawatts into the flux capacitor.");

        // Update
        mNextEvent = TestEvents.COMMENT_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newUpdateCommentAction(mNewComment));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check the last comment we get from the DB is the same
        List<CommentModel> comments = CommentSqlUtils.getCommentsForSite(sSite, SelectQuery.ORDER_ASCENDING,
                CommentStatus.ALL);
        Assert.assertEquals(mNewComment.getContent(), comments.get(0).getContent());

        // Remove that comment
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newRemoveCommentAction(mNewComment));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check the last comment we get from the DB is different
        comments = CommentSqlUtils.getCommentsForSite(sSite, SelectQuery.ORDER_ASCENDING, CommentStatus.ALL);
        if (comments.size() != 0) {
            Assert.assertNotSame(mNewComment.getId(), comments.get(0).getId());
        }
    }

    public void testRemoveAllComments() throws InterruptedException {
        fetchFirstComments();

        int count = mCommentStore.getNumberOfCommentsForSite(sSite, CommentStatus.ALL);
        Assert.assertNotSame(0, count); // Only work if the site has at least one comment.

        // Remove all comments for this site
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newRemoveCommentsAction(sSite));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        count = mCommentStore.getNumberOfCommentsForSite(sSite, CommentStatus.ALL);
        Assert.assertEquals(0, count);
    }

    public void testInstantiateAndCreateNewComment() throws InterruptedException {
        // New Comment
        createNewComment();

        // Edit comment instance
        mNewComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");

        // Create new Comment
        mNextEvent = TestEvents.COMMENT_CHANGED;
        RemoteCreateCommentPayload payload = new RemoteCreateCommentPayload(sSite, mFirstPost, mNewComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check comment has been modified in the DB
        CommentModel comment = mCommentStore.getCommentByLocalId(mNewComment.getId());
        Assert.assertTrue(comment.getContent().contains(mNewComment.getContent()));
    }

    public void testLikeAndUnlikeComment() throws InterruptedException {
        // Fetch existing comments and get first comment
        fetchFirstComments();
        CommentModel firstComment = mComments.get(0);

        // Like comment
        mNextEvent = TestEvents.COMMENT_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newLikeCommentAction(new RemoteLikeCommentPayload(sSite,
                firstComment, true)));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check comment has been modified in the DB
        CommentModel comment = mCommentStore.getCommentByLocalId(firstComment.getId());
        Assert.assertTrue(comment.getILike());

        // Unlike comment
        mNextEvent = TestEvents.COMMENT_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newLikeCommentAction(new RemoteLikeCommentPayload(sSite,
                firstComment, false)));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check comment has been modified in the DB
        comment = mCommentStore.getCommentByLocalId(firstComment.getId());
        Assert.assertFalse(comment.getILike());
    }

    public void testDeleteCommentOnce() throws InterruptedException {
        // New Comment
        createNewComment();

        // Edit comment instance
        mNewComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");

        // Create new Comment
        mNextEvent = TestEvents.COMMENT_CHANGED;
        RemoteCreateCommentPayload payload1 = new RemoteCreateCommentPayload(sSite, mFirstPost, mNewComment);
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
        RemoteCreateCommentPayload payload1 = new RemoteCreateCommentPayload(sSite, mFirstPost, mNewComment);
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
        Assert.assertEquals(comment, null);
    }

    public void testErrorDuplicatedComment() throws InterruptedException {
        // New Comment
        createNewComment();

        // Edit comment instance
        mNewComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");

        // Create new Comment
        mNextEvent = TestEvents.COMMENT_CHANGED;
        RemoteCreateCommentPayload payload1 = new RemoteCreateCommentPayload(sSite, mFirstPost, mNewComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload1));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Dispatch the same payload (with the same comment), we should get a 409 error "comment_duplicate"
        mNextEvent = TestEvents.COMMENT_CHANGED_DUPLICATE_COMMENT;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload1));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testNewCommentToAnUnknownPost() throws InterruptedException {
        CommentModel newComment = new CommentModel();

        PostModel fakePost = mFirstPost.clone();
        fakePost.setRemotePostId(111111111111111111L);

        mNextEvent = TestEvents.COMMENT_CHANGED_UNKNOWN_POST;
        RemoteCreateCommentPayload payload = new RemoteCreateCommentPayload(sSite, fakePost, newComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testReplyToAnUnknownComment() throws InterruptedException {
        CommentModel fakeComment = new CommentModel();
        CommentModel newComment = new CommentModel();

        mNextEvent = TestEvents.COMMENT_CHANGED_UNKNOWN_COMMENT;
        RemoteCreateCommentPayload payload = new RemoteCreateCommentPayload(sSite, fakeComment, newComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testInstantiateAndCreateReplyComment() throws InterruptedException {
        // Fetch existing comments and get first comment
        fetchFirstComments();
        CommentModel firstComment = mComments.get(0);

        // New Comment
        createNewComment();

        // Edit comment instance
        mNewComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");

        // Create new Reply to that first comment
        mNextEvent = TestEvents.COMMENT_CHANGED;
        RemoteCreateCommentPayload payload = new RemoteCreateCommentPayload(sSite, firstComment, mNewComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check comment has been modified in the DB
        CommentModel comment = mCommentStore.getCommentByLocalId(mNewComment.getId());

        // Using .contains() in the assert below because server might wrap the response in <p>
        Assert.assertTrue(comment.getContent().contains(mNewComment.getContent()));
        Assert.assertNotSame(mNewComment.getRemoteCommentId(), comment.getRemoteCommentId());
        Assert.assertNotSame(mNewComment.getRemoteSiteId(), comment.getRemoteSiteId());
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
        CommentModel firstComment = mComments.get(0);

        RemoteCommentPayload payload = new RemoteCommentPayload(sSite, firstComment);
        mNextEvent = TestEvents.COMMENT_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newFetchCommentAction(payload));
        // Wait for a network response / onChanged event
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testEditValidComment() throws InterruptedException {
        fetchFirstComments();

        // Get first comment
        CommentModel firstComment = mComments.get(0);

        // Edit the comment
        firstComment.setContent("If we could somehow harness this lightning: " + (new Random()).nextFloat() * 10);
        firstComment.setStatus(CommentStatus.APPROVED.toString());

        // Push the edited comment
        RemoteCommentPayload pushCommentPayload = new RemoteCommentPayload(sSite, firstComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newPushCommentAction(pushCommentPayload));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Make sure it was edited
        RemoteCommentPayload payload = new RemoteCommentPayload(sSite, firstComment);
        mNextEvent = TestEvents.COMMENT_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newFetchCommentAction(payload));
        // Wait for a network response / onChanged event
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        CommentModel updatedComment = CommentSqlUtils.getCommentByLocalCommentId(firstComment.getId());
        Assert.assertNotNull(updatedComment);
        Assert.assertTrue(updatedComment.getContent().contains(firstComment.getContent()));
    }

    public void testEditInvalidComment() throws InterruptedException {
        CommentModel comment = new CommentModel();
        comment.setContent("");
        comment.setDatePublished("");
        comment.setStatus("approved");
        // Try to push the invalid comment
        mNextEvent = TestEvents.COMMENT_CHANGED_ERROR;
        RemoteCommentPayload pushCommentPayload = new RemoteCommentPayload(sSite, comment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newPushCommentAction(pushCommentPayload));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onCommentChanged(OnCommentChanged event) {
        List<CommentModel> comments = mCommentStore.getCommentsForSite(sSite, true, CommentStatus.ALL);
        if (event.isError()) {
            AppLog.i(T.TESTS, "event error type: " + event.error.type);
            if (mNextEvent == TestEvents.COMMENT_CHANGED_UNKNOWN_COMMENT) {
                Assert.assertEquals(event.error.type, CommentErrorType.UNKNOWN_COMMENT);
            } else if (mNextEvent == TestEvents.COMMENT_CHANGED_UNKNOWN_POST) {
                Assert.assertEquals(event.error.type, CommentErrorType.UNKNOWN_POST);
            } else if (mNextEvent == TestEvents.COMMENT_CHANGED_DUPLICATE_COMMENT) {
                Assert.assertEquals(event.error.type, CommentErrorType.DUPLICATE_COMMENT);
            } else if (mNextEvent == TestEvents.COMMENT_CHANGED_ERROR) {
                Assert.assertEquals(event.error.type, CommentErrorType.UNKNOWN_COMMENT);
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
        List<PostModel> posts = mPostStore.getPostsForSite(sSite);
        mFirstPost = getFirstPublishedPost(posts);
        Assert.assertEquals(mNextEvent, TestEvents.POSTS_FETCHED);
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
