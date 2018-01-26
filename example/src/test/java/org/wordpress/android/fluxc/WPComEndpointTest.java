package org.wordpress.android.fluxc;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;

@RunWith(RobolectricTestRunner.class)
public class WPComEndpointTest {
    @Test
    public void testAllEndpoints() {
        // Sites
        Assert.assertEquals("/sites/", WPCOMREST.sites.getEndpoint());
        Assert.assertEquals("/sites/new/", WPCOMREST.sites.new_.getEndpoint());
        Assert.assertEquals("/sites/56/", WPCOMREST.sites.site(56).getEndpoint());
        Assert.assertEquals("/sites/56/post-formats/", WPCOMREST.sites.site(56).post_formats.getEndpoint());

        Assert.assertEquals("/sites/mysite.wordpress.com/", WPCOMREST.sites.siteUrl("mysite.wordpress.com").getEndpoint());

        // Sites - Posts
        Assert.assertEquals("/sites/56/posts/", WPCOMREST.sites.site(56).posts.getEndpoint());
        Assert.assertEquals("/sites/56/posts/78/", WPCOMREST.sites.site(56).posts.post(78).getEndpoint());
        Assert.assertEquals("/sites/56/posts/78/delete/", WPCOMREST.sites.site(56).posts.post(78).delete.getEndpoint());
        Assert.assertEquals("/sites/56/posts/new/", WPCOMREST.sites.site(56).posts.new_.getEndpoint());
        Assert.assertEquals("/sites/56/posts/slug:fluxc/", WPCOMREST.sites.site(56).posts.slug("fluxc").getEndpoint());

        // Sites - Media
        Assert.assertEquals("/sites/56/media/", WPCOMREST.sites.site(56).media.getEndpoint());
        Assert.assertEquals("/sites/56/media/78/", WPCOMREST.sites.site(56).media.item(78).getEndpoint());
        Assert.assertEquals("/sites/56/media/78/delete/", WPCOMREST.sites.site(56).media.item(78).delete.getEndpoint());
        Assert.assertEquals("/sites/56/media/new/", WPCOMREST.sites.site(56).media.new_.getEndpoint());

        // Plugins
        Assert.assertEquals("/sites/56/plugins/", WPCOMREST.sites.site(56).plugins.getEndpoint());
        Assert.assertEquals("/sites/56/plugins/akismet/", WPCOMREST.sites.site(56).plugins.name("akismet").getEndpoint());
        Assert.assertEquals("/sites/56/plugins/akismet/install/", WPCOMREST.sites.site(56).plugins.name("akismet")
                .install.getEndpoint());
        Assert.assertEquals("/sites/56/plugins/akismet/delete/", WPCOMREST.sites.site(56).plugins.name("akismet")
                .delete.getEndpoint());

        // Sites - Taxonomies
        Assert.assertEquals("/sites/56/taxonomies/category/terms/",
                WPCOMREST.sites.site(56).taxonomies.taxonomy("category").terms.getEndpoint());
        Assert.assertEquals("/sites/56/taxonomies/category/terms/new/",
                WPCOMREST.sites.site(56).taxonomies.taxonomy("category").terms.new_.getEndpoint());
        Assert.assertEquals("/sites/56/taxonomies/category/terms/slug:fluxc/",
                WPCOMREST.sites.site(56).taxonomies.taxonomy("category").terms.slug("fluxc").getEndpoint());
        Assert.assertEquals("/sites/56/taxonomies/post_tag/terms/",
                WPCOMREST.sites.site(56).taxonomies.taxonomy("post_tag").terms.getEndpoint());
        Assert.assertEquals("/sites/56/taxonomies/post_tag/terms/new/",
                WPCOMREST.sites.site(56).taxonomies.taxonomy("post_tag").terms.new_.getEndpoint());

        // Me
        Assert.assertEquals("/me/", WPCOMREST.me.getEndpoint());
        Assert.assertEquals("/me/settings/", WPCOMREST.me.settings.getEndpoint());
        Assert.assertEquals("/me/sites/", WPCOMREST.me.sites.getEndpoint());
        Assert.assertEquals("/me/username/", WPCOMREST.me.username.getEndpoint());

        // Users
        Assert.assertEquals("/users/new/", WPCOMREST.users.new_.getEndpoint());
        Assert.assertEquals("/users/new/", WPCOMREST.users.new_.getEndpoint());

        // Availability
        Assert.assertEquals("/is-available/email/", WPCOMREST.is_available.email.getEndpoint());
        Assert.assertEquals("/is-available/username/", WPCOMREST.is_available.username.getEndpoint());
        Assert.assertEquals("/is-available/blog/", WPCOMREST.is_available.blog.getEndpoint());
        Assert.assertEquals("/is-available/domain/", WPCOMREST.is_available.domain.getEndpoint());

        // Magic link email sender
        Assert.assertEquals("/auth/send-login-email/", WPCOMREST.auth.send_login_email.getEndpoint());
        Assert.assertEquals("/auth/send-signup-email/", WPCOMREST.auth.send_signup_email.getEndpoint());

        Assert.assertEquals("/read/feed/56/", WPCOMREST.read.feed.feed_url_or_id(56).getEndpoint());
        Assert.assertEquals("/read/feed/somewhere.site/", WPCOMREST.read.feed.feed_url_or_id("somewhere.site").getEndpoint());
    }

    @Test
    public void testUrls() {
        Assert.assertEquals("https://public-api.wordpress.com/rest/v1/sites/", WPCOMREST.sites.getUrlV1());
        Assert.assertEquals("https://public-api.wordpress.com/rest/v1.1/sites/", WPCOMREST.sites.getUrlV1_1());
        Assert.assertEquals("https://public-api.wordpress.com/rest/v1.2/sites/", WPCOMREST.sites.getUrlV1_2());
        Assert.assertEquals("https://public-api.wordpress.com/rest/v1.3/sites/", WPCOMREST.sites.getUrlV1_3());
        Assert.assertEquals("https://public-api.wordpress.com/is-available/email/", WPCOMREST.is_available.email.getUrlV0());
    }
}
