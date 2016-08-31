package org.wordpress.android.fluxc.mocked;

import org.wordpress.android.fluxc.example.AppSecretsModule;
import org.wordpress.android.fluxc.module.AppContextModule;
import org.wordpress.android.fluxc.module.ReleaseBaseModule;
import org.wordpress.android.fluxc.module.ReleaseStoreModule;
import org.wordpress.android.fluxc.module.MockedNetworkModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        AppContextModule.class,
        AppSecretsModule.class,
        ReleaseBaseModule.class,
        MockedNetworkModule.class, // Mocked module
        ReleaseStoreModule.class
})
public interface MockedNetworkAppComponent {
    void inject(AccountStoreTest object);
    void inject(MockedStack_SiteTest object);
}

