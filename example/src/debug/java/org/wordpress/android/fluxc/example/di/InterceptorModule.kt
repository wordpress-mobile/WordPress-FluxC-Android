package org.wordpress.android.fluxc.example.di

import com.facebook.stetho.okhttp3.StethoInterceptor

import dagger.Module
import dagger.Provides
import okhttp3.Interceptor

@Module
class InterceptorModule {
    @Provides
    fun provideNetworkInterceptor(): Interceptor = StethoInterceptor()
}
