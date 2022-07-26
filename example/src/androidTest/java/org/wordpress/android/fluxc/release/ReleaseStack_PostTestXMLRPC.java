package org.wordpress.android.fluxc.release;

import org.apache.commons.lang3.RandomStringUtils;
import org.greenrobot.eventbus.Subscribe;
import org.junit.Test;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged;
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged.DeletePost;
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged.UpdatePost;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.FetchPostsPayload;
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged;
import org.wordpress.android.fluxc.store.PostStore.OnPostStatusFetched;
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded;
import org.wordpress.android.fluxc.store.PostStore.PostDeleteActionType;
import org.wordpress.android.fluxc.store.PostStore.PostError;
import org.wordpress.android.fluxc.store.PostStore.PostErrorType;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;
import org.wordpress.android.fluxc.utils.WellSqlUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class ReleaseStack_PostTestXMLRPC extends ReleaseStack_XMLRPCBase {
    @Inject PostStore mPostStore;

    private static final String POST_DEFAULT_TITLE = "PostTestXMLRPC base post";
    private static final String POST_DEFAULT_DESCRIPTION = "Hi there, I'm a post from FluxC!";
    private static final String POST_STATUS_TO_TEST = "private";
    private static final double EXAMPLE_LATITUDE = 44.8378;
    private static final double EXAMPLE_LONGITUDE = -0.5792;

    private enum TestEvents {
        NONE,
        ALL_POST_REMOVED,
        POST_INSTANTIATED,
        POST_UPLOADED,
        POST_UPDATED,
        POSTS_FETCHED,
        POST_STATUS_FETCHED,
        PAGES_FETCHED,
        POST_DELETED,
        POST_RESTORED,
        ERROR_UNKNOWN_POST,
        ERROR_UNKNOWN_POST_TYPE,
        ERROR_UNAUTHORIZED,
        ERROR_GENERIC
    }

    private TestEvents mNextEvent;
    private PostError mLastPostError;
    private PostModel mPost;
    private boolean mCanLoadMorePosts;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);

        // Register and initialize sSite
        init();

        // Reset expected test event
        mNextEvent = TestEvents.NONE;

        mPost = null;
        mCanLoadMorePosts = false;

        mLastPostError = null;
    }

    @Test
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

        // The site should automatically assign the post the default category
        assertFalse(uploadedPost.getCategoryIdList().isEmpty());
    }

    @Test
    public void testUploadNewPostWithGutenbergContent() throws InterruptedException {
        String postContent = "<!-- wp:paragraph -->\n"
                             + "<p>Paragraph 1</p>\n"
                             + "<!-- /wp:paragraph -->";
        // Instantiate new post
        createNewPost();
        mPost.setTitle("Gutenberg content");
        mPost.setContent(postContent);

        // Upload new post to site
        uploadPost(mPost);

        PostModel uploadedPostBeforeFetch = mPostStore.getPostByLocalPostId(mPost.getId());
        // Make sure the uploaded post content is the same as the one we uploaded
        assertEquals(postContent, uploadedPostBeforeFetch.getContent());

        // Fetch the uploaded post from the server
        fetchPost(mPost);

        PostModel uploadedPostAfterFetch = mPostStore.getPostByLocalPostId(mPost.getId());
        // Make sure the uploaded post content is the same as the one we uploaded
        assertEquals(postContent, uploadedPostAfterFetch.getContent());
    }

    @Test
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

    @Test
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

    @Test
    public void testChangeLocalDraft() throws InterruptedException {
        createNewPost();

        // Wait one sec
        Thread.sleep(1000);
        Date testStartDate = new Date();

        // Check local change date is set and before "right now"
        assertNotNull(mPost.getDateLocallyChanged());
        Date postDate1 = DateTimeUtils.dateFromIso8601(mPost.getDateLocallyChanged());
        assertTrue(testStartDate.after(postDate1));

        setupPostAttributes();

        mPost.setTitle("From testChangingLocalDraft");

        // Wait one sec
        Thread.sleep(1000);

        // Save changes locally
        savePost(mPost);

        // Check the locallyChanged date actually changed after the post update
        Date postDate2 = DateTimeUtils.dateFromIso8601(mPost.getDateLocallyChanged());
        assertTrue(postDate2.after(postDate1));

        // Get the current copy of the post from the PostStore
        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        mPost.setTitle("From testChangingLocalDraft, redux");
        mPost.setContent("Some new content");
        mPost.setFeaturedImageId(7);

        // Wait one sec
        Thread.sleep(1000);

        // Save new changes locally
        savePost(mPost);

        // Check the locallyChanged date actually changed after the post update
        Date postDate3 = DateTimeUtils.dateFromIso8601(mPost.getDateLocallyChanged());
        assertTrue(postDate3.after(postDate2));

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

    @Test
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

    @Test
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

        deletePost(finalPost);
    }

    @Test
    public void testFetchPosts() throws InterruptedException {
        mNextEvent = TestEvents.POSTS_FETCHED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newFetchPostsAction(new FetchPostsPayload(sSite, false)));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        int firstFetchPosts = mPostStore.getPostsCountForSite(sSite);

        // Dangerous, will fail for a site with no posts
        assertTrue(firstFetchPosts > 0 && firstFetchPosts <= PostStore.NUM_POSTS_PER_FETCH);
        assertEquals(mCanLoadMorePosts, firstFetchPosts == PostStore.NUM_POSTS_PER_FETCH);

        // Dependent on site having more than NUM_POSTS_TO_REQUEST posts
        assertTrue(mCanLoadMorePosts);

        mNextEvent = TestEvents.POSTS_FETCHED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newFetchPostsAction(new FetchPostsPayload(sSite, true)));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        int currentStoredPosts = mPostStore.getPostsCountForSite(sSite);

        assertTrue(currentStoredPosts > firstFetchPosts);
        assertTrue(currentStoredPosts <= (PostStore.NUM_POSTS_PER_FETCH * 2));
    }

    @Test
    public void testFetchPages() throws InterruptedException {
        mNextEvent = TestEvents.PAGES_FETCHED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newFetchPagesAction(new FetchPostsPayload(sSite, false)));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        int firstFetchPosts = mPostStore.getPagesCountForSite(sSite);

        // Dangerous, will fail for a site with no pages
        assertTrue(firstFetchPosts > 0 && firstFetchPosts <= PostStore.NUM_POSTS_PER_FETCH);
        assertEquals(mCanLoadMorePosts, firstFetchPosts == PostStore.NUM_POSTS_PER_FETCH);
    }

    @Test
    public void testFullFeaturedPostUpload() throws InterruptedException {
        createNewPost();

        mPost.setTitle("A fully featured post");
        mPost.setContent("Some content here! <strong>Bold text</strong>.");
        String date = DateTimeUtils.iso8601UTCFromDate(new Date());
        mPost.setDateCreated(date);

        List<Long> categoryIds = new ArrayList<>(1);
        categoryIds.add((long) 1);
        mPost.setCategoryIdList(categoryIds);

        List<String> tags = new ArrayList<>(2);
        tags.add("fluxc");
        tags.add("generated-" + RandomStringUtils.randomAlphanumeric(8));
        mPost.setTagNameList(tags);

        String knownImageIds = BuildConfig.TEST_WPORG_IMAGE_IDS_TEST1;
        long featuredImageId = Long.valueOf(knownImageIds.split(",")[0]);
        mPost.setFeaturedImageId(featuredImageId);

        uploadPost(mPost);

        // Get the current copy of the post from the PostStore
        PostModel newPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        assertEquals("A fully featured post", newPost.getTitle());
        assertEquals("Some content here! <strong>Bold text</strong>.", newPost.getContent());
        assertEquals(date, newPost.getDateCreated());

        assertTrue(categoryIds.containsAll(newPost.getCategoryIdList())
                   && newPost.getCategoryIdList().containsAll(categoryIds));

        assertTrue(tags.containsAll(newPost.getTagNameList())
                   && newPost.getTagNameList().containsAll(tags));

        assertEquals(featuredImageId, newPost.getFeaturedImageId());
    }

    @Test
    public void testUploadAndEditPage() throws InterruptedException {
        createNewPost();
        mPost.setIsPage(true);
        mPost.setTitle("A fully featured page");
        mPost.setContent("Some content here! <strong>Bold text</strong>.");
        mPost.setDateCreated(DateTimeUtils.iso8601UTCFromDate(new Date()));
        uploadPost(mPost);
        assertEquals(1, mPostStore.getPagesCountForSite(sSite));

        // We should have one page and no post
        assertEquals(1, mPostStore.getPagesCountForSite(sSite));
        assertEquals(0, mPostStore.getPostsCountForSite(sSite));

        // Get the current copy of the page from the PostStore
        PostModel newPage = mPostStore.getPostByLocalPostId(mPost.getId());
        newPage.setTitle("A fully featured page - edited");
        newPage.setIsLocallyChanged(true);

        // Upload edited page
        uploadPost(newPage);

        // We should still have one page and no post
        assertEquals(1, mPostStore.getPagesCountForSite(sSite));
        assertEquals(0, mPostStore.getPostsCountForSite(sSite));
    }

    @Test
    public void testFullFeaturedPageUpload() throws InterruptedException {
        createNewPost();

        mPost.setIsPage(true);

        mPost.setTitle("A fully featured page");
        mPost.setContent("Some content here! <strong>Bold text</strong>.");
        String date = DateTimeUtils.iso8601UTCFromDate(new Date());
        mPost.setDateCreated(date);

        uploadPost(mPost);

        // Get the current copy of the page from the PostStore
        PostModel newPage = mPostStore.getPostByLocalPostId(mPost.getId());
        assertEquals(1, WellSqlUtils.getTotalPostsCount());

        // Clear local data
        removeAllPosts();

        assertEquals(0, WellSqlUtils.getTotalPostsCount());

        // Fetch the page
        fetchPost(newPage);

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPagesCountForSite(sSite));

        assertNotSame(0, newPage.getRemotePostId());

        assertEquals("A fully featured page", newPage.getTitle());
        assertEquals("Some content here! <strong>Bold text</strong>.", newPage.getContent());
        assertEquals(date, newPage.getDateCreated());
    }

    @Test
    public void testClearTagsFromPost() throws InterruptedException {
        createNewPost();

        mPost.setTitle("A post with tags");
        mPost.setContent("Some content here! <strong>Bold text</strong>.");

        List<Long> categoryIds = new ArrayList<>(1);
        categoryIds.add((long) 1);
        mPost.setCategoryIdList(categoryIds);

        List<String> tags = new ArrayList<>(2);
        tags.add("fluxc");
        tags.add("generated-" + RandomStringUtils.randomAlphanumeric(8));
        mPost.setTagNameList(tags);

        uploadPost(mPost);

        // Get the current copy of the post from the PostStore
        PostModel newPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertFalse(newPost.getTagNameList().isEmpty());

        newPost.setTagNameList(Collections.<String>emptyList());
        newPost.setIsLocallyChanged(true);

        // Upload edited post
        uploadPost(newPost);

        PostModel finalPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertTrue(finalPost.getTagNameList().isEmpty());
    }

    @Test
    public void testClearFeaturedImageFromPost() throws InterruptedException {
        createNewPost();

        mPost.setTitle("A post with featured image");

        String knownImageIds = BuildConfig.TEST_WPORG_IMAGE_IDS_TEST1;
        long featuredImageId = Long.valueOf(knownImageIds.split(",")[0]);
        mPost.setFeaturedImageId(featuredImageId);

        uploadPost(mPost);

        // Get the current copy of the post from the PostStore
        PostModel newPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        assertTrue(newPost.hasFeaturedImage());
        assertEquals(featuredImageId, newPost.getFeaturedImageId());

        newPost.clearFeaturedImage();

        uploadPost(newPost);

        // Get the current copy of the post from the PostStore
        PostModel finalPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        assertFalse(finalPost.hasFeaturedImage());
    }

    @Test
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
        assertEquals(EXAMPLE_LATITUDE, mPost.getLocation().getLatitude(), 0.1);
        assertEquals(EXAMPLE_LONGITUDE, mPost.getLocation().getLongitude(), 0.1);
    }

    @Test
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
        assertEquals(EXAMPLE_LATITUDE, mPost.getLocation().getLatitude(), 0.1);
        assertEquals(EXAMPLE_LONGITUDE, mPost.getLocation().getLongitude(), 0.1);

        // 2. Modify the post without changing the location data and update
        mPost.setTitle("A new title");
        mPost.setIsLocallyChanged(true);

        uploadPost(mPost);

        // Get the current copy of the post from the PostStore
        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals("A new title", mPost.getTitle());

        // The location data should not have been altered
        assertTrue(mPost.hasLocation());
        assertEquals(EXAMPLE_LATITUDE, mPost.getLocation().getLatitude(), 0.1);
        assertEquals(EXAMPLE_LONGITUDE, mPost.getLocation().getLongitude(), 0.1);

        // 3. Clear location data from the post and update
        mPost.clearLocation();
        mPost.setIsLocallyChanged(true);

        uploadPost(mPost);

        // Get the current copy of the post from the PostStore
        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        // The post should not have a location anymore
        assertFalse(mPost.hasLocation());
    }

    @Test
    public void testTrashRemotePost() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        uploadPost(mPost);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());
        assertNotNull(uploadedPost);

        deletePost(uploadedPost);

        // The post status should be trashed
        PostModel trashedPost = mPostStore.getPostByLocalPostId(uploadedPost.getId());
        assertNotNull(trashedPost);
        assertEquals(PostStatus.TRASHED, PostStatus.fromPost(trashedPost));
    }

    @Test
    public void testRestoreRemotePost() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        uploadPost(mPost);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());
        assertNotNull(uploadedPost);

        deletePost(uploadedPost);

        // The post status should be trashed
        PostModel trashedPost = mPostStore.getPostByLocalPostId(uploadedPost.getId());
        assertNotNull(trashedPost);
        assertEquals(PostStatus.TRASHED, PostStatus.fromPost(trashedPost));

        // restore post
        restorePost(trashedPost, TestEvents.POST_RESTORED);
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        // retrieve restored post from PostStore and make sure it's not TRASHED anymore
        PostModel restoredPost = mPostStore.getPostByRemotePostId(uploadedPost.getRemotePostId(), sSite);
        assertNotNull(restoredPost);
        assertNotEquals(PostStatus.fromPost(restoredPost), PostStatus.TRASHED);
    }

    @Test
    public void testFetchPostStatus() throws InterruptedException {
        // Instantiate new post
        createNewPost();
        setupPostAttributes();
        mPost.setStatus(POST_STATUS_TO_TEST);

        // Upload new post to site
        uploadPost(mPost);

        // Verify that post is uploaded correctly
        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());
        assertEquals(POST_STATUS_TO_TEST, uploadedPost.getStatus());

        // Fetch post status - The response will be tested in OnPostStatusFetched
        fetchPostStatus(uploadedPost);
    }

    // Error handling tests

    @Test
    public void testFetchInvalidPost() throws InterruptedException {
        PostModel post = new PostModel();
        post.setRemotePostId(6420328);

        mNextEvent = TestEvents.ERROR_GENERIC;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newFetchPostAction(new RemotePostPayload(post, sSite)));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // TODO: This will fail for non-English sites - we should be checking for an UNKNOWN_POST error instead
        // (once we make the fixes needed for PostXMLRPCClient to correctly identify post errors)
        assertEquals("Invalid post ID.", mLastPostError.message);
    }

    @Test
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

        mNextEvent = TestEvents.ERROR_GENERIC;
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

        // TODO: This will fail for non-English sites - we should be checking for an UNKNOWN_POST error instead
        // (once we make the fixes needed for PostXMLRPCClient to correctly identify post errors)
        assertEquals("Invalid post ID.", mLastPostError.message);
    }

    @Test
    public void testDeleteInvalidRemotePost() throws InterruptedException {
        PostModel invalidPost = new PostModel();
        invalidPost.setRemotePostId(6420328);

        mNextEvent = TestEvents.ERROR_GENERIC;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newDeletePostAction(new RemotePostPayload(invalidPost, sSite)));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // TODO: This will fail for non-English sites - we should be checking for an UNKNOWN_POST error instead
        // (once we make the fixes needed for PostXMLRPCClient to correctly identify post errors)
        assertEquals("Invalid post ID.", mLastPostError.message);
    }

    @Test
    public void testRestoreInvalidRemotePost() throws InterruptedException {
        PostModel invalidPost = new PostModel();
        invalidPost.setRemotePostId(6420328);
        restorePost(invalidPost, TestEvents.ERROR_GENERIC);

        // TODO: This will fail for non-English sites - we should be checking for an UNKNOWN_POST error instead
        // (once we make the fixes needed for PostXMLRPCClient to correctly identify post errors)
        assertEquals("Invalid post ID.", mLastPostError.message);
    }

    @Test
    public void testCreateNewPostWithInvalidCategory() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        List<Long> categories = new ArrayList<>();
        categories.add((long) 999999);

        mPost.setCategoryIdList(categories);

        mNextEvent = TestEvents.ERROR_GENERIC;
        mCountDownLatch = new CountDownLatch(1);

        // Upload edited post
        RemotePostPayload pushPayload = new RemotePostPayload(mPost, sSite);
        mDispatcher.dispatch(PostActionBuilder.newPushPostAction(pushPayload));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // TODO: This will fail for non-English sites - we should be checking for an UNKNOWN_TERM error instead
        // (once we make the fixes needed for PostXMLRPCClient to correctly identify post errors)
        assertEquals("Invalid term ID.", mLastPostError.message);
    }

    @Test
    public void testEditPostWithInvalidTerm() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        uploadPost(mPost);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        String dateCreated = uploadedPost.getDateCreated();

        uploadedPost.setTitle("From testEditInvalidPost");
        uploadedPost.setIsLocallyChanged(true);

        savePost(uploadedPost);

        List<Long> categories = new ArrayList<>();
        categories.add((long) 999999);

        uploadedPost.setCategoryIdList(categories);

        mNextEvent = TestEvents.ERROR_GENERIC;
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

        // TODO: This will fail for non-English sites - we should be checking for an UNKNOWN_TERM error instead
        // (once we make the fixes needed for PostXMLRPCClient to correctly identify post errors)
        assertEquals("Invalid term ID.", mLastPostError.message);
    }

    @Test
    public void testCreateNewPostWithInvalidFeaturedImage() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        mPost.setFeaturedImageId(999999);

        mNextEvent = TestEvents.ERROR_GENERIC;
        mCountDownLatch = new CountDownLatch(1);

        // Upload edited post
        RemotePostPayload pushPayload = new RemotePostPayload(mPost, sSite);
        mDispatcher.dispatch(PostActionBuilder.newPushPostAction(pushPayload));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // TODO: This will fail for non-English sites - we should be checking for an UNKNOWN_ATTACHMENT error instead
        // (once we make the fixes needed for PostXMLRPCClient to correctly identify post errors)
        assertEquals("Invalid attachment ID.", mLastPostError.message);
    }

    @Test
    public void testEditPostWithInvalidFeaturedImage() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        uploadPost(mPost);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        String dateCreated = uploadedPost.getDateCreated();

        uploadedPost.setTitle("From testEditInvalidPost");
        uploadedPost.setIsLocallyChanged(true);

        savePost(uploadedPost);

        uploadedPost.setFeaturedImageId(999999);

        mNextEvent = TestEvents.ERROR_GENERIC;
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

        // TODO: This will fail for non-English sites - we should be checking for an UNKNOWN_ATTACHMENT error instead
        // (once we make the fixes needed for PostXMLRPCClient to correctly identify post errors)
        assertEquals("Invalid attachment ID.", mLastPostError.message);
    }

    @Test
    public void testFetchPostBadCredentials() throws InterruptedException {
        PostModel post = new PostModel();
        post.setRemotePostId(10);

        mNextEvent = TestEvents.ERROR_GENERIC;
        mCountDownLatch = new CountDownLatch(1);

        SiteModel badSite = new SiteModel();
        badSite.setId(2);
        badSite.setSelfHostedSiteId(0);
        badSite.setUsername(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE);
        badSite.setPassword("wrong");
        badSite.setXmlRpcUrl(BuildConfig.TEST_WPORG_URL_SH_SIMPLE_ENDPOINT);

        mDispatcher.dispatch(PostActionBuilder.newFetchPostAction(new RemotePostPayload(post, badSite)));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals("Incorrect username or password.", mLastPostError.message);
    }

    @Test
    public void testFetchPostBadUrl() throws InterruptedException {
        PostModel post = new PostModel();
        post.setRemotePostId(10);

        mNextEvent = TestEvents.ERROR_GENERIC;
        mCountDownLatch = new CountDownLatch(1);

        SiteModel badSite = new SiteModel();
        badSite.setId(2);
        badSite.setSelfHostedSiteId(0);
        badSite.setUsername(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE);
        badSite.setPassword("wrong");
        badSite.setXmlRpcUrl("http://www.android.com");

        mDispatcher.dispatch(PostActionBuilder.newFetchPostAction(new RemotePostPayload(post, badSite)));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    // TODO: Test: Upload a page to a custom site that has pages disabled (should get a 403 'Invalid post type')

    @Test
    public void testFetchPostsAsSubscriber() throws InterruptedException {
        SiteModel site = new SiteModel();
        site.setId(2);
        site.setSelfHostedSiteId(0);
        site.setUsername(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE_SUBSCRIBER);
        site.setPassword(BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE_SUBSCRIBER);
        site.setXmlRpcUrl(BuildConfig.TEST_WPORG_URL_SH_SIMPLE_SUBSCRIBER_ENDPOINT);

        // Expecting a 401 error (authorization required)
        mNextEvent = TestEvents.ERROR_UNAUTHORIZED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newFetchPostsAction(new FetchPostsPayload(site)));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testCreatePostAsSubscriber() throws InterruptedException {
        SiteModel subscriberSite = new SiteModel();
        subscriberSite.setId(2);
        subscriberSite.setSelfHostedSiteId(0);
        subscriberSite.setUsername(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE_SUBSCRIBER);
        subscriberSite.setPassword(BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE_SUBSCRIBER);
        subscriberSite.setXmlRpcUrl(BuildConfig.TEST_WPORG_URL_SH_SIMPLE_SUBSCRIBER_ENDPOINT);

        // Instantiate new post
        mNextEvent = TestEvents.POST_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);

        createNewPost(subscriberSite);
        setupPostAttributes();

        // Attempt to upload new post to site
        mNextEvent = TestEvents.ERROR_UNAUTHORIZED;
        mCountDownLatch = new CountDownLatch(1);

        RemotePostPayload pushPayload = new RemotePostPayload(mPost, subscriberSite);
        mDispatcher.dispatch(PostActionBuilder.newPushPostAction(pushPayload));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals("Sorry, you are not allowed to post on this site.", mLastPostError.message);

        PostModel failedUploadPost = mPostStore.getPostByLocalPostId(mPost.getId());

        // Post should still exist locally, but not marked as uploaded
        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(subscriberSite));
        assertEquals(0, mPostStore.getUploadedPostsCountForSite(subscriberSite));

        assertEquals(0, failedUploadPost.getRemotePostId());
        assertTrue(failedUploadPost.isLocalDraft());
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onPostChanged(OnPostChanged event) {
        AppLog.i(T.API, "Received OnPostChanged, cause: " + event.causeOfChange);
        if (event.isError()) {
            AppLog.i(T.API, "OnPostChanged has error: " + event.error.type + " - " + event.error.message);
            mLastPostError = event.error;
            if (mNextEvent.equals(TestEvents.ERROR_UNKNOWN_POST)) {
                assertEquals(PostErrorType.UNKNOWN_POST, event.error.type);
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TestEvents.ERROR_UNKNOWN_POST_TYPE)) {
                assertEquals(PostErrorType.UNKNOWN_POST_TYPE, event.error.type);
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TestEvents.ERROR_UNAUTHORIZED)) {
                assertEquals(PostErrorType.UNAUTHORIZED, event.error.type);
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TestEvents.ERROR_GENERIC)) {
                assertEquals(PostErrorType.GENERIC_ERROR, event.error.type);
                mCountDownLatch.countDown();
            } else {
                throw new AssertionError("Unexpected error with type: " + event.error.type);
            }
            return;
        }
        if (event.causeOfChange instanceof CauseOfOnPostChanged.UpdatePost) {
            if (mNextEvent.equals(TestEvents.POST_UPDATED)) {
                UpdatePost causeOfChange = ((UpdatePost) event.causeOfChange);
                assertTrue(causeOfChange.getLocalPostId() > 0 || causeOfChange.getRemotePostId() > 0);
                mCountDownLatch.countDown();
            }
        } else if (event.causeOfChange instanceof CauseOfOnPostChanged.FetchPosts) {
            if (mNextEvent.equals(TestEvents.POSTS_FETCHED)) {
                AppLog.i(T.API, "Fetched " + event.rowsAffected + " posts, can load more: " + event.canLoadMore);
                mCanLoadMorePosts = event.canLoadMore;
                mCountDownLatch.countDown();
            }
        } else if (event.causeOfChange instanceof CauseOfOnPostChanged.FetchPages) {
            if (mNextEvent.equals(TestEvents.PAGES_FETCHED)) {
                AppLog.i(T.API, "Fetched " + event.rowsAffected + " pages, can load more: " + event.canLoadMore);
                mCanLoadMorePosts = event.canLoadMore;
                mCountDownLatch.countDown();
            }
        } else if (event.causeOfChange instanceof CauseOfOnPostChanged.DeletePost) {
            if (mNextEvent.equals(TestEvents.POST_DELETED)) {
                assertNotEquals(0, ((DeletePost) event.causeOfChange).getLocalPostId());
                assertNotEquals(0, ((DeletePost) event.causeOfChange).getRemotePostId());
                if (((DeletePost) event.causeOfChange).getPostDeleteActionType() == PostDeleteActionType.TRASH) {
                    // When a post is trashed, we fetch the updated post from remote and we need to wait for its result
                    mNextEvent = TestEvents.POST_UPDATED;
                } else {
                    mCountDownLatch.countDown();
                }
            }
        } else if (event.causeOfChange instanceof CauseOfOnPostChanged.RemoveAllPosts) {
            if (mNextEvent.equals(TestEvents.ALL_POST_REMOVED)) {
                mCountDownLatch.countDown();
            }
        } else if (event.causeOfChange instanceof CauseOfOnPostChanged.RestorePost) {
            if (mNextEvent.equals(TestEvents.POST_RESTORED)) {
                mCountDownLatch.countDown();
            }
        } else {
            throw new AssertionError("Unexpected cause of change: " + event.causeOfChange.getClass().getSimpleName());
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onPostUploaded(OnPostUploaded event) {
        AppLog.i(T.API, "Received OnPostUploaded");
        if (event.isError()) {
            AppLog.i(T.API, "OnPostUploaded has error: " + event.error.type + " - " + event.error.message);
            mLastPostError = event.error;
            if (mNextEvent.equals(TestEvents.ERROR_UNKNOWN_POST)) {
                assertEquals(PostErrorType.UNKNOWN_POST, event.error.type);
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TestEvents.ERROR_UNKNOWN_POST_TYPE)) {
                assertEquals(PostErrorType.UNKNOWN_POST_TYPE, event.error.type);
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TestEvents.ERROR_UNAUTHORIZED)) {
                assertEquals(PostErrorType.UNAUTHORIZED, event.error.type);
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TestEvents.ERROR_GENERIC)) {
                assertEquals(PostErrorType.GENERIC_ERROR, event.error.type);
                mCountDownLatch.countDown();
            } else {
                throw new AssertionError("Unexpected error with type: " + event.error.type);
            }
            return;
        }
        assertEquals(TestEvents.POST_UPLOADED, mNextEvent);
        assertFalse(event.post.isLocalDraft());
        assertFalse(event.post.isLocallyChanged());
        assertNotSame(0, event.post.getRemotePostId());

        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onPostStatusFetched(OnPostStatusFetched event) {
        assertFalse(event.isError());
        assertEquals(TestEvents.POST_STATUS_FETCHED, mNextEvent);
        assertEquals(event.remotePostStatus, POST_STATUS_TO_TEST);
        mCountDownLatch.countDown();
    }

    private void setupPostAttributes() {
        mPost.setTitle(POST_DEFAULT_TITLE);
        mPost.setContent(POST_DEFAULT_DESCRIPTION);
    }

    private PostModel createNewPost() throws InterruptedException {
        return createNewPost(null);
    }

    private PostModel createNewPost(SiteModel site) throws InterruptedException {
        if (site == null) {
            site = sSite;
        }
        PostModel post = mPostStore.instantiatePostModel(site, false);

        assertTrue(post.isLocalDraft());
        assertEquals(0, post.getRemotePostId());
        assertNotSame(0, post.getId());
        assertNotSame(0, post.getLocalSiteId());

        mPost = post;
        return post;
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

    private void deletePost(PostModel post) throws InterruptedException {
        mNextEvent = TestEvents.POST_DELETED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newDeletePostAction(new RemotePostPayload(post, sSite)));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void removeAllPosts() throws InterruptedException {
        mNextEvent = TestEvents.ALL_POST_REMOVED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newRemoveAllPostsAction());

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void restorePost(PostModel post, TestEvents nextEvent) throws InterruptedException {
        mNextEvent = nextEvent;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newRestorePostAction(new RemotePostPayload(post, sSite)));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchPostStatus(PostModel post) throws InterruptedException {
        mNextEvent = TestEvents.POST_STATUS_FETCHED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(PostActionBuilder.newFetchPostStatusAction(new RemotePostPayload(post, sSite)));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
