package org.wordpress.android.fluxc.release;

import org.wordpress.android.fluxc.example.AppSecretsModule;
import org.wordpress.android.fluxc.module.AppContextModule;
import org.wordpress.android.fluxc.module.ReleaseBaseModule;
import org.wordpress.android.fluxc.module.ReleaseNetworkModule;
import org.wordpress.android.fluxc.module.ReleaseStoreModule;

import javax.inject.Singleton;

import dagger.Component;

// Same module stack as the Release App component.
@Singleton
@Component(modules = {
        AppContextModule.class,
        AppSecretsModule.class,
        ReleaseBaseModule.class,
        ReleaseNetworkModule.class,
        ReleaseStoreModule.class
})
public interface ReleaseStack_AppComponent {
    void inject(ReleaseStack_AccountTest test);
    void inject(ReleaseStack_DiscoveryTest test);
    void inject(ReleaseStack_PostTestWPCom test);
    void inject(ReleaseStack_PostTestXMLRPC test);
    void inject(ReleaseStack_SiteTestXMLRPC test);
    void inject(ReleaseStack_SiteTestWPCom test);
}
