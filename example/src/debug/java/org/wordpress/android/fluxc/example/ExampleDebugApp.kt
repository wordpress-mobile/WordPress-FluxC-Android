package org.wordpress.android.fluxc.example

import com.facebook.stetho.Stetho

class ExampleDebugApp : ExampleApp() {
    override val component: AppComponent by lazy {
        DaggerAppComponentDebug.builder()
                .application(this)
                .build()
    }

    override fun onCreate() {
        super.onCreate()
        Stetho.initializeWithDefaults(this)
    }
}
