package org.wordpress.android.fluxc.mocked;

import org.wordpress.android.fluxc.example.di.AppSecretsModule;
import org.wordpress.android.fluxc.module.AppContextModule;
import org.wordpress.android.fluxc.module.MockedNetworkModule;
import org.wordpress.android.fluxc.module.ReleaseBaseModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        AppContextModule.class,
        AppSecretsModule.class,
        ReleaseBaseModule.class,
        MockedNetworkModule.class, // Mocked module
})
public interface MockedNetworkAppComponent {
    void inject(MockedStack_AccountTest object);
    void inject(MockedStack_JetpackTunnelTest object);
    void inject(MockedStack_SiteTest object);
    void inject(MockedStack_UploadStoreTest object);
    void inject(MockedStack_UploadTest object);
}
