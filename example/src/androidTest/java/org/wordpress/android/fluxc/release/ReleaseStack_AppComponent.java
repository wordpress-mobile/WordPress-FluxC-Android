package org.wordpress.android.fluxc.release;

import org.wordpress.android.fluxc.example.AppSecretsModule;
import org.wordpress.android.fluxc.module.AppContextModule;
import org.wordpress.android.fluxc.module.MockedToolsModule;
import org.wordpress.android.fluxc.module.ReleaseBaseModule;
import org.wordpress.android.fluxc.module.ReleaseNetworkModule;
import org.wordpress.android.fluxc.module.ReleaseOkHttpClientModule;
import org.wordpress.android.fluxc.module.ReleaseStoreModule;
import org.wordpress.android.fluxc.module.ReleaseToolsModule;

import javax.inject.Singleton;

import dagger.Component;

// Same module stack as the Release App component.
@Singleton
@Component(modules = {
        AppContextModule.class,
        AppSecretsModule.class,
        ReleaseOkHttpClientModule.class,
        ReleaseBaseModule.class,
        ReleaseNetworkModule.class,
        ReleaseStoreModule.class,
        ReleaseToolsModule.class,
        MockedToolsModule.class
})
public interface ReleaseStack_AppComponent {
    void inject(ReleaseStack_AccountTest test);
    void inject(ReleaseStack_AccountAvailabilityTest test);
    void inject(ReleaseStack_CommentTestWPCom test);
    void inject(ReleaseStack_CommentTestXMLRPC test);
    void inject(ReleaseStack_DiscoveryTest test);
    void inject(ReleaseStack_FluxCImageLoaderTest test);
    void inject(ReleaseStack_MediaTestJetpack test);
    void inject(ReleaseStack_MediaTestWPCom test);
    void inject(ReleaseStack_MediaTestXMLRPC test);
    void inject(ReleaseStack_PostTestWPCom test);
    void inject(ReleaseStack_PostTestXMLRPC test);
    void inject(ReleaseStack_SiteTestJetpack test);
    void inject(ReleaseStack_SiteTestWPCom test);
    void inject(ReleaseStack_SiteTestXMLRPC test);
    void inject(ReleaseStack_TaxonomyTestWPCom test);
    void inject(ReleaseStack_TaxonomyTestXMLRPC test);
    void inject(ReleaseStack_PluginTestJetpack test);
    void inject(ReleaseStack_PluginInfoTest test);
}
