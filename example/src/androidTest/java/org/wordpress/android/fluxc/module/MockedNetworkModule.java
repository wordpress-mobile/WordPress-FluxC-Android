package org.wordpress.android.fluxc.module;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
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
import org.wordpress.android.fluxc.network.rest.wpcom.auth.Authenticator.ErrorListener;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.Authenticator.Listener;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.Authenticator.Token;
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.media.MediaXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.post.PostXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;

@Module
public class MockedNetworkModule {
    // Induces the mocked media upload to fail when set as the author id of the MediaModel
    public static final int MEDIA_FAILURE_AUTHOR_CODE = 31337;

    // Induces the mocked request to fail when
    public static final long FAILURE_SITE_ID = 11111;

    @Singleton
    @Provides
    public OkHttpClient.Builder provideOkHttpClientBuilder() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.addInterceptor(new ResponseMockingInterceptor());
        return builder;
    }

    @Singleton
    @Provides
    public OkHttpClient provideOkHttpClientInstance(OkHttpClient.Builder builder) {
        return builder
                .addInterceptor(new ResponseMockingInterceptor())
                .build();
    }

    @Singleton
    @Provides
    public RequestQueue provideRequestQueue(OkHttpClient.Builder okHttpClientBuilder, Context appContext) {
        return Volley.newRequestQueue(appContext, new OkHttpStack(okHttpClientBuilder));
    }

    @Singleton
    @Provides
    public Authenticator provideAuthenticator(Dispatcher dispatcher, AppSecrets appSecrets, RequestQueue requestQueue) {
        Authenticator authenticator = new Authenticator(dispatcher, requestQueue, appSecrets);
        Authenticator spy = spy(authenticator);

        // Mock Authenticator with correct user: test/test
        doAnswer(
                new Answer() {
                    public Object answer(InvocationOnMock invocation) {
                        Object[] args = invocation.getArguments();
                        Listener listener = (Listener) args[4];
                        listener.onResponse(new Token("deadparrot", "", "", "", ""));
                        return null;
                    }
                }
        ).when(spy).authenticate(eq("test"), eq("test"), anyString(), anyBoolean(),
                (Listener) any(), (ErrorListener) any());

        // Mock Authenticator with erroneous user: error/error
        doAnswer(
                new Answer() {
                    public Object answer(InvocationOnMock invocation) {
                        Object[] args = invocation.getArguments();
                        ErrorListener listener = (ErrorListener) args[5];
                        listener.onErrorResponse(null);
                        return null;
                    }
                }
        ).when(spy).authenticate(eq("error"), eq("error"), anyString(), anyBoolean(),
                (Listener) any(), (ErrorListener) any());
        return spy;
    }

    @Singleton
    @Provides
    public UserAgent provideUserAgent(Context appContext) {
        return new UserAgent(appContext);
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
    public AccessToken provideAccountToken(Context appContext) {
        return new AccessToken(appContext);
    }

    @Singleton
    @Provides
    public HTTPAuthManager provideHTTPAuthManager() {
        return new HTTPAuthManager();
    }
}
