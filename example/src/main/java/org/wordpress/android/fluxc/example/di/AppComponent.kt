package org.wordpress.android.fluxc.example.di

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import org.wordpress.android.fluxc.di.WCDatabaseModule
import org.wordpress.android.fluxc.example.ExampleApp
import org.wordpress.android.fluxc.module.DatabaseModule
import org.wordpress.android.fluxc.module.OkHttpClientModule
import org.wordpress.android.fluxc.module.ReleaseNetworkModule
import javax.inject.Singleton

@Singleton
@Component(modules = [
        AndroidInjectionModule::class,
        ApplicationModule::class,
        AppConfigModule::class,
        OkHttpClientModule::class,
        ReleaseNetworkModule::class,
        MainActivityModule::class,
        WCOrderListActivityModule::class,
        WCDatabaseModule::class,
        DatabaseModule::class])
interface AppComponent : AndroidInjector<ExampleApp> {
    override fun inject(app: ExampleApp)

    // Allows us to inject the application without having to instantiate any modules, and provides the Application
    // in the app graph
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        fun build(): AppComponent
    }
}
