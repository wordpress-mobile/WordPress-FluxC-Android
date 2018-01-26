package org.wordpress.android.fluxc.post;

import android.content.Context;

import com.yarolegovich.wellsql.WellSql;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.post.PostXMLRPCClient;
import org.wordpress.android.fluxc.persistence.PostSqlUtils;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertNotEquals;

@RunWith(RobolectricTestRunner.class)
public class PostStoreUnitTest {
    private PostStore mPostStore = new PostStore(new Dispatcher(), Mockito.mock(PostRestClient.class),
            Mockito.mock(PostXMLRPCClient.class));

    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.application.getApplicationContext();

        WellSqlConfig config = new SingleStoreWellSqlConfigForTests(appContext, PostModel.class);
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void testInsertNullPost() {
        Assert.assertEquals(0, PostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(null));

        Assert.assertEquals(0, PostTestUtils.getPostsCount());
    }

    @Test
    public void testSimpleInsertionAndRetrieval() {
        PostModel postModel = new PostModel();
        postModel.setRemotePostId(42);
        PostModel result = PostSqlUtils.insertPostForResult(postModel);

        Assert.assertEquals(1, PostTestUtils.getPostsCount());
        Assert.assertEquals(42, PostTestUtils.getPosts().get(0).getRemotePostId());
        Assert.assertEquals(postModel, result);
    }

    @Test
    public void testInsertWithLocalChanges() {
        PostModel postModel = PostTestUtils.generateSampleUploadedPost();
        postModel.setIsLocallyChanged(true);
        PostSqlUtils.insertPostForResult(postModel);

        String newTitle = "A different title";
        postModel.setTitle(newTitle);

        Assert.assertEquals(0, PostSqlUtils.insertOrUpdatePostKeepingLocalChanges(postModel));
        Assert.assertEquals("A test post", PostTestUtils.getPosts().get(0).getTitle());

        Assert.assertEquals(1, PostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(postModel));
        Assert.assertEquals(newTitle, PostTestUtils.getPosts().get(0).getTitle());
    }

    @Test
    public void testPushAndFetchCollision() throws InterruptedException {
        // Test uploading a post, fetching remote posts and updating the db from the fetch first

        PostModel postModel = PostTestUtils.generateSampleLocalDraftPost();
        PostSqlUtils.insertPostForResult(postModel);

        // The post after uploading, updated with the remote post ID, about to be saved locally
        PostModel postFromUploadResponse = PostTestUtils.getPosts().get(0);
        postFromUploadResponse.setIsLocalDraft(false);
        postFromUploadResponse.setRemotePostId(42);

        // The same post, but fetched from the server from FETCH_POSTS (so no local ID until insertion)
        final PostModel postFromPostListFetch = postFromUploadResponse.clone();
        postFromPostListFetch.setId(0);

        PostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(postFromPostListFetch);
        PostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(postFromUploadResponse);

        Assert.assertEquals(1, PostTestUtils.getPosts().size());

        PostModel finalPost = PostTestUtils.getPosts().get(0);
        Assert.assertEquals(42, finalPost.getRemotePostId());
        Assert.assertEquals(postModel.getLocalSiteId(), finalPost.getLocalSiteId());
    }

    @Test
    public void testInsertWithoutLocalChanges() {
        PostModel postModel = PostTestUtils.generateSampleUploadedPost();
        PostSqlUtils.insertPostForResult(postModel);

        String newTitle = "A different title";
        postModel.setTitle(newTitle);

        Assert.assertEquals(1, PostSqlUtils.insertOrUpdatePostKeepingLocalChanges(postModel));
        Assert.assertEquals(newTitle, PostTestUtils.getPosts().get(0).getTitle());

        newTitle = "Another different title";
        postModel.setTitle(newTitle);

        Assert.assertEquals(1, PostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(postModel));
        Assert.assertEquals(newTitle, PostTestUtils.getPosts().get(0).getTitle());
    }

    @Test
    public void testGetPostsForSite() {
        PostModel uploadedPost1 = PostTestUtils.generateSampleUploadedPost();
        PostSqlUtils.insertPostForResult(uploadedPost1);

        PostModel uploadedPost2 = PostTestUtils.generateSampleUploadedPost();
        uploadedPost2.setLocalSiteId(8);
        PostSqlUtils.insertPostForResult(uploadedPost2);

        SiteModel site1 = new SiteModel();
        site1.setId(uploadedPost1.getLocalSiteId());

        SiteModel site2 = new SiteModel();
        site2.setId(uploadedPost2.getLocalSiteId());

        Assert.assertEquals(2, PostTestUtils.getPostsCount());

        Assert.assertEquals(1, mPostStore.getPostsCountForSite(site1));
        Assert.assertEquals(1, mPostStore.getPostsCountForSite(site2));
    }

    @Test
    public void testGetPostsWithFormatForSite() {
        PostModel textPost = PostTestUtils.generateSampleUploadedPost();
        PostModel imagePost = PostTestUtils.generateSampleUploadedPost("image");
        PostModel videoPost = PostTestUtils.generateSampleUploadedPost("video");
        PostSqlUtils.insertPostForResult(textPost);
        PostSqlUtils.insertPostForResult(imagePost);
        PostSqlUtils.insertPostForResult(videoPost);

        SiteModel site = new SiteModel();
        site.setId(textPost.getLocalSiteId());

        ArrayList<String> postFormat = new ArrayList<>();
        postFormat.add("image");
        postFormat.add("video");
        List<PostModel> postList = mPostStore.getPostsForSiteWithFormat(site, postFormat);

        Assert.assertEquals(2, postList.size());
        Assert.assertTrue(postList.contains(imagePost));
        Assert.assertTrue(postList.contains(videoPost));
        Assert.assertFalse(postList.contains(textPost));
    }

    @Test
    public void testGetPublishedPosts() {
        SiteModel site = new SiteModel();
        site.setId(6);

        PostModel uploadedPost = PostTestUtils.generateSampleUploadedPost();
        PostSqlUtils.insertPostForResult(uploadedPost);

        PostModel localDraft = PostTestUtils.generateSampleLocalDraftPost();
        PostSqlUtils.insertPostForResult(localDraft);

        Assert.assertEquals(2, PostTestUtils.getPostsCount());
        Assert.assertEquals(2, mPostStore.getPostsCountForSite(site));

        Assert.assertEquals(1, mPostStore.getUploadedPostsCountForSite(site));
    }

    @Test
    public void testGetPostByLocalId() {
        PostModel post = PostTestUtils.generateSampleLocalDraftPost();
        PostSqlUtils.insertPostForResult(post);

        Assert.assertEquals(post, mPostStore.getPostByLocalPostId(post.getId()));
    }

    @Test
    public void testGetPostByRemoteId() {
        PostModel post = PostTestUtils.generateSampleUploadedPost();
        PostSqlUtils.insertPostForResult(post);

        SiteModel site = new SiteModel();
        site.setId(6);

        Assert.assertEquals(post, mPostStore.getPostByRemotePostId(post.getRemotePostId(), site));
    }

    @Test
    public void testDeleteUploadedPosts() {
        SiteModel site = new SiteModel();
        site.setId(6);

        PostModel uploadedPost1 = PostTestUtils.generateSampleUploadedPost();
        PostSqlUtils.insertPostForResult(uploadedPost1);

        PostModel uploadedPost2 = PostTestUtils.generateSampleUploadedPost();
        uploadedPost2.setRemotePostId(9);
        PostSqlUtils.insertPostForResult(uploadedPost2);

        PostModel localDraft = PostTestUtils.generateSampleLocalDraftPost();
        PostSqlUtils.insertPostForResult(localDraft);

        PostModel locallyChangedPost = PostTestUtils.generateSampleLocallyChangedPost();
        PostSqlUtils.insertPostForResult(locallyChangedPost);

        Assert.assertEquals(4, mPostStore.getPostsCountForSite(site));

        PostSqlUtils.deleteUploadedPostsForSite(site, false);

        Assert.assertEquals(2, mPostStore.getPostsCountForSite(site));
    }

    @Test
    public void testDeletePost() {
        SiteModel site = new SiteModel();
        site.setId(6);

        PostModel uploadedPost1 = PostTestUtils.generateSampleUploadedPost();
        PostSqlUtils.insertPostForResult(uploadedPost1);

        PostModel uploadedPost2 = PostTestUtils.generateSampleUploadedPost();
        uploadedPost2.setRemotePostId(9);
        PostSqlUtils.insertPostForResult(uploadedPost2);

        PostModel localDraft = PostTestUtils.generateSampleLocalDraftPost();
        PostSqlUtils.insertPostForResult(localDraft);

        PostModel locallyChangedPost = PostTestUtils.generateSampleLocallyChangedPost();
        PostSqlUtils.insertPostForResult(locallyChangedPost);

        Assert.assertEquals(4, mPostStore.getPostsCountForSite(site));

        PostSqlUtils.deletePost(uploadedPost1);

        Assert.assertEquals(null, mPostStore.getPostByLocalPostId(uploadedPost1.getId()));
        Assert.assertEquals(3, mPostStore.getPostsCountForSite(site));

        PostSqlUtils.deletePost(uploadedPost2);
        PostSqlUtils.deletePost(localDraft);

        assertNotEquals(null, mPostStore.getPostByLocalPostId(locallyChangedPost.getId()));
        Assert.assertEquals(1, mPostStore.getPostsCountForSite(site));

        PostSqlUtils.deletePost(locallyChangedPost);

        Assert.assertEquals(null, mPostStore.getPostByLocalPostId(locallyChangedPost.getId()));
        Assert.assertEquals(0, mPostStore.getPostsCountForSite(site));
        Assert.assertEquals(0, PostTestUtils.getPostsCount());
    }

    @Test
    public void testPostAndPageSeparation() {
        SiteModel site = new SiteModel();
        site.setId(6);

        PostModel post = new PostModel();
        post.setLocalSiteId(6);
        post.setRemotePostId(42);
        PostSqlUtils.insertPostForResult(post);

        PostModel page = new PostModel();
        page.setIsPage(true);
        page.setLocalSiteId(6);
        page.setRemotePostId(43);
        PostSqlUtils.insertPostForResult(page);

        Assert.assertEquals(2, PostTestUtils.getPostsCount());

        Assert.assertEquals(1, mPostStore.getPostsCountForSite(site));
        Assert.assertEquals(1, mPostStore.getPagesCountForSite(site));

        Assert.assertFalse(PostTestUtils.getPosts().get(0).isPage());
        Assert.assertTrue(PostTestUtils.getPosts().get(1).isPage());

        Assert.assertEquals(1, mPostStore.getUploadedPostsCountForSite(site));
        Assert.assertEquals(1, mPostStore.getUploadedPagesCountForSite(site));
    }

    @Test
    public void testPostOrder() {
        SiteModel site = new SiteModel();
        site.setId(6);

        PostModel post = new PostModel();
        post.setLocalSiteId(6);
        post.setRemotePostId(42);
        post.setDateCreated(DateTimeUtils.iso8601UTCFromDate(new Date()));
        PostSqlUtils.insertPostForResult(post);

        PostModel localDraft = new PostModel();
        localDraft.setLocalSiteId(6);
        localDraft.setIsLocalDraft(true);
        localDraft.setDateCreated("2016-01-01T07:00:00+00:00");
        PostSqlUtils.insertPostForResult(localDraft);

        PostModel scheduledPost = new PostModel();
        scheduledPost.setLocalSiteId(6);
        scheduledPost.setRemotePostId(23);
        scheduledPost.setDateCreated("2056-01-01T07:00:00+00:00");
        PostSqlUtils.insertPostForResult(scheduledPost);

        List<PostModel> posts = PostSqlUtils.getPostsForSite(site, false);

        // Expect order draft > scheduled > published
        Assert.assertTrue(posts.get(0).isLocalDraft());
        Assert.assertEquals(23, posts.get(1).getRemotePostId());
        Assert.assertEquals(42, posts.get(2).getRemotePostId());
    }

    @Test
    public void testRemoveAllPosts() {
        PostModel uploadedPost1 = PostTestUtils.generateSampleUploadedPost();
        PostSqlUtils.insertPostForResult(uploadedPost1);

        PostModel uploadedPost2 = PostTestUtils.generateSampleUploadedPost();
        uploadedPost2.setLocalSiteId(8);
        PostSqlUtils.insertPostForResult(uploadedPost2);

        Assert.assertEquals(2, PostTestUtils.getPostsCount());

        PostSqlUtils.deleteAllPosts();

        Assert.assertEquals(0, PostTestUtils.getPostsCount());
    }
}
