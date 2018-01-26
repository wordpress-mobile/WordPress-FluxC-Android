package org.wordpress.android.fluxc.taxonomy;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.model.TermModel;

@RunWith(RobolectricTestRunner.class)
public class TermModelTest {
    @Test
    public void testEquals() {
        TermModel testCategory = TaxonomyTestUtils.generateSampleCategory();
        TermModel testCategory2 = TaxonomyTestUtils.generateSampleCategory();

        testCategory2.setRemoteTermId(testCategory.getRemoteTermId() + 1);
        Assert.assertFalse(testCategory.equals(testCategory2));
        testCategory2.setRemoteTermId(testCategory.getRemoteTermId());
        Assert.assertTrue(testCategory.equals(testCategory2));
    }
}
