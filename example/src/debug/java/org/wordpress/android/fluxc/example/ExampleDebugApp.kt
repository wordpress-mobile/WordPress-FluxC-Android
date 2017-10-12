package org.wordpress.android.fluxc.example

import com.facebook.stetho.Stetho

import org.wordpress.android.fluxc.module.AppContextModule

class ExampleDebugApp : ExampleApp() {
    override val component: AppComponent by lazy {
        DaggerAppComponentDebug.builder()
                .appContextModule(AppContextModule(applicationContext))
                .build()
    }

    override fun onCreate() {
        super.onCreate()
        Stetho.initializeWithDefaults(this)
    }
}
