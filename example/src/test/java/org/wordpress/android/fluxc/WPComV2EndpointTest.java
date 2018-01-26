package org.wordpress.android.fluxc;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2;

@RunWith(RobolectricTestRunner.class)
public class WPComV2EndpointTest {
    @Test
    public void testAllEndpoints() {
        // Users
        Assert.assertEquals("/users/username/suggestions/", WPCOMV2.users.username.suggestions.getEndpoint());
    }

    @Test
    public void testUrls() {
        Assert.assertEquals("https://public-api.wordpress.com/wpcom/v2/users/username/suggestions/",
                WPCOMV2.users.username.suggestions.getUrl());
    }
}
