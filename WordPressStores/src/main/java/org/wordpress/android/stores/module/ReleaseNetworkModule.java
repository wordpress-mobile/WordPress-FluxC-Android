package org.wordpress.android.stores.module;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.network.HTTPAuthManager;
import org.wordpress.android.stores.network.MemorizingTrustManager;
import org.wordpress.android.stores.network.OkHttpStack;
import org.wordpress.android.stores.network.UserAgent;
import org.wordpress.android.stores.network.rest.wpcom.account.AccountRestClient;
import org.wordpress.android.stores.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.stores.network.rest.wpcom.auth.AppSecrets;
import org.wordpress.android.stores.network.rest.wpcom.auth.Authenticator;
import org.wordpress.android.stores.network.rest.wpcom.site.SiteRestClient;
import org.wordpress.android.stores.network.xmlrpc.site.SiteXMLRPCClient;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.inject.Singleton;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.OkUrlFactory;

@Module
public class ReleaseNetworkModule {

    @Provides
    public OkHttpClient provideOkHttpClient(MemorizingTrustManager memorizingTrustManager) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        // TODO: We should provide a "normal" client (or with a pinned certificate trust manager) for wpcom requests
        try {
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{memorizingTrustManager}, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            builder.sslSocketFactory(sslSocketFactory);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        } catch (NoSuchAlgorithmException e) {
            // noop
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        return builder.build();
    }

    @Singleton
    @Provides
    public OkUrlFactory provideOkUrlFactory(OkHttpClient okHttpClient) {
        return new OkUrlFactory(okHttpClient);
    }

    @Singleton
    @Provides
    public RequestQueue provideRequestQueue(OkUrlFactory okUrlFactory, Context appContext) {
        return Volley.newRequestQueue(appContext, new OkHttpStack(okUrlFactory));
    }

    @Singleton
    @Provides
    public Authenticator provideAuthenticator(AppSecrets appSecrets, RequestQueue requestQueue) {
        return new Authenticator(requestQueue, appSecrets);
    }

    @Singleton
    @Provides
    public UserAgent provideUserAgent(Context appContext) {
        return new UserAgent(appContext);
    }

    @Singleton
    @Provides
    public SiteRestClient provideSiteRestClient(Dispatcher dispatcher, RequestQueue requestQueue, AccessToken token,
                                                UserAgent userAgent) {
        return new SiteRestClient(dispatcher, requestQueue, token, userAgent);
    }

    @Singleton
    @Provides
    public SiteXMLRPCClient provideSiteXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue, AccessToken token,
                                                    UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        return new SiteXMLRPCClient(dispatcher, requestQueue, token, userAgent, httpAuthManager);
    }

    @Singleton
    @Provides
    public AccountRestClient provideAccountRestClient(Dispatcher dispatcher, RequestQueue requestQueue,
                                                      AccessToken token, UserAgent userAgent) {
        return new AccountRestClient(dispatcher, requestQueue, token, userAgent);
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

    @Singleton
    @Provides
    public MemorizingTrustManager provideMemorizingTrustManager(Context appContext) {
        return new MemorizingTrustManager(appContext);
    }
}
