package org.wordpress.android.fluxc.module;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.wordpress.android.fluxc.module.MockedNetworkModule.MockedNetworkModuleBindings;
import org.wordpress.android.fluxc.network.OkHttpStack;
import org.wordpress.android.fluxc.network.rest.JsonObjectOrEmptyArray;
import org.wordpress.android.fluxc.network.rest.JsonObjectOrEmptyArrayDeserializer;
import org.wordpress.android.fluxc.network.rest.JsonObjectOrFalse;
import org.wordpress.android.fluxc.network.rest.JsonObjectOrFalseDeserializer;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.Dispatchers;
import okhttp3.OkHttpClient;

@Module(includes = MockedNetworkModuleBindings.class)
public class MockedNetworkModule {
    @Module
    interface MockedNetworkModuleBindings {
        @Named("regular")
        @Binds OkHttpClient bindsRegularOkHttpClient(OkHttpClient okHttpClient);

        @Named("custom-ssl")
        @Binds OkHttpClient provideCustomSslOkHttpClient(OkHttpClient okHttpClient);

        @Named("no-redirects")
        @Binds OkHttpClient bindsNoRedirectsOkHttpClient(OkHttpClient okHttpClient);
    }

    @Provides
    public OkHttpClient provideOkHttpClientInstance(ResponseMockingInterceptor responseMockingInterceptor) {
        return new OkHttpClient.Builder()
                .addInterceptor(responseMockingInterceptor)
                .build();
    }

    @Singleton
    @Provides
    public RequestQueue provideRequestQueue(@Named("regular") OkHttpClient okHttpClient, Context appContext) {
        return Volley.newRequestQueue(appContext, new OkHttpStack(okHttpClient));
    }

    @Singleton
    @Named("regular")
    @Provides
    public RequestQueue provideRegularRequestQueue(@Named("regular") OkHttpClient okHttpClient,
                                                          Context appContext) {
        return Volley.newRequestQueue(appContext, new OkHttpStack(okHttpClient));
    }

    @Singleton
    @Named("no-redirects")
    @Provides
    public RequestQueue provideNoRedirectsRequestQueue(@Named("no-redirects") OkHttpClient okHttpClient,
                                                              Context appContext) {
        return provideRegularRequestQueue(okHttpClient, appContext);
    }

    @Singleton
    @Named("custom-ssl")
    @Provides
    public RequestQueue provideCustomRequestQueue(@Named("custom-ssl") OkHttpClient okHttpClient,
                                                         Context appContext) {
        return Volley.newRequestQueue(appContext, new OkHttpStack(okHttpClient));
    }

    @Singleton
    @Provides
    public CoroutineContext provideCoroutineContext() {
        return Dispatchers.getDefault();
    }

    @Provides
    public Gson provideGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setLenient();
        gsonBuilder.registerTypeHierarchyAdapter(JsonObjectOrFalse.class, new JsonObjectOrFalseDeserializer());
        gsonBuilder.registerTypeHierarchyAdapter(JsonObjectOrEmptyArray.class,
                new JsonObjectOrEmptyArrayDeserializer());
        return gsonBuilder.create();
    }
}
