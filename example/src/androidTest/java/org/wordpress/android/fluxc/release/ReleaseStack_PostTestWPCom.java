package org.wordpress.android.fluxc.release;

import org.apache.commons.lang3.RandomStringUtils;
import org.greenrobot.eventbus.Subscribe;
import org.junit.Test;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged;
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged.DeletePost;
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged.RemovePost;
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged.UpdatePost;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.persistence.PostSqlUtils;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.FetchPostsPayload;
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged;
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded;
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

public class ReleaseStack_PostTestWPCom extends ReleaseStack_WPComBase {
    @Inject PostStore mPostStore;
    @Inject PostSqlUtils mPostSqlUtils;

    private static final String POST_DEFAULT_TITLE = "PostTestWPCom base post";
    private static final String POST_DEFAULT_DESCRIPTION = "Hi there, I'm a post from FluxC!";
    private static final double EXAMPLE_LATITUDE = 44.8378;
    private static final double EXAMPLE_LONGITUDE = -0.5792;

    private enum TestEvents {
        NONE,
        ALL_POST_REMOVED,
        POST_UPLOADED,
        POST_UPDATED,
        POSTS_FETCHED,
        PAGES_FETCHED,
        POST_DELETED,
        POST_REMOVED,
        POST_RESTORED,
        POST_AUTO_SAVED,
        ERROR_UNKNOWN_POST,
        ERROR_UNKNOWN_POST_TYPE,
        ERROR_UNSUPPORTED_ACTION,
        ERROR_GENERIC
    }

    private TestEvents mNextEvent;
    private boolean mCanLoadMorePosts;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);

        // Authenticate, fetch sites and initialize sSite
        init();
        // Reset expected test event
        mNextEvent = TestEvents.NONE;

        mCanLoadMorePosts = false;
    }

    // Note: This test is not specific to WPCOM (local changes only)
    @Test
    public void testRemoveLocalDraft() throws InterruptedException {
        PostModel post = createNewPost();
        setupPostAttributes(post);

        mNextEvent = TestEvents.POST_REMOVED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(PostActionBuilder.newRemovePostAction(post));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals(0, mPostSqlUtils.getPostsForSite(sSite, false).size());
    }

    @Test
    public void testUploadNewPost() throws InterruptedException {
        // Instantiate new post
        PostModel post = createNewPost();
        setupPostAttributes(post);

        // Upload new post to site
        uploadPost(post);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(post.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        assertNotSame(0, uploadedPost.getRemotePostId());
        assertFalse(uploadedPost.isLocalDraft());

        // The site should automatically assign the post the default category
        assertFalse(uploadedPost.getCategoryIdList().isEmpty());
    }

    @Test
    public void testEditRemotePost() throws InterruptedException {
        PostModel post = createNewPost();
        setupPostAttributes(post);

        uploadPost(post);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(post.getId());

        final String dateCreated = uploadedPost.getDateCreated();

        uploadedPost.setTitle("From testEditingRemotePost");
        uploadedPost.setIsLocallyChanged(true);

        // Upload edited post
        uploadPost(uploadedPost);

        PostModel finalPost = mPostStore.getPostByLocalPostId(post.getId());

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
        PostModel post = createNewPost();
        setupPostAttributes(post);

        uploadPost(post);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(post.getId());

        uploadedPost.setTitle("From testRevertingLocallyChangedPost");
        uploadedPost.setIsLocallyChanged(true);

        // Revert changes to post by replacing it with a fresh copy from the server
        fetchPost(uploadedPost);

        // Get the current copy of the post from the PostStore
        PostModel latestPost = mPostStore.getPostByLocalPostId(post.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        assertEquals(POST_DEFAULT_TITLE, latestPost.getTitle());
        assertFalse(latestPost.isLocallyChanged());
    }

    @Test
    public void testChangeLocalDraft() throws InterruptedException {
        PostModel post = createNewPost();

        // Wait one sec
        Thread.sleep(1000);
        Date testStartDate = new Date();

        // Check local change date is set and before "right now"
        assertNotNull(post.getDateLocallyChanged());
        Date postDate1 = DateTimeUtils.dateFromIso8601(post.getDateLocallyChanged());
        assertTrue(testStartDate.after(postDate1));

        setupPostAttributes(post);

        post.setTitle("From testChangingLocalDraft");

        // Wait one sec
        Thread.sleep(1000);

        // Save changes locally
        savePost(post);

        // Check the locallyChanged date actually changed after the post update
        Date postDate2 = DateTimeUtils.dateFromIso8601(post.getDateLocallyChanged());
        assertTrue(postDate2.after(postDate1));

        // Get the current copy of the post from the PostStore
        post = mPostStore.getPostByLocalPostId(post.getId());

        post.setTitle("From testChangingLocalDraft, redux");
        post.setContent("Some new content");
        post.setFeaturedImageId(7);

        // Wait one sec
        Thread.sleep(1000);

        // Save new changes locally
        savePost(post);

        // Check the locallyChanged date actually changed after the post update
        Date postDate3 = DateTimeUtils.dateFromIso8601(post.getDateLocallyChanged());
        assertTrue(postDate3.after(postDate2));

        // Get the current copy of the post from the PostStore
        post = mPostStore.getPostByLocalPostId(post.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        assertEquals("From testChangingLocalDraft, redux", post.getTitle());
        assertEquals("Some new content", post.getContent());
        assertEquals(7, post.getFeaturedImageId());
        assertFalse(post.isLocallyChanged());
        assertTrue(post.isLocalDraft());
    }

    @Test
    public void testMultipleLocalChangesToUploadedPost() throws InterruptedException {
        PostModel post = createNewPost();
        setupPostAttributes(post);

        uploadPost(post);

        post = mPostStore.getPostByLocalPostId(post.getId());

        post.setTitle("From testMultipleLocalChangesToUploadedPost");
        post.setIsLocallyChanged(true);

        // Save changes locally
        savePost(post);

        // Get the current copy of the post from the PostStore
        post = mPostStore.getPostByLocalPostId(post.getId());

        post.setTitle("From testMultipleLocalChangesToUploadedPost, redux");
        post.setContent("Some different content");
        post.setFeaturedImageId(5);

        // Save new changes locally
        savePost(post);

        // Get the current copy of the post from the PostStore
        post = mPostStore.getPostByLocalPostId(post.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        assertEquals("From testMultipleLocalChangesToUploadedPost, redux", post.getTitle());
        assertEquals("Some different content", post.getContent());
        assertEquals(5, post.getFeaturedImageId());
        assertTrue(post.isLocallyChanged());
        assertFalse(post.isLocalDraft());
    }

    @Test
    public void testChangePublishedPostToScheduled() throws InterruptedException {
        PostModel post = createNewPost();
        setupPostAttributes(post);

        uploadPost(post);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(post.getId());

        String futureDate = "2075-10-14T10:51:11+00:00";
        uploadedPost.setDateCreated(futureDate);
        uploadedPost.setIsLocallyChanged(true);

        // Upload edited post
        uploadPost(uploadedPost);

        PostModel finalPost = mPostStore.getPostByLocalPostId(post.getId());

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
        PostModel post = createNewPost();

        post.setTitle("A fully featured post");
        post.setContent("Some content here! <strong>Bold text</strong>.\r\n\r\nA new paragraph.");
        String date = DateTimeUtils.iso8601UTCFromDate(new Date());
        post.setDateCreated(date);

        List<Long> categoryIds = new ArrayList<>(1);
        categoryIds.add((long) 1);
        post.setCategoryIdList(categoryIds);

        List<String> tags = new ArrayList<>(2);
        tags.add("fluxc");
        tags.add("generated-" + RandomStringUtils.randomAlphanumeric(8));
        tags.add(RandomStringUtils.randomNumeric(8));
        post.setTagNameList(tags);

        String knownImageIds = BuildConfig.TEST_WPCOM_IMAGE_IDS_TEST1;
        long featuredImageId = Long.valueOf(knownImageIds.split(",")[0]);
        post.setFeaturedImageId(featuredImageId);

        uploadPost(post);

        // Get the current copy of the post from the PostStore
        PostModel newPost = mPostStore.getPostByLocalPostId(post.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        assertEquals("A fully featured post", newPost.getTitle());
        assertEquals("Some content here! <strong>Bold text</strong>.\r\n\r\nA new paragraph.", newPost.getContent());
        assertEquals(date, newPost.getDateCreated());

        assertTrue(categoryIds.containsAll(newPost.getCategoryIdList())
                   && newPost.getCategoryIdList().containsAll(categoryIds));

        assertTrue(tags.containsAll(newPost.getTagNameList())
                   && newPost.getTagNameList().containsAll(tags));

        assertEquals(featuredImageId, newPost.getFeaturedImageId());
    }

    @Test
    public void testUploadAndEditPage() throws InterruptedException {
        PostModel post = createNewPost();
        post.setIsPage(true);
        post.setTitle("A fully featured page");
        post.setContent("Some content here! <strong>Bold text</strong>.");
        post.setDateCreated(DateTimeUtils.iso8601UTCFromDate(new Date()));
        uploadPost(post);
        assertEquals(1, mPostStore.getPagesCountForSite(sSite));

        // We should have one page and no post
        assertEquals(1, mPostStore.getPagesCountForSite(sSite));
        assertEquals(0, mPostStore.getPostsCountForSite(sSite));

        // Get the current copy of the page from the PostStore
        PostModel newPage = mPostStore.getPostByLocalPostId(post.getId());
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
        PostModel post = createNewPost();

        post.setIsPage(true);

        post.setTitle("A fully featured page");
        post.setContent("Some content here! <strong>Bold text</strong>.\r\n\r\nA new paragraph.");
        String date = DateTimeUtils.iso8601UTCFromDate(new Date());
        post.setDateCreated(date);

        post.setFeaturedImageId(77); // Not actually valid for pages

        uploadPost(post);

        // Get the current copy of the page from the PostStore
        PostModel newPage = mPostStore.getPostByLocalPostId(post.getId());

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
        assertEquals("Some content here! <strong>Bold text</strong>.\r\n\r\nA new paragraph.", newPage.getContent());
        assertEquals(date, newPage.getDateCreated());

        assertEquals(0, newPage.getFeaturedImageId()); // The page should upload, but have the featured image stripped
    }

    @Test
    public void testClearTagsFromPost() throws InterruptedException {
        PostModel post = createNewPost();

        post.setTitle("A post with tags");
        post.setContent("Some content here! <strong>Bold text</strong>.");

        List<Long> categoryIds = new ArrayList<>(1);
        categoryIds.add((long) 1);
        post.setCategoryIdList(categoryIds);

        List<String> tags = new ArrayList<>(2);
        tags.add("fluxc");
        tags.add("generated-" + RandomStringUtils.randomAlphanumeric(8));
        post.setTagNameList(tags);

        uploadPost(post);

        // Get the current copy of the post from the PostStore
        PostModel newPost = mPostStore.getPostByLocalPostId(post.getId());

        assertFalse(newPost.getTagNameList().isEmpty());

        newPost.setTagNameList(Collections.<String>emptyList());
        newPost.setIsLocallyChanged(true);

        // Upload edited post
        uploadPost(newPost);

        PostModel finalPost = mPostStore.getPostByLocalPostId(post.getId());

        assertTrue(finalPost.getTagNameList().isEmpty());
    }

    @Test
    public void testClearFeaturedImageFromPost() throws InterruptedException {
        PostModel post = createNewPost();

        post.setTitle("A post with featured image");

        String knownImageIds = BuildConfig.TEST_WPCOM_IMAGE_IDS_TEST1;
        long featuredImageId = Long.valueOf(knownImageIds.split(",")[0]);
        post.setFeaturedImageId(featuredImageId);

        uploadPost(post);

        // Get the current copy of the post from the PostStore
        PostModel newPost = mPostStore.getPostByLocalPostId(post.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        assertTrue(newPost.hasFeaturedImage());
        assertEquals(featuredImageId, newPost.getFeaturedImageId());

        newPost.clearFeaturedImage();

        uploadPost(newPost);

        // Get the current copy of the post from the PostStore
        PostModel finalPost = mPostStore.getPostByLocalPostId(post.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        assertFalse(finalPost.hasFeaturedImage());
    }

    @Test
    public void testAddLocationToRemotePost() throws InterruptedException {
        // 1. Upload a post with no location data
        PostModel post = createNewPost();

        post.setTitle("A post with location");
        post.setContent("Some content");

        uploadPost(post);

        // Get the current copy of the post from the PostStore
        post = mPostStore.getPostByLocalPostId(post.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        assertEquals("A post with location", post.getTitle());
        assertEquals("Some content", post.getContent());

        // The post should not have a location since we never set one
        assertFalse(post.hasLocation());

        // 2. Modify the post, setting some location data
        post.setLocation(EXAMPLE_LATITUDE, EXAMPLE_LONGITUDE);
        post.setIsLocallyChanged(true);

        uploadPost(post);

        // Get the current copy of the post from the PostStore
        post = mPostStore.getPostByLocalPostId(post.getId());

        // The set location should be stored in the remote post
        assertTrue(post.hasLocation());
        assertEquals(EXAMPLE_LATITUDE, post.getLocation().getLatitude(), 0.1);
        assertEquals(EXAMPLE_LONGITUDE, post.getLocation().getLongitude(), 0.1);
    }

    @Test
    public void testUploadPostWithLocation() throws InterruptedException {
        // 1. Upload a post with location data
        PostModel post = createNewPost();

        post.setTitle("A post with location");
        post.setContent("Some content");

        post.setLocation(EXAMPLE_LATITUDE, EXAMPLE_LONGITUDE);

        uploadPost(post);

        // Get the current copy of the post from the PostStore
        post = mPostStore.getPostByLocalPostId(post.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        assertEquals("A post with location", post.getTitle());
        assertEquals("Some content", post.getContent());

        // The set location should be stored in the remote post
        assertTrue(post.hasLocation());
        assertEquals(EXAMPLE_LATITUDE, post.getLocation().getLatitude(), 0.1);
        assertEquals(EXAMPLE_LONGITUDE, post.getLocation().getLongitude(), 0.1);

        // 2. Modify the post without changing the location data and update
        post.setTitle("A new title");
        post.setIsLocallyChanged(true);

        uploadPost(post);

        // Get the current copy of the post from the PostStore
        post = mPostStore.getPostByLocalPostId(post.getId());

        assertEquals("A new title", post.getTitle());

        // The location data should not have been altered
        assertTrue(post.hasLocation());
        assertEquals(EXAMPLE_LATITUDE, post.getLocation().getLatitude(), 0.1);
        assertEquals(EXAMPLE_LONGITUDE, post.getLocation().getLongitude(), 0.1);

        // 3. Clear location data from the post and update
        post.clearLocation();
        post.setIsLocallyChanged(true);

        uploadPost(post);

        // Get the current copy of the post from the PostStore
        post = mPostStore.getPostByLocalPostId(post.getId());

        // The post should not have a location anymore
        assertFalse(post.hasLocation());
    }

    @Test
    public void testTrashRemotePost() throws InterruptedException {
        PostModel post = createNewPost();
        setupPostAttributes(post);

        uploadPost(post);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(post.getId());
        assertNotNull(uploadedPost);

        deletePost(uploadedPost);

        // The post status should be trashed
        PostModel trashedPost = mPostStore.getPostByLocalPostId(uploadedPost.getId());
        assertNotNull(trashedPost);
        assertEquals(PostStatus.TRASHED, PostStatus.fromPost(trashedPost));
    }

    @Test
    public void testRestoreRemotePost() throws InterruptedException {
        PostModel post = createNewPost();
        setupPostAttributes(post);

        uploadPost(post);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(post.getId());
        assertNotNull(uploadedPost);

        deletePost(uploadedPost);

        // The post status should be trashed
        PostModel trashedPost = mPostStore.getPostByLocalPostId(uploadedPost.getId());
        assertNotNull(trashedPost);
        assertEquals(PostStatus.TRASHED, PostStatus.fromPost(trashedPost));

        // restore post
        restorePost(uploadedPost, TestEvents.POST_RESTORED);
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        // retrieve restored post from PostStore and make sure it's not TRASHED anymore
        PostModel restoredPost = mPostStore.getPostByRemotePostId(uploadedPost.getRemotePostId(), sSite);
        assertNotNull(restoredPost);
        assertNotEquals(PostStatus.TRASHED, PostStatus.fromPost(restoredPost));
    }

    @Test
    public void testAutoSavePublishedPost() throws InterruptedException {
        // Arrange
        PostModel post = createNewPost();
        post.setStatus(PostStatus.PUBLISHED.toString());

        testAutoSavePostOrPage(post, false, false);
    }

    @Test
    public void testAutoSaveScheduledPost() throws InterruptedException {
        // Arrange
        PostModel post = createNewPost();
        post.setStatus(PostStatus.SCHEDULED.toString());
        post.setDateCreated("2075-10-14T10:51:11+00:00");

        testAutoSavePostOrPage(post, false, true);
    }

    @Test
    public void testAutoSavePublishedPage() throws InterruptedException {
        // Arrange
        PostModel post = createNewPost();
        post.setStatus(PostStatus.PUBLISHED.toString());

        testAutoSavePostOrPage(post, true, false);
    }

    private void testAutoSavePostOrPage(PostModel post, boolean isPage, boolean cleanUp) throws InterruptedException {
        // Arrange
        setupPostAttributes(post);

        post.setIsPage(isPage);
        uploadPost(post);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(post.getId());

        // Act
        uploadedPost.setContent("content edited");
        remoteAutoSavePost(uploadedPost);

        // Assert
        PostModel postAfterAutoSave = mPostStore.getPostByLocalPostId(post.getId());
        assertNotNull(postAfterAutoSave.getAutoSaveModified());
        assertNotNull(postAfterAutoSave.getAutoSavePreviewUrl());
        assertNotEquals(0, postAfterAutoSave.getAutoSaveRevisionId());

        // We don't want to perform the clean up unless necessary to keep the tests as quick as
        // possible. However, creating for example scheduled posts during a test may affect other
        // tests and hence they need to be trashed.
        if (cleanUp) {
            deletePost(uploadedPost);
        }
    }

    @Test
    public void testAutoSaveDoesNotUpdateModifiedDate() throws InterruptedException {
        // Arrange
        PostModel post = createNewPost();
        setupPostAttributes(post);

        post.setStatus(PostStatus.PUBLISHED.toString());

        uploadPost(post);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(post.getId());

        // Act
        uploadedPost.setContent("post content edited");
        remoteAutoSavePost(uploadedPost);

        // Assert
        fetchPost(uploadedPost);

        PostModel postAfterAutoSave = mPostStore.getPostByLocalPostId(post.getId());

        assertEquals(uploadedPost.getLastModified(), postAfterAutoSave.getLastModified());
        assertEquals(uploadedPost.getRemoteLastModified(), postAfterAutoSave.getRemoteLastModified());
    }

    @Test
    public void testAutoSaveModifiedDateIsDifferentThanPostModifiedDate() throws InterruptedException {
        // Arrange
        PostModel post = createNewPost();
        setupPostAttributes(post);

        post.setStatus(PostStatus.PUBLISHED.toString());

        uploadPost(post);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(post.getId());

        // Act
        uploadedPost.setContent("post content edited");
        remoteAutoSavePost(uploadedPost);

        // Assert
        fetchPost(uploadedPost);

        PostModel postAfterAutoSave = mPostStore.getPostByLocalPostId(post.getId());

        assertNotEquals(uploadedPost.getLastModified(), postAfterAutoSave.getAutoSaveModified());
        assertNotEquals(uploadedPost.getRemoteLastModified(), postAfterAutoSave.getAutoSaveModified());
    }

    @Test
    public void testAutoSaveLocalPostResultsInUnknownPostError() throws InterruptedException {
        // Arrange
        PostModel post = createNewPost();
        setupPostAttributes(post);

        post.setStatus(PostStatus.PUBLISHED.toString());

        mNextEvent = TestEvents.ERROR_UNKNOWN_POST;
        mCountDownLatch = new CountDownLatch(1);
        // Act
        mDispatcher.dispatch(PostActionBuilder.newRemoteAutoSavePostAction(new RemotePostPayload(post, sSite)));

        // Assert
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    // Error handling tests

    @Test
    public void testFetchInvalidPost() throws InterruptedException {
        PostModel post = new PostModel();
        post.setRemotePostId(6420328);
        post.setRemoteSiteId(sSite.getSiteId());

        mNextEvent = TestEvents.ERROR_UNKNOWN_POST;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newFetchPostAction(new RemotePostPayload(post, sSite)));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testEditInvalidPost() throws InterruptedException {
        PostModel post = createNewPost();
        setupPostAttributes(post);

        uploadPost(post);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(post.getId());

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

        PostModel persistedPost = mPostStore.getPostByLocalPostId(post.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        // The locally saved post should still be marked as locally changed, and local changes should be preserved
        assertEquals("From testEditInvalidPost", persistedPost.getTitle());
        assertTrue(persistedPost.isLocallyChanged());

        // The date created should not have been altered by the edit
        assertFalse(persistedPost.getDateCreated().isEmpty());
        assertEquals(dateCreated, persistedPost.getDateCreated());
    }

    @Test
    public void testDeleteInvalidRemotePost() throws InterruptedException {
        PostModel invalidPost = new PostModel();
        invalidPost.setRemotePostId(6420328);
        invalidPost.setRemoteSiteId(sSite.getSiteId());

        mNextEvent = TestEvents.ERROR_UNKNOWN_POST;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newDeletePostAction(new RemotePostPayload(invalidPost, sSite)));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testRestoreInvalidRemotePost() throws InterruptedException {
        PostModel invalidPost = new PostModel();
        invalidPost.setRemotePostId(6420328);
        invalidPost.setRemoteSiteId(sSite.getSiteId());

        restorePost(invalidPost, TestEvents.ERROR_UNKNOWN_POST);
    }

    @Test
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
            } else if (mNextEvent.equals(TestEvents.ERROR_UNSUPPORTED_ACTION)) {
                assertEquals(PostErrorType.UNSUPPORTED_ACTION, event.error.type);
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
                mCountDownLatch.countDown();
            }
        } else if (event.causeOfChange instanceof CauseOfOnPostChanged.RemovePost) {
            if (mNextEvent.equals(TestEvents.POST_REMOVED)) {
                assertNotEquals(0, ((RemovePost) event.causeOfChange).getLocalPostId());
                mCountDownLatch.countDown();
            }
        } else if (event.causeOfChange instanceof CauseOfOnPostChanged.RemoveAllPosts) {
            if (mNextEvent.equals(TestEvents.ALL_POST_REMOVED)) {
                mCountDownLatch.countDown();
            }
        } else if (event.causeOfChange instanceof CauseOfOnPostChanged.RestorePost) {
            if (mNextEvent.equals(TestEvents.POST_RESTORED)) {
                mCountDownLatch.countDown();
            }
        } else if (event.causeOfChange instanceof CauseOfOnPostChanged.RemoteAutoSavePost) {
            if (mNextEvent.equals(TestEvents.POST_AUTO_SAVED)) {
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

    private void setupPostAttributes(PostModel post) {
        post.setTitle(POST_DEFAULT_TITLE);
        post.setContent(POST_DEFAULT_DESCRIPTION);
    }

    private PostModel createNewPost() {
        PostModel post = mPostStore.instantiatePostModel(sSite, false);

        assertTrue(post.isLocalDraft());
        assertEquals(0, post.getRemotePostId());
        assertNotSame(0, post.getId());
        assertNotSame(0, post.getLocalSiteId());

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

    private void remoteAutoSavePost(PostModel post) throws InterruptedException {
        mNextEvent = TestEvents.POST_AUTO_SAVED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newRemoteAutoSavePostAction(new RemotePostPayload(post, sSite)));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
