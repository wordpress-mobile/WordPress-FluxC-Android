package org.wordpress.android.fluxc.upload;

import android.text.TextUtils;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.model.PostUploadModel;
import org.wordpress.android.fluxc.store.PostStore.PostError;
import org.wordpress.android.fluxc.store.PostStore.PostErrorType;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class PostUploadModelTest {
    @Before
    public void setUp() {
    }

    @Test
    public void testEquals() {
        PostUploadModel postUploadModel1 = new PostUploadModel(1);
        PostUploadModel postUploadModel2 = new PostUploadModel(1);

        Set<Integer> idSet = new HashSet<>();
        idSet.add(6);
        idSet.add(5);
        postUploadModel1.setAssociatedMediaIdSet(idSet);
        Assert.assertFalse(postUploadModel1.equals(postUploadModel2));

        postUploadModel2.setAssociatedMediaIdSet(idSet);

        PostError postError = new PostError(PostErrorType.UNKNOWN_POST, "Unknown post");
        postUploadModel1.setPostError(postError);

        Assert.assertFalse(postUploadModel1.equals(postUploadModel2));

        postUploadModel2.setErrorType(postError.type.toString());
        postUploadModel2.setErrorMessage(postError.message);

        Assert.assertTrue(postUploadModel1.equals(postUploadModel2));
    }

    @Test
    public void testAssociatedMediaIds() {
        PostUploadModel postUploadModel = new PostUploadModel(1);
        Set<Integer> idSet = new HashSet<>();
        idSet.add(6);
        idSet.add(5);
        postUploadModel.setAssociatedMediaIdSet(idSet);
        Assert.assertEquals("5,6", postUploadModel.getAssociatedMediaIds());
        Assert.assertTrue(idSet.containsAll(postUploadModel.getAssociatedMediaIdSet()));
        Assert.assertTrue(postUploadModel.getAssociatedMediaIdSet().containsAll(idSet));
    }

    @Test
    public void testPostError() {
        PostUploadModel postUploadModel = new PostUploadModel(1);

        assertNull(postUploadModel.getPostError());
        Assert.assertTrue(TextUtils.isEmpty(postUploadModel.getErrorType()));
        Assert.assertTrue(TextUtils.isEmpty(postUploadModel.getErrorMessage()));

        postUploadModel.setPostError(new PostError(PostErrorType.UNKNOWN_POST, "Unknown post"));
        Assert.assertNotNull(postUploadModel.getPostError());
        Assert.assertEquals(PostErrorType.UNKNOWN_POST, PostErrorType.fromString(postUploadModel.getErrorType()));
        Assert.assertEquals("Unknown post", postUploadModel.getErrorMessage());
    }
}
