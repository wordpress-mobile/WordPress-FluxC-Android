package org.wordpress.android.fluxc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class WPComEndpointTest {
    @Test
    public void testAllEndpoints() {
        // Sites
        assertEquals("/sites", WPCOMREST.sites.getFEndpoint());
        assertEquals("/sites/new", WPCOMREST.sites.new_.getFEndpoint());
        assertEquals("/sites/56", WPCOMREST.sites.site(56).getFEndpoint());
        assertEquals("/sites/56/post-formats", WPCOMREST.sites.site(56).post_formats.getFEndpoint());

        // Sites - Posts
        assertEquals("/sites/56/posts", WPCOMREST.sites.site(56).posts.getFEndpoint());
        assertEquals("/sites/56/posts/78", WPCOMREST.sites.site(56).posts.post(78).getFEndpoint());
        assertEquals("/sites/56/posts/78/delete", WPCOMREST.sites.site(56).posts.post(78).delete.getFEndpoint());
        assertEquals("/sites/56/posts/new", WPCOMREST.sites.site(56).posts.new_.getFEndpoint());
        assertEquals("/sites/56/posts/slug:fluxc", WPCOMREST.sites.site(56).posts.slug("fluxc").getFEndpoint());

        // Sites - Media
        assertEquals("/sites/56/media", WPCOMREST.sites.site(56).media.getFEndpoint());
        assertEquals("/sites/56/media/78", WPCOMREST.sites.site(56).media.item(78).getFEndpoint());
        assertEquals("/sites/56/media/78/delete", WPCOMREST.sites.site(56).media.item(78).delete.getFEndpoint());
        assertEquals("/sites/56/media/new", WPCOMREST.sites.site(56).media.new_.getFEndpoint());

        // Sites - Taxonomies
        assertEquals("/sites/56/taxonomies/category/terms",
                WPCOMREST.sites.site(56).taxonomies.taxonomy("category").terms.getFEndpoint());
        assertEquals("/sites/56/taxonomies/category/terms/new",
                WPCOMREST.sites.site(56).taxonomies.taxonomy("category").terms.new_.getFEndpoint());
        assertEquals("/sites/56/taxonomies/category/terms/slug:fluxc",
                WPCOMREST.sites.site(56).taxonomies.taxonomy("category").terms.slug("fluxc").getFEndpoint());
        assertEquals("/sites/56/taxonomies/post_tag/terms",
                WPCOMREST.sites.site(56).taxonomies.taxonomy("post_tag").terms.getFEndpoint());
        assertEquals("/sites/56/taxonomies/post_tag/terms/new",
                WPCOMREST.sites.site(56).taxonomies.taxonomy("post_tag").terms.new_.getFEndpoint());

        // Me
        assertEquals("/me", WPCOMREST.me.getFEndpoint());
        assertEquals("/me/settings", WPCOMREST.me.settings.getFEndpoint());
        assertEquals("/me/sites", WPCOMREST.me.sites.getFEndpoint());

        // Users
        assertEquals("/users/new", WPCOMREST.users.new_.getFEndpoint());
        assertEquals("/users/new", WPCOMREST.users.new_.getFEndpoint());

        // Availability
        assertEquals("/is-available/email", WPCOMREST.is_available.email.getFEndpoint());
        assertEquals("/is-available/username", WPCOMREST.is_available.username.getFEndpoint());
        assertEquals("/is-available/blog", WPCOMREST.is_available.blog.getFEndpoint());
        assertEquals("/is-available/domain", WPCOMREST.is_available.domain.getFEndpoint());

        // Magic link email sender
        assertEquals("/auth/send-login-email", WPCOMREST.auth.send_login_email.getFEndpoint());
    }

    @Test
    public void testUrls() {
        assertEquals("https://public-api.wordpress.com/rest/v1/sites", WPCOMREST.sites.getUrlV1());
        assertEquals("https://public-api.wordpress.com/rest/v1.1/sites", WPCOMREST.sites.getUrlV1_1());
        assertEquals("https://public-api.wordpress.com/rest/v1.2/sites", WPCOMREST.sites.getUrlV1_2());
        assertEquals("https://public-api.wordpress.com/rest/v1.3/sites", WPCOMREST.sites.getUrlV1_3());
        assertEquals("https://public-api.wordpress.com/is-available/email", WPCOMREST.is_available.email.getUrlV0());
    }
}
