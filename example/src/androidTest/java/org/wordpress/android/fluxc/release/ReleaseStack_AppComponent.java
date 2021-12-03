package org.wordpress.android.fluxc.release;

import org.wordpress.android.fluxc.di.WCDatabaseModule;
import org.wordpress.android.fluxc.example.di.AppConfigModule;
import org.wordpress.android.fluxc.module.AppContextModule;
import org.wordpress.android.fluxc.module.DatabaseModule;
import org.wordpress.android.fluxc.module.MockedToolsModule;
import org.wordpress.android.fluxc.module.OkHttpClientModule;
import org.wordpress.android.fluxc.module.ReleaseNetworkModule;
import org.wordpress.android.fluxc.module.ReleaseToolsModule;

import javax.inject.Singleton;

import dagger.Component;

// Same module stack as the Release App component.
@Singleton
@Component(modules = {
        AppContextModule.class,
        AppConfigModule.class,
        OkHttpClientModule.class,
        ReleaseNetworkModule.class,
        ReleaseToolsModule.class,
        MockedToolsModule.class,
        DatabaseModule.class,
        WCDatabaseModule.class
})
public interface ReleaseStack_AppComponent {
    void inject(ReleaseStack_AccountTest test);
    void inject(ReleaseStack_AccountAvailabilityTest test);
    void inject(ReleaseStack_ActivityLogTestJetpack test);
    void inject(ReleaseStack_ScanTestJetpack test);
    void inject(ReleaseStack_InsightsTestJetpack test);
    void inject(ReleaseStack_TimeStatsTestJetpack test);
    void inject(ReleaseStack_TimeStatsTestWPCom test);
    void inject(ReleaseStack_ManageInsightsTestJetpack test);
    void inject(ReleaseStack_CommentTestWPCom test);
    void inject(ReleaseStack_CommentTestXMLRPC test);
    void inject(ReleaseStack_DiscoveryTest test);
    void inject(ReleaseStack_FluxCImageLoaderTest test);
    void inject(ReleaseStack_MediaTestJetpack test);
    void inject(ReleaseStack_MediaTestWPCom test);
    void inject(ReleaseStack_MediaTestXMLRPC test);
    void inject(ReleaseStack_NotificationTest test);
    void inject(ReleaseStack_StockMediaTest test);
    void inject(ReleaseStack_WPOrgPluginTest test);
    void inject(ReleaseStack_PluginTestJetpack test);
    void inject(ReleaseStack_PostTestWPCom test);
    void inject(ReleaseStack_PostTestXMLRPC test);
    void inject(ReleaseStack_ReaderTest test);
    void inject(ReleaseStack_SiteTestJetpack test);
    void inject(ReleaseStack_SiteTestWPCom test);
    void inject(ReleaseStack_SiteTestXMLRPC test);
    void inject(ReleaseStack_TaxonomyTestWPCom test);
    void inject(ReleaseStack_TaxonomyTestXMLRPC test);
    void inject(ReleaseStack_ThemeTestJetpack test);
    void inject(ReleaseStack_ThemeTestWPCom test);
    void inject(ReleaseStack_UploadTest test);
    void inject(ReleaseStack_WCBaseStoreTest test);
    void inject(ReleaseStack_WCOrderTest test);
    void inject(ReleaseStack_WCOrderExtTest test);
    void inject(ReleaseStack_WCProductTest test);
    void inject(ReleaseStack_VerticalTest test);
    void inject(ReleaseStack_PlanOffersTest test);
    void inject(ReleaseStack_PostListTestWpCom test);
    void inject(ReleaseStack_PostListTestXMLRPC test);
    void inject(ReleaseStack_TransactionsTest test);
    void inject(ReleaseStack_WCOrderListTest test);
    void inject(ReleaseStack_PostSchedulingTestJetpack test);
    void inject(ReleaseStack_ReactNativeWPAPIRequestTest test);
    void inject(ReleaseStack_ReactNativeWPComRequestTest test);
    void inject(ReleaseStack_EncryptedLogTest test);
    void inject(ReleaseStack_XPostsTest test);
    void inject(ReleaseStack_NoRedirectsTest test);
    void inject(ReleaseStack_InPersonPaymentsWCPayTest test);
    void inject(ReleaseStack_InPersonPaymentsStripeExtensionTest test);

    void inject(ReleaseStack_WPApiPluginTest test);

    void inject(ReleaseStack_CardsTest test);
}
