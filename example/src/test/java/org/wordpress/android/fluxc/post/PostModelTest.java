package org.wordpress.android.fluxc.post;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.post.PostLocation;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.network.BaseRequest;

import java.util.ArrayList;
import java.util.List;

import static org.wordpress.android.fluxc.post.PostTestUtils.EXAMPLE_LATITUDE;
import static org.wordpress.android.fluxc.post.PostTestUtils.EXAMPLE_LONGITUDE;

@RunWith(RobolectricTestRunner.class)
public class PostModelTest {
    @Test
    public void testEquals() {
        PostModel testPost = PostTestUtils.generateSampleUploadedPost();
        PostModel testPost2 = PostTestUtils.generateSampleUploadedPost();

        testPost2.setRemotePostId(testPost.getRemotePostId() + 1);
        Assert.assertFalse(testPost.equals(testPost2));
        testPost2.setRemotePostId(testPost.getRemotePostId());
        Assert.assertTrue(testPost.equals(testPost2));
    }

    @Test
    public void testClone() {
        PostModel testPost = PostTestUtils.generateSampleLocalDraftPost();

        // Fill a few more sample fields
        testPost.setDateCreated("1955-11-05T06:15:00-0800");
        testPost.setStatus(PostStatus.SCHEDULED.toString());
        List<Long> categoryList = new ArrayList<>();
        categoryList.add(45L);
        testPost.setCategoryIdList(categoryList);

        testPost.error = new BaseRequest.BaseNetworkError(BaseRequest.GenericErrorType.PARSE_ERROR);

        PostModel clonedPost = (PostModel) testPost.clone();

        Assert.assertFalse(testPost == clonedPost);
        Assert.assertTrue(testPost.equals(clonedPost));

        // The inherited error should also be cloned
        Assert.assertFalse(testPost.error == clonedPost.error);
    }

    @Test
    public void testTerms() {
        PostModel testPost = PostTestUtils.generateSampleLocalDraftPost();

        testPost.setCategoryIdList(null);
        Assert.assertTrue(testPost.getCategoryIdList().isEmpty());

        List<Long> categoryIds = new ArrayList<>();
        testPost.setCategoryIdList(categoryIds);
        Assert.assertTrue(testPost.getCategoryIdList().isEmpty());

        categoryIds.add((long) 5);
        categoryIds.add((long) 6);
        testPost.setCategoryIdList(categoryIds);

        Assert.assertEquals(2, testPost.getCategoryIdList().size());
        Assert.assertTrue(categoryIds.containsAll(testPost.getCategoryIdList())
                   && testPost.getCategoryIdList().containsAll(categoryIds));
    }

    @Test
    public void testLocation() {
        PostModel testPost = PostTestUtils.generateSampleLocalDraftPost();

        // Expect no location if none was set
        Assert.assertFalse(testPost.hasLocation());
        Assert.assertFalse(testPost.getLocation().isValid());
        Assert.assertFalse(testPost.shouldDeleteLatitude());
        Assert.assertFalse(testPost.shouldDeleteLongitude());

        // Verify state when location is set
        testPost.setLocation(new PostLocation(EXAMPLE_LATITUDE, EXAMPLE_LONGITUDE));

        Assert.assertTrue(testPost.hasLocation());
        Assert.assertEquals(EXAMPLE_LATITUDE, testPost.getLatitude(), 0);
        Assert.assertEquals(EXAMPLE_LONGITUDE, testPost.getLongitude(), 0);
        Assert.assertEquals(new PostLocation(EXAMPLE_LATITUDE, EXAMPLE_LONGITUDE), testPost.getLocation());
        Assert.assertFalse(testPost.shouldDeleteLatitude());
        Assert.assertFalse(testPost.shouldDeleteLongitude());

        // (0, 0) is a valid location
        testPost.setLocation(0, 0);

        Assert.assertTrue(testPost.hasLocation());
        Assert.assertEquals(0, testPost.getLatitude(), 0);
        Assert.assertEquals(0, testPost.getLongitude(), 0);
        Assert.assertEquals(new PostLocation(0, 0), testPost.getLocation());
        Assert.assertFalse(testPost.shouldDeleteLatitude());
        Assert.assertFalse(testPost.shouldDeleteLongitude());

        // Clearing the location should remove the location, and flag it for deletion on the server
        testPost.clearLocation();

        Assert.assertFalse(testPost.hasLocation());
        Assert.assertFalse(testPost.getLocation().isValid());
        Assert.assertTrue(testPost.shouldDeleteLatitude());
        Assert.assertTrue(testPost.shouldDeleteLongitude());
    }

    @Test
    public void testFilterEmptyTagsOnGetTagNameList() {
        PostModel testPost = PostTestUtils.generateSampleLocalDraftPost();

        testPost.setTagNames("pony,             ,ponies");
        List<String> tags = testPost.getTagNameList();
        Assert.assertTrue(tags.contains("pony"));
        Assert.assertTrue(tags.contains("ponies"));
        Assert.assertEquals(2, tags.size());
    }

    @Test
    public void testStripTagsOnGetTagNameList() {
        PostModel testPost = PostTestUtils.generateSampleLocalDraftPost();

        testPost.setTagNames("    pony   , ponies    , #popopopopopony");
        List<String> tags = testPost.getTagNameList();

        Assert.assertTrue(tags.contains("pony"));
        Assert.assertTrue(tags.contains("ponies"));
        Assert.assertTrue(tags.contains("#popopopopopony"));
        Assert.assertEquals(3, tags.size());
    }
}
