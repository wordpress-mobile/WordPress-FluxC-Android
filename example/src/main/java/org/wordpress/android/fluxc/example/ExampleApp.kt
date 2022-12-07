package org.wordpress.android.fluxc.example

import android.app.Application
import com.yarolegovich.wellsql.WellSql
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import org.wordpress.android.fluxc.example.di.AppComponent
import org.wordpress.android.fluxc.example.di.DaggerAppComponent
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import javax.inject.Inject

open class ExampleApp : Application(), HasAndroidInjector {
    @Inject lateinit var androidInjector: DispatchingAndroidInjector<Any>
    @Inject lateinit var applicationPasswordsLogger: ApplicationPasswordsLogger

    protected open val component: AppComponent by lazy {
        DaggerAppComponent.builder()
                .application(this)
                .build()
    }

    override fun onCreate() {
        super.onCreate()
        component.inject(this)
        val wellSqlConfig = WellSqlConfig(applicationContext, WellSqlConfig.ADDON_WOOCOMMERCE)
        WellSql.init(wellSqlConfig)
        applicationPasswordsLogger.init()
    }

    override fun androidInjector(): AndroidInjector<Any> = androidInjector
}
