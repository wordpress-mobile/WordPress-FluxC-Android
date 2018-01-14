package org.wordpress.android.fluxc.instaflux;

import com.facebook.stetho.okhttp3.StethoInterceptor;

import dagger.Module;
import dagger.Provides;
import okhttp3.Interceptor;

@Module
public class InterceptorModule {
    @Provides
    public Interceptor provideNetworkInterceptor() {
        return new StethoInterceptor();
    }
}
