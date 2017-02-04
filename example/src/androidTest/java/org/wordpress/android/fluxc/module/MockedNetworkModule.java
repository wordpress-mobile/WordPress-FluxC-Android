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
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder;
import org.wordpress.android.fluxc.network.rest.wpapi.BaseWPAPIRestClient;
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
import org.wordpress.android.fluxc.network.xmlrpc.BaseXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.media.MediaXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.post.PostXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient;

import javax.inject.Named;
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
    @Singleton
    @Provides
    public OkHttpClient.Builder provideOkHttpClient() {
        return new OkHttpClient.Builder();
    }

    @Singleton
    @Provides
    public RequestQueue provideRequestQueue(OkHttpClient.Builder okHttpClient, Context appContext) {
        return Volley.newRequestQueue(appContext, new OkHttpStack(okHttpClient));
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
    public BaseXMLRPCClient provideBaseXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue, AccessToken token,
                                                    UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        return new BaseXMLRPCClient(dispatcher, requestQueue, token, userAgent, httpAuthManager);
    }

    @Singleton
    @Provides
    public BaseWPAPIRestClient provideBaseWPAPIClient(Dispatcher dispatcher, RequestQueue requestQueue,
                                                       UserAgent userAgent) {
        return new BaseWPAPIRestClient(dispatcher, requestQueue, userAgent);
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
                                                  @Named("regular") RequestQueue requestQueue,
                                                  @Named("regular") OkHttpClient.Builder okHttpClient,
                                                  AccessToken token, UserAgent userAgent) {
        return new MediaRestClient(appContext, dispatcher, requestQueue, okHttpClient, token, userAgent);
    }

    @Singleton
    @Provides
    public MediaXMLRPCClient provideMediaXMLRPCClient(Dispatcher dispatcher, OkHttpClient.Builder okClient,
                                                      @Named("regular") RequestQueue requestQueue,
                                                      AccessToken token, UserAgent userAgent,
                                                      HTTPAuthManager httpAuthManager) {
        return new MediaXMLRPCClient(dispatcher, requestQueue, okClient, token, userAgent, httpAuthManager);
    }

    @Singleton
    @Provides
    public SiteXMLRPCClient provideSiteXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue, AccessToken token,
                                                    UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        return new SiteXMLRPCClient(dispatcher, requestQueue, token, userAgent, httpAuthManager);
    }

    @Singleton
    @Provides
    public PostRestClient providePostRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue,
                                                AppSecrets appSecrets, AccessToken token, UserAgent userAgent) {
        return new PostRestClient(appContext, dispatcher, requestQueue, token, userAgent);
    }

    @Singleton
    @Provides
    public PostXMLRPCClient providePostXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue, AccessToken token,
                                                    UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        return new PostXMLRPCClient(dispatcher, requestQueue, token, userAgent, httpAuthManager);
    }

    @Singleton
    @Provides
    public AccountRestClient provideAccountRestClient(Context appContext, Dispatcher dispatcher, RequestQueue
            requestQueue, AppSecrets appSecrets, AccessToken token, UserAgent userAgent) {
        return new AccountRestClient(appContext, dispatcher, requestQueue, appSecrets, token, userAgent);
    }

    @Singleton
    @Provides
    public SelfHostedEndpointFinder provideSelfHostedEndpointFinder(Dispatcher dispatcher,
                                                                    BaseXMLRPCClient baseXMLRPCClient,
                                                                    BaseWPAPIRestClient baseWPAPIRestClient) {
        return new SelfHostedEndpointFinder(dispatcher, baseXMLRPCClient, baseWPAPIRestClient);
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
