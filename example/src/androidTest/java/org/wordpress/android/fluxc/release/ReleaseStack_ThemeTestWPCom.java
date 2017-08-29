package org.wordpress.android.fluxc.release;

import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;

import javax.inject.Inject;

public class ReleaseStack_ThemeTestWPCom extends ReleaseStack_Base {
    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }
}
