package org.wordpress.android.fluxc;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.generated.endpoint.WPORGAPI;

@RunWith(RobolectricTestRunner.class)
public class WPOrgAPIEndpointTest {
    @Test
    public void testAllEndpoints() {
        // Plugins info
        Assert.assertEquals("/plugins/info/1.0/akismet/", WPORGAPI.plugins.info.version("1.0").slug("akismet").getEndpoint());
        Assert.assertEquals("/plugins/info/1.1/", WPORGAPI.plugins.info.version("1.1").getEndpoint());
    }

    @Test
    public void testUrls() {
        Assert.assertEquals("https://api.wordpress.org/plugins/info/1.0/akismet.json",
                WPORGAPI.plugins.info.version("1.0").slug("akismet").getUrl());
        Assert.assertEquals("https://api.wordpress.org/plugins/info/1.1/",
                WPORGAPI.plugins.info.version("1.1").getUrl());
    }
}
