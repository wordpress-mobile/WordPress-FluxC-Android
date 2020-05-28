package org.wordpress.android.fluxc.module;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.OkHttpStack;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.discovery.DiscoveryWPAPIRestClient;
import org.wordpress.android.fluxc.network.discovery.DiscoveryXMLRPCClient;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.Authenticator;
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.notifications.NotificationRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient;
import org.wordpress.android.fluxc.network.wporg.plugin.PluginWPOrgClient;
import org.wordpress.android.fluxc.network.xmlrpc.media.MediaXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.post.PostXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.Dispatchers;
import okhttp3.OkHttpClient;

@Module
public class MockedNetworkModule {
    @Singleton
    @Provides
    public ResponseMockingInterceptor provideResponseMockingInterceptor() {
        return new ResponseMockingInterceptor();
    }

    @Singleton
    @Provides
    public OkHttpClient.Builder provideOkHttpClientBuilder(ResponseMockingInterceptor responseMockingInterceptor) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.addInterceptor(responseMockingInterceptor);
        return builder;
    }

    @Singleton
    @Provides
    public OkHttpClient provideOkHttpClientInstance(OkHttpClient.Builder builder) {
        return builder.build();
    }

    @Singleton
    @Provides
    public RequestQueue provideRequestQueue(OkHttpClient.Builder okHttpClientBuilder, Context appContext) {
        return Volley.newRequestQueue(appContext, new OkHttpStack(okHttpClientBuilder));
    }

    @Singleton
    @Named("regular")
    @Provides
    public RequestQueue provideRegularRequestQueue(OkHttpClient.Builder okHttpClientBuilder,
                                            Context appContext) {
        return Volley.newRequestQueue(appContext, new OkHttpStack(okHttpClientBuilder));
    }

    @Singleton
    @Named("custom-ssl")
    @Provides
    public RequestQueue provideCustomRequestQueue(OkHttpClient.Builder okHttpClientBuilder,
                                                   Context appContext) {
        return Volley.newRequestQueue(appContext, new OkHttpStack(okHttpClientBuilder));
    }

    @Singleton
    @Provides
    public CoroutineContext provideCoroutineContext() {
        return Dispatchers.getDefault();
    }

    @Singleton
    @Provides
    public Authenticator provideAuthenticator(Context appContext, Dispatcher dispatcher, AppSecrets appSecrets,
                                              RequestQueue requestQueue) {
        return new Authenticator(appContext, dispatcher, requestQueue, appSecrets);
    }

    @Singleton
    @Provides
    public SiteRestClient provideSiteRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue,
                                                AppSecrets appSecrets,
                                                AccessToken token, UserAgent userAgent) {
        return new SiteRestClient(appContext, dispatcher, requestQueue, appSecrets, token, userAgent);
    }

    @Singleton
    @Provides
    public MediaRestClient provideMediaRestClient(Dispatcher dispatcher, Context appContext,
                                                  RequestQueue requestQueue,
                                                  OkHttpClient okHttpClient,
                                                  AccessToken token, UserAgent userAgent) {
        return new MediaRestClient(appContext, dispatcher, requestQueue, okHttpClient, token, userAgent);
    }

    @Singleton
    @Provides
    public MediaXMLRPCClient provideMediaXMLRPCClient(Dispatcher dispatcher, OkHttpClient okHttpClient,
                                                      RequestQueue requestQueue,
                                                      UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        return new MediaXMLRPCClient(dispatcher, requestQueue, okHttpClient, userAgent, httpAuthManager);
    }

    @Singleton
    @Provides
    public SiteXMLRPCClient provideSiteXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue,
                                                    UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        return new SiteXMLRPCClient(dispatcher, requestQueue, userAgent, httpAuthManager);
    }

    @Singleton
    @Provides
    public PostRestClient providePostRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue,
                                                AppSecrets appSecrets, AccessToken token, UserAgent userAgent) {
        return new PostRestClient(appContext, dispatcher, requestQueue, token, userAgent);
    }

    @Singleton
    @Provides
    public PostXMLRPCClient providePostXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue,
                                                    UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        return new PostXMLRPCClient(dispatcher, requestQueue, userAgent, httpAuthManager);
    }

    @Singleton
    @Provides
    public AccountRestClient provideAccountRestClient(Context appContext, Dispatcher dispatcher, RequestQueue
            requestQueue, AppSecrets appSecrets, AccessToken token, UserAgent userAgent) {
        return new AccountRestClient(appContext, dispatcher, requestQueue, appSecrets, token, userAgent);
    }

    @Singleton
    @Provides
    public NotificationRestClient provideNotificationRestClient(Context appContext, Dispatcher dispatcher,
                                                                RequestQueue requestQueue,
                                                                AccessToken token, UserAgent userAgent) {
        return new NotificationRestClient(appContext, dispatcher, requestQueue, token, userAgent);
    }

    @Singleton
    @Provides
    public DiscoveryXMLRPCClient provideDiscoveryXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue,
                                                              UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        return new DiscoveryXMLRPCClient(dispatcher, requestQueue, userAgent, httpAuthManager);
    }

    @Singleton
    @Provides
    public DiscoveryWPAPIRestClient provideDiscoveryWPAPIRestClient(Dispatcher dispatcher, RequestQueue requestQueue,
                                                              UserAgent userAgent) {
        return new DiscoveryWPAPIRestClient(dispatcher, requestQueue, userAgent);
    }

    @Singleton
    @Provides
    public SelfHostedEndpointFinder provideSelfHostedEndpointFinder(Dispatcher dispatcher,
                                                                    DiscoveryXMLRPCClient discoveryXMLRPCClient,
                                                                    DiscoveryWPAPIRestClient discoveryWPAPIRestClient) {
        return new SelfHostedEndpointFinder(dispatcher, discoveryXMLRPCClient, discoveryWPAPIRestClient);
    }

    @Singleton
    @Provides
    public PluginRestClient providePluginRestClient(Context appContext, Dispatcher dispatcher,
                                                    RequestQueue requestQueue,
                                                    AccessToken token, UserAgent userAgent) {
        return new PluginRestClient(appContext, dispatcher, requestQueue, token, userAgent);
    }

    @Singleton
    @Provides
    public PluginWPOrgClient providePluginWPOrgClient(Dispatcher dispatcher,
                                                      RequestQueue requestQueue,
                                                      UserAgent userAgent) {
        return new PluginWPOrgClient(dispatcher, requestQueue, userAgent);
    }

    @Singleton
    @Provides
    public AccessToken provideAccountToken(Context appContext) {
        return new AccessToken(appContext);
    }

    @Singleton
    @Provides
    public HTTPAuthManager provideHTTPAuthManager() {
        return new HTTPAuthManager();
    }
}
