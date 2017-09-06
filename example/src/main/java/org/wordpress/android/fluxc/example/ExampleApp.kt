package org.wordpress.android.fluxc.example

import android.app.Application

import com.yarolegovich.wellsql.WellSql

import org.wordpress.android.fluxc.module.AppContextModule
import org.wordpress.android.fluxc.persistence.WellSqlConfig

open class ExampleApp : Application() {
    open val component: AppComponent by lazy {
        DaggerAppComponent.builder()
                .appContextModule(AppContextModule(applicationContext))
                .build()
    }

    override fun onCreate() {
        super.onCreate()
        component().inject(this)
        WellSql.init(WellSqlConfig(applicationContext))
    }

    fun component(): AppComponent = component
}
