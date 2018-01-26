package org.wordpress.android.fluxc;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.generated.endpoint.WPAPI;

@RunWith(RobolectricTestRunner.class)
public class WPAPIEndpointTest {
    @Test
    public void testAllEndpoints() {
        // Posts
        Assert.assertEquals("/posts/", WPAPI.posts.getEndpoint());
        Assert.assertEquals("/posts/56/", WPAPI.posts.id(56).getEndpoint());

        // Pages
        Assert.assertEquals("/pages/", WPAPI.pages.getEndpoint());
        Assert.assertEquals("/pages/56/", WPAPI.pages.id(56).getEndpoint());

        // Media
        Assert.assertEquals("/media/", WPAPI.media.getEndpoint());
        Assert.assertEquals("/media/56/", WPAPI.media.id(56).getEndpoint());

        // Comments
        Assert.assertEquals("/comments/", WPAPI.comments.getEndpoint());
        Assert.assertEquals("/comments/56/", WPAPI.comments.id(56).getEndpoint());

        // Settings
        Assert.assertEquals("/settings/", WPAPI.settings.getEndpoint());
    }

    @Test
    public void testUrls() {
        Assert.assertEquals("wp/v2/posts/", WPAPI.posts.getUrlV2());
        Assert.assertEquals("wp/v2/pages/", WPAPI.pages.getUrlV2());
        Assert.assertEquals("wp/v2/media/", WPAPI.media.getUrlV2());
        Assert.assertEquals("wp/v2/comments/", WPAPI.comments.getUrlV2());
        Assert.assertEquals("wp/v2/settings/", WPAPI.settings.getUrlV2());
    }
}
