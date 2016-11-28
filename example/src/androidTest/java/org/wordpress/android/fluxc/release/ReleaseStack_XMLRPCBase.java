package org.wordpress.android.fluxc.release;

import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.model.SiteModel;

public class ReleaseStack_XMLRPCBase extends ReleaseStack_Base {
    static SiteModel sSite;

    {
        sSite = new SiteModel();
        sSite.setId(1);
        sSite.setSelfHostedSiteId(0);
        sSite.setUsername(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE);
        sSite.setPassword(BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE);
        sSite.setXmlRpcUrl(BuildConfig.TEST_WPORG_URL_SH_SIMPLE_ENDPOINT);
    }
}
