package org.wordpress.android.fluxc.example.di

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import org.wordpress.android.fluxc.module.DebugOkHttpClientModule
import org.wordpress.android.fluxc.module.ReleaseNetworkModule
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(
        AndroidInjectionModule::class,
        ApplicationModule::class,
        AppConfigModule::class,
        InterceptorModule::class,
        DebugOkHttpClientModule::class,
        ReleaseNetworkModule::class,
        MainActivityModule::class,
        WCOrderListActivityModule::class))
interface AppComponentDebug : AppComponent {
    @Component.Builder
    interface Builder : AppComponent.Builder {
        @BindsInstance
        override fun application(application: Application): AppComponentDebug.Builder
    }
}
