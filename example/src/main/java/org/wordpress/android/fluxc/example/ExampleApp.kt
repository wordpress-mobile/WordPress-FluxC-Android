package org.wordpress.android.fluxc.example

import android.app.Activity
import android.app.Application
import com.yarolegovich.wellsql.WellSql
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import org.wordpress.android.fluxc.example.di.AppComponent
import org.wordpress.android.fluxc.example.di.DaggerAppComponent
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import javax.inject.Inject

open class ExampleApp : Application(), HasActivityInjector {
    @Inject lateinit var activityInjector: DispatchingAndroidInjector<Activity>

    open val component: AppComponent by lazy {
        DaggerAppComponent.builder()
                .application(this)
                .build()
    }

    override fun onCreate() {
        super.onCreate()
        component.inject(this)
        WellSql.init(WellSqlConfig(applicationContext))
    }

    override fun activityInjector(): AndroidInjector<Activity> = activityInjector
}
