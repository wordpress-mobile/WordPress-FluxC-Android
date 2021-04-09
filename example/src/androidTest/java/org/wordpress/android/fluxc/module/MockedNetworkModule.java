package org.wordpress.android.fluxc.module;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.wordpress.android.fluxc.network.OkHttpStack;

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
    public OkHttpClient.Builder provideOkHttpClientBuilder(ResponseMockingInterceptor responseMockingInterceptor) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.addInterceptor(responseMockingInterceptor);
        return builder;
    }

    @Singleton
    @Provides
    @Named("regular")
    public OkHttpClient provideRegularOkHttpClientInstance(OkHttpClient.Builder builder) {
        return builder.build();
    }

    @Singleton
    @Provides
    @Named("custom-ssl")
    public OkHttpClient provideCustomOkHttpClientInstance(OkHttpClient.Builder builder) {
        return builder.build();
    }

    @Singleton
    @Provides
    @Named("no-redirects")
    public OkHttpClient provideNoRedirectOkHttpClientInstance(OkHttpClient.Builder builder) {
        return builder.build();
    }

    @Singleton
    @Named("regular")
    @Provides
    public RequestQueue provideRegularRequestQueue(OkHttpClient.Builder okHttpClientBuilder,
                                            Context appContext) {
        return Volley.newRequestQueue(appContext, new OkHttpStack(okHttpClientBuilder));
    }

    @Singleton
    @Named("no-redirects")
    @Provides
    public RequestQueue provideNoRedirectsRequestQueue(OkHttpClient.Builder okHttpClientBuilder,
                                                       Context appContext) {
        return provideRegularRequestQueue(okHttpClientBuilder, appContext);
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
}
