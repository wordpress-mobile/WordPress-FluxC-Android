package org.wordpress.android.fluxc.mocked;

import org.wordpress.android.fluxc.di.WCDatabaseModule;
import org.wordpress.android.fluxc.example.di.AppConfigModule;
import org.wordpress.android.fluxc.module.AppContextModule;
import org.wordpress.android.fluxc.module.MockedNetworkModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        AppContextModule.class,
        AppConfigModule.class,
        MockedNetworkModule.class,
        WCDatabaseModule.class
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
    void inject(MockedStack_EditorThemeStoreTest object);
    void inject(MockedStack_WCPayTest object);
}
