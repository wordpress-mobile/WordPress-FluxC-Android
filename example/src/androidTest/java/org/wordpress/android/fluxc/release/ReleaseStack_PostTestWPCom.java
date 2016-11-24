package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.InstantiatePostPayload;
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged;
import org.wordpress.android.fluxc.store.PostStore.OnPostInstantiated;
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded;
import org.wordpress.android.fluxc.store.PostStore.PostErrorType;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;
import org.wordpress.android.fluxc.utils.WellSqlUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_PostTestWPCom extends ReleaseStack_WPComBase {
    @Inject PostStore mPostStore;

    private static final String POST_DEFAULT_TITLE = "PostTestWPCom base post";
    private static final String POST_DEFAULT_DESCRIPTION = "Hi there, I'm a post from FluxC!";
    private static final double EXAMPLE_LATITUDE = 44.8378;
    private static final double EXAMPLE_LONGITUDE = -0.5792;

    private enum TestEvents {
        NONE,
        POST_INSTANTIATED,
        POST_UPLOADED,
        POST_UPDATED,
        POSTS_FETCHED,
        PAGES_FETCHED,
        POST_DELETED,
        ERROR_UNKNOWN_POST,
        ERROR_UNKNOWN_POST_TYPE,
        ERROR_GENERIC
    }

    private TestEvents mNextEvent;
    private PostModel mPost;
    private boolean mCanLoadMorePosts;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);

        // Authenticate, fetch sites and initialize sSite
        init();
        // Reset expected test event
        mNextEvent = TestEvents.NONE;

        mPost = null;
        mCanLoadMorePosts = false;
    }

    public void testUploadNewPost() throws InterruptedException {
        // Instantiate new post
        createNewPost();
        setupPostAttributes();

        // Upload new post to site
        uploadPost(mPost);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        assertNotSame(0, uploadedPost.getRemotePostId());
        assertFalse(uploadedPost.isLocalDraft());
    }

    public void testEditRemotePost() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        uploadPost(mPost);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        final String dateCreated = uploadedPost.getDateCreated();

        uploadedPost.setTitle("From testEditingRemotePost");
        uploadedPost.setIsLocallyChanged(true);

        // Upload edited post
        uploadPost(uploadedPost);

        PostModel finalPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        assertEquals("From testEditingRemotePost", finalPost.getTitle());

        // The post should no longer be flagged as having local changes
        assertFalse(finalPost.isLocallyChanged());

        // The date created should not have been altered by the edits
        assertFalse(finalPost.getDateCreated().isEmpty());
        assertEquals(dateCreated, finalPost.getDateCreated());
    }

    public void testRevertLocallyChangedPost() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        uploadPost(mPost);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        uploadedPost.setTitle("From testRevertingLocallyChangedPost");
        uploadedPost.setIsLocallyChanged(true);

        // Revert changes to post by replacing it with a fresh copy from the server
        fetchPost(uploadedPost);

        // Get the current copy of the post from the PostStore
        PostModel latestPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        assertEquals(POST_DEFAULT_TITLE, latestPost.getTitle());
        assertFalse(latestPost.isLocallyChanged());
    }

    public void testChangeLocalDraft() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        mPost.setTitle("From testChangingLocalDraft");

        // Save changes locally
        savePost(mPost);

        // Get the current copy of the post from the PostStore
        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        mPost.setTitle("From testChangingLocalDraft, redux");
        mPost.setContent("Some new content");
        mPost.setFeaturedImageId(7);

        // Save new changes locally
        savePost(mPost);

        // Get the current copy of the post from the PostStore
        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        assertEquals("From testChangingLocalDraft, redux", mPost.getTitle());
        assertEquals("Some new content", mPost.getContent());
        assertEquals(7, mPost.getFeaturedImageId());
        assertFalse(mPost.isLocallyChanged());
        assertTrue(mPost.isLocalDraft());
    }

    public void testMultipleLocalChangesToUploadedPost() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        uploadPost(mPost);

        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        mPost.setTitle("From testMultipleLocalChangesToUploadedPost");
        mPost.setIsLocallyChanged(true);

        // Save changes locally
        savePost(mPost);

        // Get the current copy of the post from the PostStore
        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        mPost.setTitle("From testMultipleLocalChangesToUploadedPost, redux");
        mPost.setContent("Some different content");
        mPost.setFeaturedImageId(5);

        // Save new changes locally
        savePost(mPost);

        // Get the current copy of the post from the PostStore
        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        assertEquals("From testMultipleLocalChangesToUploadedPost, redux", mPost.getTitle());
        assertEquals("Some different content", mPost.getContent());
        assertEquals(5, mPost.getFeaturedImageId());
        assertTrue(mPost.isLocallyChanged());
        assertFalse(mPost.isLocalDraft());
    }

    public void testChangePublishedPostToScheduled() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        uploadPost(mPost);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        String futureDate = "2075-10-14T10:51:11+00:00";
        uploadedPost.setDateCreated(futureDate);
        uploadedPost.setIsLocallyChanged(true);

        // Upload edited post
        uploadPost(uploadedPost);

        PostModel finalPost = mPostStore.getPostByLocalPostId(mPost.getId());

        // The post should no longer be flagged as having local changes
        assertFalse(finalPost.isLocallyChanged());

        // The post should now have a future created date and should have 'future' status
        assertEquals(futureDate, finalPost.getDateCreated());
        assertEquals(PostStatus.SCHEDULED, PostStatus.fromPost(finalPost));
    }

    public void testFetchPosts() throws InterruptedException {
        mNextEvent = TestEvents.POSTS_FETCHED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newFetchPostsAction(new PostStore.FetchPostsPayload(sSite, false)));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        int firstFetchPosts = mPostStore.getPostsCountForSite(sSite);

        // Dangerous, will fail for a site with no posts
        assertTrue(firstFetchPosts > 0 && firstFetchPosts <= PostStore.NUM_POSTS_PER_FETCH);
        assertEquals(mCanLoadMorePosts, firstFetchPosts == PostStore.NUM_POSTS_PER_FETCH);

        // Dependent on site having more than NUM_POSTS_TO_REQUEST posts
        assertTrue(mCanLoadMorePosts);

        mNextEvent = TestEvents.POSTS_FETCHED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newFetchPostsAction(new PostStore.FetchPostsPayload(sSite, true)));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        int currentStoredPosts = mPostStore.getPostsCountForSite(sSite);

        assertTrue(currentStoredPosts > firstFetchPosts);
        assertTrue(currentStoredPosts <= (PostStore.NUM_POSTS_PER_FETCH * 2));
    }

    public void testFetchPages() throws InterruptedException {
        mNextEvent = TestEvents.PAGES_FETCHED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newFetchPagesAction(new PostStore.FetchPostsPayload(sSite, false)));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        int firstFetchPosts = mPostStore.getPagesCountForSite(sSite);

        // Dangerous, will fail for a site with no pages
        assertTrue(firstFetchPosts > 0 && firstFetchPosts <= PostStore.NUM_POSTS_PER_FETCH);
        assertEquals(mCanLoadMorePosts, firstFetchPosts == PostStore.NUM_POSTS_PER_FETCH);
    }

    public void testFullFeaturedPostUpload() throws InterruptedException {
        createNewPost();

        mPost.setTitle("A fully featured post");
        mPost.setContent("Some content here! <strong>Bold text</strong>.\r\n\r\nA new paragraph.");
        String date = DateTimeUtils.iso8601UTCFromDate(new Date());
        mPost.setDateCreated(date);

        List<Long> categoryIds = new ArrayList<>(1);
        categoryIds.add((long) 1);
        mPost.setCategoryIdList(categoryIds);

        uploadPost(mPost);

        // Get the current copy of the post from the PostStore
        PostModel newPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        assertEquals("A fully featured post", newPost.getTitle());
        assertEquals("Some content here! <strong>Bold text</strong>.\r\n\r\nA new paragraph.", newPost.getContent());
        assertEquals(date, newPost.getDateCreated());

        assertTrue(categoryIds.containsAll(newPost.getCategoryIdList())
                   && newPost.getCategoryIdList().containsAll(categoryIds));
    }

    public void testFullFeaturedPageUpload() throws InterruptedException {
        createNewPost();

        mPost.setIsPage(true);

        mPost.setTitle("A fully featured page");
        mPost.setContent("Some content here! <strong>Bold text</strong>.\r\n\r\nA new paragraph.");
        String date = DateTimeUtils.iso8601UTCFromDate(new Date());
        mPost.setDateCreated(date);

        mPost.setFeaturedImageId(77); // Not actually valid for pages

        uploadPost(mPost);

        // Get the current copy of the page from the PostStore
        PostModel newPage = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPagesCountForSite(sSite));

        assertNotSame(0, newPage.getRemotePostId());

        assertEquals("A fully featured page", newPage.getTitle());
        assertEquals("Some content here! <strong>Bold text</strong>.\r\n\r\nA new paragraph.", newPage.getContent());
        assertEquals(date, newPage.getDateCreated());

        assertEquals(0, newPage.getFeaturedImageId()); // The page should upload, but have the featured image stripped
    }

    public void testAddLocationToRemotePost() throws InterruptedException {
        // 1. Upload a post with no location data
        createNewPost();

        mPost.setTitle("A post with location");
        mPost.setContent("Some content");

        uploadPost(mPost);

        // Get the current copy of the post from the PostStore
        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        assertEquals("A post with location", mPost.getTitle());
        assertEquals("Some content", mPost.getContent());

        // The post should not have a location since we never set one
        assertFalse(mPost.hasLocation());

        // 2. Modify the post, setting some location data
        mPost.setLocation(EXAMPLE_LATITUDE, EXAMPLE_LONGITUDE);
        mPost.setIsLocallyChanged(true);

        uploadPost(mPost);

        // Get the current copy of the post from the PostStore
        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        // The set location should be stored in the remote post
        assertTrue(mPost.hasLocation());
        assertEquals(EXAMPLE_LATITUDE, mPost.getLocation().getLatitude());
        assertEquals(EXAMPLE_LONGITUDE, mPost.getLocation().getLongitude());
    }

    public void testUploadPostWithLocation() throws InterruptedException {
        // 1. Upload a post with location data
        createNewPost();

        mPost.setTitle("A post with location");
        mPost.setContent("Some content");

        mPost.setLocation(EXAMPLE_LATITUDE, EXAMPLE_LONGITUDE);

        uploadPost(mPost);

        // Get the current copy of the post from the PostStore
        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        assertEquals("A post with location", mPost.getTitle());
        assertEquals("Some content", mPost.getContent());

        // The set location should be stored in the remote post
        assertTrue(mPost.hasLocation());
        assertEquals(EXAMPLE_LATITUDE, mPost.getLocation().getLatitude());
        assertEquals(EXAMPLE_LONGITUDE, mPost.getLocation().getLongitude());

        // 2. Modify the post without changing the location data and update
        mPost.setTitle("A new title");
        mPost.setIsLocallyChanged(true);

        uploadPost(mPost);

        // Get the current copy of the post from the PostStore
        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals("A new title", mPost.getTitle());

        // The location data should not have been altered
        assertTrue(mPost.hasLocation());
        assertEquals(EXAMPLE_LATITUDE, mPost.getLocation().getLatitude());
        assertEquals(EXAMPLE_LONGITUDE, mPost.getLocation().getLongitude());

        // 3. Clear location data from the post and update
        mPost.clearLocation();
        mPost.setIsLocallyChanged(true);

        uploadPost(mPost);

        // Get the current copy of the post from the PostStore
        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        // The post should not have a location anymore
        assertFalse(mPost.hasLocation());
    }

    public void testDeleteRemotePost() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        uploadPost(mPost);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        mNextEvent = TestEvents.POST_DELETED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newDeletePostAction(new RemotePostPayload(uploadedPost, sSite)));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // The post should be removed from the db (regardless of whether it was deleted or just trashed on the server)
        assertEquals(null, mPostStore.getPostByLocalPostId(uploadedPost.getId()));
        assertEquals(0, WellSqlUtils.getTotalPostsCount());
        assertEquals(0, mPostStore.getPostsCountForSite(sSite));
    }

    // Error handling tests

    public void testFetchInvalidPost() throws InterruptedException {
        PostModel post = new PostModel();
        post.setRemotePostId(6420328);
        post.setRemoteSiteId(sSite.getSiteId());

        mNextEvent = TestEvents.ERROR_UNKNOWN_POST;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newFetchPostAction(new RemotePostPayload(post, sSite)));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testEditInvalidPost() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        uploadPost(mPost);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        String dateCreated = uploadedPost.getDateCreated();

        uploadedPost.setTitle("From testEditInvalidPost");
        uploadedPost.setIsLocallyChanged(true);

        savePost(uploadedPost);

        uploadedPost.setRemotePostId(289385);

        mNextEvent = TestEvents.ERROR_UNKNOWN_POST;
        mCountDownLatch = new CountDownLatch(1);

        // Upload edited post
        RemotePostPayload pushPayload = new RemotePostPayload(uploadedPost, sSite);
        mDispatcher.dispatch(PostActionBuilder.newPushPostAction(pushPayload));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        PostModel persistedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        // The locally saved post should still be marked as locally changed, and local changes should be preserved
        assertEquals("From testEditInvalidPost", persistedPost.getTitle());
        assertTrue(persistedPost.isLocallyChanged());

        // The date created should not have been altered by the edit
        assertFalse(persistedPost.getDateCreated().isEmpty());
        assertEquals(dateCreated, persistedPost.getDateCreated());
    }

    public void testDeleteInvalidRemotePost() throws InterruptedException {
        PostModel invalidPost = new PostModel();
        invalidPost.setRemotePostId(6420328);
        invalidPost.setRemoteSiteId(sSite.getSiteId());

        mNextEvent = TestEvents.ERROR_UNKNOWN_POST;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newDeletePostAction(new RemotePostPayload(invalidPost, sSite)));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testFetchPostFromInvalidSite() throws InterruptedException {
        PostModel post = new PostModel();
        post.setRemotePostId(6420328);
        post.setRemoteSiteId(99999999999L);

        SiteModel site = new SiteModel();
        site.setIsWPCom(true);
        site.setSiteId(99999999999L);

        // Expecting a generic 404 error (unknown site)
        mNextEvent = TestEvents.ERROR_GENERIC;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newFetchPostAction(new RemotePostPayload(post, site)));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onPostChanged(OnPostChanged event) {
        AppLog.i(T.API, "Received OnPostChanged, cause: " + event.causeOfChange);
        if (event.isError()) {
            AppLog.i(T.API, "OnPostChanged has error: " + event.error.type + " - " + event.error.message);
            if (mNextEvent.equals(TestEvents.ERROR_UNKNOWN_POST)) {
                assertEquals(PostErrorType.UNKNOWN_POST, event.error.type);
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TestEvents.ERROR_UNKNOWN_POST_TYPE)) {
                assertEquals(PostErrorType.UNKNOWN_POST_TYPE, event.error.type);
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TestEvents.ERROR_GENERIC)) {
                assertEquals(PostErrorType.GENERIC_ERROR, event.error.type);
                mCountDownLatch.countDown();
            } else {
                throw new AssertionError("Unexpected error with type: " + event.error.type);
            }
            return;
        }
        switch (event.causeOfChange) {
            case UPDATE_POST:
                if (mNextEvent.equals(TestEvents.POST_UPDATED)) {
                    mCountDownLatch.countDown();
                }
                break;
            case FETCH_POSTS:
                if (mNextEvent.equals(TestEvents.POSTS_FETCHED)) {
                    AppLog.i(T.API, "Fetched " + event.rowsAffected + " posts, can load more: " + event.canLoadMore);
                    mCanLoadMorePosts = event.canLoadMore;
                    mCountDownLatch.countDown();
                }
                break;
            case FETCH_PAGES:
                if (mNextEvent.equals(TestEvents.PAGES_FETCHED)) {
                    AppLog.i(T.API, "Fetched " + event.rowsAffected + " pages, can load more: " + event.canLoadMore);
                    mCanLoadMorePosts = event.canLoadMore;
                    mCountDownLatch.countDown();
                }
                break;
            case DELETE_POST:
                if (mNextEvent.equals(TestEvents.POST_DELETED)) {
                    mCountDownLatch.countDown();
                }
                break;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onPostInstantiated(OnPostInstantiated event) {
        AppLog.i(T.API, "Received OnPostInstantiated");
        if (event.isError()) {
            throw new AssertionError("Unexpected error with type: " + event.error.type);
        }
        assertEquals(TestEvents.POST_INSTANTIATED, mNextEvent);

        assertTrue(event.post.isLocalDraft());
        assertEquals(0, event.post.getRemotePostId());
        assertNotSame(0, event.post.getId());
        assertNotSame(0, event.post.getLocalSiteId());

        mPost = event.post;
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onPostUploaded(OnPostUploaded event) {
        AppLog.i(T.API, "Received OnPostUploaded");
        if (event.isError()) {
            AppLog.i(T.API, "OnPostUploaded has error: " + event.error.type + " - " + event.error.message);
            if (mNextEvent.equals(TestEvents.ERROR_UNKNOWN_POST)) {
                assertEquals(PostErrorType.UNKNOWN_POST, event.error.type);
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TestEvents.ERROR_UNKNOWN_POST_TYPE)) {
                assertEquals(PostErrorType.UNKNOWN_POST_TYPE, event.error.type);
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TestEvents.ERROR_GENERIC)) {
                assertEquals(PostErrorType.GENERIC_ERROR, event.error.type);
                mCountDownLatch.countDown();
            } else {
                throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
            }
            return;
        }
        assertEquals(TestEvents.POST_UPLOADED, mNextEvent);
        assertFalse(event.post.isLocalDraft());
        assertFalse(event.post.isLocallyChanged());
        assertNotSame(0, event.post.getRemotePostId());

        mCountDownLatch.countDown();
    }

    private void setupPostAttributes() {
        mPost.setTitle(POST_DEFAULT_TITLE);
        mPost.setContent(POST_DEFAULT_DESCRIPTION);
    }

    private void createNewPost() throws InterruptedException {
        // Instantiate new post
        mNextEvent = TestEvents.POST_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);

        InstantiatePostPayload initPayload = new InstantiatePostPayload(sSite, false);
        mDispatcher.dispatch(PostActionBuilder.newInstantiatePostAction(initPayload));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void uploadPost(PostModel post) throws InterruptedException {
        mNextEvent = TestEvents.POST_UPLOADED;
        mCountDownLatch = new CountDownLatch(1);

        RemotePostPayload pushPayload = new RemotePostPayload(post, sSite);
        mDispatcher.dispatch(PostActionBuilder.newPushPostAction(pushPayload));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchPost(PostModel post) throws InterruptedException {
        mNextEvent = TestEvents.POST_UPDATED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newFetchPostAction(new RemotePostPayload(post, sSite)));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void savePost(PostModel post) throws InterruptedException {
        mNextEvent = TestEvents.POST_UPDATED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(post));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
