package org.wordpress.android.fluxc.upload;

import android.text.TextUtils;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.model.MediaUploadModel;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;

import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class MediaUploadModelTest {
    @Before
    public void setUp() {
    }

    @Test
    public void testEquals() {
        MediaUploadModel mediaUploadModel1 = new MediaUploadModel(1);
        MediaUploadModel mediaUploadModel2 = new MediaUploadModel(1);

        mediaUploadModel1.setUploadState(MediaUploadModel.FAILED);
        Assert.assertFalse(mediaUploadModel1.equals(mediaUploadModel2));

        mediaUploadModel2.setUploadState(MediaUploadModel.FAILED);

        MediaError mediaError = new MediaError(MediaErrorType.EXCEEDS_MEMORY_LIMIT, "Too large!");
        mediaUploadModel1.setMediaError(mediaError);
        Assert.assertFalse(mediaUploadModel1.equals(mediaUploadModel2));

        mediaUploadModel2.setErrorType(mediaError.type.toString());
        mediaUploadModel2.setErrorMessage(mediaError.message);

        Assert.assertTrue(mediaUploadModel1.equals(mediaUploadModel2));
    }

    @Test
    public void testMediaError() {
        MediaUploadModel mediaUploadModel = new MediaUploadModel(1);

        assertNull(mediaUploadModel.getMediaError());
        Assert.assertTrue(TextUtils.isEmpty(mediaUploadModel.getErrorType()));
        Assert.assertTrue(TextUtils.isEmpty(mediaUploadModel.getErrorMessage()));

        mediaUploadModel.setMediaError(new MediaError(MediaErrorType.EXCEEDS_MEMORY_LIMIT, "Too large!"));
        Assert.assertNotNull(mediaUploadModel.getMediaError());
        Assert.assertEquals(MediaErrorType.EXCEEDS_MEMORY_LIMIT, MediaErrorType.fromString(mediaUploadModel.getErrorType()));
        Assert.assertEquals("Too large!", mediaUploadModel.getErrorMessage());
    }
}
