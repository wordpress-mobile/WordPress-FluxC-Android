package org.wordpress.android.fluxc.example.di

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import org.wordpress.android.fluxc.module.DatabaseModule
import org.wordpress.android.fluxc.module.OkHttpClientModule
import org.wordpress.android.fluxc.module.ReleaseNetworkModule
import javax.inject.Singleton
import org.wordpress.android.fluxc.di.WCDatabaseModule

@Singleton
@Component(modules = [
        AndroidInjectionModule::class,
        ApplicationModule::class,
        AppConfigModule::class,
        InterceptorModule::class,
        OkHttpClientModule::class,
        ReleaseNetworkModule::class,
        MainActivityModule::class,
        DatabaseModule::class,
        WCDatabaseModule::class,
        WCOrderListActivityModule::class])
interface AppComponentDebug : AppComponent {
    @Component.Builder
    interface Builder : AppComponent.Builder {
        @BindsInstance
        override fun application(application: Application): AppComponentDebug.Builder
    }
}
