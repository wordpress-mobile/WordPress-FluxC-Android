package org.wordpress.android.fluxc.mocked;

import org.wordpress.android.fluxc.example.di.AppConfigModule;
import org.wordpress.android.fluxc.module.AppContextModule;
import org.wordpress.android.fluxc.module.MockedNetworkModule;
import org.wordpress.android.fluxc.module.MockedWCNetworkModule;
import org.wordpress.android.fluxc.module.ReleaseBaseModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        AppContextModule.class,
        AppConfigModule.class,
        ReleaseBaseModule.class,
        MockedNetworkModule.class, // Mocked module
        MockedWCNetworkModule.class // Mocked module
})
public interface MockedNetworkAppComponent {
    void inject(MockedStack_AccountTest object);
    void inject(MockedStack_CacheTest object);
    void inject(MockedStack_JetpackTunnelTest object);
    void inject(MockedStack_MediaTest object);
    void inject(MockedStack_NotificationTest object);
    void inject(MockedStack_PluginTest object);
    void inject(MockedStack_SiteTest object);
    void inject(MockedStack_UploadStoreTest object);
    void inject(MockedStack_UploadTest object);
    void inject(MockedStack_WCBaseStoreTest object);
    void inject(MockedStack_WCOrdersTest object);
    void inject(MockedStack_WCProductsTest object);
    void inject(MockedStack_WCStatsTest object);
}
